package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;
import java.nio.ByteBuffer;

import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCameraTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClockTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingLayerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingProjectileTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTrackStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.RecordingArpgUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class RecordingFileBranch extends BranchPackage {

    /*
     * Writes a recording to its ARPG file and reads one back. The file keeps
     * the world it was made in, the movement states and actions by name so a
     * recording outlives either list changing, how it exports, its markers,
     * and every layer with its tracks — each track's samples packed into one
     * block by RecordingArpgUtility, a character's identity, clips and looks
     * written plainly beside them. A file that cannot be read, or no longer
     * fits the game, is logged and comes back null without touching anything.
     */

    // Write \\

    boolean write(File file, RecordingInstance recording) {

        ArpgObjectStruct recordingArpg = new ArpgObjectStruct();
        recordingArpg.addProperty("version", EditorSetting.RECORDER_FILE_VERSION);
        recordingArpg.addProperty("world", recording.getWorldName());
        recordingArpg.add("states", buildEnumNames(EntityState.VALUES));
        recordingArpg.add("actions", buildEnumNames(EntityAction.VALUES));
        recordingArpg.add("export", buildExport(recording.getExportStruct()));
        recordingArpg.add("markers", buildMarkers(recording));
        recordingArpg.add("layers", buildLayers(recording));

        return ArpgUtility.tryWriteObject(file, recordingArpg);
    }

    private ArpgArrayStruct buildEnumNames(Enum<?>[] values) {

        ArpgArrayStruct namesArpg = new ArpgArrayStruct();

        for (int i = 0; i < values.length; i++)
            namesArpg.add(values[i].name().toLowerCase());

        return namesArpg;
    }

    private ArpgObjectStruct buildExport(RecordingExportStruct exportStruct) {

        ArpgObjectStruct exportArpg = new ArpgObjectStruct();
        exportArpg.addProperty("width", exportStruct.getWidth());
        exportArpg.addProperty("height", exportStruct.getHeight());
        exportArpg.addProperty("frame_rate", exportStruct.getFrameRate());
        exportArpg.addProperty("format", ArpgUtility.toEnumName(exportStruct.getFormat()));
        exportArpg.addProperty("motion_blur", exportStruct.getMotionBlurSamples());
        exportArpg.addProperty("supersample", exportStruct.isSupersampled());
        exportArpg.addProperty("hud", exportStruct.isHud());
        exportArpg.addProperty("range_start", exportStruct.getRangeStart());
        exportArpg.addProperty("range_end", exportStruct.getRangeEnd());
        return exportArpg;
    }

    private ArpgArrayStruct buildMarkers(RecordingInstance recording) {

        ArpgArrayStruct markersArpg = new ArpgArrayStruct();

        for (int i = 0; i < recording.getMarkers().size(); i++) {

            RecordingMarkerStruct marker = recording.getMarkers().get(i);
            ArpgObjectStruct markerArpg = new ArpgObjectStruct();
            markerArpg.addProperty("time", marker.getTime());
            markerArpg.addProperty("name", marker.getName());
            markersArpg.add(markerArpg);
        }

        return markersArpg;
    }

    private ArpgArrayStruct buildLayers(RecordingInstance recording) {

        ArpgArrayStruct layersArpg = new ArpgArrayStruct();

        for (int i = 0; i < recording.getLayers().size(); i++)
            layersArpg.add(buildLayer(recording.getLayers().get(i)));

        return layersArpg;
    }

    private ArpgObjectStruct buildLayer(RecordingLayerStruct layer) {

        ArpgObjectStruct layerArpg = new ArpgObjectStruct();
        layerArpg.addProperty("name", layer.getName());
        layerArpg.add("channels", buildChannels(layer.getChannelMask()));
        layerArpg.addProperty("start", layer.getStartTime());
        layerArpg.addProperty("end", layer.getEndTime());
        layerArpg.addProperty("muted", layer.isMuted());
        layerArpg.addProperty("time_ramp", layer.getTimeRampSeconds());

        if (layer.getCameraTrack() != null)
            layerArpg.addProperty("camera", packCamera(layer.getCameraTrack()));

        if (layer.hasPlayerTrack())
            layerArpg.add("player", buildBody(layer.getPlayerTrack()));

        ArpgArrayStruct residentsArpg = new ArpgArrayStruct();

        for (int i = 0; i < layer.getResidentTracks().size(); i++)
            residentsArpg.add(buildBody(layer.getResidentTracks().get(i)));

        layerArpg.add("residents", residentsArpg);

        ArpgArrayStruct projectilesArpg = new ArpgArrayStruct();

        for (int i = 0; i < layer.getProjectileTracks().size(); i++)
            projectilesArpg.add(buildProjectile(layer.getProjectileTracks().get(i)));

        layerArpg.add("projectiles", projectilesArpg);

        if (layer.getClockTrack() != null)
            layerArpg.add("clock", buildClock(layer.getClockTrack()));

        return layerArpg;
    }

    private ArpgArrayStruct buildChannels(int channelMask) {

        ArpgArrayStruct channelsArpg = new ArpgArrayStruct();

        for (RecorderChannel channel : RecorderChannel.VALUES)
            if (channel.isIn(channelMask))
                channelsArpg.add(ArpgUtility.toEnumName(channel));

        return channelsArpg;
    }

    private ArpgObjectStruct buildBody(RecordingBodyTrackStruct track) {

        ArpgObjectStruct bodyArpg = new ArpgObjectStruct();
        bodyArpg.addProperty("template", track.getTemplateName());
        bodyArpg.addProperty("gender", track.getGenderName());
        bodyArpg.addProperty("name", track.getDisplayName());

        ArpgArrayStruct clipsArpg = new ArpgArrayStruct();

        for (int i = 0; i < track.getClipNames().size(); i++)
            clipsArpg.add(track.getClipNames().get(i));

        bodyArpg.add("clips", clipsArpg);

        ArpgArrayStruct looksArpg = new ArpgArrayStruct();

        for (int i = 0; i < track.getLooks().size(); i++) {

            ArpgObjectStruct lookArpg = new ArpgObjectStruct();
            lookArpg.addProperty("time", track.getLookTimes().getFloat(i));
            lookArpg.add("look", track.getLooks().get(i));
            looksArpg.add(lookArpg);
        }

        bodyArpg.add("looks", looksArpg);
        bodyArpg.addProperty("samples", packBody(track));
        return bodyArpg;
    }

    private ArpgObjectStruct buildProjectile(RecordingProjectileTrackStruct track) {

        ArpgObjectStruct projectileArpg = new ArpgObjectStruct();
        projectileArpg.addProperty("item", track.getItemName());
        projectileArpg.addProperty("samples", packProjectile(track));
        return projectileArpg;
    }

    private ArpgObjectStruct buildClock(RecordingClockTrackStruct track) {

        ByteBuffer samples = RecordingArpgUtility.allocateBlock(
                track.size(), EditorSetting.RECORDER_CLOCK_SAMPLE_BYTES);
        RecordingArpgUtility.writeFloats(samples, track.getTimes());
        RecordingArpgUtility.writeLongs(samples, track.getElapsedMillis());

        ByteBuffer jumps = RecordingArpgUtility.allocateBlock(
                track.getJumpTimes().size(), EditorSetting.RECORDER_CLOCK_SAMPLE_BYTES);
        RecordingArpgUtility.writeFloats(jumps, track.getJumpTimes());
        RecordingArpgUtility.writeLongs(jumps, track.getJumpMillis());

        ArpgObjectStruct clockArpg = new ArpgObjectStruct();
        clockArpg.addProperty("samples", RecordingArpgUtility.toText(samples));
        clockArpg.addProperty("jumps", RecordingArpgUtility.toText(jumps));
        return clockArpg;
    }

    // Pack \\

    private String packCamera(RecordingCameraTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(track.size(), EditorSetting.RECORDER_CAMERA_SAMPLE_BYTES);
        writePlaces(block, track);
        RecordingArpgUtility.writeFloats(block, track.getDirectionsX());
        RecordingArpgUtility.writeFloats(block, track.getDirectionsY());
        RecordingArpgUtility.writeFloats(block, track.getDirectionsZ());
        RecordingArpgUtility.writeFloats(block, track.getShakeYaws());
        RecordingArpgUtility.writeFloats(block, track.getShakePitches());
        RecordingArpgUtility.writeFloats(block, track.getShakeRolls());
        RecordingArpgUtility.writeFloats(block, track.getFovs());
        return RecordingArpgUtility.toText(block);
    }

    private String packBody(RecordingBodyTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(track.size(), EditorSetting.RECORDER_BODY_SAMPLE_BYTES);
        writePlaces(block, track);
        RecordingArpgUtility.writeInts(block, track.getStates());
        RecordingArpgUtility.writeFloats(block, track.getHorizontalSpeeds());
        RecordingArpgUtility.writeFloats(block, track.getVerticalSpeeds());
        RecordingArpgUtility.writeFloats(block, track.getBodyYaws());
        RecordingArpgUtility.writeFloats(block, track.getBodyYawRates());
        RecordingArpgUtility.writeFloats(block, track.getLookPitches());
        RecordingArpgUtility.writeFloats(block, track.getLookYaws());
        RecordingArpgUtility.writeFloats(block, track.getGroundOffsets());
        RecordingArpgUtility.writeInts(block, track.getActions());
        RecordingArpgUtility.writeInts(block, track.getClips());
        RecordingArpgUtility.writeFloats(block, track.getPhases());
        RecordingArpgUtility.writeInts(block, track.getSequences());
        return RecordingArpgUtility.toText(block);
    }

    private String packProjectile(RecordingProjectileTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(
                track.size(), EditorSetting.RECORDER_PROJECTILE_SAMPLE_BYTES);
        writePlaces(block, track);
        RecordingArpgUtility.writeFloats(block, track.getOrientationsW());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsX());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsY());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsZ());
        return RecordingArpgUtility.toText(block);
    }

    private void writePlaces(ByteBuffer block, RecordingTrackStruct track) {
        RecordingArpgUtility.writeFloats(block, track.getTimes());
        RecordingArpgUtility.writeLongs(block, track.getChunkCoordinates());
        RecordingArpgUtility.writeFloats(block, track.getPositionsX());
        RecordingArpgUtility.writeFloats(block, track.getPositionsY());
        RecordingArpgUtility.writeFloats(block, track.getPositionsZ());
    }

    // Read \\

    // The recording in the file, or null when it cannot be read or no longer fits the game
    RecordingInstance read(File file, String recordingName) {

        ArpgObjectStruct recordingArpg = ArpgUtility.tryLoadObject(file);

        if (recordingArpg == null)
            return null;

        try {
            return parseRecording(recordingArpg, recordingName);
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_UNREADABLE + file.getAbsolutePath(), e);
            return null;
        }
    }

    private RecordingInstance parseRecording(ArpgObjectStruct recordingArpg, String recordingName) {

        if (ArpgUtility.getInt(recordingArpg, "version", 0) != EditorSetting.RECORDER_FILE_VERSION
                || !ArpgUtility.hasString(recordingArpg, "world")
                || !ArpgUtility.hasArray(recordingArpg, "states")
                || !ArpgUtility.hasArray(recordingArpg, "actions")
                || !ArpgUtility.hasArray(recordingArpg, "layers"))
            return rejectFile(recordingName);

        RecordingInstance recording = create(RecordingInstance.class);
        recording.constructor(
                recordingName,
                recordingArpg.get("world").getAsString(),
                parseExport(recordingArpg));

        IntArrayList states = resolveEnumOrdinals(recordingArpg.getAsArray("states"), EntityState.VALUES);
        IntArrayList actions = resolveEnumOrdinals(recordingArpg.getAsArray("actions"), EntityAction.VALUES);

        if (ArpgUtility.hasArray(recordingArpg, "markers"))
            parseMarkers(recordingArpg.getAsArray("markers"), recording);

        ArpgArrayStruct layersArpg = recordingArpg.getAsArray("layers");

        for (int i = 0; i < layersArpg.size(); i++) {

            RecordingLayerStruct layer = parseLayer(layersArpg.get(i).getAsObject(), states, actions);

            if (layer == null)
                return rejectFile(recordingName);

            recording.addLayer(layer);
        }

        return recording;
    }

    private RecordingInstance rejectFile(String recordingName) {
        errorLog(EditorSetting.RECORDER_MESSAGE_MALFORMED + recordingName);
        return null;
    }

    // Each name's place in the enum as it stands today, the first constant for a name it no longer has
    private IntArrayList resolveEnumOrdinals(ArpgArrayStruct namesArpg, Enum<?>[] values) {

        IntArrayList ordinals = new IntArrayList(namesArpg.size());

        for (int i = 0; i < namesArpg.size(); i++) {

            String name = namesArpg.get(i).getAsString();
            int ordinal = 0;

            for (int j = 0; j < values.length; j++)
                if (values[j].name().equalsIgnoreCase(name))
                    ordinal = j;

            ordinals.add(ordinal);
        }

        return ordinals;
    }

    private RecordingExportStruct parseExport(ArpgObjectStruct recordingArpg) {

        RecordingExportStruct exportStruct = new RecordingExportStruct();

        if (!ArpgUtility.hasObject(recordingArpg, "export"))
            return exportStruct;

        ArpgObjectStruct exportArpg = recordingArpg.getAsObject("export");

        exportStruct.setSize(
                ArpgUtility.getInt(exportArpg, "width", exportStruct.getWidth()),
                ArpgUtility.getInt(exportArpg, "height", exportStruct.getHeight()));
        exportStruct.setFrameRate(ArpgUtility.getInt(exportArpg, "frame_rate", exportStruct.getFrameRate()));
        exportStruct.setFormat(ArpgUtility.getEnum(exportArpg, "format", VideoFormat.class, exportStruct.getFormat()));
        exportStruct.setMotionBlurSamples(
                ArpgUtility.getInt(exportArpg, "motion_blur", exportStruct.getMotionBlurSamples()));
        exportStruct.setSupersampled(ArpgUtility.getBoolean(exportArpg, "supersample", false));
        exportStruct.setHud(ArpgUtility.getBoolean(exportArpg, "hud", false));
        exportStruct.setRangeStart(ArpgUtility.getFloat(exportArpg, "range_start", EditorSetting.RECORDER_TIME_UNSET));
        exportStruct.setRangeEnd(ArpgUtility.getFloat(exportArpg, "range_end", EditorSetting.RECORDER_TIME_UNSET));
        return exportStruct;
    }

    private void parseMarkers(ArpgArrayStruct markersArpg, RecordingInstance recording) {

        for (int i = 0; i < markersArpg.size(); i++) {

            ArpgObjectStruct markerArpg = markersArpg.get(i).getAsObject();

            if (ArpgUtility.hasNumber(markerArpg, "time") && ArpgUtility.hasString(markerArpg, "name"))
                recording.addMarker(new RecordingMarkerStruct(
                        markerArpg.get("time").getAsFloat(),
                        markerArpg.get("name").getAsString()));
        }
    }

    private RecordingLayerStruct parseLayer(ArpgObjectStruct layerArpg, IntArrayList states, IntArrayList actions) {

        if (!ArpgUtility.hasString(layerArpg, "name")
                || !ArpgUtility.hasArray(layerArpg, "channels")
                || !ArpgUtility.hasNumber(layerArpg, "start")
                || !ArpgUtility.hasNumber(layerArpg, "end"))
            return null;

        RecordingLayerStruct layer = new RecordingLayerStruct(
                layerArpg.get("name").getAsString(),
                parseChannels(layerArpg.getAsArray("channels")),
                layerArpg.get("start").getAsFloat());

        layer.setEndTime(layerArpg.get("end").getAsFloat());
        layer.setMuted(ArpgUtility.getBoolean(layerArpg, "muted", false));
        layer.setTimeRampSeconds(ArpgUtility.getFloat(layerArpg, "time_ramp", 0f));

        if (layer.getCameraTrack() != null
                && (!ArpgUtility.hasString(layerArpg, "camera")
                        || !unpackCamera(layerArpg.get("camera").getAsString(), layer.getCameraTrack())))
            return null;

        if (ArpgUtility.hasObject(layerArpg, "player")) {

            RecordingBodyTrackStruct playerTrack = parseBody(layerArpg.getAsObject("player"), states, actions);

            if (playerTrack == null)
                return null;

            layer.setPlayerTrack(playerTrack);
        }

        if (ArpgUtility.hasArray(layerArpg, "residents") && !parseResidents(layerArpg, layer, states, actions))
            return null;

        if (ArpgUtility.hasArray(layerArpg, "projectiles") && !parseProjectiles(layerArpg, layer))
            return null;

        if (layer.getClockTrack() != null
                && (!ArpgUtility.hasObject(layerArpg, "clock")
                        || !unpackClock(layerArpg.getAsObject("clock"), layer.getClockTrack())))
            return null;

        return layer;
    }

    private int parseChannels(ArpgArrayStruct channelsArpg) {

        int channelMask = RecorderChannel.NONE;

        for (int i = 0; i < channelsArpg.size(); i++)
            channelMask |= ArpgUtility.toEnum(channelsArpg.get(i).getAsString(), RecorderChannel.class).getBit();

        return channelMask;
    }

    private boolean parseResidents(
            ArpgObjectStruct layerArpg,
            RecordingLayerStruct layer,
            IntArrayList states,
            IntArrayList actions) {

        ArpgArrayStruct residentsArpg = layerArpg.getAsArray("residents");

        for (int i = 0; i < residentsArpg.size(); i++) {

            RecordingBodyTrackStruct track = parseBody(residentsArpg.get(i).getAsObject(), states, actions);

            if (track == null)
                return false;

            layer.getResidentTracks().add(track);
        }

        return true;
    }

    private boolean parseProjectiles(ArpgObjectStruct layerArpg, RecordingLayerStruct layer) {

        ArpgArrayStruct projectilesArpg = layerArpg.getAsArray("projectiles");

        for (int i = 0; i < projectilesArpg.size(); i++) {

            ArpgObjectStruct projectileArpg = projectilesArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(projectileArpg, "item") || !ArpgUtility.hasString(projectileArpg, "samples"))
                return false;

            RecordingProjectileTrackStruct track = new RecordingProjectileTrackStruct(
                    projectileArpg.get("item").getAsString());

            if (!unpackProjectile(projectileArpg.get("samples").getAsString(), track))
                return false;

            layer.getProjectileTracks().add(track);
        }

        return true;
    }

    private RecordingBodyTrackStruct parseBody(ArpgObjectStruct bodyArpg, IntArrayList states, IntArrayList actions) {

        if (!ArpgUtility.hasString(bodyArpg, "template")
                || !ArpgUtility.hasString(bodyArpg, "gender")
                || !ArpgUtility.hasString(bodyArpg, "name")
                || !ArpgUtility.hasArray(bodyArpg, "clips")
                || !ArpgUtility.hasArray(bodyArpg, "looks")
                || !ArpgUtility.hasString(bodyArpg, "samples"))
            return null;

        RecordingBodyTrackStruct track = new RecordingBodyTrackStruct(
                bodyArpg.get("template").getAsString(),
                bodyArpg.get("gender").getAsString(),
                bodyArpg.get("name").getAsString());

        ArpgArrayStruct clipsArpg = bodyArpg.getAsArray("clips");

        for (int i = 0; i < clipsArpg.size(); i++)
            track.getClipNames().add(clipsArpg.get(i).getAsString());

        ArpgArrayStruct looksArpg = bodyArpg.getAsArray("looks");

        for (int i = 0; i < looksArpg.size(); i++) {

            ArpgObjectStruct lookArpg = looksArpg.get(i).getAsObject();

            if (!ArpgUtility.hasNumber(lookArpg, "time") || !ArpgUtility.hasObject(lookArpg, "look"))
                return null;

            track.addLook(lookArpg.get("time").getAsFloat(), lookArpg.getAsObject("look"));
        }

        if (!unpackBody(bodyArpg.get("samples").getAsString(), track, states, actions))
            return null;

        return track;
    }

    // Unpack \\

    private boolean unpackCamera(String text, RecordingCameraTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, EditorSetting.RECORDER_CAMERA_SAMPLE_BYTES);

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        readPlaces(block, count, track);
        RecordingArpgUtility.readFloats(block, count, track.getDirectionsX());
        RecordingArpgUtility.readFloats(block, count, track.getDirectionsY());
        RecordingArpgUtility.readFloats(block, count, track.getDirectionsZ());
        RecordingArpgUtility.readFloats(block, count, track.getShakeYaws());
        RecordingArpgUtility.readFloats(block, count, track.getShakePitches());
        RecordingArpgUtility.readFloats(block, count, track.getShakeRolls());
        RecordingArpgUtility.readFloats(block, count, track.getFovs());
        return true;
    }

    private boolean unpackBody(
            String text,
            RecordingBodyTrackStruct track,
            IntArrayList states,
            IntArrayList actions) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, EditorSetting.RECORDER_BODY_SAMPLE_BYTES);

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        readPlaces(block, count, track);
        RecordingArpgUtility.readInts(block, count, track.getStates());
        RecordingArpgUtility.readFloats(block, count, track.getHorizontalSpeeds());
        RecordingArpgUtility.readFloats(block, count, track.getVerticalSpeeds());
        RecordingArpgUtility.readFloats(block, count, track.getBodyYaws());
        RecordingArpgUtility.readFloats(block, count, track.getBodyYawRates());
        RecordingArpgUtility.readFloats(block, count, track.getLookPitches());
        RecordingArpgUtility.readFloats(block, count, track.getLookYaws());
        RecordingArpgUtility.readFloats(block, count, track.getGroundOffsets());
        RecordingArpgUtility.readInts(block, count, track.getActions());
        RecordingArpgUtility.readInts(block, count, track.getClips());
        RecordingArpgUtility.readFloats(block, count, track.getPhases());
        RecordingArpgUtility.readInts(block, count, track.getSequences());

        return remapOrdinals(track.getStates(), states)
                && remapOrdinals(track.getActions(), actions)
                && areClipsValid(track);
    }

    // Swaps each ordinal as the file wrote it for the constant of that name today
    private boolean remapOrdinals(IntArrayList column, IntArrayList ordinals) {

        for (int i = 0; i < column.size(); i++) {

            int written = column.getInt(i);

            if (written < 0 || written >= ordinals.size())
                return false;

            column.set(i, ordinals.getInt(written));
        }

        return true;
    }

    private boolean areClipsValid(RecordingBodyTrackStruct track) {

        IntArrayList clips = track.getClips();

        for (int i = 0; i < clips.size(); i++)
            if (clips.getInt(i) < EditorSetting.RECORDER_NO_CLIP || clips.getInt(i) >= track.getClipNames().size())
                return false;

        return true;
    }

    private boolean unpackProjectile(String text, RecordingProjectileTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, EditorSetting.RECORDER_PROJECTILE_SAMPLE_BYTES);

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        readPlaces(block, count, track);
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsW());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsX());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsY());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsZ());
        return true;
    }

    private boolean unpackClock(ArpgObjectStruct clockArpg, RecordingClockTrackStruct track) {

        if (!ArpgUtility.hasString(clockArpg, "samples") || !ArpgUtility.hasString(clockArpg, "jumps"))
            return false;

        ByteBuffer samples = RecordingArpgUtility.fromText(
                clockArpg.get("samples").getAsString(), EditorSetting.RECORDER_CLOCK_SAMPLE_BYTES);
        ByteBuffer jumps = RecordingArpgUtility.fromText(
                clockArpg.get("jumps").getAsString(), EditorSetting.RECORDER_CLOCK_SAMPLE_BYTES);

        if (samples == null || jumps == null)
            return false;

        int sampleCount = RecordingArpgUtility.readSampleCount(samples);
        int jumpCount = RecordingArpgUtility.readSampleCount(jumps);

        RecordingArpgUtility.readFloats(samples, sampleCount, track.getTimes());
        RecordingArpgUtility.readLongs(samples, sampleCount, track.getElapsedMillis());
        RecordingArpgUtility.readFloats(jumps, jumpCount, track.getJumpTimes());
        RecordingArpgUtility.readLongs(jumps, jumpCount, track.getJumpMillis());
        return true;
    }

    private void readPlaces(ByteBuffer block, int count, RecordingTrackStruct track) {
        RecordingArpgUtility.readFloats(block, count, track.getTimes());
        RecordingArpgUtility.readLongs(block, count, track.getChunkCoordinates());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsX());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsY());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsZ());
    }
}
