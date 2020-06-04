package edu.unm.health.biocomp.qed;

import java.io.*;
import java.lang.Math;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.license.*; //LicenseManager

import edu.unm.health.biocomp.util.*;

/**	QED: Quantitative Estimate of Drug-likeness (Bickerton, et al.).
	Command-line app.
*/
public class qed_app
{

  /////////////////////////////////////////////////////////////////////////////
  /**	Annotate mols with QED results.
  */
  private static void QEDGenerate(QED qed, ArrayList<Molecule> mols, int verbose)
  {
    int n_fail=0;
    for (Molecule mol: mols)
    {
      try { qed.calc(mol); }
      catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); ++n_fail; }
    }
    if (n_fail>0) System.err.println("ERROR: QED unable to process: "+n_fail);
    return;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static void QEDResults_stats(ArrayList<Molecule> mols)
      throws IOException
  {
    String score_tag="WEIGHTED_QED";
    //String score_tag="UNWEIGHTED_QED";

    ArrayList<Double> scores = new ArrayList<Double>();

    for (Molecule mol: mols)
    {
      Double score=null;
      try { score=Double.parseDouble(mol.getProperty(score_tag)); }
      catch (Exception e) { score=0.0; }
      scores.add(score);
    }
    //Collections.sort(scores);

    ArrayList<Integer> vals = new ArrayList<Integer>(Arrays.asList(0,0,0,0,0,0,0,0,0,0));
    ArrayList<Double> xmaxs = new ArrayList<Double>(Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0));

    for (Double score: scores)
    {
      vals.set((int)Math.floor(score*10), vals.get((int)Math.floor(score*10))+1);
    }

    System.err.println("using: "+score_tag);
    for (int i=0;i<vals.size();++i)
    {
      System.err.println(String.format("QED score range: [%3.1f , %3.1f): %3d mols",0.1*i,0.1*(i+1),vals.get(i)));
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"QED = Quantitative Estimate of Drug-likeness (Bickerton et al.)\n"
      +"usage: qed_app [options]\n"
      +"  required:\n"
      +"    -i IFILE .................. input mols\n"
      +"  options:\n"
      +"    -o OFILE .................. output mols, w/ scores (supported: smi|sdf|mrv)\n"
      +"    -nmax NMAX ................ quit after NMAX molecules\n"
      +"    -v ........................ verbose\n"
      +"    -h ........................ this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static int nmax=0;
  private static String ifile=null;
  private static String ofile=null;

  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-nmax")) nmax=Integer.parseInt(args[++i]);
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String args[])
	throws IOException
  {
    ParseCommand(args);
    if (ifile==null) Help("-i required.");
    if (!(new File(ifile).exists())) Help("Non-existent input file: "+ifile);

    MolImporter molReader = new MolImporter(ifile);
    QED qed = null;
    try { qed = new QED(); }
    catch (Exception e) { System.err.println("ERROR: QED init failed."); System.exit(1); }
    qed.loadDefaultAlerts();
    if (verbose>0) System.err.println(qed.numAlerts()+" default alerts loaded.");

    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    int n_fail=0;
    while (true)
    {
      Molecule mol;
      try { mol=molReader.read(); }
      catch (Exception e) { System.err.println("ERROR: MolImporter failed: "+e.getMessage()); ++n_fail; continue; }
      if (mol==null) break;
      mols.add(mol);
      if (mols.size()==nmax) { System.err.println("Warning: nmax mols: "+nmax); break; }
    }
    if (verbose>0)
    {
      String desc = MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
      System.err.println("input format:  "+molReader.getFormat()+" ("+desc+")");
      System.err.println("mols read:  "+mols.size());
    }
    QEDGenerate(qed, mols, verbose);

    String ofmt = "smiles:+n-aT*";
    if (ofile!=null)
    {
      ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt="smiles:+n-aT*"; //Kekule for compatibility
    }
    if (mols.size()>0 && ofmt.matches("^.*smiles.*$"))
    {
      ofmt+="+T"; //property-header
      for (int i=0;i<mols.get(0).getPropertyCount();++i)
        ofmt+=(mols.get(0).getPropertyKey(i)+":");
    }

    MolExporter molWriter=null;
    if (ofile!=null)
      molWriter = new MolExporter(new FileOutputStream(ofile), ofmt);
    else
      molWriter = new MolExporter(System.out, ofmt);

    if (verbose>1)
    {
      System.err.println("output format:  "+molWriter.getFormat()+" ("+ofmt+")");
    }

    int n_out=0;
    for (Molecule mol: mols)
    {
      molWriter.write(mol);
      ++n_out;
    }

    QEDResults_stats(mols);

    System.err.println("mols processed:  "+mols.size());
    System.err.println("mols out:  "+n_out);
    System.err.println("errors:  "+n_fail);
  }
}
