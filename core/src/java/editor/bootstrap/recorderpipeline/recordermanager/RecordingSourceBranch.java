package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;

import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.song.SongClipKind;
import application.bootstrap.musicpipeline.song.SongClipStruct;
import application.bootstrap.musicpipeline.song.SongData;
import application.bootstrap.musicpipeline.song.SongHandle;
import application.bootstrap.musicpipeline.song.SongLaneStruct;
import application.bootstrap.musicpipeline.songmanager.SongManager;
import application.bootstrap.musicpipeline.util.SongArpgUtility;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RecordingSourceBranch extends BranchPackage {

    /*
     * Binds every clip of a recording to what it names, at every depth: a
     * nested recording to the recording read from its file, a song to the
     * project's song, a notes clip to its notes laid out as a song of one
     * lane on its instrument, timed by its recording's tempo map, and a song
     * a Music file lays to a song of one lane laying it, the same way, so it
     * plays at the file's tempo with its volume and transpose. Nested
     * recordings are read once and kept by name, each bound in turn, and one
     * is read again whenever it is saved, along with every kept recording
     * that reaches it. While binding, the recordings being bound are kept as
     * a chain, so a clip naming one of them is left unbound with the loop
     * logged rather than walked forever. Laying a recording and saving one
     * are refused when its nested recordings reach it again at any depth —
     * directly, or through others — and the loop is named from the recording
     * itself round to it; a nested recording showing a picture must have
     * been made in its parent's world. A song or an instrument the game
     * cannot load leaves its clip unbound and silent, logged.
     */

    // Internal
    private RecordingFileBranch recordingFileBranch;
    private SongManager songManager;
    private InstrumentManager instrumentManager;

    // Directory
    private File recordingDirectory;

    // Children — every nested recording read so far, by name
    private Object2ObjectOpenHashMap<String, RecordingInstance> recordingName2Child;

    // Chain — the recordings being bound, each nesting the next
    private ObjectArrayList<String> bindingNames;

    // Base \\

    @Override
    protected void create() {

        // Children
        this.recordingName2Child = new Object2ObjectOpenHashMap<>();

        // Chain
        this.bindingNames = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.recordingFileBranch = get(RecordingFileBranch.class);
        this.songManager = get(SongManager.class);
        this.instrumentManager = get(InstrumentManager.class);
    }

    // The directory nested recordings are read from, the one the recorder saves to
    void setRecordingDirectory(File recordingDirectory) {
        this.recordingDirectory = recordingDirectory;
    }

    // Bind \\

    // Every clip of the recording bound to what it names, nested recordings bound in turn
    void bind(RecordingInstance recording) {

        bindingNames.clear();
        bindingNames.add(recording.getRecordingName());
        bindClips(recording);
        bindingNames.clear();
    }

    private void bindClips(RecordingInstance recording) {
        for (int i = 0; i < recording.getClips().size(); i++)
            bindClip(recording, recording.getClips().get(i));
    }

    // The one place a clip's name is resolved to what it plays
    void bindClip(RecordingInstance recording, RecordingClipStruct clip) {
        switch (clip.getSourceKind()) {
            case RECORDING -> clip.bindChild(resolveChild(clip.getSourceName()));
            case SONG -> clip.bindSong(clip.getSongClip() != null
                    ? laySong(recording, clip.getSongClip())
                    : resolveSong(clip.getSourceName()));
            case NOTES -> clip.bindSong(layNotes(recording, clip.getSongClip()));
            case TAKE -> {
            }
        }
    }

    // The nested recording of the name, read and bound the first time it is asked for; null when it cannot be read
    // or reaches back to a recording being bound
    private RecordingInstance resolveChild(String recordingName) {

        if (bindingNames.contains(recordingName)) {
            errorLog(EditorSetting.RECORDER_MESSAGE_NESTED_LOOP + describeChain(recordingName));
            return null;
        }

        RecordingInstance child = recordingName2Child.get(recordingName);

        if (child != null)
            return child;

        File file = ArpgUtility.resolveFile(recordingDirectory, recordingName);
        child = file.isFile() ? recordingFileBranch.read(file, recordingName) : null;

        if (child == null) {
            errorLog(EditorSetting.RECORDER_MESSAGE_NESTED_MISSING + recordingName);
            return null;
        }

        bindingNames.add(recordingName);
        bindClips(child);
        bindingNames.pop();
        recordingName2Child.put(recordingName, child);
        return child;
    }

    private String describeChain(String recordingName) {

        StringBuilder chain = new StringBuilder();

        for (int i = bindingNames.indexOf(recordingName); i < bindingNames.size(); i++)
            chain.append(bindingNames.get(i)).append(EngineSetting.SONG_LOOP_SEPARATOR);

        return chain.append(recordingName).toString();
    }

    // The project's song of the name, loaded on demand; null when the game cannot load it
    private SongData resolveSong(String songName) {

        try {
            return songManager.getSongHandleFromSongName(songName).getSongData();
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_SONG_MISSING + songName, e);
            return null;
        }
    }

    // A notes clip's notes as a song of one lane on its instrument, timed by its recording's tempo map; null when the
    // instrument cannot be loaded
    private SongData layNotes(RecordingInstance recording, SongClipStruct notes) {

        try {
            notes.resolveInstrument(instrumentManager.getInstrumentIDFromInstrumentName(notes.getInstrumentName()));
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_INSTRUMENT_MISSING + notes.getInstrumentName(), e);
            return null;
        }

        return layAlone(recording, notes);
    }

    // A Music file's song clip as a song of one lane laying the whole song it names from its start, with the clip's
    // volume and transpose, timed by the file's tempo map; null when the game cannot load the song
    private SongData laySong(RecordingInstance recording, SongClipStruct songClip) {

        SongHandle handle;

        try {
            handle = songManager.getSongHandleFromSongName(songClip.getSongName());
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_SONG_MISSING + songClip.getSongName(), e);
            return null;
        }

        SongClipStruct laid = new SongClipStruct(
                SongClipKind.SONG,
                0,
                0,
                Math.max(1, handle.getSongData().getLength()),
                EngineSetting.DEFAULT_SONG_CLIP_OFFSET,
                false,
                songClip.getVolume(),
                songClip.getTranspose(),
                EngineSetting.SONG_REFERENCE_NONE,
                SongArpgUtility.NO_NOTES,
                songClip.getSongName(),
                EngineSetting.SONG_REFERENCE_NONE);

        laid.resolveSong(handle);
        return layAlone(recording, laid);
    }

    // One clip on a song's only lane, timed by the recording's tempo map and meter, in its key
    private SongData layAlone(RecordingInstance recording, SongClipStruct clip) {
        return new SongData(
                recording.getRecordingName(),
                EngineSetting.SONG_NONE,
                recording.getRecordingName(),
                recording.getTempo(),
                recording.getBeatsPerBar(),
                recording.getBeatUnit(),
                recording.getKeyRoot(),
                recording.getKeyScale(),
                new SongLaneStruct[] { new SongLaneStruct(
                        EngineSetting.SONG_LANE_NAME_NONE,
                        EngineSetting.DEFAULT_SONG_LANE_VOLUME,
                        EngineSetting.DEFAULT_SONG_LANE_PAN,
                        EngineSetting.DEFAULT_SONG_LANE_MUTED,
                        EngineSetting.DEFAULT_SONG_LANE_SOLO) },
                new SongClipStruct[] { clip });
    }

    // Refresh \\

    // The recording saved under the name is read again the next time it is asked for, with every kept recording that
    // reaches it, so each nests it as it now stands
    void forget(String recordingName) {

        ObjectArrayList<String> stale = new ObjectArrayList<>();

        for (String keptName : recordingName2Child.keySet())
            if (keptName.equals(recordingName) || reaches(recordingName2Child.get(keptName), recordingName))
                stale.add(keptName);

        for (int i = 0; i < stale.size(); i++)
            recordingName2Child.remove(stale.get(i));
    }

    // True when the recording nests the one of the name, at any depth
    private boolean reaches(RecordingInstance recording, String recordingName) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            String nestedName = clip.getNestedName();

            if (nestedName == null)
                continue;

            if (nestedName.equals(recordingName)
                    || clip.getChild() != null && reaches(clip.getChild(), recordingName))
                return true;
        }

        return false;
    }

    // Checks \\

    // The nested recording of the name, read and bound, for a clip about to lay it; null when it cannot be read
    RecordingInstance requireChild(String recordingName) {

        bindingNames.clear();

        RecordingInstance child = resolveChild(recordingName);

        bindingNames.clear();
        return child;
    }

    // The loop laying the nested recording inside the recording would make — the recording itself, or one reaching
    // it through its own nested recordings — named from the recording round to itself; null when it makes none
    String findLoopLaying(RecordingInstance recording, RecordingInstance child) {

        String recordingName = recording.getRecordingName();

        if (child.getRecordingName().equals(recordingName))
            return recordingName + EngineSetting.SONG_LOOP_SEPARATOR + recordingName;

        ObjectArrayList<String> path = new ObjectArrayList<>();

        path.add(recordingName);
        path.add(child.getRecordingName());

        return findLoop(child, recordingName, path);
    }

    // The loop the recording's nested recordings make back to it, named from it round to itself; null for none
    String findLoopIn(RecordingInstance recording) {

        ObjectArrayList<String> path = new ObjectArrayList<>();

        path.add(recording.getRecordingName());
        return findLoop(recording, recording.getRecordingName(), path);
    }

    // Walks every nested recording by the name its clip gives, so a clip left unbound for closing a loop is still
    // followed by name
    private String findLoop(RecordingInstance recording, String rootName, ObjectArrayList<String> path) {

        ObjectArrayList<RecordingClipStruct> clips = recording.getClips();

        for (int i = 0; i < clips.size(); i++) {

            RecordingClipStruct clip = clips.get(i);
            String childName = clip.getNestedName();

            if (childName == null)
                continue;

            if (childName.equals(rootName))
                return describePath(path, childName);

            RecordingInstance child = clip.getChild() != null ? clip.getChild() : recordingName2Child.get(childName);

            if (child == null || path.contains(childName))
                continue;

            path.add(childName);

            String loop = findLoop(child, rootName, path);

            path.pop();

            if (loop != null)
                return loop;
        }

        return null;
    }

    private String describePath(ObjectArrayList<String> path, String last) {

        StringBuilder loop = new StringBuilder();

        for (int i = 0; i < path.size(); i++)
            loop.append(path.get(i)).append(EngineSetting.SONG_LOOP_SEPARATOR);

        return loop.append(last).toString();
    }

    // True when the nested recording may be laid inside the recording: made in its world, or showing no picture
    boolean isWorldShared(RecordingInstance recording, RecordingInstance child) {
        return child.getWorldName().equals(recording.getWorldName())
                || (child.resolveChannelMask() & RecorderChannel.PICTURE) == 0;
    }
}
