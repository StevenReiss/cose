/********************************************************************************/
/*										*/
/*		KeySearchSource.java						*/
/*										*/
/*	Default implementation of a source					*/
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

package edu.brown.cs.cose.keysearch;


import edu.brown.cs.cose.cosecommon.CoseLicense;
import edu.brown.cs.cose.cosecommon.CoseSource;
import edu.brown.cs.cose.license.LicenseManager;




abstract class KeySearchSource implements CoseSource {


/********************************************************************************/
/*										*/
/*	Private Storage 							*/
/*										*/
/********************************************************************************/

private int     search_index;
private String  license_uid;

private static CoseLicense      license_manager = null;


 
/********************************************************************************/
/*										*/
/*	Constructors								*/
/*										*/
/********************************************************************************/

KeySearchSource(String code,int idx)
{
   search_index = idx+1;
   license_uid = null;
   if (license_manager == null) license_manager = LicenseManager.getLicenseManager();
   if (code != null) {
      license_uid = license_manager.getLicenseUidFromSource(code);
    }
}



/********************************************************************************/
/*										*/
/*	Access methods								*/
/*										*/
/********************************************************************************/

public double getScore()			
{ 
   return search_index;
}


public String getProjectId()			
{
   return null; 
}



abstract public String getName();

abstract public String getDisplayName();

public String getPathName()			{ return getDisplayName(); }

public int getOffset()                          { return 0; }
public int getLength()                          { return 0; }
public String getLicenseUid()                   { return license_uid; }

@Override public CoseSource getBaseSource()     { return null; }

@Override public boolean isSameRepository(CoseSource src)
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
   if (uri.contains(pfx)) return true;
   return false;
}



@Override public String toString() 
{
   return getDisplayName() + "@" + getOffset() + ":" + getLength();
}



}	// end of abstract class KeySearchSource




/* end of KeySearchSource.java */
