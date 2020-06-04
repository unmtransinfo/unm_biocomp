package edu.unm.health.biocomp.qed;

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

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;
import com.oreilly.servlet.*; //Base64Encoder, Base64Decoder

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;

/**	QED: Quantitative Estimate of Drug-likeness (Bickerton, et al.).

	@author Jeremy J Yang (qed_servlet.java)
	@author Oleg Ursu (QED.java)
*/
public class qed_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=null;    // configured in web.xml
  private static int N_MAX=100; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static String SMARTSDIR=null; // configured in web.xml
  private static int scratch_retire_sec=3600;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
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
  private static File LOGFILE=null;
  private static String color1="#EEEEEE";
  private static MolImporter molReader=null;
  private static QED qed = null;
  private static String ofmt;
  private static String PREFIX=null;
  private static String HISTOIMG_URL=null;
  private static String MOL2IMG_SERVLETURL=null;
  private static String PROXY_PREFIX=null; // configured in web.xml

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    serverport=request.getServerPort();
    SERVERNAME=request.getServerName();
    REMOTEHOST=request.getHeader("X-Forwarded-For"); // client (original)
    if (REMOTEHOST!=null)
    {
      String[] addrs=Pattern.compile(",").split(REMOTEHOST);
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
      if (mrequest.getParameter("qed").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        // headerHtm must invoke marvin.js for applet for work.
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        ArrayList<Molecule> mols=QEDGenerate();
        QEDResults(mols,params,response);
        out.println(HtmUtils.OutputHtm(outputs));
        out.println(HtmUtils.FooterHtm(errors,true));
        HtmUtils.PurgeScratchDirs(Arrays.asList(SCRATCHDIR),scratch_retire_sec,false,".",(HttpServlet) this);
      }
    }
    else
    {
      String help=request.getParameter("help");	// GET param
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (help!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.println(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(HelpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
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
  private boolean initialize(HttpServletRequest request, MultipartRequest mrequest)
      throws IOException, ServletException
  {
    SERVLETNAME = this.getServletName();
    outputs = new ArrayList<String>();
    errors = new ArrayList<String>();
    params = new HttpParams();
    sizes_h = new LinkedHashMap<String, Integer>();
    sizes_w = new LinkedHashMap<String, Integer>();
    MOL2IMG_SERVLETURL =( PROXY_PREFIX+CONTEXTPATH+"/mol2img");

    String logo_htm = "<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm = ("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm = (APPNAME+" web app from UNM Translational Informatics.");
    String href = ("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD><TD>";
    imghtm = ("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm = ("JChem from ChemAxon Ltd.");
    href = ("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    HISTOIMG_URL = (PROXY_PREFIX+CONTEXTPATH+"/histoimg");

    //booleans:
    sizes_h.put("xs", 96); sizes_w.put("xs", 96);
    sizes_h.put("s", 160); sizes_w.put("s", 160);
    sizes_h.put("m", 180); sizes_w.put("m", 260);
    sizes_h.put("l", 280); sizes_w.put("l", 380);
    sizes_h.put("xl", 480); sizes_w.put("xl", 640);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    DATESTR = String.format("%04d%02d%02d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    Random rand = new Random();
    PREFIX = SERVLETNAME+"."+DATESTR+"."+String.format("%03d", rand.nextInt(1000));

    //Create webapp-specific log dir if necessary:
    File dout = new File(LOGDIR);
    if (!dout.exists())
    {
      boolean ok = dout.mkdir();
      System.err.println("LOGDIR creation "+(ok?"succeeded":"failed")+": "+LOGDIR);
      if (!ok)
      {
        errors.add("ERROR: could not create LOGDIR (logging disabled): "+LOGDIR);
      }
    }
    LOGFILE = new File(LOGDIR+"/"+SERVLETNAME+".log");
    if (!LOGFILE.exists())
    {
      try {
        LOGFILE.createNewFile();
      LOGFILE.setWritable(true, true);
      PrintWriter out_log = new PrintWriter(LOGFILE);
      out_log.println("date\tip\tN"); 
      out_log.flush();
      out_log.close();
      } catch (Exception e) {
        errors.add("ERROR: Cannot create LOGFILE (logging disabled): "+e.getMessage());
        LOGFILE = null;
      }
    }
    else if (!LOGFILE.canWrite())
    {
      errors.add("ERROR: LOGFILE not writable (logging disabled).");
      LOGFILE = null;
    }
    if (LOGFILE!=null) {
      BufferedReader buff=new BufferedReader(new FileReader(LOGFILE));
      if (buff==null)
      {
        errors.add("ERROR: Cannot open log file.");
        LOGFILE = null;
      }
      else
      {
        int n_lines=0;
        String line=null;
        String startdate=null;
        while ((line=buff.readLine())!=null)
        {
          ++n_lines;
          String[] fields = Pattern.compile("\\t").split(line);
          if (n_lines==2) startdate=fields[0];
        }
        if (n_lines>2)
        {
          calendar.set(Integer.parseInt(startdate.substring(0, 4)),
                   Integer.parseInt(startdate.substring(4, 6))-1,
                   Integer.parseInt(startdate.substring(6, 8)),
                   Integer.parseInt(startdate.substring(8, 10)),
                   Integer.parseInt(startdate.substring(10, 12)), 0);
          DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.US);
          errors.add("since "+df.format(calendar.getTime())+", times used: "+(n_lines-1));
        }
      }
    }

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

    if (mrequest==null) return true;

    /// Stuff for a run:

    for (Enumeration e=mrequest.getParameterNames(); e.hasMoreElements(); )
    {
      String key=(String)e.nextElement();
      if (mrequest.getParameter(key)!=null)
        params.setVal(key, mrequest.getParameter(key));
    }

    if (params.isChecked("verbose"))
    {
      //errors.add("JChem version: "+chemaxon.jchem.version.VersionInfo.getVersion());
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
      errors.add("server: "+CONTEXT.getServerInfo()+" [API:"+CONTEXT.getMajorVersion()+"."+CONTEXT.getMinorVersion()+"]");
      errors.add("ServletName: "+this.getServletName());
    }

    String fname="infile";
    File ifile=mrequest.getFile(fname);
    String intxt=params.getVal("intxt").replaceFirst("[\\s]+$", "");
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
          System.arraycopy(inbytes, 0, tmp, 0, size);
          inbytes=tmp;
        }
        inbytes[size]=(byte)b;
        ++size; 
      }
      byte[] tmp=new byte[size];
      System.arraycopy(inbytes, 0, tmp, 0, size);
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
        molReader=new MolImporter(new ByteArrayInputStream(inbytes), molfmt_auto);
      }
      else
      {
        molReader=new MolImporter(new ByteArrayInputStream(inbytes));
      }
    }
    else
    {
      String ifmt=params.getVal("molfmt");
      molReader=new MolImporter(new ByteArrayInputStream(inbytes), ifmt);
    }
    String fmt=molReader.getFormat();
    params.setVal("molfmt_auto", fmt);

    if (ifile!=null) ifile.delete();

    MFileFormat mffmt=MFileFormatUtil.getFormat(fmt);

    if (params.isChecked("file2txt"))
    {
      if (mffmt==MFileFormat.CDX) //binary
      {
        intxt=Base64Encoder.encode(inbytes);
        if (params.getVal("molfmt").equals("automatic"))
          params.setVal("molfmt", "cdx");
      }
      else
      {
        intxt=new String(inbytes, "utf-8");
      }
      params.setVal("intxt", intxt);
    }

    try {
      qed = new QED();
      qed.loadDefaultAlerts();
      errors.add(qed.numAlerts()+" default alerts loaded.");
    }
    catch (Exception e) { errors.add("ERROR: failed to init QED: "+e.getMessage()); }

    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String FormHtm(MultipartRequest mrequest, HttpServletResponse response)
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
    molfmt_menu=molfmt_menu.replace(params.getVal("molfmt")+"\">", params.getVal("molfmt")+"\" SELECTED>\n");

    String size_menu="<SELECT NAME=\"size\">\n";
    for (String key:sizes_h.keySet())
    {
      size_menu+=("<OPTION VALUE=\""+key+"\">"+key+" - "+sizes_h.get(key)+"x"+sizes_w.get(key)+"\n");
    }
    size_menu+="</SELECT>\n";
    size_menu=size_menu.replace("\""+params.getVal("size")+"\">", "\""+params.getVal("size")+"\" SELECTED>\n");

    String outfmt_smi=""; String outfmt_sdf="";
    if (params.getVal("outfmt").equals("smi")) outfmt_smi="CHECKED";
    else if (params.getVal("outfmt").equals("sdf")) outfmt_sdf="CHECKED";

    String htm=""
    +("<FORM NAME=\"mainform\" METHOD=POST ACTION=\""+response.encodeURL(SERVLETNAME)+"\" ENCTYPE=\"multipart/form-data\">\n")
    +("<INPUT TYPE=HIDDEN NAME=\"qed\">\n")
    +("<TABLE WIDTH=\"100%\"><TR><TD><H1>"+APPNAME+"</H1></TD>\n")
    +("<TD>- Quantitative Estimate of Drug-likeness (Bickerton, et al.)</TD>\n")
    +("<TD ALIGN=RIGHT>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
    +("</TD></TR></TABLE>\n")
    +("<HR>\n")
    +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
    +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP>\n")
    +("format:"+molfmt_menu)
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\">\n")
    +("...or paste:<BR>\n")
    +("<TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=50>"+params.getVal("intxt")+"</TEXTAREA>\n")
    +("</TD>\n")
    +("<TD VALIGN=TOP>\n")
    +("<B>Output:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"viewout\" VALUE=\"CHECKED\" "+params.getVal("viewout")+">View<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"batchout\" VALUE=\"CHECKED\" "+params.getVal("batchout")+">Batch<BR>\n")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"smi\" "+outfmt_smi+">SMILES (TSV)")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=CHECKBOX NAME=\"smiheader\" VALUE=\"CHECKED\" "+params.getVal("smiheader")+">+header<BR>\n")
    +("&nbsp;&nbsp;")
    +("<INPUT TYPE=RADIO NAME=\"outfmt\" VALUE=\"sdf\" "+outfmt_sdf+">SDF")
    +("<HR>\n")
    +("<B>Depictions:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"depict\" VALUE=\"CHECKED\" "+params.getVal("depict")+">Show<BR>\n")
    +("size:"+size_menu+"<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"showh\" VALUE=\"CHECKED\""+params.getVal("showh")+">Show Hs<BR>\n")
    +("<HR>\n")
    +("<B>Misc:</B><BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">Verbose<BR>\n")
    +("</TD></TR></TABLE>\n")
    +("<P>\n")
    +("<CENTER>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_qed(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
    +("</CENTER>\n")
    +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Returns mols annotated with QED results.
  */
  private static ArrayList<Molecule>  QEDGenerate()
  {
    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    Molecule mol;
    int n_failed=0;
    while (true)
    {
      try { mol=molReader.read(); }
      catch (Exception e) { errors.add("ERROR: MolImporter failed: "+e.getMessage()); ++n_failed; continue; }
      if (mol==null) break;

      try { qed.calc(mol); }
      catch (Exception e) { errors.add("ERROR: "+e.getMessage()); ++n_failed; }

      mols.add(mol);
      if (mols.size()==N_MAX) { errors.add("Warning: N_MAX mols: "+N_MAX); break; }
    }
    try { molReader.close(); }
    catch (IOException e) { }
    if (params.isChecked("verbose"))
    {
      String desc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
      errors.add("input format:  "+molReader.getFormat()+" ("+desc+")");
      errors.add("mols read:  "+mols.size());
    }
    if (n_failed>0) errors.add("ERRORS (unable to read mol): "+n_failed);
    return mols;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void QEDResults_stats(ArrayList<Molecule> mols, HttpParams params, HttpServletResponse response)
      throws IOException, ServletException
  {
    String score_tag="WEIGHTED_QED";
    //String score_tag="UNWEIGHTED_QED";

    ArrayList<Double> scores = new ArrayList<Double>();

    for (Molecule mol: mols)
    {
      Double score=null;
      try { score=Double.parseDouble(mol.getProperty(score_tag)); }
      catch (Exception e) { score=0.0; }
      scores.add(score);
    }
    //Collections.sort(scores);

    ArrayList<Integer> vals = new ArrayList<Integer>(Arrays.asList(0,0,0,0,0,0,0,0,0,0));
    ArrayList<Double> xmaxs = new ArrayList<Double>(Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0));

    for (Double score: scores)
    {
      vals.set((int)Math.floor(score*10), vals.get((int)Math.floor(score*10))+1);
    }

    String histohtm;

    //String vals_str="";
    //for (int i: vals) vals_str+=(""+i+",");
    //histohtm=HISTOIMG_URL+"?w=400&h=200&fgcolor=x0088CC&title=QED%20"+score_tag;
    //histohtm+="&yaxis="+URLEncoder.encode("frequency","UTF-8");
    //histohtm+="&values="+URLEncoder.encode(vals_str,"UTF-8");
    //String xmaxs_str="";
    //for (double x: xmaxs) xmaxs_str+=(""+x+",");
    //histohtm+="&xmaxs="+URLEncoder.encode(xmaxs_str,"UTF-8");
    //histohtm=("<IMG SRC=\""+histohtm+"\">");
    //outputs.add("<BLOCKQUOTE>"+histohtm+"</BLOCKQUOTE>");

    String opts="&title=QED%20"+score_tag+"&fgcolor=x0088CC";
    opts+="&yaxis="+URLEncoder.encode("frequency", "UTF-8");
    histohtm=HtmUtils.HistoImgHtm(vals, xmaxs, opts, 400, 200, HISTOIMG_URL, true, 4, "go_zoom_chartimg");
    outputs.add("<BLOCKQUOTE>"+histohtm+"</BLOCKQUOTE>");

    for (int i=0;i<vals.size();++i)
    {
      errors.add(String.format("QED score range: [%3.1f , %3.1f): %3d mols",0.1*i,0.1*(i+1),vals.get(i)));
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  // [ ] sort on QED scores?
  /////////////////////////////////////////////////////////////////////////////
  private static void QEDResults(ArrayList<Molecule> mols, HttpParams params, HttpServletResponse response)
      throws IOException, ServletException
  {
    int n_mol=0;
    int N_MAX_VIEW=1000;

    int w=sizes_w.get(params.getVal("size"));
    int h=sizes_h.get(params.getVal("size"));
    String depopts=("mode=cow&imgfmt=png&kekule=true");
    if (params.isChecked("showh")) depopts+=("&showh=true");

    if (mols.size()==0)
    {
      outputs.add("<B>ERROR: no molecules</B>");
      return;
    }
    ArrayList<String> molProps = new ArrayList<String>(Arrays.asList(mols.get(0).properties().getKeys()));
    Collections.sort(molProps);

    outputs.add("<B>qed (N="+mols.size()+"):</B>");

    QEDResults_stats(mols, params, response);

    // for download mols:
    if (params.isChecked("batchout"))
    {
      MolExporter molWriter=null;
      File fout=null;
      if (params.isChecked("batchout"))
      {
        if (params.getVal("outfmt").equals("sdf"))
        {
          ofmt="sdf";
        }
        else
        {
          ofmt="smiles:";
          if (params.isChecked("smiheader")) {
            ofmt+="T*";
          }
        }
        try {
          File dout=new File(SCRATCHDIR);
          fout=File.createTempFile(PREFIX, "_out."+params.getVal("outfmt"), dout);
        }
        catch (IOException e) {
          errors.add("ERROR: could not open temp file; check SCRATCHDIR: "+SCRATCHDIR);
          return;
        }
        if (params.isChecked("verbose"))
          errors.add("Output format: "+ofmt);
        molWriter=new MolExporter(new FileOutputStream(fout), ofmt);
      }
      for (Molecule mol: mols) { molWriter.write(mol); }
      molWriter.close();

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
      outputs.add(bhtm);
    }

    if (params.isChecked("viewout"))
    {
      String thtm="";
      thtm=("<TABLE BORDER>\n");
      thtm+="<TR>";
      if (params.isChecked("depict")) thtm+="<TH></TH>"; //1st col smiles
      thtm+="<TH>name</TH>"; //2nd col name
      for (String molProp: molProps) thtm+="<TH>"+molProp.replaceAll("_", " ")+"</TH>";
      thtm+="</TR>\n";
      for (Molecule mol:mols)
      {
        String rhtm="<TR>\n";
        String smiles=mol.exportToFormat("smiles:u");

        if (params.isChecked("depict"))
        {
          String imghtm=HtmUtils.Smi2ImgHtm(smiles, depopts, h, w, MOL2IMG_SERVLETURL, true, 4, "go_zoom_smi2img");
          rhtm+=("<TD ALIGN=CENTER BGCOLOR=\"white\">"+imghtm+"</TD>\n");
        }
        rhtm+=("<TD ALIGN=CENTER BGCOLOR=\"white\">"+mol.getName()+"</TD>\n");

        for (String molProp: molProps)
        {
          String molProp_str = mol.getProperty(molProp);
          if (molProp_str.matches("[-]?\\d+\\.\\d*$"))
          {
            try { molProp_str=String.format("%.2f", Float.parseFloat(molProp_str)); }
            catch (Exception e) { }
          }
          rhtm+="<TD ALIGN=\"center\" BGCOLOR=\"white\"><TT>"+molProp_str+"</TT></TD>";
        }

        rhtm+="</TR>\n";
        thtm+=rhtm;
        ++n_mol;
        if (n_mol>=N_MAX_VIEW) break;
        if (n_mol>=N_MAX) break;
      }
      thtm+=("</TABLE>");
      outputs.add("<CENTER>"+thtm+"</CENTER>");
      if (mols.size()>N_MAX_VIEW)
        errors.add("NOTE: view truncated at N = "+N_MAX_VIEW);
    }

    if (LOGFILE!=null) {
      PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(LOGFILE, true)));
      out_log.printf("%s\t%s\t%d\n", DATESTR, REMOTEHOST, n_mol); 
      out_log.close();
    }
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
"  for (i=0;i<form.molfmt.length;++i)\n"+
"    if (form.molfmt.options[i].value=='automatic')\n"+
"      form.molfmt.options[i].selected=true;\n"+
"  for (i=0;i<form.size.length;++i)\n"+
"    if (form.size.options[i].value=='xs')\n"+
"      form.size.options[i].selected=true;\n"+
"  form.depict.checked=true;\n"+
"  form.showh.checked=false;\n"+
"  form.viewout.checked=false;\n"+
"  form.batchout.checked=true;\n"+
"  for (i=0;i<form.outfmt.length;++i)\n"+ //radio
"    if (form.outfmt[i].value=='smi')\n"+
"      form.outfmt[i].checked=true;\n"+
"  form.smiheader.checked=true;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input specified');\n"+
"    return 0;\n"+
"  }\n"+
"  if (!form.batchout.checked && !form.viewout.checked) {\n"+
"    alert('ERROR: No output specified.');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_qed(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.qed.value='TRUE'\n"+
"  form.submit()\n"+
"}\n");
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
  {
    return (
    "<H2>"+APPNAME+" Help</H2>\n"+
    "<P>\n"+
    "QED: Quantitative Estimate of Drug-likeness (Bickerton, et al.).\n"+
    "<P>\n"+
    "Run QED for input molecules.\n"+
    "Note: QED range is [0,1.0].\n"+
    "<P>\n"+
    "Both unweighted and weighted QED are calculated.  Weights were selected for individual\n"+
    "desireability functions (e.g. rotatable bond count) to maximize information content\n"+
    "as given by Shannon entropy (see ref).\n"+
    "<P>\n"+
    "Desirability functions:\n"+
    "<UL>\n"+
    "<LI>Molecular Weight (MW)\n"+
    "<LI>LogP estimate (LOGP)\n"+
    "<LI>hydrogen bond donors (HBD)\n"+
    "<LI>hydrogen bond acceptors (HBA)\n"+
    "<LI>molecular polar surface area (PSA)\n"+
    "<LI>rotatable bonds (ROTB)\n"+
    "<LI>aromatic rings (AROM)\n"+
    "<LI>unfavourable structural patterns (ALERTS)\n"+
    "</UL>\n"+
    "<P>\n"+
    "configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "Ref: Quantifying the chemical beauty of drugs; Bickerton GR, Paolini GV, Besnard J, Muresan S, Hopkins AL. Nat Chem. 2012 Jan 24;4(2):90-8. doi: 10.1038/nchem.1243.\n"+
    "<P>\n"+
    "author/support: Jeremy Yang (qed_servlet.java)<BR>\n"+
    "author: Oleg Ursu (QED.java)<BR>\n"
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
    try { APPNAME=conf.getInitParameter("APPNAME"); }
    catch (Exception e) { APPNAME=this.getServletName(); }
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR");
    if (LOGDIR==null) LOGDIR="/tmp"+CONTEXTPATH+"_logs";
    try { MAX_POST_SIZE=Integer.parseInt(conf.getInitParameter("MAX_POST_SIZE")); }
    catch (Exception e) { MAX_POST_SIZE=1*1024*1024; }
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=100; }
    PROXY_PREFIX=((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    doPost(request,response);
  }
}

