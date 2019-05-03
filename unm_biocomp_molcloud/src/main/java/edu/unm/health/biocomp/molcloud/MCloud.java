package edu.unm.health.biocomp.molcloud;
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*; //Image
import java.awt.image.*; //BufferedImage
import java.awt.geom.*;

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;

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
	replaces AvalonMolHandler.  Also modified to facilitate usage of MCloud class by
	other applications, e.g. molcloud_servlet.java.
*/
public class MCloud
{
  static String signature = null;
  static int stop = 0; // debug stop
  static boolean debug = false;
  static double maxScale = 0.;
  static double minScale = Double.MAX_VALUE;
  static double maxfactor = 5.; // largest mol will be maxfactor x larger than smallest
  static double idealdensity = 85.;
  static int pwidth = 1000;
  static int pheight = 700;

  JFrame molFrame = null;
  MolPanel molPanel = null;
  ArrayList layoutPoints = null;
  ArrayList boxes = null;

  // Default constructor.
  public MCloud()
  {
    this.boxes = new ArrayList(); 
  }

  // creating and showing MolPanel;
  public void showGui()
  {
    molFrame = new JFrame();
    molFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    molFrame.setTitle("Molecule Cloud");
    molPanel = new MolPanel(pwidth,pheight,this);
    molFrame.getContentPane().setLayout(new BorderLayout());
    molFrame.getContentPane().add("Center",molPanel);
    molFrame.pack();
    molFrame.setLocation(200,10);
    molFrame.setVisible(true); 
    molPanel.repaint();
  }
  // --------------------------------------------------------------------------
  public void sortBoxes()
  {
    Collections.sort(this.boxes);
  }
  // --------------------------------------------------------------------------
  public void optimizeLayout(int verbose)
  {
    MolBox.sortType = MolBox.COUNT;
    Collections.sort(this.boxes); // with largest count on the top

    double v;
    int nnotmoved = 0;
    int move = 1;
    for (int step=1;step<=2000;step++)
    {
      if (verbose>1 && step%100==0) System.err.println(step+"\r");

      boolean moved = false;
      for (int i=0;i<this.boxes.size();i++)
      {
        MolBox box = (MolBox)this.boxes.get(i);
  
        double vmin = score(i);
  
        double bestx=0.,besty=0.;
        boolean ok = false;
  
        box.x += move;
        // making sure not moving outside panel area
        if (box.x - box.width/2 > 0 && box.x + box.width/2 < pwidth)
        {
          v = score(i); 
          if (v < vmin)
          {
            vmin = v;
            bestx = box.x;
            besty = box.y;
            ok = true;
          }
        }
        box.x -= 2 * move;
        if (box.x - box.width/2 > 0 && box.x + box.width/2 < pwidth)
        {
          v = score(i); 
          if (v < vmin) {
            vmin = v;
            bestx = box.x;
            besty = box.y;
            ok = true;
          }
        }
        box.x += move;
  
        box.y += move;
        if (box.y - box.height/2 > 0 && box.y + box.height/2 < pheight)
        {
          v = score(i); 
          if (v < vmin) {
            vmin = v;
            bestx = box.x;
            besty = box.y;
            ok = true;
          }
        }
        box.y -= 2 * move;
  
        if (box.y - box.height/2 > 0 && box.y + box.height/2 < pheight)
        {
          v = score(i); 
          if (v < vmin) {
            vmin = v;
            bestx = box.x;
            besty = box.y;
            ok = true;
          }
        }
        box.y += move;
  
        if (ok) {
          box.x = bestx;
          box.y = besty;
          moved = true;
        }
  
      }

      if (!moved) {
        if (++nnotmoved == 3) {
          if (debug) System.err.println("optimisation took " + step + " steps");
          break;
        }
      }
      else if (nnotmoved > 0) {
        if (verbose>1) System.err.println("reset nnm");
        nnotmoved = 0;
      }
    }
  }
  // --------------------------------------------------------------------------
  private double score(int n) {
    return score(n,false);
  }
  // --------------------------------------------------------------------------
  // calculates current layout score
  private double score(int n, boolean n_max)
  {
    double v = 0.;

    // distances between boxes
    MolBox box = (MolBox)this.boxes.get(n);
    double sq1 = Math.sqrt(box.width/2*box.width/2 + box.height/2 * box.height/2);
    for (int i=0;i<this.boxes.size();i++) {
      if (i == n) {
        if (n_max) break;
        else continue;
      }
      MolBox boxi = (MolBox)this.boxes.get(i);
      double sq2 = Math.sqrt(boxi.width/2*boxi.width/2 + boxi.height/2 * boxi.height/2);
      // distance, dx, dy (if larger > 0)
      double[] dd = distance(box,boxi);
      double limit = sq1+sq2;

      double ov = overlap(box,boxi);

      if (ov > 0) {
        v += ov * ov;
        double q = ov * ov;
      }
      else if (dd[0] < limit) {
        v += limit - dd[0];
      }

    }
    
    // borders have repulsive force
    double limit = MolBox.avrgWidth;
    double d,dd;
    d = box.x - box.w;
    if (d <= limit) {
      dd = (limit - d);
      v += dd*dd;
    }
    d = pwidth - box.x - box.w;
    if (d <= limit) {
      dd = (limit - d);
      v += dd*dd;
    }
    d = box.y - box.h;
    if (d <= limit) {
      dd = (limit - d);
      v += dd*dd;
    }
    d = pheight - box.y - box.h;
    if (d <= limit) {
      dd = (limit - d);
      v += dd*dd;
    }

    // additional push from corners
    limit = MolBox.avrgWidth * 3;
    double cs = 8.;
    d = distance(box,0,0);
    if (d <= limit) v += (limit-d) * (limit-d) * cs;
    d = distance(box,pwidth,0);
    if (d <= limit) v += (limit-d) * (limit-d) * cs;
    d = distance(box,0,pheight);
    if (d <= limit) v += (limit-d) * (limit-d) * cs;
    d = distance(box,pwidth,pheight);
    if (d <= limit) v += (limit-d) * (limit-d) * cs;

    return v;
  }
  // --------------------------------------------------------------------------
  private static double[] distance(MolBox box1, MolBox box2)
  {
    double dx = box2.x - box1.x;
    double dy = box2.y - box1.y;
    double r12 = Math.sqrt(dx * dx + dy * dy); // distance between centers

    // dx and dy are distances betwen 2 boxes (borders)
    dx = 0.;
    if (box1.x + box1.w < box2.x - box2.w) 
      dx = box2.x - box2.w - (box1.x + box2.w);
    else if (box1.x - box1.w > box2.x + box2.w) 
      dx = box1.x - box2.w - (box1.x + box2.w);
    dy = 0.;
    if (box1.y + box1.h < box2.y - box2.h) 
      dy = box2.y - box2.h - (box1.y + box2.h);
    else if (box1.y - box1.h > box2.y + box2.h) 
      dy = box1.y - box2.h - (box1.y + box2.h);


    double[] d = new double[3];
    d[0] = r12;
    d[1] = dx;
    d[2] = dy; 
    return d;
  }
  // --------------------------------------------------------------------------
  private static double distance(MolBox box, int x, int y)
  {
    double dx = box.x - x;
    double dy = box.y - y;
    double r = Math.sqrt(dx * dx + dy * dy);
    return r;
  }
  // --------------------------------------------------------------------------
  private static double overlap(MolBox box1, MolBox box2)
  {
    if (box1.x + box1.w < box2.x - box2.w) return 0.; 
    if (box1.x - box1.w > box2.x + box2.w) return 0.; 
    if (box1.y + box1.h < box2.y - box2.h) return 0.;
    if (box1.y - box1.h > box2.y + box2.h) return 0.;

    // there is some overlap
    double xov = Math.min(box1.x+box1.w,box2.x+box2.w) - Math.max(box1.x-box1.w,box2.x-box2.w);
    double yov = Math.min(box1.y+box1.h,box2.y+box2.h) - Math.max(box1.y-box1.h,box2.y-box2.h);

    return xov * yov;
  }
  // --------------------------------------------------------------------------
  // layout with largest box in the center and smaller ones around
  public void cloudLayout()
  {
    // largest first
    MolBox.sortType = MolBox.SIZE;
    Collections.sort(this.boxes);

    int n = -1;
    for (Iterator i=this.boxes.iterator();i.hasNext();)
    {
      n++;
      MolBox box = (MolBox)i.next();
      if (n == 0) {
        // the largest box is located in the center, slightly left
        box.setCenter(pwidth/2-box.w,pheight/2);
      }
      else
      {
        // iterative placement of other boxes
        double vbest = Double.MAX_VALUE;
        int xbest=0,ybest=0;
        double step = 15.;
        for (int r=5;r<Math.sqrt(pwidth*pwidth + pheight*pheight)/2.;r+=step)
        {
          int nslices = (int)(2. * Math.PI * r / step);
          if (nslices < 1) nslices = 1;
          double sliceangle = 2. * Math.PI / nslices;
          for (int k=0;k<nslices;k++)
          {
            double angle = k * sliceangle;
            int x = (int)(pwidth/2. + r * Math.sin(angle));
            int y = (int)(pheight/2. + r * Math.cos(angle));
            if (stop > 0 && n == 1) layoutPoints.add(new Point(x,y)); // debug
            box.setCenter(x,y);
            if (IsOutside(box,x,y)) continue;
            double v = score(n,true);
            if (v < vbest) {
              vbest = v;
              xbest = x;
              ybest = y;
            } 
          }
        }
        box.setCenter(xbest,ybest);
        if (stop > 0 && n >= stop) {
          box.setCenter(-1000,-1000);
        }
      }
    }
  }
  // --------------------------------------------------------------------------
  public static boolean IsOutside(MolBox box, int x, int y)
  {
    if (box.x - box.w <= 0) return true;
    if (box.x + box.w >= pwidth) return true;
    if (box.y - box.h <= 0) return true;
    if (box.y + box.h >= pheight) return true;
    return false;
  }
  // --------------------------------------------------------------------------
  public void readData(String ifile, int n_skip, int n_max, int verbose)
	throws IOException
  {
    BufferedReader in = new BufferedReader(new FileReader(ifile));
    String line;
    int n=0;
    while ((line=in.readLine())!=null)
    {
      n++;
      StringTokenizer st = new StringTokenizer(line," \t\n\r\f");
      String smiles = st.nextToken();
      if (n_skip>0 && n<=n_skip) {
        if (verbose>1) System.err.println("skipping ["+n+"]"+smiles);
        continue;
      }
      double scale = Double.parseDouble(st.nextToken()); // count or score
      if (scale>this.maxScale) this.maxScale = scale;
      if (scale<this.minScale) this.minScale = scale;
      String data = null;
      if (st.hasMoreTokens()) data = st.nextToken(); // coloring etc
      MolBox box = new MolBox(smiles,scale,data,"");
      this.boxes.add(box);
      if (n == n_max) break; // reading only n_max items
    }
  }

