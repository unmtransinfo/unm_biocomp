package edu.unm.health.biocomp.text;

import java.io.*;
import java.util.*; //Date,Collections
import java.util.regex.*; // Pattern, Matcher
import java.text.*;

import edu.unm.health.biocomp.util.*; //GetURI2List

/**	Container for Names, typically drug and compound names.

	@author Jeremy J Yang
*/
public class NameList extends ArrayList<Name>
{
  private HashSet names;
  private boolean case_sensitive;

  private java.util.Date t_loaded;
  
  /////////////////////////////////////////////////////////////////////////////
  public NameList()
  {
    this.names = new HashSet<String>();
    this.t_loaded = new java.util.Date();
    this.case_sensitive=false;
  }
  public void setCasesensitive(boolean _cs) { this.case_sensitive = _cs; }
  public boolean isCasesensitive() { return this.case_sensitive; }

  /////////////////////////////////////////////////////////////////////////////
  public NameList(Collection<String> _names)
  {
    this.names = new HashSet<String>();
    this.t_loaded = new java.util.Date();
    for (String name: _names)
      this.merge(new Name(name));
    Collections.sort(this); //default, ByNiceness
  }
  /////////////////////////////////////////////////////////////////////////////
  public boolean merge(Name _name)
  {
    if (!this.contains(this.isCasesensitive()?_name.getValue():_name.getValue().toLowerCase()))
    {
      this.add(_name);
      this.names.add(this.isCasesensitive()?_name.getValue():_name.getValue().toLowerCase());
      return true;
    }
    return false;
  }
  /////////////////////////////////////////////////////////////////////////////
  public void extend(NameList names2)
  {
    for (Name name: names2)
      this.merge(name);
  }
  /////////////////////////////////////////////////////////////////////////////
  public java.util.Date getTimestamp() { return this.t_loaded; }
  public void setTimestamp(java.util.Date _ts) { this.t_loaded=_ts; }
  public void refreshTimestamp() { this.t_loaded = new java.util.Date(); }
  /////////////////////////////////////////////////////////////////////////////
  public boolean contains(String txt)
  {
    return this.names.contains(this.isCasesensitive()?txt:txt.toLowerCase());
  }
  /////////////////////////////////////////////////////////////////////////////
  public NameList getNamesSortedByNiceness()
  {
    Collections.sort(this); //default, ByNiceness
    Collections.reverse(this);
    return this;
  }
  /////////////////////////////////////////////////////////////////////////////
  /**	Put non-matches at the bottom. */
  public NameList getNamesSortedByNiceness(String subtxt)
  {
    if (subtxt==null || subtxt.isEmpty()) return this.getNamesSortedByNiceness();
    for (Name name: this)
      name.setHit(name.toString().matches("^.*(?i:"+subtxt+").*$"));
    Collections.sort(this);
    Collections.reverse(this);
    return this;
  }
  /////////////////////////////////////////////////////////////////////////////
  public NameList getNamesSortedByAscii()
  {
    Collections.sort(this,ByAscii);
    return this;
  }
  /////////////////////////////////////////////////////////////////////////////
  public static Comparator<Name> ByAscii = new Comparator<Name>()  {     //Collections.sort(names,ByAscii)
    public int compare(Name nA,Name nB)
    { return (nA.getValue().equalsIgnoreCase(nB.getValue())?nA.getValue().compareTo(nB.getValue()):nA.getValue().compareToIgnoreCase(nB.getValue())); }
    boolean equals(Name nA,Name nB)
    { return (nA.getValue().equals(nB.getValue())); }
  };
  /////////////////////////////////////////////////////////////////////////////
  /*	Testing purposes only.
  */
  public static void main(String[] args)
	throws IOException
  {
    java.util.Date t_0 = new java.util.Date();
    NameList nlist = new NameList();

    String[] synonyms={
    "Cevex",
    "Cetemican",
    "Cescorbat",
    "Celin",
    "Cecon",
    "Cebid",
    "Ascorteal",
    "Allercorb",
    "Stuartinic",
    "Proscorbin",
    "Natrascorb",
    "Citriscorb",
    "Ascorbicin",
    "Ascorbicap",
    "Ascorbicab",
    "Ascorbajen",
    "Vitascorbol",
    "Ascorbutina",
    "Testascorbic",
    "Sodascorbate",
    "Parentrovite",
    "Laroscorbine",
    "Ascorbinsaeure",
    "Semidehydroascorbate",
    "Monodehydroascorbate",
    "cevibid",
    "Ce lent",
    "Mvc Plus",
    "vitamin C",
    "ascorbate",
    "Vicomin C",
    "Catavin C",
    "roscorbi c",
    "Planavit C",
    "BEROCCA PN",
    "Davitamon C",
    "ASC",
    "Mixture Name",
    "AA",
    "ascorbic acid",
    "Acid Ascorbic",
    "Oral Vitamin C",
    "Cevitamic acid",
    "L Ascorbic Acid",
    "Erythorbic acid",
    "Acido ascorbico",
    "SODIUM ASCORBATE",
    "Ascorbyl radical",
    "Acide ascorbique",
    "Ferrous ascorbate"};

    for (String s: synonyms)
    {
      if (!nlist.contains(s)) nlist.merge(new Name(s));
    }

    System.err.println("name count: "+nlist.size());
    System.err.println("timestamp: "+nlist.getTimestamp().toString());
    System.err.println("total elapsed time: "+time_utils.TimeDeltaStr(t_0,new java.util.Date()));
    System.err.println("sort tests...");
    System.err.println("ascii...");
    nlist = nlist.getNamesSortedByAscii();
    for (Name name: nlist) System.out.println("\t"+name.getValue());
    System.err.println("default...");
    nlist = nlist.getNamesSortedByNiceness();
    for (Name name: nlist) System.out.println("\t"+name.getValue());
  }
}
