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

import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.threads.*;
 
/**	Callable task for processing path fp similarity NxN matrix calculation.
	@author Jeremy J Yang
*/
public class Sim2D_Path_NxNTask
	implements Callable<float [][]>
{
  private ArrayList<Molecule> mols;
  private Integer fplen;
  private Integer bondcount;
  private Integer bitsper;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private int n_total;
  private int n_done_fps;
  private int n_done_rows;
  public CFParameters cfparams;
  public TaskStatus taskstatus;
  private Date t0;
  public Sim2D_Path_NxNTask(ArrayList<Molecule> mols,
	Integer fplen,Integer bondcount,Integer bitsper,
	Integer arom,Float alpha,Float beta)
  {
    this.mols=mols;
    if (fplen!=null) this.fplen=fplen;
    else this.fplen=chemaxon.descriptors.CFParameters.DEFAULT_LENGTH;
    if (bondcount!=null) this.bondcount=bondcount;
    else this.bondcount=chemaxon.descriptors.CFParameters.DEFAULT_BOND_COUNT;
    if (bitsper!=null) this.bitsper=bitsper;
    else this.bitsper=chemaxon.descriptors.CFParameters.DEFAULT_BITS_SET;
    cfparams=new CFParameters();
    cfparams.setLength(this.fplen);
    cfparams.setBondCount(this.bondcount);
    cfparams.setBitCount(this.bitsper);
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.taskstatus=new Status(this);
    this.n_total=mols.size();
    this.n_done_fps=0;
    this.n_done_rows=0;
    this.t0 = new Date();
  }
  public float [][] call()
  {
    float [][] simatrix = new float[mols.size()][mols.size()];
    ChemicalFingerprint [] fps = new ChemicalFingerprint[mols.size()];
    for (int i=0;i<mols.size();++i)
    {
      fps[i] = new ChemicalFingerprint(this.cfparams);
      try {
        fps[i].generate(mols.get(i));
      }
      catch (MDGeneratorException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++this.n_done_fps;
    }
    for (int i=0;i<mols.size();++i)
    {
      for (int j=0;j<mols.size();++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else
              simatrix[i][j]=1.0f-fps[i].getTversky(fps[j]); // No alpha or beta!
          }
          else
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else if (i>j)
              simatrix[i][j]=simatrix[j][i];
            else
              simatrix[i][j]=1.0f-fps[i].getTanimoto(fps[j]);
          }
        }
        catch (Exception e) {
          simatrix[i][j]=0.0f;
        }
      }
      ++this.n_done_rows;
    }
    return simatrix;
  }
  class Status implements TaskStatus
  {
    private Sim2D_Path_NxNTask task;
    public Status(Sim2D_Path_NxNTask task) { this.task=task; }
    public String status()
    {
      long t=(new Date()).getTime()-t0.getTime();
      int m=(int)(t/60000L);
      int s=(int)((t/1000L)%60L);
      String statstr=("["+String.format("%02d:%02d",m,s)+"]");
      if (this.task.n_done_fps<this.task.n_total)
        statstr+=(String.format(" %3d / %3d fps",this.task.n_done_fps,this.task.n_total));
      else
        statstr+=(String.format(" %3d / %3d rows",this.task.n_done_rows,this.task.n_total));
      return statstr;
    }
  }
}
