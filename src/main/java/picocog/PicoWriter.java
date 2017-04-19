/*
 * Copyright 2017, Chris Ainsley
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
package picocog;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny code generation library
 * @author Chris Ainsley
 */
public class PicoWriter implements PicoWriterItem {
   private static final String      SEP                    = "\n";
   private static final String      DI                     = "   ";
   private int                      _indents               = -1;
   private int                      _numLines              = 0;
   private boolean                  _generateIfEmpty       = true;
   private boolean                  _isDirty               = false;
   private List<PicoWriterItem>     _content               = new ArrayList <PicoWriterItem>();
   private StringBuilder            _sb                    = new StringBuilder();
   private String                   _ic  /* Indent chars*/ = DI;

   public static class IndentedLine implements PicoWriterItem {
      String _line;
      int    _indent;
      public IndentedLine(String line, int indent) {
         _line   = line;
         _indent = indent;
      }
      public String getLine() { return _line; }
      public int getIndent()  { return _indent; }
      @Override public String toString() { return "IndentedLine [_line=" + _line + ", _indent=" + _indent + "]"; }
   }
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
      _indents++;
   }
   public void indentLeft() {
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
   public PicoWriter writeln_r(String string) {
      writeln(string);
      indentRight();
      return this;
   }
   public PicoWriter writeln_l(String string) {
      indentLeft();
      writeln(string);
      return this;
   }
   public PicoWriter writeln_lr(String string) {
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
   private void flush() {
      _content.add(new IndentedLine(_sb.toString(), _indents));
      _sb.setLength(0);
      _isDirty = false;
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
   private void render(StringBuilder sb, int indentBase) {
      if (_isDirty) {
         flush();
      }
      // Some methods are flagged not to be generated if there is no body text inside the method, we don't add these to the class narrative
      if ((!isGenerateIfEmpty()) && isMethodBodyEmpty()) {
         return;
      }
       // TODO :: Will make this configurable
      for (PicoWriterItem line : _content) {
         if (line instanceof IndentedLine) {
            IndentedLine il           = (IndentedLine)line;
            final String lineText     = il.getLine();
            final int indentLevelHere = indentBase + il.getIndent();
            writeIndentedLine(sb, indentLevelHere, _ic, lineText);
         } else {
            sb.append(line.toString());
         }
      }
   }
   public String toString(int indentBase) {
      StringBuilder sb = new StringBuilder();
      render(sb, indentBase);
      return sb.toString();
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
   public String toString() {
      return toString(0);
   }
}