package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.sss.search.*; // MolSearch,SearchException
import chemaxon.struc.Molecule;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.*; //CDParameters,CFParameters,ChemicalFingerprint,MDGeneratorException,Metrics

import edu.unm.health.biocomp.smarts.*;	//SmartsFile
import edu.unm.health.biocomp.fp.*;	//BinaryFP

/**	Similarity sampling application.
	@author Jeremy J Yang
*/
public class sim_sample
{
  private static String MDL166FILE="/home/data/smarts/mdl166.sma";
  private static String SUNSETFILE="/home/data/smarts/sunsetkeys.sma";

  private static String fptype="sunset";
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"sim_sample - similarity sampling using binary fingerprints\n"
      +"usage: sim_sample [options]\n"
      +"  required:\n"
      +"    -i IFILE\n"
      +"  options:\n"
      +"    -o OFILE\n"
      +"    -fptype path|maccs|sunset ... FP type ["+fptype+"]\n"
      +"    -n_min NMIN\n"
      +"    -n_max NMAX\n"
      +"    -v [-vv] ... verbose [very]\n"
      +"    -h ... this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ifile=null;
  private static String ofile=null;
  private static Integer n_min=0;
  private static Integer n_max=0;
  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-fptype")) fptype=args[++i];
      else if (args[i].equals("-n_min")) n_min=Integer.parseInt(args[++i]);
      else if (args[i].equals("-n_max")) n_max=Integer.parseInt(args[++i]);
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
    throws IOException,MolExportException,Exception
  {
    ParseCommand(args);
    if (ifile==null) Help("Input file required.");
    if (!(new File(ifile).exists())) Help("Non-existent input file: "+ifile);
    MolImporter molReader = new MolImporter(ifile);

    MolExporter molWriter;
    if (ofile!=null)
    {
      String ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt="smiles:+n-a"; //Kekule for compatibility
      molWriter=new MolExporter(new FileOutputStream(ofile),ofmt);
    }
    else
      molWriter=new MolExporter(System.out,"smiles:+n-a");


    SmartsFile sunsetsf = new SmartsFile();
    try {
      sunsetsf.parseFile(new File(SUNSETFILE),false,"sunset");
    }
    catch (Exception e) {
      System.err.println("problem parsing smartsfile: "+e.getMessage());
    }
    if (verbose>0)
    {
      System.err.println("Sunset smarts: "+sunsetsf.size());
      System.err.println("failed smarts: "+sunsetsf.getFailedsmarts().size());
      for (String sma:sunsetsf.getFailedsmarts())
        System.err.println("\tbad smarts: \""+sma+"\"");
    }

    Molecule mol;
    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    while ((mol=molReader.read())!=null)
    {
      System.err.println(mol.getName()+":");
      mols.add(mol);
    }
    System.err.println("n_mol = "+mols.size());

    BinaryFP[] fps = fp_utils.Mols2BinaryFPs(mols,sunsetsf);
    float[][] simatrix = fp_utils.BinaryFPs2Simatrix(fps);
    List<Integer> idxs_sample = sim2d_utils.SimSample(simatrix,n_min,n_max,verbose);
    System.err.println("n_sample = "+idxs_sample.size());

    List<Molecule> mols_sample = sim2d_utils.MolsSample(mols,idxs_sample,verbose);

    for (Molecule mol_out: mols_sample)
    {
      molWriter.write(mol_out);
    }

    System.exit(0);
  }
}
