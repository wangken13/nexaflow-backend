#!/usr/bin/env sh
set -eu

infra_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

if [ -z "${ENV_FILE:-}" ]; then
  if [ -f "$infra_dir/.env.prod" ]; then
    ENV_FILE="$infra_dir/.env.prod"
  else
    ENV_FILE="$infra_dir/.env"
  fi
  export ENV_FILE
fi

exec sh "$infra_dir/scripts/deploy.sh" "$@"
