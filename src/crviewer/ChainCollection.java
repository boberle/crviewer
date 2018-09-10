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

enum SplitByType {
   text, part, paragraph
}

public class ChainCollection {

   private Chain[] chains;
   private ArrayList<Annotation> annotations;
   private HashMap<String,ArrayList<String>> propertyMap;
   private ArrayList<String> propertyList;

   public ChainCollection(Corpus corpus, SplitByType splitBy, ChainFilter filter) {
      ArrayList<Chain> tmpChains = new ArrayList<>();
      ArrayList<? extends TextChunk> chunks;
      if (splitBy == SplitByType.text) {
         chunks = corpus.getTexts();
      } else if (splitBy == SplitByType.part) {
         chunks = corpus.getParts();
      } else {
         chunks = corpus.getParagraphs();
      }
      for (TextChunk chunk : chunks) {
         if (!filter.checkChunkTextId(chunk)) continue;
         huntForChains(chunk, tmpChains, filter);
      }
      int size = 0;
      for (Chain chain : tmpChains) {
         if (filter.checkChain(chain)) size++;
      }
      this.chains = new Chain[size];
      int i = 0;
      for (Chain chain : tmpChains) {
         if (filter.checkChain(chain)) this.chains[i++] = chain;
      }
      this.applyPropertyFilter(tmpChains, filter);
      this.annotations = new ArrayList<Annotation>();
      for (Chain chain : this.chains) {
         this.annotations.addAll(chain.getAnnotations());
      }
      Collections.sort(this.annotations);
   }

   private void huntForChains(TextChunk chunk, ArrayList<Chain> tmpChains, ChainFilter filter) {
      for (Annotation annot : chunk.getAnnotations()) {
         if (!filter.checkAnnotation(annot)) continue;
         String refname = annot.getRefname();
         String chunkFullId = chunk.getId();
         boolean found = false;
         for (Chain chain : tmpChains) {
            if (chain.getRefname().equals(refname) && chain.getId().equals(chunkFullId)) {
               chain.addAnnotation(annot);
               found = true;
               break;
            }
         }
         if (!found) {
            Chain chain = new Chain(refname, chunkFullId);
            tmpChains.add(chain);
            chain.addAnnotation(annot);
         }
      }
   }
   
   private void applyPropertyFilter(ArrayList<Chain> tmpChains, ChainFilter filter) {
      for (Chain chain : tmpChains) {
         ArrayList<Annotation> newList = new ArrayList<>();
         for (Annotation annot : chain.getAnnotations()) {
            if (!filter.checkAnnotationWithPropertyFilter(annot)) {
               continue;
            }
            newList.add(annot);
         }
         chain.setAnnotations(newList);
      }
   }

   public boolean containsAnnotation(Annotation annot) {
      for (Chain chain : this.chains) {
         if (chain.getAnnotations().contains(annot))
            return true;
      }
      return false;
   }
   
   public Chain[] getChains() {
      return chains;
   }
   
   public HashMap<String,ArrayList<String>> getPropertyMap() {
      if (this.propertyMap == null) {
         this.propertyMap = new HashMap<>();
         for (Chain chain : this.chains) {
            for (Annotation annot : chain.getAnnotations()) {
               for (String propName : annot.getPropertyList()) {
                  if (!this.propertyMap.containsKey(propName)) {
                     this.propertyMap.put(propName, new ArrayList<>());
                  }
                  if (!this.propertyMap.get(propName).contains(annot.getProperty(propName))) {
                     this.propertyMap.get(propName).add(annot.getProperty(propName));
                  }
               }
            }
         }
      }
      this.propertyList = new ArrayList<>(this.propertyMap.keySet());
      Collections.sort(this.propertyList);
      return this.propertyMap;
   }
   
   public ArrayList<String> getPropertyList() {
      if (this.propertyList == null) {
         this.getPropertyMap();
      }
      return this.propertyList;
   }
   
   public ArrayList<Annotation> getAnnotations() {
      return this.annotations;
   }
}
