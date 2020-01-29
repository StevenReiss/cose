/********************************************************************************/
/*                                                                              */
/*              ResultJava.java                                                 */
/*                                                                              */
/*      Implementation of Java Results                                        */
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



package edu.brown.cs.cose.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import edu.brown.cs.ivy.jcomp.JcompAst;
import edu.brown.cs.ivy.jcomp.JcompType;
import edu.brown.cs.cose.cosecommon.CoseResult;
import edu.brown.cs.cose.cosecommon.CoseSource;

abstract class ResultJava implements ResultConstants
{


/********************************************************************************/
/*                                                                              */
/*      Private Storage                                                         */
/*                                                                              */
/********************************************************************************/

private static Set<String> check_names;
static {
   check_names = new HashSet<String>();
   check_names.add("java.awt.List");
   check_names.add("javax.swing.filechooser.FileFilter");
   check_names.add("java.sql.Date");
}




/********************************************************************************/
/*                                                                              */
/*      Helper functions                                                        */
/*                                                                              */
/********************************************************************************/

static String getJavaKeyText(Object on)
{
   if (on == null) return "";
   ASTNode n = (ASTNode) on;
   String s0 = n.toString();
   ASTNode r = n.getRoot();
   if (r != n) {
      s0 += " @ " + r.toString();
    }
   return s0;
}



/********************************************************************************/
/*                                                                              */
/*      Cloning methods                                                         */
/*                                                                              */
/********************************************************************************/

static JavaDelta getEditedText(ResultBase rslt,ASTRewrite rw,ITrackedNodePosition pos)
{
   String origtext = rslt.getEditText();
   Document d = new Document(origtext);
   TextEdit edit = null;
   try {
      edit = rw.rewriteAST(d,null);
    }
   catch (Throwable t) {
      // this yields java.lang.IllegalArgumentException: Document does not match the AST
      System.err.println("Problem rewriting AST: " + t);
      t.printStackTrace();
    }
   int nodestart = pos.getStartPosition();
   int nodelength = pos.getLength();
   if (edit == null) return null;
   try {
      edit.apply(d);
      nodestart = pos.getStartPosition();
      nodelength = pos.getLength();
    }
   catch (BadLocationException e) {
      System.err.println("Text Delta problem: " + e);
      e.printStackTrace();
    }
  return new JavaDelta(rslt,d.get(),nodestart,nodelength);
}



static CoseResult cloneJavaResult(CoseResult base,Object o,Object data) {
   ResultBase rb = (ResultBase) base;
   if (o instanceof ASTNode) {
      return rb.baseCloneResult(o.toString());
    }
   else if (o instanceof ASTRewrite) {
      ASTRewrite rw = (ASTRewrite) o;
      ITrackedNodePosition pos = (ITrackedNodePosition) data;
      JavaDelta jd = getEditedText(rb,rw,pos);
      return rb.baseCloneResult(jd);
    }
   else if (o instanceof String) {
      String newtext = (String) o;
      JavaDelta jd = new JavaDelta(rb,newtext,0,newtext.length());
      return rb.baseCloneResult(jd);
    }
   return rb.baseCloneResult(o);
}



static ASTNode getDeltaJavaStructure(ResultDelta rd) 
{
   JavaDelta jd = (JavaDelta) rd;
   return jd.getAstNode();
}



/********************************************************************************/
/*                                                                              */
/*      Delta representation                                                    */
/*                                                                              */
/********************************************************************************/

private static class JavaDelta extends ResultDelta {
   
   private int node_start;
   private int node_length;
   
   JavaDelta(ResultBase base,String newtext,int start,int len) {
      super(base,newtext);
      node_start = start;
      node_length = len;
    }
   
   ASTNode getAstNode() {
      String text = getEditText();
      CompilationUnit cu = JcompAst.parseSourceFile(text);
      PositionFinder pf = new PositionFinder(getBaseResult().getResultType(),node_start,node_length);
      cu.accept(pf);
      return pf.getAstNode();
    }
   
