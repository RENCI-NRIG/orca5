#!/bin/bash

source ${EUCA_KEY_DIR}/novarc
source $(dirname $0)/nova-common.sh

INSTANCE=$1

echo `get_nova_host $INSTANCE`

