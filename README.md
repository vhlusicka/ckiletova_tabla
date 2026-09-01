# Čkiletova tabla

Čkiletova tabla is a small offline Android application for recording FIFA tournaments between friends. It supports two to eight contestants and retains the current tournament on the device between app launches.

## Features

- Create tournaments for 2–8 uniquely named contestants, with optional football-team assignments.
- Play a finite league or a league followed by a knockout stage, with configurable match and qualifier counts.
- Automatically schedule fair home-and-away rounds, calculate standings and advance knockout winners.
- Record confirmed scores, review contestant or complete match histories, and correct the latest result.
- Follow tournament progress through winner highlighting, qualification states and a scrollable knockout bracket.
- Export final standings and all results to an Excel workbook through Android's share chooser.
- Keep tournament data offline between launches and reset it after a protected five-second countdown.

## League rules

A win is worth 3 points, a draw 1 point and a loss 0 points. Standings are ordered by points, then goal difference, then goals scored. The first contestant displayed in a fixture or result is the home contestant; the second is away.

The fixture queue is randomized within each complete home-and-away cycle. A new cycle begins only after all ordered pairings in the current cycle have been played.

## Requirements and installation

- Android 6.0 (API 23) or newer.
- Allow installation from the browser or file manager when Android asks for permission.
- Open the signed APK and select **Install**.

The release APK is generated in `app/build/outputs/apk/release/`.

## Building

The project uses Java 17, Gradle 8.9 and Android SDK 35. From the project root, run:

```sh
./gradlew assembleRelease
```

Release updates must be signed with the same `release-key.jks` file. Keep this file and its credentials private and backed up; Android will reject an update signed with a different key.

## Project information

- Author: Vilim Hlušička
- Initial version date: 6 August 2026
- Latest version date: 1 September 2026
- Current version: 1.1.7
- Application ID: `com.debelatabla.fifaleague`
