/*
 *
 * CRViewer -- Computer co-reference chain statistics.
 * 
 * Copyright 2016-2017 Bruno Oberlé.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * This program comes with ABSOLUTELY NO WARRANTY.  See the Mozilla Public
 * License, v. 2.0 for more details.
 * 
 * Some questions about the license may have been answered at
 * https://www.mozilla.org/en-US/MPL/2.0/FAQ/.
 * 
 * If you have any question, contact me at boberle.com.
 * 
 * The source code can be found at boberle.com.
 *
 */

package crviewer;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;

public class FileParser {

   private ArrayList<String> additionnal_tokens;
   private Text text;

   private String getWord(Line line) {
      return getWord(line, false);
   }

   private String getWord(Line line, boolean acceptHyphen) {
      return getWord(line, acceptHyphen, false);
   }
   
   private String getWord(Line line, boolean acceptHyphen, boolean acceptUnderscore) {
      line.eatWhite();
      int start = line.pos;
      while (line.nextChar() != 0 &&
            (Character.isLetterOrDigit(line.nextChar())
                  || (acceptHyphen && line.nextChar() == '-')
                  || (acceptUnderscore && line.nextChar() == '_'))) line.pos++;
      if (line.nextChar() == '\'') line.pos++;
      return line.text.substring(start, line.pos);
   }
   
   private String getQuote(Line line) throws ParseException {
      line.eatWhite();
      if (line.nextChar() != '"') throw new ParseException("can't parse line (no quote): " + line, Line.getLineCounter());
      line.pos++;
      int start = line.pos;
      while (line.nextChar() != 0 && line.nextChar() != '"') line.pos++;
      if (line.nextChar() == 0) throw new ParseException("can't parse line (no quote): " + line, Line.getLineCounter());
      return line.text.substring(start, line.pos++);
   }
      
   private void eatEqualOrDie(Line line) throws ParseException {
      line.eatWhite();
      if (line.nextChar() != '=') {
         throw new ParseException("can't parse line (no equal): " + line, Line.getLineCounter());
      }
      line.pos++;
   }

   private void eatColonOrDie(Line line) throws ParseException {
      line.eatWhite();
      if (line.nextChar() != ':') {
         throw new ParseException("can't parse line (no colon): " + line, Line.getLineCounter());
      }
      line.pos++;
   }

   private String getRestOfLine(Line line) {
      line.eatWhite();
      return line.text.substring(line.pos);
   }

   private void parseParagraph(Line line, ArrayList<Token> tokens, ArrayList<Annotation> annotations)
         throws ParseException {
      while (line.nextChar() != 0) {
         for (String token: this.additionnal_tokens) {
            if (line.nextis(token)) {
               tokens.add(new WordToken(token));
               line.pos += token.length();
            }
         }
         if (line.nextChar() == '{') {
            line.pos++;
            getAnnotation(line, tokens, annotations);
         } else if (line.nextChar() == '}') {
            line.pos++;
            return;
         } else if (Character.isLetterOrDigit(line.nextChar())) {
            tokens.add(new WordToken(getWord(line)));
         } else if (line.nextChar() == ' ') {
            tokens.add(new SpaceToken());
            line.pos++;
            line.eatWhite();
         } else {
            tokens.add(new PunctToken(String.valueOf(line.nextChar())));
            line.pos++;
         }
      }
   }

   private void getAnnotation(Line line, ArrayList<Token> tokens, ArrayList<Annotation> annotations)
         throws ParseException {
      Annotation annot = new Annotation(getWord(line, false, true), Token.getTokenCount(), tokens);
      annotations.add(annot);
      annot.setStart(tokens.size());
      if (line.nextChar() == ':') {
         do {
            line.pos++;
            String key = getWord(line, false, true);
            if (key.equals("")) throw new ParseException("can't parse line (property): " + line, Line.getLineCounter());
            eatEqualOrDie(line);
            String val = getWord(line);
            if (val.equals("")) val = getQuote(line);
            annot.addProperty(key, val);
         } while (line.nextChar() == ',');
      }
      line.pos++;
      parseParagraph(line, tokens, annotations);
      annot.setEnd(tokens.size()-1);
   }

