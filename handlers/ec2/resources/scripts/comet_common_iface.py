# comet_common_iface.py

import logging as LOG
import requests
import urllib3

urllib3.disable_warnings()

class CometException(Exception):
    pass

class CometInterface:
    # not used currently
    @classmethod
    def _certificate(self):
        return ('/etc/ssl/certs/certificate.pem', '/etc/ssl/certs/key.pem')

    # not used currently
    def _cacert(self):
        return '/etc/ssl/certs/DigiCertCA.crt'

    @classmethod
    def _url(self, comethost, path):
        return comethost + path

    @classmethod
    def _headers(self):
        headers = {
            'Accept': 'application/json',
        }
        return headers

    @classmethod
    def get_family(self, comethost, sliceId, unitId, readToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken
        }
        response = requests.get(self._url(comethost, '/readScope'), headers=self._headers(), params=params, verify=False)
        LOG.debug ("get_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("get_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("get_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("get_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def update_family(self, comethost, sliceId, unitId, readToken, writeToken, family, value):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        response = requests.post(self._url(comethost, '/writeScope'), headers=self._headers(), params=params, verify=False, json=value)
        LOG.debug ("update_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("update_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("update_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("update_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_family(self, comethost, sliceId, unitId, readToken, writeToken, family):
        params = {
            'contextID':sliceId,
            'family':family,
            'Key':unitId,
            'readToken':readToken,
            'writeToken':writeToken
        }
        response = requests.delete(self._url(comethost, '/deleteScope'), headers=self._headers(), params=params, verify=False)
        LOG.debug ("delete_family: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("delete_family: Received Response Message: " + response.json()["message"])
        LOG.debug ("delete_family: Received Response Status: " + response.json()["status"])
        LOG.debug ("delete_family: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def enumerate_families(self, comethost, sliceId, readToken):
        params = {
            'contextID':sliceId,
            'readToken':readToken,
        }
        response = requests.get(self._url(comethost, '/enumerateScope'), headers=self._headers(), params=params, verify=False)
        LOG.debug ("enumerate_families: Received Response Status Code: " + str(response.status_code))
        LOG.debug ("enumerate_families: Received Response Message: " + response.json()["message"])
        LOG.debug ("enumerate_families: Received Response Status: " + response.json()["status"])
        LOG.debug ("enumerate_families: Received Response Value: " + str(response.json()["value"]))
        return response

    @classmethod
    def delete_families(self, comethost, sliceId, unitId, readToken, writeToken):
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
