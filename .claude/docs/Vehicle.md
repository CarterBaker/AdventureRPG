# Vehicles

How vehicles are made, stored, sailed, ridden and broken, and what is still open. The code guide is
`.claude/docs/VehiclePipeline.md`. Every number below is a tuning value in `EngineSetting`; none is hard-coded in logic.
`.claude/CLAUDE.md` still applies in full.

---

## 1. Principles

- **A ship is a vehicle.** Cars, ships and air ships are one kind of thing: one format, one folder, one loader, one
  runtime. A vehicle's `category` (`VehicleCategory`) says what it is; today only `ship` exists.
- **No vehicle is written by hand.** Every vehicle is drawn at the drafting table and compiled into parts. The editor
  draws the game's own vehicles; the player draws theirs in the game. There is no hand-authored part list format.
- **The editor and the game share one table.** The same session, edit, compile and menu code runs in the editor's
  Shipwright tab and at a drafting table in the world. Only where the file is written differs.
- **The table builds what the game sails.** A design compiles straight into the `VehicleDesignStruct` the vehicle
  pipeline assembles; nothing about a vehicle is worked out twice.
- **Controller first.** Every table action is reachable from bindings alone (section 4); the pointer is optional.

---

## 2. Storage

| Who draws it | Written to | Registered as |
|---|---|---|
| The editor | `assets/vehicles/<Name>.arpg`, through the Info Panel | `<Name>`, its path below `vehicles/` |
| The player | `Documents/My Games/AdventureRPG/Saves/Vehicles/<Name>.arpg` | `saved/<Name>` |