   public FileParser(String filename) throws IOException,ParseException {
      this.additionnal_tokens = new ArrayList<>();
      this.text = new Text(filename);
      File file = new File(filename);
      text.setTextId(file.getName());
      Line.resetLineCounter();
      int partCounter = 1;
      int parCounter = 1;
      Part part = new Part(partCounter);
      System.out.println("Parsing file: "+filename);
      BufferedReader f = new BufferedReader(new FileReader(filename));
      boolean quit = false;
      while (!quit) {
         StringBuilder string = new StringBuilder();
         while (true) {
            String buf = f.readLine();
            if (buf == null) {
               quit = true;
               break;
            }
            string.append(buf);
            if (!buf.endsWith("\\n")) break;
            string.replace(string.length()-2, string.length(), "");
            string.append(' ');
         }
         Line line = new Line(string.toString());
         if (line.isempty()) {
            continue;
         } else if (line.isseparator()) {
            if (!part.getParagraphs().isEmpty()) {
               this.text.addPart(part);
               part = new Part(++partCounter);
            }
         } else if (line.text.startsWith("#")) {
            line.pos++;
            line.eatWhite();
            if (line.endOfLine()) continue;
            String key = getWord(line, true);
            try {
               eatColonOrDie(line);
               String val = getRestOfLine(line).trim();
               if (val.equals("") || val == null) {
                  throw new ParseException("can't understand `"+key+"'.", Line.getLineCounter());
                  // the line will be ignored
               }
               //System.out.printf("Found: key=%s, val=%s\n", key, val);
               if (key.equals("additionnaltoken") || key.equals("additionnal_token")) {
                  this.additionnal_tokens.add(val);
                  System.out.println("Found additional token: "+val);
               } else if (key.equals("part-type") || key.equals("parttype")) {
                  part.setType(val);
               } else if (key.equals("textid")) {
                  text.setTextId(val);
               } else if (key.equals("part-heading") || key.equals("partheading")) {
                  //TODO
               } else {
                  throw new ParseException("unknown line: '"+line.text+"'", Line.getLineCounter());
                  // the line will be ignored
               }
            } catch (ParseException err) {
               System.out.println("Ignoring line: '"+line.text+"'");
            }
         } else {
            Paragraph par = new Paragraph(parCounter++);
            parseParagraph(line, par.getTokens(), par.getAnnotations());
            part.addParagraph(par);
            //System.out.println(par);
         }
      }
      if (!part.getParagraphs().isEmpty()) {
         this.text.addPart(part);
      }
      f.close();
   }
   
   public Text getText() {
      return this.text;
   }

}


class Line{
   private static int lineCounter = 0;
   public String text;
   public int pos;
   public Line(String text) {
      this.text = text.trim();
      this.pos = 0;
      Line.lineCounter++;
   }
   public static int getLineCounter() {
      return Line.lineCounter;
   }
   public static void resetLineCounter() {
      Line.lineCounter = 0;
   }
   public char nextChar() {
      if (this.pos < this.text.length()) {
         return this.text.charAt(this.pos);
      }
      return 0;
   }
   public boolean nextis(String test) {
      return this.text.startsWith(test, this.pos);
   }
   public String toString() {
      return this.text;
   }
   public boolean isempty() {
      return this.text.equals("");
   }
   public void eatWhite() {
      while (this.nextChar() != 0 && this.nextChar() == ' ') this.pos++;
   }
   public boolean isseparator() {
      for (int i = 0; i < this.text.length(); i++) {
         if (this.text.charAt(i) != '*') return false;
      }
      return true;
   }
   public boolean endOfLine() {
      return !(this.pos < this.text.length());
   }
 
}
