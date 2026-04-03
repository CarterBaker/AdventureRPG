# KernelSystemsDoc

_Updated: 2026-04-03_

## Purpose

Kernel systems provide the low-level execution substrate used by the engine:

- thread pool provisioning/execution
- async/sync execution helpers
- window lifecycle management
- per-window runtime attachment support

---

## 1) Thread subsystem

### Components
- `InternalThreadManager` (manager/facade)
- `InternalLoader` (loads thread definitions)
- `InternalBuilder` (creates `ThreadHandle`)
- `ThreadHandle` (name, size, executor, disposal)
- `NamedThreadFactory` (stable thread naming)

### Load flow
1. `InternalThreadManager.create()` creates `InternalLoader` and map.
2. `InternalLoader.scan()` walks thread definition JSON files.
3. During scan, thread names are pre-registered to resource names for on-demand request mapping.
4. `load(file)` parses thread entries and creates executors via builder.
5. Handles are registered by thread name.

### Execution flow
`InternalThreadManager` exposes overloads for:
- raw async `Runnable`
- async container consumers
- sync container consumers
- mixed async+sync consumer

Each helper centrally guarantees reset/release behavior.

---

## 2) Sync/async consumer interfaces

Functional interfaces (`AsyncStructConsumer`, `SyncStructConsumer`, etc.) define strongly-typed work signatures for thread manager helpers.

Why this matters:
- avoids ad hoc lambdas with inconsistent lock/reset handling
- keeps async/sync policy in one place (thread manager)
- reduces copy-paste mistakes in worker code

---

## 3) Window subsystem

### Components
- `WindowManager`
- `WindowInstance`
- `WindowData`

### Core model
- `WindowData` stores immutable identity + mutable dimensions.
- `WindowInstance` wraps data, render queue handle, active cameras, context attachment, native handle.
- `WindowManager` owns collection of windows, active/main window, pending detached opens.

### Lifecycle behavior
- Main window is registered and opened immediately (`registerMainWindow`).
- Detached windows are queued then opened in manager update (`registerDetachedWindow` + `pendingOpen`).
- Close detection runs in update loop for non-main windows.
- Dispose path tears down VAOs, render resources, context pairing, and platform window.

---

## 4) How kernel connects to engine runtime

- Engine uses `WindowManager` to obtain main window before creating `RuntimeContext`.
- Runtime systems route behavior via `context.getWindow()`.
- Detached windows can host contexts through the same contract.
- Thread manager provides named executor services for heavy tasks without leaking threading logic into gameplay managers.

---

## 5) Operational guardrails

- Do not create executors ad hoc in gameplay code; request handles from `InternalThreadManager`.
- Always dispose thread handles (manager already does this in its dispose).
- Keep main-window special-casing inside `WindowManager` only.
- Ensure window/context pairing is one-to-one.
- Let platform layer own actual open/destroy calls; managers should request, not reimplement backend behavior.

---

## 6) Debugging checklist

### Threading
- Missing thread by name -> verify JSON scan path and pre-registration map.
- Task silently not mutating sync data -> check `tryAcquire()` path for contention.

### Windowing
- Detached window not appearing -> check it was queued in `pendingOpen`.
- Context null on detached window -> ensure pending context type was set before listener `create()`.
- Orphaned render resources -> verify `WindowInstance.dispose()` fully executes.