- The file is the `Vehicles` schema: `display_name`, `category`, `decks` (each `start`, `outline`, an optional
  `port_outline` written only where the sides differ, `waists`, `roofs`, `walls`, `half_walls`, `railings`, `lips`, `doors`, `windows`, `hatches`, `grates`, `glass_floors`, `gun_platforms`,
  `stairs`, `ladders`, `boarding_ladders`, `furniture`), `helm`, `capstan`,
  `masts` (each only its `x`; older files' `height`, `crows_nest` and `rigging` are ignored).
- `assets/vehicles/` is optional; the game boots with no vehicle of its own. A saved vehicle that cannot be read or
  built is logged and skipped; one in the assets stops the game.
- Saving a design registers it again at once, and the status line says the file it was written to. A save that
  cannot be made (no name, no helm or capstan, a problem in the design, the triangle limit) says why in a notice and
  leaves the ship on the sheet. Vehicles already afloat keep the type they were launched as (section 9).
- **Open** lists every ship kept where the table writes, to pick one. **New**, **Open** and closing the table (Close or
  Pause in the game, the tab's close button in the editor) go ahead at once over saved work; over unsaved work a
  dialog asks first: **Save** (and go on once saved), **Don't Save**, or **Cancel**. Closing the whole editor window
  does not ask yet.

---

## 3. Drawing a ship

The sheet is the plan of the deck picked, bow to the right, starboard down, in octants (half blocks), inked at
`SHIPWRIGHT_INK_SCALE` texels to the octant so everything set down is drawn as what it is: a staircase as treads
climbing darker, a ladder as rails and rungs, a hatch, grate or glass floor as a grating with its hinge drawn heavy, a
window as panes, a gun platform, a cannon in iron, every piece of furniture over its whole footprint with its front
edge marked in gold. The sea is never drawn on the sheet: a deck being drawn is inked as floor inside its planking,
like everything else. The **water gauge** in the sheet's aft margin shows where the water sits in the deck picked:
empty when the deck stands clear of the water, full when the whole deck lies under it, and filled as far up as the
waterline crosses it.

The catalogue has four tabs; each lists the options of several tools, and every tool works as described below.

| Tab | Options (by section) |
|---|---|
| Hull | Deck, Porch, Waist, Side waist, Roof, Side roof |
| Fittings | Walls: Full wall, Half wall, Railing, Lip · Doors & Windows: Door, Window · Floors: Hatch, Grate, Glass floor · Gunnery: Gun platform · Helm & Capstan · Stairs & Ladders: Staircase, Ladder, Boarding ladder |
| Masts | Every mast stepped (none picked steps a new one) |
| Furniture | Every furnishing in the game, cannons included, by the folder it is kept in |

| Tool | Does |
|---|---|
| Deck | Draws the deck's outline out to the cursor, **mirrored** port and starboard; **Porch** draws it on the cursor's side alone |
| Waist | Cuts the deck's floor open, **mirrored**, railed round; through the bottom deck too; **Side waist** one side alone |
| Roof | Lays a low stepped shingle roof over open floor with no deck above, **mirrored**; **Side roof** one side alone |
| Walls | Paints walls on open floor, **not mirrored**; railings and lips also in place of the side or a bulwark |
| Openings | Door or window **on the wall it is cut through**; hatch, grate or glass floor in a floor; gun platform facing the side |
| Stairs & Ladders | Staircase climbing one deck the way it faces; ladder; boarding ladder over the side |
| Masts | Steps a mast on the centre line; the rest is worked out |
| Furniture | Sets the helm, the capstan or any furnishing in the game down |

**Working the table.** The catalogue under the sheet is one scrolling list of every option of the tab open, grouped
under section headings, each a tile pictured by its item's model, the helm and the capstan by the very fittings a ship
builds, and every option no item stands for by a little model built the way a ship builds it (a deck a square of
floor, a porch a strip reaching out to one side, the waist a railed hole, a roof over railed floor, each wall on a strip of floor, the lip a sill between posts of planking, a window, a
hatch, grate and glass floor, a gun platform facing its port, a staircase, the ladders and a mast with its yard and
sail; `ShipwrightPreviewBranch`), the one picked lit; a click or Previous / Next Option picks one,
and an option picked by the bindings is scrolled into view. A tab opened again takes up the option it last held. Under the cursor the thing in hand is drawn whole, as it
would be set down, red where it could not stand, and the line under the sheet says why. Draw sets it down. Draw over
anything already set down **picks it up** (the tool, option and facing that set it down go into the hand) so the
next draw moves it; Erase **takes away** whatever is under the cursor, found over its whole footprint. Whatever is
under the cursor is outlined and named, with what Draw and Erase will do to it. Deck, Waist, Roof (and their
one-side options), every wall and windows are painted: Draw and Erase paint and unpaint as the cursor is dragged, and
the waist, roofs, walls and windows are built
again at every octant the stroke changes, so the sheet shows them as they are painted and erased.

Rules the compile enforces (the change is refused with the reason shown when it breaks one):

- **Mirroring.** The outline, the waist and roofs are mirrored, or worked on one side alone with Porch, Side waist and
  Side roof; a deck drawn as a porch keeps a `port_outline` apart from its starboard `outline`, and the hull, its
  side and its shrouds follow each side's own outline. Walls, openings, stairs, ladders, masts' fittings and furniture
  stand only where they are set.
- **Doors and windows** are set on a wall, bulwark or the hull's side, found **across the side** from the octant
  pointed at (that octant, else the nearest outboard, else inboard, within `SHIP_OPENING_REACH`), so one set on a
  deck's rim finds a side that curves out or in over the deck. Each is cut as a **tunnel**: through the wall and, in a
  curved side, outboard through the open octants to the planking beyond, to one depth across the whole opening, and
  every open octant beside, over or under the tunnel at the hull is closed with the same wall, so an opening never
  leaves a gap. They turn themselves across the wall; Rotate flips the side. Taking a wall down takes its doors and
  windows with it. A window turns the wall into **panes** of glass, a thin sheet (`SHIP_PANE_SUB_VOXELS`) up the middle
  of each octant that reaches out to every wall, side and pane beside it, bending round corners as it goes, so a
  window at a corner meets the wall on both faces and leaves no stray block; windows are painted along a wall like
  walls.
- **Stairs** always climb exactly one deck (`SHIP_DECK_HEIGHT` octants), one octant per octant along their facing,
  two octants wide, set down whole: the foot must be floor, the run clear of the hull, bulwarks and anything built
  under the deck above, and the octant past the last tread the next deck's floor. The floor above is opened over the
  whole run **and two octants back past the foot**, so a climber's head clears the deck above from the first step.
- **Ladders** stand on 2×2 octants of open floor and climb to the floor right over them, cutting a hatchway there, or
  in the open up the face of the raised deck they face. Rails and a rung sheet; climbable.
- **The waist** is cut out of a deck's floor, mirrored, only where the floor is open with open room below. Every floor
  octant left beside it gets a rail `SHIP_WAIST_RAIL_HEIGHT` high, so a main deck cut through its middle is left as
  railed gangways either side looking down onto the deck below, which is open to the sky. Cut the decks below as well
  and the pit runs as many decks deep. A stair or ladder climbing out of it opens the rail where it lands; a hatch set
  on it closes as much of it as the largest lid covers. Erasing the waist takes up any grate or glass floor set there.
- **The bottom deck's waist** opens the hull to the sea below and is **glazed automatically**: every octant a stroke
  cuts (or uncovers) gets a glass floor over it. Take the glass floor away and that stretch is left open as a
  **diving bay** to drop through into the sea; the hull's buoyancy is unchanged (it is worked out layer by layer, so a
  hole in the bottom does not flood it).
- **Roofs** are laid over a deck's open floor where no deck stands above (`findRoofProblem`): shingles
  (`SHIP_TEXTURE_ROOF`) whose eaves sit `SHIP_ROOF_EAVES_HEIGHT` octants over the floor, on top of the railings, half
  walls and bulwarks round the edge and flush with them, stepping up `SHIP_ROOF_RISE_SUB_VOXELS` for every octant in
  from the roof's edge to a ridge along its middle, a slab `SHIP_ROOF_THICKNESS_SUB_VOXELS` thick; a full wall under
  it rises through it. A roof over a porch covers it without a deck above.
- **Walls, half walls, railings and lips are one tool under one rule** (`findWallProblem`). Any of them is painted on
  open floor anywhere, a diving bay's rim or any waist included, replacing the waist's rail or a bulwark standing
  there; every kind but a full wall can also be painted **in place of the ship's side** (the planking between a deck
  and the one above), which is then opened up to the deck above **right through**, however far the side flares out
  over those layers, so a lip there is a doorway in the side and a railing a porch or a stern gallery. Only a wall
  that opens the ship to the sea (any kind in the side, a railing or lip in place of a bulwark) must stand **above the
  waterline**; it lowers the sill to its deck. A full wall rises to the deck above under cover or `SHIP_DECKHOUSE_HEIGHT`
  in the open; a half wall `SHIP_HALF_WALL_HEIGHT`, a railing `SHIP_RAILING_HEIGHT`, a lip a sill
  `SHIP_LIP_SUB_VOXELS` high, stepped over. Painting one kind over another replaces it; erasing puts the side, rail or
  bulwark back. Every rail, the waist's included, is built as a post up the middle of each octant under a handrail
  that runs on to the rails beside it (`SHIP_RAILING_POST_SUB_VOXELS`, `SHIP_RAILING_CAP_SUB_VOXELS`,
  `SHIP_RAILING_CAP_WIDTH_SUB_VOXELS`).
