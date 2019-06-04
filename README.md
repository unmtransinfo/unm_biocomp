# BIOCOMP <img align="right" src="/doc/images/unm_new.png" height="80">

Libraries and web apps developed at the UNM Translational Informatics Division 
(formerly Biocomputing Division). The name _BIOCOMP_ reflects this proud history.

* Maven multi-module project.
* Modules: `unm_biocomp_convert`, `unm_biocomp_db`, `unm_biocomp_depict`, 
`unm_biocomp_fp`, `unm_biocomp_freechart`,
`unm_biocomp_jchemdb`, `unm_biocomp_mcs`, `unm_biocomp_molalign`, `unm_biocomp_molcloud`, 
`unm_biocomp_qed`, `unm_biocomp_react`, `unm_biocomp_ro5`, `unm_biocomp_sim2d`, 
`unm_biocomp_smarts`, `unm_biocomp_tautomer`, `unm_biocomp_threads`, `unm_biocomp_util`
* WAR produced deploys several TID web apps, including: Convert, Depict, MolCloud,
Ro5, Sim2D and SmartsFilter.

## Dependencies

* Java 8
* Maven 3.5+
* ChemAxon (19.3.0)
* ChemAxon (14.7.7.0)
* Access to [ChemAxon Maven repository](https://hub.chemaxon.com)
(see [documentation](https://docs.chemaxon.com/display/docs/Public+Repository))
  * Requires API key.
* Access to [Oracle Maven repository](https://https://maven.oracle.com)
  * Requires credentials.
* Access to [EBI Maven repository](http://www.ebi.ac.uk/intact/maven/nexus/content/repositories/ebi-repo/)
* `unm_biocomp_sasa`, `unm_biocomp_grouping`
* Many others, including CDK, Derby, MySql, PostgreSql, Oracle, VCCLAB, Freechart, 
many Apache and other open source libraries.

## Issues with repositories

* See configuration [example settings.xml](doc/settings.xml).
* Java/SSL-cert issues may result in error: `unable to find valid
certification path to requested target` requiring solutions such as:

```
openssl s_client -showcerts -connect www.ebi.ac.uk:443
```
_(Edit output, save root.crt and intermediate.crt.)_
```
sudo keytool -importcert -keystore /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts -storepass changeit -file root.crt -alias "ebi-root"
sudo keytool -importcert -keystore /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts -storepass changeit -file intermediate.crt -alias "ebi-intermediate"
```

## Compilation

```
mvn clean package
```

## Usage

Example commmands

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.molcloud.MCloud" -Dexec.args="-i test.smi -gui -v"
```
