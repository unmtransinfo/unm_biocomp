/////////////////////////////////////////////////////////////////////////////
package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.marvin.io.MolExportException;

import chemaxon.descriptors.CDParameters;
import chemaxon.descriptors.CFParameters;
import chemaxon.descriptors.ChemicalFingerprint;
import chemaxon.descriptors.MDGeneratorException;
//import chemaxon.descriptors.ECFP;
//import chemaxon.descriptors.ECFPGenerator;
//import chemaxon.descriptors.ECFPParameters;

import chemaxon.descriptors.Metrics;

import edu.unm.health.biocomp.molfp.ConnectivityFingerprint;
import edu.unm.health.biocomp.molfp.ECMolGraphInvariants;
import edu.unm.health.biocomp.grouping.SimilarityMeasures;

import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.fp.*;
 
/**	Callable task for processing bitstring similarity search.
	@author Jeremy J Yang
*/
public class Sim2D_Bitstring_1xNTask 
	implements Callable<Boolean>
{
  private String bitstrQ;
  private ArrayList<String> bitstrs;
  private BufferedReader buffReader;
  private Float alpha;
  private Float beta;
  private Float sim_min;
  private Integer n_max;
  private Integer n_max_hits;
  private Boolean sorthits;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done;
  private Date t0;
  private Vector<Sim2DHit> hits;
  // bitstrs OR buffReader non-null; buffReader saves memory:
  public Sim2D_Bitstring_1xNTask(String bitstrQ,ArrayList<String> bitstrs,
	BufferedReader buffReader,
	Float alpha,Float beta,Float sim_min,Integer n_max,
	Integer n_max_hits,
	Boolean sorthits)
  {
    this.bitstrQ=bitstrQ;
    this.bitstrs=bitstrs;
    this.buffReader=buffReader;
    this.alpha=alpha;
    this.beta=beta;
    this.sim_min=sim_min;
    this.n_max=n_max;
    this.n_max_hits=n_max_hits;
    this.sorthits=sorthits;
    this.taskstatus=new Status(this);
    if (bitstrs!=null) this.n_total=bitstrs.size();
    else this.n_total=0;
    this.n_done=0;
    this.t0 = new Date();
    hits = new Vector<Sim2DHit>();
  }
  /////////////////////////////////////////////////////////////////////////
  public synchronized Vector<Sim2DHit> getHits() { return hits; }
  public synchronized Boolean call()
  {
    BinaryFP fpQ = new BinaryFP();
    try { fpQ.fromBitString(bitstrQ); }
    catch (Exception e) { System.err.println("bad FP: "+e.getMessage()); }
    for (int i=0;true;++i,++n_done)
    {
      if (n_max>0 && i==n_max) break;
      Sim2DHit hit = new Sim2DHit();
      hit.i=i;
      String bitstr="";
      if (bitstrs!=null)
      {
        if (i==bitstrs.size()) break;
        bitstr=bitstrs.get(i);
        hit.name=String.format("%d",i);
      }
      else if (buffReader!=null)
      {
        String line=null;
        try { line=buffReader.readLine(); }
        catch (IOException e)
        {
          hit.sim=0.0f;
          continue;
        }
        if (line==null) break;
        bitstr=line.replaceFirst("\\s.*$","");
        if (line.matches("^\\S+\\s.*$"))
          hit.name=line.replaceFirst("^\\S+\\s","");
        else
          hit.name=String.format("%d",i);
      }
      BinaryFP fp = new BinaryFP();
      try { fp.fromBitString(bitstr); }
      catch (Exception e) { System.err.println("bad FP: "+e.getMessage()); }
      if (fpQ.size()!=fp.size())
      {
        System.err.println("ERROR: FPsize != query FPsize ("+fp.size()+"!="+fpQ.size()+").");
        hit.sim=0.0f;
        continue;
      }
      try
      {
        hit.subset=fpQ.isSubsetOf(fp);
        hit.commonbitcount=fpQ.getCommonBitCount(fp);
        hit.brightness=fp.bitCount();
        if (alpha!=null && beta!=null)
          hit.sim=fpQ.tversky(fp,alpha,beta);
        else
          hit.sim=fpQ.tanimoto(fp);
      }
      catch (Exception e)
      {
        hit.sim=0.0f;
        continue;
      }
      if (hit.sim>=sim_min) { hits.add(hit); }
    }
    if (sorthits) Collections.sort(hits);
    while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    return true;
  }
  class Status implements TaskStatus
  {
    private Sim2D_Bitstring_1xNTask task;
    public Status(Sim2D_Bitstring_1xNTask task) { this.task=task; }
    public String status()
    {
      long t=(new Date()).getTime()-t0.getTime();
      int m=(int)(t/60000L);
      int s=(int)((t/1000L)%60L);
      String statstr=("["+String.format("%02d:%02d",m,s)+"]");
      statstr+=(String.format(" %5d;",task.n_done));
      if (task.n_total>0)
        statstr+=(String.format(" %.0f%%",100.0f*task.n_done/task.n_total));
      return statstr;
    }
  }
}
