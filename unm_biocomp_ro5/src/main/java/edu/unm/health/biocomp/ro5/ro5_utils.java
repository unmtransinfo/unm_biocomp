package edu.unm.health.biocomp.ro5;

import java.io.*;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.apache.http.*; //HttpEntity,
import org.apache.http.client.*; //HttpClient,
import org.apache.http.client.methods.*; //HttpGet,
import org.apache.http.impl.client.*; //HttpClientBuilder,

import chemaxon.formats.*;
import chemaxon.util.MolHandler;
import chemaxon.struc.*;
import chemaxon.struc.prop.MMoleculeProp;
import chemaxon.sss.search.*;
import chemaxon.license.*;

import edu.unm.health.biocomp.util.*; //time_utils,math_utils
import edu.unm.health.biocomp.cdk.*; //cdk_utils

/**	Lipinsky Rule of 5 analysis.
	See Help() for list of descriptors.

	@author Jeremy J Yang
*/
public class ro5_utils
{
  public static String ReadFileUrl2String(String fileurl) throws Exception
  {
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(fileurl);     
    HttpResponse response = client.execute(request);
    HttpEntity entity = response.getEntity();
    if (entity == null) { return null; }
    InputStream is = entity.getContent(); // Create an InputStream with the response
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = reader.readLine())!=null) // Read line by line
      sb.append(line + "\n");
    String resString = sb.toString(); // Result is here
    is.close(); // Close the stream
    resString = resString.replaceAll("[@/\\\\]", ""); //Backslashes cause http problems.
    return resString;
  }

  /////////////////////////////////////////////////////////////////////////////
  /**	Generate Ro5 calculations.
  */
  public static Ro5Results Ro5_Calculate(List<Molecule> mols) throws Exception
  {
    List<String> smis = new ArrayList<String>();
    for (Molecule mol: mols)
      smis.add(MolExporter.exportToFormat(mol,"smiles:u"));

    Ro5Results results = new Ro5Results();
    MolHandler mhand = new MolHandler();
    for (int i_mol=0;i_mol<mols.size();++i_mol)
    {
      Molecule mol=mols.get(i_mol);
      Ro5Result result = new Ro5Result();

      String smi = MolExporter.exportToFormat(mol,"smiles:u");
      result.setSmiles(smi);
      result.setName(mol.getName());

      System.err.println("DEBUG: molname = \""+mol.getName()+"\"; smiles = "+smi);

      mol.hydrogenize(false);
      mhand.setMolecule(mol);
      result.setMwt(mhand.calcMolWeightInDouble());

      try { result.setHbd(HBonds.getDonors(mol)); }
      catch (SearchException e) {
        System.err.println("ERROR: "+e.getMessage());
      }

      try { result.setHba(HBonds.getAcceptors(mol)); }
      catch (SearchException e) {
        System.err.println("ERROR: "+e.getMessage());
      }

      result.setLogp(cdk_utils.CalcXlogpFromSmiles(smi));
      results.setLogpProgram("CDK:XLogP");

      results.add(result);

      mol.setProperty("mol_name",mol.getName());
      mol.setProperty("mwt",String.format("%.3f",result.getMwt()));
      mol.setProperty("hbd",String.format("%d",result.getHbd()));
      mol.setProperty("hba",String.format("%d",result.getHba()));
      mol.setProperty("xlogp",String.format("%.3f",result.getLogp()));
    }
    return results;
  }

  /////////////////////////////////////////////////////////////////////////////
  private static void Test(MolImporter molReader, int vmax, int verbose) throws Exception
  {
    ArrayList<Molecule> mols = new ArrayList<Molecule>();
    while (true)
    {
      Molecule mol=null;
      try {
        if ((mol=molReader.read())==null) break;
      }
      catch (IOException e) {
        System.err.println(e.toString());
      }
      mols.add(mol);
    }
    Ro5Results results = Ro5_Calculate(mols);
    System.out.println(Ro5_ResultsTxt(results, mols, vmax));
    System.err.println("n_mol = "+mols.size());
  }

  /////////////////////////////////////////////////////////////////////////////
  private static String Ro5_ResultsTxt(Ro5Results results, ArrayList<Molecule> mols, int vmax) throws IOException
  {
    String txt="";
    String[] dataFields={ "mwt","hbd","hba","logp" };
    Integer[] vdist = {0,0,0,0,0};
    int n_fail=0;
    for (Ro5Result result: results)
    {
      int viols=result.violations();
      if (viols>vmax)
        ++n_fail;
      ++vdist[viols];
    }
    for (int i=0;i<=4;++i)
      txt+=("  "+i+" Ro5-violation mols: "+vdist[i]+"\n");
    txt+=("Results:\n");
    txt+=("mols processed: "+mols.size()+"\n");
    txt+=("mols passed: "+(mols.size()-n_fail)+"\n");
    txt+=("mols failed: "+n_fail+"\n");
    txt+=("(Where failure defined as violations > "+vmax+".)\n");
    txt+="\n";
    txt+=(Ro5_ResultsSummaryTxt(results)+"\n");
    for (String field:dataFields) { txt+=(field+","); }
    txt+="\n";
    for (int i_mol=0;i_mol<mols.size();++i_mol)
    {
      Molecule mol=mols.get(i_mol);
      Ro5Result result=results.get(i_mol);
      int viols=result.violations();
      txt+=(""+(i_mol+1)+". "+mol.getName()+"\n");
      txt+=("\tMWT: "+String.format("%.2f",result.getMwt())+"\n") ;
      txt+=("\tHBD: "+result.getHbd()+"\n");
      txt+=("\tHBA: "+result.getHba()+"\n");
      txt+=("\tLOGP: "+String.format("%.2f",result.getLogp())+"\n");
      txt+=("\tViolations: "+viols+"\n");
    }
    return txt;
  }
  /////////////////////////////////////////////////////////////////////////////
  private static String Ro5_ResultsSummaryTxt(Ro5Results results)
  {
    Integer[] vdist = {0,0,0,0,0};
    for (Ro5Result result: results) ++vdist[result.violations()];
    String txt="";
    txt+=("Ro5 violations,#mols\n");
    for (int i=0;i<=4;++i)
      txt+=(""+i+","+vdist[i]+"\n");
    txt+=("property,min,max,mean,median,std,violations\n");
    txt+=("MWT,");
    txt+=(String.format("%.1f",results.getMinMwt())+",");
    txt+=(String.format("%.1f",results.getMaxMwt())+",");
    txt+=(String.format("%.1f",results.getMeanMwt())+",");
    txt+=(String.format("%.1f",results.getPercentileMwt(50))+",");
    txt+=(String.format("%.1f",results.getStdMwt())+",");
    txt+=(""+results.getViolationsMwt()+"\n");
    txt+=("HBA,");
    txt+=(""+results.getMinHba()+",");
    txt+=(""+results.getMaxHba()+",");
    txt+=(String.format("%.1f",results.getMeanHba())+",");
    txt+=(""+Math.round(results.getPercentileHba(50))+",");
    txt+=(String.format("%.1f",results.getStdHba())+",");
    txt+=(""+results.getViolationsHba()+"\n");
    txt+=("HBD,");
    txt+=(""+results.getMinHbd()+",");
    txt+=(""+results.getMaxHbd()+",");
    txt+=(String.format("%.1f",results.getMeanHbd())+",");
    txt+=(""+Math.round(results.getPercentileHbd(50))+",");
    txt+=(String.format("%.1f",results.getStdHbd())+",");
    txt+=(""+results.getViolationsHbd()+"\n");
    txt+=("LOGP,");
    txt+=(String.format("%.1f",results.getMinLogp())+",");
    txt+=(String.format("%.1f",results.getMaxLogp())+",");
    txt+=(String.format("%.1f",results.getMeanLogp())+",");
    txt+=(String.format("%.1f",results.getPercentileLogp(50))+",");
    txt+=(String.format("%.1f",results.getStdLogp())+",");
    txt+=(""+results.getViolationsLogp()+"\n");
    return txt;
  }


  /////////////////////////////////////////////////////////////////////////////
  private static int vmax=1;
  private static void Help(String msg)
  {
    System.err.println(msg+"\n"
      +"ro5 - Lipinski Rule of 5\n"
      +"usage: ro5 [options]\n"
      +"operations:\n"
      +"    -test ................................ \n"
      +"required:\n"
      +"    -i IFILE ............................. input molecules\n"
      +"\n"
      +"options:\n"
      +"    -vmax VMAX ........................... max violations ["+vmax+"]\n"
      +"    -v ................................... verbose\n"
      +"    -h ................................... this help\n"
      +"\n"
    );
    System.exit(1);
  }
  private static int verbose=0;
  private static boolean test=false;
  private static String ifile="";
  private static String ofile="";

  /////////////////////////////////////////////////////////////////////////////
  private static void ParseCommand(String args[])
  {
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) ifile=args[++i];
      else if (args[i].equals("-o")) ofile=args[++i];
      else if (args[i].equals("-vmax")) vmax=Integer.parseInt(args[++i]);
      else if (args[i].equals("-test")) test=true;
      else if (args[i].equals("-v")) verbose=1;
      else if (args[i].equals("-h")) Help("");
      else Help("Unknown option: "+args[i]);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  public static void main(String[] args) throws IOException,Exception
  {
    ParseCommand(args);
    if (ifile.length()==0) Help("Input file required.");
    Molecule mol = null;
    MolImporter molReader = new MolImporter(ifile);
    if (test)
    {
      Test(molReader, vmax, verbose);
    }
    else
    {
      Help("No operation specified.");
    }
    System.exit(0);
  }
}
