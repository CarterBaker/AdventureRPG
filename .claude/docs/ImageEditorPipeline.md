# ImageEditorPipeline

Where the Image Editor and the Palette tab's maps live, which method owns each operation, and the traps. Read it before touching
the Image Editor, the Palette tab's maps, map generation, layered images or the New Image dialog. `.claude/CLAUDE.md` still
applies in full.

---

## What it is

The Image Editor (Textures menu) paints layered images the way Krita and Photoshop do: File, New names an image and
sets its size and background; its layers are listed in the Layers tab (New Layer and Delete Layer icons at its foot,
the same keys as the Tools tab's actions); the Tools tab's tools (Brush, Erase, Smooth, Swirl, Scatter, Fill, Pick),
the Brushes tab's brush and the Palette's colour paint the layer picked; the Palette tab lists every map the image may
carry, and Generate and Clear under the picked map's settings make and erase it. An image is one `.arpg` file in
the workspace, `Documents/My Games/AdventureRPG/Assets/Images/`: New names it, sets its size and writes it at once,
and File, Open picks one there in the system's file dialog.

## One model for every painted image

- **`ImageLayerStackStruct`** (`editor.bootstrap.imagepipeline.imagelayer`) is the layered image: size, layers bottom
  first (each an `ImageDocumentInstance`, so every stroke is an undo step), names, opacities, eyes, the layer picked,
  and the flattened composite. `CharacterPaintCanvasStruct` extends it, so a character's texture maps and the Image
  Editor's images are the same model. `ImageManager.compositeLayers` is the one flatten: a canvas whose base keeps its
  alpha (a texture map) only colours the base; an image lays every layer over the one below, straight alpha
  (`ImageColorUtility.over`).
- **Maps.** `ImageMap` lists every map (albedo, normal OpenGL and DirectX, height, displacement, bump, ambient
  occlusion, cavity, curvature, thickness, roughness, glossiness, metallic, specular, diffuse, emissive, opacity,
  tint), each with its neutral colour, its generator (`MapGenerateUtility::generate*`, a `MapGenerator`) and the
  `MapGenerateOption`s it takes. Each map keeps its own copy of its settings. `ImageManager.generateMap` is the one
  path that generates (`ImageMapBranch`, scratch in `MapGenerateScratchStruct`, grown only for a larger image). The
  albedo is the layers themselves and is never generated.
- **The maps in the Palette tab** (`editor.runtime.palette`, `ImagePaletteSystem`, under the colour wheel) follow the
  window worked in last that has a palette, tracked by `PropertiesManager.getPalette` as the Properties tab's provider
  is; every `PaletteRowSource` offering `PaletteContent.IMAGE` lists its maps there (`buildPaletteRows`): the Image
  Editor, the UV Editor and the character Paint and UV tabs. Rows come from `MapRowUtility` for both, so the layout is one: map entries (generated ones
  marked), the picked map's settings, Generate, Clear.

## Code

- `editor.bootstrap.imageeditorpipeline`: `ImageEditorManager` (images open and the one worked, strokes, history,
  Layers tab, rows API), `ImageProjectBranch` (the file), `ImageEditorLayerBranch` (layers, composite, closing
  documents), `ImageEditorMapBranch` (show, settings, Generate, Clear), `ImageEditorRowBranch` (Properties and Maps
  rows and every key). `ImageProjectInstance` is one image: its stack, composite document, map documents (null until
  generated), per-map settings, the map shown, and its undo and redo documents in order.
- `editor.runtime.imageeditor`: `ImageEditorContext` (tool canvas `IMAGE_EDITOR`, layers, properties, maps, File menu
  `FILE_MENU_IMAGE_EDITOR`), `ImageEditorToolSystem` (pointer to pixels, rows top first), `ImageEditorRenderSystem`
  (`ImageEditorShader` over the UV Editor's quad and vertex shader). The view is `UVEditorViewSystem`, the image's
  longer side spanning its unit square; its constants are EditorSetting's `FLAT_VIEW_*`.
- Menus: `ImageEditorBranch` (File and Edit actions, rows), `NewImageDialogBranch` (`menus/editor/NewImageDialog`:
  name, width, height, background), row templates in `menus/editor/ImageEditor` (`IMAGE_EDITOR_ROW_TEMPLATES`).
- Character side: `CharacterMapBranch` (per-texture map settings, Generate, Clear through `writeMap`),
  `CHARACTER_PAINT_IMAGE_MAPS` (which `ImageMap` each paint map is), keys in `CharacterPaintPropertyBranch`.

## The file

`width`, `height`, `active_layer`, `next_layer`, `layers` (bottom first: `name`, `opacity`, `visible`, `pixels` as PNG
bytes) and `maps` (keyed by the map's enum name: `settings` by option name, and `pixels` once generated). Written by
`ArpgUtility.tryWriteObject`, read by `tryLoadObject`; a layer of the wrong size refuses the read. Keys are the
`IMAGE_FILE_KEY_*` constants.

## Export and Import

The workspace-to-game flow a texturing tool and an engine share (`ImageExchangeBranch`, File menu Export and Import):

- **Export** writes every map the image holds as one PNG in the workspace's `Exports/<Image>/` folder: the albedo (the
  layers flattened) and each map once generated, named `<Image>_<suffix>.png` by `ImageMap.getExportSuffix`. A map the
  game's textures take uses that texture alias's own token (`a`, `n`, `h`, `ao`, `rough`, `m`, `s`, `e`, `tint`); every
  other map a name no alias holds (`ndx`, `disp`, `bmp`, `cav`, `curv`, `thick`, `gloss`, `dif`, `opacity`), so Import
  never mistakes one for another. A new export clears the folder first.
- **Import** asks for the texture pack (a folder under `assets/textures/`, the last one used offered) and the
  texture's name (the image's, no underscore), removes the tile's old images from the pack, copies the exported maps an
  alias takes as `<Tile>_<suffix>.png`, bakes the tile's streamed files (`TextureBakeUtility`, as the Paint tab does),
  plays the new bake at once when the tile is already loaded (a new tile loads with the next start), and clears the
  exports from the workspace. The image's own `.arpg` stays to be worked and imported again. Imported textures are the
  owner's work: never catalogue them. Any folder of exported maps imports the same way
  (`ImageEditorManager.importMaps`, `ImageExchangeBranch.importMaps`), which is how a workspace material's exports reach
  the game (see `.claude/docs/Editor.md`, Materials).

## Stamps

Pictures pressed onto the image, as a painting tool's stamp library presses them (`StampManager`'s picture side,
`StampImageBranch`):

