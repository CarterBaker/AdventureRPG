# ARPG Data Files

The `.arpg` file format, the code that reads and writes it, the text form, the content types and the ARPG tool.
Read it before reading, writing or producing any `.arpg` file or touching `engine.util.arpg`. The root `CLAUDE.md`
still applies in full.

---

## Overview

Every data file the engine reads or writes is an **`.arpg` file**: all content under `assets/` (blocks, biomes,
meshes, menus, materials, editor schemas, ...), world and sprite companions beside their PNGs, and the user files
under `Documents/My Games/AdventureRPG/` (`Settings.arpg`, `EditorSettings.arpg`, `Saves/Characters/*.arpg`,
`bin/editorLayout/*.arpg`). The engine ignores `.json` files entirely.

## The file on disk

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

## Code rules

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

## The text form (what you write)

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

## Content types

Each editor schema in `assets/schemas/<Type>.arpg` is the authoritative field list for its content type: every
field's `key`, `type` (`string`, `int`, `float`, `boolean`, `enum`, `object`, `array`, `map`, `raw`), `required`,
`default`, enum `values`, and fixed `length`. The type's Builder is the final authority on how fields are read.

Schemas also carry every rule the Builder enforces, so the editor refuses to save content the game would refuse to
load (`InfoValidationBranch` checks, `InfoIntegrityBranch` gates every save and delete). A string names other content
with `reference` (a schema; or `{"by": sibling, "schemas": {value: schema}}`), `resource` (`{"directory", "naming":
"stem"|"file"|"tile"}`) or `local` (a path in the same entry: `a[]` any element, `a[.]` the referring value's own
element, `m{}` a map's keys), with `allow` listing accepted values such as `""` for none; a map names content with
`key_reference`. Numbers take `min`/`max` (inclusive) or `above`/`below` (exclusive) and `at_least_field`; lists take
`min_count`/`max_count`; fields take `required_unless`, `required_when` (`{"key", "values"}`), `conflicts_with`,
`distinct` (inside array elements) and `unique` (top level). A schema may set `reference_form: "name"` (entries
registered by bare name) and `required_names` (entries the game cannot start without). When a Builder gains a rule,
add it to the schema in the same change. Keys a schema does not declare are shown as ignored and never block a save.

| Schema | Directory under `assets/` | Layout |
|---|---|---|
| AnimationTrees, Animations, Architectures, Behaviors, Biomes, Calendars, Clouds, Entities, Features, Furnishings, Materials, Passes, Rigs, Roads, Seasons, Settlements, Shaders, Shortcuts, Sprites, StructureLists, Structures, TextureAliases, UBOs, Weathers, Worlds | the schema's `directory` (e.g. `biomes`, `processingpasses`) | one entry per file; the path relative to the directory, without extension, is the resource name |
| Blocks, FBOs, Items, Threads, Tools | `blocks`, `application/fbos`, `items`, `application/threads`, `tools` | an array (`blocks`, `fbos`, `items`, `threads`, `tools`) of entries, each named by its `name` field |
| (no schema) Menus, Meshes, Commands | `menus`, `mesh`, `commands` | read the Builder (`MenuBuilder`, `MeshBuilder`, `CommandBuilder`, `SubVoxelArpgUtility`) |

Sprite and world companions sit beside their PNG with the same stem (`sprites/menus/Button.arpg` next to
`Button.png`; `worlds/TerraArcana.arpg` next to `TerraArcana.png`).

## The ARPG tool

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

If Gradle cannot start, run the tool from a direct compile (see `docs/Build.md`).

## Producing or changing an asset on request

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
