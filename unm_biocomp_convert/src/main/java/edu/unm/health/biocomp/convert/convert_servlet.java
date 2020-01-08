package edu.unm.health.biocomp.convert;

import java.io.*;
import java.net.*; //URLEncoder, InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*; //HttpServletRequest, HttpServletResponse, Part

import org.apache.commons.codec.binary.Base64;

import com.oreilly.servlet.*; //MultipartRequest
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;

import chemaxon.formats.*;
import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.Molecule;
import chemaxon.marvin.calculations.MarkushEnumerationPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.util.*;
import edu.unm.health.biocomp.util.http.*;

/**	Molecular file format conversion web app.

	@author Jeremy J Yang
*/
public class convert_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;	// configured in web.xml
  private static String APPNAME=null;	// configured in web.xml
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=10*1024*1024; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static Integer N_MAX=null;	// configured in web.xml
  private static Integer N_MAX_LINES=null;	// configured in web.xml
  private static String PROXY_PREFIX=null; // configured in web.xml
  private static String PREFIX=null;
  private static int scratch_retire_sec=3600;
  private static MolImporter molReader=null;
  private static int MARKUSH_ENUM_LIMIT=1000;
  private static ServletContext CONTEXT=null;
  //private static ServletConfig CONFIG=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String REMOTEAGENT=null;
  private static String DATESTR=null;
  private static File LOGFILE=null;
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
    REMOTEAGENT=request.getHeader("User-Agent");
    rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    MultipartRequest mrequest=null;
    if (request.getMethod().equalsIgnoreCase("POST"))
    {
      try { mrequest=new MultipartRequest(request,UPLOADDIR,10*1024*1024,"ISO-8859-1",
                                    new DefaultFileRenamePolicy()); }
      catch (IOException e) {
        this.getServletContext().log("not a valid MultipartRequest",e); }
    }

    // main logic:
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/css/biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList(PROXY_PREFIX+CONTEXTPATH+"/js/biocomp.js", PROXY_PREFIX+CONTEXTPATH+"/js/ddtip.js"));

    boolean ok=initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
      out.print(HtmUtils.FooterHtm(errors,true));
      return;
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("convert").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest,response));
        Date t_i = new Date();
        Convert(mrequest,response);
        Date t_f = new Date();
        long t_d=t_f.getTime()-t_i.getTime();
        int t_d_min = (int)(t_d/60000L);
        int t_d_sec = (int)((t_d/1000L)%60L);
        errors.add(SERVLETNAME+": elapsed time: "+t_d_min+"m "+t_d_sec+"s");
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
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
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
        HtmUtils.DownloadString(response,ostream,downloadtxt,
          request.getParameter("fname"));
      }
      else if (downloadfile!=null && downloadfile.length()>0) // POST param
      {
        ServletOutputStream ostream=response.getOutputStream();
        HtmUtils.DownloadFile(response, ostream, downloadfile,
          request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(APPNAME, jsincludes, cssincludes, JavaScript(), "", color1, request));
        out.println(FormHtm(mrequest, response));
        out.println("<SCRIPT>go_init(window.document.mainform)</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors, true));
      }
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request, MultipartRequest mrequest)
      throws IOException, ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs = new ArrayList<String>();
    errors = new ArrayList<String>();
    params = new HttpParams();

    String logo_htm="<TABLE CELLSPACING=5 CELLPADDING=5><TR><TD>";
    String imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/biocomp_logo_only.gif\">");
    String tiphtm=(APPNAME+" web app from UNM Translational Informatics.");
    String href=("http://medicine.unm.edu/informatics/");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD><TD>";
    imghtm=("<IMG BORDER=0 SRC=\""+PROXY_PREFIX+CONTEXTPATH+"/images/chemaxon_powered_100px.png\">");
    tiphtm=("JChem from ChemAxon Ltd.");
    href=("http://www.chemaxon.com");
    logo_htm+=(HtmUtils.HtmTipper(imghtm, tiphtm, href, 200, "white"));
    logo_htm+="</TD></TR></TABLE>";
    errors.add(logo_htm);

    //Create webapp-specific log dir if necessary:
    File dout=new File(LOGDIR);
    if (!dout.exists())
    {
      boolean ok=dout.mkdir();
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
        LOGFILE.setWritable(true,true);
        PrintWriter out_log = new PrintWriter(LOGFILE);
        out_log.println("date\tip\tN"); 
        out_log.flush();
        out_log.close();
      }
      catch (Exception e) {
        errors.add("ERROR: could not create LOGFILE (logging disabled): "+e);
        LOGFILE = null;
      }
    }
    else if (!LOGFILE.canWrite())
    {
      errors.add("ERROR: LOGFILE not writable (logging disabled).");
      LOGFILE = null;
    }
    if (LOGFILE!=null)
    {
      BufferedReader buff = new BufferedReader(new FileReader(LOGFILE));
      if (buff==null)
      {
        errors.add("ERROR: Cannot open log file.");
      }
      else
      {
        int n_lines=0;
        String line=null;
        Calendar calendar=Calendar.getInstance();
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
        DATESTR=String.format("%04d%02d%02d%02d%02d",
          calendar.get(Calendar.YEAR),
          calendar.get(Calendar.MONTH)+1,
          calendar.get(Calendar.DAY_OF_MONTH),
          calendar.get(Calendar.HOUR_OF_DAY),
          calendar.get(Calendar.MINUTE));
      }
    }

    Random rand = new Random();
    PREFIX=SERVLETNAME+"."+DATESTR+"."+String.format("%03d",rand.nextInt(1000));

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
      errors.add("Servlet ContextPath: "+CONTEXT.getContextPath()); // e.g. "/biocomp"
      //errors.add("JChem ver: "+chemaxon.jchem.version.VersionInfo.getVersion());
      errors.add("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());
    }

    String fname="infile";
    File fileDB=mrequest.getFile(fname);

    //Part filePart = request.getPart(fname);
    //errors.add("DEBUG: filePart.getName() = "+filePart.getName());
    //errors.add("DEBUG: filePart.getSubmittedFileName() = "+filePart.getSubmittedFileName());
    //errors.add("DEBUG: filePart.getContentType() = "+filePart.getContentType());
    //errors.add("DEBUG: filePart.getSize() = "+filePart.getSize());

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
            if (i==N_MAX_LINES)
            {
              errors.add("ERROR: max lines copied to input: "+N_MAX_LINES);
              break;
            }
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
    String fmt_menu_opts=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      if (fmt.equals("gzip")) continue;
      if (fmt.equals("base64")) continue;
      fmt_menu_opts+=("<OPTION VALUE=\""+fmt+"\">");
      fmt_menu_opts+=(fmt+" - "+MFileFormatUtil.getFormat(fmt).getDescription());
    }
    String ifmt_menu="<SELECT NAME=\"ifmt\">"+fmt_menu_opts+"</SELECT>";
    ifmt_menu=ifmt_menu.replace("\""+params.getVal("ifmt")+"\">",
				"\""+params.getVal("ifmt")+"\" SELECTED>");

    fmt_menu_opts=("<OPTION VALUE=\"automatic\">automatic\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      try { MolExporter test=new MolExporter(new PrintStream("/dev/null"),fmt); }
      catch (Exception e) { continue; } // non-exportable; may be missing JARs
      catch (Error e) { // chemaxon.marvin.io.formats.inchi.InchiExport NoClassDefFound Error.
        errors.add("NOTE: output format \""+fmt+"\" not available: "+e.toString());
        continue;
      }
      if (fmt.equals("gzip")) continue;
      if (fmt.equals("base64")) continue;
      fmt_menu_opts+=("<OPTION VALUE=\""+fmt+"\">");
      fmt_menu_opts+=(fmt+" - "+MFileFormatUtil.getFormat(fmt).getDescription());
    }
    String ofmt_menu="<SELECT NAME=\"ofmt\">"+fmt_menu_opts+"</SELECT>";
    ofmt_menu=ofmt_menu.replace("\""+params.getVal("ofmt")+"\">",
				"\""+params.getVal("ofmt")+"\" SELECTED>");

    String htm=
     ("<FORM NAME=\"mainform\" METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\" ENCTYPE=\"multipart/form-data\">\n")
    +("<TABLE WIDTH=\"100%\"><TR><TD><H2>"+APPNAME+"</H2></TD>\n")
    +("<TD>- molecule file format conversion (via ChemAxon JChem)</TD>\n")
    +("<TD ALIGN=RIGHT>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>Help</B></BUTTON>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"window.location.replace('"+response.encodeURL(SERVLETNAME)+"')\"><B>Reset</B></BUTTON>\n")
    +("</TD></TR></TABLE>\n")
    +("<HR>\n")
    +("<INPUT TYPE=HIDDEN NAME=\"convert\">\n")
    +("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n")
    +("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=\"TOP\">\n")
    +("<H3>Input:</H3>\n")
    +("format:"+ifmt_menu)
    +("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\" "+params.getVal("file2txt")+">file2txt<BR>\n")
    +("upload: <INPUT TYPE=\"FILE\" NAME=\"infile\"> ...or paste:\n")
    +("<BR><TEXTAREA NAME=\"intxt\" WRAP=OFF ROWS=12 COLS=60>")
    +(params.getVal("intxt"))
    +("</TEXTAREA>\n")
    +("</TD>\n")
    +("<TD VALIGN=\"TOP\">\n")
    +("<H3>Output:</H3>\n")
    +("format:"+ofmt_menu)
    +("<P>\n")
    +("<DL>\n")
    +("<DT>smiles:<DD>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_name\" VALUE=\"CHECKED\" "+params.getVal("smi_name")+">name\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_arom\" VALUE=\"CHECKED\" "+params.getVal("smi_arom")+">arom\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_stereo\" VALUE=\"CHECKED\" "+params.getVal("smi_stereo")+">stereo<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_uniq\" VALUE=\"CHECKED\" "+params.getVal("smi_uniq")+">uniq\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_r1\" VALUE=\"CHECKED\" "+params.getVal("smi_r1")+">r1\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_exph\" VALUE=\"CHECKED\" "+params.getVal("smi_exph")+">H\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_sddata\" VALUE=\"CHECKED\" "+params.getVal("smi_sddata")+">SDData\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"smi_header\" VALUE=\"CHECKED\" "+params.getVal("smi_header")+">header\n")
    +("<DT>SDF:<DD>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"sdf_v3000\" VALUE=\"CHECKED\" "+params.getVal("sdf_v3000")+">V3000\n")
    +("<DT>generic:<DD>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"add_2d\" VALUE=\"CHECKED\" "+params.getVal("add_2d")+">+2D\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"add_3d\" VALUE=\"CHECKED\" "+params.getVal("add_3d")+">+3D\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"add_name\" VALUE=\"CHECKED\" "+params.getVal("add_name")+">+IUPAC name\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"gzip\" VALUE=\"CHECKED\" "+params.getVal("gzip")+">gzip<BR>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"parts2mols\" VALUE=\"CHECKED\" "+params.getVal("parts2mols")+">parts2mols\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"enummarkush\" VALUE=\"CHECKED\" "+params.getVal("enummarkush")+">enummarkush\n")
    +("</DL>\n")
    +("<HR>\n")
    +("<H3>Misc:</H3>\n")
    +("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\" "+params.getVal("verbose")+">verbose<BR>\n")
    +("</TD></TR></TABLE>\n")
    +("<P>\n")
    +("<CENTER>\n")
    +("<BUTTON TYPE=BUTTON onClick=\"go_convert(this.form)\"><B>Go "+APPNAME+"</B></BUTTON>\n")
    +("</CENTER>\n")
    +("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Convert(MultipartRequest mrequest,HttpServletResponse response)
      throws IOException
  {
    String ifmt=molReader.getFormat();
    MFileFormat imffmt=MFileFormatUtil.getFormat(ifmt);
    String ofmt=params.getVal("ofmt");
    MFileFormat omffmt=MFileFormatUtil.getFormat(ofmt);

    File fout=null;
    MolExporter molWriter=null;
    String oext=null;
    //kludge - why does this fail for smi?
    try { oext=MFileFormatUtil.getFormat(ofmt).getExtensions()[0]; }
    catch (Exception e) { oext=params.getVal("ofmt"); }
    try {
      File dout=new File(SCRATCHDIR);
      if (!dout.exists())
      {
        boolean ok=dout.mkdir();
        System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
      }
      fout=File.createTempFile(PREFIX,"_out."+oext,dout);
    }
    catch (IOException e) {
      errors.add("ERROR: could not open temp file; check SCRATCHDIR: "+SCRATCHDIR);
      return;
    }

    int n_mols_in=0;
    int n_mols_out=0;
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

      if (n_mols_in==1)
      {
        String ofmt_full="";
        if (omffmt==MFileFormat.SMILES)
        {
          ofmt_full="smiles:";
          if (!params.isChecked("smi_stereo")) ofmt_full+="0";
          if (params.isChecked("smi_uniq")) ofmt_full+="u";
          if (params.isChecked("smi_r1")) ofmt_full+="r1";
          if (params.isChecked("smi_name")) ofmt_full+="n";
          if (params.isChecked("smi_exph")) ofmt_full+="+H";
          else ofmt_full+="-H";
          if (params.isChecked("smi_arom")) ofmt_full+="+a";
          else ofmt_full+="-a";
          if (params.isChecked("smi_sddata"))
          {
            if (params.isChecked("smi_header"))
              ofmt_full+="T";
            else
              ofmt_full+="-T";
            for (int i=0;i<mol.getPropertyCount();++i)
            {
              ofmt_full+=(mol.getPropertyKey(i)+":");
            }
          }
        }
        else if (omffmt==MFileFormat.SDF || omffmt==MFileFormat.MOL)
        {
          ofmt_full="sdf:";
          if (params.isChecked("sdf_v3000")) ofmt_full+="V3";
        }
        else
        {
          ofmt_full=ofmt;
        }
        if (params.isChecked("gzip"))
          molWriter=new MolExporter(new FileOutputStream(fout),"gzip:"+ofmt_full);
        else
          molWriter=new MolExporter(new FileOutputStream(fout),ofmt_full);

        outputs.add("input format:  "+ifmt+" ("+imffmt.getDescription()+")");
        String desc=omffmt.getDescription();
        if (params.isChecked("gzip")) desc+=", GZIPped";
        outputs.add("output format: "+ofmt_full+" ("+desc+")");

        if (params.isChecked("verbose"))
        {
          ByteArrayOutputStream obuff=new ByteArrayOutputStream();
          MolExporter molWriter2 = new MolExporter(obuff,ofmt_full);
          WriteMol(mol,molWriter2,params);
          molWriter2.close();
          errors.add("mol 1:<PRE>"+(new String(obuff.toByteArray(),"utf-8"))+"</PRE>");
        }
      }
      n_mols_out+=WriteMol(mol,molWriter,params);
    }
    molReader.close();
    outputs.add("<H2>Results:</H2>");
    outputs.add("&nbsp;mols in: "+n_mols_in);
    outputs.add("&nbsp;mols out: "+n_mols_out);
    outputs.add("&nbsp;errors: "+n_failed);

    String fname=(SERVLETNAME+"_out."+oext);
    if (params.isChecked("gzip")) fname+=".gz";
    long fsize = fout.length();
    outputs.add("&nbsp;"+
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "download "+fname+" ("+file_utils.NiceBytes(fsize)+")</BUTTON></FORM>\n");

    if (LOGFILE!=null) {
      PrintWriter out_log=new PrintWriter(new BufferedWriter(new FileWriter(LOGFILE,true)));
      out_log.printf("%s\t%s\t%d\n",DATESTR,REMOTEHOST,n_mols_out); 
      out_log.close();
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private static int WriteMol(Molecule mol,MolExporter molWriter,HttpParams params)
      throws IOException
  {
    int n_mols_out=0;
    try
    {
      if (params.isChecked("parts2mols"))
      {
        Molecule[] partmols=mol.cloneMolecule().convertToFrags();
        if (params.isChecked("verbose"))
          errors.add("n_parts: "+partmols.length);
        int i_part=0;
        for (Molecule partmol: partmols)
        {
          ++i_part;
          if (params.isChecked("add_name"))
            partmol.setName(partmol.exportToFormat("name"));
          else
            partmol.setName(mol.getName()+" ("+i_part+")");
          WriteOneMol(partmol,molWriter,params);
          ++n_mols_out;
        }
      }
      else if (params.isChecked("enummarkush"))
      {
        MarkushEnumerationPlugin plugin = new MarkushEnumerationPlugin();
        plugin.setMolecule(mol);
        plugin.run();
        Molecule markemol=plugin.getNextStructure();
        int i_marke=0;
        while (markemol!=null)
        {
          ++i_marke;
          if (params.isChecked("add_name"))
            markemol.setName(markemol.exportToFormat("name"));
          else
            markemol.setName(mol.getName()+" ("+i_marke+")");
          WriteOneMol(markemol,molWriter,params);
          ++n_mols_out;
          if (i_marke==MARKUSH_ENUM_LIMIT)
          {
            errors.add("MARKUSH_ENUM_LIMIT ("+MARKUSH_ENUM_LIMIT+") reached; output truncated.");
            break;
          }
          markemol=plugin.getNextStructure();
        }
      }
      else
      {
        WriteOneMol(mol,molWriter,params);
        ++n_mols_out;
      }
    }
    catch (MolExportException e)
    {
      errors.add("MolExportException: "+e.getMessage());
    }
    catch (NoClassDefFoundError e)	// For InChi
    {
      errors.add("NoClassDefFoundError: "+e.getMessage());
    }
    catch (PluginException e)
    {
      errors.add("PluginException: "+e.getMessage());
    }
    return n_mols_out;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void WriteOneMol(Molecule mol,MolExporter molWriter,HttpParams params)
      throws IOException
  {
    if (params.isChecked("add_2d"))
      mol.clean(2,null,null);	// calculate 2D coords
    else if (params.isChecked("add_3d"))
    {
      mol.clean(3,"S{fine}",null);	// calculate 3D coords
    }
    else if (params.isChecked("add_name"))
      mol.setName(mol.exportToFormat("name"));
    molWriter.write(mol);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript() throws IOException
  {
    return(
"function go_init(form)"+
"{\n"+
"  form.file2txt.checked=false;\n"+
"  form.intxt.value='';\n"+
"  var i;\n"+
"  for (i=0;i<form.ifmt.length;++i)\n"+
"    if (form.ifmt.options[i].value=='automatic')\n"+
"      form.ifmt.options[i].selected=true;\n"+
"  for (i=0;i<form.ofmt.length;++i)\n"+
"    if (form.ofmt.options[i].value=='smiles')\n"+
"      form.ofmt.options[i].selected=true;\n"+
"  form.smi_name.checked=true;\n"+
"  form.smi_stereo.checked=true;\n"+
"  form.smi_arom.checked=true;\n"+
"  form.smi_uniq.checked=false;\n"+
"  form.smi_r1.checked=false;\n"+
"  form.smi_exph.checked=false;\n"+
"  form.smi_sddata.checked=false;\n"+
"  form.smi_header.checked=false;\n"+
"  form.sdf_v3000.checked=false;\n"+
"  form.add_2d.checked=false;\n"+
"  form.add_3d.checked=false;\n"+
"  form.add_name.checked=false;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxt.value && !form.infile.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_convert(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.convert.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String HelpHtm()
	throws java.io.FileNotFoundException,java.io.IOException
  {
    String htm=
    ("<B>"+APPNAME+" help</B><P>\n"+
    "<P>\n"+
    "Converts between molecular formats.\n"+
    "<P>\n"+
    "Additional smiles options:\n"+
    "<UL>\n"+
    "<LI>name - append name to smiles\n"+
    "<LI>arom - aromatic smiles, off = Kekule\n"+
    "<LI>stereo - stereoisomeric smiles\n"+
    "<LI>uniq - canonical/unique smiles (note: canonicalization is\n"+
    "in the context of ChemAxon JChem, and possibly the JChem version)\n"+
    "<LI>r1 - minimum rigor, no checking.  Convert as-is, avoid non-expressibility errors.  Normally use alone.\n"+
    "<LI>+H - specify implicit hydrogens explicitly (e.g. water as [OH2])\n"+
    "<LI>+SDData - SD data appended, result a tab-delimited CSV file\n"+
    "</UL>\n"+
    "<P>\n"+
    "Additional SDF options:\n"+
    "<UL>\n"+
    "<LI>V3000 - new V3000 format (allows &gt;999 atoms); default is V2000.\n"+
    "</UL>\n"+
    "<P>\n"+
    "Additional options:\n"+
    "<UL>\n"+
    "<LI>+2D - add 2D coords\n"+
    "<LI>+3D - add 3D coords (\"fine\" mode)\n"+
    "<LI>+IUPAC name - add IUPAC name\n"+
    "<LI>gzip - gzip output\n"+
    "<LI>parts2mols - disconnected parts become separate molecules.\n"+
    "<LI>enummarkush - enumerate separate molecules from Markush (MRV).\n"+
    "</UL>\n"+
    "<P>\n"+
    "Formats supported by JChem:\n"+
    "<UL>\n");
    for (String fmt: MFileFormatUtil.getMolfileFormats())
    {
      if (fmt.equals("gzip")) continue;
      if (fmt.equals("base64")) continue;
      htm+=("<LI>"+fmt+" - ");
      htm+=(MFileFormatUtil.getFormat(fmt).getDescription()+"\n");
      try { MolExporter test=new MolExporter(new PrintStream("/dev/null"),fmt); }
      catch (MolExportException e) { htm+=(" (read-only)"); }
    }
    htm+=(
    "</UL>\n"+
    "Configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "Thanks to ChemAxon for the use of JChem in this application.\n"+
    "<P>\n"+
    "author: Jeremy Yang\n");
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
    LOGDIR=conf.getInitParameter("LOGDIR");
    if (LOGDIR==null) LOGDIR="/tmp"+CONTEXTPATH+"_logs";
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=1000; }
    try { N_MAX_LINES=Integer.parseInt(conf.getInitParameter("N_MAX_LINES")); }
    catch (Exception e) { N_MAX_LINES=10000; }
    PROXY_PREFIX=((conf.getInitParameter("PROXY_PREFIX")!=null)?conf.getInitParameter("PROXY_PREFIX"):"");
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}
