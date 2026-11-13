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
- **Tick**: the unit of musical time, `EngineSetting.SONG_TICKS_PER_BEAT` (480, the MIDI standard) to a quarter-note
  beat, as MIDI counts it. **Tempo**: quarter-note beats per minute. **Bar**: `beats_per_bar` beats of the note value
  `beat_unit` names (4 a quarter, 8 an eighth), so a bar spans `beats_per_bar × 480 × 4 / beat_unit` ticks.
- **Scale**: the pitches that sound right together in a key (C major, A minor, ...). **Scale Lock** keeps drawn and
  typed notes on them.

## Industry-standard choices

- **Sample-based instruments** in the SFZ region model (key range, velocity range, round robin, sustain loop,
  envelope), the model every sampler (SFZ players, Kontakt, SoundFont players) uses. Instruments are converted from
  the owner's VSCO 2 CE and VCSL libraries (both CC0: no credit, no restriction) by Claude, with
  `.claude/tools/SfzConvert.py`, and delivered as finished engine content.
- **Sustain loops through OpenAL Soft's `AL_SOFT_loop_points`**, set on each sample's buffer from its sound companion's
  `loopStart`/`loopEnd`. The voice loops while the note is held and stops looping on release, so it plays its tail:
  SFZ `loop_sustain`.
- **MIDI** (Standard MIDI File, type 1) is the interchange format, read and written with `javax.sound.midi`. The engine
  already needs the `java.desktop` module, so this is no new dependency. ABC is not supported.
  - Reading takes type 0 and 1 at any resolution, rescaled to 480 ticks a beat: every tempo change (rounded to three
    decimals and held inside the song range; 120 before the first), the first time and key signatures (a major or
    minor key), and a lane with one notes clip from tick 0 for every track and channel that plays notes, named after
    its track (`<track> Ch <n>` when a track plays several channels) and set by its first volume (CC7) and pan (CC10).
  - A part plays on the instrument whose `midi_program` matches the program its channel holds at its first note (the
    first by name when two share one), channel 10 on the instrument marked `percussion`, and anything else on
    `SONG_DEFAULT_INSTRUMENT` (`FolkHarp`). The first lookup loads every instrument once to index them.
  - Writing gives type 1: a first track with the song's name, time signature, key (major and minor only) and tempo
    map, then a track per lane with a channel per instrument it plays (its program, lane volume and pan at tick 0;
    percussion on channel 10; more than 15 other parts is refused). It writes exactly what playback plays, nested songs
    laid flat; sound clips cannot go into MIDI and are counted for the caller. Clip volumes are not written.
- **A piano roll** edits notes (pitch rows down the side, time across, notes as bars), inside the recorder's timeline,
  which edits clips. Musical Typing plays notes from the computer keyboard, using the layout DAWs use.
- **Nested recordings** work like Premiere's nested sequences: a recording or song laid inside another is a reference,
  so editing it changes every place it is laid. Any loop, direct or through other files, is refused, naming the loop.
  A recording's clip plays a take, a nested recording, a song or a notes clip; a nested clip's span is kept as asked and
  held within the child's current length only when read. A nested recording showing a picture must share its parent's
  world. Double-clicking a nested clip opens it (Back returns, the way down shown as a breadcrumb), and Nest Selection
  moves the selected clips into a new recording laid where they were (Premiere's Nest, After Effects' Pre-compose).
- **Recording time**: a recording keeps a tempo map and meter (`tempos`, `beats_per_bar`, `beat_unit`, written through
  `SongArpgUtility`, 120 in four four by default). A notes clip is timed by that map where it lies on the timeline, as
  a DAW's MIDI clip is; a song clip by the song's own map from its start.
- **Render** mixes a song offline and encodes it to the engine's sound format, Ogg Vorbis at 48 kHz, with the same
  ffmpeg settings `.claude/Delivery.bat` uses.

## Deliberate departures

- **Player songs are `.mid` files**, not `.arpg`. Players bring MIDI from anywhere, so the game reads
  `Documents/My Games/AdventureRPG/Songs/*.mid` (`EngineSetting.SONG_USER_DIRECTORY`). Everything the engine itself
  writes is still `.arpg`.
- **Library conversion happens only from `.claude/`.** The engine and the editor never read `.sfz` or `.wav`, never
  run ffmpeg to bring an instrument in and never acknowledge `.claude/`. Claude converts an instrument from
  `.claude/assets/` with `.claude/tools/SfzConvert.py` (the SFZ is read, each sample trimmed, mixed to mono, brought
  to full scale and encoded with Delivery.bat's ffmpeg settings, every `.arpg` sealed by the ARPG tool) and delivers
  the results; files Claude needs to see reach `.claude/assets/` through a fetch zip (`.claude/CLAUDE.md`).
- **The editor runs ffmpeg** only to render songs (a later stage). The game never runs it. The editor uses `ffmpeg` on
  the PATH, else the copy `Delivery.bat` keeps at `%LOCALAPPDATA%\AdventureRPG\ffmpeg.exe`. Without either it refuses
  with a message telling the owner to run `Delivery.bat` once. Every ffmpeg call goes through one editor utility, and
  through the thread pipeline.

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

- **Rules** (`SongBuilder`, `SongArpgUtility`, mirrored in `Songs.arpg`): `tempos` start at tick 0 and climb, each
  `bpm` inside `SONG_BPM_MIN`..`SONG_BPM_MAX` (20 to 999); `beat_unit` is 1, 2, 4, 8, 16 or 32; at most
  `SONG_LANE_COUNT` (16) lanes; a clip's `lane` is one the song has, its `length` at least 1, its `volume` 0 to 1, its
  `transpose` within ±48; `notes` hold whole notes of four ints, each pitch 0 to 127, velocity 1 to 127, and every note
  inside its clip's `length`; a `song` clip names a song, and no song reaches itself through its song clips at any
  depth (the Builder and the editor both refuse it, naming the loop: `A > B > C > A`).
- **Offset**: every clip plays its source from `offset` ticks in, so a source tick `t` sounds at `start - offset + t`,
  heard only inside `start`..`start + length`. A notes clip's source is its own notes (the editor normally keeps its
  offset at 0 and moves the notes); a song clip's is the song it lays; a sound clip's is its sound, started that far in.
