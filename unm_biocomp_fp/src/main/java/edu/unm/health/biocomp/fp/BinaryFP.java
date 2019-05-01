package edu.unm.health.biocomp.fp;

import java.io.*;
import java.util.*;
import java.lang.Exception;

import chemaxon.formats.*;
import chemaxon.sss.search.MolSearch;
import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import chemaxon.descriptors.Metrics;

import edu.unm.health.biocomp.smarts.*;
 
/**	Note that bits are numbered from 1, and within each int, 
	from least significant bit.  Java ints are currently 32 bits by decree.

	to do:
		[x] allow FP size to differ from internal storage size 
		[x] correct metrics if FP size differs from internal storage size 
		[ ] is throw Exception kosher?
		[ ] CustomFingerprint is broken but switch if/when it is fixed.

	@author Jeremy J Yang
*/
public class BinaryFP
{
  int[] bitarray;
  private int fpsize=0;	// storage size in bits (multiple of Integer.SIZE)
  private int length=0;	// formal bit-length
  public BinaryFP() { }
  public BinaryFP(int size)
  {
    setSize(size);
  }
  public void setSize(int size)
  {
    length=size;
    fpsize=(int)Math.ceil((float)size/Integer.SIZE)*Integer.SIZE;
    bitarray = new int[fpsize];
    clear();
  }
  public int size()
  {
    return length;
  }
  public void clear()
  {
    for (int i=0;i<bitarray.length;++i) bitarray[i]=0;
  }
  public boolean getBit(int i)
  {
    if (i>size()) { return false; }
    int ii = i / Integer.SIZE;	// which int?
    int j = i % Integer.SIZE;	// which bit?
    int mask = 1 << j;
    return ((bitarray[ii]&mask)!=0);
  }
  public void setBit(int i,boolean val)
  {
    if (i>size()) { return; }
    int ii = i / Integer.SIZE;	// which int?
    int j = i % Integer.SIZE;	// which bit?
    int mask = 1 << j;
    if (val)
      bitarray[ii] |= mask;
    else
      bitarray[ii] &= ~mask;
  }
  public void setBit(int i,int val)
  {
    setBit(i,(val!=0));
  }
  public float tanimoto(BinaryFP fp2)
  {
    return (1.0f - Metrics.binaryTanimoto(bitarray,fp2.bitarray));
  }
  public float tversky(BinaryFP fp2,float alpha,float beta)
  {
    return (1.0f - Metrics.binaryTversky(bitarray,alpha,fp2.bitarray,beta));
  }
  public int bitCount()
  {
    return Metrics.calcBitCount(bitarray);
  }
  /**	Returns string of 1 and 0 chars.
  */
  public String toBitString()
  {
    String str="";
    for (int i=0;i<size();++i)
    {
      str+=(getBit(i)?"1":"0");
    }
    return str;
  }
  /**	Assigns bit vector from string of 1 and 0 chars.
  */
  public boolean fromBitString(String str)
  {
    if (str.length()>fpsize)
      setSize(str.length());
    for (int i=0;i<str.length();++i)
    {
      setBit(i,str.charAt(i)=='1');
    }
    return true;
  }
  public boolean isSubsetOf(BinaryFP fp2)
  {
    for (int i=0;i<size();++i)
    {
      if (getBit(i) && !fp2.getBit(i)) return false;
    }
    return true;
  }
  public int getCommonBitCount(BinaryFP fp2)
  {
    int n=0;
    for (int i=0;i<size();++i)
    {
      if (getBit(i) && fp2.getBit(i)) ++n;
    }
    return n;
  }
  public boolean generate(SmartsFile smartsFile,Molecule mol)
        throws SearchException,Exception
  {
    if (smartsFile.size()>size())
    {
      System.err.println("DEBUG: smartsFile.size()>size()"+
        smartsFile.size()+">"+size());
      return false;
    }
    for (int i=0;i<smartsFile.size();++i)
    {
      MolSearch search=smartsFile.getSearch(i);
      search.setTarget(mol);
      setBit(i+1,search.isMatching());
    }
    return true;
  }
}
