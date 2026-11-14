package application.runtime.menueventsmanager.menus.music;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.musicpipeline.performance.PerformanceInstance;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongCursorStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongHandle;
import application.bootstrap.musicpipeline.songmanager.SongManager;
import application.bootstrap.musicpipeline.util.SongFlattenUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SongbookBranch extends BranchPackage {

    /*
     * Handles the Songbook, opened over play mode by InstrumentSystem. It
     * lists the shipped songs by their display names under Songs and the
     * player's MIDI files, newest first, under My Songs, each with a notice
     * when it has none. Choosing a song lists its parts, the lanes that sound
     * notes; choosing a part plays it on the instrument in hand through the
     * performance and closes the menu. A player's song is read when it is
     * chosen and let go once its part plays or the list goes back, the play
     * holding it for itself while it sounds. Stop ends the song playing, Back
     * steps from the parts to the songs and from the songs out of the menu.
     */

    // Internal
    private MenuManager menuManager;
    private SongManager songManager;

    // Session
    private MenuInstance songbookMenu;
    private PerformanceInstance performance;

    // Songs — the shipped songs by name, then the player's, each row's index into them
    private ObjectArrayList<String> songNames;
    private ObjectArrayList<String> userSongNames;

    // Parts — the chosen song and its lanes that sound notes
    private SongHandle chosenSong;
    private IntArrayList partLanes;
    private ObjectArrayList<SongCursorStruct> cursors;

    // Base \\

    @Override
    protected void create() {

        // Songs
        this.songNames = new ObjectArrayList<>();
        this.userSongNames = new ObjectArrayList<>();

        // Parts
        this.partLanes = new IntArrayList();
        this.cursors = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.songManager = get(SongManager.class);
    }

    // Open / Close \\

    public void openMenu(PerformanceInstance performance, WindowInstance window) {

        if (songbookMenu != null)
            return;

        this.performance = performance;
        this.songbookMenu = menuManager.openMenu(RuntimeSetting.MENU_SONGBOOK, window);
        populateSongs();
    }

    public void closeMenu() {

        if (songbookMenu == null)
            return;

        releaseChosenSong();
        this.songbookMenu = menuManager.closeMenu(songbookMenu);
        this.performance = null;
    }

    // Back steps from a song's parts to the songs, and from the songs out of the menu
    public void goBack() {

        if (chosenSong == null) {
            closeMenu();
            return;
        }

        releaseChosenSong();
        populateSongs();
    }

    // Songs \\

    // A song picked by its row's index: the shipped songs first, then the player's
    public void chooseSong(String index) {

        int row = Integer.parseInt(index);
        SongHandle songHandle = row < songNames.size()
                ? songManager.getSongHandleFromSongName(songNames.get(row))
                : songManager.loadUserSong(userSongNames.get(row - songNames.size()));

        if (songHandle == null)
            return;

        this.chosenSong = songHandle;
        populateParts();
    }

    // A part picked by its lane plays on the instrument in hand
    public void choosePart(String lane) {

        int partLane = Integer.parseInt(lane);

        if (chosenSong.isUserSong())
            performance.playUserSong(chosenSong.getSongName(), partLane);
        else
            performance.playSong(chosenSong.getSongID(), partLane);

        closeMenu();
    }

    public void stopSong() {
        performance.stopSong();
    }

    // A player's song chosen is let go; a play of it holds it for itself
    private void releaseChosenSong() {

        if (chosenSong != null && chosenSong.isUserSong())
            songManager.releaseUserSong(chosenSong);

        this.chosenSong = null;
    }

    // Lists \\

    private void populateSongs() {

        menuManager.ejectAll(songbookMenu, RuntimeSetting.ENTRY_SONGBOOK_LIST);
        songbookMenu.getEntryPoint(RuntimeSetting.ENTRY_SONGBOOK_TITLE).setFontText(RuntimeSetting.SONGBOOK_TITLE);

        songNames.clear();
        songManager.collectSongNames(songNames);
        userSongNames.clear();
        userSongNames.addAll(songManager.getUserSongNames());

        injectSection(RuntimeSetting.SONGBOOK_SECTION_SONGS);

        if (songNames.isEmpty())
            injectEmptyNotice(RuntimeSetting.SONGBOOK_EMPTY_SONGS);

        for (int i = 0; i < songNames.size(); i++)
            injectRow(RuntimeSetting.MENU_SONGBOOK_SONG_ROW, RuntimeSetting.ELEMENT_SONGBOOK_SONG_ROW_LABEL, i,
                    songManager.getSongHandleFromSongName(songNames.get(i)).getDisplayName());

        injectSection(RuntimeSetting.SONGBOOK_SECTION_USER_SONGS);

        if (userSongNames.isEmpty())
            injectEmptyNotice(RuntimeSetting.SONGBOOK_EMPTY_USER_SONGS);

        for (int i = 0; i < userSongNames.size(); i++)
            injectRow(RuntimeSetting.MENU_SONGBOOK_SONG_ROW, RuntimeSetting.ELEMENT_SONGBOOK_SONG_ROW_LABEL,
                    songNames.size() + i, userSongNames.get(i));
    }

    private void populateParts() {

        SongData songData = chosenSong.getSongData();

        menuManager.ejectAll(songbookMenu, RuntimeSetting.ENTRY_SONGBOOK_LIST);
        songbookMenu.getEntryPoint(RuntimeSetting.ENTRY_SONGBOOK_TITLE).setFontText(chosenSong.getDisplayName());
        injectSection(RuntimeSetting.SONGBOOK_SECTION_PARTS);
        collectPartLanes(songData);

        if (partLanes.isEmpty())
            injectEmptyNotice(RuntimeSetting.SONGBOOK_EMPTY_PARTS);

        for (int i = 0; i < partLanes.size(); i++) {

            int lane = partLanes.getInt(i);
            String laneName = songData.getLane(lane).getName();

            injectRow(RuntimeSetting.MENU_SONGBOOK_PART_ROW, RuntimeSetting.ELEMENT_SONGBOOK_PART_ROW_LABEL, lane,
                    laneName.isEmpty() ? String.format(RuntimeSetting.SONGBOOK_PART_FORMAT, lane + 1) : laneName);
        }
    }

    // Every lane of the song that sounds a note, nested songs laid inside it counted on the lane they lie on
    private void collectPartLanes(SongData songData) {

        int cursorCount = SongFlattenUtility.flatten(songData, EngineSetting.SONG_LANE_ALL, cursors);

        partLanes.clear();

        for (int i = 0; i < cursorCount; i++) {

            SongCursorStruct cursor = cursors.get(i);

            if (cursor.getKind() == SongClipKind.NOTES && soundsNote(cursor) && !partLanes.contains(cursor.getLane()))
                partLanes.add(cursor.getLane());
        }

        partLanes.sort(null);
    }

    private boolean soundsNote(SongCursorStruct cursor) {

        for (int note = 0; note < cursor.getClip().getNoteCount(); note++)
            if (cursor.playsNote(note))
                return true;

        return false;
    }

    // Rows \\

    private void injectSection(String title) {
        menuManager.inject(songbookMenu, RuntimeSetting.ENTRY_SONGBOOK_LIST, RuntimeSetting.MENU_SONGBOOK_SECTION,
                section -> section.setFontText(title));
    }

    private void injectEmptyNotice(String text) {
        menuManager.inject(songbookMenu, RuntimeSetting.ENTRY_SONGBOOK_LIST, RuntimeSetting.MENU_SONGBOOK_EMPTY_NOTICE,
                notice -> notice.setFontText(text));
    }

    private void injectRow(String template, String labelID, int index, String text) {
        menuManager.inject(songbookMenu, RuntimeSetting.ENTRY_SONGBOOK_LIST, template,
                row -> {
                    row.setActionArgOverride(Integer.toString(index));
                    ElementInstance label = row.findChildById(labelID);
                    if (label != null)
                        label.setFontText(text);
                });
    }

    // Accessible \\

    public boolean isOpen() {
        return songbookMenu != null;
    }
}
