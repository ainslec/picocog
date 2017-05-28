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
package org.ainslec.picocog;

/**
 * 
 * @author Chris Ainsley
 *
 */
public class IndentedLine implements PicoWriterItem {
   String _line;
   int    _indent;
   public IndentedLine(String line, int indent) {
      _line   = line;
      _indent = indent;
   }
   public String getLine() { return _line; }
   public int getIndent()  { return _indent; }
   @Override public String toString() { return _indent + ":" + _line; }
}