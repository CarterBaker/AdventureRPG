# Music Pipeline (code guide)

Where the music pipeline's code lives, which method owns each operation, and the traps. The design is
`.claude/docs/Music.md`; read both before touching instruments, note sets, the sampler, songs, MIDI or the recorder's
music side. `.claude/CLAUDE.md` still applies in full. Each music stage keeps this file current.

---

## Where things live

| What | Where |
|---|---|
| Pipeline | `application/bootstrap/musicpipeline/MusicPipeline.java`, registered after `SoundPipeline` in `ApplicationBootstrapAssembly` and `LauncherBootstrapAssembly` |
| Note sets | `notesetmanager/` (`NoteSetManager`, `NoteSetLoader`, `NoteSetBuilder`), `noteset/` (`NoteSetData`, `NoteSetHandle`, `NoteRegionStruct`, `NoteEnvelopeStruct`, `NoteLoopMode`) |
| Instruments | `instrumentmanager/` (`InstrumentManager`, `InstrumentLoader`, `InstrumentBuilder`), `instrument/` (`InstrumentData`, `InstrumentHandle`, `InstrumentRigStruct`) |
| Sampler | `samplermanager/SamplerManager`, `sampler/SamplerNoteStruct` |
| Songs | `songmanager/` (`SongManager`, `SongLoader`, `SongBuilder`), `song/` (`SongData`, `SongHandle`, `SongClipStruct`, `SongTempoStruct`, `SongLaneStruct`, `SongClipKind`, `SongCursorStruct`, `SongPlaybackInstance`) |
| Shared logic | `util/NoteUtility` (names, pitch ratios, gains, scales), `util/NoteScale`, `util/NoteEnvelopeUtility` (the one envelope curve), `util/SongArpgUtility` (the clip and note codec), `util/SongFlattenUtility` (a song laid out flat), `util/SongMidiUtility` (MIDI read and write) |
| Constants | `EngineSetting` groups `// Music Notes`, `// Music Scales`, `// Music Note Sets`, `// Music Instruments`, `// Music Sampler`, `// Music Songs`, `// Music MIDI`, `// Music Playback`; `SONG_USER_DIRECTORY` in `// Engine & Application` |
| Content | `assets/notesets/<Instrument>.arpg`, `assets/instruments/<Instrument>.arpg`, samples and companions in `assets/sounds/notes/<Instrument>/`, `assets/songs/<Folder>/<Song>.arpg` (`Test/Scale` proves the pipeline); schemas `NoteSets.arpg`, `Instruments.arpg`, `Songs.arpg` |
| Player songs | `Documents/My Games/AdventureRPG/Songs/*.mid`, created by `Lwjgl3Launcher` (game) and `Lwjgl3LauncherEditor` as required directories |
| Items | `instrument` field, read by `ItemDefinitionBuilder.parseInstrument` exactly as `weapon`; `ItemDefinitionHandle.getInstrumentID` / `hasInstrument`; an item played as an instrument is gear |
| Conversion | `.claude/tools/SfzConvert.py`, Claude-side only; nothing in the engine or the editor reads SFZ or WAV |

## Who owns what

- **Load an instrument:** `InstrumentManager.getInstrumentIDFromInstrumentName` (on demand; `InstrumentLoader`
  persists). Building it loads its note set's definition through `NoteSetManager.getNoteSetIDFromNoteSetName`, never
  its samples.
- **Load a note set's samples:** `NoteSetManager.prepare(noteSetID)`, once; it asks `SoundManager.requestSoundAsync`
  for every region's sound and `NoteSetManager.update` marks it ready (`isReady`) once every sample has landed,
  checking that every looping region's sample has loop points. Call it the first time an instrument is held, laid or
  previewed.
- **Find the region a note plays:** `NoteSetHandle.findRegion(pitch, velocity, roundRobinTurn)` over the per-pitch
  index `NoteSetBuilder.buildIndex` makes once. Live and offline playback both use it.
- **Play:** `SamplerManager.noteOn` / `noteOnAt` (placed), `noteOff`, `stopAll`. One path, `start`, begins every note;
  `letGo` lets every note go (note-off, same-pitch retrigger, polyphony); `cut` stops one at once (slot stealing,
  `stopAll`, a finished release); `update` runs every envelope.
