#!/usr/bin/env python

import xmlrpclib
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-r", "--request", dest="request",
                  help="NDL request fielname", metavar="FILE")
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default="http://localhost:11080/orca/xmlrpc")
parser.add_option("-i", "--slice-id", dest="sliceID",
                  help="Slice id, unique string")
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

mandatories = ['request', 'sliceID']

for m in mandatories:
    if not options.__dict__[m]:
        print "Mandatory option is missing\n"
        parser.print_help()
        exit(-1)

ndlReq = None
f = open(options.request,'r')
ndlReq = f.read()
# this is Python2.5 and above
#with open(options.request) as f:
#    ndlReq = f.read()

if ndlReq != None:
    print "Request NDL = \n"
    print ndlReq
else:
    exit

# Call the server and get our result.
print "Contacting ORCA xml-rpc server " + options.server + " for creating the sliver... \n"

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

print "Waiting for sliver details...\n"

sliceUrn="urn:none"
credentials=[]

result = server.orca.modifySlice(options.sliceID, credentials, ndlReq)

print result