   @Override protected String getText() {
      String text = super.getEditText();
      return text.substring(node_start,node_start+node_length);
    }
   
   @Override String getEditText() {
      return super.getEditText();
    }
   
   @Override protected String getKeyText() {
      return getJavaKeyText(getAstNode());
    }
   
   @Override CoseResult cloneResult(CoseResult cr,Object o,Object data) {
      return cloneJavaResult(cr,o,data);
    }
   
   @Override Object getDeltaStructure() {
      return getDeltaJavaStructure(this);
    }
    
}       // end of inner class JavaDelta



/********************************************************************************/
/*                                                                              */
/*      Visitor to find AST node at a given position                            */
/*                                                                              */
/********************************************************************************/

private static class PositionFinder extends ASTVisitor {
   
   private CoseResultType fragment_type;
   private ASTNode found_node;
   private int start_pos;
   private int node_len;
   
   PositionFinder(CoseResultType st,int pos,int len) {
      fragment_type = st;
      start_pos = pos;
      node_len = len;
      found_node = null;
    }
   
   ASTNode getAstNode() 		{ return found_node; }
   
   @Override public boolean visit(MethodDeclaration n) {
      if (fragment_type != CoseResultType.METHOD) return false;
      if (n.getBody() == null) return false;
      checkNode(n);
      return false;
    }
   
   @Override public boolean visit(TypeDeclaration n) {
      if (fragment_type != CoseResultType.CLASS) return true;
      if (n.isInterface()) return false;
      checkNode(n);
      return true;
    }
   
   @Override public boolean visit(CompilationUnit n) {
      if (fragment_type != CoseResultType.PACKAGE) return true;
      checkNode(n);
      return false;
    }
   
   private void checkNode(ASTNode n) {
      CompilationUnit cu = (CompilationUnit) n.getRoot();
      int s0 = cu.getExtendedStartPosition(n);
      int l0 = cu.getExtendedLength(n);
      int s1 = n.getStartPosition();
      n.getLength();
      
      int dl0 = start_pos - s0;
      if (dl0 == 0) found_node = n;
      else if (start_pos == s1) found_node = n;
      else if (found_node == null && dl0 < 0) found_node = n;
      else if (found_node == null) {
         int d1a = start_pos+node_len;
         int d1b = s0 + l0;
         if (d1a > d1b) d1a = d1b;
         int overlap = d1b - start_pos;
         if (overlap > 0 && overlap > l0 / 2) {
            found_node = n;
          }
       }
    }
   
}	// end of subclass PositionFinder





/********************************************************************************/
/*                                                                              */
/*      Find component Results                                                  */
/*                                                                              */
/********************************************************************************/

static Collection<CoseResult> getJavaResults(JavaFileResult f,CoseSearchType typ)
{
   FindVisitor fv = new FindVisitor(f,typ);
   ASTNode an = (ASTNode) f.getStructure();
   an.accept(fv);
   
   return fv.getResults();
}


private static class FindVisitor extends ASTVisitor {
   
   private CoseSearchType search_type;
   private Collection<CoseResult> found_results;
   private JavaFileResult parent_result;
   private boolean is_test;
   
   FindVisitor(JavaFileResult par,CoseSearchType st) {
      search_type = st;
      parent_result = par;
      found_results = new ArrayList<>();
      is_test = false;
    }
   
   Collection<CoseResult> getResults()		{ return found_results; }
   
   @Override public boolean visit(TypeDeclaration n) {
      switch (search_type) {
         case METHOD :
            return true;
         case CLASS :
         case ANDROIDUI :
         case PACKAGE :
            break;
         case TESTCLASS :
            is_test = false;
            break;
       }
      if (n.isInterface()) return false;
      found_results.add(new JavaClassResult(parent_result,n));
      return true;	// allow nested types to be used
    }
   
   
   @Override public void endVisit(TypeDeclaration n) {
      if (!is_test) return;
      if (search_type != CoseSearchType.TESTCLASS) return;
      if (n.isInterface()) return;
      found_results.add(new JavaClassResult(parent_result,n));
    }
   
