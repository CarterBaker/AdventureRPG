# Kernel Pipeline — Instructions for Claude

The native kernel pipeline. The design and every rule are in `.claude/docs/Kernel.md`. This file says where things
live, which method owns each operation, and the traps. `.claude/CLAUDE.md` still applies in full.

---

## Layout

- **`KernelPipeline`** creates `KernelManager`. It is registered in `ApplicationKernelAssembly` right after
  `ThreadPipeline` and before `ProfilerPipeline`, so the game, the editor, the launcher and the benchmark all have it.
- **`application/kernel/kernelpipeline/kernelmanager/`:**

| Class | Owns |
|---|---|
| `KernelManager` | The library handle (`bindLibrary`, freed in `dispose`), the palette (`registerKernelName`, `addKernelHandle`), `getKernelIDFromKernelName` (on-demand `request` on a miss), `invoke` (the only call of a kernel), `getCpuLevel`, the per-frame profiler report in `update` |
| `KernelLoader` | Opening the library on the first request (`openLibrary`: platform check for windows-x64, linux-x64, macos-x64 and macos-arm64, `Library.loadNative`, `verifyABI`, `resolveCpuLevel`), resolving a kernel's variant symbol (`request`); persists for the engine lifetime |
| `KernelBuilder` | `build(name, id, symbol, address)`: the `KernelData` with its profiler counter names, wrapped in a `KernelHandle` |

- **`application/kernel/kernelpipeline/kernel/`:**

| Class | Owns |
|---|---|
| `KernelData` | Name, ID, symbol, function address, the two counter names |
| `KernelHandle` | Wraps the data; atomic call count and busy nanoseconds (`recordCall`, `drainCallCount`, `drainBusyNanos`) |
| `KernelArgumentInstance` | One call's argument block: `reset`, `putInt`/`putLong`/`putFloat`/`putDouble`/`putBuffer`, `getAddress` |

- **C base** (`core/src/c/application/kernel/kernelpipeline/kernelmanager/`): `KernelSetting.h` (ABI version, status,
  CPU levels, variant suffixes), `KernelUtility.h` (`KERNEL_EXPORT`, `KERNEL_SYMBOL`, `KERNEL(Name)`),
  `KernelUtility.c` (`ARPG_KernelABI`, `ARPG_KernelCpuLevel` from `cpuid` on x86-64 and the baseline on arm64, the
  Windows entry point). Anything architecture-specific in C stays behind `#if defined(__x86_64__)` in this file.
- **Settings:** the `// Kernels` group of `EngineSetting` (`KERNEL_NAMES`, library module and name, symbol prefix,
  ABI version, status, CPU levels and suffixes, the CPU level property, argument slots, counter formats, the rebuild
  advice every load error carries).
- **Build:** the `natives` task in `core/build.gradle` (see `.claude/docs/Build.md`).

---

## The kernels and their callers

| Kernel | C | Java caller |
|---|---|---|
| `CaveDensity` | `core/src/c/application/bootstrap/worldpipeline/worldgenerationmanager/CaveDensityUtility.{h,c}` | `CaveCarveBranch` |

- **`CaveCarveBranch`** resolves the ID in `get()` and owns the per-thread `CaveDensityAsyncContainer` (the field):
  - `bindField(frame, column)` copies the frame's lattice and lakes and the chunk's column fields into the field and
    empties it; `queueColumn` (or `field.addPoint`) queues points with a tag; `computeField(field)` writes the slots in
    `CaveDensityArgumentStruct` order and calls `KernelManager.invoke`; `field.getDensity` / `isLakeWater` /
    `getTag` read the results back.
  - Callers: `carveVolume` (`queueCell`, `carveBlocks`, `cutBlocks` in batches of `POINT_CAPACITY / 8` blocks),
    `probeCaveFloor`, `WorldGenerationManager.resolveTideFloors`, `CaveDressingBranch`'s giant formation scan.
  - `isCarved(..., density)` and `isSeaFeature` stay Java; the sea channel and cave region warps still sample the
    lattice in Java through `resolveFramePoint` / `sampleFramePoint`.
- **Adding a kernel:** name in `EngineSetting.KERNEL_NAMES`, `<Name>Utility.{h,c}` in the caller's mirrored `c/`
  folder, the ID resolved in the caller's `get()`, buffers and one `KernelArgumentInstance` on a per-thread container,
  the slot writes in one method beside the call.

---

## Traps

- **Only `KernelManager.invoke` calls a function pointer.** Never call `JNI.invoke*` on a kernel address anywhere else,
  and never resolve a symbol outside `KernelLoader`.
- **Buffer addresses are the first element's,** whatever the buffer's position (`putBuffer` uses
  `MemoryUtil.memAddress0`). `MemoryUtil.memAddress` adds the position; passing a buffer after relative puts with it
  hands C a pointer past the data.
- **Slot order is the struct order.** The `put` sequence in the caller and the `<Name>ArgumentStruct` fields change
  together, and a change to any struct bumps `KERNEL_ABI_VERSION` in both `EngineSetting` and `KernelSetting.h`.
- **Bounds are Java's job.** Check every count against the buffer capacity before queuing (`addPoint` does); a C
  overrun crashes the JVM with no Java stack trace.
- **Resolve IDs on the main thread.** `getKernelIDFromKernelName` registers on a miss and the palette is not thread
  safe; resolve in `get()` and keep the int. `invoke` itself is safe from any thread.
- **The library opens lazily.** The launcher never requests a kernel, so it never loads native code; the first
  request (the world generation branches' `get()`) opens it.
- **`uint8_t` results alias everything.** In C, load counts and settings into locals and mark the argument struct
  `restrict` in each pass, or the vectoriser reloads them every iteration and gives up.
- **Branches block vectorising.** Write Java's `Math.max`/`min` and early returns as selects with `&`, not `&&` or
  `if`; check with `clang -Rpass=loop-vectorize -Rpass-analysis=loop-vectorize` and count packed instructions.
- **No libc.** The library is freestanding: use `__builtin_sqrtf` and friends, never `math.h`, and no struct copies or
  zero-fill loops large enough to become `memcpy`/`memset` (the Windows link fails on them, which is the point).
- **Benchmark every variant.** `gradlew lwjgl3:benchmark` runs the engine once at the CPU's level and again for each
  lower level through `adventurerpg.kernelCpuLevel`, and fails when any variant's checksums differ.
