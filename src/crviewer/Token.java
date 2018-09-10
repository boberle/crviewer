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

public abstract class Token {
   protected static int tokenCount = 0;
   protected String text;
   public String toString() {
      return this.text;
   }
   public static int getTokenCount() {
      return tokenCount;
   }
}

class SpaceToken extends Token {
   public SpaceToken() {
      this.text = " ";
   }
}

class PunctToken extends Token {
   public PunctToken(String text) {
      this.text = text;
   }
}

class WordToken extends Token {
   public WordToken(String text) {
      Token.tokenCount++;
      this.text = text;
   }   
}
