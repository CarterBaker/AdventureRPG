# Engine Cleanup Plan — Full
*All tracks. Ordered by dependency and risk.*

---

## Track 1 — Remove `render()` from the Lifecycle

### What
`render()` has zero overrides anywhere in the codebase. It exists as a dead lifecycle slot
between `lateUpdate()` and `draw()`. Remove it entirely and wire `draw()` to follow
`lateUpdate()` directly.

### Files
| File | Change |
|---|---|
| `SystemPackage.java` | Remove `render()` and `internalRender()` |
| `ManagerPackage.java` | Remove `internalRender()` override |
| `EnginePackage.java` | Remove `internalRender()` override; remove call from `updateCycle()` |
| `SystemContext.java` | Remove `RENDER` entry entirely; change `DRAW("RENDER")` → `DRAW("LATE_UPDATE")` |

`updateCycle()` becomes:
```java
private final void updateCycle() {
    this.internalUpdate();
    this.internalFixedUpdate();
    this.internalLateUpdate();
    this.internalDraw();
}
```

**Risk: ZERO.** Nothing overrides `render()`.

---

## Track 2 — Data → Struct, Embedded in Handles

### The Rule
Every `DataPackage` subclass is a plain value bag that pays full engine lifecycle overhead
(ThreadLocal creation struct, CREATE/GET/AWAKE phases, engine registry) for no benefit.
Convert all 12 to `StructPackage` with real Java constructors. Loaders use `new` directly.
`DataPackage.java` is deleted when all 12 are converted.

**Before:**
```java
XData data = create(XData.class);
data.constructor(a, b, c);
```
**After:**
```java
XStruct data = new XStruct(a, b, c);
```

Any `get()` override that initialized collections just moves into the constructor.

### Conversion Order (leaves before roots)

**Group A — No DataPackage dependencies:**
| Class | Rename to |
|---|---|
| `UniformData` | `UniformStruct` |
| `GridSlotData` | `GridSlotStruct` |
| `TextureAtlasData` | `TextureAtlasStruct` |
| `SpriteData` | `SpriteStruct` |
| `AliasData` | `AliasStruct` |

**Group B — Depends on Group A:**
| Class | Rename to |
|---|---|
| `UBOData` | `UBOStruct` |
| `TextureArrayData` | `TextureArrayStruct` |
| `EntityData` | `EntityStruct` |
| `ElementData` | `ElementStruct` |
| `MenuData` | `MenuStruct` |

**Group C — Depends on Group B:**
| Class | Rename to |
|---|---|
| `ShaderData` | `ShaderStruct` |
| `ShaderDefinitionData` | `ShaderDefinitionStruct` |

**Abstract case:**
`AtlasTileData` → `AtlasTileStruct extends StructPackage` (abstract).
Subclasses `TextureTileData` and `FontTileData` update accordingly.
`AtlasUtility` writes `atlasX/atlasY` back into the struct — mutable fields on a struct are fine.

### Handle Embedding
After conversion, handles that held a DataPackage ref hold the Struct directly:
```java
// EntityHandle before:
private EntityData entityData;

// EntityHandle after:
private EntityStruct entityStruct;
```
Manager registry maps rename in kind. When a runtime Instance is eventually cloned from a
Handle, the struct is copied across — same model as MeshHandle → MeshInstance today.

**Final delete:** `DataPackage.java` once all 12 are done.

---

## Track 3 — Fix `StatisticsStruct` → `StatisticsHandle`

### What
`StatisticsStruct extends InstancePackage` — named Struct, typed Instance, should be neither.
Statistics are held by an entity for its entire lifetime as an immutable definition set at
spawn. That's a Handle — same pattern as `BehaviorHandle` and `InventoryHandle` on `EntityHandle`.

### Fix
- Rename `StatisticsStruct` → `StatisticsHandle`
- Change `extends InstancePackage` → `extends HandlePackage`
- Update `EntityHandle` field declaration and any references to the type name

The `create(StatisticsHandle.class)` call in `EntityHandle.create()` is already correct —
handles are created through the engine exactly this way. Only the class declaration and name change.

---

## Track 4 — Delete Ghost Directory `entityManager/`

### What
There are two directories:
- `entitypipeline/entityManager/` — capital M, contains only `EntityManager.java`
- `entitypipeline/entitymanager/` — lowercase, contains `EntityManager.java` + `InternalBuilder.java` + `InternalLoader.java`

