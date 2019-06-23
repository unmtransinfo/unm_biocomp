package edu.unm.health.biocomp.util.http;

import java.io.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;
import java.net.URLEncoder;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.codec.binary.Base64;

/**	Static methods for web apps.
	Molfiles etc. are gzipped and base64ed by calling program.
	@author Jeremy J Yang
*/
public class HtmUtils
{
  private HtmUtils() {} //disable default constructor
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a image from smiles.
  */
  public static String Smi2ImgHtm(String smiles,String opts,int h,int w,
    String mol2img_url,boolean zoomable,int zoomfactor,String jsfunc)
  {
    if (opts.length()==0) opts="mode=cow";
    String smi=null;
    try { smi=URLEncoder.encode(smiles,"UTF-8"); }
    catch (UnsupportedEncodingException e) { }
    String imgurl=(mol2img_url+"?"+opts+"&h="+h+"&w="+w+"&smiles="+smi);
    String imghtm=("<IMG SRC='"+imgurl+"' BORDER=0>");
    if (zoomable)
    {
      imghtm=String.format("<A HREF=\"javascript:void(0)\" onClick=\"javascript:%s('%s','%s','%s',%d,%d)\">%s</A>",jsfunc,mol2img_url,smi,opts+"&maxscale=0",w*zoomfactor,h*zoomfactor,imghtm);
    }
    return imghtm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a image from molfile.  Parameter molfile unencoded.
  */
  public static String Molfile2ImgHtm(String molfile,String opts,int h,int w,
    String mol2img_url,boolean zoomable,int zoomfactor,String jsfunc)
  {
    String mdlcode = "";
    try { mdlcode = Text2GzBase64(molfile); }
    catch (IOException e) { }
    String htm=Mdlcode2ImgHtm(mdlcode,opts,h,w,mol2img_url,zoomable,zoomfactor,jsfunc);
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static String Text2GzBase64(String molfile)
	throws IOException
  {
    byte[] mdlbytes = molfile.getBytes("UTF-8");
    ByteArrayOutputStream bytestream = new ByteArrayOutputStream(4096);
    GZIPOutputStream gzer = new GZIPOutputStream(bytestream,4096,true);
    try {
      gzer.write(mdlbytes,0,mdlbytes.length);
      gzer.finish();
    }
    catch (UnsupportedEncodingException e) { }
    String mdlcode=Base64.encodeBase64String(bytestream.toByteArray());
    return mdlcode;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a image from molfile.  Parameter mdlcode should be
	gzip-base64 encoded molfile.
  */
  public static String Mdlcode2ImgHtm(String mdlcode,String opts,int h,int w,
    String mol2img_url,boolean zoomable,int zoomfactor,String jsfunc)
  {
    if (opts.length()==0) opts="mode=cow";
    try { mdlcode=URLEncoder.encode(mdlcode,"UTF-8"); }
    catch (UnsupportedEncodingException e) { }
    String imgurl=(mol2img_url+"?"+opts+"&h="+h+"&w="+w+"&mdlcode="+mdlcode);
    String imghtm=("<IMG SRC='"+imgurl+"' BORDER=0>");
    if (zoomable)
    {
      imghtm=String.format("<A HREF=\"javascript:void(0)\" onClick=\"javascript:%s('%s','%s','%s',%d,%d)\">%s</A>",jsfunc,mol2img_url,mdlcode,opts+"&maxscale=0",w*zoomfactor,h*zoomfactor,imghtm);
    }
    return imghtm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a image from mrvfile.  Parameter mrvfile unencoded.
  */
  public static String Mrvfile2ImgHtm(String mrvfile,ArrayList<Color> atomcolors,
	String opts,int h,int w,
	String mol2img_url,boolean zoomable,int zoomfactor,String jsfunc)
	throws IOException
  {
    String mrvcode = Text2GzBase64(mrvfile);
    return Mrvcode2ImgHtm(mrvcode,atomcolors,opts,h,w,mol2img_url,zoomable,zoomfactor,jsfunc);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a image from mrvfile (ChemAxon mrv format).
	Parameter mrvcode should be gzip-base64 encoded mrvfile.
  */
  public static String Mrvcode2ImgHtm(String mrvcode,ArrayList<Color> atomcolors,
	String opts,int h,int w,
	String mol2img_url,boolean zoomable,int zoomfactor,String jsfunc)
	throws IOException
  {
    if (opts.length()==0) opts="mode=cow";
    mrvcode=URLEncoder.encode(mrvcode,"UTF-8");
    if (atomcolors!=null)
    {
      opts+=("&atomcolors=true");
      for (int i=0;i<atomcolors.size();++i)
      {
        Color c=atomcolors.get(i);
        opts+=("&color"+i+"="+String.format("%02X%02X%02X,",c.getRed(),c.getGreen(),c.getBlue()));
      }
    }
    String imgurl=(mol2img_url+"?"+opts+"&h="+h+"&w="+w+"&mrvcode="+mrvcode);
    String imghtm=("<IMG SRC='"+imgurl+"' BORDER=0>");
    if (zoomable)
    {
      imghtm=String.format("<A HREF=\"javascript:void(0)\" onClick=\"javascript:%s('%s','%s','%s',%d,%d)\">%s</A>",jsfunc,mol2img_url,mrvcode,opts+"&maxscale=0",w*zoomfactor,h*zoomfactor,imghtm);
    }
    return imghtm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a histogram.
  */
  public static String HistoImgHtm(List<Integer> vals,List<Double> xmaxs,String opts,int w,int h,
    String histoservlet_url,boolean zoomable,int zoomfactor,String jsfunc)
	throws UnsupportedEncodingException
  {
    String vals_str="";
    for (int i: vals) vals_str+=(""+i+",");

    String xmaxs_str="";
    for (double x: xmaxs) xmaxs_str+=(""+x+",");

    opts+="&values="+URLEncoder.encode(vals_str,"UTF-8");
    opts+="&xmaxs="+URLEncoder.encode(xmaxs_str,"UTF-8");
    String imgurl=(histoservlet_url+"?"+opts+"&h="+h+"&w="+w);
    String htm=("<IMG SRC='"+imgurl+"' BORDER=0>");
    if (zoomable)
    {
      htm=String.format("<A HREF=\"javascript:void(0)\" onClick=\"javascript:%s('%s','%s',%d,%d,'%s')\">%s</A>",jsfunc,histoservlet_url,opts,w*zoomfactor,h*zoomfactor,histoservlet_url,htm);
    }
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	HTML for a barchart.
  */
  public static String BarchartImgHtm(List<Integer> vals,List<String> labels,String opts,int w,int h,
    String barchartservlet_url,boolean zoomable,int zoomfactor,String jsfunc)
	throws UnsupportedEncodingException
  {
    String vals_str="";
    for (int i: vals) vals_str+=(""+i+",");

    String labels_str="";
    for (String x: labels) labels_str+=(""+x+",");

    opts+="&values="+URLEncoder.encode(vals_str,"UTF-8");
    opts+="&labels="+URLEncoder.encode(labels_str,"UTF-8");
    String imgurl=(barchartservlet_url+"?"+opts+"&h="+h+"&w="+w);
    String htm=("<IMG SRC='"+imgurl+"' BORDER=0>");
    if (zoomable)
    {
      htm=String.format("<A HREF=\"javascript:void(0)\" onClick=\"javascript:%s('%s','%s',%d,%d,'%s')\">%s</A>",jsfunc,barchartservlet_url,opts,w*zoomfactor,h*zoomfactor,barchartservlet_url,htm);
    }
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Header HTML with included JavaScript and CSS.

	New: No more hard-coding of specific JS or CSS files in
	this function.  Use jsincludes and cssincludes for all.
	Also no JS code here.

	If jsinclude|cssinclude bare filename (no '/'),
	prepend with conventional URL prefix, e.g. "/tomcat/biocomp/js".
	If path specified, then include unmodified (could be via
	Tomcat, httpd, or external URL).

	proxy_prefix normally "tomcat", "jetty", etc., with Apache proxy to
	port 8080 or 8081, etc.
  */
  public static String HeaderHtm(String title,
	List<String> jsincludes, List<String> cssincludes,
	String js, String css,
	String color, HttpServletRequest request,
	String proxy_prefix)
  {
    String htm=(
    "<!DOCTYPE html>\n"+
    "<HTML>\n"+
    "<HEAD><TITLE>"+title+"</TITLE>\n");
    if (cssincludes!=null)
    {
      for (String cssinclude: cssincludes)
      {
        if (cssinclude.contains("/"))
          htm+=("<LINK REL=\"stylesheet\" HREF=\""+cssinclude+"\" />\n");
        else
          htm+=("<LINK REL=\"stylesheet\" type=\"text/css\" HREF=\"/"+proxy_prefix+request.getContextPath()+"/css/"+cssinclude+"\" />\n");
      }
    }
    if (css!=null && !css.isEmpty())
      htm+=("<STYLE TYPE=\"text/css\">\n"+css+"\n</STYLE>\n");
    if (jsincludes!=null)
    {
      for (String jsinclude: jsincludes)
      {
        if (jsinclude.contains("/"))
          htm+=("<SCRIPT SRC=\""+jsinclude+"\"></SCRIPT>\n");
        else
          htm+=("<SCRIPT SRC=\"/"+proxy_prefix+request.getContextPath()+"/js/"+jsinclude+"\"></SCRIPT>\n");
      }
    }
    if (js!=null && !js.isEmpty())
      htm+=("<SCRIPT>\n"+js+"\n</SCRIPT>\n");
    htm+=("</HEAD>\n"+"<BODY BGCOLOR=\""+color+"\">\n");
    htm+=("<DIV ID=\"ddtooltip\"></DIV>\n"+"<SCRIPT TYPE=\"text/javascript\">ddtip_init()</SCRIPT>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Legacy entry point.
  */
  public static String HeaderHtm(String title,
	List<String> jsincludes, List<String> cssincludes,
	String js, String css,
	String color, HttpServletRequest request)
  {
    return HeaderHtm(title, jsincludes, cssincludes, js, css, color, request, "tomcat");
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Output lines to HTML delimited by line breaks.
  */
  public static String OutputHtm(ArrayList<String> outputs)
  {
    String htm="";
    htm+=("<HR>\n");
    if (outputs.size()>0)
    {
      for (String output:outputs)
      {
        htm+=(output+"<BR>\n");
      }
    }
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Output lines to HTML delimited by line breaks; terminate HTML.
  */
  public static String FooterHtm(ArrayList<String> errors, boolean showErrors)
  {
    String htm="";
    htm+=("<HR>\n");
    if (showErrors && errors.size()>0)
    {
      for (String error: errors)
        htm+=(error+"<BR>\n");
    }
    htm+=("</BODY>\n");
    htm+=("</HTML>\n");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return URL for "SRC" field.
  */
  public static String ImageURL(String fname, HttpServletRequest request)
  {
    return ("/tomcat"+request.getContextPath()+"/images/"+fname);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Download specified string to browser with specified file name.
  */
  public static boolean DownloadString(HttpServletResponse response,
        ServletOutputStream ostream,String str,String fname)
        throws IOException
  {
    //byte[] outbytes=Base64Decoder.decodeToBytes(str) ;
    byte[] outbytes=Base64.decodeBase64(str);
    response.setContentType("application/x-savefile"); //which is better?
    //response.setContentType("application/octet-stream"); //which is better?
    response.setContentLength(outbytes.length);
    response.setDateHeader("Expires",0);
    response.setHeader("Pragma","No-cache");
    response.setHeader("Cache-Control","no-cache");
    response.setHeader("Content-Disposition","attachment; filename="+fname);
    ostream.write(outbytes);
    ostream.flush();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Download specified file to browser with specified file name.
  */
  public static boolean DownloadFile(HttpServletResponse response,
        ServletOutputStream ostream,String fpath,String fname)
        throws IOException
  {
    File f = new File(fpath);
    if (!f.exists()) return false;
    long size = f.length();
    response.setContentType("application/x-savefile"); //which is better?
    //response.setContentType("application/octet-stream"); //which is better?
    response.setContentLength((int)size);
    response.setDateHeader("Expires",0);
    response.setHeader("Pragma","No-cache");
    response.setHeader("Cache-Control","no-cache");
    response.setHeader("Content-Disposition","attachment; filename="+fname);
    FileInputStream fis=new FileInputStream(f);
    int b;
    while ((b=fis.read())>=0) ostream.write(b);
    ostream.flush();
    return true;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate HTML with tooltip. Requires corresponding JavaScript.
  */
  public static String HtmTipper(String htm_in,String tiphtm,String href,Integer width,String color,String frame)
  {
    int w=160;
    String c="yellow";
    if (width!=null) w=width;
    if (color!=null) c=color;
    tiphtm=tiphtm.replace("'","\\\\\\'");
    tiphtm=tiphtm.replace("\"","\\\"");
    String htm=("<A onMouseOver=\""
	+((frame==null||frame.isEmpty())?"":frame+".")
	+"dd_tip('"+tiphtm+"','"+c+"',"+w+")\"");
    if (href!=null && !href.isEmpty())
      htm+=(" HREF=\""+href+"\"");
    htm+=(" onMouseOut=\""
	+((frame==null||frame.isEmpty())?"":frame+".")
	+"dd_hidetip()\">"+htm_in+"</A>");
    return htm;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate HTML with tooltip. Requires corresponding JavaScript.
  */
  public static String HtmTipper(String htm_in,String tiphtm,String href,Integer width,String color)
  {
    return HtmTipper(htm_in,tiphtm,href,width,color,"");
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate HTML with tooltip. Requires corresponding JavaScript.
  */
  public static String HtmTipper(String htm_in,String tiphtm,Integer width,String color)
  {
    return HtmTipper(htm_in,tiphtm,null,width,color);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Delete temporary files; if non-existent no worries..
  */
  public static boolean DelFiles(ArrayList<String> files)
  {
    boolean ok=true;
    for (String file: files)
    {
      File f = new File(file);
      if (f.exists())
      {
        if (!f.delete()) {
          ok=false;
          //System.err.println("DEBUG: failed to delete: "+file);
        }
      }
    }
    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Deprecated .
  */
  public static boolean PurgeScratchDirs(String [] dirs,int retire_sec,boolean verbose,
	String delim,HttpServlet servlet)
  {
    return PurgeScratchDirs(Arrays.asList(dirs),retire_sec,verbose,delim,servlet);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	For purging temporary files created by
	webapps (HttpServlet servlets), where the names indicate creation time.
	Note that timestamp is expected to be in format YYYYMMDDHHMMSS.
	@param dirs Directories to purge.
	@param retire_sec How old files must be to purge.
	@param verbose If true, log result to HttpServlet context.
	@param delim Filename delimiter defining timestamp.
	@param servlet HttpServlet calling object.
  */
  public static boolean PurgeScratchDirs(List<String> dirs,int retire_sec,boolean verbose,
	String delim,HttpServlet servlet)
  {
    ServletContext context=servlet.getServletContext();
    String servletname=servlet.getServletName();
    boolean ok=true;
    Calendar calendar=Calendar.getInstance();
    int ndeleted=0;
    int ntotal=0;
    for (String dir: dirs)
    {
      File d = new File(dir);
      File[] files = d.listFiles();
      if (files==null) continue;
      for (File f: files)
      {
        ++ntotal;
        String fname=f.getName();
        String[] fields=Pattern.compile("\\.").split(fname);
        if (fields.length<3) continue;
        String datestr=fields[1];
        Date date_file=null;
        if (!Pattern.matches("\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d$",datestr))
        {
          if (verbose)
            context.log(String.format("%s: ERROR: bad date format: \"%s\"",servletname,datestr));
          ok=false;
          date_file=new Date(f.lastModified());
        }
        else
        {
          calendar.set(Integer.parseInt(datestr.substring(0,4)),
               Integer.parseInt(datestr.substring(4,6))-1,
               Integer.parseInt(datestr.substring(6,8)),
               Integer.parseInt(datestr.substring(8,10)),
               Integer.parseInt(datestr.substring(10,12)),
               Integer.parseInt(datestr.substring(12,14)));
          date_file=calendar.getTime();
        }
        Date date_now = new Date();
        long delta_s = (date_now.getTime()-date_file.getTime())/1000;
        if (delta_s>retire_sec)
        {
          if (f.canWrite())
          {
            if (f.delete())
              ++ndeleted;
            else
              context.log(String.format("%s: ERROR: failed to delete \"%s\"",servletname,fname));
          }
          else
          {
            if (verbose)
              context.log(String.format("%s: ERROR: cannot delete \"%s\"",servletname,fname));
          }
        }
      }
    }
    if (verbose)
      context.log(String.format("%s: %d/%d scratch files deleted",servletname,ndeleted,ntotal));
    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Replace HTML control characters with escape codes.
  */
  public static String HtmlEscape(String aText)
  {
    if (aText == null) { return ""; }
    StringBuffer escapedText = new StringBuffer();
    for (int i=0; i<aText.length(); i++) {
      char ch = aText.charAt(i);
      if (ch == '\'') escapedText.append("&#39;");
      else if (ch == '\"') escapedText.append("&#34;");
      else if (ch == '<') escapedText.append("&lt;");
      else if (ch == '>') escapedText.append("&gt;");
      else if (ch == '&') escapedText.append("&amp;");
      else if (ch == '=') escapedText.append("&#61;");
      else                escapedText.append(ch);
    }
    return escapedText.toString();
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Used for automated testing.  Return TSV, 2 cols, key-val, all values
	should be non-false (evaluate to true as Python strings).
	Minimally one row: "ok<TAB>1".  Values evaluated according to
	Python rules: ("", "0", "False", "F"), case insensitive, evaluated 
	to false, else true.
  */
  public static String TestTxt(String appname,Map<String,String> t)
  {
    String txt="";
    txt+=("#Test output.  TSV, key\\tval.  appname and ok are required, any others optional.\n");
    txt+=("APPNAME\t"+appname+"\n");
    txt+=("OK\t1\n");
    for (String key: t.keySet())
      txt+=(key+"\t"+t.get(key)+"\n");
    return txt;
  }
}
