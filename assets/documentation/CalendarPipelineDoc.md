# CalendarPipelineDoc

_Generated: 2026-04-03_

## Overview
CalendarPipelineDoc covers 14 classes across 5 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 14 imports.

## Package Breakdown

### `calendarpipeline`

| Class | Role |
|---|---|
| `CalendarPipeline` | Pipeline registration entry point that wires dependency order. |

### `calendarpipeline.calendar`

| Class | Role |
|---|---|
| `CalendarData` | Raw data payload struct used by handles/instances. |
| `CalendarHandle` | Persistent manager-registered wrapper around data. |

### `calendarpipeline.calendarmanager`

| Class | Role |
|---|---|
| `CalendarManager` | Owns registration, lifecycle, and public retrieval. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |

### `calendarpipeline.clock`

| Class | Role |
|---|---|
| `ClockData` | Raw data payload struct used by handles/instances. |
| `ClockHandle` | Persistent manager-registered wrapper around data. |

### `calendarpipeline.clockmanager`

| Class | Role |
|---|---|
| `ClockManager` | Owns registration, lifecycle, and public retrieval. |
| `CurrentTrackerBranch` | Manager-owned internal computation branch. |
| `DayTrackerBranch` | Manager-owned internal computation branch. |
| `InternalBufferBranch` | Manager-owned internal computation branch. |
| `MonthTrackerBranch` | Manager-owned internal computation branch. |
| `YearTrackerBranch` | Manager-owned internal computation branch. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `CalendarData` | `program.bootstrap.calendarpipeline.calendar` | Raw data payload struct used by handles/instances. |
| `CalendarHandle` | `program.bootstrap.calendarpipeline.calendar` | Persistent manager-registered wrapper around data. |
| `CalendarManager` | `program.bootstrap.calendarpipeline.calendarmanager` | Owns registration, lifecycle, and public retrieval. |
| `CalendarPipeline` | `program.bootstrap.calendarpipeline` | Pipeline registration entry point that wires dependency order. |
| `ClockData` | `program.bootstrap.calendarpipeline.clock` | Raw data payload struct used by handles/instances. |
| `ClockHandle` | `program.bootstrap.calendarpipeline.clock` | Persistent manager-registered wrapper around data. |
| `ClockManager` | `program.bootstrap.calendarpipeline.clockmanager` | Owns registration, lifecycle, and public retrieval. |
| `CurrentTrackerBranch` | `program.bootstrap.calendarpipeline.clockmanager` | Manager-owned internal computation branch. |
| `DayTrackerBranch` | `program.bootstrap.calendarpipeline.clockmanager` | Manager-owned internal computation branch. |
| `InternalBufferBranch` | `program.bootstrap.calendarpipeline.clockmanager` | Manager-owned internal computation branch. |
| `InternalBuilder` | `program.bootstrap.calendarpipeline.calendarmanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.calendarpipeline.calendarmanager` | Manager-created internal loader helper. |
| `MonthTrackerBranch` | `program.bootstrap.calendarpipeline.clockmanager` | Manager-owned internal computation branch. |
| `YearTrackerBranch` | `program.bootstrap.calendarpipeline.clockmanager` | Manager-owned internal computation branch. |

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
