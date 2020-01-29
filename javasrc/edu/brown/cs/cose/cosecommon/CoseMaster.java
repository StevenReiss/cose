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

public interface CoseMaster extends CoseConstants
{

void computeSearchResults(CoseResultSet crs);



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
      System.err.println("Can't create COSE Master: " + e);
      e.printStackTrace();
      return null;
    }
}




}       // end of interface CoseMaster




/* end of CoseMaster.java */
