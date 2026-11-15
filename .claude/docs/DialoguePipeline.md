# Dialogue Pipeline — Instructions for Claude

The generative dialogue pipeline. The design and every rule are in `.claude/docs/Dialogue.md`. This file says where
things live, which method owns each operation, and the traps. `.claude/CLAUDE.md` still applies in full.

---

## Layout

- **`DialoguePipeline`** (`application/bootstrap/dialoguepipeline/`) is registered in `ApplicationBootstrapAssembly`
  after `ProgressionPipeline` (conditions name professions) and before `EntityPipeline` (NPCs name traits). It
  creates, in order: `MoodManager`, `TraitManager`, `RelationManager`, `WordManager`, `PhraseManager`, `TopicManager`,
  `ReplyManager`, `IntentManager`, `DialogueManager`, `VerdictManager`, `ScenarioManager`.
- **Registries.** Each is a standard Manager/Loader/Builder plus Handle/Data (`x/` and `xmanager/`), array layout,
  loaded in `awake()` with `requestAll()`. Every Data carries `seed`, the 32-bit hash of its name.
  - `WordManager` also owns the form index (`findWordID`: exact, then one letter off), the tag index
    (`getTagIDFromTagName`, `getWordIDsFromTagID`, `getTagSeed`).
  - `TopicManager` maps a word to the topic it raises (`getTopicIDFromWordID`).
  - `IntentManager` caches `getConfusedIntentID()`.
  - `TraitManager.areOpposed`.
- **`util/`:**

| Class | Owns |
|---|---|
| `DialogueProfileStruct` | The being a turn is held on: name, title, profession, traits, mood levels, affinity, morality, relation |
| `DialogueExchangeStruct` | One talk: profile, heard, said, and everything the last turn worked out; its own roll stream |
| `DialogueConditionStruct` | A `when`: `matches()` for words and lines, `permits()` (adds the wordless rule) for replies and topics |
| `DialogueLineStruct` | One line cut into segments, its key, weight and condition |
| `DialogueLeanStruct` | A reply's authored leans, `resolve(profile, tone)` |
| `DialogueWeightStruct` | One model: a flat table, `score` and `shift` |
| `DialogueArpgUtility` | `parseCondition`, `parseLines` (the one place a line is cut), `parseStrings` |
| `DialogueResolverStruct` | The managers names resolve against, for the parsers |
| `DialogueFeatureUtility` | `hashName`, `mix` (the one way a feature is named) |
| `DialogueMathUtility` | `softmax`, `pick`, `findHighest` |
| `DialogueTextUtility` | `polish`, word walking and hashing |

- **`dialoguemanager/`:** `DialogueManager` is the only public entry point; its branches are package-private.

| Branch | Owns |
|---|---|
| `DialogueUnderstandBranch` | `read` (features, tone, topic), `weigh` (intent shares), `hear` |
| `DialogueRespondBranch` | The intent landing, `weigh` (candidates, features, shares), the draw, the reply landing |
| `DialogueWordingBranch` | `word`, line and word draws, `raiseFeatures` |
| `DialogueLearnBranch` | `train` (all three from nothing), `learn` (one lesson), `record` |
| `DialogueBeingBranch` | `dispose`, `relate`, `read` (entity to profile, moods settled), `write` (profile back) |

  `DialogueLoader`/`DialogueBuilder` load the lessons; `LessonStruct` (`lesson/`) writes itself with `toArpg()`.
- **Per being:** `entitypipeline/progression/DispositionHandle` (name, traits, moods and their settle time,
  relations and bonds), reset in `ProgressionManager.resetBeing`.
- **NPCs:** `NpcData` carries `traitIDs`, `traitPoolIDs`, `traitDraws`, `bonds` (`NpcBondStruct`).
  `NpcManager.raise` calls `disposeBeing`; `spawnResident` bonds the newcomer with every resident through
  `NpcPerceptionBranch.bond`. `NpcInstance` holds the `talker` and `frightened`.
