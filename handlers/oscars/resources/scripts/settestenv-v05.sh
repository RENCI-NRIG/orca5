#!/bin/bash

export OSCARS_SCRIPTS_DIR=/Users/ibaldin/workspace/orca/handlers/oscars/resources/scripts

export OSCARS_HOME=/opt/orca/oscars

#export OSCARS_IDC="https://idcdev0.internet2.edu:8443/axis2/services/OSCARS/"
export OSCARS_IDC="https://oscars.es.net/axis2/services/OSCARS"

#export OSCARS_L2_SRC="urn:ogf:network:domain=ion.internet2.edu:node=rtr.salt:port=ge-7/1/2:link=*"
export OSCARS_L2_SRC="urn:ogf:network:domain=es.net:node=nersc-mr2:port=xe-7/1/0:link=*"

#export OSCARS_L2_DST="urn:ogf:network:domain=ion.internet2.edu:node=rtr.newy:port=xe-0/0/3:link=*"
export OSCARS_L2_DST="urn:ogf:network:domain=es.net:node=star-sdn1:port=xe-2/0/0:link=*"

export OSCARS_BW="100000000"

export OSCARS_DURATION=86400

export OSCARS_DEBUG=true

export OSCARS_UNIT_VLAN_TAG=660

#export OSCARS_PATH="urn:ogf:network:domain=es.net:node=nersc-mr2:port=xe-7/1/0:link=*","urn:ogf:network:domain=es.net:node=nersc-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=jgi-mr2:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=jgi-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=lbl-mr2:port=xe-8/0/0:link=xe-8/0/0.0","urn:ogf:network:domain=es.net:node=lbl-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=slac-mr2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=slac-mr2:port=xe-2/0/0:link=xe-2/0/0.0","urn:ogf:network:domain=es.net:node=sunn-sdn2:port=xe-1/0/0:link=xe-1/0/0.0","urn:ogf:network:domain=es.net:node=sunn-sdn2:port=xe-2/2/0:link=xe-2/2/0.0","urn:ogf:network:domain=es.net:node=sunn-cr1:port=ge-1/0/0:link=ge-1/0/0.0","urn:ogf:network:domain=es.net:node=sunn-cr1:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=denv-cr2:port=xe-1/1/0:link=xe-1/1/0.0","urn:ogf:network:domain=es.net:node=denv-cr2:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=kans-cr1:port=xe-1/1/0:link=xe-1/1/0.0","urn:ogf:network:domain=es.net:node=kans-cr1:port=xe-0/1/0:link=xe-0/1/0.0","urn:ogf:network:domain=es.net:node=chic-cr1:port=xe-3/0/0:link=xe-3/0/0.0","urn:ogf:network:domain=es.net:node=chic-cr1:port=xe-3/1/0:link=xe-3/1/0.0","urn:ogf:network:domain=es.net:node=star-cr1:port=xe-1/3/0:link=xe-1/3/0.0","urn:ogf:network:domain=es.net:node=star-cr1:port=xe-0/0/0:link=xe-0/0/0.0","urn:ogf:network:domain=es.net:node=star-sdn1:port=xe-0/0/0:link=xe-0/0/0.0","urn:ogf:network:domain=es.net:node=star-sdn1:port=xe-2/0/0:link=*"

