package application.bootstrap.musicpipeline.instrumentmanager;

import java.io.File;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.musicpipeline.instrument.InstrumentData;
import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrument.InstrumentRigStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class InstrumentBuilder extends BuilderPackage {

    /*
     * Parses one instrument ARPG file into an InstrumentData and wraps it in
     * an InstrumentHandle, under the short instrument ID InstrumentManager
     * assigns its name. "note_set" names the note set it plays from, loaded
     * on demand, and "low" and "high" the pitches it plays, each defaulting
     * to the end of that note set's reach and both inside what it covers.
     * "display_name" is the name it is shown under, its own name when blank;
     * "polyphony" how many notes it holds at once, up to the sampler's note
     * count; "volume" how loud it plays, 0 to 1; "midi_program" the General
     * MIDI program a MIDI file names it by, -1 for none; and "percussion"
     * whether it plays MIDI channel 10's drums. "rigs" names every rig that
     * can play it, each with the "hold" clip a body holds it with and the
     * "play" clip it plays it with; both loop and are made for the rig they
     * are named under.
     */

    // Internal
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;
    private AnimationManager animationManager;
    private RigManager rigManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
        this.animationManager = get(AnimationManager.class);
        this.rigManager = get(RigManager.class);
    }

    // Build \\

    InstrumentHandle build(File file, String instrumentName) {

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        short instrumentID = instrumentManager.registerInstrumentName(instrumentName);
        String owner = "Instrument \"" + instrumentName + "\"";

        NoteSetHandle noteSetHandle = noteSetManager.getNoteSetHandleFromNoteSetName(
                ArpgUtility.validateString(arpg, "note_set"));
        int low = ArpgUtility.getInt(arpg, "low", noteSetHandle.getLow());
        int high = ArpgUtility.getInt(arpg, "high", noteSetHandle.getHigh());

        if (low > high)
            throwException(owner + " plays from \"low\" " + low + " above its \"high\" " + high + ". File: "
                    + file.getName());

        if (!noteSetHandle.covers(low, high))
            throwException(owner + " plays " + low + " to " + high + ", but note set \""
                    + noteSetHandle.getNoteSetName() + "\" has no region for some of those pitches (it reaches "
                    + noteSetHandle.getLow() + " to " + noteSetHandle.getHigh() + "). File: " + file.getName());

        int polyphony = ArpgUtility.getInt(arpg, "polyphony", EngineSetting.DEFAULT_INSTRUMENT_POLYPHONY);

        if (polyphony < 1 || polyphony > EngineSetting.SAMPLER_NOTE_COUNT)
            throwException(owner + " holds a \"polyphony\" outside 1 to " + EngineSetting.SAMPLER_NOTE_COUNT
                    + " notes. File: " + file.getName());

        float volume = ArpgUtility.getFloat(arpg, "volume", EngineSetting.DEFAULT_INSTRUMENT_VOLUME);

        if (volume < 0f || volume > 1f)
            throwException(owner + " plays at a \"volume\" outside 0 to 1. File: " + file.getName());

        int midiProgram = ArpgUtility.getInt(arpg, "midi_program", EngineSetting.INSTRUMENT_MIDI_PROGRAM_NONE);

        if (midiProgram < EngineSetting.INSTRUMENT_MIDI_PROGRAM_NONE
                || midiProgram > EngineSetting.INSTRUMENT_MIDI_PROGRAM_MAX)
            throwException(owner + " names a \"midi_program\" outside " + EngineSetting.INSTRUMENT_MIDI_PROGRAM_NONE
                    + " to " + EngineSetting.INSTRUMENT_MIDI_PROGRAM_MAX + ". File: " + file.getName());

        String displayName = ArpgUtility.getString(arpg, "display_name", EngineSetting.INSTRUMENT_DISPLAY_NAME_NONE);

        if (displayName.isEmpty())
            displayName = instrumentName.substring(instrumentName.lastIndexOf(EngineSetting.PATH_SEPARATOR) + 1);

        InstrumentData instrumentData = new InstrumentData(
                instrumentName,
                instrumentID,
                displayName,
                noteSetHandle,
                low,
                high,
                polyphony,
                volume,
                midiProgram,
                ArpgUtility.getBoolean(arpg, "percussion", EngineSetting.DEFAULT_INSTRUMENT_PERCUSSION),
                parseRigs(ArpgUtility.validateObject(arpg, "rigs"), owner, file));

        InstrumentHandle handle = create(InstrumentHandle.class);
        handle.constructor(instrumentData);

        return handle;
    }

    // Rig Parsing \\

    private InstrumentRigStruct[] parseRigs(ArpgObjectStruct rigsArpg, String owner, File file) {

        InstrumentRigStruct[] rigs = new InstrumentRigStruct[rigsArpg.size()];
        int index = 0;

        for (String rigName : rigsArpg.keySet()) {

            ArpgObjectStruct rigArpg = ArpgUtility.validateObject(rigsArpg, rigName);
            RigHandle rigHandle = rigManager.getRigHandleFromRigName(rigName);
            String rigOwner = owner + " rig \"" + rigName + "\"";

            rigs[index++] = new InstrumentRigStruct(
                    rigHandle,
                    resolveClip(ArpgUtility.validateString(rigArpg, "hold"), rigHandle, rigOwner, file),
                    resolveClip(ArpgUtility.validateString(rigArpg, "play"), rigHandle, rigOwner, file));
        }

        return rigs;
    }

    // Utility \\

    private AnimationClipHandle resolveClip(String clipName, RigHandle rigHandle, String owner, File file) {

        AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

        if (!clip.isLooping())
            throwException(owner + " clip \"" + clipName + "\" plays once — holding and playing an instrument "
                    + "loop. File: " + file.getName());

        if (clip.getRigHandle() != rigHandle)
            throwException(owner + " clip \"" + clipName + "\" is made for a different rig. File: "
                    + file.getName());

        return clip;
    }
}
