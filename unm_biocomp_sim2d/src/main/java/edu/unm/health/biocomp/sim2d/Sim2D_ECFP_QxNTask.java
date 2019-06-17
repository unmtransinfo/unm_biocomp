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
 
/**	Callable task for processing ECFP similarity QxN matrix calculation.
	@author Jeremy J Yang
*/
public class Sim2D_ECFP_QxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> molsQ;
  private ArrayList<Molecule> molsDB;
  private int diam;
  private int len;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private ECFPGenerator fper;
  private ECFPParameters fpparams;
  public TaskStatus taskstatus;
  private int q_total;
  private int q_done_fps;
  private int n_total;
  private int n_done_fps;
  private int q_done_rows;
  private Date t0;
  public Sim2D_ECFP_QxNTask(ArrayList<Molecule> molsQ,ArrayList<Molecule> molsDB,
	int diam,int len,
	Integer arom,Float alpha,Float beta)
  {
    this.molsQ=molsQ;
    this.molsDB=molsDB;
    this.fper = new ECFPGenerator();
    this.arom=arom;
    this.len=len;
    this.diam=diam;
    this.fpparams = new ECFPParameters();
    this.fpparams.setDiameter(this.diam);
    this.fpparams.setLength(this.len);
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
    ECFP [] fpsQ = new ECFP[molsQ.size()];
    ECFP [] fpsDB = new ECFP[molsDB.size()];
    for (int i=0;i<molsQ.size();++i)
    {
      try {
        fpsQ[i] = new ECFP(this.fpparams);
        this.fper.generate(molsQ.get(i),fpsQ[i]);
      }
      catch (MDGeneratorException e) {
        System.err.println("bad FP: "+e.getMessage());
      }
      ++this.q_done_fps;
    }
    for (int i=0;i<molsDB.size();++i)
    {
      try {
        fpsDB[i] = new ECFP(this.fpparams);
        this.fper.generate(molsDB.get(i),fpsDB[i]);
      }
      catch (MDGeneratorException e) {
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
            simatrix[i][j]=1.0f-fpsQ[i].getWeightedAsymmetricEuclidean(fpsDB[j]); //Tversky not available!
          else
            simatrix[i][j]=1.0f-fpsQ[i].getTanimoto(fpsDB[j]);
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
    private Sim2D_ECFP_QxNTask task;
    public Status(Sim2D_ECFP_QxNTask task) { this.task=task; }
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
