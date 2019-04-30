package edu.unm.health.biocomp.freechart;

import java.io.*;
import java.lang.Math;
import java.util.*;
import java.util.regex.*;
import java.awt.Color;
import java.awt.Font;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jfree.chart.*; //ChartFactory, JFreeChart, ChartColor, ChartUtilities, ChartRenderingInfo
import org.jfree.chart.plot.*; //XYPlot, PlotOrientation
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.data.statistics.*; //SimpleHistogramDataset, SimpleHistogramBin

//From jcommon*.jar:
import org.jfree.ui.*; //RectangleEdge, RectangleInsets, TextAnchor
import org.jfree.util.UnitType;

/**	A histogram is a binned frequency distribution of continuous values.
	Generates PNG or JPEG image for inline display.
	See barchartimg_servlet for frequency distribution of categorial values.
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
	<li>xmaxs - for each bin, the max X values, comma-separated
	<li>values - for each bin, the frequency, comma-separated
	<li>iconic - small, unadorned
	<li>imgfmt - "PNG" (default) or "JPEG"
	</ul>
	e.g.: ?w=400&h=200&fgcolor=x0088CC&title=mwt&values=4,9,9,7,11&xmaxs=243,274,305,336,367

	@author Jeremy Yang
*/
public class histoimg_servlet extends HttpServlet
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
  private static ArrayList<Float> xmaxes = null; //histogram-specific
  private static Float delta=null; //histogram-specific
  private static Float xmin=null; //histogram-specific
  private static Float xmax=null; //histogram-specific

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

    // Histogram-specific:

    String xmaxstr=request.getParameter("xmaxs");
    xmaxes = new ArrayList<Float>();
    if (xmaxstr!=null)
    {
      for (String vstr: Pattern.compile(",").split(xmaxstr))
      {
        float val;
        try { val=Float.parseFloat(vstr); }
        catch (NumberFormatException e) { val=0; }
        xmaxes.add(val);
      }
    }

    delta = 0.0f;
    if (values.size()==0)
      throw new ServletException("values.size()==0");
    else if (xmaxes.size()==0)
      throw new ServletException("xmaxes.size()==0");
    else if (values.size()!=xmaxes.size())
    {
      System.err.println("values.size()!=xmaxes.size(): "+values.size()+"!="+xmaxes.size());
      while (values.size()>xmaxes.size()) values.remove(values.size()-1); //Kludge
      while (values.size()<xmaxes.size()) xmaxes.remove(xmaxes.size()-1); //Kludge
    }
    if (values.size()>1)
    {
      delta=xmaxes.get(1)-xmaxes.get(0);
    }
    if (delta==0.0f)
    {
      throw new ServletException("delta==0.0f");
    }
    xmin=xmaxes.get(0)-delta;
    xmax=xmaxes.get(xmaxes.size()-1);
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

    // Done with http params.  Create histogram with JFreeChart.

    SimpleHistogramDataset histoData = new SimpleHistogramDataset("");

    double x=xmin;
    for (int i=0;i<values.size();++i)
    {
      SimpleHistogramBin bin = new SimpleHistogramBin(x,x+delta,false,true);
      bin.setItemCount(values.get(i));
      histoData.addBin(bin);
      x+=delta;
    }

    histoData.setAdjustForBinSize(false);

    JFreeChart chart = ChartFactory.createHistogram(
	title,
	xaxis,
	yaxis,
	histoData,
	(request.getParameter("horizontal")!=null)?PlotOrientation.HORIZONTAL:PlotOrientation.VERTICAL,
	(request.getParameter("legend")!=null),
	true, // Show tooltips
	true  // Show url
	);

    if (subtitle!=null)
      chart.addSubtitle(new TextTitle(subtitle,new Font(Font.SANS_SERIF,Font.ITALIC,10)));

    chart.setBackgroundPaint(bgcolor);
    chart.setBorderVisible(request.getParameter("border")!=null);

    XYPlot plot=chart.getXYPlot();
    XYItemRenderer renderer=plot.getRenderer();

    renderer.setSeriesPaint(0,fgcolor);

    ValueAxis axX=plot.getDomainAxisForDataset(0);
    ValueAxis axY=plot.getRangeAxisForDataset(0);
    // axX.setVerticalTickLabels(true);
    Integer labelangle=0; //degrees
    try { labelangle=Integer.parseInt(request.getParameter("labelangle")); }
    catch (NumberFormatException e) { labelangle=0; }
    axX.setLabelAngle(Math.PI / 180.0 * labelangle);

    TextTitle ttitle = chart.getTitle();
    // if (ttitle!=null) ttitle.setPosition(RectangleEdge.BOTTOM);

    if (iconic || width<200)
    {
      chart.setPadding(new RectangleInsets(UnitType.ABSOLUTE,0,0,0,0));
      chart.setBorderVisible(false);
      plot.setOutlineVisible(false);
      axX.setLowerMargin(0.0);
      axX.setUpperMargin(0.0);
      double xmax_ax=axX.getUpperBound();
      double xmin_ax=axX.getLowerBound();
      double ymax_ax=axY.getUpperBound();
      double ymin_ax=axY.getLowerBound();
      axX.setTickLabelsVisible(false);
      axY.setTickLabelsVisible(false);
      axX.setTickMarksVisible(false);
      axY.setTickMarksVisible(false);
      axX.setVisible(false);
      axY.setVisible(false);
      XYTextAnnotation txtann=new XYTextAnnotation(String.format("%.1f",xmin_ax),xmin_ax,ymax_ax);
      txtann.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
      txtann.setTextAnchor(TextAnchor.TOP_LEFT);
      plot.addAnnotation(txtann);
      txtann=new XYTextAnnotation(String.format("%.1f",xmax_ax),xmax_ax,ymax_ax);
      txtann.setFont(new Font(Font.MONOSPACED,Font.PLAIN,14));
      txtann.setTextAnchor(TextAnchor.TOP_RIGHT);
      plot.addAnnotation(txtann);
      if (ttitle!=null)
      {
        ttitle.setVisible(false); //does (?) work
        // chart.setTitle((TextTitle)null);
        txtann=new XYTextAnnotation(ttitle.getText(),xmax_ax-xmin_ax,ymin_ax);
        txtann.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,9));
        txtann.setPaint(Color.white);
        txtann.setTextAnchor(TextAnchor.BOTTOM_CENTER);
        plot.addAnnotation(txtann);
      }
    }
    else	// not-iconic (normal)
    {
      for (int i=0;i<values.size();++i)
      {
        Integer y=values.get(i);
        x=xmin+(i*delta);
        XYTextAnnotation txtann=new XYTextAnnotation(y.toString(),x+delta/2.0,y);
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
