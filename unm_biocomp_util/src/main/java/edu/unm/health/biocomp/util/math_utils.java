package edu.unm.health.biocomp.util;

import java.util.*;
import org.apache.commons.math3.stat.descriptive.*; //DescriptiveStatistics
import org.apache.commons.math3.random.*; //GaussianRandomGenerator

/**     Static math and stats methods.

        @author Jeremy J Yang
*/
public class math_utils
{
  private math_utils() {} //disable default constructor

  /////////////////////////////////////////////////////////////////////////////
  /**   Analyze input sample continuous values for histogram.

	@param xvals sample values
	@param nbins number of histogram bins
	@param xmaxs return parameter: max for each bin
	@param freqs return parameter: frequencies for each bin
  */
  public static void HistoAnalyze(ArrayList<Double> xvals,Integer nbins,ArrayList<Double> xmaxs,
	ArrayList<Integer> freqs)
  {
    DescriptiveStatistics dstats = new DescriptiveStatistics(xvals.size());
    for (double x: xvals) dstats.addValue(x);

    double xmin = dstats.getMin();
    double xmax = dstats.getMax();
    double mean = dstats.getMean();
    double std = dstats.getStandardDeviation();

    double range_min = Math.max(xmin,mean-3*std);
    double range_max = Math.min(xmax,mean+3*std);
    double delta = (range_max - range_min) / nbins ;

    while (xmaxs.size()<nbins) xmaxs.add(null);
    while (freqs.size()<nbins) freqs.add(0);

    for (int i=0;i<nbins;++i)
    {
      xmaxs.set(i,range_min+delta*i);
    }

    for (double x: xvals)
    {
      if (x<xmaxs.get(nbins-1))
      {
        for (int i=0;i<nbins;++i)
        {
          if (x<xmaxs.get(i))
          {
            freqs.set(i,freqs.get(i)+1);
            break;
          }
        }
      }
      else
      {
        freqs.set(nbins-1,freqs.get(nbins-1)+1);
      }
    }
    return;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**   Analyze input sample integer values for barchart.
	For limited range (20?) barchart more sensible than histogram;
	i.e. regard xvals as categories.

	@param xvals sample values
	@param range min and max
	@param freqs return parameter: frequencies for each bin
  */
  public static void BarchartAnalyze(ArrayList<Integer> xvals,Integer[] range,ArrayList<Integer> freqs)
  {
    range[0] = Integer.MAX_VALUE;
    range[1] = Integer.MIN_VALUE;
    for (int x: xvals) { if (x>range[1]) range[1]=x; }
    for (int x: xvals) { if (x<range[0]) range[0]=x; }

    int nbins = range[1] - range[0] + 1;

    while (freqs.size()<nbins) freqs.add(0);
    
    for (int x: xvals)
    {
      freqs.set(x-range[0],freqs.get(x-range[0])+1);
    }
    return;
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
  {
    RandomGenerator rg = new JDKRandomGenerator();
    //rg.setSeed(17399225432l);  // Fixed seed means same results every time

    System.err.println(" RANDOM HISTOGRAM TEST (Gaussian) ...");
    ArrayList<Double> rainfall = new ArrayList<Double>();
    // Create a GassianRandomGenerator using rg as its source of randomness
    GaussianRandomGenerator grand = new GaussianRandomGenerator(rg);
    for (int i=0;i<1000;++i) rainfall.add(grand.nextNormalizedDouble()*10 + 50);
    int nbins = 10;
    ArrayList<Double> xmaxs = new ArrayList<Double>(nbins);
    ArrayList<Integer> freqs = new ArrayList<Integer>(nbins);
    HistoAnalyze(rainfall,nbins,xmaxs,freqs);
    for (int i=0;i<nbins;++i)
    {
      System.out.println(
	String.format("\t%s - %s: %4d",
		(i==0)?"     ":String.format("%5.2f",xmaxs.get(i-1)),
		(i==(nbins-1))?"     ":String.format("%5.2f",xmaxs.get(i)),
		freqs.get(i)));
    }
    DescriptiveStatistics dstats = new DescriptiveStatistics(rainfall.size());
    for (double x: rainfall) dstats.addValue(x);
    System.err.println(" xmin = "+String.format("%9.3f",dstats.getMin()));
    System.err.println(" xmax = "+String.format("%9.3f",dstats.getMax()));
    System.err.println(" mean = "+String.format("%9.3f",dstats.getMean()));
    System.err.println("  med = "+String.format("%9.3f",dstats.getPercentile(50.0)));
    System.err.println("  var = "+String.format("%9.3f",dstats.getVariance()));
    System.err.println("  std = "+String.format("%9.3f",dstats.getStandardDeviation()));


    System.err.println("\n RANDOM BARCHART TEST (Dice Throws) ...");
    ArrayList<Integer> dicethrows = new ArrayList<Integer>();
    // Create a UniformRandomGenerator using rg as its source of randomness
    UniformRandomGenerator urand = new UniformRandomGenerator(rg);
    // Should uniformly sample [2,12]:
    for (int i=0;i<1000;++i) dicethrows.add(
	((Long)Math.round((urand.nextNormalizedDouble()/Math.sqrt(3.0)+1)/2*6 + 0.5)).intValue()
	+((Long)Math.round((urand.nextNormalizedDouble()/Math.sqrt(3.0)+1)/2*6 + 0.5)).intValue()
	);
    Integer[] range = new Integer[2]; //min,max
    freqs.clear();
    BarchartAnalyze(dicethrows,range,freqs);
    System.err.println(" dmin = "+String.format("%3d",range[0]));
    System.err.println(" dmax = "+String.format("%3d",range[1]));
    System.err.println(" nbins = "+String.format("%3d",freqs.size()));
    for (int i=0;i<freqs.size();++i)
      System.out.println(String.format("\t%d: %4d",range[0]+i,freqs.get(i)));
    dstats = new DescriptiveStatistics(dicethrows.size());
    for (Integer x: dicethrows) dstats.addValue(x.doubleValue());
    System.err.println(" xmin = "+String.format("%9.3f",dstats.getMin()));
    System.err.println(" xmax = "+String.format("%9.3f",dstats.getMax()));
    System.err.println(" mean = "+String.format("%9.3f",dstats.getMean()));
    System.err.println("  med = "+String.format("%9.3f",dstats.getPercentile(50.0)));
    System.err.println("  var = "+String.format("%9.3f",dstats.getVariance()));
    System.err.println("  std = "+String.format("%9.3f",dstats.getStandardDeviation()));
  }
}
