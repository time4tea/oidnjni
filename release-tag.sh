#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

ensure_release_commit
ensure_master

git clone https://"${GITHUB_TOKEN}"@github.com/time4tea/oidnjni.git tmp/

( cd tmp && git config user.email "nobody@example.com" && git config user.name "CI Runner" && create_tag )
