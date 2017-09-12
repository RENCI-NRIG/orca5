ORCA is a IaaS software for managing meta-clouds. It is deployed in production on ExoGENI world-wide testbed (http://www.exogeni.net). 

## Building ORCA
Building Orca is a straightforward Maven command.  Getting it to work in your preferred IDE may be more difficult.
- Checkout the source code from GitHub https://github.com/RENCI-NRIG/orca5
- `mvn clean install`

The default compile doesn't run any of the tests, because many of them depend on having a MySQL server to talk to (not a great idea, but working for now).

## Running ORCA in emulation
You can run Orca in emulation using docker containers by running some scripts:
- ./docker/docker_build.sh
- ./docker/docker_run.sh

## Testing ORCA
One of the containers started will be a MySQL container, that can be used when running the "unit tests", which are enabled via a Maven 'profile'. (Most of the unit tests are not, strictly speaking, "unit tests".  But they're still useful :)
`mvn clean install -Ptest`

Tests are run automatically on our Jenkins server.
https://ci.exogeni.net:8443/

## Interacting with ORCA
You can download [Flukes](https://github.com/RENCI-NRIG/flukes) from here: http://geni-images.renci.org/webstart/flukes.jnlp
And use that to connect to ExoGENI or your emulated ORCA.

You'll need to get GENI credentials to use ExoGENI:
http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/


## Contributing to ORCA
- Issue for the code change
- Branch, named after the issue
  - Test for the code change (TDD. Test should fail at this point)
  - Code for the code change (Test should pass at this point)
- Pull Request
- Code Review
- Merge code into `master`
