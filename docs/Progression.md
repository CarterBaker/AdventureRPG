# Progression and Statistics

How levels, attributes, stats, gear, trades, skills, deeds, professions, morality and health work, where their data
lives, and how to add to them. Every number below is a tuning value in `EngineSetting`, `RuntimeSetting` or a data
file. None is hard-coded in logic.

---

## 1. Principles

- **One being, one rulebook.** The player and every NPC run the same code: levels, attributes, trades, skills,
  professions, morality, affinity, gear, damage and regeneration.
- **Everything is data.** Attributes, stats, genders, tiers, damage types, trades, skills, deeds and professions are
  each one list file under `assets/`, each with an editor schema. Adding, renaming or removing one is a data edit.
- **Everything is a stat.** Damage, health, regeneration, learning speed, loot luck, poise and the rest resolve from
  attributes, gear, trades, skills and the active profession through one path.
- **Nothing is refunded.** Spent attribute points are permanent. The only undo is the staging step before you
  confirm.
- **Harder first, easier later.** The window of gear and experience you can reach above your level widens as you
  climb, so the game eases with level.

---

## 2. Levels and experience

- Level is a whole number from 1 with **no cap**. Experience is kept as progress through the current level, so it
  never overflows. The UI shows it as `current / needed`.
- **Kills per level grow linearly:** `kills(L) = 10 + (L − 1)` even-level kills.
- **An even-level kill is worth** `10 × L` experience, so a level needs `kills(L) × 10 × L`.
- **The window:** `window(L) = 2 + floor(0.1 × L)`. It decides two things:
  - **Kill experience.** An enemy **below your level gives nothing**. One above you counts as its own level, up to
    `L + window(L)`; anything higher still pays only that much.
  - **Wielding.** You can equip gear up to `L + window(L)`.

| Level | Window | Highest kill / gear | Kills this level |
|------:|-------:|--------------------:|-----------------:|
| 1 | +2 | 3 | 10 |
| 10 | +3 | 13 | 19 |
| 50 | +7 | 57 | 59 |
| 100 | +12 | 112 | 109 |
| 1000 | +102 | 1102 | 1,009 |

- **Everything gives experience** through deeds (§8). Deed experience is written as a share of an even-level kill,
  so it scales with level automatically.
- **On level up:** +1 attribute point, full heal, and the level-up glow (§11).

---

## 3. Power: linear attributes, exponential level

- **Linear: what you earn.** Each attribute point adds a fixed percentage to the stats it feeds.
- **Exponential: level.** Every level is worth a growth factor of `1.05`.
  - Health, damage and armor, and every item's stats, are kept **relative to their owner's own level**.
  - Only the *difference* in levels between two beings is raised to a power. A level-60 striking a level-50 hits
    `1.05^10 ≈ 1.63×` as hard as an even match.
  - The gap is clamped at 1500 levels, so nothing overflows at any level.
- **Gear is exponential in its item level.** An item `k` levels above you counts `1.05^k` times its base, and one
  `k` below counts `1.05^−k`. That is how outgrown gear decays, with no separate penalty.
- **Displayed numbers are absolute** (`relative × 1.05^level`). They read with suffixes (`1.2K`, `3.4M`, `5.6B`,
  `7.8T`), then in scientific form.
- **A level gap hits twice.** Armor is weighed against the attacker's level too, so a much lower foe can barely
  break it.

---

## 4. Attributes: ADVENTURE

| Attribute | Feeds |
|---|---|
| **A**cuity | Critical chance, luck |
| **D**exterity | Attack speed, damage |
| **V**igor | Damage, carry capacity, throw power, work power |
| **E**ndurance | Health, stamina, regeneration, work power |
| **N**imbleness | Move speed, stamina |
| **T**enacity | Armor, poise, regeneration, health |
| **U**nderstanding | Learning, retention |
| **R**esolve | Poise, retention |
| **E**loquence | Persuasion |

- Every attribute starts at **1**.
- Character creation gives **9 points**, at most **5** in any one attribute.
- Each level gives **+1 point**, with no cap per attribute after creation.
- Each point above 1 adds the attribute's percentage to every stat it feeds. The percentages live in
  `Stats.arpg`.
