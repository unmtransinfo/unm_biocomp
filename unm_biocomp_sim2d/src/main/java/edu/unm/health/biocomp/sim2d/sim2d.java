package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*; //BitSet

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.sss.search.*; // MolSearch,SearchException
import chemaxon.struc.Molecule;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.*; //CFParameters,ChemicalFingerprint,MDGeneratorException,ECFP,ECFPGenerator,ECFPParameters

//NOTE: CDK classes specifed fully qualified (org.openscience.cdk)

import edu.unm.health.biocomp.smarts.*;	//SmartsFile
import edu.unm.health.biocomp.fp.*;	//BinaryFP
import edu.unm.health.biocomp.cdk.*;

/**	Similarity search application.
	@author Jeremy J Yang
*/
public class sim2d
{
  private static String MDL166FILE="/home/data/smarts/mdl166.sma";
  private static String SUNSETFILE="/home/data/smarts/sunsetkeys.sma";
  private static String fptype="path";

  /////////////////////////////////////////////////////////////////////////////
  static void Sim2D_1xN_Path(MolImporter molReader, Molecule molQ,int verbose)
	throws IOException
  {
    CFParameters cfparams = new CFParameters();
    cfparams.setLength(chemaxon.descriptors.CFParameters.DEFAULT_LENGTH); //1024
    cfparams.setBondCount(chemaxon.descriptors.CFParameters.DEFAULT_BOND_COUNT); //7
    cfparams.setBitCount(chemaxon.descriptors.CFParameters.DEFAULT_BITS_SET); //2

    ChemicalFingerprint fpQ = new ChemicalFingerprint(cfparams);
    try { fpQ.generate(molQ); }
    catch (MDGeneratorException e) { System.err.println(e.toString()); }

    if (verbose>0)
    {
      System.err.println("path-FP length= "+cfparams.getLength());
      System.err.println("path-FP bondcount = "+cfparams.getBondCount());
      System.err.println("path-FP bitcount = "+cfparams.getBitCount());
    }
    Molecule mol;
    int n_mol=0;
    while ((mol=molReader.read())!=null)
    {
      ++n_mol;
      System.err.println(""+(n_mol)+". "+mol.getName()+":");

      float sim_tan=0.0f;
      ChemicalFingerprint fp = new ChemicalFingerprint(cfparams);
      try { fp.generate(mol); }
      catch (MDGeneratorException e) { System.err.println("problem generating FP: "+e.toString()); }
      sim_tan = 1.0f - fpQ.getTanimoto(fp);
      System.err.println("\tpath:   "+String.format("%.2f",sim_tan));
    }
    System.err.println("n_mol = "+n_mol);
  }

  /////////////////////////////////////////////////////////////////////////////
  static void Sim2D_1xN_Smarts(String sfpath,String fpname,MolImporter molReader, Molecule molQ,int verbose)
	throws IOException
  {
    SmartsFile smartsfile = new SmartsFile();
    try { smartsfile.parseFile(new File(sfpath),false,fpname); }
    catch (Exception e) { System.err.println("problem parsing smartsfile: "+e.toString()); }
    if (verbose>0)
    {
      System.err.println("smarts: "+smartsfile.size());
      System.err.println("failed smarts: "+smartsfile.getFailedsmarts().size());
      for (String sma:smartsfile.getFailedsmarts())
        System.err.println("\tbad smarts: \""+sma+"\"");
    }

    BinaryFP fpQ = new BinaryFP(smartsfile.size());
    try { fpQ.generate(smartsfile,molQ); }
    catch (Exception e) { System.err.println("problem generating FP: "+e.toString()); }

    Molecule mol;
    int n_mol=0;
    while ((mol=molReader.read())!=null)
    {
      ++n_mol;
      System.err.println(""+(n_mol)+". "+mol.getName()+":");

      float sim_tan=0.0f;
      BinaryFP fp = new BinaryFP(smartsfile.size());
      try { fp.generate(smartsfile,mol); }
      catch (Exception e) { System.err.println("problem generating FP: "+e.toString()); }
      sim_tan = fpQ.tanimoto(fp);
      System.err.println(String.format("\t%s: %.2f",fpname,sim_tan));
    }
    System.err.println("n_mol = "+n_mol);
  }

