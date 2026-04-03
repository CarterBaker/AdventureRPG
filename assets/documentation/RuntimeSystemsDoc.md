# RuntimeSystemsDoc

_Updated: 2026-04-03_

## Purpose

This document explains what runs inside `RuntimeContext`, when it runs, and how those systems coordinate with bootstrap managers.

`RuntimeContext` currently creates:
- `SkySystem`
- `PlayerSystem`
- `MenuSystem`
- `WorldSystem`
- `PlayerInputSystem`

All of them are window-aware through `context.getWindow()`.

---

## 1) Runtime startup sequence

When `GameEngine.awake()` calls `createContext(RuntimeContext.class, mainWindow)`:

1. `RuntimeContext` is created and paired with the window.
2. Runtime systems are created.
3. During `awake()` phase:
   - `PlayerSystem` spawns player for that window.
   - `MenuSystem` opens main menu for that window.
   - `WorldSystem` creates streaming grid tied to that window/player.

This guarantees world/menu/player boot around the same target window.

---

## 2) Per-system responsibilities

## `PlayerSystem`
- Resolves `PlayerManager` in `get()`.
- Calls `playerManager.spawnPlayer(context.getWindow())` in `awake()`.
- Role: bootstrap runtime actor ownership per window.

## `MenuSystem`
- Resolves `MainMenuBranch` in `get()`.
- Opens menu in `awake()` with the current context window.
- Role: ensures UI is initialized against correct render target.

## `WorldSystem`
- Resolves `PlayerManager` + `WorldStreamManager`.
- Creates grid in `awake()` using player for current window.
- Role: binds stream origin and culling scope to window-specific player/camera.

## `SkySystem`
- Resolves `PassManager` and `Sky` pass in `get()`.
- Pushes sky pass every update.
- Role: stable atmosphere pass injection in frame queue.

## `PlayerInputSystem`
- Caches keybinds from settings in `create()`.
- Resolves input/player/menu/inventory dependencies in `get()`.
- Per update:
  1. exits if no player for current window
  2. handles inventory toggle
  3. respects `menuManager.isInputLocked()`
  4. updates camera rotation
  5. writes `InputHandle` movement/action state

Role: translation layer from raw input -> gameplay intent.

---

## 3) Cross-system communication contracts

### Input lock contract
`PlayerInputSystem` must stop movement/camera writes when `MenuManager` says input is locked.
This avoids gameplay actions leaking through active menus.

### Window ownership contract
All runtime entry calls pass `context.getWindow()` so systems remain reusable for detached windows/editor contexts.

### Streaming contract
`WorldSystem` creates the grid after player spawn, and world stream/render managers then operate on that registered grid.

---

## 4) Common failure symptoms and meaning

- **No movement but menu works**: likely input lock is active.
- **No streamed world**: grid creation failed or player not available for window ID.
- **Menu opens in wrong window**: window passed to open/toggle path is mismatched.
- **Camera not rotating**: missing per-window camera in `PlayerManager` or input system not feeding mouse delta.

---

## 5) Extension points

Safe places to extend runtime without destabilizing bootstrap:

- Add a new runtime `SystemPackage` inside `RuntimeContext.create()`.
- Resolve dependencies in `get()` only.
- Use `context.getWindow()` for all player/menu/render routing.
- Gate gameplay input with `MenuManager.isInputLocked()` if UI can overlap.

That keeps multi-window/editor compatibility intact.