- **The library** is every PNG in the workspace's `Stamps` folder, read again each time the Stamps tab turns to
  pictures (when the Image Editor becomes the canvas worked last), each tile the picture fitted to a square.
- **Pressing.** A picture chosen arms the Brush (`ImageEditorManager.armsStamp`): the next click presses it centred
  there on the layer or map worked, sized by the Brush's Scale and turned by its Turn (the image canvas declares both),
  every pixel laid over what lies beneath by its alpha (`ImageEditBranch.press`, the over operator, nearest pixel), as
  one undo step. Pressing or Escape puts it down, so the Brush paints again.

## Traps

- **A map is painted by its own rule** (`ImageMap.getPaintRule`, `MapPaintRule`), set as the stroke begins
  (`ImageEditorManager.beginBrush`), never on the albedo's layers: colour maps take the paint colour; level maps turn
  Paint into Raise or Lower by whether the colour is brighter or darker than mid grey, its Strength scaled by how far
  it lies from it, so a value only moves within its range and keeps its grey; normal maps turn it into Relief
  (`ImageEditBranch.reliefPixel`), the tip's coverage slope bending each normal in slope space by the brush's radius
  and `IMAGE_RELIEF_SLOPE`, raising for a bright colour and carving for a dark one, the DirectX map's green flipped,
  every normal laid back unit length.
- **Strokes on a layer are transparent strokes** (`ImageBrushStruct.setTransparent`): paint lays over by alpha, Erase
  wears alpha away. On a map, Erase lays the map's neutral colour. The World Editor's brush never sets it.
- **Pixel edits never move the manager's revision**; only structure, settings, history and status do, so the rows are
  not rebuilt per stamp. The view reads the composite's texture, kept current by the layer edit listeners.
- **The paint colour is `CharacterEditorManager`'s**; its colour keys work with no character open.
- **Not done yet, only when the owner asks:** opening a character's texture as an Image Editor document (the model is
  shared; the documents are not yet), a brush outline over the image, layer groups, blend modes, moving layers up and
  down and selections.
