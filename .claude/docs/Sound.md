# Sound

How the sound pipeline (`SoundPipeline`) plays audio, where sound files and cues live, which system sounds which
moment, and how the recorder and the Sound Library use it. Read it before playing, loading or mixing any sound, adding
a gameplay sound, or touching the recorder's sound channel. `.claude/CLAUDE.md` still applies in full.

---

## Playing a moment from any system

Systems play **cues**, never sound files. A cue (`assets/soundcues/<name>.arpg`) names the moment; its data decides
which sounds it picks from and how. Get `SoundCueManager` in `get()`, resolve each cue name once in `awake()`, and
play by ID. Cue names are constants: `EngineSetting` (`// Sound Cue Names`) for the shared world, `RuntimeSetting`
for what a window plays itself.

```java
// Internal
private SoundCueManager soundCueManager;

// Cues
private int chopCueID;

@Override
protected void get() {
    this.soundCueManager = get(SoundCueManager.class);
}

@Override
protected void awake() {

    // Cues
    this.chopCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_CHOP);
}

// Placed in the world: fades with distance, heard from its side
soundCueManager.playCueAt(chopCueID, entity.getWorldHandle(), entity.getWorldPositionStruct());

// Heard as it is, wherever the camera stands (menus, music, a window's own moments)
soundCueManager.playCue(chopCueID);
```

- Each play picks one of the cue's sounds at random, never the last one while it has another, and strays from the
  cue's `volume` and `pitch` by up to `volumeVariance` and `pitchVariance`. A volume scale (`playCue(id, scale)`,
  `playCueAt(id, scale, ...)`) sets how hard the moment was: a fall, a blow, a speed.
- A cue ID of `INDEX_NOT_FOUND` plays nothing, so an optional cue (a menu or item action that names none) needs no
  check at the call.
- A cue's sounds resolve the first time it plays. A sound not in `assets/sounds/` yet is left out, and a cue with
  none plays nothing and logs it once: the game runs whole while sounds are still being gathered.
- A placed sound farther from the camera than its `maxDistance` is not begun, so distant footsteps never take a voice.
- `SoundLoopInstance` (`create(SoundLoopInstance.class)`, `constructor(cueID)`) holds a cue as a loop. Call
  `hold(intensity)` or `holdAt(intensity, world, place)` every frame with 0 to 1; it eases toward it over the cue's
  `fadeSeconds`, begins when heard, follows its place, and gives its voice back at 0. Never start a loop by hand.
- `SoundManager` is the layer below (raw sounds, voices, the listener). Use it only for what cues do not cover:
  `stopVoice`, `isVoicePlaying`, `getListenerDistance`, `pinHeardWindow`/`unpinHeardWindow`, the recorder and the
  Sound Library.

## Surfaces

A block (`sound` in `Blocks.arpg`) and a covering (`sound` in a covering file) name a **surface**: a folder
`assets/soundcues/surfaces/<surface>/` holding `Step`, `Dig`, `Break` and `Place`. `SoundCueManager.getSurfaceStruct`
resolves it once into a shared `SoundSurfaceStruct`; `BlockHandle.getSoundSurface()` and
`CoveringHandle.getSoundSurface()` return it, null when silent. Surfaces: `stone`, `dirt`, `sand`, `gravel`, `wood`,
`grass`, `leaves`, `glass`, `ice`. A new surface is a new folder with its four cues; nothing in code changes.

## Who sounds what

