package edu.unm.health.biocomp.fp;

import java.io.*;
import java.util.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.marvin.io.MolExportException;
import chemaxon.descriptors.ECFP;
import chemaxon.descriptors.ECFPGenerator;
import chemaxon.descriptors.ECFPParameters;
import chemaxon.descriptors.MDGeneratorException;
 
/**	Utilities for JChem ECFP generation and processing.

	@author Jeremy J Yang
*/
public class ecfp_utils
{
  private static void Help(String msg)
  {
    if (!msg.equals("")) System.err.println(msg);
    System.err.println(
      "usage: ecfp_utils <options>\n"+
      "  required:\n"+
      "          -i INFILE\n"+
      "  options:\n"+
      "          -o OUTFILE ........ fingerprints as bitstrings\n"+
      "          -diam D ........... ECFP diameter ["+ECFPParameters.DEFAULT_DIAMETER+"]\n"+
      "          -len L ............ ECFP length ["+ECFPParameters.DEFAULT_LENGTH+"]\n"+
      "          -v ................ verbose\n"
    );
    System.exit(1);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void main(String args [])
	throws IOException
  {
    if (args.length==0) Help("");
    String ifile=""; String ofile="";
    int fplen=ECFPParameters.DEFAULT_LENGTH; int fpdiam=ECFPParameters.DEFAULT_DIAMETER;
    int verbose=0;
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) { ifile=args[++i]; }
      else if (args[i].equals("-o")) { ofile=args[++i]; }
      else if (args[i].equals("-diam")) { fpdiam=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-len")) { fplen=Integer.parseInt(args[++i]); }
      else if (args[i].equals("-v")) { verbose=1; }
      else if (args[i].equals("-vv")) { verbose=2; }
      else {
        Help("illegal option: "+args[i]);
      }
    }
    if (ifile.equals("")) Help("-i required");

    MolImporter molReader;
    if (ifile.equals("-"))
      molReader=new MolImporter(System.in);
    else
      molReader=new MolImporter(ifile);

    PrintWriter fout;
    if (ofile.isEmpty() || ofile.equals("-"))
      fout=new PrintWriter(System.out);
    else
      fout=new PrintWriter(new BufferedWriter(new FileWriter(ofile,false)));

    ECFPGenerator fpgen = new ECFPGenerator();
    ECFPParameters fpparams = new ECFPParameters();
    fpparams.setDiameter(fpdiam);
    fpparams.setLength(fplen);
    ECFP fp = new ECFP(fpparams);

    Molecule mol=null;
    int n_mol=0;
    while (true)
    {
      try {
        mol=molReader.read();
      }
      catch (MolFormatException e) {
        System.err.println("ERROR: ["+(n_mol+1)+"] "+e.getMessage());
        fout.println("");
      }
      if (mol==null) break;
      ++n_mol;
      try {
        fpgen.generate(mol,fp);
        fout.println(fp.toBinaryString().replaceAll("\\|",""));
      }
      catch (MDGeneratorException e) {
        System.err.println("ERROR: ["+n_mol+"] "+e.getMessage());
        fout.println("");
      }
    }
    fout.flush();
    fout.close();
  }
}
