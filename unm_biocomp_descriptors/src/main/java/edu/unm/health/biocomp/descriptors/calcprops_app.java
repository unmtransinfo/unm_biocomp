package edu.unm.health.biocomp.descriptors;

import java.io.*;
import java.util.*;
import chemaxon.formats.*;
import chemaxon.struc.*;
import chemaxon.util.MolHandler;
import chemaxon.sss.search.SearchException;

import chemaxon.marvin.plugin.*; //CalculatorPlugin, PluginException
import chemaxon.marvin.calculations.*; //logPPlugin,logDPlugin,pKaPlugin,TPSAPlugin,MSAPlugin,RefractivityPlugin,PolarizabilityPlugin

import com.sunset.*;

/**	Calculates many properties using the ChemAxon calculator plugins.
	<br/>
	Plugins employed:
	<ul>
	<li>logPPlugin 
	<li>logDPlugin 
	<li>pKaPlugin; 
	<li>TPSAPlugin 
	<li>MSAPlugin 
	<li>RefractivityPlugin 
	<li>PolarizabilityPlugin 
	</ul>
	<br/>
	refs:<ol>
	<li> logP ref: Viswanadhan et al., J.Chem.Inf.Comput.Sci. 1989, 29, 163-172.
	<li> TPSA,MSA ref: Ertl, et al., J.Med.Chem. 2000, 43, 3714-3717
	<li> "Rapid Evaluation of Synthetic and Molecular Complexity for in
	    Silico Chemistry", Tharun Kumar Allu, Tudor I. Oprea
	    J. Chem. Inf. Model. 2005, 45, 1237-1243.  
	</ol>
	<br/>
	@author Jeremy Yang
*/
public class calcprops_app
{
  private static void Help(String msg)
  {
    if (!msg.equals("")) System.err.println(msg);
    System.err.println(
      "usage: calcprops\n"+
      "  required:\n"+
      "          -i <in_mol_file>\n"+
      "          -props prop1[,prop2...]\n"+
      "    props: logP|logD|pKa|TPSA|MSA_VDW|MSA_SOL|MR|MP|SMCM\n"+
      "  options:\n"+
      "          -o <out_mol_file>\n"+
      "          -ifmt <fmt_spec>\n"+
      "          -ofmt <fmt_spec>\n"+
      "          -v     ... verbose\n"+
      "          -vv     ... very verbose\n"+
      "  logD/pKa/TPSA options:\n"+
      "          -pH\n"+
      "          -pHLower\n"+
      "          -pHUpper\n"+
      "          -pHStep\n"+
      "\n"+
      "     logP = log octanol-water partition coefficient\n"+
      "     logD = log octanol-water distribution coefficient\n"+
      "      pKa = acid dissociation constant\n"+
      "     TPSA = total polar surface area \n"+
      "  MSA_VDW = total surface area, Vanderwaals\n"+
      "  MSA_SOL = total surface area, solvent-accessible\n"+
      "       MR = molecular refractivity\n"+
      "       MP = molecular polarizability\n"
    );
    System.exit(1);
  }

