#!/bin/bash

source $(dirname $0)/common.sh

NODENAME=${1}
cleanupNode $NODENAME
exit 0

