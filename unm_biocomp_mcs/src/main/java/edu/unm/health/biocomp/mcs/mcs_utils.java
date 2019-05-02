package edu.unm.health.biocomp.mcs;

import java.io.*;
import java.text.*;
import java.util.*;

import chemaxon.formats.*;
import chemaxon.struc.*;
import chemaxon.sss.search.SearchException;
//import chemaxon.sss.search.MCES; //JChem 5.8.3
import chemaxon.jchem.version.*;
import com.chemaxon.search.mcs.*; //MaxCommonSubstructure,McsSearchOptions,RingHandlingMode,SearchMode,TerminationCause


/**	Utility methods for Max Common Substructure processing.

	NOTE: MCES class discontinued between JChem 5.8.3 and 6.3.1,
	replaced by com.chemaxon.search.mcs.MaxCommonSubstructure.
*/
public class mcs_utils
{
  /**	Conditions for MMP (both molecules):
	  (1) non-common atoms comprise connected substructure
	  (2) substructure not partial ring
	  (3) substructure &lt;= 25% of heavy atoms
  */
  public static boolean isMatchedPair(MaxCommonSubstructure mcs,McsSearchResult mcs_result)
  {
    Molecule qmol = mcs.getQuery();
    for (MolAtom atom: qmol.getAtomArray()) atom.setSetSeq(0);
    for (Integer i: mcs_result.getMatchedQueryAtoms()) qmol.getAtom(i).setSetSeq(1);

    Molecule tmol = mcs.getTarget();
    for (MolAtom atom: tmol.getAtomArray()) atom.setSetSeq(0);
    for (Integer i: mcs_result.getMatchedTargetAtoms()) tmol.getAtom(i).setSetSeq(1);

    //Molecule qmol_unmatched = qmol.cloneMolecule(); //Deprecated as of Marvin 2014.09.01.0.
    Molecule qmol_unmatched = qmol.clone();
    //Molecule tmol_unmatched = tmol.cloneMolecule(); //Deprecated as of Marvin 2014.09.01.0.
    Molecule tmol_unmatched = tmol.clone();

    for (MolAtom atom: qmol_unmatched.getAtomArray())
    {
      if (atom.getSetSeq()==1)
        qmol_unmatched.removeAtom(atom,MoleculeGraph.RMCLEANUP_EDGES);
    }
    //int fragcount = qmol_unmatched.getFragCount(); //Deprecated as of Marvin 5.6.
    int qmol_fragcount = qmol_unmatched.getFragCount(MoleculeGraph.FRAG_KEEPING_MULTICENTERS);
    if (qmol_fragcount>1)
    {
      return false;
    }
    if (qmol_unmatched.getAtomCount()*4 > qmol.getAtomCount())
    {
      return false;
    }
    for (MolAtom atom: tmol_unmatched.getAtomArray())
    {
      if (atom.getSetSeq()==1)
        tmol_unmatched.removeAtom(atom,MoleculeGraph.RMCLEANUP_EDGES);
    }
    //int fragcount = tmol_unmatched.getFragCount(); //Deprecated as of Marvin 5.6.
    int tmol_fragcount = tmol_unmatched.getFragCount(MoleculeGraph.FRAG_KEEPING_MULTICENTERS);
    if (tmol_fragcount>1)
    {
      return false;
    }
    if (tmol_unmatched.getAtomCount()*4 > tmol.getAtomCount())
    {
      return false;
    }
    return true;
  }
}
