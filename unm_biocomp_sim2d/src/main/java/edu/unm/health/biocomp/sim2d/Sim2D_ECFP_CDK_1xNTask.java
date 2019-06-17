/////////////////////////////////////////////////////////////////////////////
package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.Molecule;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.Metrics;

//NOTE: CDK classes specifed fully qualified (org.openscience.cdk)
import org.openscience.cdk.*; //AtomContainer
import org.openscience.cdk.fingerprint.*; //Fingerprinter,ExtendedFingerprinter
import org.openscience.cdk.exception.*; //CDKException
import org.openscience.cdk.similarity.*; //Tanimoto

import edu.unm.health.biocomp.cdk.*;
import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.util.threads.*;
 
/**	Callable task for processing ECFP similarity search.
	From this ChemAxon code, calls CDK functions:
	(1) to generate ECFPs,
	(2) to calculate similarity.

	@author Jeremy J Yang
*/
public class Sim2D_ECFP_CDK_1xNTask 
	implements Callable<Boolean>
{
  private Molecule molQ;
  private ArrayList<Molecule> mols;
  private MolImporter molReader;

  private String smiQ;
  private org.openscience.cdk.AtomContainer molQ_cdk;

  private int diam;
  private int size;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private Float sim_min;
  private Integer n_max;
  private Integer n_max_hits;
  private Boolean sorthits;

  private org.openscience.cdk.fingerprint.IFingerprinter fper;

  private BitSet fpQ;

  public TaskStatus taskstatus;
  private int n_total;
  private int n_done;
  private Date t0;
  private Vector<Sim2DHit> hits;
  public Sim2D_ECFP_CDK_1xNTask(Molecule molQ,
	ArrayList<Molecule> mols,MolImporter molReader,
	Integer diam,
	Integer size,
	Integer arom,Float alpha,Float beta,
	Float sim_min,Integer n_max,
	Integer n_max_hits,
	Boolean sorthits)
  {
    this.molQ=molQ;
    this.mols=mols;
    this.molReader=molReader;
    this.size=size;
    this.diam=diam;
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.sim_min=sim_min;
    this.n_max=n_max;
    this.n_max_hits=n_max_hits;
    this.sorthits=sorthits;

    this.fper = new org.openscience.cdk.fingerprint.ExtendedFingerprinter(this.size,this.diam);
    this.smiQ = null;
    try {
      this.smiQ = MolExporter.exportToFormat(this.molQ,"smiles:-a");
      this.fpQ = cdk_utils.CalcFpFromSmiles(this.smiQ,this.fper);
    }
    catch (Exception e) { }

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
      Molecule mol;
      if (mols!=null)
      {
        if (i==mols.size()) break;
        mol=mols.get(i);
      }
      else if (molReader!=null)
      {
        try { mol=molReader.read(); }
        catch (Exception e)
        {
          hit.sim=0.0f;
          continue;
        }
        if (arom!=null)
          mol.aromatize(arom);
        else
          mol.dearomatize();
        try { hit.smiles=MolExporter.exportToFormat(mol,"smiles:-a"); }
        catch (Exception e) { hit.smiles=""; }
        hit.name=mol.getName();
        if (mol==null) break;
      }
      else { return false; } //ERROR

      String smi = null;
      BitSet fp = null;
      try {
        smi = MolExporter.exportToFormat(mol,"smiles:-a");
        fp = cdk_utils.CalcFpFromSmiles(smi,this.fper);
        hit.brightness = fp.cardinality();
        BitSet fpX = (BitSet)fpQ.clone();
        fpX.and(fp);
        hit.commonbitcount = fpX.cardinality();
        hit.subset = fpQ.equals(fpX);
        if (alpha!=null && beta!=null)
          //hit.sim=Metrics.tversky((int[])fpQ.toLongArray(),(int[])fp.toLongArray(),alpha,beta);
          hit.sim=org.openscience.cdk.similarity.Tanimoto.calculate(fpQ,fp);
        else
        {
          //hit.sim=Metrics.tanimoto((int[])fpQ.toLongArray(),(int[])fp.toLongArray());
          hit.sim=org.openscience.cdk.similarity.Tanimoto.calculate(fpQ,fp);
        }
      }
      catch (Exception e) {
        System.err.println(e.toString());
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
    private Sim2D_ECFP_CDK_1xNTask task;
    public Status(Sim2D_ECFP_CDK_1xNTask task) { this.task=task; }
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
