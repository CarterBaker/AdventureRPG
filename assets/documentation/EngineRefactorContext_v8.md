# Engine Refactor — Context Document v8

## What We Are Doing
We are performing a large-scale refactor across all systems in a Java game engine built on LibGDX. The goal is to standardize every system to follow the same structural pattern, naming rules, formatting style, engine hierarchy, and performance characteristics. Each system that is refactored must be clean, consistent, and conform to all rules below before moving on. As we go, flag any structural, performance, or correctness issues spotted in the existing code.

---

## Package Hierarchy

```
EngineUtility
├── StructPackage           ← independent, no lifecycle, no engine timing rules
│   └── DataPackage         ← raw data payload held inside a Handle or Instance
└── InstancePackage         ← engine-managed, lifecycle-aware
    └── HandlePackage       ← persistent, shareable, long-lived reference
```

### Package Descriptions

- **StructPackage** — Independent data containers. No lifecycle, no engine timing. Safe to use anywhere. May be instantiated with `new`.
- **DataPackage** — Extends StructPackage. Raw data payload held within a Handle or Instance. Simple and durable, no behavior. May be instantiated with `new`.
- **InstancePackage** — Engine-managed objects. Lightweight, lifecycle-aware. Must be instantiated via `create(Class)` only.
- **HandlePackage** — Extends InstancePackage. Persistent, shareable references. Long-lived. Must be instantiated via `create(Class)` only.
- **ManagerPackage** — Owns registration, lifecycle, and access for a given system. Has create/get/awake/update phases.
- **BuilderPackage** — Parses external data (e.g. JSON), drives creation of Handles. Bootstrap-only, self-destructs after the loader finishes.
- **LoaderPackage** — Extends ManagerPackage. Scans a directory, populates a file queue, batch-processes files each frame. Self-destructs when queue is exhausted. Owns and auto-releases its child builders.
- **BranchPackage** — Internal computation unit owned by a Manager. Lives in the same package as its manager. All methods only callable by the manager are package-private.
- **SystemPackage** — Self-contained helper that performs one specific job for its manager. No branching or routing. Package-private where appropriate.

---

## Strict Naming Rule

> **The last word of every class name must match the package it extends. No exceptions.**

| Class Name      | Must Extend        |
|-----------------|--------------------|
| `XData`         | `DataPackage`      |
| `XHandle`       | `HandlePackage`    |
| `XInstance`     | `InstancePackage`  |
| `XManager`      | `ManagerPackage`   |
| `XBuilder`      | `BuilderPackage`   |
| `XLoader`       | `LoaderPackage`    |
| `XStruct`       | `StructPackage`    |
| `XBranch`       | `BranchPackage`    |
| `XSystem`       | `SystemPackage`    |

If the last word does not match the extended package, the name is wrong and must be corrected.

---

## Standard System Structure

Every system must have the following shape:

```
XData           extends DataPackage       ← holds raw fields
XHandle         extends HandlePackage     ← holds XData, shared/cached reference
XInstance       extends InstancePackage   ← holds XData, unique runtime copy (if needed)
XManager        extends ManagerPackage    ← registration, retrieval, lifecycle
InternalBuilder extends BuilderPackage    ← JSON parsing, drives creation (bootstrap only)
InternalLoader  extends LoaderPackage     ← scans directory, drives builder, self-destructs
XBranch         extends BranchPackage     ← routing/computation unit of a manager
XSystem         extends SystemPackage     ← focused single-job helper of a manager
```

Not every system needs all classes. Omit what is not needed. Whatever exists must follow the pattern.

---

## Branch vs System

- **Branch** — used when a manager needs to perform the same operation differently depending on input or context. The manager routes between branches.
- **System** — used when a manager needs a focused, self-contained helper that does one specific job. No routing.

---

## When to Use DataPackage

