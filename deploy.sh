#!/usr/bin/env sh
set -eu

root_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec sh "$root_dir/infra/scripts/deploy.sh"
