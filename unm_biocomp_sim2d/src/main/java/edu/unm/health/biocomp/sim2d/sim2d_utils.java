package edu.unm.health.biocomp.sim2d;

import java.io.*;
import java.util.*;

import chemaxon.struc.*; //Molecule

/**	Similarity-related internal static functions.
	@author Jeremy J Yang
*/
public class sim2d_utils
{
  private sim2d_utils() {} //disallow default constructor

  /////////////////////////////////////////////////////////////////////////////
  /**	Sample individuals represented by similarity matrix,
  	maximizing diversity.  Returns list of indices.
  */
  public static List<Integer> SimSample(float [][] simatrix,int n_min,int n_max,int verbose)
  {
    //if (verbose>1) System.err.println("DEBUG: MeanSim(simatrix)="+MeanSim(simatrix,true,true));
    ArrayList<Integer> idxs_sample = new ArrayList<Integer>();
    int n=simatrix.length;
    if (n<n_max)
    {
      for (int i=0;i<n;++i) idxs_sample.add(i);
      return idxs_sample;
    }

    float min_sim=1.0f;
    Integer i_far=0;
    Integer j_far=1;
    // 1st add 2 most distant mols:
    for (int i=0;i<n;++i)
    {
      for (int j=i;j<n;++j)
      {
        if (simatrix[i][j]<min_sim)
        {
          i_far=i; j_far=j;
        }
      }
    }
    idxs_sample.add(i_far);
    idxs_sample.add(j_far);
    //if (verbose>1) System.err.println("DEBUG: min_sim = "+simatrix[i_far][j_far]);

    // Add mols for which the product of distances to already sampled mols
    // is greatest, up to n_max.
    while (idxs_sample.size()<n_max)
    {
      float dist_prod_max=0.0f;
      i_far=null;
      for (int i=0;i<n;++i)
      {
        if (idxs_sample.contains(i)) continue;
        float dist_prod = 1.0f;
        for (int j: idxs_sample)
          dist_prod *= (1.0f-simatrix[i][j]);
        if (dist_prod>dist_prod_max)
        {
          dist_prod_max=dist_prod;
          i_far=i;
        }
      }
      if (i_far==null) break;
      idxs_sample.add(i_far);
      if (verbose>1)
        System.err.println("DEBUG: dist_prod["+i_far+"]="+dist_prod_max);
    }
    return idxs_sample;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return list of mols, subset of input list, specified by index list.
  */
  public static List<Molecule> MolsSample(List<Molecule> mols,List<Integer> idxs_sample, int verbose)
  {
    ArrayList<Molecule> mols_sample = new ArrayList<Molecule>();
    for (int i=0;i<mols.size();++i)
    {
      if (idxs_sample.contains(i))
      {
        mols_sample.add(mols.get(i).cloneMolecule());
      }
    }
    return mols_sample;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static float MeanSim(float [][] simatrix,boolean square,boolean symmetric)
  {
    ArrayList<Float> listOfVals = Matrix2ListOfVals(simatrix,square,symmetric);
    return Mean(listOfVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static float StdDevSim(float [][] simatrix,boolean square,boolean symmetric)
  {
    ArrayList<Float> listOfVals = Matrix2ListOfVals(simatrix,square,symmetric);
    return StdDev(listOfVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  /// Return list of counts for ranges [0.0,0.1), [0.1,0.2),... [0.9,1.0]
  public static ArrayList<Integer> HistoCountsSim(float [][] simatrix,boolean square,boolean symmetric)
  {
    ArrayList<Float> listOfVals = Matrix2ListOfVals(simatrix,square,symmetric);
    return HistoCounts(listOfVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  /// Return list of counts for ranges [0.0,0.1), [0.1,0.2),... [0.9,1.0]
  public static ArrayList<Integer> HistoCounts(ArrayList<Float> listOfVals)
  {
    ArrayList<Integer> histoCounts = new ArrayList<Integer>();
    for (int i=0;i<10;++i) histoCounts.add(0);
    for (float val: listOfVals)
    {
      int i=Math.min((int)Math.floor(val*10),9);
      histoCounts.set(i,histoCounts.get(i)+1);
    }
    return histoCounts;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static ArrayList<Float> Matrix2ListOfVals(float [][] simatrix,boolean square,boolean symmetric)
  {
    ArrayList<Float> listOfVals = new ArrayList<Float>();
    symmetric&=square; // If not square, not symmetric.
    for (int i=0;i<simatrix.length;++i)
    {
      for (int j=0;j<simatrix[i].length;++j)
      {
        if (j==i && square) continue;
        if (j>i && symmetric) break;
        listOfVals.add(simatrix[i][j]);
      }
    }
    return listOfVals;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static float Mean(ArrayList<Float> listOfVals)
  {
    float sum=0.0f;
    float avg=0.0f;
    for (float val: listOfVals)
    {
      sum+=val;
    }
    if (listOfVals.size()==0) avg=0.0f;
    else avg = sum / listOfVals.size();
    return avg;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static float Variance(ArrayList<Float> listOfVals)
  {
    float avg=Mean(listOfVals);
    float d2sum=0.0f;  // sum of delta^2
    float var=0.0f;
    for (float val: listOfVals)
    {
      d2sum+=((val-avg)*(val-avg));
    }
    if (listOfVals.size()==0) var=0.0f;
    else var = d2sum / listOfVals.size();
    return var;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static float StdDev(ArrayList<Float> listOfVals)
  {
    return (float)Math.sqrt(Variance(listOfVals));
  }
}
