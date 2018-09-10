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

import java.util.*;

public class PartType extends TextChunk implements HasParagraphs {
   private ArrayList<Part> parts;
   private Set<String> partIds;
   private String type;

   public PartType(String type, Corpus corpus, String... moreTypes) {
      this.type = type;
      this.annotations = new ArrayList<>();
      this.parts = new ArrayList<>();
      this.partIds = new HashSet<>();
      Set<String> types = new HashSet<String>();
      types.add(type);
      for (String s : moreTypes) types.add(s);
      //System.out.println("Creating PartType for " + this.type);
      for (Part part : corpus.getParts()) {
         if (types.contains(part.getType())) {
            //System.out.println("... adding " + part.getType());
            parts.add(part);
            partIds.add(part.getId());
            this.annotations.addAll(part.getAnnotations());
         } else {
            //System.out.println("... rejecting " + part.getType());
         }
      }
   }


   public ArrayList<Part> getParts() {
      return parts;
   }
   
   public String getType() {
      return this.type;
   }
   
   public String getId() {
      return String.format("%s", this.type);
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

   @Override
   public double getAverageLinkToLinkDistance(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.partIds.contains(chain.getId())) {
            sum += chain.getAverageLinkToLinkDistance();
            count++;
         }
      }
      return ((double)sum) / ((double)count);
   }
   
   @Override
   public double getAverageLinkLength(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.partIds.contains(chain.getId())) {
            sum += chain.getAverageLinkLength();
            count++;
         }
      }
      return ((double)sum) / ((double)count);
   }
   
   @Override
   public int getChainCount(ChainCollection chainColl) {
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.partIds.contains(chain.getId())) {
            count++;
         }
      }
      return count;
   }

   @Override
   public int getLinkCount(ChainCollection chainColl) {
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.partIds.contains(chain.getId())) {
            count += chain.getAnnotations().size();
         }
      }
      return count;
   }
   
   public ArrayList<Paragraph> getParagraphs() {
      ArrayList<Paragraph> pars = new ArrayList<>();
      for (Part part : this.parts) {
         pars.addAll(part.getParagraphs());
      }
      return pars;
   }
   
   public int getParagraphCount() {
      return this.getParagraphs().size();
   }

   
   
}