  // --------------------------------------------------------------------------
  // Rescale, taking log of each box scale.
  public void scaleLog()
  {
    this.scale(true);
  }
  // --------------------------------------------------------------------------
  // Rescale, optionally taking log of each box scale.
  public void scale(boolean log)
  {
    for (Iterator i=this.boxes.iterator();i.hasNext();)
    {
      MolBox box = (MolBox)i.next();
      double scale = log?Math.log(box.scale):box.scale;
      if (scale > this.maxScale) this.maxScale = scale;
      if (scale < this.minScale) this.minScale = scale;
      box.rescale(scale);
    }
  }
  // --------------------------------------------------------------------------
  // Normalize scale based on max & min.
  public void scaleNormalize()
  {
    for (Iterator i=this.boxes.iterator();i.hasNext();)
    {
      MolBox box = (MolBox)i.next();
      double scale = (box.scale - this.minScale) / (this.maxScale - this.minScale);
      scale = 1. + (this.maxfactor - 1.) * scale;
      box.rescale(scale*scale);
    }
  }
  // --------------------------------------------------------------------------
  public void doSizes()
  { 
    // 1st pass, getting molecule dimensions in any standard scale
    double sumsize = 0.;
    for (Iterator i=this.boxes.iterator();i.hasNext();) {
      MolBox box = (MolBox)i.next();
      double size = box.width * box.height;
      sumsize += size;
    }
    double pv = sumsize * 100. / (pwidth * pheight); 
    double rescale = idealdensity / pv;
    if (debug) System.err.println("pv0="+pv+", rescale="+rescale);

    for (int step=1;step<=10;step++) // scale iteration steps
    {
      // 2nd pass, now with corrected scale
      MolBox.avrgWidth = 0.; MolBox.avrgHeight = 0.;
      sumsize = 0;
      for (Iterator i=this.boxes.iterator();i.hasNext();) {
        MolBox box = (MolBox)i.next();
        // new scale
        box.rescale(rescale);
        MolBox.avrgWidth += box.width;
        MolBox.avrgHeight += box.height;

        double size = box.width * box.height;
        sumsize += size;
      }

      pv = sumsize * 100. / (pwidth * pheight); 
      rescale = idealdensity / pv;
      //if (pv > idealdensity) rescale *= 0.9;
      //else rescale *= 1.1;
      if (debug) System.err.println("corrected pv="+pv+", rescale="+rescale);

      MolBox.avrgWidth /= this.boxes.size();
      MolBox.avrgHeight /= this.boxes.size();

      if (Math.abs(idealdensity-pv) < 1.) break;
    }
  }
  // --------------------------------------------------------------------------
  public void draw(Graphics2D g)
  {
    // display of layout points for testing
    if (layoutPoints != null) {
      for (Iterator i=layoutPoints.iterator();i.hasNext();) {
        Point p = (Point)i.next();
        //g.drawLine(p.getX(),p.getY(),p.getX(),p.getY());
        g.setColor(Color.black);
        g.drawLine(p.x-2,p.y,p.x+2,p.y);
        g.drawLine(p.x,p.y-2,p.x,p.y+2);
      }
    }    

    for (Iterator i=this.boxes.iterator();i.hasNext();) {
      MolBox box = (MolBox)i.next();
      box.paint(g);
    }

    if (signature != null) {
      // 1000 + 700 > 12
      int fontsize = (int)Math.round((pwidth + pheight) * 12. / 1700.);
      g.setColor(Color.blue);
      //g.setFont(new Font("SansSerif",Font.PLAIN,fontsize));
      g.setFont(new Font("SansSerif",Font.BOLD,fontsize));
      int w = ((FontMetrics)g.getFontMetrics()).stringWidth(signature); 
      g.drawString(signature,pwidth-w-fontsize,pheight-fontsize/2);
    }
  }
  // --------------------------------------------------------------------------
  public static void SaveImage(MCloud mcloud,String ofile)
	throws IOException
  {
    File fout = new File(ofile);
    SaveImage(mcloud,fout);
  }
  // --------------------------------------------------------------------------
  public static void SaveImage(MCloud mcloud,File fout)
	throws IOException
  {
    BufferedImage image = new BufferedImage(pwidth,pheight,BufferedImage.TYPE_USHORT_555_RGB);
    Graphics2D g = image.createGraphics();

    g.setColor(Color.white); // white bg
    g.fillRect(0,0,pwidth,pheight);
    mcloud.draw(g);

    javax.imageio.ImageIO.setUseCache(false);
    javax.imageio.ImageIO.write(image, "png", fout);
  }

