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

public abstract class TextChunk {
   
   abstract public String getId();
   abstract public int getTokenCount();
   abstract public int getWordTokenCount();
   
   protected ArrayList<Annotation> annotations;
   private ArrayList<String> propertyList;
   private HashMap<String,HashMap<String,Integer>> freq;

   public ArrayList<Annotation> getAnnotations() {
      return this.annotations;
   }

   public int annotationCount() {
      return this.annotations.size();
   }

   public ArrayList<String> getPropertyList() {
      if (this.propertyList == null) {
         Set<String> props = new HashSet<>();
         for (Annotation annot : annotations) {
            props.addAll(annot.getPropertyList());
         }
         this.propertyList = new ArrayList<>(props);
         Collections.sort(this.propertyList);
      }
      return this.propertyList;
   }
   
   /*private void ComputeFreq() {
      if (this.freq != null) return;
      for (Annotation annot: this.annotations) {
         for (String propName : annot.getPropertyList()) {
            if (!this.freq.containsKey(propName)) {
               this.freq.put(propName, new HashMap<>());
            }
            String propValue = annot.getProperty(propName);
            if (!this.freq.get(propName).containsKey(propValue)) {
               this.freq.get(propName).put(propValue, 0);
            }
            this.freq.get(propName).put(propValue, this.freq.get(propName).get(propValue)+1);
         }
      }
   }*/
   
   public int getPropertyFreq(String propName, ChainCollection chainColl) {
      int freq = 0;
      for (Annotation annot: this.annotations) {
         if (annot.hasProperty(propName)
               && chainColl.containsAnnotation(annot)) freq++;
      }
      return freq;
   }

   public int getPropertyFreq(String propName, String propValue, ChainCollection chainColl) {
      int freq = 0;
      for (Annotation annot: this.annotations) {
         if (annot.hasProperty(propName)
               && chainColl.containsAnnotation(annot)
               && annot.getProperty(propName).equals(propValue)) freq++;
      }
      return freq;
   }
   
   public double getAverageLinkToLinkDistance(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.getId().equals(chain.getId())) {
            sum += chain.getAverageLinkToLinkDistance();
            count++;
         }
      }
      return ((double)sum) / ((double)count);
   }

   public double getStabilityCoeff(ChainCollection chainColl) {
      double sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.getId().equals(chain.getId())) {
            sum += chain.getStabilityCoeff();
            count++;
         }
      }
      return sum / ((double)count);
   }
   
   public double getAverageLinkLength(ChainCollection chainColl) {
      int sum = 0;
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.getId().equals(chain.getId())) {
            sum += chain.getAverageLinkLength();
            count++;
         }
      }
      return ((double)sum) / ((double)count);
   }
   
   public int getChainCount(ChainCollection chainColl) {
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.getId().equals(chain.getId())) {
            count++;
         }
      }
      return count;
   }

   public int getLinkCount(ChainCollection chainColl) {
      int count = 0;
      for (Chain chain : chainColl.getChains()) {
         if (this.getId().equals(chain.getId())) {
            count += chain.getAnnotations().size();
         }
      }
      return count;
   }

   public double getAverageChainSize(ChainCollection chainColl) {
      return ((double)this.getLinkCount(chainColl))/((double)this.getChainCount(chainColl));
   }

   public double getChainDensity(ChainCollection chainColl) {
      return ((double)this.getChainCount(chainColl)) / ((double)this.getWordTokenCount());
   }

   public double getLinkDensity(ChainCollection chainColl) {
      return ((double)this.getLinkCount(chainColl)) / ((double)this.getWordTokenCount());
   }

   public double getAnnotationDensity() {
      return ((double)this.annotations.size()) / ((double)this.getWordTokenCount());
   }


   public Set<String> getAllRefNames() {
      Set<String> res = new HashSet<String>();
      for (Annotation annot : this.annotations) {
         res.add(annot.getRefname());
      }
      return res;
   }

}

interface HasParagraphs {
   public int getParagraphCount();
}

interface HasParts {
   public int getPartCount();
}
