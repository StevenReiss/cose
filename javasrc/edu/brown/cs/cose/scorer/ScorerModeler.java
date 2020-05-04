/********************************************************************************/
/*                                                                              */
/*              ScorerModeler.java                                              */
/*                                                                              */
/*      Build a learning model to determine score parameters                    */
/*                                                                              */
/********************************************************************************/
/*      Copyright 2013 Brown University -- Steven P. Reiss                    */
/*********************************************************************************
 *  Copyright 2013, Brown University, Providence, RI.                            *
 *                                                                               *
 *                        All Rights Reserved                                    *
 *                                                                               *
 *  Permission to use, copy, modify, and distribute this software and its        *
 *  documentation for any purpose other than its incorporation into a            *
 *  commercial product is hereby granted without fee, provided that the          *
 *  above copyright notice appear in all copies and that both that               *
 *  copyright notice and this permission notice appear in supporting             *
 *  documentation, and that the name of Brown University not be used in          *
 *  advertising or publicity pertaining to distribution of the software          *
 *  without specific, written prior permission.                                  *
 *                                                                               *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS                *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND            *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY      *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY          *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,              *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS               *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE          *
 *  OF THIS SOFTWARE.                                                            *
 *                                                                               *
 ********************************************************************************/



package edu.brown.cs.cose.scorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.brown.cs.ivy.xml.IvyXml;


