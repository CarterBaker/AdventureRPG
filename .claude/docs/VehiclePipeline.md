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
| `ShipwrightEditBranch` | Every change: strokes, placement rules, options, decks, undo, commit/refuse |
| `ShipwrightCompileBranch` | `compile(blueprint, layout)` order, weighing, floating, `toDesign` |
| `ShipwrightHullBranch` | Sections, shell, bulwarks, porch, walls, stairs, ladders, hatches |
| `ShipwrightFittingBranch` | Doors, windows, gun ports, helm, capstan, rudder, anchors, lids, ladders, glass, cargo |
| `ShipwrightRigBranch` | Bowsprit, masts, sails, crow's nests, shrouds, stays |
| `ShipwrightDraftBranch` | Sheet, ghost, side and top views |
| `ShipwrightSurveyBranch` | Stability, speed, triangle survey |

- **Data:** `ShipBlueprintStruct` (the file's content, `category` included), `ShipLayoutStruct` (one compile),
  `ShipOctantGridStruct` (octant grid and box merge), `ShipwrightSessionInstance` (one table in use), `ShipPiece`,
  `ShipwrightTool`, `ShipOpening`. **Utilities:** `ShipBlueprintArpgUtility` (the one file reader/writer),
  `ShipCellUtility` (cell packing, mirroring, steps), `ShipRopeUtility` (the one rope layer).
- **Runtime glue:** `application/runtime/shipwright/ShipwrightSystem` (input, bindings, labels),
  `ShipwrightRenderSystem`, `menus/shipwright/ShipwrightBranch` (menu events), and in the editor
  `editor/runtime/shipwright/ShipwrightTabSystem` (writes through `InfoManager` with the `Vehicles` schema).

---

## Compile order

`ShipwrightCompileBranch.compile`: sections → shell → porch → bulwarks → walls → stairs → ladders → hatches →
openings (doors, windows, gun ports) → merge pieces into parts → bowsprit → fittings (helm, capstan, rudder, anchors,
hatch lids, port lids, ladders, glass) → rig → cargo → float → waterline. Everything that cuts the grid runs before
the merge; everything built as its own part runs after. Hatches run after stairs and ladders so they can close the
openings those cut.

---

## Rules for changes

- **One path in.** A vehicle type enters the palette only through `VehicleManager.registerVehicle`, from
  `ShipwrightManager.registerVehicle`. Never add a second loader or a hand-written format.
- **One file format.** Every read and write of a vehicle file is `ShipBlueprintArpgUtility`. A new blueprint field
  gets its reader, writer, `findProblem` check and the `Vehicles` schema in the same change.
- **One placement rule each.** A placement rule lives in `ShipwrightEditBranch` (what may be set where) and is
  enforced again in the compile (`reportBuildProblem`), so a file can never load what the table would refuse.
- **New fitting.** Lay what it cuts into the grid before the merge, record its octant box in `ShipLayoutStruct`
  (`addHatch`/`addLadder`/`addPort`/`addPane` pattern), build its part in `ShipwrightFittingBranch.addFittings`, draw
  it in `ShipwrightDraftBranch`.
- **New category.** Add it to `VehicleCategory` and the schema's enum, branch the compile by the blueprint's category
  through data, and add its propulsion to `VehicleRigBranch.isPropelled`. The runtime stays shared.
- **Rope** goes through `ShipRopeUtility.addRope` or `VehiclePartStruct.addQuad`, never `addBox`.
- **A swinging lid** is a `DOOR` part: pivot on the hinge line at the lid's top, axis along the hinge,
  `openAngle` signed for the way it must turn. Check the sign against `addHatchLids` and `addPortLids`.

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
- **Stair rise vs ghost:** the ghost assumes a deck's height of rise; a stair onto a raised deck may rise less.
- **Bindings:** the table reads `CYCLE_*` and `LEVEL_*` from `KeyBindings`; adding one means `KeyBindings`,
  `Settings`, `SettingsUtility` (apply and store), `SettingsBinding` and its `RuntimeSetting` label.
