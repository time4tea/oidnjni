#!/bin/bash

set -e
set -o errexit
set -o pipefail
set -o nounset

. ./release-functions.sh

ensure_release_commit
ensure_master

# from  https://github.com/anothrNick/github-tag-action/blob/master/entrypoint.sh  - MIT License

commit=$(git rev-parse HEAD)
new=$LOCAL_VERSION

full_name=$GITHUB_REPOSITORY
git_refs_url=$(jq .repository.git_refs_url $GITHUB_EVENT_PATH | tr -d '"' | sed 's/{\/sha}//g')

echo "$dt: **pushing tag $new to repo $full_name"

git_refs_response=$(curl -s -X POST $git_refs_url -H "Authorization: token $GITHUB_TOKEN" -d @- << EOF
{
  "ref": "refs/tags/$new",
  "sha": "$commit"
}
EOF
)

git_ref_posted=$( echo "${git_refs_response}" | jq .ref | tr -d '"' )

echo "::debug::${git_refs_response}"
if [ "${git_ref_posted}" = "refs/tags/${new}" ]; then
  exit 0
else
  echo "::error::Tag was not created properly."
  exit 1
fi
