# Engine Rules

The class roles, layout, lifecycle, settings, registry and conventions every Java change follows. Read it before
writing or changing any Java code. `.claude/CLAUDE.md` still applies in full.

---

- **Class roles are encoded in the name.** Use the correct base class and suffix:
  `*Pipeline` (PipelinePackage), `*Manager` (ManagerPackage), `*Branch` (BranchPackage), `*System`
  (SystemPackage), `*Context` (ContextPackage), `*Loader` (LoaderPackage), `*Builder` (BuilderPackage),
  `*Handle` (HandlePackage), `*Instance` (InstancePackage), `*Data` (DataPackage), `*Struct` (StructPackage),
  `*Utility` (EngineUtility, all static), `*Assembly` (AssemblyPackage), `*Setting` (plain constants class).
- **Naming follows standard Java conventions** for code and assets alike: folders and packages all lowercase,
  files PascalCase (`SeasonManager.java`, `Spring.arpg`, `FBOs.arpg`; acronyms stay upper case; a texture's
  variant follows one underscore, `Skin_A.png`). A file's name is its resource name, so references in data must
  match its case exactly. Rename a file's case with `git mv` or through a temporary name, never a direct
  case-only rename on Windows, which git does not record.
- **Folder layout mirrors the pipeline.** `xpipeline/XPipeline.java`, `xpipeline/xmanager/` (manager, loader,
  builder, branches, systems), `xpipeline/x/` (handle, data, instance, structs).
- **Lifecycle is enforced.** Register systems in `create()`, resolve dependencies in `get()`. Never construct
  systems, handles, or instances with `new`; use `create(Class)`. Data and Struct objects may use constructors.
- **Handles wrap Data.** A Handle takes its Data through `constructor(...)` and delegates accessors to it.
- **All constants live in a settings file.** No magic numbers, hard-coded strings, or hard-coded paths in logic.
  - `EngineSetting` holds every constant the engine itself needs or shares across contexts.
  - A context may own its own `<Name>Setting` class, placed beside the context (e.g. `RuntimeSetting` next to
    `RuntimeContext`), for constants only that context uses and the engine does not need to define.
  - Editor-only constants go in `EditorSetting`.
  - Setting classes are plain `public static final` holders with a short class comment and `//` group labels.
  - Put each constant in exactly one settings file. If more than one context needs it, it belongs in
    `EngineSetting`.
- **Data-driven content** lives in `.arpg` files and goes through the Loader/Builder pattern, with every read,
  write and validation routed through `ArpgUtility` (see `.claude/docs/ARPG.md`). There is no Gson and no JSON
  library; never add one.
- **Registry IDs are assigned, never hashed.** A manager owns a `name2ID` map (`RegistryUtility.createNameIndex()`)
  and an `ID2Handle` `ObjectArrayList` palette (`RegistryUtility.createPalette()`, slot 0 reserved as the sentinel).
  IDs come only from `RegistryUtility.registerID` / `registerHandle`, in registration order, at load time (bootstrap
  batch or on-demand request; never at scan, never eagerly). Builders get their ID from the manager's
  `registerXName(name)`; lookups go through `RegistryUtility.getHandle`. Resolve a name once, cache the int ID, and
  index by ID afterwards. Names are the only uniqueness requirement; a duplicate name is an error. IDs differ between
  runs, so never persist an ID (saves store names) and never seed anything with one: world generation salts its
  noise with `RegistryUtility.toNameSeed(name)`. Items keep their index in the upper 16 bits (`ItemRegistryUtility`).
- **No raw `Thread` usage.** Async work goes through the thread pipeline.
- **Errors use `throwException(...)`** with a clear, specific message. Logging uses the `UtilityPackage` helpers.
- **Collections** use fastutil (`Object2ObjectOpenHashMap`, `ObjectArrayList`, `Int2ObjectOpenHashMap`, etc.),
  matching surrounding code. Name maps `key2Value` (e.g. `calendarName2CalendarHandle`).
- **Visibility is deliberate.** Keep package-private whatever only the pipeline needs (e.g. loaders, builders,
  `addXHandle`). Public only for what other systems consume.