> **If a Manager owns and stores the Handle — it gets a DataPackage.** The Data is the template. The Handle is the managed wrapper around it.
>
> **If no Manager owns it — skip the DataPackage.** The Handle or Instance is self-contained runtime state and fields live directly on it.

---

## Handle vs Instance — Lifetime Rule

The functional difference between a Handle and an Instance is **lifetime and ownership**, not structure:

- **Handle** — the original. Registered in a manager palette. Lives from game start to shutdown. The manager owns it. External systems read from it but treat it as authoritative.
- **Instance** — a clone. Handed out at runtime by the manager. Safe to mutate. Safe to dispose of when the caller is done with it. Not registered in any palette.

Both wrap the same `DataPackage` type. The Data holds everything. The Handle and Instance are distinguished only by who owns them and how long they live.

When cloning, always deep-copy the `DataPackage` — never share a mutable Data between a Handle and an Instance:

```java
XData clonedData = new XData(handle.getXData());
XInstance instance = create(XInstance.class);
instance.constructor(clonedData);
```

The identity fields (name, ID, shared references that never change) are copied by reference. The mutable per-instance fields (uniforms, runtime state) are deep-copied.

---

## Struct vs Instance Decision Rule

Use `StructPackage` when a class has **no engine lifecycle** — no `create()`, `get()`, `awake()`, or `update()`, and never touches `internal`. If it just holds data and gets passed around, it is a Struct and uses `new`. Even if a system creates it on demand at runtime, if it has no lifecycle it is a Struct.

The test: **does it need `internal`?** No → Struct. Yes → Instance or higher.

---

## Handle and Instance Rules

- When a Handle or Instance has a DataPackage, it holds it internally and delegates all getters and setters through it.
- Exposes a typed `getXData()` getter.
- Engine-side initialization uses `constructor(XData data)`.
- Do NOT add a generic `DataPackage` field to base packages. Keep everything specialized and type-safe. No casts.
- Handles and Instances may hold other Handles as runtime references.
- All fields are `private`. No public fields ever.
- Immutable fields use `private final` with getters only — no setters for things that never change after construction.

---

## Branch and System Rules

- Live in the **same package** as their Manager. Never a sub-package.
- All methods only callable by the manager are **package-private**. No exceptions.
- Only methods genuinely needed externally get `public`.
- Created via `create(XBranch.class)` or `create(XSystem.class)` inside the manager's `create()`.

---

## System Package Placement

`SystemPackage` and `BranchPackage` classes do not get their own package. They live in the **same package as their manager**. A system or branch in its own package implies independence it does not have and violates the structural rule. Move it alongside its manager.

---

## Manager Rules

- `create()` initializes all maps, creates branches/systems, calls `create(InternalLoader.class)` **after** maps are initialized.
- `get()` resolves all cross-system references. Only place this is allowed.
- Every manager that owns handles must expose the full uniform registry API — see **Manager Registry Pattern** below.
- `addX(XHandle handle)` — package-private, called by loader only.
- `removeX(...)` — cleanup.

---

## Manager Registry Pattern

Every manager that owns handles follows this exact uniform pattern. No exceptions, no abbreviations.

### Palette Fields
Two maps — name-to-ID and ID-to-handle. Never name-to-handle directly:
```java
    // Palette
    private Object2IntOpenHashMap<String> xName2XID;
    private Int2ObjectOpenHashMap<XHandle> xID2XHandle;
```

Use `Short2ObjectOpenHashMap` and `short` IDs for systems where the ID range fits (VAO, VBO, IBO, Calendar, Behavior). Use `Int2ObjectOpenHashMap` and `int` IDs for larger namespaces (Mesh, Entity, Item).

### ID Generation
IDs are always derived from the resource name via `RegistryUtility` — never sequential counters, never manually assigned:
```java
    void addXHandle(String name, XHandle handle) {
        int id = RegistryUtility.toIntID(name);
        xName2XID.put(name, id);
        xID2XHandle.put(id, handle);
    }
```

