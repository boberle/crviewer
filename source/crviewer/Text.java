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
import java.util.HashSet;

public class Text extends TextChunk implements HasParagraphs, HasParts {
   private static int textCounter = 0;
   private static HashSet<String> textIds = new HashSet<>();
   private String filename;
   private String title;
   private ArrayList<Part> parts;
   private String textId;

   public String getFilename() {
      return filename;
   }

   public void setFilename(String filename) {
      this.filename = filename;
   }

   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public ArrayList<Part> getParts() {
      return parts;
   }

   public ArrayList<Paragraph> getParagraphs() {
      ArrayList<Paragraph> pars = new ArrayList<>();
      for (Part part : this.parts) {
         pars.addAll(part.getParagraphs());
      }
      return pars;
   }

   public Text(String filename) {
      this.filename = filename;
      this.title = "";
      this.parts = new ArrayList<>();
      this.annotations = new ArrayList<>();
      this.textId = String.format("t%02d", Text.textCounter);
      Text.textCounter++;
   }

   public void addPart(Part part) {
      part.setTextId(this.textId);
      parts.add(part);
      this.annotations.addAll(part.getAnnotations());
   }

   public void setTextId(String textId) {
      if (Text.textIds.contains(textId)) {
         throw new RuntimeException("The text id: `"+textId+"' is already used.");
      }
      Text.textIds.add(textId);
      this.textId = textId;
      for (Part part : parts) {
         part.setTextId(this.textId);
      }
   }

   public String getId() {
      return this.textId;
   }

   public int getTokenCount() {
      int sum = 0;
      for (Part part : this.parts) {
         for (Paragraph par : part.getParagraphs()) {
            sum += par.getTokenCount();
         }
      }
      return sum;
   }
   
   public int getWordTokenCount() {
      int sum = 0;
      for (Part part : this.parts) {
         for (Paragraph par : part.getParagraphs()) {
            sum += par.getWordTokenCount();
         }
      }
      return sum;
   }

   public int getParagraphCount() {
      return this.getParagraphs().size();
   }
   
   public int getPartCount() {
      return this.getParts().size();
   }

}
