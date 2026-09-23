# AdventureRPG — Instructions for Claude

This is a custom, unconventional Java engine built on LWJGL. It has its own lifecycle, registry, and class
hierarchy (see `README.md`). Do not apply generic Java, Spring, libGDX, or "standard game engine" patterns.
When in doubt, find the closest existing system and mirror it exactly.

---

## Non-Negotiables

- **Always finish the job.** Every fix or implementation is complete and compiles. No TODOs, stubs,
  placeholders, "rest unchanged" snippets, or partial methods. If a change touches a caller, update the caller.
- **Always professional.** Clean, deliberate, production-quality code. No hacks, no debug leftovers, no
  commented-out code.
- **Follow the existing structure and syntax.** Read neighbouring classes before writing. Match their naming,
  layout, section headers, spacing, and idioms. The existing code is the style guide.
- **Do not "fix" the architecture.** The engine is intentionally unconventional. Never replace its patterns with
  conventional ones, and never refactor, rename, or reformat code unrelated to the task.

---

## Reuse and Compartmentalization

- **Search before writing.** Before adding a method, look for one that already does the job (`util/`,
  `EngineUtility`, `JsonUtility`, `FileUtility`, `RegistryUtility`, the owning manager, sibling branches).
  Reuse it.
- **Everything is circular.** Methods are small, single-purpose, and composable so they can be called from
  anywhere they are needed. One piece of logic lives in exactly one place; every caller routes through it.
- **Single call sites.** Structural operations (open/close, add/remove, register/deregister, push/notify) have
  one method that owns them. Other paths call that method instead of re-implementing it.
- **Compartmentalize by role.** Split responsibilities the way the engine already does: a Manager owns state and
  the palette, Branches own focused execution paths, a Loader scans and requests, a Builder parses JSON into
  Data/Handles, Utilities hold stateless shared logic.

---

## Engine Rules

- **Class roles are encoded in the name.** Use the correct base class and suffix:
  `*Pipeline` (PipelinePackage), `*Manager` (ManagerPackage), `*Branch` (BranchPackage), `*System`
  (SystemPackage), `*Context` (ContextPackage), `*Loader` (LoaderPackage), `*Builder` (BuilderPackage),
  `*Handle` (HandlePackage), `*Instance` (InstancePackage), `*Data` (DataPackage), `*Struct` (StructPackage),
  `*Utility` (UtilityPackage), `*Assembly` (AssemblyPackage).
- **Folder layout mirrors the pipeline.** `xpipeline/XPipeline.java`, `xpipeline/xmanager/` (manager, loader,
  builder, branches, systems), `xpipeline/x/` (handle, data, instance, structs).
- **Lifecycle is enforced.** Register systems in `create()`, resolve dependencies in `get()`. Never construct
  systems, handles, or instances with `new`; use `create(Class)`. Data and Struct objects may use constructors.
- **Handles wrap Data.** A Handle takes its Data through `constructor(...)` and delegates accessors to it.
- **All constants live in `EngineSetting`** (editor-only constants in `EditorSetting`). No magic numbers or
  hard-coded paths in logic.
- **JSON-driven content** goes through the Loader/Builder pattern, with validation via `JsonUtility`.
- **No raw `Thread` usage.** Async work goes through the thread pipeline.
- **Errors use `throwException(...)`** with a clear, specific message. Logging uses the `UtilityPackage` helpers.
- **Collections** use fastutil (`Object2ObjectOpenHashMap`, `ObjectArrayList`, `Int2ObjectOpenHashMap`, etc.),
  matching surrounding code. Name maps `key2Value` (e.g. `calendarName2CalendarHandle`).
- **Visibility is deliberate.** Keep package-private whatever only the pipeline needs (e.g. loaders, builders,
  `addXHandle`). Public only for what other systems consume.

---

## Formatting

- 4-space indentation, 120-column limit, LF line endings (see `.editorconfig` / `.clang-format`).
- **Spaced neatly and easy to read.** A blank line after the class declaration, after a method's opening brace
  when the body has more than one statement, and between logical groups of statements. One-line getters and
  setters stay compact. Multi-parameter signatures wrap one parameter per line, as in existing code.
- **Class comment:** at most one short paragraph, inside a `/* */` block, placed directly under the class
  declaration. Nothing more. No other block comments, no Javadoc, no explanatory essays in methods.
- **Field groups** use short `//` labels, matching existing code:

  ```java
  // Internal
  private CalendarManager calendarManager;

  // Palette
  private Object2ObjectOpenHashMap<String, CalendarHandle> calendarName2CalendarHandle;
  ```

- **Section headers** use `// Name \\` and group methods, reusing the existing names wherever they fit:
  `// Base \\`, `// Internal \\`, `// Constructor \\`, `// Management \\`, `// Load \\`, `// Build \\`,
  `// On-Demand \\`, `// Update \\`, `// Render \\`, `// Draw \\`, `// Utility \\`, `// Accessible \\`.

Reference shape:

```java
public class ExampleManager extends ManagerPackage {

    /*
     * One short paragraph describing what this class owns and how it is used.
     */

    // Internal
    private OtherManager otherManager;

    // Palette
    private Object2ObjectOpenHashMap<String, ExampleHandle> exampleName2ExampleHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.exampleName2ExampleHandle = new Object2ObjectOpenHashMap<>();
        create(ExampleLoader.class);
    }

    @Override
    protected void get() {
        this.otherManager = get(OtherManager.class);
    }

    // Management \\

    void addExampleHandle(ExampleHandle exampleHandle) {
        exampleName2ExampleHandle.put(exampleHandle.getExampleName(), exampleHandle);
    }

    // Accessible \\

    public ExampleHandle getExampleHandle(String exampleName) {
        return exampleName2ExampleHandle.get(exampleName);
    }
}
```

---

## Delivering Code

Every task is delivered as a **zip payload** the user can download, unzip, and drag straight into the project.

1. **Zip contents:** only new or modified files, each as a **complete file** (never a diff or snippet).
2. **Paths are relative to the project root**, so the zip's top level holds `core/`, `lwjgl3/`, `assets/`, etc.
   Unzipping over the project root puts every file in the right place.
3. **Nothing else in the zip.** No readmes, notes, or instruction files.
4. **Name it** `AdventureRPG_<ShortTaskName>.zip`, build it in the scratchpad directory, and send it to the
   user as a downloadable file.
5. **In the reply, list the changes explicitly:**
   - **Added:** new files, with full paths.
   - **Replaced:** modified files, with full paths.
   - **DELETE:** every file or class the user must remove by hand, with full paths and a one-line reason.
     A moved or renamed class counts as a delete of the old path plus an add of the new one. If nothing
     needs deleting, say "No deletions."
6. If the session also runs on a git branch, commit and push the same changes to that branch, but the zip is
   the deliverable.
