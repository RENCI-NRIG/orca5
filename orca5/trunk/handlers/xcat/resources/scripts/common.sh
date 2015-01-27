#!/bin/bash

# Ensure xcat commands are properly set up.
source /etc/profile.d/xcat.sh

function cleanupNode(){
    local nodename=${1}

    # FIXME: For now, we merely power off the node.
    # We should perform a second netinstall of an image that wipes the drives in the node, in
    # order to assure privacy
    rpower $nodename off >/dev/null 2>&1
}

