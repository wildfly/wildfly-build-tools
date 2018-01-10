WildFly Build Tools
========================

This project contains Maven plugins and related tools which are used for
building WildFly.  The **feature-pack-maven-plugin** is used to create
a lightweight artifact that contains a complete description of a set of 
server features.  The **wildfly-server-provisioning-maven-plugin** is used to 
provision the servers creating the full distribution.

## Feature Packs

The wildfly build process consists of first generating a WildFly 'feature pack',
which is a lightweight artifact that contains a complete description of a set 
of server features. These feature packs can be provisioned into full servers, 
or used as dependencies for other feature packs. In general the build process 
will first create a feature pack using the **wildfly-feature-pack-build-maven-plugin**, 
and then use the **wildfly-server-provisioning-maven-plugin** to provision 'thin' and 
'fat' servers into the *build* and *dist* directories respectively.

Feature packs are assembled from a *feature-pack-build.xml* file, the schema of which 
can be found in the sources: 
https://github.com/wildfly/wildfly-build-tools/blob/master/feature-pack-build-maven-plugin/src/main/resources/

In order to build a feature pack it is necessary to create three directories in the
*src/main/resources* folder:

**modules**: contains module.xml files. These files should not use <resource> references, 
but should instead use references of the form: 
`<artifact name="${org.hibernate:hibernate-core}"/>`.These artifact references will
be replaced by concrete references to a specific version or a <resource> reference
when the server is provisioned.  This makes it possible to override the version of
specific artifacts at provisioning time.  In addition, version numbers can be automatically
added to module descriptors of version 1.6 or later by adding a version attribute whose
argument is either a fixed version string or one of the given artifact references.
The version number will appear on stack traces on Java 9 and later.

**content**: contains files that are copied into the server.

**configuration**: contains configuration templates that are used to generate server configuration files.


## Usage

The plugins are configured in the "plugins" section of the pom.

```xml
<plugins>
  <plugin>
    <groupId>org.wildfly.build</groupId>
    <artifactId>wildfly-feature-pack-build-maven-plugin</artifactId>
    <executions>
      <execution>
        <id>feature-pack-build</id>
        <goals>
          <goal>build</goal>
        </goals>
        <phase>compile</phase>
        <configuration>
          <config-file>feature-pack-build.xml</config-file>
        </configuration>
      </execution>
    </executions>
  </plugin>
</plugins>
```

```xml
<plugins>
  <plugin>
    <groupId>org.wildfly.build</groupId>
    <artifactId>wildfly-server-provisioning-maven-plugin</artifactId>
    <executions>
      <execution>
        <id>server-provisioning</id>
        <goals>
          <goal>build</goal>
        </goals>
        <phase>compile</phase>
        <configuration>
          <config-file>server-provisioning.xml</config-file>
        </configuration>
      </execution>
    </executions>
  </plugin>
</plugins>
```

### Config Parameters

Most of the configuration for each plugin is contained in a separate configuration
file defined by the parameter *config-file*.

The schema for the config file for the **wildfly-feature-pack-build-maven-plugin** can be found in the sources here:
https://github.com/wildfly/wildfly-build-tools/blob/master/feature-pack-build-maven-plugin/src/main/resources/

The schema for the config file for the **wildfly-server-provisioning-maven-plugin** can
be found in the sources here:
https://github.com/wildfly/wildfly-build-tools/tree/master/provisioning/src/main/resources


### Example Config Files

Some example configurations can be found in the wildfly-core and wildfly sources:

* https://github.com/wildfly/wildfly-core/blob/master/core-feature-pack/feature-pack-build.xml
* https://github.com/wildfly/wildfly-core/blob/master/dist/server-provisioning.xml
* https://github.com/wildfly/wildfly/blob/master/feature-pack/feature-pack-build.xml
* https://github.com/wildfly/wildfly/blob/master/dist/server-provisioning.xml

#### Provisioning configuration attributes
* *copy-module-artifacts* - whether should be WildFly modules resources (JARs) copied from artifacts into `WILDFLY_HOME/modules`, next to their `module.xml`.
* *extract-schemas* - whether should be XSD files extracted from WildFly modules artifacts into `WILDFLY_HOME/docs/schema`
* *extract-schemas-groups* - groupId of artifacts, from which should be XSD files extracted (delimited by space)


#### Example Server Provisioning Filter

When provisioning a server from a feature pack, it's can be useful to exclude certain
files from the resulting distribution.  This can be done using the <filter/> config.
The following example will exclude the files "copyright.txt" and "README.txt" and
also the directory "docs/contrib".

```xml
<server-provisioning xmlns="urn:wildfly:server-provisioning:1.1" extract-schemas="true" copy-module-artifacts="true"
                     extract-schemas-groups="org.jboss.as org.wildfly org.wildfly.core org.jboss.metadata">
  <feature-packs>
    <feature-pack groupId="org.wildfly" artifactId="wildfly-feature-pack" version="${project.version}">
      <contents>
        <filter pattern="copyright.txt" include="false"/>
        <filter pattern="README.txt" include="false"/>
        <filter pattern="docs/contrib/*" include="false"/>
      </contents>
    </feature-pack>
  </feature-packs>
</server-provisioning>
```
