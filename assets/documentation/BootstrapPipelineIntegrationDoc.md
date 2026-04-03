# BootstrapPipelineIntegrationDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **334**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/BootstrapAssembly.java`

**Type:** `class BootstrapAssembly`
  
**Inheritance/implements:** `extends AssemblyPackage`
  
**Package:** `program.bootstrap`
  
**File size:** 37 lines

**What this class does:** `BootstrapAssembly` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.CalendarPipeline`
- `program.bootstrap.entitypipeline.EntityPipeline`
- `program.bootstrap.geometrypipeline.GeometryPipeline`
- `program.bootstrap.inputpipeline.InputPipeline`
- `program.bootstrap.itempipeline.ItemPipeline`
- `program.bootstrap.lightingpipeline.LightingPipeline`
- `program.bootstrap.menupipeline.MenuPipeline`
- `program.bootstrap.physicspipeline.PhysicsPipeline`
- `program.bootstrap.renderpipeline.RenderPipeline`
- `program.bootstrap.shaderpipeline.ShaderPipeline`
- `program.bootstrap.worldpipeline.WorldPipeline`
- `program.core.engine.AssemblyPackage`

**Method intent:**
- `public void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/CalendarPipeline.java`

**Type:** `class CalendarPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.calendarpipeline`
  
**File size:** 20 lines

**What this class does:** `CalendarPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendarmanager.CalendarManager`
- `program.bootstrap.calendarpipeline.clockmanager.ClockManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/calendar/CalendarData.java`

**Type:** `class CalendarData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.calendarpipeline.calendar`
  
**File size:** 64 lines

**What this class does:** `CalendarData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.calendar`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public CalendarData(String calendarName, ObjectArrayList<String> daysOfWeek, ObjectArrayList<String> monthNames, Object2ByteOpenHashMap<String> monthDays, int totalDaysInYear)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getCalendarName()` — Returns current state/value.
- `public ObjectArrayList<String> getDaysOfWeek()` — Returns current state/value.
- `public ObjectArrayList<String> getMonthNames()` — Returns current state/value.
- `public Object2ByteOpenHashMap<String> getMonthDays()` — Returns current state/value.
- `public int getTotalDaysInYear()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/calendar/CalendarHandle.java`

**Type:** `class CalendarHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.calendarpipeline.calendar`
  
**File size:** 60 lines

