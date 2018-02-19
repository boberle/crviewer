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
import java.util.regex.Pattern;


public class Chain {
   public static String stabilityCoeffProperty = null;
   public static Pattern stabilityCoeffValue = null;
   private ArrayList<Annotation> annotations;
   private String refname;
   private String id;
   private boolean hasChanged;
   private double averageLinkToLinkDistance;
   private double averageLinkLength;
   private double stabilityCoeff;
   
   public Chain(String refname, String id) {
      this.annotations = new ArrayList<>();
      this.refname = refname;
      this.id = id;
      this.averageLinkToLinkDistance = 0;
      this.averageLinkLength = 0;
      this.stabilityCoeff = 0;
      this.hasChanged = false;
   }

   public void addAnnotation(Annotation annot) {
      this.hasChanged = true;
      annot.setFullId(this.getFullId());
      annotations.add(annot);
   }

   public int getSize() {
      return annotations.size();
   }

   public String getRefname() {
      return this.refname;
   }

   public String getId() {
      return this.id;
   }

   public String getFullId() {
      return String.format("%s:%s", this.id, this.refname);
   }

   public ArrayList<Annotation> getAnnotations() {
      this.computeStatistics();
      return annotations;
   }
   
   public void setAnnotations(ArrayList<Annotation> newList) {
      this.hasChanged = true;
      annotations = newList;
   }

   public void speak() {
      this.computeStatistics();
      String res = String.format("Chain \"%s\" (id: %s). List of annotations:\n", this.refname, this.id);
      for (Annotation annot : this.annotations) {
         res += String.format(" - %s (%d, %d)\n", annot.getRefname(), annot.getStart(), annot.getEnd());
         for (String property : annot.getPropertyList()) {
            res += String.format("   - %s: %s\n", property, annot.getProperty(property));
         }
      }
      System.out.print(res);
   }

   public void computeStatistics() {
      if (!this.hasChanged)
         return;
      // sort annotations
      Collections.sort(this.annotations);
      // gap
      if (this.annotations.size() <= 1) {
         this.averageLinkToLinkDistance = 0;
      } else {
         int sum = 0;
         for (int i = 1; i < this.annotations.size(); i++) {
            sum += this.annotations.get(i).getIndex() - this.annotations.get(i - 1).getIndex();
         }
         this.averageLinkToLinkDistance = ((double) sum) / ((double) this.annotations.size() - 1);
      }
      // average annotation length
      this.averageLinkLength = 0;
      for (Annotation annot : this.annotations) {
         this.averageLinkLength += annot.getWordTokenCount();
      }
      if (this.annotations.size() > 0) {
         this.averageLinkLength /= this.annotations.size();
      }
      // stability coeff
      this.stabilityCoeff = 0;
      int totalNumberOfDesignations = 0;
      HashSet<String> headSet = new HashSet<>();
      for (Annotation annot : this.annotations) {
         if (annot.hasProperty(Chain.stabilityCoeffProperty)
               && Chain.stabilityCoeffValue.matcher(annot.getProperty(Chain.stabilityCoeffProperty)).find()) {
            totalNumberOfDesignations++;
            String head = annot.getHead();
            if (head == null) {
               head = annot.getText();
            }
            headSet.add(head.toLowerCase());
         }
      }
      if (this.annotations.size() > 0) {
         // stab coeff is defined by 1-(x-1)/(n-1), where x is the number of *different*
         // designations and n the the number of designations
         int numberOfDifferentDesignations = headSet.size();
         if (totalNumberOfDesignations == 1) {
            this.stabilityCoeff = 1;
         } else {
            this.stabilityCoeff = 1.0 - ((double)(numberOfDifferentDesignations-1)) / ((double)(totalNumberOfDesignations-1));
         }
         //System.out.println("---");
         //System.out.println("Designation count: "+totalNumberOfDesignations);
         //System.out.println("Different designation count: "+numberOfDifferentDesignations);
         //System.out.println("coeff: " +this.stabilityCoeff);
      }
      // has changed
      this.hasChanged = false;
   }

   public double getAverageLinkToLinkDistance() {
      this.computeStatistics();
      return this.averageLinkToLinkDistance;
   }

   public double getAverageLinkLength() {
      this.computeStatistics();
      return this.averageLinkLength;
   }

   public int getPropertyFreq(String propName) {
      int freq = 0;
      for (Annotation annot : this.annotations) {
         // if (annot.getProperty(propName).equals(propValue)) freq++;
         if (annot.hasProperty(propName)) freq++;
      }
      return freq;
   }
   
   public int getPropertyFreq(String propName, String propValue) {
      int freq = 0;
      for (Annotation annot : this.annotations) {
         // if (annot.getProperty(propName).equals(propValue)) freq++;
         if (annot.hasProperty(propName) && annot.getProperty(propName).equals(propValue))
            freq++;
      }
      return freq;
   }
   
   public double getStabilityCoeff() {
      this.computeStatistics();
      return this.stabilityCoeff;      
   }
   
}
