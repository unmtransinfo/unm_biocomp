package edu.unm.health.biocomp.tautomer;

import java.io.*;
import java.lang.Math;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Base64;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.marvin.calculations.TautomerizationPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;

/**	Predicts tautomers for ONE input molecule.

	@author Jeremy J Yang
*/
public class tautomers_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String APPNAME=null;   // configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=10*1024*1024; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static int N_MAX=100; // configured in web.xml
  private static String PREFIX=null;
  private static MolImporter molReader=null;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String color1="#EEEEEE";
  private static String MOL2IMG_SERVLETURL=null;
  private static String PROXY_PREFIX=null; // configured in web.xml

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    SERVERNAME=request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME=InetAddress.getLocalHost().getHostAddress();
    REMOTEHOST=request.getHeader("X-Forwarded-For"); // client (original)
    if (REMOTEHOST!=null)
    {
      String[] addrs=Pattern.compile(",").split(REMOTEHOST);
      if (addrs.length>0) REMOTEHOST=addrs[addrs.length-1];
    }
    else
    {
      REMOTEHOST=request.getRemoteAddr(); // client (may be proxy)
    }

    CONTEXTPATH=request.getContextPath();
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest=new MultipartRequest(request,UPLOADDIR,10*1024*1024,"ISO-8859-1",
                                    new DefaultFileRenamePolicy()); }
      catch (IOException lEx) {
        this.getServletContext().log("not a valid MultipartRequest",lEx); }
    }

    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/css/biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/js/biocomp.js",PROXY_PREFIX+CONTEXTPATH+"/js/ddtip.js"));
    boolean ok=initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
      out.println(HtmUtils.FooterHtm(errors,true));
      return;
    }
    if (mrequest!=null)	//method=POST, normal operation
    {
      if (mrequest.getParameter("tautomers").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));

        ArrayList<TautomerResult> tresults = Tautomers(mrequest,response);
        TautomerResultsOutput(tresults);

        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
    else
    {
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (request.getParameter("help")!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadFile(response,ostream,downloadfile,request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        out.println("<SCRIPT>go_init(window.document.mainform)</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request, MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs=new ArrayList<String>();
    errors=new ArrayList<String>();
    params=new HttpParams();
    MOL2IMG_SERVLETURL=(PROXY_PREFIX+CONTEXTPATH+"/mol2img");

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    try { LicenseManager.setLicenseFile(CONTEXT.getRealPath("")+"/.chemaxon/license.cxl"); }
    catch (Exception e) {
      errors.add("ERROR: "+e.getMessage());
      if (System.getenv("HOME") !=null) {
        try { LicenseManager.setLicenseFile(System.getenv("HOME")+"/.chemaxon/license.cxl"); }
        catch (Exception e2) {
          errors.add("ERROR: "+e2.getMessage());
        }
      }
    }
    LicenseManager.refresh();
    if (!LicenseManager.isLicensed(LicenseManager.JCHEM) || !LicenseManager.isLicensed(LicenseManager.ISOMERS_PLUGIN_GROUP))
    {
      errors.add("ERROR: ChemAxon license error; JCHEM + ISOMERS_PLUGIN_GROUP required.");
      return false;
    }

    if (mrequest==null) return true;

    /// Stuff for a run:

    for (Enumeration e=mrequest.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (mrequest.getParameter(key)!=null)
        params.setVal(key,mrequest.getParameter(key));
    }

    if (params.isChecked("verbose"))
    {
      errors.add("server: "+CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
      errors.add("ServletContextName: "+CONTEXT.getServletContextName());
      errors.add("Servlet ContextPath: "+CONTEXT.getContextPath()); // e.g.  "/biocomp"
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
    }

    String fname="infile";
    File fileDB=mrequest.getFile(fname);
    String intxtDB=params.getVal("intxt").replaceFirst("[\\s]+$","");
    String line = null;
    if (fileDB!=null)
    {
      if (params.isChecked("file2txt"))
      {

        if (params.getVal("ifmt").equals("cdx")) // inbytes binary?:
        {
          int maxbytes=400*1024;
          byte[] inbytes=new byte[maxbytes];
          FileInputStream fis=new FileInputStream(fileDB);
          int b;
          int size=0;
          while ((b=fis.read())>=0)
          {
            if (size+1>maxbytes)
            {
              errors.add("ERROR: max bytes copied to input: "+maxbytes);
              break;
            }
            inbytes[size++]=(byte)b;
          }
          byte[] tmp=new byte[size];
          System.arraycopy(inbytes,0,tmp,0,size);
          inbytes=tmp;
          //intxtDB=Base64Encoder.encode(inbytes);
          intxtDB=Base64.encodeBase64String(inbytes);
        }
        else
        {
          BufferedReader br=new BufferedReader(new FileReader(fileDB));
          intxtDB="";
          for (int i=0;(line=br.readLine())!=null;++i)
          {
            intxtDB+=(line+"\n");
          }
        }
        params.setVal("intxt",intxtDB);
      }
      else
      {
        params.setVal("intxt","");
      }
    }

    if (params.getVal("ifmt").equals("automatic"))
    {
      String orig_fname=mrequest.getOriginalFileName(fname);
      String ifmt_auto=MFileFormatUtil.getMostLikelyMolFormat(orig_fname);
      if (orig_fname!=null && ifmt_auto!=null)
      {
        if (fileDB!=null)
          molReader=new MolImporter(fileDB,ifmt_auto);
        else if (intxtDB.length()>0)
          molReader=new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()),ifmt_auto);
      }
      else
      {
        if (fileDB!=null)
          molReader=new MolImporter(new FileInputStream(fileDB));
        else if (intxtDB.length()>0)
          molReader=new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()));
      }
    }
    else
    {
      String ifmt=params.getVal("ifmt");
      if (fileDB!=null)
        molReader=new MolImporter(new FileInputStream(fileDB),ifmt);
      else if (intxtDB.length()>0)
      if (params.getVal("ifmt").equals("cdx")) // inbytes binary?:
      {
        //byte[] inbytes=Base64Decoder.decodeToBytes(intxtDB);
        byte[] inbytes=Base64.decodeBase64(intxtDB);
        molReader=new MolImporter(new ByteArrayInputStream(inbytes),"cdx");
      }
      else
        molReader=new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()),ifmt);
    }

    String fmtdesc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
    if (params.isChecked("verbose"))
    {
      errors.add("input format:  "+molReader.getFormat()+" ("+fmtdesc+")");
    }

    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response)
      throws IOException
  {
    String ifmt_menu_opts=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      if (fmt.equals("gzip") || fmt.equals("base64")) continue;
      ifmt_menu_opts+=("<OPTION VALUE=\""+fmt+"\">");
      ifmt_menu_opts+=(fmt+" - "+MFileFormatUtil.getFormat(fmt).getDescription());
    }
    String ifmt_menu="<SELECT NAME=\"ifmt\">"+ifmt_menu_opts+"</SELECT>";
    ifmt_menu=ifmt_menu.replace("\""+params.getVal("ifmt")+"\">", "\""+params.getVal("ifmt")+"\" SELECTED>");

    String ofmt_menu_opts=("");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      if (!(fmt.equals("smiles") || fmt.equals("sdf"))) continue;
      ofmt_menu_opts+=("<OPTION VALUE=\""+fmt+"\">");
      ofmt_menu_opts+=(fmt+" - "+MFileFormatUtil.getFormat(fmt).getDescription());
    }
    String ofmt_menu="<SELECT NAME=\"ofmt\">"+ofmt_menu_opts+"</SELECT>";
    ofmt_menu=ofmt_menu.replace("\""+params.getVal("ofmt")+"\">", "\""+params.getVal("ofmt")+"\" SELECTED>");    

    String htm=
      ("<FORM NAME=\"mainform\" METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\" ENCTYPE=\"multipart/form-data\">\n")
     +("<INPUT TYPE=HIDDEN NAME=\"tautomers\">\n")
     +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+APPNAME+"</H1></TD>\n")
     +("<TD ALIGN=RIGHT>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
     +("</TD></TR></TABLE>\n")
     +("<HR>\n")
     +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
     +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP WIDTH=\"60%\">\n")
     +("<H3>Input:</H3>\n")
     +("format:"+ifmt_menu)
     +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
     +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:")
     +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxt")+"</TEXTAREA>\n")
     +("</TD>\n")
     +("<TD VALIGN=TOP>\n")
     +("<TABLE WIDTH=100%><TR><TD VALIGN=TOP>\n")
     +("<TR><TD VALIGN=TOP>\n")
     +("<H3>Output:</H3>\n")
     +("format:"+ofmt_menu)
     +("</TD></TR>\n")
     +("<TR><TD VALIGN=TOP>\n")
     +("<H3>Misc:</H3>\n")
     +("pH:<INPUT TYPE=TEXT SIZE=4 NAME=\"pH\" VALUE=\""+params.getVal("pH")+"\"><BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"showh\" VALUE=\"CHECKED\" "+params.getVal("showh")+">show Hs<BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"showarom\" VALUE=\"CHECKED\" "+params.getVal("showarom")+">showarom<BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
     +("</TD></TR>\n")
     +("</TABLE>\n")
     +("</TD></TR></TABLE>\n")
     +("<P>\n")
     +("<CENTER>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"go_tautomers(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
     +("</CENTER>\n")
     +("</FORM>\n");
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static ArrayList<TautomerResult> Tautomers(MultipartRequest mrequest,HttpServletResponse response)
      throws IOException
  {
    String ifmt=molReader.getFormat();
    MFileFormat imffmt=MFileFormatUtil.getFormat(ifmt);
    String ofmt=params.getVal("ofmt");
    MFileFormat omffmt=MFileFormatUtil.getFormat(ofmt);

    File fout=null;
    String oext=null;
    //kludge - why does this fail for smi?
    try { oext=MFileFormatUtil.getFormat(ofmt).getExtensions()[0]; }
    catch (Exception e) { oext=params.getVal("ofmt"); }
    try {
      File dout = new File(SCRATCHDIR);
      if (!dout.exists())
      {
        boolean ok=dout.mkdir();
        System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
      }
      fout=File.createTempFile(PREFIX,"_out."+oext,dout);
    }
    catch (IOException e) {
      errors.add("ERROR: could not open temp file; check SCRATCHDIR: "+SCRATCHDIR);
      return null;
    }
    String ofmt_full = (ofmt.equals("smiles") ? "smiles:T*" : ofmt);

    MolExporter molWriter=new MolExporter(new FileOutputStream(fout),ofmt_full);

    ArrayList<TautomerResult> tresults = new ArrayList<TautomerResult>();

    int n_mols_in=0;
    int n_tmols_out=0;
    Molecule mol;
    int n_failed=0;
    while (true)
    {
      if (n_mols_in==N_MAX)
      {
        errors.add("Limit reached: N_MAX mols: "+N_MAX);
        break;
      }
      try { mol=molReader.read(); }
      catch (MolFormatException e)
      {
        outputs.add("MolImporter failed: "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (mol==null) break;
      ++n_mols_in;

      TautomerResult tresult = new TautomerResult(mol);

      // Generate tautomers:
      ArrayList<Molecule> tmols=Mol2Tautomers(mol,Float.parseFloat(params.getVal("pH")));

      // Output as (1) SMILES/TSV or (2) SD property.
      for (Molecule tmol: tmols)
      {
        mol.setProperty("TAUTOMER", tmol.exportToFormat("smiles"));
        molWriter.write(mol);
        tresult.addTautomer(tmol);
        ++n_tmols_out;
      }
      tresults.add(tresult);
    }
    molReader.close();
    molWriter.close();
    outputs.add("<H2>Results:</H2>");
    outputs.add("<B>mols in:</B> "+n_mols_in);
    outputs.add("<B>tautomers out:</B> "+n_tmols_out);
    outputs.add("<B>errors:</B> "+n_failed);

    String fname=(SERVLETNAME+"_out."+oext);
    if (params.isChecked("gzip")) fname+=".gz";
    String bhtm=("<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\"><B>Download "+fname+" ("+file_utils.NiceBytes(fout.length())+")</B></BUTTON></FORM>\n");
    outputs.add("<BLOCKQUOTE>"+bhtm+"<BR>\n<I>(Output file contains query molecules and associated tautomers.)</I></BLOCKQUOTE>");

    return tresults;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static ArrayList<Molecule> Mol2Tautomers(Molecule mol, Float pH)
  {
    ArrayList<Molecule> tmols=new ArrayList<Molecule>();
    TautomerizationPlugin plugin=new TautomerizationPlugin();

    // set pH (optional, only set if pH effect should be considered)
    if (pH!=null) plugin.setpH(pH);

    try {
      plugin.setMolecule(mol);
      plugin.run();
    }
    catch (PluginException e)
    {
      errors.add("ERROR: Cannot generate tautomers:"+e.getMessage());
      return tmols;
    }

    // get the tautomers
    int n_taut=plugin.getStructureCount();
    errors.add("n_tautomers: "+n_taut);
    for (int i=0;i<n_taut;++i)
    {
      Molecule tmol=plugin.getStructure(i);
      tmols.add(tmol);
    }
    return tmols;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void TautomerResultsOutput(ArrayList<TautomerResult> tresults)
      throws IOException, ServletException
  {
    int n_mols=0;
    int n_tmols=0;
    int deph=160;
    int depw=160;
    int n_cols=(int)(500.0/(float)depw); //For tautomer table.
    String depopts=("mode=cow&imgfmt=png");
    if (params.isChecked("showarom")) depopts+=("&arom_gen=true");
    else depopts+=("&kekule=true");
    if (params.isChecked("showh")) depopts+=("&showh=true");

    String imghtmQ;
    String thtm=("<TABLE BORDER>\n");
    thtm+=("<TR><TH></TH><TH>Mol</TH><TH>Tautomers</TH></TR>\n");

    for (TautomerResult tresults_this:tresults)
    {
      ++n_mols;
      thtm+=("<TR>\n");
      thtm+=("<TD ALIGN=\"right\" VALIGN=\"top\">"+n_mols+"</TD>\n");
      thtm+=("<TD ALIGN=\"center\" VALIGN=\"top\">"+tresults_this.getMol().getName()+"<BR>\n");
      String smi=tresults_this.getMol().exportToFormat("smiles");
      thtm+=(HtmUtils.Smi2ImgHtm(smi,depopts,depw,deph,MOL2IMG_SERVLETURL,true,4,"go_zoom_smi2img")+"</TD>\n");

      thtm+=("<TD><CENTER><B>TautomerCount: "+tresults_this.tautomerCount()+"</B></CENTER><BR>\n");
      String thtm2=("<TABLE BORDER=\"0\">\n");
      int n_tmols_this=0;
      for (Molecule tmol: tresults_this.tautomerList())
      {
        if (n_tmols_this%n_cols==0) thtm2+="<TR>\n";
        ++n_tmols_this;
        String tsmi=tmol.exportToFormat("smiles");
        thtm2+=("<TD VALIGN=\"top\">"+HtmUtils.Smi2ImgHtm(tsmi,depopts,depw,deph,MOL2IMG_SERVLETURL,true,4,"go_zoom_smi2img")+"</TD>\n");
        if (n_tmols_this%n_cols==0) thtm2+="</TR>\n";
      }
      if (n_tmols_this%n_cols>0)
      {
        if (n_tmols_this>n_cols)
        {
          for (int i=n_tmols_this%n_cols;i<n_cols;++i) { thtm2+=("<TD></TD>\n"); }
        }
        thtm2+=("</TR>");
      }
      thtm2+="</TABLE>\n";
      thtm+=(thtm2+"</TD></TR>\n");
      n_tmols+=n_tmols_this;
    }
    thtm+=("</TABLE>");
    outputs.add(thtm);
    errors.add("n_mols: "+n_mols+" ; n_tmols: "+n_tmols);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
  {
    return(
"function go_init(form)"+
"{\n"+
"  form.file2txt.checked=true;\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.ifmt.length;++i)\n"+
"    if (form.ifmt.options[i].value=='automatic')\n"+
"      form.ifmt.options[i].selected=true;\n"+
"  for (i=0;i<form.ofmt.length;++i)\n"+
"    if (form.ofmt.options[i].value=='smiles')\n"+
"      form.ofmt.options[i].selected=true;\n"+
"  form.showh.checked=false;\n"+
"  form.pH.value='0.0';\n"+
"  form.showarom.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input specified');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_tautomers(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.tautomers.value='TRUE'\n"+
"  form.submit()\n"+
"}\n");
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    String htm = (
    "<B>"+APPNAME+" Help</B><P>\n"+
    "<P>\n"+
    "Predicts tautomers for input molecules.\n"+
    "Uses the ChemAxon tautomerization calculator plugin.\n"+
    "<P>\n"+
    "Example input:<br/>\n"+
    "<ul>\n"+
    "<li> C([C@@H]([C@@H]1C(=C(C(=O)O1)O)O)O)O Vitamin C\n"+
    "</ul>\n"+
    "<P>\n"+
    "Output formats:\n"+
    "<ul>\n"+
    "<li><B>SMILES</B> with appended tab delimited TAUTOMER smiles, one line per tautomer.\n"+
    "<li><B>SDF</B> with TAUTOMER smiles as property, one molfile per tautomer.\n"+
    "</ul>\n"+
    "<P>\n"+
    "configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "author/support: Jeremy Yang\n"
    );
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    //CONFIG=conf;
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    try { APPNAME=conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME=this.getServletName(); }
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    PROXY_PREFIX=((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException, ServletException
  {
    doPost(request,response);
  }
}

