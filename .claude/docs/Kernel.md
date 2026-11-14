# Kernels

When and how a routine runs as C. The code guide is `.claude/docs/KernelPipeline.md`. Every constant on the Java side
is in `EngineSetting` (the `// Kernels` group); the C side holds only `KernelSetting.h`. `.claude/CLAUDE.md` still
applies in full.

---

## 1. Principles

- **C exists only as kernels behind `KernelPipeline`.** A kernel is one stateless function over memory the caller
  passes in. All state, lifecycle, registries and decisions stay in Java: there is no C engine, no second lifecycle,
  no C-side allocation and no C-side I/O.
- **Tune the Java first.** A routine moves to C only after its Java has been made as fast as Java runs it, and only
  when C then beats that tuned Java by a measured margin (section 2). The tuned Java is the baseline every kernel is
  judged against.
- **One structure, one loader, one call site.** Every kernel is reached the way LWJGL reaches OpenGL: one native
  library (`AdventureRPG`), loaded once by `KernelLoader` through `Library.loadNative`, and called only from
  `KernelManager.invoke` through `JNI.invokePI`. A new kernel adds no seam, no loader and no dependency.
- **Bit-identical, always.** Worlds regenerate from their name seeds, so a kernel's output is the Java routine's
  output bit for bit, on every CPU variant (section 7).
- **A kernel replaces its Java routine.** Once a kernel qualifies, the Java routine and every path only it used are
  deleted, so the logic lives in one place.

---

## 2. When a routine is worth moving to C

A routine is a candidate only when all of these hold:

1. **It is a measured cost.** It shows in a profile (JFR over a real run, or `gradlew lwjgl3:benchmark`) as a large
   share of a frame or a streaming task, not a guess.
2. **It runs on large batches.** One call fills a whole field: a chunk, a column, every point of a volume. Never one
   call per block or per sample; the call itself costs a JNI transition.
