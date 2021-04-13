#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

ensure_release_commit
ensure_master

create_tag

