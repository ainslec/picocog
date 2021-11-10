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
   private static final String      SEP                        = "\n";
   private static final String      DI                         = "   " ;
   private int                      indents                    = -1;
   private int                      numLines                   = 0;
   private boolean                  generateIfEmpty            = true;
   private boolean                  generate                   = true;
   boolean                          normalizeAdjacentBlankRows = false;
   
   private boolean                  isDirty                    = false;
   private List<String[]>           rows                       = new ArrayList<>(); // Used for aligning columns in the multi string writeln method.
   private List<PicoWriterItem>     content                    = new ArrayList <PicoWriterItem>();
   private StringBuilder            stringBuilder              = new StringBuilder();
   private String                   identChars                 = DI;

   public PicoWriter () {
      indents = 0;
   }
   public PicoWriter (String indentText) {
      indents = 0;
      identChars = indentText == null ? DI : indentText;
   }
   private PicoWriter (int initialIndent, String indentText) {
      indents = initialIndent < 0 ? 0 : initialIndent;
      identChars = indentText == null ? DI : indentText;
   }
   public void indentRight() {
      flushRows();
      indents++;
   }
   public void indentLeft() {
      flushRows();
      indents--;
      if (indents < 0) {
         throw new RuntimeException("Local indent cannot be less than zero");
      }
   }

   public final PicoWriter createDeferredWriter() {
      
      if (stringBuilder.length() > 0) {
         flush();
         numLines++;
      }
      
      PicoWriter inner = new PicoWriter(indents, identChars);
      content.add(inner);
      numLines++;
      
      return inner;
   }
   
   public final PicoWriter writeln(PicoWriter inner) {
      
      if (stringBuilder.length() > 0) {
         flush();
         numLines++;
      }
      
      adjustIndents(inner, indents, identChars);
      
      content.add(inner);
      numLines++;
      
      return this;
   }
   
   private void adjustIndents(PicoWriter inner, int indents, String ic) {
      if (inner != null) {
         for ( PicoWriterItem item : inner.content) {
            if (item instanceof PicoWriter) {
               adjustIndents((PicoWriter) item, indents, ic);
            } else if (item instanceof IndentedLine) {
               IndentedLine il = (IndentedLine) item;
               il.indent = il.indent + indents;
            }
         }
         inner.identChars = ic;
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
      numLines++;
      stringBuilder.append(string);
      flush();
      return this;
   }
   
   /**
    * Use this if you wish to align a series of columns
    * @param strings An array of strings that should represent columns at the current indentation level.
    * @return Returns the current instance of the {@link PicoWriter} object
    */
   public PicoWriter writeln(String ... strings) {
      rows.add(strings);
      isDirty = true;
      numLines++;
      return this;
   }
   
   /**
    * Writest a line, then returns a li
    * @param startLine
    * @param endLine
    * @return
    */
   public PicoWriter createDeferredIndentedWriter(String startLine, String endLine) {
      writeln(startLine);
      indentRight();
      PicoWriter ggg = createDeferredWriter();
      indentLeft();
      writeln(endLine);
      isDirty = true;
      numLines+=2;
      return ggg;
   }
   
   
   public boolean isEmpty() {
      return numLines == 0;
   }
   
   public void write(String string)  {
      numLines++;
      isDirty = true;
      stringBuilder.append(string);
   }
   
   private static final void writeIndentedLine(final StringBuilder sb, final int indentBase, final String indentText, final String line) {
      for (int indentIndex = 0; indentIndex < indentBase; indentIndex++) {
         sb.append(indentText);
      }
      sb.append(line);
      sb.append(SEP);
   }
   
   private boolean render(StringBuilder sb, int indentBase, boolean normalizeAdjacentBlankRows, boolean lastRowWasBlank) {
      
      if (isDirty) {
         flush();
      }
      
      // Some methods are flagged not to be generated if there is no body text inside the method, we don't add these to the class narrative
      if ((!isGenerate()) || ((!isGenerateIfEmpty()) && isMethodBodyEmpty())) {
         return lastRowWasBlank;
      }
       // TODO :: Will make this configurable
      for (PicoWriterItem item : content) {
         if (item instanceof IndentedLine) {
            IndentedLine il           = (IndentedLine)item;
            final String lineText     = il.getLine();
            final int indentLevelHere = indentBase + il.getIndent();
            boolean thisRowIsBlank    = lineText.length() == 0;

            if (normalizeAdjacentBlankRows && lastRowWasBlank && thisRowIsBlank) {
               // Don't write the line if we already had a blank line
            } else {
               writeIndentedLine(sb, indentLevelHere, identChars, lineText);
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
      return content.size() == 0 && stringBuilder.length() == 0;
   }
   
   public boolean isGenerateIfEmpty() {
      return generateIfEmpty;
   }
   
   public void setGenerateIfEmpty(boolean generateIfEmpty) {
      this.generateIfEmpty = generateIfEmpty;
   }
   
   public boolean isGenerate() {
      return generate;
   }
   
   public void setGenerate(boolean generate) {
      this.generate = generate;
   }
   
   private void flush() {
      flushRows();
      content.add(new IndentedLine(stringBuilder.toString(), indents));
      stringBuilder.setLength(0);
      isDirty = false;
   }
   
   private void flushRows() {
      if (rows.size() > 0) {
         ArrayList<Integer> maxWidth = new ArrayList<>();
         for (String[] columns : rows) {
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
         
         for (String[] columns : rows) {
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
            content.add(new IndentedLine(rowSB.toString(), indents));
            rowSB.setLength(0);
         }
         rows.clear();
      }
   }
   

   public void setNormalizeAdjacentBlankRows(boolean normalizeAdjacentBlankRows) {
      this.normalizeAdjacentBlankRows = normalizeAdjacentBlankRows;
   }
   
   public String toString(int indentBase) {
      StringBuilder sb = new StringBuilder();
      render(sb, indentBase, normalizeAdjacentBlankRows, false /* lastRowWasBlank */);
      return sb.toString();
   }
   
   public String toString() {
      return toString(0);
   }
   
   
}