3. **Its data is flat.** It reads and writes primitive arrays in struct-of-arrays order. Routines that walk handles,
   maps and object graphs (greedy meshing's merge and emission, for one) do not belong in C: flattening every handle
   they touch costs more than C gains.
4. **It is eligible** (section 7): no Math call without a bit-exact C counterpart.
5. **C wins.** After the Java is tuned, the kernel's v2 variant is at least **1.1x** faster at the median and no worse
   at the 95th percentile, and every variant is bit-identical over the whole benchmark corpus. The margin keeps
   benchmark noise from deciding.

What wins in practice is float math that vectorises (square roots, divisions, interpolation across thousands of
independent points). Branchy integer logic gains little; the JIT already compiles it about as well as clang does.

---

## 3. Tuning the Java first

Before any C, the Java routine is rewritten in place with output identical to the original:

- Flat primitive arrays in struct-of-arrays order; gather what a pass reads once (one neighbor resolution per region,
  not per sample) and index by int afterwards.
- Invariants hoisted out of loops; shifts and masks for power-of-two divisions instead of `floorDiv` / `floorMod`.
- No virtual or interface calls, handle walks or allocation per element; no boxing, iterators or lambdas.
- Inner loops shaped for the JIT's auto-vectoriser: counted int loops over arrays, no calls or branches on the element.
- Fill-a-field entry points: a pass over a whole chunk or column per call.
- No Vector API (an incubator module on JDK 21 that needs extra JVM arguments).

The tuned Java ships whether or not a kernel later replaces it.

---

## 4. Layout and the three C roles

- **Two source roots.** Java is under `core/src/java/` and `lwjgl3/src/java/`; C is under `core/src/c/`.
- **The C tree mirrors the Java tree.** A kernel sits in the `c/` folder that mirrors the Java folder of the code that
  calls it: `CaveCarveBranch` in `application/bootstrap/worldpipeline/worldgenerationmanager/` calls
  `core/src/c/application/bootstrap/worldpipeline/worldgenerationmanager/CaveDensityUtility.c`. A kernel several
  pipelines call lives in the mirror of the shared Java folder that holds the logic they share (for example
  `core/src/c/engine/util/...`). A `c/` folder exists only when a file goes in it.
- **The base** sits in the mirror of `KernelLoader`'s folder:
  `core/src/c/application/kernel/kernelpipeline/kernelmanager/` holds `KernelSetting.h`, `KernelUtility.h` and
  `KernelUtility.c`. Every C utility includes `KernelUtility.h`, the way every Java utility extends `EngineUtility`.
- **C has exactly three roles,** named the engine's way:
  - `*Utility` — stateless functions. Every kernel is `<Name>Utility.c` with its `<Name>Utility.h`, named for what it
    does.
  - `*Struct` — a `typedef`'d struct, declared in its utility's header: the kernel's `<Name>ArgumentStruct` and any
    record layout it reads.
  - `*Setting` — a header of C-side constants only.
- **Includes** are full paths from `core/src/c`, the way Java imports are:
  `#include "application/kernel/kernelpipeline/kernelmanager/KernelUtility.h"`.
- **Shared helpers** (Java's `Math.max` semantics, say) live as `static` functions in the kernel that uses them; the
  second kernel that needs one moves it to a utility in the mirror of the Java folder holding the same helper
  (`core/src/c/engine/util/mathematics/...`). A helper compiled per variant names its exported symbols through
  `KERNEL_SYMBOL` too.
- **Style** mirrors `.claude/docs/Style.md` and `.clang-format`: 4-space indentation, 120 columns, one short block
  comment under the include list, no other block comments, `//` labels for groups, no commented-out code.

---

## 5. Calling convention and argument slots

- **One signature for every kernel:** `int ARPG_<Name>_V2(void *arguments)` and `..._V3`, declared once as
  `KERNEL(<Name>)`. The return value is a status: `KERNEL_STATUS_SUCCESS` (0), anything else a failure that
  `KernelManager.invoke` turns into `throwException` naming the kernel, its symbol and the status.
- **The argument block** is an array of eight-byte slots written in order by a `KernelArgumentInstance`. The C side
  reads it as `<Name>ArgumentStruct`, whose fields are only `int64_t`, `double` or pointers, so it has no padding.
  Ints travel widened to `int64_t`, floats to `double` (exact), buffers as the address of their first element.
- **The order exists in two places only:** the struct in the kernel's header and the caller's sequence of `put` calls.
  Change one, change the other in the same edit.
- **Settings travel as arguments.** A tuning value the routine reads comes from `EngineSetting` through a slot, never
  a copy in C, so it stays defined once.

---

## 6. Memory ownership

- Kernels read and write only memory the caller passes in: no `malloc`, no `free`, no globals other than `const`
  tables, no I/O, no printing. Every kernel is reentrant, so any worker thread may call any kernel.
- Every buffer is a direct, native-order buffer from `BufferUtility`, owned by the caller, one set per thread (an
  `AsyncContainerPackage`), created once and reused: zero steady-state garbage.
- **All bounds are checked in Java before the call;** C has none. Output buffers are sized from the input with the
  exact formula, never grown in a loop.
- Where a result feeds a GL upload, the kernel writes straight into the upload buffer, so a copy is removed rather
  than added.

---

## 7. CPU variants and determinism

- **Two variants of every kernel on x86-64** are compiled into the one library: `-march=x86-64-v2` (any x86-64 CPU of
  the last fifteen years, 128-bit SIMD) with symbol suffix `_V2`, and `-march=x86-64-v3` (AVX2, FMA, BMI2, 256-bit
  SIMD) with `_V3`. On arm64 (Apple Silicon) there is one, `-march=armv8-a` (128-bit NEON), at level 2: level 2 is
  every architecture's baseline. `KernelUtility.h` picks the suffix from `KERNEL_CPU_LEVEL`, so a kernel never names
  its variant and the Java side resolves every platform the same way.
- **`ARPG_KernelCpuLevel()`** (in `KernelUtility.c`, compiled once at the baseline) answers the highest level the CPU
  runs. On x86-64 it reads `cpuid` and the OS-enabled register state directly, rather than through
  `__builtin_cpu_supports`, which needs a runtime library the freestanding build does not link: v3 needs AVX, AVX2,
  BMI1, BMI2, F16C, FMA, LZCNT, MOVBE and OS-enabled AVX state. On arm64 it answers the baseline. `KernelLoader`
  resolves every kernel at that level, capped by the JVM property `adventurerpg.kernelCpuLevel` (`2` or `3`) when it
  is set.
- **Kernels are plain loops written for the auto-vectoriser:** `restrict` pointers, struct-of-arrays data, counts and
  settings loaded into locals before the loop, selects instead of branches, no calls left after inlining. No
  intrinsics, so each kernel's logic exists exactly once.
- **Bit-identical to Java:**
  - Compile with `-O2 -mtune=generic -ffp-contract=off -fno-fast-math -std=c11`. `-ffp-contract=off` keeps v3 and arm64
    (whose compilers fuse by default) from fusing multiply-adds Java does not fuse. `-fno-math-errno` only drops `errno`
    writes (no value changes) so square roots vectorise.
  - Use `float` where Java uses `float`, `double` where it uses `double`, and keep Java's evaluation order and its
    int-then-float promotions (`floorY + fade - y` adds the ints first).
  - `Math.max` / `Math.min` on floats have their own rules (NaN wins, +0 above -0): reproduce them, never use a bare
    comparison.
  - `(float) Math.sqrt(x)` on a float `x` equals `__builtin_sqrtf(x)`: double rounding is exact for square root.
  - **Not eligible:** a routine whose Java calls `sin`, `cos`, `exp`, `log`, `pow` or any Math function without a
    bit-exact C counterpart, unless that call is first replaced by the identical arithmetic on both sides.
  - Prove it: the benchmark checksums every routine's output over the whole corpus, and every variant must match the
    Java routine's checksum before the Java routine is deleted.

---

## 8. Crashes

A fault inside a kernel (a bad pointer, an overrun) ends the process at once with **no Java stack trace**. The JVM
writes its crash log to `logs/` (`-XX:ErrorFile`); the library is built with `-g`, so that log names the C function.
This is why every bound is checked in Java before the call.

---

## 9. Build and toolchain

- **`gradlew core:natives`** compiles every `core/src/c/**/*.c` with clang and links one `AdventureRPG` library per
  target with lld, freestanding (`-ffreestanding -nostdlib`: no C runtime, no imports), into
  `core/build/natives/<os>/<arch>/adventurerpg/`: `windows/x64/AdventureRPG.dll`, `linux/x64/libAdventureRPG.so`,
  `macos/x64/libAdventureRPG.dylib` and `macos/arm64/libAdventureRPG.dylib` (macOS 11 or later; lld signs the arm64
  library ad hoc, as Apple Silicon requires). Every target builds on any host, so the jars and packages carry all
  four. `core/build/natives` is a resource folder of `core`, and `processResources` depends on the task, so `run`,
  `runEditor`, `launch`, `arpg`, `benchmark`, the jars and the Construo packages all build and load it the same way.
  `jarWin`, `jarLinux` and `jarMac` each keep only their own platform's folders.
- **Kernel sources compile once per level the target lists;** `KernelUtility.c` compiles once at the baseline.
- **Toolchain:** clang and lld, from the PATH or the Gradle property `adventurerpg.clang`. On Windows install
  llvm-mingw (no Visual Studio needed). A missing clang fails the task with the install instructions.
- **Targets** are windows-x64, linux-x64, macos-x64 and macos-arm64. On any other platform `KernelLoader` fails with
  a clear message the first time a kernel is requested. The macOS libraries are built and checked here but have not
  yet run on a Mac: the first run there must pass `gradlew lwjgl3:benchmark` with the same generation checksum as
  Windows before a Mac build ships.
- **The ABI check.** `ARPG_KernelABI()` answers `KERNEL_ABI_VERSION`; `KernelLoader` compares it with
  `EngineSetting.KERNEL_ABI_VERSION` right after loading. Bump both whenever the calling convention, the slot rules or
  any kernel's argument struct changes; a stale library is then a fatal error that says to rebuild the natives.
- **Deliveries ship C source only,** never a compiled library. Build output stays under the ignored `core/build/`.

---

## 10. Porting checklist

1. **Profile or benchmark:** the routine is a measured cost, called on large batches.
2. **Tune the Java routine** (section 3) and measure it: that is the baseline.
3. **Check eligibility** (section 7).
4. **Write** `<Name>Utility.h` (with its `<Name>ArgumentStruct`) and `<Name>Utility.c` in the `c/` folder mirroring the
   caller's Java folder (or the shared folder for a kernel several pipelines call); add the name to
   `EngineSetting.KERNEL_NAMES`.
5. **In the caller:** resolve the ID once (`getKernelIDFromKernelName` in `get()`), own one
   `KernelArgumentInstance` and one set of buffers per thread, write the slots in header order, size outputs exactly,
   call `KernelManager.invoke`.
6. **Prove** every variant bit-identical against the tuned Java over the benchmark corpus, then measure; keep the
   kernel only at 1.1x or better on v2 with no worse 95th percentile.
7. **Delete** the Java routine and every path only it used.

---

## 11. Kernels today

| Kernel | Caller | Replaced |
|---|---|---|
| `CaveDensity` | `CaveCarveBranch.computeField` (volume carving, octant cuts, tide floors, cave floor probes, giant formation scans) | `CaveCarveBranch.computeDensity`, the cave density math of `TerrainCarveUtility` and the lake chamber and guard of `CaveLakeSiteStruct` |

`CaveDensity` was 35% of chunk generation in Java. With it, generation over the benchmark corpus went from 44.5 ms to
36.9 ms per chunk on v2 and 37.6 ms on v3 (medians of three runs, 95th percentile 72 ms to 61 ms), bit-identical on
both. v3 gains nothing over v2 here: the field is bound by its divisions and lattice reads, not by vector width.

**Measured and kept in Java:** greedy meshing. Tuned in Java it went from 70.1 ms to about 19.8 ms per chunk; its
face-classification slice ran 1.46x faster in C but is about a quarter of meshing, so the whole routine would gain
about 1.07x, under the bar. Terrain noise is about 5% of streaming and does not qualify either.
