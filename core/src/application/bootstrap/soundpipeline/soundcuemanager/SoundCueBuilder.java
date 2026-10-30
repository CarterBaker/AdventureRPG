package application.bootstrap.soundpipeline.soundcuemanager;

import java.io.File;

import application.bootstrap.soundpipeline.soundcue.SoundCueData;
import application.bootstrap.soundpipeline.soundcue.SoundCueHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;

class SoundCueBuilder extends BuilderPackage {

    /*
     * Parses a sound cue ARPG into a SoundCueData and wraps it in a
     * SoundCueHandle. "sounds" names the sounds it chooses among, by their
     * names below the sound directory, and must name at least one; "volume"
     * and "pitch" set how it plays, "volumeVariance" and "pitchVariance" how
     * far each play strays from them, and "fadeSeconds" how long a cue held
     * as a loop takes to fade fully in or out. Every optional field falls back
     * to its EngineSetting default.
     */

    // Internal
    private SoundCueManager soundCueManager;

    // Base \\

    @Override
    protected void get() {
        this.soundCueManager = get(SoundCueManager.class);
    }

    // Build \\

    SoundCueHandle build(File file, File root) {

        String cueName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        int cueID = soundCueManager.registerCueName(cueName);
        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        ArpgArrayStruct soundsArpg = ArpgUtility.validateArray(arpg, "sounds");

        if (soundsArpg.size() == 0)
            throwException("Sound cue \"" + cueName + "\" field \"sounds\" must name at least one sound.");

        String[] soundNames = new String[soundsArpg.size()];

        for (int i = 0; i < soundNames.length; i++)
            soundNames[i] = soundsArpg.get(i).getAsString();

        SoundCueData cueData = new SoundCueData(
                cueName,
                cueID,
                soundNames,
                parseRange(arpg, cueName, "volume", EngineSetting.DEFAULT_SOUND_CUE_VOLUME, 0f, 1f),
                parseRange(arpg, cueName, "volumeVariance", EngineSetting.DEFAULT_SOUND_CUE_VOLUME_VARIANCE, 0f, 1f),
                parsePositive(arpg, cueName, "pitch", EngineSetting.DEFAULT_SOUND_CUE_PITCH),
                parseRange(arpg, cueName, "pitchVariance", EngineSetting.DEFAULT_SOUND_CUE_PITCH_VARIANCE, 0f, 1f),
                parsePositive(arpg, cueName, "fadeSeconds", EngineSetting.DEFAULT_SOUND_CUE_FADE_SECONDS));

        SoundCueHandle cueHandle = create(SoundCueHandle.class);
        cueHandle.constructor(cueData);

        return cueHandle;
    }

    // Parsing \\

    private float parseRange(
            ArpgObjectStruct arpg,
            String cueName,
            String field,
            float fallback,
            float minimum,
            float maximum) {

        float value = ArpgUtility.getFloat(arpg, field, fallback);

        if (value < minimum || value > maximum)
            throwException("Sound cue \"" + cueName + "\" field \"" + field + "\" must be between " + minimum
                    + " and " + maximum + ", got: " + value);

        return value;
    }

    private float parsePositive(ArpgObjectStruct arpg, String cueName, String field, float fallback) {

        float value = ArpgUtility.getFloat(arpg, field, fallback);

        if (value <= 0f)
            throwException("Sound cue \"" + cueName + "\" field \"" + field + "\" must be greater than 0, got: "
                    + value);

        return value;
    }
}
