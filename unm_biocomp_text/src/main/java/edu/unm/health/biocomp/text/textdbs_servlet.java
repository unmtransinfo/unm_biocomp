package edu.unm.health.biocomp.text;

import java.io.*;
import java.net.*; //URLEncoder, InetAddress
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.MultipartRequest;
import com.oreilly.servlet.multipart.DefaultFileRenamePolicy;
import com.oreilly.servlet.*; //Base64Encoder, Base64Decoder

import edu.unm.health.biocomp.http.*;
import edu.unm.health.biocomp.util.*;

/**	Textual Dictionary-based Similarity web app.

	@author Jeremy J Yang
*/
public class textdbs_servlet extends HttpServlet
{
  private static String SERVLETNAME=null;
  private static String CONTEXTPATH=null;
  private static String LOGDIR=null;
  private static String UPLOADDIR=null;	// configured in web.xml
  private static Integer MAX_POST_SIZE=10*1024*1024; // configured in web.xml
  private static String IMGDIRURL=null; // configured in web.xml
  private static String SCRATCHDIR=null; // configured in web.xml
  private static String PREFIX=null;
  private static int scratch_retire_sec=3600;
  private static int N_MAX=100;	// configured in web.xml
  private static ServletContext CONTEXT=null;
  private static ServletConfig config=null;
  private static ResourceBundle rb=null;
  private static PrintWriter out=null;
  private static ArrayList<String> outputs=null;
  private static ArrayList<String> errors=null;
  private static HttpParams params=null;
  //private static int SERVERPORT=0;
  private static String SERVERNAME=null;
  private static String REMOTEHOST=null;
  private static String datestr=null;
  private static File logfile=null;
  private static String color1="#EEEEEE";

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    //SERVERPORT=request.getServerPort();
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
      // REMOTEHOST=request.getRemoteHost(); // client (may be proxy)
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
    ArrayList<String> cssincludes = new ArrayList<String>(Arrays.asList("biocomp.css"));
    ArrayList<String> jsincludes = new ArrayList<String>(Arrays.asList("/marvin/marvin.js","biocomp.js","ddtip.js"));
    boolean ok=initialize(request,mrequest);
    if (!ok)
    {
      response.setContentType("text/html");
      out=response.getWriter();
      out.print(HtmUtils.HeaderHtm(SERVLETNAME,jsincludes,cssincludes,JavaScript(),color1,request));
      out.print(HtmUtils.FooterHtm(errors,true));
    }
    else if (mrequest!=null)		//method=POST, normal operation
    {
      if (mrequest.getParameter("textdbs").equals("TRUE"))
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(formHtm(mrequest,response));
        Date t_i = new Date();
        TextDBS(mrequest,response);
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
      String help=request.getParameter("help");	// GET param
      String downloadtxt=request.getParameter("downloadtxt"); // POST param
      String downloadfile=request.getParameter("downloadfile"); // POST param
      if (help!=null)	// GET method, help=TRUE
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(helpHtm());
        out.println(HtmUtils.FooterHtm(errors,true));
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
        HtmUtils.DownloadFile(response,ostream,downloadfile,
          request.getParameter("fname"));
      }
      else	// GET method, initial invocation of servlet w/ no params
      {
        response.setContentType("text/html");
        out=response.getWriter();
        out.print(HtmUtils.HeaderHtm(SERVLETNAME,jsincludes,cssincludes,JavaScript(),color1,request));
        out.println(formHtm(mrequest,response));
        out.println("<SCRIPT LANGUAGE=\"JavaScript\">");
        out.println("go_reset(window.document.mainform);");
        out.println("</SCRIPT>");
        out.println(HtmUtils.FooterHtm(errors,true));
      }
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private boolean initialize(HttpServletRequest request,MultipartRequest mrequest)
      throws IOException,ServletException
  {
    SERVLETNAME=this.getServletName();
    outputs = new ArrayList<String>();
    errors = new ArrayList<String>();
    params = new HttpParams();
    Calendar calendar=Calendar.getInstance();

    errors.add("<A HREF=\"http://medicine.unm.edu/informatics/\">"+
      "<IMG BORDER=0 SRC=\""+IMGDIRURL+"/biocomp_logo_only.gif\"></A>"+
      SERVLETNAME+" web app from UNM Translational Informatics.");

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
    }




    ArrayList<String> terms=new ArrayList<String>();

