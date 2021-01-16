#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

function maven_publish {
    local PACKAGE=$1
    local PAYLOAD="{\"username\": \"${SONATYPE_USER}\", \"password\": \"${SONATYPE_PASSWORD}\"}"

    local PUBLISHED=$(curl --fail --silent -o /dev/null https://mvnrepository.com/artifact/net.time4tea/"${PACKAGE}"/"${LOCAL_VERSION}" ; echo $?)

    if [[ $PUBLISHED == "0" ]]; then
        echo "$PACKAGE is already published. Skipping"
    else
        echo "Publishing $PACKAGE $LOCAL_VERSION into Maven central..."
        RESULT=$(curl -s -X POST -u "$BINTRAY_USER:$BINTRAY_API_KEY" -H "Content-Type: application/json" --data "$PAYLOAD" "https://bintray.com/api/v1/maven_central_sync/time4tea/oss/$PACKAGE/versions/$LOCAL_VERSION")

        if [[ ! "${RESULT}" =~ .*Successful.* ]]; then
           echo "Failed: ${RESULT}"
           exit 1
        fi
    fi
}

ensure_release_commit
ensure_master

maven_publish "oidnjni"

