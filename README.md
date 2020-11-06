# `UNM_BIOCOMP` <img align="right" src="/doc/images/unm_new.png" height="80">

Libraries and web apps developed at the UNM Translational Informatics Division
(formerly Biocomputing Division). The name _UNM\_BIOCOMP_ reflects this provenance.

* Maven multi-module project.
* Modules
  * (JARs): `unm_biocomp_convert`, `unm_biocomp_fp`, `unm_biocomp_freechart`,
`unm_biocomp_jchemdb`, `unm_biocomp_mcs`, `unm_biocomp_molcloud`,
`unm_biocomp_qed`, `unm_biocomp_react`, `unm_biocomp_ro5`, `unm_biocomp_sim2d`,
`unm_biocomp_tautomer`
  * (WAR) `biocomp_war` deploys several web apps, including: Convert, Depict, MolCloud,
Ro5, Sim2D and SmartsFilter.

## Dependencies

* Java 8
* Maven 3.5+
* ChemAxon 19.3.0+
* Access to [ChemAxon Maven repository](https://hub.chemaxon.com)
(see [documentation](https://docs.chemaxon.com/display/docs/Public+Repository))
  * Requires API key.
* Access to [Oracle Maven repository](https://https://maven.oracle.com)
  * Requires credentials, obtained via maven.oracle.com registration and
license agreement acceptance.
* Access to [EBI Maven repository](http://www.ebi.ac.uk/intact/maven/nexus/content/repositories/ebi-repo/)
* Separate <https://github.com/unmtransinfo/> repos:
  * [`unm_biocomp_util`](https://github.com/unmtransinfo/unm_biocomp_util),
[`unm_biocomp_text`](https://github.com/unmtransinfo/unm_biocomp_text),
[`unm_biocomp_smarts`](https://github.com/unmtransinfo/unm_biocomp_smarts),
[`unm_biocomp_depict`](https://github.com/unmtransinfo/unm_biocomp_depict),
[`unm_biocomp_hscaf`](https://github.com/unmtransinfo/unm_biocomp_hscaf),
[`unm_biocomp_sasa`](https://github.com/unmtransinfo/unm_biocomp_sasa),
[`unm_biocomp_grouping`](https://github.com/unmtransinfo/unm_biocomp_grouping),
[`unm_biocomp_cdk`](https://github.com/unmtransinfo/unm_biocomp_cdk),
[`unm_biocomp_biobyte`](https://github.com/unmtransinfo/unm_biocomp_biobyte),
[`unm_biocomp_vcclab`](https://github.com/unmtransinfo/unm_biocomp_vcclab),
[`unm_biocomp_descriptors`](https://github.com/unmtransinfo/unm_biocomp_descriptors)
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

MacOSX:
```
sudo keytool -importcert -keystore /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts -storepass changeit -file root.crt -alias "ebi-root"
sudo keytool -importcert -keystore /Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/security/cacerts -storepass changeit -file intermediate.crt -alias "ebi-intermediate"
```

Ubuntu:
```
sudo keytool -importcert -cacerts -storepass changeit -file root.crt -alias "ebi-root"
sudo keytool -importcert -cacerts -storepass changeit -file intermediate.crt -alias "ebi-intermediate"
```

## Compilation

* See [README_REPOS.md](doc/README_REPOS.md) for cloning and compiling dependency repos.

```
mvn clean install
```

## Deploying `BIOCOMP_WAR`

Ok for Tomcat v8/v9 also, apparently.

Copy your ChemAxon license to `/biocomp_war/src/main/webapp/.chemaxon/license.cxl` 
for inclusion in the WAR.

```
mvn --projects biocomp_war tomcat7:deploy
```

or

```
mvn --projects biocomp_war tomcat7:redeploy
```

## Testing with Jetty

<http://localhost:8080/convert>, etc.

```
mvn --projects biocomp_war jetty:run
```

## Usage

See example commmands in [README_APPS.md](doc/README_APPS.md)

## Docker

* [Dockerfile](Dockerfile)
* [Go\_DockerBuild.sh](sh/Go_DockerBuild.sh)
* [Go\_DockerRun.sh](sh/Go_DockerRun.sh)
* From Docker engine host, applications accessible via <http://localhost:9095/biocomp/>.
* ChemAxon license must be copied into running container.
