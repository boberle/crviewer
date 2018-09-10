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

public class Part extends TextChunk implements HasParagraphs {
   private ArrayList<Paragraph> paragraphs;
   private String type;
   private int partId;
   private String textId;
   
   public Part(int partId) {
      this.paragraphs = new ArrayList<>();
      this.annotations = new ArrayList<>();
      this.type = "";
      this.partId = partId;
      this.textId = null;
   }

   public void addParagraph(Paragraph par) {
      par.setTextId(this.textId);
      par.setPartId(this.partId);
      paragraphs.add(par);
      annotations.addAll(par.getAnnotations());
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }
   
   public ArrayList<Paragraph> getParagraphs() {
      return paragraphs;
   }
   
   public int getPartId() {
      return this.partId;
   }
   
   public void setPartId(int id) {
      this.partId = id;
      for (Paragraph par : paragraphs) {
         par.setPartId(this.partId);
      }
   }

   public String getTextId() {
      return this.textId;
   }
   
   public void setTextId(String textId) {
      this.textId = textId;
      for (Paragraph par : paragraphs) {
         par.setTextId(this.textId);
      }
   }
   
   public String getId() {
      return String.format("%ss%02d", this.textId, this.partId);
   }
   
   public int getTokenCount() {
      int sum = 0;
      for (Paragraph par : this.paragraphs) {
         sum += par.getTokenCount();
      }
      return sum;
   }

   
   public int getWordTokenCount() {
      int sum = 0;
      for (Paragraph par : this.paragraphs) {
         sum += par.getWordTokenCount();
      }
      return sum;
   }
   
   public int getParagraphCount() {
      return this.getParagraphs().size();
   }


}