- **Spending is staged.** On the Character page you place points and see your stats change live. Then you either
  confirm (twice, with a warning) or clear them.

---

## 5. Stats

`assets/stats/Stats.arpg` lists every stat. Each stat has:

- a base value;
- a display format (`whole`, `decimal` or `percent`);
- whether it is **scaled** (level-relative, §3);
- the attributes that feed it;
- how heavily and how strongly it can roll as a gear bonus.

| Stat | Scaled | Used by |
|---|---|---|
| health | yes | Damage taken (hidden; shown only through the darkness) |
| stamina | yes | Reserved for stamina use |
| damage | yes | Every blow and thrown item |
| armor | yes | Mitigation |
| health_regen | no | Share of health regained per second, after the regeneration delay |
| attack_speed | no | How fast every attack winds up and recovers |
| move_speed | no | Walk, run, sprint and swim speed |
| carry_capacity | no | Weight limit |
| crit_chance | no | Chance a blow lands critically |
| luck | no | Tier rolls on loot and on an NPC's own gear |
| learning | no | Trade and skill gain rate |
| retention | no | Slows trade and skill decay |
| poise | no | Resists knockback |
| persuasion | no | Affinity gains |
| throw_power | no | Throw strength |
| work_power | no | How hard a swing drives its weight: blow momentum, mining, digging and felling |

- **Total:** `(base + gear) × (1 + Σ attribute bonuses) × (1 + trade, skill and profession bonuses)`, clamped to the
  stat's range.
- **Caching:** each being's totals are cached and rebuilt only when something feeding them changes (gear, level,
  attributes, a trade or skill step, profession). They are never rebuilt per frame.

---

## 6. Gear

Every piece of gear (an item with stats) carries its own level, tier, condition and rolled bonuses.

- **Level.** The level it was forged at: the level of whoever dropped or wore it, or the level the loot table draws.
- **Tier.** Drawn from `Tiers.arpg` weights. The finder's luck and the loot table's `tier_bias` tilt the draw
  toward rarer tiers. The draw is seeded, so the same chest always yields the same gear.

| Tier | Color | Stat × | Bonuses | Weight | Frame |
|---|---|---:|---:|---:|---|
| Ashen | black | 1.00 | 0 | 1000 | Plain border |
| Ember | red | 1.15 | 0 | 450 | Colored border |
| Kindled | orange | 1.30 | 1 | 200 | Faint glow |
| Gilded | yellow | 1.50 | 1 | 90 | Glow |
| Verdant | green | 1.75 | 2 | 40 | Glow, pulse |
| Azure | blue | 2.05 | 2 | 16 | Glow, pulse, sweep |
| Regal | purple | 2.40 | 3 | 6 | Glow, pulse, sweep, motes |
| Radiant | white | 2.80 | 4 | 2 | All of the above plus a spectral sheen |

- **Frames.** Frames are drawn around inventory slots by the `TierFrame` shader. Every effect (`glow`, `pulse`,
  `sweep`, `motes`, `sheen`) is a per-tier data value. Item names in lists and details are tinted with the tier's
  color.
- **Item stat:** `definition base × tier multiplier × condition factor × 1.05^(item level − wielder level)`, plus
  bonuses scaled the same way.
- **Condition: old, never broken.**
  - Armor wears when its wearer is wounded. Weapons and tools wear when they strike or work.
  - At 0% an item still works at **50%** (`condition factor = 0.5 + 0.5 × condition`).
  - Details read the condition as a word.
- **Level lock.** Gear above your wield level can't be worn. You can still hold it in a hand, but it lends no stats
  or skill until you reach its level.
- **Stacking.** Two stacks merge only when their level, tier, condition and bonuses all match.

---

## 7. Trades and skills

**A skill belongs to an exact kind of item. A trade is the general craft those skills belong to.**

