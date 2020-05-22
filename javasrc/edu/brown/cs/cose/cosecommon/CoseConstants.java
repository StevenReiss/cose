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

import java.util.HashSet;
import java.util.Set;

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

static String [] PREFIX_PATHS = { "org", "com", "edu", "sun", "java", "oracle" };

public static boolean isRelatedPackage(String p1,String p2)
{
   if (p1 == null || p2 == null) return false;
   if (p1.equals(p2)) return true;
   if (p1.startsWith(p2)) return true;
   else if (p2.startsWith(p1)) return true;
   if (p1.length() > p2.length()) {
      String p = p1;
      p1 = p2;
      p2 = p;
    }
   
   Set<String> pfxset = new HashSet<>();
   for (String s : PREFIX_PATHS) pfxset.add(s);
   
   String pfx = null;
   int minlen = Math.min(p1.length(),p2.length());
   for (int i = 0; i < minlen; i++) {
      if (p1.charAt(i) != p2.charAt(i)) {
         pfx = p1.substring(0, i);
         break;
       }
    }
   if (pfx == null) pfx = p1.substring(0, minlen);
   if (!pfx.endsWith(".")) {
      int idx = pfx.lastIndexOf(".");
      pfx = pfx.substring(0,idx+1);
    }
   
   if (pfx.length() == 0) return false;
   pfx = pfx.substring(0,pfx.length()-1);
   String [] parts = pfx.split("\\.");
   String [] minparts = p1.split("\\.");

   int ign = 0;
   for (ign = 0; ign < parts.length; ++ign) {
      if (!pfxset.contains(parts[ign])) break;
    }
   int match = parts.length - ign;
   if (match == 0) return false;
   if (match < minparts.length - 2) return false;
   
   return true;
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


