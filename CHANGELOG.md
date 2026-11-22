# Changelog

All notable changes to this project are documented here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/). The short per-release notes
that F-Droid shows live in `fastlane/metadata/android/en-US/changelogs/`.

## [Unreleased]

## [1.7] - 2026-11-22

### Changed
- Build tooling and dependency updates only

## [1.6] - 2026-11-16

### Changed
- New launcher icon: a vernier scale, the instrument Nonius is named after
- The home screen no longer lists network status, instrument count and licence name; the wordmark opens the licences instead

## [1.5] - 2026-11-11

### Fixed
- Dragging an adjuster stays active and keeps fractional movement instead of dropping it

### Changed
- Built against SDK 37, Compose BOM and lifecycle lifted, Gradle 9.9.1

## [1.4] - 2026-10-12

### Changed
- Build tooling and dependency updates only

## [1.3] - 2026-09-27

### Added
- A sample-accurate metronome
- A G-force meter
- The volume keys count on the Counter

### Fixed
- The metronome's beat label has room above its adjusters

## [1.2] - 2026-09-27

### Added
- A-weighted dB(A) on the sound level meter, toggled against dB(Z)
- Minimum hold next to the peak hold
- True north on the compass, through a declination setting

### Fixed
- The Counter counts every finger, not one per gesture
- Switching weighting starts the holds over

## [1.1] - 2026-09-21

### Added
- The Level turns to landscape

### Fixed
- A busy microphone no longer crashes the recorder

## [1.0] - 2026-09-13

First release. Level, compass, ruler, sound level, tuner, counter, altimeter and light meter.