Bootstrap code plays for the shared world (no window owns those voices; the recorder takes them when they are placed in
its window's world). Runtime systems play what only their window hears.

| Moment | Where | Cues |
|---|---|---|
| Steps, strokes, climbing, jump, landing, splash | `MovementSoundBranch` (every entity, from `MovementManager.move`) | surface `Step`, `movement/*` |
| Swing, hit, guard, defeat, throw | `AttackBranch`, `DamageBranch`, `ThrowBranch` | `combat/*` |
| Projectile bounce | `ProjectileFlightBranch.bounce` | `combat/ProjectileBounce` |
| Block worked, broken, refused, placed | `BlockBranch` | surface `Dig`/`Break`/`Place`, `combat/Turned` |
| Leaf, chop, refused, fall, landing | `TreeChopBranch`, `TreeFallBranch` | `surfaces/leaves/Dig`, `trees/*`, `combat/Turned` |
| Pick up, set down (world and ship cargo) | `ItemBranch`, `VehicleCargoSystem` | `items/PickUp`, `items/SetDown` |
| Item actions (doors, cannons) | `ItemActionBranch`, from the action's `cue` | `furnishings/*`, `weapons/*` |
| Hull, wake, sails, rigging loops; hoist, set, anchor, helm, door, grounding | `VehicleSoundBranch` (per vehicle, `attach`/`detach` on spawn/despawn) | `ships/*` |
| Drafting table part placed or refused, launch | `ShipwrightEditBranch`, `ShipwrightManager` | `shipwright/*`, `ships/Launch` |
| Level, skill or trade step, profession (players only) | `ProgressionSoundBranch` | `progression/*` |
| Wind, trees, rain, thunder, gusts, sea, shore, birds, crickets, under water | `AmbienceSoundSystem` (`AmbienceLayer` loops) | `ambience/*` |
| Menu theme, day and night pieces | `MusicSoundSystem` | `music/*` |
| Heartbeat near death | `VitalitySystem` | `vitality/Heartbeat` |
| Defeat sting | `DefeatBranch` | `vitality/Defeat` |
| Equip, stow in the inventory | `InventoryDragBranch` | `items/Equip`, `items/Stow` |
| Menu open, close, click | `MenuManager.openMenu/closeMenu`, `ElementHitSystem` | a menu's `open_cue`, `close_cue`, `click_cue` |

- The window that is heard (`SoundListenerSystem.isHeardHere()`) places the listener: the window pinned with
  `SoundManager.pinHeardWindow`, else the one with focus, or any while none has it. Only it holds ambience, music and
  the heartbeat (`isAmbientHere()`), unless it is pinned quiet; others let them fade.
- Ambience reads the place without extra data: a biome with trees is wooded, one with ocean water is sea (a coast makes
  it shore), the camera's height above the sea surface sets how near the water is, and solar elevation sets day.
- To add a moment: write its cue file, add the name constant, resolve it in the owning class's `awake()` and play it
  at the one place the moment happens. Data that names a cue (menus, item actions) resolves it in its Builder.

## Files

- Every sound is Ogg Vorbis (`.ogg`, `SOUND_FILE_EXTENSION`), small enough to commit. `SoundLoader` refuses any other
  file under `assets/sounds/` but a companion `.arpg`, and the game will not start until it is converted. The path
  below `sounds/` without the extension is the sound's name. Generate new sounds straight to Ogg Vorbis at 48 kHz.
- Every sound is decoded once at load into 16-bit samples at no more than `SOUND_MAX_SAMPLE_RATE`, uploaded and let
  go; no CPU copy is kept. A sound with a `length` is decoded no further than it needs. A spatial sound, or one
  marked `mono`, is downmixed.
- A companion `.arpg` beside the file (schema `Sounds.arpg`) sets `category` (`music`, `effect`, `ambient`,
  `interface`, `voice`), `volume`, `pitch`, `loop`, `spatial`, `mono`, `length` (seconds kept from the start),
  `loopCrossfade` (seconds of tail faded into the head of a looping sound; 0 for a file already seamless),
  `referenceDistance`, `maxDistance` and `rolloff`. A companion may stand before its sound exists, waiting for it.
- A cue (schema `SoundCues.arpg`) holds `sounds` (one or more sound names), `volume`, `volumeVariance`, `pitch`,
  `pitchVariance` and `fadeSeconds` (for loops).
- Volumes are the player's settings (`Settings.masterVolume` and one per category), shown on the settings menu's Audio
  tab and read by `SoundVoiceBranch` whenever they change.

## The owner's library

Library sounds the game uses are copied from `.claude/catalogue/` into `assets/sounds/` under their engine names,
beside the companions already in place, and converted to Ogg Vorbis before the game runs. To use another library
sound: name its library path and engine name for the owner, write its companion (`length`, `mono` and
`loopCrossfade` keep long or surround recordings small and seamless) and name it in a cue. Library sounds are the
owner's: never catalogue them, only their companions and cues.

## Code map

- `application/bootstrap/soundpipeline/`: `SoundManager` (device, palette, voices, listener), `SoundLoader`,
  `SoundBuilder`, `SoundVoiceBranch`, `SoundDecodeUtility`; `SoundCueManager`, `SoundCueLoader`, `SoundCueBuilder`,
  `SoundCueHandle`/`SoundCueData`, `SoundLoopInstance`, `SoundSurfaceStruct`.
- `application/runtime/sound/`: `SoundListenerSystem`, `AmbienceSoundSystem`, `AmbienceLayer`, `MusicSoundSystem`.
- `engine/audio/Audio` is the backend interface (`EngineContext.audio`), implemented by `engine/lwjgl3/Lwjgl3Audio`
  over OpenAL and stb_vorbis. Never import `org.lwjgl` outside `engine/lwjgl3`. OpenAL constants are in the
  `// OpenAL Constants` group of `EngineSetting`.

## Recorder and Sound Library

- `RecorderChannel.SOUND`: a take records every sound its Dev window's systems start, every placed sound the shared
  world starts in that window's world and every unplaced one it starts, while it records (`DevRecorderSoundSystem`),
  into the take's `RecordingSoundTrackStruct`, with how loud and from which side it reached the camera. Sounds already
  playing when the take begins are captured too: a loop (ambience, rain, wind) from the take's start, any other (a
  music piece) at a negative time, so it is heard from as far in as it had played. Unlike every other channel, sounds
  add up: every unmuted clip holding the channel sounds together.