### Required Public Methods — all four, always:
```java
    public boolean hasX(String xName) {
        return xName2XID.containsKey(xName);
    }

    public int getXIDFromXName(String xName) {
        if (!xName2XID.containsKey(xName))
            // trigger load or throwException
        return xName2XID.getInt(xName);
    }

    public XHandle getXHandleFromXID(int xID) {
        XHandle handle = xID2XHandle.get(xID);
        if (handle == null)
            throwException("X ID not found: " + xID);
        return handle;
    }

    public XHandle getXHandleFromXName(String xName) {
        return getXHandleFromXID(getXIDFromXName(xName));
    }
```

Method names must be fully explicit — `getXIDFromXName`, `getXHandleFromXID`, `getXHandleFromXName`. Never abbreviated to `getXID`, `getXHandle`, `getX`.

### Naming Convention
Map field names follow the pattern `xName2XID` and `xID2XHandle` — explicit about what goes in and what comes out. Never `palette`, `registry`, `map`, or other generic names.

---

## Builder Rules

- Bootstrap-only. Created in the loader's `create()`, auto-released when loader finishes.
- Parses JSON, constructs `XData` with `new`, calls `create(XHandle.class)`, calls `handle.constructor(data)`.
- Never call a load-triggering lookup — use direct lookup only.
- Check `hasX(name)` before doing work to avoid duplicate creation.

---

## Loader Rules

- `create()` — initializes `root`, file registry map, and builder. All together.
- `get()` — resolves manager reference only.
- `scan()` — walks directory, populates file registry and `fileQueue`. No initialization here.
- `load(File)` — resolves name, calls builder, registers result with manager.
- `request(String name)` — resolves name to File, calls `request(File)` for on-demand loading.
- Use `Files.walk()` in `scan()` for recursive subdirectory structures.

---

## Data Creation Flow

```
JSON file
  └── InternalLoader.load(File)
        └── InternalBuilder.build(File, name)
              └── new XData(...)               ← DataPackage, safe to use new
                    └── create(XHandle.class)  ← engine-managed, must use create
                          └── handle.constructor(XData)
                                └── Manager.addX(handle)
```

---

## General Engine Rules

- All engine-managed objects (`InstancePackage` and subclasses) must be instantiated via `create(Class)` — never `new`.
- `StructPackage` and `DataPackage` may use `new` freely — outside the engine lifecycle.
- `get()` is the only phase where cross-system references may be resolved.
- `throwException(String)` for all error handling — **never** `throw new RuntimeException`, `throw new IllegalArgumentException`, or any raw Java exception. The internal exception system always.
- Entities are always handed out as `EntityInstance`, never `EntityHandle`.

---

## Settings vs EngineSetting

Two separate classes — never mix them up:

- **`EngineSetting`** — compile-time constants. Things that never change at runtime. `public static final` everything. Magic numbers, tuning values, paths, limits, named bit constants. If it has a fixed value baked into the codebase, it belongs here.
- **`Settings`** — user-configurable runtime values. Things the player or developer may want to change at startup via `Settings.Builder`. FOV, window size, render distance, mouse sensitivity, etc. Accessed via `internal.settings.whatever`.

If a value could reasonably be changed by a user, it goes in `Settings`. If it is an engine constant that should never vary, it goes in `EngineSetting`. When in doubt — if it has a unit the user would recognize (pixels, degrees, sensitivity) it is a `Settings` value.

---

## Builder Pattern Exception

The `Builder` static nested class inside its target class is the **one accepted exception** to the no-inner-class rule. This is a deliberate pattern for constructing `Settings` and similar configuration objects. It is acceptable because the Builder is inseparable from its target by design.

All other inner classes remain forbidden — no data holders, no result types, no anonymous helpers. Those become proper `XStruct extends StructPackage` files.

---

## Java Byte Size Constants

Never use hardcoded `4`, `2`, or `1` for byte sizes of primitives. Use the Java built-in constants:

