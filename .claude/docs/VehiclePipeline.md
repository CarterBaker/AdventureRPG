# Vehicle Pipeline — Instructions for Claude

The vehicle and drafting-table pipelines. The design and every rule are in `.claude/docs/Vehicle.md`. This file says
where things live, which method owns each operation, and the traps. `.claude/CLAUDE.md` still applies in full.

---

## Layout

- **`VehiclePipeline`** creates `VehicleManager`; **`ShipwrightPipeline`** creates `ShipwrightManager` after it.
  Both are registered in `ApplicationBootstrapAssembly`.
- **`vehiclepipeline/vehiclemanager/`** — the runtime, the same for every category:

| Class | Owns |
|---|---|
| `VehicleManager` | Palette (`registerVehicle`), `spawn`/`despawn`, fixed step, helm, sails, doors, anchors |
| `VehicleAssemblyBranch` | `assemble(design)`: links parts, sails, doors, climb zones, bounds |
| `VehicleGeometryBranch` | Solid grid, hull and part meshes, rope quads, triangle count |
| `VehicleHullFormBranch` | Dry volume, columns, lumps, draft, righting arm |
| `VehicleHullBranch` | Sea sampling, buoyancy, drag, flooding |
| `VehicleRigBranch` | Rudder (steered or free), sails, braces, `isSteered`, `isPropelled` |
| `VehicleMooringBranch` | Anchor drag, spawn pose |
| `VehicleMotionBranch`, `VehicleGroundBranch` | Integration, grounding |
| `VehicleDoorBranch`, `VehicleSoundBranch` | Door swing, sound |
| `VehicleRiderSystem` | Carrying riders, sweeps, unstick lift, climbing, boarding |
| `VehicleControlSystem` | Finding and working a control, steering input |
| `VehicleCargoSystem`, `VehicleCastSystem` | Cargo, raycasts |
| `VehicleRenderSystem`, `VehicleHullMaskSystem` | Drawing, the water mask inside a hull |

- **`shipwrightpipeline/`** — the drafting table, the only place vehicles come from:

| Class | Owns |
|---|---|
| `ShipwrightManager` | Sessions, `compileSession`, save, open, launch, `toVehicleName`, registration at boot |
| `ShipwrightLoader` / `ShipwrightBuilder` | The one vehicle loader: scans `assets/vehicles/` |
| `ShipwrightLibraryBranch` | Reading and writing files in the assets or `Saves/Vehicles` |
| `ShipwrightEditBranch` | Every change: strokes, pick up / take away / place, `findPlacementProblem`, catalogue options, decks, undo, commit/refuse |
| `ShipwrightInspectBranch` | What stands at an octant over its whole footprint (`pick`), and the footprint any tool's option covers (`resolveFootprint`) |
| `ShipwrightCompileBranch` | `compile(blueprint, layout)` order, weighing, floating, `toDesign` |
| `ShipwrightHullBranch` | Sections, shell, bulwarks, waist and its rails, walls and railings, stairs, ladders, boarding ladders, hatches; their `find...Problem` rules |
| `ShipwrightFittingBranch` | Doors, windows, gun ports, helm, capstan, rudder, anchors, hatch and port lids (`addPanel`, `resolveHingeSpin`), ladders, boarding ladders, glass, cargo |
| `ShipwrightRigBranch` | Bowsprit, masts (height, sails and nest worked out), crow's nests, shrouds, stays |
| `ShipwrightDraftBranch` | Sheet glyphs at `SHIPWRIGHT_INK_SCALE`, the thing in hand, the hover outline, side and top views |
| `ShipwrightSurveyBranch` | Stability, speed, triangle survey |

