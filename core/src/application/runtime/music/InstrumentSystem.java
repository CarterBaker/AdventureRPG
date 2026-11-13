package application.runtime.music;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.musicpipeline.performance.PerformanceInstance;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import application.runtime.menueventsmanager.menus.music.SongbookBranch;
import engine.input.InputNameUtility;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;

public class InstrumentSystem extends SystemPackage {

    /*
     * Play mode in this window. With an instrument in the main hand, the play
     * instrument binding takes it up through the window's PerformanceInstance,
     * unless the player's race has no clips for it, which a notice says. While
     * it plays, PlayerManager holds the player still and out of play, so no
     * other binding reaches the world, and this system reads its own: the
     * note keys sound while held, the octave and loudness bindings step, the
     * songbook binding opens SongbookBranch's menu, which the songbook or
     * leave binding closes again, and the leave or play instrument binding
     * puts the instrument down. The bindings that opened it are
     * ignored the frame it opens. Another menu taking the window, the
     * instrument leaving the hand or the player going leaves play mode too,
     * and leaving stops any song. A small panel names the instrument, the
     * octave the keys reach and their loudness, rewritten only when one
     * changes.
     */

    // Internal
    private PlayerManager playerManager;
    private InputManager inputManager;
    private MenuManager menuManager;
    private SongbookBranch songbookBranch;

    // Performance
    private PerformanceInstance performance;
    private short instrumentID;

    // Menus
    private MenuInstance playingMenu;
    private MenuInstance noticeMenu;
    private float noticeCountdown;

    // Labels — what the panel last showed
    private int shownOctavePitch;
    private int shownVelocity;
    private boolean shownSong;

    // Base \\

