# AdventureRPG Engine — Context Cheat Sheet
*Paste this at the top of any new conversation to onboard Claude instantly.*

---

## Project Overview
A Java/libGDX voxel game engine. The codebase lives under `com.internal.core`. All engine-managed objects follow a strict class hierarchy and lifecycle enforced by the engine itself.

---

## Class Hierarchy (inheritance chain)

```
EngineUtility
  └── UtilityPackage          — static helpers only, never instantiated
  └── StructPackage           — plain data containers, use `new` freely
  └── InstancePackage         — engine-managed, created with create()
        └── DataPackage       — bootstrap-only resource, use constructor() for pooling
        └── HandlePackage     — persistent resource, use constructor() for pooling
              └── AsyncContainerPackage  — ThreadLocal-based, reset() called automatically
              └── SyncContainerPackage   — shared + AtomicBoolean lock, tryAcquire()
  └── SystemPackage           — full lifecycle system
        └── ManagerPackage    — manages child systems, propagates lifecycle
              └── EnginePackage — root; state machine, global registry
                    └── GameEngine — concrete impl, registers 10 pipelines in bootstrap()
```

---

## Instantiation Rules (critical)

| Type | How to create |
|---|---|
| StructPackage | `new MyStruct()` — only type where `new` is allowed |
| InstancePackage and below | `create(MyClass.class)` — never use `new` |
| DataPackage / HandlePackage pooling | `constructor(...)` — NOT a Java constructor, called after pool reuse |
| Raw Java types (String, List, etc.) | `new` is fine |

**Never use `new` on anything that extends InstancePackage or below.**
**All engine-managed classes must be `public`** (reflection requirement).
**No `final` fields on Handle/Instance types** (pool reuse requires re-initialisation via `constructor()`).

---

## Lifecycle Phases (in order)

`KERNEL → BOOTSTRAP → CREATE → GET → AWAKE → RELEASE → START → UPDATE → FIXED_UPDATE → LATE_UPDATE → RENDER → DRAW → DISPOSE`

- **RELEASE** is only for bootstrap-only systems. Never call `release()` on a system that participates in UPDATE or anything after AWAKE.
- When a ManagerPackage releases a child ManagerPackage, its `systemCollection` migrates upward before removal.

---

## Threading Model (InternalThreadManager)

Five `executeAsync` signatures:

1. `executeAsync(ThreadHandle, Runnable)` — fire and forget
2. `executeAsync(ThreadHandle, T extends AsyncContainerPackage, AsyncStructConsumer<T>)` — per-thread isolated work; `reset()` called automatically
3. `executeAsync(ThreadHandle, AsyncStructConsumerMulti, ...)` — multiple async containers in one job
4. `executeAsync(ThreadHandle, T extends SyncContainerPackage, SyncStructConsumer<T>)` — shared lock; `tryAcquire()` returns false if already locked
5. `executeAsync(ThreadHandle, T extends AsyncContainerPackage, S extends SyncContainerPackage, BiSyncAsyncConsumer<T,S>)` — async + sync together

**Critical anti-patterns:**
- Never call `isLocked()` then `tryAcquire()` — TOCTOU gap. `tryAcquire()` already returns false when locked.
- Never introduce a flag to track in-flight status — the lock IS the in-flight flag.

---

## SystemContext & EngineState

- `SystemContext` — int bitmask (not short). Each lifecycle phase has an `entryMask` of legal predecessors. `verifyContext()` blocks illegal transitions.
- `EngineState` — six high-level states: `KERNEL, BOOTSTRAP, SETUP, RUNNING, PAUSED, DISPOSED`. Drives the `execute()` switch.
- `EnginePackage` overrides `verifyContext()` to always return true — it drives all transitions itself.

---

## Math / Utility Library

### Coordinate Packers (all static, final classes, no instances)
| Class | Packs into | Layout | Notes |
|---|---|---|---|
| `Coordinate2Long` | `long` | High 32 = X, Low 32 = Y | Signs preserved on unpack |
| `Coordinate3Int` | `int` | Y[29:20] Z[19:10] X[9:0] | 10 bits each, -512 to 511 |
| `Coordinate3Long` | `long` | X[63:38] Y[37:26] Z[25:0] | Unsigned unpack; neighbor add is raw long addition |

### Direction Enums
- `Direction2Vector` — 8 values (N/NE/E/SE/S/SW/W/NW). `to3D()` returns null for diagonals — always null-check.
- `Direction3Vector` — 6 values (N/E/S/W/UP/DOWN). Each pre-computes `coordinate2Long`, `coordinate3Long`, `vertOffset3Int`. `remapFace(orientation, worldFace)` and `getEncodedFace(orientation, worldFace)` use 24×6 lookup tables. Orientation = facing(0-5) × 4 + spin(0-3).

### Vectors (all extend UtilityPackage)
All fields are `public` (not final). All mutating methods return `this`.
- `Vector2 / Vector2Double / Vector2Int / Vector2Boolean`
- `Vector3 / Vector3Double / Vector3Int / Vector3Boolean`
- `Vector4 / Vector4Double / Vector4Int / Vector4Boolean`