  /////////////////////////////////////////////////////////////////////////////
  private static int LoadMolCloud(MCloud molcloud, MolImporter molReader,int n_max,boolean randomscale)
  {
    int n_mol=0;
    Molecule mol;
    int n_failed=0;
    while (true)
    {
      try {
        mol=molReader.read();
      }
      catch (IOException e)
      {
        ++n_failed;
        continue;
      }
      if (mol==null) break;
      ++n_mol;
      try {
        String molname = mol.getName();
        String[] fields = Pattern.compile("\\s+").split(molname);
        Double scale = null;
        if (randomscale)
        {
          scale = Math.random() * 100.0 + 1.0; //Avoid log(0).
          //System.err.println("DEBUG: scale = "+scale);
        }
        else
        {
          scale = Double.parseDouble(fields[0]);
        }
        String f2 = (fields.length>1)?fields[1]:"";
        String smi = MolExporter.exportToFormat(mol,"smiles:-a");
        MolBox mbox = new MolBox(smi,scale,f2,"");
        molcloud.boxes.add(mbox);
      }
      catch (Exception e)
      {
        System.err.println("ERROR (LoadMolCloud) (name format ok?  SCORE<space>OPTIONALDATA): "+e.getMessage());
        ++n_failed;
        continue;
      }
      if (n_mol==n_max) break;
    }
    try { molReader.close(); } catch (IOException e) { }
    System.err.println("errors: "+n_failed);
    return n_mol;
  }

