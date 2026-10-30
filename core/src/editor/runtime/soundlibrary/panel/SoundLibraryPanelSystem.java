package editor.runtime.soundlibrary.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.menupipeline.util.DimensionValueStruct;
import application.bootstrap.menupipeline.util.DimensionVector2Struct;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.soundlibrarypipeline.soundlibrarymanager.SoundLibraryManager;
import editor.runtime.EditorSetting;
import editor.runtime.soundlibrary.SoundLibrarySetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SoundLibraryPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the sound library.
     * When the library's revision moves it refills the list — a head for
     * every folder with how many sounds it holds, pressed to open or close
     * it, and beneath an opened one a row for each sound with its length and
     * a button to sample it, the selected sound marked — the selected sound's
     * details, the status line and whether the preview loops. The time
     * played and the scrub bar's playhead follow the preview a few times a
     * second from positions built once, so playing allocates nothing but the
     * time's text.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private SoundLibraryManager soundLibraryManager;

    // Menus
    private MenuInstance libraryMenu;
    private ElementInstance statusLabel;
    private ElementInstance timeLabel;
    private ElementInstance loopLabel;
    private ElementInstance listTitle;
    private ElementInstance playhead;

    // Scrub Bar
    private DimensionVector2Struct[] scrubPositions;
    private int shownPlayheadStep;

    // State
    private int shownRevision;
    private float refreshElapsed;

    // Base \\

    @Override
    protected void create() {

        // Scrub Bar
        this.scrubPositions = new DimensionVector2Struct[SoundLibrarySetting.SCRUB_STEPS + 1];

        for (int i = 0; i < scrubPositions.length; i++)
            scrubPositions[i] = new DimensionVector2Struct(
                    DimensionValueStruct.ofPercent(SoundLibrarySetting.SCRUB_PERCENT * i
                            / SoundLibrarySetting.SCRUB_STEPS),
                    DimensionValueStruct.ofAbsolute(0f));

        this.shownPlayheadStep = EngineSetting.INDEX_NOT_FOUND;

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.soundLibraryManager = get(SoundLibraryManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.libraryMenu = menuManager.openMenu(EditorSetting.SOUND_LIBRARY_MENU, window);

        this.statusLabel = requireEntryPoint(SoundLibrarySetting.ENTRY_STATUS);
        this.timeLabel = requireEntryPoint(SoundLibrarySetting.ENTRY_TIME);
        this.loopLabel = requireEntryPoint(SoundLibrarySetting.ENTRY_LOOP);
        this.listTitle = requireEntryPoint(SoundLibrarySetting.ENTRY_LIST_TITLE);
        this.playhead = requireEntryPoint(SoundLibrarySetting.ENTRY_PLAYHEAD);

        requireEntryPoint(SoundLibrarySetting.ENTRY_SOUNDS);
        requireEntryPoint(SoundLibrarySetting.ENTRY_DETAILS);
        requireEntryPoint(SoundLibrarySetting.ENTRY_SCRUB_TRACK);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = libraryMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Sound library menu '" + EditorSetting.SOUND_LIBRARY_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (soundLibraryManager.getRevision() != shownRevision) {
            this.shownRevision = soundLibraryManager.getRevision();
            refreshAll();
        }

        this.refreshElapsed += internal.getDeltaTime();

        if (refreshElapsed < SoundLibrarySetting.REFRESH_SECONDS)
            return;

        this.refreshElapsed = 0f;
        refreshTime();
        movePlayhead();
    }

    private void refreshAll() {
        statusLabel.setFontText(soundLibraryManager.getStatusText());
        loopLabel.setFontText(soundLibraryManager.isLooping()
                ? SoundLibrarySetting.TEXT_LOOP_ON
                : SoundLibrarySetting.TEXT_LOOP_OFF);
        refreshSounds();
        refreshDetails();
        refreshTime();
        movePlayhead();
    }

    // Sounds \\

    // A head for every folder in name order, and beneath each opened one a row for every sound it holds
    private void refreshSounds() {

        menuManager.ejectAll(libraryMenu, SoundLibrarySetting.ENTRY_SOUNDS);

        ObjectArrayList<SoundHandle> sounds = soundLibraryManager.getSounds();

        listTitle.setFontText(String.format(SoundLibrarySetting.FORMAT_LIST_TITLE, sounds.size()));

        if (sounds.isEmpty()) {
            injectText(SoundLibrarySetting.ENTRY_SOUNDS, SoundLibrarySetting.TEXT_NO_SOUNDS);
            return;
        }

        String folder = null;
        boolean opened = false;

        for (int i = 0; i < sounds.size(); i++) {

            SoundHandle soundHandle = sounds.get(i);
            String soundFolder = soundLibraryManager.resolveFolder(soundHandle);

            if (!soundFolder.equals(folder)) {
                folder = soundFolder;
                opened = soundLibraryManager.isFolderOpened(folder);
                injectFolder(folder, opened, countFolder(sounds, i, folder));
            }

            if (opened)
                injectSound(soundHandle);
        }
    }

    // How many sounds from the index on lie in the folder, the list being in name order
    private int countFolder(ObjectArrayList<SoundHandle> sounds, int from, String folder) {

        int count = 0;

        for (int i = from; i < sounds.size() && soundLibraryManager.resolveFolder(sounds.get(i)).equals(folder); i++)
            count++;

        return count;
    }

    private void injectFolder(String folder, boolean opened, int count) {

        ElementInstance head = menuManager.inject(
                libraryMenu, SoundLibrarySetting.ENTRY_SOUNDS, SoundLibrarySetting.TEMPLATE_FOLDER_ROW);

        head.setActionArgOverride(folder);
        head.findChildById(SoundLibrarySetting.ELEMENT_ROW_LABEL).setFontText(
                (opened ? SoundLibrarySetting.TEXT_FOLDER_OPENED : SoundLibrarySetting.TEXT_FOLDER_CLOSED)
                        + (folder.isEmpty() ? SoundLibrarySetting.TEXT_ROOT_FOLDER : folder));
        head.findChildById(SoundLibrarySetting.ELEMENT_ROW_VALUE).setFontText(Integer.toString(count));
    }

    // A row picked up to drag its sound onto a timeline, with its length and a button to sample it
    private void injectSound(SoundHandle soundHandle) {

        String soundName = soundHandle.getSoundName();
        ElementInstance row = menuManager.inject(
                libraryMenu,
                SoundLibrarySetting.ENTRY_SOUNDS,
                soundHandle.getSoundID() == soundLibraryManager.getSelectedSoundID()
                        ? SoundLibrarySetting.TEMPLATE_SOUND_ROW_SELECTED
                        : SoundLibrarySetting.TEMPLATE_SOUND_ROW);

        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_SELECT).setOnDragArgOverride(soundName);
        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_PLAY).setActionArgOverride(soundName);
        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_LABEL).setFontText(
                soundName.substring(soundName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1));
        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_VALUE).setFontText(
                formatTime(soundHandle.getPlayedLength()));
    }

    // Details \\

    // Everything the selected sound plays with, as its companion file sets it
    private void refreshDetails() {

        menuManager.ejectAll(libraryMenu, SoundLibrarySetting.ENTRY_DETAILS);

        if (!soundLibraryManager.hasSelection()) {
            injectText(SoundLibrarySetting.ENTRY_DETAILS, SoundLibrarySetting.TEXT_NO_SELECTION);
            injectText(SoundLibrarySetting.ENTRY_DETAILS, SoundLibrarySetting.TEXT_DRAG_HINT);
            return;
        }

        SoundHandle soundHandle = soundLibraryManager.getSelectedSound();
        String folder = soundLibraryManager.resolveFolder(soundHandle);

        injectHeader(SoundLibrarySetting.ENTRY_DETAILS, soundHandle.getSoundName());
        injectDetail(SoundLibrarySetting.TEXT_FOLDER,
                folder.isEmpty() ? SoundLibrarySetting.TEXT_ROOT_FOLDER : folder);
        injectDetail(SoundLibrarySetting.TEXT_LENGTH, formatTime(soundHandle.getPlayedLength()));
        injectDetail(SoundLibrarySetting.TEXT_CATEGORY, ArpgUtility.toEnumName(soundHandle.getCategory()));
        injectDetail(SoundLibrarySetting.TEXT_FORMAT, String.format(SoundLibrarySetting.FORMAT_FORMAT,
                soundHandle.getChannels() == EngineSetting.SOUND_MONO_CHANNELS
                        ? SoundLibrarySetting.TEXT_MONO
                        : SoundLibrarySetting.TEXT_STEREO,
                soundHandle.getSampleRate()));
        injectDetail(SoundLibrarySetting.TEXT_VOLUME,
                String.format(SoundLibrarySetting.FORMAT_DECIMAL, soundHandle.getVolume()));
        injectDetail(SoundLibrarySetting.TEXT_PITCH,
                String.format(SoundLibrarySetting.FORMAT_DECIMAL, soundHandle.getPitch()));
        injectDetail(SoundLibrarySetting.TEXT_LOOPS, toYesNo(soundHandle.isLoop()));
        injectDetail(SoundLibrarySetting.TEXT_PLACED, toYesNo(soundHandle.isSpatial()));

        if (soundHandle.isSpatial()) {
            injectDetail(SoundLibrarySetting.TEXT_HEARD_WITHIN,
                    String.format(SoundLibrarySetting.FORMAT_DISTANCE, soundHandle.getReferenceDistance()));
            injectDetail(SoundLibrarySetting.TEXT_FADES_TO,
                    String.format(SoundLibrarySetting.FORMAT_DISTANCE, soundHandle.getMaxDistance()));
        }

        injectText(SoundLibrarySetting.ENTRY_DETAILS, SoundLibrarySetting.TEXT_DRAG_HINT);
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                libraryMenu, SoundLibrarySetting.ENTRY_DETAILS, SoundLibrarySetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(SoundLibrarySetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Playhead \\

    private void refreshTime() {

        if (!soundLibraryManager.hasSelection()) {
            timeLabel.setFontText(SoundLibrarySetting.TEXT_EMPTY);
            return;
        }

        timeLabel.setFontText(String.format(
                SoundLibrarySetting.FORMAT_PLAYHEAD,
                formatTime(soundLibraryManager.getPlayhead()),
                formatTime(soundLibraryManager.getSelectedSound().getLength())));
    }

    // The scrub bar's playhead at the share of the selected sound played, moved only when its step changes
    private void movePlayhead() {

        float fraction = soundLibraryManager.hasSelection()
                ? soundLibraryManager.getPlayhead() / soundLibraryManager.getSelectedSound().getLength()
                : 0f;
        int step = Math.clamp(Math.round(fraction * SoundLibrarySetting.SCRUB_STEPS), 0,
                SoundLibrarySetting.SCRUB_STEPS);

        if (step == shownPlayheadStep)
            return;

        this.shownPlayheadStep = step;
        playhead.setPositionOverride(scrubPositions[step]);
    }

    // Utility \\

    private void injectHeader(int entryPoint, String text) {
        menuManager.inject(libraryMenu, entryPoint, SoundLibrarySetting.TEMPLATE_HEADER_ROW)
                .findChildById(SoundLibrarySetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(libraryMenu, entryPoint, SoundLibrarySetting.TEMPLATE_TEXT_ROW)
                .findChildById(SoundLibrarySetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private String toYesNo(boolean yes) {
        return yes ? SoundLibrarySetting.TEXT_YES : SoundLibrarySetting.TEXT_NO;
    }

    private String formatTime(float seconds) {

        int minutes = (int) (seconds / SoundLibrarySetting.SECONDS_PER_MINUTE);

        return String.format(
                SoundLibrarySetting.FORMAT_TIME, minutes, seconds - minutes * SoundLibrarySetting.SECONDS_PER_MINUTE);
    }
}