- **Gain and pitch:** region gain (`NoteUtility.toGain(volume)`) times `NoteUtility.toVelocityGain` times instrument
  volume times `NoteEnvelopeUtility.gainAt` / `releaseGainAt`; pitch `NoteUtility.toPitchRatio(pitch, root, tune)`,
  applied through `SoundManager.setVoicePitch` (which pitch bend will also use).
- **Load a song:** `SongManager.getSongIDFromSongName` (on demand; `SongLoader` persists). `SongBuilder.build` reads
  every clip through `SongArpgUtility.readClip`, resolves a notes clip's instrument
  (`InstrumentManager.getInstrumentIDFromInstrumentName`), a song clip's song (`resolveSong`, which loads it on demand)
  and checks a sound clip's sound exists (`SoundManager.hasSoundFile`); the sound's ID is found when the song plays.
- **Refuse a loop:** `SongBuilder.buildingSongNames` is the chain of songs being built, each laying the next. A song
  clip naming one of them fails at once with the loop (`describeLoop`), before anything loads twice; a song already
  built cannot reach one being built for the first time, so the chain is the whole check. The editor's twin is the
  schema's `acyclic` rule (`InfoValidationBranch.validateAcyclic`), which walks the live entries.
- **Convert ticks and seconds:** only `SongTempoStruct.toSeconds(tick)` and `toTicks(seconds)`, over the tempo map with
  the seconds of every change worked out once.
- **Lay a song out:** `SongFlattenUtility.flatten(songData, lane, cursors)`: every notes and sound clip that plays,
  nested songs opened in place, each a `SongCursorStruct` with its window, shift, transpose, gain and lane of the
  played song. Playback and `SongMidiUtility.write` both use it, so export is exactly what plays.
- **Play a song:** `create(SongPlaybackInstance.class)`, then `play(songID, instrumentOverride, lane)` or
  `playUserSong(name, ...)`, `update(deltaTime)` every frame, `stop()`. `start` lays it out, resolves each cursor's
  instrument (the override when not `INSTRUMENT_NONE`) and sound once and calls `NoteSetManager.prepare`; `update`
  waits until `isReady`, then lets go held notes whose end tick has passed (`letGoNotes`), walks each cursor's notes
  from its `nextNote` to the current tick (`walk`), plays them through `SamplerManager.noteOn(..., volume)` and keeps
  their handles in `heldNotes`/`heldEndTicks` (`hold`, which lets the soonest-ending note go when all
  `SONG_PLAYBACK_HELD_NOTE_COUNT` are taken); a sound clip starts through `SoundManager.playSound` from
  `toSeconds(windowStart) - toSeconds(shift)` into its sound and stops at its window's end (`walkSound`). It stops
  itself once the song's length has passed and nothing is held.
- **MIDI:** `SongMidiUtility.read(file, instrumentManager)` and `write(songData, file, instrumentManager)`, both
  throwing a catchable `InternalException` for a file they cannot handle. Program and percussion lookups go through
  `InstrumentManager.getInstrumentIDFromMidiProgram` / `getPercussionInstrumentID`, indexed once
  (`indexMidiPrograms`, which loads every instrument through `InstrumentLoader.requestEvery`).
- **Player songs:** `SongManager.getUserSongNames`, `loadUserSong` (holds one more play; reads through
  `SongMidiUtility.read` when none holds it), `releaseUserSong`, `isUserSongHeld`. `SongPlaybackInstance` loads on
  `playUserSong` and releases on `stop`, so a user song lives exactly as long as something plays it.
- **Sustain loops:** `SoundManager.createBuffer` sets `loopStart`/`loopEnd` on the buffer at upload, through
  `Audio.setBufferLoopPoints` (`AL_SOFT_loop_points`, checked in `Lwjgl3Audio.openDevice`). `NoteLoopMode` decides
  whether a voice loops while held (`loopsHeld`), after release (`loopsReleased`), and whether note-off fades it
  (`releases`); the sampler only reads those.

## Converting an instrument (Claude-side)

1. The owner's SFZ and the WAVs it names must be in `.claude/assets/`. If they are only in `.claude/catalogue/`, send a
   fetch zip (`.claude/CLAUDE.md`) and ask the owner to commit and push.
