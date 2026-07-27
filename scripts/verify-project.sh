#!/usr/bin/env bash
set -euo pipefail

./gradlew \
  clean \
  testStandardDebugUnitTest \
  lintStandardDebug \
  assembleStandardDebug \
  --stacktrace

echo "Guardexa verification completed."
