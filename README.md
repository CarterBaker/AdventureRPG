# AdventureRPG

AdventureRPG is a custom Java/LWJGL engine project with a desktop runtime target.

## Project modules

- `core` — shared application and engine logic.
- `lwjgl3` — desktop launcher/runtime integration (LWJGL3).

## Getting started

This project uses [Gradle](https://gradle.org/) with the included wrapper.

- Windows: `gradlew.bat <task>`
- macOS/Linux: `./gradlew <task>`

## Common Gradle tasks

- `lwjgl3:run` — run the desktop application.
- `lwjgl3:jar` — build a runnable JAR (`lwjgl3/build/libs`).
- `build` — compile and package all modules.
- `test` — run tests (if present).
- `clean` — remove module build output.

You can scope shared tasks to a specific module with `<module>:<task>`.
For example: `core:clean`.

## Helpful Gradle flags

- `--daemon` — use the Gradle daemon.
- `--offline` — use cached dependencies only.
- `--refresh-dependencies` — re-resolve dependencies.
- `--continue` — continue as far as possible after task failures.
