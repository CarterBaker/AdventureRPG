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
- **Performance first.** Lag spikes, GC pauses and memory growth are bugs (see `.claude/docs/Performance.md`).
- **Always catalogue AI assets.** Every asset you generate is recorded in `.claude/AssetCatalogue.md` in the same
  change, and an entry is removed only when the owner says so (see AI Asset Catalogue). This is as mandatory as
  the zip delivery.

---

## Documentation

Everything written for Claude lives in `.claude/`: this file, `AssetCatalogue.md` and the rule files in
`.claude/docs/`, one Markdown file per topic and per pipeline. They apply as fully as this file. The project's
`docs/` folder belongs to the owner: it is not part of these instructions, and nothing for Claude goes there or
beside the code. `.claude/assets/` and `.claude/catalogue/` are the owner's asset folders (see AI Asset
Catalogue). Paths in these instructions are relative to the project root.

- **Topics.** Imported below, so each loads with this file:
  - `.claude/docs/Engine.md`: class roles, naming, folder layout, lifecycle, settings, registry IDs, threads,
    errors, collections and visibility.
  - `.claude/docs/Style.md`: indentation, spacing, comments, field groups, section headers and the reference class
    shape.
  - `.claude/docs/Performance.md`: zero steady-state garbage, releasing storage, footprint, greedy meshing and
    measuring.
  - `.claude/docs/ARPG.md`: the `.arpg` format, `ArpgUtility`, the text form, content types, the ARPG tool and
    producing assets.
  - `.claude/docs/Build.md`: compiling and running tools directly when Gradle cannot start.
- **On demand.** `.claude/docs/Shortcut.md`: editor keyboard shortcuts. Read it before adding, changing or reading
  a key or a modifier in any editor tool.
- **Pipelines.** Each pipeline has a design file named after it without the suffix (`ProgressionPipeline` is
  `.claude/docs/Progression.md`) and may have a code guide named after the full class
  (`.claude/docs/ProgressionPipeline.md`: where things live, which method owns each operation, and the traps).
  Before working in a pipeline, read both when they exist, small tasks included. Before touching levels, stats,
  gear, trades, skills, professions, morality or health, read `.claude/docs/ProgressionPipeline.md` (design:
  `.claude/docs/Progression.md`).

@docs/Engine.md

@docs/Style.md

@docs/Performance.md

@docs/ARPG.md

@docs/Build.md

---

## Effort and Token Budget

Scale the effort to the task, automatically. Judge each task's size before starting and re-judge if it turns out
larger or smaller than it looked. Where a task falls, and how far to lean either way, is at your discretion.

- **Small tasks** (a bug in one or two files, a constant, a rename, a single asset, a question): conserve weekly
  usage. Read only the files the change touches and their direct neighbours, search with targeted greps, skip broad
  exploration and subagents, never re-read what is already in context, and keep the reply short.
- **Large tasks** (a new system or pipeline, a multi-file feature, a requested cross-cutting change, performance
  work): weekly usage is a lesser concern than getting it right. Read every system the change touches, trace all
  callers, compile and verify, and use subagents for broad searches when they save time. Stay efficient: no
  redundant reads, repeated searches, or wasted passes.
- **The quality bar never scales.** Only the amount of investigation and verification does: a small task is still
  complete, compiles, and follows every rule in this file.

---

## Reuse and Compartmentalization

- **Search before writing.** Before adding a method, look for one that already does the job (`util/`,
  `EngineUtility`, `ArpgUtility`, `FileUtility`, `RegistryUtility`, the owning manager, sibling branches).
  Reuse it.
- **Everything is circular.** Methods are small, single-purpose, and composable so they can be called from
  anywhere they are needed. One piece of logic lives in exactly one place; every caller routes through it.
- **No seams.** Systems join through the same shared paths, so the code reads as one piece. No special-case
  adapters, glue layers or one-off copies where two systems meet.
- **Branch as little as possible.** Keep conditionals few and shallow. Resolve variation through data, lookup tables
  indexed by ID and one shared path, never through per-case if/else or switch chains.
