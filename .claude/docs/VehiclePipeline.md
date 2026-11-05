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
| `ShipwrightEditBranch` | Every change: strokes, pick up / take away / place, `findPlacementProblem`, the tabs' entries (`getOptionCount` / `getOptionIndex` / `pickOption` / `hold` / `selectTab`), the bottom waist's glazing, cannon mounting, decks, undo, commit/refuse |
| `ShipwrightInspectBranch` | What stands at an octant over its whole footprint (`pick`), and the footprint any tool's option covers (`resolveFootprint`) |
| `ShipwrightCompileBranch` | `compile(blueprint, layout)` order, weighing, floating, `toDesign` |
| `ShipwrightHullBranch` | Sections (per side, `laySections` / `resolveSection`), shell, bulwarks, waist and its rails, walls, railings and lips, stairs, ladders, boarding ladders, hatches, grates and glass floors (`layCovers`), roofs (`layRoofs`, `findRoofProblem`), the railings' posts and handrails (`addRailings`) and window panes (`addPanes`, both through `addArm`); their `find...Problem` rules |
| `ShipwrightFittingBranch` | Doors, windows, gun platforms and their ports (`findPlatformProblem`), cannons (`findCannonProblem`), helm, capstan, rudder, anchors, hatch and port lids (`addPanel`, `resolveHingeSpin`), grates, glass floors, ladders, boarding ladders, windows glazed into `PANES` (`glazeWindow`), roofs (`addRoofs`), portholes (`addPortholes`), cargo |
| `ShipwrightRigBranch` | Bowsprit, masts (foot, height, sails and nest worked out), crow's nests, shrouds, the ladder up each mast over its nest, stays |
| `ShipwrightPreviewBranch` | The catalogue's little models of every option no item stands for, the helm and the capstan among them |
| `ShipwrightDraftBranch` | Sheet glyphs at `SHIPWRIGHT_INK_SCALE`, the thing in hand, the hover outline, the water gauge, side and top views |
| `ShipwrightSurveyBranch` | Stability, speed, triangle survey |

