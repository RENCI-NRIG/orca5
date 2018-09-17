ORCA is configured to release artifacts to [The Central Repository](http://central.sonatype.org/) Open Source Software Repository Hosting (OSSRH).

## Releasing Artifacts
If you have all of the requirements fulfilled, this project is easy to release using the Maven Release Plugin


1. `export GPG_TTY=$(tty)`
1. `mvn release:clean release:prepare -Dresume=false -DdryRun=true`
1. `mvn release:clean release:prepare -Dresume=false `
1. `mvn release:perform`
1. [Release the deployment](http://central.sonatype.org/pages/releasing-the-deployment.html) from OSSRH staging to the Central Repository

## Requirements

* ORCA has an associated project and namespace in OSSRH, but you will need your own login to deploy artifacts.  [Create a JIRA account](https://issues.sonatype.org/secure/Signup!default.jspa) using your RENCI email address, and then [Create a ticket](https://issues.sonatype.org/secure/CreateIssue.jspa?pid=10134) to add yourself to the list of allowed deployers for net.exogeni.orca. 

* Everything in this project is [configured to use Apache Maven](http://central.sonatype.org/pages/apache-maven.html) to build deployable artifacts, but you will need to configure your development environment too. Specifically you need to have following two plugins in pom.xml for release profile.

`    <plugin>
        <groupId>org.sonatype.plugins</groupId>
        <artifactId>nexus-staging-maven-plugin</artifactId>
        <version>1.6.7</version>
        <executions>
            <execution>
                <id>default-deploy</id>
                <phase>deploy</phase>
                <goals>
                    <goal>deploy</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <serverId>ossrh</serverId>
            <nexusUrl>https://oss.sonatype.org/</nexusUrl>
            <autoReleaseAfterClose>true</autoReleaseAfterClose>
        </configuration>
    </plugin>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-gpg-plugin</artifactId>
        <version>1.5</version>
        <executions>
            <execution>
                <id>sign-artifacts</id>
                <phase>verify</phase>
                <goals>
                    <goal>sign</goal>
                </goals>
            </execution>
        </executions>
    </plugin>`


* You need to be able to [sign components using GPG](http://central.sonatype.org/pages/working-with-pgp-signatures.html). 

* You will probably want to configure Apache Maven to use [encrypted passwords](https://maven.apache.org/guides/mini/guide-encryption.html).  Maven will use your OSSRH Jira password to push artifacts to the OSSRH Nexus server.

* If you are using the development VM, you may need to setup a [ssh config](http://nerderati.com/2011/03/17/simplify-your-life-with-an-ssh-config-file/) to specify the correct IdentityFile to use when connecting to GitHub.
