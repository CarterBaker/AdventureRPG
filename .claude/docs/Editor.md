# Editor Workflow

How the owner works in the editor and what every editor window must therefore do. Read it before adding or changing
any editor window, panel, tool, brush or value row. `.claude/CLAUDE.md` still applies in full.

---

## One task per tab

- **Many windows, never one crowded one.** Each tab does one task and works with the tabs around it. When a window
  starts doing two jobs, split it into two tabs that share their manager's state, the way Krita splits its dockers.
  Never cram another panel or palette into an existing window to save a tab. The one way a canvas does several jobs
  is layers (below): its tasks are layers picked in the Layers tab, never modes or toolbars of its own.
- **Tabs talk through shared state.** What is chosen in one tab (a biome, a brush, a tree, a ship, a bone) is the
  choice every other tab sees at once. Tabs never keep private copies of a shared choice.
- **Name a tab for what it holds.** A list of brushes is the Brushes tab; a window that paints the world's images is
  the World Editor. Never a generic name that hides what the tab is for.
- **File, Edit and Settings are icons; shared tabs live in the Tools menu.** The toolbar
  (`menus/editor/EditorWindow.arpg`) opens with File, Edit and Settings as square icon buttons (`menus/FileIcon`, a
  floppy disk; `menus/EditIcon`, a pencil; `menus/GearIcon`, a gear opening the Settings dialog), a thin divider, the
  menus, a divider closing them, the search box centred in the room left, a divider, then Layouts. Every divider in the
  bar is the same thin line, so the groups read alike. Every other menu lists a tab under the section for what it works on. The tabs whose view
  changes with the window worked in last, the ones that serve the others, all sit in the Tools menu, the first: Data
  (Hierarchy, Properties, Layers), Canvas (Tools, Brushes, Stamps, Palette) and Media (the
  Recorder). A new shared tab joins the Tools menu, never another menu. The sword (`menus/Sword`,
  `EditorSetting.WINDOW_ICON`) is every editor window's OS icon, set by `EditorFrameSystem` through
  `WindowPlatform.setWindowIcon`, never drawn in the toolbar. The menus between the two dividers lay out as
  equal cells (`StripLayoutUtility`'s grid helpers, in `EditorToolbarSystem`): each at its preferred width while the bar has
  room, all shrinking together as it narrows, then wrapping onto more rows, the bar growing and the dock shrinking
  beneath it. A dropdown's height is `"fit"`: `MenuBuilder` makes it the height of the rows it holds as it loads, so a
  row added or removed never leaves a dropdown clipped or padded; never count a dropdown's height by hand. The
  Materials menu, after Textures, holds the Materials
  tab, the editor's one list of materials, and the Material Viewer (see Materials below).
  The Entities menu (once Characters) holds what only an entity has (its Entity section: Appearance, Equipment, Hair;
  then Animation); the Mesh menu holds the general modelling tools any rigged model takes
  (its Mesh section: Mesh, Topology, Sculpt, Paint, UV, UV Editor; its Rigging section: Rig, Weights). A tab or tool
  shown to the owner says Entity, never Character; the code's `Character*` class names stay as they are. The Debug
  menu's Output section holds the ARPG Viewer beside the consoles. Its Preview opens `DevContext`, the game as it runs with the command console's systems; there is no other runtime
  tab. The Command Console is one window whose view follows the window worked in last
  (`CommandManager.resolveViewTarget`): the character tabs' commands while a character tab was, the game's while a Dev
  window was.
- **Each tab's job** is listed under The tabs, below.

## The tabs

- **World Editor.** Paints the biomes and places settlements and cameras on its Biomes layer, and shapes the ground on
  its Heights layer with one Raise brush that lowers while Ctrl is held (`WorldEditor/InvertStroke`). On either layer,
  Move only looks: its left drag pans the view as the right drag does. The biome it paints is chosen in the Palette
  tab, which lists the biomes the world itself names in its `biomes` field (edited with the world in the Worlds tab)
  while the Biomes layer is picked, the chosen biome selected in the Hierarchy as the palette is shown, so its fields
  are the Properties tab's. The Biome Planner only plans.
- **Content.** Entities and races, like all content, are picked in the Hierarchy and edited in the Properties tab.
- **Character tool tabs.** Making a race's character is split into Appearance, Equipment, Hair, Mesh, Topology,
  Sculpt, Weights, Paint, Rig and UV (see `.claude/docs/CharacterEditorPipeline.md`), all sharing the region, bone and
  vertices selected. The Appearance tab's Properties rows hold every slider and control, and the Rig tab marks which
  bone channels are sliders. The Hair tab grooms strands painted with the material chosen in the Materials tab. Every
  bone the Palette tab lists (Mesh, Weights, Rig) has an eye, as a layer has (`CharacterViewRowKind.EYE_ITEM`,
  `CharacterEditorManager.toggleRowEye`): shut, it hides the bone, every bone under it and the vertices they carry most,
  the bones below keeping their own eyes as a hidden group's layers do.
- **UV Editor.** Lays out the UVs of the faces the UV tab selects.
- **Shared beside them.** The Properties tab (also the Animation Editor's); the Palette tab, the colour wheel holding
  the paint colour every painting window shares over the maps of the image worked (the texture painted, or the Image
  Editor's image), the colour's sliders in the Properties tab while the Palette was worked in last.
- **Image Editor** (under Textures). Paints layered images as Krita does, its layers in the Layers tab and its maps in
  the Palette tab (see `.claude/docs/ImageEditorPipeline.md`).
- **Recorder.** The transport and nothing else: Record, Play, Pause, Stop and the Loop switch (which runs playing round
  the range marked with I and O, or the whole media file), the clock, and the status line saying what they did or why
  they could not. A take records whether or not the game is paused, so the pause menu that frees the cursor never
  stops it; music plays with no Preview tab, through the Recorder or Timeline tab. Back is a Recorder shortcut.
- **Timeline.** The clips, laid on lanes as Premiere lays them: five at first and one more each time the highest takes
  a clip, each strip only its number and the name its ... gives it. Its title names what is open. Once zoomed, its
  view scrolls under the cursor with a left drag on its empty lanes or a right drag anywhere over its lanes or ruler,
  clips included.
- **Keys and Keys Graph.** The Keys tab holds the rows of the layer picked in the Layers tab, and for an animation it is
  a dope sheet as Blender's is: a summary row for every bone and morph marking every key it holds, the one picked (in
  the Layers tab, the Animation Editor or by its name here) marked, its values (position, rotation and scale on each
  axis) listed in the Properties tab at the playhead, each keyed there when changed, with the one the graph shows chosen
  there; the Keys Graph the curve selected, a key clicked twice freeing or joining its handles. Each opens its own menu
  through a shared `RecorderRowSystem`, and the pane tab worked in last is the one the recorder's deletes act on.
- **Recording in the shared tabs.** The Layers tab lists the take's tracks (camera, player, characters, items, vehicles,
  weather, clock, sounds) or, in its Takes view, every take of the open recording, each dragged onto the Timeline's
  lanes to lay it; each pane keeps its own view (the Timeline opens on the Takes, the Keys and Keys Graph on the take's
  layers). The Tools tab holds every take, clip, key, sound, transition and node action. The Properties tab lists the
  channels armed and the export first, then the clip's channels, mute and time ramp (the lane and clip selected, its mix
  too) while the Recorder or Timeline is worked in on a media file (an animation clip has no mix: only its layer's
  name), the file's tempo, meter and key while the Recorder is, and the keys selected first (their time and value, moved
  together, and their mode) while the Keys or Keys Graph is. Delete removes what is selected in the pane worked in last;
  Edit, Undo and Redo (Ctrl+Z, Ctrl+Y) step through every edit of the open recording (`RecordingHistoryBranch`); File,
  Export exports.
