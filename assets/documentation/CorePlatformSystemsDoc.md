# CorePlatformSystemsDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **27**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/core/app/Application.java`

**Type:** `interface Application`
  
**Package:** `program.core.app`
  
**File size:** 11 lines

**What this class does:** `Application` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/app/ApplicationListener.java`

**Type:** `interface ApplicationListener`
  
**Package:** `program.core.app`
  
**File size:** 22 lines

**What this class does:** `ApplicationListener` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/app/CoreContext.java`

**Type:** `class CoreContext`
  
**Package:** `program.core.app`
  
**File size:** 21 lines

**What this class does:** `CoreContext` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Who this class talks to (direct imports):**
- `program.core.graphics.Graphics`
- `program.core.input.Input`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/app/Game.java`

**Type:** `class Game`
  
**Inheritance/implements:** `implements ApplicationListener`
  
**Package:** `program.core.app`
  
**File size:** 66 lines

**What this class does:** `Game` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Method intent:**
- `public void setScreen(Screen screen)` — Mutates internal state for this object.
- `public Screen getScreen()` — Returns current state/value.
- `public void render()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/core/app/Screen.java`.
- `public void resize(int width, int height)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void pause()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void resume()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose()` — Releases owned resources and unregisters state.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/app/Screen.java`

**Type:** `interface Screen`
  
**Package:** `program.core.app`
  
**File size:** 23 lines

**What this class does:** `Screen` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/app/Version.java`

**Type:** `class Version`
  
**Package:** `program.core.app`
  
**File size:** 10 lines

**What this class does:** `Version` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.app`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/DisplayModeStruct.java`

**Type:** `class DisplayModeStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 36 lines

**What this class does:** `DisplayModeStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package  DisplayModeStruct(int width, int height, int refreshRate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public int getRefreshRate()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3Application.java`

**Type:** `class Lwjgl3Application`
  
**Inheritance/implements:** `implements Application`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 164 lines

**What this class does:** `Lwjgl3Application` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.app.Application`
- `program.core.app.ApplicationListener`
- `program.core.app.CoreContext`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package public Lwjgl3Application(ApplicationListener listener, Lwjgl3ApplicationConfiguration config)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void applyWindowHints(int glMajor, int glMinor)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void registerCallbacks(long handle, Lwjgl3Input inp, Lwjgl3WindowAdapter adapter)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void loop()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Lwjgl3Window newWindow(Lwjgl3WindowConfiguration config)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void removeSecondaryWindow(long handle)` — Unregisters and tears down child references.
- `public void exit()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3ApplicationConfiguration.java`

**Type:** `class Lwjgl3ApplicationConfiguration`
  
**Inheritance/implements:** `extends Lwjgl3WindowConfiguration`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 85 lines

**What this class does:** `Lwjgl3ApplicationConfiguration` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Method intent:**
- `public DisplayModeStruct getDisplayMode()` — Returns current state/value. Called via static reference from: `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`, `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`.
- `public void setOpenGLVersion(int major, int minor)` — Mutates internal state for this object.
- `public void setFullscreenMode(DisplayModeStruct mode)` — Mutates internal state for this object.
- `public void useVsync(boolean vsync)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setWindowPosition(int x, int y)` — Mutates internal state for this object.
- `public void setWindowListener(Lwjgl3WindowAdapter listener)` — Mutates internal state for this object.
- `public int getGlMajor()` — Returns current state/value.
- `public int getGlMinor()` — Returns current state/value.
- `public boolean isFullscreen()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isVsync()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getWindowX()` — Returns current state/value.
- `public int getWindowY()` — Returns current state/value.
- `public Lwjgl3WindowAdapter getWindowListener()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3GL.java`

**Type:** `class Lwjgl3GL`
  
**Inheritance/implements:** `implements GL30`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 363 lines

