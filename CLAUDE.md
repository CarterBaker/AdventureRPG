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
  `EngineUtility`, `ArpgUtility`, `FileUtility`, `RegistryUtility`, the owning manager, sibling branches).
  Reuse it.
- **Everything is circular.** Methods are small, single-purpose, and composable so they can be called from
  anywhere they are needed. One piece of logic lives in exactly one place; every caller routes through it.
- **Single call sites.** Structural operations (open/close, add/remove, register/deregister, push/notify) have
  one method that owns them. Other paths call that method instead of re-implementing it.
- **Compartmentalize by role.** Split responsibilities the way the engine already does: a Manager owns state and
  the palette, Branches own focused execution paths, a Loader scans and requests, a Builder parses an ARPG
  object tree into Data/Handles, Utilities hold stateless shared logic.

---

## Engine Rules

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
  write and validation routed through `ArpgUtility` (see **ARPG Data Files**). There is no Gson and no JSON
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

## ARPG Data Files

Every data file the engine reads or writes is an **`.arpg` file**: all content under `assets/` (blocks, biomes,
meshes, menus, materials, editor schemas, ...), world and sprite companions beside their PNGs, and the user files
under `Documents/My Games/AdventureRPG/` (`Settings.arpg`, `EditorSettings.arpg`, `Saves/Characters/*.arpg`,
`bin/editorLayout/*.arpg`). The engine ignores `.json` files entirely.

### The file on disk

```
offset  size  field
0       4     magic "ARPG" (ASCII)
4       1     format version (EngineSetting.ARPG_FORMAT_VERSION, currently 2)
5       4     CRC32C checksum of the payload, little-endian
9       n     payload (the binary document below), stored unencrypted for speed
```

- The integrity kernel is `ArpgIntegrityUtility`: a hardware-accelerated CRC32C check with no provider to start,
  after which the payload is decoded in place without copying. Speed is the priority; the files are not
  encrypted, only checked.
- Writing is deterministic: identical content always produces identical bytes, so re-saving an unchanged file
  never shows up as a git change.
- Any flipped bit, truncation, or foreign file fails the checksum and is rejected with a clear error. **Never
  hand-write, hex-edit, patch, or concatenate `.arpg` bytes.** The only ways to produce one are the engine
  (`ArpgUtility.writeObject`) and the ARPG tool.
- `.gitattributes` marks `*.arpg binary`. Keep it: text normalisation would corrupt the files.
- Bumping `ARPG_FORMAT_VERSION` makes every existing file unreadable. Before a format change, `export` everything
  with the old build, then `convert` with the new one.
- Payload encoding (`ArpgBinaryUtility`): a varint count of distinct strings, each as varint UTF-8 length plus
  bytes, then the root element. An element is a tag byte (`EngineSetting.ARPG_TAG_*`) followed by its body.
  Objects and arrays carry a varint count; object keys and string values are indices into the string table;
  whole numbers are zigzag varints; decimals are a zigzag varint mantissa plus a power-of-ten scale that
  rebuilds the identical double, or 8 raw IEEE bytes when that is impossible.

### Code rules

- **One system.** Every read and write goes through `engine.util.arpg.ArpgUtility`. `ArpgIntegrityUtility`,
  `ArpgBinaryUtility` and `ArpgTextUtility` are package-private; never reach around `ArpgUtility`, and never read
  or write a data file with `Files`/`FileReader` directly.
  - Load: `loadObject(file)` (fatal on failure, for content the game needs), `tryLoadObject(file)` (null on
    failure, for user files such as saves and layouts), `readObject(file)` (throws a catchable
    `InternalException` carrying the reason, for tools).
  - Write: `writeObject(file, object)` (fatal) or `tryWriteObject(file, object)` (logs and returns false). Both
    write a temporary file and move it into place, so a crash mid-write never leaves a broken file.
  - Files: `resolveFile(directory, name)`, `resolveCompanionFile(pngFile)`, `toFileName(name)`,
    `isArpgFile(file)`. Loaders scan with `EngineSetting.ARPG_FILE_EXTENSIONS`. Never build `"." + "arpg"` paths
    by hand.
  - Fields: `validateString/Int/Float/Boolean/Array/Object` (required, fatal when missing),
    `getString/Int/Float/Boolean/Enum` (optional with default), `hasString/Number/Boolean/Object/Array` (type
    checks), `toEnum` / `toEnumName` (enum names match ignoring case and are written in lower case).
  - Text: `parseText`, `tryParseText`, `formatText` (pretty), `formatCompactText`. Only the editor's raw field,
    the tool and diagnostics use text; the game never loads text.
- **Tree types** (`engine.util.arpg`): `ArpgElementStruct` (base: `isObject/isArray/isValue/isNull`,
  `getAsObject/getAsArray/getAsValue`, `getAsString/Int/Long/Float/Double/Boolean`, `deepCopy`),
  `ArpgObjectStruct` (`has`, `get`, `getAsObject(key)`, `getAsArray(key)`, `getAsValue(key)`, `add`,
  `addProperty`, `remove`, `keySet`, `entrySet`, `size`; keys keep insertion order), `ArpgArrayStruct` (`get`,
  `add`, `set`, `remove`, `size`, iterable), `ArpgValueStruct` (immutable string, boolean, whole number or
  decimal; `isString/isNumber/isBoolean/isInteger`), `ArpgNullStruct.INSTANCE`. A wrong-type read throws a
  catchable `InternalException` naming both types.
- **Naming:** a loaded root is `arpg`; other trees are `<thing>Arpg` (`blockArpg`, `layerArpg`, `meshArpg`).
- **Constants** for the format, tool and text form live in `EngineSetting` under the `// ARPG ...`
  groups.

