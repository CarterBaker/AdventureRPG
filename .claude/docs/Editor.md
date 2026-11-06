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
  lowers the ground; the Biome Palette lists biomes only. The Character Editor is to be split into a mesh window
  (worked with the Tools and Brushes tabs) and an appearance window, and the Recorder into separate tabs.
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
- **Every value has a range.** A value row knows its minimum and maximum so its slider means something; a value with
  no natural bound takes a sensible editing range and still accepts any typed number the setter allows.

## The Info Panel

- **The Info Panel shows whatever was last selected, in any window**: a brush, a biome, a tree, a ship, a settlement,
  a character, an animation. A window that selects content routes the choice through `InfoManager.selectEntry`, and
  content picked in the Hierarchy becomes the window's choice through a selection listener, so both directions stay
  in step.
