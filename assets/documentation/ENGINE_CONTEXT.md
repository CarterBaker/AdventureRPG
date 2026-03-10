# Engine Context Document

## Engine Overview
- **Framework:** LibGDX (abstracted — direct LibGDX/LWJGL calls are banned outside of utility classes)
- **Language:** Java
- **Renderer:** OpenGL via LibGDX GL20/GL30, deferred batch renderer
- **Coordinate Space:** y+ is UP, bottom-left origin (GL convention throughout)
- **Screen Space:** pixel units. screenW/screenH cached from WindowInstance

---

## Package Architecture — Base Classes

Every engine object extends a base package. These are the rules:

| Base Class | Purpose | Lifecycle |
|---|---|---|
| `ManagerPackage` | Top-level systems, owns sub-objects | create → get → awake → update |
| `SystemPackage` | Stateful sub-systems owned by a manager | create → get → awake → update |
| `BranchPackage` | Logic branches owned by a manager/system | create → get → awake → update |
| `BuilderPackage` | Bootstrap-only builders | get → build methods |
| `LoaderPackage` | File loaders, self-releases when queue empties | scan → create → get → awake → load(File) → onComplete |
| `HandlePackage` | Immutable definitions built at bootstrap | No lifecycle — constructor() called manually |
| `InstancePackage` | Live runtime objects | create() called by engine, constructor() called manually |
| `DataPackage` | Bootstrap-only data carriers | constructor() called manually |
| `StructPackage` | Plain data structs | Created with `new`, no engine lifecycle |
| `UtilityPackage` | Static utility methods | Static only |

### Key Rules
- `create(SomeClass.class)` — instantiates and registers a child object through the engine
- `get(SomeClass.class)` — retrieves a registered object from the engine registry
- `throwException(String)` — engine-standard error reporting
- `debug(String)` — engine-standard debug logging, prints package name + message
- **`StructPackage`** is the ONLY package created with `new` directly — no create()/constructor() needed
- HandlePackage and InstancePackage use `constructor()` method (not Java constructor) called after `create()`
- Managers never touch LibGDX/GL directly — all GL calls go through GLSLUtility classes

---

## Render Pipeline

### Flow
```
MenuManager.update() → pushRenderCall(model, depth, mask)
                     ↓
RenderSystem batches by depth → materialID
                     ↓
RenderSystem.draw() flushes queue — applies MaskStruct per call at draw time
```

### Key Classes
- `RenderSystem` — collects render calls, flushes in `draw()`. Batched by `Int2ObjectAVLTreeMap<depth, Int2ObjectOpenHashMap<materialID, RenderBatchHandle>>`
- `RenderCallHandle` — pairs ModelInstance + cached uniforms + cached UBOs + `MaskStruct mask` (null = no mask)
- `RenderBatchHandle` — groups render calls by material, caches source UBOs
- `MaskStruct extends StructPackage` — x, y, w, h in GL pixel space (bottom-left origin). Created with `new MaskStruct(x, y, w, h)`
- `GLSLUtility` (render pipeline) — all GL calls. Has `enableScissor(x,y,w,h)`, `disableScissor()`, depth, blend, cull, shader, VAO, draw, UBO methods

### Render Depths
- `0` — depth test ON (world geometry)
- `1` — depth test OFF (sprites/UI backgrounds)
- `2` — depth test OFF (font/text, always on top of sprites)

### Scissor/Mask Architecture
- Scissor state travels WITH the render call as a `MaskStruct`
- `RenderSystem.draw()` only calls `glScissor` when the mask changes between calls
- `MenuManager` maintains a pure-data `maskStack` — no GL calls in menu pipeline
- Y coordinate in MaskStruct is already GL bottom-left space (no flip needed)

---

## Menu Pipeline

### JSON Structure
```json
{
  "elements": [ /* top-level masters — reusable definitions */ ],
  "menus": [
    {
      "id": "MenuName",
      "lock_input": false,
      "raycast_input": true,
      "entry_points": ["container_element_id"],
      "elements": [ /* menu-specific placements */ ]
    }
  ]
}
```

