# Images
With new ORCA release, users can create their images as below.

## Determine Horizon details
Select the Node and right click to View Properties of the Node in flukes
![alt text](images/flukes.png)

## Horizon dashboard & Create Image
Access dashboard using credentials determined above and go to Compute -> Instances screen. Note down the Project Name shown on the top left corner.

![alt text](images/horizon.png)

Select VM from which to create the image and click 'Create Snapshot'

![alt text](images/createsnapshot.png)

Go to Compute -> Images screen to check status of image. It would change from queued to Active
![alt text](images/activeimage.png)

Once the image is Active, click on Image Name to find out ID.
![alt text](images/imageid.png)

## Download Image
Download python image client which is required to download the image. Use the python client to download the image as indicated below.

```
wget  http://geni-images.renci.org/images/tools/image_client.py

python3.6 image_client.py -e http://rocky-hn.exogeni.net:8222 -p tenant-Slice1-B7P6GiWwOm -u owner-Slice1-B7P6GiWwOm -w GjxUjA2Pwr -i f95b02a4-064f-4c34-8c80-2dbc887c5af9 -f ./testImage.qcow2
```
NOTE: Python 3.6 and requests package should be installed before running image_client

## Create xml file for the image
- Generate shasum for the image
```
sha1sum testImage.qcow2
df9849bcb5bf4c7196b9252238be2cc3b2f0ad9b
```
- image xml file should include sha1sum for the image and http link to download the image as shown below:
```
<images>
     <image>
          <type>QCOW2</type>
          <signature>df9849bcb5bf4c7196b9252238be2cc3b2f0ad9b</signature>
          <url>http://<url for the location of the image>/testImage/testImage.qcow2</url>
     </image>
</images>
```
## CLI mechanism to create the image (only to be used by developers)
- Create a VM instance on Openstack with the base OS image like centos, debian etc.
- Logon to the VM, switch to root user and install neuca tools
NOTE: neuca-tools must be installed with python2.7
```
sudo su - 
yum install -y iscsi-initiator-utils
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python get-pip.py
pip install -U pip
pip install -U boto
pip install python-daemon==2.1.2
pip install netaddr
yum install -y git
git clone https://github.com/RENCI-NRIG/neuca-guest-tools.git
cd neuca-guest-tools/neuca-py
python setup.py install
```
- Edit /etc/cloud/cloud.cfg to ensure root login is enabled
- Edit /root/.ssh/authorized_keys file to remove any redirect messages
- Create a service file /usr/lib/systemd/system/neucad.service for centos7 based system
```
[Unit]
Description=neucad.service

[Service]
Type=oneshot
RemainAfterExit=yes
StandardOutput=journal+console
ExecStart=/bin/python /usr/bin/neucad start
ExecStop=/bin/python /usr/bin/neucad stop

[Install]
WantedBy=default.target
```
- Enable neucad service
```
systemctl enable neucad.service
service neucad start
```
- Create an init file for centos6, debian, fedora or ubuntu system
```
#!/bin/sh
#
### BEGIN INIT INFO
# Provides: lampp
# Required-Start:
# Required-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: A front-end init script that starts neucad
### END INIT INFO

prog="neucad"
exec=" /usr/bin/neucad"

start() {
     /usr/bin/python /usr/local/bin/neucad start
}

stop() {
     /usr/bin/python /usr/local/bin/neucad stop
}
case "$1" in
    start)
        $1
        ;;
    stop)
        $1
        ;;
    *)
        echo $"Usage: $0 {start|stop}"
        exit 2
esac
exit $?
```
- Logon to head node and switch to root user
- Source Openstack keystone and execute following command
```
source  /var/tmp/cred.tenant-kthare10-slice2-aD7NZwyeV8.owner-kthare10-slice2-aD7NZwyeV8
openstack server list
nova image-create <server_name> <image_name>
openstack image show <image_name>
glance image-download --file /tmp/<image_name>.qcow2 <image_id>
```
NOTE: credentials file for each slice would be present in /var/tmp directory with convention /var/tmp/cred.tenant-<slice-name>...