- **Data:** `ShipBlueprintStruct` (the file's content, `category` included), `ShipLayoutStruct` (one compile),
  `ShipOctantGridStruct` (octant grid and box merge), `ShipwrightSessionInstance` (one table in use: tool, option,
  facing, cursor, `hover` and `placementProblem`), `ShipPickStruct` (what stands somewhere, and its footprint),
  `ShipPiece`, `ShipwrightTool`, `ShipOpening`, `ShipClimb`. **Utilities:** `ShipBlueprintArpgUtility` (the one file reader/writer),
  `ShipCellUtility` (cell packing, mirroring, steps), `ShipRopeUtility` (the one rope layer).
- **Runtime glue:** `application/runtime/shipwright/ShipwrightSystem` (input, bindings, labels, the lit tool),
  `ShipwrightCatalogueSystem` (the scrolling, sectioned tile list injected from the `shipwright_catalogue_*`
  templates; `ShipwrightManager.getOptionSection` / `getOptionItem` feed it), `ShipwrightDialogSystem` (the ship
  list, the unsaved-changes question through `requestLeave` / `ShipwrightLeave`, the save notice, the close action),
  `ShipwrightRenderSystem`, `menus/shipwright/ShipwrightBranch` (menu events), and in the editor
  `editor/runtime/shipwright/ShipwrightTabSystem` (writes through `InfoManager` with the `Vehicles` schema), and the
  editor's Ship Viewer, `editor/runtime/shipviewer/` (draws a palette type's own meshes at rest). The ship it shows is
  `ShipViewerManager`'s choice, set by the hierarchy's Vehicles selection and by `ShipwrightTabSystem` when a ship is
  opened or saved at the table; `ShipViewerSystem` looks it up again by name whenever the choice or
  `VehicleManager.getPaletteRevision` moves, as a ship saved again replaces its type and frees the old meshes.

---

## Compile order

`ShipwrightCompileBranch.compile`: sections → shell → bulwarks → waist → walls and railings → stairs → ladders → boarding
ladders → hatches →
openings (doors, windows, gun ports) → merge pieces into parts → bowsprit → fittings (helm, capstan, rudder, anchors,
hatch lids, port lids, ladders, glass) → rig → cargo → float → waterline. Everything that cuts the grid runs before
the merge; everything built as its own part runs after. Hatches run after stairs and ladders so they can close the
openings those cut. The waist runs before walls, stairs and furniture so they see its cut floor; it cuts every octant
first and rails the rim after, so a waist painted wide never rails its own octants.

---

## Rules for changes

- **One path in.** A vehicle type enters the palette only through `VehicleManager.registerVehicle`, from
  `ShipwrightManager.registerVehicle`. Never add a second loader or a hand-written format.
- **One file format.** Every read and write of a vehicle file is `ShipBlueprintArpgUtility`. A new blueprint field
  gets its reader, writer, `findProblem` check and the `Vehicles` schema in the same change.
- **One placement rule each.** Each kind's rule is one `find...Problem` query (`ShipwrightHullBranch` for stairs,
  ladders, boarding ladders and hatches; `ShipwrightEditBranch` for furniture, masts, doors and windows), returning
  the reason or `SHIP_PROBLEM_NONE`. `ShipwrightEditBranch.findPlacementProblem` asks it for the ghost (through
  `ShipwrightManager.inspect`) and before setting anything down, and the compile's `lay...` asks the same query before
  laying (`reportBuildProblem`), so the ghost, the table and a loaded file never disagree.
- **One footprint each.** What an option covers once set down is `ShipwrightInspectBranch.resolveFootprint`; picking,
  the ghost, the hover outline, the sheet's glyphs and the furniture overlap check all read it.
- **New option.** Add it to its tool's enum or list, `getOptionCount` / `getOptionIndex` / `pickOption` in
  `ShipwrightEditBranch`, `ShipwrightManager.getOptionName`, its footprint, its `find...Problem`, its glyph in
  `ShipwrightDraftBranch.drawGlyph` and its `pick` order in `ShipwrightInspectBranch`.
- **New fitting.** Lay what it cuts into the grid before the merge, record its octant box in `ShipLayoutStruct`
  (`addHatch`/`addLadder`/`addBoardingLadder`/`addPort`/`addPane` pattern), build its part in `ShipwrightFittingBranch.addFittings`, draw
  it in `ShipwrightDraftBranch`.
- **New category.** Add it to `VehicleCategory` and the schema's enum, branch the compile by the blueprint's category
  through data, and add its propulsion to `VehicleRigBranch.isPropelled`. The runtime stays shared.
