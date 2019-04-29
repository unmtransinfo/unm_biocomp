package edu.unm.health.biocomp.util;

/**	Static methods for file processing.
	@author Jeremy J Yang
*/
public class file_utils
{
  private file_utils() {} //disable default constructor
  /////////////////////////////////////////////////////////////////////////////
  /**	Human readable file size.
  */
  public static String NiceBytes(long bytes)
  {
    if (bytes<1.0e3)
      return String.format("%dB",bytes);
    else if (bytes<1.0e6)
      return String.format("%.1fKB",((float)bytes/1.0e3));
    else if (bytes<1.0e9)
      return String.format("%.1fMB",((float)bytes/1.0e6));
    else if (bytes<1.0e12)
      return String.format("%.1fGB",((double)bytes/1.0e9));
    else
      return String.format("%.1fTB",((double)bytes/1.0e12));
  }
}