- `Float.BYTES` instead of `* 4`
- `Integer.BYTES` instead of `* 4` or `allocateDirect(4)`
- `Short.BYTES` instead of `* 2`

These are self-documenting and correct on all platforms.

---

## Default Field Values

Never initialize fields inline with their default values. Java's defaults are `0`, `false`, `null`, `0f`, `0L`. Writing `= false` or `= 0f` on a declaration is noise. Only meaningful non-default values belong inline — and even those should go in `create()` for engine-managed objects:

```java
    // Wrong — noise
    private boolean locked = false;
    private float sensitivity = 0.15f;

    // Right — defaults dropped, meaningful value in create()
    private boolean locked;
    private float sensitivity;

    @Override
    protected void create() {
        this.locked = false;
        this.sensitivity = internal.settings.mouseSensitivity;
    }
```

Exception: `private final` fields with a fixed compile-time value may be initialized inline if they are truly constant and not driven by settings.

---

## Constants and Magic Numbers

> **Every magic number, tuning value, and named constant belongs in `EngineSetting`. No exceptions.**

- No hardcoded literals in logic code. If a number has a meaning beyond trivial arithmetic, it belongs in `EngineSetting` with a descriptive name.
- Constants read from `EngineSetting` in `create()` are cached as instance fields — never read from settings on every frame or in hot paths.
- The only acceptable inline literals are trivial math (`* 2`, `/ 4`, `index + 1`) where the meaning is fully self-evident from context.
- If you see `0.5f`, `86400000`, `65535`, `0xFFFF`, `24`, or any other naked number in logic code, it needs a named constant.

### Settings Field Naming in Classes

When caching `EngineSetting` constants into instance fields, the field name is `camelCase` — never the `SCREAMING_SNAKE_CASE` of the constant:

```java
    // Wrong
    private int CHUNK_SIZE;

    // Right
    private int chunkSize;
```

The constant name is for `EngineSetting` only. Inside a class it is a regular private field.

---

## Performance Rules

### Prefer final
`private final` on every field that does not change after construction. Final fields communicate intent, enable JIT optimizations, and prevent accidental mutation.

### Primitive collections always
Never use boxed collections. FastUtil primitive collections everywhere:
- `IntArrayList` not `ArrayList<Integer>`
- `Object2IntOpenHashMap` not `HashMap<String, Integer>`
- `FloatArrayList` not `ArrayList<Float>`
- No boxing, no unboxing, no autoboxing anywhere

### Arrays over collections for fixed sizes
When the size is known or bounded at creation time, a plain array beats any collection for cache locality and GC pressure. Prefer `float[]` over `FloatArrayList` when the size is fixed.

### Cache variables early — no repeated getter chains
Any getter chain called more than once in a method must be cached into a local variable at the top of the method. `a.getB().getC()` called twice is one unnecessary pointer dereference. Cache it:
```java
    VAOData vaoData = vaoInstance.getVAOData();
    int stride = vaoData.getVertStride();
    int[] attrSizes = vaoData.getAttrSizes();
```

### No allocation in hot paths
`update()`, render loops, geometry builders, anything called per-frame or per-block must not allocate. Rules:
- All scratch objects pre-allocated in `create()` and reused
- High-frequency short-lived objects use the **cursor array pattern** — never `new`, never `Pool<T>`
- Never `new Vector3()`, `new FloatArrayList()`, `new int[]` inside a method called per-frame

### Pre-Allocated Cursor Array Pattern
For objects created and discarded at high frequency within a single frame (e.g. render calls), pre-allocate a fixed array in `create()` and hand items out via a cursor. Reset the cursor at the start of each frame. No Pool, no free(), no GC:

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

The capacity constant belongs in `EngineSetting`. `Pool<T>` from LibGDX is never used — the cursor array is always preferred.

### Zero-Allocation Hot Path Iteration
FastUtil for-each loops allocate an iterator on every call. In anything called per-frame, replace with index-based iteration using `elements()`:

```java
    // Wrong — allocates iterator every call
    for (var entry : someMap.int2ObjectEntrySet()) { ... }

    // Right — zero allocation
    Object[] elements = list.elements();
    int size = list.size();
    for (int i = 0; i < size; i++) {
        SomeType item = (SomeType) elements[i];
    }
```

For maps that need value iteration in a hot path, maintain a parallel `ObjectArrayList` populated at registration time and iterate that instead. After warmup — when all materials, depths, and batches are registered — the hot path becomes pure index-based array reads with zero allocation per frame.

### Early exit — cheapest checks first
Arrange conditions so the cheapest and most likely to fail come first. Return or `continue` as early as possible to avoid unnecessary work downstream.

### No string concatenation in hot paths
String concat allocates a new `String` object on every call. In anything called per-frame or per-block, use a pre-allocated `StringBuilder` or avoid string building entirely.

### throwException is not for hot paths
`throwException` is for validation, error states, and bootstrap failures — not for conditions that occur in normal per-frame execution. Normal flow control uses `return`, `continue`, and boolean checks.

---

## GLSLUtility Pattern

Stateless OpenGL helper classes extend `UtilityPackage`, are package-private, and there is one per package that needs them. They never hold state. All methods are `static`. They exist solely to keep raw GL calls out of the system classes that use them:

```java
class GLSLUtility extends UtilityPackage {

    /*
     * Stateless OpenGL helpers for XSystem. Package-private.
     */

    static void enableDepth() { ... }
    static void drawElements(int indexCount) { ... }
}
```

If two packages need overlapping GL operations, each gets its own `GLSLUtility`. Do not share across packages.

---

## Class Comment Rule

Every class gets a block comment directly below the class declaration, before the first field group. Describes what the class is responsible for, what it owns, and any key lifetime or constraint rules. Concise — one to three sentences. Never filler.

```java
public class MeshInstance extends InstancePackage {

    /*
     * Runtime mesh created on demand. Owns its VAO, VBO, and IBO instances
     * and is responsible for releasing them via MeshManager.removeMesh().
     */

    // Internal
    private VAOInstance vaoInstance;
```

Rules:
- Block comment `/* */` only — never `//`
- Blank line between the class declaration opening `{` and the comment
- Blank line after the comment before the first field group
- Describes: what it is, what it owns or holds, any key constraints or lifetime rules
- No redundant filler

---

## Constructor and Method Opening Brace Rule

Any constructor or method body that starts with a `// label` gets a blank line after the opening brace — even if it is only one line of code. This is the same rule as all other methods. The label triggers the blank:

```java
    public BehaviorData(
            String behaviorName,
            short behaviorID,
            float jumpDuration) {

        // Identity
        this.behaviorName = behaviorName;
        this.behaviorID = behaviorID;

        // Rules
        this.jumpDuration = jumpDuration;
    }
```

The closing `)` of the parameter list and the first `// label` always have a blank line between them. No exceptions.

---

## Formatting & Style Guide

### Section Headers
`// Name \\` style. Always a blank line above and after:
```java
    // Accessible \\

    public String getName() {
```

### Field Groups
Grouped under `// Label`. Blank line between groups:
```java
    // Internal
    private File root;
    private BehaviorManager behaviorManager;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;
```

### Inline Label Groups
`// label` sits directly above its group. First label in a block — no blank line above it. Every subsequent label — blank line above it:
```java
    public void constructor(...) {

        // Identity
        this.name = name;
        this.id = id;

        // Rules
        this.jumpDuration = jumpDuration;
    }
```

### Method Parameters — One Per Line
When a method declaration or call has more than a few parameters, each parameter goes on its own line, indented consistently. Never cram multiple parameters on one line. This applies equally to declarations, calls, and switch expression arrow targets:

