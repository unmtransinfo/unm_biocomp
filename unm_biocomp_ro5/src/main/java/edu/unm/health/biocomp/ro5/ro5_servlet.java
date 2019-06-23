package edu.unm.health.biocomp.ro5;

import java.io.*;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.*; //MultipartRequest, ParameterParser
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.struc.prop.MMoleculeProp;
import chemaxon.sss.search.*;
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.util.*; //time_utils,math_utils
import edu.unm.health.biocomp.util.http.*;

/**	Lipinsky Rule of 5 analysis.
	See Help() for list of descriptors.

	@author Jeremy J Yang
*/
public class ro5_servlet extends HttpServlet
{
  private static ResourceBundle rb=null;
  //private static ServletConfig CONFIG=null;
  private static String SERVLETNAME=null;
  private static ServletContext CONTEXT=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;	// configured in web.xml
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static Integer N_MAX=100;	// configured in web.xml
  private static Integer MAX_POST_SIZE=null;	// configured in web.xml
  private static int scratch_retire_sec=3600;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static String smi2img_url=null;
  private static String barchartimg_url=null;
  private static String histoimg_url=null;
  private static HttpParams params=null;
  private static ArrayList<Molecule> mols=null;
  private static byte[] inbytes=null;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String datestr=null;
  private static File LOGFILE=null;
  private static String PREFIX=null;
  private static String ofmt="";
  private static String color1="#EEEEEE";

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    //serverport=request.getServerPort();
    SERVERNAME=request.getServerName();
    if (SERVERNAME.equals("localhost")) SERVERNAME=InetAddress.getLocalHost().getHostAddress();
    REMOTEHOST=request.getHeader("X-Forwarded-For"); // client (original)
    if (REMOTEHOST!=null)
    {
      String[] addrs=java.util.regex.Pattern.compile(",").split(REMOTEHOST);
      if (addrs.length>0) REMOTEHOST=addrs[addrs.length-1];
    }
    else
    {
      // REMOTEHOST=request.getRemoteHost(); // client (may be proxy)
      REMOTEHOST=request.getRemoteAddr(); // client (may be proxy)
    }
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try
      {
        mrequest=new MultipartRequest(request,UPLOADDIR,MAX_POST_SIZE,"ISO-8859-1",
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
      out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
      out.print(HtmUtils.FooterHtm(errors,true));
      return;
    }
    if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("ro5").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(FormHtm(mrequest,response));
        Date t_0 = new Date();
        Ro5Results results=null;
        try {
          results=ro5_utils.Ro5_Calculate(mols);
          outputs.add(Ro5_ResultsHtm(mols,results,params,response));
          errors.add("LOGP calculated by: "+results.getLogpProgram());
        }
        catch (Exception e) { errors.add("ERROR: "+e.toString()); }//catches LicenseException

        PrintWriter out_log=new PrintWriter(
          new BufferedWriter(new FileWriter(LOGFILE,true)));
        out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,mols.size()); 
        out_log.close();

