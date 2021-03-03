/*
 * Copyright 2017 - 2021, Chris Ainsley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ainslec.picocog;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny code generation library
 * @author Chris Ainsley
 */
public class PicoWriter implements PicoWriterItem {
   private static final String      SEP                         = "\n";
   private static final String      DI                          = "   " ;
   private int                      _indents                    = -1;
   private int                      _numLines                   = 0;
   private boolean                  _generateIfEmpty            = true;
   private boolean                  _generate                   = true;
   boolean                          _normalizeAdjacentBlankRows = false;
   
   private boolean                  _isDirty                    = false;
   private List<String[]>           _rows                       = new ArrayList<>(); // Used for aligning columns in the multi string writeln method.
   private List<PicoWriterItem>     _content                    = new ArrayList <PicoWriterItem>();
   private StringBuilder            _sb                         = new StringBuilder();
   private String                   _ic   /* Indent chars*/     = DI;

   public PicoWriter () {
      _indents = 0;
   }
   public PicoWriter (String indentText) {
      _indents = 0;
      _ic = indentText == null ? DI : indentText;
   }
   private PicoWriter (int initialIndent, String indentText) {
      _indents = initialIndent < 0 ? 0 : initialIndent;
      _ic = indentText == null ? DI : indentText;
   }
   public void indentRight() {
      flushRows();
      _indents++;
   }
   public void indentLeft() {
      flushRows();
      _indents--;
      if (_indents < 0) {
         throw new RuntimeException("Local indent cannot be less than zero");
      }
   }

   public final PicoWriter createDeferredWriter() {
      
      if (_sb.length() > 0) {
         flush();
         _numLines++;
      }
      
      PicoWriter inner = new PicoWriter(_indents, _ic);
      _content.add(inner);
      _numLines++;
      
      return inner;
   }
   
   public final PicoWriter writeln(PicoWriter inner) {
      
      if (_sb.length() > 0) {
         flush();
         _numLines++;
      }
      
      adjustIndents(inner, this._indents, this._ic);
      
      _content.add(inner);
      _numLines++;
      
      return this;
   }
   
   private void adjustIndents(PicoWriter inner, int indents, String ic) {
      if (inner != null) {
         for ( PicoWriterItem item : inner._content) {
            if (item instanceof PicoWriter) {
               adjustIndents((PicoWriter) item, indents, ic);
            } else if (item instanceof IndentedLine) {
               IndentedLine il = (IndentedLine) item;
               il._indent = il._indent + indents;
            }
         }
         inner._ic = ic;
      }
   }
   
   public PicoWriter writeln_r(String string) {
      writeln(string);
      indentRight();
      return this;
   }
   
   public PicoWriter writeln_l(String string) {
      flushRows();
      indentLeft();
      writeln(string);
      return this;
   }
   
   public PicoWriter writeln_lr(String string) {
      flushRows();
      indentLeft();
      writeln(string);
      indentRight();
      return this;
   }
   
   public PicoWriter writeln(String string) {
      _numLines++;
      _sb.append(string);
      flush();
      return this;
   }
   
   /**
    * Use this if you wish to align a series of columns
    * @param strings An array of strings that should represent columns at the current indentation level.
    * @return Returns the current instance of the {@link PicoWriter} object
    */
   public PicoWriter writeln(String ... strings) {
      _rows.add(strings);
      _isDirty = true;
      _numLines++;
      return this;
   }
   
   public boolean isEmpty() {
      return _numLines == 0;
   }
   
   public void write(String string)  {
      _numLines++;
      _isDirty = true;
      _sb.append(string);
   }
   
   private static final void writeIndentedLine(final StringBuilder sb, final int indentBase, final String indentText, final String line) {
      for (int indentIndex = 0; indentIndex < indentBase; indentIndex++) {
         sb.append(indentText);
      }
      sb.append(line);
      sb.append(SEP);
   }
   
