package edu.unm.health.biocomp.qed;

import static java.lang.Math.exp;
import static java.lang.Math.log;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*; // CommandLine, CommandLineParser, HelpFormatter, OptionBuilder, Options, ParseException, PosixParser
import org.apache.commons.cli.Option.*; // Builder

import chemaxon.formats.*; // MolExporter, MolFormatException, MolImporter
import chemaxon.license.LicenseException;
import chemaxon.struc.Molecule;
import chemaxon.sss.search.*; // SearchException, StandardizedMolSearch
import chemaxon.marvin.calculations.*; // HBDAPlugin, TPSAPlugin, TopologyAnalyserPlugin, logPPlugin
import chemaxon.marvin.io.MolExportException;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.reaction.*; // Standardizer, StandardizerException

/**
 * @author Oleg Ursu
 */
public class QED {
  
  public static final HashMap<String, HashMap<String, Double>> PARAMS = new HashMap<String, HashMap<String,Double>>();
  public static final HashMap<String, Double> UNWEIGHTS = new HashMap<String, Double>();
  public static final HashMap<String, Double> WEIGHTS = new HashMap<String, Double>();
  
  private String inputFileName;
  private String outputFileName;
  private String outputFormat = "smiles";
  private String alertsFileName = null;
  private logPPlugin logPCalculator;
  private HashMap<String, Double> desirabilityFunctions = new HashMap<String, Double>();
  private Standardizer std;
  private ArrayList<Molecule> alerts;
  private StandardizedMolSearch ms;
  private TopologyAnalyserPlugin topoPlugin;
  private TPSAPlugin tpsaCalculator;
  private HBDAPlugin hbdaCalculator = new HBDAPlugin();
  private boolean verbose = false;
  private boolean skipOnError = false;
  
  /**
   * @param args
   * @throws IOException 
   * @throws IllegalArgumentException 
   * @throws MolExportException 
   */
  public static void main(String[] args) throws Exception {
    QED runner = new QED();
    if (!runner.processCommandLine(args)) { return; }
    runner.run();
  }
  
  @SuppressWarnings("static-access")
  public boolean processCommandLine(String[] args) {
    CommandLineParser parser = new PosixParser();
    Options opts = new Options();
    opts.addOption(Option.builder("i").longOpt("ifile").required().hasArg()
      .argName("IFILE").desc("Input file").build());
    opts.addOption(Option.builder("o").longOpt("ofile").required().hasArg()
      .argName("OFILE").desc("Output file").build());
    opts.addOption(Option.builder("f").longOpt("ofmt").hasArg()
      .argName("OFMT").desc("Output format (smiles|sdf) [smiles]").build());
    opts.addOption(Option.builder("a").longOpt("alertsfile").hasArg()
      .argName("AFILE").desc("Alerts file (smarts)").build());
    opts.addOption(Option.builder("g").longOpt("skip_on_error")
      .hasArg(false).desc("Continue on error [false]").build());
    opts.addOption("v", "verbose", false, "Verbose.");
    opts.addOption("h", "help", false, "Show this help.");
    HelpFormatter helpFormater = new HelpFormatter();
    try {
      CommandLine cmd = parser.parse(opts, args);
      inputFileName = cmd.getOptionValue("i");
      outputFileName = cmd.getOptionValue("o");
      if (cmd.hasOption("f")) {
        outputFormat = cmd.getOptionValue("f");
        outputFormat = outputFormat.toLowerCase();
        if (outputFormat.equals("smi")) { outputFormat = "smiles"; }
      }
      if (cmd.hasOption("a")) { alertsFileName = cmd.getOptionValue("a"); }
      if (cmd.hasOption("v")) { verbose = true; }
      if (cmd.hasOption("g")) { skipOnError = true; }
      if (cmd.hasOption("h")) { throw new ParseException("Help requested"); }
    } catch (ParseException ex) {
      System.err.println(ex.getMessage());
      helpFormater.printHelp("QED", "", opts, "QED: Quantitative Estimate of Drug-likeness (Bickerton, et al.)", true);
      return false;
    }
    return true;
  }
  