```java
    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            int currentSize,
            int tangentSize) {

    return tryExpand(
            chunkInstance,
            subChunkInstance,
            biomePaletteHandle,
            xyz,
            direction3Vector,
            currentSize,
            tangentSize);

    case FULL -> fullGeometryBranch.assembleQuads(
            chunkInstance,
            subChunkInstance,
            biomePaletteHandle,
            xyz,
            direction3Vector);
```

Short calls with 2-3 simple parameters may stay on one line when they are clearly readable.

### No Brackets on Single-Statement Branches
Never use `{ }` when a branch body is a single statement. This applies to `if`, `else`, `else if`, `for`, `while`, and `do while`:

```java
    if (vboHandle == null || iboHandle == null)
        return null;

    for (JsonElement val : vertex)
        vertices.add(val.getAsFloat());
```

The only exception is when the body itself requires braces — in that case the outer branch must also use braces.

### Method Body Spacing
Blank lines express **logical separation**, not line count.

**Opening brace blank line** — required when the method contains a `// label`, an `if` block, mixed content, or a `return` after statements. Not required when every line is the same type of operation:

```java
    // Same operation — no opening blank
    void addTemplate(String name, int id, EntityHandle handle) {
        name2ID.put(name, id);
        id2Handle.put(id, handle);
    }

    // Has // label — blank after open even if only one real line
    @Override
    protected void get() {

        // Internal
        this.entityManager = get(EntityManager.class);
    }

    // if + other statements
    void request(String name) {

        File file = name2File.get(name);

        if (file == null)
            throwException("...");

        request(file);
    }
```

**Complex branchy methods** — each case is its own visual unit. Blank line before every branch and between every meaningful block of statements inside a branch, even if it is only one line:

```java
    if (element.isJsonArray()) {

        JsonArray vertex = element.getAsJsonArray();

        if (vertex.size() != vertStride)
            throwException("...");

        for (JsonElement val : vertex)
            vertices.add(val.getAsFloat());

        currentVertex++;
    }

    else if (element.isJsonObject()) {
        expandQuad(element.getAsJsonObject(), vertices, quadIndices,
                currentVertex, vertStride, vaoInstance, file);
        currentVertex += 4;
    }

    else
        throwException("VBO element must be a vertex array or quad object in file: " + file.getName());
```

**If blocks** — blank line before and after when other statements surround them.

**Return** — blank line before `return` when preceded by any group of statements.

**Short methods** — single-purpose 1-2 liners get no blank lines:
```java
    public String getName() {
        return data.name;
    }
```

### Bracket Body Spacing
Any `{ }` block with real logic mirrors method body rules — blank after open brace if content warrants it, blank lines around nested `if` blocks:

```java
        if (facing == Direction3Vector.UP || facing == Direction3Vector.DOWN) {

            float ax = Math.abs(cameraDirection.x);
            float az = Math.abs(cameraDirection.z);

            if (ax >= az)
                spin = cameraDirection.x > 0 ? 1 : 3;
            else
                spin = cameraDirection.z > 0 ? 0 : 2;
        }
```

### else / else-if / catch / finally Separation
Always on their own line after the closing `}`. Always a blank line between the closing `}` and the `catch` or `finally`. Never on the same line. Independent `if` blocks always have a blank line between them:

```java
        if (placeX < 0) {
            placeChunkX--;
            placeX += CHUNK_SIZE;
        }
        else if (placeX >= CHUNK_SIZE) {
            placeChunkX++;
            placeX -= CHUNK_SIZE;
        }

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(file -> {
                        xName2File.put(name, file);
                        fileQueue.offer(file);
                    });
        }
        catch (IOException e) {
            throwException("...", e);
        }
```

Stream/lambda chains inside `try` stay together — one logical unit, no blank lines inside.

### Method Ordering Rules
- Engine lifecycle (`create`, `get`, `awake`, `update`) always first under `// Internal \\`
- Call chain order — method A calls B calls C → A then B then C top to bottom
- Getters and setters never separated — getter then setter for the same field immediately after
- All getters/setters follow field declaration order
- Utility methods appear after the method that calls them

