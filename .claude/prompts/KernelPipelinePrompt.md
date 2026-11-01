Implement the complete source layout change, hot-path tuning and native kernel system below for AdventureRPG in
this one conversation, start to finish, without stopping between parts and without asking questions: every
decision is already made below. It is a large, cross-cutting task: read every system it touches, trace every
caller, use subagents for broad searches, and follow .claude/CLAUDE.md and every file in .claude/docs/ exactly
(Engine, Style, Performance, ARPG, Build). Run the Version check from CLAUDE.md first. Deliver everything as one
zip, AdventureRPG_Delivery.zip with TASK KernelPipeline, per CLAUDE.md's "Delivering Code" rules, with the full
Added / Replaced / DELETE / Copied / Catalogue lists. Never commit or push.

# Goal

Split the source tree by language: Java under a java folder, C under a c folder that mirrors the Java tree. Make the
hottest CPU routines as fast as Java can run them, then let the engine run the ones C beats further as C, through
the same path LWJGL already uses for every OpenGL call, with one structure, one loader and one call site, so that
moving a routine to C adds no new seam, no new hitch and no new dependency. Ship the tuned Java, the infrastructure
and the first kernel together with a benchmark tool, so every later port is a small, repeatable task.

# Current state (verify each point before relying on it)

- Java sources sit directly under core/src/ (application, editor, engine, launcher; core/build.gradle sets
  srcDirs = ['src']) and lwjgl3/src/ (package lwjgl3; lwjgl3/build.gradle sets srcDirs = ['src']). Paths to them
  appear in README.md, .claude/CLAUDE.md (the manifest example), .claude/docs/Build.md (the direct compile), the
  pipeline guides in .claude/docs/ and .claude/prompts/TextureStreamingPrompt.md; grep the whole repository
  outside assets/ for "core/src" and "lwjgl3/src" to find every one. .editorconfig and .clang-format set the
  formatting rules.
- LWJGL 3.3.4 (lwjgl3/build.gradle) already loads native libraries and calls C through function pointers.
  org.lwjgl.system.Library.loadNative(Class, String module, String name) loads a library from the classpath
  (SharedLibraryLoader extracts it from a jar when needed) or java.library.path; SharedLibrary.getFunctionAddress
  returns a symbol's address; org.lwjgl.system.JNI holds the generic invokers, including invokeI(long) and
  invokePI(long, long) (int return, function address last). Confirm the classpath layout loadNative expects for
  a module (<os>/<arch>/<module path>/<library file>) by reading LWJGL's Library and Platform before relying on it.
- Off-heap memory: engine.util.memory.BufferUtility creates native-order direct buffers for GL upload and reuses
  scratch buffers; org.lwjgl.system.MemoryUtil.memAddress returns a direct buffer's address.
