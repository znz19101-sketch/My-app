#!/usr/bin/env bash
set -euo pipefail

required=(
  GUARDEXA_KEYSTORE_PATH
  GUARDEXA_KEYSTORE_PASSWORD
  GUARDEXA_KEY_ALIAS
  GUARDEXA_KEY_PASSWORD
)

for key in "${required[@]}"; do
  if [[ -z "${!key:-}" ]]; then
    echo "Missing environment variable: $key" >&2
    exit 1
  fi
done

./gradlew bundleStandardRelease --stacktrace
