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

import chemaxon.descriptors.*; //CDParameters, CFParameters, ChemicalFingerprint, MDGeneratorException, Metrics

import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.util.threads.*;
 
/**	Callable task for processing path fp similarity search.
	@author Jeremy J Yang
*/
public class Sim2D_Path_1xNTask
	implements Callable<Boolean>
{
  private Molecule molQ;
  private ArrayList<Molecule> mols;
  private MolImporter molReader;
  private Integer fplen;
  private Integer bondcount;
  private Integer bitsper;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private Float sim_min;
  private Integer n_max;
  private Integer n_max_hits;
  private Boolean sorthits;
  private int n_total;
  private int n_done;
  public ChemicalFingerprint fpQ;
  public CFParameters cfparams;
  public TaskStatus taskstatus;
  private Date t0;
  private Vector<Sim2DHit> hits;
  public Sim2D_Path_1xNTask(Molecule molQ,
	ArrayList<Molecule> mols,MolImporter molReader,
	Integer fplen,Integer bondcount,Integer bitsper,
	Integer arom,Float alpha,Float beta,Float sim_min,Integer n_max,
	Integer n_max_hits,
	Boolean sorthits)
  {
    this.molQ=molQ;
    this.mols=mols;
    this.molReader = molReader;
    if (fplen!=null) this.fplen=fplen;
    else this.fplen = chemaxon.descriptors.CFParameters.DEFAULT_LENGTH;
    if (bondcount!=null) this.bondcount=bondcount;
    else this.bondcount = chemaxon.descriptors.CFParameters.DEFAULT_BOND_COUNT;
    if (bitsper!=null) this.bitsper=bitsper;
    else this.bitsper = chemaxon.descriptors.CFParameters.DEFAULT_BITS_SET;
    cfparams = new CFParameters();
    cfparams.setLength(this.fplen);
    cfparams.setBondCount(this.bondcount);
    cfparams.setBitCount(this.bitsper);
    fpQ = new ChemicalFingerprint(cfparams);
    try { fpQ.generate(this.molQ); }
    catch (MDGeneratorException e) {
      System.err.println("bad FP: "+e.getMessage());
    }
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.sim_min=sim_min;
    this.n_max=n_max;
    this.n_max_hits=n_max_hits;
    this.sorthits=sorthits;
    this.taskstatus = new Status(this);
    this.n_total = (mols!=null) ? mols.size():0;
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
      if (n_max!=null && n_max>0 && i==n_max) break;
      Sim2DHit hit = new Sim2DHit();
      hit.i=i;
      Molecule mol;
      if (mols!=null)
      {
        if (i==mols.size()) break;
        mol = mols.get(i);
      }
      else if (molReader!=null)
      {
        try {
          mol = molReader.read();
        }
        catch (Exception e) {
          hit.sim=0.0f;
          System.err.println(e.getMessage());
          continue;
        }
        if (mol==null) break;
        if (arom!=null)
          mol.aromatize(arom);
        else
          mol.dearomatize();
        try { hit.smiles = MolExporter.exportToFormat(mol,"smiles:u"); }
        catch (Exception e) { hit.smiles=""; }
        hit.name = mol.getName();
      }
      else { return false; } //ERROR

      ChemicalFingerprint fp = new ChemicalFingerprint(this.cfparams);
      try {
        fp.generate(mol);
        if (alpha!=null && beta!=null)
          hit.sim = 1.0f-this.fpQ.getTversky(fp);	// No alpha or beta!
        else
          hit.sim = 1.0f-this.fpQ.getTanimoto(fp);
        hit.subset = fpQ.isSubSetOf(fp);
        hit.commonbitcount = fpQ.getCommonBitCount(fp);
        hit.brightness = fp.getBrightness();
      }
      catch (MDGeneratorException e) {
        System.err.println("bad FP: "+e.getMessage());
        hit.sim=0.0f;
      }
      if (hit.sim>=sim_min) { hits.add(hit); }
      if (molReader!=null) n_total = molReader.estimateNumRecords();
    }
    if (sorthits) Collections.sort(hits);
    while (hits.size()>n_max_hits) hits.remove(hits.size()-1);
    return true;
  }
  class Status implements TaskStatus
  {
    private Sim2D_Path_1xNTask task;
    public Status(Sim2D_Path_1xNTask task) { this.task=task; }
    public String status()
    {
      long t=(new Date()).getTime()-t0.getTime();
      int m=(int)(t/60000L);
      int s=(int)((t/1000L)%60L);
      String statstr=("["+String.format("%02d:%02d", m, s)+"]");
      statstr+=(String.format(" %6d", task.n_done));
      if (task.n_total>0)
        statstr+=(String.format(" (%.0f%%)", 100.0f*task.n_done/task.n_total));
      return statstr;
    }
  }
}
