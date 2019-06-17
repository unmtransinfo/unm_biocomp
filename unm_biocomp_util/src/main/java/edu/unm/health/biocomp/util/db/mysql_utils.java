package edu.unm.health.biocomp.util.db;

import java.io.*;
import java.util.*;
import java.sql.*;
import javax.sql.*; //DataSource
import javax.naming.*; //Context,InitialContext,NamingException

import com.mysql.jdbc.Driver;

/**	Static utility methods for MySQL databases.
	<br>
	@author Jeremy J Yang
*/
public class mysql_utils
{
  private static String dbhost="localhost";
  private static String dbname="badapple";
  private static String dbusr="bard";
  private static String dbpw="stratford";
  private static Integer dbport=3306;

  /////////////////////////////////////////////////////////////////////////////
  /**	Return text with server status information.
  */
  public static String ServerStatusTxt(Connection dbcon)
  {
    String txt="";
    try {
      //ResultSet rset=ExecuteSql(dbcon,"STATUS");
      ResultSet rset=ExecuteSql(dbcon,"SELECT VERSION()");
      if (rset.next())
        txt+=("MySQL server version: "+rset.getString(1));
      rset.getStatement().close();
    }
    catch (Exception e)
    { txt+=("error: "+e.getMessage()); }
    return txt;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement. Normally use this function.
  */
  public static ResultSet ExecuteSql(Connection dbcon,String sql)
      throws SQLException
  {
    Statement stmt=dbcon.createStatement();
    //Statement stmt=dbcon.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
    ResultSet rset=stmt.executeQuery(sql);
    return rset;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement. Use this function for non-queries,
        UPDATE, INSERT, DELETE, CREATE, REINDEX, etc.
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
  /**	Return MySQL connection.
  */
  public static Connection DBConnect(String dbhost,Integer dbport,String dbname,String dbusr,String dbpw)
    throws SQLException
  {
    DriverManager.registerDriver(new com.mysql.jdbc.Driver());
    Connection dbcon=DriverManager.getConnection("jdbc:mysql://"+dbhost+":"+dbport+"/"+dbname,dbusr,dbpw);
    return dbcon;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Return Connection, from J2EE Context and DataSource definition.
  */
  public static Connection DBConnectContext(String ctx_name,String ds_name)
	throws SQLException,NamingException
  {
    Context ctx_init = new InitialContext();
    Context ctx = (Context) ctx_init.lookup(ctx_name);
    DataSource ds = (DataSource) ctx.lookup(ds_name);
    Connection dbcon=ds.getConnection();
    return dbcon;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static void Describe(Connection dbcon,int verbose)
    throws SQLException
  {
    if (verbose>0)
    {
      DatabaseMetaData meta=dbcon.getMetaData();
      System.err.println("server: "+meta.getDatabaseProductName()+" "+meta.getDatabaseMajorVersion()+"."+meta.getDatabaseMinorVersion());
      System.err.println("driver (client): "+meta.getDriverName()+" "+meta.getDriverVersion());
    }

    for (String table: ListTables(dbcon))
    {
      System.err.print(table+"\n\t");
      List<String> cols = ListColumns(dbcon,table);
      for (int j=0;j<cols.size();++j)
        System.err.print((j>0?",":"")+cols.get(j));
      System.err.println("\n\trowcount: "+Rowcount(dbcon,table));
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  public static List<String> ListTables(Connection dbcon)
    throws SQLException
  {
    ArrayList<String> tables = new ArrayList<String>();
    ResultSet rset=ExecuteSql(dbcon,"SHOW TABLES");
    while (rset.next()) tables.add(rset.getString(1));
    rset.getStatement().close();
    Collections.sort(tables);
    return tables;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static List<String> ListColumns(Connection dbcon,String table)
    throws SQLException
  {
    ArrayList<String> cols = new ArrayList<String>();
    ResultSet rset=ExecuteSql(dbcon,"DESCRIBE "+table);
    while (rset.next()) cols.add(rset.getString(1));
    rset.getStatement().close();
    return cols;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Integer Rowcount(Connection dbcon,String table)
    throws SQLException
  {
    Integer n=null;
    ResultSet rset=ExecuteSql(dbcon,"SELECT count(*) FROM "+table);
    if (rset.next()) n = rset.getInt(1);
    rset.getStatement().close();
    return n;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static int verbose=0;
  private static Boolean describe=false;
  private static Boolean bard=false;
  private static String ctx_name="java:comp/env";
  private static String ds_name="jdbc/bard";

  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"mysql_utils - mysql utilities\n"
      +"usage: mysql_utils [options]\n"
      +"  operation:\n"
      +"    -describe ................... describe database\n"
      +"  parameters:\n"
      +"    -dbhost DBHOST .............. ["+dbhost+"]\n"
      +"    -dbport DBPORT .............. ["+dbport+"]\n"
      +"    -dbname DBNAME .............. ["+dbname+"]\n"
      +"    -dbusr DBUSR ................ ["+dbusr+"]\n"
      +"    -dbpw DBPW .................. [*********]\n"
      +"    -context_name CTX ........... ["+ctx_name+"]\n"
      +"    -datasource_name DS ......... ["+ds_name+"]\n"
      +"  options:\n"
      +"    -v[v] ....................... verbose [very]\n"
      +"    -h .......................... this help\n");
    System.exit(1);
  }
  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    if (args.length==0) Help("");
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-dbhost")) dbhost=args[++i];
      else if (args[i].equals("-dbname")) dbname=args[++i];
      else if (args[i].equals("-dbusr")) dbusr=args[++i];
      else if (args[i].equals("-dbpw")) dbpw=args[++i];
      else if (args[i].equals("-dbport")) dbport=Integer.parseInt(args[++i]);
      else if (args[i].equals("-describe")) describe=true;
      else if (args[i].equals("-bard")) bard=true;
      else if (args[i].equals("-context_name")) ctx_name=args[++i];
      else if (args[i].equals("-datasource_name")) ds_name=args[++i];
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-vv")) verbose=2;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Simple test connection.
  */
  public static void main(String[] args)
	throws IOException,SQLException
  {
    ParseCommand(args);

    Connection dbcon=null;

    if (bard)
    {
      try { dbcon=DBConnectContext(ctx_name,ds_name); }
      catch (Exception e) { Help("MySQL BARD connection failed:"+e.getMessage()); }
    }

    try { dbcon=DBConnect(dbhost,dbport,dbname,dbusr,dbpw); }
    catch (SQLException e) { Help("MySQL connection failed:"+e.getMessage()); }

    if (dbcon!=null)
      System.err.println("MySQL connection ok: jdbc:mysql://"+dbhost+":/"+dbname);

    if (describe)
    {
      Describe(dbcon,verbose);
    }
    else
    {
      Help("ERROR: no operation specified.");
    }
  }
}
