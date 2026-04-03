# LightingPipelineDoc

_Generated: 2026-04-03_

## Overview
LightingPipelineDoc covers 5 classes across 3 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 5 imports.

## Package Breakdown

### `lightingpipeline`

| Class | Role |
|---|---|
| `LightingPipeline` | Pipeline registration entry point that wires dependency order. |

### `lightingpipeline.directionallight`

| Class | Role |
|---|---|
| `DirectionalLightHandle` | Persistent manager-registered wrapper around data. |

### `lightingpipeline.naturallightmanager`

| Class | Role |
|---|---|
| `MoonLightSystem` | Single-job helper system for focused tasks. |
| `NaturalLightManager` | Owns registration, lifecycle, and public retrieval. |
| `SunLightSystem` | Single-job helper system for focused tasks. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `DirectionalLightHandle` | `program.bootstrap.lightingpipeline.directionallight` | Persistent manager-registered wrapper around data. |
| `LightingPipeline` | `program.bootstrap.lightingpipeline` | Pipeline registration entry point that wires dependency order. |
| `MoonLightSystem` | `program.bootstrap.lightingpipeline.naturallightmanager` | Single-job helper system for focused tasks. |
| `NaturalLightManager` | `program.bootstrap.lightingpipeline.naturallightmanager` | Owns registration, lifecycle, and public retrieval. |
| `SunLightSystem` | `program.bootstrap.lightingpipeline.naturallightmanager` | Single-job helper system for focused tasks. |

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
