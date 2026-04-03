# LightingPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **5**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/lightingpipeline/LightingPipeline.java`

**Type:** `class LightingPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.lightingpipeline`
  
**File size:** 18 lines

**What this class does:** `LightingPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.lightingpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.lightingpipeline.naturallightmanager.NaturalLightManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/lightingpipeline/directionallight/DirectionalLightHandle.java`

**Type:** `class DirectionalLightHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.lightingpipeline.directionallight`
  
**File size:** 84 lines

**What this class does:** `DirectionalLightHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.lightingpipeline.directionallight`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void constructor(UBOHandle uboHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public void push()` — Queues data for downstream systems (often render queues).
- `public Vector3 getDirection()` — Returns current state/value.
- `public Vector3 getColor()` — Returns current state/value.
- `public float getIntensity()` — Returns current state/value.
- `public UBOHandle getUBOHandle()` — Returns current state/value.
- `public void setDirection(float x, float y, float z)` — Mutates internal state for this object.
- `public void setColor(float r, float g, float b)` — Mutates internal state for this object.
- `public void setIntensity(float intensity)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/lightingpipeline/naturallightmanager/MoonLightSystem.java`

**Type:** `class MoonLightSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.lightingpipeline.naturallightmanager`
  
**File size:** 118 lines

**What this class does:** `MoonLightSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.lightingpipeline.naturallightmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.clockmanager.ClockManager`
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void update(float visualTimeOfDay)` — Runs frame-step maintenance and logic.
- `private float computeLunarPhase()` — Performs class-specific logic; see call sites and owning manager flow.
- `private float computeIntensity(float moonT, float lunarPhase)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 getDirection()` — Returns current state/value.
- `public Vector3 getColor()` — Returns current state/value.
- `public float getIntensity()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/lightingpipeline/naturallightmanager/NaturalLightManager.java`

**Type:** `class NaturalLightManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.lightingpipeline.naturallightmanager`
  
**File size:** 108 lines

**What this class does:** `NaturalLightManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.lightingpipeline.naturallightmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.clockmanager.ClockManager`
- `program.bootstrap.lightingpipeline.directionallight.DirectionalLightHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void update()` — Runs frame-step maintenance and logic.
- `private float lerp(float a, float b, float t)` — Performs class-specific logic; see call sites and owning manager flow.
- `public SunLightSystem getSunLightSystem()` — Returns current state/value.
- `public MoonLightSystem getMoonLightSystem()` — Returns current state/value.
- `public DirectionalLightHandle getDirectionalLight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/lightingpipeline/naturallightmanager/SunLightSystem.java`

**Type:** `class SunLightSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.lightingpipeline.naturallightmanager`
  
**File size:** 76 lines

**What this class does:** `SunLightSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.lightingpipeline.naturallightmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void update(float visualTimeOfDay)` — Runs frame-step maintenance and logic.
- `private float computeIntensity(float t)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 getDirection()` — Returns current state/value.
- `public Vector3 getColor()` — Returns current state/value.
- `public float getIntensity()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
