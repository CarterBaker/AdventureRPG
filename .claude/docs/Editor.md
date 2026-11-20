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
- **Name a tab for what it holds.** A list of biomes is the Biome Outliner; a window that paints the world's images is
  the World Editor. Never a generic name that hides what the tab is for.
- **Tools sit at the foot of each menu.** The editor's menus (`menus/editor/EditorWindow.arpg`) list a tab under the
  section for what it works on, and the tabs that serve the others (the Tools and Brushes tabs, the Outliner, Maps,
  Stamps and the Color Wheel, the Recorder) under a Tools section, always the menu's last: Content's Tools holds Tools
  and Brushes, Entities' and Mesh's the Outliner, Textures' Maps, Stamps and the Color Wheel, Media's the Recorder. The
  Materials menu, after Textures, holds the Materials tab, the editor's one list of materials (see Materials below).
  The Entities menu (once Characters) holds what only an entity has (its Entity section: Appearance, Equipment, Hair;
  then Animation); the Mesh menu holds the general modelling tools any rigged model takes
  (its Mesh section: Mesh, Topology, Sculpt, Paint, UV, UV Editor; its Rigging section: Rig, Weights). A tab or tool
  shown to the owner says Entity, never Character; the code's `Character*` class names stay as they are. The Debug
  menu's Preview opens `DevContext`, the game as it runs with the command console's systems; there is no other runtime
  tab. The Command Console is one window whose view follows the window worked in last
  (`CommandManager.resolveViewTarget`): the character tabs' commands while a character tab was, the game's while a Dev
  window was.