   @Override public boolean visit(MethodDeclaration n) {
      if (n.getBody() == null) return false;
      
      switch (search_type) {
         case METHOD :
            break;
         case CLASS :
         case PACKAGE :
         case ANDROIDUI :
            return false;
         case TESTCLASS :
            if (n.getName().getIdentifier().startsWith("test")) is_test = true;
            if (is_test) return false;
            return true;
       }
      found_results.add(new JavaMethodResult(parent_result,n));
      return false;
    }
   
   
   
   @Override public void endVisit(MethodInvocation mi) {
      String nid = mi.getName().getFullyQualifiedName();
      if (nid.contains("assert") || nid.contains("assume") ||
            nid.contains("Assert") || nid.contains("Assume")) {
         is_test = true;
       }
    }
   
   @Override public boolean visit(MarkerAnnotation ma) {
      String nm = ma.getTypeName().getFullyQualifiedName();
      if (nm.endsWith("Test")) is_test = true;
      return false;
    }
   
   @Override public boolean visit(NormalAnnotation na) {
      String nm = na.getTypeName().getFullyQualifiedName();
      if (nm.endsWith("Test")) is_test = true;
      return false;
    }
   
}	// end of subclass FindVisitor



/********************************************************************************/
/*                                                                              */
/*      Code for sorting file results in a package                              */
/*                                                                              */
/********************************************************************************/

private static class FileSorter {
   
   private List<CoseResult> base_list;
   private Map<CoseResult,List<CoseResult>> depend_names;
   private Map<String,CoseResult> class_names;
   
   FileSorter(List<CoseResult> frags) {
      base_list = frags;
      depend_names = new HashMap<>();
      class_names = new HashMap<>();
      for (CoseResult ff : frags) {
         CompilationUnit cu = (CompilationUnit) ff.getStructure();
         for (Object o : cu.types()) {
            if (o instanceof TypeDeclaration) {
               TypeDeclaration td = (TypeDeclaration) o;
               String nm = td.getName().getIdentifier();
               class_names.put(nm,ff);
             }
            break;
          }
       }
      for (CoseResult ff : frags) {
         CompilationUnit cu = (CompilationUnit) ff.getStructure();
         addDepends(ff,cu);
       }
    }
   
   List<CoseResult> sort() {
      if (depend_names.isEmpty()) return base_list;
      List<CoseResult> rslt = new ArrayList<>();
      Set<CoseResult> done = new HashSet<>();
      while (rslt.size() < base_list.size()) {
         boolean chng = false;
         CoseResult fst = null;
         for (CoseResult ff : base_list) {
            if (done.contains(ff)) continue;
            if (fst == null) fst = ff;
            boolean allok = true;
            List<CoseResult> rqs = depend_names.get(ff);
            if (rqs != null) {
               for (CoseResult xf : rqs) {
                  if (!done.contains(xf)) allok = false;
                }
             }
            if (allok) {
               rslt.add(ff);
               done.add(ff);
               chng = true;
             }
          }
         if (!chng) {
            rslt.add(fst);
            done.add(fst);
          }
       }
      return rslt;
    }
   
   private void addDepends(CoseResult ff,CompilationUnit cu) {
      String pkg = null;
      if (cu.getPackage() != null) {
         pkg = cu.getPackage().getName().getFullyQualifiedName();
       }
      for (Object o : cu.types()) {
         if (o instanceof TypeDeclaration) {
            TypeDeclaration td = (TypeDeclaration) o;
            addDepends(ff,pkg,td);
          }
       }
    }
   
   private void addDepends(CoseResult to,String pkg,TypeDeclaration td) {
      addDepend(td.getSuperclassType(),pkg,to);
      for (Object o : td.superInterfaceTypes()) {
         Type it = (Type) o;
         addDepend(it,pkg,to);
       }
    }
   