- **Raised decks.** The side between two decks blends from one outline to the next, so a deck drawn a little wider
  flares the side and a little narrower draws it in; a deck drawn more than `SHIP_TUMBLEHOME_OCTANTS` inside the deck
  below instead **rises from it with straight sides**, leaving the lower deck's floor round it open (with its own
  bulwark at the outer edge), so a narrow deckhouse or walkway can be drawn on a deck and stairs set beside it on the
  open floor, climbing sideways onto it. Those straight sides are the ship's side under the wall rule, so the inner wall a
  porch or a narrower deck leaves standing on the deck below is taken out by painting a lip, railing or half wall in
  its place.
- **Grates** are an opening laid over a hole in a deck's floor (a waist, or any opening a stair or ladder leaves),
  covering the whole run of it up to `SHIP_GRATE_MAX_OCTANTS` with a fixed grate flush with the deck: thin slats
  (`SHIP_GRATE_SLAT_SUB_VOXELS`) with wide gaps (`SHIP_GRATE_GAP_SUB_VOXELS`), walked on, seen through, never opened.
  The waist rail along each side it covers is taken down so it is walked onto.
- **Glass floors** are laid the same way, the bottom deck's waist included: glass set a sub-voxel below an iron frame
  and a bar every `SHIP_GLASS_FLOOR_PANE_OCTANTS` octants either way.
