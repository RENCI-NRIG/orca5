#!/bin/bash

source $(dirname $0)/common.sh

NODENAME=${1}
IP=$(nodels $NODENAME nodelist.comments | awk -F, '{print substr($2, length("PUBLIC_IP=")+1)}')
echo $IP

