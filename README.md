# Biocomp

Maven-ization of lobo repository.  Work in progress.


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
      <id>central-chemaxon</id>
    </server>
    <server>
      <username>[your_hub_user]</username>
      <password>[your_hub_API_key]</password>
      <id>snapshots</id>
    </server>
  </servers>

  <mirrors>
    <mirror>
      <id>maven-repository</id>
      <name>Maven Repo, via HTTP</name>
      <url>http://repo1.maven.org/maven2/</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>

  <profiles>
    <profile>
      <repositories>
        <repository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
         <id>central-chemaxon</id>
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
          <id>central-chemaxon</id>
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