- While a session runs, `RecorderManager.holdSound` pins its Dev window as the one heard, so clicking into the
  Recorder tab never freezes the listener or fades the ambience a take records; while the timeline plays the sound
  channel the pin is quiet, so live ambience, music and heartbeat never double what the take holds.
- The Keys pane shows the selected clip's take's Sounds layer (`RecorderKeysSystem.injectSounds`): its own row marks
  every sound, and opened it holds a row per sound name with a bar for every time it is heard. A sound is selected by
  clicking its bar (or its row's name for the first one), dragged along its row to retime it, and the inspector
  (`RecorderPanelSystem.refreshSound`, also in the Clip panel) types its volume, pitch and length, moves the playhead
  to it, copies it to the playhead, replaces it with the Sound Library's selection, or removes it or every one of it;
  Delete removes the selected sound after selected keys. A sound from the Sound Library dropped on the Keys rows, or
  added with "Add the Sound Library's selection at the playhead", joins the selected clip's take. Edits live in
  `RecorderManager` (`// Sound Layer`) over `RecordingSoundBranch`; each moves `getSoundRevision()`, which stops the
  timeline's voices so they start again as edited.
- A sound dropped from the Sound Library (or added at the playhead) becomes a take holding that sound alone
  (`RecordingTakeStruct.isSoundOf`), reused for every later drop of the same sound, laid as an ordinary clip.
- Exports mix every sound clip offline (`RecordingMixBranch`) into 48 kHz 16-bit stereo PCM, written beside each
  video frame: a PCM track in MP4 and MOV, a second OpenDML stream in AVI.
- The Sound Library tab (`SoundLibraryContext`, state in `SoundLibraryManager`) lists every loaded sound by folder,
  previews the selected one (play, pause, stop, rewind, loop, scrub), and drags rows onto any Recorder tab's lanes
  through `RecorderBranch.dragSound`.