  public QED() throws PluginException, StandardizerException {
    logPCalculator = new logPPlugin();
    logPCalculator.setlogPMethod(logPPlugin.METHOD_WEIGHTED);
    logPCalculator.setWeightOfMethods(1, 1, 1, 0);
    logPCalculator.setCloridIonConcentration(0.1);
    logPCalculator.setNaKIonConcentration(0.1);
    logPCalculator.setUserTypes("logPTrue,logPMicro");
    std = new Standardizer("sgroups:expand..keepone..neutralize..removeexplicitH..aromatize");
    alerts = new ArrayList<Molecule>();
    ms = new StandardizedMolSearch();
    topoPlugin = new TopologyAnalyserPlugin();
    tpsaCalculator = new TPSAPlugin();
    tpsaCalculator.setExcludeSulfur(true);
    tpsaCalculator.setExcludePhosphorus(true);
    hbdaCalculator = new HBDAPlugin();
    hbdaCalculator.setExcludeHalogens(true);
    hbdaCalculator.setExcludeSulfur(true);
    desirabilityFunctions.put("MW", Double.valueOf(0.0));
    desirabilityFunctions.put("LOGP", Double.valueOf(0.0));
    desirabilityFunctions.put("HBA", Double.valueOf(0.0));
    desirabilityFunctions.put("HBD", Double.valueOf(0.0));
    desirabilityFunctions.put("PSA", Double.valueOf(0.0));
    desirabilityFunctions.put("ROTB", Double.valueOf(0.0));
    desirabilityFunctions.put("AROM", Double.valueOf(0.0));
    desirabilityFunctions.put("ALERTS", Double.valueOf(0.0));
    if (verbose) {
      System.out.println("QED initialized");
    }
  }
  
  static {
    // ADS parameter sets for 8 molecular properties
    HashMap<String, Double> MW = new HashMap<String, Double>();
    MW.put("A", 2.817065973);
    MW.put("B", 392.5754953);
    MW.put("C", 290.7489764);
    MW.put("D", 2.419764353);
    MW.put("E", 49.22325677);
    MW.put("F", 65.37051707);
    MW.put("DMAX", 104.98055614);
    PARAMS.put("MW", MW);
    
    HashMap<String, Double> LOGP = new HashMap<String, Double>();
    LOGP.put("A", 3.172690585);
    LOGP.put("B", 137.8624751);
    LOGP.put("C", 2.534937431);
    LOGP.put("D", 4.581497897);
    LOGP.put("E", 0.822739154);
    LOGP.put("F", 0.576295591);
    LOGP.put("DMAX", 131.31866035);
    PARAMS.put("LOGP", LOGP);
    
    HashMap<String, Double> HBA = new HashMap<String, Double>();
    HBA.put("A", 2.948620388);
    HBA.put("B", 160.4605972);
    HBA.put("C", 3.615294657);
    HBA.put("D", 4.435986202);
    HBA.put("E", 0.290141953);
    HBA.put("F", 1.300669958);
    HBA.put("DMAX", 148.77630464);
    PARAMS.put("HBA", HBA);
    
    HashMap<String, Double> HBD = new HashMap<String, Double>();
    HBD.put("A", 1.618662227);
    HBD.put("B", 1010.051101);
    HBD.put("C", 0.985094388);
    HBD.put("D", 0.000000000001);
    HBD.put("E", 0.713820843);
    HBD.put("F", 0.920922555);
    HBD.put("DMAX", 258.16326158);
    PARAMS.put("HBD", HBD);
    
    HashMap<String, Double> PSA = new HashMap<String, Double>();
    PSA.put("A", 1.876861559);
    PSA.put("B", 125.2232657);
    PSA.put("C", 62.90773554);
    PSA.put("D", 87.83366614);
    PSA.put("E", 12.01999824);
    PSA.put("F", 28.51324732);
    PSA.put("DMAX", 104.56861672);
    PARAMS.put("PSA", PSA);
    
    HashMap<String, Double> ROTB = new HashMap<String, Double>();
    ROTB.put("A", 0.01);
    ROTB.put("B", 272.4121427);
    ROTB.put("C", 2.558379970);
    ROTB.put("D", 1.565547684);
    ROTB.put("E", 1.271567166);
    ROTB.put("F", 2.758063707);
    ROTB.put("DMAX", 105.44204028);
    PARAMS.put("ROTB", ROTB);
    
    HashMap<String, Double> AROM = new HashMap<String, Double>();
    AROM.put("A", 3.217788970);
    AROM.put("B", 957.7374108);
    AROM.put("C", 2.274627939);
    AROM.put("D", 0.000000000001);
    AROM.put("E", 1.317690384);
    AROM.put("F", 0.375760881);
    AROM.put("DMAX", 312.33726097);
    PARAMS.put("AROM", AROM);
    
    HashMap<String, Double> ALERTS = new HashMap<String, Double>();
    ALERTS.put("A", 0.01);
    ALERTS.put("B", 1199.094025);
    ALERTS.put("C", -0.09002883);
    ALERTS.put("D", 0.000000000001);
    ALERTS.put("E", 0.185904477);
    ALERTS.put("F", 0.875193782);
    ALERTS.put("DMAX", 417.72531400);
    PARAMS.put("ALERTS", ALERTS);
    
    UNWEIGHTS.put("MW", 1.0);
    UNWEIGHTS.put("LOGP", 1.0);
    UNWEIGHTS.put("HBA", 1.0);
    UNWEIGHTS.put("HBD", 1.0);
    UNWEIGHTS.put("PSA", 1.0);
    UNWEIGHTS.put("ROTB", 1.0);
    UNWEIGHTS.put("AROM", 1.0);
    UNWEIGHTS.put("ALERTS", 1.0);
    
    WEIGHTS.put("MW", 0.66);
    WEIGHTS.put("LOGP", 0.46);
    WEIGHTS.put("HBA", 0.05);
    WEIGHTS.put("HBD", 0.61);
    WEIGHTS.put("PSA", 0.06);
    WEIGHTS.put("ROTB", 0.65);
    WEIGHTS.put("AROM", 0.48);
    WEIGHTS.put("ALERTS", 0.95);
  }
  