The capital-M `EntityManager.java` declares `package com.internal.bootstrap.entitypipeline.entitymanager`
(lowercase) in its own header — it's a misplaced duplicate of the file that already exists in
the correct location. Delete the entire `entityManager/` directory.

---

## Track 5 — Fix Package Directory Casing

Java package names must be all lowercase. Four directories violate this:

| Directory | Fix |
|---|---|
| `shaderpipeline/Shader/` | rename to `shader/` |
| `shaderpipeline/Texture/` | rename to `texture/` |
| `core/util/mathematics/Extras/` | rename to `extras/` |
| `core/kernel/SyncConsumer/` | rename to `syncconsumer/` |

All `package` declarations in files inside these directories update to match.
All `import` statements referencing these packages update across the codebase.

**Note:** This is a rename-only change — no logic touches. But it affects imports broadly so
do it as a single commit per directory with IDE-assisted refactor, not by hand.

---

## Track 6 — Rename `WorldRenderInstance.dispose()`

### What
`WorldRenderInstance` has a `public void dispose()` method used by `ChunkQueueManager` and
`MegaQueueManager` to unload a render instance. `InstancePackage` doesn't currently have a
lifecycle `dispose()`, but if one is ever added, this method silently becomes it — the wrong
behavior with no compiler warning.

### Fix
Rename `WorldRenderInstance.dispose()` → `WorldRenderInstance.unload()`.
Update the 4 call sites in `ChunkQueueManager` and `MegaQueueManager`.

---

## Track 7 — Remove Duplicate Comment in `LoaderPackage`

### What
The class-level Javadoc comment in `LoaderPackage.java` is pasted twice in full — the entire
block appears verbatim back-to-back at the top of the file. Delete the first copy.

---

## Track 8 — Document `GLSLUtility` Pattern (No Code Change)

### What
There are 11 separate `GLSLUtility.java` files across 11 packages. This is intentional and
correct — each wraps only the GL calls its subsystem needs, preventing cross-system GL
coupling. But it looks like duplication at first glance and will confuse anyone reading
the codebase fresh.

### Fix
Add one line to `ENGINE_CONTEXT.md`:

> **GLSLUtility** — each pipeline package has its own GLSLUtility containing only the GL
> calls that package is permitted to make. This is intentional scoping, not duplication.
> Never import another package's GLSLUtility.

---

## Execution Order

```
Track 4  Ghost directory delete     — 1 file deleted, zero risk
Track 7  Duplicate comment          — 1 line deleted, zero risk
Track 1  Remove render()            — 4 files, zero risk
Track 6  Rename dispose()           — 5 files, zero risk
Track 5  Package casing (per dir)   — IDE refactor, one directory at a time
Track 3  StatisticsStruct fix       — decide, then 1-2 files
Track 2  Data → Struct (per group)  — Group A → B → C → AtlasTile, then delete DataPackage
Track 8  Document GLSLUtility       — 1 line in ENGINE_CONTEXT.md
```

---

## Final State After All Tracks

### Type Taxonomy
| Type | Created by | Lifecycle | Purpose |
|---|---|---|---|
| `StructPackage` | `new` | None | Pure data — definitions, values, GPU handles |
| `HandlePackage` | `create()` + `constructor()` | None (manual) | Bootstrap definition, holds a Struct |
| `InstancePackage` | `create()` + `constructor()` | create/get/awake | Runtime object, copies Struct from Handle |
| `ManagerPackage` | `create()` | Full lifecycle | Owns and tracks handles/instances |
| `SystemPackage` | `create()` | Full lifecycle | Stateful sub-system |
| `BranchPackage` | `create()` | Full lifecycle | Logic branch |
| `BuilderPackage` | `create()` | Bootstrap only | Assembles Handles from Structs |
| `LoaderPackage` | `create()` | Bootstrap only | Parses files into Structs |

### Deleted
- `DataPackage.java` — absorbed by StructPackage
- `render()` / `internalRender()` / `SystemContext.RENDER` — dead lifecycle slot removed
- `entityManager/EntityManager.java` — ghost duplicate

### Packages Renamed (lowercase)
- `shaderpipeline.Shader` → `shaderpipeline.shader`
- `shaderpipeline.Texture` → `shaderpipeline.texture`
- `mathematics.Extras` → `mathematics.extras`
- `kernel.SyncConsumer` → `kernel.syncconsumer`
