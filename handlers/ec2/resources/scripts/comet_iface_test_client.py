from comet_common_iface import *
import json

cid="04700364-ec9b-4958-b726-b063754a9143"
rId="aa491da3-3f23-4a5a-9b9e-33f98884570b"
readToken="361a67ac-db43-49ac-8868-54c1abe34b60"
writeToken="7b1c4d09-2440-498a-bebb-a54aaaf16c5b"
family="interfaces"
comethost="https://13.59.255.221:8111/"
ca="/Users/komalthareja/comet/inno-hn_exogeni_net/DigiCertCA.der"
client="/Users/komalthareja/comet/inno-hn_exogeni_net/star_exogeni_net.crt"
key="/Users/komalthareja/comet/inno-hn_exogeni_net/inno-hn_exogeni_net.key"

try:
    comet=CometInterface(comethost,ca,client,key)
    resp = comet.get_family(cid, rId, readToken, family)
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

        resp = comet.update_family(cid, rId, readToken, writeToken, family, updatedVal)
        if resp.status_code != 200:
            raise ApiError('Cannot Update Family: {}'.format(resp.status_code))

    resp = comet.enumerate_families(cid, readToken)
    if resp.status_code != 200:
        raise ApiError('Cannot Enumerate Scope: {}'.format(resp.status_code))
        value = resp.json()["value"]["entries"]
        for x in value:
                print ("Family:" + x["family"])

    resp = comet.delete_family(cid, rId, readToken, writeToken, family)
    if resp.status_code != 200:
        raise ApiError('Cannot Delete Family: {}'.format(resp.status_code))

    r = comet.delete_families(cid, rId, readToken, writeToken)
    if r != True:
       raise ApiError('Cannot Delete Families')
except Exception as e:
    print("Exception occurred: " + str(type(e)) + " : " + str(e) + "\n")
