# Changelog

All notable changes to **Čkiletova tabla** are recorded in this file. Entries are grouped by released application version, with the newest release first.

## [1.1.7] — 2026-09-01

### Changed

- Reduced the protected tournament-reset countdown from ten seconds to five seconds.
- Widened the goal-difference column so signed values remain on one line.
- Condensed the README feature overview for easier scanning.
- Updated regression and Appium coverage for the reset countdown and signed goal differences.

## [1.1.6] — 2026-09-01

### Added

- Added Excel export after a tournament finishes.
- Added a final standings sheet with the persisted tournament date and a results sheet containing every match.
- Added an Android share-chooser handoff with the generated workbook attached.
- Added regression and Appium coverage for export availability and workbook sharing.

## [1.1.5] — 2026-08-16

### Added

- Added this version changelog.
- Added the current version's release notes to the in-app Info dialog.
- Added bottom spacing below the Info button so it is not obscured by Android navigation controls.

## [1.1.4] — 2026-08-08

### Changed

- Tournament type is no longer selected by default.
- Tournament numeric settings are empty by default.
- Tournament type and matches per contestant are required before a tournament can start.
- League and knockout tournaments additionally require a valid even number of knockout qualifiers.
- Resetting a tournament returns all tournament settings to their unselected state.

## [1.1.3] — 2026-08-07

### Changed

- Removed home and away labels from knockout matches and knockout match histories.
- Highlighted the winner of a completed league-only tournament in green.
- Muted contestants who do not qualify for the knockout stage.
- Muted eliminated contestants in completed knockout bracket matches.

## [1.0.2] — 2026-08-07

### Added

- Added permanent labels above tournament-setting number fields.
- Added an explicit **GO TO KNOCKOUT** transition after the league stage.
- Added a horizontally scrollable knockout bracket.
- Added persistent knockout rounds, byes, pairings, and results.

## [1.0.1] — 2026-08-07

### Added

- Added finite league-only and league-plus-knockout tournament formats.
- Added configurable league matches per contestant and knockout qualifier counts.
- Added automatic knockout qualification, byes, winner progression, and tournament completion.
- Added round-based league scheduling to limit contestant waiting time.
- Added played-match totals to the league table.
- Added the black-and-white goalkeeper cat application icon.

### Changed

- Made the Info and team-assignment controls more subtle.
- Moved the Info control to the bottom center of the setup screen.

## [1.0.0] — 2026-08-07

### Added

- Added contestant entry for two to eight players.
- Added optional football-team assignment for each contestant.
- Added randomized home-and-away league scheduling.
- Added standings with points, played matches, wins, draws, losses, and goal difference.
- Added match entry with result confirmation.
- Added contestant-specific and complete match histories.
- Added correction of the latest match result.
- Added persistent local tournament storage.
- Added a protected ten-second tournament reset.
- Added the Info dialog with author and application version details.
