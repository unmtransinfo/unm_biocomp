package edu.unm.health.biocomp.ro5;

import java.io.*;
import java.util.*;
import org.apache.commons.math3.stat.descriptive.*; //DescriptiveStatistics


/**     For Ro5 results storage.
*/
public class Ro5Results extends ArrayList<Ro5Result>
{
  private DescriptiveStatistics dstats_logp;
  private DescriptiveStatistics dstats_mwt;
  private DescriptiveStatistics dstats_hba;
  private DescriptiveStatistics dstats_hbd;
  private String program_logp;

  public Ro5Results()
  {
    this.dstats_logp = null;
    this.dstats_mwt = null;
    this.dstats_hba = null;
    this.dstats_hbd = null;
  }

  public int getViolationsLogp()
  {
    int n=0;
    for (Ro5Result result: this)
      n+=(result.isViolationLogp()?1:0);
    return n;
  }
  public int getViolationsMwt()
  {
    int n=0;
    for (Ro5Result result: this)
      n+=(result.isViolationMwt()?1:0);
    return n;
  }
  public int getViolationsHba()
  {
    int n=0;
    for (Ro5Result result: this)
      n+=(result.isViolationHba()?1:0);
    return n;
  }
  public int getViolationsHbd()
  {
    int n=0;
    for (Ro5Result result: this)
      n+=(result.isViolationHbd()?1:0);
    return n;
  }

  private void loadDescriptiveStatistics()
  {
    this.dstats_logp = new DescriptiveStatistics(this.size());
    this.dstats_mwt = new DescriptiveStatistics(this.size());
    this.dstats_hba = new DescriptiveStatistics(this.size());
    this.dstats_hbd = new DescriptiveStatistics(this.size());
    for (Ro5Result result: this)
    {
      if (result.getLogp()!=null) this.dstats_logp.addValue(result.getLogp());
      if (result.getMwt()!=null) this.dstats_mwt.addValue(result.getMwt());
      if (result.getHba()!=null) this.dstats_hba.addValue(((Integer)result.getHba()).doubleValue());
      if (result.getHbd()!=null) this.dstats_hbd.addValue(((Integer)result.getHbd()).doubleValue());
    }
  }

  public double getMinLogp()
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getMin();
  }
  public double getMinMwt()
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getMin();
  }
  public long getMinHba()
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return Math.round(this.dstats_hba.getMin());
  }
  public long getMinHbd()
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return Math.round(this.dstats_hbd.getMin());
  }

  public double getMaxLogp()
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getMax();
  }
  public double getMaxMwt()
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getMax();
  }
  public long getMaxHba()
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return Math.round(this.dstats_hba.getMax());
  }
  public long getMaxHbd()
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return Math.round(this.dstats_hbd.getMax());
  }

  public double getMeanLogp()
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getMean();
  }
  public double getMeanMwt()
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getMean();
  }
  public double getMeanHba()
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return this.dstats_hba.getMean();
  }
  public double getMeanHbd()
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return this.dstats_hbd.getMean();
  }

  public double getStdLogp()
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getStandardDeviation();
  }
  public double getStdMwt()
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getStandardDeviation();
  }
  public double getStdHba()
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return this.dstats_hba.getStandardDeviation();
  }
  public double getStdHbd()
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return this.dstats_hbd.getStandardDeviation();
  }

  public double getVarLogp()
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getVariance();
  }
  public double getVarMwt()
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getVariance();
  }
  public double getVarHba()
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return this.dstats_hba.getVariance();
  }
  public double getVarHbd()
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return this.dstats_hbd.getVariance();
  }

  public double getPercentileLogp(double p)
  {
    if (this.dstats_logp==null) this.loadDescriptiveStatistics();
    return this.dstats_logp.getPercentile(p);
  }
  public double getPercentileMwt(double p)
  {
    if (this.dstats_mwt==null) this.loadDescriptiveStatistics();
    return this.dstats_mwt.getPercentile(p);
  }
  public double getPercentileHba(double p)
  {
    if (this.dstats_hba==null) this.loadDescriptiveStatistics();
    return this.dstats_hba.getPercentile(p);
  }
  public double getPercentileHbd(double p)
  {
    if (this.dstats_hbd==null) this.loadDescriptiveStatistics();
    return this.dstats_hbd.getPercentile(p);
  }

  public String getLogpProgram() { return this.program_logp; }
  public void setLogpProgram(String _program_logp) { this.program_logp = _program_logp; }
}
