# Engine Development Context

## Engine Overview
Java/LibGDX custom engine. Architecture packages: `ManagerPackage`, `LoaderPackage`, `BuilderPackage`, `SystemPackage`, `HandlePackage`, `InstancePackage`, `DataPackage`, `StructPackage`, `BranchPackage`, `UtilityPackage`, `PipelinePackage`. Lifecycle: `create()` → `get()` → `awake()` → `update()`. fastutil collections throughout. LibGDX never leaks into engine classes — abstracted behind utility classes. All handles, data, and instances go through `create()` then `constructor()` — never `new` directly.

---

## Project: Voxel RPG (TerraArcana)
Starting world: `TerraArcana`. Voxel terrain with greedy meshing, chunk/subchunk system, biome-based vertex colour blending, block rotation via UBO lookup table in vert shader.

---

## Pipeline Structure

### GeometryPipeline
- `DynamicGeometryManager` — top-level geometry manager. Contains `InternalBuildManager` which owns `FullGeometryBranch`, `PartialGeometryBranch`, `ComplexGeometryBranch`, `LiquidGeometryBranch`, and now `FontGeometryBranch`
- `DynamicGeometryManager.buildGlyphModel(materialID, glyph, atlasPixelSize)` — public method added for font pipeline
- `DynamicModelHandle` — owns `FloatArrayList vertices` + `ShortArrayList indices`. Methods: `addQuadVertices()`, `tryAddVertices()`, `mergeWithOffset(source, offsetIndices, offsets)` (NEW — applies per-attribute offset while merging quads from another model)
- `DynamicPacketInstance` — internal to geometry system, never held externally. Has `merge(other, offsetIndices, offsets)` — not used by fonts since fonts work at model level
- Packets are internal. `DynamicModelHandle` is the public currency.

### ShaderPipeline / TexturePipeline
- `AtlasTileData` (NEW) — abstract `DataPackage` in `com.internal.core.util.atlas`. Base for any tile that feeds `AtlasUtility`. Fields: `tileWidth`, `tileHeight`, `atlasX`, `atlasY`. Methods: `setTileDimensions()`, `setAtlasPosition()`, getters
- `TextureTileData` — now extends `AtlasTileData`. Removed its own position/dimension fields
- `AtlasUtility` — takes `ObjectArrayList<? extends AtlasTileData>`. Completely decoupled from texture system. MaxRects BSSF packing, always square power-of-2, grows and retries. Static `pack()` returns `atlasPixelSize`
- `TextureArrayData` — `atlasPixelSize` (pixel size, not grid count)
- `TextureHandle` — carries `tileWidth`, `tileHeight` forwarded from tile
- UV scale fix in `InternalLoader.seedUBO()`: `uvScaleX = tileWidth / atlasPixelSize` NOT `1 / atlasPixelSize`

### MenuPipeline
Package: `com.internal.bootstrap.menupipeline`

```
MenuPipeline.create() order:
  1. ButtonEventsManager
  2. ElementSystem
  3. FontManager       ← NEW
  4. MenuManager
```

---

## Font System (COMPLETE — Phase 1)

### Packages
- `com.internal.bootstrap.menupipeline.fonts` — data/handle/instance types
- `com.internal.bootstrap.menupipeline.fontmanager` — manager/loader/builder/utilities
- `com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry` — `FontGeometryBranch`

### Classes

**`AtlasTileData`** — `com.internal.core.util.atlas`
Abstract DataPackage. Base for `TextureTileData` and `FontTileData`. Holds tile dimensions and packed atlas position.

**`FontTileData`** — `com.internal.bootstrap.menupipeline.fonts`
Extends `AtlasTileData`. Bootstrap-only. Holds rasterized glyph `BufferedImage`, `codepoint`, `bearingX`, `bearingY`, `advance`. `constructor(codepoint, image, bearingX, bearingY, advance)`. Call `clearImage()` after atlas upload.

**`GlyphMetricStruct`** — `com.internal.bootstrap.menupipeline.fonts`
Immutable struct. Fields: `atlasX`, `atlasY`, `width`, `height`, `bearingX`, `bearingY`, `advance`. All public final.

**`FontHandle`** — `com.internal.bootstrap.menupipeline.fonts`
Immutable. `constructor(name, gpuHandle, materialID, atlasPixelSize, glyphs, glyphModels)`. Owns `Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs` and `Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels` (one pre-built origin-space quad per glyph, static forever).