- **Skills.** Each item names its skill in its `skill` field (a sword is `longsword`, a pickaxe is `pickaxe`).
  - A skill rises only by using that kind of item: striking, guarding, throwing, breaking blocks, felling, sowing.
  - It grants its bonuses only while such an item is held, in either hand.
  - Bare hands train `unarmed`. Thrown items train `thrown`.
- **Trades.** Trades are broad crafts: Blades, Mining, Farming, Diplomacy and so on.
  - Each skill lists the trades it feeds and how much. Practising a skill practises those trades.
  - Deeds feed trades too.
  - A trade's bonuses always apply.
- **Trades carry over.** An item's effective skill is its own skill plus **25%** of the trades that skill feeds.

**Trades, skills and morality share one rule set.** Each is a value in a range, `−100` to `+100` by default (set
per definition). Below zero means inept: bonuses turn into penalties. A profession can start you negative in trades
it neglects.

- **Learning.** Gains shrink as the value climbs (`× 1 / (1 + value / 50)`), are multiplied by the *learning* stat
  and the active profession's trade learning, and are **tripled while below the past peak**.
- **Decay over game time.** Decay runs on the world clock and is settled in one closed-form step, so time away is
  caught up exactly.
  - Nothing decays for **2 game days** after last use.
  - After that a value drifts toward 0, **slower the higher it is** (`× 1 / (1 + |value| / 40)`), and slower still
    with *retention*.
  - It never falls below **25% of its peak** (muscle memory).
- **Steps.** A value's displayed step is its whole number, and each step gained fires a deed (§8).

---

## 8. Deeds

`assets/deeds/Deeds.arpg` lists every action the world cares about. Each deed carries:

- experience, as a share of an even-level kill;
- a morality shift;
- trade gains;
- whether it is counted (counts feed profession requirements).

Code fires a deed by name. The data decides what it is worth.

- **Fired now:** `kill`, `trade_practice`, `trade_advance`, `skill_advance`.
- **Defined, waiting for their systems:** quests, buying and selling, shops, land, titles, favors, charity, laws,
  theft, contraband, banishment, raids, recruitment, knighting, ordination, coronation and `king_slain_unseen`.
- **Kills shift morality against the victim's.** Killing the good darkens you, and killing the wicked lightens you.

---

## 9. Professions

- **One profession is active at a time.** You may switch to any profession you have unlocked.
- **Starting professions** can be picked in the character creator: Adventurer, Villager, Farmer, Woodcutter, Miner,
  Hunter and Peddler.
- **Every other profession unlocks** when all requirements of any one of its groups are met. A requirement can be:
  - a level;
  - a trade or skill value;
  - a morality bound;
  - a deed count;
  - another profession unlocked;
  - a **grant**, given by an event such as a captain recruiting you.
- **Effects:**
  - stat bonuses;
  - trade learning multipliers;
  - the trades a newcomer starts with (which can be negative);
  - the attribute weights an NPC holding it spends its points by.
- **Titles by gender.** For example, Baron/Baroness, Lord/Lady, King/Queen and Priest/Priestess.
- **NPCs hold professions too.** Each NPC file names one; otherwise the NPC is a Villager.

| Profession | Unlocks by |
|---|---|
| Guard | Granted (recruitment), morality ≥ 0, level ≥ 5 |
| Captain | Guard, Leadership ≥ 20, level ≥ 20 |
| Knight | Guard, morality ≥ 300, Defense ≥ 30, knighted |
| Blacksmith | Shop bought, 10 crafted items sold, Smithing ≥ 15 |
| Merchant | Shop bought, 100 items sold, Commerce ≥ 20 |
| Mercenary | 200 kills, level ≥ 15 |
| Rogue | 25 thefts, Stealth ≥ 15 |
| Smuggler | 10 contraband sales, Seafaring ≥ 10 |
| Outlaw | Banished |
| Raider | A village raided |
| Priest | Morality ≥ 500, ordained |
| Baron | Land bought, Leadership ≥ 30, a title granted |
| Lord | Baron, Diplomacy ≥ 40, two titles granted |
| King | (A king slain unseen **or** crowned), Leadership ≥ 60, level ≥ 50 |

---

