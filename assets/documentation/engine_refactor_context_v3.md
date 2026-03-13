# Engine Refactor — Context Document v3

## What We Are Doing
We are performing a large-scale refactor across all systems in a Java game engine built on LibGDX. The goal is to standardize every system to follow the same structural pattern, naming rules, formatting style, and engine hierarchy. Each system that is refactored must be clean, consistent, and conform to all rules below before moving on.

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

- **StructPackage** — Independent data containers. No lifecycle, no engine timing. Safe to use anywhere.
- **DataPackage** — Extends StructPackage. Serves as the raw data payload held within a Handle or Instance. Simple and durable, no behavior.
- **InstancePackage** — Engine-managed objects. Lightweight, lifecycle-aware. May only be instantiated via the engine `create` method.
- **HandlePackage** — Extends InstancePackage. Persistent, shareable references. Long-lived. Used for GPU resources, engine-managed handles, etc.
- **ManagerPackage** — Owns registration, lifecycle, and access for a given system. Has create/get phases.
- **BuilderPackage** — Parses external data (e.g. JSON), drives creation of Handles and registers them with the Manager. Self-destructs after bootstrap to save memory.
- **LoaderPackage** — Extends ManagerPackage. Scans a directory, populates a file queue, and batch-processes files each frame. Self-destructs when the queue is exhausted. Owns and auto-releases its child builders.

---

## Strict Naming Rule

> **The last word of every class name must match the package it extends. No exceptions.**

| Class Name     | Must Extend       |
|----------------|-------------------|
| `XData`        | `DataPackage`     |
| `XHandle`      | `HandlePackage`   |
| `XInstance`    | `InstancePackage` |
| `XManager`     | `ManagerPackage`  |
| `XBuilder`     | `BuilderPackage`  |
| `XLoader`      | `LoaderPackage`   |
| `XStruct`      | `StructPackage`   |

If the last word does not match the extended package, the name is wrong and must be corrected.

---

## Standard System Structure

Every system must have the following shape:

```
XData           extends DataPackage       ← holds raw fields (e.g. GPU handles, counts)
XHandle         extends HandlePackage     ← holds an XData, shared/cached reference
XInstance       extends InstancePackage   ← holds an XData, unique runtime copy (if needed)
XManager        extends ManagerPackage    ← registration, upload, removal, retrieval
InternalBuilder extends BuilderPackage    ← JSON parsing, drives creation (bootstrap only)
InternalLoader  extends LoaderPackage     ← scans directory, drives builder, self-destructs
```

Not every system needs all classes. If a system has no runtime duplicate need it may omit `XInstance`. If it has no JSON-driven creation it may omit `InternalBuilder`. Whatever classes exist must follow the pattern.

---

## When to Use DataPackage

> **If a Manager owns and stores the Handle — it gets a DataPackage.** The Data is the template. The Handle is the managed wrapper around it.
>
> **If no Manager owns it — skip the DataPackage.** The Handle or Instance is self-contained runtime state and fields live directly on it.

Examples:
- `EntityData` — yes, `EntityManager` owns the template handles
- `BehaviorData` — yes, `BehaviorManager` owns them
- `StatisticsHandle` — no manager, lives on `EntityInstance` — fields go directly on the handle
- `EntityStateHandle` — no manager, pure runtime state — no Data layer
- `InventoryHandle` — no manager, pure runtime references — no Data layer

---

- Both `XHandle` and `XInstance` hold an `XData` internally.
- Each exposes a typed getter: `getXData()`.
- Each exposes a `constructor(XData data)` method for engine-side initialization.
- Do NOT add a generic `DataPackage` field to the base `HandlePackage` or `InstancePackage`. Keep everything specialized and type-safe. No casts.
- Handles and Instances may also hold other Handles — these are per-object runtime references, not shared templates.

---

## Manager Rules

- Owns the name-to-handle registry (typically `Object2ObjectOpenHashMap`).
- `create()` initializes all maps and calls `create(InternalLoader.class)`.
- Exposes:
  - `hasX(String name)` — pure registry lookup, no load trigger
  - `getXHandleDirect(String name)` — direct lookup, no load trigger, safe inside builders
  - `getXHandle(String name)` — auto-triggers a load on miss, **never call from inside a builder**
  - `addX(XHandle handle)` — package-private registration called by the loader
  - `removeX(...)` — cleanup

