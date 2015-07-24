#!/usr/bin/env python

import httplib
import xmlrpclib

class HTTPS_with_timeout(httplib.HTTPS):
    def __init__(self, host='', port=None, key_file=None, cert_file=None, strict=None, timeout=5.0):
        if port == 0: port = None
        self._setup(self._connection_class(host, port, key_file, cert_file, strict, timeout=timeout))

    def getresponse(self, *args, **kw):
        return self._conn.getresponse(*args, **kw)

class TimeoutSafeTransport(xmlrpclib.SafeTransport, object):
    def __init__(self, use_datetime=0):
        super(TimeoutSafeTransport, self).__init__(use_datetime)
        self.connection_timeout = 10.0
        self.read_timeout = 30.0

    def set_connection_timeout(self, timeout):
        self.connection_timeout = timeout

    def set_read_timeout(self, timeout):
        self.read_timeout = timeout

    def make_connection(self, host):
        host, extra_headers, x509 = self.get_host_info(host)
        return HTTPS_with_timeout(host, None, timeout=self.connection_timeout, **(x509 or {}))

    def send_content(self, connection, request_body):
        connection.putheader("Content-Type", "text/xml")
        connection.putheader("Content-Length", str(len(request_body)))
        connection.endheaders()
        connection._conn.sock.settimeout(self.read_timeout)
        if request_body:
            connection.send(request_body)
