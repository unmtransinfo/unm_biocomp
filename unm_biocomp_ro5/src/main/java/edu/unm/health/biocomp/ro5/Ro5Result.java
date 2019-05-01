package edu.unm.health.biocomp.ro5;

import java.io.*;
import java.util.*;

/**	For Ro5 results storage.
*/
public class Ro5Result
{
  public int i;
  private String smiles;
  private String name;
  private Integer hbd;
  private Integer hba;
  private Double mwt;
  private Float logp;
  public Ro5Result() { }
  public boolean isViolationHbd()  { return  (this.hbd==null ||  this.hbd>5); }
  public boolean isViolationHba()  { return  (this.hba==null ||  this.hba>10); }
  public boolean isViolationMwt()  { return  (this.mwt==null ||  this.mwt>500.0f); }
  public boolean isViolationLogp() { return (this.logp==null || this.logp>5.0f); }
  public int violations()
  {
    int v=0;
    if (isViolationHbd()) ++v;
    if (isViolationHba()) ++v;
    if (isViolationMwt()) ++v;
    if (isViolationLogp()) ++v;
    return v;
  }
  public Integer getHbd() { return this.hbd; }
  public void setHbd(Integer _hbd) { this.hbd=_hbd; }
  public Integer getHba() { return this.hba; }
  public void setHba(Integer _hba) { this.hba=_hba; }
  public Double getMwt() { return this.mwt; }
  public void setMwt(double _mwt) { this.mwt=_mwt; }
  public Float getLogp() { return this.logp; }
  public void setLogp(Float _logp) { this.logp=_logp; }
  public String getName() { return this.name; }
  public void setName(String _name) { this.name=_name; }
  public String getSmiles() { return this.smiles; }
  public void setSmiles(String _smiles) { this.smiles=_smiles; }
}
