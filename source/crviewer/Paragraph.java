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

public class Paragraph extends TextChunk {
   private ArrayList<Token> tokens;
   private String textId;
   private int partId;
   private int parId;

   public Paragraph(int parId) {
      tokens = new ArrayList<>();
      annotations = new ArrayList<>();
      this.parId = parId;
      this.partId = 0;
      this.textId = null;
   }

   public String toString() {
      String res = "List of annotations:\n";
      for (Annotation annot : annotations) {
         res += " - " + annot.getLeftContext() + "*" + annot.getText() + "*" + annot.getRightContext() + "\n"
               + annot.toString();
      }
      res += tokens.toString();
      return res;
   }

   public ArrayList<Annotation> getAnnotations() {
      return annotations;
   }

   public ArrayList<Token> getTokens() {
      return tokens;
   }

   public String getTextId() {
      return textId;
   }

   public void setTextId(String textId) {
      this.textId = textId;
   }

   public int getPartId() {
      return partId;
   }

   public void setPartId(int partId) {
      this.partId = partId;
   }

   public int getParId() {
      return parId;
   }

   public void setParId(int parId) {
      this.parId = parId;
   }

   public String getId() {
      return String.format("%ss%02dp%02d", this.textId, this.partId, this.parId);
   }

   @Override
   public int getTokenCount() {
      return this.tokens.size();
   }
   
   @Override
   public int getWordTokenCount() {
      int res = 0;
      for (Token token : this.tokens) {
         if (token instanceof WordToken) res++;
      }
      return res;
   }

}