  public static void main(String[] args)
    throws IOException,chemaxon.marvin.plugin.PluginException,SearchException 
  {
    if (args.length==0) Help("");
    String ifile="";
    String ofile=null;
    String ifmt="";
    String ofmt=null;
    String props="";
    float pH=7.0f;
    float pHLower=0.0f;
    float pHUpper=0.0f;
    float pHStep=0.0f;
    int verbose=0;
    for (int i=0;i<args.length;++i)
    {
      if (args[i].equals("-i")) { ifile=args[++i]; }
      else if (args[i].equals("-o")) { ofile=args[++i]; }
      else if (args[i].equals("-ifmt")) { ifmt=args[++i]; }
      else if (args[i].equals("-ofmt")) { ofmt=args[++i]; }
      else if (args[i].equals("-props")) { props=args[++i]; }
      else if (args[i].equals("-pH")) { pH=Float.parseFloat(args[++i]); }
      else if (args[i].equals("-pHLower")) { pHLower=Float.parseFloat(args[++i]); }
      else if (args[i].equals("-pHUpper")) { pHUpper=Float.parseFloat(args[++i]); }
      else if (args[i].equals("-pHStep")) { pHStep=Float.parseFloat(args[++i]); }
      else if (args[i].equals("-v")) { verbose=1; }
      else if (args[i].equals("-vv")) { verbose=2; }
      else {
        Help("illegal option: "+args[i]);
      }
    }
    if (ifile.equals("")) Help("-i required");
    if (props.equals("")) Help("-props required");

    MolImporter molReader;
    if (ifile.equals("-"))
    {
      if (ifmt.equals("")) Help("-ifmt required with \"-i -\"");
      molReader=new MolImporter(System.in,ifmt);
    }
    else
    {
      molReader=new MolImporter(ifile);
    }

    MolExporter molWriter=null;
    if (ofile!=null)
    {
      if (ofile.equals("-"))
      {
        if (ofmt==null) Help("-ofmt required with \"-o -\"");
        molWriter=new MolExporter(System.out,ofmt);
      }
      else
      {
        if (ofmt==null)
          ofmt=MFileFormatUtil.getMostLikelyMolFormat(ofile);
        molWriter=new MolExporter(new FileOutputStream(ofile),ofmt);
      }
    }

    logPPlugin plugin_logP=null;
    logDPlugin plugin_logD=null;
    pKaPlugin plugin_pKa=null;
    TPSAPlugin plugin_TPSA=null;
    MSAPlugin plugin_MSA_VDW=null;
    MSAPlugin plugin_MSA_SOL=null;
    RefractivityPlugin plugin_MR=null;
    PolarizabilityPlugin plugin_MP=null;
    //SolubilityPlugin plugin_logS=null;

    // Configure calculator plugins:
    String[] proplist=props.split(",");
    for (String prop: proplist)
    {

      if (prop.equalsIgnoreCase("logP"))
      {
        plugin_logP=new logPPlugin();
        plugin_logP.setUserTypes("logPTrue,logPMicro,logPNonionic,logDpI,increments");
      }
      else if (prop.equalsIgnoreCase("logD"))
      {
        plugin_logD=new logDPlugin();
        if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
        {
          plugin_logD.setpHLower(pHLower);
          plugin_logD.setpHUpper(pHUpper);
          plugin_logD.setpHStep(pHStep);
        }
        else
        {
          plugin_logD.setpH(pH);
        }
        plugin_logD.setCloridIonConcentration(0.2);
        plugin_logD.setNaKIonConcentration(0.2);
      }
      else if (prop.equalsIgnoreCase("pKa"))
      {
        plugin_pKa=new pKaPlugin();
        if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
        {
          plugin_pKa.setpHLower(pHLower);
          plugin_pKa.setpHUpper(pHUpper);
          plugin_pKa.setpHStep(pHStep);
        }
        else
        {
          plugin_pKa.setpH(pH);
        }
        plugin_pKa.setMaxIons(6);
        plugin_pKa.setBasicpKaLowerLimit(-5.0);
        plugin_pKa.setAcidicpKaUpperLimit(25.0);
      }
      else if (prop.equalsIgnoreCase("TPSA"))
      {
        plugin_TPSA=new TPSAPlugin();
        plugin_TPSA.setpH(pH);
      }
      else if (prop.equalsIgnoreCase("MSA_VDW"))
      {
        plugin_MSA_VDW=new MSAPlugin();
        plugin_MSA_VDW.setpH(pH);
        plugin_MSA_VDW.setSurfaceAreaType(MSAPlugin.VAN_DER_WAALS);
      }
      else if (prop.equalsIgnoreCase("MSA_SOL"))
      {
        plugin_MSA_SOL=new MSAPlugin();
        plugin_MSA_SOL.setpH(pH);
        plugin_MSA_SOL.setSurfaceAreaType(MSAPlugin.SOLVENT);
      }
      else if (prop.equalsIgnoreCase("MR"))
      {
        plugin_MR=new RefractivityPlugin();
      }
      else if (prop.equalsIgnoreCase("MP"))
      {
        plugin_MP=new PolarizabilityPlugin();
        plugin_MP.setpH(pH);
      }

//    else if (prop.equalsIgnoreCase("logS"))
//    {
//      plugin_logS=new SolubilityPlugin();
//      if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
//      {
//        plugin_logS.setpHLower(pHLower);
//        plugin_logS.setpHUpper(pHUpper);
//        plugin_logS.setpHStep(pHStep);
//      }
//      else
//      {
//        plugin_logS.setpH(pH);
//      }
//    }
    }

    Molecule mol;
    while ((mol=molReader.read())!=null)
    {
      System.err.println(mol.getName()+":");
     
      for (String prop: proplist)
      {
        if (prop.equalsIgnoreCase("logP"))
        {
          plugin_logP.setMolecule(mol);
          plugin_logP.run();

          // get the overall logP value
          double logp=plugin_logP.getlogPMicro();	//logP of input
          double logpn=plugin_logP.getlogPNonionic(); 	//logP of neutral
          double logdpi=plugin_logP.getlogDpI(); 	//logD at pI
          double logpt=plugin_logP.getlogPTrue(); 	//typical from above
  
          System.err.println(String.format("logP: %.2f",logp));
          if (verbose>0)
          {
            System.err.println(String.format("Nonionic logP : %.2f",logpn));
            System.err.println(String.format("logD at pI : %.2f",logdpi));
            System.err.println(String.format("True logP : %.2f",logpt));
          }
          mol.setProperty("logP",String.format("%.2f",logp));
          if (verbose>1)
          {
            // get the incremental values
            int count=mol.getAtomCount();
            double[] increments=new double[count];
            for (int i=0;i<count;++i)
            {
              increments[i]=plugin_logP.getAtomlogPIncrement(i);
            }
            System.err.println("logP increments: ");
            for (int i=0;i<count;++i)
              System.err.print(String.format("%.2f;",increments[i]));
            System.err.println();
          }
        }
        else if (prop.equalsIgnoreCase("logD"))
        {
          plugin_logD.setMolecule(mol);
          plugin_logD.run();

          if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
          {
            double[] pHs=plugin_logD.getpHs();
            double[] logDs=plugin_logD.getlogDs();
  
            // get and print logD values
            String buff="";
            for (int i=0;i< pHs.length;++i)
            {
              System.err.println(String.format("%.2f,%.2f",logDs[i],pHs[i]));
              if (i>0) buff+="\n";
              buff+=(String.format("%.2f,%.2f",logDs[i],pHs[i]));
            }
            mol.setProperty("logD,pH",buff);
          }
          else
          {
            double logD=plugin_logD.getlogD();
            System.err.println(String.format("logD: %.2f",logD));
            mol.setProperty("logD",String.format("%.2f",logD));
          }
        }
        else if (prop.equalsIgnoreCase("pKa"))
        {
          plugin_pKa.setMolecule(mol);
          plugin_pKa.run();

          if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
          {
            // get the 3 strongest ACIDIC pKa values
            double[] acidicpKa=new double[3];
            int[] acidicIndexes=new int[3];
            plugin_pKa.getMacropKaValues(pKaPlugin.ACIDIC,acidicpKa,acidicIndexes);
            System.err.print("3 strongest ACIDIC pKa values: ");
            System.err.println(String.format("%.2f,%.2f,%.2f",
              acidicpKa[0],acidicpKa[1],acidicpKa[2]));
            mol.setProperty("3_acidic_pkas",String.format("%.2f,%.2f,%.2f",
              acidicpKa[0],acidicpKa[1],acidicpKa[2]));
  
            // get the 3 strongest BASIC pKa values
            double[] basicpKa=new double[3];
            int[] basicIndexes=new int[3];
            plugin_pKa.getMacropKaValues(pKaPlugin.BASIC,basicpKa,basicIndexes);
            System.err.print("3 strongest BASIC pKa values: ");
            System.err.println(String.format("%.2f,%.2f,%.2f",
              basicpKa[0],basicpKa[1],basicpKa[2]));
            mol.setProperty("3_basic_pkas",String.format("%.2f,%.2f,%.2f",
              basicpKa[0],basicpKa[1],basicpKa[2]));
  
            if (verbose>0)
            {
              // get pKa values for each atom
              int count=mol.getAtomCount();
              System.err.println("Atom pKa values:");
              for (int i=0;i<count;++i)
              {
                MolAtom atom=mol.getAtom(i);
                System.err.print(String.format("\t%s%d: ",atom.getSymbol(),i+1));
                // get ACIDIC and BASIC pKa values
                double[] apkas=plugin_pKa.getpKaValues(i,pKaPlugin.ACIDIC);
                double[] bpkas=plugin_pKa.getpKaValues(i,pKaPlugin.BASIC);
                if (apkas!=null)
                {
                  System.err.print("acidic: ");
                  for (int j=0;j<apkas.length;++j)
                  {
                    if (j>0) System.err.print(",");
                    System.err.print(String.format("%.2f",apkas[j]));
                  }
                }
                if (bpkas!=null)
                {
                  System.err.print("  basic: ");
                  for (int j=0;j<bpkas.length;++j)
                  {
                    if (j>0) System.err.print(",");
                    System.err.print(String.format("%.2f",bpkas[j]));
                  }
                }
                System.err.println("");
              }
  
              // get microspecies distributions
              double[] pHs=plugin_pKa.getpHs(); // pH values
              int mscount=plugin_pKa.getMsCount();
              System.err.println("Microspecies distributions:");
              for (int i=0;i<mscount;++i)
              {
                Molecule ms=plugin_pKa.getMsMolecule(i);
                String smiles=ms.exportToFormat("smiles:u");
                System.err.println(smiles);
                double[] distr=plugin_pKa.getMsDistribution(i);
                for (int j=0;j<distr.length;++j)
                {
                  System.err.println(String.format("\t%.2f",distr[j]));
                }
              }
            }
          }
          else
          {
            // get microspecies data (molecule and distribution)
            int count=plugin_pKa.getMsCount();
            for (int i=0;i<count;++i)
            {
              Molecule ms=plugin_pKa.getMsMolecule(i);
              String smiles=ms.exportToFormat("smiles:u");
              System.err.println("Microspecies distribution:");
              System.err.println(smiles);
              double distr=plugin_pKa.getSingleMsDistribution(i);
              System.err.println(String.format("\t%.2f",distr));
            }
          }
        }
        else if (prop.equalsIgnoreCase("TPSA"))
        {
          plugin_TPSA.setMolecule(mol);
          plugin_TPSA.run();

          double tpsa=plugin_TPSA.getSurfaceArea();
          System.err.println(String.format("TPSA: %.2f",tpsa));
          mol.setProperty("TPSA",String.format("%.2f",tpsa));
        }
        else if (prop.equalsIgnoreCase("MSA_VDW"))
        {
          plugin_MSA_VDW.setMolecule(mol);
          plugin_MSA_VDW.run();

          double msa_vdw=plugin_MSA_VDW.getSurfaceArea();
          System.err.println(String.format("MSA_VDW: %.2f",msa_vdw));
          mol.setProperty("MSA_VDW",String.format("%.2f",msa_vdw));
        }
        else if (prop.equalsIgnoreCase("MSA_SOL"))
        {
          plugin_MSA_SOL.setMolecule(mol);
          plugin_MSA_SOL.run();

          double msa_sol=plugin_MSA_SOL.getSurfaceArea();
          System.err.println(String.format("MSA_SOL: %.2f",msa_sol));
          mol.setProperty("MSA_SOL",String.format("%.2f",msa_sol));
        }
        else if (prop.equalsIgnoreCase("MR"))
        {
          plugin_MR.setMolecule(mol);
          plugin_MR.run();

          double mr=plugin_MR.getRefractivity();
          System.err.println(String.format("MR: %.2f",mr));
          mol.setProperty("MR",String.format("%.2f",mr));
        }
        else if (prop.equalsIgnoreCase("MP"))
        {
          plugin_MP.setMolecule(mol);
          plugin_MP.run();

          double mp=plugin_MP.getMolPolarizability();
          System.err.println(String.format("MP: %.2f",mp));
          mol.setProperty("MP",String.format("%.2f",mp));
        }
        else if (prop.equalsIgnoreCase("SMCM"))
        {
          double smcm=Complexity.complexity(mol);
          System.err.println(String.format("SMCM: %.2f",smcm));
          mol.setProperty("mol_smcm",String.format("%.2f",smcm));
        }

/////////////////////////////////
//      else if (prop.equalsIgnoreCase("logS"))
//      {
//        plugin_logS.setMolecule(mol);
//        plugin_logS.run();
//
//        if (pHLower!=0.0f && pHUpper!=0.0f && pHStep!=0.0f)
//        {
//          double[] pHs = plugin_logS.getpHs();
//          double[] logSs = plugin_logS.getlogSs();
//          String buff="";
//          for (int i=0;i<pHs.length;++i)
//          {
//            System.err.println(String.format("%.2f,%.2f",logSs[i],pHs[i]));
//            if (i>0) buff+="\n";
//            buff+=String.format("%.2f,%.2f",logSs[i],pHs[i]);
//          }
//          mol.setProperty("logS,pH",buff);
//        }
//        else
//        {
//          double logS=plugin_logS.getlogS();
//          System.err.println(String.format("logS,pH: %.2f,%.2f",logS,pH));
//          mol.setProperty("logS",String.format("%.2f",logS));
//        }
//      }
/////////////////////////////////

      }

      if (molWriter!=null)
      {
        molWriter.write(mol);
      }
    }
    System.exit(0);
  }
}