---

## Builder Rules

- Builders are bootstrap-only. Created during the load phase and self-destruct afterward.
- Created via `create(InternalBuilder.class)` inside the loader's `create()`.
- Parses JSON, constructs `XData` with `new`, calls `create(XHandle.class)`, calls `handle.constructor(data)`.
- Must never call `getXHandle` with load trigger — use direct lookup only.
- Must check `hasX(name)` before doing any work to avoid duplicate creation.

---

## Loader Rules

- `create()` — initializes `root`, file registry map, and builder. All together.
- `get()` — resolves manager reference only.
- `scan()` — walks directory, populates file registry and `fileQueue`. No initialization here.
- `load(File)` — resolves name, calls builder, registers result with manager.
- `request(String name)` — resolves name to File, calls `request(File)` for on-demand loading.
- `directory()` is only used for shallow single-level directories. Use `Files.walk()` in `scan()` for recursive subdirectory structures.

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

- All engine-managed objects (`InstancePackage` and subclasses) must be instantiated via `create(Class)` — never via `new`.
- `StructPackage` and `DataPackage` objects may use `new` freely — they are outside the engine lifecycle.
- `get()` is the only phase where cross-system references may be resolved.
- `throwException(String)` is used for all error handling — no raw Java exceptions.

---

## Formatting & Style Guide

This style must be followed exactly. Every class produced must match this formatting.

### Section Headers
Use `// Name \\` style headers. Always a blank line above and after them:
```java
    // Accessible \\

    public String getName() {
```

### Inline Label Groups
Inside methods and constructors, `// label` sits directly above its group of statements.
- First label in a block has no blank line above it.
- Every subsequent label has a blank line above it.

```java
    public void constructor(...) {
        // Identity
        this.name = name;
        this.id = id;

        // Rules
        this.jumpDuration = jumpDuration;
    }
```

### Field Groups
Fields are grouped under a `// Label` comment. A blank line separates groups:
```java
    // Internal
    private File root;
    private BehaviorManager behaviorManager;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;
```

### Method Body Spacing
Blank lines are about **logical separation**, not raw line count. The rules:

**1. Opening brace blank line** — any method containing a `// label`, an `if` block, mixed logical content, or a `return` after statements gets a blank line after the opening brace. Methods where every line is the same type of operation (e.g. two map puts, two assignments) do not:

```java
    // Same operation — no opening blank needed
    void addEntityTemplate(String templateName, int templateID, EntityHandle entityHandle) {
        name2TemplateID.put(templateName, templateID);
        id2EntityHandle.put(templateID, entityHandle);
    }

    // Has a // label — blank after opening brace even though only one real line of code
    @Override
    protected void get() {

        // Internal
        this.entityManager = get(EntityManager.class);
    }

    // Mixed content — blank after opening brace
    public int getTemplateID(String templateName) {

        if (!name2TemplateID.containsKey(templateName))
            ((InternalLoader) internalLoader).request(templateName);

        return name2TemplateID.getInt(templateName);
    }

    // If block present — blank after open, blank around if, blank before final call
    void request(String templateName) {

        File file = templateName2File.get(templateName);

        if (file == null)
            throwException("...");

        request(file);
    }
```

**2. If blocks** — always a blank line before and after an `if` block when other statements are present around it.

**3. Label groups** — blank line before each `// label` after the first. The label itself is the separator between groups:

```java
    @Override
    protected void create() {

        // Movement
        this.walkSpeed = 1.4f;
        this.movementSpeed = 3.3f;
        this.sprintSpeed = 7f;

        // Physics
        this.jumpHeight = 0.5f;

        // Interaction
        this.reach = 1f;
    }
```

**4. Return separation** — blank line before `return` if it is preceded by any group of statements:

