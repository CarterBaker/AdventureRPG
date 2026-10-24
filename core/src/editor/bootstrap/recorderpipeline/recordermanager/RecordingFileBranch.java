package editor.bootstrap.recorderpipeline.recordermanager;

import java.io.File;
import java.nio.ByteBuffer;

import application.bootstrap.entitypipeline.entity.EntityAction;
import application.bootstrap.entitypipeline.entity.EntityState;
import editor.bootstrap.recorderpipeline.recording.RecorderChannel;
import editor.bootstrap.recorderpipeline.recording.RecorderCurveProperty;
import editor.bootstrap.recorderpipeline.recording.RecorderKeyMode;
import editor.bootstrap.recorderpipeline.recording.RecorderOverlay;
import editor.bootstrap.recorderpipeline.recording.RecordingBodyTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCameraTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClipStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingClockTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingCurveTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingExportStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingInstance;
import editor.bootstrap.recorderpipeline.recording.RecordingMarkerStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayStreamStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingProjectileTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTakeStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingVehicleTrackStruct;
import editor.bootstrap.recorderpipeline.recording.RecordingWeatherTrackStruct;
import editor.bootstrap.recorderpipeline.recording.VideoFormat;
import editor.bootstrap.recorderpipeline.util.RecordingArpgUtility;
import editor.runtime.EditorSetting;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class RecordingFileBranch extends BranchPackage {

    /*
     * Writes a recording to its ARPG file and reads one back. The file keeps
     * the world it was made in, the movement states and actions by name so a
     * recording outlives either list changing, how it exports, its markers,
     * its bin of takes with their tracks — each track's samples packed into
     * one block by RecordingArpgUtility, a character's identity, clips and
     * looks written plainly beside them, an overlay's frames as one block of
     * frame sizes and one of their packed bytes — and the clips on its
     * timeline, each naming its take by its place in the bin. Beside each
     * track lie the curves it has been given, each naming its property and
     * part with its keys packed into one block, their modes named once for
     * the whole file as the states and actions are. A file of the
     * first version, which laid takes straight onto the timeline as layers,
     * is read as a bin of those takes each placed once where it was
     * recorded, and is saved in the new form from then on. A file that cannot
     * be read, or no longer fits the game, is logged and comes back null
     * without touching anything.
     */

    // Read
    private boolean upgraded;
    private IntArrayList keyModes;

    // Write \\

    boolean write(File file, RecordingInstance recording) {

        ArpgObjectStruct recordingArpg = new ArpgObjectStruct();
        recordingArpg.addProperty("version", EditorSetting.RECORDER_FILE_VERSION);
        recordingArpg.addProperty("world", recording.getWorldName());
        recordingArpg.add("states", buildEnumNames(EntityState.VALUES));
        recordingArpg.add("actions", buildEnumNames(EntityAction.VALUES));
        recordingArpg.add("key_modes", buildEnumNames(RecorderKeyMode.VALUES));
        recordingArpg.add("export", buildExport(recording.getExportStruct()));
        recordingArpg.add("markers", buildMarkers(recording));
        recordingArpg.add("takes", buildTakes(recording));
        recordingArpg.add("clips", buildClips(recording));

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

    private ArpgArrayStruct buildTakes(RecordingInstance recording) {

        ArpgArrayStruct takesArpg = new ArpgArrayStruct();

        for (int i = 0; i < recording.getTakes().size(); i++)
            takesArpg.add(buildTake(recording.getTakes().get(i)));

        return takesArpg;
    }

    private ArpgObjectStruct buildTake(RecordingTakeStruct take) {

        ArpgObjectStruct takeArpg = new ArpgObjectStruct();
        takeArpg.addProperty("name", take.getName());
        takeArpg.add("channels", buildChannels(take.getChannelMask()));
        takeArpg.addProperty("duration", take.getDuration());

        if (take.getCameraTrack() != null) {
            takeArpg.addProperty("camera", packCamera(take.getCameraTrack()));
            takeArpg.add("camera_curves", buildCurves(take.getCameraTrack()));
        }

        if (take.hasPlayerTrack())
            takeArpg.add("player", buildBody(take.getPlayerTrack()));

        ArpgArrayStruct residentsArpg = new ArpgArrayStruct();

        for (int i = 0; i < take.getResidentTracks().size(); i++)
            residentsArpg.add(buildBody(take.getResidentTracks().get(i)));

        takeArpg.add("residents", residentsArpg);

        ArpgArrayStruct projectilesArpg = new ArpgArrayStruct();

        for (int i = 0; i < take.getProjectileTracks().size(); i++)
            projectilesArpg.add(buildProjectile(take.getProjectileTracks().get(i)));

        takeArpg.add("projectiles", projectilesArpg);

        ArpgArrayStruct vehiclesArpg = new ArpgArrayStruct();

        for (int i = 0; i < take.getVehicleTracks().size(); i++)
            vehiclesArpg.add(buildVehicle(take.getVehicleTracks().get(i)));

        takeArpg.add("vehicles", vehiclesArpg);

        if (take.getWeatherTrack() != null) {
            takeArpg.addProperty("weather", packWeather(take.getWeatherTrack()));
            takeArpg.add("weather_curves", buildCurves(take.getWeatherTrack()));
        }

        if (take.getClockTrack() != null)
            takeArpg.add("clock", buildClock(take.getClockTrack()));

        if (take.getOverlayTrack() != null)
            takeArpg.add("interface", buildOverlays(take.getOverlayTrack()));

        return takeArpg;
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
        bodyArpg.add("curves", buildCurves(track));
        return bodyArpg;
    }

    private ArpgObjectStruct buildProjectile(RecordingProjectileTrackStruct track) {

        ArpgObjectStruct projectileArpg = new ArpgObjectStruct();
        projectileArpg.addProperty("item", track.getItemName());
        projectileArpg.addProperty("samples", packProjectile(track));
        projectileArpg.add("curves", buildCurves(track));
        return projectileArpg;
    }

    private ArpgObjectStruct buildVehicle(RecordingVehicleTrackStruct track) {

        ArpgObjectStruct vehicleArpg = new ArpgObjectStruct();
        vehicleArpg.addProperty("vehicle", track.getVehicleName());
        vehicleArpg.addProperty("masts", track.getMastCount());
        vehicleArpg.addProperty("sails", track.getSailCount());
        vehicleArpg.addProperty("doors", track.getDoorCount());
        vehicleArpg.addProperty("samples", packVehicle(track));
        vehicleArpg.add("curves", buildCurves(track));
        return vehicleArpg;
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
        clockArpg.add("curves", buildCurves(track));
        return clockArpg;
    }

    // Every curve the track has been given, by its property's name and its part
    private ArpgArrayStruct buildCurves(RecordingCurveTrackStruct track) {

        ArpgArrayStruct curvesArpg = new ArpgArrayStruct();

        for (int slot = 0; slot < track.getCurveCount(); slot++) {

            RecordingCurveStruct curve = track.getCurve(slot);

            if (curve == null || curve.isEmpty())
                continue;

            ArpgObjectStruct curveArpg = new ArpgObjectStruct();
            curveArpg.addProperty("property", ArpgUtility.toEnumName(curve.getProperty()));
            curveArpg.addProperty("part", curve.getPart());
            curveArpg.addProperty("keys", packCurve(curve));
            curvesArpg.add(curveArpg);
        }

        return curvesArpg;
    }

    private ArpgArrayStruct buildOverlays(RecordingOverlayTrackStruct track) {

        ArpgArrayStruct overlaysArpg = new ArpgArrayStruct();

        for (RecorderOverlay overlay : RecorderOverlay.VALUES) {

            RecordingOverlayStreamStruct stream = track.getStream(overlay);

            if (stream.isEmpty())
                continue;

            ArpgObjectStruct overlayArpg = new ArpgObjectStruct();
            overlayArpg.addProperty("overlay", ArpgUtility.toEnumName(overlay));
            overlayArpg.addProperty("frames", packOverlayFrames(stream));
            overlayArpg.addProperty("data", RecordingArpgUtility.toText(packOverlayData(stream)));
            overlaysArpg.add(overlayArpg);
        }

        return overlaysArpg;
    }

    private ArpgArrayStruct buildClips(RecordingInstance recording) {

        ArpgArrayStruct clipsArpg = new ArpgArrayStruct();

        for (int i = 0; i < recording.getClips().size(); i++) {

            RecordingClipStruct clip = recording.getClips().get(i);
            ArpgObjectStruct clipArpg = new ArpgObjectStruct();
            clipArpg.addProperty("take", recording.getTakes().indexOf(clip.getTake()));
            clipArpg.addProperty("lane", clip.getLane());
            clipArpg.addProperty("start", clip.getStart());
            clipArpg.addProperty("in", clip.getInPoint());
            clipArpg.addProperty("out", clip.getOutPoint());
            clipArpg.add("channels", buildChannels(clip.getChannelMask()));
            clipArpg.addProperty("muted", clip.isMuted());
            clipArpg.addProperty("time_ramp", clip.getTimeRampSeconds());
            clipsArpg.add(clipArpg);
        }

        return clipsArpg;
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

    private String packCurve(RecordingCurveStruct curve) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(curve.size(), EditorSetting.RECORDER_CURVE_KEY_BYTES);
        RecordingArpgUtility.writeFloats(block, curve.getTimes());
        RecordingArpgUtility.writeDoubles(block, curve.getValues());
        RecordingArpgUtility.writeInts(block, curve.getModes());
        RecordingArpgUtility.writeFloats(block, curve.getInTimes());
        RecordingArpgUtility.writeDoubles(block, curve.getInValues());
        RecordingArpgUtility.writeFloats(block, curve.getOutTimes());
        RecordingArpgUtility.writeDoubles(block, curve.getOutValues());
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

    private String packVehicle(RecordingVehicleTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(track.size(), resolveVehicleSampleBytes(
                track.getMastCount(), track.getSailCount(), track.getDoorCount()));
        writePlaces(block, track);
        RecordingArpgUtility.writeFloats(block, track.getOrientationsW());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsX());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsY());
        RecordingArpgUtility.writeFloats(block, track.getOrientationsZ());
        RecordingArpgUtility.writeFloats(block, track.getRudderAngles());
        RecordingArpgUtility.writeInts(block, track.getAnchored());
        RecordingArpgUtility.writeFloats(block, track.getBraceAngles());
        RecordingArpgUtility.writeFloats(block, track.getSailHoists());
        RecordingArpgUtility.writeFloats(block, track.getDoorOpenings());
        return RecordingArpgUtility.toText(block);
    }

    private String packWeather(RecordingWeatherTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(
                track.size(), EditorSetting.RECORDER_WEATHER_SAMPLE_BYTES);
        RecordingArpgUtility.writeFloats(block, track.getTimes());
        RecordingArpgUtility.writeDoubles(block, track.getFlowsX());
        RecordingArpgUtility.writeDoubles(block, track.getFlowsZ());
        RecordingArpgUtility.writeFloats(block, track.getGustTimes());
        return RecordingArpgUtility.toText(block);
    }

    // Each frame's moment, size, kind and packed length, so the packed bytes beside it can be split back apart
    private String packOverlayFrames(RecordingOverlayStreamStruct stream) {

        ByteBuffer block = RecordingArpgUtility.allocateBlock(
                stream.size(), EditorSetting.RECORDER_OVERLAY_FRAME_BYTES);
        RecordingArpgUtility.writeFloats(block, stream.getTimes());
        RecordingArpgUtility.writeInts(block, stream.getWidths());
        RecordingArpgUtility.writeInts(block, stream.getHeights());
        RecordingArpgUtility.writeInts(block, stream.getKeyframes());

        for (int i = 0; i < stream.size(); i++)
            block.putInt(stream.getFrame(i).length);

        return RecordingArpgUtility.toText(block);
    }

    private byte[] packOverlayData(RecordingOverlayStreamStruct stream) {

        byte[] data = new byte[Math.toIntExact(stream.getByteCount())];
        int offset = 0;

        for (int i = 0; i < stream.size(); i++) {

            byte[] frame = stream.getFrame(i);

            System.arraycopy(frame, 0, data, offset, frame.length);
            offset += frame.length;
        }

        return data;
    }

    private void writePlaces(ByteBuffer block, RecordingTrackStruct track) {
        RecordingArpgUtility.writeFloats(block, track.getTimes());
        RecordingArpgUtility.writeLongs(block, track.getChunkCoordinates());
        RecordingArpgUtility.writeFloats(block, track.getPositionsX());
        RecordingArpgUtility.writeFloats(block, track.getPositionsY());
        RecordingArpgUtility.writeFloats(block, track.getPositionsZ());
    }

    private int resolveVehicleSampleBytes(int mastCount, int sailCount, int doorCount) {
        return EditorSetting.RECORDER_VEHICLE_SAMPLE_BYTES
                + (mastCount + sailCount + doorCount) * EditorSetting.RECORDER_VEHICLE_PART_BYTES;
    }

    // Read \\

    // The recording in the file, or null when it cannot be read or no longer fits the game
    RecordingInstance read(File file, String recordingName) {

        ArpgObjectStruct recordingArpg = ArpgUtility.tryLoadObject(file);

        this.upgraded = false;

        if (recordingArpg == null)
            return null;

        try {
            return parseRecording(recordingArpg, recordingName);
        } catch (InternalException e) {
            errorLog(EditorSetting.RECORDER_MESSAGE_UNREADABLE + file.getAbsolutePath(), e);
            return null;
        }
    }

    // True when the last file read was of the first version, so it is saved in the new form at once
    boolean isUpgraded() {
        return upgraded;
    }

    private RecordingInstance parseRecording(ArpgObjectStruct recordingArpg, String recordingName) {

        int version = ArpgUtility.getInt(recordingArpg, "version", 0);
        boolean layered = version == EditorSetting.RECORDER_FILE_VERSION_LAYERS;

        this.upgraded = layered;

        if (version != EditorSetting.RECORDER_FILE_VERSION && !layered
                || !ArpgUtility.hasString(recordingArpg, "world")
                || !ArpgUtility.hasArray(recordingArpg, "states")
                || !ArpgUtility.hasArray(recordingArpg, "actions")
                || !ArpgUtility.hasArray(recordingArpg, layered ? "layers" : "takes"))
            return rejectFile(recordingName);

        RecordingInstance recording = create(RecordingInstance.class);
        recording.constructor(
                recordingName,
                recordingArpg.get("world").getAsString(),
                parseExport(recordingArpg));

        IntArrayList states = resolveEnumOrdinals(recordingArpg.getAsArray("states"), EntityState.VALUES);
        IntArrayList actions = resolveEnumOrdinals(recordingArpg.getAsArray("actions"), EntityAction.VALUES);

        this.keyModes = ArpgUtility.hasArray(recordingArpg, "key_modes")
                ? resolveEnumOrdinals(recordingArpg.getAsArray("key_modes"), RecorderKeyMode.VALUES)
                : new IntArrayList();

        if (ArpgUtility.hasArray(recordingArpg, "markers"))
            parseMarkers(recordingArpg.getAsArray("markers"), recording);

        boolean parsed = layered
                ? parseLayers(recordingArpg.getAsArray("layers"), recording, states, actions)
                : parseTakes(recordingArpg.getAsArray("takes"), recording, states, actions)
                        && (!ArpgUtility.hasArray(recordingArpg, "clips")
                                || parseClips(recordingArpg.getAsArray("clips"), recording));

        return parsed ? recording : rejectFile(recordingName);
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

    private boolean parseTakes(
            ArpgArrayStruct takesArpg,
            RecordingInstance recording,
            IntArrayList states,
            IntArrayList actions) {

        for (int i = 0; i < takesArpg.size(); i++) {

            ArpgObjectStruct takeArpg = takesArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(takeArpg, "name")
                    || !ArpgUtility.hasArray(takeArpg, "channels")
                    || !ArpgUtility.hasNumber(takeArpg, "duration"))
                return false;

            RecordingTakeStruct take = new RecordingTakeStruct(
                    takeArpg.get("name").getAsString(),
                    parseChannels(takeArpg.getAsArray("channels")));

            take.setDuration(takeArpg.get("duration").getAsFloat());

            if (!parseTracks(takeArpg, take, states, actions))
                return false;

            recording.addTake(take);
        }

        return true;
    }

    private boolean parseClips(ArpgArrayStruct clipsArpg, RecordingInstance recording) {

        for (int i = 0; i < clipsArpg.size(); i++) {

            ArpgObjectStruct clipArpg = clipsArpg.get(i).getAsObject();

            if (!ArpgUtility.hasNumber(clipArpg, "take")
                    || !ArpgUtility.hasNumber(clipArpg, "start")
                    || !ArpgUtility.hasNumber(clipArpg, "in")
                    || !ArpgUtility.hasNumber(clipArpg, "out")
                    || !ArpgUtility.hasArray(clipArpg, "channels"))
                return false;

            int takeIndex = clipArpg.get("take").getAsInt();

            if (takeIndex < 0 || takeIndex >= recording.getTakes().size())
                return false;

            RecordingClipStruct clip = new RecordingClipStruct(
                    recording.getTakes().get(takeIndex),
                    clipArpg.get("in").getAsFloat(),
                    clipArpg.get("out").getAsFloat(),
                    Math.clamp(ArpgUtility.getInt(clipArpg, "lane", 0), 0, EditorSetting.RECORDER_LANE_COUNT - 1),
                    clipArpg.get("start").getAsFloat(),
                    parseChannels(clipArpg.getAsArray("channels")));

            clip.setSpan(clip.getInPoint(), clip.getOutPoint());
            clip.setMuted(ArpgUtility.getBoolean(clipArpg, "muted", false));
            clip.setTimeRampSeconds(ArpgUtility.getFloat(clipArpg, "time_ramp", 0f));
            recording.addClip(clip);
        }

        return true;
    }

    // Each first-version layer becomes a take of its own, timed from its own start, and one clip of it laid where
    // the layer stood, on a lane of its own as high as the timeline allows
    private boolean parseLayers(
            ArpgArrayStruct layersArpg,
            RecordingInstance recording,
            IntArrayList states,
            IntArrayList actions) {

        for (int i = 0; i < layersArpg.size(); i++) {

            ArpgObjectStruct layerArpg = layersArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(layerArpg, "name")
                    || !ArpgUtility.hasArray(layerArpg, "channels")
                    || !ArpgUtility.hasNumber(layerArpg, "start")
                    || !ArpgUtility.hasNumber(layerArpg, "end"))
                return false;

            float start = layerArpg.get("start").getAsFloat();
            float end = layerArpg.get("end").getAsFloat();
            RecordingTakeStruct take = new RecordingTakeStruct(
                    layerArpg.get("name").getAsString(),
                    parseChannels(layerArpg.getAsArray("channels")));

            take.setDuration(Math.max(0f, end - start));

            if (!parseTracks(layerArpg, take, states, actions))
                return false;

            take.shiftTimes(-start);
            recording.addTake(take);

            RecordingClipStruct clip = new RecordingClipStruct(
                    take,
                    0f,
                    take.getDuration(),
                    Math.min(i, EditorSetting.RECORDER_LANE_COUNT - 1),
                    start,
                    take.getChannelMask());

            clip.setMuted(ArpgUtility.getBoolean(layerArpg, "muted", false));
            clip.setTimeRampSeconds(ArpgUtility.getFloat(layerArpg, "time_ramp", 0f));
            recording.addClip(clip);
        }

        return true;
    }

    // Channels by name; the first version's world was the residents and the thrown items together
    private int parseChannels(ArpgArrayStruct channelsArpg) {

        int channelMask = RecorderChannel.NONE;

        for (int i = 0; i < channelsArpg.size(); i++) {

            String channelName = channelsArpg.get(i).getAsString();

            if (channelName.equalsIgnoreCase(EditorSetting.RECORDER_FILE_CHANNEL_WORLD))
                channelMask |= RecorderChannel.CHARACTERS.getBit() | RecorderChannel.ITEMS.getBit();
            else
                channelMask |= ArpgUtility.toEnum(channelName, RecorderChannel.class).getBit();
        }

        return channelMask;
    }

    private boolean parseTracks(
            ArpgObjectStruct takeArpg,
            RecordingTakeStruct take,
            IntArrayList states,
            IntArrayList actions) {

        if (take.getCameraTrack() != null
                && (!ArpgUtility.hasString(takeArpg, "camera")
                        || !unpackCamera(takeArpg.get("camera").getAsString(), take.getCameraTrack())
                        || !parseCurves(takeArpg, "camera_curves", take.getCameraTrack())))
            return false;

        if (ArpgUtility.hasObject(takeArpg, "player")) {

            RecordingBodyTrackStruct playerTrack = parseBody(takeArpg.getAsObject("player"), states, actions);

            if (playerTrack == null)
                return false;

            take.setPlayerTrack(playerTrack);
        }

        if (ArpgUtility.hasArray(takeArpg, "residents") && !parseResidents(takeArpg, take, states, actions))
            return false;

        if (ArpgUtility.hasArray(takeArpg, "projectiles") && !parseProjectiles(takeArpg, take))
            return false;

        if (ArpgUtility.hasArray(takeArpg, "vehicles") && !parseVehicles(takeArpg, take))
            return false;

        if (take.getWeatherTrack() != null
                && (!ArpgUtility.hasString(takeArpg, "weather")
                        || !unpackWeather(takeArpg.get("weather").getAsString(), take.getWeatherTrack())
                        || !parseCurves(takeArpg, "weather_curves", take.getWeatherTrack())))
            return false;

        if (take.getClockTrack() != null
                && (!ArpgUtility.hasObject(takeArpg, "clock")
                        || !unpackClock(takeArpg.getAsObject("clock"), take.getClockTrack())
                        || !parseCurves(takeArpg.getAsObject("clock"), "curves", take.getClockTrack())))
            return false;

        return take.getOverlayTrack() == null
                || !ArpgUtility.hasArray(takeArpg, "interface")
                || parseOverlays(takeArpg.getAsArray("interface"), take.getOverlayTrack());
    }

    private boolean parseResidents(
            ArpgObjectStruct takeArpg,
            RecordingTakeStruct take,
            IntArrayList states,
            IntArrayList actions) {

        ArpgArrayStruct residentsArpg = takeArpg.getAsArray("residents");

        for (int i = 0; i < residentsArpg.size(); i++) {

            RecordingBodyTrackStruct track = parseBody(residentsArpg.get(i).getAsObject(), states, actions);

            if (track == null)
                return false;

            take.getResidentTracks().add(track);
        }

        return true;
    }

    private boolean parseProjectiles(ArpgObjectStruct takeArpg, RecordingTakeStruct take) {

        ArpgArrayStruct projectilesArpg = takeArpg.getAsArray("projectiles");

        for (int i = 0; i < projectilesArpg.size(); i++) {

            ArpgObjectStruct projectileArpg = projectilesArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(projectileArpg, "item") || !ArpgUtility.hasString(projectileArpg, "samples"))
                return false;

            RecordingProjectileTrackStruct track = new RecordingProjectileTrackStruct(
                    projectileArpg.get("item").getAsString());

            if (!unpackProjectile(projectileArpg.get("samples").getAsString(), track)
                    || !parseCurves(projectileArpg, "curves", track))
                return false;

            take.getProjectileTracks().add(track);
        }

        return true;
    }

    private boolean parseVehicles(ArpgObjectStruct takeArpg, RecordingTakeStruct take) {

        ArpgArrayStruct vehiclesArpg = takeArpg.getAsArray("vehicles");

        for (int i = 0; i < vehiclesArpg.size(); i++) {

            ArpgObjectStruct vehicleArpg = vehiclesArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(vehicleArpg, "vehicle")
                    || !ArpgUtility.hasNumber(vehicleArpg, "masts")
                    || !ArpgUtility.hasNumber(vehicleArpg, "sails")
                    || !ArpgUtility.hasNumber(vehicleArpg, "doors")
                    || !ArpgUtility.hasString(vehicleArpg, "samples"))
                return false;

            RecordingVehicleTrackStruct track = new RecordingVehicleTrackStruct(
                    vehicleArpg.get("vehicle").getAsString(),
                    vehicleArpg.get("masts").getAsInt(),
                    vehicleArpg.get("sails").getAsInt(),
                    vehicleArpg.get("doors").getAsInt());

            if (track.getMastCount() < 0 || track.getSailCount() < 0 || track.getDoorCount() < 0
                    || !unpackVehicle(vehicleArpg.get("samples").getAsString(), track)
                    || !parseCurves(vehicleArpg, "curves", track))
                return false;

            take.getVehicleTracks().add(track);
        }

        return true;
    }

    private boolean parseOverlays(ArpgArrayStruct overlaysArpg, RecordingOverlayTrackStruct track) {

        for (int i = 0; i < overlaysArpg.size(); i++) {

            ArpgObjectStruct overlayArpg = overlaysArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(overlayArpg, "overlay")
                    || !ArpgUtility.hasString(overlayArpg, "frames")
                    || !ArpgUtility.hasString(overlayArpg, "data"))
                return false;

            RecordingOverlayStreamStruct stream = track.getStream(
                    ArpgUtility.toEnum(overlayArpg.get("overlay").getAsString(), RecorderOverlay.class));

            if (!unpackOverlay(overlayArpg.get("frames").getAsString(), overlayArpg.get("data").getAsString(), stream))
                return false;
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

        if (!unpackBody(bodyArpg.get("samples").getAsString(), track, states, actions)
                || !parseCurves(bodyArpg, "curves", track))
            return null;

        return track;
    }

    // The curves the track was given, none for a file written before curves; one for a property or part the track
    // no longer holds is left behind, and a malformed one fails the file
    private boolean parseCurves(ArpgObjectStruct ownerArpg, String key, RecordingCurveTrackStruct track) {

        if (!ArpgUtility.hasArray(ownerArpg, key))
            return true;

        ArpgArrayStruct curvesArpg = ownerArpg.getAsArray(key);

        for (int i = 0; i < curvesArpg.size(); i++) {

            ArpgObjectStruct curveArpg = curvesArpg.get(i).getAsObject();

            if (!ArpgUtility.hasString(curveArpg, "property") || !ArpgUtility.hasString(curveArpg, "keys"))
                return false;

            RecorderCurveProperty property = findCurveProperty(curveArpg.get("property").getAsString());
            int part = ArpgUtility.getInt(curveArpg, "part", EngineSetting.INDEX_NOT_FOUND);
            int slot = property != null ? track.findCurveSlot(property, part) : EngineSetting.INDEX_NOT_FOUND;

            if (slot == EngineSetting.INDEX_NOT_FOUND)
                continue;

            RecordingCurveStruct curve = new RecordingCurveStruct(property, part);

            if (!unpackCurve(curveArpg.get("keys").getAsString(), curve))
                return false;

            track.setCurve(slot, curve);
        }

        return true;
    }

    private RecorderCurveProperty findCurveProperty(String name) {

        for (RecorderCurveProperty property : RecorderCurveProperty.values())
            if (property.name().equalsIgnoreCase(name))
                return property;

        return null;
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

    private boolean unpackCurve(String text, RecordingCurveStruct curve) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, EditorSetting.RECORDER_CURVE_KEY_BYTES);

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        RecordingArpgUtility.readFloats(block, count, curve.getTimes());
        RecordingArpgUtility.readDoubles(block, count, curve.getValues());
        RecordingArpgUtility.readInts(block, count, curve.getModes());
        RecordingArpgUtility.readFloats(block, count, curve.getInTimes());
        RecordingArpgUtility.readDoubles(block, count, curve.getInValues());
        RecordingArpgUtility.readFloats(block, count, curve.getOutTimes());
        RecordingArpgUtility.readDoubles(block, count, curve.getOutValues());

        if (count == 0 || !curve.isOrdered() || !remapOrdinals(curve.getModes(), keyModes))
            return false;

        curve.resolveHandles();
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

    private boolean unpackVehicle(String text, RecordingVehicleTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, resolveVehicleSampleBytes(
                track.getMastCount(), track.getSailCount(), track.getDoorCount()));

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        readPlaces(block, count, track);
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsW());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsX());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsY());
        RecordingArpgUtility.readFloats(block, count, track.getOrientationsZ());
        RecordingArpgUtility.readFloats(block, count, track.getRudderAngles());
        RecordingArpgUtility.readInts(block, count, track.getAnchored());
        RecordingArpgUtility.readFloats(block, count * track.getMastCount(), track.getBraceAngles());
        RecordingArpgUtility.readFloats(block, count * track.getSailCount(), track.getSailHoists());
        RecordingArpgUtility.readFloats(block, count * track.getDoorCount(), track.getDoorOpenings());
        return true;
    }

    private boolean unpackWeather(String text, RecordingWeatherTrackStruct track) {

        ByteBuffer block = RecordingArpgUtility.fromText(text, EditorSetting.RECORDER_WEATHER_SAMPLE_BYTES);

        if (block == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);

        RecordingArpgUtility.readFloats(block, count, track.getTimes());
        RecordingArpgUtility.readDoubles(block, count, track.getFlowsX());
        RecordingArpgUtility.readDoubles(block, count, track.getFlowsZ());
        RecordingArpgUtility.readFloats(block, count, track.getGustTimes());
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

    // The frames split back apart from the packed bytes by their lengths, every byte accounted for
    private boolean unpackOverlay(String framesText, String dataText, RecordingOverlayStreamStruct stream) {

        ByteBuffer block = RecordingArpgUtility.fromText(framesText, EditorSetting.RECORDER_OVERLAY_FRAME_BYTES);
        byte[] data = RecordingArpgUtility.fromText(dataText);

        if (block == null || data == null)
            return false;

        int count = RecordingArpgUtility.readSampleCount(block);
        int timesAt = EditorSetting.RECORDER_BLOCK_HEADER_BYTES;
        int widthsAt = timesAt + count * Float.BYTES;
        int heightsAt = widthsAt + count * Integer.BYTES;
        int keyframesAt = heightsAt + count * Integer.BYTES;
        int lengthsAt = keyframesAt + count * Integer.BYTES;
        int offset = 0;

        for (int i = 0; i < count; i++) {

            int length = block.getInt(lengthsAt + i * Integer.BYTES);

            if (length < 0 || offset + length > data.length)
                return false;

            byte[] frame = new byte[length];

            System.arraycopy(data, offset, frame, 0, length);
            offset += length;

            stream.addFrame(
                    block.getFloat(timesAt + i * Float.BYTES),
                    block.getInt(widthsAt + i * Integer.BYTES),
                    block.getInt(heightsAt + i * Integer.BYTES),
                    block.getInt(keyframesAt + i * Integer.BYTES) != 0,
                    frame);
        }

        return offset == data.length;
    }

    private void readPlaces(ByteBuffer block, int count, RecordingTrackStruct track) {
        RecordingArpgUtility.readFloats(block, count, track.getTimes());
        RecordingArpgUtility.readLongs(block, count, track.getChunkCoordinates());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsX());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsY());
        RecordingArpgUtility.readFloats(block, count, track.getPositionsZ());
    }
}
