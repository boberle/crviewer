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

public class Corpus extends TextChunk implements HasParagraphs, HasParts {
   private ArrayList<Text> texts;

   public ArrayList<Text> getTexts() {
      return texts;
   }

   public ArrayList<Paragraph> getParagraphs() {
      ArrayList<Paragraph> pars = new ArrayList<>();
      for (Text text : this.texts) {
         for (Part part : text.getParts()) {
            pars.addAll(part.getParagraphs());
         }
      }
      return pars;
   }
   
   public ArrayList<Part> getParts() {
      ArrayList<Part> parts = new ArrayList<>();
      for (Text text : this.texts) {
         parts.addAll(text.getParts());
      }
      return parts;
   }

   public Corpus() {
      this.texts = new ArrayList<>();
      this.annotations = new ArrayList<>();
   }

   public void addText(Text text) {
      texts.add(text);
      this.annotations.addAll(text.getAnnotations());
   }

   public String getId() {
      return "corpus";
   }

   public int getTokenCount() {
      int sum = 0;
      for (Text text : this.texts) {
         for (Part part : text.getParts()) {
            for (Paragraph par : part.getParagraphs()) {
               sum += par.getTokenCount();
            }
         }
      }
      return sum;
   }

   public int getWordTokenCount() {
      int sum = 0;
      for (Text text : this.texts) {
         for (Part part : text.getParts()) {
            for (Paragraph par : part.getParagraphs()) {
               sum += par.getWordTokenCount();
            }
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


   @Override
   public double getAverageLinkToLinkDistance(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         sum += chain.getAverageLinkToLinkDistance();
         count++;
      }
      return ((double)sum) / ((double)count);
   }
   
   @Override
   public double getStabilityCoeff(ChainCollection chainColl) {
      double sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         sum += chain.getStabilityCoeff();
         count++;
      }
      return ((double)sum) / ((double)count);
   }
   
   @Override
   public double getAverageLinkLength(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         sum += chain.getAverageLinkLength();
         count++;
      }
      return ((double)sum) / ((double)count);
   }
   
   @Override
   public int getChainCount(ChainCollection chainColl) {
      return chainColl.getChains().length;
   }

   @Override
   public int getLinkCount(ChainCollection chainColl) {
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         count += chain.getAnnotations().size();
      }
      return count;
   }

}
