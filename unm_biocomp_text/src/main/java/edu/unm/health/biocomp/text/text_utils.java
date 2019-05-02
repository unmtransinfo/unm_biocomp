package edu.unm.health.biocomp.text;

import java.io.*;
import java.util.*;


/**	Static methods for textual analysis including similarity.
	todo: [ ] in-memory dictionary for fast[er] D-L comparison

	@author	Jeremy Yang
	@see	DamerauLevenshtein
*/
public class text_utils
{
   public static void main(String [] args)
   {
     if (args.length < 2)
     {
       System.err.println("usage: text_utils <string1> <string2>");
       System.exit(1);
     }

     String s1 = args[0];
     String s2 = args[1];
     DamerauLevenshtein damlev = new DamerauLevenshtein(s1,s2);

     System.err.println(String.format("sim = %d",damlev.getSimilarity()));
   }
}
