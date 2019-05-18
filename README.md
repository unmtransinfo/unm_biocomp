# Biocomp

* Maven multi-module project.
* Modules: `unm_biocomp_convert`, `unm_biocomp_db`, `unm_biocomp_depict`, 
`unm_biocomp_fp`, `unm_biocomp_freechart`,
`unm_biocomp_jchemdb`, `unm_biocomp_mcs`, `unm_biocomp_molalign`, `unm_biocomp_molcloud`, 
`unm_biocomp_qed`, `unm_biocomp_react`, `unm_biocomp_ro5`, `unm_biocomp_sim2d`, 
`unm_biocomp_smarts`, `unm_biocomp_tautomer`, `unm_biocomp_threads`, `unm_biocomp_util`
* Modules developed by Oleg Ursu: `unm_biocomp_sasa`, `unm_biocomp_grouping`
* `unm_biocomp_http` is deprecated by merging into `unm_biocomp_util`.
* WAR produced deploys several TID web apps, including: Convert, Depict, MolCloud,
Ro5, Sim2D and SmartsFilter.

### ChemAxon repository

ChemAxon provides a Maven repository at <https://hub.chemaxon.com> and documentation for use at
<https://docs.chemaxon.com/display/docs/Public+Repository>. As the docs say: "To integrate a product, you only need to add its top-level module as dependency, all required modules
will be downloaded transitively (with the exception of the module naming which also needs to be added separately)." The top-level modules are `jchem-main` and `marvin-app`.
At this writing, I'm using version is 19.3.0.  An account and API key is required and may be configured via Maven settings.xml thus:

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd" xmlns="http://maven.apache.org/SETTINGS/1.1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <servers>
    <server>
      <username>[YOUR_HUB_USERNAME]</username>
      <password>[YOUR_HUB_API_KEY]</password>
      <id>chemaxon-hub</id>
    </server>
    <server>
      <username>[YOUR_HUB_USERNAME]</username>
      <password>[YOUR_HUB_API_KEY]</password>
      <id>snapshots</id>
    </server>
  </servers>
  <profiles>
    <profile>
      <repositories>
        <repository>
          <id>maven-repository</id>
          <name>Maven Repo, via HTTP</name>
          <url>http://repo1.maven.org/maven2/</url>
        </repository>
        <repository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
         <id>chemaxon-hub</id>
          <name>libs-release</name>
          <url>https://hub.chemaxon.com/artifactory/libs-release</url>
        </repository>
        <repository>
          <snapshots />
          <id>snapshots</id>
          <name>libs-snapshot</name>
          <url>https://hub.chemaxon.com/artifactory/libs-snapshot</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
          <id>chemaxon-hub</id>
          <name>plugins-release</name>
          <url>https://hub.chemaxon.com/artifactory/plugins-release</url>
        </pluginRepository>
        <pluginRepository>
          <snapshots />
          <id>snapshots</id>
          <name>plugins-release</name>
          <url>https://hub.chemaxon.com/artifactory/plugins-release</url>
        </pluginRepository>
      </pluginRepositories>
      <id>artifactory</id>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>artifactory</activeProfile>
  </activeProfiles>
</settings>
```

## Demo commands

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.molcloud.MCloud" -Dexec.args="-i test.smi -gui -v"
```
