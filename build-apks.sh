#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

chmod +x ./gradlew
./gradlew :mobile:assembleDebug :wear:assembleDebug --stacktrace

echo ""
echo "Build complete"
echo "Phone APK: $ROOT_DIR/mobile/build/outputs/apk/debug/mobile-debug.apk"
echo "Watch APK: $ROOT_DIR/wear/build/outputs/apk/debug/wear-debug.apk"
