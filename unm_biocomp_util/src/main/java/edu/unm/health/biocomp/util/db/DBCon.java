package edu.unm.health.biocomp.util.db;

import java.io.*;
import java.util.*;
import java.sql.*; //Connection,DriverManager,Driver


/**	Class to support multiple db drivers: PostgreSQL, MySql, Derby, Microsoft.

	Allowed values for database type: "postgres", "mysql", "derby" or "microsoft".
	For Derby, several params are ignored: dbhost, dbport, dbusr, dbpw.
        Derby dbname is path comprised of dir and name, e.g. "/tmp/derby/mydb_1".
	Derby local/embedded dbs only.

	Avoids redundant driver registration, which can cause Tomcat problems.

	@see org.postgresql.Driver
	@see com.mysql.jdbc.Driver
	@see org.apache.derby.jdbc.EmbeddedDriver
	@see net.sourceforge.jtds.jdbc.Driver

	@author Jeremy Yang
*/
public class DBCon
{
  private DBCon() {} //disallow default constructor
  private String dbtype;
  private String dbdomain="HEALTH"; //Microsoft only
  private Connection con;

  /**	Normal constructor.
  */
  public DBCon(String _dbtype,String dbhost,Integer dbport,String dbname,String dbusr,String dbpw)
	throws Exception
  {
    this.setDBType(_dbtype);

    if (_dbtype.equalsIgnoreCase("postgres"))
    {
      if (!this.isDriverRegistered(org.postgresql.Driver.class))
        DriverManager.registerDriver(new org.postgresql.Driver());
      Connection dbcon=DriverManager.getConnection("jdbc:postgresql://"+dbhost+":"+dbport+"/"+dbname,dbusr,dbpw);
      setConnection(dbcon);
    }
    else if (_dbtype.equalsIgnoreCase("mysql"))
    {
      if (!this.isDriverRegistered(com.mysql.jdbc.Driver.class))
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
      Connection dbcon=DriverManager.getConnection("jdbc:mysql://"+dbhost+":"+dbport+"/"+dbname,dbusr,dbpw);
      setConnection(dbcon);
    }
    else if (_dbtype.equalsIgnoreCase("microsoft"))
    {
      if (!this.isDriverRegistered(net.sourceforge.jtds.jdbc.Driver.class))
        DriverManager.registerDriver(new net.sourceforge.jtds.jdbc.Driver());
      Connection dbcon=DriverManager.getConnection("jdbc:jtds:sqlserver://"+dbhost+":"+dbport+";DatabaseName="+dbname+";domain="+dbdomain+";Instance=master",dbusr,dbpw);
      setConnection(dbcon);
    }
    else if (_dbtype.equalsIgnoreCase("derby"))
    {
      //System.err.println("DEBUG: Attempting Derby connection: jdbc:derby:"+dbname);
      if (!this.isDriverRegistered(org.apache.derby.jdbc.EmbeddedDriver.class))
        DriverManager.registerDriver(new org.apache.derby.jdbc.EmbeddedDriver());
      Connection dbcon=DriverManager.getConnection("jdbc:derby:"+dbname);
      //if (dbcon!=null) System.err.println("DEBUG: Derby connection ok: jdbc:derby:"+dbname);
      setConnection(dbcon);
    }
    else
    {
      throw new Exception("Unknown dbtype: \""+_dbtype+"\"");
    }
  }

  public String getDBType() { return this.dbtype; }
  public void setDBType(String _dbtype) { this.dbtype=_dbtype; }
  public Connection getConnection() { return this.con; }
  public void setConnection(Connection _con) { this.con=_con; }
  public void close() throws SQLException { this.con.close(); }
  public Boolean isClosed() throws SQLException { return this.con.isClosed(); }

  public String serverStatusTxt()
	throws SQLException
  {
    if (this.getDBType().equalsIgnoreCase("postgres")) { return pg_utils.ServerStatusTxt(this.con); }
    else if (this.getDBType().equalsIgnoreCase("mysql")) { return mysql_utils.ServerStatusTxt(this.con); }
    else if (this.getDBType().equalsIgnoreCase("derby")) { return derby_utils.ServerStatusTxt(this.con); }
    else if (this.getDBType().equalsIgnoreCase("microsoft")) { return jtds_utils.ServerStatusTxt(this.con); }
    return null;
  }

  public ResultSet executeSql(String sql)
	throws SQLException
  {
    if (this.getDBType().equalsIgnoreCase("postgres")) { return pg_utils.ExecuteSql(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("mysql")) { return mysql_utils.ExecuteSql(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("derby")) { return derby_utils.ExecuteSql(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("microsoft")) { return jtds_utils.ExecuteSql(this.con,sql); }
    return null;
  }

  public Boolean execute(String sql)
	throws SQLException
  {
    if (this.getDBType().equalsIgnoreCase("postgres")) { return pg_utils.Execute(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("mysql")) { return mysql_utils.Execute(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("derby")) { return derby_utils.Execute(this.con,sql); }
    else if (this.getDBType().equalsIgnoreCase("microsoft")) { return jtds_utils.Execute(this.con,sql); }
    return false;
  }

  public Boolean isDriverRegistered(Class dclass)
	throws SQLException
  {
    Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
    boolean is_registered = false;
    for (int i=0; drivers.hasMoreElements(); )
    {
      java.sql.Driver d=(java.sql.Driver)drivers.nextElement();
      ++i;
      //System.out.println("DEBUG: "+i+". driver registered: "+d.getClass().getName());
      //System.out.println("DEBUG: "+((d.getClass().equals(dclass))?"  EQUAL":"UNEQUAL")+": "+d.getClass().getName()+", "+dclass.getName());
      is_registered |= (d.getClass().equals(dclass));
    }
    //System.out.println("DEBUG: is_registered: "+is_registered);
    return is_registered;
  }
}
