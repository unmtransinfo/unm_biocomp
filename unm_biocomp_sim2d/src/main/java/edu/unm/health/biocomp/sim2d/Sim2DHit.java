package edu.unm.health.biocomp.sim2d;

import java.util.*;

/**	For search results storage and hitlist sorting.
	@author Jeremy J Yang
*/
public class Sim2DHit implements Comparable<Object>
{
  public int i;
  public float sim=-1.0f;
  public boolean subset;
  public int brightness;
  public int bitcount;
  public int commonbitcount;
  public String smiles;
  public String name;
  public Sim2DHit() {};
  public int compareTo(Object o) throws ClassCastException
  {
    return (sim<((Sim2DHit)o).sim ? 1 : (sim>((Sim2DHit)o).sim ? -1 : 0));
  }
}