**What this class does:** `CalendarHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.calendar`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(CalendarData calendarData)` — Engine-side initialization entrypoint invoked post-create.
- `public CalendarData getCalendarData()` — Returns current state/value.
- `public String getCalendarName()` — Returns current state/value.
- `public int getDaysPerWeek()` — Returns current state/value.
- `public int getMonthCount()` — Returns current state/value.
- `public int getTotalDaysInYear()` — Returns current state/value.
- `public String getDay(int index)` — Returns current state/value.
- `public String getMonthName(int index)` — Returns current state/value.
- `public byte getMonthDays(int index)` — Returns current state/value.
- `public byte getMonthDays(String monthName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/calendarmanager/CalendarManager.java`

**Type:** `class CalendarManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.calendarpipeline.calendarmanager`
  
**File size:** 74 lines

**What this class does:** `CalendarManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.calendarmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addCalendarHandle(CalendarHandle calendarHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasCalendar(String calendarName)` — Boolean existence/availability check.
- `public short getCalendarIDFromCalendarName(String calendarName)` — Returns current state/value.
- `public CalendarHandle getCalendarHandleFromCalendarID(short calendarID)` — Returns current state/value.
- `public CalendarHandle getCalendarHandleFromCalendarName(String calendarName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/calendarmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.calendarpipeline.calendarmanager`
  
**File size:** 90 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.calendarmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarData`
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `package CalendarHandle build(File file, String calendarName)` — Constructs derived runtime/handle data from source input.
- `private ObjectArrayList<String> parseDaysOfWeek(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object2ByteOpenHashMap<String> parseMonths(JsonObject json, ObjectArrayList<String> monthNames)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int calculateTotalDaysInYear(Object2ByteOpenHashMap<String> monthDays)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/calendarmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.calendarpipeline.calendarmanager`
  
**File size:** 90 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.calendarmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String calendarName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clock/ClockData.java`

**Type:** `class ClockData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clock`
  
**File size:** 166 lines

**What this class does:** `ClockData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clock`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ClockData(long worldEpochStart)` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.
- `public long getTotalDaysElapsed()` — Returns current state/value.
- `public void setTotalDaysElapsed(long totalDaysElapsed)` — Mutates internal state for this object.
- `public long getTotalDaysWithOffset()` — Returns current state/value.
- `public void setTotalDaysWithOffset(long totalDaysWithOffset)` — Mutates internal state for this object.
- `public double getDayProgress()` — Returns current state/value.
- `public void setDayProgress(double dayProgress)` — Mutates internal state for this object.
- `public double getVisualTimeOfDay()` — Returns current state/value.
- `public void setVisualTimeOfDay(double visualTimeOfDay)` — Mutates internal state for this object.
- `public double getYearProgress()` — Returns current state/value.
- `public void setYearProgress(double yearProgress)` — Mutates internal state for this object.
- `public double getVisualYearProgress()` — Returns current state/value.
- `public void setVisualYearProgress(double visualYearProgress)` — Mutates internal state for this object.
- `public int getCurrentMinute()` — Returns current state/value.
- `public void setCurrentMinute(int currentMinute)` — Mutates internal state for this object.
- `public int getCurrentHour()` — Returns current state/value.
- `public void setCurrentHour(int currentHour)` — Mutates internal state for this object.
- `public int getCurrentDayOfWeek()` — Returns current state/value.
- `public void setCurrentDayOfWeek(int currentDayOfWeek)` — Mutates internal state for this object.
- `public int getCurrentDayOfMonth()` — Returns current state/value.
- `public void setCurrentDayOfMonth(int currentDayOfMonth)` — Mutates internal state for this object.
- `public int getCurrentMonth()` — Returns current state/value.
- `public void setCurrentMonth(int currentMonth)` — Mutates internal state for this object.
- `public int getCurrentYear()` — Returns current state/value.
- `public void setCurrentYear(int currentYear)` — Mutates internal state for this object.
- `public int getCurrentAge()` — Returns current state/value.
- `public void setCurrentAge(int currentAge)` — Mutates internal state for this object.
- `public float getRandomNoiseFromDay()` — Returns current state/value.
- `public void setRandomNoiseFromDay(float randomNoiseFromDay)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clock/ClockHandle.java`

**Type:** `class ClockHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.calendarpipeline.clock`
  
**File size:** 149 lines

**What this class does:** `ClockHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clock`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ClockData clockData)` — Engine-side initialization entrypoint invoked post-create.
- `public ClockData getClockData()` — Returns current state/value.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.
- `public long getTotalDaysElapsed()` — Returns current state/value.
- `public void setTotalDaysElapsed(long totalDaysElapsed)` — Mutates internal state for this object.
- `public long getTotalDaysWithOffset()` — Returns current state/value.
- `public void setTotalDaysWithOffset(long totalDaysWithOffset)` — Mutates internal state for this object.
- `public double getDayProgress()` — Returns current state/value.
- `public void setDayProgress(double dayProgress)` — Mutates internal state for this object.
- `public double getVisualTimeOfDay()` — Returns current state/value.
- `public void setVisualTimeOfDay(double visualTimeOfDay)` — Mutates internal state for this object.
- `public double getYearProgress()` — Returns current state/value.
- `public void setYearProgress(double yearProgress)` — Mutates internal state for this object.
- `public double getVisualYearProgress()` — Returns current state/value.
- `public void setVisualYearProgress(double visualYearProgress)` — Mutates internal state for this object.
- `public int getCurrentMinute()` — Returns current state/value.
- `public void setCurrentMinute(int currentMinute)` — Mutates internal state for this object.
- `public int getCurrentHour()` — Returns current state/value.
- `public void setCurrentHour(int currentHour)` — Mutates internal state for this object.
- `public int getCurrentDayOfWeek()` — Returns current state/value.
- `public void setCurrentDayOfWeek(int currentDayOfWeek)` — Mutates internal state for this object.
- `public int getCurrentDayOfMonth()` — Returns current state/value.
- `public void setCurrentDayOfMonth(int currentDayOfMonth)` — Mutates internal state for this object.
- `public int getCurrentMonth()` — Returns current state/value.
- `public void setCurrentMonth(int currentMonth)` — Mutates internal state for this object.
- `public int getCurrentYear()` — Returns current state/value.
- `public void setCurrentYear(int currentYear)` — Mutates internal state for this object.
- `public int getCurrentAge()` — Returns current state/value.
- `public void setCurrentAge(int currentAge)` — Mutates internal state for this object.
- `public float getRandomNoiseFromDay()` — Returns current state/value.
- `public void setRandomNoiseFromDay(float randomNoiseFromDay)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/ClockManager.java`

**Type:** `class ClockManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 143 lines

**What this class does:** `ClockManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.bootstrap.calendarpipeline.calendarmanager.CalendarManager`
- `program.bootstrap.calendarpipeline.clock.ClockData`
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldmanager.WorldManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void update()` — Runs frame-step maintenance and logic.
- `private void validateSettings()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void wireData(WorldHandle activeWorld)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void advanceGameClock()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void switchWorld(WorldHandle newWorld)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ClockHandle getClockHandle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/CurrentTrackerBranch.java`

**Type:** `class CurrentTrackerBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 157 lines

**What this class does:** `CurrentTrackerBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void assignData(ClockHandle clockHandle, float daysPerDay)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void setDaysPerDay(float daysPerDay)` — Mutates internal state for this object.
- `package boolean advanceTime()` — Performs class-specific logic; see call sites and owning manager flow.
- `package double calculateRawTimeOfDay(double dayProgress)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int calculateMinute(double rawTimeOfDay)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int calculateHour(double rawTimeOfDay)` — Performs class-specific logic; see call sites and owning manager flow.
- `package double calculateVisualTimeOfDay(double rawTimeOfDay, double yearProgress)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/DayTrackerBranch.java`

**Type:** `class DayTrackerBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 197 lines

**What this class does:** `DayTrackerBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void buildDayConversionTables()` — Constructs derived runtime/handle data from source input.
- `package boolean advanceTime()` — Performs class-specific logic; see call sites and owning manager flow.
- `package long calculateTotalDaysWithOffset(long totalDaysElapsed)` — Performs class-specific logic; see call sites and owning manager flow.
- `package float calculateRandomNoise(long totalDaysWithOffset)` — Performs class-specific logic; see call sites and owning manager flow.
- `package double calculateYearProgress(long totalDaysWithOffset)` — Performs class-specific logic; see call sites and owning manager flow.
- `package double calculateVisualYearProgress(double yearProgress)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int calculateDayOfWeek(long totalDaysWithOffset)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int getDayOfYearFromDayAndMonth(int dayOfMonth, int month)` — Returns current state/value.
- `package int getDayOfMonthFromDayOfYear(int dayOfYear)` — Returns current state/value.
- `package int getMonthFromDayOfYear(int dayOfYear)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/InternalBufferSystem.java`

**Type:** `class InternalBufferBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 71 lines

**What this class does:** `InternalBufferBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void assignData(ClockHandle clockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushData(float deltaTime)` — Queues data for downstream systems (often render queues).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/MonthTrackerBranch.java`

**Type:** `class MonthTrackerBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 48 lines

**What this class does:** `MonthTrackerBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void assignData(ClockHandle clockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean advanceTime()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/calendarpipeline/clockmanager/YearTrackerBranch.java`

**Type:** `class YearTrackerBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.calendarpipeline.clockmanager`
  
**File size:** 70 lines

**What this class does:** `YearTrackerBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.calendarpipeline.clockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.calendarpipeline.calendar.CalendarHandle`
- `program.bootstrap.calendarpipeline.clock.ClockHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void assignData(CalendarHandle calendarHandle, ClockHandle clockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean advanceTime()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/EntityPipeline.java`

**Type:** `class EntityPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.entitypipeline`
  
**File size:** 22 lines

**What this class does:** `EntityPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behaviormanager.BehaviorManager`
- `program.bootstrap.entitypipeline.entitymanager.EntityManager`
- `program.bootstrap.entitypipeline.playermanager.PlayerManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behavior/BehaviorData.java`

**Type:** `class BehaviorData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.entitypipeline.behavior`
  
**File size:** 48 lines

**What this class does:** `BehaviorData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behavior`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public BehaviorData(String behaviorName, short behaviorID, float jumpDuration)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBehaviorName()` — Returns current state/value.
- `public short getBehaviorID()` — Returns current state/value.
- `public float getJumpDuration()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behavior/BehaviorHandle.java`

**Type:** `class BehaviorHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.behavior`
  
**File size:** 40 lines

**What this class does:** `BehaviorHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behavior`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(BehaviorData behaviorData)` — Engine-side initialization entrypoint invoked post-create.
- `public BehaviorData getBehaviorData()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public short getBehaviorID()` — Returns current state/value.
- `public float getJumpDuration()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/BehaviorManager.java`

**Type:** `class BehaviorManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 72 lines

**What this class does:** `BehaviorManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBehavior(BehaviorHandle handle)` — Registers a child object into manager-owned collections.
- `public boolean hasBehavior(String behaviorName)` — Boolean existence/availability check.
- `public short getBehaviorIDFromBehaviorName(String behaviorName)` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandleFromBehaviorID(short behaviorID)` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandleFromBehaviorName(String behaviorName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 37 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorData`
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `package BehaviorHandle build(File file, String behaviorName)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/behaviormanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.entitypipeline.behaviormanager`
  
**File size:** 90 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.behaviormanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String behaviorName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityData.java`

**Type:** `class EntityData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 89 lines

**What this class does:** `EntityData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public EntityData(Vector3 sizeMin, Vector3 sizeMax, float weightMin, float weightMax, float eyeLevel, String behaviorName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 getSizeMin()` — Returns current state/value.
- `public Vector3 getSizeMax()` — Returns current state/value.
- `public float getWeightMin()` — Returns current state/value.
- `public float getWeightMax()` — Returns current state/value.
- `public float getEyeLevel()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public Vector3 getRandomSize()` — Returns current state/value.
- `public float getRandomWeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityHandle.java`

**Type:** `class EntityHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 54 lines

**What this class does:** `EntityHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public void constructor(EntityData entityData)` — Engine-side initialization entrypoint invoked post-create.
- `public EntityData getEntityData()` — Returns current state/value.
- `public float getWeightMin()` — Returns current state/value.
- `public float getWeightMax()` — Returns current state/value.
- `public float getEyeLevel()` — Returns current state/value.
- `public String getBehaviorName()` — Returns current state/value.
- `public Vector3 getRandomSize()` — Returns current state/value.
- `public float getRandomWeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`

**Type:** `class EntityInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 168 lines

**What this class does:** `EntityInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.inventory.InventoryHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.physicspipeline.util.BlockCompositionStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.InstancePackage`
- `program.core.util.mathematics.vectors.Vector3`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(EntityData entityData, WorldHandle worldHandle, BehaviorHandle behaviorHandle, Vector3 position, long chunkCoordinate, Vector3 size, float weight)` — Engine-side initialization entrypoint invoked post-create.
- `private void setEntitySize(Vector3 size)` — Mutates internal state for this object.
- `public void updateBlockComposition()` — Runs frame-step maintenance and logic.
- `public EntityData getEntityData()` — Returns current state/value.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public BehaviorHandle getBehaviorHandle()` — Returns current state/value.
- `public void setBehaviorHandle(BehaviorHandle behaviorHandle)` — Mutates internal state for this object.
- `public EntityStateHandle getEntityStateHandle()` — Returns current state/value.
- `public StatisticsHandle getStatisticsHandle()` — Returns current state/value.
- `public InventoryHandle getInventoryHandle()` — Returns current state/value.
- `public InputHandle getInputHandle()` — Returns current state/value.
- `public WorldPositionStruct getWorldPositionStruct()` — Returns current state/value.
- `public Vector3Int getBlockComposition()` — Returns current state/value.
- `public BlockCompositionStruct getBlockCompositionStruct()` — Returns current state/value.
- `public Vector3 getSize()` — Returns current state/value.
- `public void setSize(Vector3 size)` — Mutates internal state for this object.
- `public float getWeight()` — Returns current state/value.
- `public float getEyeHeight()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityState.java`

**Type:** `enum EntityState`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 16 lines

**What this class does:** `EntityState` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entity/EntityStateHandle.java`

**Type:** `class EntityStateHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.entity`
  
**File size:** 69 lines

**What this class does:** `EntityStateHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entity`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public EntityState getMovementState()` — Returns current state/value.
- `public void setMovementState(EntityState movementState)` — Mutates internal state for this object.
- `public Vector3 getGravityVelocity()` — Returns current state/value.
- `public Vector2 getHorizontalVelocity()` — Returns current state/value.
- `public long getJumpStartTime()` — Returns current state/value.
- `public void setJumpStartTime(long jumpStartTime)` — Mutates internal state for this object.
- `public boolean isGrounded()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/EntityManager.java`

**Type:** `class EntityManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 114 lines

**What this class does:** `EntityManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.behaviormanager.BehaviorManager`
- `program.bootstrap.entitypipeline.entity.EntityData`
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionUtility`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldmanager.WorldManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addEntityTemplate(String templateName, EntityHandle entityHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasTemplate(String templateName)` — Boolean existence/availability check.
- `public int getTemplateIDFromTemplateName(String templateName)` — Returns current state/value.
- `public EntityHandle getEntityHandleFromTemplateID(int templateID)` — Returns current state/value.
- `public EntityHandle getEntityHandleFromTemplateName(String templateName)` — Returns current state/value.
- `public EntityInstance spawnEntity(EntityHandle entityHandle)` — Creates runtime entity/camera/player state for active context. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`.
- `public EntityInstance spawnEntity(String templateName)` — Creates runtime entity/camera/player state for active context. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entity/EntityInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 101 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityData`
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package EntityHandle build(File file)` — Constructs derived runtime/handle data from source input.
- `private Vector3 parseSizeMin(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector3 parseSizeMax(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseWeightMin(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseWeightMax(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float parseEyeLevel(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseBehaviorName(JsonObject json, File file)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/entitymanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.entitypipeline.entitymanager`
  
**File size:** 90 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.entitymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String templateName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/inventory/InventoryHandle.java`

**Type:** `class InventoryHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.inventory`
  
**File size:** 59 lines

**What this class does:** `InventoryHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.inventory`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.backpack.BackpackInstance`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public ItemDefinitionHandle getMainHand()` — Returns current state/value.
- `public void setMainHand(ItemDefinitionHandle mainHand)` — Mutates internal state for this object.
- `public boolean hasMainHand()` — Boolean existence/availability check.
- `public ItemDefinitionHandle getOffHand()` — Returns current state/value.
- `public void setOffHand(ItemDefinitionHandle offHand)` — Mutates internal state for this object.
- `public boolean hasOffHand()` — Boolean existence/availability check.
- `public BackpackInstance getBackpack()` — Returns current state/value.
- `public void setBackpack(BackpackInstance backpack)` — Mutates internal state for this object.
- `public boolean hasBackpack()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/BlockBranch.java`

**Type:** `class BlockBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 234 lines

**What this class does:** `BlockBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package boolean tryBreak(EntityInstance entity, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void resetBreakTarget()` — Performs class-specific logic; see call sites and owning manager flow.
- `private int getBreakTier(EntityInstance entity)` — Returns current state/value.
- `private boolean isCorrectTool(EntityInstance entity, BlockHandle block)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildAffected(ChunkInstance chunk, long chunkCoordinate, int blockX, int blockY, int blockZ, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildSubChunk(ChunkInstance chunk, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void rebuildNeighbour(int chunkX, int chunkZ, int subChunkY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void mergeAndRender(ChunkInstance chunk, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writeBlock(ChunkInstance chunk, int blockX, int blockY, int blockZ, int subChunkY, short blockID)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/ItemBranch.java`

**Type:** `class ItemBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 143 lines

**What this class does:** `ItemBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate4Long`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean place(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveItemOrientation(Direction3Vector hitFace, Vector3 cameraDirection)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/placementmanager/PlacementManager.java`

**Type:** `class PlacementManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.placementmanager`
  
**File size:** 116 lines

**What this class does:** `PlacementManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.placementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void update(EntityInstance entity, Vector3 origin, Vector3 direction, boolean breakAction, boolean placeAction)` — Runs frame-step maintenance and logic.
- `private boolean handleBreakAction(EntityInstance entity, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean handlePlaceAction(EntityInstance entity, Vector3 direction, BlockCastStruct castStruct)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/playermanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.entitypipeline.playermanager`
  
**File size:** 50 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.playermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void updatePlayerPosition(WorldPositionStruct playerPosition)` — Runs frame-step maintenance and logic.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/playermanager/PlayerManager.java`

**Type:** `class PlayerManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.entitypipeline.playermanager`
  
**File size:** 220 lines

**What this class does:** `PlayerManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.playermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.entitymanager.EntityManager`
- `program.bootstrap.entitypipeline.placementmanager.PlacementManager`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.physicspipeline.movementmanager.MovementManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.util.WorldPositionUtility`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public EntityInstance spawnPlayer(WindowInstance window)` — Creates runtime entity/camera/player state for active context.
- `private void calculatePlayerPosition(int windowID, EntityInstance player, CameraInstance camera)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writeMovementState(EntityInstance player)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean verifyPlayerPosition(EntityInstance player, WorldPositionStruct worldPositionStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `public EntityInstance getPlayerForWindow(int windowID)` — Returns current state/value.
- `public boolean hasPlayerForWindow(int windowID)` — Boolean existence/availability check.
- `public CameraInstance getCameraForWindow(int windowID)` — Returns current state/value.
- `public WorldPositionStruct getPlayerPositionForWindow(int windowID)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/entitypipeline/statistics/StatisticsHandle.java`

**Type:** `class StatisticsHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.entitypipeline.statistics`
  
**File size:** 83 lines

**What this class does:** `StatisticsHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.entitypipeline.statistics`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public float getWalkSpeed()` — Returns current state/value.
- `public void setWalkSpeed(float walkSpeed)` — Mutates internal state for this object.
- `public float getMovementSpeed()` — Returns current state/value.
- `public void setMovementSpeed(float movementSpeed)` — Mutates internal state for this object.
- `public float getSprintSpeed()` — Returns current state/value.
- `public void setSprintSpeed(float sprintSpeed)` — Mutates internal state for this object.
- `public float getJumpHeight()` — Returns current state/value.
- `public void setJumpHeight(float jumpHeight)` — Mutates internal state for this object.
- `public float getReach()` — Returns current state/value.
- `public void setReach(float reach)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/GeometryPipeline.java`

**Type:** `class GeometryPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.geometrypipeline`
  
**File size:** 30 lines

**What this class does:** `GeometryPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffer/CompositeBufferData.java`

**Type:** `class CompositeBufferData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffer`
  
**File size:** 161 lines

**What this class does:** `CompositeBufferData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffer`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.DataPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package public CompositeBufferData(MeshHandle meshHandle, int[] instanceAttrSizes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getCompositeVAO()` — Returns current state/value.
- `public void setCompositeVAO(int compositeVAO)` — Mutates internal state for this object.
- `public int getInstanceVBO()` — Returns current state/value.
- `public void setInstanceVBO(int instanceVBO)` — Mutates internal state for this object.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public void setMeshHandle(MeshHandle meshHandle)` — Mutates internal state for this object.
- `public int[] getInstanceAttrSizes()` — Returns current state/value.
- `public void setInstanceAttrSizes(int[] instanceAttrSizes)` — Mutates internal state for this object.
- `public int getFloatsPerInstance()` — Returns current state/value.
- `public void setFloatsPerInstance(int floatsPerInstance)` — Mutates internal state for this object.
- `public int getIndexCount()` — Returns current state/value.
- `public void setIndexCount(int indexCount)` — Mutates internal state for this object.
- `public float[] getInstanceData()` — Returns current state/value.
- `public void setInstanceData(float[] instanceData)` — Mutates internal state for this object.
- `public int getInstanceCount()` — Returns current state/value.
- `public void setInstanceCount(int instanceCount)` — Mutates internal state for this object.
- `public int getMaxInstances()` — Returns current state/value.
- `public void setMaxInstances(int maxInstances)` — Mutates internal state for this object.
- `public int getCpuVersion()` — Returns current state/value.
- `public void setCpuVersion(int cpuVersion)` — Mutates internal state for this object.
- `public int getUploadedVersion()` — Returns current state/value.
- `public void setUploadedVersion(int uploadedVersion)` — Mutates internal state for this object.
- `public boolean isNeedsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setNeedsGpuRealloc(boolean needsGpuRealloc)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffer/CompositeBufferInstance.java`

**Type:** `class CompositeBufferInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffer`
  
**File size:** 159 lines

**What this class does:** `CompositeBufferInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffer`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(CompositeBufferData compositeBufferData)` — Engine-side initialization entrypoint invoked post-create.
- `public CompositeBufferData getCompositeBufferData()` — Returns current state/value.
- `public int getCompositeVAO()` — Returns current state/value.
- `public void setCompositeVAO(int vao)` — Mutates internal state for this object.
- `public int getInstanceVBO()` — Returns current state/value.
- `public void setInstanceVBO(int vbo)` — Mutates internal state for this object.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int[] getInstanceAttrSizes()` — Returns current state/value.
- `public int getFloatsPerInstance()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.
- `public float[] getInstanceData()` — Returns current state/value.
- `public int getInstanceCount()` — Returns current state/value.
- `public int getMaxInstances()` — Returns current state/value.
- `public boolean needsUpload()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean needsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int addInstance(float[] data)` — Registers a child object into manager-owned collections.
- `public void updateInstance(int index, float[] data)` — Runs frame-step maintenance and logic.
- `public int removeInstance(int index)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void grow()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void markUploaded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clearNeedsGpuRealloc()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`

**Type:** `class CompositeBufferManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffermanager`
  
**File size:** 60 lines

**What this class does:** `CompositeBufferManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferData`
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `public void constructor(CompositeBufferInstance buffer, MeshHandle meshHandle, int[] instanceAttrSizes)` — Engine-side initialization entrypoint invoked post-create.
- `public void grow(CompositeBufferInstance buffer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dispose(CompositeBufferInstance buffer)` — Releases owned resources and unregisters state.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.compositebuffermanager`
  
**File size:** 106 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.compositebuffermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`

**Method intent:**
- `package int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createInstancedVAO(int meshVBOHandle, int[] meshAttrSizes, int meshIBOHandle, int instanceVBOHandle, int[] instanceAttrSizes)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteBuffer(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteVAO(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/ComplexGeometryBranch.java`

**Type:** `class ComplexGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `ComplexGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/DynamicGeometryManager.java`

**Type:** `class DynamicGeometryManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 86 lines

**What this class does:** `DynamicGeometryManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public boolean build(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance)` — Constructs derived runtime/handle data from source input.
- `public boolean buildSubChunk(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance, int subChunkIndex)` — Constructs derived runtime/handle data from source input.
- `public void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `public DynamicGeometryAsyncContainer getDynamicGeometryAsyncInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/DynamicGeometryType.java`

**Type:** `enum DynamicGeometryType`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 15 lines

**What this class does:** `DynamicGeometryType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FontGeometryBranch.java`

**Type:** `class FontGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 67 lines

**What this class does:** `FontGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`

**Type:** `class FullGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 619 lines

**What this class does:** `FullGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.block.BlockRotationType`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkNeighborStruct`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction2Vector`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean blockHasFace(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private SubChunkInstance getComparativeSubChunkInstance(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, Direction3Vector direction3Vector)` — Returns current state/value.
- `private boolean compareNeighbor(BlockHandle blockHandleA, BlockHandle blockHandleB)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean assembleQuad(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean tryExpand(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, int xyz, Direction3Vector direction3Vector, Direction3Vector expandDirection, Direction3Vector tangentDirection, int currentSize, int tangentSize, BiomeHandle biomeHandle, BlockHandle blockHandle, short baseOrientation, BitSet accumulatedBatch, BitSet batchReturn)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean compareNext(BiomeHandle biomeHandleA, BiomeHandle biomeHandleB, BlockHandle blockHandleA, BlockHandle blockHandleB, short orientationA, short orientationB)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean prepareFace(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, byte sizeA, byte sizeB, Direction3Vector direction3Vector, Direction3Vector tangentDirectionA, Direction3Vector tangentDirectionB, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveOrientation(BlockPaletteHandle rotationPaletteHandle, int xyz)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveTextureID(BlockHandle blockHandle, Direction3Vector worldFace, int orientation)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveEncodedFace(BlockHandle blockHandle, Direction3Vector worldFace, int orientation)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float getVertColor(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int vertXYZ, Color[] vertColors)` — Returns current state/value.
- `private SubChunkInstance getComparativeSubChunkInstance(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, int xyz, VertBlockNeighbor3Vector vertBlockNeighbor3Vector)` — Returns current state/value.
- `private float blendColors(Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean finalizeFace(Int2ObjectOpenHashMap<FloatArrayList> verts, DynamicPacketInstance dynamicPacketInstance, Direction3Vector direction3Vector, int materialId, TextureHandle textureHandle, int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ, float vert0Color, float vert1Color, float vert2Color, float vert3Color, int encodedFace)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/InternalBuildManager.java`

**Type:** `class InternalBuildManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 237 lines

**What this class does:** `InternalBuildManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.ManagerPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean build(DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer, ChunkInstance chunkInstance, SubChunkInstance subChunkInstance)` — Constructs derived runtime/handle data from source input.
- `private boolean assembleQuads(DynamicGeometryType geometry, ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void buildGlyphModel(DynamicModelHandle model, GlyphMetricStruct glyph, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/LiquidGeometryBranch.java`

**Type:** `class LiquidGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `LiquidGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/PartialGeometryBranch.java`

**Type:** `class PartialGeometryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager`
  
**File size:** 42 lines

**What this class does:** `PartialGeometryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package boolean assembleQuads(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance, BlockPaletteHandle biomePaletteHandle, BlockPaletteHandle blockPaletteHandle, BlockPaletteHandle rotationPaletteHandle, DynamicPacketInstance dynamicPacketInstance, int xyz, Direction3Vector direction3Vector, BiomeHandle biomeHandle, BlockHandle blockHandle, Int2ObjectOpenHashMap<FloatArrayList> verts, BitSet accumulatedBatch, BitSet batchReturn, Color[] vertColors)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/util/DynamicGeometryAsyncContainer.java`

**Type:** `class DynamicGeometryAsyncContainer`
  
**Inheritance/implements:** `extends AsyncContainerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`
  
**File size:** 69 lines

**What this class does:** `DynamicGeometryAsyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.AsyncContainerPackage`
- `program.core.util.mathematics.extras.Color`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Int2ObjectOpenHashMap<FloatArrayList> getVerts()` — Returns current state/value.
- `public BitSet[] getDirectionalBatches()` — Returns current state/value.
- `public BitSet getBatchReturn()` — Returns current state/value.
- `public Color[] getVertColors()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/util/VertBlockNeighbor3Vector.java`

**Type:** `enum VertBlockNeighbor3Vector`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`
  
**File size:** 71 lines

**What this class does:** `VertBlockNeighbor3Vector` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util`.

**Who this class talks to (direct imports):**
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Coordinate3Long`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `package  VertBlockNeighbor3Vector(int x, int y, int z)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Direction2Vector to2D()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicmodel/DynamicModelHandle.java`

**Type:** `class DynamicModelHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicmodel`
  
**File size:** 167 lines

**What this class does:** `DynamicModelHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicmodel`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public void constructor(int materialID, VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public int tryAddVertices(FloatArrayList sourceVerts, int offset, int length)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addQuadVertices(FloatArrayList sourceVerts)` — Registers a child object into manager-owned collections.
- `public void mergeWithOffset(DynamicModelHandle source, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void appendQuadIndices(int baseVertex, int quadCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getMaterialID()` — Returns current state/value.
- `public VAOHandle getVAOHandle()` — Returns current state/value.
- `public FloatArrayList getVertices()` — Returns current state/value.
- `public ShortArrayList getIndices()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getVertexCount()` — Returns current state/value.
- `public boolean isFull()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicpacket/DynamicPacketInstance.java`

**Type:** `class DynamicPacketInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicpacket`
  
**File size:** 193 lines

**What this class does:** `DynamicPacketInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicpacket`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public boolean tryLock()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setReady()` — Mutates internal state for this object.
- `public void unlock()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean addVertices(int materialId, FloatArrayList vertList)` — Registers a child object into manager-owned collections.
- `public boolean merge(DynamicPacketInstance other, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `private FloatArrayList applyOffset(FloatArrayList vertices, int[] offsetIndices, float[] offsets)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public DynamicPacketState getState()` — Returns current state/value.
- `public boolean hasModels()` — Boolean existence/availability check.
- `public Int2ObjectOpenHashMap<ObjectArrayList<DynamicModelHandle>> getMaterialID2ModelCollection()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/dynamicpacket/DynamicPacketState.java`

**Type:** `enum DynamicPacketState`
  
**Package:** `program.bootstrap.geometrypipeline.dynamicpacket`
  
**File size:** 14 lines

**What this class does:** `DynamicPacketState` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.dynamicpacket`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOData.java`

**Type:** `class IBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 35 lines

**What this class does:** `IBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public IBOData(int indexHandle, int indexCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOHandle.java`

**Type:** `class IBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 29 lines

**What this class does:** `IBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(IBOData iboData)` — Engine-side initialization entrypoint invoked post-create.
- `public IBOData getIBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibo/IBOInstance.java`

**Type:** `class IBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibo`
  
**File size:** 29 lines

**What this class does:** `IBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibo`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(IBOData iboData)` — Engine-side initialization entrypoint invoked post-create.
- `public IBOData getIBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 74 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`

**Method intent:**
- `package IBOHandle uploadIndexData(VAOInstance vaoInstance, IBOHandle iboHandle, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`, `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`.
- `package IBOInstance uploadIndexData(VAOInstance vaoInstance, IBOInstance iboInstance, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`, `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`.
- `private IBOData upload(VAOInstance vaoInstance, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void removeIndexData(IBOData iboData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/IBOManager.java`

**Type:** `class IBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 127 lines

**What this class does:** `IBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerIBO(String resourceName, IBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public IBOHandle addIBOFromData(String resourceName, short[] indices, VAOInstance vaoInstance)` — Registers a child object into manager-owned collections.
- `public boolean hasIBO(String iboName)` — Boolean existence/availability check.
- `public short getIBOIDFromIBOName(String iboName)` — Returns current state/value.
- `public IBOHandle getIBOHandleFromIBOID(short iboID)` — Returns current state/value.
- `public IBOHandle getIBOHandleFromIBOName(String iboName)` — Returns current state/value.
- `public IBOHandle getIBOHandleDirect(String iboName)` — Returns current state/value.
- `public IBOInstance createIBOInstance(VAOInstance vaoInstance, ShortArrayList indices)` — Allocates/initializes child systems or resources.
- `public void removeIBO(IBOData iboData)` — Unregisters and tears down child references.
- `public void removeIBO(IBOHandle iboHandle)` — Unregisters and tears down child references.
- `public void removeIBOInstance(IBOInstance iboInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/ibo/IBOInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/ibomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.ibomanager`
  
**File size:** 149 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.ibomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry, VAOInstance vaoInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private IBOHandle buildFromData(JsonArray indicesArray, VAOInstance vaoInstance, File file)` — Constructs derived runtime/handle data from source input.
- `private boolean hasQuadEntries(JsonObject json)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshData.java`

**Type:** `class MeshData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 68 lines

**What this class does:** `MeshData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOData`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MeshData(VAOData vaoData, VBOData vboData, IBOData iboData)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VAOData getVAOData()` — Returns current state/value.
- `public VBOData getVBOData()` — Returns current state/value.
- `public IBOData getIBOData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getVertexCount()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshHandle.java`

**Type:** `class MeshHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 76 lines

**What this class does:** `MeshHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(VAOInstance vaoInstance, VBOHandle vboHandle, IBOHandle iboHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOInstance getVAOInstance()` — Returns current state/value.
- `public VBOHandle getVBOHandle()` — Returns current state/value.
- `public IBOHandle getIBOHandle()` — Returns current state/value.
- `public MeshData getMeshData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`

**Type:** `class MeshInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.mesh`
  
**File size:** 75 lines

**What this class does:** `MeshInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.mesh`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOInstance vaoInstance, VBOInstance vboInstance, IBOInstance iboInstance)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOInstance getVAOInstance()` — Returns current state/value.
- `public VBOInstance getVBOInstance()` — Returns current state/value.
- `public IBOInstance getIBOInstance()` — Returns current state/value.
- `public MeshData getMeshData()` — Returns current state/value.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getIndexHandle()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 291 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOHandle`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package MeshHandle buildMeshHandle(File root, File file, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private boolean hasQuadEntries(JsonObject json)` — Boolean existence/availability check.
- `private QuadExpansionStruct expandVBO(JsonObject json, VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void expandQuad(JsonObject quadObj, FloatArrayList vertices, ShortArrayList quadIndices, int baseVertex, int vertStride, VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void validateVAOUVCompatibility(VAOInstance vaoInstance, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float[][] resolveLocalUVs(JsonObject quadObj, File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float snapUV(float local, float tileMin, float tileMax, int tilePixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean hasValidElement(JsonObject json, String key)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 125 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String resourceName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/MeshManager.java`

**Type:** `class MeshManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 128 lines

**What this class does:** `MeshManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.ibo.IBOInstance`
- `program.bootstrap.geometrypipeline.ibomanager.IBOManager`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.mesh.MeshInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.bootstrap.geometrypipeline.vbomanager.VBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addMeshHandle(String meshName, MeshHandle meshHandle)` — Registers a child object into manager-owned collections.
- `public void request(String resourceName)` — Triggers on-demand loading or lookup.
- `public boolean hasMesh(String meshName)` — Boolean existence/availability check.
- `public int getMeshIDFromMeshName(String meshName)` — Returns current state/value.
- `public MeshHandle getMeshHandleFromMeshID(int meshID)` — Returns current state/value.
- `public MeshHandle getMeshHandleFromMeshName(String meshName)` — Returns current state/value.
- `public MeshInstance createMesh(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices)` — Allocates/initializes child systems or resources.
- `public void removeMesh(MeshData meshData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.
- `public void removeMesh(MeshHandle meshHandle)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.
- `public void removeMesh(MeshInstance meshInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/mesh/MeshInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/meshmanager/QuadExpansionStruct.java`

**Type:** `class QuadExpansionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.geometrypipeline.meshmanager`
  
**File size:** 23 lines

**What this class does:** `QuadExpansionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.meshmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package  QuadExpansionStruct(float[] vertices, short[] indices)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/model/ModelInstance.java`

**Type:** `class ModelInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.model`
  
**File size:** 61 lines

**What this class does:** `ModelInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.model`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MeshData meshData, MaterialInstance material)` — Engine-side initialization entrypoint invoked post-create.
- `public MeshData getMeshData()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public int getVAO()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int getVBO()` — Returns current state/value.
- `public int getVertCount()` — Returns current state/value.
- `public int getIBO()` — Returns current state/value.
- `public int getIndexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/modelmanager/ModelManager.java`

**Type:** `class ModelManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.modelmanager`
  
**File size:** 111 lines

**What this class does:** `ModelManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.modelmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.mesh.MeshInstance`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public ModelInstance createModel(MeshData meshData, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshData meshData, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshHandle meshHandle, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshHandle meshHandle, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshInstance meshInstance, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(MeshInstance meshInstance, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(ModelInstance modelInstance, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(ModelInstance modelInstance, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices, int materialID)` — Allocates/initializes child systems or resources.
- `public ModelInstance createModel(VAOHandle vaoTemplate, FloatArrayList vertices, ShortArrayList indices, MaterialInstance material)` — Allocates/initializes child systems or resources.
- `private ModelInstance buildModel(MeshData meshData, MaterialInstance material)` — Constructs derived runtime/handle data from source input.
- `public void removeMesh(MeshInstance meshInstance)` — Unregisters and tears down child references.
- `public void removeMesh(ModelInstance modelInstance)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOData.java`

**Type:** `class VAOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 46 lines

**What this class does:** `VAOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public VAOData(int attributeHandle, int[] attrSizes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getAttributeHandle()` — Returns current state/value.
- `public int getVertStride()` — Returns current state/value.
- `public int[] getAttrSizes()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOHandle.java`

**Type:** `class VAOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 29 lines

**What this class does:** `VAOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(int[] attrSizes)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOData getVAOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vao/VAOInstance.java`

**Type:** `class VAOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vao`
  
**File size:** 29 lines

**What this class does:** `VAOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vao`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VAOData vaoData)` — Engine-side initialization entrypoint invoked post-create.
- `public VAOData getVAOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 132 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`

**Method intent:**
- `package VAOInstance createVAOInstance(VAOInstance vaoInstance, VAOHandle template)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package int cloneVAO(int[] attrSizes, int vertexHandle, int indexHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `private VAOData createData(int[] attrSizes)` — Allocates/initializes child systems or resources.
- `package void removeVAOData(VAOData vaoData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package void removeVAOInstance(VAOInstance vaoInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.
- `package void removeVAOHandle(int vao)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 112 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, File sourceFile, Map<String, File> registry)` — Performs class-specific logic; see call sites and owning manager flow.
- `private VAOHandle buildLayout(JsonArray jsonArray, File file)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vaomanager/VAOManager.java`

**Type:** `class VAOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vaomanager`
  
**File size:** 180 lines

**What this class does:** `VAOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vaomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerVAO(String resourceName, VAOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean hasVAO(String vaoName)` — Boolean existence/availability check.
- `public short getVAOIDFromVAOName(String vaoName)` — Returns current state/value.
- `public VAOHandle getVAOHandleFromVAOID(short vaoID)` — Returns current state/value.
- `public VAOHandle getVAOHandleFromVAOName(String vaoName)` — Returns current state/value.
- `public VAOHandle getVAOHandleDirect(String vaoName)` — Returns current state/value.
- `public VAOInstance createVAOInstance(VAOHandle template)` — Allocates/initializes child systems or resources.
- `public int getVAOForWindow(MeshData meshData, int windowID)` — Returns current state/value.
- `public void removeWindowVAOs(int windowID)` — Unregisters and tears down child references.
- `public void removeSourceVAOClones(int sourceVAO)` — Unregisters and tears down child references.
- `public void removeVAOData(VAOData vaoData)` — Unregisters and tears down child references.
- `public void removeVAOInstance(VAOInstance vaoInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vao/VAOInstance.java`.
- `private long composeWindowKey(int sourceVAO, int windowID)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int extractWindowID(long key)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int extractSourceVAO(long key)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOData.java`

**Type:** `class VBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 35 lines

**What this class does:** `VBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public VBOData(int vertexHandle, int vertexCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getVertexHandle()` — Returns current state/value.
- `public int getVertexCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOHandle.java`

**Type:** `class VBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 29 lines

**What this class does:** `VBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(VBOData vboData)` — Engine-side initialization entrypoint invoked post-create.
- `public VBOData getVBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbo/VBOInstance.java`

**Type:** `class VBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbo`
  
**File size:** 29 lines

**What this class does:** `VBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbo`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(VBOData vboData)` — Engine-side initialization entrypoint invoked post-create.
- `public VBOData getVBOData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 88 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.geometrypipeline.vao.VAOData`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`

**Method intent:**
- `package VBOHandle uploadVertexData(VAOInstance vaoInstance, VBOHandle vboHandle, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`, `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.
- `package VBOInstance uploadVertexData(VAOInstance vaoInstance, VBOInstance vboInstance, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`, `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.
- `private VBOData upload(VAOInstance vaoInstance, float[] vertices)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void removeVertexData(VBOData vboData)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 140 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void build(String resourceName, File file, Map<String, File> registry, VAOInstance vaoInstance)` — Constructs derived runtime/handle data from source input.
- `private void resolveRef(String refName, String sourceResourceName, File sourceFile, Map<String, File> registry, VAOInstance vaoInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private VBOHandle buildFromData(JsonArray verticesArray, VAOInstance vaoInstance, File file)` — Constructs derived runtime/handle data from source input.
- `private boolean containsQuadObjects(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/geometrypipeline/vbomanager/VBOManager.java`

**Type:** `class VBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.geometrypipeline.vbomanager`
  
**File size:** 127 lines

**What this class does:** `VBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.geometrypipeline.vbomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.vao.VAOInstance`
- `program.bootstrap.geometrypipeline.vbo.VBOData`
- `program.bootstrap.geometrypipeline.vbo.VBOHandle`
- `program.bootstrap.geometrypipeline.vbo.VBOInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void registerVBO(String resourceName, VBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VBOHandle addVBOFromData(String resourceName, float[] vertices, VAOInstance vaoInstance)` — Registers a child object into manager-owned collections.
- `public boolean hasVBO(String vboName)` — Boolean existence/availability check.
- `public short getVBOIDFromVBOName(String vboName)` — Returns current state/value.
- `public VBOHandle getVBOHandleFromVBOID(short vboID)` — Returns current state/value.
- `public VBOHandle getVBOHandleFromVBOName(String vboName)` — Returns current state/value.
- `public VBOHandle getVBOHandleDirect(String vboName)` — Returns current state/value.
- `public VBOInstance createVBOInstance(VAOInstance vaoInstance, FloatArrayList vertices)` — Allocates/initializes child systems or resources.
- `public void removeVBO(VBOData vboData)` — Unregisters and tears down child references.
- `public void removeVBO(VBOHandle vboHandle)` — Unregisters and tears down child references.
- `public void removeVBOInstance(VBOInstance vboInstance)` — Unregisters and tears down child references. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/vbo/VBOInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

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

## `core/src/program/bootstrap/itempipeline/ItemPipeline.java`

**Type:** `class ItemPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.itempipeline`
  
**File size:** 22 lines

**What this class does:** `ItemPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager`
- `program.bootstrap.itempipeline.itemrotationmanager.ItemRotationManager`
- `program.bootstrap.itempipeline.tooltypemanager.ToolTypeManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/backpack/BackpackInstance.java`

**Type:** `class BackpackInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.itempipeline.backpack`
  
**File size:** 48 lines

**What this class does:** `BackpackInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.backpack`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void addItem(ItemDefinitionHandle item)` — Registers a child object into manager-owned collections.
- `public void removeItem(ItemDefinitionHandle item)` — Unregisters and tears down child references.
- `public ObjectArrayList<ItemDefinitionHandle> getItems()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinition/ItemDefinitionData.java`

**Type:** `class ItemDefinitionData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinition`
  
**File size:** 81 lines

**What this class does:** `ItemDefinitionData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinition`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ItemDefinitionData(String itemName, int itemID, float weight, boolean twoHanded, boolean isBackpack, MeshHandle meshHandle, int materialID)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getItemName()` — Returns current state/value.
- `public int getItemID()` — Returns current state/value.
- `public float getWeight()` — Returns current state/value.
- `public boolean isTwoHanded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isBackpack()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinition/ItemDefinitionHandle.java`

**Type:** `class ItemDefinitionHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinition`
  
**File size:** 66 lines

**What this class does:** `ItemDefinitionHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinition`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ItemDefinitionData itemDefinitionData)` — Engine-side initialization entrypoint invoked post-create.
- `public ItemDefinitionData getItemDefinitionData()` — Returns current state/value.
- `public String getItemName()` — Returns current state/value.
- `public int getItemID()` — Returns current state/value.
- `public short getNameShort()` — Returns current state/value.
- `public short getEnchantShort()` — Returns current state/value.
- `public float getWeight()` — Returns current state/value.
- `public boolean isTwoHanded()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isBackpack()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 96 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionData`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.itempipeline.util.ItemRegistryUtility`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package ObjectArrayList<ItemDefinitionHandle> build(File jsonFile, File root)` — Constructs derived runtime/handle data from source input.
- `private ItemDefinitionHandle parseItem(JsonObject itemJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 92 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String itemName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/ItemDefinitionManager.java`

**Type:** `class ItemDefinitionManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.itemdefinitionmanager`
  
**File size:** 85 lines

**What this class does:** `ItemDefinitionManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemdefinitionmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addItem(ItemDefinitionHandle item)` — Registers a child object into manager-owned collections.
- `public boolean hasItem(String itemName)` — Boolean existence/availability check.
- `public int getItemIDFromItemName(String itemName)` — Returns current state/value.
- `public ItemDefinitionHandle getItemHandleFromItemID(int itemID)` — Returns current state/value.
- `public ItemDefinitionHandle getItemHandleFromItemName(String itemName)` — Returns current state/value.
- `public void request(String itemName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemrotationmanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.itempipeline.itemrotationmanager`
  
**File size:** 118 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemrotationmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void pushItemRotationData()` — Queues data for downstream systems (often render queues).
- `private Matrix4 buildRotation(Direction3Vector face, int spin)` — Constructs derived runtime/handle data from source input.
- `private Matrix4 faceRotation(Direction3Vector face)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 rotX(float deg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 rotZ(float deg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Matrix4 axisRotation(float ax, float ay, float az, float deg)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/itemrotationmanager/ItemRotationManager.java`

**Type:** `class ItemRotationManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.itemrotationmanager`
  
**File size:** 11 lines

**What this class does:** `ItemRotationManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.itemrotationmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltype/ToolTypeData.java`

**Type:** `class ToolTypeData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltype`
  
**File size:** 48 lines

**What this class does:** `ToolTypeData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltype`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ToolTypeData(String toolTypeName, short toolTypeID, String defaultModelPath)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getToolTypeName()` — Returns current state/value.
- `public short getToolTypeID()` — Returns current state/value.
- `public String getDefaultModelPath()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltype/ToolTypeHandle.java`

**Type:** `class ToolTypeHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.itempipeline.tooltype`
  
**File size:** 40 lines

**What this class does:** `ToolTypeHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltype`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ToolTypeData toolTypeData)` — Engine-side initialization entrypoint invoked post-create.
- `public ToolTypeData getToolTypeData()` — Returns current state/value.
- `public String getToolTypeName()` — Returns current state/value.
- `public short getToolTypeID()` — Returns current state/value.
- `public String getDefaultModelPath()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 57 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeData`
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `package ObjectArrayList<ToolTypeHandle> build(File jsonFile, File root)` — Constructs derived runtime/handle data from source input.
- `private ToolTypeHandle parseTool(JsonObject toolJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 117 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `private void preRegisterToolTypeNames(File file, String resourceName)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String toolTypeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/tooltypemanager/ToolTypeManager.java`

**Type:** `class ToolTypeManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.itempipeline.tooltypemanager`
  
**File size:** 86 lines

**What this class does:** `ToolTypeManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.tooltypemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.tooltype.ToolTypeHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addToolType(ToolTypeHandle tool)` — Registers a child object into manager-owned collections.
- `public boolean hasToolType(String toolTypeName)` — Boolean existence/availability check.
- `public short getToolTypeIDFromToolTypeName(String toolTypeName)` — Returns current state/value.
- `public ToolTypeHandle getToolTypeHandleFromToolTypeID(short toolTypeID)` — Returns current state/value.
- `public ToolTypeHandle getToolTypeHandleFromToolTypeName(String toolTypeName)` — Returns current state/value.
- `public void request(String toolTypeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/itempipeline/util/ItemRegistryUtility.java`

**Type:** `class ItemRegistryUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.itempipeline.util`
  
**File size:** 50 lines

**What this class does:** `ItemRegistryUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.itempipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public int toItemIntID(String name)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/itempipeline/itemdefinitionmanager/InternalBuilder.java`.
- `public boolean isCollision(String incomingName, String existingName, int id)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

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

## `core/src/program/bootstrap/menupipeline/MenuPipeline.java`

**Type:** `class MenuPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.menupipeline`
  
**File size:** 25 lines

**What this class does:** `MenuPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fontmanager.FontManager`
- `program.bootstrap.menupipeline.menueventsmanager.MenuEventsManager`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.bootstrap.menupipeline.raycastsystem.RaycastSystem`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementData.java`

**Type:** `class ElementData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 128 lines

**What this class does:** `ElementData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ElementData(String id, ElementType type, String spriteName, String text, String fontName, float[] color, LayoutStruct layout, boolean mask, StackDirection stackDirection, DimensionValue spacing, TextAlign textAlign)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getId()` — Returns current state/value.
- `public ElementType getType()` — Returns current state/value.
- `public String getSpriteName()` — Returns current state/value.
- `public String getText()` — Returns current state/value.
- `public String getFontName()` — Returns current state/value.
- `public float[] getColor()` — Returns current state/value.
- `public LayoutStruct getLayout()` — Returns current state/value.
- `public boolean isMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `public StackDirection getStackDirection()` — Returns current state/value.
- `public DimensionValue getSpacing()` — Returns current state/value.
- `public TextAlign getTextAlign()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasText()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasColor()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementHandle.java`

**Type:** `class ElementHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 129 lines

**What this class does:** `ElementHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ElementData data, Runnable clickAction, MenuAwareAction menuAwareAction, ObjectArrayList<ElementPlacementStruct> children)` — Engine-side initialization entrypoint invoked post-create.
- `public ElementData getElementData()` — Returns current state/value.
- `public String getId()` — Returns current state/value.
- `public ElementType getType()` — Returns current state/value.
- `public String getSpriteName()` — Returns current state/value.
- `public String getText()` — Returns current state/value.
- `public String getFontName()` — Returns current state/value.
- `public float[] getColor()` — Returns current state/value.
- `public LayoutStruct getLayout()` — Returns current state/value.
- `public boolean isMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `public StackDirection getStackDirection()` — Returns current state/value.
- `public DimensionValue getSpacing()` — Returns current state/value.
- `public TextAlign getTextAlign()` — Returns current state/value.
- `public Runnable getClickAction()` — Returns current state/value.
- `public MenuAwareAction getMenuAwareAction()` — Returns current state/value.
- `public ObjectArrayList<ElementPlacementStruct> getChildren()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasText()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasColor()` — Boolean existence/availability check.
- `public boolean hasClickAction()` — Boolean existence/availability check.
- `public boolean hasMenuAwareAction()` — Boolean existence/availability check.
- `public boolean hasChildren()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementInstance.java`

**Type:** `class ElementInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 297 lines

**What this class does:** `ElementInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.core.engine.InstancePackage`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(ElementData data, SpriteInstance spriteInstance, FontInstance fontInstance, String textOverride, Runnable resolvedAction, LayoutStruct layoutOverride, ObjectArrayList<ElementInstance> children)` — Engine-side initialization entrypoint invoked post-create.
- `public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void computeStackedTransform(float left, float top, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void execute()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addChild(ElementInstance child)` — Registers a child object into manager-owned collections.
- `public void removeChild(ElementInstance child)` — Unregisters and tears down child references.
- `public ElementInstance findChildById(String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setScrollX(float x)` — Mutates internal state for this object.
- `public void setScrollY(float y)` — Mutates internal state for this object.
- `public float getScrollX()` — Returns current state/value.
- `public float getScrollY()` — Returns current state/value.
- `public float getMaxScrollX()` — Returns current state/value.
- `public float getMaxScrollY()` — Returns current state/value.
- `public void setContentW(float w)` — Mutates internal state for this object.
- `public void setContentH(float h)` — Mutates internal state for this object.
- `public float getContentW()` — Returns current state/value.
- `public float getContentH()` — Returns current state/value.
- `public void setPositionOverride(DimensionVector2 pos)` — Mutates internal state for this object.
- `public void clearPositionOverride()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementData getElementData()` — Returns current state/value.
- `public SpriteInstance getSpriteInstance()` — Returns current state/value.
- `public FontInstance getFontInstance()` — Returns current state/value.
- `public ObjectArrayList<ElementInstance> getChildren()` — Returns current state/value.
- `public Matrix4 getTransform()` — Returns current state/value.
- `public float getComputedLeft()` — Returns current state/value.
- `public float getComputedTop()` — Returns current state/value.
- `public float getComputedW()` — Returns current state/value.
- `public float getComputedH()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasChildren()` — Boolean existence/availability check.
- `public String getText()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`

**Type:** `enum ElementOrigin`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 52 lines

**What this class does:** `ElementOrigin` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package  ElementOrigin(float x, float y)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementOrigin fromString(String name)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`, `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public float getX()` — Returns current state/value.
- `public float getY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementPlacementStruct.java`

**Type:** `class ElementPlacementStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 99 lines

**What this class does:** `ElementPlacementStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ElementPlacementStruct(ElementHandle master)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public ElementPlacementStruct(ElementHandle master, String spriteNameOverride, String textOverride, float[] colorOverride, Runnable clickActionOverride, MenuAwareAction menuAwareActionOverride, LayoutStruct layoutOverride)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setMaster(ElementHandle master)` — Mutates internal state for this object.
- `public ElementHandle getMaster()` — Returns current state/value.
- `public String getSpriteNameOverride()` — Returns current state/value.
- `public String getTextOverride()` — Returns current state/value.
- `public float[] getColorOverride()` — Returns current state/value.
- `public Runnable getClickActionOverride()` — Returns current state/value.
- `public MenuAwareAction getMenuAwareActionOverride()` — Returns current state/value.
- `public LayoutStruct getLayoutOverride()` — Returns current state/value.
- `public boolean hasColorOverride()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementType.java`

**Type:** `enum ElementType`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 15 lines

**What this class does:** `ElementType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/FontManager.java`

**Type:** `class FontManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 116 lines

**What this class does:** `FontManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addFont(String fontName, FontHandle fontHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasFont(String fontName)` — Boolean existence/availability check.
- `public int getFontIDFromFontName(String fontName)` — Returns current state/value.
- `public FontHandle getFontHandleFromFontID(int fontID)` — Returns current state/value.
- `public FontHandle getFontHandleFromFontName(String fontName)` — Returns current state/value.
- `public FontInstance cloneFont(String fontName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String fontName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/FontRasterizerUtility.java`

**Type:** `class FontRasterizerUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 75 lines

**What this class does:** `FontRasterizerUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontTileData`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package ObjectArrayList<FontTileData> rasterize(File fontFile, int size, String charset, InternalBuilder builder)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`.
- `private Font loadFont(File fontFile, int size)` — Parses external data into engine objects.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 68 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.image.Pixmap`
- `program.core.engine.UtilityPackage`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushTexture2D(BufferedImage image)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`.
- `package void deleteTexture2D(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/FontManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 158 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.bootstrap.menupipeline.fonts.FontTileData`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.AtlasUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package FontHandle build(String name, File fontFile)` — Constructs derived runtime/handle data from source input.
- `private BufferedImage compositeAtlas(ObjectArrayList<FontTileData> tiles, int atlasPixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Int2ObjectOpenHashMap<GlyphMetricStruct> buildGlyphTable(ObjectArrayList<FontTileData> tiles, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `private Int2ObjectOpenHashMap<DynamicModelHandle> buildGlyphModels(Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs, int materialID, VAOHandle vaoHandle, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `package FontTileData createFontTile()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 91 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String fontName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontHandle.java`

**Type:** `class FontHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 86 lines

**What this class does:** `FontHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(String name, int gpuHandle, int materialID, int atlasPixelSize, Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs, Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels)` — Engine-side initialization entrypoint invoked post-create.
- `public String getName()` — Returns current state/value.
- `public int getGPUHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public boolean hasGlyph(int codepoint)` — Boolean existence/availability check.
- `public GlyphMetricStruct getGlyph(int codepoint)` — Returns current state/value.
- `public DynamicModelHandle getGlyphModel(int codepoint)` — Returns current state/value.
- `public Int2ObjectOpenHashMap<GlyphMetricStruct> getGlyphs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<DynamicModelHandle> getGlyphModels()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontInstance.java`

**Type:** `class FontInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 171 lines

**What this class does:** `FontInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.core.engine.InstancePackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `public void constructor(FontHandle handle, DynamicModelHandle mergedModel)` — Engine-side initialization entrypoint invoked post-create.
- `public void setText(String text)` — Mutates internal state for this object.
- `public void setColor(float r, float g, float b, float a)` — Mutates internal state for this object.
- `public Vector4 getColor()` — Returns current state/value.
- `public void upload(ModelManager modelManager, MaterialManager materialManager)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void release(ModelManager modelManager)` — Performs class-specific logic; see call sites and owning manager flow.
- `public FontHandle getHandle()` — Returns current state/value.
- `public DynamicModelHandle getMergedModel()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public float getTextWidth()` — Returns current state/value.
- `public float getTextHeight()` — Returns current state/value.
- `public boolean hasModel()` — Boolean existence/availability check.
- `public boolean isDirty()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontTileData.java`

**Type:** `class FontTileData`
  
**Inheritance/implements:** `extends AtlasTileData`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 72 lines

**What this class does:** `FontTileData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.core.util.atlas.AtlasTileData`

**Method intent:**
- `public void constructor(int codepoint, BufferedImage image, int bearingX, int bearingY, int advance)` — Engine-side initialization entrypoint invoked post-create.
- `public void clearImage()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getCodepoint()` — Returns current state/value.
- `public BufferedImage getImage()` — Returns current state/value.
- `public int getBearingX()` — Returns current state/value.
- `public int getBearingY()` — Returns current state/value.
- `public int getAdvance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/GlyphMetricStruct.java`

**Type:** `class GlyphMetricStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 49 lines

**What this class does:** `GlyphMetricStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public GlyphMetricStruct(int atlasX, int atlasY, int width, int height, int bearingX, int bearingY, int advance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuData.java`

**Type:** `class MenuData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 55 lines

**What this class does:** `MenuData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MenuData(String name, boolean lockInput, boolean raycastInput, ObjectArrayList<String> entryPoints)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getName()` — Returns current state/value.
- `public boolean isLockInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRaycastInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<String> getEntryPoints()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuHandle.java`

**Type:** `class MenuHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 55 lines

**What this class does:** `MenuHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(MenuData data, ObjectArrayList<ElementPlacementStruct> placements)` — Engine-side initialization entrypoint invoked post-create.
- `public MenuData getMenuData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public boolean isLockInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRaycastInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<String> getEntryPoints()` — Returns current state/value.
- `public ObjectArrayList<ElementPlacementStruct> getPlacements()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuInstance.java`

**Type:** `class MenuInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 117 lines

**What this class does:** `MenuInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.core.engine.InstancePackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `public void constructor(MenuData data, ObjectArrayList<ElementInstance> elements, WindowInstance window)` — Engine-side initialization entrypoint invoked post-create.
- `public ElementInstance getEntryPoint(int index)` — Returns current state/value.
- `public void addToEntryPoint(int index, ElementInstance element)` — Registers a child object into manager-owned collections.
- `public void removeFromEntryPoint(int index, ElementInstance element)` — Unregisters and tears down child references.
- `private ElementInstance findById(ObjectArrayList<ElementInstance> list, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void show()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void hide()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MenuData getMenuData()` — Returns current state/value.
- `public ObjectArrayList<ElementInstance> getElements()` — Returns current state/value.
- `public WindowInstance getWindow()` — Returns current state/value.
- `public int getWindowID()` — Returns current state/value.
- `public boolean isVisible()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/MenuEventsManager.java`

**Type:** `class MenuEventsManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager`
  
**File size:** 26 lines

**What this class does:** `MenuEventsManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch`
- `program.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch`
- `program.bootstrap.menupipeline.menueventsmanager.util.GenericButtonBranch`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/menus/InventoryBranch.java`

**Type:** `class InventoryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.menus`
  
**File size:** 104 lines

**What this class does:** `InventoryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.menus`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.inventory.InventoryHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void openInventory(EntityInstance entity, WindowInstance window)` — Activates UI/window/menu surface.
- `public void closeInventory()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void toggleInventory(EntityInstance entity, WindowInstance window)` — Switches state between active/inactive variants.
- `public boolean isOpen()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void rebuildUI(InventoryHandle inventory)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void injectSlot(String displayName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void selectItem()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/menus/MainMenuBranch.java`

**Type:** `class MainMenuBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.menus`
  
**File size:** 46 lines

**What this class does:** `MainMenuBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.menus`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public MenuInstance openMenu(WindowInstance window)` — Activates UI/window/menu surface.
- `public MenuInstance closeMenu()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/util/GenericButtonBranch.java`

**Type:** `class GenericButtonBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.util`
  
**File size:** 15 lines

**What this class does:** `GenericButtonBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.BranchPackage`

**Method intent:**
- `public void quitGame()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/ElementSystem.java`

**Type:** `class ElementSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 202 lines

**What this class does:** `ElementSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.fontmanager.FontManager`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.core.engine.SystemPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean hasMaster(String key)` — Boolean existence/availability check.
- `package ElementHandle getMaster(String key)` — Returns current state/value.
- `package void registerMaster(String key, ElementHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package Iterable<String> getMasterKeys()` — Returns current state/value.
- `package boolean isFileLoading(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void beginFileLoad(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void endFileLoad(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ObjectArrayList<ElementInstance> createInstances(ObjectArrayList<ElementPlacementStruct> placements, Supplier<MenuInstance> parentRef)` — Allocates/initializes child systems or resources.
- `private ElementInstance createInstance(ElementPlacementStruct placement, Supplier<MenuInstance> parentRef)` — Allocates/initializes child systems or resources.
- `private Runnable resolveAction(ElementHandle master, ElementPlacementStruct placement, Supplier<MenuInstance> parentRef)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ElementInstance createDetachedInstance(ElementPlacementStruct placement)` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`

**Type:** `class FileParserUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 107 lines

**What this class does:** `FileParserUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementOrigin`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.core.engine.UtilityPackage`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package String[] parseOnClick(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ElementType parseElementType(String type, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `package LayoutStruct parseLayout(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package LayoutStruct parseLayoutOverride(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package Vector2 parseOriginField(JsonObject json, String key)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 646 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementOrigin`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.menu.MenuData`
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void init(File root)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ObjectArrayList<MenuHandle> processFile(File file, String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void resolveAllDeferredRefs()` — Performs class-specific logic; see call sites and owning manager flow.
- `private MenuHandle buildMenuHandle(String fileName, String filePath, JsonObject menuJson)` — Constructs derived runtime/handle data from source input.
- `private void registerTopLevelMasters(String filePath, JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ObjectArrayList<ElementPlacementStruct> buildPlacements(String filePath, JsonObject parent)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildPlacement(String filePath, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildInlinePlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildUsePlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildRefPlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementHandle buildMasterFromJson(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementHandle resolveRefKey(String refKey)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ElementHandle resolveTemplate(String usePath, String localId)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File tryResolveFile(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File resolveFile(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String resolveSpriteName(String elementId, String spritePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object resolveClickActionRaw(String actionClass, String actionMethod, String actionArg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object resolveTarget(String className, String methodName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Method resolveMethod(Object target, String className, String methodName, String arg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private LayoutStruct parseLayout(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private LayoutStruct parseLayoutOverride(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector2 parseOriginField(JsonObject json, String key)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ElementType parseElementType(String type, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String[] parseOnClick(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseFontName(JsonObject json, ElementType type, String elementId)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float[] parseColor(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 120 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File file)` — Parses external data into engine objects.
- `protected void onComplete()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void request(String menuName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/MenuManager.java`

**Type:** `class MenuManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 598 lines

**What this class does:** `MenuManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.menu.MenuData`
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `private void updateRaycast()` — Runs frame-step maintenance and logic.
- `private boolean hitTestElements(ObjectArrayList<ElementInstance> elements, float mouseX, float mouseY, float clipLeft, float clipTop, float clipRight, float clipBottom)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isHit(ElementInstance element, float mouseX, float mouseY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderElement(ElementInstance element, float parentLeft, float parentTop, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderStackedElement(ElementInstance element, float left, float top, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderElementContent(ElementInstance element)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderStacked(ElementInstance parent, StackDirection dir)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushSpriteRenderCall(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void pushFontRenderCall(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void pushMask(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void popMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `private MaskStruct currentMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void uploadFontModels(ObjectArrayList<ElementInstance> elements)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void releaseFontModels(ObjectArrayList<ElementInstance> elements)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey, Consumer<ElementInstance> customizer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void eject(MenuInstance menu, int entryPoint, ElementInstance instance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void refreshText(ElementInstance element)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void applyInputLock(int delta)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void applyRaycastLock(int delta)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addMenu(String menuName, MenuHandle menuHandle)` — Registers a child object into manager-owned collections.
- `public boolean isInputLocked()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/runtime/input/PlayerInputSystem.java`.
- `public boolean hasMenu(String menuName)` — Boolean existence/availability check.
- `public int getMenuIDFromMenuName(String menuName)` — Returns current state/value.
- `public MenuHandle getMenuHandleFromMenuID(int menuID)` — Returns current state/value.
- `public MenuHandle getMenuHandleFromMenuName(String menuName)` — Returns current state/value.
- `public MenuInstance openMenu(String menuName, WindowInstance window)` — Activates UI/window/menu surface. Called via static reference from: `core/src/program/bootstrap/menupipeline/menu/MenuInstance.java`.
- `public MenuInstance closeMenu(MenuInstance instance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<MenuInstance> getActiveMenus()` — Returns current state/value.
- `public void request(String menuName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/raycastsystem/RaycastSystem.java`

**Type:** `class RaycastSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.menupipeline.raycastsystem`
  
**File size:** 141 lines

**What this class does:** `RaycastSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.raycastsystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void update(ObjectArrayList<MenuInstance> activeMenus, float screenW, float screenH)` — Runs frame-step maintenance and logic.
- `private boolean hitTestElements(ObjectArrayList<ElementInstance> elements, float mouseX, float mouseY, float clipLeft, float clipTop, float clipRight, float clipBottom)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isHit(ElementInstance element, float mouseX, float mouseY)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setActive(boolean active)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/DimensionValue.java`

**Type:** `class DimensionValue`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 50 lines

**What this class does:** `DimensionValue` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package private DimensionValue(float value, boolean percentage)` — Performs class-specific logic; see call sites and owning manager flow.
- `public DimensionValue parse(String raw)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`, `core/src/program/bootstrap/menupipeline/util/DimensionVector2.java`.
- `public float resolve(float parentDimension)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/DimensionVector2.java`

**Type:** `class DimensionVector2`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 59 lines

**What this class does:** `DimensionVector2` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public DimensionVector2(DimensionValue x, DimensionValue y)` — Performs class-specific logic; see call sites and owning manager flow.
- `public DimensionVector2 parse(JsonObject json, String key, String defaultX, String defaultY)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`, `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public DimensionValue getX()` — Returns current state/value.
- `public DimensionValue getY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/LayoutStruct.java`

**Type:** `class LayoutStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 85 lines

**What this class does:** `LayoutStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public LayoutStruct(Vector2 anchor, Vector2 pivot, DimensionVector2 position, DimensionVector2 size, DimensionVector2 minSize, DimensionVector2 maxSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `public LayoutStruct merge(LayoutStruct base, LayoutStruct override)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public Vector2 getAnchor()` — Returns current state/value.
- `public Vector2 getPivot()` — Returns current state/value.
- `public DimensionVector2 getPosition()` — Returns current state/value.
- `public DimensionVector2 getSize()` — Returns current state/value.
- `public DimensionVector2 getMinSize()` — Returns current state/value.
- `public DimensionVector2 getMaxSize()` — Returns current state/value.
- `public boolean hasMinSize()` — Boolean existence/availability check.
- `public boolean hasMaxSize()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/MenuAwareAction.java`

**Type:** `interface MenuAwareAction`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 14 lines

**What this class does:** `MenuAwareAction` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuInstance`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/StackDirection.java`

**Type:** `enum StackDirection`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 22 lines

**What this class does:** `StackDirection` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Method intent:**
- `public StackDirection fromString(String s)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/TextAlign.java`

**Type:** `enum TextAlign`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 18 lines

**What this class does:** `TextAlign` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Method intent:**
- `public TextAlign fromString(String s)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/PhysicsPipeline.java`

**Type:** `class PhysicsPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.physicspipeline`
  
**File size:** 18 lines

**What this class does:** `PhysicsPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.physicspipeline.movementmanager.MovementManager`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/BlockCollisionBranch.java`

**Type:** `class BlockCollisionBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 144 lines

**What this class does:** `BlockCollisionBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.physicspipeline.util.BlockCompositionStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void calculate(Vector3 position, Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean hasCollisionInDirection(BlockCompositionStruct blockCompositionStruct, Direction3Vector direction, float axisPosition, float axisSize, float axisMovement)` — Boolean existence/availability check.
- `private boolean isColliding(BlockHandle block, Direction3Vector direction, float axisPosition, float axisSize, float axisMovement)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/GravityBranch.java`

**Type:** `class GravityBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 138 lines

**What this class does:** `GravityBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.behavior.BehaviorHandle`
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void calculate(Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void postCollision(Vector3 pre, Vector3 post, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/MovementBranch.java`

**Type:** `class MovementBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 106 lines

**What this class does:** `MovementBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.entity.EntityState`
- `program.bootstrap.entitypipeline.entity.EntityStateHandle`
- `program.bootstrap.entitypipeline.statistics.StatisticsHandle`
- `program.bootstrap.inputpipeline.input.InputHandle`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void calculate(Vector3 movement, EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float selectSpeed(EntityState state, StatisticsHandle stats)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`

**Type:** `class MovementManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.physicspipeline.movementmanager`
  
**File size:** 123 lines

**What this class does:** `MovementManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.movementmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.util.WorldPositionStruct`
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void move(EntityInstance entity)` — Performs class-specific logic; see call sites and owning manager flow.
- `private long updateChunkCoordinateFrom(Vector3 position, int chunkCoordinateX, int chunkCoordinateY)` — Runs frame-step maintenance and logic.
- `private int calculateChunkCoordinateAxisFrom(float axis)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/BlockCastBranch.java`

**Type:** `class BlockCastBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 179 lines

**What this class does:** `BlockCastBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void cast(long chunkCoordinate, Vector3 rayOrigin, Vector3 direction, float maxDistance, BlockCastStruct out)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/RaycastManager.java`

**Type:** `class RaycastManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 62 lines

**What this class does:** `RaycastManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.physicspipeline.util.BlockCastStruct`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.core.engine.ManagerPackage`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public void castBlock(long chunkCoordinate, Vector3 rayOrigin, Vector3 direction, float maxDistance, BlockCastStruct out)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ScreenRayStruct getScreenRay()` — Returns current state/value.
- `public boolean hasScreenRay()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/raycastmanager/ScreenCastBranch.java`

**Type:** `class ScreenCastBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.physicspipeline.raycastmanager`
  
**File size:** 52 lines

**What this class does:** `ScreenCastBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.raycastmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.core.engine.BranchPackage`
- `program.core.kernel.windowmanager.WindowManager`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package boolean cast(ScreenRayStruct out)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/BlockCastStruct.java`

**Type:** `class BlockCastStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 125 lines

**What this class does:** `BlockCastStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `public boolean isHit()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setHit(boolean hit)` — Mutates internal state for this object.
- `public float getDistance()` — Returns current state/value.
- `public void setDistance(float distance)` — Mutates internal state for this object.
- `public Direction3Vector getHitFace()` — Returns current state/value.
- `public void setHitFace(Direction3Vector hitFace)` — Mutates internal state for this object.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public void setChunkCoordinate(long chunkCoordinate)` — Mutates internal state for this object.
- `public int getSubChunkY()` — Returns current state/value.
- `public void setSubChunkY(int subChunkY)` — Mutates internal state for this object.
- `public BlockHandle getBlock()` — Returns current state/value.
- `public void setBlock(BlockHandle block)` — Mutates internal state for this object.
- `public int getBlockX()` — Returns current state/value.
- `public void setBlockX(int blockX)` — Mutates internal state for this object.
- `public int getBlockY()` — Returns current state/value.
- `public void setBlockY(int blockY)` — Mutates internal state for this object.
- `public int getBlockZ()` — Returns current state/value.
- `public void setBlockZ(int blockZ)` — Mutates internal state for this object.
- `public int getHitSubX()` — Returns current state/value.
- `public void setHitSubX(int hitSubX)` — Mutates internal state for this object.
- `public int getHitSubY()` — Returns current state/value.
- `public void setHitSubY(int hitSubY)` — Mutates internal state for this object.
- `public int getHitSubZ()` — Returns current state/value.
- `public void setHitSubZ(int hitSubZ)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/BlockCompositionStruct.java`

**Type:** `class BlockCompositionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 171 lines

**What this class does:** `BlockCompositionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector3`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public BlockCompositionStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void updateBlockComposition(Vector3Int blockComposition, Vector3 currentPosition, long chunkCoordinate)` — Runs frame-step maintenance and logic.
- `private void buildBlockComposition(Vector3Int blockComposition, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private void buildAdjacentBlocks(Vector3Int blockComposition, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private void addBlockToMap(int blockX, int blockY, int blockZ, long chunkCoordinate, Int2LongOpenHashMap map)` — Registers a child object into manager-owned collections.
- `public Int2LongOpenHashMap getBlockCompositionMap()` — Returns current state/value.
- `public Int2LongOpenHashMap getAllBlocksForSide(Direction3Vector direction)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/physicspipeline/util/ScreenRayStruct.java`

**Type:** `class ScreenRayStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.physicspipeline.util`
  
**File size:** 69 lines

**What this class does:** `ScreenRayStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.physicspipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ScreenRayStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void init(int windowID, float screenX, float screenY, float screenW, float screenH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getWindowID()` — Returns current state/value.
- `public float getScreenX()` — Returns current state/value.
- `public float getScreenY()` — Returns current state/value.
- `public float getScreenW()` — Returns current state/value.
- `public float getScreenH()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/RenderPipeline.java`

**Type:** `class RenderPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.renderpipeline`
  
**File size:** 21 lines

**What this class does:** `RenderPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.cameramanager.CameraManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/cameramanager/CameraBufferSystem.java`

**Type:** `class CameraBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.cameramanager`
  
**File size:** 82 lines

**What this class does:** `CameraBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.cameramanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void pushForWindow(WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void pushPerspective(WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void pushOrtho(WindowInstance window)` — Queues data for downstream systems (often render queues).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/cameramanager/CameraManager.java`

**Type:** `class CameraManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.renderpipeline.cameramanager`
  
**File size:** 63 lines

**What this class does:** `CameraManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.cameramanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.InstancePackage`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.util.camera.CameraData`
- `program.core.util.camera.CameraInstance`
- `program.core.util.camera.OrthographicCameraData`
- `program.core.util.camera.OrthographicCameraInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void pushCamera(WindowInstance window)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/renderpipeline/cameramanager/CameraBufferSystem.java`.
- `public ObjectLinkedOpenHashSet<CameraInstance> getCameraInstances()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositebatch/CompositeBatchStruct.java`

**Type:** `class CompositeBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositebatch`
  
**File size:** 98 lines

**What this class does:** `CompositeBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositebatch`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public CompositeBatchStruct(MaterialInstance material)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void add(CompositeBufferInstance buffer)` — Registers a child object into manager-owned collections.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ObjectArrayList<CompositeBufferInstance> getBuffers()` — Returns current state/value.
- `public UBOHandle[] getCachedSourceUBOs()` — Returns current state/value.
- `public UniformStruct<?>[] getCachedUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`

**Type:** `class CompositeRenderSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositerendersystem`
  
**File size:** 271 lines

**What this class does:** `CompositeRenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositerendersystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.renderpipeline.compositebatch.CompositeBatchStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void submit(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void draw(WindowInstance window)` — Flushes or submits rendering work.
- `private void drawBuffer(CompositeBufferInstance buffer, int windowID)` — Flushes or submits rendering work.
- `private void upload(CompositeBufferInstance buffer, WindowBufferGpuState gpuState)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void bindMaterial(CompositeBatchStruct batch)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void ensureUploadBuffer(int floatCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void ensureGpuObjects(CompositeBufferInstance buffer, WindowBufferGpuState gpuState)` — Performs class-specific logic; see call sites and owning manager flow.
- `private WindowCompositeState getOrCreateCompositeState(int windowID)` — Returns current state/value.
- `private WindowBufferGpuState getOrCreateGpuState(CompositeBufferInstance buffer, int windowID)` — Returns current state/value.
- `public void removeWindow(int windowID)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/compositerendersystem/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.renderpipeline.compositerendersystem`
  
**File size:** 147 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.compositerendersystem`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void enableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void updateInstanceVBO(int vbo, FloatBuffer data, int floatCount)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createDynamicInstanceVBO(int maxInstances, int floatsPerInstance)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package int createInstancedVAO(int meshVBOHandle, int[] meshAttrSizes, int meshIBOHandle, int instanceVBOHandle, int[] instanceAttrSizes)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void drawElementsInstanced(int vao, int indexCount, int instanceCount)` — Flushes or submits rendering work. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void useShader(int shaderHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBlock(int shaderProgram, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.
- `package void bindUniformBuffer(int bindingPoint, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void deleteBuffer(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.
- `package void deleteVAO(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`, `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/renderbatch/RenderBatchStruct.java`

**Type:** `class RenderBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.renderbatch`
  
**File size:** 77 lines

**What this class does:** `RenderBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.renderbatch`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public RenderBatchStruct(MaterialInstance material)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addRenderCall(RenderCallStruct renderCall)` — Registers a child object into manager-owned collections.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance getRepresentativeMaterial()` — Returns current state/value.
- `public ObjectArrayList<RenderCallStruct> getRenderCalls()` — Returns current state/value.
- `public UBOHandle[] getCachedSourceUBOs()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendercall/RenderCallStruct.java`

**Type:** `class RenderCallStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendercall`
  
**File size:** 72 lines

**What this class does:** `RenderCallStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendercall`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.StructPackage`

**Method intent:**
- `public void init(ModelInstance modelInstance, MaskStruct mask)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public MaterialInstance getMaterialInstance()` — Returns current state/value.
- `public UniformStruct<?>[] getCachedUniforms()` — Returns current state/value.
- `public UBOInstance[] getCachedInstanceUBOs()` — Returns current state/value.
- `public MaskStruct getMask()` — Returns current state/value.
- `public boolean hasMask()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 128 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package void clearBuffer()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void clearDepthBuffer()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void setViewport(int width, int height)` — Mutates internal state for this object. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableDepth()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableBlending()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableBlending()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void enableCulling()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void disableCulling()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void enableScissor(int x, int y, int w, int h)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void disableScissor()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void useShader(int shaderHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindVAO(int vaoHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void unbindVAO()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void drawElements(int indexCount)` — Flushes or submits rendering work. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBuffer(int bindingPoint, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void bindUniformBlockToProgram(int shaderProgram, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`.
- `package void updateUniformBuffer(int gpuHandle, int offset, java.nio.ByteBuffer data)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void swapBuffers(long nativeHandle)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderManager.java`

**Type:** `class RenderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 107 lines

**What this class does:** `RenderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem`
- `program.bootstrap.renderpipeline.cameramanager.CameraManager`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `public void draw()` — Flushes or submits rendering work.
- `public void draw(WindowInstance window)` — Flushes or submits rendering work.
- `public void pushRenderCall(ModelInstance modelInstance, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer)` — Queues data for downstream systems (often render queues).
- `public void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void removeWindowResources(WindowInstance window)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderQueueHandle.java`

**Type:** `class RenderQueueHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 53 lines

**What this class does:** `RenderQueueHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct`
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `package RenderCallStruct nextCall()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/rendermanager/RenderSystem.java`

**Type:** `class RenderSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.renderpipeline.rendermanager`
  
**File size:** 247 lines

**What this class does:** `RenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.rendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.mesh.MeshData`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.renderpipeline.compositerendersystem.CompositeRenderSystem`
- `program.bootstrap.renderpipeline.renderbatch.RenderBatchStruct`
- `program.bootstrap.renderpipeline.rendercall.RenderCallStruct`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void draw(WindowInstance window)` — Flushes or submits rendering work.
- `private void bindMaterial(MaterialInstance material, int depth)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void bindSourceUBOs(RenderBatchStruct batch)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushInstanceUBOs(RenderCallStruct renderCall)` — Queues data for downstream systems (often render queues).
- `private void pushInstanceUniforms(RenderCallStruct renderCall)` — Queues data for downstream systems (often render queues).
- `private void drawBatchedRenderCall(RenderCallStruct renderCall, WindowInstance window)` — Flushes or submits rendering work.
- `package void pushCompositeCall(MaterialInstance material, CompositeBufferInstance buffer, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `package void removeWindowResources(WindowInstance window)` — Unregisters and tears down child references.
- `package void pushRenderCall(ModelInstance modelInstance, int depth, MaskStruct mask, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `private void insertDepthSorted(RenderQueueHandle queue, int depth)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/renderpipeline/util/MaskStruct.java`

**Type:** `class MaskStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.renderpipeline.util`
  
**File size:** 49 lines

**What this class does:** `MaskStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.renderpipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public MaskStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void set(int x, int y, int w, int h)` — Mutates internal state for this object.
- `public int getX()` — Returns current state/value.
- `public int getY()` — Returns current state/value.
- `public int getW()` — Returns current state/value.
- `public int getH()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ShaderPipeline.java`

**Type:** `class ShaderPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.shaderpipeline`
  
**File size:** 25 lines

**What this class does:** `ShaderPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.passmanager.PassManager`
- `program.bootstrap.shaderpipeline.shadermanager.ShaderManager`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialData.java`

**Type:** `class MaterialData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 127 lines

**What this class does:** `MaterialData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MaterialData(String materialName, int materialID, ShaderHandle shaderHandle, Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs, Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public MaterialData(MaterialData source)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialHandle.java`

**Type:** `class MaterialHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 80 lines

**What this class does:** `MaterialHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(MaterialData data)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public MaterialData getMaterialData()` — Returns current state/value.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/material/MaterialInstance.java`

**Type:** `class MaterialInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.material`
  
**File size:** 80 lines

**What this class does:** `MaterialInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.material`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MaterialData data)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public MaterialData getMaterialData()` — Returns current state/value.
- `public String getMaterialName()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public ShaderHandle getShaderHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs()` — Returns current state/value.
- `public UBOInstance getInstanceUBO(int bindingPoint)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getUniform(String uniformName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 138 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.material.MaterialData`
- `program.bootstrap.shaderpipeline.material.MaterialHandle`
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.bootstrap.shaderpipeline.shadermanager.ShaderManager`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void build(File file, String materialName)` — Constructs derived runtime/handle data from source input.
- `private boolean isSamplerType(UniformType type)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int resolveTextureHandle(String textureName, String uniformName, String materialName)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 89 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String materialName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/materialmanager/MaterialManager.java`

**Type:** `class MaterialManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.materialmanager`
  
**File size:** 95 lines

**What this class does:** `MaterialManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.materialmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.material.MaterialData`
- `program.bootstrap.shaderpipeline.material.MaterialHandle`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addMaterial(String materialName, MaterialHandle handle)` — Registers a child object into manager-owned collections.
- `public void request(String materialName)` — Triggers on-demand loading or lookup.
- `public boolean hasMaterial(String materialName)` — Boolean existence/availability check.
- `public int getMaterialIDFromMaterialName(String materialName)` — Returns current state/value.
- `public MaterialHandle getMaterialHandleFromMaterialID(int materialID)` — Returns current state/value.
- `public MaterialHandle getMaterialHandleFromMaterialName(String materialName)` — Returns current state/value.
- `public MaterialInstance cloneMaterial(String materialName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public MaterialInstance cloneMaterial(int materialID)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassData.java`

**Type:** `class PassData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 71 lines

**What this class does:** `PassData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public PassData(String passName, int passID, MeshHandle meshHandle, MaterialInstance material, ModelInstance modelInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`

**Type:** `class PassHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 62 lines

**What this class does:** `PassHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(PassData passData)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public PassData getPassData()` — Returns current state/value.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`

**Type:** `class PassInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.pass`
  
**File size:** 61 lines

**What this class does:** `PassInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.pass`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(PassData passData)` — Engine-side initialization entrypoint invoked post-create.
- `public void setUBO(UBOInstance ubo)` — Mutates internal state for this object.
- `public <T> void setUniform(String uniformName, T value)` — Mutates internal state for this object.
- `public PassData getPassData()` — Returns current state/value.
- `public String getPassName()` — Returns current state/value.
- `public int getPassID()` — Returns current state/value.
- `public MeshHandle getMeshHandle()` — Returns current state/value.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 64 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.pass.PassData`
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package PassHandle build(File file, String passName)` — Constructs derived runtime/handle data from source input.
- `private MeshHandle getMeshHandleFromJson(JsonObject json)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 81 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String passName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/passmanager/PassManager.java`

**Type:** `class PassManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.passmanager`
  
**File size:** 136 lines

**What this class does:** `PassManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.passmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.pass.PassData`
- `program.bootstrap.shaderpipeline.pass.PassHandle`
- `program.bootstrap.shaderpipeline.pass.PassInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package void addPassHandle(PassHandle handle)` — Registers a child object into manager-owned collections.
- `public void pushPass(PassHandle pass, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassInstance pass, int depth)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassHandle pass, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void pushPass(PassInstance pass, int depth, WindowInstance window)` — Queues data for downstream systems (often render queues).
- `public void request(String passName)` — Triggers on-demand loading or lookup.
- `public boolean hasPass(String passName)` — Boolean existence/availability check.
- `public int getPassIDFromPassName(String passName)` — Returns current state/value.
- `public PassHandle getPassHandleFromPassID(int passID)` — Returns current state/value.
- `public PassHandle getPassHandleFromPassName(String passName)` — Returns current state/value.
- `public PassInstance clonePass(int passID)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`, `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`.
- `public PassInstance clonePass(String passName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/pass/PassHandle.java`, `core/src/program/bootstrap/shaderpipeline/pass/PassInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderData.java`

**Type:** `class ShaderData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 68 lines

**What this class does:** `ShaderData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ShaderData(String shaderName, int shaderID, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `package void addCompiledUBOBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public String getShaderName()` — Returns current state/value.
- `public int getShaderID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getCompiledUBOBlockNames()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderHandle.java`

**Type:** `class ShaderHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 61 lines

**What this class does:** `ShaderHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ShaderData shaderData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void addCompiledUBOBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public ShaderData getShaderData()` — Returns current state/value.
- `public String getShaderName()` — Returns current state/value.
- `public int getShaderID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.
- `public ObjectArrayList<String> getCompiledUBOBlockNames()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderSourceStruct.java`

**Type:** `class ShaderSourceStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 119 lines

**What this class does:** `ShaderSourceStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ShaderSourceStruct(ShaderType shaderType, String shaderName, File shaderFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setVersion(String version)` — Mutates internal state for this object.
- `public void addDirectInclude(ShaderSourceStruct include)` — Registers a child object into manager-owned collections.
- `public void addBufferBlockName(String blockName)` — Registers a child object into manager-owned collections.
- `public void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `public void setVert(ShaderSourceStruct vert)` — Mutates internal state for this object.
- `public void setFrag(ShaderSourceStruct frag)` — Mutates internal state for this object.
- `public void addFlattenedInclude(ShaderSourceStruct include)` — Registers a child object into manager-owned collections.
- `public ShaderType getShaderType()` — Returns current state/value.
- `public String getShaderName()` — Returns current state/value.
- `public File getShaderFile()` — Returns current state/value.
- `public String getVersion()` — Returns current state/value.
- `public ObjectArrayList<ShaderSourceStruct> getDirectIncludes()` — Returns current state/value.
- `public ObjectArrayList<String> getBufferBlockNames()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public ShaderSourceStruct getVert()` — Returns current state/value.
- `public ShaderSourceStruct getFrag()` — Returns current state/value.
- `public ObjectArrayList<ShaderSourceStruct> getFlattenedIncludes()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shader/ShaderType.java`

**Type:** `enum ShaderType`
  
**Package:** `program.bootstrap.shaderpipeline.shader`
  
**File size:** 13 lines

**What this class does:** `ShaderType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shader`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/FileParserUtility.java`

**Type:** `class FileParserUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 172 lines

**What this class does:** `FileParserUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package String convertFileToRawText(File file)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/GLSLUtility.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package ObjectArrayList<String> convertRawTextToArray(String rawText)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `private String stripLineComments(String text)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String stripBlockComments(String text)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean lineStartsWith(String line, String token)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int countCharInString(String str, char target)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int extractBufferBinding(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int findLastTypeDelimiter(String declaration)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package int parseIntOrDefault(String str, int defaultValue)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.
- `package String extractPayloadAfterToken(String line, String token)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 156 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package int createShaderProgram(ShaderSourceStruct assembly)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`.
- `private int compileShaderFromSource(int type, String source, String shaderName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String preprocessShaderSource(ShaderSourceStruct assembly, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String stripDirectives(String source)` — Performs class-specific logic; see call sites and owning manager flow.
- `package int getUniformLocation(int programHandle, String uniformName)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`.
- `package void bindUniformBlock(int programHandle, String blockName, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`, `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.
- `package void deleteShaderProgram(int programHandle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 292 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.bootstrap.shaderpipeline.shader.ShaderType`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void parseShaderFile(ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseVersionInfo(ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniforms(ShaderSourceStruct source, ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isUniformBlockStart(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseUniformBlockName(String line)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniformDeclaration(String declaration, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseVariableNames(String namesStr, UniformType uniformType, ShaderSourceStruct source)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseIncludes(ShaderSourceStruct source, ObjectArrayList<String> lines)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ShaderSourceStruct buildAssembly(File jsonFile)` — Constructs derived runtime/handle data from source input.
- `private ShaderSourceStruct collectIncludes(ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void collectPostOrder(ShaderSourceStruct assembly, ShaderSourceStruct source, ObjectArrayList<ShaderSourceStruct> visited)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 227 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderData`
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.shader.ShaderSourceStruct`
- `program.bootstrap.shaderpipeline.shader.ShaderType`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void categorizeFile(File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String shaderName)` — Triggers on-demand loading or lookup.
- `private ShaderHandle assembleShader(ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void assembleBuffers(ShaderHandle shader, ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void addBuffersFromSource(ShaderHandle shader, ShaderSourceStruct source)` — Registers a child object into manager-owned collections.
- `private void assembleUniforms(ShaderHandle shader, ShaderSourceStruct assembly)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void addUniformsFromSource(ShaderHandle shader, ShaderSourceStruct source)` — Registers a child object into manager-owned collections.
- `private void addUniform(ShaderHandle shader, UniformData uniformData)` — Registers a child object into manager-owned collections.
- `package ShaderSourceStruct getSourceStruct(String key)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/shadermanager/ShaderManager.java`

**Type:** `class ShaderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.shadermanager`
  
**File size:** 97 lines

**What this class does:** `ShaderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.shadermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.shader.ShaderHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addShaderHandle(ShaderHandle handle)` — Registers a child object into manager-owned collections.
- `package void bindShaderToUBO(ShaderHandle shader, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String shaderName)` — Triggers on-demand loading or lookup.
- `public boolean hasShader(String shaderName)` — Boolean existence/availability check.
- `public int getShaderIDFromShaderName(String shaderName)` — Returns current state/value.
- `public ShaderHandle getShaderHandleFromShaderID(int shaderID)` — Returns current state/value.
- `public ShaderHandle getShaderHandleFromShaderName(String shaderName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteData.java`

**Type:** `class SpriteData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 116 lines

**What this class does:** `SpriteData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public SpriteData(String name, int gpuHandle, int width, int height, float borderLeft, float borderBottom, float borderRight, float borderTop, ModelInstance modelInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public SpriteData(SpriteData source, ModelInstance modelInstance, UBOInstance sliceData)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public float getBorderLeft()` — Returns current state/value.
- `public float getBorderBottom()` — Returns current state/value.
- `public float getBorderRight()` — Returns current state/value.
- `public float getBorderTop()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public UBOInstance getSliceData()` — Returns current state/value.
- `public boolean hasSlice()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteHandle.java`

**Type:** `class SpriteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 69 lines

**What this class does:** `SpriteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(SpriteData spriteData)` — Engine-side initialization entrypoint invoked post-create.
- `public SpriteData getSpriteData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public float getBorderLeft()` — Returns current state/value.
- `public float getBorderBottom()` — Returns current state/value.
- `public float getBorderRight()` — Returns current state/value.
- `public float getBorderTop()` — Returns current state/value.
- `public boolean hasSlice()` — Boolean existence/availability check.
- `public ModelInstance getModelInstance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/sprite/SpriteInstance.java`

**Type:** `class SpriteInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.sprite`
  
**File size:** 54 lines

**What this class does:** `SpriteInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.sprite`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(SpriteData spriteData)` — Engine-side initialization entrypoint invoked post-create.
- `public SpriteData getSpriteData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getWidth()` — Returns current state/value.
- `public int getHeight()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public UBOInstance getSliceData()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 62 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.image.Pixmap`
- `program.core.engine.UtilityPackage`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushSprite(BufferedImage image)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalLoader.java`.
- `package void deleteSprite(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/spritemanager/SpriteManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 61 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `package BufferedImage loadImage(File file)` — Parses external data into engine objects.
- `package float[] parseCompanionBorder(File imageFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File getCompanionJson(File imageFile)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 151 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.mesh.MeshHandle`
- `program.bootstrap.geometrypipeline.meshmanager.MeshManager`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.sprite.SpriteData`
- `program.bootstrap.shaderpipeline.sprite.SpriteHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String spriteName)` — Triggers on-demand loading or lookup.
- `package MeshHandle getDefaultMeshHandle()` — Returns current state/value.
- `package int getDefaultMaterialID()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/spritemanager/SpriteManager.java`

**Type:** `class SpriteManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.spritemanager`
  
**File size:** 148 lines

**What this class does:** `SpriteManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.spritemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.sprite.SpriteData`
- `program.bootstrap.shaderpipeline.sprite.SpriteHandle`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector2`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addSpriteHandle(String spriteName, SpriteHandle handle)` — Registers a child object into manager-owned collections.
- `public void request(String spriteName)` — Triggers on-demand loading or lookup.
- `public boolean hasSprite(String spriteName)` — Boolean existence/availability check.
- `public int getSpriteIDFromSpriteName(String spriteName)` — Returns current state/value.
- `public SpriteHandle getSpriteHandleFromSpriteID(int spriteID)` — Returns current state/value.
- `public SpriteHandle getSpriteHandleFromSpriteName(String spriteName)` — Returns current state/value.
- `public SpriteInstance cloneSprite(String spriteName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/sprite/SpriteHandle.java`, `core/src/program/bootstrap/shaderpipeline/sprite/SpriteInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureArrayStruct.java`

**Type:** `class TextureArrayStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 98 lines

**What this class does:** `TextureArrayStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public TextureArrayStruct(int id, String name, int atlasPixelSize, TextureAtlasStruct[] textureArray)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void registerFoundAlias(int aliasId)` — Performs class-specific logic; see call sites and owning manager flow.
- `public IntSet getFoundAliasIds()` — Returns current state/value.
- `public void registerTile(TextureTileStruct tile)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Object2ObjectOpenHashMap<String, TextureTileStruct> getTileCoordinateMap()` — Returns current state/value.
- `public void clearAtlases()` — Performs class-specific logic; see call sites and owning manager flow.
- `public BufferedImage[] getRawImageArray()` — Returns current state/value.
- `public int getID()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureAtlasStruct.java`

**Type:** `class TextureAtlasStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 40 lines

**What this class does:** `TextureAtlasStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public TextureAtlasStruct(int atlasSize, BufferedImage atlas)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void clearImage()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getAtlasSize()` — Returns current state/value.
- `package BufferedImage getAtlas()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureData.java`

**Type:** `class TextureData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 110 lines

**What this class does:** `TextureData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public TextureData(String tileName, int tileID, int arrayID, String arrayName, int gpuHandle, int atlasPixelSize, int tileWidth, int tileHeight, float u0, float v0, float u1, float v1)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getTileName()` — Returns current state/value.
- `public int getTileID()` — Returns current state/value.
- `public int getArrayID()` — Returns current state/value.
- `public String getArrayName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public int getTileWidth()` — Returns current state/value.
- `public int getTileHeight()` — Returns current state/value.
- `public float getU0()` — Returns current state/value.
- `public float getV0()` — Returns current state/value.
- `public float getU1()` — Returns current state/value.
- `public float getV1()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureHandle.java`

**Type:** `class TextureHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 75 lines

**What this class does:** `TextureHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(TextureData textureData)` — Engine-side initialization entrypoint invoked post-create.
- `public TextureData getTextureData()` — Returns current state/value.
- `public String getTileName()` — Returns current state/value.
- `public int getTileID()` — Returns current state/value.
- `public int getArrayID()` — Returns current state/value.
- `public String getArrayName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public int getTileWidth()` — Returns current state/value.
- `public int getTileHeight()` — Returns current state/value.
- `public float getU0()` — Returns current state/value.
- `public float getV0()` — Returns current state/value.
- `public float getU1()` — Returns current state/value.
- `public float getV1()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texture/TextureTileStruct.java`

**Type:** `class TextureTileStruct`
  
**Inheritance/implements:** `extends AtlasTileData`
  
**Package:** `program.bootstrap.shaderpipeline.texture`
  
**File size:** 76 lines

**What this class does:** `TextureTileStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texture`.

**Who this class talks to (direct imports):**
- `program.core.util.atlas.AtlasTileData`

**Method intent:**
- `package public TextureTileStruct(int id, String name, String atlas, int aliasCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setImage(BufferedImage image, int layer)` — Mutates internal state for this object.
- `public BufferedImage getImage(int layer)` — Returns current state/value.
- `public void clearImages()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getID()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `package String getAtlas()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/AliasLibrarySystem.java`

**Type:** `class AliasLibrarySystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 139 lines

**What this class does:** `AliasLibrarySystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.SystemPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void loadAliases()` — Parses external data into engine objects.
- `private void loadAliasFile(File file)` — Parses external data into engine objects.
- `private void ensureCapacity(int requiredCapacity)` — Performs class-specific logic; see call sites and owning manager flow.
- `public AliasStruct[] getAllAliases()` — Returns current state/value.
- `public int getAliasCount()` — Returns current state/value.
- `public int get(String aliasVariation)` — Returns current state/value.
- `public int getOrDefault(String aliasVariation)` — Returns current state/value.
- `public AliasStruct getAlias(int aliasId)` — Returns current state/value.
- `public Color getDefaultColor(int id)` — Returns current state/value.
- `public String getUniformName(int aliasId)` — Returns current state/value.
- `public boolean hasAlias(String aliasVariation)` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/AliasStruct.java`

**Type:** `class AliasStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 40 lines

**What this class does:** `AliasStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public AliasStruct(String aliasType, Color defaultColor, String uniformName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package String getAliasType()` — Returns current state/value.
- `public Color getAliasColor()` — Returns current state/value.
- `public String getUniformName()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 96 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.image.Pixmap`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushTextureArray(BufferedImage[] layers)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalLoader.java`.
- `package void deleteTextureArray(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/texturemanager/TextureManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 187 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureAtlasStruct`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.core.engine.BuilderPackage`
- `program.core.util.AtlasUtility`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package TextureArrayStruct build(List<File> imageFiles, File sourceDirectory, String arrayName)` — Constructs derived runtime/handle data from source input.
- `private LinkedHashMap<String, TextureTileStruct> createTextureTiles(List<File> imageFiles, File sourceDirectory, String arrayName)` — Allocates/initializes child systems or resources.
- `private LinkedHashMap<String, TextureTileStruct> organizeTextureTiles(LinkedHashMap<String, TextureTileStruct> tileMap)` — Performs class-specific logic; see call sites and owning manager flow.
- `private TextureAtlasStruct[] compositeAtlasLayers(ObjectArrayList<TextureTileStruct> tiles, int atlasPixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private TextureArrayStruct createTextureArray(LinkedHashMap<String, TextureTileStruct> tileMap, String arrayName, int atlasPixelSize, TextureAtlasStruct[] atlasLayers)` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 165 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File directory)` — Parses external data into engine objects.
- `package void request(String arrayName)` — Triggers on-demand loading or lookup.
- `private void seedUBO(String arrayName, TextureArrayStruct arrayStruct)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushToGPU(TextureArrayStruct arrayStruct)` — Queues data for downstream systems (often render queues).
- `private void clearHeapImages(TextureArrayStruct arrayStruct)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/texturemanager/TextureManager.java`

**Type:** `class TextureManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.texturemanager`
  
**File size:** 153 lines

**What this class does:** `TextureManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.texturemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.texture.TextureArrayStruct`
- `program.bootstrap.shaderpipeline.texture.TextureData`
- `program.bootstrap.shaderpipeline.texture.TextureHandle`
- `program.bootstrap.shaderpipeline.texture.TextureTileStruct`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void registerTile(TextureTileStruct tile, float u0, float v0, float u1, float v1, TextureArrayStruct array, int gpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String arrayName)` — Triggers on-demand loading or lookup.
- `public boolean hasTexture(String textureName)` — Boolean existence/availability check.
- `public int getTileIDFromTextureName(String textureName)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromTileID(int tileID)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromTextureName(String textureName)` — Returns current state/value.
- `public boolean hasArray(String arrayName)` — Boolean existence/availability check.
- `public int getArrayIDFromArrayName(String arrayName)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromArrayID(int arrayID)` — Returns current state/value.
- `public TextureHandle getTextureHandleFromArrayName(String arrayName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOData.java`

**Type:** `class UBOData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 146 lines

**What this class does:** `UBOData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.DataPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `package public UBOData(String blockName, int requestedBinding)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UBOData(UBOData source, int newGpuHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `package void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `package void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public ByteBuffer getStagingBuffer()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getRequestedBinding()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public int getBufferID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getCompiledUniform(String name)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOHandle.java`

**Type:** `class UBOHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 94 lines

**What this class does:** `UBOHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(UBOData uboData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addUniformDeclaration(UniformData uniform)` — Registers a child object into manager-owned collections.
- `public void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public UBOData getUBOData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getRequestedBinding()` — Returns current state/value.
- `public ObjectArrayList<UniformData> getUniformDeclarations()` — Returns current state/value.
- `public int getBufferID()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public UniformStruct<?> getCompiledUniform(String name)` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`

**Type:** `class UBOInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubo`
  
**File size:** 68 lines

**What this class does:** `UBOInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubo`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(UBOData uboData)` — Engine-side initialization entrypoint invoked post-create.
- `public void addCompiledUniform(String name, UniformStruct<?> uniform)` — Registers a child object into manager-owned collections.
- `public void updateUniform(String name, Object value)` — Runs frame-step maintenance and logic.
- `public UBOData getUBOData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public int getGpuHandle()` — Returns current state/value.
- `public int getBindingPoint()` — Returns current state/value.
- `public int getTotalSizeBytes()` — Returns current state/value.
- `public ObjectArrayList<String> getUniformKeys()` — Returns current state/value.
- `public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 50 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.core.util.memory.BufferUtils`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package int createUniformBuffer()` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void allocateUniformBuffer(int buffer, int sizeBytes)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void bindUniformBufferBase(int buffer, int bindingPoint)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void updateUniformBuffer(int buffer, int offset, ByteBuffer data)` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `package void deleteUniformBuffer(int buffer)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 89 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOData`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.engine.BuilderPackage`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package UBOHandle parse(File file)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void parseUniforms(JsonObject json, UBOHandle handle, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private UniformType parseUniformType(String blockName, String uniformName, String raw)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 79 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String blockName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`

**Type:** `class UBOManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.shaderpipeline.ubomanager`
  
**File size:** 248 lines

**What this class does:** `UBOManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.ubomanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOData`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformData`
- `program.bootstrap.shaderpipeline.uniforms.UniformStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformUtility`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void buildBuffer(UBOHandle handle)` — Constructs derived runtime/handle data from source input. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOData.java`.
- `public UBOInstance createUBOInstance(UBOHandle handle)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOHandle.java`, `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`.
- `public void destroyInstance(UBOInstance instance)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubo/UBOInstance.java`.
- `public void push(UBOHandle handle)` — Queues data for downstream systems (often render queues).
- `public void push(UBOInstance instance)` — Queues data for downstream systems (often render queues).
- `private int resolveBinding(int requestedBinding, String blockName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int allocateBindingPoint()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void releaseBindingPoint(int binding)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformDeclarations)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void populateUniforms(UBOHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String blockName)` — Triggers on-demand loading or lookup.
- `public boolean hasUBO(String uboName)` — Boolean existence/availability check.
- `public int getUBOIDFromUBOName(String uboName)` — Returns current state/value.
- `public UBOHandle getUBOHandleFromUBOID(int uboID)` — Returns current state/value.
- `public UBOHandle getUBOHandleFromUBOName(String uboName)` — Returns current state/value.
- `public UBOHandle findUBOHandle(String blockName)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformAttributeStruct.java`

**Type:** `class UniformAttributeStruct`
  
**Inheritance/implements:** `<T> extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 127 lines

**What this class does:** `UniformAttributeStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.util.memory.BufferUtils`
- `program.core.engine.StructPackage`

**Method intent:**
- `package protected UniformAttributeStruct(UniformType type, T defaultValue)` — Performs class-specific logic; see call sites and owning manager flow.
- `package protected UniformAttributeStruct(UniformType type, int count, T defaultValue)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeUBOBufferSize(UniformType type, int count)` — Performs class-specific logic; see call sites and owning manager flow.
- `public final ByteBuffer getByteBuffer()` — Returns current state/value.
- `protected final void writeToBuffer(ByteBuffer buffer, T value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `package final void push(int handle)` — Queues data for downstream systems (often render queues).
- `public UniformAttributeStruct<T> clone()` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void set(T value)` — Mutates internal state for this object.
- `public final void setObject(Object value)` — Mutates internal state for this object.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public T getValue()` — Returns current state/value.
- `public UniformType getUniformType()` — Returns current state/value.
- `public int getCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformData.java`

**Type:** `class UniformData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 48 lines

**What this class does:** `UniformData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public UniformData(UniformType uniformType, String uniformName)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UniformData(UniformType uniformType, String uniformName, int count)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformType getUniformType()` — Returns current state/value.
- `public String getUniformName()` — Returns current state/value.
- `public int getCount()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformStruct.java`

**Type:** `class UniformStruct`
  
**Inheritance/implements:** `<T> extends StructPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 58 lines

**What this class does:** `UniformStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public UniformStruct(int uniformHandle, UniformAttributeStruct<T> attribute)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public UniformStruct(int uniformHandle, int offset, UniformAttributeStruct<T> attribute)` — Performs class-specific logic; see call sites and owning manager flow.
- `public final void push()` — Queues data for downstream systems (often render queues).
- `public UniformStruct<T> clone()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getUniformHandle()` — Returns current state/value.
- `public int getOffset()` — Returns current state/value.
- `public UniformAttributeStruct<T> attribute()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformType.java`

**Type:** `enum UniformType`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 262 lines

**What this class does:** `UniformType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Method intent:**
- `package  FLOAT("float", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void writeElement(ByteBuffer buffer, Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  DOUBLE("double", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  INT("int", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  BOOL("bool", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2("vec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3("vec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4("vec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_DOUBLE("dvec2", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_DOUBLE("dvec3", 32, 24)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_DOUBLE("dvec4", 32, 32)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_INT("ivec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_INT("ivec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_INT("ivec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR2_BOOLEAN("bvec2", 8, 8)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR3_BOOLEAN("bvec3", 16, 12)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  VECTOR4_BOOLEAN("bvec4", 16, 16)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX2("mat2", 16, 32)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX3("mat3", 16, 48)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX4("mat4", 16, 64)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX2_DOUBLE("dmat2", 32, 64)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX3_DOUBLE("dmat3", 32, 96)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  MATRIX4_DOUBLE("dmat4", 32, 128)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  SAMPLE_IMAGE_2D("sampler2D", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  SAMPLE_IMAGE_2D_ARRAY("sampler2DArray", 4, 4)` — Performs class-specific logic; see call sites and owning manager flow.
- `package  UniformType(String glslName, int std140Alignment, int std140Size)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getGLSLName()` — Returns current state/value.
- `public int getStd140Alignment()` — Returns current state/value.
- `public int getStd140Size()` — Returns current state/value.
- `public UniformType fromString(String glslName)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/UniformUtility.java`

**Type:** `class UniformUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms`
  
**File size:** 444 lines

**What this class does:** `UniformUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms`.

**Who this class talks to (direct imports):**
- `program.core.engine.UtilityPackage`

**Method intent:**
- `package private UniformUtility()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int align(int offset, int alignment)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`, `core/src/program/bootstrap/shaderpipeline/uniforms/UniformAttributeStruct.java`.
- `public int getStd140Alignment(UniformData ud)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public int getStd140Size(UniformData ud)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public UniformAttributeStruct<?> createUniformAttribute(UniformData ud)` — Allocates/initializes child systems or resources. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/shadermanager/InternalLoader.java`, `core/src/program/bootstrap/shaderpipeline/ubomanager/UBOManager.java`.
- `public void applyFromJsonObject(UniformAttributeStruct<?> attribute, String uniformName, JsonObject uniformData)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/shaderpipeline/materialmanager/InternalBuilder.java`.
- `public void applySingle(UniformAttributeStruct<?> attribute, String type, JsonElement value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void applyArray(UniformAttributeStruct<?> attribute, String type, JsonElement valueElement)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2 parseVector2(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Double parseVector2Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Int parseVector2Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector2Boolean parseVector2Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3 parseVector3(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Double parseVector3Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Int parseVector3Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector3Boolean parseVector3Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4 parseVector4(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Double parseVector4Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Int parseVector4Int(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Vector4Boolean parseVector4Boolean(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix2 parseMatrix2(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix3 parseMatrix3(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix4 parseMatrix4(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix2Double parseMatrix2Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix3Double parseMatrix3Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.
- `public Matrix4Double parseMatrix4Double(JsonArray array)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix2DoubleUniform.java`

**Type:** `class Matrix2DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix2Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 40 lines

**What this class does:** `Matrix2DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2Double`

**Method intent:**
- `package public Matrix2DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix2Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix2Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix2Uniform.java`

**Type:** `class Matrix2Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix2>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 40 lines

**What this class does:** `Matrix2Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2`

**Method intent:**
- `package public Matrix2Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix2 value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix2 value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix3DoubleUniform.java`

**Type:** `class Matrix3DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix3Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 39 lines

**What this class does:** `Matrix3DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`
- `program.core.util.mathematics.matrices.Matrix3Double`

**Method intent:**
- `package public Matrix3DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix3Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix3Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix3Uniform.java`

**Type:** `class Matrix3Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 44 lines

**What this class does:** `Matrix3Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`

**Method intent:**
- `package public Matrix3Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix4DoubleUniform.java`

**Type:** `class Matrix4DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Matrix4Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 39 lines

**What this class does:** `Matrix4DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`
- `program.core.util.mathematics.matrices.Matrix4Double`

**Method intent:**
- `package public Matrix4DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Matrix4Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Matrix4Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrices/Matrix4Uniform.java`

**Type:** `class Matrix4Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrices`
  
**File size:** 44 lines

**What this class does:** `Matrix4Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrices`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `package public Matrix4Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix2ArrayUniform.java`

**Type:** `class Matrix2ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 54 lines

**What this class does:** `Matrix2ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2`

**Method intent:**
- `package public Matrix2ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix2DoubleArrayUniform.java`

**Type:** `class Matrix2DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 54 lines

**What this class does:** `Matrix2DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix2Double`

**Method intent:**
- `package public Matrix2DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix3ArrayUniform.java`

**Type:** `class Matrix3ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 57 lines

**What this class does:** `Matrix3ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3`

**Method intent:**
- `package public Matrix3ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix3DoubleArrayUniform.java`

**Type:** `class Matrix3DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 52 lines

**What this class does:** `Matrix3DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix3Double`

**Method intent:**
- `package public Matrix3DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix4ArrayUniform.java`

**Type:** `class Matrix4ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 57 lines

**What this class does:** `Matrix4ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `package public Matrix4ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/matrixArrays/Matrix4DoubleArrayUniform.java`

**Type:** `class Matrix4DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.matrixArrays`
  
**File size:** 52 lines

**What this class does:** `Matrix4DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.matrixArrays`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.memory.BufferUtils`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.matrices.Matrix4Double`

**Method intent:**
- `package public Matrix4DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/samplers/SampleImage2DArrayUniform.java`

**Type:** `class SampleImage2DArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.samplers`
  
**File size:** 44 lines

**What this class does:** `SampleImage2DArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.samplers`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.graphics.gl.GL30`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public SampleImage2DArrayUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/samplers/SampleImage2DUniform.java`

**Type:** `class SampleImage2DUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.samplers`
  
**File size:** 43 lines

**What this class does:** `SampleImage2DUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.samplers`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.core.util.graphics.gl.GL20`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public SampleImage2DUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `public boolean isSampler()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void bindTexture(int unit)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/BooleanArrayUniform.java`

**Type:** `class BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/DoubleArrayUniform.java`

**Type:** `class DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/FloatArrayUniform.java`

**Type:** `class FloatArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `FloatArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public FloatArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalarArrays/IntegerArrayUniform.java`

**Type:** `class IntegerArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalarArrays`
  
**File size:** 48 lines

**What this class does:** `IntegerArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalarArrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public IntegerArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/BooleanUniform.java`

**Type:** `class BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/DoubleUniform.java`

**Type:** `class DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/FloatUniform.java`

**Type:** `class FloatUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Float>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `FloatUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public FloatUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Float value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Float value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/scalars/IntegerUniform.java`

**Type:** `class IntegerUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Integer>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.scalars`
  
**File size:** 28 lines

**What this class does:** `IntegerUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.scalars`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`

**Method intent:**
- `package public IntegerUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Integer value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Integer value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2ArrayUniform.java`

**Type:** `class Vector2ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 66 lines

**What this class does:** `Vector2ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public Vector2ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2BooleanArrayUniform.java`

**Type:** `class Vector2BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Boolean`

**Method intent:**
- `package public Vector2BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2DoubleArrayUniform.java`

**Type:** `class Vector2DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Double`

**Method intent:**
- `package public Vector2DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector2IntArrayUniform.java`

**Type:** `class Vector2IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 52 lines

**What this class does:** `Vector2IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `package public Vector2IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3ArrayUniform.java`

**Type:** `class Vector3ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 68 lines

**What this class does:** `Vector3ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public Vector3ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3BooleanArrayUniform.java`

**Type:** `class Vector3BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Boolean`

**Method intent:**
- `package public Vector3BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3DoubleArrayUniform.java`

**Type:** `class Vector3DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Double`

**Method intent:**
- `package public Vector3DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector3IntArrayUniform.java`

**Type:** `class Vector3IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 53 lines

**What this class does:** `Vector3IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public Vector3IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4ArrayUniform.java`

**Type:** `class Vector4ArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 70 lines

**What this class does:** `Vector4ArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `package public Vector4ArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void applyObject(Object value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4BooleanArrayUniform.java`

**Type:** `class Vector4BooleanArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4BooleanArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Boolean`

**Method intent:**
- `package public Vector4BooleanArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4DoubleArrayUniform.java`

**Type:** `class Vector4DoubleArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4DoubleArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Double`

**Method intent:**
- `package public Vector4DoubleArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectorarrays/Vector4IntArrayUniform.java`

**Type:** `class Vector4IntArrayUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object[]>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectorarrays`
  
**File size:** 54 lines

**What this class does:** `Vector4IntArrayUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectorarrays`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Int`

**Method intent:**
- `package public Vector4IntArrayUniform(int elementCount)` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object[] value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object[] value)` — Performs class-specific logic; see call sites and owning manager flow.
- `public int elementCount()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2BooleanUniform.java`

**Type:** `class Vector2BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Boolean`

**Method intent:**
- `package public Vector2BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2DoubleUniform.java`

**Type:** `class Vector2DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Double`

**Method intent:**
- `package public Vector2DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2IntUniform.java`

**Type:** `class Vector2IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector2Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector2IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `package public Vector2IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector2Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector2Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector2Uniform.java`

**Type:** `class Vector2Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector2Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public Vector2Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3BooleanUniform.java`

**Type:** `class Vector3BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Boolean`

**Method intent:**
- `package public Vector3BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3DoubleUniform.java`

**Type:** `class Vector3DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Double`

**Method intent:**
- `package public Vector3DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3IntUniform.java`

**Type:** `class Vector3IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector3Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector3IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3Int`

**Method intent:**
- `package public Vector3IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector3Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector3Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector3Uniform.java`

**Type:** `class Vector3Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector3Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public Vector3Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4BooleanUniform.java`

**Type:** `class Vector4BooleanUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Boolean>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4BooleanUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Boolean`

**Method intent:**
- `package public Vector4BooleanUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Boolean value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Boolean value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4DoubleUniform.java`

**Type:** `class Vector4DoubleUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Double>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4DoubleUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Double`

**Method intent:**
- `package public Vector4DoubleUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Double value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Double value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4IntUniform.java`

**Type:** `class Vector4IntUniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Vector4Int>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 29 lines

**What this class does:** `Vector4IntUniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4Int`

**Method intent:**
- `package public Vector4IntUniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Vector4Int value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Vector4Int value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/shaderpipeline/uniforms/vectors/Vector4Uniform.java`

**Type:** `class Vector4Uniform`
  
**Inheritance/implements:** `extends UniformAttributeStruct<Object>`
  
**Package:** `program.bootstrap.shaderpipeline.uniforms.vectors`
  
**File size:** 35 lines

**What this class does:** `Vector4Uniform` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.shaderpipeline.uniforms.vectors`.

**Who this class talks to (direct imports):**
- `program.core.app.CoreContext`
- `program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct`
- `program.bootstrap.shaderpipeline.uniforms.UniformType`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `package public Vector4Uniform()` — Performs class-specific logic; see call sites and owning manager flow.
- `public UniformAttributeStruct<?> createDefault()` — Allocates/initializes child systems or resources.
- `protected void push(int handle, Object value)` — Queues data for downstream systems (often render queues).
- `protected void applyValue(Object value)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/WorldPipeline.java`

**Type:** `class WorldPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.worldpipeline`
  
**File size:** 35 lines

**What this class does:** `WorldPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.gridmanager.GridManager`
- `program.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem`
- `program.bootstrap.worldpipeline.worldmanager.WorldManager`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biome/BiomeData.java`

**Type:** `class BiomeData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.biome`
  
**File size:** 42 lines

**What this class does:** `BiomeData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biome`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `package public BiomeData(String biomeName, short biomeID, Color biomeColor)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBiomeName()` — Returns current state/value.
- `public short getBiomeID()` — Returns current state/value.
- `public Color getBiomeColor()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biome/BiomeHandle.java`

**Type:** `class BiomeHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.biome`
  
**File size:** 39 lines

**What this class does:** `BiomeHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biome`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `public void constructor(BiomeData biomeData)` — Engine-side initialization entrypoint invoked post-create.
- `public BiomeData getBiomeData()` — Returns current state/value.
- `public String getBiomeName()` — Returns current state/value.
- `public short getBiomeID()` — Returns current state/value.
- `public Color getBiomeColor()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/BiomeManager.java`

**Type:** `class BiomeManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 77 lines

**What this class does:** `BiomeManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBiome(BiomeHandle biomeHandle)` — Registers a child object into manager-owned collections.
- `public void request(String biomeName)` — Triggers on-demand loading or lookup.
- `public boolean hasBiome(String biomeName)` — Boolean existence/availability check.
- `public short getBiomeIDFromBiomeName(String biomeName)` — Returns current state/value.
- `public BiomeHandle getBiomeHandleFromBiomeID(short biomeID)` — Returns current state/value.
- `public BiomeHandle getBiomeHandleFromBiomeName(String biomeName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 27 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeData`
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.BuilderPackage`
- `program.core.util.FileUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.extras.Color`

**Method intent:**
- `package BiomeHandle build(File file, File root)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/biomemanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.biomemanager`
  
**File size:** 80 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.biomemanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biome.BiomeHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void scan()` — Discovers files/resources for later load.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String biomeName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockData.java`

**Type:** `class BlockData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 104 lines

**What this class does:** `BlockData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package public BlockData(String blockName, short blockID, DynamicGeometryType geometry, BlockRotationType rotationType, int materialID, int northTexture, int eastTexture, int southTexture, int westTexture, int upTexture, int downTexture, int breakTier, short requiredToolTypeID, int durability)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getBlockName()` — Returns current state/value.
- `public short getBlockID()` — Returns current state/value.
- `public DynamicGeometryType getGeometry()` — Returns current state/value.
- `public BlockRotationType getRotationType()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getTextureForFace(Direction3Vector direction)` — Returns current state/value.
- `public int getBreakTier()` — Returns current state/value.
- `public short getRequiredToolTypeID()` — Returns current state/value.
- `public int getDurability()` — Returns current state/value.
- `public boolean isUnbreakable()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockHandle.java`

**Type:** `class BlockHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 68 lines

**What this class does:** `BlockHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `public void constructor(BlockData blockData)` — Engine-side initialization entrypoint invoked post-create.
- `public BlockData getBlockData()` — Returns current state/value.
- `public String getBlockName()` — Returns current state/value.
- `public short getBlockID()` — Returns current state/value.
- `public DynamicGeometryType getGeometry()` — Returns current state/value.
- `public BlockRotationType getRotationType()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getTextureForFace(Direction3Vector direction)` — Returns current state/value.
- `public int getBreakTier()` — Returns current state/value.
- `public short getRequiredToolTypeID()` — Returns current state/value.
- `public int getDurability()` — Returns current state/value.
- `public boolean isUnbreakable()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockPaletteHandle.java`

**Type:** `class BlockPaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 281 lines

**What this class does:** `BlockPaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.util.ChunkCoordinate3Int`
- `program.core.engine.HandlePackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `public void constructor(int paletteAxisSize, int paletteThreshold, short defaultBlockId)` — Engine-side initialization entrypoint invoked post-create.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void allocatePackedArray()` — Performs class-specific logic; see call sites and owning manager flow.
- `private int calculateBitsNeeded(int paletteSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int readPackedValue(int index)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void writePackedValue(int index, int value)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void expandBits(int newBits)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int readPackedValueFrom(long[] data, int bits, int index)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int getCellIndex(int packedXYZ)` — Returns current state/value.
- `private void convertToDirect()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void setBlockByIndex(int index, short blockId)` — Mutates internal state for this object.
- `private void collapse()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void dumpInteriorBlocks(short airBlockId)` — Performs class-specific logic; see call sites and owning manager flow.
- `public short getBlock(int packedXYZ)` — Returns current state/value.
- `public void setBlock(int packedXYZ, short blockId)` — Mutates internal state for this object.
- `public short getBlock(int x, int y, int z)` — Returns current state/value.
- `public void setBlock(int x, int y, int z, short blockId)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/block/BlockRotationType.java`

**Type:** `enum BlockRotationType`
  
**Package:** `program.bootstrap.worldpipeline.block`
  
**File size:** 17 lines

**What this class does:** `BlockRotationType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.block`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/BlockManager.java`

**Type:** `class BlockManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 82 lines

**What this class does:** `BlockManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.ManagerPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addBlock(BlockHandle blockHandle)` — Registers a child object into manager-owned collections.
- `public void request(String blockName)` — Triggers on-demand loading or lookup.
- `public boolean hasBlock(String blockName)` — Boolean existence/availability check.
- `public int getBlockIDFromBlockName(String blockName)` — Returns current state/value.
- `public BlockHandle getBlockHandleFromBlockID(int blockID)` — Returns current state/value.
- `public BlockHandle getBlockHandleFromBlockName(String blockName)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalBufferSystem.java`

**Type:** `class InternalBufferSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 59 lines

**What this class does:** `InternalBufferSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.core.engine.SystemPackage`
- `program.core.util.mathematics.extras.Direction3Vector`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `private void pushBlockOrientationMap()` — Queues data for downstream systems (often render queues).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 161 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.itempipeline.tooltypemanager.ToolTypeManager`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.texturemanager.TextureManager`
- `program.bootstrap.worldpipeline.block.BlockData`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.block.BlockRotationType`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package ObjectArrayList<BlockHandle> build(File file, File root)` — Constructs derived runtime/handle data from source input.
- `private BlockHandle parseBlock(JsonObject blockJson, String pathPrefix)` — Performs class-specific logic; see call sites and owning manager flow.
- `private DynamicGeometryType parseBlockType(String typeStr)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/blockmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.blockmanager`
  
**File size:** 117 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.blockmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void scan()` — Discovers files/resources for later load.
- `private void preRegisterBlockNames(File file, String resourceName)` — Performs class-specific logic; see call sites and owning manager flow.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String blockName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkData.java`

**Type:** `enum ChunkData`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 88 lines

**What this class does:** `ChunkData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`

**Method intent:**
- `package  ChunkData(boolean dumpable, GridSlotDetailLevel minimumLevel, String[] requiresNames, String[] leadsToNames)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void link()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkDataSyncContainer.java`

**Type:** `class ChunkDataSyncContainer`
  
**Inheritance/implements:** `extends SyncContainerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 100 lines

**What this class does:** `ChunkDataSyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.core.engine.SyncContainerPackage`

**Method intent:**
- `public void create()` — Allocates/initializes child systems or resources.
- `public void resetData()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean beginWork(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean beginWorkLocked(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void endWork(int workType)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean[] getData()` — Returns current state/value.
- `public boolean hasData(ChunkData dataType)` — Boolean existence/availability check.
- `public boolean setData(ChunkData dataType, boolean value)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkDataUtility.java`

**Type:** `class ChunkDataUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 140 lines

**What this class does:** `ChunkDataUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `public ChunkData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`.
- `private boolean isNeeded(ChunkData stage, boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isDirectlyRequired(ChunkData stage, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean requiresMet(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ChunkData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`, `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`.
- `private boolean leadsToSafe(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void cascadeClear(ChunkData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkInstance.java`

**Type:** `class ChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 149 lines

**What this class does:** `ChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long coordinate, VAOHandle vaoHandle, short airBlockId, short defaultBiomeId, Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean merge()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ChunkDataSyncContainer getChunkDataSyncContainer()` — Returns current state/value.
- `public SubChunkInstance[] getSubChunks()` — Returns current state/value.
- `public SubChunkInstance getSubChunk(int subChunkCoordinate)` — Returns current state/value.
- `public ChunkNeighborStruct getChunkNeighbors()` — Returns current state/value.
- `public WorldItemInstancePaletteHandle getWorldItemInstancePaletteHandle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunk/ChunkNeighborStruct.java`

**Type:** `class ChunkNeighborStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunk`
  
**File size:** 50 lines

**What this class does:** `ChunkNeighborStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `package public ChunkNeighborStruct(long chunkCoordinate, ChunkInstance chunkInstance, Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks)` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getNeighborCoordinate(int direction2VectorIndex)` — Returns current state/value.
- `public ChunkInstance getNeighborChunk(int direction2VectorIndex)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/AssessmentBranch.java`

**Type:** `class AssessmentBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 34 lines

**What this class does:** `AssessmentBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkNeighborStruct`
- `program.core.engine.BranchPackage`
- `program.core.util.mathematics.extras.Direction2Vector`

**Method intent:**
- `public void assessChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/BatchBranch.java`

**Type:** `class BatchBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 36 lines

**What this class does:** `BatchBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.megastreammanager.MegaStreamManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/BuildBranch.java`

**Type:** `class BuildBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 55 lines

**What this class does:** `BuildBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void buildChunk(ChunkInstance chunkInstance)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueItem.java`

**Type:** `enum ChunkQueueItem`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 13 lines

**What this class does:** `ChunkQueueItem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkQueueManager.java`

**Type:** `class ChunkQueueManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 396 lines

**What this class does:** `ChunkQueueManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkDataUtility`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.queue.QueueInstance`
- `program.core.util.queue.QueueItemHandle`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void executeQueue()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void scanGridSlots(GridInstance grid)` — Discovers files/resources for later load.
- `private void loadQueue(GridInstance grid)` — Parses external data into engine objects.
- `private void assessActiveChunks(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flushActiveChunks(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private QueueOperation determineQueueOperation(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private QueueOperation toOperation(ChunkData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean reserveAsyncWork(ChunkDataSyncContainer syncContainer, QueueOperation operation)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void invalidateChunkBatch(ChunkInstance chunk)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkStreamManager.java`

**Type:** `class ChunkStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 82 lines

**What this class does:** `ChunkStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void start()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateChunkBatch(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public VAOHandle getChunkVAO()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/DumpBranch.java`

**Type:** `class DumpBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 121 lines

**What this class does:** `DumpBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkDataUtility`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `public void dumpChunkData(ChunkInstance chunkInstance, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void executeDump(ChunkInstance chunkInstance, ChunkData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpGenerationData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpBuildData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpMergeData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpRenderData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpItemData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void dumpItemRenderData(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/GenerationBranch.java`

**Type:** `class GenerationBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 101 lines

**What this class does:** `GenerationBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worldgenerationmanager.WorldGenerationManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void getNewChunk(ChunkInstance chunkInstance)` — Returns current state/value.
- `private boolean loadChunk(ChunkInstance chunkInstance, ChunkDataSyncContainer container)` — Parses external data into engine objects.
- `private void generateChunk(ChunkInstance chunkInstance, ChunkDataSyncContainer container)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ItemLoadBranch.java`

**Type:** `class ItemLoadBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 58 lines

**What this class does:** `ItemLoadBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void loadItems(ChunkInstance chunkInstance)` — Parses external data into engine objects.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ItemRenderBranch.java`

**Type:** `class ItemRenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 53 lines

**What this class does:** `ItemRenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worlditemplacementsystem.WorldItemPlacementSystem`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderItems(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/MergeBranch.java`

**Type:** `class MergeBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 52 lines

**What this class does:** `MergeBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.BranchPackage`
- `program.core.kernel.thread.ThreadHandle`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void mergeChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/QueueOperation.java`

**Type:** `enum QueueOperation`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 21 lines

**What this class does:** `QueueOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/chunkstreammanager/RenderBranch.java`

**Type:** `class RenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.chunkstreammanager`
  
**File size:** 49 lines

**What this class does:** `RenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.chunkstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/grid/GridInstance.java`

**Type:** `class GridInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.grid`
  
**File size:** 269 lines

**What this class does:** `GridInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.grid`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.util.WorldWrapUtility`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.core.engine.InstancePackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `public void constructor(EntityInstance focalEntity, WindowInstance windowInstance, int totalSlots, long[] loadOrder, LongOpenHashSet gridCoordinates, Long2ObjectOpenHashMap<GridSlotHandle> gridSlots, float radiusSquared, int maxChunks)` — Engine-side initialization entrypoint invoked post-create.
- `private void rebuildRenderQueue()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void queueChunk(GridSlotHandle slot, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void queueMega(GridSlotHandle slot, long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean updateActiveChunkCoordinate()` — Runs frame-step maintenance and logic.
- `public long getActiveChunkCoordinate()` — Returns current state/value.
- `public GridSlotHandle getNextScanSlot()` — Returns current state/value.
- `public long getChunkCoordinateForSlot(long gridCoordinate)` — Returns current state/value.
- `public long getMegaCoordinateForSlot(long gridCoordinate)` — Returns current state/value.
- `public GridSlotHandle getGridSlotForChunk(long chunkCoordinate)` — Returns current state/value.
- `public EntityInstance getFocalEntity()` — Returns current state/value.
- `public WindowInstance getWindowInstance()` — Returns current state/value.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public int getTotalSlots()` — Returns current state/value.
- `public long[] getLoadOrder()` — Returns current state/value.
- `public long getGridCoordinate(int i)` — Returns current state/value.
- `public LongOpenHashSet getGridCoordinates()` — Returns current state/value.
- `public GridSlotHandle getGridSlot(long gridCoordinate)` — Returns current state/value.
- `public float getRadiusSquared()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<ChunkInstance> getActiveChunks()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<MegaChunkInstance> getActiveMegaChunks()` — Returns current state/value.
- `public LongLinkedOpenHashSet getLoadRequests()` — Returns current state/value.
- `public LongLinkedOpenHashSet getUnloadRequests()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getChunkRenderQueue()` — Returns current state/value.
- `public Long2ObjectLinkedOpenHashMap<GridSlotHandle> getMegaRenderQueue()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridmanager/GridBuildSystem.java`

**Type:** `class GridBuildSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.gridmanager`
  
**File size:** 235 lines

**What this class does:** `GridBuildSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.shaderpipeline.ubo.UBOHandle`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.shaderpipeline.ubomanager.UBOManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Constructs derived runtime/handle data from source input.
- `private float calculateRadius()` — Performs class-specific logic; see call sites and owning manager flow.
- `private long[] assignLoadOrder(float radius)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Long2ObjectOpenHashMap<GridSlotHandle> createGridSlotHandles(LongOpenHashSet gridCoordinates, GridInstance gridInstance)` — Allocates/initializes child systems or resources.
- `private GridSlotHandle createGridSlotHandle(long gridCoordinate, UBOInstance slotUBO, float chunkDistanceFromCenter, float chunkAngleFromCenter, float megaDistanceFromCenter, float megaAngleFromCenter, GridSlotDetailLevel detailLevel, GridInstance gridInstance)` — Allocates/initializes child systems or resources.
- `private void populateCoveredSlots(Long2ObjectOpenHashMap<GridSlotHandle> gridSlots)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridmanager/GridManager.java`

**Type:** `class GridManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.gridmanager`
  
**File size:** 30 lines

**What this class does:** `GridManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public GridInstance buildGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Constructs derived runtime/handle data from source input.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridslot/GridSlotDetailLevel.java`

**Type:** `enum GridSlotDetailLevel`
  
**Package:** `program.bootstrap.worldpipeline.gridslot`
  
**File size:** 35 lines

**What this class does:** `GridSlotDetailLevel` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridslot`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`

**Method intent:**
- `package  GridSlotDetailLevel(int level, int maxChunkDistance, RenderType renderMode)` — Performs class-specific logic; see call sites and owning manager flow.
- `public GridSlotDetailLevel getDetailLevelForDistance(float absoluteChunkDistance)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/worldpipeline/gridmanager/GridBuildSystem.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/gridslot/GridSlotHandle.java`

**Type:** `class GridSlotHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.gridslot`
  
**File size:** 101 lines

**What this class does:** `GridSlotHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.gridslot`.

**Who this class talks to (direct imports):**
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `public void constructor(long gridCoordinate, UBOInstance slotUBO, float chunkDistanceFromCenter, float chunkAngleFromCenter, float megaDistanceFromCenter, float megaAngleFromCenter, GridSlotDetailLevel detailLevel, GridInstance gridInstance)` — Engine-side initialization entrypoint invoked post-create.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public long getMegaCoordinate()` — Returns current state/value.
- `public long getGridCoordinate()` — Returns current state/value.
- `public UBOInstance getSlotUBO()` — Returns current state/value.
- `public float getChunkDistanceFromCenter()` — Returns current state/value.
- `public float getChunkAngleFromCenter()` — Returns current state/value.
- `public float getMegaDistanceFromCenter()` — Returns current state/value.
- `public float getMegaAngleFromCenter()` — Returns current state/value.
- `public GridSlotDetailLevel getDetailLevel()` — Returns current state/value.
- `public ObjectArrayList<GridSlotHandle> getCoveredSlots()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaBatchStruct.java`

**Type:** `class MegaBatchStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 109 lines

**What this class does:** `MegaBatchStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `package public MegaBatchStruct()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void constructor(long megaChunkCoordinate, int megaScale)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean registerChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void updateChunk(long coord, ChunkInstance chunkInstance)` — Runs frame-step maintenance and logic.
- `public void recordMerged(long coordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void clearMerged()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isReadyToRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public long getMegaChunkCoordinate()` — Returns current state/value.
- `public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks()` — Returns current state/value.
- `public ObjectArrayList<ChunkInstance> getBatchedChunkList()` — Returns current state/value.
- `public ChunkInstance getBatchedChunk(long chunkCoordinate)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaChunkInstance.java`

**Type:** `class MegaChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 177 lines

**What this class does:** `MegaChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long megaChunkCoordinate, VAOHandle vaoHandle, int megaScale)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean batchAndMerge(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean mergeChunk(ChunkInstance chunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void finalizeGeometry()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MegaDataSyncContainer getMegaDataSyncContainer()` — Returns current state/value.
- `public boolean isReadyToRender()` — Performs class-specific logic; see call sites and owning manager flow.
- `public Long2ObjectOpenHashMap<ChunkInstance> getBatchedChunks()` — Returns current state/value.
- `public ObjectArrayList<ChunkInstance> getBatchedChunkList()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaData.java`

**Type:** `enum MegaData`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 68 lines

**What this class does:** `MegaData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`

**Method intent:**
- `package  MegaData(boolean dumpable, GridSlotDetailLevel maximumLevel, String[] requiresNames, String[] leadsToNames)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void link()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaDataSyncContainer.java`

**Type:** `class MegaDataSyncContainer`
  
**Inheritance/implements:** `extends SyncContainerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 61 lines

**What this class does:** `MegaDataSyncContainer` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.core.engine.SyncContainerPackage`

**Method intent:**
- `public void create()` — Allocates/initializes child systems or resources.
- `public void resetData()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean[] getData()` — Returns current state/value.
- `public boolean hasData(MegaData dataType)` — Boolean existence/availability check.
- `public boolean setData(MegaData dataType, boolean value)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megachunk/MegaDataUtility.java`

**Type:** `class MegaDataUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.megachunk`
  
**File size:** 120 lines

**What this class does:** `MegaDataUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megachunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.core.engine.UtilityPackage`

**Method intent:**
- `public MegaData nextToLoad(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`.
- `private boolean isNeeded(MegaData stage, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean requiresMet(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public MegaData nextToDump(boolean[] flags, GridSlotDetailLevel slotLevel)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`.
- `private boolean leadsToSafe(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void cascadeClear(MegaData stage, boolean[] flags)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaAssessBranch.java`

**Type:** `class MegaAssessBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 32 lines

**What this class does:** `MegaAssessBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.core.engine.BranchPackage`

**Method intent:**
- `public void assessMega(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaDumpBranch.java`

**Type:** `class MegaDumpBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 82 lines

**What this class does:** `MegaDumpBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void dumpMega(MegaChunkInstance mega, MegaDataSyncContainer sync, long megaCoord)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void clearChunkBatchFlags(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaMergeBranch.java`

**Type:** `class MegaMergeBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 52 lines

**What this class does:** `MegaMergeBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void mergeChunkIntoMega(ChunkInstance chunkInstance, MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueManager.java`

**Type:** `class MegaQueueManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 351 lines

**What this class does:** `MegaQueueManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotDetailLevel`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.megachunk.MegaDataUtility`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `package void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private int computeMegaMax(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flushActiveMegas(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaChunkInstance createMega(long megaCoord, GridInstance grid, int megaMax, Long2ObjectLinkedOpenHashMap<MegaChunkInstance> activeMegaChunks)` — Allocates/initializes child systems or resources.
- `private MegaChunkInstance configureMega(MegaChunkInstance mega, long megaCoord, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void assessActiveMegas(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaQueueOperation determineOperation(MegaDataSyncContainer sync, GridSlotHandle gridSlotHandle)` — Performs class-specific logic; see call sites and owning manager flow.
- `private MegaQueueOperation toOperation(MegaData stage)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void unloadMega(MegaChunkInstance mega, long megaCoord, int megaMax)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void clearChunkBatchFlags(MegaChunkInstance mega)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaQueueOperation.java`

**Type:** `enum MegaQueueOperation`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 14 lines

**What this class does:** `MegaQueueOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaRenderBranch.java`

**Type:** `class MegaRenderBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 81 lines

**What this class does:** `MegaRenderBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkData`
- `program.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaChunkInstance`
- `program.bootstrap.worldpipeline.megachunk.MegaData`
- `program.bootstrap.worldpipeline.megachunk.MegaDataSyncContainer`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.engine.BranchPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void renderMega(MegaChunkInstance mega, MegaDataSyncContainer sync)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/megastreammanager/MegaStreamManager.java`

**Type:** `class MegaStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.megastreammanager`
  
**File size:** 44 lines

**What this class does:** `MegaStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.megastreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void onGridRebuilt(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void onGridRemoved(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void batchChunk(ChunkInstance chunkInstance, GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/subchunk/SubChunkInstance.java`

**Type:** `class SubChunkInstance`
  
**Inheritance/implements:** `extends WorldRenderInstance`
  
**Package:** `program.bootstrap.worldpipeline.subchunk`
  
**File size:** 107 lines

**What this class does:** `SubChunkInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.subchunk`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle`
- `program.bootstrap.worldpipeline.worldrendermanager.RenderType`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderInstance`
- `program.bootstrap.worldpipeline.worldrendermanager.WorldRenderManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderManager, WorldHandle worldHandle, long coordinate, VAOHandle vaoHandle, short airBlockId, short defaultBiomeId)` — Engine-side initialization entrypoint invoked post-create.
- `public void reset()` — Performs class-specific logic; see call sites and owning manager flow.
- `public BlockPaletteHandle getBiomePaletteHandle()` — Returns current state/value.
- `public BlockPaletteHandle getBlockPaletteHandle()` — Returns current state/value.
- `public BlockPaletteHandle getBlockRotationPaletteHandle()` — Returns current state/value.
- `public WorldItemPaletteHandle getWorldItemPaletteHandle()` — Returns current state/value.
- `public short getBlock(int x, int y, int z)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/ChunkCoordinate3Int.java`

**Type:** `class ChunkCoordinate3Int`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 236 lines

**What this class does:** `ChunkCoordinate3Int` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Direction3Vector`

**Method intent:**
- `package private ChunkCoordinate3Int()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flattenBlockCoordinates()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void flattenInteriorBlockCoordinates()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int[] getBlockCoordinates()` — Returns current state/value.
- `public int getBlockCoordinate(int index)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/InternalBuildManager.java`.
- `public int[] getInteriorBlockCoordinates()` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/worldpipeline/block/BlockPaletteHandle.java`.
- `public int getInteriorBlockCoordinate(int index)` — Returns current state/value.
- `public int getIndex(int packed)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int convertToBlockSpace(int vertPacked, VertBlockNeighbor3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighbor(int packed, Direction3Vector direction)` — Returns current state/value.
- `public int getNeighborAndWrap(int packed, Direction3Vector direction)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighborWithOffset(int packed, Direction3Vector tangent, int offset)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public boolean isAtEdge(int packed, Direction3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public boolean isAtEdge(int packed, VertBlockNeighbor3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int convertToVertSpace(int packed, Direction3Vector direction)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getNeighborFromVert(int vertPacked, VertBlockNeighbor3Vector neighbor)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.
- `public int getVertCoordinateFromOffset(int vertPacked, Direction3Vector direction, int offset)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/geometrypipeline/dynamicgeometrymanager/FullGeometryBranch.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldPositionStruct.java`

**Type:** `class WorldPositionStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 31 lines

**What this class does:** `WorldPositionStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public Vector3 getPosition()` — Returns current state/value.
- `public void setPosition(Vector3 position)` — Mutates internal state for this object.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public void setChunkCoordinate(long chunkCoordinate)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldPositionUtility.java`

**Type:** `class WorldPositionUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 98 lines

**What this class does:** `WorldPositionUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryType`
- `program.bootstrap.worldpipeline.block.BlockHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2Int`

**Method intent:**
- `public long getRandomChunk(WorldHandle worldHandle)` — Returns current state/value. Called via static reference from: `core/src/program/bootstrap/entitypipeline/entitymanager/EntityManager.java`.
- `public int findSafeSpawnHeight(ChunkInstance chunkInstance, BlockManager blockManager, int blockX, int totalY, int blockZ)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/entitypipeline/playermanager/PlayerManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/util/WorldWrapUtility.java`

**Type:** `class WorldWrapUtility`
  
**Inheritance/implements:** `extends UtilityPackage`
  
**Package:** `program.bootstrap.worldpipeline.util`
  
**File size:** 47 lines

**What this class does:** `WorldWrapUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.UtilityPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public Vector3 wrapAroundChunk(Vector3 input)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`.
- `public long wrapAroundWorld(WorldHandle worldHandle, long input)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/physicspipeline/movementmanager/MovementManager.java`, `core/src/program/bootstrap/worldpipeline/chunk/ChunkNeighborStruct.java`, `core/src/program/bootstrap/worldpipeline/grid/GridInstance.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/world/WorldData.java`

**Type:** `class WorldData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.worldpipeline.world`
  
**File size:** 101 lines

**What this class does:** `WorldData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.world`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.core.engine.DataPackage`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package public WorldData(String worldName, int worldID, Pixmap world, Vector2Int worldScale, float gravityMultiplier, Vector3 gravityDirection, float daysPerDay, String calendarName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getWorldName()` — Returns current state/value.
- `public int getWorldID()` — Returns current state/value.
- `public Pixmap getWorld()` — Returns current state/value.
- `public Vector2Int getWorldScale()` — Returns current state/value.
- `public float getGravityMultiplier()` — Returns current state/value.
- `public Vector3 getGravityDirection()` — Returns current state/value.
- `public float getDaysPerDay()` — Returns current state/value.
- `public String getCalendarName()` — Returns current state/value.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/world/WorldHandle.java`

**Type:** `class WorldHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.world`
  
**File size:** 70 lines

**What this class does:** `WorldHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.world`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.core.engine.HandlePackage`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `public void constructor(WorldData data)` — Engine-side initialization entrypoint invoked post-create.
- `public WorldData getWorldData()` — Returns current state/value.
- `public String getWorldName()` — Returns current state/value.
- `public int getWorldID()` — Returns current state/value.
- `public Pixmap getWorld()` — Returns current state/value.
- `public Vector2Int getWorldScale()` — Returns current state/value.
- `public float getGravityMultiplier()` — Returns current state/value.
- `public Vector3 getGravityDirection()` — Returns current state/value.
- `public float getDaysPerDay()` — Returns current state/value.
- `public String getCalendarName()` — Returns current state/value.
- `public long getWorldEpochStart()` — Returns current state/value.
- `public void setWorldEpochStart(long worldEpochStart)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldgenerationmanager/WorldGenerationManager.java`

**Type:** `class WorldGenerationManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldgenerationmanager`
  
**File size:** 109 lines

**What this class does:** `WorldGenerationManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldgenerationmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.biomemanager.BiomeManager`
- `program.bootstrap.worldpipeline.block.BlockPaletteHandle`
- `program.bootstrap.worldpipeline.blockmanager.BlockManager`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.OpenSimplex2`
- `program.core.util.mathematics.extras.Coordinate2Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `public void setSeed(long seed)` — Mutates internal state for this object.
- `public long getSeed()` — Returns current state/value.
- `public boolean generateSubChunk(long chunkCoordinate, SubChunkInstance subChunkInstance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemCompositeInstance.java`

**Type:** `class WorldItemCompositeInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 34 lines

**What this class does:** `WorldItemCompositeInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(MaterialInstance material, CompositeBufferInstance compositeBuffer)` — Engine-side initialization entrypoint invoked post-create.
- `public MaterialInstance getMaterial()` — Returns current state/value.
- `public CompositeBufferInstance getCompositeBuffer()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemInstance.java`

**Type:** `class WorldItemInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 67 lines

**What this class does:** `WorldItemInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `public void constructor(ItemDefinitionHandle itemDefinitionHandle, long chunkCoordinate, int packedBlockCoordinate, long packedPosition, int packedItem)` — Engine-side initialization entrypoint invoked post-create.
- `public ItemDefinitionHandle getItemDefinitionHandle()` — Returns current state/value.
- `public long getChunkCoordinate()` — Returns current state/value.
- `public int getPackedBlockCoordinate()` — Returns current state/value.
- `public long getPackedPosition()` — Returns current state/value.
- `public int getPackedItem()` — Returns current state/value.
- `public int getInstanceSlot()` — Returns current state/value.
- `public void setInstanceSlot(int slot)` — Mutates internal state for this object.
- `public void clearInstanceSlot()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemInstancePaletteHandle.java`

**Type:** `class WorldItemInstancePaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 73 lines

**What this class does:** `WorldItemInstancePaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `public void addItem(WorldItemInstance item)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemInstance item)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<WorldItemInstance> getItems()` — Returns current state/value.
- `public ObjectArrayList<WorldItemInstance> getItemsAtBlock(int packedBlockCoordinate)` — Returns current state/value.
- `public boolean hasItemsAtBlock(int packedBlockCoordinate)` — Boolean existence/availability check.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemPaletteHandle.java`

**Type:** `class WorldItemPaletteHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 43 lines

**What this class does:** `WorldItemPaletteHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor()` — Engine-side initialization entrypoint invoked post-create.
- `public void addItem(WorldItemStruct item)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemStruct item)` — Unregisters and tears down child references.
- `public void clear()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<WorldItemStruct> getItems()` — Returns current state/value.
- `public boolean isEmpty()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int size()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditem/WorldItemStruct.java`

**Type:** `class WorldItemStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditem`
  
**File size:** 14 lines

**What this class does:** `WorldItemStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditem`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public WorldItemStruct(long packedPosition, int packedItem)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditemplacementsystem/WorldItemPlacementSystem.java`

**Type:** `class WorldItemPlacementSystem`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditemplacementsystem`
  
**File size:** 170 lines

**What this class does:** `WorldItemPlacementSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditemplacementsystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.subchunk.SubChunkInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle`
- `program.bootstrap.worldpipeline.worlditem.WorldItemStruct`
- `program.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate3Int`
- `program.core.util.mathematics.extras.Coordinate4Long`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void buildChunkInstances(ChunkInstance chunk, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `public void pushChunkToRenderer(ChunkInstance chunk, long chunkCoordinate)` — Queues data for downstream systems (often render queues).
- `public void pullChunkFromRenderer(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public WorldItemInstance placeItem(ChunkInstance chunk, int subChunkCoordinate, long packedPosition, int packedItem, ItemDefinitionHandle def)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void removeItem(ChunkInstance chunk, WorldItemInstance instance)` — Unregisters and tears down child references.
- `private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate)` — Constructs derived runtime/handle data from source input.
- `private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate, ItemDefinitionHandle def)` — Constructs derived runtime/handle data from source input.
- `private void removeMatchingStruct(SubChunkInstance subChunk, long packedPosition, int packedItem)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worlditemrendersystem/WorldItemRenderSystem.java`

**Type:** `class WorldItemRenderSystem`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worlditemrendersystem`
  
**File size:** 214 lines

**What this class does:** `WorldItemRenderSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worlditemrendersystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance`
- `program.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager`
- `program.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.worldpipeline.worlditem.WorldItemCompositeInstance`
- `program.bootstrap.worldpipeline.worlditem.WorldItemInstance`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.kernel.windowmanager.WindowManager`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.extras.Coordinate2Long`
- `program.core.util.mathematics.extras.Coordinate4Long`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `public void push(long chunkCoordinate, ObjectArrayList<WorldItemInstance> items)` — Queues data for downstream systems (often render queues).
- `public void pull(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addItem(WorldItemInstance instance, long chunkCoordinate)` — Registers a child object into manager-owned collections.
- `public void removeItem(WorldItemInstance instance)` — Unregisters and tears down child references.
- `private void addToBuffer(WorldItemInstance instance, int chunkX, int chunkZ)` — Registers a child object into manager-owned collections.
- `private void removeFromBuffer(WorldItemInstance instance)` — Unregisters and tears down child references.
- `private WorldItemCompositeInstance getOrCreateComposite(ItemDefinitionHandle def)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 98 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.core.util.image.Pixmap`
- `program.bootstrap.worldpipeline.world.WorldData`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`
- `program.core.util.JsonUtility`
- `program.core.util.RegistryUtility`
- `program.core.util.mathematics.vectors.Vector2Int`
- `program.core.util.mathematics.vectors.Vector3`

**Method intent:**
- `package WorldHandle build(File file, File root, String worldName)` — Constructs derived runtime/handle data from source input.
- `private File resolveCompanionJson(File pngFile)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector2Int calculateWorldScale(Pixmap pixmap)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 87 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String worldName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldmanager/WorldManager.java`

**Type:** `class WorldManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldmanager`
  
**File size:** 96 lines

**What this class does:** `WorldManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `package void addWorld(String worldName, WorldHandle worldHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasWorld(String worldName)` — Boolean existence/availability check.
- `public int getWorldIDFromWorldName(String worldName)` — Returns current state/value.
- `public WorldHandle getWorldHandleFromWorldID(int worldID)` — Returns current state/value.
- `public WorldHandle getWorldHandleFromWorldName(String worldName)` — Returns current state/value.
- `public WorldHandle getActiveWorld()` — Returns current state/value.
- `public void setActiveWorld(String worldName)` — Mutates internal state for this object.
- `public void request(String worldName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/FrustumCullingSystem.java`

**Type:** `class FrustumCullingSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 147 lines

**What this class does:** `FrustumCullingSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.core.engine.SystemPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`
- `program.core.util.camera.CameraInstance`

**Method intent:**
- `protected void awake()` — Runs startup-time runtime activation work.
- `package void refresh(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean isChunkVisible(GridSlotHandle slot)` — Performs class-specific logic; see call sites and owning manager flow.
- `package boolean isMegaVisible(GridSlotHandle slot)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isWithinAngle(float slotAngle, float tolerance)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float getCameraAngle(CameraInstance camera)` — Returns current state/value.
- `private float getDiagonalHalfFov(CameraInstance camera)` — Returns current state/value.
- `private float getAbsPitch(CameraInstance camera)` — Returns current state/value.
- `private float getPitchT(float absPitch, float power)` — Returns current state/value.
- `private float getEffectiveHalfAngle(float halfFov, float t)` — Returns current state/value.
- `private float getPitchMaxDistanceSq(float t)` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/RenderOperation.java`

**Type:** `enum RenderOperation`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 15 lines

**What this class does:** `RenderOperation` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/RenderType.java`

**Type:** `enum RenderType`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 15 lines

**What this class does:** `RenderType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/WorldRenderInstance.java`

**Type:** `class WorldRenderInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 73 lines

**What this class does:** `WorldRenderInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.InstancePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(WorldRenderManager worldRenderSystem, WorldHandle worldHandle, RenderType renderType, long coordinate, VAOHandle vaoHandle)` — Engine-side initialization entrypoint invoked post-create.
- `public void dispose()` — Releases owned resources and unregisters state.
- `public WorldHandle getWorldHandle()` — Returns current state/value.
- `public long getCoordinate()` — Returns current state/value.
- `public DynamicPacketInstance getDynamicPacketInstance()` — Returns current state/value.
- `protected DynamicPacketInstance getDynamicPacket()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldrendermanager/WorldRenderManager.java`

**Type:** `class WorldRenderManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldrendermanager`
  
**File size:** 277 lines

**What this class does:** `WorldRenderManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldrendermanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance`
- `program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketState`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.shaderpipeline.ubo.UBOInstance`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridslot.GridSlotHandle`
- `program.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`
- `program.core.settings.EngineSetting`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void lateUpdate()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderWorld()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderGridMegas(GridInstance grid, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderGridChunks(GridInstance grid, WindowInstance window)` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean addChunkInstance(WorldRenderInstance worldRenderInstance)` — Registers a child object into manager-owned collections.
- `public boolean addMegaInstance(WorldRenderInstance worldRenderInstance)` — Registers a child object into manager-owned collections.
- `private boolean hasGridSlotForChunk(long coordinate)` — Boolean existence/availability check.
- `private ObjectArrayList<ModelInstance> buildModelList(WorldRenderInstance worldRenderInstance)` — Constructs derived runtime/handle data from source input.
- `public void removeChunkInstance(long coordinate)` — Unregisters and tears down child references.
- `public void removeMegaInstance(long coordinate)` — Unregisters and tears down child references.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/worldpipeline/worldstreammanager/WorldStreamManager.java`

**Type:** `class WorldStreamManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.worldpipeline.worldstreammanager`
  
**File size:** 121 lines

**What this class does:** `WorldStreamManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.worldpipeline.worldstreammanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.worldpipeline.chunk.ChunkInstance`
- `program.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager`
- `program.bootstrap.worldpipeline.grid.GridInstance`
- `program.bootstrap.worldpipeline.gridmanager.GridManager`
- `program.bootstrap.worldpipeline.megastreammanager.MegaStreamManager`
- `program.bootstrap.worldpipeline.world.WorldHandle`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic. Called via static reference from: `core/src/program/bootstrap/worldpipeline/chunkstreammanager/ChunkStreamManager.java`.
- `public GridInstance createGrid(EntityInstance focalEntity, WindowInstance windowInstance)` — Allocates/initializes child systems or resources.
- `public void removeGrid(GridInstance grid)` — Unregisters and tears down child references.
- `public void rebuildGrid(GridInstance grid)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateChunkBatch(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void invalidateMegaForChunk(long chunkCoordinate)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<GridInstance> getGrids()` — Returns current state/value.
- `public boolean hasGrids()` — Boolean existence/availability check.
- `public ChunkInstance getChunkInstance(long chunkCoordinate)` — Returns current state/value.
- `public WorldHandle getActiveWorldHandle()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
