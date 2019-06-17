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
import edu.unm.health.biocomp.util.threads.*;
import edu.unm.health.biocomp.smarts.*;
 
/**	Callable task for processing smarts-based fp similarity QxN matrix calculation.
	@author Jeremy J Yang
*/
public class Sim2D_Smarts_QxNTask
	implements Callable<float [][]>
{
  private ArrayList<Molecule> molsQ;
  private ArrayList<Molecule> molsDB;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private SmartsFile smaf;
  public TaskStatus taskstatus;
  private int q_total;
  private int n_total;
  private int q_done_fps;
  private int n_done_fps;
  private int q_done_rows;
  private Date t0;
  public Sim2D_Smarts_QxNTask(ArrayList<Molecule> molsQ,ArrayList<Molecule> molsDB,
	SmartsFile smaf,Integer arom,Float alpha,Float beta)
  {
    this.molsQ=molsQ;
    this.molsDB=molsDB;
    this.smaf=smaf;
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.taskstatus=new Status(this);
    this.q_total=molsQ.size();
    this.n_total=molsDB.size();
    this.q_done_fps=0;
    this.n_done_fps=0;
    this.q_done_rows=0;
    this.t0 = new Date();
  }
  public float [][] call()
  {
    float [][] simatrix = new float[molsQ.size()][molsDB.size()];
    BinaryFP [] fpsQ = new BinaryFP[molsQ.size()];
    BinaryFP [] fpsDB = new BinaryFP[molsDB.size()];
    for (int i=0;i<molsQ.size();++i)
    {
      fpsQ[i] = new BinaryFP(smaf.size());
      try {
        fpsQ[i].generate(smaf,molsQ.get(i));
      }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      catch (Exception e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++q_done_fps;
    }
    for (int i=0;i<molsDB.size();++i)
    {
      fpsDB[i] = new BinaryFP(smaf.size());
      try {
        fpsDB[i].generate(smaf,molsDB.get(i));
      }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      catch (Exception e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++n_done_fps;
    }
    for (int i=0;i<molsQ.size();++i)
    {
      for (int j=0;j<molsDB.size();++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            simatrix[i][j]=fpsQ[i].tversky(fpsDB[j],alpha,beta);
          }
          else
          {
            simatrix[i][j]=fpsQ[i].tanimoto(fpsDB[j]);
          }
        }
        catch (Exception e) {
          simatrix[i][j]=0.0f;
        }
      }
      ++q_done_rows;
    }
    return simatrix;
  }
  class Status implements TaskStatus
  {
    private Sim2D_Smarts_QxNTask task;
    public Status(Sim2D_Smarts_QxNTask task) { this.task=task; }
    public String status()
    {
      long t=(new Date()).getTime()-t0.getTime();
      int m=(int)(t/60000L);
      int s=(int)((t/1000L)%60L);
      String statstr=("["+String.format("%02d:%02d",m,s)+"]");
      if (this.task.q_done_fps<this.task.q_total)
        statstr+=(String.format(" %3d / %3d query fps",this.task.q_done_fps,this.task.q_total));
      else if (this.task.n_done_fps<this.task.n_total)
        statstr+=(String.format(" %3d / %3d DB fps",this.task.n_done_fps,this.task.n_total));
      else
        statstr+=(String.format(" %3d / %3d rows",this.task.q_done_rows,this.task.q_total));
      return statstr;
    }
  }
}