  // --------------------------------------------------------------------------
  private static void Help()
  {
    System.err.println("MoleculeCloud v1.21x");
    System.err.println("Usage: java -jar MCloud.jar -i INFILE [options]");
    System.err.println("required:");
    System.err.println("     -i IFILE ....... input molecule file (SMI, SDF, etc.)");
    System.err.println("                      (mol name format: SCORE<space>OPTIONALDATA)");
    System.err.println("  and");
    System.err.println("     -gui ........... display via swing gui");
    System.err.println("  or");
    System.err.println("     -o OFILE ....... save image to PNG file");
    System.err.println("options:");
    System.err.println("     -x value ....... image width ["+pwidth+"]");
    System.err.println("     -y value ....... image height ["+pheight+"]");
    System.err.println("     -nmax NMAX ..... process only first NMAX mols");
    System.err.println("     -skip N ........ skip first n structures");
    System.err.println("     -logscale ...... scale by log(score)");
    System.err.println("     -randomscale ... randomly scaled (for testing)");
    System.err.println("     -opt ........... optimize layout");
    System.err.println("     -v [-vv] ....... verbose [very]");
    System.err.println("===========================================================================");
    System.err.println("MoleculeCloud by Peter Ertl and Bernd Rohde @ Novartis, Basel, Switzerland ");
    System.err.println("This version modified by Jeremy Yang @ UNM, Albuquerque, USA.              ");
    System.err.println("===========================================================================");
    System.exit(0);
  }

