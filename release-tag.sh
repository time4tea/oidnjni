#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

ensure_release_commit
ensure_master

git clone https://"${GITHUB_TOKEN}"@github.com/time4tea/oidnjni.git tmp/

( cd tmp && create_tag )