**`FontInstance`** — `com.internal.bootstrap.menupipeline.fonts`
`constructor(handle, mergedModel)`. Owns a `DynamicModelHandle mergedModel` (empty until `setText()`) and a `ModelInstance modelInstance` (null until `MenuManager.uploadFontModel()`). `setText(String)` clears merged model, walks string, merges each glyph's pre-built model with cursor x/y offset via `mergeWithOffset()`. Marks `dirty=true`. `setColor(r,g,b,a)` for tint. `setModelInstance()` clears dirty flag. `clearModelInstance()` on menu close.

**`FontGeometryBranch`** — `com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry`
BranchPackage. Gets `VAOManager`, resolves `"util/LabelVAO"` via `getVAOHandleFromName()` in `awake()`. `buildGlyphModel(materialID, glyph, atlasPixelSize)` — builds one origin-space quad `DynamicModelHandle`. VAO layout `[2, 2, 1]` = 5 floats: x, y, u, v, color. `OFFSET_INDEX_X=0`, `OFFSET_INDEX_Y=1` (static constants used by `FontInstance.setText()`).

**`FontRasterizerUtility`** — `com.internal.bootstrap.menupipeline.fontmanager`
AWT only — no LibGDX. Rasterizes TTF/OTF to per-glyph `BufferedImage`. Needs builder reference for `create(FontTileData.class)` since it has no engine context.

