package edu.unm.health.biocomp.molcloud;

import java.io.*; //BufferedReader,StringReader,ByteArrayInputStream

import java.util.Random;

import java.awt.*; //BasicStroke,Color,Graphics2D,Rectangle,Canvas,Label
import java.awt.geom.*; //Line2D,geom.Rectangle2D,AffineTransform
import java.awt.image.*; //BufferedImage

import javax.imageio.ImageIO; //ImageIO

import chemaxon.formats.*; //MolImporter
import chemaxon.struc.*; //Molecule


/**	MCloud MolHandler rendering the chemical structures through JChem/Marvin.

 */
class MarvinMolHandler implements MolHandler
{
  final static double STD_BOND = 1.54;
  final static double STD_BOX  = 200.0;
  private double width = 300.0;
  private double height = 240.0;

  private double scale=1.0;

  private Molecule mol=null;

  public MarvinMolHandler(String smiles)
  {
    try { this.mol=MolImporter.importMol(smiles,"smiles:"); }
    catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); }
  }
  public MarvinMolHandler(String smiles,String name)
  {
    try { this.mol=MolImporter.importMol(smiles,"smiles:"); }
    catch (Exception e) { System.err.println("ERROR: "+e.getMessage()); }
    if (this.mol!=null) this.mol.setName(name);
  }

  public void rescale(double scale)
  {
    // Rectangle area should be scaled by "scale", so sides scaled by sqrt(scale).
    this.scale *= Math.sqrt(scale);
  }

  public double getWidth()
  {
    double w = Math.max(this.scale*this.width,1.0);
    return(w);
  }

  public double getHeight()
  {
    double h = Math.max(this.scale*this.height,1.0);
    return(h);
  }

  public void draw(Graphics2D grph, double x, double y, double w, double h)
  {
    // Shade
    BasicStroke orgStroke = (BasicStroke)grph.getStroke(); 
    BasicStroke borderStroke = new BasicStroke(2.f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
    grph.setColor(Color.gray);
    grph.setStroke(borderStroke);
    grph.draw(new Line2D.Double(x-w+1,y-h+this.getHeight(),x-w+this.getWidth(),y-h+this.getHeight()));
    grph.draw(new Line2D.Double(x-w+this.getWidth(),y-h+1,x-w+this.getWidth(),y-h+this.getHeight()));
    grph.setStroke(orgStroke);
    // Clip
    Rectangle orgClip = grph.getClipBounds();
    grph.setClip(new Rectangle2D.Double(x-w,y-h,this.getWidth(),this.getHeight()));

    // Color rectangle background random pastel.
    Random random = new Random();
    int red   = (int)(0.9*255+0.1*random.nextInt(255));
    int green = (int)(0.9*255+0.1*random.nextInt(255));
    int blue  = (int)(0.9*255+0.1*random.nextInt(255));
    Color color = new Color(red,green,blue);
    grph.setColor(color);
    grph.fill(new Rectangle2D.Double(x-w,y-h,this.getWidth(),this.getHeight()));
    grph.setColor(Color.black);
    grph.draw(new Rectangle2D.Double(x-w,y-h,this.getWidth(),this.getHeight()));
    grph.setClip(orgClip);

    // Binary PNG image.
    byte[] pngbytes=null;
    BufferedImage img=null;

    try {
      // PNG background "#00ffffff" for transparent.
      String imgfmt = "png:w"+this.getWidth()+",h"+this.getHeight()+",b32,#00ffffff";
      //System.err.println("DEBUG (MarvinMolHandler.draw): this.getWidth()="+this.getWidth()+" this.getHeight()="+this.getHeight()+" imgfmt="+imgfmt);
      pngbytes=MolExporter.exportToBinFormat(this.mol,imgfmt);
      img=ImageIO.read(new ByteArrayInputStream(pngbytes));
    }
    catch (IOException e)
    {
      System.err.println("ERROR (MarvinMolHandler.draw()): "+e.getMessage());
    }

    AffineTransform xform = new AffineTransform();
    xform.setToTranslation(x-w,y-h);

    // Add some text, visible through transparent mol img.
    // Really draw the molecule.
    Canvas canvas = new Canvas(grph.getDeviceConfiguration());
    String molname=((this.mol.getName()!=null)?this.mol.getName():"");
    grph.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,9));
    grph.setColor(Color.gray);
    grph.drawString(molname,(float)(x-w+10),(float)(y+h-10));
    grph.drawImage(img,xform,canvas);
  }
}
