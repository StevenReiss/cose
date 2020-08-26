/********************************************************************************/
/*                                                                              */
/*              CoseSource.java                                                 */
/*                                                                              */
/*      External representation of a source                                     */
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



package edu.brown.cs.cose.cosecommon;



public interface CoseSource extends CoseConstants
{

String getProjectId();
String getPathName();
double getScore();
String getName();
String getDisplayName();
int getOffset();
int getLength();
String getLicenseUid();
CoseSource getBaseSource();


public default boolean isRelatedRepository(CoseSource src,boolean exact)
{
   String full = getName();
   String uri = src.getName();
   if (full.equals(uri)) return true;
   
   int idx1 = full.indexOf(":");
   int idx2 = full.lastIndexOf("/");
   String pfx = full.substring(idx1+1,idx2);
   int idx3 = pfx.indexOf("/blob/");
   if (idx3 > 0) {
      int idx4 = pfx.indexOf("/",idx3+7);
      if (idx4 >= 0) pfx = pfx.substring(0,idx4+1);
    }
   if (uri.contains(pfx)) return true;  // same repo
   if (exact) return false;
   
   String pf1 = full.substring(idx1+1,idx2);
   int idx5 = pf1.indexOf("//");
   int idx6 = pf1.indexOf("/",idx5+2);
   int idx7 = pf1.indexOf("/",idx6+1);
   String pf2 = pf1.substring(0,idx7);
   if (uri.contains(pf2)) return true;
   
   return false;
}



}       // end of interface CoseSource




/* end of CoseSource.java */

