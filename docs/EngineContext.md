# AdventureRPG — Engine Context

## Package Hierarchy

```
EngineUtility
├── StructPackage           ← independent, no lifecycle, no engine timing
│   └── DataPackage         ← raw data payload held inside a Handle or Instance
└── InstancePackage         ← engine-managed, lifecycle-aware
    └── HandlePackage       ← persistent, shareable, long-lived reference
```

- **StructPackage** — No lifecycle. Safe anywhere. Use `new`.
- **DataPackage** — Extends StructPackage. Raw data payload. Use `new`.
- **InstancePackage** — Engine-managed. Use `create(Class)` only.
- **HandlePackage** — Extends InstancePackage. Persistent, long-lived. Use `create(Class)` only.
- **ManagerPackage** — Owns registration, lifecycle, and access for a system.
- **BuilderPackage** — Parses external data, drives Handle creation. Bootstrap-only, self-destructs.
- **LoaderPackage** — Extends ManagerPackage. Scans directory, batch-processes files. Self-destructs when queue exhausted. Owns and auto-releases child builders.
- **BranchPackage** — Internal computation unit owned by a Manager. Lives in same package as manager.
- **SystemPackage** — Self-contained single-job helper for a manager. No branching.

---

## Strict Naming Rule

> **The last word of every class name must match the package it extends. No exceptions.**

| Class Name  | Must Extend       |
|-------------|-------------------|
| `XData`     | `DataPackage`     |
| `XHandle`   | `HandlePackage`   |
| `XInstance` | `InstancePackage` |
| `XManager`  | `ManagerPackage`  |
| `XBuilder`  | `BuilderPackage`  |
| `XLoader`   | `LoaderPackage`   |
| `XStruct`   | `StructPackage`   |
| `XBranch`   | `BranchPackage`   |
| `XSystem`   | `SystemPackage`   |

---

## When to Use DataPackage

- **Manager owns the Handle** → Handle gets a DataPackage. Data is the template. Handle is the managed wrapper.
- **No Manager owns it** → Skip DataPackage. Handle or Instance is self-contained.

---

## Handle vs Instance — Lifetime Rule

- **Handle** — original. Registered in manager palette. Lives from start to shutdown. Manager owns it.
- **Instance** — clone. Handed out at runtime. Safe to mutate. Safe to dispose. Not registered anywhere.

Both wrap the same DataPackage type. When cloning, always deep-copy the DataPackage — never share mutable Data between Handle and Instance.

```java
XData clonedData = new XData(handle.getXData());
XInstance instance = create(XInstance.class);
instance.constructor(clonedData);
```

---

## Struct vs Instance Decision Rule

Does it need `internal`? **No → Struct. Yes → Instance or higher.**

StructPackage has no `create()`, `get()`, `awake()`, or `update()`. If it just holds data and gets passed around, it is a Struct and uses `new`.

---

## Handle and Instance Rules

- Holds DataPackage internally. Delegates all getters and setters through it.
- Exposes a typed `getXData()` getter.
- Engine-side initialization uses `constructor(XData data)`.
- No generic `DataPackage` field on base packages. Keep everything typed. No casts.
- All fields `private`. No public fields ever.
- Immutable fields use `private final` with getters only.

---

## Branch vs System

- **Branch** — manager needs to perform the same operation differently depending on input or context. Manager routes between branches.
- **System** — manager needs a focused, self-contained helper that does one specific job. No routing.

---

## Branch and System Rules

- Live in the **same package** as their Manager. Never a sub-package.
- All methods only callable by the manager are **package-private**.
- Only methods genuinely needed externally get `public`.
- Created via `create(XBranch.class)` or `create(XSystem.class)` inside the manager's `create()`.

---

## Manager Rules

- `create()` — initializes all maps, creates branches/systems, calls `create(InternalLoader.class)` after maps are initialized.
- `get()` — resolves all cross-system references. Only place this is allowed.
- `addX(XHandle handle)` — package-private, called by loader only.

---

## Manager Registry Pattern

Two maps — name-to-ID and ID-to-handle. Never name-to-handle directly.

```java
private Object2IntOpenHashMap<String> xName2XID;
private Int2ObjectOpenHashMap<XHandle> xID2XHandle;
```

Use `Short2ObjectOpenHashMap` and `short` IDs for small ID ranges (VAO, VBO, IBO, Calendar, Behavior).
Use `Int2ObjectOpenHashMap` and `int` IDs for larger namespaces (Mesh, Entity, Item).

IDs always derived from resource name via `RegistryUtility` — never sequential counters.

```java
void addXHandle(String name, XHandle handle) {
    int id = RegistryUtility.toIntID(name);
    xName2XID.put(name, id);
    xID2XHandle.put(id, handle);
}
```

### Required Public Methods — all four, always

```java
public boolean hasX(String xName)
public int getXIDFromXName(String xName)
public XHandle getXHandleFromXID(int xID)
public XHandle getXHandleFromXName(String xName)
```

