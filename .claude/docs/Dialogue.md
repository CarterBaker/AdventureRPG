# Dialogue

How a being is talked to: what the player types, how it is understood, how a reply is chosen and worded, how the
models learn, and where the data lives. The code guide is `.claude/docs/DialoguePipeline.md`. Every constant is in
`EngineSetting` (the `// Dialogue` and `// Dialogue Models` groups), the talk menu's in `RuntimeSetting` and the
trainer's in `TrainerSetting`. `.claude/CLAUDE.md` still applies in full.

---

## 1. Principles

- **Free text in, generated text out, offline.** The player types anything; the being answers in words assembled for
  that being at that moment. Nothing is scripted line for line, nothing calls out of the game, and nothing but the
  content files ships.
- **A stated departure from the industry standard.** The standard for free-text generative characters today is a
  pretrained large language model. One cannot be met here: it is hundreds of megabytes to gigabytes, cannot be taught
  by a designer's verdicts, cannot be held to a dictionary, and cannot be bound to a being's profession and nature.
  The engine instead uses the classical statistical dialogue system, itself the standard wherever a system must be
  small, controllable and trainable in place: an intent classifier, a learned response policy and a statistical
  generator over a lexicon and a grammar, each a linear model over hashed features.
- **Everything is data.** Words, intents, replies, phrases, topics, traits, moods, relations, verdicts and scenarios
  are `.arpg` content with schemas, edited in the Hierarchy and the Properties tab like all content.
- **Models are built, never stored.** The three models are trained at every boot from the content and the lessons
  (§7), so they always match the dictionary and replies as they stand. This departs from shipping a trained model
  file: training takes tens of milliseconds, and a stored model would go stale with every content edit.
- **No history.** A talk keeps only the last line heard and the last line said (§9).
- **No kernel.** A turn is one short sentence against a few thousand weights: about 6 microseconds in Java. It is not
  a large batch and does not meet `.claude/docs/Kernel.md` §2, so it stays in Java. The weight tables are flat float
  arrays read row by row, so a kernel could take the scoring loop if a later, heavier model is ever measured to need it.

---

## 2. A turn

`DialogueManager.converse(exchange)` takes one `DialogueExchangeStruct` through three steps.

1. **Understand** (`DialogueUnderstandBranch`). The sentence is cut into words. Each is looked up in the dictionary by
   any of its forms, or by a form one letter away (a letter dropped, added, mistyped or swapped; forms of five letters
   or more only). Features are raised for every known word, every tag it carries, every unknown word, the opening word,
   each pair of neighbours and each `?` or `!`. The tone is the mean tone of the words that carry one; the topic is
   the first word that raises one. The understanding model gives every intent a share of belief; the largest wins, or
   the `confused` intent when no word was known or the share is under `DIALOGUE_CONFIDENCE_MIN`.
