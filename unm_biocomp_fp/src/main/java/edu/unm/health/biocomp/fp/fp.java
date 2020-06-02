package edu.unm.health.biocomp.fp;

import java.io.*;
import java.util.*;

import chemaxon.formats.*;	//MolImporter
import chemaxon.struc.*; //Molecule

import edu.unm.health.biocomp.smarts.*;	//SmartsFile
 
/**	Test program for fingerprint processing.
*/
public class fp
{
  private static String MDL166FILE="/home/data/smarts/mdl166.sma";
  
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String args[])
	throws IOException
  {
    if (args.length != 1)
    {
      System.err.println("syntax: fp <infile>");
      System.exit(1);
    }
    String ifile=args[0];
    MolImporter molReader = new MolImporter(ifile);

    SmartsFile smaf = new SmartsFile();
    smaf = new SmartsFile();
    try {
      smaf.parseFile(new File(MDL166FILE),false,"mdl166");
    }
    catch (Exception e) {
      System.err.println("problem parsing smartsfile: "+e.getMessage());
    }
 
    Molecule mol;
    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    while ((mol=molReader.read())!=null)
      mols.add(mol);
    BinaryFP[] fps = fp_utils.Mols2BinaryFPs(mols,smaf);

    for (BinaryFP fp: fps)
      System.out.println(fp.toBitString());

    float[][] simatrix = fp_utils.BinaryFPs2Simatrix(fps);
    for (int i=0;i<simatrix.length;++i)
    {
      for (int j=0;j<simatrix[i].length;++j)
      {
        System.out.print(String.format("%5.2f",simatrix[i][j]));
      }
      System.out.println("");
    }
  }
}