Method names fully explicit — `getXIDFromXName`, `getXHandleFromXID`, `getXHandleFromXName`. Never abbreviated.

Map field names follow `xName2XID` and `xID2XHandle` — explicit about what goes in and what comes out. Never `palette`, `registry`, `map`.

---

## Builder Rules

- Bootstrap-only. Created in loader's `create()`, auto-released when loader finishes.
- Constructs `XData` with `new`, calls `create(XHandle.class)`, calls `handle.constructor(data)`.
- Never call a load-triggering lookup — use direct lookup only.
- Check `hasX(name)` before doing work to avoid duplicate creation.

---

## Loader Rules

- `create()` — initializes `root`, file registry map, and builder.
- `get()` — resolves manager reference only.
- `scan()` — walks directory, populates file registry and `fileQueue`. No initialization here.
- `load(File)` — resolves name, calls builder, registers result with manager.
- `request(String name)` — resolves name to File, calls `request(File)` for on-demand loading.
- Use `Files.walk()` in `scan()` for recursive subdirectory structures.

### Loader Phase Order
`create()` → `get()` → `scan()` → `// Load \\` → `// On-Demand \\`

### Manager Phase Order
`create()` → `get()` → `// Management \\` → `// Accessible \\`

---

## Data Creation Flow

```
JSON file
  └── InternalLoader.load(File)
        └── InternalBuilder.build(File, name)
              └── new XData(...)
                    └── create(XHandle.class)
                          └── handle.constructor(XData)
                                └── Manager.addX(handle)
```

---

## General Engine Rules

- All engine-managed objects (`InstancePackage` and subclasses) must be instantiated via `create(Class)` — never `new`.
- `StructPackage` and `DataPackage` may use `new` freely.
- `get()` is the only phase where cross-system references may be resolved.
- `throwException(String)` for all error handling — never `throw new RuntimeException` or any raw Java exception.
- Entities are always handed out as `EntityInstance`, never `EntityHandle`.
- On-demand loading is always triggered through the manager's get method — never call `request()` externally.

---

## Settings vs EngineSetting

- **`EngineSetting`** — compile-time constants. `public static final` everything. Magic numbers, tuning values, paths, limits, named bit constants.
- **`Settings`** — user-configurable runtime values. FOV, window size, render distance, mouse sensitivity. Accessed via `internal.settings.whatever`.

If it has a unit the user would recognize (pixels, degrees, sensitivity) it is a `Settings` value. If it is an engine constant that should never vary, it belongs in `EngineSetting`.

---

## Builder Pattern Exception

The `Builder` static nested class inside its target class is the one accepted exception to the no-inner-class rule. All other inner classes are forbidden — they become proper `XStruct extends StructPackage` files.

---

## Java Byte Size Constants

Never use hardcoded `4`, `2`, or `1` for primitive byte sizes.

- `Float.BYTES` instead of `* 4`
- `Integer.BYTES` instead of `* 4`
- `Short.BYTES` instead of `* 2`

---

## Default Field Values

Never initialize fields inline with Java defaults (`= false`, `= 0f`, `= null`, `= 0`). Only meaningful non-default values belong inline — and even those go in `create()` for engine-managed objects.

Exception: `private final` fields with a fixed compile-time value may be initialized inline.

---

## Constants and Magic Numbers

Every magic number, tuning value, and named constant belongs in `EngineSetting`. No hardcoded literals in logic code.

Constants read from `EngineSetting` in `create()` are cached as instance fields — never read from settings in hot paths.

Cached `EngineSetting` fields use `camelCase` inside a class — never `SCREAMING_SNAKE_CASE`.

---

## Performance Rules

### Prefer final
`private final` on every field that does not change after construction.

### Primitive collections always
Never boxed collections. FastUtil everywhere.
- `IntArrayList` not `ArrayList<Integer>`
- `Object2IntOpenHashMap` not `HashMap<String, Integer>`

### Arrays over collections for fixed sizes
When size is known at creation time, plain array beats any collection. Prefer `float[]` over `FloatArrayList` when size is fixed.

### Cache variables early
Any getter chain called more than once in a method must be cached at the top.

```java
VAOData vaoData = vaoInstance.getVAOData();
int stride = vaoData.getVertStride();
```

### No allocation in hot paths
All scratch objects pre-allocated in `create()` and reused. High-frequency short-lived objects use the cursor array pattern — never `new`, never `Pool<T>`.

### Pre-Allocated Cursor Array Pattern

```java
// create()
this.renderCallBuffer = new RenderCallStruct[EngineSetting.MAX_RENDER_CALLS_PER_FRAME];
for (int i = 0; i < renderCallBuffer.length; i++)
    renderCallBuffer[i] = new RenderCallStruct();

// start of draw()
renderCallCursor = 0;

// hot path
RenderCallStruct call = renderCallBuffer[renderCallCursor++];
call.init(modelInstance, mask);
```

