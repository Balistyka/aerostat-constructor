#!/usr/bin/env bash
# Перевірка на емуляторі: встановлення, запуск, UI-тести, знімки екранів.
set -u
OUT=ci-output/device
mkdir -p "$OUT/shots"
APK=app/build/outputs/apk/debug/app-debug.apk
TAPK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
STATUS=0

adb wait-for-device
adb shell getprop ro.build.version.release | tee "$OUT/android_version.txt"
adb shell wm size | tee "$OUT/screen.txt"

echo "== Встановлення APK ==" | tee "$OUT/install.txt"
adb install -r "$APK" 2>&1 | tee -a "$OUT/install.txt"
grep -q "Success" "$OUT/install.txt" || STATUS=1

adb logcat -c
echo "== Запуск ==" | tee "$OUT/launch.txt"
adb shell am start -W -n ua.aerostat/.MainActivity 2>&1 | tee -a "$OUT/launch.txt"
sleep 6
adb exec-out screencap -p > "$OUT/shots/launch_phone_device.png"
PID=$(adb shell pidof ua.aerostat | tr -d '\r')
echo "PID після запуску: ${PID:-немає}" | tee -a "$OUT/launch.txt"
[ -n "$PID" ] || STATUS=1
adb logcat -d -b crash > "$OUT/crash_log.txt" 2>&1
if [ -s "$OUT/crash_log.txt" ] && grep -q "ua.aerostat" "$OUT/crash_log.txt"; then STATUS=1; echo "Знайдено падіння" | tee -a "$OUT/launch.txt"; fi

echo "== Instrumented UI-тести (MainFlowTest) =="
./gradlew --no-daemon connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=ua.aerostat.MainFlowTest 2>&1 | tee "$OUT/connected_tests_log.txt"
CT=${PIPESTATUS[0]}
echo "connectedDebugAndroidTest exit code: $CT" | tee -a "$OUT/connected_tests_log.txt"
[ "$CT" -eq 0 ] || STATUS=1
cp -r app/build/reports/androidTests "$OUT/androidTests-report" 2>/dev/null || true
cp -r app/build/outputs/androidTest-results "$OUT/androidTest-results" 2>/dev/null || true

shots () {
  local prefix=$1
  adb install -r "$APK" >/dev/null 2>&1
  adb install -r -t "$TAPK" >/dev/null 2>&1
  adb shell pm clear ua.aerostat >/dev/null 2>&1
  adb shell am instrument -w -e class ua.aerostat.ScreenshotTest -e shotPrefix "$prefix" \
    ua.aerostat.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee "$OUT/screenshots_${prefix}_log.txt"
  for f in $(adb shell run-as ua.aerostat ls files/shots 2>/dev/null | tr -d '\r'); do
    adb exec-out run-as ua.aerostat cat "files/shots/$f" > "$OUT/shots/$f"
  done
}

echo "== Знімки: вузький екран =="
shots phone
echo "== Знімки: розгорнутий екран (як у розкладеного смартфона) =="
adb shell wm size 2208x1840
adb shell wm density 420
sleep 3
shots wide
adb shell wm size reset
adb shell wm density reset

ls -la "$OUT/shots"
echo "DEVICE_STATUS=$STATUS" | tee "$OUT/status.txt"
exit $STATUS