- **Nesting**: a song laid inside another plays at the outer song's tempo, its clips opened in place with every
  offset, transpose and volume around them added up; a muted clip, a muted lane, or an unsoloed lane while another of
  its song is soloed, plays nothing at any depth. Playback and MIDI export share this one layout
  (`SongFlattenUtility`).
- **Mix**: a note plays at its velocity, scaled by its clips' and lanes' volumes. A lane's `pan` is kept and exported to
  MIDI, but is not yet heard live: the sound pipeline has no pan for a voice that is not placed in the world.

An instrument can be played by every race whose rig it names. A race keeps one rig for all its genders, so a rig entry
is the per-race animation, exactly as weapons name their attack clips per rig.

## Where things live

- Code: `application/bootstrap/musicpipeline/` (`MusicPipeline`, managers `NoteSetManager`, `InstrumentManager`,
  `SamplerManager`, `SongManager`, each in its own `xmanager/` folder with its data beside it), registered in
  `ApplicationBootstrapAssembly` and `LauncherBootstrapAssembly` beside `SoundPipeline`.
- Shared stateless logic in `musicpipeline/util/`: `NoteUtility` (names, pitch ratios, scales), `NoteEnvelopeUtility`
  (one envelope curve for live and offline playback), `SongArpgUtility` (the one clip and note codec, used by
  `SongBuilder` and the editor), `SongFlattenUtility` (one layout of a song, nested songs opened in place, for playback
  and export), `SongMidiUtility` (MIDI read and write), `SamplerMixUtility` (offline mixing for render and video
  export).
- Songs play through `SongPlaybackInstance` (`song/`), created by the system that plays one and updated by it each
  frame.
- Player songs: `SongManager.getUserSongNames()` lists the `.mid` files newest first, `loadUserSong(name)` reads one
  into a `SongHandle` kept by name apart from the palette (no song ID), and `releaseUserSong` lets it go with the last
  play that holds it; an unreadable file is logged and plays nothing. Project songs keep assigned IDs and load on
  demand. The game and editor launchers create `Songs/` with their other required directories.
- Songs the owner makes are Music files, saved by the editor straight into `assets/songs/` through the song codec and
  listed under Songs in the Recordings tab; New asks Video or Music. The Timeline shows a Music file's
  `SONG_LANE_COUNT` lanes with a mixer strip each, a bars-and-beats ruler and a beat grid it snaps to, refuses takes and
  nested recordings, draws notes clips with Draw, and exports through its Export MIDI and Render actions. The Piano Roll
  tab edits the notes clip selected there; Musical Typing plays and records into it.
- Songs the owner makes are saved by the editor straight into `assets/songs/`. Exported MIDI goes to
  `Documents/My Games/AdventureRPG/Songs/`, where player songs live. Video recordings stay in `Recordings/`.
- Constants: `EngineSetting` (`// Music ...` groups) for anything the game uses; `EditorSetting` for the editor.

## Performance

- Note sets load on demand, the first time an instrument is held, laid or previewed, never at bootstrap. An
  instrument's samples live in `assets/sounds/notes/<Instrument>/` and load with its note set, decoded on a worker.
- Instrument samples are `mono` and trimmed with `length` to their loop end plus release tail. Each stage that
  touches loading states the decoded bytes of every converted note set.
- `SamplerManager` keeps a fixed array of live notes sized once from `EngineSetting`. Note-on, note-off, envelope and
  voice stealing allocate nothing. Notes take voices from `SoundManager`'s pool through its API, with each
  instrument's `polyphony` as its limit.
- Song scheduling walks the packed note arrays between the last and the current tick, with no iterators or boxing.
  A song is laid out once, when it starts, into cursors the playback instance keeps and reuses; held notes live in two
  fixed int arrays. Playing the test song measured 0 bytes allocated by `play` on a reused instance and 0 bytes across
  265 frame updates (and across 1,057 updates driven in one frame).
- A song's clock starts only once every instrument it plays has its samples, so its first notes are never lost.
- Musical Typing and the Piano Roll's playhead allocate nothing per frame (fixed arrays, positions built once); a note
  written, drawn or edited is an edit and builds one new codec clip. Render decodes and mixes on the General thread in
  stretches of `RECORDER_RENDER_STRETCH_FRAMES`.
