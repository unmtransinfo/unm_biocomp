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
import edu.unm.health.biocomp.threads.*;
 
/**	Callable task for processing ECFP similarity NxN matrix calculation.
	@author Jeremy J Yang
*/
public class Sim2D_ECFP_NxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> mols;
  private int diam;
  private int len;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private ECFPGenerator fper;
  private ECFPParameters fpparams;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done_fps;
  private int n_done_rows;
  private Date t0;
  public Sim2D_ECFP_NxNTask(ArrayList<Molecule> mols,
	int diam,int len,
	Integer arom,Float alpha,Float beta)
  {
    this.mols=mols;
    this.fper = new ECFPGenerator();
    this.diam=diam;
    this.len=len;
    this.fpparams = new ECFPParameters();
    this.fpparams.setDiameter(this.diam);
    this.fpparams.setLength(this.len);
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;
    this.taskstatus=new Status(this);
    this.n_total=mols.size();
    this.n_done_fps=0;
    this.n_done_rows=0;
    this.t0 = new Date();
  }
  /////////////////////////////////////////////////////////////////////////
  public float [][] call()
  {
    float [][] simatrix = new float[mols.size()][mols.size()];
    ECFP [] fps = new ECFP[mols.size()];
    for (int i=0;i<mols.size();++i)
    {
      try {
        fps[i] = new ECFP(this.fpparams);
        this.fper.generate(mols.get(i),fps[i]);
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
              simatrix[i][j]=1.0f-fps[i].getWeightedAsymmetricEuclidean(fps[j]); //Tversky not available!
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
    private Sim2D_ECFP_NxNTask task;
    public Status(Sim2D_ECFP_NxNTask task) { this.task=task; }
    public String status()
    {
      if (this.task.n_done_fps<this.task.n_total)
        return (String.format(" %3d / %3d fps",this.task.n_done_fps,this.task.n_total));
      else
        return (String.format(" %3d / %3d rows",this.task.n_done_rows,this.task.n_total));
    }
  }
}
