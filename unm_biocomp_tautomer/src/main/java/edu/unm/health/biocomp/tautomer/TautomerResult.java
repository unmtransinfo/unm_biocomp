package edu.unm.health.biocomp.tautomer;

import java.io.*;
import java.util.*;

import chemaxon.struc.*; //Molecule


/**	Result of tautomer prediction.

	@author Jeremy J Yang
*/
public class TautomerResult
{
  private Molecule mol;
  private String name;
  private ArrayList<Molecule> tautomers;

  public TautomerResult()
  {
    this.mol = null;
    this.tautomers = new ArrayList<Molecule>();
  }
  public TautomerResult(Molecule _mol)
  {
    this.mol = _mol;
    this.tautomers = new ArrayList<Molecule>();
  }

  public String getName() { return this.name; }
  public void setName(String _name) { this.name = _name; }

  public Molecule getMol() { return this.mol; }
  public void setMol(Molecule _mol) { this.mol = _mol; }

  public void addTautomer(Molecule _tmol) { this.tautomers.add(_tmol); }
  public int tautomerCount() { return(this.tautomers.size()); }
  public ArrayList<Molecule> tautomerList() { return(this.tautomers); }
}
