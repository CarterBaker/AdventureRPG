# Build

How to compile the Java and the native kernels and run the project's tools, with Gradle or, when it cannot start,
directly. Read it when a change needs compiling or a tool needs running. `.claude/CLAUDE.md` still applies in full.

---

## Direct Compile

If Gradle cannot start (a cloud session: `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 17 and
toolchain downloads may be blocked), compile directly with the installed JDK 21:

```
mkdir -p $S/libs && cd $S/libs    # $S = your scratchpad directory
for a in org/lwjgl/lwjgl/3.3.4/lwjgl-3.3.4.jar org/lwjgl/lwjgl-glfw/3.3.4/lwjgl-glfw-3.3.4.jar \
         org/lwjgl/lwjgl-opengl/3.3.4/lwjgl-opengl-3.3.4.jar org/lwjgl/lwjgl-openal/3.3.4/lwjgl-openal-3.3.4.jar \
         org/lwjgl/lwjgl-stb/3.3.4/lwjgl-stb-3.3.4.jar org/jcodec/jcodec/0.2.5/jcodec-0.2.5.jar \
         it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar
do curl -sSfLO "https://repo1.maven.org/maven2/$a"; done
cd <project root>
CP=$(ls $S/libs/*.jar | tr '\n' ':')
javac -nowarn -encoding UTF-8 -d $S/out -cp "$CP" $(find core/src/java lwjgl3/src/java -name '*.java')
java -cp "$S/out:$CP" lwjgl3.Lwjgl3LauncherArpg decode assets/blocks/TerraArcanaBlocks.arpg
```

## Native Kernels

`gradlew core:natives` compiles every `core/src/c/**/*.c` and links the `AdventureRPG` library for windows-x64,
linux-x64, macos-x64 and macos-arm64 into `core/build/natives/<os>/<arch>/adventurerpg/` (see
`.claude/docs/Kernel.md`). `core`'s `processResources` depends on it, so every run, tool and packaging task builds it
first. It needs clang and lld: on Windows install llvm-mingw and put its `bin` folder on the PATH, or set
`adventurerpg.clang=<path to clang>` in `gradle.properties`. Deliveries ship the C source only.

The same build directly, from the project root (`$N` = a folder under your scratchpad); kernel sources compile once
per level the target lists, `KernelUtility.c` once at level 2. Add `-Werror` to prove zero warnings:

```
F="-std=c11 -O2 -mtune=generic -ffp-contract=off -fno-fast-math -fno-math-errno -ffreestanding"
F="$F -fvisibility=hidden -g -Wall -Wextra -Icore/src/c"
B=core/src/c/application/kernel/kernelpipeline/kernelmanager/KernelUtility.c
for T in x86_64-linux-gnu:-fPIC:linux/x64/adventurerpg/libAdventureRPG.so:2,3 \
         x86_64-w64-windows-gnu::windows/x64/adventurerpg/AdventureRPG.dll:2,3 \
         x86_64-apple-macos11::macos/x64/adventurerpg/libAdventureRPG.dylib:2,3 \
         arm64-apple-macos11::macos/arm64/adventurerpg/libAdventureRPG.dylib:2
do IFS=: read TR PIC OUT LV <<< "$T"; O=""
   M() { case $TR in arm64*) echo armv8-a;; *) echo x86-64-v$1;; esac; }
   clang --target=$TR $F $PIC -march=$(M 2) -DKERNEL_CPU_LEVEL=2 -c $B -o $N/obj_${TR}_base.o
   O="$O $N/obj_${TR}_base.o"
   for C in $(find core/src/c -name '*.c' ! -name KernelUtility.c); do for L in ${LV//,/ }
   do clang --target=$TR $F $PIC -march=$(M $L) -DKERNEL_CPU_LEVEL=$L -c $C -o $N/obj_${TR}_$(basename $C .c)_$L.o
      O="$O $N/obj_${TR}_$(basename $C .c)_$L.o"; done; done
   mkdir -p $N/natives/$(dirname $OUT); clang --target=$TR -shared -nostdlib -fuse-ld=lld $O -o $N/natives/$OUT
done
```

## Benchmark

`gradlew lwjgl3:benchmark` times world generation (through the kernels) and meshing over a fixed corpus behind a
hidden window, once at the CPU's highest kernel level and again for each lower level, and exits non-zero when any
variant's output differs. Directly, after the direct compile and the natives above, with the
`-natives-linux` jars of the five LWJGL modules also in `$S/libs` (`lwjgl-3.3.4-natives-linux.jar` and so on) and a
virtual display in a cloud session:

```
cd assets && xvfb-run -a java -XX:+UseZGC -XX:+ZGenerational -cp "$S/out:$N/natives:$CP" lwjgl3.Lwjgl3LauncherBenchmark
```
