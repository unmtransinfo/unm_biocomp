package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*; //BitSet
import java.util.concurrent.*; // ExecutorService, Executors

import chemaxon.formats.*; //MolExporter
import chemaxon.struc.*; //Molecule, MoleculeGraph
import chemaxon.descriptors.*; //CFParameters

//NOTE: CDK classes specifed fully qualified (org.openscience.cdk)

import edu.unm.health.biocomp.util.threads.*; //TaskUtils
import edu.unm.health.biocomp.smarts.*;	//SmartsFile
import edu.unm.health.biocomp.fp.*;	//BinaryFP
import edu.unm.health.biocomp.cdk.*;

/**	Similarity search application.
	@author Jeremy J Yang
*/
public class sim2d
{
  /////////////////////////////////////////////////////////////////////////////
  static Vector<Sim2DHit> Sim2D_1xN_Path_LaunchThread(MolImporter molReader, Molecule molQ, Integer n_max, Integer n_max_hits, Float min_sim, int verbose) throws Exception
  {
    Sim2D_Path_1xNTask simtask = new Sim2D_Path_1xNTask(
	molQ,
	null, //mols (if preloaded)
	molReader,
	chemaxon.descriptors.CFParameters.DEFAULT_LENGTH, //1024
	chemaxon.descriptors.CFParameters.DEFAULT_BOND_COUNT, //7
	chemaxon.descriptors.CFParameters.DEFAULT_BITS_SET, //2
	MoleculeGraph.AROM_GENERAL,
	null, //alpha
	null, //beta
	min_sim,
	n_max,
	n_max_hits,
	true); // sorthits
    int tpoll = 1000; //msec
    ExecutorService exec = Executors.newSingleThreadExecutor();
    TaskUtils.ExecTask(exec, simtask, simtask.taskstatus, APPNAME, tpoll);
    Vector<Sim2DHit> hits = simtask.getHits();
    Collections.sort(hits); //Why needed? Should be redundant.
    return hits;
  }

  /////////////////////////////////////////////////////////////////////////////
  static Vector<Sim2DHit> Sim2D_1xN_Smarts_LaunchThread(MolImporter molReader, String sfpath, String fpname, Molecule molQ, Integer n_max, Integer n_max_hits, Float min_sim, int verbose) throws Exception
  {
    SmartsFile smartsfile = new SmartsFile();
    smartsfile.parseFile(new File(sfpath), false, fpname);
    Sim2D_Smarts_1xNTask simtask = new Sim2D_Smarts_1xNTask(
	molQ,
	null, //mols (if preloaded)
	molReader,
	smartsfile,
	MoleculeGraph.AROM_GENERAL,
	null, //alpha
	null, //beta
	min_sim,
	n_max,
	n_max_hits,
	true); // sorthits
    int tpoll = 1000; //msec
    ExecutorService exec = Executors.newSingleThreadExecutor();
    TaskUtils.ExecTask(exec, simtask, simtask.taskstatus, APPNAME, tpoll);
    Vector<Sim2DHit> hits = simtask.getHits();
    Collections.sort(hits); //Why needed? Should be redundant.
    return hits;
  }

  /////////////////////////////////////////////////////////////////////////////
  static Vector<Sim2DHit> Sim2D_1xN_ECFP_LaunchThread(MolImporter molReader, Molecule molQ, Integer n_max, Integer n_max_hits, Float min_sim, int verbose) throws Exception
  {
    Sim2D_ECFP_1xNTask simtask = new Sim2D_ECFP_1xNTask(
	molQ,
	null, //mols (if preloaded)
	molReader,
	chemaxon.descriptors.ECFPParameters.DEFAULT_DIAMETER, //4
	chemaxon.descriptors.ECFPParameters.DEFAULT_LENGTH, //1024
	MoleculeGraph.AROM_GENERAL,
	null, //alpha
	null, //beta
	min_sim,
	n_max,
	n_max_hits,
	true); // sorthits
    int tpoll = 1000; //msec
    ExecutorService exec = Executors.newSingleThreadExecutor();
    TaskUtils.ExecTask(exec, simtask, simtask.taskstatus, APPNAME, tpoll);
    Vector<Sim2DHit> hits = simtask.getHits();
    Collections.sort(hits); //Why needed? Should be redundant.
    return hits;
  }

  /////////////////////////////////////////////////////////////////////////////
  static Vector<Sim2DHit> Sim2D_1xN_ECFP_CDK_LaunchThread(MolImporter molReader, Molecule molQ, Integer n_max, Integer n_max_hits, Float min_sim, int verbose) throws Exception
  {
    Sim2D_ECFP_CDK_1xNTask simtask = new Sim2D_ECFP_CDK_1xNTask(
	molQ,
	null, //mols (if preloaded)
	molReader,
	4,
	1024,
	MoleculeGraph.AROM_GENERAL,
	null, //alpha
	null, //beta
	min_sim,
	n_max,
	n_max_hits,
	true); // sorthits
    int tpoll = 1000; //msec
    ExecutorService exec = Executors.newSingleThreadExecutor();
    TaskUtils.ExecTask(exec, simtask, simtask.taskstatus, APPNAME, tpoll);
    Vector<Sim2DHit> hits = simtask.getHits();
    Collections.sort(hits); //Why needed? Should be redundant.
    return hits;
  }