2. Compile directly (`.claude/docs/Build.md`) and run, from the project root:

   ```
   python3 -I .claude/tools/SfzConvert.py "<.sfz under .claude/assets>" <Instrument> \
       --arpg "java -cp $S/out:<jars> lwjgl3.Lwjgl3LauncherArpg" --scratch $S/convert \
       [--display-name "<Name>"] [--midi-program <0..127>] [--percussion]
   ```

   It refuses a keyswitch patch, prints the report (opcodes left out, crossfades split at their middle, envelope values
   most regions give, loops, decoded bytes) and writes the samples, companions, note set and, when none exists, the
   instrument. Re-running replaces the samples and note set and keeps the instrument file.
3. `verify` every file it wrote, boot the editor once (the builders are the final authority), and state each note
   set's decoded bytes. Deliver the files; the note sets and sounds are the owner's library and are never catalogued,
   an instrument file whose fields Claude set by hand (a `midi_program`, rigs) is.

## Traps

- **Persistent loaders.** `SoundLoader`, `NoteSetLoader` and `InstrumentLoader` override `LoaderPackage.persists()`, so
  they and their builders stay alive for runtime on-demand loads. Every other loader still releases itself after
  bootstrap, so an instrument loaded while the game runs can only name rigs and clips already loaded.
- **No samples at bootstrap.** `SoundLoader.scan` never queues a sound under `sounds/notes/`. A note set's samples
  exist in the palette only after `prepare`; `noteOn` on an instrument whose samples have not landed asks for them and
  returns -1.
- **Gain never above one.** OpenAL clamps a source's gain at 1, so a region's `volume` is at most 0 dB. The converter
  brings every sample to full scale and takes that gain back out of its region's volume, so it plays exactly as the
  SFZ sets it.
- **The envelope runs per frame.** `SamplerManager.update` sets each voice's gain once a frame; OpenAL Soft smooths
  the steps. Offline mixing (render, video export) must follow `NoteEnvelopeUtility` sample by sample, never re-derive
  the curve.
- **Voices are shared.** Notes take voices from `SoundManager`'s 64-voice pool, which steals the oldest voice that does
  not loop; a released note's voice can be taken. `SAMPLER_NOTE_COUNT` (32) bounds sounding notes; an instrument's
  `polyphony` bounds its held notes.
- **The recorder hears a voice's start only.** `DevRecorderSoundSystem` records the volume and pitch a voice began
  with; envelope and pitch changes made afterwards are not captured.
- **The launcher.** `LauncherBootstrapAssembly` creates `AnimationPipeline` because `InstrumentBuilder` resolves rig
  clips; the launcher's asset manifest queues no clip, so an instrument the launcher loads must have no rigs, or its
  rigs and clips must be on the manifest.
- **Display text.** The editor's font has no em dash; never put one in text the editor shows.
- **Songs play at the played song's tempo.** A nested song's own tempo map is ignored inside another; only the outer
  song's `SongTempoStruct` turns ticks into time.
- **Notes are kept sorted.** `SongArpgUtility.readNotes` and `SongMidiUtility.read` sort through
  `SongArpgUtility.sortNotes`; playback walks forward from `nextNote` and relies on it. Anything that builds a
  `SongClipStruct` by hand sorts its notes first.
- **Pan is not live.** Lane `pan` is read, validated and written to MIDI, but `SoundManager` has no pan for an
  unplaced voice, so live playback is centred until the sound pipeline gains one.
- **The MIDI index loads every instrument.** The first `getInstrumentIDFromMidiProgram` or `getPercussionInstrumentID`
  loads all of `assets/instruments/`, so every instrument's rig clips must be loaded by then (see the launcher trap).
- **No percussion instrument yet.** None is marked `percussion`, so a MIDI file's channel 10 plays on the default
  instrument, and `Test/Scale`'s drum line is sound clips. Converting a VCSL drum kit (with `--percussion`) fixes both.
- **Schema rules for songs** are editor vocabulary added for this pipeline: `record` (the notes), `ascending_from`
  (tempo ticks), `one_of` (`beat_unit`), `index_into` (a clip's `lane`) and `acyclic` (a song clip's `song`), built in
  `InfoSchemaBuilder.parseRule`/`verifyEntryRules` and checked in `InfoValidationBranch`.
