# Čkiletova tabla

Čkiletova tabla is a small offline Android application for recording FIFA tournaments between friends. It supports two to eight contestants and retains the current tournament on the device between app launches.

## Features

- Add 2–8 uniquely named contestants.
- Optionally assign a football team to each contestant.
- Automatically schedule repeating home-and-away league cycles.
- Arrange fixtures in rounds so every available contestant plays before the next round begins, preventing long waits between matches.
- Ensure every contestant plays every opponent once at home and once away per complete cycle.
- Enter a score and confirm it before it is recorded.
- Calculate points, wins, draws, losses and goal difference automatically.
- Open any contestant to see their complete match history.
- View all recorded matches with home and away teams clearly identified.
- Correct the result of the latest match.
- Reset the tournament after a protected ten-second countdown.
- Store all tournament information locally; no account or internet connection is required.

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
- Latest version date: 7 August 2026
- Current version: 1.0.0
- Application ID: `com.debelatabla.fifaleague`
