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
* ChemAxon JChem (19.3.0 ok)
* `unm_biocomp_sasa`, `unm_biocomp_grouping`
* Many others, including CDK, Derby, MySql, PostgreSql, Oracle, VCCLAB, Freechart, 
many Apache and other open source libraries.

## ChemAxon repository

ChemAxon provides a Maven repository at <https://hub.chemaxon.com> and documentation for use at
<https://docs.chemaxon.com/display/docs/Public+Repository>. As the docs say: "To integrate a product,
you only need to add its top-level module as dependency, all required modules will be downloaded transitively
(with the exception of the module naming which also needs to be added separately)." The top-level modules are
`jchem-main` and `marvin-app`. At this writing, we are using version 19.3.0 but also for some legacy
dependencies version 14.7.7.0 which appears to be the earliest version available from the repository. 
An account and API key is required and may be configured as in this [example Maven
settings.xml](doc/settings.xml).

## Compilation

```
mvn clean package
```

## Usage

Example commmands

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.molcloud.MCloud" -Dexec.args="-i test.smi -gui -v"
```
