package edu.unm.health.biocomp.react;

import java.io.*;
import java.text.*;
import java.util.*;

import chemaxon.formats.*; //MolExporter
import chemaxon.struc.*;
import chemaxon.util.MolHandler;

// Requires JChem 5.11+:
import chemaxon.standardizer.*; //Standardizer,
import chemaxon.standardizer.advancedactions.*; //TransformAction
import chemaxon.standardizer.configuration.*; //StandardizerConfiguration,
import chemaxon.standardizer.configuration.writer.*; //StandardizerXMLWriter,

public class standardize_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	
  */
  public static void StandardizeMols(ArrayList<Molecule> mols,Standardizer std)
      throws IOException
  {
    for (Molecule mol: mols)
    {
      System.err.println("DEBUG (before): "+MolExporter.exportToFormat(mol,"smiles"));
      std.standardize(mol);
      System.err.println("DEBUG (after): "+MolExporter.exportToFormat(mol,"smiles"));
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"standardize_utils - standardization test program \n"
      +"\n"
      +"usage: standardize_utils [options]\n"
      +"  required:\n"
      +"    -i IFILE .................. input molecule[s]\n"
      +"    -smirksfile SMIRKSFILE ............ SMIRKS file (applied sequentially)\n"
      +"  options:\n"
      +"    -o OFILE .................. output reactions or products\n"
      +"    -v ........................ verbose\n"
      +"    -h ........................ this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ifile=null;
  private static String smirksfile=null;
  private static String ofile=null;
  private static String smifmt="cxsmiles:u-L-l-e-d-D-p-R-f-w";

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-smirksfile")) smirksfile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
    throws IOException
  {
    ParseCommand(args);
    if (ifile==null) Help("Input file required.");
    if (!(new File(ifile).exists())) Help("Non-existent input file: "+ifile);
    MolImporter molReader = new MolImporter(ifile);
    if (smirksfile==null) Help("-smirksfile required.");
    BufferedReader buff=new BufferedReader(new FileReader(smirksfile));
    ArrayList<String> smirkses = new ArrayList<String>();
    String line;
    while ((line=buff.readLine())!=null)
    {
      smirkses.add(line);
    }

    MolExporter molWriter=null;
    if (ofile!=null)
    {
      String ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt="smiles:+n-a"; //Kekule for compatibility
      molWriter=new MolExporter(new FileOutputStream(ofile),ofmt);
    }

    if (verbose>0)
      System.err.println("JChem version: "+com.chemaxon.version.VersionInfo.getVersion());

    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    int n_failed=0;
    while (true)
    {
      Molecule m=null;
      try { m=molReader.read(); }
      catch (MolFormatException e)
      {
        System.err.println("ERROR: MolImporter failed: "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (m==null) break;
      m.aromatize(MoleculeGraph.AROM_GENERAL); // aromatize so smirks work correctly.
      mols.add(m);
    }
    molReader.close();

    for (Molecule mol: mols)
      System.err.println("DEBUG (before):"+MolExporter.exportToFormat(mol,"smiles"));

    StandardizerConfiguration stdconf = new StandardizerConfiguration();
    HashMap<String,String> params = new HashMap<String,String>();

    int i_x=0;
    for (String smirks: smirkses)
    {
      ++i_x;

      // Way #1 (preferred but does not work):
      //params.put("ID","smirks_"+i_x);
      //params.put("Structure",smirks);

      TransformAction transact = new TransformAction(params);

      // Way #2 (stdconf is valid but crashes):
      RxnMolecule rxn=null;
      try { rxn=RxnMolecule.getReaction(MolImporter.importMol(smirks,"smarts:")); }
      catch (MolFormatException e) { System.err.println("ERROR: "+e.getMessage()); }
      if (rxn==null) { System.err.println("ERROR: rxn==null"); continue; }
      
      transact.setTransform(rxn);
      transact.setID("smirks_"+i_x);

      stdconf.addAction(transact);
    }

    Standardizer std = new Standardizer(stdconf);

    if (std.getConfiguration()==null)
    {
      System.err.println("ERROR: std.getConfiguration()==null");
    }
    else
    {
      System.err.println("ERROR: std.getConfiguration().isValid(): "+std.getConfiguration().isValid());
      if (!std.getConfiguration().isValid())
        System.err.println("ERROR: "+StandardizerConfiguration.createErrorMessage(std.getConfiguration()));
      System.err.println("ERROR: std.getConfiguration().getActionCount(): "+std.getConfiguration().getActionCount());
      System.err.println("ERROR: std.getConfiguration(): "+std.getConfiguration().toString());

      StandardizerXMLWriter writer = new StandardizerXMLWriter();
      writer.writeConfiguration(std.getConfiguration(), System.out);

    }

    StandardizeMols(mols,std);

    for (Molecule mol: mols)
      System.err.println("DEBUG (after): "+MolExporter.exportToFormat(mol,"smiles"));

  }
}