**`GLSLUtility`** (font) — `com.internal.bootstrap.menupipeline.fontmanager`
Only class in font pipeline importing LibGDX. `pushTexture2D(BufferedImage)` → GPU handle. `deleteTexture2D(handle)`. Uses `GL20.GL_TEXTURE_MAG_FILTER` (NOT `GL_TEXTURE_MAX_LEVEL` — that doesn't exist in GL20).

**`InternalBuilder`** (font) — `com.internal.bootstrap.menupipeline.fontmanager`
Gets `DynamicGeometryManager`, `MaterialManager`. `init(root)` called in loader `awake()`. `build(name, configFile)` pipeline: rasterize → `AtlasUtility.pack()` → composite atlas → `GLSLUtility.pushTexture2D()` → build glyph metric table → `DynamicGeometryManager.buildGlyphModel()` per glyph → construct `FontHandle`. Material ID via `materialManager.getMaterialIDFromMaterialName()`. Creates `FontTileData` via `create()` for rasterizer.

**`InternalLoader`** (font) — `com.internal.bootstrap.menupipeline.fontmanager`
Scans `EngineSetting.FONT_PATH` for JSON files. `load(file)` → builder → `fontManager.addFont()`. `request(fontName)` for on-demand load. Self-destructs when queue empties.

**`FontManager`** — `com.internal.bootstrap.menupipeline.fontmanager`
Creates `InternalLoader`. Gets `VAOManager`. In `awake()`: resolves `labelVAOHandle = vaoManager.getVAOHandleFromName("util/LabelVAO")`. `cloneFont(name)` → creates fresh `DynamicModelHandle` + `FontInstance`. `request(name)` for on-demand load. `dispose()` calls `GLSLUtility.deleteTexture2D()` per handle.

### Font JSON format
```json
{
  "file": "Roboto-Regular.ttf",
  "size": 24,
  "material": "fonts/Default",
  "characters": "ABCabc..."
}
```
File lives in `assets/fonts/`. Font name = JSON file stem.

### VAO
`assets/mesh/util/LabelVAO.json`:
```json
{ "vao": [2, 2, 1] }
```
5 floats per vertex: x, y, u, v, packed color.

### EngineSetting additions
```java
public static final String FONT_PATH = "fonts";
public static final ObjectArraySet<String> FONT_FILE_EXTENSIONS = new ObjectArraySet<>(new String[]{"ttf","otf"});
```

---

## Element System (Phase 1 changes)

### ElementData
Added `fontName` field. `constructor()` and `constructorUse()` now take `fontName` parameter. `constructorRef()` unchanged (refs carry no font). `getFontName()` accessor added.

### ElementHandle
Added `fontName` field. `constructor()` now takes `fontName`. `getFontName()` and `hasFont()` accessors added.

### ElementInstance
Added `FontInstance fontInstance` field. `constructor()` now takes `FontInstance fontInstance` as third parameter (after `spriteInstance`). `getFontInstance()` and `hasFont()` accessors added.

### ElementSystem
Gets `FontManager`. In `createInstance()`: if `master.hasFont()`, calls `fontManager.cloneFont(master.getFontName())`, then immediately calls `fontInstance.setText(initialText)` using override text or handle text. Passes `fontInstance` into `ElementInstance.constructor()`.

### Menu InternalBuilder
Gets `FontManager`. `parseInlineElement()` calls `parseFontName(json, elementType, id)` which: reads `"font"` field, validates element is `LABEL` type, validates `fontManager.hasFont(fontName)` — fails at load time on bad names. `buildMaster()` passes `data.getFontName()` into `ElementHandle.constructor()`. `buildUsePlacement()` forwards `template.getFontName()` when copying template handle.

### MenuManager
Gets `ModelManager` and `MaterialManager`. `openMenu()` calls `uploadFontModels(liveElements)` after `createInstances()` before adding to active list. `closeMenu()` calls `releaseFontModels(instance.getElements())`. `renderElement()` calls `pushFontRenderCall(element)` after sprite push. Font renders at depth `0` (in front of sprites at depth `1`). `uploadFontModel()` clones material, sets `"u_fontAtlas"` uniform with GPU handle, calls `modelManager.createModel()`. `releaseFontModel()` calls `modelManager.removeMesh()` and `font.clearModelInstance()`. Both upload and release recurse into children.

---

## VAOManager — correct accessor methods
- `getVAOHandleFromName(name)` — external callers, triggers load on miss. Use from managers/systems in `awake()` or runtime
- `getVAOHandleDirect(name)` — internal builders only, no load trigger, safe inside a `load()` call

## MaterialManager — correct accessor methods
- `getMaterialIDFromMaterialName(name)` — triggers load on miss
- `getMaterialFromMaterialID(id)` — direct lookup
- `cloneMaterial(materialID)` — deep copies uniforms into a new `MaterialInstance`

---

## Phase 2 — Runtime Element Injection (PLANNED, NOT STARTED)
- `MenuInstance.addElement(container, ElementInstance)` / `removeElement(ElementInstance)`
- `ElementSystem.cloneHandle(ElementHandle)` — clone a defined handle into an instance at runtime
- Items inject their own label elements into a named container on an open menu
- Item elements defined in JSON as a handle, cloned at runtime — not hardcoded

## Phase 3 — Container Clipping (PLANNED, NOT STARTED)
- `CONTAINER` element type gets `mask: true` boolean in JSON
- `MenuManager` render traversal sets/clears GL scissor rect per masked container
- Children outside container bounds are clipped

## Phase 4 — Scrollable Containers (PLANNED, NOT STARTED)
- Scroll offset on container `ElementInstance`
- Input/raycast feeds scroll delta into container
- Children render with offset applied
- Scrollbar defined in container JSON

---

## Inventory System (PLANNED, NOT STARTED)
Dependency order: fonts (done) → item handles/instances → entity inventory handle → inventory UI via Phase 2 runtime injection.
- All entities hold an inventory handle
- Items need `ItemHandle` / `ItemInstance`
- Inventory UI = open menu + runtime-injected item label elements into scrollable container

---

## Commercial-Safe Fonts (Google Fonts / OFL)
- Press Start 2P — pixel/retro
- Cinzel — fantasy/RPG
- Rajdhani — sci-fi HUD
- Roboto — general purpose
- MedievalSharp — gothic/dark fantasy

---

## Key Rules / Conventions
1. All handles, data, instances go through `create()` then `constructor()` — never `new`
2. LibGDX never leaks into engine classes — always wrapped in a `GLSLUtility` or equivalent
3. Packets (`DynamicPacketInstance`) are internal — never held externally. `DynamicModelHandle` is the public currency
4. `VAOManager.getVAOHandleFromName()` for external callers, `getVAOHandleDirect()` inside builders
5. `MaterialManager.getMaterialIDFromMaterialName()` — not `getMaterialID()`
6. Atlas always square, always power-of-2
7. Font pipeline uses same `AtlasUtility.pack()` as texture pipeline — decoupled via `AtlasTileData` base class
8. Glyph models built once at font load time, never modified. Label merged model built once at `setText()`, only rebuilt if text changes. GPU `ModelInstance` created at menu open, released at menu close
9. Text renders at depth `0`, sprites at depth `1` in `MenuManager`
10. `FontGeometryBranch` lives in `dynamicgeometrymanager.dynamicgeometry` alongside the other branches. Called through `DynamicGeometryManager.buildGlyphModel()`