2. **Respond** (`DialogueRespondBranch`). The intent lands first: it moves the being's regard for the speaker and its
   moods (§5). The being as it then stands is the *decided* profile. The intent's replies the being may give are
   gathered (the `confused` intent's when none is open), each scored by its authored leans plus what the response model
   has learned, and one is drawn by those shares. The reply then moves regard and moods in turn and names its action.
3. **Word** (`DialogueWordingBranch`). The reply is worded `DIALOGUE_WORDING_CANDIDATES` times. Each wording draws one
   of the reply's lines the being may say, expands the phrases it calls for, fills its dictionary draws and values,
   and is polished (spaces, `a`/`an`, capitals, closing stop). The wording model scores each on its lines, its drawn
   words and its neighbouring word pairs; one that repeats the last thing said is marked down; one is drawn.

---

## 3. Content

| Type | Directory | What it is |
|---|---|---|
| Words | `words/` | The dictionary: `name` (the word as said), `forms` heard as it, `tags`, `tone` (-1..1), `when` |
| Intents | `intents/` | What a speaker can mean: `examples`, the `affinity` and `moods` it lands with, its `replies` |
| Replies | `replies/` | What a being can answer: `when`, `leans`, `lines`, `affinity`, `moods`, `action` |
| Phrases | `phrases/` | Named pieces of speech lines call for |
| Topics | `topics/` | Subjects: the `words` that raise one, `when` (who knows it), `lines` |
| Traits | `traits/` | Lasting nature and speech: `opposite`, `wordless`, resting `moods`, `affinity_gain`/`_loss` |
| Moods | `moods/` | Passing feelings: `half_life_seconds` |
| Relations | `relations/` | What one being is to another: `bond`, `inverse` |
| Verdicts | `verdicts/` | Trainer judgements: worth to `understanding`, `response`, `wording` |
| Scenarios | `scenarios/` | Trainer beings: `listener`, `profession`, `traits`, `moods`, `affinity`, `morality`, `relation` |
| Lessons | `lessons/` | Judged turns, written only by the trainer (no schema) |

All but lessons are array-layout files with a schema, named by `name`. The intent `confused` is required.

### Lines

A line is `{"text", "weight", "when"}`. Its text may hold:

- `{name}`: a phrase, expanded in place. `{topic}` is reserved: the lines of the topic raised.
- `[tag tag]`: a dictionary word carrying every tag listed that the being may say.
- `<player>`, `<name>`, `<title>`, `<subject>`: the speaker's name, the being's name, its profession's title, and the
  word that raised the topic.

A line naming `<subject>` is only said once a topic was raised; one expanding `{topic}` only of a topic the being
knows. Lines end without a closing stop where a reply adds one; the polish drops a doubled stop.

### Conditions (`when`)

`traits` (all held), `not_traits` (none held), `moods` (dominant mood one of; `calm` for none), `professions` (one
of), `relations` (one of; `stranger` for none), `affinity_min`, `affinity_max`, `topic` (`any`, `none`, `known`,
`unknown`). Words, lines, replies and topics all take it.

**Wordless beings.** A being with a `wordless` trait (a beast) is only ever given a reply or a topic whose `when`
names a wordless trait. Beasts therefore answer only in the replies written for them, and know no topics.

### Leans

A reply's `leans` say who it comes naturally to before training: `weight`, `traits`, `moods` (times the level felt),
`relations`, and `affinity`, `morality` and `tone` (each times a value from -1 to 1). A difference of about 1.5
between two replies is strong.

---

## 4. Beings

- **Traits** are fixed: an NPC definition lists `traits` every resident has, a `trait_pool` and `trait_draws` drawn
  from it by the resident's identity (never a trait it has or one opposed to one it has). A being holds at most
  `DIALOGUE_TRAIT_MAX`.
- **A name** is drawn from the dictionary words tagged `name` and the being's gender name, by identity.
- **Moods** are levels from 0 to 1. Each rests at the sum of what the being's traits lean it to and fades toward rest
  by its half life, settled in closed form whenever the being is next talked to. The strongest mood past
  `DIALOGUE_MOOD_THRESHOLD` is the dominant mood; none is calm.
- **Relations.** An NPC definition's `bonds` bind its residents to those of another NPC that come into the world
  within `blocks`: each holds what the other is to it. A relation's `bond` is added to the holder's opinion of the
  other (`ConductBranch.resolveOpinion`), so an NPC that `defends` takes the side of its young, mate or fellow.
- **Overhearing.** What is said to a resident moves the regard of residents within `NPC_EARSHOT_BLOCKS` who think well
  of that resident, by `NPC_OVERHEAR_SHARE` of the shift times how well they think of it.

All of this is on `DispositionHandle` (per being) and read into a `DialogueProfileStruct` for each turn.

---

## 5. Regard and mood

- An intent's affinity lands as `affinity * (1 + |tone|) + tone * DIALOGUE_TONE_AFFINITY`; a reply's as written.
- Gains are scaled by the product of the being's traits' `affinity_gain`, losses by `affinity_loss`.
- A mood shift is scaled by `1 +` the being's resting level of that mood.
- In game the shift goes through `ProgressionManager.shiftAffinity`, so the speaker's persuasion scales gains.

---

## 6. Actions

A reply's `action`: `none`, `leave` (ends the talk), `attack` (the resident turns on the speaker) and `flee` (it runs
from them for as long as they stay its foe). `DialogueAction` holds them; the runtime `DialogueBranch` performs them
through `NpcManager`.

---

## 7. Learning

- **Lessons.** A verdict on a turn is kept as a lesson: what was heard and meant, the being as it stood when it chose,
  the reply and its wording, the verdict and its three worths, and a better reply when named. Everything is by name or
  by a line's stable key, so lessons outlive content edits; a name no longer held is passed over.
- **Understanding** is multinomial logistic regression: every intent's examples and every lesson that says what a
  sentence meant, passed over `DIALOGUE_UNDERSTANDING_PASSES` times in a fixed shuffle.
- **Response** is a softmax policy: a verdict moves the given reply's share up or down among those that were open; a
  named better reply is moved toward directly.
- **Wording** raises or lowers every feature of the judged wording, and line weights follow, so a wording judged
  badly is built less often.
- **Determinism.** Features are hashed from names, never IDs, and training order is fixed, so the same content and
  lessons give the same models on every machine.
- **Lessons generalise.** A verdict on one being also shifts beings that share features with it. Judge varied beings
  rather than one being many times.

---

## 8. Tools

- **The dictionary and every other type** are edited in the Hierarchy and the Properties tab.
- **The trainer** (`Trainer.bat`, `gradlew lwjgl3:trainer`) holds practice talks over the console against a scenario,
  with the game's own content and models. Slash commands judge the last answer (`/good`, `/nonsense`, any verdict),
  correct it (`/meant <intent>`, `/better <reply>`), explain it (`/why`), take a lesson back (`/undo`), change being
  (`/scenario <name>`, `/reset`) and save (`/save`, `/quit`).

---

## 9. In game

Facing a calm resident within `DIALOGUE_REACH_BLOCKS`, the activate binding opens the talk menu (`menus/Dialogue.arpg`).
The resident stands and attends. The player types and Enter says it; the answer is shown with the line it answers,
and nothing earlier. The pause binding closes the talk; a reply that ends it lingers `DIALOGUE_END_SECONDS`.

---

## 10. Numbers

Measured on the direct build, 564 words, 32 intents with 467 examples, 80 replies:

| Measure | Value |
|---|---|
| Training at boot | 34 ms (no lessons), 37 ms with 16 |
| One turn | 5.9 microseconds, 264 bytes (the sentence's word strings) |
| Understanding | 464/467 on its examples, 109/111 on held-out paraphrases |
| Model memory | about 2.9 MB (understanding 16384 buckets x 33 intents, response 2048 x 81, wording 32768) |

---

## 11. Not yet built

See the handoff list delivered with delivery 77: the editor's trainer tab, saving dispositions, world knowledge in
answers, trade, gifts and romance actions, crimes, breeding, console text entry and more content.
