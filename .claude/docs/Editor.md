# Editor Workflow

How the owner works in the editor and what every editor window must therefore do. Read it before adding or changing
any editor window, panel, tool, brush or value row. `.claude/CLAUDE.md` still applies in full.

---

## One task per tab

- **Many windows, never one crowded one.** Each tab does one task and works with the tabs around it. When a window
  starts doing two jobs, split it into two tabs that share their manager's state, the way Krita splits its dockers.
  Never cram another mode, panel or palette into an existing window to save a tab.
- **Tabs talk through shared state.** What is chosen in one tab (a biome, a brush, a tree, a ship, a bone) is the
  choice every other tab sees at once. Tabs never keep private copies of a shared choice.
- **Name a tab for what it holds.** A palette of biomes is the Biome Palette; a window that paints heights is the
  Height Editor. Never a generic name that hides what the tab is for.
- **Split so far:** the World Editor paints biomes and places settlements and cameras; the Height Editor raises and
  lowers the ground; the Entity Editor lists the entities and the race and genders of the one picked, its fields in
  the Properties tab; Character Appearance, Character Build and Character Equipment each shape one part of how a
  race looks, and the Character Mesh Editor models its meshes and rig with the Tools and Brushes tabs; the Recorder
  is its timeline, with Recorder Keys, Recorder Bin, Recorder Assets and Recorder Settings beside it, each opening its
  own menu through a shared `RecorderRowSystem`, and the pane tab worked in last being the one the recorder's deletes
  act on.
- **The one exception** is the Shipwright: it is shared with the game, so it keeps its own tools and layout.

## Tools and brushes

- **Every editor tool lives in the Tools tab**, chosen there or by the shared tool keys, for whatever canvas was
  worked in last (see `.claude/docs/Engine.md` and `.claude/docs/Shortcut.md`). A tool is never a panel button.
- **Brushes live in the Brushes tab, never in the Tools tab.** The Tools tab shows tools, their actions and their
  options only. Every stroking tool on every canvas stamps with the one brush chosen in the Brushes tab.
- **Brushes work like Krita's presets:** soft or hard, round, square or diamond, turned and squashed, dense or
  speckled, and each listed with a thumbnail of the stroke it makes. Keep plenty of them; a new kind of tip is a new
  `assets/brushes/` file, not code.
- **A tool means one thing everywhere.** Raise builds up and Lower wears down, a soft brush fades its edge and a hard
  one keeps it, on whichever canvas takes them. A canvas whose values cannot blend (biome colors name biomes) simply
  does not take the tools that blend.

## Values

- **Every number has a slider and a typed value.** A value row shows its name, a slider across its range, a box
  that opens for typing an exact value (the "manual set"), and step buttons. Dragging the slider updates the value
  and everything showing it live, in place, without rebuilding the panel.
- **One slider path.** Sliders share `editor.runtime.slider.SliderUtility` (track share, step, knob); typed values go
  through the name dialog and the same setter the slider and step buttons use.
- **Every slider has a range.** A value with a declared range (a tool option) slides across it; a character, face,
  animation or tree value slides across a fixed count of its own steps centred where it stood when its row was
  listed, through the tool's own step, so the tool's clamps still hold. Either way a typed value can go anywhere the
  setter allows.

## The Properties tab

- **One shared Properties tab, never a properties window per tool.** It shows what the window worked in last
  provides (`editor.bootstrap.propertiespipeline`): a window implements `PropertiesProvider`, names its
  `PropertiesContent` and creates a `PropertiesSourceSystem`. `ROWS` are a character, face or animation tab's value
  rows (the window is also a `CharacterViewRowSource`), `BIOMES` the World Editor's biome palette while it paints, and
  `INFO` the content entry selected in the Hierarchy, the editor's Info Panel, which the Hierarchy and every content
  tool show. A new kind of content is a new `PropertiesContent` and a `PropertiesView` system in the Properties tab,
  never a new window. Its toolbar and status line always work on the Hierarchy's selection.
- **The Info entry follows whatever was last selected, in any window**: a brush, a biome, a tree, a ship, a
  settlement, a character, an animation. A window that selects content routes the choice through
  `InfoManager.selectEntry`, and content picked in the Hierarchy becomes the window's choice through a selection
  listener, so both directions stay in step.
