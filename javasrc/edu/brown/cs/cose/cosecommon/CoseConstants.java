/********************************************************************************/
/*										*/
/*		CoseConstants.java						*/
/*										*/
/*	Constants for cose search common code					*/
/*										*/
/********************************************************************************/
/*	Copyright 2007 Brown University -- Steven P. Reiss		      */
/*********************************************************************************
 *  Copyright 2007, Brown University, Providence, RI.				 *
 *										 *
 *			  All Rights Reserved					 *
 *										 *
 *  Permission to use, copy, modify, and distribute this software and its	 *
 *  documentation for any purpose other than its incorporation into a		 *
 *  commercial product is hereby granted without fee, provided that the 	 *
 *  above copyright notice appear in all copies and that both that		 *
 *  copyright notice and this permission notice appear in supporting		 *
 *  documentation, and that the name of Brown University not be used in 	 *
 *  advertising or publicity pertaining to distribution of the software 	 *
 *  without specific, written prior permission. 				 *
 *										 *
 *  BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS		 *
 *  SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND		 *
 *  FITNESS FOR ANY PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY	 *
 *  BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY 	 *
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,		 *
 *  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS		 *
 *  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE 	 *
 *  OF THIS SOFTWARE.								 *
 *										 *
 ********************************************************************************/

package edu.brown.cs.cose.cosecommon;






public interface CoseConstants
{


enum CoseSearchLanguage {
   JAVA,
   JAVASCRIPT,
   XML,
}


enum CoseSearchType {
   METHOD,
   CLASS,
   PACKAGE,
   ANDROIDUI,
   TESTCLASS,
}


enum CoseScopeType {
   FILE,
   PACKAGE,
   PACKAGE_UI,
   PACKAGE_IFACE,
   PACKAGE_USED,
   SYSTEM,
}


enum CoseSearchEngine {
   GITHUB,
   CODEEX,
   GITZIP, 
   GITREPO,
   SEARCHCODE,
}


enum CoseResultType {
   FILE,
   CLASS,
   METHOD,
   PACKAGE
}



/********************************************************************************/
/*                                                                              */
/*     License information                                                      */
/*                                                                              */
/********************************************************************************/

String LICENSE_DATABASE = "s6";

String SCORE_DATA_FILE = "/ws/volfred/s6/scores.data";



/********************************************************************************/
/*                                                                              */
/*      Common utility methods                                                  */
/*                                                                              */
/********************************************************************************/

public static boolean isRelatedPackage(String p1,String p2)
{
   if (p1 == null || p2 == null) return false;
   if (p1.equals(p2)) return true;
   if (p1.startsWith(p2)) return true;
   else if (p2.startsWith(p1)) return true;
   else {
      int idx = -1;
      for (int i = 0; i < 3; ++i) {
         idx = p2.indexOf(".",idx+1);
         if (idx < 0) break;
       }
      if (idx >= 0 && idx < p1.length() &&
            p2.substring(0,idx).equals(p1.substring(0,idx))) {
         return true;
       }
    }
   
   return false;
}


static String [] JAVA_PATHS = new String[] {
   "java.applet.",
      "java.awt.",
      "java.beans.",
      "java.io.",
      "java.lang.",
      "java.math.",
      "java.net.",
      "java.nio.",
      "java.rmi.",
      "java.security.",
      "java.sql.",
      "java.text.",
      "java.time.",
      "java.util.",
      "javax.accessibility.",
      "javax.activation.",
      "javax.activity.",
      "javax.annotation.",
      "javax.crypto.",
      "javax.imageio.",
      "javax.jws.",
      "javax.lang.model.",
      "javax.management.",
      "javax.naming.",
      "javax.net.",
      "javax.print.",
      "javax.rmi.",
      "javax.script.",
      "javax.security.auth.",
      "javax.security.cert.",
      "javax.security.sasl.",
      "javax.sound.midi.",
      "javax.sound.sampled.",
      "javax.sql.",
      "javax.swing.",
      "javax.tools.",
      "javax.transaction.",
      "javax.xml.",
      "org.ietf.jgss.",
      "org.omg.CORBA.",
      "org.omg.CORBA_2_3.",
      "org.omg.CosNaming.",
      "org.omg.Dynamic.",
      "org.omg.DynamicAny.",
      "org.omg.IOP.",
      "org.omg.Messaging.",
      "org.omg.PortableInterceptor.",
      "org.omg.PortableServer.",
      "org.omg.SendingContext.",
      "org.omg.stub.java.rmi.",
      "org.w3c.dom.",
      "org.xml.sax.",
};



public static boolean isStandardJavaLibrary(String path)
{
   for (String s : JAVA_PATHS) {
      if (path.startsWith(s)) return true;
    }
   return false;
}




}	// end of interface CoseConstants



/*   end of CoseConstants.java */


