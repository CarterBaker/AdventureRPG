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
- **Performance first.** Lag spikes, GC pauses and memory growth are bugs (see `docs/Performance.md`).

---

## Documentation

The detailed rules live in `docs/`, one Markdown file per topic and one per pipeline. They apply as fully as this
file. The Markdown files are written for Claude; the owner's own documents in `docs/` (`.docx`) are not part of
these instructions.

- **Topics.** Imported below, so each loads with this file:
  - `docs/Engine.md`: class roles, naming, folder layout, lifecycle, settings, registry IDs, threads, errors,
    collections and visibility.
  - `docs/Style.md`: indentation, spacing, comments, field groups, section headers and the reference class shape.
  - `docs/Performance.md`: zero steady-state garbage, releasing storage, footprint, greedy meshing and measuring.
  - `docs/ARPG.md`: the `.arpg` format, `ArpgUtility`, the text form, content types, the ARPG tool and producing
    assets.
  - `docs/Build.md`: compiling and running tools directly when Gradle cannot start.
- **Pipelines.** Each pipeline has its own file named after it without the suffix (`ProgressionPipeline` is
  `docs/Progression.md`). Before working in a pipeline, read its file and its folder's `CLAUDE.md` when they exist,
  small tasks included. Before touching levels, stats, gear, trades, skills, professions, morality or health, read
  `core/src/application/bootstrap/progressionpipeline/CLAUDE.md` (design: `docs/Progression.md`).

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
