# This code is no longer being maintained. The repo is kept only for historical reasons.

ORCA is a IaaS software for managing meta-clouds. It is deployed in production on ExoGENI world-wide testbed (http://www.exogeni.net). 

## Building ORCA
Building Orca is a straightforward Maven command.
- Checkout the source code from GitHub https://github.com/RENCI-NRIG/orca5
- `mvn clean install`

## Running ORCA in emulation
You can run Orca in emulation using docker containers by running some scripts:
- ./docker/docker_build.sh
- ./docker/docker_run.sh

## Testing ORCA
The default compile doesn't run any of the tests, because many of them depend on having a MySQL server to talk to (not a great idea, but working for now).

One of the docker containers started (in emulation) will be a MySQL container, that can be used when running the "unit tests", which are enabled via a Maven 'profile'. (Most of the unit tests are not, strictly speaking, "unit tests".  But they're still useful :)
* `mvn clean install -Ptest`

Tests are run automatically on our Jenkins server, following every commit pushed to GitHub.
https://ci.exogeni.net:8443/

### Tests that are run
Many, but not all, of the current ORCA tests are run as if requests were submitted to the Controller:
- [OrcaXmlrpcHandlerTest](https://github.com/RENCI-NRIG/orca5/blob/master/controllers/xmlrpc/src/test/java/orca/controllers/xmlrpc/OrcaXmlrpcHandlerTest.java)
- [OrcaRegressionTest](https://github.com/RENCI-NRIG/orca5/blob/master/controllers/xmlrpc/src/test/java/orca/controllers/xmlrpc/OrcaRegressionTest.java)
- [OrcaRegressionModifyTest](https://github.com/RENCI-NRIG/orca5/blob/master/controllers/xmlrpc/src/test/java/orca/controllers/xmlrpc/OrcaRegressionModifyTest.java)

## Interacting with ORCA
You can download [Flukes](https://github.com/RENCI-NRIG/flukes) from here: http://geni-images.renci.org/webstart/flukes.jnlp
And use that to connect to ExoGENI or your emulated ORCA.

You'll need to get GENI credentials to use ExoGENI:
http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/

You can also use [Ahab](https://github.com/RENCI-NRIG/ahab) to programmatically manipulate slices in ORCA.

## Contributing to ORCA
- Issue for the code change
- Branch, named after the issue
  - Test for the code change (TDD. Test should fail at this point)
  - Code for the code change (Test should pass at this point)
  - Code can be automatically formatted following Orca guidelines using the maven command `mvn formatter:format`
- Pull Request
- Code Review
- Merge code into `master`

Your code will not be formatted to follow the Orca style guidelines using the normal maven build commands (e.g. `mvn clean install`), but it can be formatted using the maven command `mvn formatter:format`.  The style guidelines can also be imported into Eclipse from the file [orca_formatter_style.xml](/tools/build/src/main/resources/orca/orca_formatter_style.xml)

## Orca deployment structure in ExoGENI

- [Actor deployment](link)
- [ORCA AM](link)