    File fileQ=mrequest.getFile("infileq");
    String intxtQ=params.getVal("intxtq").replaceFirst("[\\s]+$","");
    byte[] txtbytes = new byte[1024];
    if (fileQ!=null)
    {
      FileInputStream fis=new FileInputStream(fileQ);
      int asize=txtbytes.length;
      int size=0;
      int b;
      while ((b=fis.read())>=0)
      {
        if (size+1>asize)
        {
          asize*=2;
          byte[] tmp=new byte[asize];
          System.arraycopy(txtbytes,0,tmp,0,size);
          txtbytes=tmp;
        }
        txtbytes[size]=(byte)b;
        ++size;
      }
      fileQ.delete();
      byte[] tmp=new byte[size];
      System.arraycopy(txtbytes,0,tmp,0,size);
      txtbytes=tmp;
      if (params.isChecked("file2txt"))
      {
        intxtQ=new String(txtbytes,"utf-8");
        params.setVal("intxtq",intxtQ);
      }
    }
    else if (intxtQ.length()>0)
    {
      txtbytes=intxtQ.getBytes("utf-8");
    }
    if (txtbytes==null)
    {
      errors.add("ERROR: no input terms");
      return false;
    }
    intxtQ=new String(txtbytes,"utf-8");
    buff=new BufferedReader(new StringReader(intxtQ));
    terms.clear();
    while ((line=buff.readLine())!=null)
    {
      terms.add(line);
      if (terms.size()==N_MAX)
      {
        errors.add("N_MAX limit reached: "+N_MAX);
        break;
      }
    }
    if (params.isChecked("verbose"))
      errors.add("terms read:  "+terms.size());
    





    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String formHtm(MultipartRequest mrequest,HttpServletResponse response)
	throws IOException
  {

    String htm="";
    htm+=("<FORM NAME=\"mainform\" METHOD=POST");
    htm+=(" ACTION=\""+response.encodeURL(SERVLETNAME)+"\"");
    htm+=(" ENCTYPE=\"multipart/form-data\">\n");
    htm+=("<TABLE WIDTH=\"100%\"><TR><TD><H2>"+SERVLETNAME+"</H2></TD>\n");
    htm+=("<TD ALIGN=RIGHT>\n");
    htm+=("<BUTTON TYPE=BUTTON onClick=\"void window.open('"+response.encodeURL(SERVLETNAME)+"?help=TRUE','helpwin','width=600,height=400,scrollbars=1,resizable=1')\"><B>help</B></BUTTON>\n");
    htm+=("<BUTTON TYPE=BUTTON onClick=\"go_reset(this.form)\">\n");
    htm+=("<B>clear</B></BUTTON>\n");
    htm+=("</TD></TR></TABLE>\n");
    htm+=("<HR>\n");
    htm+=("<INPUT TYPE=HIDDEN NAME=\"textdbs\">\n");
    htm+=("<TABLE WIDTH=\"100%\" CELLPADDING=5 CELLSPACING=5>\n");
    htm+=("<TR BGCOLOR=\"#CCCCCC\"><TD VALIGN=TOP>\n");
    htm+=("<INPUT TYPE=CHECKBOX NAME=\"file2txt\" VALUE=\"CHECKED\"");
    htm+=(" "+params.getVal("file2txt")+">file2txt<BR>\n");
    htm+=("upload: <INPUT TYPE=\"FILE\" NAME=\"infileq\"> ...or paste:\n");
    htm+=("<BR><TEXTAREA NAME=\"intxtq\" WRAP=OFF ROWS=12 COLS=60>");
    htm+=(params.getVal("intxtq"));
    htm+=("</TEXTAREA>\n");
    htm+=("</TD>\n");
    htm+=("<TD VALIGN=TOP>\n");
    htm+=("<B>output:</B>\n");
    htm+=("<P>\n");
    htm+=("<HR>\n");
    htm+=("<B>misc:</B><BR>\n");
    htm+=("<INPUT TYPE=CHECKBOX NAME=\"verbose\" VALUE=\"CHECKED\"");
    htm+=(" "+params.getVal("verbose")+">verbose<BR>\n");
    htm+=("</TD></TR></TABLE>\n");
    htm+=("<P>\n");
    htm+=("<CENTER>\n");
    htm+=("<BUTTON TYPE=BUTTON onClick=\"go_textdbs(this.form)\">\n");
    htm+=("<B>textdbs</B></BUTTON>\n");
    htm+=("</CENTER>\n");
    htm+=("</FORM>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void TextDBS(MultipartRequest mrequest,HttpServletResponse response)
      throws IOException
  {

    File fout=null;
    try {
      File dout=new File(SCRATCHDIR);
      if (!dout.exists())
      {
        boolean ok=dout.mkdir();
        System.err.println("SCRATCHDIR creation "+(ok?"succeeded":"failed")+": "+SCRATCHDIR);
      }
      fout=File.createTempFile(PREFIX,"_out.txt",dout);
    }
    catch (IOException e) {
      errors.add("ERROR: could not open temp file; check SCRATCHDIR: "+SCRATCHDIR);
      return;
    }

    int n_in=0;
    int n_out=0;

    int n_failed=0;
    while (true)
    {
      if (n_in==N_MAX)
      {
        errors.add("Limit reached: N_MAX mols: "+N_MAX);
        break;
      }

      ++n_in;



    }
    outputs.add("<B>RESULT</B>:");
    outputs.add("&nbsp;in: "+n_in);
    outputs.add("&nbsp;out: "+n_out);
    outputs.add("&nbsp;errors: "+n_failed);

    String fname=(SERVLETNAME+"_out.csv");
    long fsize = fout.length();
    outputs.add("&nbsp;"+
      "<FORM METHOD=\"POST\" ACTION=\""+response.encodeURL(SERVLETNAME)+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"downloadfile\" VALUE=\""+fout.getAbsolutePath()+"\">\n"+
      "<INPUT TYPE=HIDDEN NAME=\"fname\" VALUE=\""+fname+"\">\n"+
      "<BUTTON TYPE=BUTTON onClick=\"this.form.submit()\">"+
      "download "+fname+" ("+file_utils.NiceBytes(fsize)+")</BUTTON></FORM>\n");

    PrintWriter out_log=new PrintWriter(
      new BufferedWriter(new FileWriter(logfile,true)));
    out_log.printf("%s\t%s\t%d\n",datestr,REMOTEHOST,n_out); 
    out_log.close();
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String JavaScript() throws IOException
  {
    return(
"function go_reset(form)"+
"{\n"+
"  form.file2txt.checked=false;\n"+
"  form.intxtq.value='';\n"+
"  var i;\n"+
"  form.verbose.checked=false;\n"+
"}\n"+
"function checkform(form)\n"+
"{\n"+
"  if (!form.intxtq.value && !form.infileq.value) {\n"+
"    alert('ERROR: No input molecules specified');\n"+
"    return 0;\n"+
"  }\n"+
"  return 1;\n"+
"}\n"+
"function go_textdbs(form)\n"+
"{\n"+
"  if (!checkform(form)) return;\n"+
"  form.textdbs.value='TRUE';\n"+
"  form.submit()\n"+
"}\n"
    );
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String helpHtm()
	throws java.io.FileNotFoundException,java.io.IOException
  {
    String htm=
    ("<B>"+SERVLETNAME+" help</B><P>\n"+
    "<P>\n"+
    "Configured with molecule limit N_MAX = "+N_MAX+"\n"+
    "<P>\n"+
    "<P>\n"+
    "author: Jeremy Yang\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void init(ServletConfig conf) throws ServletException
  {
    super.init(conf);
    // read servlet parameters (from web.xml):
    UPLOADDIR=conf.getInitParameter("UPLOADDIR");
    if (UPLOADDIR==null)
      throw new ServletException("Please supply UPLOADDIR parameter");
    SCRATCHDIR=conf.getInitParameter("SCRATCHDIR");
    if (SCRATCHDIR==null) SCRATCHDIR="/tmp";
    LOGDIR=conf.getInitParameter("LOGDIR")+CONTEXTPATH;
    if (LOGDIR==null) LOGDIR="/usr/local/tomcat/logs"+CONTEXTPATH;
    IMGDIRURL=conf.getInitParameter("IMGDIRURL");
    if (IMGDIRURL==null) IMGDIRURL="/images";
    try { N_MAX=Integer.parseInt(conf.getInitParameter("N_MAX")); }
    catch (Exception e) { N_MAX=1000; }
    CONTEXT=getServletContext();	// inherited method
    config=conf;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    doPost(request,response);
  }
}