`Pool<T>` from legacy backend is never used. Cursor array always preferred.

### Zero-Allocation Hot Path Iteration
FastUtil for-each loops allocate an iterator. In anything called per-frame, use index-based iteration:

```java
Object[] elements = list.elements();
int size = list.size();
for (int i = 0; i < size; i++) {
    SomeType item = (SomeType) elements[i];
}
```

For maps needing value iteration in hot paths, maintain a parallel `ObjectArrayList` populated at registration time.

### Early exit — cheapest checks first
Cheapest and most likely to fail conditions come first. Return or `continue` as early as possible.

### No string concatenation in hot paths
Use a pre-allocated `StringBuilder` or avoid string building entirely.

### throwException is not for hot paths
`throwException` is for validation and bootstrap failures only. Normal flow uses `return`, `continue`, and boolean checks.

---

## GLSLUtility Pattern

Stateless OpenGL helper classes extend `UtilityPackage`, are package-private, one per package. Never hold state. All methods `static`. One per package that needs them — do not share across packages.

```java
class GLSLUtility extends UtilityPackage {
    static void enableDepth() { ... }
    static void drawElements(int indexCount) { ... }
}
```

---

## Class Comment Rule

Every class gets a block comment directly below the class declaration, before the first field group.

```java
public class MeshInstance extends InstancePackage {

    /*
     * Runtime mesh created on demand. Owns its VAO, VBO, and IBO instances
     * and is responsible for releasing them via MeshManager.removeMesh().
     */

    // Internal
    private VAOInstance vaoInstance;
```

- Block comment `/* */` only — never `//`
- Blank line between class declaration opening `{` and the comment
- Blank line after the comment before the first field group
- One to three sentences. No filler.

---

## Formatting & Style Guide

### Section Headers
```java
    // Accessible \\

    public String getName() {
```

### Field Groups
```java
    // Internal
    private File root;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;
```

### Inline Label Groups
First label in a block — no blank line above it. Every subsequent label — blank line above it.

```java
    public void constructor(...) {

        // Identity
        this.name = name;

        // Rules
        this.jumpDuration = jumpDuration;
    }
```

### Method Parameters — One Per Line
When a method has more than a few parameters, each goes on its own line.

```java
    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz) {
```

### No Brackets on Single-Statement Branches

```java
    if (vboHandle == null || iboHandle == null)
        return null;

    for (JsonElement val : vertex)
        vertices.add(val.getAsFloat());
```

### Method Body Spacing

Opening brace blank line required when the method contains a `// label`, an `if` block, or mixed content.

```java
    // Same operation — no opening blank
    void addTemplate(String name, int id, EntityHandle handle) {
        name2ID.put(name, id);
        id2Handle.put(id, handle);
    }

    // Has // label — blank after open
    @Override
    protected void get() {

        // Internal
        this.entityManager = get(EntityManager.class);
    }
```

Complex branchy methods — each case is its own visual unit. Blank line before every branch and between every meaningful block inside a branch.

Blank line before `return` when preceded by any group of statements.

Short single-purpose 1-2 liners get no blank lines.

### else / else-if / catch / finally
Always on their own line after the closing `}`. Always a blank line between independent `if` blocks.

```java
        if (placeX < 0) {
            placeChunkX--;
            placeX += chunkSize;
        }
        else if (placeX >= chunkSize) {
            placeChunkX++;
            placeX -= chunkSize;
        }
```

### Method Ordering Rules
- Engine lifecycle (`create`, `get`, `awake`, `update`) always first under `// Internal \\`
- Call chain order — method A calls B calls C → A then B then C top to bottom
- Getters and setters never separated — getter then setter for the same field immediately after
- All getters/setters follow field declaration order
- Utility methods appear after the method that calls them

### Getter / Setter / Has Pattern

```java
    public ItemDefinitionHandle getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemDefinitionHandle mainHand) {
        this.mainHand = mainHand;
    }

    public boolean hasMainHand() {
        return mainHand != null;
    }
```

### Constructor Opening Brace Rule
Any constructor or method body that starts with a `// label` gets a blank line after the opening brace.

```java
    public BehaviorData(
            String behaviorName,
            float jumpDuration) {

        // Identity
        this.behaviorName = behaviorName;

        // Rules
        this.jumpDuration = jumpDuration;
    }
```

---

## InputHandle Pattern

Every entity owns an `InputHandle`. Written each frame by `PlayerManager` (from `InputSystem`) for the player, or by AI systems for NPCs. `MovementManager` reads from it and never knows the source.

`InputSystem.writeToHandle(InputHandle handle, Vector3 facingDirection)` is the translation point for player input. AI writes booleans directly onto the entity's `InputHandle`.

Switching the active player entity means pointing `PlayerManager` at a different `EntityInstance` — nothing else changes.
