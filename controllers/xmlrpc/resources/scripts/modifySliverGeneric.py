#!/usr/bin/env python2.6

import xmlrpclib
import ConfigParser
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-i", "--slice-id", dest="sliceID",
                  help="Slice ID, unique string")
parser.add_option("-r", "--reservation", dest="reservation",
                  help="Reservation GUID")
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default="http://localhost:11443/orca/xmlrpc")
parser.add_option("-c", "--cert", dest="cert",
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey",
                  help="Private key file (or a PEM file if contains both private key and cert)")
parser.add_option("-m", "--modify-subcommand", dest="modifySubcommand",
                  help="Modify subcommand name to be passed directly to handler")
parser.add_option("-f", "--properties-file", dest="propFileName",
                  help="Name of an ini file containing a single section 'properties' defining property names and their values")
(options, args) = parser.parse_args()

class SafeTransportWithCert(xmlrpclib.SafeTransport):
     __cert_file = ""
     __key_file = ""
     _use_datetime = False
     def __init__(self, certFile, keyFile):
         self.__cert_file = certFile
         self.__key_file = keyFile

     def make_connection(self,host):
         host_with_cert = (host, {
                       'key_file'  :  self.__key_file,
                       'cert_file' :  self.__cert_file
             } )
         return  xmlrpclib.SafeTransport.make_connection(self,host_with_cert)

mandatories = ['sliceID', 'reservation', 'modifySubcommand', 'propFileName']

for m in mandatories:
    if not options.__dict__[m]:
        print "Mandatory option is missing\n"
        parser.print_help()
        exit(-1)

# Read the config file and convert to list of dictionaries
cparser = ConfigParser.RawConfigParser()
propsFile = cparser.read(options.propFileName)
dprops = dict(cparser.items('properties'))
param = [ dprops ]

print "Passing parameters: "
for x in dprops:
    print x +" = " + dprops[x]

# Create an object to represent our server.
server_url = options.server;
credentials = []

if server_url.startswith('https://'):
    if options.cert == None or options.privateKey == None:
        print "For using secure (https) transport, you must specify the path to your certificate and private key"
        parser.print_help()
        exit(-1)
    # create secure transport with client cert
    transport = SafeTransportWithCert(options.cert, options.privateKey)
    server = xmlrpclib.Server(server_url, transport=transport)
else:
    server = xmlrpclib.Server(server_url)

# Call the server and get our result.
print "Issuing modify " + options.modifySubcommand + " subcommand for reservation ... \n"
result = server.orca.modifySliver(options.sliceID, options.reservation, credentials, options.modifySubcommand, param)
print result