- **Tracks** (`RecordingsContext`). Every media file, video and music in one list; its rows drag onto the Timeline to
  lay.
- **Note Library** (under Library in the Media menu, with the Sound Library). Empty until an instrument or a note set
  is picked in the Hierarchy, then the notes it plays, each sounded by its button and dragged onto the Timeline as a
  note clip that is stretched and cut like any clip. A library's drag, like Record, makes a media file when none is
  open.
- **Scene Assets** (under Preview in the Debug menu, `editor.bootstrap.sceneassetspipeline`). What the Preview tab shows
  as it runs: the block under the crosshair in full (block, biome, face hit and every face's texture with its pack,
  material, shader, position) and every block, biome, texture, material and shader in view, from a grid of rays
  `DevSceneAssetsSystem` casts across the view about once a second while a Scene Assets tab is open. A row naming
  content opens it in the Properties tab.
- **Animations are edited on the same timeline.** There is no animation timeline of its own: the Animation Editor is the
  viewport, showing the character chosen (the player at first) posed at the Timeline's playhead, beside the Recorder,
  Timeline, Keys, Keys Graph and Layers tabs. While animations are worked on (the Animation Editor worked in since any
  Dev window) the Tracks tab lists the clips on that character's rig instead of the media files, and a clip clicked
  there or picked in the Hierarchy opens on the Timeline in the media file's place (saved first), each of its layers
  (its own tracks and morphs, then every part a stamp joined) a clip on a lane of its own, together the one clip; the
  layer pressed is the one whose bones (nested as the rig nests them) and morphs the Layers tab lists, each axis of each
  channel a curve on the Keys and Keys Graph tabs, keyed, moved and bent like any take's; Record turns the recording of
  keys on and off as Blender's record button does, and only while it is lit does posing in the Animation Editor key the
  bones moved at the playhead; the Tracks tab's Media / Animations switch turns its list either way between; the
  Recorder plays it, Undo and Redo step one history for every edit of it, a pose drag one step, and the bone picked in
  the Layers tab is the Animation Editor's and the other way round (see `.claude/docs/CharacterEditorPipeline.md`,
  Animation). Its clip is never moved, cut or recorded into, and it saves, reverts and deletes as content.
