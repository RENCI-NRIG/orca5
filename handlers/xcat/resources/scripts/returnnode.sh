#!/bin/bash

source $(dirname $0)/common.sh

NODENAME=${1}
nodech $NODENAME nodelist.comments^="SLICE_ID=.*" nodelist.comments,="SLICE_ID=NONE"