    @Override
    protected void create() {

        // Performance
        this.performance = create(PerformanceInstance.class);
        this.instrumentID = EngineSetting.INSTRUMENT_NONE;
    }

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputManager.class);
        this.menuManager = get(MenuManager.class);
        this.songbookBranch = get(SongbookBranch.class);
    }

    @Override
    protected void dispose() {
        leave();
    }

    // Update \\

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        updateNotice();

        if (!performance.isPerforming()) {

            if (inputManager.bindingClicked(KeyBindings.PLAY_INSTRUMENT, window)
                    && playerManager.isPlayingForWindow(window.getWindowID()))
                enter(window);

            return;
        }

        if (!isStillPlayable(window)) {
            leave();
            return;
        }

        if (songbookBranch.isOpen())
            updateSongbook(window);
        else if (updateBindings(window))
            return;

        performance.update(internal.getDeltaTime());
        refreshLabels();
    }

    // The bindings of play mode, true when one put the instrument down
    private boolean updateBindings(WindowInstance window) {

        if (inputManager.bindingClicked(KeyBindings.LEAVE_INSTRUMENT, window)
                || inputManager.bindingClicked(KeyBindings.PLAY_INSTRUMENT, window)) {
            leave();
            return true;
        }

        if (inputManager.bindingClicked(KeyBindings.SONGBOOK, window)) {
            performance.releaseKeys();
            songbookBranch.openMenu(performance, window);
            return false;
        }

        for (int key = 0; key < KeyBindings.NOTES.length; key++)
            updateKey(key, inputManager.bindingHeld(KeyBindings.NOTES[key], window));

        if (inputManager.bindingClicked(KeyBindings.OCTAVE_DOWN, window))
            performance.stepOctave(-1);

        if (inputManager.bindingClicked(KeyBindings.OCTAVE_UP, window))
            performance.stepOctave(1);

        if (inputManager.bindingClicked(KeyBindings.SOFTER, window))
            performance.stepVelocity(-1);

        if (inputManager.bindingClicked(KeyBindings.LOUDER, window))
            performance.stepVelocity(1);

        return false;
    }

    // The leave and songbook bindings close the Songbook over play mode
    private void updateSongbook(WindowInstance window) {
        if (inputManager.bindingClicked(KeyBindings.LEAVE_INSTRUMENT, window)
                || inputManager.bindingClicked(KeyBindings.SONGBOOK, window))
            songbookBranch.closeMenu();
    }

    // A key held sounds its note until let go; one held before its samples landed sounds once they have
    private void updateKey(int key, boolean held) {

        if (held == performance.isPressed(key))
            return;

        if (held)
            performance.press(key);
        else
            performance.release(key);
    }

    // Enter / Leave \\

    private void enter(WindowInstance window) {

        EntityInstance player = playerManager.getPlayerForWindow(window.getWindowID());
        short heldInstrumentID = resolveHeldInstrument(player);

        if (heldInstrumentID == EngineSetting.INSTRUMENT_NONE)
            return;

        if (!performance.begin(player, heldInstrumentID)) {
            showNotice(RuntimeSetting.PLAYING_NOTICE_RACE, window);
            return;
        }

        this.instrumentID = heldInstrumentID;
        playerManager.setPerformingForWindow(window.getWindowID(), true);
        this.playingMenu = menuManager.openMenu(RuntimeSetting.MENU_PLAYING, window);
        playingMenu.getEntryPoint(RuntimeSetting.ENTRY_PLAYING_TITLE)
                .setFontText(performance.getInstrumentHandle().getDisplayName());
        this.shownOctavePitch = EngineSetting.INDEX_NOT_FOUND;
    }

    private void leave() {

        if (!performance.isPerforming())
            return;

        WindowInstance window = context.getWindow();

        songbookBranch.closeMenu();
        performance.end();
        playerManager.setPerformingForWindow(window.getWindowID(), false);
        this.playingMenu = menuManager.closeMenu(playingMenu);
        this.instrumentID = EngineSetting.INSTRUMENT_NONE;
    }

    // Play mode holds while the player stands with the same instrument in hand and no other menu takes the window
    private boolean isStillPlayable(WindowInstance window) {

        int windowID = window.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return false;

        if (!songbookBranch.isOpen() && window.getMenuListHandle().isInputLocked())
            return false;

        return resolveHeldInstrument(playerManager.getPlayerForWindow(windowID)) == instrumentID;
    }

    // The instrument the main hand holds, INSTRUMENT_NONE when it holds none
    private short resolveHeldInstrument(EntityInstance player) {

        ItemInstance held = player.getInventoryHandle().getMainHand();

        return held != null && held.getItemDefinitionHandle().hasInstrument()
                ? held.getItemDefinitionHandle().getInstrumentID()
                : EngineSetting.INSTRUMENT_NONE;
    }

    // Notice \\

    private void showNotice(String text, WindowInstance window) {

        if (noticeMenu == null)
            this.noticeMenu = menuManager.openMenu(RuntimeSetting.MENU_PLAYING_NOTICE, window);

        noticeMenu.getEntryPoint(RuntimeSetting.ENTRY_PLAYING_NOTICE).setFontText(text);
        this.noticeCountdown = RuntimeSetting.PLAYING_NOTICE_SECONDS;
    }

    private void updateNotice() {

        if (noticeMenu == null)
            return;

        this.noticeCountdown -= internal.getDeltaTime();

        if (noticeCountdown <= 0f)
            this.noticeMenu = menuManager.closeMenu(noticeMenu);
    }

    // Labels \\

    // The panel's octave, loudness and hints rewritten when the keys move or a song starts or ends
    private void refreshLabels() {

        int octavePitch = performance.getBaseOctavePitch();
        int velocity = performance.getVelocity();
        boolean song = performance.isSongPlaying();

        if (octavePitch == shownOctavePitch && velocity == shownVelocity && song == shownSong)
            return;

        this.shownOctavePitch = octavePitch;
        this.shownVelocity = velocity;
        this.shownSong = song;

        playingMenu.getEntryPoint(RuntimeSetting.ENTRY_PLAYING_DETAIL).setFontText(String.format(
                RuntimeSetting.PLAYING_DETAIL_FORMAT,
                NoteUtility.toNoteName(octavePitch),
                NoteUtility.toNoteName(octavePitch + EngineSetting.NOTE_SEMITONES_PER_OCTAVE),
                velocity));
        playingMenu.getEntryPoint(RuntimeSetting.ENTRY_PLAYING_HINT).setFontText(String.format(
                song ? RuntimeSetting.PLAYING_SONG_FORMAT : RuntimeSetting.PLAYING_HINT_FORMAT,
                InputNameUtility.getName(KeyBindings.SONGBOOK),
                InputNameUtility.getName(KeyBindings.LEAVE_INSTRUMENT)));
    }
}