  public String[] getMolPropKeys(Molecule mol) {
    String[] keys = new String[mol.properties().size() + 18];
    System.arraycopy(mol.properties().getKeys(), 0, keys, 0, keys.length - 18);
    int i = keys.length - 18;
    for (String str : WEIGHTS.keySet()) {
      keys[i++] = str;
      keys[i++] = str + "_DES";
    }
    keys[i++] = "UNWEIGHTED_QED";
    keys[i] = "WEIGHTED_QED";
    return keys;
  }
  
  public void calc(Molecule mol) throws PluginException, SearchException, IOException {
    double mw = mol.getMass();
    desirabilityFunctions.put("MW", Double.valueOf(ads(mw, PARAMS.get("MW").get("A"), PARAMS.get("MW").get("B"), PARAMS.get("MW").get("C"), PARAMS.get("MW").get("D"), PARAMS.get("MW").get("E"), PARAMS.get("MW").get("F"), PARAMS.get("MW").get("DMAX"))));
    //mol.properties().set("MW", new MDoubleProp(mw));
    mol.setProperty("MW", Double.toString(mw));
    logPCalculator.setMolecule(mol);
    logPCalculator.run();
    double logp = logPCalculator.getlogPTrue();
    desirabilityFunctions.put("LOGP", Double.valueOf(ads(logp, PARAMS.get("LOGP").get("A"), PARAMS.get("LOGP").get("B"), PARAMS.get("LOGP").get("C"), PARAMS.get("LOGP").get("D"), PARAMS.get("LOGP").get("E"), PARAMS.get("LOGP").get("F"), PARAMS.get("LOGP").get("DMAX"))));
    //mol.properties().set("LOGP", new MDoubleProp(logp));
    mol.setPropertyObject("LOGP", Double.toString(logp));
    hbdaCalculator.setMolecule(mol);
    hbdaCalculator.run();
    int hba = hbdaCalculator.getAcceptorAtomCount();
    desirabilityFunctions.put("HBA", Double.valueOf(ads(hba, PARAMS.get("HBA").get("A"), PARAMS.get("HBA").get("B"), PARAMS.get("HBA").get("C"), PARAMS.get("HBA").get("D"), PARAMS.get("HBA").get("E"), PARAMS.get("HBA").get("F"), PARAMS.get("HBA").get("DMAX"))));
    //mol.properties().set("HBA", new MIntegerProp(hba));
    mol.setProperty("HBA", Integer.toString(hba));
    int hbd = hbdaCalculator.getDonorAtomCount();
    desirabilityFunctions.put("HBD", Double.valueOf(ads(hbd, PARAMS.get("HBD").get("A"), PARAMS.get("HBD").get("B"), PARAMS.get("HBD").get("C"), PARAMS.get("HBD").get("D"), PARAMS.get("HBD").get("E"), PARAMS.get("HBD").get("F"), PARAMS.get("HBD").get("DMAX"))));
    //mol.properties().set("HBD", new MIntegerProp(hbd));
    mol.setProperty("HBD", Integer.toString(hbd));
    tpsaCalculator.setMolecule(mol);
    tpsaCalculator.run();
    double psa = tpsaCalculator.getSurfaceArea();
    desirabilityFunctions.put("PSA", Double.valueOf(ads(psa, PARAMS.get("PSA").get("A"), PARAMS.get("PSA").get("B"), PARAMS.get("PSA").get("C"), PARAMS.get("PSA").get("D"), PARAMS.get("PSA").get("E"), PARAMS.get("PSA").get("F"), PARAMS.get("PSA").get("DMAX"))));
    //mol.properties().set("PSA", new MDoubleProp(psa));
    mol.setProperty("PSA", Double.toString(psa));
    topoPlugin.setMolecule(mol);
    topoPlugin.run();
    int rotb = topoPlugin.getRotatableBondCount();
    desirabilityFunctions.put("ROTB", Double.valueOf(ads(rotb, PARAMS.get("ROTB").get("A"), PARAMS.get("ROTB").get("B"), PARAMS.get("ROTB").get("C"), PARAMS.get("ROTB").get("D"), PARAMS.get("ROTB").get("E"), PARAMS.get("ROTB").get("F"), PARAMS.get("ROTB").get("DMAX"))));
    //mol.properties().set("ROTB", new MIntegerProp(rotb));
    mol.setProperty("ROTB", Integer.toString(rotb));
    int arom = topoPlugin.getAromaticRingCount();
    desirabilityFunctions.put("AROM", Double.valueOf(ads(arom, PARAMS.get("AROM").get("A"), PARAMS.get("AROM").get("B"), PARAMS.get("AROM").get("C"), PARAMS.get("AROM").get("D"), PARAMS.get("AROM").get("E"), PARAMS.get("AROM").get("F"), PARAMS.get("AROM").get("DMAX"))));
    // mol.properties().set("AROM", new MIntegerProp(arom));
    mol.setProperty("AROM", Integer.toString(arom));
    int alerts = alertCount(mol);
    desirabilityFunctions.put("ALERTS", Double.valueOf(ads(alerts, PARAMS.get("ALERTS").get("A"), PARAMS.get("ALERTS").get("B"), PARAMS.get("ALERTS").get("C"), PARAMS.get("ALERTS").get("D"), PARAMS.get("ALERTS").get("E"), PARAMS.get("ALERTS").get("F"), PARAMS.get("ALERTS").get("DMAX"))));
    //mol.properties().set("ALERTS", new MIntegerProp(alerts));
    mol.setProperty("ALERTS", Integer.toString(alerts));
    double unweightedNumerator = 0.0;
    double weightedNumerator = 0.0;
    for (Map.Entry<String, Double> df : desirabilityFunctions.entrySet()) {
      unweightedNumerator += UNWEIGHTS.get(df.getKey()) * log(df.getValue());
      weightedNumerator += WEIGHTS.get(df.getKey()) * log(df.getValue());
      //mol.properties().set(df.getKey() + "_DES", new MDoubleProp(df.getValue()));
      mol.setProperty(df.getKey() + "_DES", df.getValue().toString());
    }
    double qedUW = exp(unweightedNumerator/sum(UNWEIGHTS.values()));
    double qedW = exp(weightedNumerator/sum(WEIGHTS.values()));
    //mol.properties().set("UNWEIGHTED_QED", new MDoubleProp(qedUW));
    mol.setProperty("UNWEIGHTED_QED", Double.toString(qedUW));
    //mol.properties().set("WEIGHTED_QED", new MDoubleProp(qedW));
    mol.setProperty("WEIGHTED_QED", Double.toString(qedW));
  }
  