   private void addDepend(Type t,String pkg,CoseResult to) {
      if (t == null) return;
      if (t.isSimpleType()) {
         SimpleType st = (SimpleType) t;
         String tnm = st.getName().getFullyQualifiedName();
         if (pkg != null && tnm.startsWith(pkg)) tnm = tnm.substring(pkg.length() + 1);
         int idx = tnm.indexOf(".");
         if (idx >= 0) tnm = tnm.substring(0,idx);
         CoseResult frm = class_names.get(tnm);
         if (frm != null || frm == to) {
            List<CoseResult> dps = depend_names.get(to);
            if (dps == null) {
               dps = new ArrayList<>();
               depend_names.put(to,dps);
             }
            dps.add(frm);
          }
       }
    }
   
}	// end of inner class FileSorter



/********************************************************************************/
/*                                                                              */
/*      Merge a file into an AST                                                */
/*                                                                              */
/********************************************************************************/

@SuppressWarnings("unchecked")
private static CompilationUnit mergeIntoAst(CompilationUnit rn,CompilationUnit nn,
      Set<String> pkgs)
{
   String pnm = null;
   PackageDeclaration pd = rn.getPackage();
   if (pd != null) {
      pnm = pd.getName().getFullyQualifiedName();
    }
   
   Set<String> imps = new HashSet<String>();
   for (Iterator<?> it = rn.imports().iterator(); it.hasNext(); ) {
      ImportDeclaration id = (ImportDeclaration) it.next();
      if (importFromPackage(id,pkgs)) it.remove();
      else {
	 String fq = getImportRename(id,pnm,pkgs);
	 if (fq == null) fq = id.getName().getFullyQualifiedName();
	 imps.add(fq);
       }
    }
   
   fixNameConflicts(rn,nn);
   
   for (Object nimp : nn.imports()) {
      ImportDeclaration id = (ImportDeclaration) nimp;
      if (importFromPackage(id,pkgs)) continue;
      String nfq = getImportRename(id,pnm,pkgs);
      if (nfq != null && imps.contains(nfq)) continue;
      String fq = id.getName().getFullyQualifiedName();
      if (imps.contains(fq)) continue;
      ImportDeclaration nid = (ImportDeclaration) ASTNode.copySubtree(rn.getAST(),id);
      if (nfq != null) {
	 Name nnm = JcompAst.getQualifiedName(rn.getAST(),nfq);
	 nid.setName(nnm);
       }
      rn.imports().add(nid);
    }
   
   for (Object ntyp : nn.types()) {
      AbstractTypeDeclaration td = (AbstractTypeDeclaration) ntyp;
      ASTNode ntd = ASTNode.copySubtree(rn.getAST(),td);
      rn.types().add(ntd);
    }
   
   return rn;
}



/********************************************************************************/
/*                                                                              */
/*      Fix up name conflicts on file merge                                     */
/*                                                                              */
/********************************************************************************/

private static void fixNameConflicts(CompilationUnit orig,CompilationUnit add)
{
   Set<String> imptyps = new HashSet<String>();
   for (Iterator<?> it = orig.imports().iterator(); it.hasNext(); ) {
      ImportDeclaration id = (ImportDeclaration) it.next();
      if (id.isOnDemand()) continue;
      if (id.isStatic()) continue;
      String nm = id.getName().getFullyQualifiedName();
      imptyps.add(nm);
    }
   
   Set<String> checks = new HashSet<String>(check_names);
   for (Iterator<?> it = add.imports().iterator(); it.hasNext(); ) {
      ImportDeclaration id = (ImportDeclaration) it.next();
      if (id.isOnDemand()) continue;
      if (id.isStatic()) continue;
      String nm = id.getName().getFullyQualifiedName();
      if (imptyps.contains(nm)) {
	 it.remove();
       }
      else {
	 int idx = nm.lastIndexOf(".");
	 if (idx < 0) continue;
	 String tnm = nm.substring(idx);
	 for (String origtyp : imptyps) {
	    if (origtyp.endsWith(tnm)) {
	       checks.add(nm);
	       it.remove();
	       break;
	     }
	  }
       }
    }
   // check if add uses java.awt.List, javax.swing.filechooser.FileFilter, ...
   // if so, remove any explicit imports and change all internal type references
   // to be fully qualified
   
   NameChecker nc = new NameChecker(checks);
   add.accept(nc);
}


private static class NameChecker extends ASTVisitor {