**What this class does:** `Lwjgl3GL` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`
- `program.core.util.graphics.gl.GL30`

**Method intent:**
- `public void glEnable(int cap)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDisable(int cap)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDepthFunc(int func)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDepthMask(boolean flag)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBlendFunc(int src, int dst)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glCullFace(int mode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glFrontFace(int mode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glScissor(int x, int y, int w, int h)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glViewport(int x, int y, int w, int h)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glClearColor(float r, float g, float b, float a)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glClear(int mask)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glCreateProgram()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glCreateShader(int type)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glShaderSource(int shader, String source)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glCompileShader(int shader)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glGetShaderiv(int shader, int pname, IntBuffer params)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String glGetShaderInfoLog(int shader)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glAttachShader(int program, int shader)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDetachShader(int program, int shader)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteShader(int shader)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glLinkProgram(int program)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glGetProgramiv(int program, int pname, IntBuffer params)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String glGetProgramInfoLog(int program)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUseProgram(int program)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteProgram(int program)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glGetUniformLocation(int program, String name)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glGetUniformBlockIndex(int program, String name)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformBlockBinding(int program, int index, int binding)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform1i(int l, int v0)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform1f(int l, float v0)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform2i(int l, int v0, int v1)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform2f(int l, float v0, float v1)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform3i(int l, int v0, int v1, int v2)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform3f(int l, float v0, float v1, float v2)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform4i(int l, int v0, int v1, int v2, int v3)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform4f(int l, float v0, float v1, float v2, float v3)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform1iv(int l, int c, int[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform1fv(int l, int c, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform2iv(int l, int c, int[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform2fv(int l, int c, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform3iv(int l, int c, int[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform3fv(int l, int c, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform4iv(int l, int c, int[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniform4fv(int l, int c, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix2fv(int l, int c, boolean t, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix3fv(int l, int c, boolean t, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix4fv(int l, int c, boolean t, float[] v, int o)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix2fv(int l, int c, boolean t, FloatBuffer v)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix3fv(int l, int c, boolean t, FloatBuffer v)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glUniformMatrix4fv(int l, int c, boolean t, FloatBuffer v)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glGenTexture()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBindTexture(int target, int texture)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glActiveTexture(int texture)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glTexParameteri(int target, int pname, int param)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteTexture(int texture)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Buffer pixels)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glTexImage3D(int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, ByteBuffer pixels)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int glGenBuffer()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBindBuffer(int target, int buffer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBindBufferBase(int target, int index, int buffer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteBuffer(int buffer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteBuffers(int n, IntBuffer buffers)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBufferData(int target, int size, Buffer data, int usage)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBufferSubData(int target, int offset, int size, Buffer data)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glGenVertexArrays(int n, IntBuffer arrays)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glBindVertexArray(int array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDeleteVertexArrays(int n, IntBuffer arrays)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glEnableVertexAttribArray(int index)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glVertexAttribDivisor(int index, int divisor)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int pointer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDrawElements(int mode, int count, int type, int indices)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void glDrawElementsInstanced(int mode, int count, int type, int indices, int instancecount)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3Graphics.java`

**Type:** `class Lwjgl3Graphics`
  
**Inheritance/implements:** `implements Graphics`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 72 lines

**What this class does:** `Lwjgl3Graphics` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.graphics.Graphics`

**Method intent:**
- `package  Lwjgl3Graphics(int width, int height, boolean fullscreen)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void setSize(int width, int height)` — Mutates internal state for this object.
- `package void setDelta(float delta)` — Mutates internal state for this object.
- `package void setFullscreen(boolean fullscreen)` — Mutates internal state for this object.
- `package void setWindow(Lwjgl3Window window)` — Mutates internal state for this object.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public float getDeltaTime()` — Returns current state/value.
- `public boolean isFullscreen()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Lwjgl3Window getWindow()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3Input.java`

**Type:** `class Lwjgl3Input`
  
**Inheritance/implements:** `implements Input`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 141 lines

**What this class does:** `Lwjgl3Input` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.input.Input`
- `program.core.input.InputProcessor`

**Method intent:**
- `package  Lwjgl3Input(long window)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onCursor(double x, double y)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onMouseButton(int button, int action)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onScroll(double dx, double dy)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onKey(int key, int action)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onChar(int codepoint)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void endFrame()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setInputProcessor(InputProcessor processor)` — Mutates internal state for this object.
- `public void setCursorCatched(boolean captured)` — Mutates internal state for this object.
- `public int getDeltaX()` — Returns current state/value.
- `public int getDeltaY()` — Returns current state/value.
- `public int getX()` — Returns current state/value.
- `public int getY()` — Returns current state/value.
- `public float getScrollX()` — Returns current state/value.
- `public float getScrollY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3ManagedWindow.java`

**Type:** `class Lwjgl3ManagedWindow`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 29 lines

**What this class does:** `Lwjgl3ManagedWindow` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Method intent:**
- `package  Lwjgl3ManagedWindow(long handle, Lwjgl3Input input)` — Performs class-specific logic; see call sites and owning manager flow.
- `package long getHandle()` — Returns current state/value.
- `package Lwjgl3Input getInput()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3Window.java`

**Type:** `class Lwjgl3Window`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 57 lines

**What this class does:** `Lwjgl3Window` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Method intent:**
- `package  Lwjgl3Window(long handle, Lwjgl3Input input)` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getHandle()` — Returns current state/value.
- `public Lwjgl3Input getInput()` — Returns current state/value.
- `public int getPositionX()` — Returns current state/value.
- `public int getPositionY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3WindowAdapter.java`

**Type:** `class Lwjgl3WindowAdapter`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 13 lines

**What this class does:** `Lwjgl3WindowAdapter` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Method intent:**
- `public boolean closeRequested()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/backends/lwjgl3/Lwjgl3WindowConfiguration.java`

**Type:** `class Lwjgl3WindowConfiguration`
  
**Package:** `program.core.backends.lwjgl3`
  
**File size:** 27 lines

**What this class does:** `Lwjgl3WindowConfiguration` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.backends.lwjgl3`.

**Method intent:**
- `public void setTitle(String title)` — Mutates internal state for this object.
- `public void setWindowedMode(int width, int height)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/graphics/Graphics.java`

**Type:** `interface Graphics`
  
**Package:** `program.core.graphics`
  
**File size:** 17 lines

**What this class does:** `Graphics` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.graphics`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/input/Input.java`

**Type:** `interface Input`
  
**Package:** `program.core.input`
  
**File size:** 38 lines

**What this class does:** `Input` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.input`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/input/InputProcessor.java`

**Type:** `interface InputProcessor`
  
**Package:** `program.core.input`
  
**File size:** 45 lines

**What this class does:** `InputProcessor` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.input`.

**Method intent:**
- `package default boolean keyDown(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean keyUp(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean keyTyped(char character)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean touchDown(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean touchUp(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean touchCancelled(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean touchDragged(int screenX, int screenY, int pointer)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean mouseMoved(int screenX, int screenY)` — Performs class-specific logic; see call sites and owning manager flow.
- `package default boolean scrolled(float amountX, float amountY)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/settings/EngineSetting.java`

**Type:** `class EngineSetting`
  
**Package:** `program.core.settings`
  
**File size:** 288 lines

**What this class does:** `EngineSetting` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.settings`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/settings/Loader.java`

**Type:** `class Loader`
  
**Package:** `program.core.settings`
  
**File size:** 48 lines

**What this class does:** `Loader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.settings`.

**Method intent:**
- `public Settings load(File file, Gson gson)` — Parses external data into engine objects. Called via static reference from: `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`, `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`.
- `public void save(File file, Settings settings, Gson gson)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`, `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/settings/Settings.java`

**Type:** `class Settings`
  
**Package:** `program.core.settings`
  
**File size:** 195 lines

**What this class does:** `Settings` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.settings`.

**Who this class talks to (direct imports):**
- `program.core.input.Input`

**Method intent:**
- `package public Settings(Builder builder)` — Mutates internal state for this object.
- `public Builder FOV(float FOV)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder windowWidth(int windowWidth)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder windowHeight(int windowHeight)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder windowX(int windowX)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder windowY(int windowY)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder fullscreen(boolean fullscreen)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder maxRenderDistance(int maxRenderDistance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder mouseSensitivity(float mouseSensitivity)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyForward(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyBack(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyLeft(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyRight(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyJump(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyWalk(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keySprint(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Builder keyInventory(int key)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Settings build()` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/core/settings/SettingsDeserializer.java`

**Type:** `class SettingsDeserializer`
  
**Inheritance/implements:** `implements JsonDeserializer<Settings>`
  
**Package:** `program.core.settings`
  
**File size:** 43 lines

**What this class does:** `SettingsDeserializer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.core.settings`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`

**Type:** `class Lwjgl3Launcher`
  
**Package:** `lwjgl3`
  
**File size:** 98 lines

**What this class does:** `Lwjgl3Launcher` provides subsystem-specific behavior inferred from its APIs and collaborators in `lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.backends.lwjgl3.Lwjgl3Application`
- `program.core.backends.lwjgl3.Lwjgl3ApplicationConfiguration`
- `program.core.backends.lwjgl3.Lwjgl3Graphics`
- `program.core.backends.lwjgl3.Lwjgl3Window`
- `program.core.backends.lwjgl3.Lwjgl3WindowAdapter`
- `program.core.engine.Main`
- `program.core.settings.Loader`
- `program.core.settings.Settings`

**Method intent:**
- `public void main(String[] args)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void configureAwtForEngineRasterization()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void createApplication()` — Allocates/initializes child systems or resources.
- `public boolean closeRequested()` — Performs class-specific logic; see call sites and owning manager flow.
- `private Lwjgl3ApplicationConfiguration buildConfig(Settings settings)` — Constructs derived runtime/handle data from source input.
- `private void saveWindowInfoOnClose(File file, Settings settings)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`

**Type:** `class Lwjgl3LauncherEditor`
  
**Package:** `lwjgl3`
  
**File size:** 98 lines

**What this class does:** `Lwjgl3LauncherEditor` provides subsystem-specific behavior inferred from its APIs and collaborators in `lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.backends.lwjgl3.Lwjgl3Application`
- `program.core.backends.lwjgl3.Lwjgl3ApplicationConfiguration`
- `program.core.backends.lwjgl3.Lwjgl3Graphics`
- `program.core.backends.lwjgl3.Lwjgl3Window`
- `program.core.backends.lwjgl3.Lwjgl3WindowAdapter`
- `program.core.engine.MainEditor`
- `program.core.settings.Loader`
- `program.core.settings.Settings`

**Method intent:**
- `public void main(String[] args)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void configureAwtForEngineRasterization()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void createApplication()` — Allocates/initializes child systems or resources.
- `public boolean closeRequested()` — Performs class-specific logic; see call sites and owning manager flow.
- `private Lwjgl3ApplicationConfiguration buildConfig(Settings settings)` — Constructs derived runtime/handle data from source input.
- `private void saveWindowInfoOnClose(File file, Settings settings)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `lwjgl3/src/lwjgl3/Lwjgl3WindowPlatform.java`

**Type:** `class Lwjgl3WindowPlatform`
  
**Inheritance/implements:** `implements WindowPlatform`
  
**Package:** `lwjgl3`
  
**File size:** 149 lines

**What this class does:** `Lwjgl3WindowPlatform` provides subsystem-specific behavior inferred from its APIs and collaborators in `lwjgl3`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.backends.lwjgl3.Lwjgl3Application`
- `program.core.backends.lwjgl3.Lwjgl3Graphics`
- `program.core.backends.lwjgl3.Lwjgl3Window`
- `program.core.backends.lwjgl3.Lwjgl3WindowConfiguration`
- `program.core.engine.WindowPlatform`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `public void openWindow(WindowInstance window)` — Activates UI/window/menu surface.
- `public void destroyWindow(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean shouldClose(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void makeContextCurrent(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void swapBuffers(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void restoreMainContext()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void syncWindowSize(WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `lwjgl3/src/lwjgl3/StartupHelper.java`

**Type:** `class StartupHelper`
  
**Package:** `lwjgl3`
  
**File size:** 221 lines

**What this class does:** Adds some utilities to ensure that the JVM was started with the {@code -XstartOnFirstThread} argument, which is required on macOS for LWJGL 3 to function. Also helps on Windows when users have names with characters from outside the Latin alphabet, a common cause of startup crashes. <br> <a href= "https://jvm-gaming.org/t/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/57547">Based on this java-gaming.org post by kappa</a> @author damios

**Who this class talks to (direct imports):**
- `program.core.app.Version`

**Method intent:**
- `package private StartupHelper()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean startNewJvmIfRequired(boolean redirectOutput)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`, `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`, `lwjgl3/src/lwjgl3/StartupHelper.java`.
- `public boolean startNewJvmIfRequired()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `lwjgl3/src/lwjgl3/Lwjgl3Launcher.java`, `lwjgl3/src/lwjgl3/Lwjgl3LauncherEditor.java`, `lwjgl3/src/lwjgl3/StartupHelper.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
