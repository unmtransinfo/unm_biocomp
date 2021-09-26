/////////////////////////////////////////////////////////////////////////////
/// to do:
///    [ ] count mols first, get n_total
///    [ ] understand threads/object-synchronization!  Truncate hits!
/////////////////////////////////////////////////////////////////////////////
package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.concurrent.*; // ExecutorService, Executors
import java.lang.Math;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.struc.prop.MMoleculeProp;
import chemaxon.sss.search.*;
import chemaxon.license.*; //LicenseManager

import chemaxon.descriptors.CFParameters;
import chemaxon.descriptors.ChemicalFingerprint;
import chemaxon.descriptors.MDGeneratorException;

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.util.threads.*; //TaskUtils
import edu.unm.health.biocomp.smarts.*;

/**	Similarity searching and matrix computation by various methods.

	@author Jeremy Yang
*/
public class sim2d_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static int N_MAX=10000;	// configured in web.xml
  private static int N_MAX_MATRIX=50;	// configured in web.xml
  private static Integer MAX_POST_SIZE=null;	// configured in web.xml
  private static Boolean ENABLE_NOLIMIT=null;	// configured in web.xml
  private static String SCRATCHDIR=null;	// configured in web.xml
  private static String DATADIR=null;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static ArrayList<Molecule> molsQ=null;
  private static ArrayList<Molecule> molsDB=null;
  private static Molecule molQ=null;
  private static ArrayList<String> bitstrsQ=null; // for bitstring input
  private static ArrayList<String> bitstrsDB=null; // for bitstring input
  private static String bitstrQ=null; // for bitstring input
  private static ArrayList<String> namesQ=null; // for bitstring input
  private static ArrayList<String> namesDB=null; // for bitstring input
  private static String nameQ=null; // for bitstring input
  private static LinkedHashMap<String,Integer> depsizes_h=null;
  private static LinkedHashMap<String,Integer> depsizes_w=null;
  private static ArrayList<String> HEATCOLORS=null;
  //private static int serverport=0;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String DATESTR=null;
  private static String TMPFILE_PREFIX=null;
  private static String ofmt="";
  private static String MACCSFILE="mdl166.sma";
  private static String SUNSETFILE="sunsetkeys.sma";
  private static String PUBCHEMFILE="chemfp_pubchem.sma";
  private static SmartsFile maccssf=null;
  private static SmartsFile sunsetsf=null;
  private static SmartsFile pubchemsf=null;
  private static int scratch_retire_sec=3600;
  private static String color1="#EEEEEE";
  private static String PROGRESS_WIN_NAME=null;
  private static Float alpha=null;
  private static Float beta=null;
  private static Integer pathfplen=null;
  private static Integer pathbcount=null;
  private static Integer pathbitsper=null;
  private static Integer ecfpdiam=null;
  private static Integer ecfplen=null;
  private static Integer ECFPDIAM_DEFAULT=4;
  private static Integer ECFPLEN_DEFAULT=1024;
  private static Integer n_max_hits=null;
  private static Float sim_min=null;
  private static Integer arom=null;
  private static MolImporter molReaderQ=null;
  private static MolImporter molReaderDB=null;
  private static BufferedReader buffReaderQ=null; //for bitstrs
  private static BufferedReader buffReaderDB=null; //for bitstrs
  private static boolean CA_ECFP_IS_LICENSED=false; //ChemAxon license flag (not in FreeWeb-2015).
  private static String SMI2IMG_SERVLETURL=null;
  private static String PROXY_PREFIX=null;	// configured in web.xml

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    SERVERNAME = request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME = InetAddress.getLocalHost().getHostAddress();
    REMOTEHOST = request.getHeader("X-Forwarded-For"); // client (original)
    if (REMOTEHOST!=null)
    {
      String[] addrs = Pattern.compile(",").split(REMOTEHOST);
      if (addrs.length>0) REMOTEHOST = addrs[addrs.length-1];
    }
    else
    {
      REMOTEHOST = request.getRemoteAddr(); // client (may be proxy)
    }
    rb = ResourceBundle.getBundle("LocalStrings",request.getLocale());
    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest = new MultipartRequest(request,UPLOADDIR,MAX_POST_SIZE,"ISO-8859-1",new DefaultFileRenamePolicy()); }
      catch (IOException e) { CONTEXT.log("not a valid MultipartRequest",e); }
    }

    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/css/biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/js/biocomp.js",PROXY_PREFIX+CONTEXTPATH+"/js/ddtip.js"));
    boolean ok = initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out = response.getWriter();
      out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
      out.print(HtmUtils.FooterHtm(errors,true));
      return;
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE"))
      {
        response.setContentType("text/html");
        out = response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response,params.getVal("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,'"+params.getVal("formmode")+"',true)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
      }
      else if (mrequest.getParameter("sim2d").equalsIgnoreCase("TRUE"))
      {
        //response.setBufferSize(0); //call before any content written; if content written or object committed, IllegalStateException. 
        response.setContentType("text/html");
        out = response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response,params.getVal("formmode")));
        out.flush();
        response.flushBuffer();
        Date t_i = new Date();
        if (params.getVal("mode").equals("NxN"))
        {
          float [][] simatrix=null;
          if (params.getVal("molfmtDB").equals("bitstr"))
          {
            simatrix = Sim2d_NxN_Bitstr_LaunchThread(mrequest,response,params);
            Sim2d_Matrix_Bitstr_Results(simatrix,params,response);
          }
          else
          {
            simatrix = Sim2d_NxN_LaunchThread(mrequest,response,params);
            Sim2d_Matrix_Results(simatrix,params,response);
          }
        }
        else if (params.getVal("mode").equals("QxN"))
        {
          float [][] simatrix=null;
          if (params.getVal("molfmtDB").equals("bitstr"))
          {
            simatrix = Sim2d_QxN_Bitstr_LaunchThread(mrequest,response,params);
            Sim2d_Matrix_Bitstr_Results(simatrix,params,response);
          }
          else
          {
            simatrix = Sim2d_QxN_LaunchThread(mrequest,response,params);
            Sim2d_Matrix_Results(simatrix,params,response);
          }
        }
        else
        {
          Vector<Sim2DHit> hits=null;
          if (params.getVal("molfmtDB").equals("bitstr"))
          {
            hits = Sim2d_1xN_Bitstr_LaunchThread(mrequest,response,params);
            Sim2d_1xN_Bitstr_Results(hits,params,response);
          }
          else
          {
            hits = Sim2d_1xN_LaunchThread(mrequest,response,params);
            if (hits==null)
              errors.add("DEBUG: hits==null (from Sim2d_1xN_LaunchThread)");
            else
              Sim2d_1xN_Results(hits,params,response);
          }
        }
        out.println("<SCRIPT>pwin.parent.focus(); pwin.focus(); pwin.close();</SCRIPT>");
        Date t_f = new Date();
        long t_d=t_f.getTime()-t_i.getTime();
        int t_d_min = (int)(t_d/60000L);
        int t_d_sec = (int)((t_d/1000L)%60L);
        errors.add(SERVLETNAME+": elapsed time: "+t_d_min+"m "+t_d_sec+"s");
        out.print(HtmUtils.OutputHtm(outputs));
        out.print(HtmUtils.FooterHtm(errors,true));
      }
    }
    else
    {
      String downloadtxt = request.getParameter("downloadtxt"); // POST param
      String downloadfile = request.getParameter("downloadfile"); // POST param
      if (request.getParameter("help")!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out = response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.print(HelpHtm());
        out.print(HtmUtils.FooterHtm(errors,true));
      }
      else if (request.getParameter("test")!=null)	// GET method, test=TRUE
      {
        response.setContentType("text/plain");
        out = response.getWriter();
        HashMap<String,String> t = new HashMap<String,String>();
        t.put("JCHEM_LICENSE_OK", (LicenseManager.isLicensed(LicenseManager.JCHEM)?"True":"False"));
        out.print(HtmUtils.TestTxt(APPNAME,t));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream = response.getOutputStream();
        HtmUtils.DownloadFile(response,ostream,downloadfile,request.getParameter("fname"));
      }
      else if (downloadtxt!=null && downloadtxt.length()>0) // POST param
      {
        ServletOutputStream ostream = response.getOutputStream();
        HtmUtils.DownloadString(response,ostream,downloadtxt,request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet
      {
        response.setContentType("text/html");
        out = response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response,request.getParameter("formmode")));
        out.println("<SCRIPT>go_init(window.document.mainform,'"+request.getParameter("formmode")+"',false)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
      }
    }
    HtmUtils.PurgeScratchDirs(Arrays.asList(SCRATCHDIR),scratch_retire_sec,false,".",(HttpServlet) this);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String Val2HeatColor(float val,float minval,float maxval)
  {
    int i=0;
    if (val<=minval)
    {
      i=0;
    }
    else if (val>=maxval)
    {
      i = HEATCOLORS.size()-1;
    }
    else
    {
      float delta = maxval-minval;
      i = (int) Math.floor(((val-minval)/delta)*(float)HEATCOLORS.size());
      i = Math.min(i,HEATCOLORS.size()-1);
    }
    return HEATCOLORS.get(i);
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request,MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME = this.getServletName();
    PROGRESS_WIN_NAME = (SERVLETNAME+"_progress_win");
    params = new HttpParams();
    outputs = new ArrayList<String>();
    errors = new ArrayList<String>();
    SMI2IMG_SERVLETURL = (PROXY_PREFIX+CONTEXTPATH+"/mol2img");

    String logo_htm = "<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm = ("<IMG BORDER=\"0\" SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm = (APPNAME+" web app from UNM Translational Informatics.");
    String href = ("https://datascience.unm.edu/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm = ("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm = ("JChem and Marvin from ChemAxon Ltd.");
    href = ("https://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm = ("<IMG BORDER=\"0\" HEIGHT=\"60\" SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/cdk_logo.png\">");
    tiphtm=("CDK");
    href = ("https://sourceforge.net/projects/cdk/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    depsizes_h = new LinkedHashMap<String,Integer>();
    depsizes_w = new LinkedHashMap<String,Integer>();
    depsizes_h.put("xs",64); depsizes_w.put("xs",64);
    depsizes_h.put("s",96); depsizes_w.put("s",96);
    depsizes_h.put("m",120); depsizes_w.put("m",140);
    depsizes_h.put("l",160); depsizes_w.put("l",200);
    depsizes_h.put("xl",220); depsizes_w.put("xl",280);

    HEATCOLORS = new ArrayList<String>();
    HEATCOLORS.add("333333");
    HEATCOLORS.add("444444");
    HEATCOLORS.add("555555");
    HEATCOLORS.add("774444");
    HEATCOLORS.add("993333");
    HEATCOLORS.add("BB2222");
    HEATCOLORS.add("CC2222");
    HEATCOLORS.add("DD1111");
    HEATCOLORS.add("E61111");
    HEATCOLORS.add("EE1111");
    HEATCOLORS.add("F01111");
    HEATCOLORS.add("FF1111");
    HEATCOLORS.add("FF0000");

    Calendar calendar=Calendar.getInstance();
    calendar.setTime(new Date());
    DATESTR = String.format("%04d%02d%02d%02d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
    Random rand = new Random();
    TMPFILE_PREFIX = SERVLETNAME+"."+DATESTR+"."+String.format("%03d",rand.nextInt(1000));

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
    if (!LicenseManager.isLicensed(LicenseManager.JCHEM))
    {
      errors.add("ERROR: ChemAxon license error; JCHEM required.");
      return false;
    }
    if (!LicenseManager.isLicensed(LicenseManager.ECFP_FCFP))
    {
      errors.add("Warning: ChemAxon ECFP/FCFP unlicensed ; ChemAxon ECFP disabled.");
      CA_ECFP_IS_LICENSED=false;
    }

    if (mrequest==null) { return true; }

    for (Enumeration e = mrequest.getParameterNames(); e.hasMoreElements(); )
    {
      String key = (String)e.nextElement();
      if (mrequest.getParameter(key)!=null)
        params.setVal(key,mrequest.getParameter(key));
    }

    if (params.isChecked("verbose"))
    {
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
      errors.add("server: "+CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
    }

    if (mrequest.getParameter("changemode").equalsIgnoreCase("TRUE")) { return true; }

    /// Stuff for a run:

    if (params.getVal("arom").equals("gen"))
      arom = MoleculeGraph.AROM_GENERAL;
    else if (params.getVal("arom").equals("bas"))
      arom = MoleculeGraph.AROM_BASIC;
    else if (params.getVal("arom").equals("none"))
      arom=null;

    if (params.isChecked("nolimit"))
    {
      N_MAX=0;
      N_MAX_MATRIX=0;
      MAX_POST_SIZE = Integer.MAX_VALUE;
    }

    String fnameQ = "infileQ";
    File fileQ = mrequest.getFile(fnameQ);
    String intxtQ = params.getVal("intxtQ").replaceFirst("[\\s]+$","");
    String fname = "infile";
    File fileDB = mrequest.getFile(fname);
    String intxtDB = params.getVal("intxt").replaceFirst("[\\s]+$","");
    String line = null;

    if (fileQ!=null)
    {
      if (params.isChecked("file2txtQ"))
      {
        BufferedReader br = new BufferedReader(new FileReader(fileQ));
        intxtQ="";
        for (int i=0;(line=br.readLine())!=null;++i)
        {
          intxtQ+=(line+"\n");
          if (i==5000)
          {
            errors.add("ERROR: max lines copied to input: "+5000);
            break;
          }
        }
        params.setVal("intxtQ",intxtQ);
      }
      else 
      {
        params.setVal("intxtQ","");
      }
    }
    if (fileDB!=null)
    {
      if (params.isChecked("file2txt") && fileDB!=null)
      {
        BufferedReader br = new BufferedReader(new FileReader(fileDB));
        intxtDB="";
        for (int i=0;(line=br.readLine())!=null;++i)
        {
          intxtDB+=(line+"\n");
          if (i==5000)
          {
            errors.add("ERROR: max lines copied to input: "+5000);
            break;
          }
        }
        params.setVal("intxt",intxtDB);
      }
      else
      {
        params.setVal("intxt","");
      }
    }
    if (params.getVal("mode").equals("1xN") || params.getVal("mode").equals("QxN"))
    {
      if (params.getVal("molfmtQ").equals("bitstr"))
      {
        if (params.getVal("mode").equals("1xN"))
        {
          bitstrQ = new String(intxtQ);
          bitstrQ = bitstrQ.trim().replaceFirst("\\s.*$","");
          nameQ = new String(intxtQ);
          if (nameQ.matches("^\\S+\\s.*$"))
            nameQ = nameQ.trim().replaceFirst("^\\S+\\s","");
          else
            nameQ = "(unnamed query)";
          if (params.isChecked("verbose"))
            errors.add("FP Bitstring query; length: "+bitstrQ.length());
        }
      }
      else
      {
        // Read query mol:
        if (params.getVal("molfmtQ").equals("automatic"))
        {
          String ifmt_auto = MFileFormatUtil.getMostLikelyMolFormat(mrequest.getOriginalFileName(fnameQ));
          if (ifmt_auto!=null)
          {
            if (fileQ!=null)
              molReaderQ = new MolImporter(new FileInputStream(fileQ),ifmt_auto);
            else if (intxtQ.length()>0)
              molReaderQ = new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()),ifmt_auto);
          }
          else
          {
            if (fileQ!=null)
              molReaderQ = new MolImporter(new FileInputStream(fileQ));
            else if (intxtQ.length()>0)
              molReaderQ = new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()));
          }
        }
        else
        {
          String ifmt = params.getVal("molfmtQ");
          molReaderQ = new MolImporter(fileQ,ifmt);
        }
        if (params.getVal("mode").equals("1xN"))
        {
          molQ = molReaderQ.read();
          if (arom!=null)
            molQ.aromatize(arom);
          else
            molQ.dearomatize();
          molReaderQ.close();
        }
      }
    }
    if (params.getVal("mode").equals("QxN"))
    {
      molsQ = new ArrayList<Molecule>();
      bitstrsQ = new ArrayList<String>();
      namesQ = new ArrayList<String>();
    }
    molsDB = new ArrayList<Molecule>();
    bitstrsDB = new ArrayList<String>();
    namesDB = new ArrayList<String>();

    if (params.getVal("molfmtDB").equals("bitstr"))
    {
      // NxN: read/store database bitstrs:
      // 1xN: pass buffReader for memory savings.
      if (fileDB!=null)
        buffReaderDB = new BufferedReader(new FileReader(fileDB));
      else
        buffReaderDB = new BufferedReader(new StringReader(intxtDB));
      if (params.getVal("mode").equals("NxN") || params.getVal("mode").equals("QxN"))
      {
        for (int i=0;(line=buffReaderDB.readLine())!=null;++i)
        {
          line=line.trim();
          String bitstr = line.replaceFirst("\\s.*$","");
          bitstrsDB.add(bitstr);
          if (params.isChecked("verbose"))
            errors.add("FP Bitstring "+(i+1)+". length: "+bitstr.length());
          String name;
          if (line.matches("^\\S+\\s.*$"))
            name = line.replaceFirst("^\\S+\\s","");
          else
            name = String.format("%d",(i+1));
          namesDB.add(name);
        }
      }
      if (params.getVal("mode").equals("QxN"))
      {
        if (fileQ!=null)
          buffReaderQ = new BufferedReader(new FileReader(fileQ));
        else
          buffReaderQ = new BufferedReader(new StringReader(intxtQ));
        for (int i=0;(line=buffReaderQ.readLine())!=null;++i)
        {
          line = line.trim();
          String bitstr = line.replaceFirst("\\s.*$","");
          bitstrsQ.add(bitstr);
          if (params.isChecked("verbose"))
            errors.add("FP Bitstring "+(i+1)+". length: "+bitstr.length());
          String name;
          if (line.matches("^\\S+\\s.*$"))
            name = line.replaceFirst("^\\S+\\s","");
          else
            name = String.format("%d",(i+1));
          namesQ.add(name);
        }
      }
    }
    else
    {
      // NxN: read/store database mols:
      // QxN: read/store query and database mols:
      // 1xN: pass molReader for memory savings.
      if (params.getVal("molfmtDB").equals("automatic"))
      {
        String ifmt_auto = MFileFormatUtil.getMostLikelyMolFormat(mrequest.getOriginalFileName(fname));
        if (ifmt_auto!=null)
        {
          if (fileDB!=null)
            molReaderDB = new MolImporter(fileDB,ifmt_auto);
          else if (intxtDB.length()>0)
            molReaderDB = new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()),ifmt_auto);
        }
        else
        {
          if (fileDB!=null)
            molReaderDB = new MolImporter(new FileInputStream(fileDB));
          else if (intxtDB.length()>0)
            molReaderDB = new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()));
        }
      }
      else
      {
        molReaderDB = new MolImporter(new FileInputStream(fileDB),params.getVal("molfmtDB"));
      }
      if (params.isChecked("verbose"))
        errors.add("input DB format:  "+molReaderDB.getFormat()+" ("+MFileFormatUtil.getFormat(molReaderDB.getFormat()).getDescription()+")");
      if (params.getVal("mode").equals("QxN"))
      {
        if (params.getVal("molfmtQ").equals("automatic"))
        {
          String ifmt_auto = MFileFormatUtil.getMostLikelyMolFormat(mrequest.getOriginalFileName(fname));
          if (ifmt_auto!=null)
          {
            if (fileQ!=null)
              molReaderQ = new MolImporter(fileQ,ifmt_auto);
            else if (intxtQ.length()>0)
              molReaderQ = new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()),ifmt_auto);
          }
          else
          {
            if (fileQ!=null)
              molReaderQ = new MolImporter(new FileInputStream(fileQ));
            else if (intxtQ.length()>0)
              molReaderQ = new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()));
          }
        }
        else
        {
          molReaderQ = new MolImporter(new FileInputStream(fileQ),params.getVal("molfmtQ"));
        }
      }
      if (params.isChecked("verbose") && molReaderQ!=null)
        errors.add("input query format:  "+molReaderQ.getFormat()+" ("+MFileFormatUtil.getFormat(molReaderQ.getFormat()).getDescription()+")");
      if (params.getVal("mode").equals("QxN"))
      {
        Molecule m;
        int n_failed=0;
        while (true)
        {
          try {
            m = molReaderQ.read();
          }
          catch (MolFormatException e)
          {
            ++n_failed;
            errors.add("ERROR: ["+n_failed+"]: "+e.getMessage());
            continue;
          }
          if (m==null) break;
    
          if (N_MAX_MATRIX>0 && molsQ.size()==N_MAX_MATRIX)
          {
            outputs.add("Warning: mol list truncated at N_MAX_MATRIX: "+N_MAX_MATRIX);
            errors.add("Warning: mol list truncated at N_MAX_MATRIX: "+N_MAX_MATRIX);
            break;
          }
          if (arom!=null)
            m.aromatize(arom);
          else
            m.dearomatize();
          molsQ.add(m);
        }
        molReaderQ.close();
        if (params.isChecked("verbose"))
        {
          errors.add("query mols read:  "+molsQ.size());
        }
      }
      if (params.getVal("mode").equals("NxN") || params.getVal("mode").equals("QxN"))
      {
        Molecule m;
        int n_failed=0;
        while (true)
        {
          try {
            m = molReaderDB.read();
          }
          catch (MolFormatException e)
          {
            ++n_failed;
            errors.add("ERROR: ["+n_failed+"]: "+e.getMessage());
            continue;
          }
          if (m==null) break;
    
          if (N_MAX_MATRIX>0 && molsDB.size()==N_MAX_MATRIX)
          {
            outputs.add("Warning: mol list truncated at N_MAX_MATRIX: "+N_MAX_MATRIX);
            errors.add("Warning: mol list truncated at N_MAX_MATRIX: "+N_MAX_MATRIX);
            break;
          }
          if (arom!=null)
            m.aromatize(arom);
          else
            m.dearomatize();
          molsDB.add(m);
        }
        molReaderDB.close();
        if (params.isChecked("verbose"))
        {
          errors.add("DB mols read:  "+molsDB.size());
        }
      }
    }
    if (params.getVal("mode").equals("1xN"))
    {
      try { sim_min = Float.parseFloat(params.getVal("sim_min")); }
      catch (NumberFormatException e) {
        sim_min=0.0f;
        errors.add("ERROR: cannot parse sim_min; using default:"+sim_min);
      }
      try { n_max_hits = Integer.parseInt(params.getVal("n_max_hits")); }
      catch (NumberFormatException e) {
        n_max_hits=400;
        errors.add("ERROR: cannot parse n_max_hits; using default: "+n_max_hits);
      }
    }

    if (params.getVal("metric").equals("tversky"))
    {
      try { alpha = Float.parseFloat(params.getVal("alpha")); }
      catch (NumberFormatException e) {
        errors.add("ERROR: cannot parse alpha.");
        return false;
      }
      try { beta = Float.parseFloat(params.getVal("beta")); }
      catch (NumberFormatException e) {
        errors.add("ERROR: cannot parse beta.");
        return false;
      }
    }
    if (params.getVal("fptype").equals("ecfp"))
    {
      try { ecfpdiam = Integer.parseInt(params.getVal("ecfpdiam")); }
      catch (NumberFormatException e) {
        ecfpdiam=ECFPDIAM_DEFAULT;
        errors.add("ERROR: cannot parse ecfpdiam; using default: "+ecfpdiam);
      }
      try { ecfplen = Integer.parseInt(params.getVal("ecfplen")); }
      catch (NumberFormatException e) {
        ecfplen=ECFPLEN_DEFAULT;
        errors.add("ERROR: cannot parse ecfplen; using default: "+ecfplen);
      }
    }
    else if (params.getVal("fptype").equals("path"))
    {
      try { pathfplen = Integer.parseInt(params.getVal("pathfplen")); }
      catch (NumberFormatException e) {
        pathfplen = CFParameters.DEFAULT_LENGTH;
        errors.add("ERROR: cannot parse pathfplength; using default: "+pathfplen);
      }
      try { pathbcount = Integer.parseInt(params.getVal("pathbcount")); }
      catch (NumberFormatException e) {
        pathbcount = CFParameters.DEFAULT_BOND_COUNT;
        errors.add("ERROR: cannot parse fpbondcount; using default: "+pathbcount);
      }
      try { pathbitsper = Integer.parseInt(params.getVal("pathbitsper")); }
      catch (NumberFormatException e) {
        pathbitsper = CFParameters.DEFAULT_BITS_SET;
        errors.add("ERROR: cannot parse pathbitsper; using default: "+pathbitsper);
      }
    }
    //if (fileQ!=null) fileQ.delete();
    //if (fileDB!=null) fileDB.delete();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response,String formmode)
      throws IOException,ServletException
  {
    if (formmode==null) formmode="normal";
    String formmode_normal=""; String formmode_expert="";
    if (formmode.equals("expert")) formmode_expert="CHECKED";
    else if (formmode.equals("normal")) formmode_normal="CHECKED";
    else formmode_normal="CHECKED";

    String molfmt_menuQ = "<SELECT NAME=\"molfmtQ\">\n";
    molfmt_menuQ+=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      String desc = MFileFormatUtil.getFormat(fmt).getDescription();
      molfmt_menuQ+=("<OPTION VALUE=\""+fmt+"\">"+desc+"\n");
    }
    if (formmode.equals("expert"))
      molfmt_menuQ+=("<OPTION VALUE=\"bitstr\">FP Bitstring\n");
    molfmt_menuQ+=("</SELECT>");
    molfmt_menuQ = molfmt_menuQ.replace("\""+params.getVal("molfmtQ")+"\">",
				"\""+params.getVal("molfmtQ")+"\" SELECTED>");

    String molfmt_menuDB = "<SELECT NAME=\"molfmtDB\">\n";
    molfmt_menuDB+=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      String desc = MFileFormatUtil.getFormat(fmt).getDescription();
      molfmt_menuDB+=("<OPTION VALUE=\""+fmt+"\">"+desc+"\n");
    }
    if (formmode.equals("expert"))
      molfmt_menuDB+=("<OPTION VALUE=\"bitstr\">FP Bitstring\n");
    molfmt_menuDB+=("</SELECT>");
    molfmt_menuDB = molfmt_menuDB.replace("\""+params.getVal("molfmtDB")+"\">",
				"\""+params.getVal("molfmtDB")+"\" SELECTED>");

    String depsize_menu = "<SELECT NAME=\"depsize\">\n";
    for (String key:depsizes_h.keySet())
    {
      depsize_menu+=("<OPTION VALUE=\""+key+"\">"+key+" - ");
      depsize_menu+=(depsizes_h.get(key)+"x"+depsizes_w.get(key)+"\n");
    }
    depsize_menu+="</SELECT>\n";
    depsize_menu = depsize_menu.replace("\""+params.getVal("depsize")+"\">","\""+params.getVal("depsize")+"\" SELECTED>");

    int[] pathfplens = {512,1024,2048,4096};
    String pathfplen_menu = "<SELECT NAME=\"pathfplen\">\n";
    for (int i:pathfplens) pathfplen_menu+=("<OPTION VALUE=\""+i+"\">"+i);
    pathfplen_menu+="</SELECT>\n";
    pathfplen_menu = pathfplen_menu.replace("\""+params.getVal("pathfplen")+"\">","\""+params.getVal("pathfplen")+"\" SELECTED>");

    int[] pathbcounts = {5,6,7,8,9};
    String pathbcount_menu = "<SELECT NAME=\"pathbcount\">\n";
    for (int i:pathbcounts) pathbcount_menu+=("<OPTION VALUE=\""+i+"\">"+i);
    pathbcount_menu+="</SELECT>\n";
    pathbcount_menu = pathbcount_menu.replace("\""+params.getVal("pathbcount")+"\">","\""+params.getVal("pathbcount")+"\" SELECTED>");

    int[] pathbitspers = {1,2,3};
    String pathbitsper_menu = "<SELECT NAME=\"pathbitsper\">\n";
    for (int i:pathbitspers) pathbitsper_menu+=("<OPTION VALUE=\""+i+"\">"+i);
    pathbitsper_menu+="</SELECT>\n";
    pathbitsper_menu = pathbitsper_menu.replace("\""+params.getVal("pathbitsper")+"\">","\""+params.getVal("pathbitsper")+"\" SELECTED>");

    int[] ecfpdiams = {2,4,6};
    String ecfpdiam_menu = "<SELECT NAME=\"ecfpdiam\">\n";
    for (int i:ecfpdiams) ecfpdiam_menu+=("<OPTION VALUE=\""+i+"\">"+i);
    ecfpdiam_menu+="</SELECT>\n";
    ecfpdiam_menu = ecfpdiam_menu.replace("\""+params.getVal("ecfpdiam")+"\">","\""+params.getVal("ecfpdiam")+"\" SELECTED>");

    int[] ecfplens = {512,1024,2048,4096};
    String ecfplen_menu = "<SELECT NAME=\"ecfplen\">\n";
    for (int i:ecfplens) ecfplen_menu+=("<OPTION VALUE=\""+i+"\">"+i);
    ecfplen_menu+="</SELECT>\n";
    ecfplen_menu = ecfplen_menu.replace("\""+params.getVal("ecfplen")+"\">","\""+params.getVal("ecfplen")+"\" SELECTED>");

    String arom_gen=""; String arom_bas=""; String arom_none="";
    if (params.getVal("arom").equals("gen")) arom_gen="CHECKED";
    else if (params.getVal("arom").equals("bas")) arom_bas="CHECKED";
    else arom_none="CHECKED";

    String mode_1xN=""; String mode_NxN=""; String mode_QxN="";
    if (params.getVal("mode").equals("NxN")) mode_NxN="CHECKED";
    else if (params.getVal("mode").equals("QxN")) mode_QxN="CHECKED";
    else if (params.getVal("mode").equals("1xN")) mode_1xN="CHECKED";
    else mode_1xN="CHECKED";

    String fptype_ecfp=""; String fptype_sunset=""; String fptype_pubchem="";
    String fptype_maccs=""; String fptype_path="";
    String fptype_all="";
    if (params.getVal("fptype").equals("sunset")) fptype_sunset="CHECKED";
    else if (params.getVal("fptype").equals("ecfp")) fptype_ecfp="CHECKED";
    else if (params.getVal("fptype").equals("path")) fptype_path="CHECKED";
    else if (params.getVal("fptype").equals("maccs")) fptype_maccs="CHECKED";
    else if (params.getVal("fptype").equals("pubchem")) fptype_pubchem="CHECKED";
    else if (params.getVal("fptype").equals("all")) fptype_all="CHECKED";
    else fptype_sunset="CHECKED";

    String metric_tanimoto=""; String metric_tversky="";
    if (params.getVal("metric").equals("tversky")) metric_tversky="CHECKED";
    else if (params.getVal("metric").equals("tanimoto")) metric_tanimoto="CHECKED";
    else metric_tanimoto="CHECKED";

    String outhtm=(
    "<FORM NAME=\"mainform\" METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\" ENCTYPE=\"multipart/form-data\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"sim2d\">\n"
    +"<INPUT TYPE=HIDDEN NAME=\"changemode\">\n"
    +"<TABLE WIDTH=\"100%\"><TR>\n"
    +("<TD WIDTH=\"15%\"><H2>"+APPNAME+"</H2></TD>\n")
    +"<TD>- 2D fingerprint-based similarity\n"
    +"</TD>\n"
    +"<TD ALIGN=RIGHT>\n"
    +"<B>mode:</B>\n"
    +("<INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"normal\" onClick=\"go_changemode(document.mainform)\" "+formmode_normal+">normal\n")
    +("<INPUT TYPE=RADIO NAME=\"formmode\" VALUE=\"expert\" onClick=\"go_changemode(document.mainform)\" "+formmode_expert+">expert\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_demo(this.form, 'normal')\"><B>Demo</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"?formmode="+formmode+"')\"><B>Reset</B></BUTTON>\n")
    +"</TD></TR></TABLE>\n"
    +"<HR>\n"
    +"<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n"
    +"<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP WIDTH=\"50%\">\n"
    +("<B>query[s]:</B> format:"+molfmt_menuQ+"\n")
    +"<INPUT TYPE=CHECKBOX NAME=\"file2txtQ\" VALUE=\"CHECKED\" "+params.getVal("file2txtQ")+">file2txt<BR>\n")
    +"upload: <INPUT TYPE=\"FILE\" NAME=\"infileQ\"> ...or paste:\n"
    +("<BR><TEXTAREA NAME=\"intxtQ\" WRAP=OFF ROWS=8 COLS=60>"+params.getVal("intxtQ")+"</TEXTAREA>\n")
    +"<BR>\n"
    +("<B>DB mols:</B> format:"+molfmt_menuDB+"\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +"upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:\n"
    +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxt")+"</TEXTAREA>\n")
    +"</TD>\n"
    +"<TD VALIGN=TOP>\n"
    +"<B>mode:</B><BR>\n"
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"mode\" VALUE=\"1xN\" "+mode_1xN+">search (1XN)\n")
    +("maxhits:<INPUT TYPE=TEXT SIZE=4 NAME=\"n_max_hits\" VALUE=\""+params.getVal("n_max_hits")+"\">\n")
    +("sim_min:<INPUT TYPE=TEXT SIZE=4 NAME=\"sim_min\" VALUE=\""+params.getVal("sim_min")+"\">\n");
    if (formmode.equals("expert"))
      outhtm+=("<INPUT TYPE=CHECKBOX NAME=\"sorthits\" VALUE=\"CHECKED\" "+params.getVal("sorthits")+">sort hits<BR>");
    else
      outhtm+=("<INPUT TYPE=HIDDEN NAME=\"sorthits\" VALUE=\"TRUE\">\n<BR>\n");
    outhtm+=(
      ("&nbsp;<INPUT TYPE=RADIO NAME=\"mode\" VALUE=\"QxN\" "+mode_QxN+">matrix (QxN)<BR>\n")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"mode\" VALUE=\"NxN\" "+mode_NxN+">matrix (NxN)\n")
      +("<HR>\n")
      +("<B>fingerprint method:</B><BR>\n"));

    outhtm+=("&nbsp;<INPUT TYPE=RADIO NAME=\"fptype\" VALUE=\"ecfp\" "+fptype_ecfp+">ECFP\n");
    if (formmode.equals("expert"))
      outhtm+=("fplen:"+ecfplen_menu+"\ndiameter:"+ecfpdiam_menu+"\n");
    else
    {
      outhtm+=("<INPUT TYPE=HIDDEN NAME=\"ecfpdiam\" VALUE=\""+ecfpdiam+"\">\n");
      outhtm+=("<INPUT TYPE=HIDDEN NAME=\"ecfplen\" VALUE=\""+ecfplen+"\">\n");
    }
    outhtm+=("<BR>\n");
    outhtm+=("&nbsp;<INPUT TYPE=RADIO NAME=\"fptype\" VALUE=\"maccs\" "+fptype_maccs+">MACCS<BR>\n");
    outhtm+=("&nbsp;<INPUT TYPE=RADIO NAME=\"fptype\" VALUE=\"sunset\" "+fptype_sunset+">Sunset<BR>\n");
    if (formmode.equals("expert"))
    {
      outhtm+=(
        ("&nbsp;<INPUT TYPE=RADIO NAME=\"fptype\" VALUE=\"pubchem\" "+fptype_pubchem+">PubChem<BR>\n")
        +("&nbsp;<INPUT TYPE=RADIO NAME=\"fptype\" VALUE=\"path\" "+fptype_path+">JChemPath\n")
        +("fplen:"+pathfplen_menu+"\n")
        +("pathbonds:"+pathbcount_menu+"\n")
        +("pathbits:"+pathbitsper_menu+"<BR>\n")
        +("<HR>\n")
        +("<B>metric:</B><BR>\n")
        +("&nbsp;<INPUT TYPE=RADIO NAME=\"metric\" VALUE=\"tanimoto\" "+metric_tanimoto+">Tanimoto<BR>\n")
        +("&nbsp;<INPUT TYPE=RADIO NAME=\"metric\" VALUE=\"tversky\" "+metric_tversky+">Tversky\n")
        +("&alpha;:<INPUT TYPE=TEXT SIZE=4 NAME=\"alpha\" VALUE=\""+params.getVal("alpha")+"\">\n")
        +("&beta;:<INPUT TYPE=TEXT SIZE=4 NAME=\"beta\" VALUE=\""+params.getVal("beta")+"\"><BR>\n"));
    }
    else
      outhtm+=("<INPUT TYPE=HIDDEN NAME=\"metric\" VALUE=\"tanimoto\">\n");
    outhtm+=(
      ("<HR>\n")
      +("<B>output:</B><BR>\n")
      +("<INPUT TYPE=CHECKBOX NAME=\"out_view\" VALUE=\"CHECKED\" "+params.getVal("out_view")+">view")
      +("&nbsp;<INPUT TYPE=CHECKBOX NAME=\"depict\" VALUE=\"CHECKED\" "+params.getVal("depict")+">depict\n"));
    if (formmode.equals("expert")) outhtm+=("size:"+depsize_menu+"<BR>\n");
    else outhtm+=("<INPUT TYPE=HIDDEN NAME=\"depsize\" VALUE=\"s\"><BR>\n");
    outhtm+=(
      ("<INPUT TYPE=CHECKBOX NAME=\"full_output\" VALUE=\"CHECKED\" "+params.getVal("full_output")+">full_output")
      +("<BR>")
      +("<HR>\n")
      +("<B>misc:</B><BR>\n"));
    if (formmode.equals("expert"))
    {
      outhtm+=(("aromaticity:<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"gen\" "+arom_gen+">gen\n")
        +("&nbsp;<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"bas\" "+arom_bas+">bas\n")
        +("&nbsp;<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"none\" "+arom_none+">none<BR>\n"));
    }
    if (formmode.equals("expert") && ENABLE_NOLIMIT)
      outhtm+=("<INPUT TYPE=CHECKBOX NAME=\"nolimit\" VALUE=\"CHECKED\" "+params.getVal("nolimit")+">no-limit, input size <I>(defaults: N_MAX="+N_MAX+", N_MAX_MATRIX="+N_MAX_MATRIX+")</I><BR>\n");
    else
      outhtm+=("<INPUT TYPE=HIDDEN NAME=\"nolimit\" VALUE=\"\">\n");
    outhtm+=(
      ("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
      +("</TD></TR></TABLE>\n")
      +("<P>\n")
      +("<CENTER>\n")
      +("<BUTTON TYPE=BUTTON onClick=\"go_sim2d(this.form,'"+formmode+"')\"><B>Go "+APPNAME+"</B></BUTTON>\n")
      +("</CENTER>\n")
      +("</FORM>\n"));
    return outhtm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static Vector<Sim2DHit> Sim2d_1xN_LaunchThread(MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    Vector<Sim2DHit> hits=null;
    ExecutorService exec=Executors.newSingleThreadExecutor();
    int tpoll=1000; //msec
    if (molsDB.size()>1000) tpoll=5000;

    if (params.getVal("fptype").equals("path"))
    {
      Sim2D_Path_1xNTask simtask =
        new Sim2D_Path_1xNTask(molQ,null,molReaderDB,pathfplen,pathbcount,pathbitsper,
		arom,null,null,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
      TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,
	out,response,PROGRESS_WIN_NAME);
      hits=simtask.getHits();
      if (params.isChecked("sorthits")) Collections.sort(hits);

      //This triggers null ptr exc -- synchro problem?
      //while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    }
    else if (params.getVal("fptype").equals("maccs")
	|| params.getVal("fptype").equals("sunset")
	|| params.getVal("fptype").equals("pubchem"))
    {
      Sim2D_Smarts_1xNTask simtask=null;
      SmartsFile smaf=null;
      if (params.getVal("fptype").equals("maccs")) smaf=maccssf;
      else if (params.getVal("fptype").equals("pubchem")) smaf=pubchemsf;
      else smaf=sunsetsf;
      if (params.getVal("metric").equals("tversky"))
        simtask = new Sim2D_Smarts_1xNTask(molQ,null,molReaderDB,smaf,arom,alpha,beta,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
      else
        simtask = new Sim2D_Smarts_1xNTask(molQ,null,molReaderDB,smaf,arom,null,null,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
      TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,
	out,response,PROGRESS_WIN_NAME);
      hits=simtask.getHits();
      if (params.isChecked("sorthits")) Collections.sort(hits);

      //This triggers null ptr exc -- synchro problem?
      //while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    }
    else if (params.getVal("fptype").equals("ecfp"))
    {
      if (CA_ECFP_IS_LICENSED)
      {
        Sim2D_ECFP_1xNTask simtask=null;
          simtask = new Sim2D_ECFP_1xNTask(molQ,null,molReaderDB,ecfpdiam,ecfplen,arom,null,null,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
          if (params.getVal("metric").equals("tversky"))
            errors.add("Warning: Tversky not yet available for JChem ECFPs; using WeightedAsymmetricEuclidean.");
        TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
        hits=simtask.getHits();
        if (params.isChecked("sorthits")) Collections.sort(hits);
      }
      else
      {
        Sim2D_ECFP_CDK_1xNTask simtask=null;
        if (params.getVal("metric").equals("tversky"))
          simtask = new Sim2D_ECFP_CDK_1xNTask(molQ,null,molReaderDB,ecfpdiam,ecfplen,arom,alpha,beta,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
        else
          simtask = new Sim2D_ECFP_CDK_1xNTask(molQ,null,molReaderDB,ecfpdiam,ecfplen,arom,null,null,sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
        try {
          TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
          hits=simtask.getHits();
          if (params.isChecked("sorthits")) Collections.sort(hits);
        } catch (Exception e) { errors.add(e.getMessage()); }
      }

      //This triggers null ptr exc -- synchro problem?
      //while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    }
    return hits;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Sim2d_1xN_Results(Vector<Sim2DHit> hits,HttpParams params,
  	HttpServletResponse response)
      throws IOException,ServletException
  {
    int n_max_display=Math.min(400,n_max_hits);
    int n_hits=Math.min(hits.size(),n_max_hits);
    int w=depsizes_w.get(params.getVal("depsize"));
    int h=depsizes_h.get(params.getVal("depsize"));
    String depopts=("mode=cow");
    depopts+=("&imgfmt=png");
    params.setVal("dep_arom",params.getVal("arom"));
    if (params.getVal("dep_arom").equals("gen")) depopts+=("&arom_gen=true");
    else if (params.getVal("dep_arom").equals("bas")) depopts+=("&arom_bas=true");
    else if (params.getVal("dep_arom").equals("none")) depopts+=("&kekule=true");
    else depopts+=("&arom_gen=true");

    String smiQ=molQ.exportToFormat("smiles:u");
    String imghtmQ=HtmUtils.Smi2ImgHtm(smiQ,depopts,h,w,SMI2IMG_SERVLETURL,true,4,"go_zoom_smi2img");
    outputs.add("<B>query: "+molQ.getName()+"</B>");
    if (params.isChecked("depict"))
      outputs.add("<BLOCKQUOTE>"+imghtmQ+"</BLOCKQUOTE>");
    String fptype=params.getVal("fptype");
    if (fptype.equals("ecfp"))
      fptype+=(CA_ECFP_IS_LICENSED?"":" (CDK)");
    outputs.add("fptype: "+fptype);
    outputs.add("metric: "+params.getVal("metric"));
    outputs.add("N =  "+hits.size());
    outputs.add("N(hits) =  "+n_hits);
    File dout=new File(SCRATCHDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
    }
    File fout=File.createTempFile(TMPFILE_PREFIX,"_1xN_out.txt",dout);
    PrintWriter fout_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout,true)));
    if (params.isChecked("full_output"))
      fout_writer.printf("queryMolName\t");
    fout_writer.printf("molName");
    fout_writer.printf("\t"+params.getVal("metric")+"-"+params.getVal("fptype"));
    fout_writer.printf("\n");
    String thtm=("<TABLE BORDER>\n");
    thtm+="<TR>";
    thtm+="<TH>&nbsp;</TH>";
    thtm+="<TH>mol</TH>";
    thtm+="<TH>"+params.getVal("metric")+"-"+params.getVal("fptype")+"</TH>";
    thtm+="</TR>\n";

    for (int i=0;i<hits.size();++i)
    {
      if (i==n_max_hits) break;
      String smi=hits.get(i).smiles;
      String molname=hits.get(i).name;
      if (params.isChecked("verbose") && i<n_max_display)
      {
        errors.add(smi+" "+molname);
      }
      String rhtm="<TR>";
      rhtm+=("<TD VALIGN=TOP>"+(i+1)+".</TD>");
      rhtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
      if (params.isChecked("depict"))
      {
        String imghtm=HtmUtils.Smi2ImgHtm(smi,depopts,h,w,SMI2IMG_SERVLETURL,true,4,"go_zoom_smi2img");
        rhtm+=(imghtm+"<BR>\n");
      }
      rhtm+="<TT>"+molname+"</TT></TD>\n";
      
      // Must replace tabs.
      if (params.isChecked("full_output"))
        fout_writer.printf(molQ.getName().replace("\t", " ")+"\t");
      fout_writer.printf(molname.replace("\t", " "));

      float maxsim=(params.getVal("fptype").equals("ecfp")?0.5f:1.0f);
      if (params.getVal("fptype").equals("sunset") ||
      	params.getVal("fptype").equals("maccs") ||
      	params.getVal("fptype").equals("pubchem") ||
        params.getVal("fptype").equals("path"))
      {
        String color=Val2HeatColor(hits.get(i).sim,0.0f,maxsim);
        rhtm+=("<TD BGCOLOR=\"#"+color+"\" ALIGN=CENTER>");
        rhtm+=(String.format("%.2f",hits.get(i).sim));
        fout_writer.printf(String.format("\t%.2f",hits.get(i).sim));
        if (hits.get(i).subset) rhtm+=("*");
        rhtm+=("</TD>\n");
        if (params.isChecked("verbose") && i<n_max_display)
        {
          errors.add(String.format("&nbsp;&nbsp;"+params.getVal("fptype")+"-sim = %.2f",hits.get(i).sim));
          if (hits.get(i).subset) errors.add("&nbsp;&nbsp;"+params.getVal("fptype")+"-FP(query) is subset.");
          errors.add("&nbsp;&nbsp;"+params.getVal("fptype")+"-CommonBitCount = "+hits.get(i).commonbitcount);
          errors.add("&nbsp;&nbsp;"+params.getVal("fptype")+"-BitCount = "+hits.get(i).brightness);
        }
      }
      else if (params.getVal("fptype").equals("ecfp"))
      {
        String color=Val2HeatColor(hits.get(i).sim,0.0f,maxsim);
        rhtm+=("<TD BGCOLOR=\"#"+color+"\" ALIGN=CENTER>");
        rhtm+=(String.format("%.2f",hits.get(i).sim));
        fout_writer.printf(String.format("\t%.2f",hits.get(i).sim));
        if (hits.get(i).subset) rhtm+=("*");
        rhtm+=("</TD>\n");
        if (params.isChecked("verbose") && i<n_max_display)
        {
          errors.add(String.format("&nbsp;&nbsp;ecfp-sim = %.2f",hits.get(i).sim));
          errors.add("&nbsp;&nbsp;ecfp-CommonBitCount = "+hits.get(i).commonbitcount);
          errors.add("&nbsp;&nbsp;ecfp-BitCount = "+hits.get(i).brightness);
        }
      }
      rhtm+=("</TR>\n");
      if (i<n_max_display) thtm+=(rhtm);
      fout_writer.printf("\n");
    }
    thtm+="</TABLE>\n";
    fout_writer.close();
    if (params.isChecked("out_view"))
    {
      outputs.add(thtm);
      if (n_max_hits>n_max_display) outputs.add("Display truncated; n_max_display = "+n_max_display);
    }
    String fname=(SERVLETNAME+"_1xN_out_csv.txt");
    long fsize = fout.length();
    String bhtm= (("<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">")
      +("<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">")
      +("<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">")
      +("<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">")
      +("download "+fname+" (N="+n_hits+"; ")
      +(file_utils.NiceBytes(fsize)+")</BUTTON></FORM>"));
    outputs.add(bhtm);
    errors.add(SERVLETNAME+": n_hits = "+hits.size());
  }
  /////////////////////////////////////////////////////////////////////////////
  private static float [][] Sim2d_NxN_LaunchThread(MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    int tpoll=1000; //msec
    if (molsDB.size()>100) tpoll=5000;
    float [][] simatrix=null;
    ExecutorService exec=Executors.newSingleThreadExecutor();
    if (params.getVal("fptype").equals("path"))
    {
      Sim2D_Path_NxNTask simtask = new Sim2D_Path_NxNTask(molsDB,pathfplen,pathbcount,pathbitsper,arom,null,null);
      simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    }
    else if (params.getVal("fptype").equals("maccs")
	|| params.getVal("fptype").equals("sunset")
	|| params.getVal("fptype").equals("pubchem"))
    {
      Sim2D_Smarts_NxNTask simtask=null;
      SmartsFile smaf=null;
      if (params.getVal("fptype").equals("maccs")) smaf=maccssf;
      else if (params.getVal("fptype").equals("pubchem")) smaf=pubchemsf;
      else smaf=sunsetsf;
      if (params.getVal("metric").equals("tversky"))
        simtask = new Sim2D_Smarts_NxNTask(molsDB,smaf,arom,alpha,beta);
      else
        simtask = new Sim2D_Smarts_NxNTask(molsDB,smaf,arom,null,null);
      simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    }
    else if (params.getVal("fptype").equals("ecfp"))
    {
      if (CA_ECFP_IS_LICENSED)
      {
        Sim2D_ECFP_NxNTask simtask=null;
          simtask = new Sim2D_ECFP_NxNTask(molsDB,ecfpdiam,ecfplen,arom,null,null);
        if (params.getVal("metric").equals("tversky"))
          errors.add("Warning: Tversky not yet available for JChem ECFPs; using WeightedAsymmetricEuclidean.");
        simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
      }
      else
      {
        Sim2D_ECFP_CDK_NxNTask simtask=null;
        if (params.getVal("metric").equals("tversky"))
          simtask = new Sim2D_ECFP_CDK_NxNTask(molsDB,ecfpdiam,ecfplen,arom,alpha,beta);
        else
          simtask = new Sim2D_ECFP_CDK_NxNTask(molsDB,ecfpdiam,ecfplen,arom,null,null);
        try {
          simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
        } catch (Exception e) { errors.add(e.getMessage()); }
      }
    }
    return simatrix; 
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Sim2d_Matrix_Results(float [][] simatrix,HttpParams params,
	HttpServletResponse response)
      throws IOException,ServletException
  {
    if (params.getVal("mode").equals("QxN"))
      outputs.add("<B>RESULTS:</B> query mols: "+molsQ.size()+"  DB mols: "+molsDB.size());
    else
      outputs.add("<B>RESULTS:</B> DB mols: "+molsDB.size());
    int w=depsizes_w.get(params.getVal("depsize"));
    int h=depsizes_h.get(params.getVal("depsize"));
    String depopts=("mode=cow");
    depopts+=("&imgfmt=png");
    params.setVal("dep_arom",params.getVal("arom"));
    if (params.getVal("dep_arom").equals("gen")) depopts+=("&arom_gen=true");
    else if (params.getVal("dep_arom").equals("bas")) depopts+=("&arom_bas=true");
    else if (params.getVal("dep_arom").equals("none")) depopts+=("&kekule=true");
    else depopts+=("&arom_gen=true");

    File dout=new File(SCRATCHDIR);
    if (!dout.exists())
   {
     boolean ok=dout.mkdir();
     System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
   }
    File fout=File.createTempFile(TMPFILE_PREFIX,"_matrix_out.txt",dout);
    PrintWriter fout_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout,true)));
    String thtm=("<TABLE BORDER>\n<TR>");
    String fptype=params.getVal("fptype");
    if (fptype.equals("ecfp"))
      fptype+=(CA_ECFP_IS_LICENSED?"":" (CDK)");
    outputs.add("fptype: "+fptype);
    thtm+=("<TH>"+fptype+" / "+params.getVal("metric")+"</TH>");
    fout_writer.printf(params.getVal("fptype")+":"+params.getVal("metric"));
    for (int i=0;i<molsDB.size();++i)
    {
      String molnameDB=molsDB.get(i).getName();
      thtm+=("<TD ALIGN=\"center\">"+molnameDB);
      if (params.isChecked("depict"))
      {
        String smi=molsDB.get(i).exportToFormat("smiles:u");
        String imghtm=HtmUtils.Smi2ImgHtm(smi,depopts,h,w,SMI2IMG_SERVLETURL,true,4,"go_zoom_smi2img");
        thtm+=("<BR>\n"+imghtm);
      }
      thtm+=("</TD>");
      molnameDB=molnameDB.replace("\"","\"\""); // Must quote name for CSV in case of commas.
      fout_writer.printf(",\""+molnameDB+"\"");
    }
    thtm+="</TR>\n";
    fout_writer.printf("\n");
    int n_rows=(params.getVal("mode").equals("QxN")?molsQ.size():molsDB.size());
    for (int i=0;i<n_rows;++i)
    {
      Molecule molQ=(params.getVal("mode").equals("QxN")?molsQ.get(i):molsDB.get(i));
      String molnameQ=molQ.getName();
      String rhtm=("<TR>\n<TD>"+molnameQ);
      molnameQ=molnameQ.replace("\"","\"\""); // Must quote name for CSV in case of commas.
      fout_writer.printf("\""+molnameQ+"\"");
      if (params.isChecked("depict"))
      {
        String smi=molQ.exportToFormat("smiles:u");
        String imghtm=HtmUtils.Smi2ImgHtm(smi,depopts,h,w,SMI2IMG_SERVLETURL,true,4,"go_zoom_smi2img");
        rhtm+=("<BR>\n"+imghtm);
      }
      rhtm+=("</TD>\n");
      for (int j=0;j<molsDB.size();++j)
      {
        float maxsim=(params.getVal("fptype").equals("ecfp")?0.5f:1.0f);
        String color=Val2HeatColor(simatrix[i][j],0.0f,maxsim);
        rhtm+=("<TD BGCOLOR=\"#"+color+"\" ALIGN=CENTER>");
        rhtm+=(String.format("%.2f",simatrix[i][j]));
        rhtm+=("</TD>\n");
        fout_writer.printf(String.format(",%.2f",simatrix[i][j]));
      }
      fout_writer.printf("\n");
      rhtm+="</TR>\n";
      thtm+=rhtm;
    }
    fout_writer.close();
    thtm+="</TABLE>\n";
    if (params.isChecked("out_view"))
      outputs.add(thtm);

    // Generate aggregate statistics for simatrix.
    ArrayList<Float> listOfVals = sim2d_utils.Matrix2ListOfVals(simatrix,params.getVal("mode").equals("NxN"),params.getVal("metric").equals("tanimoto"));
    float avgsim = sim2d_utils.Mean(listOfVals);
    float sdsim = sim2d_utils.StdDev(listOfVals);
    outputs.add("matrix pairwise similarity statistics:");
    outputs.add(String.format("avg: %.2f; SD: %.2f; N=%d",avgsim,sdsim,listOfVals.size()));
    ArrayList<Integer> histoCounts = sim2d_utils.HistoCounts(listOfVals);
    String histotxt="";
    for (int i=0;i<9;++i)
    {
      histotxt+=(String.format("[%.1f-%.1f) (%3d): ",0.1f*i,0.1f*(i+1),histoCounts.get(i)));
      for (int j=0;j<histoCounts.get(i)*100/listOfVals.size();++j) histotxt+="*";
      histotxt+="\n";
    }
    histotxt+=(String.format("[0.9-1.0] (%3d): ",histoCounts.get(9)));
    for (int j=0;j<histoCounts.get(9)*100/listOfVals.size();++j) histotxt+="*";
    histotxt+="\n";
    outputs.add("<PRE>"+histotxt+"</PRE>");

    // Download output as CSV.
    String fname=(SERVLETNAME+"_matrix_out_csv.txt");
    long fsize = fout.length();
    String bhtm=("<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">");
    bhtm+=("<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">");
    if (params.getVal("mode").equals("QxN"))
      bhtm+=("download "+fname+" ("+molsQ.size()+"x"+molsDB.size()+"; ");
    else
      bhtm+=("download "+fname+" (N="+molsDB.size()+"; ");
    bhtm+=(file_utils.NiceBytes(fsize)+")</BUTTON></FORM>");
    outputs.add(bhtm);

    if (params.getVal("mode").equals("QxN"))
      errors.add(SERVLETNAME+": query mols processed: "+molsQ.size());
    errors.add(SERVLETNAME+": DB mols processed: "+molsDB.size());
  }
  /////////////////////////////////////////////////////////////////////////////
  private static float [][] Sim2d_QxN_LaunchThread(MultipartRequest mrequest,
	HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    int tpoll=1000; //msec
    if (molsDB.size()>100) tpoll=5000;
    float [][] simatrix=null;
    ExecutorService exec=Executors.newSingleThreadExecutor();
    if (params.getVal("fptype").equals("path"))
    {
      Sim2D_Path_QxNTask simtask = new Sim2D_Path_QxNTask(molsQ,molsDB,pathfplen,pathbcount,pathbitsper,arom,null,null);
      simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    }
    else if (params.getVal("fptype").equals("maccs")
	|| params.getVal("fptype").equals("sunset")
	|| params.getVal("fptype").equals("pubchem"))
    {
      Sim2D_Smarts_QxNTask simtask=null;
      SmartsFile smaf=null;
      if (params.getVal("fptype").equals("maccs")) smaf=maccssf;
      else if (params.getVal("fptype").equals("pubchem")) smaf=pubchemsf;
      else smaf=sunsetsf;
      if (params.getVal("metric").equals("tversky"))
        simtask = new Sim2D_Smarts_QxNTask(molsQ,molsDB,smaf,arom,alpha,beta);
      else
        simtask = new Sim2D_Smarts_QxNTask(molsQ,molsDB,smaf,arom,null,null);
      simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    }
    else if (params.getVal("fptype").equals("ecfp"))
    {
      if (CA_ECFP_IS_LICENSED)
      {
        Sim2D_ECFP_QxNTask simtask=null;
        if (params.getVal("metric").equals("tversky"))
          errors.add("Warning: Tversky not yet available for JChem ECFPs; using WeightedAsymmetricEuclidean.");
        simtask = new Sim2D_ECFP_QxNTask(molsQ,molsDB,ecfpdiam,ecfplen,arom,null,null);
        simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
      }
      else
      {
        Sim2D_ECFP_CDK_QxNTask simtask=null;
        if (params.getVal("metric").equals("tversky"))
          simtask = new Sim2D_ECFP_CDK_QxNTask(molsQ,molsDB,ecfpdiam,ecfplen,arom,alpha,beta);
        else
          simtask = new Sim2D_ECFP_CDK_QxNTask(molsQ,molsDB,ecfpdiam,ecfplen,arom,null,null);
        try {
          simatrix=TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
        } catch (Exception e) { errors.add(e.getMessage()); }
      }
    }
    return simatrix; 
  }
  /////////////////////////////////////////////////////////////////////////////
  private static Vector<Sim2DHit> Sim2d_1xN_Bitstr_LaunchThread(MultipartRequest mrequest,
	HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    int tpoll=1000; //msec
    if (bitstrsDB.size()>1000) tpoll=5000;
    Sim2D_Bitstring_1xNTask simtask = null;
    if (params.getVal("metric").equals("tversky"))
    {
      simtask = new Sim2D_Bitstring_1xNTask(bitstrQ,null,buffReaderDB,alpha,beta,
		sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
    }
    else
    {
      simtask = new Sim2D_Bitstring_1xNTask(bitstrQ,null,buffReaderDB,null,null,
		sim_min,N_MAX,n_max_hits,params.isChecked("sorthits"));
    }
    ExecutorService exec=Executors.newSingleThreadExecutor();
    TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,SERVLETNAME,
	tpoll,out,response,PROGRESS_WIN_NAME);
    Vector<Sim2DHit> hits=simtask.getHits();
    if (params.isChecked("sorthits")) Collections.sort(hits);

    //This triggers null ptr exc -- synchro problem?
    //while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    return hits;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Sim2d_1xN_Bitstr_Results(Vector<Sim2DHit> hits,HttpParams params,
	HttpServletResponse response)
      throws IOException,ServletException
  {
    int n_max_display=Math.min(400,n_max_hits);
    int n_hits=Math.min(hits.size(),n_max_hits);
    outputs.add("<B>query: "+nameQ+"</B>");
    outputs.add("metric: "+params.getVal("metric"));
    outputs.add("N =  "+hits.size());
    outputs.add("N(hits) =  "+n_hits);
 
    File dout=new File(SCRATCHDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
    }
    File fout=File.createTempFile(TMPFILE_PREFIX,"_1xN_out.txt",dout);
    PrintWriter fout_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout,true)));
    String thtm=("<TABLE BORDER>\n");
    thtm+="<TR>";
    thtm+="<TH>&nbsp;</TH>";
    thtm+="<TH>FP</TH>";
    fout_writer.printf("fp");
    thtm+="<TH>"+params.getVal("metric")+"</TH>";
    fout_writer.printf(","+params.getVal("metric"));
    thtm+="</TR>\n";
    fout_writer.printf("\n");
    if (params.isChecked("verbose"))
      errors.add("query fpsize:"+bitstrQ.length());

    for (int i=0;i<hits.size();++i)
    {
      if (i==n_max_hits) break;
      int idx=hits.get(i).i; // if unsorted, idx==i
      String name = hits.get(i).name;
      String rhtm="<TR>";
      rhtm+=("<TD VALIGN=TOP>"+(i+1)+".</TD>");
      rhtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
      rhtm+="<TT>"+name+"</TT></TD>\n";
      name=name.replace("\"","\"\""); // quote name in case of commas.
      fout_writer.printf("\""+name+"\"");
      String color=Val2HeatColor(hits.get(i).sim,0.0f,1.0f);
      rhtm+=("<TD BGCOLOR=\"#"+color+"\" ALIGN=CENTER>");
      rhtm+=(String.format("%.2f",hits.get(i).sim));
      fout_writer.printf(String.format(",%.2f",hits.get(i).sim));
      if (hits.get(i).subset) rhtm+=("*");
      rhtm+=("</TD>\n");
      if (params.isChecked("verbose") && i<n_max_display)
      {
        errors.add(name+":");
        errors.add(String.format("&nbsp;&nbsp;sim = %.2f",hits.get(i).sim));
        if (hits.get(i).subset) errors.add("&nbsp;&nbsp;FP(query) is subset.");
        errors.add("&nbsp;&nbsp;CommonBitCount = "+hits.get(i).commonbitcount);
        errors.add("&nbsp;&nbsp;BitCount = "+hits.get(i).brightness);
      }
      rhtm+=("</TR>\n");
      if (i<n_max_display) thtm+=rhtm;
      fout_writer.printf("\n");
    }
    thtm+="</TABLE>\n";
    fout_writer.close();
    if (params.isChecked("out_view"))
    {
      outputs.add(thtm);
      if (n_max_hits>n_max_display)
        outputs.add("Display truncated at N = "+n_max_display);
    }
    String fname=(SERVLETNAME+"_1xN_out_csv.txt");
    long fsize = fout.length();
    String bhtm=("<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">");
    bhtm+=("<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">");
    bhtm+=("download "+fname+" (N="+n_hits+"; ");
    bhtm+=(file_utils.NiceBytes(fsize)+")</BUTTON></FORM>");
    outputs.add(bhtm);

    errors.add(SERVLETNAME+": hit count: "+hits.size());
  }
  /////////////////////////////////////////////////////////////////////////////
  private static float [][] Sim2d_NxN_Bitstr_LaunchThread(
	MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    int tpoll=1000; //msec
    if (bitstrsDB.size()>100) tpoll=5000;
    Sim2D_Bitstring_NxNTask simtask = null;

    if (params.getVal("metric").equals("tversky"))
    {
      simtask = new Sim2D_Bitstring_NxNTask(bitstrsDB,alpha,beta);
    }
    else
    {
      simtask = new Sim2D_Bitstring_NxNTask(bitstrsDB,null,null);
    }
    ExecutorService exec=Executors.newSingleThreadExecutor();
    float [][] simatrix = TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,
	SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    return simatrix;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static float [][] Sim2d_QxN_Bitstr_LaunchThread(
	MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    int tpoll=1000; //msec
    if (bitstrsDB.size()>100) tpoll=5000;
    Sim2D_Bitstring_QxNTask simtask = null;

    if (params.getVal("metric").equals("tversky"))
    {
      simtask = new Sim2D_Bitstring_QxNTask(bitstrsQ,bitstrsDB,alpha,beta);
    }
    else
    {
      simtask = new Sim2D_Bitstring_QxNTask(bitstrsQ,bitstrsDB,null,null);
    }
    ExecutorService exec=Executors.newSingleThreadExecutor();
    float [][] simatrix = TaskUtils.ExecTaskWeb(exec,simtask,simtask.taskstatus,
	SERVLETNAME,tpoll,out,response,PROGRESS_WIN_NAME);
    return simatrix;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Sim2d_Matrix_Bitstr_Results(float [][] simatrix, HttpParams params,HttpServletResponse response)
      throws IOException,ServletException
  {
    if (params.getVal("mode").equals("QxN"))
      outputs.add("<B>RESULTS:</B> query bitstrs: "+bitstrsQ.size()+"  DB bitstrs: "+bitstrsDB.size());
    else
      outputs.add("<B>RESULTS:</B> DB bitstrs: "+bitstrsDB.size());
    File dout = new File(SCRATCHDIR);
    if (!dout.exists())
    {
      boolean ok = dout.mkdir();
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
    }
    File fout = File.createTempFile(TMPFILE_PREFIX,"_matrix_out.txt",dout);
    PrintWriter fout_writer = new PrintWriter(new BufferedWriter(new FileWriter(fout,true)));
    String thtm="";
    thtm = ("<TABLE BORDER>\n<TR>");
    thtm+=("<TH>"+params.getVal("metric")+"</TH>");
    fout_writer.printf(params.getVal("metric"));
    for (int i=0;i<bitstrsDB.size();++i)
    {
      String nameDB = namesDB.get(i);
      thtm+=("<TH>"+nameDB+"</TH>");
      nameDB = nameDB.replace("\"","\"\"");
      fout_writer.printf(","+nameDB);
      //thtm+=("<TH>"+(i+1)+"</TH>");
      //fout_writer.printf(","+(i+1));
    }
    thtm+="</TR>\n";
    fout_writer.printf("\n");
    int n_rows = (params.getVal("mode").equals("QxN")?bitstrsQ.size():bitstrsDB.size());
    for (int i=0;i<n_rows;++i)
    {
      String nameQ = (params.getVal("mode").equals("QxN")?namesQ.get(i):namesDB.get(i));
      String rhtm = ("<TR>\n<TD>"+nameQ+"</TD>\n");
      nameQ = nameQ.replace("\"","\"\""); // quote name in case of commas.
      fout_writer.printf("\""+nameQ+"\"");
      for (int j=0;j<bitstrsDB.size();++j)
      {
        float maxsim = (params.getVal("fptype").equals("ecfp")?0.5f:1.0f);
        String color = Val2HeatColor(simatrix[i][j],0.0f,maxsim);
        rhtm+=("<TD BGCOLOR=\"#"+color+"\" ALIGN=CENTER>");
        rhtm+=(String.format("%.2f",simatrix[i][j]));
        rhtm+=("</TD>\n");
        fout_writer.printf(String.format(",%.2f",simatrix[i][j]));
      }
      fout_writer.printf("\n");
      rhtm+="</TR>\n";
      thtm+=rhtm;
    }
    fout_writer.close();
    thtm+="</TABLE>\n";
    if (params.isChecked("out_view"))
      outputs.add(thtm);

    // Download output as CSV.
    String fname = (SERVLETNAME+"_matrix_out_csv.txt");
    long fsize = fout.length();
    String bhtm = ("<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">");
    bhtm+=("<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">");
    bhtm+=("<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">");
    if (params.getVal("mode").equals("QxN"))
      bhtm+=("download "+fname+" ("+bitstrsQ.size()+"x"+bitstrsDB.size()+"; ");
    else
      bhtm+=("download "+fname+" (N="+bitstrsDB.size()+"; ");
    bhtm+=(file_utils.NiceBytes(fsize)+")</BUTTON></FORM>");
    outputs.add(bhtm);

    if (params.getVal("mode").equals("QxN"))
      errors.add(SERVLETNAME+": query FPs processed: "+bitstrsQ.size());
    errors.add(SERVLETNAME+": DB FPs processed: "+bitstrsDB.size());
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
  {
    String DEMO_DATASET = (
"CN1C(=O)N(C)C(=O)C(N(C)C=N2)=C12 caffeine\n"+
"COc1cc2c(ccnc2cc1)C(O)C4CC(CC3)C(C=C)CN34 quinine\n"+
"CC1(C)SC2C(NC(=O)Cc3ccccc3)C(=O)N2C1C(=O)O benzylpenicillin\n"+
"CCC(=C(c1ccc(OCCN(C)C)cc1)c1ccccc1)c1ccccc1 Tamoxifen\n"+
"CNCCC(c1ccccc1)Oc2ccc(cc2)C(F)(F)F.Cl Prozac\n"+
"NC(C)Cc1ccccc1 adderall\n"+
"CNC(=C[N+](=O)[O-])NCCSCC1=CC=C(O1)CN(C)C.Cl Zantac\n"+
"Oc2cc(cc1OC(C3CCC(=CC3c12)C)(C)C)CCCCC THC\n"+
"CC(=CCO)C=CC=C(C)C=CC1=C(C)CCC1(C)C Vitamin A\n"+
"Oc1cccc(C=Cc2cc(O)cc(O)c2)c1 resveratrol\n"+
"CCOC(=O)C1=CC(OC(CC)CC)C(NC(C)=O)C(N)C1 Tamiflu\n"+
"OC(=O)CC(O)(CC(O)=O)C(O)=O.CCCc1nn(C)c2c1NC(=NC2=O)c1cc(ccc1OCC)S(=O)(=O)N1CCN(C)CC1 Viagra\n"+
"COc1cc(C=CC(=O)CC(=O)C=Cc2ccc(O)c(OC)c2)ccc1O Curcumin\n"+
"COC1CC(CCC1O)C=C(C)C1OC(=O)C2CCCCN2C(=O)C(=O)C2(O)OC(C(CC2C)OC)C(CC(C)CC(C)=CC(CC=C)C(=O)CC(O)C1C)OC Tacrolimus\n"+
"CC12CC(=O)C3C(CCC4=CC(=O)CCC34C)C1CCC2(O)C(=O)CO Cortisone\n"+
"CN1c2ccc(cc2C(=NCC1=O)c3ccccc3)Cl Valium\n");
    String js=(
"var DEMO_DATASET=`"+DEMO_DATASET+"`;\n"+
"function go_sim2d(form, formmode)\n"+
"{\n"+
"  if (!checkform(form,formmode)) return;\n"+
"  var x=300;\n"+
"  if (typeof window.screenX!='undefined') x+=window.screenX;\n"+
"  else x+=window.screenLeft; //IE\n"+
"  var y=300;\n"+
"  if (typeof window.screenY!='undefined') y+=window.screenY;\n"+
"  else y+=window.screenTop; //IE\n"+
"  var pwin=window.open('','"+PROGRESS_WIN_NAME+"',\n"+
"  'width=400,height=100,left='+x+',top='+y+',scrollbars=1,resizable=1,location=0,status=0,toolbar=0');\n"+
"  if (!pwin) {\n"+
"    alert('ERROR: popup windows must be enabled for progress indicator.');\n"+
"    return false;\n"+
"  }\n"+
"  pwin.focus();\n"+
"  pwin.document.close(); //if window exists, clear\n"+
"  pwin.document.open('text/html');\n"+
"  pwin.document.writeln('<HTML><HEAD>');\n"+
"  pwin.document.writeln('<LINK REL=\"stylesheet\" type=\"text/css\" HREF=\""+PROXY_PREFIX+CONTEXTPATH+"/css/biocomp.css\" />');\n"+
"  pwin.document.writeln('</HEAD><BODY BGCOLOR=\"#DDDDDD\">');\n"+
"  pwin.document.writeln('"+SERVLETNAME+"...<BR>');\n"+
" pwin.document.writeln('"+DateFormat.getDateInstance(DateFormat.FULL).format(new Date())+"<BR>');\n"+
"\n"+
"  if (navigator.appName.indexOf('Explorer')<0)\n"+
"    pwin.document.title='"+SERVLETNAME+" progress'; //not-ok for IE\n"+
"\n"+
"  form.sim2d.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"+
"function go_demo(form, formmode)\n"+
"{\n"+
"  go_init(form, formmode, false);\n"+
"  form.intxt.value=DEMO_DATASET;\n"+
"  for (i=0;i<form.mode.length;++i)\n"+ //radio
"    if (form.mode[i].value=='NxN')\n"+
"      form.mode[i].checked=true;\n"+
"  form.out_view.checked=true;\n"+
"  form.depict.checked=true;\n"+
"  go_sim2d(form, formmode);\n"+
"}\n"+
"function go_init(form, formmode, changemode)"+
"{\n"+
"  var i;\n"+
"  if (formmode=='expert')\n"+
"  {\n"+
"    form.sorthits.checked=true;\n"+
"    form.alpha.value='0.9';\n"+
"    form.beta.value='0.1';\n"+
"    for (i=0;i<form.arom.length;++i)\n"+ //radio
"      if (form.arom[i].value=='gen')\n"+
"        form.arom[i].checked=true;\n"+
"    for (i=0;i<form.pathfplen.length;++i)\n"+
"      if (form.pathfplen.options[i].value=='"+CFParameters.DEFAULT_LENGTH+"')\n"+
"        form.pathfplen.options[i].selected=true;\n"+
"    for (i=0;i<form.pathbcount.length;++i)\n"+
"      if (form.pathbcount.options[i].value=='"+CFParameters.DEFAULT_BOND_COUNT+"')\n"+
"        form.pathbcount.options[i].selected=true;\n"+
"    for (i=0;i<form.pathbitsper.length;++i)\n"+
"      if (form.pathbitsper.options[i].value=='"+CFParameters.DEFAULT_BITS_SET+"')\n"+
"        form.pathbitsper.options[i].selected=true;\n"+
"    for (i=0;i<form.ecfpdiam.length;++i)\n"+
"      if (form.ecfpdiam.options[i].value=='"+ECFPDIAM_DEFAULT+"')\n"+
"        form.ecfpdiam.options[i].selected=true;\n"+
"    for (i=0;i<form.ecfplen.length;++i)\n"+
"      if (form.ecfplen.options[i].value=='"+ECFPLEN_DEFAULT+"')\n"+
"        form.ecfplen.options[i].selected=true;\n"+
"    for (i=0;i<form.metric.length;++i)\n"+ //radio
"      if (form.metric[i].value=='tanimoto')\n"+
"        form.metric[i].checked=true;\n"+
"  }\n"+
"  if (changemode) return;\n"+
"  form.file2txtQ.checked=true;\n"+
"  form.file2txt.checked=false;\n"+
"  form.intxt.value='';\n"+
"  form.intxtQ.value='';\n"+
"  form.n_max_hits.value='400';\n"+
"  form.sim_min.value='0.0';\n"+
"  form.full_output.checked=true;\n"+
"  form.out_view.checked=false;\n"+
"  form.depict.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"  for (i=0;i<form.molfmtDB.length;++i)\n"+
"    if (form.molfmtDB.options[i].value=='automatic')\n"+
"      form.molfmtDB.options[i].selected=true;\n"+
"  for (i=0;i<form.molfmtQ.length;++i)\n"+
"    if (form.molfmtQ.options[i].value=='automatic')\n"+
"      form.molfmtQ.options[i].selected=true;\n"+
"  for (i=0;i<form.depsize.length;++i)\n"+
"    if (form.depsize.options[i].value=='s')\n"+
"      form.depsize.options[i].selected=true;\n"+
"  for (i=0;i<form.mode.length;++i)\n"+ //radio
"    if (form.mode[i].value=='1xN')\n"+
"      form.mode[i].checked=true;\n"+
"  for (i=0;i<form.fptype.length;++i)\n"+ //radio
"    if (form.fptype[i].value=='sunset')\n"+
"      form.fptype[i].checked=true;\n"+
"}\n"+
"function checkform(form,formmode)\n"+
"{\n"+
"  var i;\n"+
"  var mode;\n"+
"  for (i=0;i<form.mode.length;++i)\n"+
"    if (form.mode[i].checked)\n"+
"      mode=form.mode[i].value;\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return false;\n"+
"  }\n"+
"  if (mode=='1xN' && !form.intxtQ.value && !form.infileQ.value) {\n"+
"    alert('ERROR: Query molecule required in search mode.');\n"+
"    return false;\n"+
"  }\n"+
"  if (mode=='QxN' && !form.intxtQ.value && !form.infileQ.value) {\n"+
"    alert('ERROR: Query molecules required in QxN mode.');\n"+
"    return false;\n"+
"  }\n"+
"  var fptype;\n"+
"  for (i=0;i<form.fptype.length;++i)\n"+ //radio
"    if (form.fptype[i].checked)\n"+
"      fptype=form.fptype[i].value;\n"+
"  if (formmode=='expert')\n"+
"  {\n"+
"    var metric;\n"+
"    for (i=0;i<form.metric.length;++i)\n"+ //radio
"      if (form.metric[i].checked)\n"+
"        metric=form.metric[i].value;\n");
  if (!CA_ECFP_IS_LICENSED)
    js+=(
"    if (fptype=='ecfp' && metric=='tversky')\n"+
"    {\n"+
"      alert('ERROR: tversky metric currently not supported by CDK ECFP.');\n"+
"      return false;\n"+
"    }\n");
  js+=(
"    if (metric=='tversky' && !(form.alpha.value && form.beta.value))\n"+
"    {\n"+
"      alert('ERROR: tversky metric requires alpha and beta.');\n"+
"      return false;\n"+
"    }\n"+
"    if (metric=='tversky' && fptype=='path')\n"+
"    {\n"+
"      alert('ERROR: tversky not supported for path FPs.');\n"+
"      return false;\n"+
"    }\n"+
"  }\n"+
"  var molfmtQ='';\n"+
"  var molfmtDB='';\n"+
"  for (i=0;i<form.molfmtQ.length;++i)\n"+
"    if (form.molfmtQ.options[i].selected)\n"+
"      molfmtQ=form.molfmtQ.options[i].value;\n"+
"  for (i=0;i<form.molfmtDB.length;++i)\n"+
"    if (form.molfmtDB.options[i].selected)\n"+
"      molfmtDB=form.molfmtDB.options[i].value;\n"+
"  if (mode=='1xN' &&\n"+
"      ((molfmtQ=='bitstr' && molfmtDB!='bitstr') ||\n"+
"       (molfmtQ!='bitstr' && molfmtDB=='bitstr')))\n"+
"  {\n"+
"    alert('ERROR: FP Bitstring format required for both query and database.');\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"function go_changemode(form)\n"+
"{\n"+
"  form.changemode.value='TRUE';\n"+
"  form.submit();\n"+
"}\n"
    );
    return js;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    String htm = 
    ("<H2>"+APPNAME+" help</H2>\n"+
    "<P>\n"+
    "This web application computes 2D molecular similarity using the\n"+
    "Tanimoto or Tversky metrics and fingerprints generated using several\n"+
    "choices of algorithm.\n"+
    "<P>\n"+
    "<B>Metrics:</B>\n"+
    "Molecular fingerprints combined with a similarity measure comprise a\n"+
    "metric for chemical space.\n"+
    "Given two bitarrays, there are many methods by which to compare them,\n"+
    "Tanimoto being one preferred general purpose method.\n"+
    "Tversky similarity may be described as substructural similarity,\n"+
    "where the &alpha; and &beta; parameters weight the query and\n"+
    "database molecules, respectively.  Note that with &alpha;=1 and\n"+
    "&beta;=1, Tversky=Tanimoto.  Recommended: &alpha;=0.9, &beta;=0.1.\n"+
    "<BLOCKQUOTE>\n"+
    "Tanimoto similarity = c / (a + b + c)<BR>\n"+
    "Tversky similarity = c / (&alpha;*a + &beta;*b + c)<BR>\n"+
    "<I>where:</I><BR>\n"+
    "&nbsp;&nbsp;c = bits in common<BR>\n"+
    "&nbsp;&nbsp;a = bits in FP(A)<BR>\n"+
    "&nbsp;&nbsp;b = bits in FP(B)<BR>\n"+
    "\n"+
    "</BLOCKQUOTE>\n"+
    "\n"+
    "<P>\n"+
    "<B>Path based fingerprints:</B>\n"+
    "JChem hashed chemical fingerprints are used. Default parameters:\n"+
    "length = "+CFParameters.DEFAULT_LENGTH+", "+
    "bonds/path = "+CFParameters.DEFAULT_BOND_COUNT+", "+
    "bits/path = "+CFParameters.DEFAULT_BITS_SET+", "+
    "<P>\n"+
    "Hashed chemical fingerprints involve exhaustive enumeration of molecular\n"+
    "subgraphs (in this case linear paths) up to a specified size, in this case \n"+
    "specified in bonds per path.  Each path is hashed to one or more bits in\n"+
    "a bit-array.  The size of the bit-array and bits set per path are also\n"+
    "configurable.\n"+
    "<P>\n"+
    "<B>Structural key based fingerprints:</B>\n"+
    "An alternative to hashed fingerprints are structural key based\n"+
    "fingerprints, notably those based on the MACCS keyset (MDL).  These fingerprints\n"+
    "recognize a specific hard-coded set of chemical patterns.\n"+
    "MDL has published a 166-key\n"+
    "set and a 960-key set.  Only the 166-key set is implemented here.\n"+
    "\n"+
    "Also available are fingerprints based on structural keys from\n"+
    "<A HREF=\"http://www.sunsetmolecular.com\">Sunset Molecular</A>.\n"+
    "Current key set size = 560.\n"+
    "Also available are fingerprints based on structural keys from\n"+
    "<A HREF=\"ftp://ftp.ncbi.nih.gov/pubchem/data_spec/pubchem_fingerprints.txt\">PubChem</A>.\n"+
    "The SMARTS used are adapted from a set published by Andrew Dalke, via the\n"+
    "<A HREF=\"http://code.google.com/p/chem-fingerprints/source/browse/chemfp/substruct.patterns\">ChemFP Project</A>.\n"+
    "According to PubChem there are 881 keys defined in this set.  However, a few which represent counts of\n"+
    "common elements (e.g. 16+ Oxygens, 32+ Carbons, etc.) have been omitted as these\n"+
    "would degrade performance significantly.  The key count is currently 753 (Dec. 2011) so there appear to\n"+
    "be others missing also.\n"+
    "Structural keys are designed for medicinal chemistry and pharmaceutical\n"+
    "research, and are considered slightly superior for this purpose than\n"+
    "hashed fingerprints.  An advantage of hashed fingerprints is their generality\n"+
    "and lack of bias toward known patterns.\n"+
    "<P>\n"+
    "<B>Extended connectivity fingerprints (ECFPs):</B> are similar to path-based,\n"+
    "using graph theory based exhaustive enumeration of subgraph patterns. \n"+
    "The dimensionality of ECFPs is large, perhaps 2^32 or 10^10, but can\n"+
    "be compactly stored as a sparse matrix (list of ints). \n"+
    "Popularized by Accelrys/Scitegic Pipeline Pilot.\n"+
    "This web app utilizes alternative ECFP implementations: (1) the JChem ECFP class\n"+
    "[default], and (2) CDK implementation by Alex Clark.  If the ChemAxon ECFP license is\n"+
    "not available, the CDK implementation is used.\n"+
    "<P>\n"+
    (CA_ECFP_IS_LICENSED?  "ChemAxon ECFP: <B>IS LICENSED.</B>": "ChemAxon ECFP <B>NOT LICENSED; USING CDK.</B>")+
    "\n<P>\n"
    );
    htm+=(
    "ECFP default parameters:\n"+
    "diameter = "+ECFPDIAM_DEFAULT+", "+
    "length = "+ECFPLEN_DEFAULT+"."+
    "<P>\n"+
    "<B>QxN mode</B>: in QxN matrix mode, each\n"+
    "pairwise similarity is calculated for all query molecules against database molecules,\n"+
    "producing a matrix of Q rows and N columns.\n"+
    "<P>\n"+
    "<B>NxN mode</B>: in NxN matrix mode, the query molecule is ignored, and each\n"+
    "pairwise similarity is calculated for all database molecules.\n"+
    "<P>\n"+
    "<B>FP Bitstring input</B>: in expert mode bitstring input may be used.\n"+
    "Bitstrings consist of ascii 1's and 0's optionally followed by a space\n"+
    "and name.   Bitstrings must all be the same length.  Specify input\n"+
    "format \"FP Bitstring\".\n"+
    "<P>\n"+
    "Another use of fingerprints is for substructure search\n"+
    "pre-screening.  Fingerprints may be designed such that the bits of\n"+
    "FP(A) will be a subset of FP(B) if A is a substructure of B.  This use\n"+
    "is not relevant to this application.\n"+
    "<P>\n"+
    "This web-app uses Java\n"+
    "servlet Mol2Img.java, to\n"+
    "depict the molecules as inline PNG images. \n"+
    "<P>\n"+
    "Configured with:<UL>\n"+
    "<LI> N_MAX = "+N_MAX+"<I>(maximum number of similarity calculations)</I>\n"+
    "<LI> N_MAX_MATRIX = "+N_MAX_MATRIX+"\n"+
    "<LI> MAX(Q x N) = N_MAX_MATRIX<SUP>2</SUP> = "+N_MAX_MATRIX*N_MAX_MATRIX+"\n"+
    "<LI> MAX_POST_SIZE = "+file_utils.NiceBytes(MAX_POST_SIZE)+"\n"+
    "</UL>\n"+
    "<P>\n"+
    "Note that download files purged after "+(scratch_retire_sec/60)+"min.\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "<B>Webapp author/support:</B>Jeremy Yang\n"+
    "<P>\n"+
    "<B>References:</B><OL>\n"+
    "<LI><A HREF=\"http://www.chemaxon.com/jchem/doc/user/fingerprint.html\">"+
    "ChemAxon JChem Base Guide: Chemical Hashed Fingerprints</A>\n"+
    "<LI><A HREF=\"http://sourceforge.net/projects/cdk/\">CDK: The Chemistry Development Kit</A>\n"+
    "<LI><A HREF=\"http://www.daylight.com/dayhtml/doc/theory/theory.finger.html\">"+
    "Daylight Theory Manual: Fingerprints</A>\n"+
    "<LI><A HREF=\"http://www.mdli.com/solutions/white_papers/downloads/public/SSKeys_whitepaper.pdf\">"+
    "MDL keys white paper</A>\n"+
    "<LI> Similarity Searching of Chemical Databases Using Atom Environment Descriptors (MOLPRINT 2D): Evaluation of Performance, Andreas Bender, Hamse Y. Mussa, and Robert C. Glen, <I>J. Chem. Inf. Comput. Sci.</I>, Vol. 44, No. 5, 2004.\n"+
    "</OL>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT = getServletContext();
    CONTEXTPATH = CONTEXT.getContextPath();
    // read servlet parameters (from web.xml):
    try { APPNAME = conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME = this.getServletName(); }
    UPLOADDIR = conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null) throw new ServletException("Please supply UPLOADDIR parameter");
    DATADIR = CONTEXT.getRealPath("")+"/data";
    try { N_MAX = Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=10000; }
    try { N_MAX_MATRIX = Integer.parseInt(conf.getInitParameter("N_MAX_MATRIX")); }
    catch (Exception e) { N_MAX_MATRIX=50; }
    try { MAX_POST_SIZE = Integer.parseInt(conf.getInitParameter("MAX_POST_SIZE")); }
    catch (Exception e) { MAX_POST_SIZE=10*1024*1024; }
    try { ENABLE_NOLIMIT = Boolean.parseBoolean(conf.getInitParameter("ENABLE_NOLIMIT")); }
    catch (Exception e) { ENABLE_NOLIMIT=false; }
    SCRATCHDIR = conf.getInitParameter("SCRATCHDIR");
    PROXY_PREFIX = ((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");
    // Parse all smarts files into SmartsFile objects.
    // By doing this here in init(), then it happens only
    // once for each servlet instance and not each submit.
    String fpath = DATADIR+"/smarts/"+MACCSFILE;
    try {
      maccssf = new SmartsFile();
      maccssf.parseFile(new File(fpath),false,"maccs166");
    }
    catch (Exception e) { CONTEXT.log("problem reading: "+fpath+" ; "+e.getMessage()); }
    CONTEXT.log("loaded smarts file: "+fpath+" ("
      +maccssf.getRawtxt().length()+" bytes , "
      +maccssf.size()+" smarts, "
      +maccssf.getDefines().size()+" defines, "
      +maccssf.getFailedsmarts().size()+" failed smarts)");
    for (String sma:maccssf.getFailedsmarts())
      CONTEXT.log("bad smarts: \""+sma+"\"");
    fpath = DATADIR+"/smarts/"+SUNSETFILE;
    try {
      sunsetsf = new SmartsFile();
      sunsetsf.parseFile(new File(fpath),false,"sunset");
    }
    catch (Exception e) { CONTEXT.log("problem reading: "+fpath+" ; "+e.getMessage()); }
    CONTEXT.log("loaded smarts file: "+fpath+" ("
      +sunsetsf.getRawtxt().length()+" bytes , "
      +sunsetsf.size()+" smarts, "
      +sunsetsf.getDefines().size()+" defines, "
      +sunsetsf.getFailedsmarts().size()+" failed smarts)");
    for (String sma:sunsetsf.getFailedsmarts())
      CONTEXT.log("bad smarts: \""+sma+"\"");
    fpath = DATADIR+"/smarts/"+PUBCHEMFILE;
    try {
      pubchemsf = new SmartsFile();
      pubchemsf.parseFile(new File(fpath),false,"pubchem");
    }
    catch (Exception e) { CONTEXT.log("problem reading: "+fpath+" ; "+e.getMessage()); }
    CONTEXT.log("loaded smarts file: "+fpath+" ("
      +pubchemsf.getRawtxt().length()+" bytes , "
      +pubchemsf.size()+" smarts, "
      +pubchemsf.getDefines().size()+" defines, "
      +pubchemsf.getFailedsmarts().size()+" failed smarts)");
    for (String sma:pubchemsf.getFailedsmarts())
      CONTEXT.log("bad smarts: \""+sma+"\"");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}

