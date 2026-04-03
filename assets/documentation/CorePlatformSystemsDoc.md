# CorePlatformSystemsDoc

_Generated: 2026-04-03_

## Overview
CorePlatformSystemsDoc covers 23 classes across 5 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 3 imports.

## Package Breakdown

### `app`

| Class | Role |
|---|---|
| `Application` | Specialized support class for this subsystem. |
| `ApplicationListener` | Specialized support class for this subsystem. |
| `CoreContext` | Runtime context that groups frame-executed systems. |
| `Game` | Specialized support class for this subsystem. |
| `Screen` | Specialized support class for this subsystem. |
| `Version` | Specialized support class for this subsystem. |

### `backends.lwjgl3`

| Class | Role |
|---|---|
| `DisplayModeStruct` | Lightweight struct without engine lifecycle. |
| `Lwjgl3Application` | Specialized support class for this subsystem. |
| `Lwjgl3ApplicationConfiguration` | Specialized support class for this subsystem. |
| `Lwjgl3GL` | Specialized support class for this subsystem. |
| `Lwjgl3Graphics` | Specialized support class for this subsystem. |
| `Lwjgl3Input` | Specialized support class for this subsystem. |
| `Lwjgl3ManagedWindow` | Specialized support class for this subsystem. |
| `Lwjgl3Window` | Specialized support class for this subsystem. |
| `Lwjgl3WindowAdapter` | Specialized support class for this subsystem. |
| `Lwjgl3WindowConfiguration` | Specialized support class for this subsystem. |

### `graphics`

| Class | Role |
|---|---|
| `Graphics` | Specialized support class for this subsystem. |

### `input`

| Class | Role |
|---|---|
| `Input` | Specialized support class for this subsystem. |
| `InputProcessor` | Specialized support class for this subsystem. |

### `settings`

| Class | Role |
|---|---|
| `EngineSetting` | Specialized support class for this subsystem. |
| `Loader` | Bootstrap loader that scans files and requests builds. |
| `Settings` | Specialized support class for this subsystem. |
| `SettingsDeserializer` | Specialized support class for this subsystem. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `Application` | `program.core.app` | Specialized support class for this subsystem. |
| `ApplicationListener` | `program.core.app` | Specialized support class for this subsystem. |
| `CoreContext` | `program.core.app` | Runtime context that groups frame-executed systems. |
| `DisplayModeStruct` | `program.core.backends.lwjgl3` | Lightweight struct without engine lifecycle. |
| `EngineSetting` | `program.core.settings` | Specialized support class for this subsystem. |
| `Game` | `program.core.app` | Specialized support class for this subsystem. |
| `Graphics` | `program.core.graphics` | Specialized support class for this subsystem. |
| `Input` | `program.core.input` | Specialized support class for this subsystem. |
| `InputProcessor` | `program.core.input` | Specialized support class for this subsystem. |
| `Loader` | `program.core.settings` | Bootstrap loader that scans files and requests builds. |
| `Lwjgl3Application` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3ApplicationConfiguration` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3GL` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3Graphics` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3Input` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3ManagedWindow` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3Window` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3WindowAdapter` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Lwjgl3WindowConfiguration` | `program.core.backends.lwjgl3` | Specialized support class for this subsystem. |
| `Screen` | `program.core.app` | Specialized support class for this subsystem. |
| `Settings` | `program.core.settings` | Specialized support class for this subsystem. |
| `SettingsDeserializer` | `program.core.settings` | Specialized support class for this subsystem. |
| `Version` | `program.core.app` | Specialized support class for this subsystem. |

## Naming Convention Reference

| Suffix | Meaning |
|---|---|
| `Data` | Raw data payload struct used by handles/instances. |
| `Handle` | Persistent manager-registered wrapper around data. |
| `Instance` | Runtime mutable clone of a handle. |
| `Manager` | Owns registration, lifecycle, and public retrieval. |
| `Loader` | Bootstrap loader that scans files and requests builds. |
| `Builder` | Bootstrap builder that parses source data into handles. |
| `Branch` | Manager-owned internal computation branch. |
| `System` | Single-job helper system for focused tasks. |
| `Struct` | Lightweight struct without engine lifecycle. |
