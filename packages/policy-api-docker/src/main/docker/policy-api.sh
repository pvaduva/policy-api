#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
#  Modifications Copyright (C) 2019 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#

JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk/
KEYSTORE="${POLICY_HOME}/etc/ssl/policy-keystore"
KEYSTORE_PASSWD="Pol1cy_0nap"
TRUSTSTORE="${POLICY_HOME}/etc/ssl/policy-truststore"
TRUSTSTORE_PASSWD="Pol1cy_0nap"


if [ "$#" -eq 1 ]; then
    CONFIG_FILE=$1
else
    CONFIG_FILE=${CONFIG_FILE}
fi

if [ -z "$CONFIG_FILE" ]
  then
    CONFIG_FILE="${POLICY_HOME}/etc/defaultConfig.json"
fi

echo "Policy api config file: $CONFIG_FILE"

$JAVA_HOME/bin/java -cp "${POLICY_HOME}/etc:${POLICY_HOME}/lib/*" -Djavax.net.ssl.keyStore="$KEYSTORE" -Djavax.net.ssl.keyStorePassword="$KEYSTORE_PASSWD" -Djavax.net.ssl.trustStore="$TRUSTSTORE" -Djavax.net.ssl.trustStorePassword="$TRUSTSTORE_PASSWD" org.onap.policy.api.main.startstop.Main -c $CONFIG_FILE