### Getter / Setter / Has Pattern
Grouped per field in declaration order:
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

### Loader Phase Order
`create()` → `get()` → `scan()` → `// Load \\` → `// On-Demand \\`

### Manager Phase Order
`create()` → `get()` → `// Management \\` → `// Accessible \\`

---

## Refactor Checklist (per system)

- [ ] `XData` exists, extends `DataPackage`, fields `private final` with getters only where immutable
- [ ] `XHandle` exists, extends `HandlePackage`, holds `XData`, delegates all getters/setters
- [ ] `XInstance` exists if needed, extends `InstancePackage`, holds cloned `XData`
- [ ] Handle wraps original Data — never shared with an Instance
- [ ] Instance wraps deep-copied Data — safe to mutate, safe to dispose
- [ ] `XManager` exists, extends `ManagerPackage`, follows manager rules
- [ ] `InternalBuilder` exists if needed, uses `new XData(...)` and `create(XHandle.class)`
- [ ] `InternalLoader` exists if needed, follows loader phase order
- [ ] `XBranch` / `XSystem` classes live in same package as manager, never a sub-package
- [ ] All branch and system methods only callable by manager are package-private
- [ ] All class names match their extended package
- [ ] No `new` for engine-managed objects
- [ ] No load-triggering lookups inside builders
- [ ] No inner classes anywhere except `Builder` nested in its target — every other class has its own file
- [ ] No public fields — `private` always, getters/setters for access
- [ ] `private final` on all immutable fields
- [ ] No raw Java exceptions — `throwException` only
- [ ] No magic numbers in logic code — all constants in `EngineSetting`
- [ ] Runtime user settings in `Settings`, engine constants in `EngineSetting` — never mixed
- [ ] Cached `EngineSetting` fields use `camelCase` — never `SCREAMING_SNAKE_CASE`
- [ ] No boxed collections — FastUtil primitives everywhere
- [ ] No repeated getter chains — cache locals at top of method
- [ ] No allocation in hot paths — scratch objects pre-allocated in `create()`
- [ ] High-frequency per-frame objects use cursor array pattern — never `Pool<T>` or `new`
- [ ] Hot path iteration uses index-based `elements()` — no FastUtil for-each iterators
- [ ] Parallel `ObjectArrayList` maintained for map values that need hot-path iteration
- [ ] Arrays preferred over collections for fixed sizes
- [ ] Early exit — cheapest checks first
- [ ] Entities handed out as `EntityInstance`, never `EntityHandle`
- [ ] Every class has a block comment describing its responsibility
- [ ] Long method declarations and call sites use one parameter per line
- [ ] No braces on single-statement branches
- [ ] Formatting matches style guide exactly
- [ ] Getters/setters grouped per field in declaration order
- [ ] Method body spacing correct throughout
- [ ] Complex branchy methods treat each case as a visual unit
- [ ] else/else-if/catch/finally always on own line
- [ ] Independent if blocks separated by blank line
- [ ] Call chain ordering top to bottom
- [ ] Engine lifecycle methods first under `// Internal \\`
- [ ] Every `// label` after the first in a block has a blank line above it
- [ ] Manager palette uses name-to-ID and ID-to-handle maps, never name-to-handle directly
- [ ] All manager IDs derived from `RegistryUtility.toShortID` or `toIntID` — no sequential counters
- [ ] All four registry methods present — `hasX`, `getXIDFromXName`, `getXHandleFromXID`, `getXHandleFromXName`
- [ ] Method names fully explicit — never abbreviated
- [ ] `Float.BYTES`, `Integer.BYTES`, `Short.BYTES` used — never hardcoded `4` or `2`
- [ ] No inline default value assignments — `= false`, `= 0f`, `= null` removed from declarations
- [ ] GLSLUtility extends `UtilityPackage`, is package-private, one per package
- [ ] No `Pool<T>` anywhere — cursor array pattern used instead
