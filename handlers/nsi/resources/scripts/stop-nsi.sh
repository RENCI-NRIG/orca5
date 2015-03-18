#!/bin/bash

# Delete a reservation for a NSI circuit
# Parameters are: 
# From nsi.site.properties: NSI_HOSTKEY, NSI_HOSTCERT, NSI_CERTDIR, NSI_VERIFY [Optional], NSI_SERVICE, NSI_PROVIDER, NSI_REQUESTER, NSI_DEBUG [Optional].
# From controller: NSI_CONNECTION_ID.

# Note:
# If NSI_VERIFY is not set, the certificate will be verified (default = true)
# If NSI_DEBUG is not set, no additional output will be shown (default = false)

# prep
if [ -z ${NSI_CONNECTION_ID} ]; then
  	echo "stop-nsi.sh: NSI_CONNECTION_ID must be specified, exiting" >&2
    exit 1
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
    VERIFY="-z"
fi

# Detailed output?
if [ "$NSI_DEBUG" = "true" ]; then
    VERBOSE="-v" # verbose output
fi

if [ "${VERBOSE}" ]; then
    echo "Running reservation cancel for ${NSI_CONNECTION_ID}" >&2
fi

if [ "${VERBOSE}" ]; then
    echo "command: ${NSI_PYTHON} ${NSI_ONSA} terminate -r ${NSI_REQUESTER} -p ${NSI_PROVIDER} -u ${NSI_SERVICE} -l ${NSI_HOSTCERT} -k ${NSI_HOSTKEY} -i ${NSI_CERTDIR} -c ${NSI_CONNECTION_ID} ${TLS} ${VERIFY} ${VERBOSE}" >&2
fi
    
# Terminate reservation
OUTPUT=$($NSI_PYTHON $NSI_ONSA terminate -r $NSI_REQUESTER -p $NSI_PROVIDER -u $NSI_SERVICE -l $NSI_HOSTCERT -k $NSI_HOSTKEY -i $NSI_CERTDIR -c $NSI_CONNECTION_ID $TLS $VERIFY $VERBOSE)

STATUS=`echo "$OUTPUT" | grep "${NSI_CONNECTION_ID} ${NSI_TERMINATEDS}"`

if [ -z "${STATUS}" ]; then
    echo "Unable to close reservation ${NSI_CONNECTION_ID}" >&2
    echo "$OUTPUT" >&2
    exit 1
fi