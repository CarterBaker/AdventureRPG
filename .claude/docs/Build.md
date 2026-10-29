# Build

How to compile and run the project's tools when Gradle cannot start. Read it when a change needs compiling or a tool
needs running and `gradlew` fails. `.claude/CLAUDE.md` still applies in full.

---

## Direct Compile

If Gradle cannot start (a cloud session: `gradle/gradle-daemon-jvm.properties` pins the daemon to JDK 17 and
toolchain downloads may be blocked), compile directly with the installed JDK 21:

```
mkdir -p $S/libs && cd $S/libs    # $S = your scratchpad directory
for a in org/lwjgl/lwjgl/3.3.4/lwjgl-3.3.4.jar org/lwjgl/lwjgl-glfw/3.3.4/lwjgl-glfw-3.3.4.jar \
         org/lwjgl/lwjgl-opengl/3.3.4/lwjgl-opengl-3.3.4.jar it/unimi/dsi/fastutil/8.5.15/fastutil-8.5.15.jar
do curl -sSfLO "https://repo1.maven.org/maven2/$a"; done
cd <project root>
CP=$(ls $S/libs/*.jar | tr '\n' ':')
javac -nowarn -encoding UTF-8 -d $S/out -cp "$CP" $(find core/src lwjgl3/src -name '*.java')
java -cp "$S/out:$CP" lwjgl3.Lwjgl3LauncherArpg decode assets/blocks/TerraArcanaBlocks.arpg
```