- **Split so far:** the World Editor paints the biomes and places settlements and cameras on its Biomes layer and raises
  and lowers the ground on its Heights layer, the biome it paints chosen in the Biome Outliner and shown in the
  Properties tab, while the Biome Planner only plans; entities and races, like all content, are picked in the Hierarchy
  and edited in the Properties tab; every part of making a race's character is its own character tool tab (Appearance,
  Equipment, Hair, Mesh, Topology, Sculpt, Weights, Paint, Rig and UV, see `.claude/docs/CharacterEditorPipeline.md`;
  the Hair tab grooms strands painted with the material chosen in the Materials tab; the
  Appearance outliner holds every slider and control, the Rig tab marks which bone channels are sliders), sharing the
  region, bone and vertices selected, the UV Editor laying out the UVs of the faces the UV tab selects, with the
  Outliner beside them (also the Animation Editor's), the Color Wheel holding the paint colour every painting window
  shares and the Maps tab the maps of the image worked (the texture painted, or the Image Editor's image); the Image
  Editor under Textures paints layered images as Krita does, its layers in the Layers tab and its maps in the Maps tab
  (see `.claude/docs/ImageEditorPipeline.md`); the Recorder is its transport (Record, Play, Pause, Stop and the Loop
  switch, which runs playing round the range marked with I and O or the whole media file, with the clock and the status
  line that says what they did or why they could not, nothing else) and the Timeline its clips, with the Keys (the rows
  of the layer picked in the Layers tab) and the Keys Graph (the curve selected, a key clicked twice freeing or joining
  its handles) beside them, each opening its own menu through a shared `RecorderRowSystem`, and the pane tab worked in
  last being the one the recorder's deletes act on. A take records whether or not the game is paused, so the pause menu
  that frees the cursor never stops it; music plays with no Preview tab, through the Recorder or Timeline tab. The
  Layers tab lists the take's tracks (camera, player, characters, items, vehicles, weather, clock, sounds) or, in its
  Takes view, every take of the open recording, each dragged onto the Timeline's lanes to lay it, each pane keeping its
  own view (the Timeline opens on the Takes, the Keys and Keys Graph on the take's layers); the Tools tab holds every
  take, clip, key, sound, transition and node action; the Properties tab the clip's channels, mute and time ramp; the
  Character Outliner the channels armed and the export while the Recorder or Timeline is worked in, and the keys
  selected (their time and value, moved together, and their mode) while the Keys or Keys Graph is; Delete removes what
  is selected in the pane worked in last; Edit, Undo and Redo (Ctrl+Z, Ctrl+Y) step through every edit of the open
  recording (`RecordingHistoryBranch`); File, Export exports. The Media tab lists every media file, video and music in one list, and its rows drag onto the Timeline to lay;
  the Timeline shows its lanes as Premiere does, five at first and one more each time the highest takes a clip, each
  strip only its number and the name its ... gives it, the lane and clip selected edited in the Properties tab (its mix
  too), and the file's tempo, meter and key set in the Properties tab while the Recorder is worked in; the Note Library,
  under Library in the Media menu with the Sound Library, stays empty until an instrument or a note set is picked in the
  Hierarchy and then lists the notes it plays, each sounded by its button and dragged onto the Timeline as a note clip
  that is stretched and cut like any clip. A library's drag, like Record, makes a media file when none is open. The Timeline's title names what is open; Back is a Recorder shortcut. The Timeline's view, once zoomed,
  scrolls under the cursor with a left drag on its empty lanes or a right drag anywhere over its lanes or ruler, clips
  included. The Scene Assets tab, under Preview in the Debug menu (`editor.bootstrap.sceneassetspipeline`), lists what the Preview tab shows as it
  runs: the block under the crosshair in full (block, biome, face hit and every face's texture with its pack, material,
  shader, position) and every block, biome, texture, material and shader in view, from a grid of rays
  `DevSceneAssetsSystem` casts across the view about once a second while a Scene Assets tab is open; a row naming
  content opens it in the Properties tab.
- **The one exception** is the Shipwright: it is shared with the game, so it keeps its own tools and layout.

## Layers

- **One canvas, many layers.** A window whose canvas does several jobs lists them as layers in the shared Layers tab
  (`editor.bootstrap.layerspipeline`), as Krita and Photoshop list theirs: the layer picked is the one the window's
  tools work on, groups fold what they hold, and an eye shows or hides a layer that can be hidden. The Layers tab
  lives beside the Hierarchy and Properties tabs (the Content menu) and, like Properties, shows the window worked in
  last that has layers. This is the standard for putting several tools in one window, and the base the editor's
  image editing is built on.
- **A window that has layers** implements `LayersProvider` (its revision, title, the stack as `LayerNodeStruct`s top
  first, `selectLayer`, `toggleLayerVisible`, and the tool action keys the tab's New Layer and Delete Layer icons run,
  `getAddLayerKey` / `getRemoveLayerKey`, empty where the stack takes none) and creates a `LayersSourceSystem`. Layer
  keys are stable names (an enum's name, `region/mode`, a layer document's name), never IDs. The window keeps the layer
  picked itself and every system reads it there; a layer picked hands its tool canvas, Properties rows and File and Edit
  table to the shared tabs through the window's own providers, which answer for the layer picked.
- **So far:** the World Editor (Heights over Biomes, each with an eye), the character Mesh, Topology, Sculpt, Weights,
  Rig and UV tabs (the Body and every region within it, one region picked for all of them; no eyes), the character
  Paint tab (the paint layers of the map painted, each above the base with an eye), the Image Editor (every layer of
  the image, each with an eye; its maps are listed in the Maps tab, never here) and the Item Editor (every part of the
  item, each one material, the part picked being the one its tools lay; no eyes).
- **New Layer and Delete Layer** are the icons at the foot of the Layers tab, as Photoshop's and Krita's Layers
  dockers have them; each runs the window's own tool action through `LayersManager.addLayer` / `removeLayer`, so the
  icon, the Tools tab's action and its shortcut do one thing.

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
- **The hint bar** along the foot of the main window lists the active window's shortcuts for its current canvas and
  tool, read from the `hint`s in its `assets/shortcuts/` file (see `.claude/docs/Shortcut.md`), never hard-coded.
  It is rebuilt only when the active window, its canvas, its tool or a shortcut's keys change.
- **Text is never unreadable.** Text shrinks to fit its element no smaller than the Minimum Text Size on the Settings
  dialog's General page; past that it is cut behind an ellipsis. Docked panels never split smaller than
  `EditorSetting.DOCK_PANEL_MIN_SIZE`.
- **Tab strips shrink, then wrap.** Every tab bar (a dock's strip, the Hierarchy's tabs) lays out through
  `StripLayoutUtility`: each tab keeps its preferred width while its row has room, all shrink together as it narrows,
  and once none can shrink past its minimum the strip wraps onto another row, the header growing and the content
  shrinking beneath it. Never a fixed-width strip that hides tabs, and never a second wrapping routine.

## Tools and brushes

- **Every editor tool lives in the Tools tab**, chosen there or by the shared tool keys, for whatever canvas was
  worked in last (see `.claude/docs/Engine.md` and `.claude/docs/Shortcut.md`). A tool is never a panel button.
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

- **One list of materials.** The Materials tab (`editor.runtime.materials`, under the Materials menu) lists every
  texture of every baked pack, pack by pack, as Substance and Blender list a material library. `MaterialsManager`
  (`editor.bootstrap.materialspipeline`) keeps the material chosen, which every window that lays a material reads: the
  Hair tab's new strands and Brush lay it (hair, `characters/standard`'s strand textures, draws as intended; any other
  is the owner's choice), and the Item Editor gives it to a new part. A row pressed is chosen and picked up; let go over
  a window that implements `MaterialDropProvider`, the material is dropped there once (`MaterialsManager.update` sees
  the release), and the Item Editor colours the part under the pointer with it.

## Item Editor

- **Sub-voxel items, worked as a voxel modeller's.** Every part is one material and a layer in the Layers tab, added
  and deleted by its icons or the Tools tab's Parts actions (`ITEM_EDITOR_KEY_PART_ADD`, `_REMOVE`), renamed by F2.
  The tools (`ToolCanvas.ITEM`) are Brush, Erase, Wall, Fill and Pick; Brush and Erase stamp the Brushes tab's tip on
  the face struck, Size in sub-voxels (none is one sub-voxel, the usual way to work), each column of the tip as deep as
  its coverage times Strength, so a soft tip thins at its feathered edge; the stamp is shown as the sub-voxels it would
  build or dig, merged like the item. A stroke is held to the layer it began on (`ItemBrushBranch`). The mesh is
  greedily merged by `SubVoxelManager` as it changes.
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
  Maps tab. They act on the window worked in last (`editor.bootstrap.filemenupipeline`,
  `FileMenuManager`), name it at their head and mute what it does not take.
- **A tool names its actions as data.** Its context implements `FileMenuProvider`, returns its
  `FileMenuTableStruct` constant from `EditorSetting` (the branch and, per `FileMenuAction`, the method its own keys
  already run) and creates a `FileMenuSourceSystem`. Every item runs through `ElementHitSystem.executeAction` on the
  tool's window, the route its shortcuts take, so a menu item and its key always do the same thing. A new tool adds a
  table; it never adds a Save, New, Revert, Undo or Redo button.
- **The Shipwright keeps its own buttons** because the game shares its table; the File and Edit menus reach it all the
  same through `EditorSetting.FILE_MENU_SHIPWRIGHT`.

## The workspace

- **New work lives in the workspace**, `Documents/My Games/AdventureRPG/Assets/` beside the saves, one folder to each
  kind (`Characters` for models, `Hair` for grooms, `Items` for items, `Images`, `Exports` for exported maps and, under
  `Exports/Hair`,
  exported hair cards, `Stamps` for stamp pictures, `Models` for `.glb` files), resolved only through
  `WorkspaceUtility.resolveFolder`. A workspace file is not game
  content: the Hierarchy never lists it, and it reaches the game only through Import, as a painting or modelling
  tool's project file reaches an engine through its export.

## Values

- **Every number has a slider and a typed value.** A value row shows its name, a slider across its range, a box
  that opens for typing an exact value (the "manual set"), and step buttons. Dragging the slider updates the value
  and everything showing it live, in place, without rebuilding the panel.
- **Every value takes the control that fits it.** A switch (on or off: Mirror, X-Ray) is a switch flipped by one
  click and filled while on; a choice of named settings (Bones, Comb) is a drop-down naming the one in use, its choices
  listed under its row while open; only a number has a slider. A tool option declares its kind in `ToolCanvas`
  (`ToolOptionKind`: `toggle(...)`, `choice(...)` or a number range) and the Tools tab picks the row template for it
  (`ToolsSetting.TEMPLATE_OPTION_ROWS`), never a slider over named steps.
- **One slider path.** Sliders share `editor.runtime.slider.SliderUtility` (track share, step, knob); typed values go
  through the name dialog and the same setter the slider and step buttons use.
- **Every slider has a range.** A value with a declared range (a tool option, a race's shape slider in the Character
  Appearance outliner) slides across it; a character, face, animation or tree value slides across a fixed count of its
  own steps centred where it stood when its row was listed, through the tool's own step, so the tool's clamps still
  hold. Either way a typed value can go anywhere the setter allows.

## The Properties tab

- **One shared Properties tab, never a properties window per tool.** It shows what the window worked in last
  provides (`editor.bootstrap.propertiespipeline`): a window implements `PropertiesProvider`, names its
  `PropertiesContent` and creates a `PropertiesSourceSystem`. `ROWS` are a character, face or animation tab's value
  rows (the window is also a `CharacterViewRowSource`), and `INFO` the content entry selected in the Hierarchy, the
  editor's Info Panel, which the Hierarchy and every content tool show. A new kind of content is a new `PropertiesContent` and a `PropertiesView` system in the Properties tab,
  never a new window. Its status line always works on the Hierarchy's selection.
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
