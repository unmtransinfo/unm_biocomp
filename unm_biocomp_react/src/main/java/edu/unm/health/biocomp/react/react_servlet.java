package edu.unm.health.biocomp.react;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.*; //MultipartRequest, Base64Encoder, Base64Decoder
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*; //MolExporter
import chemaxon.struc.*;
import chemaxon.license.*;
import chemaxon.util.MolHandler;
import chemaxon.reaction.*;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;

/**	Reaction transform application using Reactor.

	to do: <br>
	* [ ] MarvinSketch reaction? <br>
	* [ ] depict smirks button <br>
	* [ ] download buttons. <br>

	@author Jeremy J Yang
*/
public class react_servlet extends HttpServlet
{
  private static String SERVERNAME=null;
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static int N_MAX=100; // configured in web.xml
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static ArrayList<Molecule> MOLS=null;
  private static String REMOTEHOST=null;
  private static String datestr=null;
  private static File logfile=null;
  private static String color1="#EEEEEE";
  private static String MOL2IMG_SERVLETURL=null;
  private static String PROXY_PREFIX=null;	// configured in web.xml

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
      try { mrequest=new MultipartRequest(request,UPLOADDIR,10*1024*1024,"ISO-8859-1",new DefaultFileRenamePolicy()); }
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
      out.println(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
      out.println(HtmUtils.FooterHtm(errors,true));
      return;
    }
    if (mrequest!=null)	//method=POST, normal operation
    {
      if (mrequest.getParameter("react").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        ArrayList<ArrayList<Molecule> > rxnmols = react_utils.ReactMols(MOLS,params.getVal("smirks"),params.isChecked("recurse"),params.isChecked("verbose"));
        ReactOutput(rxnmols,params.isChecked("verbose"));
        PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(logfile,true)));
        out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,rxnmols.size()); 
        out_log.close();
        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
    else
    {
      String help=request.getParameter("help");	// GET param
      if (help!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        out.println("<SCRIPT>go_reset(window.document.mainform)</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request,MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs=new ArrayList<String>();
    errors=new ArrayList<String>();
    params=new HttpParams();
    MOLS=new ArrayList<Molecule>();
    MOL2IMG_SERVLETURL=(PROXY_PREFIX+CONTEXTPATH+"/mol2img");

    errors.add("<A HREF=\"http://medicine.unm.edu/informatics/\">"+
      "<IMG BORDER=0 SRC=\""+HtmUtils.ImageURL("biocomp_logo_only.gif",request)+"\"></A>"+
      SERVLETNAME+" web app from UNM Translational Informatics.");
    errors.add("<A HREF=\"http://www.chemaxon.com\">"+
      "<IMG BORDER=0 SRC=\""+HtmUtils.ImageURL("chemaxon_powered_100px.png",request)+"\"></A>\n"+
          "JChem from ChemAxon Ltd.");

    //Create webapp-specific log dir if necessary:
    File dout=new File(LOGDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      System.err.println("LOGDIR creation "+(ok?"succeeded":"failed")+": "+LOGDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create LOGDIR: "+LOGDIR);
        return false;
      }
    }

    String logpath=LOGDIR+"/"+SERVLETNAME+".log";
    logfile=new File(logpath);
    if (!logfile.exists())
    {
      try { logfile.createNewFile(); }
      catch (IOException e)
      {
        errors.add("ERROR: Cannot create log file:"+e.getMessage());
        return false;
      }
      logfile.setWritable(true,true);
      PrintWriter out_log=new PrintWriter(logfile);
      out_log.println("date\tip\tN"); 
      out_log.flush();
      out_log.close();
    }
    if (!logfile.canWrite())
    {
      errors.add("ERROR: Log file not writable.");
      return false;
    }
    BufferedReader buff=new BufferedReader(new FileReader(logfile));
    if (buff==null)
    {
      errors.add("ERROR: Cannot open log file.");
      return false;
    }

    int n_lines=0;
    String line=null;
    String startdate=null;
    while ((line=buff.readLine())!=null)
    {
      ++n_lines;
      String[] fields=Pattern.compile("\\t").split(line);
      if (n_lines==2) startdate=fields[0];
    }
    buff.close(); //Else can result in error: "Too many open files"
    Calendar calendar=Calendar.getInstance();
    if (n_lines>2)
    {
      calendar.set(Integer.parseInt(startdate.substring(0,4)),
               Integer.parseInt(startdate.substring(4,6))-1,
               Integer.parseInt(startdate.substring(6,8)),
               Integer.parseInt(startdate.substring(8,10)),
               Integer.parseInt(startdate.substring(10,12)),0);

      DateFormat df=DateFormat.getDateInstance(DateFormat.FULL,Locale.US);
      errors.add("since "+df.format(calendar.getTime())+", times used: "+(n_lines-1));
    }

    calendar.setTime(new Date());
    datestr=String.format("%04d%02d%02d%02d%02d",
      calendar.get(Calendar.YEAR),
      calendar.get(Calendar.MONTH)+1,
      calendar.get(Calendar.DAY_OF_MONTH),
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE));

    LicenseManager.refresh();
    if (!LicenseManager.isLicensed(LicenseManager.JCHEM) || !LicenseManager.isLicensed(LicenseManager.REACTOR))
    {
      errors.add("ERROR: ChemAxon license error; JCHEM + REACTOR required.");
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
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
      errors.add("server: "+CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
      errors.add("ServletName: "+this.getServletName());
    }
    LicenseManager.refresh();
    if (!(new Reactor()).isLicensed())
    {
      errors.add("Warning: Reactor license not found.");
      return false;
    }

    String fname="infile";
    File ifile=mrequest.getFile(fname);
    String intxt=params.getVal("intxt").replaceFirst("[\\s]+$","");
    byte[] inbytes=new byte[1024];
    if (ifile!=null)
    {
      FileInputStream fis=new FileInputStream(ifile);
      int asize=inbytes.length;
      int size=0;
      int b;
      while ((b=fis.read())>=0)
      {
        if (size+1>asize)
        {
          asize*=2;
          byte[] tmp=new byte[asize];
          System.arraycopy(inbytes,0,tmp,0,size);
          inbytes=tmp;
        }
        inbytes[size]=(byte)b;
        ++size; 
      }
      byte[] tmp=new byte[size];
      System.arraycopy(inbytes,0,tmp,0,size);
      inbytes=tmp;
    }
    else if (intxt.length()>0)
    {
      if (params.getVal("molfmt").equals("cdx"))
        inbytes=Base64Decoder.decodeToBytes(intxt);
      else
        inbytes=intxt.getBytes("utf-8");
    }
    else
    {
      errors.add("No input data.");
      return false;
    }

    MolImporter molReader=null;
    if (params.getVal("molfmt").equals("automatic"))
    {
      String orig_fname=mrequest.getOriginalFileName(fname);
      String molfmt_auto=MFileFormatUtil.getMostLikelyMolFormat(orig_fname);
      if (orig_fname!=null && molfmt_auto!=null)
        molReader=new MolImporter(new ByteArrayInputStream(inbytes),molfmt_auto);
      else
        molReader=new MolImporter(new ByteArrayInputStream(inbytes));
    }
    else
    {
      molReader=new MolImporter(new ByteArrayInputStream(inbytes),params.getVal("molfmt"));
    }
    String fmt=molReader.getFormat();
    params.setVal("molfmt_auto",fmt);

    if (ifile!=null) ifile.delete();

    MFileFormat mffmt=MFileFormatUtil.getFormat(fmt);

    if (params.isChecked("file2txt"))
    {
      if (mffmt==MFileFormat.CDX) //binary
      {
        intxt=Base64Encoder.encode(inbytes);
        if (params.getVal("molfmt").equals("automatic"))
          params.setVal("molfmt","cdx");
      }
      else
      {
        intxt=new String(inbytes,"utf-8");
      }
      params.setVal("intxt",intxt);
    }

    Molecule m;
    int n_failed=0;
    while (true)
    {
      try { m=molReader.read(); }
      catch (MolFormatException e)
      {
        errors.add("ERROR: MolImporter failed: "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (m==null) break;
      m.aromatize(MoleculeGraph.AROM_GENERAL); // aromatize so smirks work correctly.
      MOLS.add(m);
    }
    molReader.close();

    //Check SMIRKS:
    MolHandler rxnHandler=new MolHandler();
    try { rxnHandler.setMolecule(params.getVal("smirks")); }
    catch (MolFormatException e) {
      errors.add("ERROR: "+e.getMessage());
      return false;
    }
    Molecule rxn=rxnHandler.getMolecule();
    if (!rxn.isReaction())
    {
      errors.add("ERROR: invalid reaction (!isReaction()).");
      return false;
    }
    Reactor reactor = new Reactor();
    try { reactor.setReaction(rxnHandler.getMolecule()); }
    catch (chemaxon.reaction.ReactionException e) {
      errors.add("ERROR: "+e.getMessage());
      return false;
    }

    if (params.isChecked("verbose"))
    {
      String desc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
      errors.add("input format:  "+molReader.getFormat()+" ("+desc+")");
      errors.add("mols read:  "+MOLS.size());
    }
    if (n_failed>0) errors.add("ERRORS (unable to read mol): "+n_failed);
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response)
      throws IOException
  {
    String molfmt_menu="<SELECT NAME=\"molfmt\">\n";
    molfmt_menu+=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      String desc=MFileFormatUtil.getFormat(fmt).getDescription();
      molfmt_menu+=("<OPTION VALUE=\""+fmt+"\">"+desc+"\n");
    }
    molfmt_menu+=("</SELECT>\n");
    molfmt_menu=molfmt_menu.replace(params.getVal("molfmt")+"\">",params.getVal("molfmt")+"\" SELECTED>\n");

    String htm=
      ("<FORM NAME=\"mainform\" METHOD=POST ACTION=\""+response.encodeURL(SERVLETNAME)+"\" ENCTYPE=\"multipart/form-data\">\n")
     +("<INPUT TYPE=HIDDEN NAME=\"react\">\n")
     +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+SERVLETNAME+"</H1></TD><TD>- reactions via SMIRKS</TD>\n")
     +("<TD ALIGN=RIGHT>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>help</B></BUTTON>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
     +("</TD></TR></TABLE>\n")
     +("<HR>\n")
     +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
     +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP WIDTH=\"50%\">\n")
     +("format:"+molfmt_menu)
     +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
     +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:")
     +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxt")+"</TEXTAREA>\n")
     +("</TD>\n")
     +("<TD VALIGN=TOP>\n")
     +("<B>method:</B><BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"recurse\" VALUE=\"CHECKED\" "+params.getVal("recurse")+">recurse<BR>\n")
     +("<B>output:</B><BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"showmaps\" VALUE=\"CHECKED\" "+params.getVal("showmaps")+">showmaps<BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"showh\" VALUE=\"CHECKED\" "+params.getVal("showh")+">show Hs<BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"showarom\" VALUE=\"CHECKED\" "+params.getVal("showarom")+">showarom<BR>\n")
     +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
     +("</TD></TR>\n")
     +("<TR BGCOLOR=\"#CCCCCC\"><TD COLSPAN=2 ALIGN=\"center\">\n")
     +("smirks: <INPUT TYPE=\"text\" NAME=\"smirks\" SIZE=\"100\"  VALUE=\""+params.getVal("smirks")+"\">\n")
     +("</TD></TR></TABLE>\n")
     +("<P>\n")
     +("<CENTER>\n")
     +("<BUTTON TYPE=BUTTON onClick=\"go_react(this.form)\"><B>react</B></BUTTON>\n")
     +("</CENTER>\n")
     +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	
  */
  private static void ReactOutput(ArrayList<ArrayList<Molecule> > rxns,Boolean verbose)
      throws IOException
  {
    int h=300;
    int w=800;
    String depopts="";
    if (params.isChecked("showarom")) depopts+=("&arom_gen=true");
    else depopts+=("&kekule=true");
    if (params.isChecked("showmaps")) depopts+=("&showmaps=true");
    if (params.isChecked("showh")) depopts+=("&showh=true");

    int n_mol=0;
    String thtm="";
    thtm=("<TABLE CELLSPACING=2 CELLPADDING=2>\n");
    for (List<Molecule> rxns_this: rxns)
    {
      ++n_mol;
      Molecule mol=MOLS.get(n_mol-1);
      Molecule[] reactants=mol.cloneMolecule().convertToFrags();
      if (verbose)
      {
        errors.add("mol ["+n_mol+"]: "+MolExporter.exportToFormat(mol,"smiles"));
        int i_r=0;
        for (Molecule reac: reactants) {
          ++i_r;
          errors.add("&nbsp; reactant ["+i_r+"]: "+MolExporter.exportToFormat(reac,"smiles"));
        }
      }

      int i_x=0;
      for (Molecule rxn: rxns_this)
      {
        ++i_x;
        String rxnsmi=MolExporter.exportToFormat(rxn,"smiles:u");
        String imghtm=HtmUtils.Smi2ImgHtm(rxnsmi,depopts,h,w,MOL2IMG_SERVLETURL,true,4,"go_zoom_smi2img");
        thtm+=("<TR><TD>"+imghtm+"</TD></TR>\n");
        if (verbose) errors.add("&nbsp; reaction ["+i_x+"]: "+rxnsmi);
      }
      thtm+=("<TR><TD><HR></TD></TR>\n");
      if (n_mol==N_MAX) break;
    }
    thtm+=("</TABLE>");
    outputs.add("<CENTER>"+thtm+"</CENTER>");
    outputs.add("N = "+n_mol);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
  {
    return(
"function go_reset(form)"+
"{\n"+
"  form.file2txt.checked=true;\n"+
"  form.smirks.value='';\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.molfmt.length;++i)\n"+
"    if (form.molfmt.options[i].value=='automatic')\n"+
"      form.molfmt.options[i].selected=true;\n"+
"  form.showh.checked=false;\n"+
"  form.showarom.checked=false;\n"+
"  form.showmaps.checked=false;\n"+
"  form.recurse.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input reactants specified');\n"+
"    return 0;\n"+
"  }\n"+
"  if (!form.smirks.value) {\n"+
"    alert('ERROR: No input reaction (smirks) specified');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_react(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.react.value='TRUE'\n"+
"  form.submit()\n"+
"}\n");
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    return (
    "<B>"+SERVLETNAME+" help</B><P>\n"+
    "<P>\n"+
    "Reactants must be submitted as reactant mixtures represented by\n"+
    "dot-disconnected smiles.  Reactants need not be in order\n"+
    "corresponding with the smirks reactants.\n"+
    "<P>\n"+
    "<B>Examples:</B>\n"+
    "</P><P>\n"+
    "nitro standardization: <BR>\n"+
    "<PRE>\n"+
    "smirks:\n"+
    "[N:1](=[O:2])=[O:3]>>[N+1:1](=[O:2])-[O-:3]\n"+
    "reactants:\n"+
    "CCCN(=O)=O\n"+
    "c1ccccc1CN(=O)=O\n"+
    "</PRE>\n"+
    "</P><P>\n"+
    "<PRE>\n"+
    "smirks:\n"+
    "[O:1]=[C:2][Cl:3].[N:4][H:5]>>[O:1]=[C:2][N:4]\n"+
    "reactants:\n"+
    "CC(=O)Cl.NCC\n"+
    "</PRE>\n"+
    "halogenation of alkenes: <BR>\n"+
    "<PRE>\n"+
    "smirks:\n"+
    "[F,Cl,Br:1][F,Cl,Br:2].[CX4;H3,H2:3][CX3H:4]=[CX3H:5][CX4;H3,H2:6]>>[C:3][C:4]([*:1])[C:5]([*:2])[C:6]\n"+
    "reactants:\n"+
    "[Br][Br].CC=CC\n"+
    "[F][F].CCC=CC\n"+
    "[Br][Br].CC=CCC=CCC\n"+
    "</PRE>\n"+
    "amino acid polymerization: <BR>\n"+
    "<PRE>\n"+
    "smirks:\n"+
    "[N:1][H:5].[CX3:2](=[O:3])[OH:4]>>[C:2](=[O:3])[N:1].[O:4][H:5]\n"+
    "reactants:\n"+
    "NC(C)C(=O)O.NCC(=O)O\n"+
    "NC(C)C(=O)O.NC(Cc1ccccc1)C(=O)O\n"+
    "NC(Cc1ccccc1)C(=O)O.NC(C)C(=O)O\n"+
    "N1CCCC1C(=O)O.NCC(=O)O\n"+
    "</PRE>\n"+
    "<PRE>\n"+
    "smirks:\n"+
    "[#9,#17,#35,#53:2][C:1]=O.[H:4][N:3][#6]>>[#6][N:3][C:1]=O\n"+
    "reactants:\n"+
    "COc1cccc(CC(Cl)=O)c1.CC(C)N\n"+
    "</PRE>\n"+
    "</P>\n"+
    "<P>\n"+
    "configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "author/support: Jeremy Yang\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    //CONFIG=conf;
    // read servlet parameters (from web.xml):
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    LOGDIR=conf.getInitParameter("LOGDIR");
    if (LOGDIR==null) LOGDIR="/tmp"+CONTEXTPATH+"_logs";
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
