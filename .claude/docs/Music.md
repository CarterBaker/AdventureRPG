# Music

The design of the music pipeline (`MusicPipeline`): instruments, note sets, the sampler that plays them, songs, MIDI,
the recorder's music side and playing music in the game. Read it before touching any of those. The code guide,
`.claude/docs/MusicPipeline.md`, is written by the first stage and kept current by every stage after it.
`.claude/CLAUDE.md` still applies in full.

The work is built in five stages, one delivery each, from `.claude/prompts/Music1InstrumentsPrompt.md` to
`Music5InGameMusicPrompt.md`. Each stage's prompt is pasted into a new conversation; this file holds the decisions they
all share.

---

## Words

The owner is not a musician, so code, labels and hints use these plain meanings:

- **Note**: one sound at one pitch, from a start for a length, at a velocity (how hard it is played, 1 to 127).
- **Pitch**: the MIDI note number, 0 to 127; 60 is middle C (`C4`). One step is a semitone; 12 steps are an octave.
- **Sample**: one recording of an instrument playing one note. **Multi-sampled**: recorded every few notes, the notes
  between pitched from the nearest recording.
- **Region**: which sample plays for which range of pitches and velocities (the SFZ term).
- **Sustain loop**: a stretch inside a sample that repeats while the note is held; on release the sample plays on
  through its recorded tail.
- **Envelope**: how a note fades in (attack), settles (decay to sustain) and fades out after release (release).
- **Tick**: the unit of musical time, `EngineSetting.SONG_TICKS_PER_BEAT` (480, the MIDI standard) to a beat.
  **Tempo**: beats per minute. **Bar**: `beats_per_bar` beats.
- **Scale**: the pitches that sound right together in a key (C major, A minor, ...). **Scale Lock** keeps drawn and
  typed notes on them.

## Industry-standard choices

- **Sample-based instruments** in the SFZ region model (key range, velocity range, round robin, sustain loop,
  envelope), the model every sampler (SFZ players, Kontakt, SoundFont players) uses. Instruments are imported from the
  owner's VSCO 2 CE and VCSL libraries (both CC0: no credit, no restriction) by an SFZ importer in the editor.
- **Sustain loops through OpenAL Soft's `AL_SOFT_loop_points`**, set on each sample's buffer from its sound companion's
  `loopStart`/`loopEnd`. The voice loops while the note is held and stops looping on release, so it plays its tail:
  SFZ `loop_sustain`.
- **MIDI** (Standard MIDI File, type 1) is the interchange format, read and written with `javax.sound.midi`. The engine
  already needs the `java.desktop` module, so this is no new dependency. ABC is not supported.
- **A piano roll** edits notes (pitch rows down the side, time across, notes as bars), inside the recorder's timeline,
  which edits clips. Musical Typing plays notes from the computer keyboard, using the layout DAWs use.
- **Nested recordings** work like Premiere's nested sequences: a recording or song laid inside another is a reference,
  so editing it changes every place it is laid. Any loop, direct or through other files, is refused.
- **Render** mixes a song offline and encodes it to the engine's sound format, Ogg Vorbis at 48 kHz, with the same
  ffmpeg settings `.claude/Delivery.bat` uses.

## Deliberate departures

- **Player songs are `.mid` files**, not `.arpg`. Players bring MIDI from anywhere, so the game reads
  `Documents/My Games/AdventureRPG/Songs/*.mid` (`EngineSetting.SONG_USER_DIRECTORY`). Everything the engine itself
  writes is still `.arpg`.
- **The editor runs ffmpeg** to import samples and render songs. These are owner-only tools: the game never runs it.
  The editor uses `ffmpeg` on the PATH, else the copy `Delivery.bat` keeps at `%LOCALAPPDATA%\AdventureRPG\ffmpeg.exe`.
  Without either it refuses with a message telling the owner to run `Delivery.bat` once. Every ffmpeg call goes through
  one editor utility, and through the thread pipeline.
