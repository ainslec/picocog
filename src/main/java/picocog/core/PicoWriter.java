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
package picocog.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny code generation library
 * @author Chris Ainsley
 */
public class PicoWriter implements PicoWriterItem {
   private static final String      SEP                    = "\n";
   private static final String      DEFAULT_INDENT_CHARS   = "   ";
   private int                      _numIndents            = -1;
   private int                      _numLinesWritten       = 0;
   private boolean                  _generateIfEmpty       = true;
   private boolean                  _isDirty               = false;
   private List<PicoWriterItem>   _content                 = new ArrayList <PicoWriterItem>();
   private StringBuilder            _currentLineSB         = new StringBuilder();
   @SuppressWarnings("unused")
   private int                      _currentLineNumber     = 0;
   private String                   _indentChars           = DEFAULT_INDENT_CHARS;
   //private int                      _withinThisBookmark    = -1;
   //private boolean                  _withinBookmark        = false;

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
      _numIndents = 0;
   }
   public PicoWriter (String indentText) {
      _numIndents = 0;
      _indentChars = indentText == null ? DEFAULT_INDENT_CHARS : indentText;
   }
   private PicoWriter (int initialIndent, String indentText) {
      _numIndents = initialIndent < 0 ? 0 : initialIndent;
      _indentChars = indentText == null ? DEFAULT_INDENT_CHARS : indentText;
   }
   public void indentRight() {
      _numIndents++;
   }
   public void indentLeft() {
      _numIndents--;
      if (_numIndents < 0) {
         throw new RuntimeException("Local indent cannot be less than zero");
      }
   }

   public final PicoWriter createDeferredWriter() {
      if (_currentLineSB.length() > 0) {
         flush();
         _numLinesWritten++;
      }
      PicoWriter inner = new PicoWriter(_numIndents, _indentChars);
      _content.add(inner);
      _numLinesWritten++;
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
      _numLinesWritten++;
      _currentLineSB.append(string);
      flush();
      return this;
   }
   private void flush() {
//      if (_withinBookmark) {
//         _content.add(_currentLineNumber, new IndentedLine(_currentLineSB.toString(), _localIndent));
//         // Shift forward future bookmarks
//         for (int i=_withinThisBookmark+1; i < _bookmarks.size(); i++) {
//            BookMark bm = _bookmarks.get(i);
//            bm.shiftForwardDueToInsertionByEarlierBookmark();
//         }
//      } else {
         _content.add(new IndentedLine(_currentLineSB.toString(), _numIndents));
//      }
      _currentLineNumber++;
      _currentLineSB.setLength(0);
      _isDirty = false;
   }
   public boolean isEmpty() {
      return _numLinesWritten == 0;
   }
   public void write(String string)  {
      _numLinesWritten++;
      _isDirty = true;
      _currentLineSB.append(string);
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
            writeIndentedLine(sb, indentLevelHere, _indentChars, lineText);
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
      return _content.size() == 0 && _currentLineSB.length() == 0;
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
//   private static final void writeIndent(final StringBuilder sb, final int indentBase, final String indentText) {
//      for (int i = 0; i < indentBase; i++) {
//         sb.append(indentText);
//      }
//   }
//
   // private int                      _savedIndentForRestorationAfterExitingBookmark = _localIndent;
//private List<BookMark>           _bookmarks             = new ArrayList <BookMark>();
//   public class BookMark {
//      int _indent   = 0;
//      int _position = 0;
//      public BookMark(int currentIndent, int currentPosition) {
//         _indent   = currentIndent;
//         _position = currentPosition;
//      }
//      public int getCurrentIndent()                             { return _indent; }
//      public int getCurrentPosition()                           { return _position; }
//      public void shiftForwardDueToInsertionByEarlierBookmark() { _position++; }
//   }

//   public int bookmark() {
//      final BookMark bookmark = new BookMark(_localIndent, _currentLineNumber);
//      _bookmarks.add(bookmark);
//      return _bookmarks.size()-1;
//   }
//   public void enterBookmark(int id) {
//      if (id < _bookmarks.size() && id >=0) {
//         BookMark bookmark = _bookmarks.get(id);
//         _withinBookmark = true;
//         _savedIndentForRestorationAfterExitingBookmark = _localIndent;
//         _localIndent = bookmark.getCurrentIndent();
//         _currentLineNumber = bookmark.getCurrentPosition();
//         _withinThisBookmark = id;
//      } else {
//         throw new RuntimeException("Specified bad bookmark index : " + id + " (base zero), number of bookmarks : " + _bookmarks.size());
//      }
//   }
//   public void exitBookmark() {
//      _currentLineNumber  = _content.size();
//      _localIndent        = _savedIndentForRestorationAfterExitingBookmark;
//      _withinBookmark     = false;
//      _withinThisBookmark = -1;
//   }
}