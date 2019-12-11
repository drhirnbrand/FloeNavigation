#/bin/bash -f

gradle app:build
adb shell pm clear de.awi.floenavigation
adb install -r app/build/outputs/apk/release/app-release.apk