  // --------------------------------------------------------------------------
  public static void main(String[] args)
  {
    if (args.length==0) Help();

    MCloud molcloud = new MCloud();
    String ofile = null;
    boolean doOptimization = false;
    boolean logscale = false;
    boolean noMolecules = false;
    boolean doGui = false;
    boolean doImage = false;
    boolean randomscale = false;
    int n_max = Integer.MAX_VALUE;
    int verbose = 0;
    String ifile = null;
    String ifile_oldway = null;
    int n_skip = 0;
    for (int i=0;i<args.length;i++) {
      if (args[i].equals("-i")) ifile = args[++i];
      else if (args[i].equals("-f")) ifile_oldway = args[++i];
      else if (args[i].equals("-o")) { doImage=true; ofile=args[++i]; }
      else if (args[i].equals("-opt")) doOptimization = true;
      else if (args[i].equals("-logscale")) logscale = true;
      else if (args[i].equals("-nomols")) noMolecules = true;
      else if (args[i].equals("-nmax")) n_max = Integer.parseInt(args[++i]); 
      else if (args[i].equals("-x")) pwidth = Integer.parseInt(args[++i]);
      else if (args[i].equals("-y")) pheight = Integer.parseInt(args[++i]);
      else if (args[i].equals("-skip")) n_skip = Integer.parseInt(args[++i]);
      else if (args[i].equals("-gui")) doGui = true;
      else if (args[i].equals("-randomscale")) randomscale = true;
      else if (args[i].equals("-v")) verbose = 1;
      else if (args[i].equals("-vv")) verbose = 2;
      else if (args[i].equals("-stop")) {
        molcloud.stop = Integer.parseInt(args[++i]);
        molcloud.layoutPoints = new ArrayList();
      }
    }

    MolBox.avrgWidth = MolBox.avrgHeight = 0.;
    MolBox.sortType = MolBox.SIZE; // sorting acording to the size (number of atoms)
    if (!noMolecules) MolBox.showMolecules = true;

    MolImporter molReader=null;

    if (ifile_oldway!=null)
    {
      try { molcloud.readData(ifile_oldway,n_skip,n_max,verbose); }
      catch (IOException e) {
        System.err.println("ERROR: Cannot read file: "+ifile_oldway+" ;"+e.getMessage());
        System.exit(-1);
      }
    }
    else if (ifile!=null)
    {
      try {
        molReader = new MolImporter(ifile);
        if (verbose>0) {
          String desc=MFileFormatUtil.getFormat(molReader.getFormat()).getDescription();
          System.err.println("Input: "+ifile+" : format: "+molReader.getFormat()+" ("+desc+")");
        }
      }
      catch (IOException e) {
        System.err.println("ERROR: Cannot read file: "+ifile+" ;"+e.getMessage());
        System.exit(-1);
      }
      int n_mol = LoadMolCloud(molcloud,molReader,n_max,randomscale);
      System.err.println("mols in: "+n_mol);
      if (n_mol==n_max)
        System.err.println("Limit reached: N_MAX mols: "+n_max);
    }
    else
    {
      System.err.println("ERROR: input file required.");
      Help();
    }

    if (!doGui && !doImage)
    {
      System.err.println("ERROR: no output specified; -gui or -o required.");
      Help();
    }

    molcloud.scale(logscale);
    molcloud.scaleNormalize();
    molcloud.doSizes();
    molcloud.sortBoxes();
    molcloud.cloudLayout(); 
    if (doGui) molcloud.showGui();

    if (doOptimization) molcloud.optimizeLayout(verbose);

    if (doGui) molcloud.molPanel.repaint();

    if (doImage)
    {
      try { SaveImage(molcloud,ofile); }
      catch (IOException e) {
        System.err.println("ERROR: Cannot write file: "+ofile+" ;"+e.getMessage());
      }
      if (verbose>0) System.err.println("File " + ofile  + " written");
    }
  }
}
// * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * 
