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
import java.util.regex.Pattern;

public class ChainFilter {
   
   private class PropertyFilter {
      public String name;
      public Pattern regex;
      public PropertyFilter(String name, String regex) {
         this.name = name;
         this.regex = Pattern.compile(regex);
      }
   }
   private int minSize;
   private Pattern refname;
   private Pattern textId;
   private ArrayList<PropertyFilter> properties;
   public ChainFilter(int minSize) {
      this.minSize = minSize;
      this.refname = null;
      this.textId = null;
      this.properties = new ArrayList<>();
   }
   public void setRefname(String refname) {
      if (refname.equals("")) {
         this.refname = null;
      } else {
         this.refname = Pattern.compile(refname);
      }
   }
   public void setTextId(String textId) {
      if (textId.equals("")) {
         this.textId = null;
      } else {
         this.textId = Pattern.compile(textId);
      }
   }
   public void addPropertyFilter(String name, String regex) {
      this.properties.add(new PropertyFilter(name, regex));
   }
   public boolean checkChunkTextId(TextChunk chunk) {
      return this.textId == null || this.textId.matcher(chunk.getId()).find();
   }
   public boolean checkAnnotation(Annotation annot) {
      if (this.refname != null && !this.refname.matcher(annot.getRefname()).find()) return false;
      /*for (PropertyFilter filter : this.properties) {
         if (!annot.hasProperty(filter.name)) return false;
         if (!filter.regex.matcher(annot.getProperty(filter.name)).find()) return false;
         //if (!annot.getProperty(filter.name).matches(filter.regex)) return false;
      }*/
      return true;
   }
   public boolean checkAnnotationWithPropertyFilter(Annotation annot) {
      for (PropertyFilter filter : this.properties) {
         if (!annot.hasProperty(filter.name)) return false;
         if (!filter.regex.matcher(annot.getProperty(filter.name)).find()) return false;
         //if (!annot.getProperty(filter.name).matches(filter.regex)) return false;
      }
      return true;
   }
   public boolean checkChain(Chain chain) {
      return chain.getSize() >= this.minSize;
   }
}
