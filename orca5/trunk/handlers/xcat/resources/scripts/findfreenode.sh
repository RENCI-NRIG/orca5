#!/bin/bash

source $(dirname $0)/common.sh

function findFreeBareMetalNode() {
    # Create an array of nodes with a SLICE_ID of NONE
    local FREENODES=($(nodels $XCAT_BAREMETAL_GROUP nodelist.comments | grep NONE | awk -F: '{print $1}'))
    # Check the length of the array. If 0, we have no nodes available and must return.
    if [ ${#FREENODES[@]} -eq 0 ]; then
        return 1
    fi

    # We have bare metal nodes available!
    # Grab the first node in the array, mark it used by setting the SLICE_ID, and return it (using echo).
    local MYNODE="${FREENODES[0]}"
    nodech $MYNODE nodelist.comments^="SLICE_ID=NONE" nodelist.comments,="SLICE_ID=$SLICE_ID"
    echo ${MYNODE}
}

# Sanity checks
if [ -z $XCAT_BAREMETAL_GROUP ]; then
    echo "findfreenode.sh: xCAT bare metal node group not provided. Ensure that you have correctly set the xcat.baremetal.group property in xcat.site.properties!"
    exit 1
fi

# Check if the xCAT commands are on the path
which tabdump >/dev/null 2>&1
if [ "$?" != "0" ]; then
    echo "findfreenode.sh: xCAT's commands do not appear to be available in the PATH; please ensure xCAT is installed correctly. Exiting."
    exit 1
fi

# Last but not least, does the AM have permission to run the needed xCAT commands?
tabdump 2>&1 | grep -v -q "Error: Permission denied"
if [ "$?" != "0" ]; then
    echo "findfreenode.sh: Our userid has not been granted adequate permission within xCAT to perform the required tasks. Exiting."
    exit 1
fi

NODENAME=$(findFreeBareMetalNode)

if [ -z ${NODENAME} ]; then
    echo "findfreenode.sh: No available bare metal nodes found. Exiting."
    exit 1
fi

echo $NODENAME
