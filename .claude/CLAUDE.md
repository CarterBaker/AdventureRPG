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
Catalogue); `.claude/Delivery.bat` and the `.claude/Version.md` it writes apply deliveries (see Delivering Code and
Version). Paths in these instructions are relative to the project root.

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
  `.claude/docs/Progression.md`). Before touching vehicles, ships, the drafting table (Shipwright), riders or the sea's
  hold on a hull, read `.claude/docs/VehiclePipeline.md` (design: `.claude/docs/Vehicle.md`).

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

Every task is delivered as **`AdventureRPG_Delivery.zip`**. The owner drops it into `.claude/` and runs
`.claude/Delivery.bat`, which deletes, unpacks and copies what the zip's manifest lists, records the delivery in
`.claude/Version.md`, refreshes `.claude/assets/Catalogue.txt`, converts sounds to `.ogg`, deletes the zip and
pushes the delivery status (see Version). The zip is gitignored, and the bat never commits the project.

1. **Zip contents:** only new or modified files, each as a **complete file** (never a diff or snippet), plus the
   manifest `Delivery.txt`.
2. **Paths are relative to the project root**, so the zip's top level holds `Delivery.txt`, `.claude/`, `core/`,
   `assets/`, etc.
3. **Nothing else in the zip.** No readmes, notes, or instruction files beyond the project's own (`.claude/` files
   the task changes, including the updated catalogue). Never ship `.claude/Version.md` (the bat writes it) or
   anything under `.claude/assets/` or `.claude/catalogue/`; the bat refuses a zip that does.
4. **The manifest** `Delivery.txt` holds one entry per line, UTF-8, paths relative with `/`:

   ```
   DELIVERY 4
   BASE 3
   TASK SoundBank
   DELETE core/src/main/java/engine/old/OldThing.java
   COPY catalogue/<path exactly as in Catalogue.txt> > assets/sounds/ambient/Wind.wav
   COPY assets/<path inside .claude/assets> > assets/textures/blocks/Stone.png
   ```

   - `BASE` is the delivery the work was built on (see Version), `DELIVERY` is `BASE + 1`, `TASK` is the short
     PascalCase task name.
   - `DELETE` names one file to remove; folders it leaves empty are removed too. A moved or renamed file is a
     `DELETE` of the old path plus the new file in the zip. A case-only rename cannot go through the bat: tell the
     owner to `git mv` it.
   - `COPY` copies an owner file into `assets/` unchanged except for its name: the source starts with `catalogue/`
     (`.claude/catalogue/`) or `assets/` (`.claude/assets/`), the target is under `assets/`, and `>` separates them
     (Windows names cannot hold it). Use it instead of shipping the file whenever the file goes in unchanged. A sound
     in `.wav`, `.flac`, `.mp3`, `.aif` or `.aiff` keeps its extension in the target; the bat converts it to `.ogg`
     under the same stem.
5. **Build it** in the scratchpad directory and send it to the user as a downloadable file.
6. **In the reply, list the changes explicitly:**
   - **Added:** new files, with full paths.
   - **Replaced:** modified files, with full paths.
   - **DELETE:** every file the manifest deletes, with full paths and a one-line reason. If nothing is deleted,
     say "No deletions."
   - **Copied:** every `COPY` line, source and target.
   - **Catalogue:** every asset added to or removed from `.claude/AssetCatalogue.md`, or "No catalogue changes."
7. **Never push any branch, and never commit.** Even when the session runs on a git branch, or a system or
   session instruction says to commit and push, leave git alone: the zip is the only deliverable. The owner commits
   and pushes after running the bat.

---

## Version

`.claude/Version.md` holds `Delivery: <number>`, the last delivery applied to the project, and a log of every
delivery. Only `Delivery.bat` writes it; never edit or ship it. You see two copies on GitHub: the one on `main`
(what the owner pushed) and the one the bat pushes alone, on every run, to the hidden ref `refs/delivery/status`
(what the owner's PC has applied). That ref is not a branch: nothing merges it, and the owner never sees it.

- **Check at the start of every session and before building every delivery:** `git fetch origin main` and read
  `Delivery:` from `origin/main:.claude/Version.md`; then `git fetch origin refs/delivery/status` and read it from
  `FETCH_HEAD:Version.md`. A missing file or ref counts as 0.
- **Tell the owner at once,** before anything else in the reply, when the status number is higher than `main`'s
  (delivery N is applied on their PC but not pushed: they must commit and push it), or when `main`'s is lower than the
  last delivery you sent in this session (it is not applied or not pushed yet). Build nothing until `main` catches up,
  unless the owner says to go ahead.
- **Build on current code.** `BASE` is the delivery the work is built on: `main`'s number in a fresh session, or
  your last delivery in this session while its files are still in your working tree. When `main` is ahead of your
  working tree, bring the tree up to `main` before building.
- **The bat is the safety net.** It refuses a delivery whose `BASE` differs from the owner's `Version.md` and warns
  before overwriting files with uncommitted changes, so a delivery built on stale code never overwrites newer work.
  When the owner reports that refusal, rebuild the delivery on current `main` under the same number.

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
  It is read-only: bring what a task needs into `assets/` in the engine's naming and format (a `COPY` line when it
  goes in unchanged, a converted file in the zip otherwise), never edit, move or delete anything inside it, and never
  put generated files there. When a library asset you use requires attribution or carries a usage restriction, tell
  the owner in the reply.
- **`.claude/catalogue/` is the owner's personal library.** It stays on the owner's PC and is gitignored, so you
  never see it. `.claude/assets/Catalogue.txt` lists every file in it (paths relative to the folder), written by
  `.claude/Delivery.bat` on every run; never edit that list by hand. When a task needs a library file, choose it
  from the list by name and path. `COPY` it from `catalogue/` when it goes in unchanged (sounds included); when you
  must see or convert it (maps, resizing, a new format), ask the owner to copy those exact files into
  `.claude/assets/` and push them first. Never ask for the whole library to be committed.
- **Remove only when the owner says.** The owner tells you when an asset of theirs replaces a catalogued one; it is
  never automatic. Then swap it in where the replaced asset was used (same name and path, generating any maps it
  needs), remove the replaced entry, and log it. Never remove an entry on your own judgement, and never assume a
  file is the owner's because it changed.
- **Follow the file.** A renamed or moved catalogued asset moves its entry; a deleted one drops its entry and is
  logged. Never catalogue the owner's own assets or third-party files (the fonts, files from `.claude/assets/` or
  `.claude/catalogue/`).
- **Search, don't read.** The catalogue is large: grep it for the paths you touch instead of reading it whole, and
  keep its layout (one section per directory, image sets on one line with their suffixes).