  /////////////////////////////////////////////////////////////////////////////
  static void Sim2D_1xN_Ecfp(MolImporter molReader, Molecule molQ,int verbose)
	throws IOException
  {
    ECFPGenerator ecfper = new ECFPGenerator();
    int ecfp_diam = chemaxon.descriptors.ECFPParameters.DEFAULT_DIAMETER; //4
    int ecfp_len = chemaxon.descriptors.ECFPParameters.DEFAULT_LENGTH; //1024

    if (verbose>0)
    {
      System.err.println("ecfp-FP length = "+ecfp_len);
      System.err.println("ecfp-FP diameter = "+ecfp_diam);
    }

    ECFPParameters ecfp_params = new ECFPParameters();
    ecfp_params.setDiameter(ecfp_diam);
    ecfp_params.setLength(ecfp_len);
    ECFP ecfpQ = new ECFP(ecfp_params);
    try { ecfper.generate(molQ,ecfpQ); }
    catch (MDGeneratorException e) { System.err.println("ERROR: "+e.toString()); }

    Molecule mol;
    int n_mol=0;
    while ((mol=molReader.read())!=null)
    {
      ++n_mol;
      System.err.println(""+(n_mol)+". "+mol.getName()+":");

      float sim_tan=0.0f;
      ECFP ecfp = new ECFP(ecfp_params);
      try {
        ecfper.generate(mol,ecfp);
        sim_tan = 1.0f - ecfpQ.getTanimoto(ecfp);
      }
      catch (MDGeneratorException e) {
        System.err.println("ERROR: "+e.toString());
        sim_tan=0.0f;
      }
      System.err.println("\tecfp: "+String.format("%.2f",sim_tan));
    }
    System.err.println("n_mol = "+n_mol);
  }
  /////////////////////////////////////////////////////////////////////////////
  static void Sim2D_1xN_Ecfp_CDK(MolImporter molReader, Molecule molQ,int verbose)
	throws Exception
  {
    org.openscience.cdk.fingerprint.IFingerprinter fper = new org.openscience.cdk.fingerprint.ExtendedFingerprinter(1024,4);
    String smiQ = MolExporter.exportToFormat(molQ,"smiles:-a");
    BitSet fpQ = cdk_utils.CalcFpFromSmiles(smiQ,fper);

    Molecule mol;
    int n_mol=0;
    while ((mol=molReader.read())!=null)
    {
      ++n_mol;
      System.err.println(""+(n_mol)+". "+mol.getName()+":");

      float sim_tan=0.0f;
      String smi = null;
      BitSet fp = null;
      try {
        smi = MolExporter.exportToFormat(mol,"smiles:-a");
        fp = cdk_utils.CalcFpFromSmiles(smi,fper);
        sim_tan=org.openscience.cdk.similarity.Tanimoto.calculate(fpQ,fp);
      }
      catch (Exception e) { System.err.println(e.toString()); }
      System.err.println("\tecfp_cdk: "+String.format("%.2f",sim_tan));
    }
    System.err.println("n_mol = "+n_mol);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"sim2d - similarity using binary fingerprints\n"
      +"\n"
      +"usage: sim2d [options]\n"
      +"required:\n"
      +"    -i IFILE ............................. input molecules\n"
      +"    and\n"
      +"    -qsmi SMILES ......................... query smiles\n"
      +"    or\n"
      +"    -qmol QFILE .......................... query molecule\n"
      +"\n"
      +"options:\n"
      +"    -fptype FPTYPE ....................... FP type ["+fptype+"]\n"
      +"    -v ................................... verbose\n"
      +"    -h ................................... this help\n"
      +"\n"
      +"FPTYPEs: maccs|sunset|path|ecfp|ecfp_cdk\n"
    );
    System.exit(1);
  }
  private static int verbose=0;
  private static String qsmi="";
  private static String ifile="";
  private static String qfile="";
  private static String ofile="";

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-qsmi")) qsmi=args[++i];
      else if (args[i].equals("-qmol")) qfile=args[++i];
      else if (args[i].equals("-fptype")) fptype=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
    throws IOException,MolExportException,Exception
  {
    ParseCommand(args);
    if (ifile.length()==0) Help("Input file required.");

    MolImporter molReaderQ = null;
    Molecule molQ = null;
    if (qsmi.length()>0)
    {
      molQ=MolImporter.importMol(qsmi,"smiles:d");
    }
    else if (qfile.length()>0)
    {
      molReaderQ = new MolImporter(new File(qfile),"smiles:d");
      try { molQ=molReaderQ.read(); }
      catch (MolFormatException e) { System.err.println(e.toString()); }
    }
    else
    {
      Help("Input query required: -qsmi or -qmol.");
    }

    System.err.println("query mol: "+MolExporter.exportToFormat(molQ,"smiles:u")+" "+molQ.getName());

    MolImporter molReader = new MolImporter(ifile);

    if (fptype.equalsIgnoreCase("path"))
    {
      Sim2D_1xN_Path(molReader,molQ,verbose);
    }
    else if (fptype.equalsIgnoreCase("maccs"))
    {
      if (verbose>0) {
        System.err.println("MDL166 SMARTS file: "+MDL166FILE);
      }
      Sim2D_1xN_Smarts(MDL166FILE,"MACCS",molReader,molQ,verbose);
    }
    else if (fptype.equalsIgnoreCase("sunset"))
    {
      if (verbose>0) {
        System.err.println("SUNSET SMARTS file: "+SUNSETFILE);
      }
      Sim2D_1xN_Smarts(SUNSETFILE,"Sunset",molReader,molQ,verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp"))
    {
      Sim2D_1xN_Ecfp(molReader,molQ,verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp_cdk"))
    {
      Sim2D_1xN_Ecfp_CDK(molReader,molQ,verbose);
    }
    else
    {
      Help("Invalid fptype: "+fptype);
    }

    System.exit(0);
  }
}