public class ScorerModeler implements ScorerConstants
{



/********************************************************************************/
/*                                                                              */
/*      Main program                                                            */
/*                                                                              */
/********************************************************************************/

public static void main(String [] args)
{
   ScorerModeler sm = new ScorerModeler(args);
   sm.buildModel();
}



/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private Map<String,String> property_names;
private String property_file;
private String output_file;
private List<File> log_files;
private Map<String,SourceData> source_map;

private static final double SAMPLE_SIZE = 0.015;



/********************************************************************************/
/*                                                                              */
/*      Constructors                                                            */
/*                                                                              */
/********************************************************************************/

private ScorerModeler(String [] args)
{
   property_names = new TreeMap<>();
   property_file = "scores.data";
   output_file = "scoremodel.csv";
   log_files = new ArrayList<>();
   source_map = new HashMap<>();
   
   scanArgs(args); 
}



/********************************************************************************/
/*                                                                              */
/*      Processing methods                                                      */
/*                                                                              */
/********************************************************************************/

private void buildModel()
{
   getPropertySet();
   
   for (File f : log_files) {
      scanLogFile(f);
    }
   
   generateOutput();
}


/********************************************************************************/
/*                                                                              */
/*      Get the set of used properties                                          */
/*                                                                              */
/********************************************************************************/

private void getPropertySet()
{
   try (BufferedReader br = new BufferedReader(new FileReader(property_file))) {
      SourceData cursource = null;
      for ( ; ; ) {
         String line = br.readLine();
         if (line == null) break;
         if (line.startsWith("-")) continue;
         int idx = line.indexOf(":");
         if (idx <= 0) continue;
         String key = line.substring(0,idx);
         String val = line.substring(idx+1).trim();
         
         if (key.equals("SOURCE")) {
            cursource = new SourceData(val);
            source_map.put(val,cursource);
            continue;
          }
         String typ = null;
         Object pval = null;
         if (val.equals("false") || val.equals("true")) {
            typ = "Boolean";
            pval = Boolean.valueOf(val);
          }
         else {
            try {
               pval = Integer.parseInt(val);
               typ = "Int";
             }
            catch (NumberFormatException e) { }
            if (typ == null) {
               try {
                  pval = Double.parseDouble(val);
                  typ = "Double";
                }
               catch (NumberFormatException e) { }
             }
          }
         if (typ == null) {
            typ = "String";
            pval = val;
          }
         property_names.put(key,typ);
         cursource.setProperty(key,pval);
       }
      br.close();
    }
   catch (IOException e) {
      System.err.println("SCORERMODELER: Couldn't read property file");
      System.exit(1);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Log file scanning                                                       */
/*                                                                              */
/********************************************************************************/

private void scanLogFile(File f) 
{
   try (BufferedReader br = new BufferedReader(new FileReader(f))) {
      for ( ; ; ) {
         String line = br.readLine();
         if (line == null) break;
         line = line.trim();
         if (line.startsWith("<SOLSRC>") && line.endsWith("</SOLSRC>")) {
            int idx1 = line.indexOf(">") + 1;
            int idx2 = line.lastIndexOf("<");
            String src = line.substring(idx1,idx2).trim();
            src = IvyXml.decodeXmlString(src);
            SourceData sd = source_map.get(src);
            if (sd != null) {
               sd.noteUsed();
             }
            else {
               System.err.println("SCORERMODEL: Source " + src + " not found");
             }
          }
       }
    }
   catch (IOException e) { }
}



/********************************************************************************/
/*                                                                              */
/*      Output methods                                                          */
/*                                                                              */
/********************************************************************************/

private void generateOutput()
{
   int idx = output_file.lastIndexOf(".");
   String ext = output_file.substring(idx+1);
   if (ext.equals("arff")) {
      // output arff
    }
   else {
      outputCSV();
    }
}



private void outputCSV()
{
   int idx = output_file.lastIndexOf(".");
   String sam = output_file.substring(0,idx) + "_sample" + output_file.substring(idx);
   
   try {
      PrintWriter pw = new PrintWriter(output_file);
      PrintWriter pws = new PrintWriter(sam);
      // output header
      for (String s : property_names.keySet()) {
         pw.print(s);
         pw.print(",");
         pws.print(s);
         pws.print(",");
       }
      pw.println("USED");
      pws.println("USED");
      
      // output data
      for (SourceData sd : source_map.values()) {
         sd.outputCSV(pw);
         if (!sd.isUsed()) {
            if (Math.random() >= SAMPLE_SIZE) continue;
          }
         sd.outputCSV(pws);
       }
      
      pw.close();
      pws.close();
    }
   catch (IOException e) {
      System.err.println("SCORERMODEL: Problem creating output: " + e);
      System.exit(1);
    }
}



/********************************************************************************/
/*                                                                              */
/*      Argument processing methods                                             */
/*                                                                              */
/********************************************************************************/

private void scanArgs(String [] args) 
{
   for (int i = 0; i < args.length; ++i) {
      if (args[i].startsWith("-")) {
         if (args[i].startsWith("-p") && i+1 < args.length) {           // -p <prop file>
            property_file = args[++i];
          }
         else if (args[i].startsWith("-o") && i+1 < args.length) {      // -o output
            output_file = args[++i];
          }
         badArgs();
       }
      else {
         File f = new File(args[i]);
         if (f.exists()) log_files.add(f);
         else badArgs();
       }
    }
   
   if (log_files.isEmpty()) {
      File d = new File(".");
      for (String fnm: d.list()) {
         if (fnm.endsWith(".debug") || fnm.endsWith(".mtout") ||
               fnm.endsWith(".out") || fnm.endsWith(".mtdbg")) {
            File f = new File(fnm);
            log_files.add(f);
          }
       }
    }
}



private void badArgs()
{
   System.err.println("SCORERMODELER: scorermodeler [-p propfile] logfile ...");
   System.exit(1);
}


/********************************************************************************/
/*                                                                              */
/*      Source Data                                                             */
/*                                                                              */
/********************************************************************************/

private class SourceData {

   private String source_name;
   private Map<String,Object> property_set;
   private boolean is_used;
   
   SourceData(String name) {
      source_name = name;
      property_set = new HashMap<>();
      is_used = false;
    }
   
   void setProperty(String key,Object val) {
      property_set.put(key,val);
    }
   
   void noteUsed() {
      is_used = true;
    }
   
   boolean isUsed()                     { return is_used; }
   
   @Override public String toString() {
      StringBuffer buf = new StringBuffer();
      if (is_used) buf.append("*");
      buf.append(source_name);
      return buf.toString();
    }
   
   void outputCSV(PrintWriter pw) {
      // skip non-methods
      if (property_set.get("ABSTRACT") == null) return;
      
      for (String s : property_names.keySet()) {
         Object o = property_set.get(s);
         if (o == null) pw.print("");
         else pw.print(o.toString());
         pw.print(",");
       }
      if (is_used) 
         pw.println("true");
      else 
         pw.println("false");
    }
   
}       // end of inner class SourceData





}       // end of class ScorerModeler




/* end of ScorerModeler.java */

