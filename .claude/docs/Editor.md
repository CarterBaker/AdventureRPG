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
- **Split so far:** the World Editor paints the biomes and places settlements and cameras on its Biomes layer and raises
  and lowers the ground on its Heights layer, the biome it paints chosen in the Biome Outliner and shown in the
  Properties tab, while the Biome Planner only plans; entities and races, like all content, are picked in the Hierarchy
  and edited in the Properties tab; every part of making a race's character is its own character tool tab (Appearance,
  Equipment, Mesh, Sculpt, Weights, Paint, Rig, UV and Stamp, see `.claude/docs/CharacterEditorPipeline.md`; the
  Appearance outliner holds every slider and control, the Rig tab marks which bone channels are sliders),
  sharing the region, bone and vertices selected, the UV Editor laying out the UVs of the faces the UV tab selects, with
  the Outliner beside them (also the Animation Editor's), the Color Wheel holding the paint colour and Texture Maps the
  map painted; the Recorder is its transport (Record, Play, Pause and Stop, nothing else) and the Timeline
  its clips, with Recorder Keys, Recorder Bin and Recorder Settings beside them, each opening its own
  menu through a shared `RecorderRowSystem`, and the pane tab worked in last being the one the recorder's deletes act
  on; the Media tab lists every media file, video and music in one list, and its rows drag onto the Timeline to lay;
  the Timeline shows its lanes with a mixer strip each, the lane and clip selected edited in the Properties tab, and
  the file's tempo, meter and key set in the Properties tab while the Recorder is worked in; the Note Library lists the
  notes of the instrument picked in the Hierarchy, each dragged onto the Timeline as a note clip that is stretched and
  cut like any clip. The Timeline's title names what is open; Back is a Recorder shortcut. The Scene Assets tab,
  beside the ARPG Viewer in the Content menu (`editor.bootstrap.sceneassetspipeline`), lists what the Dev window
  shows as it runs: the block under the crosshair in full (block, biome, face hit and every face's texture with its
  pack, material, shader, position) and every block, biome, texture, material and shader in view, from a grid of rays
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
  first, `selectLayer` and `toggleLayerVisible`) and creates a `LayersSourceSystem`. Layer keys are stable names (an
  enum's name, `region/mode`), never IDs. The window keeps the layer picked itself and every system reads it there;
  a layer picked hands its tool canvas, Properties rows and File and Edit table to the shared tabs through the
  window's own providers, which answer for the layer picked.
- **So far:** the World Editor (Heights over Biomes, each with an eye), the character Mesh, Sculpt, Weights, Rig and UV
  tabs (the Body and every region within it, one region picked for all of them; no eyes) and the character Paint tab
  (the paint layers of the map painted, each above the base with an eye).

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
  name, the one in use tinted, its details below; the part chosen is the one the Character Stamp tab's Stamp tool
  places next, at its own Scale and Twist (see `.claude/docs/CharacterEditorPipeline.md`).
- **A tool means one thing everywhere.** Raise builds up and Lower wears down, a soft brush fades its edge and a hard
  one keeps it, on whichever canvas takes them. A canvas whose values cannot blend (biome colors name biomes) simply
  does not take the tools that blend.

## The File and Edit menus

- **Every file action lives in the editor's File and Edit menus**, never as a tool's own button: New, New File,
  Duplicate, Save, Save All, Revert, Delete, Import Model, Export Model, Generate Maps and Save Selection As Stamp under
  File, Undo and Redo under Edit. They act on the window worked in last (`editor.bootstrap.filemenupipeline`,
  `FileMenuManager`), name it at their head and mute what it does not take.
- **A tool names its actions as data.** Its context implements `FileMenuProvider`, returns its
  `FileMenuTableStruct` constant from `EditorSetting` (the branch and, per `FileMenuAction`, the method its own keys
  already run) and creates a `FileMenuSourceSystem`. Every item runs through `ElementHitSystem.executeAction` on the
  tool's window, the route its shortcuts take, so a menu item and its key always do the same thing. A new tool adds a
  table; it never adds a Save, New, Revert, Undo or Redo button.
- **The Shipwright keeps its own buttons** because the game shares its table; the File and Edit menus reach it all the
  same through `EditorSetting.FILE_MENU_SHIPWRIGHT`.

## Values

- **Every number has a slider and a typed value.** A value row shows its name, a slider across its range, a box
  that opens for typing an exact value (the "manual set"), and step buttons. Dragging the slider updates the value
  and everything showing it live, in place, without rebuilding the panel.
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
