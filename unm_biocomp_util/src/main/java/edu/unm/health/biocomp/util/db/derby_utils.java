package edu.unm.health.biocomp.util.db;

import java.io.*;
import java.util.*;
import java.sql.*; //DriverManager,Driver,SQLException,Connection,Statement,ResultSet
import javax.sql.*;
import javax.naming.*;

import org.apache.derby.jdbc.EmbeddedDriver;

/**	Static utility methods for Derby databases.
	Only embedded/local databases (no client-server).
	<br>
	Uses Derby JDBC driver (org.apache.derby.jdbc.EmbeddedDriver);

	Properties:
 	derby.stream.error.file ["derby.log"] (.file takes precendence over .field)
 	derby.stream.error.field [System.err] 
	<br>
	@author Jeremy J Yang
*/
public class derby_utils
{

  /////////////////////////////////////////////////////////////////////////////
  /**	Return Derby connection (embedded-driver), creating new database
	if create==true.
	Why so verbose?
  */
  public static Connection DBConnectEmbedded(String dbname,String dbdir,Boolean create)
    throws SQLException
  {
    DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
    Connection dbcon=DriverManager.getConnection("jdbc:derby:"+dbdir+"/"+dbname+(create?";create=true":""));
    return dbcon;
  }
  public static Connection DBConnectEmbedded(String dbname,String dbdir)
    throws SQLException
  {
    return DBConnectEmbedded(dbname,dbdir,false);
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return text with server status information.
  */
  public static String ServerStatusTxt(Connection dbcon)
    throws SQLException
  {
    DatabaseMetaData meta=dbcon.getMetaData();
    String txt="";
    txt+=("server: "+meta.getDatabaseProductName()+" "+meta.getDatabaseMajorVersion()+"."+meta.getDatabaseMinorVersion()+"\n");
    txt+=("driver: "+meta.getDriverName()+" "+meta.getDriverVersion()+"\n");
    txt+=("JDBC: "+meta.getJDBCMajorVersion()+"."+meta.getJDBCMinorVersion()+"\n");
    return txt;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement. Normally use this function.
  */
  public static ResultSet ExecuteSql(Connection dbcon,String sql)
      throws SQLException
  {
    //Statement stmt=dbcon.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
    Statement stmt=dbcon.createStatement();
    ResultSet rset=stmt.executeQuery(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement. Use this function for non-queries,
        UPDATE, INSERT, DELETE, DROP, CREATE, REINDEX, etc.
        Note that autocommit is normally true so there is no
        need to call commit() directly.
  */
  public static boolean Execute(Connection dbcon,String sql)
      throws SQLException
  {
    Statement stmt=dbcon.createStatement();
    boolean ok=stmt.execute(sql);
    stmt.close();
    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static ArrayList<String> GetSchemaList(Connection dbcon)
      throws SQLException
  {
    ArrayList<String> schemas = new ArrayList<String>();
    DatabaseMetaData meta=dbcon.getMetaData();
    ResultSet rset=meta.getSchemas();
    while (rset.next()) schemas.add(rset.getString(1));
    rset.getStatement().close();
    Collections.sort(schemas);
    return schemas;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static ArrayList<String> GetTableList(Connection dbcon)
      throws SQLException
  {
    ArrayList<String> tables = new ArrayList<String>();
    ResultSet rset=ExecuteSql(dbcon,"SELECT s.schemaname||'.'||t.tablename FROM sys.systables t,sys.sysschemas s WHERE t.schemaid=s.schemaid AND t.tabletype='T' ORDER by s.schemaname,t.tablename");
    while (rset.next()) tables.add(rset.getString(1));
    rset.getStatement().close();
    Collections.sort(tables);
    return tables;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static ArrayList<String> GetTableList(Connection dbcon,String dbschema)
      throws SQLException
  {
    ArrayList<String> tables = new ArrayList<String>();
    ResultSet rset=ExecuteSql(dbcon,"SELECT t.tablename FROM sys.systables t,sys.sysschemas s WHERE t.schemaid=s.schemaid AND t.tabletype='T' AND s.schemaname='"+dbschema+"' ORDER by t.tablename");
    while (rset.next()) tables.add(rset.getString(1));
    rset.getStatement().close();
    Collections.sort(tables);
    return tables;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Boolean DropAllTables(Connection dbcon,int verbose)
      throws SQLException
  {
    List<String> tables=GetTableList(dbcon);
    boolean ok=true;
    for (String table: tables)
    {
      boolean ok_this=Execute(dbcon,"DROP TABLE "+table);	//Returns false incorrectly.  Why?
      if (verbose>0)
      {
        //System.out.println("DROP TABLE "+table+"\t"+(ok_this?"[SUCCESS]":"[FAILURE]"));
        System.out.println("DROP TABLE "+table+"...");
      }
      //ok&=ok_this;
    }
    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void DescribeSchema(Connection dbcon,String dbschema,int verbose)
      throws SQLException
  {
    List<String> dbtables=GetTableList(dbcon,dbschema);
    System.out.println("\n"+dbname+":");
    for (String dbtable: dbtables)
    {
      DescribeTable(dbcon,dbschema,dbtable,verbose);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void DescribeTable(Connection dbcon,String dbschema,String dbtable,int verbose)
      throws SQLException
  {
    System.out.println(dbschema+"."+dbtable+":");
    ResultSet rset=ExecuteSql(dbcon,"SELECT count(*) FROM "+dbtable);
    rset.next();
    System.out.println("\trowcount: "+rset.getString(1));
    rset.getStatement().close();

    String sql=("SELECT * FROM "+dbschema+"."+dbtable);
    PreparedStatement pstmt = dbcon.prepareStatement(sql);
    ResultSetMetaData rsmd = pstmt.getMetaData();
    int numColumns = rsmd.getColumnCount();
    int i=0;
    while (++i <= numColumns) {
      System.out.println("\t"+i+". "+rsmd.getColumnName(i)+" ("+rsmd.getColumnTypeName(i)+")");
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static List<String> GetColumnList(Connection dbcon,String dbschema,String dbtable,int verbose)
      throws SQLException
  {
    String sql=("SELECT * FROM "+dbschema+"."+dbtable);
    PreparedStatement pstmt = dbcon.prepareStatement(sql);
    ResultSetMetaData rsmd = pstmt.getMetaData();
    int numColumns = rsmd.getColumnCount();
    ArrayList<String> dbcols = new ArrayList<String>();
    for (int i=1; i <= numColumns; ++i) {
      dbcols.add(rsmd.getColumnName(i));
    }
    return dbcols;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Boolean ImportTableFromCSV(Connection dbcon,String dbschema,String dbtable,String fpath,
	String delim,String qchar,String charset,boolean replace,int verbose)
      throws SQLException
  {
    String sql=("CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE (");
    sql+=("'"+dbschema+"',");
    sql+=("'"+dbtable+"',");
    sql+=("'"+fpath+"',");
    sql+=("'"+delim+"',");
    sql+=("'"+qchar+"',");
    sql+=("'"+charset+"',");
    sql+=(""+(replace?1:0)+")");
    boolean ok=Execute(dbcon,sql);
    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Boolean ExportTableToCSV(Connection dbcon,String dbschema,
	String dbtable,
	String fpath,String delim,String qchar,String charset,int verbose)
      throws SQLException,IOException,FileNotFoundException
  {
    String sql=("CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (");
    sql+=("'"+dbschema+"',");
    sql+=("'"+dbtable+"',");
    sql+=("'"+fpath+"',");
    sql+=((delim==null)?("null,"):("'"+delim+"',"));
    sql+=((qchar==null)?("null,"):("'"+qchar+"',"));
    sql+=((charset==null)?("null)"):("'"+charset+"')"));
    //System.err.println("DEBUG: sql=\""+sql+"\"");
    boolean ok=Execute(dbcon,sql);
    List<String> colnames =  GetColumnList(dbcon,dbschema,dbtable,verbose);
    if (verbose>0)
    {
      System.err.println("table \""+dbtable+"\" exported to \""+ofile+"\"; columns: ");
      for (String colname: colnames) System.err.println("\t"+colname);
    }

    //Columns to header line.  Why so hard?
    File ftmp=File.createTempFile("derby_utils","_tmp.csv");
    PrintWriter ftmp_writer=new PrintWriter(new BufferedWriter(new FileWriter(ftmp,false)));
    int i=0;
    for (String colname: colnames) 
      ftmp_writer.write(((i++>0)?",":"")+"\""+colname+"\"");
    ftmp_writer.write("\n");

    File fout = new File(fpath);
    BufferedReader bur=new BufferedReader(new FileReader(fout));
    String line;
    while ((line=bur.readLine())!=null)
      ftmp_writer.write(line+"\n");

    bur=new BufferedReader(new FileReader(ftmp));
    PrintWriter fout_writer=new PrintWriter(new BufferedWriter(new FileWriter(fout,false))); //overwrite
    while ((line=bur.readLine())!=null)
      fout_writer.write(line+"\n");
    ftmp.delete();

    return ok;
  }
  /////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////
  private static String dbname="scratchy";
  private static String dbschema="APP";
  private static String dbdir=".";
  private static String ofile="";
  private static Boolean export_table=false;
  private static String dbtable="";
  private static int verbose=0;
  private static Boolean describe=false;
  private static Boolean create=false;
  private static Boolean list_tables=false;
  private static Boolean drop_all_tables=false;

  /////////////////////////////////////////////////////////////////////////////
  private static void Help(String msg)
  {
    System.out.println(msg+"\n"
      +"derby_utils - derby utilities\n"
      +"usage: derby_utils [options]\n"
      +"  required:\n"
      +"    -dbname DBNAME ......... db name ["+dbname+"]\n"
      +"  mode (one of):\n"
      +"    -create ................ create db\n"
      +"    -describe .............. describe (schema or table)\n"
      +"    -export_table .......... export table (CSV)\n"
      +"    -list_tables ........... list tables\n"
      +"    -drop_all_tables ....... drop all tables\n"
      +"  options:\n"
      +"    -dbdir DBDIR ........... directory of db ["+dbdir+"]\n"
      +"    -dbschema DBSCHEMA ..... db schema ["+dbschema+"]\n"
      +"    -dbtable TNAME ......... db table\n"
      +"    -o OFILE ............... output file\n"
      +"    -v ..................... verbose\n"
      +"    -vv .................... very verbose\n"
      +"    -h ..................... this help\n");
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
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-describe")) describe=true;
      else if (args[i].equals("-create")) create=true;
      else if (args[i].equals("-export_table")) export_table=true;
      else if (args[i].equals("-drop_all_tables")) drop_all_tables=true;
      else if (args[i].equals("-list_tables")) list_tables=true;
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

    Connection dbcon=null;
    try { dbcon=DBConnectEmbedded(dbname,dbdir,create); }
    catch (SQLException e) { Help("Derby connection failed: "+e.getMessage()); }
    if (verbose>0 && dbcon!=null)
    {
      System.err.println("Derby connection ok: jdbc:derby:"+dbdir+"/"+dbname);
      if (verbose>1)
        System.err.println(ServerStatusTxt(dbcon));
    }

    if (describe)
    {
      if (!dbtable.isEmpty())
        DescribeTable(dbcon,dbschema,dbtable,verbose);
      else
        DescribeSchema(dbcon,dbschema,verbose);
    }
    else if (create)
    {
      System.err.println("Attempted Derby db creation: "+((dbcon!=null)?"[SUCCESS]":"[FAILURE]")+" (jdbc:derby:"+dbdir+"/"+dbname+")");
    }
    else if (export_table)
    {
      if (ofile.isEmpty()) { Help("ERROR: -export_table requires -o OFILE"); }
      if (dbtable.isEmpty()) { Help("ERROR: -export_table requires -dbtable TNAME"); }
      boolean ok = ExportTableToCSV(dbcon,dbschema,dbtable,ofile,null,null,null,verbose);
    }
    else if (list_tables)
    {
      ArrayList<String> dbtables = GetTableList(dbcon,dbschema);
      for (String dbtable: dbtables)
        System.out.println("\t"+dbschema+"."+dbtable);
    }
    else if (drop_all_tables)
    {
      boolean ok=DropAllTables(dbcon,verbose);
      System.err.println("Attempted Derby db drop_all_tables: "+(ok?"[SUCCESS]":"[FAILURE]")+" (jdbc:derby:"+dbdir+"/"+dbname+")");
    }
    else
    {
      Help("ERROR: no operation specified.");
    }
    if (dbcon!=null) dbcon.close();
  }
}
