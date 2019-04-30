package edu.unm.health.biocomp.freechart;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.Color;
import java.awt.Font;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jfree.chart.*; //ChartFactory, JFreeChart, ChartColor, ChartUtilities, ChartRenderingInfo
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.*; //ValueAxis,CategoryAxis
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.data.category.DefaultCategoryDataset;

//From jcommon*.jar:
import org.jfree.ui.*; //RectangleEdge, RectangleInsets, TextAnchor
import org.jfree.util.UnitType;

/**	Bar chart depicts frequencies for categorical values.
	Generate PNG or JPEG image for inline display.

	<br>
	The following form inputs are allowed:
	<ul>
	<li>w - width of image
	<li>h - height of image
	<li>bgcolor - background color
	<li>fgcolor - foreground color (bars)
	<li>border -
	<li>title -
	<li>subtitle -
	<li>labels - for each bin, comma-separated
	<li>values - for each bin, the frequency, comma-separated
	<li>iconic - small, unadorned
	<li>imgfmt - "PNG" (default) or "JPEG"
	</ul>
	e.g.: ?w=700&h=300&fgcolor=x0088CC&title=rainfall&values=4,9,9,1,1,2&labels=NM,IN,NY,LA,CA,AZ

	@author Jeremy Yang
*/
public class barchartimg_servlet extends HttpServlet
{
  private static String imgfmt=null;
  private static String title=null;
  private static String subtitle=null;
  private static Integer width=null;
  private static Integer height=null;
  private static String xaxis=null;
  private static String yaxis=null;
  private static Color fgcolor=null;
  private static Color bgcolor=null;
  private static Boolean iconic=null;
  private static ArrayList<Integer> values = null;
  private static Integer y_max=null;
  private static ArrayList<String> labels = null; //barchart-specific

  public void initialize(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    // Common params:

    imgfmt=request.getParameter("imgfmt");
    imgfmt=(imgfmt!=null&&(imgfmt.equalsIgnoreCase("jpeg")||imgfmt.equalsIgnoreCase("jpg")))?"jpeg":"png";

    title=request.getParameter("title");
    subtitle=request.getParameter("subtitle");

    try { width=Integer.parseInt(request.getParameter("w")); }
    catch (NumberFormatException e) { width=240; }
    try { height=Integer.parseInt(request.getParameter("h")); }
    catch (NumberFormatException e) { height=160; }

    xaxis=request.getParameter("xaxis");
    yaxis=request.getParameter("yaxis");

    String fgcolorstr=request.getParameter("fgcolor");
    if (fgcolorstr==null) fgcolorstr=request.getParameter("color");
    fgcolor=RGB2Color(fgcolorstr);
    if (fgcolorstr==null) fgcolor=ChartColor.CYAN;

    String bgcolorstr=request.getParameter("bgcolor");
    bgcolor=RGB2Color(bgcolorstr);
    if (bgcolorstr==null) bgcolor=ChartColor.WHITE;

    iconic=(request.getParameter("iconic")!=null?true:false);

    String valuestr=request.getParameter("values");
    values = new ArrayList<Integer>();
    if (valuestr!=null)
    {
      for (String vstr: Pattern.compile(",").split(valuestr))
      {
        Integer val;
        try { val=Integer.parseInt(vstr); }
        catch (NumberFormatException e) { val=0; }
        values.add(val);
        if (y_max==null || (val!=null && val>y_max)) y_max=val;
      }
    }

    // Barchart-specific:

    labels = new ArrayList<String>();
    String labelstr=request.getParameter("labels");
    if (labelstr!=null)
    {
      for (String lstr: Pattern.compile(",").split(labelstr))
      {
        labels.add(lstr);
      }
    }
    
    if (values.size()==0)
      throw new ServletException("values.size()==0");
    else if (values.size()!=labels.size())
      throw new ServletException("values.size()!=labels.size()");
  }