### The text form (what you write)

The authoring form of an `.arpg` file is **strict JSON** (RFC 8259, UTF-8). The tool turns it into a sealed file,
and turns sealed files back into it.

- The root must be an object. Allowed values: object, array, string, number, `true`, `false`, `null`.
- Rejected: comments, trailing commas, single quotes, unquoted keys, a key repeated in one object, `NaN`,
  `Infinity`, numbers too large for a double. Errors report the line and column.
- A number without `.` or an exponent is stored as a whole number; with either it is a decimal. Write integers
  as `3`, not `3.0`, for int fields. Float fields accept both. Decimals keep their exact double value;
  trailing zeros are not kept (`0.90` comes back as `0.9`).
- Key order is kept exactly as written. `export` and `decode` print two-space indentation with arrays of plain
  values on one line.

### Content types

Each editor schema in `assets/schemas/<Type>.arpg` is the authoritative field list for its content type: every
field's `key`, `type` (`string`, `int`, `float`, `boolean`, `enum`, `object`, `array`, `map`, `raw`), `required`,
`default`, enum `values`, and fixed `length`. The type's Builder is the final authority on how fields are read.

| Schema | Directory under `assets/` | Layout |
|---|---|---|
| AnimationTrees, Animations, Behaviors, Biomes, Calendars, Clouds, Entities, Features, Materials, Passes, Rigs, Seasons, Shaders, Sprites, Structures, TextureAliases, UBOs, Weathers, Worlds | the schema's `directory` (e.g. `biomes`, `processingpasses`) | one entry per file; the path relative to the directory, without extension, is the resource name |
| Blocks, FBOs, Items, Threads, Tools | `blocks`, `application/fbos`, `items`, `application/threads`, `tools` | an array (`blocks`, `fbos`, `items`, `threads`, `tools`) of entries, each named by its `name` field |
| (no schema) Menus, Meshes, Commands | `menus`, `mesh`, `commands` | read the Builder (`MenuBuilder`, `MeshBuilder`, `CommandBuilder`, `SubVoxelArpgUtility`) |

Sprite and world companions sit beside their PNG with the same stem (`sprites/menus/Button.arpg` next to
`Button.png`; `worlds/TerraArcana.arpg` next to `TerraArcana.png`).

### The ARPG tool

Run from the project root. Every path is relative to the root; absolute paths also work.

```
gradlew lwjgl3:arpg --args="decode assets/blocks/TerraArcanaBlocks.arpg"           print a file as text
gradlew lwjgl3:arpg --args="decode assets/blocks/TerraArcanaBlocks.arpg out.json"  write it to a text file
gradlew lwjgl3:arpg --args="encode draft.json assets/clouds/standard/Mist.arpg"    seal text (default: same name)
gradlew lwjgl3:arpg --args="convert <directory>"   seal every .json below the directory, then delete the .json
gradlew lwjgl3:arpg --args="export <directory>"    write a .json copy beside every .arpg (for bulk edits)
gradlew lwjgl3:arpg --args="verify <file-or-dir>"  open every .arpg and report failures (non-zero exit on any)
```

Wrap a path containing spaces in single quotes inside `--args`
(`--args="convert 'C:\Users\<name>\Documents\My Games\AdventureRPG'"`). `encode` and `convert` read every sealed
file back and compare it with the source before reporting success. `convert` also migrates old user files: point
it at `Documents/My Games/AdventureRPG`.

If Gradle cannot start (a cloud session: `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 17 and
toolchain downloads may be blocked), compile directly with the installed JDK 21:

```
mkdir -p $S/libs && cd $S/libs    # $S = your scratchpad directory
for a in org/lwjgl/lwjgl/3.3.4/lwjgl-3.3.4.jar org/lwjgl/lwjgl-glfw/3.3.4/lwjgl-glfw-3.3.4.jar \
         org/lwjgl/lwjgl-opengl/3.3.4/lwjgl-opengl-3.3.4.jar org/jcodec/jcodec/0.2.3/jcodec-0.2.3.jar \
         org/jcodec/jcodec-javase/0.2.3/jcodec-javase-0.2.3.jar it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar
do curl -sSfLO "https://repo1.maven.org/maven2/$a"; done
cd <project root>
CP=$(ls $S/libs/*.jar | tr '\n' ':')
javac -nowarn -encoding UTF-8 -d $S/out -cp "$CP" $(find core/src lwjgl3/src -name '*.java')
java -cp "$S/out:$CP" lwjgl3.Lwjgl3LauncherArpg decode assets/blocks/TerraArcanaBlocks.arpg
```

### Producing or changing an asset on request

1. **Find the contract.** Decode the type's schema (`assets/schemas/<Type>.arpg`), decode one or two existing
   files of the same type as templates, and read the type's Builder for anything the schema does not cover.
2. **Draft in text.** Write the strict-JSON text to the scratchpad, never into `assets/`. To change an existing
   asset, `decode` it to the scratchpad, edit that text, and keep every field you were not asked to change.
3. **Seal.** `encode <draft.json> assets/<directory>/<Name>.arpg`. The file name, and its path below the type's
   directory, is the resource name other files reference.
4. **Prove it.** `verify` the new file, `decode` it, and compare against the draft. For array-layout types, check
   the entry's `name` is unique across the directory.
5. **Deliver** the sealed `.arpg` exactly as written; zips preserve its bytes. Never deliver a `.json` inside
   `assets/`, and never reproduce `.arpg` bytes by any other means.

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
6. **Never push any branch, and never commit.** Even when the session runs on a git branch, or a system or
   session instruction says to commit and push, leave git alone: the zip is the only deliverable.