- **The one exception** is the Shipwright: it is shared with the game, so it keeps its own tools and layout.
- **Layouts are the user's.** Every layout lives in the user's `bin/editorLayout/` folder and is listed in the Layouts
  menu as Photoshop lists its workspaces (`LayoutManager`); the editor ships none. Only when the owner hands over
  layouts of their own to ship are any added to the project.

## Layers

- **One canvas, many layers.** A window whose canvas does several jobs lists them as layers in the shared Layers tab
  (`editor.bootstrap.layerspipeline`), as Krita and Photoshop list theirs: the layer picked is the one the window's
  tools work on, groups fold what they hold, and an eye shows or hides a layer that can be hidden. The Layers tab
  lives beside the Hierarchy and Properties tabs (the Tools menu) and, like Properties, shows the window worked in
  last that has layers. This is the standard for putting several tools in one window, and the base the editor's
  image editing is built on.
- **A window that has layers** implements `LayersProvider` (its revision, title, the stack as `LayerNodeStruct`s top
  first, `selectLayer`, `toggleLayerVisible`, and the tool action keys the tab's New Layer and Delete Layer icons run,
  `getAddLayerKey` / `getRemoveLayerKey`, empty where the stack takes none) and creates a `LayersSourceSystem`. Layer
  keys are stable names (an enum's name, `region/mode`, a layer document's name), never IDs. The window keeps the layer
  picked itself and every system reads it there; a layer picked hands its tool canvas, Properties rows and File and Edit
  table to the shared tabs through the window's own providers, which answer for the layer picked.
- **So far:** the World Editor (Heights over Biomes, each with an eye), the character Mesh, Topology, Sculpt, Weights,
  Rig and UV tabs (the Body and every region within it, one region picked for all of them; every region but the Body
  has an eye hiding its vertices on the part edited, `CharacterEditorManager.toggleRegionShown`), the character
  Paint tab (the paint layers of the map painted, each above the base with an eye), the Image Editor (every layer of
  the image, each with an eye; its maps are listed in the Palette tab, never here) and the Item Editor (every part of the
  item, each one material, the part picked being the one its tools lay; no eyes).
- **Up and Down** pick the layer listed above or below the one picked while the Layers tab is worked in
  (`LayersManager.stepLayer`, `assets/shortcuts/Layers.arpg`).
- **New Layer and Delete Layer** are the icons at the foot of the Layers tab, as Photoshop's and Krita's Layers
  dockers have them; each runs the window's own tool action through `LayersManager.addLayer` / `removeLayer`, so the
  icon, the Tools tab's action and its shortcut do one thing.

## Search

- **One way to type a search.** Every search box types and matches through `SearchFieldStruct`
  (`editor.runtime.menueventsmanager.menus.search`): a click focuses it, it takes any character the editor font draws,
  matches ignoring case, shows its placeholder muted while empty and unfocused, Escape clears it and a click elsewhere
  puts it down. While one is focused the shortcuts hold off (`ShortcutManager.isWithheld`), as for every typing dialog.
- **The toolbar's search** (`EditorSearchBranch`) is a search everywhere over the toolbar menus' items, read from each
  menu's own dropdown data the first time it is used (an item is any node with an action and a name; a node whose id
  ends `_header` names the section below it), so a menu item added is found with nothing more to write. Matches list
  under the box, names opening with the query first, then names holding it, then items whose menu or section holds it;
  Up and Down mark one, Enter or a click runs the menu item's own action. Never keep a second list of the menus.
