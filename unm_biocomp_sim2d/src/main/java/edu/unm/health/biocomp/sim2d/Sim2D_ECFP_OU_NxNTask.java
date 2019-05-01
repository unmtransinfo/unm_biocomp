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
 
/**	Callable task for processing ECFP similarity NxN matrix calculation.
	Calls ECFP code by Oleg Ursu.
	@author Jeremy J Yang
*/
public class Sim2D_ECFP_OU_NxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> mols;
  private int diam;
  private Integer arom;
  private Float alpha;
  private Float beta;
  private ConnectivityFingerprint fper;
  public TaskStatus taskstatus;
  private int n_total;
  private int n_done_fps;
  private int n_done_rows;
  private Date t0;
  public Sim2D_ECFP_OU_NxNTask(ArrayList<Molecule> mols,
	int diam,
	Integer arom,Float alpha,Float beta)
  {
    this.mols=mols;
    this.fper = new ConnectivityFingerprint();
    this.fper.setAtomInvariantsGenerator(new ECMolGraphInvariants());
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
    int [][] fps = new int[mols.size()][];
    for (int i=0;i<mols.size();++i)
    {
      this.fper.setMolecule(mols.get(i).cloneMolecule());
      try {
        this.fper.calculate(this.diam);
        fps[i]=fper.getFingerprint();
        if (fps[i]!=null)
        {
          Arrays.sort(fps[i]);
        }
      }
      catch (SearchException e) {
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
              simatrix[i][j]=SimilarityMeasures.tversky(fps[i],fps[j],alpha,beta);
          }
          else
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else if (i>j)
              simatrix[i][j]=simatrix[j][i];
            else
              simatrix[i][j]=SimilarityMeasures.tanimoto(fps[i],fps[j]);
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
    private Sim2D_ECFP_OU_NxNTask task;
    public Status(Sim2D_ECFP_OU_NxNTask task) { this.task=task; }
    public String status()
    {
      if (this.task.n_done_fps<this.task.n_total)
        return (String.format(" %3d / %3d fps",this.task.n_done_fps,this.task.n_total));
      else
        return (String.format(" %3d / %3d rows",this.task.n_done_rows,this.task.n_total));
    }
  }
}
