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
public class MolPanel extends JPanel
{
  int xsize, ysize;
  MCloud mcloud; // defined draw method (used also in image saving)

  MolPanel(double xsize, double ysize, MCloud mcloud)
  {
    super();
    this.xsize = (int)xsize;
    this.ysize = (int)ysize;
    this.mcloud = mcloud;
    setBackground(Color.white);
    setPreferredSize(new Dimension((int)xsize,(int)ysize));
  }
  // --------------------------------------------------------------------------
  public void paintComponent(Graphics gg)
  {
    super.paintComponent(gg); // white bg
    Graphics2D g = (Graphics2D)gg;
    mcloud.draw(g);
  }
}
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
