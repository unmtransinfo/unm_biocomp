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

/**	Callable task for processing bitstring similarity NxN matrix computation.
	@author Jeremy J Yang
*/
public class Sim2D_Bitstring_NxNTask 
	implements Callable<float [][]>
{
  private ArrayList<String> bitstrs;
  private Float alpha;
  private Float beta;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done_fps;
  private int n_done_rows;
  private Date t0;
  public Sim2D_Bitstring_NxNTask(ArrayList<String> bitstrs,
	Float alpha,Float beta)
  {
    this.bitstrs=bitstrs;
    this.alpha=alpha;
    this.beta=beta;
    this.taskstatus=new Status(this);
    this.n_total=bitstrs.size();
    this.n_done_fps=0;
    this.n_done_rows=0;
    this.t0 = new Date();
  }
  /////////////////////////////////////////////////////////////////////////
  public float [][] call()
  {
    float [][] simatrix = new float[bitstrs.size()][bitstrs.size()];
    BinaryFP [] fps = new BinaryFP[bitstrs.size()];
    for (int i=0;i<bitstrs.size();++i)
    {
      fps[i] = new BinaryFP();
      try { fps[i].fromBitString(bitstrs.get(i)); }
      catch (Exception e) { System.err.println("bad FP: "+e.getMessage()); }
      ++this.n_done_fps;
    }
    for (int i=0;i<bitstrs.size();++i)
    {
      for (int j=0;j<bitstrs.size();++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else
              simatrix[i][j]=fps[i].tversky(fps[j],alpha,beta);
          }
          else
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else if (i>j)
              simatrix[i][j]=simatrix[j][i];
            else
              simatrix[i][j]=fps[i].tanimoto(fps[j]);
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
    private Sim2D_Bitstring_NxNTask task;
    public Status(Sim2D_Bitstring_NxNTask task) { this.task=task; }
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
