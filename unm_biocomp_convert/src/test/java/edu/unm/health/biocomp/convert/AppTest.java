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

    /**
     * Rigourous Test
     */
    public void testApp()
    {
        String intxt = "NCCc1cc(O)c(O)cc1";
        String infmt = "smiles";
        String outfmt = "sdf";
	MolImporter molReader=new MolImporter(new ByteArrayInputStream(intxt.getBytes()), ifmt);
	Molecule mol = molReader.read();
	ByteArrayOutputStream obuff=new ByteArrayOutputStream();
	MolExporter molWriter = new MolExporter(obuff, ofmt);
	molWriter.write(mol);
        String otxt = new String(obuff.toByteArray(),"utf-8");
        assertTrue(otxt.length>0);
    }
}
