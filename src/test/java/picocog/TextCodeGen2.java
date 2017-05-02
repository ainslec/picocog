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

import java.io.File;

import com.ainslec.picocog.PicoWriter;

import junit.framework.TestCase;

/**
 * 
 * @author Chris Ainsley
 *
 */
public class TextCodeGen2 extends TestCase {
   
   public static class Document {
      public Table[] tables;
   }
   public static class Table {
      public String id;
      public Field[] fields;
   }
   
   public static class Field {
      public String  id;
      public String  type;
      public Integer length;
   }


   private static Document loadDoc() {
      Document d = new Document();
      Table[] tables = new Table[2];
      d.tables = tables;
      
      {
         Table t = new Table();
         t.id = "customer";
         Field[] fields = new Field[6];
         t.fields = fields;
         fields[0] = new Field();
         fields[1] = new Field();
         fields[2] = new Field();
         fields[3] = new Field();
         fields[4] = new Field();
         fields[5] = new Field();
         fields[0].id = "id";
         fields[1].id = "address1";
         fields[2].id = "address2";
         fields[3].id = "address3";
         fields[4].id = "zipcode";
         fields[5].id = "country";
         fields[0].type = "char";
         fields[1].type = "char";
         fields[2].type = "char";
         fields[3].type = "char";
         fields[4].type = "char";
         fields[5].type = "char";
         d.tables[0] = t;
      }
      {
         Table t = new Table();
         t.id = "order";
         
         Field[] fields = new Field[3];
         t.fields = fields;
         fields[0] = new Field();
         fields[1] = new Field();
         fields[2] = new Field();
         fields[0].id = "id";
         fields[1].id = "customer_id";
         fields[2].id = "date_of_purchase";
         fields[0].type = "integer";
         fields[1].type = "char";
         fields[2].type = "datetime";
         d.tables[1] = t;
      }
      
      return d;
   }
   
   public void testCodeGen() {
      Document   doc     = loadDoc();
      String packageName = "com.sampledomain.db";
      File folder        = null;
      
      for (Table t : doc.tables) {
         PicoWriter outer          = new PicoWriter();
         outer.writeln("package "+packageName+";");
         outer.writeln("");
         outer.writeln_r("public class " + toJavaCamelCase(t.id) + " {");
         PicoWriter inner = outer.createDeferredWriter();
         outer.writeln("");
         for (Field field : t.fields) {
            String ccFieldId      = toJavaCamelCase(field.id);
            if ("char".equals(field.type)) {
               inner.writeln  ("String " + field.id + ";");
               outer.writeln_r("public void set" + ccFieldId + "() {");
               outer.writeln     ("this." + ccFieldId + " = " + ccFieldId +";");
               outer.writeln_l("}");
               outer.writeln_r("public String get" + ccFieldId + "() {");
               outer.writeln     ("return this." + ccFieldId + ";");
               outer.writeln_l("}");
            } else if("integer".equals(field.type)) {
               inner.writeln  ("int " + field.id + ";");
               outer.writeln_r("public void set" + ccFieldId + "() {");
               outer.writeln     ("this." + ccFieldId + " = " + ccFieldId +";");
               outer.writeln_l("}");
               outer.writeln_r("public int get" + ccFieldId + "() {");
               outer.writeln     ("return this." + ccFieldId + ";");
               outer.writeln_l("}");
            } else if("datetime".equals(field.type)) {
               inner.writeln  (java.util.Date.class.getName() + " " + field.id + ";");
               outer.writeln_r("public void set" + ccFieldId + "() {");
               outer.writeln  ("this." + ccFieldId + " = " + ccFieldId +";");
               outer.writeln_l("}");
               outer.writeln_r("public "+java.util.Date.class.getName()+" get" + ccFieldId + "() {");
               outer.writeln     ("return this." + ccFieldId + ";");
               outer.writeln_l("}");
            } else {
               throw new RuntimeException("Unknown field type : " + field.type);
            }
         }
         outer.writeln_l("}");
         writeJavaFile(folder, toJavaCamelCase(t.id), outer);
      }
   }
   
   private static String toJavaCamelCase(String id) {
      return id; // Assume this is implemented ... 
   }
   private static void writeJavaFile(File folder, String generatedClassName, PicoWriter cb) {
      System.out.println(cb); // Assume this is implemented
   }

}
