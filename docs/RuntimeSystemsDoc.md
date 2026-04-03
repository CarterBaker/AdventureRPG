# RuntimeSystemsDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **6**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/runtime/RuntimeContext.java`

**Type:** `class RuntimeContext`
  
**Inheritance/implements:** `extends ContextPackage`
  
**Package:** `program.runtime`
  
**File size:** 40 lines

**What this class does:** `RuntimeContext` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime`.

**Who this class talks to (direct imports):**
- `program.core.engine.ContextPackage`
- `program.runtime.input.PlayerInputSystem`
- `program.runtime.lighting.SkySystem`
- `program.runtime.menu.MenuSystem`
- `program.runtime.player.PlayerSystem`
- `program.runtime.world.WorldSystem`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/runtime/input/PlayerInputSystem.java`

**Type:** `class PlayerInputSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.runtime.input`
  
**File size:** 131 lines

**What this class does:** `PlayerInputSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime.input`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.playermanager.PlayerManager`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.core.engine.SystemPackage`
- `program.core.util.camera.CameraInstance`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `private void handleInventoryInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void updateCameraRotation()` — Runs frame-step maintenance and logic.
- `private void writePlayerInput()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/runtime/lighting/SkySystem.java`

**Type:** `class SkySystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.runtime.lighting`
  
**File size:** 34 lines

**What this class does:** `SkySystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime.lighting`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.bootstrap.shaderpipeline.passmanager.PassManager`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/runtime/menu/MenuSystem.java`

**Type:** `class MenuSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.runtime.menu`
  
**File size:** 30 lines

**What this class does:** `MenuSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime.menu`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/runtime/player/PlayerSystem.java`

**Type:** `class PlayerSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.runtime.player`
  
**File size:** 31 lines

**What this class does:** `PlayerSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime.player`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.playermanager.PlayerManager`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/runtime/world/WorldSystem.java`

**Type:** `class WorldSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.runtime.world`
  
**File size:** 36 lines

**What this class does:** `WorldSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.runtime.world`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.playermanager.PlayerManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
