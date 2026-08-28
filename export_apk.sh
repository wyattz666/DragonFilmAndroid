#!/bin/bash
set -e

echo "🚀 Bắt đầu build DragonFilm APK..."

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

./gradlew assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk DragonFilm.apk

echo "🎉 Build APK thành công! File lưu tại: $(pwd)/DragonFilm.apk"
