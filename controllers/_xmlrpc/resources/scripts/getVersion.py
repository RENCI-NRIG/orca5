#!/usr/bin/env python

import xmlrpclib
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default="http://localhost:11080/orca/xmlrpc")
(options, args) = parser.parse_args()

# Create an object to represent our server.
server_url = options.server;
server = xmlrpclib.Server(server_url);

# Call the server and get our result.
print "Querying ORCA xml-rpc server for current AM API version ... \n"
result = server.orca.getVersion()
print "Current API version = %r" % (result)
