package edu.unm.health.biocomp.util.db;

import java.sql.*;

import net.sourceforge.jtds.jdbc.Driver;

/**	Static utility methods for MS SQL Server databases.
	<br>
	Not working yet.  How do we implement the "Windows" authentication method?
	<br>
	@author Jeremy J Yang
*/
public class jtds_utils
{
  /////////////////////////////////////////////////////////////////////////////
  /**	Return text with server status information.
  */
  public static String ServerStatusTxt(Connection dbcon)
  {
    int n=0;
    String txt="";
    try {
      txt+=("catalog: "+dbcon.getCatalog()+" ;");
      ResultSet rset=ExecuteSql(dbcon,"SELECT table_schema,table_name FROM information_schema.tables");
      while (rset.next())
      {
        ++n;
        //txt+=(rset.getString(1)+":"+rset.getString(2)+"\n");
      }
      rset.getStatement().close();
    }
    catch (Exception e)
    {
      txt+=("error: "+e.getMessage());
    }
    txt+=(" tables: "+n+"\n");
    return txt;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**   Executes SQL statement. Normally use this function.
  */
  public static ResultSet ExecuteSql(Connection dbcon,String sql)
      throws SQLException
  {
    Statement stmt=dbcon.createStatement();
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
  /**	Return connection.
  */
  public static Connection DBConnect(String dbhost,Integer dbport,String dbid,String dbdomain,String dbusr,String dbpw)
    throws SQLException
  {
    DriverManager.registerDriver(new net.sourceforge.jtds.jdbc.Driver());
    //Connection dbcon=DriverManager.getConnection("jdbc:jtds:sqlserver://"+dbhost+":"+dbport+";databaseName="+dbid,dbusr,dbpw);
    Connection dbcon=DriverManager.getConnection("jdbc:jtds:sqlserver://"+dbhost+":"+dbport+";DatabaseName="+dbid+";domain="+dbdomain+";user="+dbusr+";password="+dbpw);
    return dbcon;
  }
  /////////////////////////////////////////////////////////////////////////////
}
