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

import org.ainslec.picocog.PicoWriter;

import junit.framework.TestCase;

public class TestPico extends TestCase {
   
   public void testBasic() {
      PicoWriter w = new PicoWriter();
      
      w.writeln("int foo = calcFoo();");
      w.writeln("");
      w.writeln_r("if (foo == 0) {");
      w.writeln("sayHello();");
      w.writeln_lr("} else if (foo < 100) {");
      w.writeln("sayGoodbye();");
      w.writeln_lr("} else {");
      w.writeln("sayAnything();");
      w.writeln_l("}");
      
      System.out.println(w.toString());
   }
   
   public void testAdvanced() {
      PicoWriter topWriter = new PicoWriter();
      
      String myPackageName = "com.samplepackage";
      String myClassName   = "MyClass";
      
      topWriter.writeln ("package " + myPackageName + ";");
      topWriter.writeln ("");
      topWriter.writeln_r ("public class "+myClassName+" {");
      
      PicoWriter memvarWriter    = topWriter.createDeferredWriter();
      topWriter.writeln_r ("{");
      PicoWriter indentedSection = topWriter.createDeferredWriter();
      topWriter.writeln_l ("}");
      topWriter.writeln("");

      PicoWriter methodSection = topWriter.createDeferredWriter();
      
      memvarWriter.writeln("String myString = null;" );
      indentedSection.writeln("// Contents of the indented section (1)");
      memvarWriter.writeln("String myString2 = null;" );
      indentedSection.writeln("// Contents of the indented section (2)");
      
      PicoWriter mainMethod = methodSection.createDeferredWriter();
      
      mainMethod.writeln_r("public static void main(String[] args) {");
      mainMethod.writeln_r("if (args.length == 0) {");
      mainMethod.writeln("System.out.println(\"Require more than one argument\");");
      mainMethod.writeln_lr("} else if (args.length == 1) {");
      mainMethod.writeln("doSomething();");
      mainMethod.writeln_lr("} else {");
      mainMethod.writeln("System.out.println(\"Too many arguments\");");
      mainMethod.writeln_l("}");
      mainMethod.writeln_l("}");
      mainMethod.writeln("");
      topWriter.writeln_l ("}");
      System.out.println(topWriter.toString());
   }
}
