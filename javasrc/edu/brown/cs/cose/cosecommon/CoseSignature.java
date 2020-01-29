/********************************************************************************/
/*                                                                              */
/*              CoseSignature.java                                              */
/*                                                                              */
/*      Generic signature (optional) for search                                 */
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

public interface CoseSignature
{

String getName();


static interface CosePackageSignature extends CoseSignature {
   List<CoseClassSignature> getCoseClasses();
}


static interface CoseClassSignature extends CoseSignature {
   List<CoseMethodSignature> getCoseMethods();
   List<CoseFieldSignature> getCoseFields();
   boolean isInterface();
   String getSuperClass();
   List<String> getInterfaces();
}


static interface CoseMethodSignature extends CoseSignature {
   String getReturnTypeName();
   List<String> getParameterTypeNames();
   List<String> getExceptionTypeNames();
   List<String> getParameterNames();
   boolean isStatic();
   boolean isAbstract();
}


static interface CoseFieldSignature extends CoseSignature {
   String getTypeName();
   boolean isStatic();
}



}       // end of interface CoseSignature




/* end of CoseSignature.java */