```java
    public Vector3 getRandomSize() {

        float x = sizeMin.x + (float) (Math.random() * (sizeMax.x - sizeMin.x));
        float y = sizeMin.y + (float) (Math.random() * (sizeMax.y - sizeMin.y));
        float z = sizeMin.z + (float) (Math.random() * (sizeMax.z - sizeMin.z));

        return new Vector3(x, y, z);
    }
```

**5. Short methods** — single-purpose 1-2 line methods get no blank lines at all:

```java
    public String getName() {
        return behaviorData.behaviorName;
    }
```

### Method Ordering Rules
- **Engine lifecycle methods** (`create`, `get`, `awake`, `update`) always come first under `// Internal \\`.
- **Call chain order** — if method A calls method B which calls method C, they appear in that order top to bottom.
- **Getters and setters are never separated** — getter immediately followed by setter for the same field.
- **All getters/setters follow field declaration order** — match the order fields are declared at the top of the class.
- **Utility methods** that are called by other methods appear after the method that calls them, not before.

### Getter / Setter / Has Pattern
Get, set, and has for the same field are always grouped together in field declaration order:
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

    public ItemDefinitionHandle getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemDefinitionHandle offHand) {
        this.offHand = offHand;
    }
```

### Try/Catch/Finally
`catch` and `finally` always on their own line separated from the closing `}` above them. Stream/lambda chains inside a `try` block stay together with no blank lines — they are one logical unit regardless of how many lines they span:

```java
        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String name = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        xName2File.put(name, file);
                        fileQueue.offer(file);
                    });
        }
        catch (IOException e) {
            throwException("...", e);
        }
        finally {
            // cleanup if needed
        }
```

### Loader Phase Order
Always in this order: `create()`, `get()`, `scan()`, then `// Load \\`, then `// On-Demand \\`:
```java
    // Base \\

    @Override
    protected void create() {
        // Internal
        this.root = new File(EngineSetting.X_JSON_PATH);

        // File Registry
        this.xName2File = new Object2ObjectOpenHashMap<>();
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        // Internal
        this.xManager = get(XManager.class);
    }

    @Override
    protected void scan() {
        if (!root.exists() || !root.isDirectory())
            throwException("...");
        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String name = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        xName2File.put(name, file);
                        fileQueue.offer(file);
                    });
        }
        catch (IOException e) {
            throwException("...", e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {
        String name = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        XHandle handle = internalBuilder.build(file, name);
        if (handle == null)
            throwException("Failed to build from: " + file.getAbsolutePath());
        xManager.addX(handle);
    }

    // On-Demand \\

    void request(String name) {
        File file = xName2File.get(name);
        if (file == null)
            throwException("...");
        request(file);
    }
```

### Manager Phase Order
Always: `create()` (maps + loader), `get()` (if needed), then `// Management \\`, then `// Accessible \\`:
```java
    // Base \\

    @Override
    protected void create() {
        // Palette
        this.name2X = new Object2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addX(XHandle handle) {
        name2X.put(handle.getXName(), handle);
    }

    // Accessible \\

    public XHandle getX(String name) {
        // ...
    }
```

---

## Refactor Checklist (per system)

When refactoring a system, verify:

- [ ] `XData` exists and extends `DataPackage`, fields are `public final` where immutable
- [ ] `XHandle` exists, extends `HandlePackage`, holds `XData`, delegates all getters through it
- [ ] `XInstance` exists if needed, extends `InstancePackage`, holds `XData`
- [ ] `XManager` exists, extends `ManagerPackage`, follows manager rules
- [ ] `InternalBuilder` exists if needed, extends `BuilderPackage`, uses `new XData(...)` and `create(XHandle.class)`
- [ ] `InternalLoader` exists if needed, extends `LoaderPackage`, follows loader phase order
- [ ] All class names match their extended package
- [ ] No `new` calls for engine-managed objects
- [ ] No load-triggering lookups inside builders
- [ ] All old `XStruct` references updated to `XData`
- [ ] Formatting matches style guide exactly
- [ ] Getters and setters grouped together per field in declaration order
- [ ] Method body spacing applied — blank after opening brace, between logical groups, before return
- [ ] Call chain ordering respected top to bottom
- [ ] Engine lifecycle methods first under `// Internal \\`
- [ ] Every `// label` after the first in a block has a blank line above it