   private Set<String> name_checks;
   
   NameChecker(Set<String> chks) {
      name_checks = chks;
    }
   
   @Override public boolean visit(ImportDeclaration d) {
      if (!d.isOnDemand() && !d.isStatic()) {
         String nm = d.getName().getFullyQualifiedName();
         if (name_checks.contains(nm)) {
            CompilationUnit cu = (CompilationUnit) d.getParent();
            cu.imports().remove(d);
          }
       }
      return false;
    }
   
   @Override public boolean visit(QualifiedName n) {
      JcompType jt = JcompAst.getJavaType(n);
      if (jt == null) return true;
      if (!name_checks.contains(jt.getName())) return true;
      return false;
    }
   
   @SuppressWarnings("unchecked")
   @Override public boolean visit(SimpleType n) {
      JcompType jt = JcompAst.getJavaType(n);
      if (jt != null) {
         String nm = jt.getName();
         if (name_checks.contains(nm)) {
            Name qn = JcompAst.getQualifiedName(n.getAST(),nm);
            ASTNode par = n.getParent();
            StructuralPropertyDescriptor spd = n.getLocationInParent();
            if (spd.isChildProperty()) {
               Class<?> c = ((ChildPropertyDescriptor) spd).getChildType();
               if (c != SimpleType.class) {
                  par.setStructuralProperty(spd,qn);
                }
             }
            else if (spd.isChildListProperty()) {
               List<Object> l = (List<Object>) par.getStructuralProperty(spd);
               int idx = l.indexOf(n);
               if (idx >= 0) l.set(idx,qn);
             }
          }
       }
      return true;
    }
   
}	// end of inner class NameChecker





private static boolean importFromPackage(ImportDeclaration id,Set<String> pkgs)
{
   for (String pkg : pkgs) {
      String fq = id.getName().getFullyQualifiedName();
      if (id.isOnDemand() && pkg.equals(fq)) return true;
      
      if (!fq.startsWith(pkg)) continue;
      int idx = pkg.length();
      if (fq.length() <= idx) continue;
      if (fq.charAt(idx) != '.') continue;
      if (fq.length() <= idx+2) continue;
      String rnm = fq.substring(idx+1);
      if (Character.isUpperCase(rnm.charAt(0)) && rnm.indexOf(".") < 0) return true;
    }
   
   return false;
}



private static String getImportRename(ImportDeclaration id,String pnm,Set<String> pkgs)
{
   String bnm = null;
   for (String pkg : pkgs) {
      if (pkg.equals(pnm)) continue;
      String fq = id.getName().getFullyQualifiedName();
      if (id.isOnDemand()) continue;
      if (!fq.startsWith(pkg)) continue;
      int idx = pkg.length();
      if (fq.charAt(idx) != '.') continue;
      if (fq.length() <= idx+2) continue;
      String rnm = fq.substring(idx+1);
      if (!Character.isUpperCase(rnm.charAt(0))) continue;
      if (bnm == null || bnm.length() > rnm.length()) bnm = rnm;
    }
   
   if (bnm == null) return null;
   if (pnm == null) return bnm;
   
   return pnm + "." + bnm;
}




/********************************************************************************/
/*                                                                              */
/*      Code for project determination                                          */
/*                                                                              */
/********************************************************************************/

public static Set<String> getRelatedJavaProjects(CoseResult fj)
{
   Set<String> rslt = new HashSet<String>();
   CompilationUnit cu = (CompilationUnit) fj.getStructure();
   PackageDeclaration pd = cu.getPackage();
   if (pd == null) return rslt;
   String nm = pd.getName().getFullyQualifiedName();
   if (fj.getPackages() != null) {
      for (String s : fj.getPackages()) {
	 if (s.length() < nm.length())
	    nm = s;
       }
    }
   
   for (Object o : cu.imports()) {
      ImportDeclaration id = (ImportDeclaration) o;
      if (id.isStatic()) continue;
      String inm = id.getName().getFullyQualifiedName();
      if (!id.isOnDemand()) {
	 int idx = inm.lastIndexOf(".");
	 if (idx < 0) continue;
	 inm = inm.substring(0,idx);
       }
      if (inm.equals(nm)) continue;
      if (inm.startsWith(nm)) rslt.add(inm);
      else if (nm.startsWith(inm)) rslt.add(inm);
      else {
	 int idx = -1;
	 for (int i = 0; i < 3; ++i) {
	    idx = nm.indexOf(".",idx+1);
	    if (idx < 0) break;
	  }
	 if (idx >= 0 && idx < inm.length() &&
               nm.substring(0,idx).equals(inm.substring(0,idx))) {
	    rslt.add(inm);
	  }
       }
    }
   
   return rslt;
}



public static Set<String> getUsedJavaProjects(CoseResult fj)
{
   Set<String> rslt = new HashSet<String>();
   for (CoseResult ffrag : fj.getInnerResults()) {
      CompilationUnit cu = (CompilationUnit) ffrag.getStructure();
      PackageDeclaration pd = cu.getPackage();
      if (pd == null) continue;
      String nm = pd.getName().getFullyQualifiedName();
      rslt.add(nm);
    }
   
   return rslt;
}



/********************************************************************************/
/*                                                                              */
/*      JavaFileResult -- file Result for Java source                           */
/*                                                                              */
/********************************************************************************/

static class JavaFileResult extends ResultFile {
   