- **The Settings dialog's search** (`EditorSettingsBranch`) filters the rows of every page by name and value, each
  page under a header of its own name, a section kept whole when its name or its page's matches; choosing a page
  clears it.

## The active window

- **Exactly one editor window is active.** `ActiveWindowManager` (`editor.bootstrap.tabpipeline`) owns it: the tab
  content window focused last, through its view or its tab's chrome. Pressing the mouse in a window makes it active
  and performs the press in the same frame; the first click is never swallowed. Every consumer reads that owner (the
  File and Edit menus, the Tools tab, the Properties tab, the shortcuts, the character mode and recorder pane systems)
  and the ones that act only on some windows take the most recent of those through `findRecent`. Never keep a copy.
- **Hover-only input stays hover-only.** The wheel scrolls and zooms whatever the pointer is over, active or not
  (`EditorInputSystem.isPointerOver`); clicks, drags and keys need the active window (`isPointerActive`).
- **The active window is marked.** Its tab header is tinted and its border drawn in the theme's Active Window color,
  and a hovered inactive tab's header takes the fainter Hovered Window tint, both on the Settings dialog's Colors
  page under Window Frame.
- **Everything says what it acts on.** A shared tab or menu that acts on a window other than its own names that window
  after its text in parentheses, the one way, through `ActiveWindowManager.nameContext` (the window's tab title):
  "Properties (Sculpt)", "Palette (World Editor)", the Tools, Layers and Palette tabs' titles, every File and Edit
  item the tool takes ("Save (World Editor)"), the Tools menu's shared tabs as it opens ("Layers (Mesh)",
  `EditorBranch.refreshToolsMenu`) and the head of the hint bar ("(World Editor)"). Never build such a
  label by hand.
- **The hint bar** along the foot of the main window lists the active window's shortcuts for its current canvas and
  tool, read from the `hint`s in its `assets/shortcuts/` file (see `.claude/docs/Shortcut.md`), never hard-coded.
  It is rebuilt only when the active window, its canvas, its tool or a shortcut's keys change.
- **Text is never unreadable.** Text shrinks to fit its element no smaller than the Minimum Text Size on the Settings
  dialog's General page; past that it is cut behind an ellipsis. Docked panels never split smaller than
  `EditorSetting.DOCK_PANEL_MIN_SIZE`.
- **Tab strips shrink, then wrap.** Every tab bar (a dock's strip, the Hierarchy's tabs) lays out through
  `StripLayoutUtility`: each tab is as wide as its whole title (`ElementInstance.resolveTextWidth`) while its row has
  room; as it narrows the widest shrink first, down together to one shared width (`resolveRowCap`), each no narrower
  than its whole title at the Minimum Text Size, so every letter stays visible; and once no tab can shrink further the
  strip wraps onto another row (`resolveRowEnd`), the header growing and the content shrinking beneath it. Never a fixed-width strip that hides
  tabs, and never a second wrapping routine.

## Tools and brushes

- **Every editor tool lives in the Tools tab**, chosen there or by the shared tool keys, for whatever canvas was
  worked in last (see `.claude/docs/Engine.md` and `.claude/docs/Shortcut.md`). A tool is never a panel button.
- **Every tool has an emblem.** Each `EditorTool` shows its icon beside its name, the sprite named after it in
  PascalCase under `menus/tools/` (`EditorTool.getIcon`, `EditorSetting.TOOL_ICON_FOLDER`): a new tool adds its 16-pixel
  white emblem there, in the same line style as the rest.
