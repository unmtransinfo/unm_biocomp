package edu.unm.health.biocomp.text;

import java.util.*;
import java.util.regex.*;

/**	Provides fast in-memory Name data storage; used by NameList.
	Typically used to sort synonyms for human consumption.
	@author Jeremy J Yang
*/
/////////////////////////////////////////////////////////////////////////////
public class Name
	implements Comparable<Object>
{
  private static Integer NICEST_LEN = 7;   //debatable!
  private String txt;
  private Integer niceness;
  private Boolean hit; //In general a hit should be ranked above a non-hit.

  private Name() {} //not allowed
  public Name(String _txt)
  {
    this.txt = _txt;
    this.niceness = NameNicenessScore(_txt);
    this.hit = false;
  }
  public String getValue() { return this.txt; }
  public String toString() { return this.txt; }

  public void setValue(String _txt)
  {
    this.txt = _txt;
    this.niceness = NameNicenessScore(_txt);
  }
  public void setHit(boolean _hit) { this.hit = _hit; }
  public Boolean isHit() { return this.hit; }

  /////////////////////////////////////////////////////////////////////////////
  /**   Score compound names for human readability.  Well known commercial
        and common names should rank high.
  */
  public static int NameNicenessScore(String name)
  {
    int score=0;
    Pattern pat_proper = Pattern.compile("^[A-Z][a-z]+$");
    Pattern pat_text = Pattern.compile("^[A-Za-z ]+$");
    Pattern pat_okstart = Pattern.compile("^[A-z][A-z][A-z][A-z][A-z][A-z][A-z].*$");
    if (pat_proper.matcher(name).find()) score+=100;
    else if (pat_text.matcher(name).find()) score+=50;
    else if (pat_okstart.matcher(name).find()) score+=10;
    if (name.matches("[\\[\\]\\{\\}]")) score-=50;
    if (name.matches("[_/\\(\\)]")) score-=10;
    if (name.matches("\\d")) score-=10;
    score -= Math.abs(NICEST_LEN-name.length());
    return score;
  }

  /////////////////////////////////////////////////////////////////////////////
  public int compareTo(Object o)        //native-order (by niceness)
  {
    return (this.isHit() && !((Name)o).isHit() ?  1 : (!this.isHit() && ((Name)o).isHit() ? -1 : 
      (this.niceness > ((Name)o).niceness ?  1 : (this.niceness < ((Name)o).niceness ? -1 : 0))));
  }
}
