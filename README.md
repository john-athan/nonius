# Nonius

Eight measuring instruments Android does not ship with, in one app of about a
megabyte and a half, with no network permission at all.

No ads. No trackers. No analytics. One permission, the microphone, asked for
when you open the sound level meter or the tuner and never before.

---

A phone carries a gravity sensor, a magnetometer, a barometer, a light sensor
and a microphone. Android exposes none of them to you. The apps that fill the
gap are mostly a banner ad with a bubble drawn on top.

Nonius is the other kind: every face is drawn as an instrument rather than
assembled out of stock components, every reading that depends on hardware has a
calibration you can set, and nothing is stored beyond the six numbers that
calibrate this one device.

## The instruments

| Tool | Reads | Sensor | Notes |
| --- | --- | --- | --- |
| Level | pitch and roll | gravity | round vial and tube, zero point per device, degrees, percent or mm/m |
| Compass | heading and field | rotation vector, magnetometer | the same face doubles as a metal detector |
| Ruler | millimetres, inches | none | calibrated against a bank card, not against the panel's own claim |
| Sound level | dB(Z) | microphone | asks for the unprocessed source so gain control cannot rewrite the number |
| Tuner | pitch and cents | microphone | YIN detection, concert pitch adjustable from 415 to 466 Hz |
| Counter | a tally | none | tap anywhere, survives being closed |
| Altimeter | height and weather | barometer | height above a mark you set, which is exact enough to measure a staircase |
| Light meter | lux | light sensor | six decades, from moonlight to direct sun |

Tools whose sensor the device does not have are shown greyed out with the
reason, rather than opening and then apologising.

## Why it is small

No dependency injection framework, no navigation library, no HTTP client, no
image loader, no font file, no database. Compose, the activity, and the
lifecycle bridge for collecting sensor flows. That is the whole dependency list,
and the release APK is about 1.5 MiB.

## Building

Requirements: JDK 21, Android SDK 36, Gradle (wrapper included).

```bash
git clone https://github.com/john-athan/nonius
cd nonius
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Checks, the same ones CI runs:

```bash
tools/check.sh
```

## Layout

```
app/src/main/kotlin/dev/kaleve/nonius/
  core/       the arithmetic, plain Kotlin, no Android import, unit tested
  sensor/     sensors and the microphone as flows
  data/       six settings on SharedPreferences
  ui/         palette, type, the kit every instrument is drawn from
  tools/      one file per instrument
  Tools.kt    the registry: a ninth instrument is a file and a line here
```

Everything in `core/` runs on the JVM without a device and without Robolectric,
which is why the tilt maths, the barometric formula, the pitch detection and the
decibel arithmetic have tests and the drawing does not.

## Licence

GPL-3.0-or-later. The libraries inside the APK are Apache-2.0 and their notice
is readable in the app under Licences.