- **Brushes live in the Brushes tab, never in the Tools tab.** The Tools tab shows tools, their actions and their
  options only. Every stroking tool on every canvas stamps with the one brush chosen in the Brushes tab, and so does
  Select wherever the canvas sizes a brush (the character Mesh and Weights tabs' vertices), selecting as much as the tip
  covers so the tools that follow act through its falloff.
- **Brushes work like Krita's presets:** soft or hard, round, square or diamond, turned and squashed, dense or
  speckled, and each listed with a thumbnail of the stroke it makes. Keep plenty of them; a new kind of tip is a new
  `assets/brushes/` file, not code.
- **Stamps live in the Stamps tab**, as brushes live in the Brushes tab: tiles of every body part with its emblem and
  name, the one in use tinted, its details below. A part chosen arms the Sculpt tab's Brush: the Brush places it
  instead of stroking, at the Brush's Scale and Turn, or lays its fur combed by Comb, and puts it down once placed or
  on Escape (see `.claude/docs/CharacterEditorPipeline.md`). There is no Stamp tool and no tab of its own. The game's
  stamps are fixed entries: nothing makes a new one yet. While the Image Editor is the canvas worked last, the tab
  lists pictures instead (every PNG in the workspace's `Stamps` folder), and a picture chosen arms the Image Editor's
  Brush the same way: it presses the picture at the click, at the Brush's Scale and Turn, then puts it down (see
  `.claude/docs/ImageEditorPipeline.md`). The tab lists only the kind the canvas worked last takes.
- **A tool means one thing everywhere.** Raise builds up and Lower wears down, a soft brush fades its edge and a hard
  one keeps it, on whichever canvas takes them. A canvas whose values cannot blend (biome colors name biomes) simply
  does not take the tools that blend.

## Materials

- **One list of materials.** The Materials tab (`editor.runtime.materials`, under the Materials menu) lists the
  workspace material open, then every texture of every baked pack, pack by pack, as Substance and Blender list a
  material library, as a grid of tiles under each pack's header, each showing what the material looks like (its
  albedo, `MaterialThumbnailBranch`, made a few a frame, `MATERIALS_THUMBNAILS_PER_FRAME`, and kept while the editor
  runs) over its name, as many to a row as the tab's width holds. `MaterialsManager` (`editor.bootstrap.materialspipeline`) keeps the material chosen, which every
  window that lays a material reads: the Hair tab's new strands and Brush lay it (hair, `characters/standard`'s strand
  textures, draws as intended; any other is the owner's choice), the Item Editor gives it to a new part, the Paint
  tab's Brush and Fill lay it in place of the colour and the Material Viewer shows it. A row pressed is chosen and picked up; let go over a
  window that implements `MaterialDropProvider`, the material is dropped there once (`MaterialsManager.update` sees
  the release), and the Item Editor colours the part under the pointer with it.
- **A material is a texture set and its parameters**, as Substance and Unreal's material instances hold one: the
  texture it is made from (its base), the texture or the PNG image any of its maps (Albedo, Normal, Height, AO,
  Metallic, Roughness, Tint) is taken from instead (an image wins), and the parameters the surface shaders take (Base
  Color, Tiling, Normal Strength, Roughness Offset, Metallic Offset; `MATERIALS_PARAMETER_*`). The shader is the
  surface's own (character, item, block): a painted material is drawn by whatever draws the surface it lands on, so a
  material names no shader of its own. `MaterialSampleBranch` reads every map's image texel by texel the one way the
  material brush and Export share, and hands the same images to the viewer as editor textures
  (`MaterialsManager.usePreviewTexture`, made again only when an image changes), whose shader applies the parameters
  as the brush does. A map wanting a generated image (a normal or roughness from an albedo) takes the Image Editor's
  exported map: generate it in the Palette tab, Export, then choose the PNG.
- **Paint a material** as Substance Painter does: there is no material tool. The Paint tab's Brush and Fill paint the
  Palette's colour, or, while the Materials tab was worked in after the Palette
  (`MaterialsManager.isPaintingMaterial`, `ActiveWindowManager.findRecent`), the material chosen there in its place:
  working in either tab switches what they lay. The material is laid on every map at once, each texel taking the material's own at its UV repeated by the Tiling, through the brush's coverage and Strength
  (`CharacterPaintBranch.stamp`); a stroke is one undo step across every map and texture it reached. Characters,
  worn clothing and baked items are all painted there.
- **The Material Viewer** (`editor.runtime.materialviewer`, under the Materials menu) shows the material chosen on a
  sphere, cube, cylinder or plane, the Tools tab's Shape option (`ToolCanvas.MATERIAL_VIEWER`, a canvas-wide choice),
  lit by one light its Light tool (key `G`) turns when dragged from the shape; the camera is the shared
  `ViewCameraSystem`. While the Materials tab or the viewer was worked in last, the Properties tab holds the material's
  rows
  (`MaterialRowBranch`): a game texture's name and how to make a material from it, or the workspace material's base,
  each map's texture (From Base or any material) with a Choose Image action under it (the system's file dialog,
  starting in the workspace's Exports), and every parameter as a slider. Edit, Undo and Redo (Ctrl+Z, Ctrl+Y) step
  through every edit of the open material, a slider's drag one step (`MaterialHistoryBranch`), and Ctrl+S saves.
