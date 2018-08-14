# comet_common_iface.py

import logging as LOG
import requests
import urllib3
import string
from random import shuffle


urllib3.disable_warnings()
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class CometException(Exception):
    pass

class CometInterface:
    @classmethod
    def __init__(self, cometHost, caCert, clientCert, clientKey):
        self._cometHost = cometHost.split(",")
        if caCert != None:
            self._verify = caCert
            self._cert = (clientCert, clientKey)
        else :
            self._verify = False
            self._cert = None

    @classmethod
    def _headers(self):
        headers = {
            'Accept': 'application/json',
        }
        return headers

    @classmethod
    def invokeRoundRobinApi(self, operation, sliceId, rId, readToken, writeToken, family, value):
        response = None
        shuffle(self._cometHost)
        for host in self._cometHost:
            if operation == 'get_family' :
                response = self.get_family(host, sliceId, rId, readToken, family)
            elif operation == 'update_family' :
                response = self.update_family(host, sliceId, rId, readToken, writeToken, family, value)
            elif operation == 'delete_family' :
                response = self.delete_family(host, sliceId, rId, readToken, writeToken, family)
            elif operation == 'enumerate_families' :
                response = self.enumerate_families(host, sliceId, readToken)
            elif operation == 'delete_families' :
                response = delete_families(host, sliceId, rId, readToken, writeToken)
            if response.status_code == 200:
                break
        return response

    @classmethod
    def get_family(self, host, sliceId, rId, readToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken
        }
        if self._verify == False:
            response = requests.get((host + '/readScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.get((host + '/readScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("get_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("get_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("get_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("get_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def update_family(self, host, sliceId, rId, readToken, writeToken, family, value):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        if self._verify == False:
            response = requests.post((host +'/writeScope'), headers=self._headers(), params=params, verify=False, json=value)
        else:
            response = requests.post((host +'/writeScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify, json=value)
        LOG.debug ("update_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("update_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("update_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("update_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_family(self, host, sliceId, rId, readToken, writeToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':rId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        if self._verify == False:
            response = requests.delete((host +'/deleteScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.delete((host +'/deleteScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("delete_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("delete_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("delete_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("delete_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def enumerate_families(self, host, sliceId, readToken):
        params = {
            'contextID':sliceId,
            'readToken':readToken,
        }
        if self._verify == False:
            response = requests.get((host +'/enumerateScope'), headers=self._headers(), params=params, verify=False)
        else:
            response = requests.get((host +'/enumerateScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("enumerate_families: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("enumerate_families: Received Response Message: " + response.json()["message"])
        LOG.debug ("enumerate_families: Received Response Status: " + response.json()["status"])
        LOG.debug ("enumerate_families: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_families(self, host, sliceId, rId, readToken, writeToken):
        retVal=True
        response = self.enumerate_families(sliceId, readToken)
        if response.status_code != 200:
            raise CometException('delete_families: Cannot Enumerate Scope: ' + str(response.status_code))
        if response.json()["value"] and response.json()["value"]["entries"]:
            for key in response.json()["value"]["entries"]:
                LOG.debug ("delete_families: Deleting Family: '" + key["family"] + "' SliceId: '" + sliceId + "' rId: '" + rId + "'")
                response = self.delete_family(sliceId, rId, readToken, writeToken, key["family"])
                if response.status_code != 200:
                    LOG.debug('delete_families: Cannot Delete Family: ' + key["family"])
                    retVal|=False
        return retVal
