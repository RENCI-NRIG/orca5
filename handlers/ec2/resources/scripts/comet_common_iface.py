# comet_common_iface.py

import logging as LOG
import requests
import urllib3

urllib3.disable_warnings()
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class CometException(Exception):
    pass

class CometInterface:
    @classmethod
    def __init__(self, cometHost, caCert, clientCert, clientKey):
        self._cometHost = cometHost
        if caCert != None:
            self._verify = caCert
            self._cert = (clientCert, clientKey)
        else :
            self._verify = False
            self._cert = None

    @classmethod
    def _url(self, path):
        return self._cometHost + path

    @classmethod
    def _headers(self):
        headers = {
            'Accept': 'application/json',
        }
        return headers

    @classmethod
    def get_family(self, sliceId, unitId, readToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken
        }
        if self._verify == False:
            response = requests.get(self._url('/readScope'), headers=self._headers(), params=params, verify=False)
        else:
            #response = requests.get(self._url('/readScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify)
            response = requests.get(self._url('/readScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("get_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("get_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("get_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("get_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def update_family(self, sliceId, unitId, readToken, writeToken, family, value):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        if self._verify == False:
            response = requests.post(self._url('/writeScope'), headers=self._headers(), params=params, verify=False, json=value)
        else:
            response = requests.post(self._url('/writeScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify, json=value)
        LOG.debug ("update_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("update_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("update_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("update_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_family(self, sliceId, unitId, readToken, writeToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        if self._verify == False:
            response = requests.delete(self._url('/deleteScope'), headers=self._headers(), params=params, verify=False)
        else:
            #response = requests.delete(self._url('/deleteScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify)
            response = requests.delete(self._url('/deleteScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("delete_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("delete_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("delete_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("delete_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def enumerate_families(self, sliceId, readToken):
        params = {
            'contextID':sliceId,
            'readToken':readToken,
        }
        if self._verify == False:
            response = requests.get(self._url('/enumerateScope'), headers=self._headers(), params=params, verify=False)
        else:
            #response = requests.get(self._url('/enumerateScope'), headers=self._headers(), params=params, cert= self._cert, verify=self._verify)
            response = requests.get(self._url('/enumerateScope'), headers=self._headers(), params=params, cert= self._cert, verify=False)
        LOG.debug ("enumerate_families: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("enumerate_families: Received Response Message: " + response.json()["message"])
        LOG.debug ("enumerate_families: Received Response Status: " + response.json()["status"])
        LOG.debug ("enumerate_families: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_families(self, sliceId, unitId, readToken, writeToken):
        retVal=True
        response = self.enumerate_families(sliceId, readToken)
        if response.status_code != 200:
            raise CometException('delete_families: Cannot Enumerate Scope: ' + str(response.status_code))
        if response.json()["value"] and response.json()["value"]["entries"]:
            for key in response.json()["value"]["entries"]:
                LOG.debug ("delete_families: Deleting Family: '" + key["family"] + "' SliceId: '" + sliceId + "' unitId: '" + unitId + "'")
                response = self.delete_family(sliceId, unitId, readToken, writeToken, key["family"])
                if response.status_code != 200:
                    LOG.debug('delete_families: Cannot Delete Family: ' + key["family"])
                    retVal|=False
        return retVal
