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
import org.openscience.cdk.*; //CDK,DefaultChemObjectBuilder,AtomContainer,ChemF
import org.openscience.cdk.fingerprint.*; //Fingerprinter,ExtendedFingerprinter,Substruct
import org.openscience.cdk.exception.*; // CDKException, InvalidSmilesException
import org.openscience.cdk.similarity.*; // Tanimoto


import edu.unm.health.biocomp.cdk.*;
import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.util.threads.*;
 
/**	Callable task for processing ECFP similarity search.
	From this ChemAxon code, calls CDK functions:
	(1) to generate ECFPs,
	(2) to calculate similarity.

	@author Jeremy J Yang
*/
public class Sim2D_ECFP_CDK_NxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> mols;
  private ArrayList<org.openscience.cdk.AtomContainer> mols_cdk;

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

  public TaskStatus taskstatus;
  private int n_total;
  private int n_done_fps;
  private int n_done_rows;
  private Date t0;
  public Sim2D_ECFP_CDK_NxNTask(ArrayList<Molecule> mols,
	Integer diam,
	Integer size,
	Integer arom,Float alpha,Float beta)
  {
    this.mols=mols;
    this.size=size;
    this.diam=diam;
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;

    this.fper = new org.openscience.cdk.fingerprint.ExtendedFingerprinter(this.size,this.diam);

    this.taskstatus = new Status(this);
    if (mols!=null) this.n_total=mols.size();
    else this.n_total=0;
    this.n_done_fps=0;
    this.n_done_rows=0;
    this.t0 = new Date();
  }
  /////////////////////////////////////////////////////////////////////////
  public float [][] call()
  {
    float [][] simatrix = new float[mols.size()][mols.size()];
    BitSet[] fps = new BitSet[mols.size()];
    for (int i=0;i<mols.size();++i)
    {
      String smi = null;
      BitSet fp = null;
      try {
        smi = MolExporter.exportToFormat(mols.get(i),"smiles:-a");
        fp = cdk_utils.CalcFpFromSmiles(smi,this.fper);
      }
      catch (Exception e) {
        System.err.println(e.toString());
      }
      fps[i]=fp;
      ++this.n_done_fps;
    }

    for (int i=0;i<fps.length;++i)
    {
      for (int j=0;j<fps.length;++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else
              simatrix[i][j]= org.openscience.cdk.similarity.Tanimoto.calculate(fps[i],fps[j]);
          }
          else
          {
            if (i==j)
              simatrix[i][j]=1.0f;
            else if (i>j)
              simatrix[i][j]=simatrix[j][i];
            else
              simatrix[i][j]=org.openscience.cdk.similarity.Tanimoto.calculate(fps[i],fps[j]);
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
    private Sim2D_ECFP_CDK_NxNTask task;
    public Status(Sim2D_ECFP_CDK_NxNTask task) { this.task=task; }
    public String status()
    {
      if (this.task.n_done_fps<this.task.n_total)
        return (String.format(" %3d / %3d fps",this.task.n_done_fps,this.task.n_total));
      else
        return (String.format(" %3d / %3d rows",this.task.n_done_rows,this.task.n_total));
    }
  }
}
