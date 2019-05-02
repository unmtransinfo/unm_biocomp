package edu.unm.health.biocomp.descriptors;

import java.io.*;
import java.net.*; // URLEncoder, InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.*; // MultipartRequest, ParameterParser
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.sss.search.*;
import chemaxon.license.*;

import org.openscience.cdk.*; // CDK,DefaultChemObjectBuilder,AtomContainer,Molecule
import org.openscience.cdk.interfaces.*; //IAtomContainer
import org.openscience.cdk.smiles.*; //SmilesParser
import org.openscience.cdk.qsar.*; //DescriptorValue
import org.openscience.cdk.qsar.descriptors.molecular.*; //XLogPDescriptor
import org.openscience.cdk.exception.*; // CDKException, InvalidSmilesException

import edu.unm.health.biocomp.http.*;
import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.biobyte.*;
import edu.unm.health.biocomp.vcclab.*;

/**	Calculates LogP, octanol-water partition coefficients by various methods.

	@author Jeremy J Yang
*/
public class logp_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static String LOGDIR=null;
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static String BB_ROOT=null;	// configured in web.xml
  private static Boolean BB_OK=false;
  private static Integer N_MAX=100;	// configured in web.xml
  private static Integer MAX_POST_SIZE=null;	// configured in web.xml
  private static int scratch_retire_sec=3600;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static ArrayList<Molecule> mols=null;
  private static byte[] inbytes=null;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String datestr=null;
  private static File logfile=null;
  private static String PREFIX=null;
  private static String ofmt="";
  private static String color1="#EEEEEE";

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
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try
      {
        mrequest=new MultipartRequest(request,UPLOADDIR,MAX_POST_SIZE,"ISO-8859-1", new DefaultFileRenamePolicy());
      }
      catch (IOException lEx) {
        this.getServletContext().log("not a valid MultipartRequest",lEx);
      }
    }

    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList("biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList("/marvin/marvin.js","biocomp.js","ddtip.js"));
    boolean ok=false;
    ok=initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(APPNAME,jsincludes,cssincludes,JavaScript(),color1,request));
      out.print(HtmUtils.FooterHtm(errors,true));
      return;
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("logp").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(FormHtm(mrequest,response));
        Date t_i = new Date();
        try { Logp(mols); }
        catch (Exception e) { errors.add("Exception: "+e.getMessage()); }
        Logp_Results(mols,params,response);
        errors.add(SERVLETNAME+": elapsed time: "+time_utils.TimeDeltaStr(t_i,new Date()));
        out.print(HtmUtils.OutputHtm(outputs));
        out.print(HtmUtils.FooterHtm(errors,true));
        HtmUtils.PurgeScratchDirs(Arrays.asList(SCRATCHDIR),scratch_retire_sec,false,".",(HttpServlet) this);
      }
    }
    else
    {
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (request.getParameter("help")!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else if (request.getParameter("test")!=null)      // GET method, test=TRUE
      {
        response.setContentType("text/plain");
        out=response.getWriter();
        HashMap<String,String> t = new HashMap<String,String>();
        t.put("JCHEM_LICENSE_OK",(LicenseManager.isLicensed(LicenseManager.JCHEM)?"True":"False"));
        out.print(HtmUtils.TestTxt(APPNAME,t));
      }
      else if (downloadtxt!=null && downloadtxt.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadString(response,ostream,request.getParameter("downloadtxt"),
          request.getParameter("fname"));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadFile(response,ostream,downloadfile,
          request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME,jsincludes,cssincludes,JavaScript(),color1,request));
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
    mols=new ArrayList<Molecule>();
    Calendar calendar=Calendar.getInstance();

    String logo_htm="<TABLE CELLSPACING=\"5\" CELLPADDING=\"5\"><TR>";
    String imghtm=("<IMG BORDER=\"0\" SRC=\"/tomcat"+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=("<TD>"+HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white")+"</TD>");

    imghtm=("<IMG BORDER=\"0\" SRC=\"/tomcat"+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem and Marvin from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=("<TD>"+HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white")+"</TD>");

    imghtm=("<IMG BORDER=\"0\" HEIGHT=\"60\" SRC=\"/tomcat"+CONTEXTPATH+"/images/cdk_logo.png\">");
    tiphtm=("CDK");
    href=("http://sourceforge.net/projects/cdk/");
    logo_htm+=("<TD>"+HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white")+"</TD>");

    imghtm=("<IMG BORDER=\"0\" HEIGHT=\"70\" SRC=\"/tomcat"+CONTEXTPATH+"/images/biobyte_logo.png\">");
    tiphtm=("ClogP from BioByte, Inc.");
    href=("http://www.biobyte.com");
    logo_htm+=("<TD>"+HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white")+"</TD>");

    imghtm=("<IMG BORDER=\"0\" HEIGHT=\"70\" SRC=\"/tomcat"+CONTEXTPATH+"/images/vcclab_logo.png\">");
    tiphtm=("AlogPS from VCCLAB.org");
    href=("http://www.vcclab.org");
    logo_htm+=("<TD>"+HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white")+"</TD>");

    logo_htm+="</TR></TABLE>";
    errors.add(logo_htm);

    // Check status of BioByte/ClogP; error if not present or not licensed.
    if (BB_ROOT==null)
    {
      BB_OK=false;
      errors.add("ERROR: BioByte/ClogP not found.");
    }
    else
    { 
      try { 
        BB_OK=clogp_utils.checkLicense(BB_ROOT);
        if (!BB_OK) errors.add("ERROR: BioByte/ClogP license expired; disabled.");
      }
      catch (Exception e) {
        errors.add("ERROR: BioByte/ClogP license error; disabled.");
        BB_OK=false;
      }
    }

    inbytes=new byte[1024];

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
      logfile.createNewFile();
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

    Random rand = new Random();
    PREFIX=SERVLETNAME+"."+datestr+"."+String.format("%03d",rand.nextInt(1000));

    dout=new File(SCRATCHDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create SCRATCHDIR: "+SCRATCHDIR);
        return false;
      }
    }

    LicenseManager.refresh();
    if (!LicenseManager.isLicensed(LicenseManager.JCHEM))
    {
      errors.add("ERROR: ChemAxon license error; JCHEM required.");
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
      //errors.add("JChem version: "+chemaxon.jchem.version.VersionInfo.getVersion());
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
      errors.add("server: "+CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
      errors.add("servername: "+SERVERNAME);
    }

    String fname="infile";
    File file=mrequest.getFile(fname);
    String intxt=params.getVal("intxt").replaceFirst("[\\s]+$","");
    if (file!=null)
    {
      FileInputStream fis=new FileInputStream(file);
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
      inbytes=intxt.getBytes("utf-8");
    }
    else
    {
      errors.add("No input data.");
      return false;
    }

    // Here's where we should check inbytes for char
    // or binary content -- how?

    if (params.isChecked("file2txt"))
    {
      intxt=new String(inbytes,"utf-8");
      params.setVal("intxt",intxt);
    }

    if (file!=null) file.delete();

    MolImporter molReader=null;
    if (params.getVal("molfmt").equals("automatic"))
    {
      String orig_fname=mrequest.getOriginalFileName(fname);
      String ifmt_auto=MFileFormatUtil.getMostLikelyMolFormat(orig_fname);
      if (orig_fname!=null && ifmt_auto!=null)
      {
        molReader=new MolImporter(new ByteArrayInputStream(inbytes),ifmt_auto);
      }
      else
      {
        molReader=new MolImporter(new ByteArrayInputStream(inbytes));
      }
    }
    else
    {
      String ifmt=params.getVal("molfmt");
      molReader=new MolImporter(new ByteArrayInputStream(inbytes),ifmt);
    }
    // ofmt=molReader.getFormat();	// for output

    Molecule mol;
    int n_failed=0;
    while (true)
    {
      try {
        mol=molReader.read();
      }
      catch (MolFormatException e)
      {
        errors.add("ERROR: MolImporter failed: "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (mol==null) break;

      mols.add(mol);
      if (mols.size()==N_MAX)
      {
        errors.add("Warning: N_MAX mols: "+N_MAX);
        break;
      }
    }
    molReader.close();
    if (params.isChecked("verbose"))
    {
      String desc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
      errors.add("input format:  "+molReader.getFormat()+" ("+desc+")");
      errors.add("mols read:  "+mols.size());
    }
    if (n_failed>0) errors.add("ERRORS (unable to read mol): "+n_failed);

    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response)
  {
    String molfmt_menu="<SELECT NAME=\"molfmt\">\n";
    molfmt_menu+=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      String desc=MFileFormatUtil.getFormat(fmt).getDescription();
      molfmt_menu+=("<OPTION VALUE=\""+fmt+"\">"+desc+"\n");
    }
    molfmt_menu+=("</SELECT>");
    molfmt_menu=molfmt_menu.replace("\""+params.getVal("molfmt")+"\">",
				"\""+params.getVal("molfmt")+"\" SELECTED>");

    String outfmt_smi=""; String outfmt_sdf=""; 
    if (params.getVal("outfmt").equals("smi")) outfmt_smi="CHECKED";
    else if (params.getVal("outfmt").equals("sdf")) outfmt_sdf="CHECKED";

    String htm=
     ("<FORM NAME=\"mainform\" METHOD=POST")
    +(" ACTION=\""+response.encodeURL(SERVLETNAME)+"\"")
    +(" ENCTYPE=\"multipart/form-data\">\n")
    +("<TABLE WIDTH=\"100%\"><TR><TD><H2>"+APPNAME+"</H2></TD><TD>- compute LogP</TD>\n")
    +("<TD ALIGN=RIGHT>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
    +("</TD></TR></TABLE>\n")
    +("<HR>\n")
    +("<INPUT TYPE=HIDDEN NAME=\"logp\">\n")
    +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
    +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP>\n")
    +("<B>input:</B> format:"+molfmt_menu)
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:")
    +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxt")+"</TEXTAREA>\n")
    +("</TD>\n")
    +("<TD VALIGN=TOP>\n")
    +("<B>output:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"viewout\" VALUE=\"CHECKED\" "+params.getVal("viewout")+">view<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"batchout\" VALUE=\"CHECKED\" "+params.getVal("batchout")+">batch<BR>\n")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"smi\" "+outfmt_smi+">smiles (TSV)")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=CHECKBOX NAME=\"smiheader\" VALUE=\"CHECKED\" "+params.getVal("smiheader")+">+header<BR>\n")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"sdf\" "+outfmt_sdf+">sdf")
    +("<HR>\n")
    +("<B>general:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose&nbsp;")
    +("</TD></TR></TABLE>\n")
    +("<P>\n")
    +("<CENTER>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_logp(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
    +("</CENTER>\n")
    +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Logp(ArrayList<Molecule> mols)
	throws IOException,LicenseException,Exception
  {
    List<String> smis = new ArrayList<String>();
    for (Molecule mol: mols)
    {
      String smi=null;
      try { smi = MolExporter.exportToFormat(mol,"smiles:-a"); }
      catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }
      if (smi!=null) smis.add(smi);
    }

    List<String[]> clogp_results=null;

    clogp_results=clogp_utils.RunClogp(smis,BB_ROOT,SCRATCHDIR);

    List<AlogpsResult> alogps_results=null;
    String alogps_prog="?";
    try { alogps_results = vcclab_utils.GetAlogpsResults(smis); }
    catch (Exception e) { errors.add("ERROR: ALOGPS: "+e.getMessage()); }

    int i_mol=0;
    MolHandler mhand = new MolHandler();
    for (Molecule mol: mols)
    {
      String smi = smis.get(i_mol);
      mol.setProperty("mol_name",mol.getName());

      SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
      IAtomContainer molcdk = null;
      try { molcdk = sp.parseSmiles(smi); }
      catch (InvalidSmilesException e) { System.err.println(e.toString()); }

      String clogp="nan";
      if (clogp_results.size()>i_mol)
        clogp=clogp_results.get(i_mol)[0];
      try { float x=Float.parseFloat(clogp); }
      catch (Exception e) { errors.add("ERROR: error calculating ClogP for: "+mol.getName());  }
      mol.setProperty("clogp",clogp); //as string

      Float alogs=null;
      Float alogp=null;
      if (alogps_results.size()>i_mol)
      {
        AlogpsResult alogpsresult=alogps_results.get(i_mol);
        alogp=alogpsresult.logp;
        alogs=alogpsresult.logs;
        if (alogpsresult.error!=null)
          errors.add("ERROR: alogps: "+alogpsresult.error);
        if (alogpsresult.warning!=null && params.isChecked("verbose"))
          errors.add("Warning: alogps: "+alogpsresult.warning);
        alogps_prog=alogpsresult.program;
      }
      mol.setProperty("alogs",String.format("%.3f",alogs));
      mol.setProperty("alogp",String.format("%.3f",alogp));

      ++i_mol;
    }
    int n_mols=mols.size();
    errors.add("ALOGPS results via <A HREF=\"http://www.vcclab.org\">vcclab.org</A> web service: "+alogps_prog);

    PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(logfile,true)));
    out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,n_mols); 
    out_log.close();
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Logp_Results(ArrayList<Molecule> mols,HttpParams params, HttpServletResponse response) throws IOException
  {
    outputs.add("<B>RESULT:</B> mols processed: "+mols.size());
    int w_dep=140;
    int h_dep=100;
    String depopts=("mode=cow&imgfmt=png");
    depopts+=("&kekule=true");

    // This is our convention; Apache proxies the 8080 port via /tomcat.
    String smi2img_servleturl=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/mol2img");

    // ALOGS optional?
    ArrayList<String> dataFields = new ArrayList<String>(Arrays.asList("clogp","alogp","alogs"));

    // for download mols:
    MolExporter molWriter=null;
    File fout=null;
    if (params.isChecked("batchout"))
    {
      if (params.getVal("outfmt").equals("sdf"))
      {
        ofmt="sdf";
      }
      else if (params.getVal("outfmt").equals("smi"))
      {
        ofmt="smiles:u";
        ofmt+="-a";
        if (params.isChecked("smiheader"))
          ofmt+="Tmol_name:";
        else
          ofmt+="-T";
        for (String field:dataFields)
          ofmt+=(field+":");
      }
      try {
        File dout=new File(SCRATCHDIR);
        fout=File.createTempFile(PREFIX,"_out."+params.getVal("outfmt"),dout);
      }
      catch (IOException e) {
        errors.add("ERROR: could not open temp file; check SCRATCHDIR: "+SCRATCHDIR);
        return;
      }
      molWriter=new MolExporter(new FileOutputStream(fout),ofmt);
    }

    String thtm="";
    if (params.isChecked("viewout")) 
    {
      thtm+=("<TABLE BORDER>\n");
      thtm+=("<TR>\n");
      thtm+=("<TH>&nbsp;</TH>\n");
      for (String field:dataFields)
        thtm+=("<TH>"+field.replace("_"," ")+"</TH>\n");
      thtm+=("</TR>\n");
    }

    int i_mol=0;
    int N_MAX_VIEW=1000;
    for (Molecule mol: mols)
    {
      ++i_mol;
      String opts=depopts;
      String smi=null;
      try { smi = MolExporter.exportToFormat(mol,"smiles:-a"); }
      catch (Exception e) { errors.add("ERROR: "+e.getMessage()); }

      if (params.isChecked("viewout")) 
      {
        String rhtm="";
        rhtm+="<TR>";
        rhtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
        String imghtm=HtmUtils.Smi2ImgHtm(smi,opts,h_dep,w_dep,smi2img_servleturl,true,4,"go_zoom_smi2img");
        rhtm+=(imghtm+"<BR>\n");
        float clogp=0.0f;
        try {
          clogp=Float.parseFloat(mol.getProperty("clogp"));
          rhtm+=("<TD ALIGN=CENTER><TT>"+String.format("%.2f",clogp)+"</TT></TD>\n");
        }
        catch (Exception e) {
          rhtm+=("<TD ALIGN=CENTER><TT>err</TT></TD>\n");
          errors.add("ERROR: error calculating ClogP for: "+smi); 
        }
        float alogp=0.0f;
        try {
          alogp=Float.parseFloat(mol.getProperty("alogp"));
          rhtm+=("<TD ALIGN=CENTER><TT>"+String.format("%.2f",alogp)+"</TT></TD>\n");
        }
        catch (Exception e) {
          rhtm+=("<TD ALIGN=CENTER><TT>err</TT></TD>\n");
          errors.add("ERROR: error calculating AlogP for: "+smi);
        }
        float alogs=0.0f;
        try {
          alogs=Float.parseFloat(mol.getProperty("alogs"));
          rhtm+=("<TD ALIGN=CENTER><TT>"+String.format("%.2f",alogs)+"</TT></TD>\n");
        }
        catch (Exception e) {
          rhtm+=("<TD ALIGN=CENTER><TT>err</TT></TD>\n");
          errors.add("ERROR: error calculating AlogS.");
        }
        rhtm+="</TR>\n";
        if (i_mol<N_MAX_VIEW) { thtm+=rhtm; }
      }
      if (params.isChecked("batchout")) { molWriter.write(mol); }
    }
    int n_mols=mols.size();

    if (params.isChecked("viewout")) 
    {
      thtm+=("</TABLE>");
      outputs.add(thtm);
      if (i_mol>N_MAX_VIEW) 
        errors.add("NOTE: view truncated at N = "+N_MAX_VIEW);
    }
    if (params.isChecked("batchout"))
    {
      String fname=(SERVLETNAME+"_out."+params.getVal("outfmt"));
      long fsize = fout.length();
      String note="";
      if (params.getVal("outfmt").equals("smi"))
        note="<I>(Note: includes all data in TSV format.)</I>";
      else if (params.getVal("outfmt").equals("sdf"))
        note="<I>(Note: includes all data in SD format.)</I>";
      String bhtm=("&nbsp;"+
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\"><B>"+
      "Download "+fname+" ("+file_utils.NiceBytes(fsize)+")</B></BUTTON>"+note+"</FORM>");
      outputs.add("<BLOCKQUOTE>"+bhtm+"</BLOCKQUOTE>");
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
  {
    return(
"function go_reset(form)"+
"{\n"+
"  form.file2txt.checked=true;\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.molfmt.length;++i)\n"+
"    if (form.molfmt.options[i].value=='automatic')\n"+
"      form.molfmt.options[i].selected=true;\n"+
"  form.viewout.checked=true;\n"+
"  form.batchout.checked=true;\n"+
"  form.smiheader.checked=true;\n"+
"  form.verbose.checked=false;\n"+
"  for (i=0;i<form.outfmt.length;++i)\n"+ //radio
"    if (form.outfmt[i].value=='smi')\n"+
"      form.outfmt[i].checked=true;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return 0;\n"+
"  }\n"+
"  if (!form.batchout.checked && !form.viewout.checked) {\n"+
"    alert('ERROR: No output specified.');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_logp(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.logp.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    String htm=("<B>"+APPNAME+" Help</B><P>\n"+
    "<P>\n"+
    "Computes the following:\n"+
    "<P>\n"+
    "<UL>\n"+
    "<LI> clogp - BioByte ClogP\n"+
    "<LI> alogp - VCCLAB.ORG logP\n"+
    "<LI> alogs - VCCLAB.ORG logS\n"+
    "</UL>\n"+
    "<P>\n"+
    "The output can be downloaded in SDF or SMI format.\n"+
    "In SMI format, the data is appended in tab-separated fields.\n"+
    "<P>\n"+
    "Configured with <UL>\n"+
    "<LI> N_MAX = "+N_MAX+"\n"+
    "</UL>\n"+
    "<P>\n"+
    "Thanks to <A HREF=\"http://www.chemaxon.com\">ChemAxon</A> for the use of JChem in this application.\n"+
    "<P>\n"+
    "Thanks to <A HREF=\"http://www.biobyte.com\">BioByte</A> for the use of ClogP in this application.\n"+
    "<P>\n"+
    "Thanks to <A HREF=\"http://www.vcclab.org\">VCCLAB.ORG</A> for the use of ALOGPS in this application.\n"+
    "<P>\n"+
    "Author: Jeremy Yang\n"+
    "<P>\n"+
    "References:<UL>\n"+
    "<LI>Abraham MH, Acree WE, Leo AJ, Hoekman D, Cavanaugh JE, Water-solvent partition coefficients and Delta Log P values as predictors for blood-brain distribution; application of the Akaike information criterion, J Pharm Sci. 2010 May;99(5):2492-501.\n"+
    "<LI>Tetko, I. V.; Tanchuk, V. Y. Application of associative neural networks for prediction of lipophilicity in ALOGPS 2.1 program, J. Chem. Inf. Comput.  Sci., 2002, 42, 1136-45.\n"+
    "<LI>Tetko, I. V.; Tanchuk, V. Y.; Kasheva, T. N.; Villa, A. E. Estimation of aqueous solubility of chemical compounds using E-state indices, J. Chem. Inf.  Comput. Sci., 2001, 41, 1488-93.\n"+
    "</UL>\n"
    );
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    //CONFIG=conf;
    try { APPNAME=conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME=this.getServletName(); }
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter.");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR")+CONTEXTPATH;
    if (LOGDIR==null) LOGDIR="/usr/local/tomcat/logs"+CONTEXTPATH;
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    try { MAX_POST_SIZE=Integer.parseInt(conf.getInitParameter("MAX_POST_SIZE")); }
    catch (Exception e) { MAX_POST_SIZE=1*1024*1024; }
    BB_ROOT=conf.getInitParameter("BB_ROOT");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}