- **In game:** `runtime/menueventsmanager/menus/dialogue/DialogueBranch` and `assets/menus/Dialogue.arpg`.
- **Trainer:** `engine/root/TrainerEngine`, `application/trainer/` (`TrainerContext`, `TrainerSetting`,
  `talk/TrainerTalkSystem`), `lwjgl3/Lwjgl3LauncherTrainer`, the `trainer` task in `lwjgl3/build.gradle`.

---

## Single call sites

| Operation | Method |
|---|---|
| A turn on a profile (trainer, tests) | `DialogueManager.converse(exchange)` |
| A turn with a living being | `DialogueManager.converse(exchange, listener, speaker)` |
| A talk's exchange | `DialogueManager.createExchange()`, then `exchange.begin(seed)` per talk |
| A verdict | `DialogueManager.judge(...)`; undo `forgetLesson()`; persist `saveLessons()`; rebuild `retrain()` |
| A being's nature | `disposeBeing` (from `NpcManager.raise`), `nameBeing` (from `SaveManager`), `relateBeings` |
| The being the player faces | `PlayerManager.findFacedEntityForWindow` (through `CombatManager.findTarget`) |
| A resident in talk | `NpcManager.beginTalk` / `endTalk`, `confront`, `frighten`, `overhear` |

---

## Traps

- **Never persist or hash an ID.** Features come from `seed`s and `DialogueFeatureUtility.mix`; lessons hold names.
  A feature built from an ID would train differently on every machine.
- **One feature path.** Live turns and lesson replay must raise identical features. `DialogueLearnBranch` calls the
  same `read`, `weigh` and `raiseFeatures` a live turn runs; never rebuild features anywhere else.
- **The decided profile.** A reply is chosen on, and a lesson records, the profile after the intent landed and before
  the reply did (`exchange.getDecided()`). Conditions and features read that, not the live profile.
- **Branch scratch is sized in `awake()`.** `DialogueManager.awake()` builds the weight tables; branch `awake()`s run
  after it, and `DialogueLearnBranch` is created last because its `awake()` trains.
- **Phrases may call each other.** `PhraseBuilder` resolves a called phrase with `registerPhraseName` (no load), and
  `PhraseManager.awake()` catches a name no file declares. Other builders use `getPhraseIDFromPhraseName`.
  Expansion stops at `DIALOGUE_EXPANSION_DEPTH_MAX`.
- **A form belongs to one word, a word raises one topic.** Both are load errors.
- **Wordless content needs a wordless trait in its `when`** at the reply or topic; lines inside need none.
- **Every directory must hold a file.** A loader fails on a missing directory, and git keeps no empty one.
  `lessons/Lessons.arpg` ships empty; never ship it again, it is the owner's training.
- **Lessons from other files are read-only.** Only lessons recorded this session or loaded from
  `DIALOGUE_LESSON_FILE` are written by `saveLessons()`.
- **The trainer's console.** The engine's log takes over `System.out`; the launcher hands the real console to
  `TrainerEngine` before it starts.

---

## Recipes

- **New word, intent, reply, phrase, topic, trait, mood, relation, verdict or scenario.** Add the entry to its file
  (or in the editor). An intent needs examples and at least one reply; a reply needs a line.
- **New line feature** (a bracket kind or a value). `DialogueSegment` or `DialogueValue`, cut in
  `DialogueArpgUtility.cutLine`, written in `DialogueWordingBranch.expand` / `appendValue`.
- **New condition key.** `DialogueConditionStruct`, `DialogueArpgUtility.parseCondition`, the `when` object in
  every schema that carries it.
- **New context the response model should weigh** (a crime witnessed, the hour). A value on `DialogueProfileStruct`
  (and on `LessonStruct`, by name), a salt in `EngineSetting`, one `raise` in `DialogueRespondBranch.raiseFeatures`,
  filled in `DialogueBeingBranch.read`.
- **New action.** `DialogueAction`, the `action` enum of the Replies schema, a case in `DialogueBranch.act`, and the
  one `NpcManager` (or other manager) method that performs it.
- **Changing content from text.** `.claude/docs/ARPG.md`: decode, edit, encode, verify.
