
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

LOCAL_VERSION=$(jq -r .version $DIR/version.json)

echo "Local Version is $LOCAL_VERSION"

function ensure_release_commit {
    local CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)

    if [[ "$CHANGED_FILES" != *version.json* ]]; then
        echo "Version did not change on this commit. Ignoring"
        exit 0
    fi
}

function ensure_master() {
  branch=${CI_BRANCH:-$(git rev-parse --abbrev-ref HEAD)}

  if [ $branch != "master" ]
  then
    echo "Not making a release from a branch: $branch"
    exit 0
  fi
}

function create_tag {
    git tag -a "$LOCAL_VERSION" -m "oidnjni version $LOCAL_VERSION"
    git push origin "$LOCAL_VERSION"
}


