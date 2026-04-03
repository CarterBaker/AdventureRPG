# InputPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **3**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/inputpipeline/InputPipeline.java`

**Type:** `class InputPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.inputpipeline`
  
**File size:** 18 lines

**What this class does:** `InputPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.inputpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/inputpipeline/input/InputHandle.java`

**Type:** `class InputHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.inputpipeline.input`
  
**File size:** 137 lines

**What this class does:** `InputHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.inputpipeline.input`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public boolean isForward()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setForward(boolean forward)` — Mutates internal state for this object.
- `public boolean isBack()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setBack(boolean back)` — Mutates internal state for this object.
- `public boolean isLeft()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setLeft(boolean left)` — Mutates internal state for this object.
- `public boolean isRight()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setRight(boolean right)` — Mutates internal state for this object.
- `public boolean isJump()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setJump(boolean jump)` — Mutates internal state for this object.
- `public boolean isWalk()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setWalk(boolean walk)` — Mutates internal state for this object.
- `public boolean isSprint()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setSprint(boolean sprint)` — Mutates internal state for this object.
- `public boolean isPrimaryAction()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setPrimaryAction(boolean primaryAction)` — Mutates internal state for this object.
- `public boolean isSecondaryAction()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setSecondaryAction(boolean secondaryAction)` — Mutates internal state for this object.
- `public Vector3 getFacingDirection()` — Returns current state/value.
- `public void setFacingDirection(float x, float y, float z)` — Mutates internal state for this object.
- `public int getHorizontalX()` — Returns current state/value.
- `public int getHorizontalZ()` — Returns current state/value.
- `public int getVertical()` — Returns current state/value.
- `public boolean hasHorizontalInput()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/inputpipeline/inputsystem/InputSystem.java`

**Type:** `class InputSystem`
  
**Inheritance/implements:** `extends SystemPackage implements InputProcessor`
  
**Package:** `program.bootstrap.inputpipeline.inputsystem`
  
**File size:** 186 lines

**What this class does:** `InputSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.inputpipeline.inputsystem`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.input.InputProcessor`
- `program.core.engine.SystemPackage`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void start()` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public boolean keyDown(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean keyUp(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean touchDown(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean touchUp(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean mouseMoved(int screenX, int screenY)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean keyTyped(char character)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean touchDragged(int screenX, int screenY, int pointer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean scrolled(float amountX, float amountY)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean touchCancelled(int screenX, int screenY, int pointer, int button)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void captureCursor(boolean captured)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/runtime/input/PlayerInputSystem.java`.
- `public boolean keyHeld(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean keyJustPressed(int keycode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2 getMouseDelta()` — Returns current state/value.
- `public float getMouseX()` — Returns current state/value.
- `public float getMouseY()` — Returns current state/value.
- `public boolean isLeftClick()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRightClick()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRawLeftClick()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
