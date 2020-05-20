package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.lang.reflect.*; //Method
import java.util.*; //BitSet
import java.util.concurrent.*; // ExecutorService, Executors

import chemaxon.formats.*; //MolImporter, MolExporter
import chemaxon.struc.*; //Molecule, MoleculeGraph
import chemaxon.descriptors.*; //CFParameters, ECFPParameters

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
	CFParameters.DEFAULT_LENGTH, //1024
	CFParameters.DEFAULT_BOND_COUNT, //7
	CFParameters.DEFAULT_BITS_SET, //2
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
    Collections.sort(simtask.getHits()); //Why needed? Should be redundant.
    return simtask.getHits();
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
    Collections.sort(simtask.getHits()); //Why needed? Should be redundant.
    return simtask.getHits();
  }

  /////////////////////////////////////////////////////////////////////////////
  static Vector<Sim2DHit> Sim2D_1xN_ECFP_LaunchThread(MolImporter molReader, Molecule molQ, Integer n_max, Integer n_max_hits, Float min_sim, int verbose) throws Exception
  {
    Sim2D_ECFP_1xNTask simtask = new Sim2D_ECFP_1xNTask(
	molQ,
	null, //mols (if preloaded)
	molReader,
	ECFPParameters.DEFAULT_DIAMETER, //4
	ECFPParameters.DEFAULT_LENGTH, //1024
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
    Collections.sort(simtask.getHits()); //Why needed? Should be redundant.
    return simtask.getHits();
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
    Collections.sort(simtask.getHits()); //Why needed? Should be redundant.
    return simtask.getHits();
  }

  /////////////////////////////////////////////////////////////////////////////
  static void WriteHits(Vector<Sim2DHit> hits, String fpname, Integer n_max_hits, Boolean output_smiles, PrintWriter fout_writer, int verbose) throws Exception
  {
    fout_writer.write((output_smiles?"SMILES\t":"")+"Name\t"+fpname+"_Similarity\tFP_Is_Subset\n");
    int i=0;
    for (; i<hits.size(); ++i)
    {
      String smi = hits.get(i).smiles;
      String molname = hits.get(i).name;
      String sim = String.format("%.2f", hits.get(i).sim);
      String subset = hits.get(i).subset ? "1":"0";
      fout_writer.write((output_smiles?(smi+"\t"):"")+molname+"\t"+sim+"\t"+subset+"\n");
      fout_writer.flush();
      if (i>=n_max_hits) break;
    }
    System.err.println("n_hits = "+i);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String APPNAME = "SIM2D";
  private static String fptype="path";

  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +APPNAME+" - similarity using binary fingerprints\n"
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
      +"    -o OFILE ............................. output hits (TSV)\n"
      +"    -fptype FPTYPE ....................... fingerprint type (path|ecfp|smartsfile) ["+fptype+"]\n"
      +"    -smartsfile SMARTSFILE ............... SMARTS file, for -fptype smartsfile, e.g. mdl166.sma, sunsetkeys.sma\n"
      +"    -n_max NMAX .......................... max db mols\n"
      +"    -n_max_hits NMAX_HITS ................ max hit mols\n"
      +"    -min_sim MIN_SIM ..................... minimum similarity\n"
      +"    -output_smiles ....................... include SMILES in output\n"
      +"    -v ................................... verbose\n"
      +"    -h ................................... this help\n"
      +"\n"
    );
    System.exit(1);
  }
  private static int verbose=0;
  private static String ifile=null;
  private static String qsmi=null;
  private static String qfile=null;
  private static String ofile=null;
  private static String smartsfile=null;
  private static Integer n_max_hits=10;
  private static Integer n_max=null;
  private static Float min_sim=0.0f;
  private static Boolean output_smiles=false;

  // java.util.concurrent.ExecutionException: java.lang.NullPointerException
  // May be due to handling of variable "hits" returned from
  // asynchronous thread.
  private static volatile Vector<Sim2DHit> hits = null;

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
      else if (args[i].equals("-output_smiles")) output_smiles=true;
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception
  {
    ParseCommand(args);
    if (verbose>0) {
      String jre_ver=null;
      try {
        Class c = Class.forName("java.lang.Runtime"); // JRE9+
        Method methods[] = c.getMethods();
        for (int i=0; i<methods.length; ++i) {
          if (methods[i].getName() == "version") {
            jre_ver = methods[i].invoke(c).toString();
            break;
          }
        }
      } catch (Exception e) {
        jre_ver = System.getProperty("java.version"); // JRE8-
      }
      System.err.println("JRE_VERSION: "+jre_ver);
    }

    if (ifile==null) Help("Input file required.");

    File fout = (ofile!=null) ? (new File(ofile)) : null;
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout, false)))) : new PrintWriter(System.out);

    Molecule molQ = null;
    if (qsmi!=null) {
      molQ = MolImporter.importMol(qsmi, "smiles:d");
    }
    else if (qfile!=null) {
      MolImporter molReaderQ = new MolImporter(new File(qfile), "smiles:d");
      try { molQ = molReaderQ.read(); }
      catch (MolFormatException e) { Help(e.toString()); }
    }
    else {
      Help("Input query required: -qsmi or -qmol.");
    }

    System.err.println("query mol: "+MolExporter.exportToFormat(molQ, "smiles:u")+" "+molQ.getName());

    MolImporter molReader = new MolImporter(ifile);

    if (fptype.equalsIgnoreCase("path")) {
      hits = Sim2D_1xN_Path_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp")) {
      hits = Sim2D_1xN_ECFP_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
    }
    else if (fptype.equalsIgnoreCase("ecfp_cdk")) {
      hits = Sim2D_1xN_ECFP_CDK_LaunchThread(molReader, molQ, n_max, n_max_hits, min_sim, verbose);
    }
    else if (fptype.equalsIgnoreCase("smartsfile")) {
      if (smartsfile==null) {
        Help("SMARTS file required.");
      }
      else if (!(new File(smartsfile)).exists()) {
        Help("SMARTS file not found: "+smartsfile);
      }
      hits = Sim2D_1xN_Smarts_LaunchThread(molReader, smartsfile, (new File(smartsfile)).getName(), molQ, n_max, n_max_hits, min_sim, verbose);
      fptype = (new File(smartsfile)).getName();
    }
    else {
      Help("Invalid fptype: "+fptype);
    }
    WriteHits(hits, fptype, n_max_hits, output_smiles, fout_writer, verbose);
    fout_writer.close();
    System.exit(0);
  }
}
