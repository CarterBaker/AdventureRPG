# Progression Pipeline — Instructions for Claude

The progression and statistics system. The design and every formula are in `.claude/docs/Progression.md`. This file
says where things live, which method owns each operation, and the traps. `.claude/CLAUDE.md` still applies in full.

---

## Layout

- **`ProgressionPipeline`** creates the nine registries, then `ProgressionManager`. It is registered in
  `ApplicationBootstrapAssembly` before `ItemPipeline`, because items, loot, NPCs and combat all read it.
- **Registries.** Each is a standard Manager/Loader/Builder plus Handle/Data, with `name2ID` and a palette, loaded in
  `awake()` with `requestAll()`.
  - `attribute`, `gender` and `damagetype`;
  - `stat` (+ `StatFormat`);
  - `tier` (`rollTier`, `getLowestTierID`);
  - `trade`, `skill` and `deed`;
  - `profession` (+ `RequirementKind`, `RequirementStruct`, `RequirementGroupStruct`).
- **Cached engine IDs.** Managers cache the IDs of the names the engine itself uses:
  - `StatManager`: `health`, `damage`, `armor`, …;
  - `DeedManager`: `kill`, `trade_practice`, `trade_advance`, `skill_advance`;
  - `DamageTypeManager`: `blunt`.

  The names live in `EngineSetting` (`STAT_*`, `DEED_*`, `DAMAGE_TYPE_*`). Use the cached getter, never a
  per-call name lookup.
- **`util/`** holds the shared pieces:
  - `ValueMapStruct` (an ID→float map parsed from an ARPG object of names);
  - `ProgressionArpgUtility` (`parseValueMap`, `parseNames`);
  - `ProficiencyStruct` and `ProficiencyUtility` (learn, decay, step: the one rule set for trades, skills and
    morality);
  - `PowerUtility` (`toFactor` for level gaps, `formatAbsolute` for display);
  - `IdentityUtility` (stable 64-bit identities from a name or a spawn).
- **`progressionmanager/`** holds `ProgressionManager`, the only public entry point, and its package-private
  branches:

| Branch | Owns |
|---|---|
| `StatBranch` | The one stat total: gear, attributes, trades, profession, held skills; cache and preview |
| `ExperienceBranch` | Level curve, window, wield level, experience award, kill experience |
| `AttributeBranch` | Staging, creation staging, clear, confirm |
| `ProficiencyBranch` | Practice, learning, decay settlement, effective skill, step deeds |
| `ConductBranch` | Deeds, morality, affinity, opinion, kill morality |
| `CareerBranch` | Choose, switch, grant, unlock checks, requirement values, NPC attribute weights |
| `VitalityBranch` | Wounds, bleeding, regeneration, restore |
| `GearBranch` | Forging (level, tier, bonuses), item stats, condition, wield check, wear |

Per-being state lives in `entitypipeline/progression/`:

| Handle | State |
|---|---|
| `ProgressionHandle` | Level, level progress, unspent and staged points, attributes, revision |
| `ProficiencyHandle` | Trades and skills as `ProficiencyStruct`s, revision |
| `ConductHandle` | Identity, morality, affinity list, deed counts |
| `CareerHandle` | Unlocked and granted professions, the active profession, revision |

`StatisticsHandle` holds the cached stat totals and the revisions they were built at, plus health, bleed, the
regeneration delay and the defeated flag.

---

## Single call sites

Every caller outside the pipeline goes through `ProgressionManager`. Never reach into a branch, and never write a
handle's fields from outside.

| Operation | Method |
|---|---|
| A being enters or leaves the world | `addBeing` / `removeBeing` (called by `EntityManager`) |
| A being starts over | `resetBeing(entity, identity)` |
| Give the player their save identity | `identify` (called by `SaveManager`) |
| Make an NPC at a level and profession | `raiseBeing` (called by `NpcManager.raise`) |
| Read a stat | `EntityInstance.getStat(statID)` → `resolveStats` |
| Experience | `awardExperience`; kills only through `recordKill` (from `DamageBranch`) |
| Spend points | `stageAttribute` / `stageCreationAttribute` → `confirmAttributes` / `clearStagedAttributes` |
| Use an item's skill | `practiceHeldSkill` (or `practiceSkill` for a named one); trades through `practiceTrade` |
| Any notable act | `performDeed(entity, deedID, magnitude)` |
| Profession | `chooseProfession` (creation), `switchProfession`, `grantProfession` (events) |
| Damage to health | `wound(entity, share, bleeding)`, only from `DamageBranch`; full heal `restore` |
| New gear | `forgeGear` (loot draws, NPC dress); stack splits copy gear through `ItemInstance.copyGear` |
| Gear wear | `wearGear` (weapons, tools), `wearArmor` (wounds) |
| Game time | `getTime()`, the world clock in game seconds |