## 10. Genders

`assets/genders/Genders.arpg` lists the genders: male, female and asexual. Each entity template lists the genders
its race may be made as. Humanoids may be male or female. A race with none listed (an ent, say) is asexual. The
creator steps through the race's genders, and an NPC's gender is drawn from its spawn.

---

## 11. Morality and affinity

- **Morality** is a value from −1000 to +1000. Deeds move it, and it does not decay.
- **Level-up glow.** A flash from the screen edges whose color follows morality: white at +1000, grey at 0, black at
  −1000, blended between.
- **Affinity.** Every being keeps a list of how much it likes each other being it knows (−100 to +100).
  - Wounds sour a victim on its attacker.
  - Persuasion multiplies affinity gains.
  - **Effective opinion** = personal affinity + a morality sway (like likes like).
  - Beings are keyed by a stable identity: a player by their character's name, an NPC by its world spawn. An NPC that
    reloads with its chunk is the same NPC.

---

## 12. Health, damage, regeneration and death

- **Health is hidden.** It is kept as a share of full health, so max health can change without healing or hurting.
- **Damage types.** `assets/damagetypes/DamageTypes.arpg` lists them, and each says whether armor applies and whether
  it bleeds. Weapons name each attack's `damage_type`; thrown items hit blunt.

| Type | Armor | Bleeds |
|---|---|---|
| slash | yes | yes |
| pierce | yes | yes |
| blunt | yes | no |
| fall | no | no |
| drowning | no | no |
| asphyxiation | no | no |

- **Every blow carries momentum**, `mass × speed`, swung or thrown alike.
  - A swing drives the weight of what is wielded, or the body's own weapon's `mass`, at
    `31.5 × work_power / (mass + 1.5)`.
  - A throw leaves at `36 × throw_power / (weight + 1.5)` and lands with the item's weight at whatever speed it still
    has.
