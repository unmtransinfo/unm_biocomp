package edu.unm.health.biocomp.fp;

import java.io.*;
import java.util.*;

import chemaxon.formats.*;	//MolFormatException
import chemaxon.util.MolHandler;
import chemaxon.sss.search.*;
import chemaxon.struc.*;
import chemaxon.marvin.io.MolExportException;

import edu.unm.health.biocomp.smarts.*;	//SmartsFile
 
/**	Static methods for fingerprint processing.
*/
public class fp_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate FPs, smarts based, for input mols.
  */
  public static BinaryFP[] Mols2BinaryFPs(List<Molecule> mols, String smafpath)
	throws IOException
  {
    SmartsFile smaf = new SmartsFile();
    smaf = new SmartsFile();
    try { smaf.parseFile(new File(smafpath), false, smafpath); }
    catch (Exception e) { System.err.println("Problem parsing smartsfile: "+e.getMessage()); }
    return Mols2BinaryFPs(mols, smaf);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate FPs, smarts based, for input mols.
  */
  public static BinaryFP[] Mols2BinaryFPs(List<Molecule> mols, SmartsFile smaf)
  {
    BinaryFP [] fps = new BinaryFP[mols.size()];
    for (int i=0;i<mols.size();++i)
    {
      BinaryFP fp = new BinaryFP(smaf.size());
      try { fp.generate(smaf, mols.get(i)); }
      catch (Exception e) { System.err.println("Bad FP: "+e.getMessage()); fp=null; }
      fps[i]=fp;
    }
    return fps;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate similarity matrix (symmetrical square array) from input FPs.
  */
  public static float[][] BinaryFPs2Simatrix(BinaryFP[] fps)
  {
    float [][] simatrix = new float[fps.length][fps.length];
    for (int i=0;i<fps.length;++i)
    {
      for (int j=0;j<fps.length;++j)
      {
        try {
          if (i==j)
            simatrix[i][j]=1.0f;
          else if (i>j)
            simatrix[i][j]=simatrix[j][i];
          else
            simatrix[i][j]=fps[i].tanimoto(fps[j]);
        }
        catch (Exception e) {
          simatrix[i][j]=0.0f;
        }
      }
    }
    return simatrix;
  }
}
