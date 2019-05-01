/**
 * 
 */
package edu.unm.health.biocomp.ro5;

import java.io.IOException;

import chemaxon.formats.MolImporter;
import chemaxon.sss.search.MolSearch;

/**
 * @author Oleg Ursu
 *
 */
public class Pattern {
		
	public String smarts;
	public MolSearch pattern;
	
	public Pattern(String smarts) {
		this.smarts = smarts;
		try {
			this.pattern = new MolSearch();
			this.pattern.setQuery(MolImporter.importMol(smarts, "smarts:d"));
		} catch(IOException ex) {
			return;
		}
	}
	
}
