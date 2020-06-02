package edu.unm.health.biocomp.fp;

import java.io.*;
import java.util.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.*; // CDParameters, CFParameters, ChemicalFingerprint, MDGeneratorException, Metrics, ECFP, ECFPGenerator, ECFPParameters, MDGeneratorException,
 
import edu.unm.health.biocomp.smarts.*; //SmartsFile

/**	Utilities for FP generation and processing.
	@author Jeremy J Yang
*/
public class fp_gen
{
  private static String fptype="path";
  private static Integer path_len = CFParameters.DEFAULT_LENGTH;
  private static Integer path_bondcount = CFParameters.DEFAULT_BOND_COUNT;
  private static Integer path_bitsper = CFParameters.DEFAULT_BITS_SET;
  private static Integer ecfp_len = ECFPParameters.DEFAULT_LENGTH;
  private static Integer ecfp_diam = ECFPParameters.DEFAULT_DIAMETER;
  private static String smartsfile = null;
  private static void Help(String msg)
  {
    if (!msg.equals("")) System.err.println(msg);
    System.err.println(
      "Usage: fp_gen <options>\n"+
      "  required:\n"+
      "      -i INFILE\n"+
      "  options:\n"+
      "      -o OUTFILE ............... SMILES or SDF, with FPs as bitstrings\n"+
      "      -fp_type FPTYPE .......... (path|ecfp|smarts) ["+fptype+"]\n"+
      "      -path_len PL ............. path length ["+path_len+"]\n"+
      "      -path_bondcount BC ....... path bondcount ["+path_bondcount+"]\n"+
      "      -path_bitsper BS ......... path bitsper ["+path_bitsper+"]\n"+
      "      -ecfp_diam ED ............ ECFP diameter ["+ecfp_diam+"]\n"+
      "      -ecfp_len EL ............. ECFP length ["+ecfp_len+"]\n"+
      "      -smartsfile SMARTSFILE ... (structural keys)\n"+
      "      -v ....................... verbose\n"
    );
    System.exit(1);
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void FPGenerate_Path(MolImporter molReader, Integer len, Integer bondcount, Integer bitsper, MolExporter molWriter) throws Exception
  {
    CFParameters cfparams = new CFParameters();
    cfparams.setLength(len);
    cfparams.setBondCount(bondcount);
    cfparams.setBitCount(bitsper);
    ChemicalFingerprint fp = new ChemicalFingerprint(cfparams);

    Molecule mol=null;
    int n_mol=0;
    while (true)
    {
      try { mol=molReader.read(); }
      catch (MolFormatException e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      if (mol==null) break;
      ++n_mol;
      try { fp.generate(mol); }
      catch (Exception e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      mol.setProperty("fp_"+fptype, fp.toBinaryString().replaceAll("\\|", ""));
      molWriter.write(mol);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void FPGenerate_Smarts(MolImporter molReader, String fpath, MolExporter molWriter) throws Exception
  {
    SmartsFile smaf = new SmartsFile();
    smaf.parseFile(new File(fpath), false, (new File(fpath)).getName());

    System.err.println("loaded smarts file: "+fpath+" ("+smaf.getRawtxt().length()+" bytes , "+smaf.size()+" smarts, "+smaf.getDefines().size()+" defines, "+smaf.getFailedsmarts().size()+" failed smarts)");

    BinaryFP fp = new BinaryFP(smaf.size());
    Molecule mol=null;
    int n_mol=0;
    while (true)
    {
      try { mol=molReader.read(); }
      catch (MolFormatException e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      if (mol==null) break;
      ++n_mol;
      try { fp.generate(smaf, mol); }
      catch (Exception e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      mol.setProperty("fp_"+fptype+"_"+smaf.getName(), fp.toBitString());
      molWriter.write(mol);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void FPGenerate_ECFP(MolImporter molReader, Integer diam, Integer len, MolExporter molWriter) throws Exception
  {
    ECFPGenerator fpgen = new ECFPGenerator();
    ECFPParameters fpparams = new ECFPParameters();
    fpparams.setDiameter(diam);
    fpparams.setLength(len);
    ECFP fp = new ECFP(fpparams);
    Molecule mol=null;
    int n_mol=0;
    while (true)
    {
      try { mol=molReader.read(); }
      catch (MolFormatException e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      if (mol==null) break;
      ++n_mol;
      try { fpgen.generate(mol, fp); }
      catch (Exception e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] ("+mol.getName()+") "+e.getMessage());
      }
      mol.setProperty("fp_"+fptype, fp.toBinaryString().replaceAll("\\|", ""));
      molWriter.write(mol);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String args [])
	throws Exception
  {
    if (args.length==0) Help("");
    String ifile=null; String ofile=null;
    int verbose=0;
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) { ifile=args[++i]; }
      else if (args[i].equals("-o")) { ofile=args[++i]; }
      else if (args[i].equals("-fptype")) { fptype=args[++i]; }
      else if (args[i].equals("-path_len")) { path_len=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-path_bondcount")) { path_bondcount=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-path_bitsper")) { path_bitsper=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-ecfp_diam")) { ecfp_diam=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-ecfp_len")) { ecfp_len=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-smartsfile")) { smartsfile=args[++i]; }
      else if (args[i].equals("-v")) { verbose=1; }
      else if (args[i].equals("-vv")) { verbose=2; }
      else { Help("Invalid option: "+args[i]); }
    }
    if (ifile==null) Help("-i required");

    MolImporter molReader;
    if (ifile.equals("-"))
      molReader = new MolImporter(System.in);
    else
      molReader = new MolImporter(ifile);

    MolExporter molWriter;
    if (ofile!=null)
    {
      String ofmt = MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt = "smiles:+n-aT*"; //Kekule for compatibility
      molWriter = new MolExporter(new FileOutputStream(ofile),ofmt);
    }
    else
      molWriter = new MolExporter(System.out, "smiles:+n-aT*");

    if (fptype.equals("smarts")) {
      FPGenerate_Smarts(molReader, smartsfile, molWriter);
    }
    else if (fptype.equals("path")) {
      FPGenerate_Path(molReader, path_len, path_bondcount, path_bitsper, molWriter);
    }
    else if (fptype.equals("ecfp")) {
      FPGenerate_ECFP(molReader, ecfp_diam, ecfp_len, molWriter);
    }
    else {
      Help("Invalid FPTYPE: "+fptype);
    }

  }
}
