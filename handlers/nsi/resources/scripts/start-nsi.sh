#!/bin/bash 

# Create a reservation for a NSI circuit
# Parameters are: 
# From nsi.site.properties: NSI_HOSTKEY, NSI_HOSTCERT, NSI_CERTDIR, NSI_VERIFY [Optional], NSI_SERVICE, NSI_PROVIDER, NSI_REQUESTER, NSI_USER, NSI_D_START_TAG, NSI_D_END_TAG, NSI_DEBUG [Optional].
# From controller: NSI_L2_SRC, NSI_L2_DST, NSI_BW [Optional], NSI_DURATION, NSI_START_TIME, NSI_END_TIME, NSI_TAGS.

# Note:
# If NSI_VERIFY is not set, the certificate will be verified (default = true)
# If NSI_DEBUG is not set, no additional output will be shown (default = false)
# If NSI_BW is not set, it will be 100 Mbps (handler.xml)
# Either NSI_START_TIME and NSI_END_TIME, or NSI_DURATION, must be set

# ORCA uses bps and OpenNSA Mbps
NSI_BW=$(($NSI_BW/1000000))
# if less than 100Mbps, set to 100Mbps
if [ $((${NSI_BW} < 100)) = "1" ]; then
	NSI_BW=100
fi

if [ "$NSI_START_TIME" = "\${config.start_time}" ] || [ "$NSI_END_TIME" = "\${config.end_time}" ]; then
	if [ ${NSI_DURATION} ]; then
	    # Tweak this value. NTP helps here as the time needs to be in sync with the OpenNSA server.
	    NSI_START_TIME=10
		NSI_END_TIME=$((NSI_DURATION+NSI_START_TIME))
	else
		echo "start-nsi.sh: NSI_DURATION, NSI_START_TIME and NSI_END_TIME, not set" >&2 
		echo "start-nsi.sh: Either NSI_DURATION or NSI_START_TIME and NSI_END_TIME must be set" >&2 
	fi
fi

# In case nsi.python was not set in nsi.site.properties. The system's default python will be used.
if [ "$NSI_PYTHON" = "\${nsi.python}" ]; then
    unset NSI_PYTHON
fi

# TLS?
if [ "$NSI_TLS" = "true" ]; then
	TLS="-x" # enable TLS
fi

# Verify certificate?
if [ "$NSI_VERIFY" = "false" ]; then
	VERIFY="-z" # will skip verification
fi

# Detailed output?
if [ "$NSI_DEBUG" = "true" ]; then
	VERBOSE="-v" # verbose output
fi

# FIXME: Remove and fix appropriately after SC14 demos
# Hardcoded just for testing
NSI_START_TIME=10
NSI_END_TIME=$((NSI_END_TIME+NSI_START_TIME))

# send stdout there
# FIXME: use proper logging location, use a property for the location
# Also, this is a security hole *waiting* to be exploited via the trap.
OUTFILE="/tmp/nsi.tmp"
trap 'rm -f $OUTFILE' EXIT

for CURR_TAG in $(echo ${NSI_TAGS} | tr "," " "); do
   
	if [ "${VERBOSE}" ]; then
    	echo "command: ${NSI_PYTHON} ${NSI_ONSA} reserveprovision -r ${NSI_REQUESTER} -p ${NSI_PROVIDER} -s ${NSI_L2_SRC}#${CURR_TAG} -d ${NSI_L2_DST}#${CURR_TAG} -j user=${NSI_USER} -u ${NSI_SERVICE} -a +${NSI_START_TIME} -e +${NSI_END_TIME} -l ${NSI_HOSTCERT} -k ${NSI_HOSTKEY} -i ${NSI_CERTDIR} -b ${NSI_BW} ${TLS} ${VERIFY} ${VERBOSE} > ${OUTFILE}" >&2 
	fi

	$NSI_PYTHON $NSI_ONSA reserveprovision -r "$NSI_REQUESTER" -p "$NSI_PROVIDER" -s "$NSI_L2_SRC"#"$CURR_TAG" -d "$NSI_L2_DST"#"$CURR_TAG" -j user="$NSI_USER" -u "$NSI_SERVICE" -a +"$NSI_START_TIME" -e +"$NSI_END_TIME" -l "$NSI_HOSTCERT" -k "$NSI_HOSTKEY" -i "$NSI_CERTDIR" -b "$NSI_BW" "$TLS" "$VERIFY" "$VERBOSE" > "$OUTFILE"

	CONNECTION_ID=`grep provisioned ${OUTFILE} | cut -d ' ' -f 2`
	NO_AVAIL=`grep "No type mapping for tag" ${OUTFILE}`

	if [ ${CONNECTION_ID} ]; then
		# This should be the only content of stdout
		echo $CONNECTION_ID" "$CURR_TAG
		exit 0
	elif [ -z "$NO_AVAIL" ]; then
		echo "Reservation failed for an unknown reason." >&2
		cat ${OUTFILE} >&2
    	exit 1
	fi

done

# All vlans are unavailable.
echo "There are no vlan tags available!" >&2
exit 1
