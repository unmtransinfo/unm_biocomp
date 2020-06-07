package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.lang.reflect.*; //Method
import java.util.*; //BitSet
import java.util.concurrent.*; // ExecutorService, Executors

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import chemaxon.formats.*; //MolImporter, MolExporter
import chemaxon.struc.*; //Molecule, MoleculeGraph
import chemaxon.descriptors.*; //CFParameters, ECFPParameters

//NOTE: CDK classes specifed fully qualified (org.openscience.cdk)

import edu.unm.health.biocomp.util.threads.*; //TaskUtils
import edu.unm.health.biocomp.util.jre.*; //JREUtils
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
  static void WriteHits(Vector<Sim2DHit> hits, String fpname, Integer n_max_hits, PrintWriter fout_writer, int verbose) throws Exception
  {
    fout_writer.write("SMILES\tName\t"+fpname+"_Similarity\tFP_Is_Subset\n");
    int i=0;
    for (; i<hits.size(); ++i)
    {
      String smi = hits.get(i).smiles;
      String molname = hits.get(i).name;
      String sim = String.format("%.2f", hits.get(i).sim);
      String subset = hits.get(i).subset ? "1":"0";
      fout_writer.write(smi+"\t"+molname+"\t"+sim+"\t"+subset+"\n");
      fout_writer.flush();
      if (i>=n_max_hits) break;
    }
    System.err.println("n_hits = "+i);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String APPNAME = "SIM2D";
  private static String fptype="path";
  private static String ifile=null;
  private static String qsmi=null;
  private static String qfile=null;
  private static String ofile=null;
  private static String smartsfile=null;
  private static Integer n_max_hits=10;
  private static Integer n_max=null;
  private static Float min_sim=0.0f;
  private static int verbose=0;

  // java.util.concurrent.ExecutionException: java.lang.NullPointerException
  // May be due to handling of variable "hits" returned from
  // asynchronous thread.
  private static volatile Vector<Sim2DHit> hits = null;

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws Exception
  {
    String HELPHEADER =  "SIM2D: similarity using binary fingerprints";
    Options opts = new Options();
    opts.addOption(Option.builder("i").required().hasArg().argName("IFILE").desc("Input file").build());
    opts.addOption(Option.builder("o").hasArg().argName("OFILE").desc("Output file").build());
    opts.addOption(Option.builder("qsmi").hasArg().argName("QSMI").desc("Query SMILES").build());
    opts.addOption(Option.builder("qfile").hasArg().argName("QFILE").desc("Query molecule file").build());
    opts.addOption(Option.builder("fptype").hasArg().argName("FPTYPE").desc("FP type (path|ecfp|smarts)").build());
    opts.addOption(Option.builder("smartsfile").hasArg().argName("SMARTSFILE").desc("SMARTS file, for -fptype smarts").build());
    opts.addOption(Option.builder("min_sim").type(Float.class).hasArg().argName("MIN_SIM").desc("Min similarity").build());
    opts.addOption(Option.builder("n_max").type(Integer.class).hasArg().argName("N_MAX").desc("Max mols processed").build());
    opts.addOption(Option.builder("n_max_hits").type(Integer.class).hasArg().argName("N_MAX_HITS").desc("Max hits returned").build());
    opts.addOption("v", "verbose", false, "Verbose.");
    opts.addOption("h", "help", false, "Show this help.");
    HelpFormatter helper = new HelpFormatter();
    CommandLineParser clip = new PosixParser();
    CommandLine clic = null;
    try {
      clic = clip.parse(opts, args);
    } catch (ParseException e) {
      helper.printHelp(APPNAME, HELPHEADER, opts, e.getMessage(), true);
      System.exit(0);
    }
    ifile = clic.getOptionValue("i");
    if (clic.hasOption("o")) ofile = clic.getOptionValue("o");
    if (clic.hasOption("qsmi")) qsmi = clic.getOptionValue("qsmi");
    if (clic.hasOption("qfile")) qfile = clic.getOptionValue("qfile");
    if (clic.hasOption("smartsfile")) { smartsfile = clic.getOptionValue("smartsfile"); }
    if (clic.hasOption("n_max")) { n_max = (Integer)(clic.getParsedOptionValue("n_max")); }
    if (clic.hasOption("n_max_hits")) { n_max_hits = (Integer)(clic.getParsedOptionValue("n_max_hits")); }
    if (clic.hasOption("min_sim")) { min_sim = (Float)(clic.getParsedOptionValue("min_sim")); }
    if (clic.hasOption("v")) { verbose = 1; }
    if (clic.hasOption("h")) {
      helper.printHelp(APPNAME, HELPHEADER, opts, "", true);
      System.exit(0);
    }

    if (verbose>0) System.err.println("JRE_VERSION: "+JREUtils.JREVersion());

    File fout = (ofile!=null) ? (new File(ofile)) : null;
    PrintWriter fout_writer = (fout!=null) ? (new PrintWriter(new BufferedWriter(new FileWriter(fout, false)))) : new PrintWriter(System.out);

    Molecule molQ = null;
    if (qsmi!=null) {
      molQ = MolImporter.importMol(qsmi, "smiles:d");
    }
    else if (qfile!=null) {
      MolImporter molReaderQ = new MolImporter(new File(qfile), "smiles:d");
      molQ = molReaderQ.read();
    }
    else {
      helper.printHelp(APPNAME, HELPHEADER, opts, "Input query required: -qsmi or -qmol.", true);
      System.exit(0);
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
       
        helper.printHelp(APPNAME, HELPHEADER, opts, "SMARTS file required.", true);
        System.exit(0);
      }
      else if (!(new File(smartsfile)).exists()) {
        helper.printHelp(APPNAME, HELPHEADER, opts, ("SMARTS file not found: "+smartsfile), true);
        System.exit(0);
      }
      hits = Sim2D_1xN_Smarts_LaunchThread(molReader, smartsfile, (new File(smartsfile)).getName(), molQ, n_max, n_max_hits, min_sim, verbose);
      fptype = (new File(smartsfile)).getName();
    }
    else {
        helper.printHelp(APPNAME, HELPHEADER, opts, ("Invalid fptype: "+fptype), true);
        System.exit(0);
    }
    WriteHits(hits, fptype, n_max_hits, fout_writer, verbose);
    fout_writer.close();
    System.exit(0);
  }
}
