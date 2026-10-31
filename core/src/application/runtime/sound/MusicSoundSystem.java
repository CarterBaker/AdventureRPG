package application.runtime.sound;

import java.util.SplittableRandom;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.soundpipeline.soundcue.SoundLoopInstance;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.runtime.RuntimeSetting;
import application.runtime.world.WorldSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;

public class MusicSoundSystem extends SystemPackage {

    /*
     * Plays this window's music. At the main menu — no player yet, the world
     * seen from one of its cameras, or a character being made — the menu's
     * theme is held as a loop. In the world a piece is played now and
     * then — the day's while the sun is up, the night's once it is down —
     * each followed by a silence of a random length before the next, so the
     * music comes and goes rather than running on. Only the window that is
     * heard plays music, unless it is pinned quiet; one that stops being
     * heard lets its theme fade and stops its piece, and starts again after a
     * short silence.
     */

    // Internal
    private SoundManager soundManager;
    private SoundCueManager soundCueManager;
    private PlayerManager playerManager;
    private WorldSystem worldSystem;
    private SoundListenerSystem soundListenerSystem;

    // Menu
    private SoundLoopInstance menuTheme;

    // Pieces
    private int dayCueID;
    private int nightCueID;
    private int pieceVoice;
    private float silenceSeconds;
    private SplittableRandom random;

    // Base \\

    @Override
    protected void create() {

        // Pieces
        this.pieceVoice = EngineSetting.INDEX_NOT_FOUND;
        this.silenceSeconds = RuntimeSetting.SOUND_MUSIC_FIRST_GAP_SECONDS;
        this.random = new SplittableRandom(RuntimeSetting.SOUND_MUSIC_RANDOM_SEED);
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.soundCueManager = get(SoundCueManager.class);
        this.playerManager = get(PlayerManager.class);
        this.worldSystem = get(WorldSystem.class);
        this.soundListenerSystem = get(SoundListenerSystem.class);
    }

    @Override
    protected void awake() {

        // Menu
        this.menuTheme = create(SoundLoopInstance.class);
        menuTheme.constructor(soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_MUSIC_MENU));

        // Pieces
        this.dayCueID = soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_MUSIC_DAY);
        this.nightCueID = soundCueManager.getCueIDFromCueName(RuntimeSetting.SOUND_CUE_MUSIC_NIGHT);
    }

    // Update \\

    @Override
    protected void update() {

        int windowID = context.getWindow().getWindowID();
        boolean heard = soundListenerSystem.isAmbientHere();
        boolean inWorld = playerManager.hasPlayerForWindow(windowID)
                && !playerManager.isMenuViewForWindow(windowID)
                && !playerManager.isCharacterPreview(windowID);

        menuTheme.hold(heard && !inWorld ? 1f : 0f);

        if (heard && inWorld)
            playPieces();
        else
            stopPiece();
    }

    // A piece once the silence after the last one has run out, the day's or the night's by the hour
    private void playPieces() {

        if (soundManager.isVoicePlaying(pieceVoice) || soundManager.isVoicePaused(pieceVoice))
            return;

        this.silenceSeconds -= internal.getDeltaTime();

        if (silenceSeconds > 0f)
            return;

        this.pieceVoice = soundCueManager.playCue(isDay() ? dayCueID : nightCueID);
        this.silenceSeconds = RuntimeSetting.SOUND_MUSIC_GAP_SECONDS_MIN + (float) random.nextDouble()
                * (RuntimeSetting.SOUND_MUSIC_GAP_SECONDS_MAX - RuntimeSetting.SOUND_MUSIC_GAP_SECONDS_MIN);
    }

    private void stopPiece() {

        soundManager.stopVoice(pieceVoice);
        this.pieceVoice = EngineSetting.INDEX_NOT_FOUND;
        this.silenceSeconds = RuntimeSetting.SOUND_MUSIC_FIRST_GAP_SECONDS;
    }

    private boolean isDay() {

        GridInstance grid = worldSystem.getGridInstance();

        return grid == null || grid.getClockInstance().getSolarElevation() > 0.0;
    }
}
