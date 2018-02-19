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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Annotation implements Comparable<Annotation> {
   public static int contextWidth = 5;
   private String refname;
   private ArrayList<Token> tokens;
   private ArrayList<Token> wordTokens;
   private HashMap<String, String> properties;
   private int start;
   private int end;
   private int index;
   private int headIndex;
   private String fullId;

   public Annotation(String refname, int index, ArrayList<Token> tokens) {
      this.refname = refname;
      this.tokens = tokens;
      this.start = 0;
      this.end = 0;
      this.index = index;
      this.headIndex = -1;
      this.fullId = "";
      this.properties = new HashMap<>();
   }

   public int getStart() {
      return start;
   }

   public int getEnd() {
      return end;
   }

   public void setStart(int start) {
      this.start = start;
   }

   public void setEnd(int end) {
      this.end = end;
   }
   
   public void setFullId(String fullId) {
      this.fullId = fullId;
   }
   
   public String getFullId() {
      return this.fullId;
   }

   public void addProperty(String key, String val) {
      //System.out.printf("add prop: %s, %s\n", key, val);
      if (key.equals("head")) {
         try {
            this.headIndex = Integer.valueOf(val);
         } catch (NumberFormatException e) {
            // nothing
         }
      } else if (key.equals("expansion")) {
         properties.put("expansion", val.equals("") ? "no" : "yes");
         properties.put("expansion-adj", val.indexOf("a")==-1 ? "no" : "yes");
         properties.put("expansion-verb", val.indexOf("v")==-1 ? "no" : "yes");
         properties.put("expansion-apposition", val.indexOf("p")==-1 ? "no" : "yes");
         properties.put("expansion-relative", val.indexOf("r")==-1 ? "no" : "yes");
         properties.put("expansion-noun", val.indexOf("n")==-1 ? "no" : "yes");
         properties.put("expansion-sub", val.indexOf("s")==-1 ? "no" : "yes");
         for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (c!='a' && c!='v' && c!='p' && c!='r' && c!='n' && c!='s') {
               System.out.println("** unknown expnasion: "+c);
               System.exit(1);
            }
         }
      } else {
         properties.put(key, val);
      }
   }
   
   public ArrayList<Token> getWordTokens() {
      if (this.wordTokens == null) {
         
         this.wordTokens = new ArrayList<>();
         for (int i = this.start; i <= this.end; i++) {
            if (this.tokens.get(i) instanceof WordToken) {
               this.wordTokens.add(this.tokens.get(i));
            }
         }
         /*System.out.println(this.getText());
         System.out.print("- list of of tokens: ");
         for (Token foo: this.wordTokens) {
            System.out.print("-- " + foo);
         }
         System.out.println(" --\n");*/
      }
      return this.wordTokens;
   }
   
   public String getHead() {
      try {
         String head = this.getWordTokens().get(this.headIndex).toString();
         //System.out.println("the head index is: "+this.headIndex+", the head is: "+head);
         return head;
      } catch (IndexOutOfBoundsException e) {
         return null;
      }
   }

   public String getProperty(String key) {
      return properties.get(key);
   }

   public boolean hasProperty(String key) {
      return properties.containsKey(key);
   }

   public Set<String> getPropertyList() {
      return this.properties.keySet();
   }

   public String toString() {
      String res = "Annotation: \"" + this.getText() + "\":\n";
      Set<String> keys = this.properties.keySet();
      for (String k : keys) {
         res += " - " + k + ": " + this.properties.get(k) + "\n";
      }
      return res;
   }

   public String getLeftContext() {
      String left = "";
      for (int c = 0, i = start - 1; 0 <= i; i--) {
         if ((tokens.get(i)) instanceof WordToken)
            c++;
         if (c > Annotation.contextWidth)
            break;
         left = tokens.get(i) + left;
      }
      return left;
   }

   public String getRightContext() {
      String right = "";
      for (int c = 0, i = end + 1; i < tokens.size(); i++) {
         if ((tokens.get(i)) instanceof WordToken)
            c++;
         if (c > Annotation.contextWidth)
            break;
         right += tokens.get(i);
      }
      return right;
   }

   public String getText() {
      String text = "";
      for (int i = start; i <= end; i++) {
         text += tokens.get(i);
      }
      return text;
   }
   
   public String getTextWithRightContext() {
      return "*"+this.getText()+"*"+this.getRightContext();
   }

   public String getRefname() {
      return this.refname;
   }

   public int getIndex() {
      return this.index;
   }

   public int getWordTokenCount() {
      int res = 0;
      for (int i = this.start; i <= this.end; i++) {
         if (this.tokens.get(i) instanceof WordToken) {
            res++;
         }
      }
      return res;
   }

   @Override
   public int compareTo(Annotation arg) {
      return this.index - arg.getIndex();
   }
}
