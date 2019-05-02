package edu.unm.health.biocomp.molcloud;
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.Image;
import java.awt.image.*;
import java.awt.geom.*;

/**	Molecule Cloud, Copyright (c) 2012, Novartis Institutes for BioMedical Research Inc
	written by Peter Ertl and Bernhard Rohde.
	<br>
	This software is released under the terms of FreeBSD license,
	See license.txt in the distribution.
	For usage details see readme.txt in the distribution
	<br>
	P. Ertl, B. Rohde: The Molecule Cloud - compact visualization of large collections of molecules
	J. Cheminformatics 2012, 4:12, http://www.jcheminf.com/content/4/1/12/.
	<br>
	This version further edited by Jeremy Yang, UNM Translational Informatics Division,
	to employ ChemAxon Marvin beans for molecule depiction, i.e. MarvinMolHandler 
	replaces AvalonMolHandler.
*/
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
public class MolBox implements Comparable
{
  private String smiles = null;
  private String data = null; // coloring etc.
  private String text = null; // name, score, etc.
  private MolHandler molHandler = null;

  double x,y; // center
  double width,height;
  double w,h; // width/2 ....
  double scale;

  static boolean showMolecules = false;
  static double avrgWidth,avrgHeight;
  static int sortType = 0;
  static final int SIZE = 1, COUNT = 2;

  //public void setText(String _text) { this.text=_text; }
  //public String getText() { return this.text; }

  MolBox(String _smiles, double _scale, String _data, String _text)
  {
    this.smiles = _smiles;
    this.scale = _scale;
    this.data = _data;
    this.text = _text;
    // test handler just to display rectangles (for testing)
    // molHandler = new TestMolHandler(smiles);

    // see readme txt for how to connect the Avalon Handler
    //molHandler = new AvalonMolHandler(smiles);

    // (Jeremy Yang)
    // MarvinMolHandler generates image using ChemAxon Marvin beans API.
    molHandler = new MarvinMolHandler(_smiles,_text);
  }

  public void rescale(double rescale)
  {
    molHandler.rescale(rescale);
    width = molHandler.getWidth();
    height = molHandler.getHeight();
    w = width / 2.;
    h = height / 2.;
  }

  public void setCenter(double x, double y)
  {
    this.x = (int)Math.round(x);
    this.y = (int)Math.round(y);
  }

  public String toString() {
    String s = "MolBox:";
    s += " " + smiles;
    return s;
  }

  public void paint(Graphics2D g)
  {
    if (showMolecules) {
      molHandler.draw(g,x,y,w,h);
    }
    else {
      g.setColor(Color.red);
      g.draw(new Rectangle2D.Double(x-w,y-h,width,height));
      g.setColor(Color.black);
    }
  }

  // smaller on the top (so that they are visible)
  public int compareTo(Object o)
  {
    MolBox box = (MolBox)o;

    if (sortType == COUNT) {
      if (scale > box.scale) return 1;
      else if (scale < box.scale) return -1;
    }
    else if (sortType == SIZE) {
      if (width * height < box.width * box.height) return 1;
      else if (width * height > box.width * box.height) return -1;
    }
    else 
      throw new RuntimeException("Bad sort type");
    return 0;
  }

  public void print() {
    System.out.println(this);
  }
}
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