- **Hatches** lie in a deck's floor over open room inside the hull below (never on the bottom deck, never over the
  outside where the side curves in under the floor; the waist and a stair's headroom keep the same rule). Set on a stair, ladder or waist
  opening, the lid closes the whole opening; anywhere else it cuts a 2×2 hole. Every lid is built as a hatch: a plank
  frame flush with the deck, slats set a sub-voxel down between it with gaps you can see through, two battens under
  them. It is **hinged along its longer side** (the facing picks which of the two) and swings up and over with
  Activate.
- **Boarding ladders** hang from the edge of an **open** deck (both octants the outermost of its floor, no deck
  over them) down the outside of the hull to the keel, rungs between two ropes, clear of the widest the side reaches
  below, with a step bridging the ladder's head to the deck. The bulwark is opened where it comes aboard. They face
  outboard by themselves and are climbed from the water or the deck like any ladder.
- **Gun platforms** (Fittings tab) are part of the ship: a low bed (`SHIP_GUN_PLATFORM_SUB_VOXELS`) over a cannon's
  footprint on open floor, facing the side. Each cuts a **port of its own** right through the planking before it,
  however far out the side flares (within `SHIP_OPENING_REACH`), its lid on the outer face, `SHIP_GUN_PORT_SILL` octants over the floor, the height of the barrel of a cannon standing on the bed,
  closed from outside by a framed lid, its board set in from the frame and battened inside, that swings out and up
  about its top edge with Activate. A platform facing a bulwark or nothing (one on an open deck) and two platforms
  whose ports would touch are refused. A port lowers the opening sill, so one under the waterline is a shape problem.
- **Cannons** (`ship/Cannon`) stand only on a gun platform: drawn over an empty platform the cannon is set down where
  the platform stands, facing its way, its barrel in the port. A cannon is cargo and can be taken away; the platform
  and its port stay. Files kept before gun platforms get one under every cannon when read.
