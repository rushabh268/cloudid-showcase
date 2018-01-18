#!/usr/bin/env bash

if [ "$#" -ne 3 ]; then
    echo "Usage: start.sh [CONFIG_FILE] [TRUST_DOMAIN] [SPIRE_SERVER]"
    exit 1
fi

CONFIG_FILE=$1
TRUST_DOMAIN=$2
SPIRE_SERVER=$3

NODE_NAME=$(curl -Gs http://localhost:10255/pods/ | grep -o '"nodeName":"[^"]*"' | head -n 1 | cut -d : -f 2- | tr -d '"')
JOIN_TOKEN=$(/opt/spire/spire-server token generate -spiffeID spiffe://${TRUST_DOMAIN}/k8s/node/${NODE_NAME} -serverAddr ${SPIRE_SERVER} | cut -d : -f 2- | tr -d ' ')

echo "Start spire-agent for node '${NODE_NAME}' with token '${JOIN_TOKEN}'"
exec /opt/spire/spire-agent run -config /spire/config/agent.conf -joinToken ${JOIN_TOKEN}