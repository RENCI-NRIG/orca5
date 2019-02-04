Orca changes to address openstack upgrade
Refer https://github.com/RENCI-NRIG/exogeni/issues/230 for more details

# Updates made so far:
## Ec2 Handler 
- Following additional parameters added to **config/ec2.site.properties**
   a) ec2.management.network
   b) ec2.security.group
   c) ec2.public.network
### Nova changes
- nova_essex_common updated to use openstack commands instead of nova commands
### EC2 Handler changes
#### Join Handler
- Uses qcow2 if available and uses ami image only if qcow2 is not available to spawn instance
- Does not create users or applies ssh keys on instance. This is now done by neuca tools.
#### Modify SSH Handler
- Updated to push SSH keys to COMET
- Does not create users or applies ssh keys on instance. This is now done by neuca tools.
#### Modify Interface Handler
##### Add interface
- Modified to pass ipversion as net_type
- Modified to pass instance_id as well as instance_name
- Interface is added as following:
  a) Create a port; port name is constructed as instance_name.mac_addr e.g. instance-00000004.fe:16:3e:00:00:02; port is created with --disable-port-security
b) Attach port to server
```
openstack port create --project $tenant_id --network $network_uuid --enable --disable-port-security --mac-address $mac_addr $port_name -fjson
openstack server add port $instance_id $port_id instance-00000004.fe:16:3e:00:00:02
```
##### Remove interface
- Remove the port from the attached instance
- Delete the port
```
openstack port show $port_name_id -fjson
openstack server remove port $instance_id $port_uuid
openstack port delete $port_id
```
#### Leave Handler
- remove_all_interfaces function updated
- Finds all the ports associated with the network
```
openstack port list --project $tenant_id --network $network_uuid -fjson
```
- For each port
      a) Removes the port from the attached instance
      b) Deletes the port
```
openstack port show $port_name_id -fjson
openstack server remove port $instance_id $port_uuid
openstack port delete $port_id
```
## Ant Tasks
Existing Handler Ant Tasks have been redesigned into following 2 tasks
### NEucaGenerateGlobalUserDataTask
This task generates global user data stored in openstack. This task is invoked before openstack instance creation to be able to pass userdata to instance by EC2 Join handler.

### NEucaGenerateCometData
 This task supports 3 operations namely generate, add or delete.
- Generate: This operation is responsible for creating users, interfaces, routes, scripts and storage information for instance and pushing it to COMET. It is invoked after openstack instance is created by EC2 Join handler.
- Add: This operation is responsible for supporting modify(add/update) operations on users, interfaces, routes, scripts and storage. It also pushes updates to COMET. It is invoked after openstack instance is created by EC2 Modify handler.
- Delete: This operation is responsible for supporting modify(delete) operations on users, interfaces, routes, scripts and storage. It also pushes updates to COMET. It is invoked after openstack instance is created by EC2 Modify handler.

## Quantum Handler
- Following additional configuration parameters required in **config/quantum.properties**
     a) quantum.dataplane.network=physnet2
     b) quantum.openflow.network=physnet3

- Following additional configuration parameters required in **handlers/providers/quantum/build.properties**
     a) ec2.keys=${orca.home}/ec2/

### Join Handler
- Updated to pass quantum.dataplane.network, quantum.openflow.network and ec2.keys before invoking neuca-quantum-create-net
- Network name is constructed as net_type:network:vlan_tag:max_rate:burst_rate
- Example network name - ipv4:vlan-data:2:10000:1250
- Network is created with --disable-port-security option
- Network is created via following commands:
```
# check if network already exists 
openstack network list --project $tenant_id $net_id -fjson
# if network does not exist create it
openstack network create --project $tenant_id --provider-network-type $type --provider-physical-network $physNetwork --provider-segment $vlan_tag --disable-port-security $name -fjson
```
### Leave Handler
- Updated to pass quantum.dataplane.network, quantum.openflow.network and ec2.keys before invoking neuca-quantum-delete-net
- Finds all the ports associated with the network
```
openstack port list --project $tenant_id --network $network_uuid -fjson
```
- For each port
      a) Removes the port from the attached instance
      b) Deletes the port
```
openstack port show $port_name_id -fjson
openstack server remove port $instance_id $port_uuid
openstack port delete $port_id
```
- Deletes network
```
openstack network delete $network_uuid
```
 