- **The SFZ importer reads `.sfz` and `.wav` files.** It is an import tool, like MIDI import; nothing at runtime reads
  them.

## Content types

All are schema-driven (`assets/schemas/<Type>.arpg`), so they appear in the Hierarchy and the Properties tab with no
editor code. Each Builder rule is mirrored in its schema. Keys use snake_case, as in `Weapons.arpg`; sound companions
keep their camelCase.

| Type | Directory | Holds |
|---|---|---|
| NoteSets | `assets/notesets/` | `regions[]`: `sound`, `root`, `low`, `high`, `velocity_low`, `velocity_high`, `round_robin`, `round_robin_count`, `tune` (cents), `volume` (dB), `loop_mode` (`none`, `one_shot`, `sustain`, `continuous`); plus `envelope` {`attack`, `hold`, `decay`, `sustain`, `release`} and `velocity_tracking` |
| Instruments | `assets/instruments/` | `display_name`, `note_set`, `low`, `high`, `polyphony`, `volume`, `midi_program` (General MIDI 0..127, or -1), `percussion` (plays on MIDI channel 10), `rigs` {rig name: {`hold`, `play`}} looping clips made for that rig |
| Items | (existing) | gains `instrument`, read exactly as `weapon` is |
| Songs | `assets/songs/` | `display_name`, `tempos[]` {`tick`, `bpm`}, `beats_per_bar`, `beat_unit`, `key_root`, `key_scale`, `lanes[]` {`name`, `volume`, `pan`, `muted`, `solo`}, `clips[]` (below) |
| Sounds | (existing) | companions gain `loopStart`, `loopEnd` (seconds; both 0 for none) |

A song clip has `kind` (`notes`, `song`, `sound`), `lane`, `start`, `length`, `offset` (ticks), `muted`, `volume`,
`transpose` (semitones); a `notes` clip has `instrument` and `notes`, a flat int array of `start, length, pitch,
velocity` per note, in ticks from the clip's start; a `song` clip has `song`; a `sound` clip has `sound`.

An instrument can be played by every race whose rig it names. A race keeps one rig for all its genders, so a rig entry
is the per-race animation, exactly as weapons name their attack clips per rig.

## Where things live

- Code: `application/bootstrap/musicpipeline/` (`MusicPipeline`, managers `NoteSetManager`, `InstrumentManager`,
  `SamplerManager`, `SongManager`, each in its own `xmanager/` folder with its data beside it), registered in
  `ApplicationBootstrapAssembly` and `LauncherBootstrapAssembly` beside `SoundPipeline`.
- Shared stateless logic in `musicpipeline/util/`: `NoteUtility` (names, pitch ratios, scales), `NoteEnvelopeUtility`
  (one envelope curve for live and offline playback), `SongArpgUtility` (the one clip and note codec, used by
  `SongBuilder` and the editor), `SongMidiUtility` (MIDI read and write), `SamplerMixUtility` (offline mixing for
  render and video export).
- Songs the owner makes are saved by the editor straight into `assets/songs/`. Exported MIDI goes to
  `Documents/My Games/AdventureRPG/Songs/`, where player songs live. Video recordings stay in `Recordings/`.
- Constants: `EngineSetting` (`// Music ...` groups) for anything the game uses; `EditorSetting` for the editor.

## Performance

- Note sets load on demand, the first time an instrument is held, laid or previewed, never at bootstrap.
- Instrument samples are `mono` and trimmed with `length` to their loop end plus release tail. Each stage that
  touches loading states the decoded bytes of every imported note set.
- `SamplerManager` keeps a fixed array of live notes sized once from `EngineSetting`. Note-on, note-off, envelope and
  voice stealing allocate nothing. Notes take voices from `SoundManager`'s pool through its API, with each
  instrument's `polyphony` as its limit.
- Song scheduling walks the packed note arrays between the last and the current tick, with no iterators or boxing.
