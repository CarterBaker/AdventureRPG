# Editor Shortcuts

How the editor's keyboard shortcuts work and how to add one. Read it before adding, changing or reading a key or a
modifier in any editor tool. The root `CLAUDE.md` still applies in full.

---

## 1. Principles

- **Every editor key is data.** A tool's keys live in one file, `assets/shortcuts/<Tool>.arpg`, editable in the
  editor's own Hierarchy and Info Panel (the Shortcuts tab), or rebound on the Settings dialog's Shortcuts page,
  which writes the same file through the Info Panel's save and applies the new keys at once
  (`ShortcutManager.rebindShortcut`). Editor code never reads a key or a modifier with `isKeyClicked`, `isKeyDown`
  or `isKeyHeld` to act on it; it adds a shortcut instead. The only exceptions are text entry (the name dialog, the
  command console), the Settings dialog waiting for a rebind's keys, a dialog's own Enter and Escape, and the keys
  of a modal gesture already under way (a transform's X, Y, Z, Enter and Escape).
- **One route.** A pressed shortcut runs a `class` and `method` exactly as a menu button does, through
  `ElementHitSystem.executeAction`, on the instance registered in the tool's context, inside its crash boundary. A
  shortcut and its button therefore always do the same thing.
- **Scoped to the tool.** A file names the tool's context class. Its shortcuts work only while that tool's window —
  or its tab's chrome — holds focus, and never while the name or export dialog is open.
- **Exact chords.** A pressed shortcut fires when its key goes down with exactly its modifiers held, so Ctrl+S never
  also runs S. Two pressed shortcuts of one tool on the same chord fail at load.
- **Shared tool keys.** `assets/shortcuts/Tools.arpg` names `ToolsContext`: its keys choose the editor's generic tools
  (`V` Select, `B` Brush, `E` Erase, `F` Fill, `I` Pick, `P` Place, `W` Wall), resize the brush (`[` and `]`) and step
  through brush presets (`-` and `=`). Besides the Tools tab, they work in every window `ToolManager` counts as a tool
  window — each canvas (World Editor, Item Editor, Character Editor) and the Brushes tab — after that window's own
  keys, so a window's own shortcut wins where both use a key (the Character Editor's `E` extrudes, `F` makes a face).
  Rebinding one on the Settings dialog's Shortcuts page rebinds it in every editor at once.

---

## 2. The file

```json
{
  "context": "editor.runtime.recorder.RecorderContext",
  "pressed": [
    { "name": "Delete", "key": "Delete",
      "class": "editor.runtime.menueventsmanager.menus.RecorderBranch", "method": "deleteSelection" },
    { "name": "AddKey", "key": "K",
      "class": "editor.runtime.menueventsmanager.menus.RecorderBranch", "method": "actSetting", "arg": "key_add" },
    { "name": "Save", "key": "S", "modifiers": ["control"],
      "class": "editor.runtime.menueventsmanager.menus.RecorderBranch", "method": "saveRecording" }
  ],
  "held": [
    { "name": "ExtendSelection", "modifiers": ["shift"] }
  ]
}
```

- `key` is the key's name as the editor shows it (`InputNameUtility`), ignoring case: `A`, `Num 1`, `Delete`,
  `Space`, `Page Up`, `Bracket Left`, `Numpad Add`.
- `modifiers` are `control`, `shift` and `alt`; either side's key holds a modifier.
- A pressed shortcut's method may take a `String` (the `arg`) and a `WindowInstance` (the tool's window). It gets no
  menu or element. A bad class or method fails at boot, naming the shortcut.
- A held shortcut has no action and needs a key or at least one modifier.
- A shortcut's registered name is the file's name, a `/`, then its own: `Recorder/ExtendSelection`.

---

## 3. Adding a shortcut

1. **A key that runs something.** Find the public method the tool's menu button already calls and add a `pressed`
   entry naming it. When no method fits, add one to the tool's Branch (or, for a context-local system, to that
   system) that routes into the same manager method the button uses, then add the entry. Guard it the way the
   button's own path is guarded; a shortcut can fire at any moment the tool is focused.
2. **A modifier a gesture reads.** Add a `held` entry, put its registered name in the tool's settings file
   (`EditorSetting.RECORDER_SHORTCUT_*`, `CharacterEditorSetting.SHORTCUT_*`), resolve it once in `start()` with
   `ShortcutManager.getShortcutIDFromShortcutName`, and read it with `ShortcutManager.isHeld(id, window)`.
3. **A new tool.** Add `assets/shortcuts/<Tool>.arpg` naming the tool's context class. Nothing else registers it.
4. Seal the file with the ARPG tool (`docs/ARPG.md`) and boot the editor once: every action is resolved at load.

---

## 4. Selection conventions

Every tool that selects many things follows one gesture set, each modifier a held shortcut of the tool's own:

- **Click** selects one thing alone; a click on nothing clears the selection.
- **Shift** adds: a click toggles the thing in or out, a drag on nothing draws a box whose contents join.
- **Ctrl** removes: a click takes the thing out, a drag on nothing draws a box whose contents leave.
- Dragging anything selected moves everything selected together.
- **Delete** removes everything selected as one edit.

The Recorder's Keys pane (keys on a curve's row or graph), the Character Editor's mesh modes (vertices) and the World
Editor's Select tool (hand-placed settlements and structures) follow it.