### Element Fields
```json
{
  "id": "my_element",
  "type": "sprite|texture|button|label|container",
  "sprite": "path/to/sprite",
  "font": "FontName",
  "text": "Hello",
  "color": [r, g, b, a],
  "align": "left|center|right",
  "mask": true,
  "stack": "vertical|horizontal",
  "spacing": "2px",
  "anchor": "CENTER|TOP_LEFT|...",
  "pivot": "CENTER|TOP_LEFT|...",
  "position": { "x": "0%", "y": "0%" },
  "size": { "x": "100%", "y": "50px" },
  "min_size": { "x": "300px", "y": "80px" },
  "max_size": { "x": "100%", "y": "100%" },
  "on_click": { "class": "com.example.Branch", "method": "methodName", "arg": "optionalArg" },
  "elements": [ /* children */ ]
}
```

### Element Referencing
- `"ref": "filePath/elementId"` — references a master from another location, layout override only
- `"use": "filePath/elementId"` — inherits a master, can override sprite/text/color/layout/action/children

### Master Key Format
`"filePath/elementId"` where filePath is relative to menu root without extension.
Example: file `menus/Items.json`, element `item_slot` → key `Items/item_slot`

### Dimension Values
- `"50%"` — percentage of parent dimension
- `"50px"` or `"50p"` — absolute pixels
- `"50"` — absolute pixels (bare number)

### Anchor / Pivot Origins
`BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, TOP_LEFT, TOP_CENTER, TOP_RIGHT`

### Entry Points
Declared per-menu as a string array. Index 0 = first string, index 1 = second, etc.
`menuInstance.getEntryPoint(0)` returns the live `ElementInstance` with that id.

---

## Menu Class Hierarchy

### Bootstrap (not kept after load)
- `ElementData` — parsed JSON data carrier
- `MenuData` — parsed menu declaration

### Immutable Definitions (kept forever)
- `ElementHandle` — master element definition. Fields: id, type, spriteName, text, fontName, color, layout, mask, stackDirection, spacing, textAlign, clickAction, menuAwareAction, children
- `MenuHandle` — menu definition. Fields: name, placements, lockInput, raycastInput, entryPoints
- `ElementPlacementHandle` — pairs ElementHandle + optional ElementOverrideStruct
- `ElementOverrideStruct` — optional overrides: spriteName, text, color, clickAction, menuAwareAction, layout

### Live Runtime (allocated on openMenu, released on closeMenu)
- `MenuInstance` — live menu. Has getEntryPoint(int), addToEntryPoint, removeFromEntryPoint
- `ElementInstance` — live element. Has computeTransform, computeStackedTransform, scroll state, content size, addChild, removeChild, findChildById, positionOverride
- `FontInstance` — owns merged glyph model + GPU model lifecycle. Has setText, upload(ModelManager, MaterialManager), release(ModelManager)
- `SpriteInstance` — static model handle reference, no GPU lifecycle

---

## Font Pipeline

### Architecture
- `FontHandle` — immutable. Holds GPU atlas texture, glyph metric table, one pre-built `DynamicModelHandle` per glyph (pixel-space quads, origin at 0,0)
- `FontInstance` — live. Merges per-glyph models with cursor offsets into one `DynamicModelHandle` (mergedModel), then uploads to GPU as `ModelInstance`
- Glyph verts are pixel-space: a 20×32 glyph has verts 0→20, 0→32
- `fontTransform` is translation-only: moves the whole merged mesh to its screen position
- VAO layout: `[2, 2]` — 4 floats per vertex: x, y, u, v
- `OFFSET_INDEX_X = 0`, `OFFSET_INDEX_Y = 1`

### setText Flow
1. `mergedModel.clear()`
2. For each codepoint: `mergeWithOffset(glyphModel, [0,1], [offsetX, offsetY])`
3. `offsetX = cursorX + bearingX`, `offsetY = cursorY + (bearingY - height)`
4. `cursorX += advance`, `textHeight = max(textHeight, metric.height)`
5. `dirty = true`

### GPU Upload/Release
- `FontInstance.upload(ModelManager, MaterialManager)` — checks dirty + non-empty, releases old model, creates new ModelInstance
- `FontInstance.release(ModelManager)` — removes mesh, nulls reference
- Called by `MenuManager.uploadFontModels` / `releaseFontModels` on openMenu/closeMenu
- `MenuManager.refreshText(ElementInstance)` — re-uploads after runtime setText

### UV Orientation
- Verts built bottom-left origin with v0/v1 swapped to correct GL y-flip:
  - BL: u0,v1 | BR: u1,v1 | TR: u1,v0 | TL: u0,v0

---

## Input System

### Key Principles
- Movement keys blocked when `locked = true`
- UI keys (inventory, debug items) tracked regardless of lock — set in `keyDown` before lock check
- Just-pressed flags use **consume pattern** — `consumeX()` reads and clears atomically, immune to update order
- No flags cleared in update() — only consumed by callers

