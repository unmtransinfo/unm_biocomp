package edu.unm.health.biocomp.convert;

import java.io.*;
import java.util.*;

import junit.framework.*; //Test, TestCase, TestSuite

import chemaxon.formats.*;
import chemaxon.marvin.io.MolExportException;
import chemaxon.struc.Molecule;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public AppTest( String testName )
  {
    super( testName );
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite()
  {
    return new TestSuite( AppTest.class );
  }

  public void testSmiles2Sdf()
  {
    String itxt = "NCCc1cc(O)c(O)cc1";
    String ifmt = "smiles";
    String ofmt = "sdf";
    MolImporter molReader = null;
    Molecule mol = null;
    System.err.println("INPUT: \""+itxt+"\"");
    try {
      molReader=new MolImporter(new ByteArrayInputStream(itxt.getBytes()), ifmt);
      mol = molReader.read();
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    ByteArrayOutputStream obuff = new ByteArrayOutputStream();
    MolExporter molWriter = null;
    String otxt = null;
    try {
      molWriter = new MolExporter(obuff, ofmt);
      molWriter.write(mol);
      otxt = new String(obuff.toByteArray(), "utf-8");
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    System.err.println("OUTPUT: \""+otxt+"\"");
    assertTrue(otxt.length()>0);
  }
  public void testInchi2Smiles()
  {
    String itxt = "InChI=1S/C4H5NO2/c1-2-3(5)4(6)7/h1,3H,5H2,(H,6,7)";
    String ifmt = "inchi";
    String ofmt = "smiles";
    MolImporter molReader = null;
    Molecule mol = null;
    System.err.println("INPUT: \""+itxt+"\"");
    try {
      molReader=new MolImporter(new ByteArrayInputStream(itxt.getBytes()), ifmt);
      mol = molReader.read();
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    ByteArrayOutputStream obuff = new ByteArrayOutputStream();
    MolExporter molWriter = null;
    String otxt = null;
    try {
      molWriter = new MolExporter(obuff, ofmt);
      molWriter.write(mol);
      otxt = new String(obuff.toByteArray(), "utf-8");
    } catch (Exception e) {
      System.err.println(e.toString());
    }
    System.err.println("OUTPUT: \""+otxt+"\"");
    assertTrue(otxt.length()>0);
  }
}
