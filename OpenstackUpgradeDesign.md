Orca changes to address openstack upgrade
Refer https://github.com/RENCI-NRIG/exogeni/issues/230 for more details

# ORCA Updates to support Openstack Upgrade:
## Ec2 Handler 
- Following additional parameters added to **config/ec2.site.properties**
```
ec2.public.network=public
ec2.management.network=mgmt-network
ec2.slice.user.email=slice_owner@geni.net
ec2.slice.user.role=geni_slice_owner
ec2.slice.admin.user=admin
```
### Nova changes
- nova_essex_common updated to use openstack commands instead of nova commands
- nova_essex_common updated to include a new class Project responsible for following actions:
  - Creating Openstack Project per slice
  - Creating Openstack user in Project per slice and assigning appropriate roles
  - Creating a key pair using user public keys
  - Creating Security Group rules per slice
  - All subsequent resources requested in the Slice are provisioned in this project  
### EC2 Handler changes
#### Join Handler
- Create Openstack Project, User, Key Pair and Security Group rules if it does not exist
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
- Delete server
- Delete Openstack Security Group rules, Key Pair, User and Project if Project does not have any other compute or network resources
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
```
quantum.dataplane.network=physnet2
quantum.openflow.network=openflow
ec2.slice.user.email=slice_owner@geni.net
ec2.slice.user.role=geni_slice_owner
ec2.slice.admin.user=admin
```

- Following additional configuration parameters required in **handlers/providers/quantum/build.properties**
```
ec2.keys=${orca.home}/ec2/
```
### Join Handler
- Create Openstack Project and User if it does not exist
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
- Delete Openstack Security Group rules, Key Pair, User and Project if Project does not have any other compute or network resources 

## Console access and Management Network for Slices
 [ExoGENI Workflow](https://github.com/RENCI-NRIG/exogeni/blob/master/infrastructure/exogeni/openstack-queens/exogeni-workflow.md) page proposes following actions on slice creation:
### Slice Resources
- Create a project
- Create a user
- Create management network
- Create security group
- Create security group rules
- Create a user key pair

Current ORCA framework does not have a mechanism to trigger slice specific functionality on AM at slice creation or deletion. This can be achieved in either of the ways:

### Option 1:
Create a new reservation for Slice resources(indicated above) and trigger their creation/termination via join and leave. This amounts to lot of changes in code. So this approach is not considered.
### Option 2: 
Triggering slice specific start/cleanup code via join/leave for the resources. This requires start/cleanup should have checks to ensure no duplicate Slice resources(indicated above) created/deleted. This approach is **feasible and is implemented**. But has limitations along with possible alternates listed below.

#### Limitations
With option 2, it is possible that the creation of Slice resources is triggered simultaneously by multiple JOIN events for multiple resources. To avoid duplicate creation of slice resources, it is required that Openstack prevents creation of duplicates. Openstack prevents creation of duplicate projects, users, key pair and service group rules, but allows creation of duplicate network and security groups with the same name.

#### Alternate Solution
To avoid the duplicate network and security group, we have chosen to take an alternate solution:
- Create management network shared across all tenants. This network will be used for management interface by all the slices
```
openstack network create --provider-network-type vlan --provider-physical-network physnet1 --enable --share mgmt-network
openstack subnet create --subnet-range 10.103.0.0/24 --dhcp --allocation-pool start=10.103.0.101,end=10.103.0.200 --gateway 10.103.0.1 --network mgmt-network subnet-mgmt-network
openstack router create router-mgmt-network
openstack router add subnet router-mgmt-network subnet-mgmt-network
```
- Create Project per slice
- Create User per slice
- Create User key pair per slice using user's public key
- Add Security Group Rules per slice to the default Security Group. Use default security group to spawn VM instances.

At slice cleanup, delete all the below resources in addition to existing behavior
- All security group rules added, 
- Default Security Group
- User
- Project


NOTE: AM handler cannot do SSH test to VM with the above approach as AM does not have access to private key of the user.
