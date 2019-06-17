/////////////////////////////////////////////////////////////////////////////
package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.MDGeneratorException;
import chemaxon.descriptors.ECFP;
import chemaxon.descriptors.ECFPGenerator;
import chemaxon.descriptors.ECFPParameters;

import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.util.threads.*;
 
/**	Callable task for processing ECFP similarity search.
	@author Jeremy J Yang
*/
public class Sim2D_ECFP_1xNTask 
	implements Callable<Boolean>
{
  private Molecule molQ;
  private ArrayList<Molecule> mols;
  private MolImporter molReader;
  private int diam;
  private int len;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private Float sim_min;
  private Integer n_max;
  private Integer n_max_hits;
  private Boolean sorthits;
  private ECFPGenerator fper;
  private ECFP fpQ;
  private ECFPParameters fpparams;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done;
  private Date t0;
  private Vector<Sim2DHit> hits;
  public Sim2D_ECFP_1xNTask(Molecule molQ,
	ArrayList<Molecule> mols,MolImporter molReader,
	int diam,int len,
	Integer arom,Float alpha,Float beta,
	Float sim_min,Integer n_max,
	Integer n_max_hits,
	Boolean sorthits)
  {
    this.molQ=molQ;
    this.mols=mols;
    this.molReader=molReader;
    this.fper = new ECFPGenerator();
    this.diam=diam;
    this.len=len;
    this.fpparams = new ECFPParameters();
    this.fpparams.setDiameter(this.diam);
    this.fpparams.setLength(this.len);
    this.fpQ = new ECFP(this.fpparams);
    try { this.fper.generate(this.molQ,this.fpQ); }
    catch (MDGeneratorException e) { System.err.println("ERROR: "+e.getMessage()); }
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.sim_min=sim_min;
    this.n_max=n_max;
    this.n_max_hits=n_max_hits;
    this.sorthits=sorthits;
    this.taskstatus=new Status(this);
    if (mols!=null) this.n_total=mols.size();
    else this.n_total=0;
    this.n_done=0;
    this.t0 = new Date();
    hits = new Vector<Sim2DHit>();
  }
  /////////////////////////////////////////////////////////////////////////
  public synchronized Vector<Sim2DHit> getHits() { return hits; }
  public synchronized Boolean call()
  {
    for (int i=0;true;++i,++n_done)
    {
      if (n_max>0 && i==n_max) break;
      Sim2DHit hit = new Sim2DHit();
      hit.i=i;
      Molecule mol=null;
      if (mols!=null)
      {
        if (i==mols.size()) break;
        mol=mols.get(i);
      }
      else if (molReader!=null)
      {
        try { mol=molReader.read(); }
        catch (MolFormatException e)
        {
          hit.sim=0.0f;
          continue;
        }
        catch (IOException e)
        {
          hit.sim=0.0f;
          continue;
        }
        if (arom!=null)
          mol.aromatize(arom);
        else
          mol.dearomatize();
        try { hit.smiles=MolExporter.exportToFormat(mol,"smiles:u"); }
        catch (Exception e) { hit.smiles=""; }
        hit.name=mol.getName();
        if (mol==null) break;
      }
      else { return false; } //ERROR

      ECFP fp=new ECFP(this.fpparams);
      try {
        this.fper.generate(mol,fp);
        if (this.fpQ!=null && fp!=null)
        {
          if (alpha!=null && beta!=null)
            hit.sim=(1.0f-this.fpQ.getWeightedAsymmetricEuclidean(fp));//Tversky not available!
            // YES IT IS!! See chemaxon.descriptors.MolecularDescriptor.getDissimilarity(MolecularDescriptor other, int parametrizedMetricIndex)
          else
            hit.sim=1.0f-this.fpQ.getTanimoto(fp);
        }
        else
          hit.sim=0.0f;
        hit.brightness=fp.getBrightness();
        hit.commonbitcount=0;	//undefined
        hit.subset=false;	//undefined
      }
      catch (MDGeneratorException e) {
        System.err.println("ERROR: "+e.getMessage());
        hit.sim=0.0f;
      }
      if (hit.sim>=sim_min) { hits.add(hit); }
      if (molReader!=null) n_total=molReader.estimateNumRecords();
    }
    if (sorthits) Collections.sort(hits);
    while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    return true;
  }
  class Status implements TaskStatus
  {
    private Sim2D_ECFP_1xNTask task;
    public Status(Sim2D_ECFP_1xNTask task) { this.task=task; }
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
