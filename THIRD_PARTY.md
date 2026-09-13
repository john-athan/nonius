# Third-party material in Nonius

This app is GPL-3.0-or-later. Its own source was written for this project. This
file records the material that came from elsewhere and the obligations attached
to it.

## Dependencies shipped inside the APK

An Android APK bundles its libraries, so the published artifact contains this
code and carries its notices. All of it is Apache-2.0, which is compatible with
GPL-3.0 in this direction (Apache-2.0 code may be combined into a GPL-3.0 work,
not the reverse).

The four declared dependencies and everything they pull in, as resolved on the
release runtime classpath (177 coordinates, all Apache-2.0):

| Library | License | Copyright |
| --- | --- | --- |
| androidx.compose (ui, ui-graphics, foundation, animation) | Apache-2.0 | Copyright (c) The Android Open Source Project |
| androidx.activity:activity-compose | Apache-2.0 | Copyright (c) The Android Open Source Project |
| androidx.lifecycle:lifecycle-runtime-compose | Apache-2.0 | Copyright (c) The Android Open Source Project |
| the AndroidX libraries those three depend on (annotation, arch.core, autofill, collection, concurrent, core, customview, documentfile, dynamicanimation, emoji2, graphics, interpolator, legacy, loader, localbroadcastmanager, navigationevent, print, profileinstaller, savedstate, startup, tracing, transition, versionedparcelable, window) | Apache-2.0 | Copyright (c) The Android Open Source Project |
| Kotlin standard library, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 | Copyright (c) JetBrains s.r.o. |
| org.jetbrains:annotations | Apache-2.0 | Copyright (c) JetBrains s.r.o. |
| com.google.guava:listenablefuture (the empty placeholder artifact) | Apache-2.0 | Copyright (c) The Guava Authors |
| org.jspecify:jspecify | Apache-2.0 | Copyright (c) The JSpecify Authors |

Checked against the OSV database on 2026-09-13: 177 coordinates queried, no
advisory against any of them.

Apache-2.0 requires the license text and any NOTICE content to travel with the
binary. The Android toolchain does not do this on its own, so the app carries
`app/src/main/res/raw/licenses.txt` and shows it behind the Licences link at the
foot of the case. It reads with no network, which is also what F-Droid expects.

## Algorithms

The tuner uses the YIN difference function, published by Alain de Cheveigne and
Hideki Kawahara in 2002 ("YIN, a fundamental frequency estimator for speech and
music"). The paper describes the method; the implementation in
`core/Yin.kt` was written for this project from that description and copies no
code.

The altimeter uses the international barometric formula, which is a published
physical relation and not anyone's property.

## Measurements taken as given

ISO 7810 ID-1 gives the bank card used to calibrate the ruler as 85.60 mm by
53.98 mm. The standard is a specification, not code, and only the two numbers
are used here.

## Icons and metadata

`app/src/main/res/` and `fastlane/metadata/` contain the launcher icon and store
text produced for this project.
