package editor.runtime.soundlibrary;

public class SoundLibrarySetting {

    /*
     * Constants used only by SoundLibraryContext — the row templates its
     * panels are filled from and their elements, its entry points, the scrub
     * bar's fixed steps and the size of its playhead, how often the time
     * refreshes, and every word the tab shows.
     */

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/SoundLibrary/sound_library_header_row";
    public static final String TEMPLATE_TEXT_ROW = "editor/SoundLibrary/sound_library_text_row";
    public static final String TEMPLATE_DETAIL_ROW = "editor/SoundLibrary/sound_library_detail_row";
    public static final String TEMPLATE_FOLDER_ROW = "editor/SoundLibrary/sound_library_folder_row";
    public static final String TEMPLATE_SOUND_ROW = "editor/SoundLibrary/sound_library_sound_row";
    public static final String TEMPLATE_SOUND_ROW_SELECTED = "editor/SoundLibrary/sound_library_sound_row_selected";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "sound_library_row_label";
    public static final String ELEMENT_ROW_VALUE = "sound_library_row_value";
    public static final String ELEMENT_ROW_SELECT = "sound_library_row_select";
    public static final String ELEMENT_ROW_PLAY = "sound_library_row_play";

    // Entry Points
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_TIME = 1;
    public static final int ENTRY_LOOP = 2;
    public static final int ENTRY_SOUNDS = 3;
    public static final int ENTRY_DETAILS = 4;
    public static final int ENTRY_SCRUB_TRACK = 5;
    public static final int ENTRY_PLAYHEAD = 6;
    public static final int ENTRY_LIST_TITLE = 7;

    // Scrub Bar
    public static final int SCRUB_STEPS = 500;
    public static final float SCRUB_PERCENT = 100f;
    public static final float REFRESH_SECONDS = 0.05f;
    public static final int SECONDS_PER_MINUTE = 60;

    // Text
    public static final String TEXT_EMPTY = "";
    public static final String TEXT_NO_SOUNDS = "No sounds yet — put WAV or OGG files under assets/sounds";
    public static final String TEXT_NO_SELECTION = "Select a sound to see it here";
    public static final String TEXT_ROOT_FOLDER = "sounds";
    public static final String TEXT_FOLDER_OPENED = "- ";
    public static final String TEXT_FOLDER_CLOSED = "+ ";
    public static final String TEXT_FOLDER = "Folder";
    public static final String TEXT_LENGTH = "Length";
    public static final String TEXT_CATEGORY = "Category";
    public static final String TEXT_FORMAT = "Format";
    public static final String TEXT_VOLUME = "Volume";
    public static final String TEXT_PITCH = "Pitch";
    public static final String TEXT_LOOPS = "Loops";
    public static final String TEXT_PLACED = "Placed in the world";
    public static final String TEXT_HEARD_WITHIN = "Full volume within";
    public static final String TEXT_FADES_TO = "Fades out by";
    public static final String TEXT_YES = "Yes";
    public static final String TEXT_NO = "No";
    public static final String TEXT_MONO = "Mono";
    public static final String TEXT_STEREO = "Stereo";
    public static final String TEXT_LOOP_ON = "Loop on";
    public static final String TEXT_LOOP_OFF = "Loop off";
    public static final String TEXT_DRAG_HINT = "Drag a sound onto a Recorder tab's timeline to lay it over the takes";

    // Formats
    public static final String FORMAT_LIST_TITLE = "Sounds (%d)";
    public static final String FORMAT_FORMAT = "%s, %d Hz";
    public static final String FORMAT_DECIMAL = "%.2f";
    public static final String FORMAT_DISTANCE = "%.1f blocks";
    public static final String FORMAT_TIME = "%d:%05.2f";
    public static final String FORMAT_PLAYHEAD = "%s / %s";
}