### Current UI Keys
- `I` — inventory toggle → `consumeInventoryJustPressed()`
- `NUM_1` — DEBUG add debugApple → `consumeDebugItem1JustPressed()`
- `NUM_2` — DEBUG add debugTable → `consumeDebugItem2JustPressed()`

---

## Inventory System

### Classes
- `InventoryHandle` — owned by `EntityHandle`. Always has a `BackpackInstance` (created in engine `create()`, never null). Fields: mainHand, offHand, backpack
- `BackpackInstance` — `ObjectArrayList<ItemDefinitionHandle> items`. Has addItem, removeItem, getItems, size, isEmpty
- `ItemDefinitionHandle` — fields: itemName, itemID, weight, twoHanded, isBackpack

### Current Items (debug/debug.json)
- `debugApple`
- `debugTable`

---

## ButtonEventsManager

Registered branches:
- `GenericButtonBranch`
- `MainMenuBranch` — opens/closes `MainMenu/Main`
- `InventoryBranch` — opens/closes `Inventory/Inventory`, rebuilds item list UI

---

## Runtime Injection API

```java
// Add element to entry point with customization
ElementInstance el = menuManager.inject(menu, entryPointIndex, "MasterKey", instance -> {
    ElementInstance label = instance.findChildById("label_id");
    if (label != null)
        label.getFontInstance().setText("Display Name");
});

// Remove element and release GPU resources
menuManager.eject(menu, entryPointIndex, element);

// Re-upload font after changing text on live element
menuManager.refreshText(element);

// Scroll a stacked container
container.setScrollY(value); // clamped on read via getScrollY()
float max = container.getMaxScrollY(); // contentH - computedH
```

---

## Current JSON Files

### menus/MainMenu.json
- Masters: `close` (button with label "Close"), `title` (sprite)
- Menu `Main`: lock_input true, raycast_input true
- Contains: title ref, container with close button and quit button

### menus/Inventory.json
- Menu `Inventory`: lock_input false, raycast_input true
- Entry points: `["item_list", "scroll_handle"]`
- Panel (sprite, CENTER_LEFT), contains:
  - `item_list` — container, mask true, stack vertical, spacing 2px, 88% wide
  - `scrollbar_track` — sprite, 10% wide, contains `scroll_handle`

### menus/Items.json
- Master: `item_slot` — button, size 100%×50px, on_click selectItem()
  - Child: `item_label` — label, font MontserratAlternates, align left, color black

### assets/debug/debug.json
- Items: debugApple, debugTable

---

## Debug / Temp Code — TO REMOVE

### InputSystem.java
```
// Fields:
private boolean debugItem1JustPressed, debugItem1Down;
private boolean debugItem2JustPressed, debugItem2Down;

// In keyDown:
NUM_1 / NUM_2 handling

// Methods:
consumeDebugItem1JustPressed()
consumeDebugItem2JustPressed()
```

### PlayerManager.java
```
// Import:
import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;

// Field:
private ItemDefinitionManager itemDefinitionManager;

// In get():
this.itemDefinitionManager = get(ItemDefinitionManager.class);

// In handleInventoryInput():
if (inputSystem.consumeDebugItem1JustPressed()) debugAddToBackpack("debugApple");
if (inputSystem.consumeDebugItem2JustPressed()) debugAddToBackpack("debugTable");

// Entire method:
private void debugAddToBackpack(String itemName) { ... }
```

### InventoryBranch.java
```
// selectItem() body is empty stub — wire when item interaction implemented
```

---

## Known Working / Verified
- Font renders correctly: pixel-space verts, translation-only transform, UV y-flip correct
- Text aligns left/center/right correctly
- Stacked containers lay out top-down correctly (cursor starts at top, steps downward)
- Render call batching by depth + materialID working
- MaskStruct travels with render calls — scissor applied at draw time, not traversal time
- Consume pattern for input flags — immune to manager update order
- Inventory opens on I, items inject and display with correct text
- Entry point system working — getEntryPoint(0) resolves by name from handle

## Known Issues / Not Yet Implemented
- Scrollbar handle not wired to scroll state programmatically
- selectItem() in InventoryBranch is a stub
- No item icon/sprite support yet — text only
- MainHand / OffHand slots not displayed in UI yet (backpack only for now)
- HUD hotbar (bottom-right, always open) not yet built
