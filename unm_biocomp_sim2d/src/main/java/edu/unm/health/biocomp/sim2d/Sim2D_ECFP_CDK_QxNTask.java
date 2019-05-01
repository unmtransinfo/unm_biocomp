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

import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.similarity.Tanimoto;

import edu.unm.health.biocomp.cdk.*;
import edu.unm.health.biocomp.fp.*;
import edu.unm.health.biocomp.threads.*;
 
/**	Callable task for processing ECFP similarity search.
	From this ChemAxon code, calls CDK functions:
	(1) to generate ECFPs,
	(2) to calculate similarity.

	@author Jeremy J Yang
*/
public class Sim2D_ECFP_CDK_QxNTask 
	implements Callable<float [][]>
{
  private ArrayList<Molecule> molsQ;
  private ArrayList<Molecule> molsDB;

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
  private int q_total;
  private int q_done_fps;
  private int q_done_rows;
  private int n_total;
  private int n_done_fps;
  private Date t0;
  public Sim2D_ECFP_CDK_QxNTask(ArrayList<Molecule> molsQ,ArrayList<Molecule> molsDB,
	Integer diam,
	Integer size,
	Integer arom,Float alpha,Float beta)
  {
    this.molsQ=molsQ;
    this.molsDB=molsDB;
    this.size=size;
    this.diam=diam;
    this.arom=arom;
    this.alpha=alpha;
    this.beta=beta;

    this.fper = new org.openscience.cdk.fingerprint.ExtendedFingerprinter(this.size,this.diam);

    this.taskstatus = new Status(this);
    this.q_total=molsQ.size();
    this.q_done_fps=0;
    this.q_done_rows=0;
    this.n_total=molsDB.size();
    this.n_done_fps=0;
    this.t0 = new Date();
  }
  /////////////////////////////////////////////////////////////////////////
  public float [][] call()
  {
    float [][] simatrix = new float[molsQ.size()][molsDB.size()];
    BitSet[] fpsQ = new BitSet[molsQ.size()];
    BitSet[] fpsDB = new BitSet[molsDB.size()];
    for (int i=0;i<molsQ.size();++i)
    {
      String smi = null;
      BitSet fp = null;
      try {
        smi = MolExporter.exportToFormat(molsQ.get(i),"smiles:-a");
        fp = cdk_utils.CalcFpFromSmiles(smi,this.fper);
      }
      catch (Exception e) {
        System.err.println(e.toString());
      }
      fpsQ[i]=fp;
      ++this.q_done_fps;
    }
    for (int i=0;i<molsDB.size();++i)
    {
      String smi = null;
      BitSet fp = null;
      try {
        smi = MolExporter.exportToFormat(molsDB.get(i),"smiles:-a");
        fp = cdk_utils.CalcFpFromSmiles(smi,this.fper);
      }
      catch (Exception e) {
        System.err.println(e.toString());
      }
      fpsDB[i]=fp;
      ++this.n_done_fps;
    }

    for (int i=0;i<fpsQ.length;++i)
    {
      for (int j=0;j<fpsDB.length;++j)
      {
        try {
          if (alpha!=null && beta!=null)
          {
            simatrix[i][j]= org.openscience.cdk.similarity.Tanimoto.calculate(fpsQ[i],fpsDB[j]);
          }
          else
          {
            simatrix[i][j]=org.openscience.cdk.similarity.Tanimoto.calculate(fpsQ[i],fpsDB[j]);
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
    private Sim2D_ECFP_CDK_QxNTask task;
    public Status(Sim2D_ECFP_CDK_QxNTask task) { this.task=task; }
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
