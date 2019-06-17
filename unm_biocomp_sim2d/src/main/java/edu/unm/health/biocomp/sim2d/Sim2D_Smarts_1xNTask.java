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

import chemaxon.descriptors.Metrics;

import edu.unm.health.biocomp.molfp.ConnectivityFingerprint;
import edu.unm.health.biocomp.molfp.ECMolGraphInvariants;
import edu.unm.health.biocomp.grouping.SimilarityMeasures;

import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.smarts.*;
 
/**	Callable task for processing smarts-based fp similarity search.
	@author Jeremy J Yang
*/
public class Sim2D_Smarts_1xNTask
	implements Callable<Boolean>
{
  private Molecule molQ;
  private ArrayList<Molecule> mols;
  private MolImporter molReader;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private Float sim_min;
  private Integer n_max;
  private Integer n_max_hits;
  private Boolean sorthits;
  private SmartsFile smaf;
  BinaryFP fpQ;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done;
  private Date t0;
  private Vector<Sim2DHit> hits;
  // EITHER mols or molReader should be non-null; using molReader saves memory:
  public Sim2D_Smarts_1xNTask(Molecule molQ,
	ArrayList<Molecule> mols,MolImporter molReader,
	SmartsFile smaf,
	Integer arom,Float alpha,Float beta,Float sim_min,Integer n_max,
	Integer n_max_hits,Boolean sorthits)
  {
    this.molQ=molQ;
    this.mols=mols;
    this.molReader=molReader;
    this.smaf=smaf;
    fpQ = new BinaryFP(this.smaf.size());
    try { fpQ.generate(smaf,this.molQ); }
    catch (SearchException e) {
      System.err.println("bad FP: "+e.getMessage());
    }
    catch (Exception e) {
      System.err.println("bad FP: "+e.getMessage());
    }
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
      Molecule mol;
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

      BinaryFP fp = new BinaryFP(smaf.size());
      try {
        fp.generate(smaf,mol);
        if (alpha!=null && beta!=null)
          hit.sim=this.fpQ.tversky(fp,alpha,beta);
        else
          hit.sim=this.fpQ.tanimoto(fp);

        hit.subset=fpQ.isSubsetOf(fp);
        hit.commonbitcount=fpQ.getCommonBitCount(fp);
        hit.brightness=fp.bitCount();
      }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
        hit.sim=0.0f;
      }
      catch (Exception e) {
        System.err.println("bad FP: "+e.getMessage());
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
    private Sim2D_Smarts_1xNTask task;
    public Status(Sim2D_Smarts_1xNTask task) { this.task=task; }
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
