from comet_common_iface import *
import json

cid="04700364-ec9b-4958-b726-b063754a9143"
unitId="aa491da3-3f23-4a5a-9b9e-33f98884570b"
readToken="361a67ac-db43-49ac-8868-54c1abe34b60"
writeToken="7b1c4d09-2440-498a-bebb-a54aaaf16c5b"
family="interfaces"

resp = CometInterface.get_family(cid, unitId, readToken, family)
if resp.status_code != 200:
    raise ApiError('Cannot Read Family: {}'.format(resp.status_code))
if resp.json()["value"] and not resp.json()["value"]["error"]:
    value = resp.json()["value"]["value"]
    interfaces = json.loads(json.loads(value)["val_"])
    for x in interfaces:
        if x["mac"] == "fe163e007908":
            x["ip"] = "1.2.3.4"
    updatedVal = json.loads(value)
    updatedVal["val_"] = json.dumps(interfaces)

    resp = CometInterface.update_family(cid, unitId, readToken, writeToken, family, updatedVal)
    if resp.status_code != 200:
        raise ApiError('Cannot Update Family: {}'.format(resp.status_code))

resp = CometInterface.enumerate_families(cid, readToken)
if resp.status_code != 200:
    raise ApiError('Cannot Enumerate Scope: {}'.format(resp.status_code))
    value = resp.json()["value"]["entries"]
    for x in value:
            print ("Family:" + x["family"])

resp = CometInterface.delete_family(cid, unitId, readToken, writeToken, family)
if resp.status_code != 200:
    raise ApiError('Cannot Delete Family: {}'.format(resp.status_code))

r = CometInterface.delete_families(cid, unitId, readToken, writeToken)
if r != True:
   raise ApiError('Cannot Delete Families')