- **Data:** `ShipBlueprintStruct` (the file's content, `category` included), `ShipLayoutStruct` (one compile),
  `ShipOctantGridStruct` (octant grid and box merge), `ShipwrightSessionInstance` (one table in use: tab, the entry
  each tab last held, tool, option, the side option of Deck, Waist and Roof (`getSideOption`, `isMirroring`), facing,
  cursor, `hover` and `placementProblem`), `ShipPickStruct` (what stands somewhere, and its footprint), `ShipPiece`
  (with the height a slab piece keeps and its `ShipPieceShape`: merged blocks, a railing or a pane),
  `ShipwrightTab` (the catalogue's four tabs: fixed entries of tool, option and section, then a following tool's
  options), `ShipwrightTool` (a sided tool's one-side title, `getTitle(option)`), `ShipWall`, `ShipOpening`, `ShipClimb`. **Utilities:** `ShipBlueprintArpgUtility` (the one file reader/writer),
  `ShipCellUtility` (cell packing, mirroring, steps), `ShipRopeUtility` (the one rope layer).
- **Runtime glue:** `application/runtime/shipwright/ShipwrightSystem` (input, bindings, labels, the lit tool),
  `ShipwrightCatalogueSystem` (the scrolling, sectioned tile list injected from the `shipwright_catalogue_*`
  templates; `ShipwrightManager.getOptionSection` / `getOptionItem` feed it), `ShipwrightDialogSystem` (the ship
  list, the unsaved-changes question through `requestLeave` / `ShipwrightLeave`, the save notice, the close action),
  `ShipwrightRenderSystem`, `menus/shipwright/ShipwrightBranch` (menu events), and in the editor
  `editor/runtime/shipwright/ShipwrightTabSystem` (writes through `InfoManager` with the `Vehicles` schema), and the
  editor's Ship Viewer, `editor/runtime/shipviewer/`. The ship it shows is `ShipViewerManager`'s choice: a vehicle by
  name, set by the hierarchy's Vehicles selection, or the table's session, set by `ShipwrightTabSystem` when the table
  opens and whenever a ship is written from it. For a vehicle `ShipViewerSystem` looks it up again whenever the choice
  or `VehicleManager.getPaletteRevision` moves; for the session it calls `ShipwrightManager.buildDraft` into a
  `ShipDraftStruct` of its own (its own compiled layout and resting meshes) whenever
  `ShipwrightSessionInstance.getSurveyRevision` moves, freeing the last draft's meshes first (`releaseDraft`).
  `ShipViewerRenderSystem` draws its meshes, and every cargo slot's first item, with the viewer's one material and its
  `u_cutawayHeight` uniform (items never cut; a slot above the cut is skipped); `ShipViewerCameraSystem` orbits, pans
  with the pointer, snaps (centred) and centres, and keeps the camera's right and up for the gizmo
  `ShipViewerMenuSystem` places from positions made once; buttons route through `ShipViewerBranch`. The game's own
  vehicle and item materials write the deferred G-buffer, which an editor viewport never lights, so the viewer keeps a
  material of its own on the game's vehicle vertex shader.

---

## Compile order

`ShipwrightCompileBranch.compile`: sections → shell → bulwarks → waist → walls, railings and lips → stairs → ladders →
boarding ladders → hatches → grates and glass floors → roofs (cells recorded) → openings (doors, windows glazed as
panes, gun platforms and their ports) → merge pieces into parts (rails as posts and handrails, panes as sheets reaching
to their neighbours) → bowsprit → fittings (helm, capstan, rudder, anchors, hatch lids, grates, glass floors, port lids,
ladders, roofs) → rig → cargo → float → portholes (and float again when any were
framed) → waterline. Everything that cuts the grid runs before
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
- **New option.** Add it to its tool's enum or list, list it in its `ShipwrightTab` (tool, option, section), handle it
  in `ShipwrightEditBranch.getHeldOption` / `hold` when it is a new tool, `ShipwrightManager.getOptionName`, its
  footprint, its `find...Problem`, its glyph in `ShipwrightDraftBranch.drawGlyph`, its picture in
  `ShipwrightPreviewBranch` and its `pick` order in `ShipwrightInspectBranch`.
- **Catalogue indices are entries.** `getOptionIndex`, `pickOption` and the tiles' action argument are the tab's entry
  index, not the tool's option; read the tool's own option through `getHeldOption` / `getEntryOption`.
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
- **Outlines and sections are per side.** `getOutline(deck, x)` and `getSection(x, y)` give the wider side; anything
  built on one side (shrouds, channels, anchors, the boarding ladder, `isVolume`, `isOnDeck`) reads that side's own
  value (`getOutline(deck, x, side)`, `getSection(x, y, side)`, `findWidestSection(layout, x, side)`), the side from
  `ShipCellUtility.toSide(z)`. A mirrored stroke writes both sides; `port_outline` is written only when they differ.
- **Roof cells are grouped by floor.** `addRoofs` reads `ShipLayoutStruct.getRoofCells` in runs of one floor, which
  holds because `layRoofs` walks the blueprint's sorted `roofs`, whose level is the packed cell's top bits. It tells
  where a roof meets open air from the merged grid's floor beside it, so the grid must still hold its pieces after
  `mergePieces`.
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
- **Gun ports are one per gun platform.** Ports belong to the platform, never to the cannon (cargo can be taken away);
  `findPlatformProblem` refuses a platform that cuts no planking and one whose port touches a port already cut; never
  reintroduce open ports over a bulwark. A cannon's cell must equal a platform's cell (`findCannonProblem`), and the
  table snaps a cannon in hand to the platform under the cursor (`resolveMountCell`, `isMounting`).
- **Rails are posts.** `ShipPiece.RAILS` is still one octant in the grid for every rule and collision check, but is
  built by `ShipwrightHullBranch.addRailings` as posts and handrails, never merged whole. Lips and gun platforms are
  slab pieces: `ShipOctantGridStruct.mergeInto` keeps only `getHeight()` sub-voxels of their top layer.
- **Every opening through a wall is a tunnel.** Doors, windows and gun ports go through
  `ShipwrightFittingBranch.cutTunnel`: one depth for the whole opening, outboard through a curved side's open octants
  (`resolveTunnelReach`), and every open octant around it at the hull closed with the same wall (`frameTunnel`). A door
  or window finds its wall across the side with `resolveOpeningZ`, and the table keeps it at the octant found
  (`resolveWallCell`), so the compile, the cargo and the glyph agree.
- **Strokes compile as they go.** Walls, the waist and windows (`isPaintedHand`, all but the deck) are compiled at
  every octant a stroke changes; the stroke keeps whether a build problem stood before it began
  (`hadStrokeBuildProblem`), so the strict refusal at its end still compares with the ship before the stroke.
- **Glazed windows keep the sea out.** Only an open cut through the side lowers the opening sill; a window does not.
  `addPortholes` runs after the first float (it needs the draft) and the hull is floated again when it framed any.
- **The bottom deck's waist is glazed by the edit branch,** not the compile: `glazeWaist` adds a glass floor placement
  over each octant a waist stroke cut or uncovered, compiling after each, inside the same undo step.
- **Every wall kind shares one rule and one layer.** `findWallProblem` is the one rule for full walls, half walls,
  railings and lips (open floor, the waist's rail or a bulwark, or in place of the side for all but a full wall; the
  waterline only where `opensSide`), and `layWall` the one layer: in place of the side it calls `openSide`, which
  clears every layer up to the deck above walking outboard through a flared side (`clearOutboard`), otherwise
  `clearLanding` takes down a rail or bulwark; walls run after bulwarks and the waist. There is no porch: never add an
  overhanging floor; a porch is a railed stretch of deck.
- **Straight sides over a narrow deck.** `resolveSection` blends the side between two decks unless the deck above is
  drawn more than `SHIP_TUMBLEHOME_OCTANTS` inside the one below; then its own outline rises straight from the lower
  floor.
- **Every catalogue picture is built, not drawn.** `ShipwrightPreviewBranch` lays each option no item stands for in a
  scratch layout and builds it with the hull, fitting and compile branches' own builders (`mergePieces`,
  `addRailings`, `addHatchLids`, `addGrates`, `addGlassFloors`, `addPortLids`, `addLadders`, `addBoardingLadders`,
  `addPanes`, `buildHelm`, `buildCapstan`); a new option gets its picture there.
- **Grates and glass floors are fixed covers.** `layCovers` runs after hatches; `layOpeningCover` is the one extent
  rule for hatch lids and covers, `findCoverProblem` the one placement rule, and a cover takes down the waist rail
  along its sides (`unrailCover`). `addGrates` and `addGlassFloors` build every cover of a kind as fixed parts; never
  give one a hinge.
- **Masts step on decks, not the keel.** `resolveFootLayer` is the lowest deck drawn under both octants; heads are
  measured from `resolveHeadBase` and ranked from the middle mast of the line. The crow's nest stands over
  `SHIP_CROWS_NEST_TIERS` tiers, and `addMastLadder` builds the ladder up the mast's after face from the nest through
  `ShipwrightFittingBranch.addLadder`, the one ladder builder.
- **Dialogs hide the table.** A label draws over every menu's sprites, so the table's menus are hidden
  (`ShipwrightSystem.setMenusShown`) while a dialog is open, as the Settings menu hides its parent; the catalogue's
  icons are skipped then too.
- **Every leave goes through `ShipwrightDialogSystem.requestLeave`.** New, Open, Close and Pause never call the
  manager directly; the editor tab's close button reaches it through `TabManager.setCloseGuard`, and the dialog's
  close action closes the tab (`TabManager.closeTabOf`).
- **Bindings:** the table reads `CYCLE_*`, `LEVEL_*`, `TURN_LEFT`, `TURN_RIGHT` and `ROTATE_ITEM` from
  `KeyBindings`; adding one means `KeyBindings`,
  `Settings`, `SettingsUtility` (apply and store), `SettingsBinding` and its `RuntimeSetting` label.