  /////////////////////////////////////////////////////////////////////////////
  public Color RGB2Color(String colorcode)
  {
    int r=128;
    int g=128;
    int b=128;
    if (colorcode==null) return null;
    if (Pattern.matches("^[xX][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]$",colorcode))
    {
      r=Integer.parseInt(colorcode.substring(1,3),16);
      g=Integer.parseInt(colorcode.substring(3,5),16);
      b=Integer.parseInt(colorcode.substring(5,7),16);
    }
    else return null;
    return new Color(r,g,b);
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doGet(HttpServletRequest request,HttpServletResponse response)
      throws IOException,ServletException
  {
    initialize(request,response);

    // Done with http params.  Create barchart with JFreeChart.

    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (int i=0;i<values.size();++i)
    {
      Double x = 0.0;
      try { x = new Double(values.get(i)); }
      catch (Exception e) { }
      dataset.addValue(x,"",labels.get(i));
    }

    JFreeChart chart = ChartFactory.createBarChart(
	title,
	xaxis,
	yaxis,
	dataset,
	(request.getParameter("horizontal")!=null)?PlotOrientation.HORIZONTAL:PlotOrientation.VERTICAL,
	(request.getParameter("legend")!=null),
	true, // Show tooltips
	true  // Show url
	);

    if (subtitle!=null)
      chart.addSubtitle(new TextTitle(subtitle,
        new Font(Font.SANS_SERIF,Font.ITALIC,10)));

    chart.setBackgroundPaint(bgcolor);
    chart.setBorderVisible(request.getParameter("border")!=null);

    CategoryPlot plot = (CategoryPlot) chart.getPlot();
    CategoryItemRenderer renderer = plot.getRenderer();

    renderer.setSeriesPaint(0,fgcolor);

    CategoryAxis axX=plot.getDomainAxisForDataset(0);
    ValueAxis axY=plot.getRangeAxisForDataset(0);

    TextTitle ttitle = chart.getTitle();
    // if (ttitle!=null) ttitle.setPosition(RectangleEdge.BOTTOM);

    if (iconic || width<200)
    {
      chart.setPadding(new RectangleInsets(UnitType.ABSOLUTE,0,0,0,0));
      chart.setBorderVisible(false);
      plot.setOutlineVisible(false);
      axX.setLowerMargin(0.0);
      axX.setUpperMargin(0.0);
      double xmax_ax=axX.getLowerMargin();
      double xmin_ax=axX.getUpperMargin();
      double ymax_ax=axY.getUpperBound();
      double ymin_ax=axY.getLowerBound();
      axX.setTickLabelsVisible(false);
      axX.setTickMarksVisible(false);
      axX.setVisible(false);
      axY.setTickLabelsVisible(false);
      axY.setTickMarksVisible(false);
      axY.setVisible(false);

      CategoryTextAnnotation txtann;

      if (ttitle!=null)
      {
        ttitle.setVisible(false);

        //txtann=new CategoryTextAnnotation(ttitle.getText(),xmax_ax-xmin_ax,ymin_ax);
        //txtann.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,9));
        //txtann.setPaint(Color.white);
        //txtann.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        //plot.addAnnotation(txtann);
      }
    }
    else
    {
      for (int i=0;i<values.size();++i)
      {
        List<Comparable> cats=(List<Comparable>)(plot.getCategories());
        Integer y=values.get(i);
        CategoryTextAnnotation txtann=new CategoryTextAnnotation(y.toString(),cats.get(i),y);
        txtann.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        txtann.setPaint(Color.black);
        if (y_max!=null && (float)y/y_max>0.9)
          txtann.setTextAnchor(TextAnchor.TOP_CENTER);
        else
          txtann.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        plot.addAnnotation(txtann);
      }
    }

    // output:
    ChartRenderingInfo crinfo=null;
    boolean alpha=true;
    int pngz=4;	//compression [0-9]
    float jpgq=0.50f;	//quality [0-1]
    ServletOutputStream ostream=response.getOutputStream();
    if (imgfmt.equalsIgnoreCase("jpeg"))
    {
      response.setContentType("image/jpeg");
      ChartUtilities.writeChartAsJPEG(ostream,jpgq,chart,width,height,crinfo);
    }
    else
    {
      response.setContentType("image/png");
      ChartUtilities.writeChartAsPNG(ostream,chart,width,height,crinfo,alpha,pngz);
    }
    ostream.flush();
    ostream.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  public void doPost(HttpServletRequest request,HttpServletResponse response)
    throws IOException,ServletException
  {
    doGet(request,response);
  }
}
