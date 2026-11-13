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
| Shared logic | `util/NoteUtility` (names, pitch ratios, gains, scales), `util/NoteScale`, `util/NoteEnvelopeUtility` (the one envelope curve), `util/SongArpgUtility` (the tempo, meter, clip and note codec), `util/SongFlattenUtility` (a song laid out flat), `util/SongMidiUtility` (MIDI read and write), `util/SamplerMixUtility` (the one offline sampler render), `sampler/SamplerPcmStruct` (a sample decoded for it) |
| Recorder music side | `editor/bootstrap/recorderpipeline/`: `recording/RecordingKind` (Video or Music), `RecordingLaneStruct` (a Music file's lane mix), `RecordingSourceKind`, `RecordingClipStruct` (sources, codec clip), `RecordingPlayingStruct`, `RecordingInstance` (kind, tempo, meter, key, lanes, grid, `findPlaying`); `recordermanager/RecordingSourceBranch` (binding, child cache, loops, worlds), `RecordingFileBranch` (Video, version 3), `RecordingMusicBranch` (Music file read, write, `toSong`), `RecordingRowBranch` (Properties rows), `RecordingRenderBranch` (Render), `RecordingMixBranch` (video export); `util/FfmpegUtility`; `editor/runtime/dev/recorder/DevRecorderSoundSystem` (live) |
| Piano Roll | `recorderpipeline/pianorollmanager/` (`PianoRollManager`, `PianoRollHistoryBranch`, `PianoRollTypingBranch`), `pianoroll/PianoRollDragKind`; tab `editor/runtime/pianoroll/` (`PianoRollContext`, `PianoRollSetting`, `panel/PianoRollSystem`); `menueventsmanager/menus/PianoRollBranch`; menu `assets/menus/editor/PianoRoll.arpg` (also the music row templates); keys `assets/shortcuts/PianoRoll.arpg` |
| Performance | `performance/PerformanceInstance` (an entity playing its held instrument: keys, octave, velocity, a song part, the PLAY stance); play mode `application/runtime/music/InstrumentSystem`; Songbook `menueventsmanager/menus/music/SongbookBranch`, menu `assets/menus/SongbookMenu.arpg` (Songbook, Playing panel, Notice) |
| Clips and items | `assets/animations/humanoid/instruments/Humanoid<Pipe/Flute/Harp/Lute/Fiddle/Cradle/Drum><Hold/Play>`, the tree's `perform` layer (`play` action, arms, hands and fingers); items `assets/items/instruments/Instruments.arpg`, meshes `assets/mesh/items/instruments/` |
| Constants | `EngineSetting` groups `// Music Notes`, `// Music Scales`, `// Music Note Sets`, `// Music Instruments`, `// Music Sampler`, `// Music Songs`, `// Music MIDI`, `// Music Playback`, `// Music Performance`; `SONG_USER_DIRECTORY` in `// Engine & Application` |
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

- **A recording's clip source:** `RecordingClipStruct` reads its source through `resolveSourceChannelMask`,
  `resolveSourceDuration` and the lookups `getTake`, `getChild`, `getSong`; nothing else branches on its kind.
  `RecordingSourceBranch.bindClip` is the one place a name is resolved: a nested recording through the child cache
  (`resolveChild`, read once, bound in turn, dropped with every recording reaching it by `forget` when one is saved), a
  song through `SongManager`, a notes clip to a one-lane `SongData` on the recording's tempo map (`layNotes`).
- **What plays a channel:** `RecordingInstance.findPlaying(channel, time, without, carried, playing)` takes the highest
  lane, then the later laid, walks into a nested recording at `toChildTime`, and lets the clip below take over when the
  nest plays nothing there; the transition's `without`/`carried` hold at every depth, and
  `RecordingTransitionBranch.resolve` descends into the nest the picture is drawn from.
- **Song time in a recording:** `toSongSeconds`, `resolveSongTickOffset` and `toSongTick` map a clip's source time to
  the song it sounds: a song from its own start, a notes clip from where it lies on the recording's tempo map.
- **Playing from a timeline:** `SongPlaybackInstance.follow(songData, volume)` then `followTo(tick)` each frame; the
  first tick after its samples land passes over every earlier note. `getHeldCount`/`getHeldNote` with
  `SamplerManager.getNoteVoice` let the recorder keep those voices out of a take.
- **Offline render:** `SamplerMixUtility.collectSamples` names every sample a cursor can sound (the caller decodes each
  once into a `SamplerPcmStruct`), `mixCursor` adds a cursor over a frame window; each stretch is computed from the
  notes alone, round robin turned by the note's index, a note let go at its end or when its pitch is struck again.
- **Music files:** a Music file is a `RecordingInstance` of `RecordingKind.MUSIC`, so the Timeline, the transport and
  live playback are the recorder's own. `RecorderManager.createRecording(name, kind)`, `openSong`, `saveRecording` and
  the rest pick the file by kind (`getRecordingFile(name, kind)`: Videos beside the recordings, Music files through
  `SongManager.resolveSongFile`). `RecordingMusicBranch.read` turns each song clip into a timeline clip where its ticks
  fall (a notes clip keeps its notes as its codec clip, a song clip its song, volume and transpose through
  `RecordingClipStruct.ofSongClip`, a sound clip becomes the bin's take of that sound via
  `RecordingSoundBranch.requireSoundTake`); `write` goes through `toSong(recording, false)` and
  `SongArpgUtility.writeSong`, notes clips normalised to offset 0 holding only the notes heard. Saving calls
  `SongManager.rebuildSong`, which reads the song again into the handle it already has, so every song laying it hears
  the change.
- **Codec clips:** a clip carrying `getSongClip()` (a notes clip, a Music file's song clip) is timed by the recording's
  tempo map where it lies (`resolveSongOrigin`), bound by `RecordingSourceBranch.layNotes` / `laySong` to a one-lane
  song, and is replaced whole by every edit through `RecorderManager.replaceSongClip` (rebind, sound revision, unsaved);
  never change one in place.
- **Tempo, meter, key, mixer, metronome:** `RecorderManager.setTempo` retimes every clip so it keeps its bars and beats,
  `setMeter`, `setKey`, the lane setters and `toggleMetronome` (`tickMetronome` clicks `RECORDER_METRONOME_SOUND` on
  every beat). `RecordingInstance.resolveLaneGain` is the one lane mix, read by `DevRecorderSoundSystem` and
  `RecordingMixBranch`. Rows: `RecordingRowBranch` behind `RecorderManager.buildRows`/`stepRow`/`cycleRow`/
  `setRowValue`.
- **Recording notes:** `record()` on a Music file runs the transport with no take (`isTaking()` false), laying a notes
  clip when none is selected; `PianoRollTypingBranch` writes typed notes into it.
- **Piano roll edits:** every edit in `PianoRollManager` loads the notes as five-int records (the selection carried),
  edits them, and `commit` sorts and writes a new codec clip, kept by `PianoRollHistoryBranch` for undo. Snap is laid on
  the song's ticks (`snapTick`), Scale Lock is `lockPitch`.
- **Export:** `RecorderManager.exportMidi` writes `SongMidiUtility.write(toSong(recording, true))` to
  `Documents/My Games/AdventureRPG/Songs/<Name>.mid`. `renderMusic` hands the same song to `RecordingRenderBranch`,
  which flattens it, decodes and mixes it on the General thread through `SamplerMixUtility` (lane pan by equal power,
  `RECORDER_RENDER_TAIL_SECONDS` past the end) and encodes it through `FfmpegUtility.encodeVorbis` to
  `assets/sounds/music/<Name>.ogg`; the main thread writes the `music` companion.
- **Loops and worlds:** `RecordingSourceBranch.findLoopLaying` (laying) and `findLoopIn` (saving) walk nested recordings
  by name and name the loop; `isWorldShared` refuses a nested picture from another world.

- **Play an instrument:** `create(PerformanceInstance.class)`, `canPlay(entity, instrumentID)` (the rig check),
  `begin`, then `press`/`release(key)`, `stepOctave`, `stepVelocity`, `playSong`/`playUserSong(name, lane)`,
  `stopSong`, `update(deltaTime)` every frame, `end`. `animate` raises `EntityAction.PLAY` through
  `EntityActionHandle.holdMove` with the hold or play clip; a held stance with a looping clip cycles it
  (`EntityActionHandle.isLooping`, `getPhase`).
- **Fit a part to an instrument:** `NoteUtility.resolveOctaveShift` (whole part) and `fitToRange` (each note), applied
  by `SongPlaybackInstance` only when it plays on an override instrument; `setPlace(world, place)` sends its notes
  through `SamplerManager.noteOnAt(..., volume, ...)`.
- **Play mode:** `InstrumentSystem` enters on `KeyBindings.PLAY_INSTRUMENT` while the window is in play, holds the
  player through `PlayerManager.setPerformingForWindow` (which `isPlayingForWindow` and `PauseMenuBranch.canPause`
  read), reads `KeyBindings.NOTES` and the other music bindings, and opens `SongbookBranch`. Rebinding conflicts are
  scoped by `SettingsBinding.sharesScope` (`RuntimeSetting.SETTINGS_SCOPE_*`).

## Converting an instrument (Claude-side)

1. The owner's SFZ and the WAVs it names must be in `.claude/assets/`. If they are only in `.claude/catalogue/`, send a
   fetch zip (`.claude/CLAUDE.md`) and ask the owner to commit and push.
2. Compile directly (`.claude/docs/Build.md`) and run, from the project root:

   ```
   python3 -I .claude/tools/SfzConvert.py "<.sfz under .claude/assets>" <Instrument> \
       --arpg "java -cp $S/out:<jars> lwjgl3.Lwjgl3LauncherArpg" --scratch $S/convert \
       [--display-name "<Name>"] [--midi-program <0..127>] [--percussion]
   ```

   It refuses a keyswitch patch, leaves out regions that play only on release (`trigger=release`), reads random
   round robin (`lorand`/`hirand`) as round-robin turns, prints the report (opcodes left out, crossfades split at their middle, envelope values
   most regions give, loops, decoded bytes) and writes the samples, companions, note set and, when none exists, the
   instrument. Re-running replaces the samples and note set and keeps the instrument file.
3. `verify` every file it wrote, boot the editor once (the builders are the final authority), and state each note
   set's decoded bytes. Deliver the files; the note sets and sounds are the owner's library and are never catalogued,
   an instrument file whose fields Claude set by hand (a `midi_program`, rigs) is.

## Traps

- **Action nodes never loop.** `AnimationTreeBuilder` refuses an action node whose clip loops; the `play` node's own
  clip is a still fallback (`HumanoidBlock`), and the instrument's looping clip plays as the move clip, cycled by the
  action's phase.
- **No sustain loops in these libraries.** VCSL and VSCO samples carry no loop points, so a held note plays out its
  recording and then stops; every converted note set reports `0 of N samples loop`.
- **A user song is held twice while its part plays.** `SongbookBranch` loads it on choosing (one hold) and lets it go
  on playing or going back; the play's own `playUserSong` holds it until the song stops.

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
- **Nested spans are never clamped in storage.** `getInPoint`/`getOutPoint`/`getLength` clamp to the source as it now
  runs; the file writes `getStoredInPoint`/`getStoredOutPoint`/`getStoredChannelMask`. A clip whose source is unbound
  keeps its stored span and plays nothing.
- **Recording file version 3.** Each clip writes `source` and its `take` index, `recording`, `song` or `notes` (the song
  codec's clip, lane 0, start 0); the recording writes `tempos`, `beats_per_bar`, `beat_unit`. Versions 1 and 2 read
  as takes at 120 bpm and are saved as 3 when opened.
- **Live song fades** set the volume of notes started after the fade moves; a note already sounding keeps its own.
- **Pan is not live.** Lane `pan` is read, validated and written to MIDI, but `SoundManager` has no pan for an
  unplaced voice, so live playback is centred until the sound pipeline gains one.
- **The MIDI index loads every instrument.** The first `getInstrumentIDFromMidiProgram` or `getPercussionInstrumentID`
  loads all of `assets/instruments/`, so every instrument's rig clips must be loaded by then (see the launcher trap).
- **No percussion instrument yet.** None is marked `percussion`, so a MIDI file's channel 10 plays on the default
  instrument, and `Test/Scale`'s drum line is sound clips. Converting a VCSL drum kit (with `--percussion`) fixes both.
- **Lifecycle names.** Managers and branches inherit `render()`, `release()`, `draw()` and the rest; a public method of
  that name overrides the engine's hook and runs every frame. Render is `renderMusic`.
- **Musical Typing claims keys.** While it is on, `ShortcutManager.claimKeys` makes every other shortcut on a note,
  octave or velocity chord give way in the Piano Roll's window; those four pressed shortcuts are `claimed` and run only
  then. Note-on is `ShortcutManager.isPressed`, note-off `isHeld` going false.
- **Lane mute and solo are live; pan is heard only in Render**, not in live playback (see Pan is not live).
- **Schema rules for songs** are editor vocabulary added for this pipeline: `record` (the notes), `ascending_from`
  (tempo ticks), `one_of` (`beat_unit`), `index_into` (a clip's `lane`) and `acyclic` (a song clip's `song`), built in
  `InfoSchemaBuilder.parseRule`/`verifyEntryRules` and checked in `InfoValidationBranch`.