- **Workspace materials.** File, New names a material made from the texture chosen and writes
  `Assets/Materials/<Name>.arpg` at once (`base`, `maps` and `images` keyed by alias, an image kept by its path below
  the workspace or in full outside it, `parameters` by key); Open, Save, Revert and
  Delete work on it; Export writes its seven maps, shaped by its parameters and once across the base's size (Tiling is
  how it is laid, never baked), as `Exports/Materials/<Name>/<Name>_<suffix>.png`; Import takes them into a pack as a
  tile and bakes it through the Image Editor's one import path (`ImageEditorManager.importMaps`). Hair and items take
  only the game's textures, so while a workspace material is chosen they take its base until it is imported.

## Item Editor

- **Sub-voxel items, worked as a voxel modeller's.** Every part is one material and a layer in the Layers tab, added
  and deleted by its icons or the Tools tab's Parts actions (`ITEM_EDITOR_KEY_PART_ADD`, `_REMOVE`), renamed by F2.
  The tools (`ToolCanvas.ITEM`) are Brush, Wall, Fill, Pick and Move; Brush stamps the Brushes tab's tip on the face
  struck, building, or digging while Ctrl is held (`ItemEditor/InvertStroke`), Size in sub-voxels (none is one
  sub-voxel, the usual way to work), each column of the tip as deep as its coverage times Strength of the Depth (one
  layer unless raised), so a soft tip thins at its feathered edge; the stamp is shown as the sub-voxels it would build or
  dig, merged like the item. A stroke is held to the layer it began on (`ItemBrushBranch`). Move only looks: every press
  goes to the view camera. The mesh is greedily merged by `SubVoxelManager` as it changes.
- **Workspace items.** New, Open, Save, Revert and Delete work on `Assets/Items/<Name>.arpg`, a sub-voxel mesh file;
  Import writes `assets/mesh/items/<Name>.arpg` and makes an item naming it in the file asked for, unless content
  already names that mesh. An item of the game opens when picked in any Hierarchy tab: the item itself, the item an
  entry of a schema that names items names (a furnishing, a loot table), or the item naming the entry (an instrument),
  and saves in place.
- **Export bakes the item for the mesh tools** (`ItemBakeBranch`), as a voxel modeller exports a mesh: the greedy quads
  become a rigged mesh skinned wholly to the root of the shared one-bone `ItemRig`, every quad its own UV island laid in
  the block's texel directions and shelf-packed into one atlas (`ITEM_BAKE_ATLAS_SIZES`, up to
  `ITEM_BAKE_TEXELS_PER_CELL` texels a sub-voxel), every map the parts' materials carry baked into it part by part as a
  material-ID fill, written as `assets/textures/items/baked/<Item>_<suffix>.png`, baked and shown at once
  (`TextureManager.applyTileBake`). The mesh opens in the character tabs as the workspace model `items/<Item>`
  (`CharacterEditorManager.openBakedModel`; an open one takes it as one edit), to paint, unwrap and model as any mesh.
  Baking again lays the maps out afresh from the cubes, painting over the last bake: paint after the shape is done.
  The first modelling tab (Mesh, Topology, Sculpt, Weights, Paint, Rig, UV) made active after the Item Editor was
  worked in shows the item's model at once (`ItemEditorManager.showActiveItemInMeshTabs`): its last export when it has
  one, never baked over, else a first export. The Item Editor shares the modelling tabs' view when stacked with them
  (`VIEW_CAMERA_SHARE_CHARACTER`), its cubes and their baked mesh lying in the same block space.
  The Tools tab's Paint Item action (`ITEM_EDITOR_KEY_PAINT`) does it in one step: it exports, brings the Paint tab
  forward (`TabManager.showCharacterPaint`, a new one only when none is open) and chooses its Brush.
- **Painted items reach the game** through the mesh tabs' File, Import: an item model (`items/<Item>`) is written to
  `assets/mesh/items/painted/<Item>.arpg` and named by the item's `painted` field, which the item it paints offers
  first (the one whose sub-voxels are `items/<Item>`). The item keeps its sub-voxels for its shape, grip and contents;
  `PaintedMeshBranch` builds what it is drawn with in the painted layout (`PaintedItemVAO`: position, normal, UV,
  tile), and `ItemModelManager.resolveMaterial` draws it with the painted twin of whatever item material it is asked
  for (`ITEM_PAINTED_MATERIAL_TWINS`; a viewer of its own registers its twin, as the Ship Viewer does).