Float/Double variants: `length()`, `lengthSquared()`, `normalize()` (no-op on zero length).
Int variants: `pack()` → delegates to matching Coordinate packer.
Boolean variants: `and / or / xor / not`. Aggregates: `any() / all()`.
Direction methods on Vector2: `up/down/left/right`. On Vector3: `up/down/north/south/east/west`.

### Matrices (all extend UtilityPackage)
Storage: `public final float[] val` (or `double[]`), column-major (OpenGL). Constructor args are row-major and transposed internally. Element accessor pattern: `getM<row><col>()` / `setM<row><col>()`.
- `Matrix2 / Matrix2Double` — val[4]
- `Matrix3 / Matrix3Double` — val[9], has `fromGDX()`
- `Matrix4 / Matrix4Double` — val[16], has `fromGDX()`

**Warning:** `multiply(scalar)` multiplies only the diagonal, not every element.
`divide(Matrix)` = multiply by inverse. `inverse()` throws if det == 0.

### Quaternion
`public final float[] val` — layout `[W, X, Y, Z]`. `multiply(Quaternion)` is Hamilton product. `multiply(scalar)` scales W only. `conjugate()` negates X/Y/Z. `setFromEulerAngles(yaw, pitch, roll)` takes degrees.

### Color
`public float r, g, b, a`. Static constants: WHITE, BLACK, RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, GRAY, CLEAR. `toPackedFloat()` / `fromPackedFloat()` use ABGR8888 (libGDX convention; high alpha bit masked). `lerp(Color, float t)` mutates `this`.

### OpenSimplex2 (static, no engine base class)
All methods take `long seed` first, return `float`.
- 2D: `noise2` / `noise2_ImproveX` (better for vertical sandbox worlds)
- 3D: `noise3_ImproveXY` (Z is "different"), `noise3_ImproveXZ` (Y is "different" — use for terrain as `noise3_ImproveXZ(x, Y, z)`), `noise3_Fallback`
- 4D: `noise4_ImproveXYZ_ImproveXY/XZ`, `noise4_ImproveXYZ`, `noise4_ImproveXY_ImproveZW` (seamless loop trick), `noise4_Fallback`

### FileUtility
`verifyDirectory`, `collectFiles` (recursive), `collectFilesShallow`, `collectSubdirectories`, `collectAllSubdirectories`, `getFileName`, `getExtension`, `hasExtension`, `getPathWithFileNameWithout/WithExtension`, `splitFileNameByUnderscore` (exactly one underscore required).

### JsonUtility
`validate*` methods throw on missing/wrong-type keys. `get*` methods return defaults.
`loadJsonObject(File)`, `validateString/Int/Boolean/Float/Array`, `validateArray(json, key, requiredSize)` (pass 0 to skip size check), `getString/getInt/getBoolean/getFloat`.

### Queue System
`QueueInstance extends InstancePackage` — round-robin multi-queue dispatcher.
`addQueueItem(String)` — call during CREATE. `getNextQueueItem()` — returns current queue or null on frame limit. Rotates queues on batch limit. Limits sourced from `EngineSetting.MAX_CHUNK_STREAM_PER_FRAME` and `MAX_CHUNK_STREAM_PER_BATCH`.

---

## Bootstrap Flow

```
KERNEL   → InternalThreadManager created, thread defs loaded from internal/threads JSON
BOOTSTRAP → 10 pipelines registered: Geometry, Shader, Render, World, Physics,
             Input, Entity, Calendar, Lighting, Menu
CREATE   → Runtime systems allocated; full CREATE → GET → AWAKE → RELEASE cycle
START    → Final pre-loop setup
UPDATE   → Per-frame cycle begins
```

---

## Settings

Built via Builder pattern, deserialized from `settings.json` via `SettingsDeserializer + Loader`.
Key defaults: FOV=70, windowWidth=1280, windowHeight=720, maxRenderDistance=64, FIXED_TIME_STEP=0.02f.
`EngineSetting` — static constants for asset paths, chunk sizes, texture dimensions, timing values.
`debug=true` enables settings persistence on shutdown via `Main.dispose()`.

Fixed update accumulator: deltaTime added to elapsedTime each frame; FIXED_UPDATE stepped for each full fixedInterval elapsed; capped at maxSteps=5 to prevent spiral-of-death.

---

## Code Style Conventions

- Section headers in source: `// Section Name \\` (backslash on both ends)
- No Javadoc — inline comments only, kept brief
- `constructor()` is always package-private (`void constructor(...)`) — never `public`, never a Java constructor
- All `throwException(...)` calls route through `UtilityPackage` — never throw directly
- `public static final` arrays on enums: `VALUES = values()`, `LENGTH = values().length`
- Static lookup tables initialised in `static { }` blocks, one block per logical group
- Chaining is universal — every mutating method on vectors/matrices/color/quaternion returns `this`

---

## Document History

The CoreEngineRulebook.docx covers sections 1–27:
1–18: Core engine packages (class hierarchy, lifecycle, threading, settings, WindowInstance, type contracts)
19: Utility classes (FileUtility, JsonUtility, PixmapUtility)
20: Queue system (QueueInstance, QueueItemHandle)
21: Coordinate packers (Coordinate2Long, Coordinate3Int, Coordinate3Long)
22: Direction enums (Direction2Vector, Direction3Vector)
23: Color
24: Vector types (all 12)
25: Matrix types (all 6)
26: Quaternion
27: OpenSimplex2
