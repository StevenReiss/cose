/********************************************************************************/
/*                                                                              */
/*              CoseRequest.java                                                */
/*                                                                              */
/*      External representation of a request for searching                      */
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

import java.util.List;
import java.util.Set;

public interface CoseRequest extends CoseConstants
{

int getNumberOfThreads();
int getNumberOfResults();
int getMaxPackageFiles();
default int getMaxPackages()
{
   return getMaxPackageFiles()/4;
}

CoseSearchType getCoseSearchType();
CoseScopeType getCoseScopeType();
List<CoseKeywordSet> getCoseKeywordSets();
CoseSearchLanguage getLanguage();
Set<CoseSearchEngine> getEngines();
Set<String> getSpecificSources();
CoseSignature getCoseSignature();
List<String> getKeyTerms();
String editSource(String orig);

boolean useAndroid();  

boolean doDebug();

interface CoseKeywordSet {
   List<String> getWords();
}

}       // end of interface CoseRequest




/* end of CoseRequest.java */