  /////////////////////////////////////////////////////////////////////////////
  static void WriteHits(Vector<Sim2DHit> hits, String fpname, Integer n_max_hits, PrintWriter fout_writer, int verbose)
	throws Exception
  {
    fout_writer.write("Name\t"+fpname+"_Similarity\tFP_Is_Subset\n");
    int i=0;
    for (; i<hits.size(); ++i)
    {
      String smi = hits.get(i).smiles;
      String molname = hits.get(i).name;
      String sim = String.format("%.2f", hits.get(i).sim);
      String subset = hits.get(i).subset ? "1":"0";
      fout_writer.write(molname+"\t"+sim+"\t"+subset+"\n");
      fout_writer.flush();
      if (i>=n_max_hits) break;
    }
    System.err.println("n_hits = "+i);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String fptype="path";

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
      +"    -fptype FPTYPE ....................... fingerprint type (path|ecfp|smartsfile) ["+fptype+"]\n"
      +"    -smartsfile SMARTSFILE ............... SMARTS file, for -fptype smartsfile, e.g. mdl166.sma, sunsetkeys.sma\n"
      +"    -n_max NMAX .......................... max db mols\n"
      +"    -n_max_hits NMAX_HITS ................ max hit mols\n"
      +"    -min_sim MIN_SIM ..................... minimum similarity\n"
      +"    -v ................................... verbose\n"
      +"    -h ................................... this help\n"
      +"\n"
    );
    System.exit(1);
  }
  private static String APPNAME = "sim2d";
  private static int verbose=0;
  private static String ifile=null;
  private static String qsmi=null;
  private static String qfile=null;
  private static String ofile=null;
  private static String smartsfile=null;
  private static Integer n_max_hits=10;
  private static Integer n_max=null;
  private static Float min_sim=0.0f;

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
      else if (args[i].equals("-smartsfile")) smartsfile=args[++i];
      else if (args[i].equals("-min_sim")) min_sim=Float.parseFloat(args[++i]);
      else if (args[i].equals("-n_max")) n_max=Integer.parseInt(args[++i]);
      else if (args[i].equals("-n_max_hits")) n_max_hits=Integer.parseInt(args[++i]);
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception
  {
    ParseCommand(args);
    if (ifile==null) Help("Input file required.");

    File fout = (ofile!=null) ? (new File(ofile)) : null;
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout, false)))) : new PrintWriter(System.out);

    MolImporter molReaderQ = null;
    Molecule molQ = null;
    if (qsmi!=null) {
      molQ = MolImporter.importMol(qsmi, "smiles:d");
    }
    else if (qfile!=null) {
      molReaderQ = new MolImporter(new File(qfile), "smiles:d");
      try { molQ=molReaderQ.read(); }
      catch (MolFormatException e) { System.err.println(e.toString()); }
    }
    else {
      Help("Input query required: -qsmi or -qmol.");
    }

    System.err.println("query mol: "+MolExporter.exportToFormat(molQ, "smiles:u")+" "+molQ.getName());

    MolImporter molReader = new MolImporter(ifile);

    if (fptype.equalsIgnoreCase("path")) {
      Vector<Sim2DHit> hits = Sim2D_1xN_Path_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
      WriteHits(hits, fptype, n_max_hits, fout_writer, verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp")) {
      Vector<Sim2DHit> hits = Sim2D_1xN_ECFP_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
      WriteHits(hits, fptype, n_max_hits, fout_writer, verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp_cdk")) {
      Vector<Sim2DHit> hits = Sim2D_1xN_ECFP_CDK_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
      WriteHits(hits, fptype, n_max_hits, fout_writer, verbose);
    }
    else if (fptype.equalsIgnoreCase("smartsfile")) {
      if (smartsfile==null) {
        Help("SMARTS file required.");
      }
      if (!(new File(smartsfile)).exists()) {
        Help("SMARTS file not found: "+smartsfile);
      }
      if (verbose>0) {
        System.err.println("SMARTS file: "+smartsfile);
      }
      Vector<Sim2DHit> hits = Sim2D_1xN_Smarts_LaunchThread(molReader, smartsfile, (new File(smartsfile)).getName(), molQ, n_max, n_max_hits, min_sim, verbose);
      WriteHits(hits, (new File(smartsfile)).getName(), n_max_hits, fout_writer, verbose);
    }
    else {
      Help("Invalid fptype: "+fptype);
    }
    fout_writer.close();
    System.exit(0);
  }
}
