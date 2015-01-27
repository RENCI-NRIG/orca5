#!/bin/bash

PID=~/flowvisor/flowvisor.pid
CFG=~/flowvisor/conf.d

kill -9 `cat $PID`
~/flowvisor/flowvisor -D --pidfile=$PID -a $CFG -v 0 ptcp:

