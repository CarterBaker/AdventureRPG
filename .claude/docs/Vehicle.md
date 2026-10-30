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

- The file is the `Vehicles` schema: `display_name`, `category`, `decks` (each `start`, `outline`, `walls`,
  `half_walls`, `doors`, `windows`, `hatches`, `stairs`, `ladders`, `furniture`), `helm`, `capstan`, `porch`, `masts`.
- `assets/vehicles/` is optional; the game boots with no vehicle of its own. A saved vehicle that cannot be read or
  built is logged and skipped; one in the assets stops the game.
- Saving a design registers it again at once. Vehicles already afloat keep the type they were launched as (section 9).

---

## 3. Drawing a ship

The sheet is the plan of the deck picked, bow to the right, starboard down, in octants (half blocks).

| Tool | Does | Option (cycle) |
|---|---|---|
| Deck | Draws the deck's outline out to the cursor, **mirrored** port and starboard | — |
| Walls | Paints walls on open floor, **not mirrored** | Full / half height |
| Openings | Door or window **on the wall it is cut through**; hatch in a floor | Door / Window / Hatch |
| Stairs & Ladders | Stair foot climbing the way it faces; ladder foot (1×1 block) | Stair / Ladder |
| Masts | Steps a mast on the centre line, or picks one | Picks the mast |
| Furniture | Sets the helm, the capstan or any `ship/` furnishing (cannons included) down | Picks the item |

Rules the compile enforces (the change is refused with the reason shown when it breaks one):

- **Mirroring.** Only the outline is mirrored. Walls, openings, stairs, ladders, masts' fittings and furniture stand
  only where they are set.
- **Doors and windows** must be set on a wall, bulwark or hull octant. They cut exactly their footprint through that
  wall and nothing beyond it, and turn themselves across the wall; Rotate flips the side. Taking a wall down takes its
  doors and windows with it. A window is glazed with a solid, see-through pane.
- **Stairs** rise one octant per octant along their facing, two octants wide, to the first floor level with their
  last tread. The floor above is opened over the whole run **and two octants back past the foot**, so a climber's head
  clears the deck above from the first step. The ghost under the cursor shows the run, red when it leaves the deck.
- **Ladders** stand on 2×2 octants of open floor and climb to the floor right over them, cutting a hatchway there, or
  in the open up the face of the raised deck they face. Rails and a rung sheet; climbable.
- **Hatches** lie in a deck's floor over a deck below (never on the bottom deck). Set on a stair or ladder opening, the
  lid closes the whole opening; anywhere else it cuts a 2×2 hole. Lids swing open with Activate.
- **Cannons** (`ship/Cannon`) facing the side cut a gun port right before the muzzle: through a bulwark it is open,
  through the planking it gets a lid that swings out and up. A port in the hull lowers the opening sill, so one under
  the waterline is a shape problem.
- **Masts** are placed by hand. The **bowsprit is automatic** on every hull at least `SHIP_BOWSPRIT_MIN_HULL_LENGTH`
  long, sized to the hull. Sails follow each mast's height.
- **Anchors are automatic**: one per `SHIP_ANCHOR_LENGTH_PER_ANCHOR` blocks of hull, up to `SHIP_MAX_ANCHORS`, bow
  pair first, then the quarters, hung at catheads outboard of the side. Their cables show only while they are down.
- **Facings.** A placement's quarter turn is the way it faces on the sheet: a cannon facing starboard points its
  muzzle to starboard, and a door or window faces across the wall it is cut through.
- **Saving needs** a name, a buildable ship within the triangle limit, a **helm** and a **capstan** (it weighs the
  anchors).
- **The test ship** is `assets/vehicles/Galleon.arpg`: a large Spanish galleon of seven decks (hold, orlop, lower and
  upper gun decks, quarterdeck and forecastle, poop, poop royal), four masts, forty guns, cabins, stairs, ladders and
  hatches, drawn only with what the table offers.

---

## 4. Table bindings

| Binding | Default | Does |
|---|---|---|
| Move | WASD | Moves the cursor an octant, repeating while held |
| Jump / Sprint | Space / Shift | Draws / erases with the tool in hand |
| Activate | E | Next tool |
| Rotate Item | R | Turns what is set down next |
| Previous / Next Option | Q / F | Steps the tool's option |
| Level Up / Down | Page Up / Page Down (and the wheel) | Steps the deck |
| Save, Undo, Redo | Ctrl+S, Ctrl+Z, Ctrl+Y | As everywhere |
| Pause | Escape | Leaves a table in the game |

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
- **Untested in play:** the lid hinge directions, ladder climbing, anchor placement, the unstick lift, washed riders,
  the free rudder gain and storm strength were built and compiled but not yet sailed. Tune them from a test ship.
- **Saves moved:** ships a player saved before to `Saves/Ships/` load only once moved to `Saves/Vehicles/`.
- **Q/F:** F is also the editor's Focus Selected binding.
