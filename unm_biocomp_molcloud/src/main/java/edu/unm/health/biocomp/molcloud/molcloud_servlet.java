package edu.unm.health.biocomp.molcloud;

import java.io.*;
import java.lang.Math;
import java.net.*; //URLEncoder,InetAddress;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;
import com.oreilly.servlet.*; //Base64Encoder, Base64Decoder

import org.apache.commons.math3.stat.descriptive.*; //DescriptiveStatistics
import org.apache.commons.math3.stat.*; //StatUtils

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*; //Molecule, MolAtom
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.qed.*;
import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;

/**	MolCloud web app. 
	@author Jeremy J Yang
*/
public class molcloud_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static ResourceBundle rb=null;
  //private static ServletConfig CONFIG=null;
  private static ServletContext CONTEXT=null;
  private static String CONTEXTPATH=null;
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static String DEMOSMIFILE=null;       // configured in web.xml
  private static Integer MAX_POST_SIZE=null;    // configured in web.xml
  private static Integer N_MAX=null; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static int scratch_retire_sec=3600;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static LinkedHashMap<String,Integer> sizes_h=null;
  private static LinkedHashMap<String,Integer> sizes_w=null;
  private static int serverport=0;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String DATESTR=null;
  private static String color1="#EEEEEE";
  private static MolImporter molReader=null;
  private static String ofmt;
  private static String PREFIX=null;
  private static MCloud molcloud = null;
  private static File fout_img = null;
  private static String SHOWIMGAPP=null;
  private static String PROXY_PREFIX=null; // configured in web.xml

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
    CONTEXTPATH=request.getContextPath();
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest=new MultipartRequest(request,UPLOADDIR,MAX_POST_SIZE,"ISO-8859-1",
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
      if (mrequest.getParameter("molcloud").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));

        int n_mol = LoadMolCloud(molcloud,molReader,params);
        if (params.isChecked("logscale")) molcloud.scaleLog();
        molcloud.scaleNormalize();
        molcloud.doSizes();
        molcloud.sortBoxes();
        molcloud.cloudLayout(); 
        if (params.isChecked("opt_layout")) molcloud.optimizeLayout(0);

        outputs.add("MolCloud generated with mol count: "+molcloud.boxes.size());
        outputs.add("Scores: "+params.getVal("scoremethod")+
		(params.getVal("scoremethod").equals("sdtag")?(" ("+params.getVal("score_sdtag")+")"):"")
		);
        outputs.add("<BLOCKQUOTE>"+StatsHtm(molcloud, params.getVal("scoremethod"))+"</BLOCKQUOTE>");
        if (molcloud.boxes.size()>0)
        {
	  int w = sizes_w.get(params.getVal("size"));
	  int h = sizes_h.get(params.getVal("size"));
          DisplayMolcloud(molcloud,w,h);
        }
        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
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
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
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
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        out.println("<SCRIPT>go_init(window.document.mainform)</SCRIPT>");
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
    sizes_h=new LinkedHashMap<String,Integer>();
    sizes_w=new LinkedHashMap<String,Integer>();

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem and Marvin from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm,tiphtm,href,200,"white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    SHOWIMGAPP=(PROXY_PREFIX+CONTEXTPATH+"/showimg");

    sizes_h.put("xs",480); sizes_w.put("xs",640);
    sizes_h.put("s",640); sizes_w.put("s",800);
    sizes_h.put("m",700); sizes_w.put("m",1000);
    sizes_h.put("l",1000); sizes_w.put("l",1400);
    sizes_h.put("xl",1600); sizes_w.put("xl",2400);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    DATESTR = String.format("%04d%02d%02d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    Random rand = new Random();
    PREFIX = SERVLETNAME+"."+DATESTR+"."+String.format("%03d",rand.nextInt(1000));

    //In fact, a valid license is not required.
    //LicenseManager.refresh();
    //if (!LicenseManager.isLicensed(LicenseManager.JCHEM))
    //{
    //  errors.add("ERROR: ChemAxon license error; JCHEM is required.");
    //}

    File dout = new File(SCRATCHDIR);
    if (!dout.exists())
    {
      boolean ok = dout.mkdir();
      System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create SCRATCHDIR: "+SCRATCHDIR);
        return false;
      }
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
      errors.add("N_MAX = "+N_MAX);
    }

    if (params.getVal("scoremethod").equals("qed"))
    {
      if (!LicenseManager.isLicensed(LicenseManager.PREDICTOR_PLUGIN))
      {
        outputs.add("ERROR: ChemAxon license PREDICTOR_PLUGIN required for QED; please choose another method.");
        return false;
      }
      if (!LicenseManager.isLicensed(LicenseManager.STANDARDIZER))
      {
        outputs.add("ERROR: ChemAxon license STANDARDIZER required for QED; please choose another method.");
        return false;
      }
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
      {
        inbytes=Base64Decoder.decodeToBytes(intxt);
      }
      else
      {
        inbytes=intxt.getBytes("utf-8");
      }
    }
    else
    {
      errors.add("No input data.");
      return false;
    }

    if (params.getVal("molfmt").equals("automatic"))
    {
      String orig_fname=mrequest.getOriginalFileName(fname);
      String molfmt_auto=MFileFormatUtil.getMostLikelyMolFormat(orig_fname);
      if (orig_fname!=null && molfmt_auto!=null)
      {
        molReader=new MolImporter(new ByteArrayInputStream(inbytes),molfmt_auto);
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

    MCloud.pwidth = sizes_w.get(params.getVal("size"));
    MCloud.pheight = sizes_h.get(params.getVal("size"));
    MolBox.avrgWidth = MolBox.avrgHeight = 0.;
    MolBox.sortType = MolBox.SIZE; // sorting acording to the size (number of atoms)
    MolBox.showMolecules = true;

    try { molcloud = new MCloud(); }
    catch (Exception e)
    {
      errors.add("ERROR: failed to init MCloud: "+e.getMessage());
      return false;
    }
    fout_img = File.createTempFile(PREFIX,".png",dout);

    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Read mols from file, load MCloud with newly instantiated MolBox objects.
  	Apply specified scoring method.
  */
  private static int LoadMolCloud(MCloud molcloud, MolImporter molReader,HttpParams params)
  {
    QED qed = null;
    if (params.getVal("scoremethod").equals("qed"))
    {
      try {
        qed = new QED();
        qed.loadDefaultAlerts();
        errors.add(qed.numAlerts()+" default alerts loaded.");
      }
      catch (Exception e) { errors.add("ERROR: failed to init QED: "+e.getMessage()); }
    }

    int n_mol=0;
    int n_failed=0;
    while (true)
    {
      Molecule mol;
      try { mol=molReader.read(); }
      catch (IOException e)
      {
        errors.add("ERROR: MolImporter failed: "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (mol==null) break;
      if (mol.getAtomCount()==0) {
        errors.add("ERROR: Atom count = 0 ; mol name: "+mol.getName());
        ++n_failed;
        continue;
      }
      ++n_mol;
      try
      {
        mol.implicitizeHydrogens(MolAtom.ALL_H);
        String molname = mol.getName();
        String[] namefields = Pattern.compile("\\s+").split(molname);
        Double scale = null;
        if (params.getVal("scoremethod").equals("nameval"))
        {
          scale=Double.parseDouble(namefields[0]);
        }
        else if (params.getVal("scoremethod").equals("sdtag"))
        {
          scale=Double.parseDouble(mol.getProperty(params.getVal("score_sdtag")));
        }
        else if (params.getVal("scoremethod").equals("qed") && qed!=null)
        {
          try { qed.calc(mol); }
          catch (Exception e) {
            errors.add("ERROR: QED failed ["+n_mol+"]: "+e.getMessage());
            ++n_failed;
          }
          scale = 100.0 * Double.parseDouble(mol.getProperty("WEIGHTED_QED"));
        }
        else //random
        {
          scale=Math.random() * 100.0 + 1.0; //Avoid log(0).
        }
        //errors.add("DEBUG: scale = "+scale);
        String f2 = (namefields.length>1)?namefields[1]:"";
        String smi = MolExporter.exportToFormat(mol,"smiles:-a");
        String label="";
        if (params.getVal("label").equals("name")) label=molname;
        else if (params.getVal("label").equals("score")) label=String.format("%.1f",scale);
        else label="";
        MolBox mbox = new MolBox(smi,scale,f2,label);
        molcloud.boxes.add(mbox);
      }
      catch (Exception e)
      {
        errors.add("ERROR (LoadMolCloud): "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (n_mol==N_MAX)
      {
        errors.add("Limit reached: N_MAX mols: "+N_MAX);
        break;
      }
    }
    try { molReader.close(); } catch (IOException e) { }
    if (n_failed>0) errors.add("errors: "+n_failed);
    errors.add("NOTE (LoadMolCloud): mols loaded: "+n_mol);
    return n_mol;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Html for box of statistics.
  */
  private static String StatsHtm(MCloud molcloud, String method)
  {
    int n_box=molcloud.boxes.size();
    if (n_box==0) return "ERROR: no molcloud data.";
    DescriptiveStatistics dstats = new DescriptiveStatistics(n_box);
    for (int i=0;i<n_box;i++)
    {
      MolBox box = (MolBox)molcloud.boxes.get(i);
      dstats.addValue(box.scale);
    }

    double xmin = dstats.getMin();
    double xmax = dstats.getMax();
    double mean = dstats.getMean();
    double std = dstats.getStandardDeviation();

    String htm="<TABLE WIDTH=\"25%\" CELLSPACING=2 CELLPADDING=2><TR>\n";
    htm+=("<TR><TH></TH><TH ALIGN=\"center\">"+method+"</TH></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\">xmin</TD><TD BGCOLOR=\"white\" ALIGN=\"center\">"+String.format("%9.3f",xmin)+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\">xmax</TD><TD BGCOLOR=\"white\" ALIGN=\"center\">"+String.format("%9.3f",xmax)+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\">mean</TD><TD BGCOLOR=\"white\" ALIGN=\"center\">"+String.format("%9.3f",mean)+"</TD></TR>\n");
    htm+=("<TR><TD ALIGN=\"right\">std</TD><TD BGCOLOR=\"white\" ALIGN=\"center\">"+String.format("%9.3f",std)+"</TD></TR>\n");
    htm+=("</TABLE>");
    return htm;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Launch popup window displaying MolCloud; also output link in case popups are blocked.
  */
  private static void DisplayMolcloud(MCloud molcloud,int w,int h)
  {
    try { MCloud.SaveImage(molcloud,fout_img); }
    catch (IOException e) {
      errors.add("ERROR: Cannot write file: "+fout_img.getAbsolutePath()+" ;"+e.getMessage());
      return;
    }
    catch (Exception e) {
      errors.add("ERROR: "+e.getMessage());
      for (StackTraceElement ste: e.getStackTrace()) errors.add(ste.toString());
      return;
    }
    //errors.add("DEBUG: File written:"+fout_img.getAbsolutePath());

    //Launch popup window:
    String title="View"+APPNAME;
    outputs.add("<SCRIPT>go_molcloud_win('"+SHOWIMGAPP+"','"+fout_img.getAbsolutePath()+"',"+w+","+h+",'"+title+"')</SCRIPT>");
    outputs.add("(<I>If MolCloud popup does not appear, use button:</I>)");
    String bhtm=("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+SHOWIMGAPP+"?imgfile="+fout_img.getAbsolutePath()+"','"+title+"','width="+w+",height="+h+",scrollbars=1,resizable=1')\"><B>View "+APPNAME+"...</B></BUTTON>\n");
    outputs.add("<BLOCKQUOTE>"+bhtm+"</BLOCKQUOTE>");
    return;
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

    String size_menu="<SELECT NAME=\"size\">\n";
    for (String key:sizes_h.keySet())
    {
      size_menu+=("<OPTION VALUE=\""+key+"\">"+key+" - "+sizes_h.get(key)+"x"+sizes_w.get(key)+"\n");
    }
    size_menu+="</SELECT>\n";
    size_menu=size_menu.replace("\""+params.getVal("size")+"\">","\""+params.getVal("size")+"\" SELECTED>\n");

    String scoremethod_nameval=""; String scoremethod_qed=""; String scoremethod_random=""; String scoremethod_sdtag="";
    if (params.getVal("scoremethod").equals("qed")) scoremethod_qed="CHECKED";
    else if (params.getVal("scoremethod").equals("sdtag")) scoremethod_sdtag="CHECKED";
    else if (params.getVal("scoremethod").equals("nameval")) scoremethod_nameval="CHECKED";
    else scoremethod_random="CHECKED";

    String label_name=""; String label_score=""; String label_none="";
    if (params.getVal("label").equals("score")) label_score="CHECKED";
    else if (params.getVal("label").equals("name")) label_name="CHECKED";
    else label_none="CHECKED";

    String htm=""
    +("<FORM NAME=\"mainform\" METHOD=POST")
    +(" ACTION=\""+response.encodeURL(SERVLETNAME)+"\"")
    +(" ENCTYPE=\"multipart/form-data\">\n")
    +("<INPUT TYPE=HIDDEN NAME=\"molcloud\">\n")
    +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+APPNAME+"</H1></TD>\n")
    +("<TD>- Molecule Cloud generator (algorithm by P. Ertl &amp; B. Rohde, Novartis)</TD>")
    +("<TD ALIGN=RIGHT>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_demo(this.form)\"><B>Demo</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
    +("</TD></TR></TABLE>\n")
    +("<HR>\n")
    +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
    +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP WIDTH=\"50%\">\n")
    +("format:"+molfmt_menu)
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\">\n")
    +("...or paste:<BR>\n")
    +("<TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=50>"+params.getVal("intxt")+"</TEXTAREA>\n")
    +("</TD>\n")
    +("<TD VALIGN=TOP>\n")
    +("<B>scaling:</B><BR>\n")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"scoremethod\" VALUE=\"qed\" "+scoremethod_qed+">qed - drug-likeness<BR>\n")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"scoremethod\" VALUE=\"random\" "+scoremethod_random+">random<BR>\n")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"scoremethod\" VALUE=\"nameval\" "+scoremethod_nameval+">nameval - parse mol name<BR>")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"scoremethod\" VALUE=\"sdtag\" "+scoremethod_sdtag+">sdtag\n")
    +("&nbsp;<INPUT TYPE=TEXT NAME=\"score_sdtag\" VALUE=\""+params.getVal("score_sdtag")+"\" SIZE=20><BR>\n")
    +("&nbsp;<INPUT TYPE=CHECKBOX NAME=\"logscale\" VALUE=\"CHECKED\" "+params.getVal("logscale")+">logscale<BR>\n")
    +("<B>layout:</B><BR>\n")
    +("&nbsp;<INPUT TYPE=CHECKBOX NAME=\"opt_layout\" VALUE=\"CHECKED\" "+params.getVal("opt_layout")+">optimize<BR>\n")
    +("<B>depiction labels:</B><BR>\n")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"label\" VALUE=\"name\" "+label_name+">molname")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"label\" VALUE=\"score\" "+label_score+">score\n")
    +("&nbsp;<INPUT TYPE=RADIO NAME=\"label\" VALUE=\"none\" "+label_none+">none<BR>\n")
    +("<HR>\n")
    +("<B>misc:</B><BR>\n")
    +("&nbsp;image size:"+size_menu+"<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
    +("</TD></TR></TABLE>\n")
    +("<P>\n")
    +("<CENTER>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_molcloud(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
    +("</CENTER>\n")
    +("</FORM>\n");
    return htm;
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
"  form.file2txt.checked=true;\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.molfmt.length;++i)\n"+
"    if (form.molfmt.options[i].value=='automatic')\n"+
"      form.molfmt.options[i].selected=true;\n"+
"  for (i=0;i<form.size.length;++i)\n"+
"    if (form.size.options[i].value=='m')\n"+
"      form.size.options[i].selected=true;\n"+
"  for (i=0;i<form.scoremethod.length;++i)\n"+ //radio
"    if (form.scoremethod[i].value=='random')\n"+
"      form.scoremethod[i].checked=true;\n"+
"  for (i=0;i<form.label.length;++i)\n"+ //radio
"    if (form.label[i].value=='none')\n"+
"      form.label[i].checked=true;\n"+
"  form.logscale.checked=true;\n"+
"  form.opt_layout.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input specified');\n"+
"    return false;\n"+
"  }\n"+
"  for (i=0;i<form.scoremethod.length;++i)\n"+ //radio
"  {\n"+
"    if (form.scoremethod[i].checked)\n"+
"    {\n"+
"      if (form.scoremethod[i].value=='sdtag' && !form.score_sdtag.value)\n"+
"      {\n"+
"        alert('ERROR: SDTAG with scores must be specified.');\n"+
"        return false;\n"+
"      }\n"+
"    }\n"+
"  }\n"+
"  return true;\n"+
"}\n"+
"function go_demo(form) {\n"+
"  go_init(form);\n"+
"  form.intxt.value=demotxt;\n"+
"  form.molcloud.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"+
"function go_molcloud(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.molcloud.value='TRUE'\n"+
"  form.submit()\n"+
"}\n"+
"function go_molcloud_win(url,fpath,w,h,title)\n"+
"{\n"+
"  var cwin=window.open(url+'?imgfile='+encodeURI(fpath),title,\n"+
"    'width='+w+',height='+h+',scrollbars=1,resizable=1,location=0,status=0,toolbar=0');\n"+
"  cwin.focus();\n"+
"}\n");
    return(js);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    return (
    "<H1>"+APPNAME+" help</H1>\n"+
    "<HR>\n"+
    "Ref: P. Ertl, B. Rohde: The Molecule Cloud - compact visualization of large collections of molecules J. Cheminformatics 2012, 4:12, <A HREF=\"http://www.jcheminf.com/content/4/1/12/\">http://www.jcheminf.com/content/4/1/12/</A>.\n"+
    "<P>\n"+
    "Depictions in the molecule cloud may be scaled using scores from the input molecule file.\n"+
    "Scores may be specified via:\n"+
    "<UL>\n"+
    "<LI>QED - Quantitative Estimate of Drug-likeness (Bickerton, et al.)\n"+
    "<LI>randomly (for demo purposes)\n"+
    "<LI>mol name (may be first field in longer name string)\n"+
    "<LI>SD data field (input format SDF)\n"+
    "</UL>\n"+
    "<P>\n"+
    "The output is a PNG image.\n"+
    "<P>\n"+
    "configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "webapp author: Jeremy Yang<BR>\n"+
    "QED code author: Oleg Ursu<BR>\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Read servlet parameters (from web.xml).
  */
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    CONTEXT=getServletContext();
    CONTEXTPATH=CONTEXT.getContextPath();
    //CONFIG=conf;
    APPNAME=conf.getInitParameter("APPNAME");
    if (APPNAME==null) APPNAME=this.getServletName();
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    try { MAX_POST_SIZE=Integer.parseInt(conf.getInitParameter("MAX_POST_SIZE")); }
    catch (Exception e) { MAX_POST_SIZE=1*1024*1024; }
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    DEMOSMIFILE=CONTEXT.getRealPath("")+"/data/"+conf.getInitParameter("DEMOSMIFILE");
    PROXY_PREFIX=((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException, ServletException
  {
    doPost(request,response);
  }
}