- **Rope** goes through `ShipRopeUtility.addRope` or `VehiclePartStruct.addQuad`, never `addBox`.
- **A swinging lid** is a `DOOR` part built by `createLid` and `addPanel` (frame, recessed slats, battens on the inner
  face, all inside the fitting's octant box so it never meets cargo): pivot on the hinge line at the lid's top, axis
  along the hinge, `openAngle` signed for the way it must turn. A hatch hinges on the side `resolveHingeSpin` gives
  (a longer side); check the sign against `addHatchLids` and `addPortLids`.

---

## Traps

- **Items turn the other way round** from the sheet's quarter turns. Every furnishing's footprint and slot goes
  through `ShipwrightFittingBranch.resolveItemSpin`, which also turns a door a quarter further
  (`SHIP_DOOR_FACING_TURN`) so it lies in the wall it faces across. Never pass a cell's spin to an item directly.
- **Everything stays inside the model grid.** The sub-voxel grid holds no negative cell, and a box below or beside it
  is fatal at assembly. Clamp anything hung outboard (anchors) to the sheet and to the keel.
- **Part names are unique** across a vehicle; numbered fittings (`Hatch1`, `PortLid1`, `Anchor1`, `AnchorCable1`)
  rely on it. `SUB_VOXEL_MAX_PARTS` caps parts at 255; every hatch, port lid and anchor is a part.
- **Quad-only parts have flat bounds** (a min equal to its max on one axis). Nothing may assume a part has boxes;
  `isEmpty()` counts both.
- **Fitting boxes are in grid octants** (the sheet's x plus `originX`); blueprint cells are in sheet octants.
- **Edit-time checks read the last compiled layout**, so they see the ship as it stood before the change.
- **The vehicle shader's alpha is a dither share.** Any vehicle texture with partial alpha is drawn see-through.
- **Stairs always rise `SHIP_DECK_HEIGHT`.** A stair that cannot land a deck up is refused, never shortened;
  `SHIP_STAIR_MAX_RISE` is only the tallest a ladder climbs.
- **Menu entry points name inline elements.** An element built with `use` takes its template's id, so an entry point
  naming it resolves to nothing. Every element the system reaches by entry point (the tool buttons, the name label) is
  written inline in `menus/Shipwright.arpg`.
- **The sheet's ink is `SHIPWRIGHT_INK_SCALE` texels to the octant.** The session's sheet size stays in octants (the
  cursor's units); the pixels, the upload and the ink rect multiply by the scale.
- **Masts are only a place.** `ShipMastStruct` keeps its `x` alone; height, sails and the crow's nest are worked out
  in `ShipwrightRigBranch.addRig` and kept per mast in the layout (`getMastHeight`, `getMastSailCount`,
  `hasCrowsNest`). Read them from the layout, never from the blueprint.
- **Rails are landings.** `clearLanding` opens a bulwark, rail or railing where a stair or ladder lands;
  the table lets the waist widen into a rim that carries only its own rail.
- **Gun ports are one per cannon.** `cutPort` refuses a cannon that cuts no planking and one whose port touches a port
  already cut; never reintroduce open ports over a bulwark.
- **Railings replace the edge.** `layRailing` clears the side, bulwark or rail standing over its octant up to the deck
  above before laying the rail, so walls run after bulwarks and the waist; `findRailingProblem` is the one rule (above
  the last waterline, open floor or the deck's own side or bulwark). There is no porch: never add an overhanging
  floor; a porch is a railed stretch of deck.
- **Dialogs hide the table.** A label draws over every menu's sprites, so the table's menus are hidden
  (`ShipwrightSystem.setMenusShown`) while a dialog is open, as the Settings menu hides its parent; the catalogue's
  icons are skipped then too.
- **Every leave goes through `ShipwrightDialogSystem.requestLeave`.** New, Open, Close and Pause never call the
  manager directly; the editor tab's close button reaches it through `TabManager.setCloseGuard`, and the dialog's
  close action closes the tab (`TabManager.closeTabOf`).
- **Bindings:** the table reads `CYCLE_*`, `LEVEL_*`, `TURN_LEFT`, `TURN_RIGHT` and `ROTATE_ITEM` from
  `KeyBindings`; adding one means `KeyBindings`,
  `Settings`, `SettingsUtility` (apply and store), `SettingsBinding` and its `RuntimeSetting` label.
