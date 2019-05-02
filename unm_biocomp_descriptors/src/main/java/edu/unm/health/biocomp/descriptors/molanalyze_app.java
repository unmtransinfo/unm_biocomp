package edu.unm.health.biocomp.descriptors;

import java.io.*;
import java.util.*;
import chemaxon.formats.*;
import chemaxon.struc.*;

import chemaxon.calculations.*; //ElementalAnalyser, TopologyAnalyser

/**	Generates many of the descriptors available from the ChemAxon
	ElementalAnalyser and TopologyAnalyser.
	@author Jeremy Yang
*/
public class molanalyze_app
{
  private static void Help(String msg)
  {
    if (!msg.equals("")) System.err.println(msg);
    System.err.println(
      "usage: molanalyze\n"+
      "  required:\n"+
      "          -i <in_mol_file>\n"+
      "  options:\n"+
      "          -o <out_mol_file> ... SDF includes data\n"+
      "          -ifmt <fmt_spec>\n"+
      "          -ofmt <fmt_spec>\n"+
      "          -table_out     ... TSV output\n"+
      "          -v     ... verbose\n"+
      "          -vv     ... very verbose\n"
    );
    System.exit(1);
  }

  public static void main(String[] args)
    throws IOException
  {
    if (args.length==0) Help("");
    String ifile="";
    String ofile=null;
    String ifmt="";
    String ofmt=null;
    boolean table_out=false;
    int verbose=0;
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) { ifile=args[++i]; }
      else if (args[i].equals("-o")) { ofile=args[++i]; }
      else if (args[i].equals("-ifmt")) { ifmt=args[++i]; }
      else if (args[i].equals("-ofmt")) { ofmt=args[++i]; }
      else if (args[i].equals("-table_out")) { table_out=true; }
      else if (args[i].equals("-v")) { verbose=1; }
      else if (args[i].equals("-vv")) { verbose=2; }
      else {
        Help("illegal option: "+args[i]);
      }
    }
    if (ifile.equals("")) Help("-i required");

    MolImporter molReader;
    if (ifile.equals("-"))
    {
      if (ifmt.equals("")) Help("-ifmt required with \"-i -\"");
      molReader=new MolImporter(System.in,ifmt);
    }
    else
    {
      molReader=new MolImporter(ifile);
    }

    MolExporter molWriter=null;
    if (table_out) ofmt="smiles:TMass:ExactMass:Formula:DotDisconnectedFormula:IsotopeFormula:TotalCharge:Composition:IsotopeComposition:AtomCount:HeavyAtomCount:BondCount:BalabanIndex:CyclomaticNumber:FragmentCount:FusedRingCount:HararyIndex:LargestRingSize:PlattIndex:RandicIndex:RingCount:RingSystemCount:RotatableBondCount:SzegedIndex:WienerIndex:HyperWienerIndex:WienerPolarity";
    if (ofile!=null)
    {
      if (ofile.equals("-"))
      {
        if (ofmt==null) Help("-ofmt required with \"-o -\"");
        molWriter=new MolExporter(System.out,ofmt);
      }
      else
      {
        if (ofmt==null)
          ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
        molWriter=new MolExporter(new FileOutputStream(ofile),ofmt);
      }
    }

    ElementalAnalyser elemanal = new ElementalAnalyser();

    double mass;
    double exactMass; // wt of MS ion using most frequent natural isotopes
    int massPrecision=2;
    String formula;
    String dotDisconnectedFormula;
    String isotopeFormula;
    int totalCharge;
    String composition;
    String isotopeComposition;

    TopologyAnalyser topoanal = new TopologyAnalyser();

    int atomCount;
    int heavyAtomCount;
    int bondCount;
    double balabanIndex;
    int cyclomaticNumber;
    int fragmentCount;
    int fusedRingCount;
    double hararyIndex;
    int largestRingSize;
    int plattIndex;
    double randicIndex;
    int ringCount;
    int ringSystemCount;
    int rotatableBondCount;
    double szegedIndex;
    int wienerIndex;
    int hyperWienerIndex;
    int wienerPolarity;

    Molecule mol;
    String smi;
    while ((mol=molReader.read())!=null)
    {
      smi=mol.exportToFormat("smiles");
      System.err.println("name: \""+mol.getName()+"\"");
      System.err.println("comment: \""+mol.getComment()+"\"");
      System.err.println("smi: "+smi);

      if (verbose>1)
      {
        System.err.println("Properties from file:");
        for  (String key:mol.properties().getKeys())
        {
          System.err.println("\t"+key+":"+mol.getProperty(key));
        }
      }

      elemanal.setMolecule(mol);
      mass = elemanal.mass();
      exactMass = elemanal.exactMass();
      formula = elemanal.formula();
      dotDisconnectedFormula = elemanal.dotDisconnectedFormula();
      isotopeFormula = elemanal.isotopeFormula();
      totalCharge=mol.getTotalCharge();
      composition = elemanal.composition(massPrecision);
      isotopeComposition = elemanal.isotopeComposition(massPrecision);
     
      if (verbose>0)
      {
        System.err.println("Properties calculated:");
        System.err.println(String.format("\tMass : %.2f",mass));
        System.err.println(String.format("\tExactMass : %.2f",exactMass));
        System.err.println(String.format("\tMF : %s",formula));
        if (!formula.equals(dotDisconnectedFormula))
          System.err.println(String.format("\tdotDisconnectedFormula : %s",dotDisconnectedFormula));
        if (!formula.equals(isotopeFormula))
          System.err.println(String.format("\tIsotopeFormula : %s",isotopeFormula));
        System.err.println(String.format("\tElementalComposition : %s",composition));
        if (!composition.equals(isotopeComposition))
          System.err.println(String.format("\tIsotopeComposition : %s",isotopeComposition));
        System.err.println(String.format("\tTotalCharge : %d",totalCharge));
      }

      topoanal.setMolecule(mol);
          
      atomCount=topoanal.atomCount();
      heavyAtomCount=atomCount-elemanal.atomCount(1);
      bondCount=topoanal.bondCount();
      balabanIndex=topoanal.balabanIndex();
      cyclomaticNumber=topoanal.cyclomaticNumber();
      fragmentCount=topoanal.fragmentCount();
      fusedRingCount=topoanal.fusedRingCount();
      hararyIndex=topoanal.hararyIndex();
      largestRingSize=topoanal.largestRingSize();
      plattIndex=topoanal.plattIndex();
      randicIndex=topoanal.randicIndex();
      ringCount=topoanal.ringCount();
      ringSystemCount=topoanal.ringSystemCount();
      rotatableBondCount=topoanal.rotatableBondCount();
      szegedIndex=topoanal.szegedIndex();
      wienerIndex=topoanal.wienerIndex();
      hyperWienerIndex=topoanal.hyperWienerIndex();
      wienerPolarity=topoanal.wienerPolarity();

      if (verbose>0)
      {
        System.err.println(String.format("\tAtomCount : %d",atomCount));
        System.err.println(String.format("\tHeavyAtomCount : %d",heavyAtomCount));
        System.err.println(String.format("\tBondCount : %d",bondCount));
        System.err.println(String.format("\tBalabanIndex : %.2f",balabanIndex));
        System.err.println(String.format("\tCyclomaticNumber : %d",cyclomaticNumber));
        System.err.println(String.format("\tFragmentCount : %d",fragmentCount));
        System.err.println(String.format("\tFusedRingCount : %d",fusedRingCount));
        System.err.println(String.format("\tHararyIndex : %.2f",hararyIndex));
        System.err.println(String.format("\tLargestRingSize : %d",largestRingSize));
        System.err.println(String.format("\tPlattIndex : %d",plattIndex));
        System.err.println(String.format("\tRandicIndex : %.2f",randicIndex));
        System.err.println(String.format("\tRingCount : %d",ringCount));
        System.err.println(String.format("\tRingSystemCount : %d",ringSystemCount));
        System.err.println(String.format("\tRotatableBondCount : %d",rotatableBondCount));
        System.err.println(String.format("\tSzegedIndex : %.2f",szegedIndex));
        System.err.println(String.format("\tWienerIndex : %d",wienerIndex));
        System.err.println(String.format("\tHyperWienerIndex : %d",hyperWienerIndex));
        System.err.println(String.format("\tWienerPolarity : %d",wienerPolarity));
      }

      if (verbose>1)
      {
        System.err.println("\t\tatomIdx\tsymbol\tSetSeq");
        System.err.println("\t\t-------\t------\t------");
        for (int i=0;i<mol.getAtomCount();++i)
        {
          System.err.print(String.format("\t\t%d.",i));
          System.err.print(String.format("\t%s",mol.getAtom(i).getSymbol()));
          System.err.print(String.format("\t%d",mol.getAtom(i).getSetSeq()));
          System.err.println("");
        }
        System.err.println("\t\tbondIdx\ttype\tConj\tQuery");
        System.err.println("\t\t-------\t------\t------\t------");
        for (int i=0;i<mol.getBondCount();++i)
        {
          System.err.print(String.format("\t\t%d.",i));
          System.err.print(String.format("\t%d",mol.getBond(i).getType()));
          System.err.print(String.format("\t%s",mol.getBond(i).isConjugated()));
          System.err.print(String.format("\t%s",mol.getBond(i).isQuery()));
          System.err.println("");
        }
      }

      if (molWriter!=null)
      {
        molWriter.write(mol);
      }
    }
    System.exit(0);
  }
}

