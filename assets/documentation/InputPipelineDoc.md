# InputPipelineDoc

_Generated: 2026-04-03_

## Overview
InputPipelineDoc covers 3 classes across 3 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 3 imports.

## Package Breakdown

### `inputpipeline`

| Class | Role |
|---|---|
| `InputPipeline` | Pipeline registration entry point that wires dependency order. |

### `inputpipeline.input`

| Class | Role |
|---|---|
| `InputHandle` | Persistent manager-registered wrapper around data. |

### `inputpipeline.inputsystem`

| Class | Role |
|---|---|
| `InputSystem` | Single-job helper system for focused tasks. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `InputHandle` | `program.bootstrap.inputpipeline.input` | Persistent manager-registered wrapper around data. |
| `InputPipeline` | `program.bootstrap.inputpipeline` | Pipeline registration entry point that wires dependency order. |
| `InputSystem` | `program.bootstrap.inputpipeline.inputsystem` | Single-job helper system for focused tasks. |

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