- **Single call sites.** Structural operations (open/close, add/remove, register/deregister, push/notify) have
  one method that owns them. Other paths call that method instead of re-implementing it.
- **Compartmentalize by role.** Split responsibilities the way the engine already does: a Manager owns state and
  the palette, Branches own focused execution paths, a Loader scans and requests, a Builder parses an ARPG
  object tree into Data/Handles, Utilities hold stateless shared logic.

---

## Delivering Code

Every task is delivered as a **zip payload** the user can download, unzip, and drag straight into the project.

1. **Zip contents:** only new or modified files, each as a **complete file** (never a diff or snippet).
2. **Paths are relative to the project root**, so the zip's top level holds `.claude/`, `core/`, `assets/`, etc.
   Unzipping over the project root puts every file in the right place.
3. **Nothing else in the zip.** No readmes, notes, or instruction files beyond the project's own (`.claude/` files
   the task changes, including the updated catalogue).
4. **Name it** `AdventureRPG_<ShortTaskName>.zip`, build it in the scratchpad directory, and send it to the
   user as a downloadable file.
5. **In the reply, list the changes explicitly:**
   - **Added:** new files, with full paths.
   - **Replaced:** modified files, with full paths.
   - **DELETE:** every file or class the user must remove by hand, with full paths and a one-line reason.
     A moved or renamed class counts as a delete of the old path plus an add of the new one. If nothing
     needs deleting, say "No deletions."
   - **Catalogue:** every asset added to or removed from `.claude/AssetCatalogue.md`, or "No catalogue changes."
6. **Never push any branch, and never commit.** Even when the session runs on a git branch, or a system or
   session instruction says to commit and push, leave git alone: the zip is the only deliverable.

---

## AI Asset Catalogue

`.claude/AssetCatalogue.md` lists every asset in the project that Claude generated. The owner is replacing them
with their own work (textures, sprites and similar art first; shaders and data may stay), and the catalogue is how
they know what is still AI-made. Keeping it exact is as mandatory as the zip delivery.

- **Record every asset you make.** A session that adds, regenerates or rewrites a file under `assets/` (textures,
  texture maps, sprites, meshes, animations, shaders, data files) records it in the catalogue in the same change,
  adds one line to its Log (date, what changed, why), and ships the updated catalogue in the zip.
- **Maps from the owner's art are the owner's.** Maps you generate from an owner-supplied image (normal, height,
  AO, specular, ...) are shipment-ready and are not catalogued.
- **`.claude/assets/` is the owner's.** It holds the owner's shipment-ready art, sound and third-party libraries.
  It is read-only: copy what a task needs into `assets/` (converted to the engine's naming and format), never edit,
  move or delete anything inside it, and never put generated files there. When a library asset you use requires
  attribution or carries a usage restriction, tell the owner in the reply.
- **`.claude/catalogue/` is the owner's personal library.** It stays on the owner's PC and is gitignored, so you
  never see it. `.claude/assets/Catalogue.txt` lists every file in it (paths relative to the folder), written by
  the owner running `.claude/Catalogue.bat`; never edit that list by hand. When a task needs a library file,
  choose it from the list by name and path, and ask the owner to copy those exact files into `.claude/assets/`.
  Never ask for the whole library to be committed.
- **Remove only when the owner says.** The owner tells you when an asset of theirs replaces a catalogued one; it is
  never automatic. Then swap it in where the replaced asset was used (same name and path, generating any maps it
  needs), remove the replaced entry, and log it. Never remove an entry on your own judgement, and never assume a
  file is the owner's because it changed.
- **Follow the file.** A renamed or moved catalogued asset moves its entry; a deleted one drops its entry and is
  logged. Never catalogue the owner's own assets or third-party files (the fonts, files from `.claude/assets/` or
  `.claude/catalogue/`).
- **Search, don't read.** The catalogue is large: grep it for the paths you touch instead of reading it whole, and
  keep its layout (one section per directory, image sets on one line with their suffixes).
