# Biocomp

Maven-ization of lobo repository. 


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
      <username>[your_hub_user]</username>
      <password>[your_hub_API_key]</password>
      <id>chemaxon-hub</id>
    </server>
    <server>
      <username>[your_hub_user]</username>
      <password>[your_hub_API_key]</password>
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

### Local repository (_TO BE IMPROVED_)

Currently two internal dependencies must be manually installed in a local Maven
repository thus:

```
mvn install:install-file -Dfile=grouping.jar -DgroupId=olegursu -DartifactId=grouping -Dversion=2018 -Dpackaging=jar -DlocalRepositoryPath=/var/www/html/.m2/
mvn install:install-file -Dfile=sasa.jar -DgroupId=olegursu -DartifactId=sasa -Dversion=2018 -Dpackaging=jar -DlocalRepositoryPath=/var/www/html/.m2/
```

