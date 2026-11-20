#!/usr/bin/env bash
# Checks that the editor starts and its menus open, in a cloud session with no screen. Claude runs this, never the
# engine or the editor. From the project root:
#
#     .claude/tools/EditorSmoke.sh <work-dir> [seconds]
#
# It compiles every Java source into <work-dir>/out, builds the Linux kernels into <work-dir>/natives once (again only
# when a C source is newer), runs the editor under a virtual display for the seconds asked (45 by default), saves a
# screenshot of it as <work-dir>/EditorSmoke.png and prints every error the editor's session log wrote. It exits
# non-zero when the editor logged an error other than having no audio device. The libraries come from
# /opt/adventurerpg/libs when the environment's setup script fetched them, otherwise they are fetched into
# <work-dir>/libs. See .claude/docs/Build.md.

set -euo pipefail

WORK=$(mkdir -p "${1:?work directory}" && cd "$1" && pwd)
RUN_SECONDS=${2:-45}
ROOT=$(pwd)
LIBS=/opt/adventurerpg/libs
MAVEN=https://repo1.maven.org/maven2
JARS="org/lwjgl/lwjgl/3.3.4/lwjgl-3.3.4 org/lwjgl/lwjgl-glfw/3.3.4/lwjgl-glfw-3.3.4
org/lwjgl/lwjgl-opengl/3.3.4/lwjgl-opengl-3.3.4 org/lwjgl/lwjgl-openal/3.3.4/lwjgl-openal-3.3.4
org/lwjgl/lwjgl-stb/3.3.4/lwjgl-stb-3.3.4 org/lwjgl/lwjgl-tinyfd/3.3.4/lwjgl-tinyfd-3.3.4"
OTHER_JARS="org/jcodec/jcodec/0.2.5/jcodec-0.2.5 it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15"

# Libraries
if [ ! -d "$LIBS" ]; then
    LIBS=$WORK/libs
    mkdir -p "$LIBS"
    for A in $JARS $OTHER_JARS; do
        [ -f "$LIBS/$(basename "$A").jar" ] || curl -sSfL -o "$LIBS/$(basename "$A").jar" "$MAVEN/$A.jar"
    done
    for A in $JARS; do
        N=$(basename "$A")-natives-linux.jar
        [ -f "$LIBS/$N" ] || curl -sSfL -o "$LIBS/$N" "$MAVEN/$A-natives-linux.jar"
    done
fi
CP=$(ls "$LIBS"/*.jar | tr '\n' ':')

# Java
rm -rf "$WORK/out"
javac -nowarn -encoding UTF-8 -d "$WORK/out" -cp "$CP" $(find core/src/java lwjgl3/src/java -name '*.java')

# Kernels, Linux only, as .claude/docs/Build.md builds them for every target
LIB=$WORK/natives/linux/x64/adventurerpg/libAdventureRPG.so
if [ ! -f "$LIB" ] || [ -n "$(find core/src/c -name '*.[ch]' -newer "$LIB")" ]; then
    F="-std=c11 -O2 -mtune=generic -ffp-contract=off -fno-fast-math -fno-math-errno -ffreestanding"
    F="$F -fvisibility=hidden -g -Wall -Wextra -Icore/src/c -fPIC --target=x86_64-linux-gnu"
    OBJ=$WORK/natives_obj
    mkdir -p "$OBJ" "$(dirname "$LIB")"
    BASE=core/src/c/application/kernel/kernelpipeline/kernelmanager/KernelUtility.c
    clang $F -march=x86-64-v2 -DKERNEL_CPU_LEVEL=2 -c "$BASE" -o "$OBJ/base.o"
    O="$OBJ/base.o"
    for C in $(find core/src/c -name '*.c' ! -name KernelUtility.c); do
        for L in 2 3; do
            clang $F -march=x86-64-v$L -DKERNEL_CPU_LEVEL=$L -c "$C" -o "$OBJ/$(basename "$C" .c)_$L.o"
            O="$O $OBJ/$(basename "$C" .c)_$L.o"
        done
    done
    clang --target=x86_64-linux-gnu -shared -nostdlib -fuse-ld=lld $O -o "$LIB"
fi

# Run
LOGS="$HOME/Documents/My Games/AdventureRPG/Logs"
mkdir -p "$LOGS"
BEFORE=$(ls "$LOGS" | wc -l)
cat > "$WORK/run.sh" <<EOF
cd "$ROOT/assets"
java -cp "$WORK/out:$WORK/natives:$CP" lwjgl3.Lwjgl3LauncherEditor > "$WORK/editor.out" 2>&1 &
P=\$!
sleep $RUN_SECONDS
import -window root "$WORK/EditorSmoke.png"
kill \$P
EOF
timeout $((RUN_SECONDS + 60)) xvfb-run -a -s "-screen 0 1600x900x24" bash "$WORK/run.sh" > /dev/null 2>&1 || true

# Report
if [ "$(ls "$LOGS" | wc -l)" -le "$BEFORE" ]; then
    echo "The editor wrote no session log; see $WORK/editor.out"
    exit 1
fi
LOG="$LOGS/$(ls -t "$LOGS" | head -1)"
ERRORS=$(grep -E '\[(ERROR|FATAL)\]' "$LOG" | grep -v 'No audio device' || true)
echo "Screenshot: $WORK/EditorSmoke.png"
if [ -n "$ERRORS" ]; then
    echo "$ERRORS"
    exit 1
fi
echo "The editor started with no errors."