   private boolean render(StringBuilder sb, int indentBase, boolean normalizeAdjacentBlankRows, boolean lastRowWasBlank) {
      
      if (_isDirty) {
         flush();
      }
      
      // Some methods are flagged not to be generated if there is no body text inside the method, we don't add these to the class narrative
      if ((!isGenerate()) || ((!isGenerateIfEmpty()) && isMethodBodyEmpty())) {
         return lastRowWasBlank;
      }
       // TODO :: Will make this configurable
      for (PicoWriterItem item : _content) {
         if (item instanceof IndentedLine) {
            IndentedLine il           = (IndentedLine)item;
            final String lineText     = il.getLine();
            final int indentLevelHere = indentBase + il.getIndent();
            boolean thisRowIsBlank    = lineText.length() == 0;

            if (normalizeAdjacentBlankRows && lastRowWasBlank && thisRowIsBlank) {
               // Don't write the line if we already had a blank line
            } else {
               writeIndentedLine(sb, indentLevelHere, _ic, lineText);
            }
            
            lastRowWasBlank = thisRowIsBlank;
         } else if (item instanceof PicoWriter) {
            lastRowWasBlank = ((PicoWriter)item).render(sb, indentBase, normalizeAdjacentBlankRows, lastRowWasBlank);
         } else {
            String string = item.toString();
            sb.append(string);
         }
      }
      
      return lastRowWasBlank;
   }

   public boolean isMethodBodyEmpty() {
      return _content.size() == 0 && _sb.length() == 0;
   }
   
   public boolean isGenerateIfEmpty() {
      return _generateIfEmpty;
   }
   
   public void setGenerateIfEmpty(boolean generateIfEmpty) {
      _generateIfEmpty  = generateIfEmpty;
   }
   
   public boolean isGenerate() {
      return _generate;
   }
   
   public void setGenerate(boolean generate) {
      _generate = generate;
   }
   
   private void flush() {
      flushRows();
      _content.add(new IndentedLine(_sb.toString(), _indents));
      _sb.setLength(0);
      _isDirty = false;
   }
   
   private void flushRows() {
      if (_rows.size() > 0) {
         ArrayList<Integer> maxWidth = new ArrayList<>();
         for (String[] columns : _rows) {
            int numColumns = columns.length;
            for (int i=0; i < numColumns; i++) {
               String currentColumnStringValue = columns[i];
               int currentColumnStringValueLength = currentColumnStringValue == null ? 0 : currentColumnStringValue.length();
               if (maxWidth.size() < i+1) {
                  maxWidth.add(currentColumnStringValueLength);
               } else {
                  if (maxWidth.get(i) < currentColumnStringValueLength) {
                     maxWidth.set(i, currentColumnStringValueLength);
                  }
               }
            }
         }
         
         StringBuilder rowSB = new StringBuilder();
         
         for (String[] columns : _rows) {
            int numColumns = columns.length;
            for (int i=0; i < numColumns; i++) {
               String currentColumnStringValue = columns[i];
               int currentItemWidth = currentColumnStringValue == null ? 0 : currentColumnStringValue.length();
               int maxWidth1 = maxWidth.get(i);
               rowSB.append(currentColumnStringValue == null ? "" : currentColumnStringValue);
               
               if (currentItemWidth < maxWidth1) {
                  for (int j=currentItemWidth; j < maxWidth1; j++) {
                     rowSB.append(" "); // right pad
                  }
               }
            }
            _content.add(new IndentedLine(rowSB.toString(), _indents));
            rowSB.setLength(0);
         }
         _rows.clear();
      }
   }
   

   public void setNormalizeAdjacentBlankRows(boolean normalizeAdjacentBlankRows) {
      _normalizeAdjacentBlankRows = normalizeAdjacentBlankRows;
   }
   
   public String toString(int indentBase) {
      StringBuilder sb = new StringBuilder();
      render(sb, indentBase, _normalizeAdjacentBlankRows, false /* lastRowWasBlank */);
      return sb.toString();
   }
   
   public String toString() {
      return toString(0);
   }
}
