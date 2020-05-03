/********************************************************************************/
/*                                                                              */
/*              CoseResult.java                                                 */
/*                                                                              */
/*      Fragment representing an initial solution                               */
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

import java.util.Collection;
import java.util.Set;

public interface CoseResult extends CoseConstants
{

CoseSource getSource();
CoseResultType getResultType();
CoseResult getParent();

String getKeyText();
String getText();
String getEditText();   // get text to apply edits to

Collection<CoseResult> getResults(CoseSearchType st);
Collection<CoseResource> getResources();

String getBasePackage();
Collection<String> getPackages();
boolean addPackage(String pkg);

void addInnerResult(CoseResult cf);
void addResource(CoseResource cr);
Collection<CoseResult> getInnerResults();

Set<String> getRelatedProjects();
Set<String> getUsedProjects();

Object getStructure();          // ASTNode, Element, .
Object checkStructure();
Object clearStructure();

CoseResult cloneResult(Object diffs,Object data);
boolean isCloned();

CoseScores getScores(CoseRequest req);
CoseScores getScores(CoseRequest req,Object struct);

}       // end of interface CoseResult




/* end of CoseResult.java */

