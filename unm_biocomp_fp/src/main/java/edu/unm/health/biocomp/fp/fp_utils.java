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
	Note: sim2d_lib is dependent on fp_lib.
	Note: fp_lib is dependent on smarts_lib.
	Note: smarts_lib not dependent on fp_lib nor sim2d_lib.
	Please avoid circular dependencies.
*/
public class fp_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate FPs, smarts based, for input mols.
  */
  public static BinaryFP[] Mols2BinaryFPs(List<Molecule> mols,String smafpath)
	throws IOException
  {
    SmartsFile smaf = new SmartsFile();
    smaf = new SmartsFile();
    try {
      smaf.parseFile(new File(smafpath),false,smafpath);
    }
    catch (Exception e) {
      System.err.println("problem parsing smartsfile: "+e.getMessage());
    }
    return Mols2BinaryFPs(mols,smaf);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Generate FPs, smarts based, for input mols.
  */
  public static BinaryFP[] Mols2BinaryFPs(List<Molecule> mols,SmartsFile smaf)
  {
    BinaryFP [] fps = new BinaryFP[mols.size()];
    for (int i=0;i<mols.size();++i)
    {
      BinaryFP fp = new BinaryFP(smaf.size());
      try { fp.generate(smaf,mols.get(i)); }
      catch (SearchException e) {
        System.err.println("bad FP: "+e.getMessage());
        fp=null;
      }
      catch (Exception e) {
        System.err.println("bad FP: "+e.getMessage());
        fp=null;
      }
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