The UI writes progression only through `ProgressionDisplayBranch` (`runtime/menueventsmanager/util/`). It formats
stats, experience, professions, steps and tier colors, and its `getDisplayRevision` tells a view when to redraw.

---

## Rules that are easy to break

- **Lifecycle.**
  - `get(X.class)` is legal only inside `get()`, for handles and instances too.
  - Progression handles look up their managers in `get()` and allocate their arrays in `awake()`, sized from
    registry counts.
  - Never size anything from a registry in `create()`: the registries have not loaded yet.
- **Relative power.**
  - Scaled stats and item stats are relative to their owner's level.
  - Only a level difference goes through `PowerUtility.toFactor`.
  - Never store or compare absolute values. `formatAbsolute` is for display only.
- **Revisions only count up.**
  - Bump the owning handle's revision whenever anything feeding stats changes. This covers:
    - `InventoryHandle.markChanged` for gear;
    - `ProgressionHandle` for level and attributes;
    - `ProficiencyHandle` for whole steps only, never every fractional gain;
    - `CareerHandle` for profession changes.
  - A missed bump leaves stale stats; a bump per frame defeats the cache.
- **Steps, not values, invalidate.** Trade and skill bonuses apply per whole step, so practice that does not cross a
  step must not bump the revision.
- **Decay is closed form.** `ProficiencyBranch.settle` catches a being up for any elapsed game time in one step. It
  is spread across frames (`PROFICIENCY_SETTLE_BEINGS_PER_FRAME`). Never loop decay per tick.
- **No IDs in saves or seeds.**
  - Handles keep names or IDs that resolve back to names.
  - Identities come from `IdentityUtility`, never a registry ID.
  - Loot and gear seeds are salted with `EngineSetting` salts and name seeds.
- **Gear identity.** Two gear stacks merge only when `ItemInstance.isSameGear` holds. Anything that splits or copies
  a stack calls `copyGear`.
- **Health is a share** from 0 to 1, not points. Max health can change without moving it.
- **Damage types.** Only `DamageBranch` decides mitigation and bleeding, from the `DamageTypeHandle`. A new damage
  source passes a damage type ID to `CombatManager.damage(...)`.
- **The squint is not health.** The vitality overlay (`runtime/vitality/VitalitySystem`, pass `Vitality`,
  `LAYER_VITALITY`) is separate from post-processing and from the sun squint. Never couple them.
- **Zero steady-state garbage.** Totals, previews and gear reads use scratch arrays on their owner. No boxing, no
  lambdas in the per-frame paths (`update`, `settle`, regeneration).

---

## Recipes

- **New stat.** Add an entry to `Stats.arpg`. If the engine reads it, also:
  1. add a `STAT_<NAME>` name to `EngineSetting`;
  2. cache its ID in `StatManager`;
  3. read it through `entity.getStat(statManager.get<Name>StatID())` at the one place it applies.
- **New deed fired by code.** Add the entry to `Deeds.arpg`, a `DEED_<NAME>` to `EngineSetting` and a cached ID in
  `DeedManager`. Then call `progressionManager.performDeed` from the single method that owns the act.
- **New requirement kind.** Do all four:
  1. add it to `RequirementKind`;
  2. parse it in `ProfessionBuilder`;
  3. answer it in `CareerBranch.resolveRequirementValue`;
  4. add it to the `Professions` schema's enum values.
- **New progression registry.** Mirror `trade/` and `trademanager/` exactly. Create it in `ProgressionPipeline`
  ahead of anything that reads it, and give it a schema under `assets/schemas/` with the next free order.
- **New damage source** (falls, drowning). Call `CombatManager.damage(source, target, amount, damageTypeID,
  direction)` with the type cached by `DamageTypeManager`, and add a cached getter there for any new type.
- **New per-being state.** Add it to the matching handle in `entitypipeline/progression/`, give the handle a name
  based accessor for saving, and reset it in that handle's `reset`.
