# EngineCoreDoc

_Generated/updated: 2026-04-03_

## Purpose

This document explains how the **engine actually runs** from launch to shutdown, and how core systems communicate.
It is intentionally architecture-first (not class-list-first) so someone new can trace runtime behavior quickly.

---

## 1) High-level execution model

The concrete engine (`GameEngine`) extends `EnginePackage` and provides three things:

1. **Bootstrap graph** (`bootstrap()`): creates `BootstrapAssembly`.
2. **Core runtime references** (`get()`): resolves `WindowManager` and `RenderManager`.
3. **Main runtime context** (`awake()`): creates a `RuntimeContext` and pairs it with the main window.
4. **Frame flush** (`draw()`): delegates final rendering to `RenderManager.draw()`.

In short:

`GameEngine` = wiring + root context creation + draw delegation.

---

## 2) What EnginePackage owns

`EnginePackage` is the root orchestrator. It owns:

- Global system registry (`internalRegistry`)
- Lifecycle/context enforcement (`SystemContext` checks)
- Context pairing (`createContext`, `destroyContext`)
- Engine state progression (`EngineState` transitions)
- Frame timing and fixed-step scheduling
- Thread/window service access via manager retrieval

### Important creation constraint

`EnginePackage` cannot be constructed directly.
It requires `setupConstructor(settings, game, path, gson, windowPlatform)` first, which stages an `EngineStruct` through `ThreadLocal` to inject constructor-time dependencies safely.

---

## 3) Lifecycle phases in practice

This is the practical order to reason about behavior:

1. **KERNEL setup**: root internals are initialized.
2. **BOOTSTRAP**: `GameEngine.bootstrap()` creates `BootstrapAssembly`.
3. **CREATE / GET / AWAKE** propagate through registered systems.
4. **UPDATE loop** runs continuously.
5. **DRAW** flushes queued render work.
6. **EXIT/DISPOSE** tears down contexts and systems.

### Why `get()` matters

Cross-system references are resolved in `get()` so creation order and dependency resolution remain deterministic.
This prevents hidden late-bound lookups and keeps startup failures obvious.

---

## 4) Assembly and pipeline relationship

`BootstrapAssembly` creates all pipelines in dependency order:

1. Geometry
2. Shader
3. Render
4. Item
5. Physics
6. Input
7. Entity
8. World
9. Calendar
10. Lighting
11. Menu

This order defines what can be safely retrieved in downstream `get()` calls.

---

## 5) Context + window model

A `ContextPackage` is not a framebuffer descriptor; it is a **runtime system bundle bound to a window**.

- `EnginePackage.createContext(ContextClass, WindowInstance)` pairs one context to one window.
- A window cannot have two contexts at once.
- Runtime systems use `context.getWindow()` so the same systems can run on different windows.

This is what allows game/editor/multi-window behavior without forking every runtime system.

---

## 6) Core communication paths

### Rendering
- Runtime systems push data/passes during update/awake.
- `RenderManager` performs final flush in engine draw path.
- Detached windows flush on their own listener render callback after push phase.

### Windowing
- `WindowManager` owns main + detached windows.
- Main window is registered and opened immediately.
- Detached windows are queued (`pendingOpen`) and platform-opened during update.

### Threading
- `InternalThreadManager` owns named executors loaded from definitions.
- Async/sync container execution helpers centralize lock/reset behavior.

---

## 7) Ground rules that prevent engine drift

- Use `create(Class)` for engine-managed types (`InstancePackage` and above).
- Use `new` only for pure data/struct objects.
- Resolve cross-system refs in `get()`.
- Keep manager API explicit (`getXFromY` style) rather than generic maps exposed everywhere.
- Treat runtime contexts as the unit of per-window behavior.

---

## 8) If you are debugging startup

Check in this order:

1. `setupConstructor(...)` called before engine instantiation
2. `BootstrapAssembly` created in `GameEngine.bootstrap()`
3. Window registration happened before `createContext(...)`
4. Missing system retrievals are failing in `get()` (expected, preferred)
5. Render queue reaches `RenderManager.draw()`

This order catches most "engine starts but nothing happens" failures.