- Kernel pipelines live in application/kernel/ and are created by ApplicationKernelAssembly (Thread, Window,
  Input, FrameRate, Profiler). GameEngine, EditorEngine and LauncherEngine all create that assembly.
  ThreadPipeline (ThreadManager, ThreadLoader, ThreadBuilder, ThreadHandle; definitions in
  assets/application/threads/*.arpg, EngineSetting.THREAD_CATALOG_PATH) is the reference shape: mirror it.
- EngineUtility holds the kernel managers it is assigned in their awake() (assignThreadManager and friends);
  ThreadManager.executeAsync times each task for the profiler; ProfilerManager.recordCount takes a counter name.
- Candidate hot routines: terrain greedy meshing (geometrypipeline/dynamicgeometrymanager, FullGeometryBranch and
  its siblings) and terrain noise fill (worldpipeline/util/TerrainNoiseUtility over
  engine/util/mathematics/extras/NoiseUtility). Noise is sampled per point today; a kernel only pays when it
  fills a whole field per call.
- The ARPG tool (lwjgl3/src/lwjgl3/Lwjgl3LauncherArpg.java, Gradle task lwjgl3:arpg) is the precedent for a
  headless tool with its own launcher and task.
- The game already runs on generational ZGC (lwjgl3/build.gradle, gameJvmArgs); leave the JVM arguments alone.
- clang and gcc exist in the cloud container, and its CPU supports AVX2 and FMA, so both CPU variants below can
  be built and run there; the owner builds on Windows.

# Decisions (final)

1. Source layout. Every Java file moves from core/src/<package path> to core/src/java/<package path> and from
   lwjgl3/src/lwjgl3 to lwjgl3/src/java/lwjgl3. Packages, class names and file contents do not change for the move
   itself. All C lives under core/src/c/, whose folders mirror the Java folders: a kernel sits in the c/ folder
   that mirrors the Java folder of the code calling it, so world pipeline code calls kernels in the world
   pipeline's own c/ folders (a kernel called from application/bootstrap/worldpipeline/util/ lives in
   core/src/c/application/bootstrap/worldpipeline/util/). A kernel called from more than one pipeline follows
   the same rule as shared Java: it lives in the mirror of the shared Java folder that holds the logic both use
   (core/src/c/engine/util/... for engine utilities). Create a c/ folder only when a file goes in it; no empty
   mirror folders. C files follow the engine's role naming, and C has exactly three roles, because C holds no
   state, lifecycle or registry (all of that stays in Java, so there is no C engine and no second lifecycle):
   *Utility (stateless functions: every kernel lives in a <Name>Utility.c with its <Name>Utility.h, named for
   what it does, such as TerrainNoiseUtility), *Struct (a typedef'd struct, such as a kernel's
   <Name>ArgumentStruct, declared in its utility's header) and *Setting (a header of C-side constants only).
   Update srcDirs in core/build.gradle and lwjgl3/build.gradle, and every path reference found by the grep
   above, including CLAUDE.md's manifest example and the direct compile in Build.md. Add a [*.{c,h}] section with
   indent_size = 4 to .editorconfig, and treat *.c and *.h in .gitattributes the way it treats *.java, if it names
   *.java. Deliver the move per CLAUDE.md: a DELETE line for every old path and the complete file at its new path.
   Add one line to Engine.md's folder-layout rule naming the two roots.
2. Tune the Java first. Before writing any C, optimise both candidate routines in Java, in place, keeping their
   output bit-identical: flat primitive arrays in struct-of-arrays order, invariants hoisted out of loops, no
   virtual or interface calls per block or per sample, inner loops shaped so the JIT's auto-vectoriser can take
   them (counted int loops over arrays, no calls or branches on the element), bounds checks the JIT can prove
   away, and fill-a-field entry points that do a whole chunk per call instead of one sample per call. No Vector
   API (it is an incubator module on JDK 21 and needs extra JVM arguments). Measure each candidate before and
   after with the benchmark tool. The tuned Java ships whether or not a kernel replaces it, and it is the
   baseline every C version is measured against.
3. Name: KernelPipeline. "native" is a reserved word in Java and cannot be a package, so the handle folder could
   never be native/. Layout: core/src/java/application/kernel/kernelpipeline/KernelPipeline.java,
   kernelpipeline/kernelmanager/ (KernelManager, KernelLoader, KernelBuilder), kernelpipeline/kernel/
   (KernelData, KernelHandle, KernelArgumentInstance). Created in ApplicationKernelAssembly after ThreadPipeline
   and before ProfilerPipeline; update the assembly's class comment.
4. One calling convention for every kernel: int ARPG_<Name>(void *arguments). The argument block is an array of
   8-byte slots written in order; the C side reads it as a struct whose fields are only int64_t, double or
   pointers, so it has no padding. Floats and ints travel widened; buffers travel as addresses. The return value
   is a status: 0 is success, anything else is a failure the Java side turns into throwException naming the
   kernel and the status. Every kernel call therefore goes through JNI.invokePI and nothing else.
5. One call site: KernelManager.invoke(int kernelID, KernelArgumentInstance arguments). It is the only code that
   calls a kernel's function pointer. It times the call for the profiler the way executeAsync times tasks and
   checks the status. Callers resolve the kernel name to an ID once (getKernelIDFromKernelName, on-demand request
   on a miss, mirroring getThreadHandleFromThreadName), cache the int and index by it afterwards.
6. Registry: kernels are data-driven like threads. assets/application/kernels/Kernels.arpg holds an array
   "kernels" of entries named by "name"; EngineSetting.KERNEL_CATALOG_PATH points at it. IDs come only from
   RegistryUtility, in registration order, at load time. The symbol is EngineSetting.KERNEL_SYMBOL_PREFIX ("ARPG_")
   plus the name plus the CPU variant's suffix (decision 9); a missing symbol is a fatal load error naming the
   kernel and the symbol. Add a schema for Kernels only if Threads has one; check assets/schemas first and mirror
   it exactly.
7. Loading: one library, AdventureRPG, module "adventurerpg", loaded once by KernelLoader through
   Library.loadNative. Right after loading, call the exported int ARPG_KernelABI(void) and compare it with
   EngineSetting.KERNEL_ABI_VERSION; a mismatch (a stale library) is a fatal error that says to rebuild the
   natives. KernelManager frees the library in dispose(). KernelManager assigns itself to EngineUtility in awake()
   like the other kernel managers.
8. Arguments and memory: KernelArgumentInstance owns one direct buffer from BufferUtility sized to
   EngineSetting.KERNEL_ARGUMENT_SLOTS slots, with a cursor and putInt/putLong/putFloat/putDouble/putBuffer
   (putBuffer writes MemoryUtil.memAddress of a direct buffer), plus reset(). Each caller owns one instance per
   thread, created once and reused: zero steady-state garbage. Kernels read and write only memory the caller
   passes in: no malloc, no free, no globals other than const tables, no I/O, no printing, fully reentrant so
   any worker thread may call any kernel. All bounds and capacities are checked on the Java side before the
   call, because C has no bounds checks; output buffers are sized from the input with the exact formula, never
   grown in a loop. Where the routine's result feeds a GL upload, the kernel writes straight into the upload
   buffer, so the copy is removed rather than added.
9. CPU variants: every kernel source is compiled twice into the one library, once with -march=x86-64-v2 (any
   x86-64 CPU of the last fifteen years, 128-bit SIMD) and once with -march=x86-64-v3 (AVX2, FMA and BMI2,
   256-bit SIMD). A macro in KernelUtility.h names each kernel's symbol with the variant's suffix (_V2 or _V3),
   so a kernel is written once and never mentions its variant. KernelUtility.c, compiled once at the v2 level,
   also exports int ARPG_KernelCpuLevel(void), which checks every feature x86-64-v3 requires (avx, avx2, bmi,
   bmi2, f16c, fma, lzcnt, movbe) with __builtin_cpu_supports and returns the highest level the CPU runs.
   KernelLoader resolves every kernel at that level, capped by the JVM property named in
   EngineSetting.KERNEL_CPU_LEVEL_PROPERTY when it is set, so the benchmark can measure both variants on one
   machine. Kernels are plain loops written for the auto-vectoriser (restrict pointers, struct-of-arrays data, no
   aliasing, no calls in inner loops); no intrinsics, so each kernel's logic exists exactly once.
10. Determinism: every variant's output must be bit-identical to the tuned Java routine, because worlds are
    regenerated from name seeds. Compile with -O2 -mtune=generic -ffp-contract=off -fno-fast-math -std=c11
    -fvisibility=hidden -g plus the variant's -march, and export only ARPG_ symbols; -ffp-contract=off keeps the
    v3 build from fusing multiply-adds that Java does not fuse. Use float where Java uses float and double where
    Java uses double. A routine whose Java version calls a Math function with no bit-exact C counterpart (sin,
    cos, exp, log, pow and similar) is not eligible unless that call is replaced by the identical arithmetic on
    both sides first.
11. C source shape. The C base sits in the mirror of the folder whose code calls it, KernelLoader's:
    core/src/c/application/kernel/kernelpipeline/kernelmanager/ holds KernelSetting.h (ABI version, status codes,
    variant suffixes), KernelUtility.h (the export and variant-symbol macros) and KernelUtility.c
    (ARPG_KernelABI, ARPG_KernelCpuLevel). Every C utility includes KernelUtility.h, the way every Java utility
    extends EngineUtility. C helpers shared by several kernels are ordinary utilities in the mirror of the Java
    folder that holds the same helpers (math helpers in core/src/c/engine/util/mathematics/...), created only
    when a second kernel needs them. Each kernel's <Name>ArgumentStruct lists its slots in exactly the order the
    Java caller writes them; the caller's put sequence is the only other place that order appears. The include
    root is core/src/c, and includes are written as full paths from it, the way Java imports are
    ("application/kernel/kernelpipeline/kernelmanager/KernelUtility.h"). C style mirrors
    Style.md and .clang-format: 4-space indentation, 120 columns, one short block comment under each file's
    include list, no other block comments, // labels for groups, no commented-out code.
12. Build: a Gradle task core:natives compiles every core/src/c/**/*.c with clang (kernel sources once per
    variant) and links them into core/build/natives/<os>/<arch>/adventurerpg/ (AdventureRPG.dll on Windows,
    libAdventureRPG.so on Linux), using the exact layout loadNative expects, and adds core/build/natives to
    core's main resources so dev runs, the jar and the Construo package all load it identically. core's
    processResources depends on it, so run, runEditor, launch, arpg and the packaging tasks build it
    automatically. The clang path comes from the PATH or the Gradle property adventurerpg.clang; when clang is
    missing the task fails with a message saying how to install it. On Windows the target is
    x86_64-w64-windows-gnu (llvm-mingw's clang, no Visual Studio needed). Targets are windows-x64 and linux-x64;
    macOS is not a target, and on any other platform KernelLoader fails with a clear message. Confirm the jarMac,
    jarLinux and jarWin excludes still select the right library. Deliveries ship C source only, never a compiled
    library. Generated output stays under the already ignored core/build/.
13. Crashes: keep -XX:ErrorFile pointed at logs/; the -g build makes the JVM crash log name C functions. State
    in Kernel.md that a C fault ends the process with no Java stack trace.
14. Benchmark tool: lwjgl3/src/java/lwjgl3/Lwjgl3LauncherBenchmark.java with Gradle task lwjgl3:benchmark, built
    the way the ARPG tool is. It runs each candidate routine headless over a fixed corpus (a fixed world name
    salted through RegistryUtility.toNameSeed, a fixed list of chunk coordinates, inputs built exactly as the
    pipeline builds them), with warm-up iterations then measured iterations from EngineSetting constants, and
    prints the median and 95th percentile time per call and the bytes allocated per call (ThreadMXBean thread
    allocated bytes) for the Java routine and for each kernel variant the CPU runs. Kernel runs go through
    KernelManager.invoke, never a separate path; the variant is selected through the CPU level property. If the
    kernel pipeline cannot start headless, add the smallest headless boot that creates only ThreadPipeline and
    KernelPipeline, mirroring LauncherEngine, and route the tool through it. If a candidate needs a GL context,
    benchmark its CPU half only.
15. The first kernel: after tuning (decision 2), write both candidates as C kernels and benchmark both variants
    of each through KernelManager. Port every candidate whose v2 variant is at least 1.1x faster than the tuned
    Java at the median and whose 95th percentile is no worse, with every variant bit-identical over the whole
    corpus; the 1.1x margin keeps benchmark noise from deciding. A ported kernel replaces the Java routine at its
    single call site, and the Java routine is deleted, so the logic lives in one place. A candidate that does not
    qualify keeps its tuned Java and its C file is not shipped. If no candidate qualifies, deliver the source
    layout change, the tuned Java and the benchmark tool (without kernel paths), ship no kernel infrastructure
    and no core/src/c/, and report the numbers.
16. Profiler: per kernel, calls and total milliseconds per frame through ProfilerManager.recordCount. Counter
    names are built once in KernelBuilder and stored on the handle, never concatenated per call.
17. Documentation: .claude/docs/Kernel.md (design: when a routine is worth moving to C, tuning the Java first,
    eligibility, the mirrored folder layout, the three C roles, the calling convention, the argument slots, memory
    ownership, CPU variants, determinism, crashes, build and toolchain, and the porting checklist below) and
    .claude/docs/KernelPipeline.md (code guide: where things live, which method owns each operation, the traps).
    Add both to the pipeline list in .claude/CLAUDE.md with a "Before touching kernels or any C code under
    core/src/c/, read ..." line, add the natives task and the direct clang commands to .claude/docs/Build.md, and
    add one line to .claude/docs/Engine.md stating that C code exists only as kernels behind KernelPipeline.

# Porting checklist (write it into Kernel.md and follow it for the first kernel)

1. Profile or benchmark: the routine must be a measured cost, called on large batches (one call per chunk or
   field, never per block or per sample).
2. Tune the Java routine (decision 2) and measure it; that is the baseline.
3. Check eligibility (decision 10).
4. Write <Name>Utility.h (with its <Name>ArgumentStruct) and <Name>Utility.c in the c/ folder mirroring the
   caller's Java folder (or the shared folder, for a kernel several pipelines call), add the entry to
   Kernels.arpg.
5. In the caller: resolve the ID once, own one KernelArgumentInstance per thread, write the slots in header
   order, size outputs exactly, call KernelManager.invoke.
6. Prove every variant bit-identical against the tuned Java over the benchmark corpus, then measure; keep the
   kernel only at 1.1x or better on v2 with no worse 95th percentile.
7. Delete the Java routine and every path that only it used.

# Keep unchanged

The engine's lifecycle, registry rules, class roles and package layout; the thread pipeline; greedy meshing results
and quad packing; world generation results; the ARPG format; every JVM argument; every routine that is not a
candidate.

# Verification before delivering

- Confirm no Java file remains outside core/src/java/ and lwjgl3/src/java/, and that every package declaration
  still matches its folder.
- Compile all Java with Gradle or, if it cannot start, the direct compile in .claude/docs/Build.md (updated for the
  new roots). Zero errors.
- Build the natives for linux-x64 with clang in the container, both variants, using the exact flags the Gradle
  task uses, with -Wall -Wextra -Werror. Zero warnings.
- Run the benchmark tool in the container: original Java, tuned Java, v2 and v3 medians, 95th percentiles,
  allocation per call, and the bit-identical comparison of every version over the whole corpus.
- Confirm the ABI check fires on a mismatched version, a missing symbol fails with a clear message, and the CPU
  level property selects v2 on this AVX2 machine, then restore.
- Run ArpgUtility verify over assets/application/.
- Re-read the whole diff adversarially for leftovers: no TODOs, no stubs, no dead Java routine, no debug output,
  no second call path to a kernel's function pointer, no stale "core/src/" path without java/ or c/.
- In the reply, state the measured numbers (original Java, tuned Java, v2, v3; per call and projected per chunk)
  and exactly what the owner must do on Windows: install llvm-mingw and put its bin folder on the PATH (or set
  adventurerpg.clang), run gradlew lwjgl3:benchmark, then play and compare the profiler's frame times against the
  previous build.
