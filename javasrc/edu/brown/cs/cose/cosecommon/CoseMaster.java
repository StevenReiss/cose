/********************************************************************************/
/*                                                                              */
/*              CoseMaster.java                                                 */
/*                                                                              */
/*      description of class                                                    */
/*                                                                              */
/*      Written by spr                                                          */
/*                                                                              */
/********************************************************************************/



package edu.brown.cs.cose.cosecommon;

import java.lang.reflect.Constructor;

import org.w3c.dom.Element;

import edu.brown.cs.ivy.file.IvyLog;

public interface CoseMaster extends CoseConstants
{

CoseResultSet computeSearchResults(CoseResultSet crs);
CoseResult createResult(Element xml);



static public CoseMaster createMaster(CoseRequest req)
{
   String cls = "edu.brown.cs.cose.keysearch.KeySearchMaster";
   try {
      Class<?> cz = Class.forName(cls);
      Constructor<?> cnst = cz.getConstructor(CoseRequest.class);
      CoseMaster cr = (CoseMaster) cnst.newInstance(req);
      return cr;
    }
   catch (Exception e) {
      IvyLog.logE("COSE","Can't create COSE Master",e);
      return null;
    }
}





static public CoseResultEditor createJavaEditor() {
   String cls = "edu.brown.cs.cose.result.ResultJavaEditor";
   try {
      Class<?> cz = Class.forName(cls);
      Constructor<?> cnst = cz.getConstructor();
      CoseResultEditor cr = (CoseResultEditor) cnst.newInstance();
      return cr;
    }
   catch (Exception e) {
      IvyLog.logE("COSE","Can't create COSE Java Editor",e);
      return null;
    }
}



}       // end of interface CoseMaster




/* end of CoseMaster.java */
