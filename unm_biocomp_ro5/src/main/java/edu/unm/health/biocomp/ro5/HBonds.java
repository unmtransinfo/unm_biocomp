/**
 * 
 */
package edu.unm.health.biocomp.ro5;

import java.io.IOException;

import chemaxon.sss.search.SearchException;
import chemaxon.struc.Molecule;
import chemaxon.struc.MoleculeGraph;
import chemaxon.util.MolHandler;

/**
 * Computes HB donors & acceptors
 * addapted from TCP
 * @author Oleg Ursu
 *
 */
public class HBonds {

	public static final HBPattern[] DONORS = new HBPattern[] {
		// H-bond donor -- any OH which is not a part of a carboxylic or sulphonic acid
		new HBPattern("[OH1,oH1;!$([O,o]C=O);!$([O,o]S=O)]", 1),
		// H-bond donor -- any NH, NH2 or NH3+ group
		new HBPattern("[NH1,nH1]", 1),
		new HBPattern("[NH2,nH2]", 2),
		new HBPattern("[NH3,nH3]", 3)
		
	};
	
	public static final HBPattern[] ACCEPTORS = new HBPattern[] {
		/* H-bond acceptors                                 */
		/* ethers are allowed, while esters are not         */
		new HBPattern("[OH0,oH0;!$([O,o]C=O);!$([O,o]S=O)]", 1),
		/* alcohols and carboxylic or sulphonic acids        */
		new HBPattern("[OH1,oH1]", 1),
		/* amines are allowed, while amides and nitro/nitroso compounds are not */
		new HBPattern("[N,n;!$([N,n]C=O);!$([N,n]S=O);!$([N,n]=O);!$([N+]);$([n+])]", 1)
	};
	
	public static final int getDonors(Molecule mol) throws SearchException {
		int hbd = 0;
		int[][] matches = null;
		for(HBPattern dp : DONORS) {
			dp.pattern.setTarget(mol);
			matches = dp.pattern.findAll();
			if(matches != null) {
				hbd += matches.length * dp.factor;
			}
		}
		return hbd;
	}
	
	public static final int getAcceptors(Molecule mol) throws SearchException {
		int hba = 0;
		int[][] matches = null;
		for(HBPattern ap : ACCEPTORS) {
			ap.pattern.setTarget(mol);
			matches = ap.pattern.findAll();
			if(matches != null) {
				hba += matches.length * ap.factor;
			}
		}
		return hba;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException, SearchException {
		MolHandler mh = new MolHandler("CC(C)C[C@H](NC(=O)[C@H](CC1=CC=C(O)C=C1)NC(=O)[C@H](CC1=CNC=N1)NC(=O)[C@H](CCCNC(N)=N)NC(=O)C1=CC=CC=C1N)C(=O)N[C@@H](CC(N)=O)C(=O)N[C@@H](CC(C)C)C(=O)N[C@@H](C(C)C)C(=O)N[C@@H]([C@@H](C)O)C(=O)N[C@@H](CCCNC(N)=N)C(=O)N[C@@H](CCC(N)=O)C(=O)N[C@@H](CCCNC(N)=N)C(=O)N[C@@H](CC1=CC=C(O)C=C1)C(N)=O");
		mh.aromatize(MoleculeGraph.AROM_GENERAL);
		System.out.println(getDonors(mh.getMolecule()));
		System.out.println(getAcceptors(mh.getMolecule()));
	}

}
