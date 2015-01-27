#!/usr/bin/env python2.6

import xmlrpclib
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default='https://localhost:9443/xmlrpc')
parser.add_option("-c", "--cert", dest="cert", 
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey", 
                  help="Private key file (or a PEM file if contains both private key and cert)")
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

# Create an object to represent our server.
server_url = options.server;

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
print "Querying ORCA xml-rpc server for available resources ... \n"
credentials = []
options = {}
#result = server.xmlrpcService.ListResources(credentials, options)
result = server.orca.listResources(credentials, options)
print "Advertisement RSpec/NDL of available resources \n"
print result['ret']
