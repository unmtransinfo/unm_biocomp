package edu.unm.health.biocomp.react;

import java.io.*;
import java.text.*;
import java.util.*;

import chemaxon.formats.*; //MolExporter
import chemaxon.struc.*;
import chemaxon.util.MolHandler;
import chemaxon.reaction.*;
import chemaxon.util.iterator.*;

public class graphmol
{
  /////////////////////////////////////////////////////////////////////////////
  /**	
  */
  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"graphmol - generate reduced-graph unique smiles\n"
      +"\n"
      +"usage: graphmol [options]\n"
      +"  required:\n"
      +"    -i IFILE .................. input molecule[s]\n"
      +"  options:\n"
      +"    -o OFILE .................. output edge-unweighted molecular graphs\n"
      +"    -v ........................ verbose\n"
      +"    -h ........................ this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ifile=null;
  private static String ofile=null;
  private static String smifmt="smiles:u0-L-l-e-d-D-p-R-f-w+n-a";

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
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
    ArrayList<String> smirkses = new ArrayList<String>();

    MolExporter molWriter=null;
    if (ofile!=null)
      molWriter=new MolExporter(new FileOutputStream(ofile),smifmt);
    else
      molWriter=new MolExporter(System.out,smifmt);

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
      mols.add(m);
    }
    molReader.close();

    for (Molecule mol: mols)
    {
      mol.dearomatize(); //so types should all be 1,2,3
      MolAtom[] atoms = mol.getAtomArray();
      for (MolAtom atom: atoms)
      {
        int aidx=mol.indexOf(atom);
        int hcount=atom.getImplicitHcount();
        int chg=atom.getCharge();
        while (chg<0)
        {
          atom.setCharge(++chg);
        }
        while (chg>0)
        {
          atom.setCharge(--chg);
          if (hcount>1) atom.setImplicitHcount(--hcount);
        }
      }

      MolBond[] bonds = mol.getBondArray();
      for (MolBond bond: bonds)
      {
        int bidx=mol.indexOf(bond);
        MolAtom a1=bond.getAtom1();
        int hcount1=a1.getImplicitHcount();
        MolAtom a2=bond.getAtom2();
        int hcount2=a2.getImplicitHcount();
        int btype = bond.getType();
        while (btype>1)
        {
          bond.setType(--btype);
          if (hcount1>1) a1.setImplicitHcount(--hcount1);
          if (hcount2>1) a2.setImplicitHcount(--hcount2);
        }
      }
    }
    for (Molecule mol: mols)
    {
      molWriter.write(mol);
    }

  }
}
