package edu.unm.health.biocomp.molcloud;

import java.io.*;
import java.text.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;


import edu.unm.health.biocomp.http.*;

/**	Outputs PNG, JPEG, or GIF image from file for inline display.
	<br>
	@author Jeremy J Yang
*/
public class showimg_servlet extends HttpServlet
{
  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
	throws IOException,ServletException
  {
    ResourceBundle rb=ResourceBundle.getBundle("LocalStrings",request.getLocale());

    String imgfmt=request.getParameter("imgfmt");
    if (imgfmt==null) imgfmt="png";
    response.setContentType("image/"+imgfmt);

    String imgfile=request.getParameter("imgfile");
    if (imgfile==null) System.exit(1);
    byte[] imgbytes = new byte[1024];
    FileInputStream fis = new FileInputStream(new File(imgfile));
    int asize=imgbytes.length;
    int size=0;
    int b;
    while ((b=fis.read())>=0)
    {
      if (size+1>asize)
      {
        asize*=2;
        byte[] tmp = new byte[asize];
        System.arraycopy(imgbytes,0,tmp,0,size);
        imgbytes=tmp;
      }
      imgbytes[size]=(byte)b;
      ++size;
    }
    byte[] tmp = new byte[size];
    System.arraycopy(imgbytes,0,tmp,0,size);
    imgbytes=tmp;

    // output:
    ServletOutputStream ostream=response.getOutputStream();
    ostream.write(imgbytes);
    ostream.close();
  }
  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
	throws IOException,ServletException
  { doGet(request,response); }
}