- **Rigs.** Every baked item stands on the shared `ItemRig`, so no item needs a rig, and its bones cannot be changed
  (`CharacterRigBranch.refuseShared`). The Rig tab's New Rig For The Model gives any workspace model a rig of its own:
  a copy of its rig named after it, with a rest clip and an animation tree playing it to preview it by, written and
  made known at once, the model then opening again on it; Add Child Bone and Remove Bone (or Place and Erase) then
  shape it.

## The File and Edit menus

- **Every file action lives in the editor's File and Edit menus**, never as a tool's own button: New, Open,
  Duplicate, Save, Save All, Revert, Delete, Import and Export under File, Undo and Redo under Edit. New is the one way
  to make anything: it names what it makes, and a workspace file is written at once (in the Hierarchy, New makes a new
  file when a folder is picked in a schema that keeps its entries in files, a new entry otherwise). Open picks a workspace file in the system's own
  file dialog (`WorkspaceUtility.chooseFile`, LWJGL's tinyfd), listing only the files the window takes (`.arpg`, and
  `.glb` too in the character tabs) and taking a workspace file only from the folder of the window's kind. Generating maps is not a file action: Generate and Clear live under the map's settings in the
  Palette tab. They act on the last window that takes file actions (`editor.bootstrap.filemenupipeline`,
  `FileMenuManager.getTargetWindow`), name it at their head and after every item it takes, and mute what it does not
  take. A shared tab (Properties, Tools, Layers, Palette) takes none, so working in it leaves the menus on
  the tool worked in before; the Properties tab's Ctrl+N, Ctrl+D, Ctrl+S, Ctrl+Shift+S, Ctrl+Z and Ctrl+Y run the File
  menu's items (`FileMenuBranch.run`), so a key there does what the menu says.
- **A tool names its actions as data.** Its context implements `FileMenuProvider`, returns its
  `FileMenuTableStruct` constant from `EditorSetting` (the branch and, per `FileMenuAction`, the method its own keys
  already run) and creates a `FileMenuSourceSystem`. Every item runs through `ElementHitSystem.executeAction` on the
  tool's window, the route its shortcuts take, so a menu item and its key always do the same thing. A new tool adds a
  table; it never adds a Save, New, Revert, Undo or Redo button.
- **The Shipwright keeps its own buttons** because the game shares its table; the File and Edit menus reach it all the
  same through `EditorSetting.FILE_MENU_SHIPWRIGHT`.

## The workspace

- **New work lives in the workspace**, `Documents/My Games/AdventureRPG/Assets/` beside the saves, one folder to each
  kind (`Characters` for models, `Hair` for grooms, `Items` for items, `Materials` for materials, `Images`, `Exports`
  for exported maps and, under `Exports/Hair` and `Exports/Materials`, exported hair cards and material maps, `Stamps` for stamp pictures, `Models` for `.glb` files, `Media` for songs and media files, video and music), resolved only through
  `WorkspaceUtility.resolveFolder`. A workspace file is not game
  content: the Hierarchy never lists it, and it reaches the game only through Import, as a painting or modelling
  tool's project file reaches an engine through its export.

## Values

- **Every number has a slider and a typed value.** A value row shows its name, a slider across its range, a box
  that opens for typing an exact value (the "manual set"), and step buttons. Dragging the slider updates the value
  and everything showing it live, in place, without rebuilding the panel.
- **Every value takes the control that fits it.** A switch (on or off: Mirror, X-Ray) is a switch flipped by one
  click and filled while on; a choice of named settings (Shape, Comb) is a drop-down naming the one in use, its choices
  listed under its row while open; only a number has a slider. A tool option declares its kind in `ToolCanvas`
  (`ToolOptionKind`: `toggle(...)`, `choice(...)` or a number range) and the Tools tab picks the row template for it
  (`ToolsSetting.TEMPLATE_OPTION_ROWS`), never a slider over named steps.
- **One slider path.** Sliders share `editor.runtime.slider.SliderUtility` (track share, step, knob); typed values go
  through the name dialog and the same setter the slider and step buttons use.