   private CompilationUnit ast_node;
   
   JavaFileResult(CoseSource src,String code) {
      super(src,code);
      ast_node = JcompAst.parseSourceFile(code);
    }
   
   @Override public Object getStructure()               { return ast_node; }
   @Override public Object checkStructure()             { return ast_node; }
   
   @Override protected Object getDeltaStructure(ResultDelta rd) {
      return getDeltaJavaStructure(rd);
    }
   
   @Override public Collection<CoseResult> getResults(CoseSearchType t) {
      return getJavaResults(this,t);
    }
   
   @Override public String getKeyText() {
      return getJavaKeyText(getStructure());
    }
   
   @Override public String getEditText()                { return getText(); }
   
   @Override public Set<String> getRelatedProjects(){ 
      return getRelatedJavaProjects(this);
    }
   
   @Override public Set<String> getUsedProjects(){ 
      return getUsedJavaProjects(this);
    }
   
   @Override public CoseResult cloneResult(Object o,Object data) {
      return cloneJavaResult(this,o,data);
    }
   
   
}       // end of inner class JavaFileResult



/********************************************************************************/
/*                                                                              */
/*      JavaMethodResult -- method result for Java source                       */
/*                                                                              */
/********************************************************************************/

static class JavaMethodResult extends ResultPart {
   
   private MethodDeclaration method_node; 
   
   JavaMethodResult(JavaFileResult par,MethodDeclaration n) {
      super(par,n.getStartPosition(),n.getLength());
      method_node = n;
    }
   
   @Override public CoseResultType getResultType()      { return CoseResultType.METHOD; }
   @Override public Object getStructure()               { return method_node; }
   @Override public Object checkStructure()             { return method_node; }
   @Override protected Object getDeltaStructure(ResultDelta rd) {
      return getDeltaJavaStructure(rd);
    }
   
   @Override public String getKeyText() {
      return getJavaKeyText(getStructure());
    }
   
   @Override public String getEditText()                { return getParent().getEditText(); }
   
   @Override public CoseResult cloneResult(Object o,Object data) {
      return cloneJavaResult(this,o,data);
    }
   
}       // end of inner class JavaMethodResult



/********************************************************************************/
/*                                                                              */
/*      JavaClassResult -- class result for Java source                         */
/*                                                                              */
/********************************************************************************/

static class JavaClassResult extends ResultPart { 

