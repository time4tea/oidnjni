#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

ensure_master

BINTRAY_VERSION=`curl -s https://bintray.com/api/v1/packages/time4tea/oss/oidnjni/versions/_latest | jq -r .name`

if [[ "$LOCAL_VERSION" == "$BINTRAY_VERSION" ]]; then
    echo "Version has not changed"
    exit 0
fi

echo "Attempting to release $LOCAL_VERSION (old version $BINTRAY_VERSION)"

./gradlew -PreleaseVersion=$LOCAL_VERSION clean javadocJar sourcesJar assemble bintrayUpload
