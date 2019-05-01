package edu.unm.health.biocomp.react;

import java.io.*;
import java.text.*;
import java.util.*;

import chemaxon.formats.*; //MolExporter
import chemaxon.struc.*;
import chemaxon.util.MolHandler;
import chemaxon.reaction.*;
import chemaxon.util.iterator.*;

public class react_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Returns list of list of reactions (one list per input mol).
  */
  public static ArrayList<ArrayList<Molecule> > ReactMols(
	List<Molecule> mols,
	String smirks,
	Boolean recurse,
	Boolean verbose)
      throws IOException
  {
    Reactor reactor = new Reactor();
    try { reactor.setReactionString(smirks); }
    //catch (chemaxon.reaction.ReactionException e) {
    catch (Exception e) {
      System.err.println("DEBUG: smirks = \""+smirks+"\"");
      System.err.println("ERROR: "+e.getMessage());
      return null;
    }

    reactor.setIgnoreRules(Reactor.IGNORE_REACTIVITY|Reactor.IGNORE_TOLERANCE);
    reactor.setDuplicateFiltering(Reactor.ORDER_INSENSITIVE_DUPLICATE_FILTERING);
    reactor.setOutputReactionMappingStyle(Reactor.MAPPING_STYLE_MATCHING);

    return ReactMols(mols,reactor,recurse,verbose);
  }


  /////////////////////////////////////////////////////////////////////////////
  /**	Returns list of list of reactions (one list per input mol).
  */
  public static ArrayList<ArrayList<Molecule> > ReactMols(
	List<Molecule> mols,
	Reactor reactor,
	Boolean recurse,
	Boolean verbose)
      throws IOException
  {
    ArrayList<ArrayList<Molecule> > rxnmols = new ArrayList<ArrayList<Molecule> >();

    int n_mol=0;
    for (Molecule mol:mols)
    {
      ++n_mol;
      if (verbose) System.err.println("mol ["+n_mol+"]: "+MolExporter.exportToFormat(mol,"smiles"));

      // Products:
      ArrayList<Molecule> prodmols = ReactMol2Products(mol,reactor,recurse,verbose);

      // Reactions:
      ArrayList<Molecule> rxnmols_this = ReactMol2Reactions(mol,reactor,recurse,verbose);
      rxnmols.add(rxnmols_this);
    }
    return rxnmols;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Returns list of products, for one input mol, one input Reactor (from smirks).
  */
  public static ArrayList<Molecule> ReactMol2Products(
	Molecule mol,
	Reactor reactor,
	Boolean recurse,
	Boolean verbose)
      throws IOException
  {
    ArrayList<Molecule> prodmols = new ArrayList<Molecule>();

    ConcurrentReactorProcessor crepro = new ConcurrentReactorProcessor();
    crepro.setReactor(reactor);
    Molecule[] reactants=mol.cloneMolecule().convertToFrags();
    MoleculeIterator[] miters = new MoleculeIterator[reactants.length];
    for (int j=0;j<reactants.length;++j)
      miters[j]=MoleculeIteratorFactory.createMoleculeIterator(reactants);
    try { crepro.setReactantIterators(miters,ConcurrentReactorProcessor.MODE_COMBINATORIAL); }
    catch (ReactionException e) {
      System.err.println("ERROR: "+e.getMessage());
      return null;
    }

    reactor.setResultType(Reactor.PRODUCT_OUTPUT);
    int i_p_set=0;
    while (true)
    {
      Molecule[] products=null;
      try { products=crepro.react(); }
      catch (ReactionException e) {
        System.err.println("ERROR: "+e.getMessage());
        return null;
      }
      if (products==null)
      {
        //if (i_p_set==0 && verbose) System.err.println("DEBUG: No products.");
        break;
      }
      ++i_p_set;
      for (Molecule prod: products)
      {
        prodmols.add(prod);
        if (recurse)
        {
          ArrayList<Molecule> prodmols_nextgen = ReactMol2Products(prod,reactor,true,verbose);
          if (prodmols_nextgen!=null && prodmols_nextgen.size()>0)
            prodmols.addAll(prodmols_nextgen);	//deduplicate?
        }
      }
    }
    int i_p=0;
    for (Molecule prod: prodmols)
    {
      ++i_p;
      if (verbose) System.err.println("\tproduct ["+i_p+"]: "+MolExporter.exportToFormat(prod,"smiles"));
    }
    return prodmols;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Returns list of reactions, for one input mol, one input Reactor (from smirks).
  */
  public static ArrayList<Molecule> ReactMol2Reactions(
	Molecule mol,
	Reactor reactor,
	Boolean recurse,
	Boolean verbose)
      throws IOException
  {
    if (recurse)
    {
      return ReactMol2ReactionsRecurse(mol,reactor,verbose);
    }

    ArrayList<Molecule> rxnmols = new ArrayList<Molecule>();

    ConcurrentReactorProcessor crepro = new ConcurrentReactorProcessor();
    crepro.setReactor(reactor);
    Molecule[] reactants=mol.cloneMolecule().convertToFrags();
    MoleculeIterator[] miters = new MoleculeIterator[reactants.length];
    for (int j=0;j<reactants.length;++j)
      miters[j]=MoleculeIteratorFactory.createMoleculeIterator(reactants);
    try { crepro.setReactantIterators(miters,ConcurrentReactorProcessor.MODE_COMBINATORIAL); }
    catch (ReactionException e) {
      System.err.println("ERROR: "+e.getMessage());
      return null;
    }

    reactor.setResultType(Reactor.REACTION_OUTPUT);
    int i_r_set=0;
    while (true)
    {
      Molecule[] rxns=null;
      try { rxns=crepro.react(); }
      catch (ReactionException e) {
        System.err.println("ERROR: "+e.getMessage());
        return null;
      }
      if (rxns==null)
      {
        //if (i_r_set==0 && verbose) System.err.println("DEBUG: No reactions.");
        break;
      }
      ++i_r_set;
      int i_r=0;
      for (Molecule rxn: rxns)
      {
        ++i_r;
        rxnmols.add(rxn);
        String rxnsmi=MolExporter.exportToFormat(rxn,"smiles:u");
        if (verbose) System.err.println("\treaction ["+i_r+"]: "+rxnsmi);
      }
    }
    return rxnmols;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Returns list of products, for one input mol, one input Reactor (from smirks).
  */
  public static ArrayList<Molecule> ReactMol2ReactionsRecurse(
	Molecule mol,
	Reactor reactor,
	Boolean verbose)
      throws IOException
  {
    ArrayList<Molecule> rxnmols = new ArrayList<Molecule>();

    ConcurrentReactorProcessor crepro = new ConcurrentReactorProcessor();
    crepro.setReactor(reactor);
    Molecule[] reactants=mol.cloneMolecule().convertToFrags();
    MoleculeIterator[] miters = new MoleculeIterator[reactants.length];
    for (int j=0;j<reactants.length;++j)
      miters[j]=MoleculeIteratorFactory.createMoleculeIterator(reactants);
    try { crepro.setReactantIterators(miters,ConcurrentReactorProcessor.MODE_COMBINATORIAL); }
    catch (ReactionException e) {
      System.err.println("ERROR: "+e.getMessage());
      return null;
    }

    reactor.setResultType(Reactor.PRODUCT_OUTPUT);
    int i_p_set=0;
    int i_r=0;
    while (true)
    {
      Molecule[] products=null;
      try { products=crepro.react(); }
      catch (ReactionException e) {
        System.err.println("ERROR: "+e.getMessage());
        return null;
      }
      if (products==null)
      {
        //if (i_p_set==0 && verbose) System.err.println("DEBUG: No products.");
        break;
      }
      ++i_p_set;
      int i_p=0;
      RxnMolecule rxnmol = new RxnMolecule();
      rxnmol.addComponent(mol.cloneMolecule(),RxnMolecule.REACTANTS,true);
      for (Molecule prod: products) {
        ++i_p;
        rxnmol.addComponent(prod.cloneMolecule(),RxnMolecule.PRODUCTS,true);
        if (verbose) {
          System.err.println("\tproduct ["+i_p+"]: "+MolExporter.exportToFormat(prod,"smiles"));
        }
        ArrayList<Molecule> prods_nextgen = ReactMol2Products(prod,reactor,true,verbose);
        if (prods_nextgen!=null && prods_nextgen.size()>0)
        {
          for (Molecule prod_nextgen: prods_nextgen)
            rxnmol.addComponent(prod_nextgen.cloneMolecule(),RxnMolecule.PRODUCTS,true);
        }
      }
      rxnmols.add(rxnmol);
      ++i_r;
      String rxnsmi=MolExporter.exportToFormat(rxnmol,"smiles:u");
      if (verbose) System.err.println("\treaction ["+i_r+"]: "+rxnsmi);
    }
    return rxnmols;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"react_utils - reaction transform test program \n"
      +"\n"
      +"usage: react_utils [options]\n"
      +"  required:\n"
      +"    -i IFILE .................. input molecule[s]\n"
      +"    and\n"
      +"    -smirks SMIRKS ............ SMIRKS reaction transform\n"
      +"  options:\n"
      +"    -o OFILE .................. output reactions or products\n"
      +"    -recurse .................. re-apply reactions to products etc.\n"
      +"    -out_reactions ............ output reactions (default: products)\n"
      +"    -v ........................ verbose\n"
      +"    -h ........................ this help\n");
    System.exit(1);
  }
  private static int verbose=0;
  private static String ifile=null;
  private static String smirks=null;
  private static String smirksfile=null;
  private static Boolean recurse=false;
  private static Boolean out_reactions=false;
  private static String ofile=null;
  private static String smifmt="cxsmiles:u-L-l-e-d-D-p-R-f-w";

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-smirks")) smirks=args[++i];
      else if (args[i].equals("-smirksfile")) smirksfile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-recurse")) recurse=true;
      else if (args[i].equals("-out_reactions")) out_reactions=true;
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

    if (smirks==null) Help("-smirks required.");

    MolExporter molWriter=null;
    if (ofile!=null)
    {
      String ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
      if (ofmt.equals("smiles")) ofmt="smiles:+n-a"; //Kekule for compatibility
      molWriter=new MolExporter(new FileOutputStream(ofile),ofmt);
    }

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
      m.aromatize(MoleculeGraph.AROM_GENERAL); // aromatize so smirks work correctly.
      mols.add(m);
    }
    molReader.close();

    ArrayList<ArrayList<Molecule> > rxnmols = ReactMols(mols,smirks,recurse,(verbose>0));

  }
}