   private AbstractTypeDeclaration ast_node;
   
   JavaClassResult(JavaFileResult par,AbstractTypeDeclaration atd) {
      super(par,atd.getStartPosition(),atd.getLength());
      ast_node = atd;
    }

   @Override public CoseResultType getResultType()      { return CoseResultType.CLASS; }
   @Override public Object getStructure()               { return ast_node; }
   @Override public Object checkStructure()             { return ast_node; }
   @Override protected Object getDeltaStructure(ResultDelta rd) {
      return getDeltaJavaStructure(rd);
    }
   
   @Override public String getEditText()                { return getParent().getEditText(); }
   
   @Override public String getKeyText() {
      return getJavaKeyText(getStructure());
    }
   
   @Override public CoseResult cloneResult(Object o,Object data) {
      return cloneJavaResult(this,o,data);
    }
   
}       // end of inner class JavaClassResult



/********************************************************************************/
/*                                                                              */
/*      JavaPackageResult -- package result for Java source                     */
/*                                                                              */
/********************************************************************************/

static class JavaPackageResult extends ResultGroup {

   private ASTNode ast_node;
   private String source_text;
   private Set<String> used_packages;
   private String base_package;
   
   JavaPackageResult(CoseSource src) {
      super(src);
      used_packages = new HashSet<>();
      ast_node = null;
      source_text = null;
      base_package = null;
    }
   
   @Override public Object getStructure() {
      if (ast_node == null) {
         buildRoot();
       }
     return ast_node; 
    }  
   
   @Override public Object checkStructure() {
      return getStructure();
   } 
   @Override protected Object getDeltaStructure(ResultDelta rd) {
      return getDeltaJavaStructure(rd);
    }
   
   @Override public String getKeyText() {
      return getJavaKeyText(getStructure());
    }
   
   @Override public String getText() {
      buildRoot();
      return source_text;
    }
   
   @Override public String getEditText() {
      return getText(); 
    }
   
   @Override public Collection<String> getPackages() {
      return new ArrayList<String>(used_packages);
    }
   @Override public String getBasePackage() {
      if (base_package == null && ast_node == null) buildRoot();
      return base_package;
    }
   @Override public boolean addPackage(String pkg) {
      if (ast_node == null) buildRoot();
      return used_packages.add(pkg);
    }
   
   @Override public synchronized void addInnerResult(CoseResult sf) {
      super.addInnerResult(sf);
      JcompAst.setKeep((ASTNode) sf.getStructure(),false);
      ast_node = null;
    }
   
   @Override public Set<String> getRelatedProjects(){ 
      return getRelatedJavaProjects(this);
    }
   @Override public Set<String> getUsedProjects(){ 
      return getUsedJavaProjects(this);
    }
   
   @Override public CoseResult cloneResult(Object o,Object data) {
      return cloneJavaResult(this,o,data);
    }
   
   private void buildRoot() {
      if (ast_node != null) return;
      if (inner_results.size() == 0) return;
      
      FileSorter fs = new FileSorter(inner_results);
      inner_results = fs.sort();
      CompilationUnit root = null;
      
      for (CoseResult ff : inner_results) {
         CompilationUnit fn = (CompilationUnit) ff.getStructure();
         if (root == null) {
            AST nast = JcompAst.createNewAst();
            root = (CompilationUnit) ASTNode.copySubtree(nast,fn);
          }
         else root = mergeIntoAst(root,fn,used_packages);
       }
      if (used_packages.size() == 0) {
         PackageDeclaration pd = root.getPackage();
         if (pd != null) {
            String pnm = pd.getName().getFullyQualifiedName();
            base_package = pnm;
            used_packages.add(pnm);
          }
       }
      // is this really needed?
      source_text = root.toString();
      root = JcompAst.parseSourceFile(source_text);
      ast_node = root;
    }
   
}       // end of inner class JavaPackageResult





}       // end of class ResultJava




/* end of ResultJava.java */

