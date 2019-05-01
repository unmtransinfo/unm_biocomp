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

import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.threads.*;
 
/**	Callable task for processing ECFP similarity QxN matrix calculation.
	Calls ECFP code by Oleg Ursu.
	@author Jeremy J Yang
*/
public class Sim2D_ECFP_OU_QxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> molsQ;
  private ArrayList<Molecule> molsDB;
  private int diam;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private ConnectivityFingerprint fper;
  public TaskStatus taskstatus;
  private int q_total;
  private int q_done_fps;
  private int n_total;
  private int n_done_fps;
  private int q_done_rows;
  private Date t0;
  public Sim2D_ECFP_OU_QxNTask(ArrayList<Molecule> molsQ,ArrayList<Molecule> molsDB,
	int diam,Integer arom,Float alpha,Float beta)
  {
    this.molsQ=molsQ;
    this.molsDB=molsDB;
    this.fper = new ConnectivityFingerprint();
    this.fper.setAtomInvariantsGenerator(new ECMolGraphInvariants());
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.taskstatus=new Status(this);
    this.q_total=molsQ.size();
    this.q_done_fps=0;
    this.n_total=molsDB.size();
    this.n_done_fps=0;
    this.q_done_rows=0;
    this.t0 = new Date();
  }
  /////////////////////////////////////////////////////////////////////////
  public float [][] call()
  {
    float [][] simatrix = new float[molsQ.size()][molsDB.size()];
    int [][] fpsQ = new int[molsQ.size()][];
    int [][] fpsDB = new int[molsDB.size()][];
    for (int i=0;i<molsQ.size();++i)
    {
      this.fper.setMolecule(molsQ.get(i).cloneMolecule());
      try {
        this.fper.calculate(this.diam);
        fpsQ[i]=fper.getFingerprint();
        if (fpsQ[i]!=null)
        {
          Arrays.sort(fpsQ[i]);
        }
      }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++this.q_done_fps;
    }
    for (int i=0;i<molsDB.size();++i)
    {
      this.fper.setMolecule(molsDB.get(i).cloneMolecule());
      try {
        this.fper.calculate(this.diam);
        fpsDB[i]=fper.getFingerprint();
        if (fpsDB[i]!=null)
        {
          Arrays.sort(fpsDB[i]);
        }
      }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++this.n_done_fps;
    }
    for (int i=0;i<molsQ.size();++i)
    {
      for (int j=0;j<molsDB.size();++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            simatrix[i][j]=SimilarityMeasures.tversky(fpsQ[i],fpsDB[j],alpha,beta);
          }
          else
          {
            simatrix[i][j]=SimilarityMeasures.tanimoto(fpsQ[i],fpsDB[j]);
          }
        }
        catch (Exception e) {
          simatrix[i][j]=0.0f;
        }
      }
      ++this.q_done_rows;
    }
    return simatrix;
  }
  class Status implements TaskStatus
  {
    private Sim2D_ECFP_OU_QxNTask task;
    public Status(Sim2D_ECFP_OU_QxNTask task) { this.task=task; }
    public String status()
    {
      if (this.task.q_done_fps<this.task.q_total)
        return (String.format(" %3d / %3d query fps",this.task.q_done_fps,this.task.q_total));
      else if (this.task.n_done_fps<this.task.n_total)
        return (String.format(" %3d / %3d DB fps",this.task.n_done_fps,this.task.n_total));
      else
        return (String.format(" %3d / %3d rows",this.task.q_done_rows,this.task.q_total));
    }
  }
}
