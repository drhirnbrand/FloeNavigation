#/bin/bash -f

gradle app:build

adb shell pm clear de.awi.floenavigation
adb shell pm uninstall de.awi.floenavigation

adb shell pm clear de.awi.floenavigation.debug
adb shell pm uninstall de.awi.floenavigation.debug

adb install app/build/outputs/apk/release/app-release.apk