  private double sum(Collection<Double> collection) {
    double sum = 0.0;
    for (Double d : collection) {
      sum += d;
    }
    return sum;
  }
  
  public void run() {
    MolImporter reader = null;
    MolExporter writer = null;
    Molecule mol;
    try {
      if (alertsFileName == null) {
        loadDefaultAlerts();
      } else {
        loadAlertsFromFile(alertsFileName);
      }
      if (verbose) {
        if (alertsFileName != null) {
          System.out.println(alerts.size() + " alerts loaded from " + alertsFileName);
        } else {
          System.out.println(alerts.size() + " default alerts loaded");
        }
      }
      int count = 0;
      reader = new MolImporter(new FileInputStream(inputFileName));
      mol = reader.read();
      String[] keys = getMolPropKeys(mol);
      writer = new MolExporter(new FileOutputStream(outputFileName), outputFormat, true, keys);
      Molecule cloneMol = mol.cloneMolecule();
      try {
        std.standardize(cloneMol);
        calc(cloneMol);
      } catch (SearchException e) {
        e.printStackTrace();
        if (!skipOnError) {
          return;
        }
      } catch (LicenseException e) {
        e.printStackTrace();
        return;
      } catch (PluginException e) {
        e.printStackTrace();
        if (!skipOnError) {
          return;
        }
      }
      replaceMolProps(cloneMol, mol);
      writer.write(mol);
      count++;
      while((mol = reader.read()) != null) {
        count++;
        cloneMol = mol.cloneMolecule();
        try {
          std.standardize(cloneMol);
          calc(cloneMol);
        } catch (SearchException e) {
          e.printStackTrace();
          if (!skipOnError) {
            return;
          }
        } catch (LicenseException e) {
          e.printStackTrace();
          return;
        } catch (PluginException e) {
          e.printStackTrace();
          if (!skipOnError) {
            return;
          }
        }
        replaceMolProps(cloneMol, mol);
        writer.write(mol);
        if (verbose && count % 100 == 0) {
          System.out.println(count + " records processed");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (writer != null) {
        try {
          writer.close();
        } catch (MolExportException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  
  public int alertCount(Molecule mol) {
    ms.setTarget(mol);
    int count = 0;
    for (Molecule query : alerts) {
      ms.setQuery(query);
      try {
        if (ms.isMatching()) {
          count++;
        }
      } catch (SearchException e) {
        e.printStackTrace();
        try {
          System.err.println(MolExporter.exportToFormat(query, "mrv"));
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
    }
    return count;
  }
  
  private void replaceMolProps(Molecule src, Molecule dest) {
    dest.properties().clear();
    for (String propName : src.properties().getKeys()) {
      dest.properties().set(propName, src.properties().get(propName));
    }
  }
  
  //Asymmetric Double Sigmoidal functions
  
  public double ads(double x, double a, double  b, double c, double d, double e, double f, double dx_max) {
    return ((a+(b/(1+exp(-1*(x-c+d/2)/e))*(1-1/(1+exp(-1*(x-c-d/2)/f)))))/dx_max);
  }
  
  public double ads(int x, double a, double  b, double c, double d, double e, double f, double dx_max) {
    return ((a+(b/(1+exp(-1*(x-c+d/2)/e))*(1-1/(1+exp(-1*(x-c-d/2)/f)))))/dx_max);
  }
  
  public void loadAlertsFromFile(File file) {
    if (file == null) {
      throw new UnsupportedOperationException("NULL input file");
    }
    BufferedReader reader = null;
    String line;
    try {
      alerts.clear();
      reader = new BufferedReader(new FileReader(file));
      while((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          continue;
        }
        String[] tokens = line.split("\\s+");
        alerts.add(MolImporter.importMol(tokens[0], "smarts:d"));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
  public void loadAlertsFromFile(String fname) {
    if (fname == null) {
      throw new UnsupportedOperationException("NULL input file");
    }
    loadAlertsFromFile(new File(fname));
  }
  
  public void loadDefaultAlerts() throws MolFormatException {
    alerts.clear();
    alerts.add(MolImporter.importMol("*1[O,S,N]*1","smarts:d"));
    alerts.add(MolImporter.importMol("[S,C](=[O,S])[F,Br,Cl,I]","smarts:d"));
    alerts.add(MolImporter.importMol("[CX4][Cl,Br,I]","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c]S(=O)(=O)O[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("[$([CH]),$(CC)]#CC(=O)[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("[$([CH]),$(CC)]#CC(=O)O[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("n[OH]","smarts:d"));
    alerts.add(MolImporter.importMol("[$([CH]),$(CC)]#CS(=O)(=O)[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("C=C(C=O)C=O","smarts:d"));
    alerts.add(MolImporter.importMol("n1c([F,Cl,Br,I])cccc1","smarts:d"));
    alerts.add(MolImporter.importMol("[CH1](=O)","smarts:d"));
    alerts.add(MolImporter.importMol("[O,o][O,o]","smarts:d"));
    alerts.add(MolImporter.importMol("[C;!R]=[N;!R]","smarts:d"));
    alerts.add(MolImporter.importMol("[N!R]=[N!R]","smarts:d"));
    alerts.add(MolImporter.importMol("[#6](=O)[#6](=O)","smarts:d"));
    alerts.add(MolImporter.importMol("[S,s][S,s]","smarts:d"));
    alerts.add(MolImporter.importMol("[N,n][NH2]","smarts:d"));
    alerts.add(MolImporter.importMol("C(=O)N[NH2]","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c]=S","smarts:d"));
    alerts.add(MolImporter.importMol("[$([CH2]),$([CH][CX4]),$(C([CX4])[CX4])]=[$([CH2]),$([CH][CX4]),$(C([CX4])[CX4])]","smarts:d"));
    alerts.add(MolImporter.importMol("C1(=[O,N])C=CC(=[O,N])C=C1","smarts:d"));
    alerts.add(MolImporter.importMol("C1(=[O,N])C(=[O,N])C=CC=C1","smarts:d"));
    alerts.add(MolImporter.importMol("a21aa3a(aa1aaaa2)aaaa3","smarts:d"));
    alerts.add(MolImporter.importMol("a31a(a2a(aa1)aaaa2)aaaa3","smarts:d"));
    alerts.add(MolImporter.importMol("a1aa2a3a(a1)A=AA=A3=AA=A2","smarts:d"));
    alerts.add(MolImporter.importMol("c1cc([NH2])ccc1","smarts:d"));
    alerts.add(MolImporter.importMol("[Hg,Fe,As,Sb,Zn,Se,se,Te,B,Si,Na,Ca,Ge,Ag,Mg,K,Ba,Sr,Be,Ti,Mo,Mn,Ru,Pd,Ni,Cu,Au,Cd,Al,Ga,Sn,Rh,Tl,Bi,Nb,Li,Pb,Hf,Ho]","smarts:d"));
    alerts.add(MolImporter.importMol("I","smarts:d"));
    alerts.add(MolImporter.importMol("OS(=O)(=O)[O-]","smarts:d"));
    alerts.add(MolImporter.importMol("[N+](=O)[O-]","smarts:d"));
    alerts.add(MolImporter.importMol("C(=O)N[OH]","smarts:d"));
    alerts.add(MolImporter.importMol("C1NC(=O)NC(=O)1","smarts:d"));
    alerts.add(MolImporter.importMol("[SH]","smarts:d"));
    alerts.add(MolImporter.importMol("[S-]","smarts:d"));
    alerts.add(MolImporter.importMol("c1ccc([Cl,Br,I,F])c([Cl,Br,I,F])c1[Cl,Br,I,F]","smarts:d"));
    alerts.add(MolImporter.importMol("c1cc([Cl,Br,I,F])cc([Cl,Br,I,F])c1[Cl,Br,I,F]","smarts:d"));
    alerts.add(MolImporter.importMol("[CR1]1[CR1][CR1][CR1][CR1][CR1][CR1]1","smarts:d"));
    alerts.add(MolImporter.importMol("[CR1]1[CR1][CR1]cc[CR1][CR1]1","smarts:d"));
    alerts.add(MolImporter.importMol("[CR2]1[CR2][CR2][CR2][CR2][CR2][CR2][CR2]1","smarts:d"));
    alerts.add(MolImporter.importMol("[CR2]1[CR2][CR2]cc[CR2][CR2][CR2]1","smarts:d"));
    alerts.add(MolImporter.importMol("[CH2R2]1N[CH2R2][CH2R2][CH2R2][CH2R2][CH2R2]1","smarts:d"));
    alerts.add(MolImporter.importMol("[CH2R2]1N[CH2R2][CH2R2][CH2R2][CH2R2][CH2R2][CH2R2]1","smarts:d"));
    alerts.add(MolImporter.importMol("C#C","smarts:d"));
    alerts.add(MolImporter.importMol("[OR2,NR2]@[CR2]@[CR2]@[OR2,NR2]@[CR2]@[CR2]@[OR2,NR2]","smarts:d"));
    alerts.add(MolImporter.importMol("[$([N+R]),$([n+R]),$([N+]=C)][O-]","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c]=N[OH]","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c]=NOC=O","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c](=O)[CX4,CR0X3,O][C,c](=O)","smarts:d"));
    alerts.add(MolImporter.importMol("c1ccc2c(c1)ccc(=O)o2","smarts:d"));
    alerts.add(MolImporter.importMol("[O+,o+,S+,s+]","smarts:d"));
    alerts.add(MolImporter.importMol("N=C=O","smarts:d"));
    alerts.add(MolImporter.importMol("[NX3,NX4][F,Cl,Br,I]","smarts:d"));
    alerts.add(MolImporter.importMol("c1ccccc1OC(=O)[#6]","smarts:d"));
    alerts.add(MolImporter.importMol("[CR0]=[CR0][CR0]=[CR0]","smarts:d"));
    alerts.add(MolImporter.importMol("[C+,c+,C-,c-]","smarts:d"));
    alerts.add(MolImporter.importMol("N=[N+]=[N-]","smarts:d"));
    alerts.add(MolImporter.importMol("C12C(NC(N1)=O)CSC2","smarts:d"));
    alerts.add(MolImporter.importMol("c1c([OH])c([OH,NH2,NH])ccc1","smarts:d"));
    alerts.add(MolImporter.importMol("P","smarts:d"));
    alerts.add(MolImporter.importMol("[N,O,S]C#N","smarts:d"));
    alerts.add(MolImporter.importMol("C=C=O","smarts:d"));
    alerts.add(MolImporter.importMol("[Si][F,Cl,Br,I]","smarts:d"));
    alerts.add(MolImporter.importMol("[SX2]O","smarts:d"));
    alerts.add(MolImporter.importMol("[SiR0,CR0](c1ccccc1)(c2ccccc2)(c3ccccc3)","smarts:d"));
    alerts.add(MolImporter.importMol("O1CCCCC1OC2CCC3CCCCC3C2","smarts:d"));
    alerts.add(MolImporter.importMol("N=[CR0][N,n,O,S]","smarts:d"));
    alerts.add(MolImporter.importMol("[cR2]1[cR2][cR2]([Nv3X3,Nv4X4])[cR2][cR2][cR2]1[cR2]2[cR2][cR2][cR2]([Nv3X3,Nv4X4])[cR2][cR2]2","smarts:d"));
    alerts.add(MolImporter.importMol("C=[C!r]C#N","smarts:d"));
    alerts.add(MolImporter.importMol("[cR2]1[cR2]c([N+0X3R0,nX3R0])c([N+0X3R0,nX3R0])[cR2][cR2]1","smarts:d"));
    alerts.add(MolImporter.importMol("[cR2]1[cR2]c([N+0X3R0,nX3R0])[cR2]c([N+0X3R0,nX3R0])[cR2]1","smarts:d"));
    alerts.add(MolImporter.importMol("[cR2]1[cR2]c([N+0X3R0,nX3R0])[cR2][cR2]c1([N+0X3R0,nX3R0])","smarts:d"));
    alerts.add(MolImporter.importMol("[OH]c1ccc([OH,NH2,NH])cc1","smarts:d"));
    alerts.add(MolImporter.importMol("c1ccccc1OC(=O)O","smarts:d"));
    alerts.add(MolImporter.importMol("[SX2H0][N]","smarts:d"));
    alerts.add(MolImporter.importMol("c12ccccc1(SC(S)=N2)","smarts:d"));
    alerts.add(MolImporter.importMol("c12ccccc1(SC(=S)N2)","smarts:d"));
    alerts.add(MolImporter.importMol("c1nnnn1C=O","smarts:d"));
    alerts.add(MolImporter.importMol("s1c(S)nnc1NC=O","smarts:d"));
    alerts.add(MolImporter.importMol("S1C=CSC1=S","smarts:d"));
    alerts.add(MolImporter.importMol("C(=O)Onnn","smarts:d"));
    alerts.add(MolImporter.importMol("OS(=O)(=O)C(F)(F)F","smarts:d"));
    alerts.add(MolImporter.importMol("N#CC[OH]","smarts:d"));
    alerts.add(MolImporter.importMol("N#CC(=O)","smarts:d"));
    alerts.add(MolImporter.importMol("S(=O)(=O)C#N","smarts:d"));
    alerts.add(MolImporter.importMol("N[CH2]C#N","smarts:d"));
    alerts.add(MolImporter.importMol("C1(=O)NCC1","smarts:d"));
    alerts.add(MolImporter.importMol("S(=O)(=O)[O-,OH]","smarts:d"));
    alerts.add(MolImporter.importMol("NC[F,Cl,Br,I]","smarts:d"));
    alerts.add(MolImporter.importMol("C=[C!r]O","smarts:d"));
    alerts.add(MolImporter.importMol("[NX2+0]=[O+0]","smarts:d"));
    alerts.add(MolImporter.importMol("[OR0,NR0][OR0,NR0]","smarts:d"));
    alerts.add(MolImporter.importMol("(C(=O)O[C,H1]).(C(=O)O[C,H1]).(C(=O)O[C,H1])","smarts:d"));
    alerts.add(MolImporter.importMol("[CX2R0][NX3R0]","smarts:d"));
    alerts.add(MolImporter.importMol("c1ccccc1[C;!R]=[C;!R]c2ccccc2","smarts:d"));
    alerts.add(MolImporter.importMol("[NX3R0,NX4R0,OR0,SX2R0][CX4][NX3R0,NX4R0,OR0,SX2R0]","smarts:d"));
    alerts.add(MolImporter.importMol("[s,S,c,C,n,N,o,O]~[n+,N+](~[s,S,c,C,n,N,o,O])(~[s,S,c,C,n,N,o,O])~[s,S,c,C,n,N,o,O]","smarts:d"));
    alerts.add(MolImporter.importMol("[s,S,c,C,n,N,o,O]~[nX3+,NX3+](~[s,S,c,C,n,N])~[s,S,c,C,n,N]","smarts:d"));
    alerts.add(MolImporter.importMol("[*]=[N+]=[*]","smarts:d"));
    alerts.add(MolImporter.importMol("[SX3](=O)[O-,OH]","smarts:d"));
    alerts.add(MolImporter.importMol("N#N","smarts:d"));
    alerts.add(MolImporter.importMol("F.F.F.F","smarts:d"));
    alerts.add(MolImporter.importMol("[R0;D2][R0;D2][R0;D2][R0;D2]","smarts:d"));
    alerts.add(MolImporter.importMol("[cR,CR]~C(=O)NC(=O)~[cR,CR]","smarts:d"));
    alerts.add(MolImporter.importMol("C=!@CC=[O,S]","smarts:d"));
    alerts.add(MolImporter.importMol("[#6,#8,#16][C,c](=O)O[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("c[C;R0](=[O,S])[C,c]","smarts:d"));
    alerts.add(MolImporter.importMol("c[SX2][C;!R]","smarts:d"));
    alerts.add(MolImporter.importMol("C=C=C","smarts:d"));
    alerts.add(MolImporter.importMol("c1nc([F,Cl,Br,I,S])ncc1","smarts:d"));
    alerts.add(MolImporter.importMol("c1ncnc([F,Cl,Br,I,S])c1","smarts:d"));
    alerts.add(MolImporter.importMol("c1nc(c2c(n1)nc(n2)[F,Cl,Br,I])","smarts:d"));
    alerts.add(MolImporter.importMol("[C,c]S(=O)(=O)c1ccc(cc1)F","smarts:d"));
    alerts.add(MolImporter.importMol("[15N,13C,18O,2H,34S]","smarts:d"));
  }
  
  public int numAlerts()
  {
    return alerts.size();
  }
}
