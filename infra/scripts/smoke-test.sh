#!/usr/bin/env sh
set -eu

base_url=${1:-http://127.0.0.1}
tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT

assert_status() {
  expected=$1
  url=$2
  actual=$(curl -ksS -o "$tmp_dir/body" -w '%{http_code}' "$url")
  if [ "$actual" != "$expected" ]; then
    echo "Expected HTTP $expected from $url, received $actual" >&2
    cat "$tmp_dir/body" >&2
    exit 1
  fi
}

assert_status 200 "$base_url/healthz"
assert_status 200 "$base_url/readyz"
assert_status 200 "$base_url/api/auth/captcha"
assert_status 401 "$base_url/api/customer"

echo "Smoke tests passed for $base_url"