- **A blow's amount:** `max(1, opening + momentum × 0.2) × the attack's damage`. A swing opens with the striker's
  damage stat; a throw opens with the item's own damage at the thrower's level.
- **Breaking:** a blow does `momentum / 20` points of durability work on a block or a tree.
  - The right tool, or any blow on a block that names no tool, adds its work up across strokes.
  - Anything else breaks a block only by doing its whole durability in one blow.
- **A blow:** `share = amount × 1.05^(source level − target level) / health`.
  - Armor turns part of it aside: `× 50 / (50 + armor × 1.05^(target level − source level))`.
  - A critical blow deals `× 2`.
  - The blow wears the target's armor and sours the target on its source.
- **The darkness.** A full-screen overlay of its own, separate from post-processing and from the squint.
  - It creeps in below **70%** health.
  - It is nearly black just above death and fully black at death.
- **The veins.** Bleeding wounds draw bloodshot veins in from the screen edges.
  - They are drawn procedurally in the same overlay.
  - They fade with time and as health returns.
  - Non-bleeding damage only darkens.
- **Regeneration.** The *health_regen* share per second, starting **5 seconds** after the last wound.
- **Death:**
  - The player returns to the main menu.
  - An NPC falls, scattering what it carried and its loot, drawn with the killer's luck.
  - The killer earns kill experience and the kill deed.

---

## 13. NPCs

- An NPC's level is drawn per resident from its file's `level_min` to `level_max`, seeded by its world spawn. The same
  resident always has the same level.
- It gets the same attribute points a player would have at that level, spent by its profession's weights.
- Its trades start from its profession and grow with level.
- Its gear is forged at its own level with its own luck.

---

## 14. UI

- **No HUD.** Health shows only through the darkness and the veins.
- **The Character page** opens from the inventory's equipment board. It shows the name, profession title, level,
  experience, morality word and unspent points, with tabs:
  - **Attributes:** stage points, see your stats change live, then confirm or clear.
  - **Trades:** each trade by group, at its step, with its past peak while it lies below it.
  - **Skills:** each skill's own step and its effective step.
  - **Profession:** every profession, with requirements and progress for locked ones; switch to an unlocked one.
- **The character creator:**
  - the Class tab is gone;
  - the **Attributes** tab spends the creation points;
  - the **Profession** tab picks a starting profession and shows what it lends;
  - the Body tab steps through the race's genders.
- **The inventory:**
  - tier frames on every slot;
  - a stats box on the equipment board;
  - item details show the tier, level (red when too high to wield), condition, and every stat and bonus at its
    absolute value.

---

## 15. Data files

| File | Schema | Holds |
|---|---|---|
| `assets/attributes/Attributes.arpg` | Attributes | The nine attributes |
| `assets/stats/Stats.arpg` | Stats | Every stat, its format, scaling and attribute feeds |
| `assets/genders/Genders.arpg` | Genders | Genders |
| `assets/tiers/Tiers.arpg` | Tiers | Gear tiers, weights, bonuses and frame effects |
| `assets/damagetypes/DamageTypes.arpg` | DamageTypes | Damage types |
| `assets/trades/Trades.arpg` | Trades | Trades, groups, ranges and bonuses |
| `assets/skills/Skills.arpg` | Skills | Skills, the trades they feed, their while-held bonuses |
| `assets/deeds/Deeds.arpg` | Deeds | Deeds |
| `assets/professions/Professions.arpg` | Professions | Professions, titles, requirements and effects |

Existing types gained fields:

- **items:** `skill`, and `stats` naming entries of `Stats.arpg`;
- **weapons:** each attack's `damage_type`;
- **NPCs:** `profession`, `level_min`, `level_max`;
- **loot tables:** `level_min`, `level_max`, `tier_bias`;
- **entities:** `genders`.

### Adding content

1. Decode the list file and its schema (`gradlew lwjgl3:arpg --args="decode assets/trades/Trades.arpg"`).
2. Add the entry to the decoded text. Names are lower-case snake case and must be unique.
3. Seal it with `encode` and check it with `verify`.

Things to know when adding content:

- A new stat can be given to items, professions, trades and skills straight away.
- An attribute only affects a stat when that stat lists it.
- A new deed does nothing until code fires it by name. Its count, though, can already gate professions.

---

## 16. Saving

Every per-being value lives on a handle that holds plain values and names, so a save can write and read it
directly. Nothing is saved yet. A continued character starts its progression fresh. This covers:

- level, level progress and unspent points;
- attributes;
- each trade and skill's value, peak and last use;
- morality;
- affinity entries;
- deed counts;
- unlocked and active professions;
- each item's level, tier, condition and bonuses.

---

## 17. Not yet built

Quests, buying and selling, shops and land, crafting and repair, fall and drowning damage sources, laws,
banishment, recruitment and titles. Their deeds and professions are defined and waiting. Hard and easy mode,
marriage and houses come later.

---

## 18. Tuning constants

| Constant | Value | Where |
|---|---|---|
| Growth per level, widest gap | 1.05, 1500 | `POWER_*` |
| Kills for level 1, added per level, experience per kill level | 10, 1, 10 | `KILLS_*`, `EXPERIENCE_PER_KILL_LEVEL` |
| Window base, per level | 2, 0.1 | `LEVEL_WINDOW_*` |
| Attribute start, creation pool, creation max, per level | 1, 9, 5, 1 | `ATTRIBUTE_*` |
| Condition floor | 50% | `CONDITION_FLOOR` |
| Learning softness, decay softness, relearn boost | 50, 40, ×3 | `PROFICIENCY_*` |
| Memory floor, decay grace | 25% of peak, 2 game days | `PROFICIENCY_*` |
| Skill carry from trades | 25% | `SKILL_TRADE_CARRY` |
| Armor mitigation scale, critical multiplier | 50, ×2 | `ARMOR_MITIGATION_SCALE`, `CRIT_DAMAGE_MULTIPLIER` |
| Regeneration delay | 5 s | `REGENERATION_DELAY_SECONDS` |
| Darkness start | 70% health | `RuntimeSetting.VITALITY_DARKNESS_START` |
| Morality range, affinity range | ±1000, ±100 | `MORALITY_MAX`, `AFFINITY_MAX` |
