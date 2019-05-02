package edu.unm.health.biocomp.jchemdb;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import java.sql.*; //DriverManager,Driver,SQLException,Connection,Statement,ResultSet

import com.chemaxon.version.VersionInfo;

import chemaxon.util.ConnectionHandler;
import chemaxon.jchem.db.*; //JChemSearch,Updater,UpdateHandler,DatabaseProperties
import chemaxon.sss.search.*; //JChemSearchOptions
import chemaxon.struc.*; //Molecule
import chemaxon.sss.*; //SearchConstants
import chemaxon.util.*; //HitColoringAndAlignmentOptions
import chemaxon.formats.*; //MolImporter,MolFormatException

import edu.unm.health.biocomp.util.*;

/**	Utilities for JChem DB queries and admin.
	Currently expects embedded Derby Db.
	Currently requires write-access (can this be fixed?).

	@author Jeremy J Yang
*/
public class jchemdb_utils
{
  private static int SEARCH_MAXTIME=60000; //ms

  /////////////////////////////////////////////////////////////////////////////
  public static ConnectionHandler GetConnectionHandler(String dbdir,String dbname,String tprop)
	throws SQLException,ClassNotFoundException
  {
    ConnectionHandler chand = new ConnectionHandler();
    chand.setDriver("org.apache.derby.jdbc.EmbeddedDriver");
    chand.setUrl("jdbc:derby:"+dbdir+"/"+dbname);
    chand.setPropertyTable(tprop);
    chand.connectToDatabase();
    return chand;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static JChemSearch GetJChemSearch(ConnectionHandler chand,String schema,String stab)
  {
    JChemSearch searcher = new JChemSearch();
    searcher.setConnectionHandler(chand);
    searcher.setStructureTable(schema+"."+stab);
    searcher.setRunMode(JChemSearch.RUN_MODE_SYNCH_COMPLETE);
    return searcher;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Molecule[] SimilaritySearch(ConnectionHandler chand,JChemSearch searcher,
	String query_str, //Smiles or other fmt
	Float thresh, int limit,
	ArrayList<String> fieldNames,ArrayList<Object[]> fieldVals)
	throws Exception
  {
    JChemSearchOptions searchOpts = new JChemSearchOptions(JChemSearch.SIMILARITY);
    searchOpts.setDissimilarityMetric("TANIMOTO");
    //searchOpts.setDissimilarityMetric("TVERSKY");
    //searchOpts.setDissimilarityMetricParameters("0.3,0.7");
    searchOpts.setDissimilarityThreshold(thresh);
    return StructureSearch(chand,searcher,query_str,limit,searchOpts,fieldNames,fieldVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Molecule[] SubstructureSearch(ConnectionHandler chand,JChemSearch searcher,
	String query_str, //Smiles or other fmt
	int limit,
	ArrayList<String> fieldNames,ArrayList<Object[]> fieldVals)
	throws Exception
  {
    JChemSearchOptions searchOpts = new JChemSearchOptions(JChemSearch.SUBSTRUCTURE);
    searchOpts.setHitOrdering(SearchConstants.HIT_ORDERING_NONE);
    return StructureSearch(chand,searcher,query_str,limit,searchOpts,fieldNames,fieldVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Molecule[] FullstructureSearch(ConnectionHandler chand,JChemSearch searcher,
	String query_str, //Smiles or other fmt
	int limit,
	ArrayList<String> fieldNames,ArrayList<Object[]> fieldVals)
	throws Exception
  {
    JChemSearchOptions searchOpts = new JChemSearchOptions(JChemSearch.FULL); //Less strict than JChemSearch.DUPLICATE.
    searchOpts.setHitOrdering(SearchConstants.HIT_ORDERING_NONE);
    return StructureSearch(chand,searcher,query_str,limit,searchOpts,fieldNames,fieldVals);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Why does this not work with read-only db?
	"An SQL data change is not permitted for a read-only connection, user or database."
  */
  public static Molecule[] StructureSearch(ConnectionHandler chand,JChemSearch searcher,
	String query_str, //Smiles or other fmt
	int limit,
	JChemSearchOptions searchOpts,
	ArrayList<String> fieldNames,ArrayList<Object[]> fieldVals)
	throws Exception
  {
    searchOpts.setMaxResultCount(limit);
    //searchOpts.setMaxTime(SEARCH_MAXTIME); //ms //Deprecated?
    searcher.setQueryStructure(query_str);
    searcher.setSearchOptions(searchOpts);
    searcher.run();
    int[] hit_ids = searcher.getResults();
    Molecule[] hit_mols = new Molecule[0];
    //System.err.println("DEBUG: StructureSearch(): hit count: "+hit_ids.length);
    if (hit_ids.length>0)
    {
      HitColoringAndAlignmentOptions hit_opts = null;
      hit_mols = searcher.getHitsAsMolecules(hit_ids,hit_opts,fieldNames,fieldVals);
    }
    return hit_mols;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String dbname="db";
  private static String dbschema="APP";
  private static String dbdir="/home/data/drugdb/.config/localdb";
  private static String ofile="";
  private static String dbtable="";
  private static String stable="STRUCTURES";
  private static String ptable="JCHEMPROPERTIES";
  private static int verbose=0;
  private static Boolean describe=false;
  private static Boolean create=false;
  private static Boolean list_tables=false;
  private static Boolean test_sim=false;
  private static Boolean test_sub=false;
  private static Boolean test_full=false;
  private static Boolean update=false;
  private static Boolean version=false;
  private static Boolean fix_version=false;
  private static Boolean show_properties=false;
  private static String test_query="NCCc1ccc(O)c(O)c1";

  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.out.println(msg+"\n"
      +"jchemdb_utils - jchemdb utilities (JChemBase)\n"
      +"(For generic utilities use derby_utils.sh.)\n"
      +"usage: jchemdb_utils [options]\n"
      +"\n"
      +"operations (one of):\n"
      +"  -describe .............. describe db\n"
      +"  -update ................ update structure table\n"
      +"  -test_sim .............. test structure similarity search\n"
      +"  -test_sub .............. test sub-structure search\n"
      +"  -test_full ............. test full-structure search\n"
      +"  -show_properties ....... show structures table properties\n"
      +"  -version ............... show versions\n"
      +"  -fix_version ........... fix version to match client (hack)\n"
      +"\n"
      +"required:\n"
      +"  -dbname DBNAME ......... db name ["+dbname+"]\n"
      +"\n"
      +"options:\n"
      +"  -dbdir DBDIR ........... directory of db ["+dbdir+"]\n"
      +"  -dbschema DBSCHEMA ..... db schema ["+dbschema+"]\n"
      +"  -dbtable TNAME ......... db table\n"
      +"  -ptable TNAME .......... JChem properties table ["+ptable+"]\n"
      +"  -stable TNAME .......... JChem structures table ["+stable+"]\n"
      +"  -query QUERY ........... structure search test query ["+test_query+"]\n"
      +"  -o OFILE ............... output file\n"
      +"  -v[v] .................. verbose [very]\n"
      +"  -h ..................... this help\n");
    System.exit(1);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    if (args.length==0) Help("");
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-dbname")) dbname=args[++i];
      else if (args[i].equals("-dbdir")) dbdir=args[++i];
      else if (args[i].equals("-dbschema")) dbschema=args[++i];
      else if (args[i].equals("-dbtable")) dbtable=args[++i];
      else if (args[i].equals("-ptable")) ptable=args[++i];
      else if (args[i].equals("-stable")) stable=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-describe")) describe=true;
      else if (args[i].equals("-test_sim")) test_sim=true;
      else if (args[i].equals("-test_sub")) test_sub=true;
      else if (args[i].equals("-test_full")) test_full=true;
      else if (args[i].equals("-update")) update=true;
      else if (args[i].equals("-version")) version=true;
      else if (args[i].equals("-fix_version")) fix_version=true;
      else if (args[i].equals("-show_properties")) show_properties=true;
      else if (args[i].equals("-query")) test_query=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args)
        throws IOException,SQLException
  {
    ParseCommand(args);

    ArrayList<String> fieldNames = new ArrayList<String>(Arrays.asList("NAME","CAS_REG_NO","ID","CD_ID"));
    ArrayList<Object[]> fieldVals = new ArrayList<Object[]>(fieldNames.size());

    ConnectionHandler chand = null;
    try { chand = GetConnectionHandler(dbdir,dbname,ptable); }
    catch (Exception e) { Help("Connection failed: "+e.getMessage()); }

    System.err.println("===");

    if (chand!=null) System.err.println("Connection ok: "+dbdir+"/"+dbname);

    JChemSearch searcher = GetJChemSearch(chand,dbschema,stable);

    if (describe)
    {
      System.err.println("PropertyTable: "+chand.getPropertyTable());
      System.err.println("StructureTable: "+searcher.getStructureTable());
    }
    else if (show_properties)
    {
      DatabaseProperties dbprops = new DatabaseProperties(chand,false);
      Map<String,String> pmap = dbprops.getTableProperties(stable);
      ArrayList<String> keys = new ArrayList<String>(pmap.keySet());
      Collections.sort(keys);
      for (String key: keys)
        System.err.println(String.format("%-44s: %s",stable+"."+key,pmap.get(key)));
    }
    else if (version)
    {
      DatabaseProperties dbprops = new DatabaseProperties(chand,false);
      String db_jcver = dbprops.getTableProperty(stable,"JChemVersion");
      System.err.println("Client JChem version: "+VersionInfo.getVersion()+" ; DB JChem version: "+db_jcver+" ("+(db_jcver.equals(VersionInfo.getVersion())?"EQUAL":"NOT EQUAL")+")");
    }
    else if (fix_version)
    {
      DatabaseProperties dbprops = new DatabaseProperties(chand,false);
      String db_jcver = dbprops.getTableProperty(stable,"JChemVersion");
      System.err.println("Client JChem version: "+VersionInfo.getVersion()+" ; DB JChem version: "+db_jcver+" ("+(db_jcver.equals(VersionInfo.getVersion())?"EQUAL":"NOT EQUAL")+")");
      if (db_jcver.equals(VersionInfo.getVersion()))
      {
        System.err.println("Versions equal; no fix needed.");
      }
      else
      {
        dbprops.setTableProperty(stable,"JChemVersion",VersionInfo.getVersion());
        db_jcver = dbprops.getTableProperty(stable,"JChemVersion");
        System.err.println("Client JChem version: "+VersionInfo.getVersion()+" ; DB JChem version: "+db_jcver+" ("+(db_jcver.equals(VersionInfo.getVersion())?"EQUAL":"NOT EQUAL")+")");
      }
    }
    else if (test_sim)
    {
      System.err.println("Test similarity search...");
      try {
        Molecule[] hit_mols = SimilaritySearch(chand,searcher,test_query,0.6F,10,fieldNames,fieldVals);
        for (int i_hit=0; i_hit<hit_mols.length; ++i_hit)
        {
          System.err.println("  "
		+String.format("%3d",(i_hit+1))+"."
		+String.format(" [%.4f]",(1.0F-searcher.getDissimilarity(i_hit)))
                +String.format(" %5d",((Integer)(fieldVals.get(i_hit)[2])))
                +" "+MolExporter.exportToFormat(hit_mols[i_hit],"cxsmiles:a")
                +String.format("\t%s:%s",fieldNames.get(0),(String)(fieldVals.get(i_hit)[0]))
                +String.format("\t%s:%s",fieldNames.get(1),(String)(fieldVals.get(i_hit)[1]))
                );
        }
      }
      catch (Exception e) { System.err.println("ERROR: "+e.toString()); }
    }
    else if (test_sub)
    {
      System.err.println("Test substructure search...");
      try {
        Molecule[] hit_mols = SubstructureSearch(chand,searcher,test_query,10,fieldNames,fieldVals);
        for (int i_hit=0; i_hit<hit_mols.length; ++i_hit)
        {
          System.err.println("  "
		+String.format("%3d",(i_hit+1))+"."
                +String.format(" %5d",((Integer)(fieldVals.get(i_hit)[2])))
                +" "+MolExporter.exportToFormat(hit_mols[i_hit],"cxsmiles:a")
                +String.format("\t%s:%s",fieldNames.get(0),(String)(fieldVals.get(i_hit)[0]))
                +String.format("\t%s:%s",fieldNames.get(1),(String)(fieldVals.get(i_hit)[1]))
                );
        }
      }
      catch (Exception e) { System.err.println("ERROR: "+e.toString()); }
    }
    else if (test_full)
    {
      System.err.println("Test fullstructure search...");
      try {
        Molecule[] hit_mols = FullstructureSearch(chand,searcher,test_query,10,fieldNames,fieldVals);
        for (int i_hit=0; i_hit<hit_mols.length; ++i_hit)
        {
          System.err.println("  "
		+String.format("%3d",(i_hit+1))+"."
                +String.format(" %5d",((Integer)(fieldVals.get(i_hit)[2])))
                +" "+MolExporter.exportToFormat(hit_mols[i_hit],"cxsmiles:a")
                +String.format("\t%s:%s",fieldNames.get(0),(String)(fieldVals.get(i_hit)[0]))
                +String.format("\t%s:%s",fieldNames.get(1),(String)(fieldVals.get(i_hit)[1]))
                );
        }
      }
      catch (Exception e) { System.err.println("ERROR: "+e.toString()); }
    }
    else if (update)
    {
      Updater upd = new Updater(chand);
      Updater.UpdateInfo updi = null;
      while ((updi = upd.getNextUpdateInfo()) != null)
      {
        System.err.println("\n" + updi.processingMessage + "\n");
        String message = upd.performCurrentUpdate();
        System.err.println(message);

        if (updi.isOperationRequired) break; // stopping, since further updates may depend on this one
      }
    }
    else
    {
      Help("ERROR: no operation specified.");
    }
    if (chand!=null) chand.close();
  }
}
