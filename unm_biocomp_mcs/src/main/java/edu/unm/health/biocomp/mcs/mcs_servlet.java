package edu.unm.health.biocomp.mcs;

import java.io.*;
import java.net.*; //URLEncoder, InetAddress
import java.text.*;
import java.lang.Math;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import java.awt.Color;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.*; //MultipartRequest
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.struc.prop.MMoleculeProp;
import chemaxon.sss.search.*; //SearchException, MCES
import chemaxon.jchem.version.*;

import com.chemaxon.search.mcs.*; //MaxCommonSubstructure,TerminationCause,SearchMode,McsSearchOptions

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;
import edu.unm.health.biocomp.depict.molalign.*;

/**	Max Common Substructure app.

	NOTE: Class MCES exists in JChem 5.8.3 but not in 6.3.1.
	New class com.chemaxon.search.mcs.MaxCommonSubstructure.

	@author Jeremy J Yang
*/
public class mcs_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;	// configured in web.xml
  private static String APPNAME=null;   // configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static String DEMOSMIFILE=null;	// configured in web.xml
  private static int N_MAX=100;	// configured in web.xml
  private static int N_MAX_MATRIX=20;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static byte[] inbytes=null;
  private static Molecule molQ=null;
  private static ArrayList<Molecule> mols=null;
  private static MolImporter molReader=null;
  private static LinkedHashMap<String,Integer> sizes_h=null;
  private static LinkedHashMap<String,Integer> sizes_w=null;
  private static int serverport=0;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static Calendar calendar=null;
  private static String datestr=null;
  private static File logfile=null;
  private static String ofmt="";
  private static String color1="#EEEEEE";
  private static Integer arom=null;

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    serverport=request.getServerPort();
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
        mrequest=new MultipartRequest(request,UPLOADDIR,10*1024*1024,"ISO-8859-1",
                                    new DefaultFileRenamePolicy());
      }
      catch (IOException lEx) {
        this.getServletContext().log("not a valid MultipartRequest",lEx);
      }
    }
    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList("biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList("/marvin/marvin.js","biocomp.js","ddtip.js"));
    boolean ok=initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
      out.print(HtmUtils.FooterHtm(errors,true));
      return;
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("mcs").equalsIgnoreCase("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(FormHtm(mrequest,response));
        if (params.getVal("runmode").equals("NxN"))
        {
          MCS_NxN(mrequest,response,params);
        }
        else
        {
          MCS_1xN(mrequest,response,params);
        }
        out.print(HtmUtils.OutputHtm(outputs));
        out.print(HtmUtils.FooterHtm(errors,true));
      }
    }
    else
    {
      String help=request.getParameter("help");	// GET param
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      if (help!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,false));
      }
      else if (downloadtxt!=null && downloadtxt.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadString(response,ostream,request.getParameter("downloadtxt"),
          request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(FormHtm(mrequest,response));
        out.println("<SCRIPT>go_init(window.document.mainform)</SCRIPT>");
        out.print(HtmUtils.FooterHtm(errors,true));
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
    sizes_h=new LinkedHashMap<String,Integer>();
    sizes_w=new LinkedHashMap<String,Integer>();
    calendar=Calendar.getInstance();
    mols=new ArrayList<Molecule>();

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem and Marvin from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    inbytes=new byte[1024];
    sizes_h.put("xs",96); sizes_w.put("xs",96);
    sizes_h.put("s",120); sizes_w.put("s",120);
    sizes_h.put("m",160); sizes_w.put("m",180);
    sizes_h.put("l",200); sizes_w.put("l",240);
    sizes_h.put("xl",300); sizes_w.put("xl",400);

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
    buff.close(); //Else can result in error: "Too many open files"
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
      errors.add("ServletName: "+this.getServletName());
    }

    if (params.getVal("arom").equals("gen"))
      arom=MoleculeGraph.AROM_GENERAL;
    else if (params.getVal("arom").equals("bas"))
      arom=MoleculeGraph.AROM_BASIC;
    else if (params.getVal("arom").equals("none"))
      arom=null;

    String fnameQ="infileQ";
    File fileQ=mrequest.getFile(fnameQ);
    String intxtQ=params.getVal("intxtQ").replaceFirst("[\\s]+$","");
    String fnameDB="infileDB";
    File fileDB=mrequest.getFile(fnameDB);
    String intxtDB=params.getVal("intxtDB").replaceFirst("[\\s]+$","");

    if (fileQ!=null)
    {
      if (params.isChecked("file2txtQ"))
      {
        BufferedReader br=new BufferedReader(new FileReader(fileQ));
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
      if (params.isChecked("file2txtDB") && fileDB!=null)
      {
        BufferedReader br=new BufferedReader(new FileReader(fileDB));
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
        params.setVal("intxtDB",intxtDB);
      }
      else
      {
        params.setVal("intxtDB","");
      }
    }

    // NxN: read/store database mols:
    // 1xN: pass molReader for memory savings.
    String ifmtDB=null;
    if (params.getVal("ifmtDB").equals("automatic"))
    {
      String ifmt_auto=MFileFormatUtil.getMostLikelyMolFormat(mrequest.getOriginalFileName(fnameDB));
      if (ifmt_auto!=null)
        ifmtDB=ifmt_auto;
    }
    else
    {
      ifmtDB=params.getVal("ifmtDB");
    }
    if (ifmtDB!=null && !ifmtDB.isEmpty())
    {
      if (fileDB!=null)
        molReader=new MolImporter(fileDB,ifmtDB);
      else if (intxtDB.length()>0)
        molReader=new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()),ifmtDB);
    }
    else
    {
      if (fileDB!=null)
        molReader=new MolImporter(new FileInputStream(fileDB));
      else if (intxtDB.length()>0)
        molReader=new MolImporter(new ByteArrayInputStream(intxtDB.getBytes()));
    }

    String fmtdesc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
    if (params.isChecked("verbose"))
    {
      errors.add("input format:  "+molReader.getFormat()+" ("+fmtdesc+")");
    }

    if (params.getVal("runmode").equals("1xN"))
    {
      // Read query mol:
      MolImporter molReaderQ=null;
      String ifmtQ=null;
      if (params.getVal("ifmtQ").equals("automatic"))
      {
        String ifmt_auto=MFileFormatUtil.getMostLikelyMolFormat(mrequest.getOriginalFileName(fnameQ));
        if (ifmt_auto!=null)
          ifmtQ=ifmt_auto;
      }
      else
      {
        ifmtQ=params.getVal("ifmtQ");
      }
      if (ifmtQ!=null && !ifmtQ.isEmpty())
      {
        if (fileQ!=null)
          molReaderQ=new MolImporter(new FileInputStream(fileQ),ifmtQ);
        else if (intxtQ.length()>0)
          molReaderQ=new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()),ifmtQ);
      }
      else
      {
        if (fileQ!=null)
          molReaderQ=new MolImporter(new FileInputStream(fileQ));
        else if (intxtQ.length()>0)
          molReaderQ=new MolImporter(new ByteArrayInputStream(intxtQ.getBytes()));
      }
      if (molReaderQ!=null) molQ=molReaderQ.read();
      else molQ=molReader.read();	// molQ is 1st DB mol
      if (arom!=null)
        molQ.aromatize(arom);
      else
        molQ.dearomatize();
      if (molReaderQ!=null) molReaderQ.close();
    }
    else if (params.getVal("runmode").equals("NxN"))
    {
      Molecule m;
      int n_failed=0;
      while (true)
      {
        try {
          m=molReader.read();
        }
        catch (MolFormatException e)
        {
          errors.add(SERVLETNAME+"ERROR: MolImporter failed: "+e.getMessage());
          ++n_failed;
          continue;
        }
        if (m==null) break;
    
        if (arom!=null)
          m.aromatize(arom);
        else
          m.dearomatize();
        mols.add(m);
        if (mols.size()==N_MAX_MATRIX)
        {
          errors.add("Warning: mol list truncated at N_MAX_MATRIX mols: "+N_MAX_MATRIX);
          break;
        }
      }
      molReader.close();
      if (params.isChecked("verbose"))
        errors.add("mols read:  "+mols.size());
      if (n_failed>0) errors.add(SERVLETNAME+"ERROR (unable to read mol): "+n_failed);
    }
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest,HttpServletResponse response)
  {
    String ifmtQ_menu="<SELECT NAME=\"ifmtQ\">\n<OPTION VALUE=\"automatic\">automatic\n";
    for (String fmt: MFileFormatUtil.getMolfileFormats())
      ifmtQ_menu+=("<OPTION VALUE=\""+fmt+"\">"+MFileFormatUtil.getFormat(fmt).getDescription()+"\n");
    ifmtQ_menu+=("</SELECT>");
    ifmtQ_menu=ifmtQ_menu.replace("\""+params.getVal("ifmtQ")+"\">","\""+params.getVal("ifmtQ")+"\" SELECTED>");
    String ifmtDB_menu="<SELECT NAME=\"ifmtDB\">\n<OPTION VALUE=\"automatic\">automatic\n";
    for (String fmt: MFileFormatUtil.getMolfileFormats())
      ifmtDB_menu+=("<OPTION VALUE=\""+fmt+"\">"+MFileFormatUtil.getFormat(fmt).getDescription()+"\n");
    ifmtDB_menu+=("</SELECT>");
    ifmtDB_menu=ifmtDB_menu.replace("\""+params.getVal("ifmtDB")+"\">","\""+params.getVal("ifmtDB")+"\" SELECTED>");

    String size_menu="<SELECT NAME=\"size\">\n";
    for (String key:sizes_h.keySet())
      size_menu+=("<OPTION VALUE=\""+key+"\">"+key+" - "+sizes_h.get(key)+"x"+sizes_w.get(key)+"\n");
    size_menu+="</SELECT>\n";
    size_menu=size_menu.replace("\""+params.getVal("size")+"\">","\""+params.getVal("size")+"\" SELECTED>");

    String arom_gen=""; String arom_bas=""; String arom_none="";
    if (params.getVal("arom").equals("gen")) arom_gen="CHECKED";
    else if (params.getVal("arom").equals("bas")) arom_bas="CHECKED";
    else arom_none="CHECKED";

    String depictNxN_mcs=""; String depictNxN_target="";
    if (params.getVal("depictNxN").equals("mcs")) depictNxN_mcs="CHECKED";
    else depictNxN_target="CHECKED";

    String searchmode_fast=""; String searchmode_normal="";
    if (params.getVal("searchmode").equals("fast")) searchmode_fast="CHECKED";
    else searchmode_normal="CHECKED";

    String runmode_1xN=""; String runmode_NxN="";
    if (params.getVal("runmode").equals("NxN")) runmode_NxN="CHECKED";
    else if (params.getVal("runmode").equals("1xN")) runmode_1xN="CHECKED";
    else runmode_1xN="CHECKED";

    String htm=
      ("<FORM NAME=\"mainform\" METHOD=POST")
      +(" ACTION=\""+response.encodeURL(SERVLETNAME)+"\"")
      +(" ENCTYPE=\"multipart/form-data\">\n")
      +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+APPNAME+"</H1></TD>\n")
      +("<TD> - Maximum Common Substructure analysis</TD>\n")
      +("<TD ALIGN=RIGHT>")
      +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>")
      +("<BUTTON TYPE=BUTTON onClick=\"go_demo(this.form)\"><B>Demo</B></BUTTON>\n")
      +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
      +("</TD></TR></TABLE>\n")
      +("<HR>\n")
      +("<INPUT TYPE=HIDDEN NAME=\"mcs\">\n")
      +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
      +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP>")
      +("<B>query:</B> format:"+ifmtQ_menu)
      +("<INPUT TYPE=CHECKBOX NAME=\"file2txtQ\" VALUE=\"CHECKED\" "+params.getVal("file2txtQ")+">file2txt<BR>\n")
      +("upload: <INPUT TYPE=\"FILE\" NAME=\"infileQ\"> ...or paste:")
      +("<BR><TEXTAREA NAME=\"intxtQ\" WRAP=OFF ROWS=8 COLS=60>"+params.getVal("intxtQ")+"</TEXTAREA>\n")
      +("<BR>\n")
      +("<B>mols:</B> format:"+ifmtDB_menu)
      +("<INPUT TYPE=CHECKBOX NAME=\"file2txtDB\" VALUE=\"CHECKED\" "+params.getVal("file2txtDB")+">file2txt<BR>")
      +("upload: <INPUT TYPE=\"FILE\" NAME=\"infileDB\"> ...or paste:")
      +("<BR><TEXTAREA NAME=\"intxtDB\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxtDB")+"</TEXTAREA>\n")
      +("</TD>\n")
      +("<TD VALIGN=TOP>")
      +("<B>runmode:</B><BR>\n")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"runmode\" VALUE=\"1xN\" "+runmode_1xN+">search (1XN)<BR>\n")
      +("&nbsp;&nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME=\"show_query\" VALUE=\"CHECKED\" "+params.getVal("show_query")+">show query each line<BR>")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"runmode\" VALUE=\"NxN\" "+runmode_NxN+">matrix (NxN)\n")
      +("<BR>\n")
      +("&nbsp;&nbsp;&nbsp;depict:<INPUT TYPE=RADIO NAME=\"depictNxN\" VALUE=\"mcs\" "+depictNxN_mcs+">mcs")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"depictNxN\" VALUE=\"target\" "+depictNxN_target+">target<BR>\n")
      +("<HR>\n")
      +("<B>mcs:</B><BR>\n")
      +("&nbsp;searchmode:<INPUT TYPE=RADIO NAME=\"searchmode\" VALUE=\"fast\" "+searchmode_fast+">fast")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"searchmode\" VALUE=\"normal\" "+searchmode_normal+">normal<BR>\n")
      +("&nbsp;arom:<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"gen\" "+arom_gen+">gen")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"bas\" "+arom_bas+">bas")
      +("&nbsp;<INPUT TYPE=RADIO NAME=\"arom\" VALUE=\"none\" "+arom_none+">none<BR>\n")
      +("&nbsp;minsize:<INPUT TYPE=TEXT NAME=\"minsize\" SIZE=5 VALUE=\""+params.getVal("minsize")+"\"><BR>\n")
      +("&nbsp;<INPUT TYPE=CHECKBOX NAME=\"connected\" VALUE=\"CHECKED\" "+params.getVal("connected")+">connected<BR>\n")
      +("<HR>\n")
      +("<B>depictions:</B><BR>\n")
      +("size:"+size_menu+"<BR>\n")
      +("<HR>\n")
      +("<B>misc:</B><BR>\n")
      +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
      +("</TD></TR></TABLE>\n")
      +("<P>\n")
      +("<CENTER>")
      +("<BUTTON TYPE=BUTTON onClick=\"go_mcs(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
      +("</CENTER>\n")
      +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void MCS_1xN(MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    ArrayList<Color> atomColors = new ArrayList<Color>();
    atomColors.add(new Color(0x000000));	//black
    atomColors.add(new Color(0xFF0000));	//red

    int w=sizes_w.get(params.getVal("size"));
    int h=sizes_h.get(params.getVal("size"));
    String depictopts=("mode=cow");
    depictopts+=("&imgfmt=png");
    params.setVal("dep_arom",params.getVal("arom"));
    if (params.getVal("dep_arom").equals("gen")) depictopts+=("&arom_gen=true");
    else if (params.getVal("dep_arom").equals("bas")) depictopts+=("&arom_bas=true");
    else if (params.getVal("dep_arom").equals("none")) depictopts+=("&kekule=true");

    // This is our convention; Apache proxies the 8080 port via /tomcat.
    String mol2img_servleturl=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/mol2img");

    String smifmt="smiles:";
    String mrvfmt="mrv:-H";
    if (params.getVal("arom").equals("gen")) { smifmt+="+a"; mrvfmt+="+a"; }
    else if (params.getVal("arom").equals("bas")) { smifmt+="+a_bas"; mrvfmt+="+a_bas"; }
    else if (params.getVal("arom").equals("none")) { smifmt+="-a"; mrvfmt+="-a"; }

    if (molQ.getDim()!=2) molQ.clean(2,null,null);

    String smiQ=MolExporter.exportToFormat(molQ,smifmt);
    String mrvcodeQ=MolExporter.exportToFormat(molQ,"base64:gzip:"+mrvfmt);
    String imghtmQ=HtmUtils.Mrvcode2ImgHtm(mrvcodeQ,atomColors,depictopts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");

    if (!params.isChecked("show_query"))
    {
      outputs.add("<B>query: "+molQ.getName()+"</B>");
      outputs.add("<BLOCKQUOTE>"+imghtmQ+"</BLOCKQUOTE>");
    }
    if (params.isChecked("verbose"))
      errors.add("query: "+smiQ+" "+molQ.getName()+" (natoms="+molQ.getAtomCount()+",nbonds="+molQ.getBondCount()+")");

    String tablehtm=("<TABLE BORDER>\n");
    tablehtm+="<TR>";
    if (params.isChecked("show_query")) tablehtm+="<TH>query</TH>";
    tablehtm+="<TH>mol</TH><TH>mcs</TH>";
    tablehtm+="</TR>\n";

    McsSearchOptions mcsOpts = new McsSearchOptions.Builder()
	.connectedMode(params.isChecked("connected"))
	.minFragmentSize(Integer.parseInt(params.getVal("minsize")))
	.chargeMatching(false)
	.isotopeMatching(false)
	.build();

    MaxCommonSubstructure mcs = MaxCommonSubstructure.newInstance(mcsOpts);

    if (params.getVal("searchmode").equals("fast")) mcs.setSearchMode(SearchMode.FAST);
    else mcs.setSearchMode(SearchMode.NORMAL);

    mcs.setQuery(molQ);

    Molecule mol = null;
    int n_mols=0;
    int n_failed=0;
    while (true)
    {
      try { mol=molReader.read(); }
      catch (MolFormatException e)
      { errors.add(SERVLETNAME+"ERROR: MolImporter failed: "+e.getMessage()); ++n_failed; continue; }
      if (mol==null) break;
   
      if (arom!=null)
        mol.aromatize(arom);
      else
        mol.dearomatize();

      if (mol.getDim()!=2) mol.clean(2,null,null);

      // Check for error "Target molecule cannot contain query features, unless
      // exact query atom/bond matching option is used or atom/bond types are
      // ignored."
      try {
        mcs.setTarget(mol);
      }
      catch (Exception e) {
        errors.add("ERROR: Exception: "+e.getMessage());
        ++n_failed;
        continue;
      }

      McsSearchResult mcs_result = mcs.nextResult();

      boolean ok=molalign_utils.AlignToMCS(mcs,mcs_result); //before getAsMolecule() so molMCS aligned.

      Molecule molMCS=mcs_result.getAsMolecule();
      String smiMCS=molMCS.toFormat(smifmt);

      Enum<TerminationCause> status=mcs.getTerminationCause();
      if (params.isChecked("verbose"))
      {
        if      (status==TerminationCause.FINISHED)   errors.add("TerminationCause.FINISHED");
        else if (status==TerminationCause.OPTIMAL)    errors.add("TerminationCause.OPTIMAL");
        else if (status==TerminationCause.TIME_LIMIT) errors.add("TerminationCause.TIME_LIMIT");
        errors.add("mol: "+mol.toFormat(smifmt)+" "+mol.getName()+" (natoms="+mol.getAtomCount()+",nbonds="+mol.getBondCount()+")");
        errors.add("MCS: "+smiMCS+" (natoms="+molMCS.getAtomCount()+",nbonds="+molMCS.getBondCount()+")");
        errors.add("MCS component count: "+mcs_result.getFragmentCount());


        boolean is_mmp=mcs_utils.isMatchedPair(mcs,mcs_result);
        if (is_mmp)
          errors.add("matched molecular pair candidates: "+molQ.getName()+" &amp; "+mol.getName());
      }

      // highlighting the MCS match
      // PROBLEM with JChem version: 17.1.9.0 -- FIX!
      try {
        for (MolAtom atom: molMCS.getAtomArray())  atom.setSetSeq(0);
        for (MolBond bond: molMCS.getBondArray())  bond.setSetSeq(0);
        for (Integer i: mcs_result.getMatchedQueryAtoms()) molMCS.getAtom(i).setSetSeq(1);
        for (Integer i: mcs_result.getMatchedQueryBonds()) molMCS.getBond(i).setSetSeq(1);
        for (Integer i: mcs_result.getMatchedTargetAtoms()) molMCS.getAtom(i).setSetSeq(1);
        for (Integer i: mcs_result.getMatchedTargetBonds()) molMCS.getBond(i).setSetSeq(1);
      }
      catch (Exception e) {
        errors.add("ERROR: Exception: "+e.getMessage());
      }

      String opts=depictopts;
      opts+=("&atomcolors=TRUE&color0=000000&color1=FF0000");

      String smi=MolExporter.exportToFormat(mol,smifmt);
      String mrvcode=MolExporter.exportToFormat(mol,"base64:gzip:"+mrvfmt);

      tablehtm+="<TR>";
      if (params.isChecked("show_query"))
      {
        mrvcodeQ=MolExporter.exportToFormat(molQ,"base64:gzip:"+mrvfmt);
        imghtmQ=HtmUtils.Mrvcode2ImgHtm(mrvcodeQ,atomColors,depictopts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
        tablehtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
        tablehtm+=(imghtmQ+"<BR>\n");
        tablehtm+=("<TT>"+molQ.getName()+"</TT></TD>\n");
      }
      tablehtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
      String imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcode,atomColors,opts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
      tablehtm+=(imghtm+"<BR>\n");
      tablehtm+="<TT>"+mol.getName()+"</TT></TD>\n";

      if (molMCS!=null && molMCS.getAtomCount()>0)
      {
        String mrvcodeMCS=MolExporter.exportToFormat(molMCS,"base64:gzip:"+mrvfmt);
        if (status==TerminationCause.OPTIMAL)
          tablehtm+=("<TD ALIGN=CENTER VALIGN=TOP>");
        else if (status==TerminationCause.FINISHED)
          tablehtm+=("<TD BGCOLOR=\"#F0FF00\" ALIGN=CENTER VALIGN=TOP>");
        else	// TIME_LIMIT
          tablehtm+=("<TD BGCOLOR=\"#FF8888\" ALIGN=CENTER VALIGN=TOP>");
        imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcodeMCS,atomColors,depictopts,h,w,
				mol2img_servleturl,true,4,
				"go_zoom_mrv2img");
        tablehtm+=(imghtm);
        tablehtm+=("<BR>\nna="+molMCS.getAtomCount()+",nb="+molMCS.getBondCount());
      }
      else
      {
        tablehtm+=("<TD ALIGN=CENTER>");
        tablehtm+=("~");
        ++n_failed;
        errors.add("ERROR: MCS failed for ["+(n_mols+1)+"] \""+mol.getName()+"\"");
      }
      tablehtm+="</TD>\n";
      tablehtm+=("</TR>\n");
      ++n_mols;
    }
    tablehtm+="</TABLE>\n";

    outputs.add("RESULT: mols processed: "+n_mols+"  mols failed: "+n_failed);
    outputs.add(tablehtm);

    PrintWriter out_log=new PrintWriter(
      new BufferedWriter(new FileWriter(logfile,true)));
    out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,n_mols); 
    out_log.close();
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void MCS_NxN(MultipartRequest mrequest,HttpServletResponse response,HttpParams params)
      throws IOException,ServletException
  {
    ArrayList<Color> atomColors = new ArrayList<Color>();
    atomColors.add(new Color(0x000000));	//black
    atomColors.add(new Color(0xFF0000));	//red

    int w=sizes_h.get(params.getVal("size"));
    int h=sizes_h.get(params.getVal("size"));
    String depictopts=("mode=cow");
    depictopts+=("&imgfmt=png");
    params.setVal("dep_arom",params.getVal("arom"));
    if (params.getVal("dep_arom").equals("gen")) depictopts+=("&arom_gen=true");
    else if (params.getVal("dep_arom").equals("bas")) depictopts+=("&arom_bas=true");
    else if (params.getVal("dep_arom").equals("none")) depictopts+=("&kekule=true");

    // This is our convention; Apache proxies the 8080 port via /tomcat.
    String mol2img_servleturl=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/mol2img");

    String smifmt="smiles:";
    String mrvfmt="mrv:-H";
    if (params.getVal("arom").equals("gen")) { smifmt+="+a"; mrvfmt+="+a"; }
    else if (params.getVal("arom").equals("bas")) { smifmt+="+a_bas"; mrvfmt+="+a_bas"; }
    else if (params.getVal("arom").equals("none")) { smifmt+="-a"; mrvfmt+="-a"; }

    int N=Math.min(mols.size(),N_MAX_MATRIX);
    MaxCommonSubstructure [][] mcses = new MaxCommonSubstructure[N][N];
    McsSearchResult [][] mcs_results = new McsSearchResult[N][N];

    int n_errors=0;
    for (int i=0;i<N;++i)
    {
      Molecule molQ=mols.get(i);
      if (molQ.getDim()!=2) molQ.clean(2,null,null);
      for (int j=0;j<N;++j)
      {
        if (i==j)
        {
          continue;
        }
        Molecule molT=mols.get(j);
        if (molT.getDim()!=2) molT.clean(2,null,null);

        McsSearchOptions mcsOpts = new McsSearchOptions.Builder()
	.connectedMode(params.isChecked("connected"))
	.minFragmentSize(Integer.parseInt(params.getVal("minsize")))
	.chargeMatching(false)
	.isotopeMatching(false)
	.build();

        MaxCommonSubstructure mcs = MaxCommonSubstructure.newInstance(mcsOpts);


        mcses[i][j]=mcs;

        if (params.getVal("searchmode").equals("fast")) mcs.setSearchMode(SearchMode.FAST);
        else mcs.setSearchMode(SearchMode.NORMAL);
        mcs.setQuery(molQ);
        mcs.setTarget(molT);

        McsSearchResult mcs_result = mcs.nextResult();

        mcs_results[i][j]=mcs_result;

        Molecule molMCS=mcs_result.getAsMolecule();
        if (molMCS==null)
        {
          ++n_errors;
          errors.add("ERROR: MCS failed for ["+(i+1)+"]["+(j+1)+"]");
          continue;
        }
        String smiMCS=molMCS.toFormat(smifmt);
        Enum<TerminationCause> status=mcs.getTerminationCause();
        if (params.isChecked("verbose"))
        {
          if      (status==TerminationCause.FINISHED)   errors.add("TerminationCause.FINISHED");
          else if (status==TerminationCause.OPTIMAL)    errors.add("TerminationCause.OPTIMAL");
          else if (status==TerminationCause.TIME_LIMIT) errors.add("TerminationCause.TIME_LIMIT");
          errors.add("mol: "+molT.toFormat(smifmt)+" "+molT.getName()+" (natoms="+molT.getAtomCount()+",nbonds="+molT.getBondCount()+")");
          errors.add("MCS: "+smiMCS+" (natoms="+molMCS.getAtomCount()+",nbonds="+molMCS.getBondCount()+")");
          errors.add("MCS component count: "+mcs_result.getFragmentCount());
        }
      }
    }
    outputs.add("RESULT: mols processed: "+N+"  errors: "+n_errors);

    String tablehtm=("<TABLE BORDER>\n");
    tablehtm+="<TR><TH COLSPAN=2 ROWSPAN=2>&nbsp;</TH>";
    for (int i=0;i<N;++i)
      tablehtm+=("<TH>"+HtmUtils.HtmTipper("<B>"+(i+1)+"</B>",mols.get(i).getName(),w,"white")+"</TH>\n");
    tablehtm+="</TR>\n";
    tablehtm+="<TR>";
    String opts;
    for (int i=0;i<N;++i)
    {
      String mrvcode=MolExporter.exportToFormat(mols.get(i),"base64:gzip:"+mrvfmt);
      //opts=depictopts+"&bgcolor=#DDDDDD";
      opts=depictopts+"&transparent=TRUE";
      String imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcode,null,opts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
      tablehtm+=("<TD ALIGN=CENTER VALIGN=TOP>"+imghtm+"</TD>\n");
    }
    tablehtm+="</TR>\n";

    for (int i=0;i<N;++i)
    {
      String rowhtm=("<TR><TH>"+HtmUtils.HtmTipper("<B>"+(i+1)+"</B>",mols.get(i).getName(),w,"white")+"</TH>\n");
      Molecule molQ=mols.get(i);
      if (molQ.getDim()!=2) molQ.clean(2,null,null);
      String mrvcode=MolExporter.exportToFormat(molQ,"base64:gzip:"+mrvfmt);
      //opts=depictopts+"&bgcolor=#DDDDDD";
      opts=depictopts+"&transparent=TRUE";
      String imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcode,null,opts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
      rowhtm+=("<TD ALIGN=CENTER VALIGN=TOP>"+imghtm+"</TD>\n");
      for (int j=0;j<N;++j)
      {
        if (i==j)
        {
          rowhtm+=("<TD>&nbsp;</TD>\n");
          continue;
        }
        Molecule molT=mols.get(j);
        MaxCommonSubstructure mcs = mcses[i][j];
        Molecule molMCS=mcs_results[i][j].getAsMolecule();
        if (molMCS==null)
        {
          rowhtm+=("<TD ALIGN=CENTER>~</TD>\n");
          continue;
        }
        else if (molMCS.getAtomCount()==0)
        {
          rowhtm+=("<TD ALIGN=CENTER>~</TD>\n");
        }
        else
        {
          if (params.getVal("depictNxN").equals("mcs"))
          {
            mrvcode=MolExporter.exportToFormat(molMCS,"base64:gzip:"+mrvfmt);
            imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcode,null,depictopts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
          }
          else
          {
            // highlighting the MCS match
            // PROBLEM with JChem version: 17.1.9.0 -- FIX!
            try {
              for (MolAtom atom: molMCS.getAtomArray())  atom.setSetSeq(0);
              for (MolBond bond: molMCS.getBondArray())  bond.setSetSeq(0);
              for (Integer ia: mcs_results[i][j].getMatchedQueryAtoms()) molMCS.getAtom(ia).setSetSeq(1);
              for (Integer ia: mcs_results[i][j].getMatchedQueryBonds()) molMCS.getBond(ia).setSetSeq(1);
              for (Integer ia: mcs_results[i][j].getMatchedTargetAtoms()) molMCS.getAtom(ia).setSetSeq(1);
              for (Integer ia: mcs_results[i][j].getMatchedTargetBonds()) molMCS.getBond(ia).setSetSeq(1);
            }
            catch (Exception e) {
              errors.add("ERROR: Exception: "+e.getMessage());
            }

            mrvcode=MolExporter.exportToFormat(molT,"base64:gzip:"+mrvfmt);
            opts=depictopts+"&atomcolors=TRUE&color0=000000&color1=FF0000";
            imghtm=HtmUtils.Mrvcode2ImgHtm(mrvcode,atomColors,opts,h,w,
				mol2img_servleturl,
				true,4,
				"go_zoom_mrv2img");
          }
          rowhtm+=("<TD ALIGN=CENTER VALIGN=TOP>"+imghtm+"</TD>\n");
        }
      }
      rowhtm+="</TR>\n";
      tablehtm+=rowhtm;
    }
    tablehtm+="</TABLE>\n";
    outputs.add(tablehtm);

    PrintWriter out_log=new PrintWriter(
      new BufferedWriter(new FileWriter(logfile,true)));
    out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,N); 
    out_log.close();
    return;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
	throws IOException
  {
    String js="var demotxt='';";
    if (DEMOSMIFILE!=null) {
      BufferedReader buff=new BufferedReader(new FileReader(DEMOSMIFILE));
      String line=null;
      String startdate=null;
      while ((line=buff.readLine())!=null)
        js+=("demotxt+='"+line+"\\n';\n");
      buff.close();
    }
    js+=(
"function go_init(form)"+
"{\n"+
"  form.file2txtQ.checked=false;\n"+
"  form.file2txtDB.checked=false;\n"+
"  form.show_query.checked=true;\n"+
"  form.connected.checked=false;\n"+
"  form.minsize.value='"+McsSearchOptions.getDefault().getMinFragmentSize()+"';\n"+
"  form.intxtQ.value='';\n"+
"  form.intxtDB.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.ifmtQ.length;++i)\n"+
"    if (form.ifmtQ.options[i].value=='automatic')\n"+
"      form.ifmtQ.options[i].selected=true;\n"+
"  for (i=0;i<form.ifmtDB.length;++i)\n"+
"    if (form.ifmtDB.options[i].value=='automatic')\n"+
"      form.ifmtDB.options[i].selected=true;\n"+
"  for (i=0;i<form.size.length;++i)\n"+
"    if (form.size.options[i].value=='s')\n"+
"      form.size.options[i].selected=true;\n"+
"  for (i=0;i<form.arom.length;++i)\n"+ //radio
"    if (form.arom[i].value=='none')\n"+
"      form.arom[i].checked=true;\n"+
"  for (i=0;i<form.depictNxN.length;++i)\n"+ //radio
"    if (form.depictNxN[i].value=='mcs')\n"+
"      form.depictNxN[i].checked=true;\n"+
"  for (i=0;i<form.runmode.length;++i)\n"+ //radio
"    if (form.runmode[i].value=='1xN')\n"+
"      form.runmode[i].checked=true;\n"+
"  for (i=0;i<form.searchmode.length;++i)\n"+ //radio
"    if (form.searchmode[i].value=='normal')\n"+
"      form.searchmode[i].checked=true;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxtDB.value && !form.infileDB.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"function go_demo(form) {\n"+
"  go_init(form);\n"+
"  form.intxtDB.value=demotxt;\n"+
"  form.mcs.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"+
"function go_mcs(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.mcs.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"
    );
    return(js);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    return (
    "<H2>"+APPNAME+" Help</H2>\n"+
    "<P>\n"+
    "MCS = Maximum Common Substructure (or Subgraph).  More precisely, MCES,\n"+
    "Maximum Common Edge Subgraph.  This algorithm was released in\n"+
    "JChem v5.4.0 and prior to that, suggested, implemented, and presented \n"+
    "at the 2009 US UGM by Oleg Ursu.  A distinctive feature is the ability\n"+
    "to perceive disconnected MCES, for example, two scaffolds separated by.\n"+
    "differing linkers.\n"+
    "<P>\n"+
    "In 1xN mode if no query is provided, the 1st molecule is used as the query.\n"+
    "<P>\n"+
    "Configured limits<UL>\n"+
    "<LI>N_MAX = "+N_MAX+"\n"+
    "<LI>N_MAX_MATRIX = "+N_MAX_MATRIX+"\n"+
    "</UL>\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "author: Jeremy Yang\n"+
    "\n");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    //CONFIG=conf;
    // read servlet parameters (from web.xml):
    try { APPNAME=conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME=this.getServletName(); }
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    LOGDIR=conf.getInitParameter("LOGDIR")+CONTEXTPATH;
    if (LOGDIR==null) LOGDIR="/usr/local/tomcat/logs"+CONTEXTPATH;
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    DEMOSMIFILE=CONTEXT.getRealPath("")+"/data/"+conf.getInitParameter("DEMOSMIFILE");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}