        errors.add(SERVLETNAME+": elapsed time: "+time_utils.TimeDeltaStr(t_0,new Date()));
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
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
      }
      else if (request.getParameter("test")!=null)	// GET method, test=TRUE
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
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request, "tomcat"));
        out.println(FormHtm(mrequest,response));
        out.println("<SCRIPT LANGUAGE=\"JavaScript\">");
        out.println("go_reset(window.document.mainform);");
        out.println("</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request,
                                    MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs=new ArrayList<String>();
    errors=new ArrayList<String>();
    params=new HttpParams();
    mols=new ArrayList<Molecule>();
    Calendar calendar=Calendar.getInstance();

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\"/tomcat"+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));

    //logo_htm+="</TD><TD>";
    //imghtm=("<IMG BORDER=0 HEIGHT=\"60\" SRC=\"/tomcat"+CONTEXTPATH+"/images/eADMET_logo.png\">");
    //tiphtm=("ALOGPS from VCCLAB and eADMET.");
    //href=("http://www.eadmet.com");
    //logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));

    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=\"0\" HEIGHT=\"60\" SRC=\"/tomcat"+CONTEXTPATH+"/images/cdk_logo.png\">");
    tiphtm=("CDK");
    href=("http://sourceforge.net/projects/cdk/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));

    logo_htm+="</TD></TR></TABLE>";
    errors.add("<CENTER>"+logo_htm+"</CENTER>");

    // This is our convention; Apache proxies the 8080 port via /tomcat.
    smi2img_url=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/mol2img");
    barchartimg_url=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/barchartimg");
    histoimg_url=("http://"+SERVERNAME+"/tomcat"+CONTEXTPATH+"/histoimg");

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
    LOGFILE=new File(logpath);
    if (!LOGFILE.exists())
    {
      LOGFILE.createNewFile();
      LOGFILE.setWritable(true,true);
      PrintWriter out_log=new PrintWriter(LOGFILE);
      out_log.println("date\tip\tN"); 
      out_log.flush();
      out_log.close();
    }
    if (!LOGFILE.canWrite())
    {
      errors.add("ERROR: Log file not writable.");
      return false;
    }
    BufferedReader buff=new BufferedReader(new FileReader(LOGFILE));
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
      String[] fields=java.util.regex.Pattern.compile("\\t").split(line);
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

    //Really needed?  Yes.  "Structure Search"
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

      mol.aromatize(MoleculeGraph.AROM_GENERAL);

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

    String vmax_0=""; String vmax_1=""; String vmax_2=""; String vmax_3="";
    if (params.getVal("vmax").equals("0")) vmax_0="CHECKED";
    else if (params.getVal("vmax").equals("1")) vmax_1="CHECKED";
    else if (params.getVal("vmax").equals("2")) vmax_2="CHECKED";
    else if (params.getVal("vmax").equals("3")) vmax_3="CHECKED";
    else vmax_1="CHECKED";

    String outfmt_smiles=""; String outfmt_sdf=""; 
    if (params.getVal("outfmt").equals("smiles")) outfmt_smiles="CHECKED";
    else if (params.getVal("outfmt").equals("sdf")) outfmt_sdf="CHECKED";

    String htm=""
    +("<FORM NAME=\"mainform\" METHOD=POST")
    +(" ACTION=\""+response.encodeURL(SERVLETNAME)+"\"")
    +(" ENCTYPE=\"multipart/form-data\">\n")
    +("<TABLE WIDTH=\"100%\"><TR><TD><H2>"+APPNAME+"</H2></TD><TD>- Lipinsky Rule of 5 analysis\n")
    +("<TD ALIGN=RIGHT>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
    +("</TD></TR></TABLE>\n")
    +("<HR>\n")
    +("<INPUT TYPE=HIDDEN NAME=\"ro5\">\n")
    +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
    +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP>\n")
    +("<B>input:</B> fmt:"+molfmt_menu)
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:")
    +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>"+params.getVal("intxt")+"</TEXTAREA>\n")
    +("</TD>\n")
    +("<TD VALIGN=TOP>\n")
    +("<B>analysis:</B><BR>\n")
    +("max violations:<INPUT TYPE=RADIO NAME=\"vmax\" VALUE=\"0\" "+vmax_0+">0")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"vmax\" VALUE=\"1\" "+vmax_1+">1")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"vmax\" VALUE=\"2\" "+vmax_2+">2")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"vmax\" VALUE=\"3\" "+vmax_3+">3")
    +("<HR>\n")
    +("<B>output:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"batchout\" VALUE=\"CHECKED\" "+params.getVal("batchout")+">batch<BR>\n")
    +("&nbsp;&nbsp;<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"smiles\" "+outfmt_smiles+">smiles")
    +("&nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME=\"smiheader\" VALUE=\"CHECKED\" "+params.getVal("smiheader")+">+header<BR>\n")
    +("&nbsp;&nbsp;<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"sdf\" "+outfmt_sdf+">sdf<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"viewout_detail\" VALUE=\"CHECKED\" "+params.getVal("viewout_detail")+">expanded view<BR>\n")
    +("&nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME=\"depict\" VALUE=\"CHECKED\" "+params.getVal("depict")+">depict\n")
    +("&nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME=\"depict_arom\" VALUE=\"CHECKED\" "+params.getVal("depict_arom")+">+arom<BR>\n")
    +("&nbsp;&nbsp;<INPUT TYPE=CHECKBOX NAME=\"histo_show\" VALUE=\"CHECKED\" "+params.getVal("histo_show")+">histograms\n")
    +("<HR>\n")
    +("<B>misc:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose")
    +("<BR>\n")
    +("</TD></TR></TABLE>\n")
    +("<P>\n")
    +("<CENTER>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_ro5(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
    +("</CENTER>\n")
    +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate histogram or barchart HTML.  Barcharts are used for integral
	variables hba and hbd.
  */
  private static String Ro5_ChartHtm(Ro5Results results,String field,int w,int h)
      throws Exception
  {
    ArrayList<Double> dvals = new ArrayList<Double>(); //Double vals
    ArrayList<Integer> ivals = new ArrayList<Integer>(); //Integer vals
    for (Ro5Result result: results)
    {
      if (field.equalsIgnoreCase("mwt")) dvals.add(result.getMwt());
      else if (field.equalsIgnoreCase("logp") && result.getLogp()!=null) dvals.add((double)result.getLogp());
      else if (field.equalsIgnoreCase("hbd")) ivals.add(result.getHbd());
      else if (field.equalsIgnoreCase("hba")) ivals.add(result.getHba());
    }
    int nbins=10;
    ArrayList<Double> xmaxs = new ArrayList<Double>(nbins);
    ArrayList<Integer> freqs = new ArrayList<Integer>(nbins);

    String opts="&title="+field+"&fgcolor=x0088CC";
    String charthtm="";

    if (field.equalsIgnoreCase("mwt")||field.equalsIgnoreCase("logp")) 
    {
      math_utils.HistoAnalyze(dvals,nbins,xmaxs,freqs);
      if (freqs.size()!=xmaxs.size())
        errors.add("DEBUG: freqs.size()!=xmaxs.size(): "+freqs.size()+"!="+xmaxs.size());
      charthtm=HtmUtils.HistoImgHtm(freqs,xmaxs,opts,w,h,histoimg_url,true,4,"go_zoom_chartimg");
    }
    else if (field.equalsIgnoreCase("hbd")||field.equalsIgnoreCase("hba")) 
    {
      Integer[] range = new Integer[2];
      math_utils.BarchartAnalyze(ivals,range,freqs);
      ArrayList<String> labels = new ArrayList<String>();
      for (int i=range[0];i<=range[1];++i)
        labels.add(""+i);
      charthtm=HtmUtils.BarchartImgHtm(freqs,labels,opts,w,h,barchartimg_url,true,4,"go_zoom_chartimg");
    }
    return charthtm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate summary statistics and charts.
  */
  private static String Ro5_ResultsSummaryHtm(Ro5Results results,HttpParams params,HttpServletResponse response)
      throws IOException
  {
    Integer[] vdist = {0,0,0,0,0};
    for (Ro5Result result: results) ++vdist[result.violations()];

    String[] colors = {"#AAFFAA","#FAF000","#FFAAAA","#FFAAAA","#FFAAAA"};
    String thtm0=("<TABLE BORDER CELLPADDING=2 CELLSPACING=2>\n");
    thtm0+=("<TR><TH WIDTH=\"50%\">Ro5 violations</TH><TH WIDTH=\"50%\">#mols</TH></TR>\n");
    for (int i=0;i<=4;++i)
      thtm0+=("<TR><TH>"+i+"</TH><TD BGCOLOR=\""+colors[i]+"\" ALIGN=\"center\">"+vdist[i]+"</TD></TR>\n");
    thtm0+=("</TABLE>");

    String thtm1=
       ("<TABLE CELLPADDING=2 CELLSPACING=2>\n")
      +("<TR><TH>property</TH><TH>min</TH><TH>max</TH><TH>mean</TH><TH>median</TH><TH>std</TH>\n")
      +("<TH>violations</TH><TH></TH></TR>")
      +("<TR><TD HEIGHT=\"80\" ALIGN=\"center\"><B>MWT</B></TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMinMwt())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMaxMwt())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMeanMwt())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getPercentileMwt(50))+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getStdMwt())+"</TD>\n")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getViolationsMwt()+"</TD>");
    String charthtm="";
    try { charthtm = Ro5_ChartHtm(results,"mwt",120,70); }
    catch (Exception e) { charthtm="(error)"; errors.add("ERROR: "+e.toString()); }
    thtm1+=
       ("<TD BGCOLOR=\"white\" ALIGN=\"center\">"+charthtm+"</TD>\n")
      +("</TR>")
      +("<TR><TD HEIGHT=\"80\" ALIGN=\"center\"><B>HBA</B></TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getMinHba()+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getMaxHba()+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMeanHba())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+Math.round(results.getPercentileHba(50))+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getStdHba())+"</TD>\n")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getViolationsHba()+"</TD>");
    try { charthtm = Ro5_ChartHtm(results,"hba",120,70); }
    catch (Exception e) { charthtm="(error)"; errors.add("ERROR: "+e.toString()); }
    thtm1+=
       ("<TD BGCOLOR=\"white\" ALIGN=\"center\">"+charthtm+"</TD>\n")
      +("</TR>")
      +("<TR><TD HEIGHT=\"80\" ALIGN=\"center\"><B>HBD</B></TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getMinHbd()+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getMaxHbd()+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMeanHbd())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+Math.round(results.getPercentileHbd(50))+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getStdHbd())+"</TD>\n")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getViolationsHbd()+"</TD>");
    try { charthtm = Ro5_ChartHtm(results,"hbd",120,70); }
    catch (Exception e) { charthtm="(error)"; errors.add("ERROR: "+e.toString()); }
    thtm1+=
       ("<TD BGCOLOR=\"white\" ALIGN=\"center\">"+charthtm+"</TD>\n")
      +("</TR>")
      +("<TR><TD HEIGHT=\"80\" ALIGN=\"center\"><B>LOGP</B></TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMinLogp())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMaxLogp())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getMeanLogp())+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getPercentileLogp(50))+"</TD>")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+String.format("%.1f",results.getStdLogp())+"</TD>\n")
      +("<TD ALIGN=\"center\" BGCOLOR=\"white\">"+results.getViolationsLogp()+"</TD>");
    try { charthtm = Ro5_ChartHtm(results,"logp",120,70); }
    catch (Exception e) { charthtm="(error)"; errors.add("ERROR: "+e.toString()); }
    thtm1+=
       ("<TD BGCOLOR=\"white\" ALIGN=\"center\">"+charthtm+"</TD>\n")
      +("</TR>")
      +("</TABLE>");

    String thtm=("<TABLE CELLPADDING=5 CELLSPACING=5><TR><TD VALIGN=\"top\">"+thtm0+"</TD><TD VALIGN=\"top\">"+thtm1+"</TD></TR></TABLE>");
    return thtm;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String Ro5_ResultsHtm(ArrayList<Molecule> mols,
	Ro5Results results,HttpParams params,HttpServletResponse response)
  {

    String htm="";
    int w_dep=96;
    int h_dep=96;
    String depopts=("mode=cow");
    depopts+=("&imgfmt=png");
    if (params.isChecked("depict_arom")) depopts+=("&arom_gen=true");
    else depopts+=("&kekule=true");

    String[] dataFields={ "mwt","hbd","hba","logp" };

    Integer vmax = null;
    try { vmax = Integer.parseInt(params.getVal("vmax")); }
    catch (Exception e) { vmax = 1; }

    Integer[] vdist = {0,0,0,0,0};
    int n_fail=0;
    for (Ro5Result result: results)
    {
      int viols=result.violations();
      if (viols>vmax)
        ++n_fail;
      ++vdist[viols];
    }
    for (int i=0;i<=4;++i)
      errors.add("&nbsp; "+i+" Ro5-violation mols: "+vdist[i]);

    htm+=("<H2>Results:</H2>\n");
    String thtm="<TABLE CELLSPACING=\"3\" CELLPADDING=\"3\" WIDTH=\"20%\">\n";
    thtm+=("<TR><TD ALIGN=\"right\">mols processed:</TD><TD BGCOLOR=\"white\">"+mols.size()+"</TD></TR>\n");
    thtm+=("<TR><TD ALIGN=\"right\">mols passed:</TD><TD BGCOLOR=\"white\">"+(mols.size()-n_fail)+"</TD></TR>\n");
    thtm+=("<TR><TD ALIGN=\"right\">mols failed:</TD><TD BGCOLOR=\"white\">"+n_fail+"</TD></TR>\n");
    thtm+=("<TR><TD COLSPAN=\"2\">(Where failure defined as violations &gt; "+vmax+".)</TD></TR>\n");
    thtm+="</TABLE>\n";
    htm+=("<BLOCKQUOTE>"+thtm+"</BLOCKQUOTE>\n");

    try {
      htm+=("<BLOCKQUOTE>"+Ro5_ResultsSummaryHtm(results,params,response)+"</BLOCKQUOTE>\n");
    } catch (IOException e) { errors.add("ERROR: "+e.toString()); }

    // for download mols:
    MolExporter molWriter=null;
    File fout=null;
    MolExporter molWriter_fail=null;
    File fout_fail=null;
    if (params.isChecked("batchout"))
    {
      ofmt=params.getVal("outfmt");
      if (params.getVal("outfmt").equals("smiles"))
      {
        ofmt+=":u"; //unique-smi
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
        if (!dout.exists())
        {
          boolean ok=dout.mkdir();
          System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
        }
        fout=File.createTempFile(PREFIX,"_pass."+params.getVal("outfmt"),dout);
        fout_fail=File.createTempFile(PREFIX,"_fail."+params.getVal("outfmt"),dout);
      }
      catch (IOException e) {
        errors.add("ERROR: cannot open file; check SCRATCHDIR: "+SCRATCHDIR);
        return htm;
      }
      try {
        molWriter=new MolExporter(new FileOutputStream(fout),ofmt);
        molWriter_fail=new MolExporter(new FileOutputStream(fout_fail),ofmt);
      } catch (IOException e) { errors.add("ERROR: "+e.toString()); }
    }

    thtm="";
    if (params.isChecked("viewout_detail")) 
    {
      thtm+=("<TABLE CELLPADDING=2 CELLSPACING=2>\n");
      thtm+=("<TR>\n<TH>&nbsp;</TH><TH>&nbsp;</TH>\n");
      if (params.isChecked("depict")) thtm+=("<TH>&nbsp;</TH>\n");
      for (String field:dataFields) { thtm+=("<TH>"+field+"</TH>\n"); }
      thtm+=("<TH>Ro5 violations</TH></TR>\n");
    }

    int N_MAX_VIEW=1000;
    for (int i_mol=0;i_mol<mols.size();++i_mol)
    {
      Molecule mol=mols.get(i_mol);
      Ro5Result result=results.get(i_mol);

      int viols=result.violations();
      try {
        if (viols>1)
          if (params.isChecked("batchout")) { molWriter_fail.write(mol); }
        else if (viols==1)
          if (params.isChecked("batchout")) { molWriter.write(mol); }
        else
          if (params.isChecked("batchout")) { molWriter.write(mol); }
      } catch (IOException e) { errors.add("ERROR: "+e.toString()); }

      if (params.isChecked("viewout_detail")) 
      {
        String rhtm=("<TR><TD ALIGN=RIGHT VALIGN=TOP>"+(i_mol+1)+". </TD>");
        if (params.isChecked("depict"))
        {
          rhtm+=("<TD BGCOLOR=\"white\" ALIGN=CENTER>");
          String imghtm=HtmUtils.Smi2ImgHtm(result.getSmiles(),depopts,h_dep,w_dep,smi2img_url,true,4,"go_zoom_smi2img");
          rhtm+=(imghtm+"</TD>\n");
          rhtm+=("<TD BGCOLOR=\"white\"><TT>"+result.getName()+"</TT></TD>\n");
        }
        else
        {
          rhtm+=("<TD BGCOLOR=\"white\">");
          String imghtm=HtmUtils.Smi2ImgHtm(result.getSmiles(),depopts,h_dep,w_dep,smi2img_url,false,4,null);
          rhtm+=("<TT>"+HtmUtils.HtmTipper(result.getName(),imghtm,w_dep,"white")+"</TT></TD>\n");
        }
        String bgcolor="#FFFFFF";
        String[] colors = { "#AAFFAA","#FAF000","#FFAAAA","#FFAAAA","#FFAAAA" };

        bgcolor=(result.isViolationMwt()?colors[viols]:"#FFFFFF");
        rhtm+=("<TD BGCOLOR=\""+bgcolor+"\" ALIGN=CENTER><TT>"+String.format("%.2f",result.getMwt())+"</TT></TD>\n");

        bgcolor=(result.isViolationHbd()?colors[viols]:"#FFFFFF");
        rhtm+=("<TD BGCOLOR=\""+bgcolor+"\" ALIGN=CENTER><TT>"+result.getHbd()+"</TT></TD>\n");

        bgcolor=(result.isViolationHba()?colors[viols]:"#FFFFFF");
        rhtm+=("<TD BGCOLOR=\""+bgcolor+"\" ALIGN=CENTER><TT>"+result.getHba()+"</TT></TD>\n");

        bgcolor=(result.isViolationLogp()?colors[viols]:"#FFFFFF");
        rhtm+=("<TD BGCOLOR=\""+bgcolor+"\" ALIGN=CENTER><TT>"+String.format("%.2f",result.getLogp())+"</TT></TD>\n");

        rhtm+=("<TD ALIGN=CENTER BGCOLOR=\""+colors[viols]+"\">"+viols+"</TD>\n");

        rhtm+="</TR>\n";
        if (i_mol+1<N_MAX_VIEW) { thtm+=rhtm; }
      }
    }
    thtm+=("</TABLE>");

    if (params.isChecked("batchout"))
    {
      String fname=(SERVLETNAME+"_pass."+params.getVal("outfmt"));
      String bhtm_pass=("&nbsp;"+
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "download "+fname+" ("+file_utils.NiceBytes(fout.length())+")</BUTTON>"+
      "<I>(passed mols + data in "+params.getVal("outfmt")+" format)</I></FORM>");

      fname=(SERVLETNAME+"_fail."+params.getVal("outfmt"));
      String bhtm_fail=("&nbsp;"+
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout_fail.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "download "+fname+" ("+file_utils.NiceBytes(fout_fail.length())+")</BUTTON>"+
      "<I>(failed mols + data in "+params.getVal("outfmt")+" format)</I></FORM>");

      htm+=("<H3>Downloads:</H3><BLOCKQUOTE>"+bhtm_pass+"<BR>\n"+bhtm_fail+"</BLOCKQUOTE>\n");
    }
    if (params.isChecked("viewout_detail")) 
    {
      htm+=("<H3>Detail:</H3>\n");
      htm+=("<BLOCKQUOTE>"+thtm+"</BLOCKQUOTE>\n");
      if (mols.size()>N_MAX_VIEW) 
        errors.add("NOTE: view truncated at N = "+N_MAX_VIEW);
    }
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript()
  {
    return(
"function go_reset(form)"+
"{\n"+
"  form.file2txt.checked=false;\n"+
"  form.depict.checked=true;\n"+
"  form.histo_show.checked=true;\n"+
"  form.depict_arom.checked=false;\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.molfmt.length;++i)\n"+
"    if (form.molfmt.options[i].value=='automatic')\n"+
"      form.molfmt.options[i].selected=true;\n"+
"  for (i=0;i<form.vmax.length;++i)\n"+ //radio
"    if (form.vmax[i].value=='1')\n"+
"      form.vmax[i].checked=true;\n"+
"  form.viewout_detail.checked=false;\n"+
"  form.batchout.checked=true;\n"+
"  form.smiheader.checked=true;\n"+
"  form.verbose.checked=false;\n"+
"  //form.vverbose.checked=false;\n"+
"  for (i=0;i<form.outfmt.length;++i)\n"+ //radio
"    if (form.outfmt[i].value=='smiles')\n"+
"      form.outfmt[i].checked=true;\n"+
"  for (i=0;i<form.viewmode.length;++i)\n"+ //radio
"    if (form.viewmode[i].value=='detail')\n"+
"      form.viewmode[i].checked=true;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return false;\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"//function fix_verbose(form,v)\n"+
"//{\n"+
"//  if (v==2&&form.vverbose.checked)\n"+
"//    form.verbose.checked=true;\n"+
"//  else if (v==1&&!form.verbose.checked)\n"+
"//    form.vverbose.checked=false;\n"+
"//}\n"+
"function go_ro5(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.ro5.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    return ("<H3>"+APPNAME+" help</H3>\n"+
    "<P>\n"+
    "<B>Theory:</B>\n"+
    "<P>\n"+
    "The Lipinski Rule of Five (Ro5) was formulated by Christopher A. Lipinski\n"+
    "and published in 1997, and is intended to guide library selection for\n"+
    "drug discovery.  The following molecular descriptors are used:\n"+
    "<P>\n"+
    "<UL>\n"+
    "<LI> mwt - avg molecular weight\n"+
    "<LI> hbd - hydrogen bond donor count\n"+
    "<LI> hba - hydrogen bond acceptor count\n"+
    "<LI> LogP - predicted octanol-water partition coefficient (XLOGP by CDK)\n"+
    "</UL>\n"+
    "<P>\n"+
    "The Ro5 specifies that compounds should violate no more than one of the\n"+
    "following criteria:\n"+
    "<UL>\n"+
    "<LI> mwt &lt;= 500\n"+
    "<LI> hbd &lt;= 5\n"+
    "<LI> hba &lt;= 10\n"+
    "<LI> LogP &lt; 5.0\n"+
    "</UL>\n"+
    "<P>\n"+
    "In this app, max violations is an adjustable parameter (vmax), with default set to 1.\n"+
    "<P>\n"+
    "It is generally understood that these criteria do not apply to biologics, nor to\n"+
    "transporter mediated targets.\n"+
    "<P>\n"+
    "The Ro5 represented a major conceptual advance and remains an influential standard\n"+
    "and practical tool for drug discovery library design.  Many investigators have since explored\n"+
    "concepts of drug-likeness, lead-likeness, and chemical-biology space,\n"+
    "all of which expands upon the Ro5 and the seminal Lipinski paper.\n"+
    "Although the Ro5 remains a practically useful tool, improved methods\n"+
    "exist, which often depend on the specific goals of the chemical\n"+
    "library design.  The UNM Division of Translational Informatics, led by Prof. Tudor Oprea,\n"+
    "has developed an extensive set of library design filters.  However, the\n"+
    "original Rule of Five remains important and useful, hence this web app\n"+
    "has been developed to accurately implement the Lipinski Ro5.\n"+
    "<P>\n"+
    "<B>Web app features:</B>\n"+
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
    "authors:\n"+
    "<UL>\n"+
    "<LI> Jeremy Yang (web app)\n"+
    "<LI> Oleg Ursu (HBD/HBA code)\n"+
    "</UL>\n"+
    "<P>\n"+
    "references:<UL>\n"+
    "<LI>C. A. Lipinski, F. Lombardo, B. W. Dominy, P. J. Feeney. Adv. Drug Deliv. Rev. 23, 3-25 (1997).\n"+
    "</UL>\n"
    );
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
      throw new ServletException("Please supply UPLOADDIR parameter (web.xml).");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR")+CONTEXTPATH;
    if (LOGDIR==null) LOGDIR="/usr/local/tomcat/logs"+CONTEXTPATH;
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    try { MAX_POST_SIZE=Integer.parseInt(conf.getInitParameter("MAX_POST_SIZE")); }
    catch (Exception e) { MAX_POST_SIZE=1*1024*1024; }
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}