- **Every slider has a range.** A value with a declared range (a tool option, a race's shape slider in the Appearance
  tab's Properties rows) slides across it; a character, face, animation or tree value slides across a fixed count of its
  own steps centred where it stood when its row was listed, through the tool's own step, so the tool's clamps still
  hold. Either way a typed value can go anywhere the setter allows.

## The Palette tab

- **One Palette, where a tool's choices are picked.** The Palette tab (`editor.runtime.palette`) is where the window
  worked in last offers what its tools pick from, as Krita's and Photoshop's palettes and swatches are: Properties is for
  the settings of what is picked, the Hierarchy for picking content by hand, Tools for tools, Brushes for brushes. A
  window that has a palette implements `PaletteProvider`, naming its `PaletteContent` (its mode or layer may name it),
  and is followed through `PropertiesManager.getPalette`; the tab hands it to the `PaletteView` system for that content,
  as the Properties tab does. A window whose palette is a list implements `PaletteRowSource`
  (`buildPaletteRows`, its revision, templates and slider steps), the one path every listed palette takes
  (`RowPaletteSystem`). The contents: `IMAGE`, the colour wheel (`ColorWheelSystem`) over the window's rows (the maps of
  the image it paints, and the parts it picks from), for the Image Editor, the UV Editor and the Paint and UV tabs, the
  colour's sliders in the Properties tab while the Palette was worked in last; `ROWS`, the list a tool picks from (the
  Equipment tab's gear and anchors, the modelling tabs' parts, the Mesh, Weights and Rig tabs' bones, the Animation
  Editor's bones, the Animation Tree Editor's layers and nodes), the Properties tab keeping that window's settings
  while the Palette is worked in; `BIOMES`, the World Editor's Biomes layer, a grid of swatches with each biome's name
  under it, as many to a row as the tab's width holds, the one chosen highlighted and selected in the Hierarchy so its
  fields are the Properties tab's; `NONE`, a mode or layer with nothing to pick from (Appearance, Heights).
- **The standard every tool follows.** What a tool picks from (parts, bones, gear, nodes, takes, biomes, colours) is
  the Palette's; the settings of what is picked are the Properties tab's; content is picked by hand in the Hierarchy;
  tools are the Tools tab's and brushes the Brushes tab's; a canvas's jobs are layers in the Layers tab. A tool never
  lists what it picks from in the Properties tab or in a panel of its own. A new kind of choice is a new
  `PaletteContent` and a `PaletteView` system, never a new window. Still to move: the Recorder's takes (now the Layers
  tab's Takes view), with the media workflow.

## The Properties tab

- **One shared Properties tab, never a properties or outliner window per tool.** It shows what the window worked in
  last provides (`editor.bootstrap.propertiespipeline`): a window implements `PropertiesProvider`, names its
  `PropertiesContent` and creates a `PropertiesSourceSystem`. `ROWS` are a tool window's settings (the window is also a
  `CharacterViewRowSource`): the values of what is picked (the bone, key, node, part or UVs selected), then the
  settings of the window itself (the preview's gender, pose and side, Appearance's sliders and features, the clip, the
  recording's settings, the keys selected, how bones are drawn), as Substance Painter's Properties and Krita's Tool
  Options are; `INFO` is the content entry selected in the Hierarchy, the editor's Info Panel, which the Hierarchy and
  every content tool show. What a window picks from is never here: it is the Palette tab's (above). A new kind of
  content is a new `PropertiesContent` and a
  `PropertiesView` system in the Properties tab, never a new window. Its status line always works on the Hierarchy's
  selection; it takes no File or Edit actions of its own (see The File and Edit menus).
- **Headers fold.** Every header row of a tool's rows (`ROWS`, the Palette tab's rows, any `CharacterViewPanelSystem` panel) is a
  button that folds the rows under it shut or opens them again, its marker (`row_fold_label`, + or -) saying which; the
  panel keeps what it was handed, so a fold lays it out again without asking the tool, and the folds of a panel hold
  while it lists other windows' rows. A tool's header template carries the `toggleHeader` action and the marker.
- **Info rows are laid out for editing.** Every field reads as words (`animation_tree` is Animation Tree), plain values
  before groups. A number has a slider (across its declared range when it has both ends, otherwise steps about its
  value), step buttons and a typed value; a boolean, an enum, or a string naming other content or a file is a choice
  stepped through every value it may take, `allow`ed values such as none first, and still typed. A group standing in a
  list or a map has a Copy button that duplicates it beside itself. Rows refresh in place while their layout holds, so
  a dragged slider keeps its row. Any schema gains all of this by declaring its types, ranges and references; never
  write a tool window to edit one kind of content's fields.
- **The Info entry follows whatever was last selected, in any window**: a brush, a biome, a tree, a ship, a
  settlement, a character, an animation. A window that selects content routes the choice through
  `InfoManager.selectEntry`, and content picked in the Hierarchy becomes the window's choice through a selection
  listener, so both directions stay in step.
