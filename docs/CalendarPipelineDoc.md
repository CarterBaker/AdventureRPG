# CalendarPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **14**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

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