- **Masts** are placed by hand; **everything else about them is worked out**. A mast is stepped on the lowest deck
  drawn under both its octants, so it never pokes through a deck drawn shorter than the rest. Every mast's head is
  measured from the lowest deck any mast rises from, as high as the hull's length (`SHIP_MAST_HEIGHT_SHARE`) and beam
  (`SHIP_MAST_BEAM_SHARE`) let it: the middle mast of the line is the tallest and each is shorter by up to
  `SHIP_MAST_END_TAPER` the farther along the line it stands, so a mast stepped through a raised deck is no taller for
  it. Its sails are every tier that fits under its head. A mast that carries the `SHIP_CROWS_NEST_TIERS` (course,
  topsail and topgallant) gets a **crow's nest over the topgallant**. Every mast is rigged so **every sail and the nest
  can be reached**: the shrouds climb from a **channel** (`SHIP_CHANNEL_REACH_SUB_VOXELS` out from the hull at the
  deck's edge) to the nest's rim, level with its floor, where its rail is left open, or to the mast's head where it has
  none, close in under the head and spreading toward the foot (`SHIP_SHROUD_CURVE`); over a nest a **ladder runs up
  the after face of the mast** from the nest's floor to the head, past every upper yard. The **bowsprit is automatic** on every hull at least
  `SHIP_BOWSPRIT_MIN_HULL_LENGTH` long, sized to the hull.
- **Windows** under water are fine: glass keeps the sea out, so a window does not lower the opening sill. Every
  window in the side the waterline reaches is framed in iron as a **porthole** (a rim through the planking with its
  corners filled in) and the ship is floated again with the frames aboard.
- **Furniture** must stand on open floor. Every door and piece of furniture is cargo: a real item, taken up, set down
  and used like the same item in the world. Chests, barrels and cargo crates open (`ship/Chest`, `ship/Stores`).
- **Anchors are automatic**: one per `SHIP_ANCHOR_LENGTH_PER_ANCHOR` blocks of hull, up to `SHIP_MAX_ANCHORS`, bow
  pair first, then the quarters, hung at catheads outboard of the side. Their cables show only while they are down.
- **Facings.** A placement's quarter turn is the way it faces on the sheet: a cannon facing starboard points its
  muzzle to starboard, and a door or window faces across the wall it is cut through.
- **Saving needs** a name, a buildable ship within the triangle limit, a **helm** and a **capstan** (it weighs the
  anchors).
- **The test ship** is `assets/vehicles/Galleon.arpg`: a large Spanish galleon of seven decks (hold, orlop, lower gun
  deck, main deck, quarterdeck and forecastle, poop, poop royal), four masts, thirty guns each behind its own lidded
  port, the main deck's waist cut open over the lower gun deck, hatches, forty windows, a boarding ladder over either
  side of the main deck's gangways amidships, and a captain's loft of two floors (great cabin and bedchamber, a stair
  inside) with a railed stern gallery off it, drawn only with what the table offers and editable piece by piece at
  the table.

---

## 4. Table bindings

| Binding | Default | Does |
|---|---|---|
| Move | WASD | Moves the cursor an octant, repeating while held |
| Jump / Sprint | Space / Shift (left / right click) | Draws, sets down or picks up / erases or takes away |
| Activate | E | Next tab |
| Turn Left / Turn Right | , / . (`<` / `>`) | Turns what is set down next a quarter turn either way |
| Rotate Item | R | Turns what is set down next, as Turn Right |
| Previous / Next Option | Q / F | Steps through the tab open |
| Level Up / Down | Page Up / Page Down (and the wheel) | Steps the deck |
| Save, Undo, Redo | Ctrl+S, Ctrl+Z, Ctrl+Y | As everywhere |
| Pause | Escape | Leaves a table in the game (asking over unsaved work); answers any open dialog as Cancel |

All are rebindable on the Controls tab. The engine has no gamepad input yet; a controller is mapped onto these.

---

## 5. Sailing

- **Propulsion** is anything driving the vehicle: today a sail hoisted past `VEHICLE_PROPELLED_HOIST`. A motor,
  wheels or a propeller joins the same check (`VehicleRigBranch.isPropelled`).
- **The helm steers only when** someone stands at it (taken with Activate, lost on walking off or swimming), **every
  anchor is up**, and the vehicle is **propelled**. Otherwise the wheel does nothing.
- **Unmanned, the rudder swings free** with the sea running across the hull (`VEHICLE_FREE_RUDDER_GAIN`), so a ship
  under sail with nobody at the helm drifts where the waves turn it.
- **Anchors** are dropped and weighed together at the capstan. While down they drag the hull's drift and swing; the
  hull still floats, heaves and rolls. Every vehicle enters the world at anchor.
- **Rudder** lift needs way on the hull: it only bites while water streams past it.

---

## 6. Storms and capsizing

- Sea state is weather turbulence; storms raise it to `OCEAN_TURBULENCE_MAX_STRENGTH`, the swell to
  `OCEAN_WAVE_SWELL_MAX_AMPLITUDE_BLOCKS` and the chop to `OCEAN_WAVE_CHOP_MAX_AMPLITUDE_BLOCKS`. The same values feed
  the GPU, so a wave drawn is the wave a hull meets. Calm seas are untouched by the storm caps.
- **Nothing holds a hull upright** but its own form. Every lump of dry hull is lifted by the sea it displaces against
  the live wave surface, sails push through their centre high above the hull, and ballast is laid at the table so a
  ship floats at its share of its sill.
- **Flooding:** a hull heeled past `VEHICLE_DOWNFLOOD_UPRIGHTNESS`, or with `VEHICLE_SWAMP_SHARE` of its deck edge
  under, takes water through every edge column under. Water aboard runs to the low side and heels it further, so a
  ship can and does capsize; one full of water sinks. It is pumped out only while upright and dry at the edge.
- The table's stability line says whether a design rights itself, how far, or capsizes as launched.

---

## 7. Riders

- A rider's feet are kept in the vehicle's model frame and carried rigidly with the deck after every vehicle step,
  so heave, roll and pitch move the rider exactly with the deck.
- Every move is swept through the vehicle's solid sub-voxels in its own frame, continuously, one axis at a time.
- **A deck that has moved up into an entity** (a non-rider, or between two moves) lifts it clear first, up to
  `VEHICLE_RIDER_UNSTICK_LIMIT`, when it then stands clear.
- **A rider washed off its feet** by a sea coming aboard stays aboard while it is over the vehicle's footprint; only
  swimming clear of the hull or leaving its reach puts it ashore.
- **Ladders and shrouds** are climbed along the vehicle's up axis; a climber within `VEHICLE_CLIMB_CREST_HEIGHT` of
  a ladder's top walks off it as it climbs.
- Falling through is still possible by design where the floor opens: an open hatch, an open stairwell, a capsized hull.

---

## 8. Geometry

- Hull, decks, walls, stairs and spars are octant boxes merged by the greedy mesher, as every sub-voxel mesh is.
- **Rope is never sub-voxels.** Stays, cables, shroud netting and ladder rungs are flat quads (`VehiclePartStruct`
  quads) drawn from both sides with their texture's cut-outs open: `Rope`, `Ratlines`, `Rungs`. They fill no
  sub-voxel; ladders and shrouds are climbable through climb zones built around each quad.
- **Glass** is a solid pane a sub-voxel thick textured `ShipGlass`. The vehicle shader keeps a texel's alpha share of
  its pixels through an ordered dither, so glass is see-through in the deferred pass. There is no blended
  transparency pass; true blended glass would need one.

---

## 9. Open items

- **Live refit:** saving a design does not change vehicles of it already afloat; they keep the old type until
  launched again. Refitting them in place needs their cargo restowed.
- **Categories:** only `ship` exists. A car or an air ship needs its own compile step (chassis and wheels, envelope)
  and its own propulsion and support (ground, air lift) alongside the sea's.
- **Naming:** the table and its classes are still called Shipwright; a generic name is the owner's call.
- **Untested in play:** the lid hinge directions of the new hatch and port lids, climbing boarding ladders from the
  water, ladder climbing, climbing onto a crow's nest and up the mast's ladder, the waist's rails, anchor placement, the unstick lift, washed riders, the free rudder gain and storm
  strength were built and compiled but not yet sailed. Tune them from a test ship. So were the post-and-rail railings,
  lips, glass floors, portholes, gun platforms with a cannon's barrel in its port, and dropping through a diving bay.
- **The Ship Viewer** (editor) shows the ship on the Shipwright's table as it is drawn, built again whenever the sheet
  rests, with its furniture in place; a vehicle chosen in the hierarchy is shown instead until the table is saved
  from. Left drag orbits, right drag pans with the ship following the pointer, the wheel zooms. The axis gizmo turns
  with the view like a scene gizmo: an arm out to a cone for every axis, X (the length) red, Y (up) green, Z (across)
  blue and their opposites grey; clicking a cone looks back at the ship along that axis, centred, and its middle
  square centres the view. The toolbar's − and + step the cutaway down a deck at a time (every deck above the one
  shown is cut away) and back up to all decks.
- **Diving bay:** the sea surface is masked out of the whole hull, so the water does not show inside the bay, and
  nothing yet climbs back up into it from the sea; a ladder that hangs into a bay is still to come.
- **Furniture use:** cargo is taken up, set down, opened and acted on as in the world, but the engine has no sitting
  or sleeping yet, and a container's model must span one block, so beds, tables, desks, sideboards and bookshelves
  cannot hold anything.
- **Saves moved:** ships a player saved before to `Saves/Ships/` load only once moved to `Saves/Vehicles/`.
- **Q/F:** F is also the editor's Focus Selected binding.
