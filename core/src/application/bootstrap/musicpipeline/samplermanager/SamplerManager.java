package application.bootstrap.musicpipeline.samplermanager;

import application.bootstrap.musicpipeline.instrument.InstrumentHandle;
import application.bootstrap.musicpipeline.instrumentmanager.InstrumentManager;
import application.bootstrap.musicpipeline.noteset.NoteEnvelopeStruct;
import application.bootstrap.musicpipeline.noteset.NoteRegionStruct;
import application.bootstrap.musicpipeline.noteset.NoteSetHandle;
import application.bootstrap.musicpipeline.notesetmanager.NoteSetManager;
import application.bootstrap.musicpipeline.sampler.SamplerNoteStruct;
import application.bootstrap.musicpipeline.util.NoteEnvelopeUtility;
import application.bootstrap.musicpipeline.util.NoteUtility;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class SamplerManager extends ManagerPackage {

    /*
     * Plays instruments note by note, as a sampler plays an SFZ instrument:
     * noteOn(instrument, pitch, velocity) finds the region the pitch plays at
     * that velocity on its note set's next round robin turn, plays its sample
     * through one of SoundManager's voices pitched from the sample's root and
     * hands back a note handle, and noteOff lets it go; noteOnAt places it in
     * the world. A note's gain is its region's volume times its velocity's
     * share times its instrument's volume times the volume it was played at
     * times its envelope, worked out each frame. A pitch played again lets the note already sounding it go; an
     * instrument holding as many notes as its polyphony lets its oldest go;
     * with every slot sounding, the oldest note let go is cut, else the oldest
     * of all. An instrument whose samples have not loaded asks for them and
     * plays nothing until they land. Notes live in a fixed array sized once,
     * so playing allocates nothing.
     */

    // Internal
    private SoundManager soundManager;
    private InstrumentManager instrumentManager;
    private NoteSetManager noteSetManager;

    // Notes
    private SamplerNoteStruct[] notes;
    private long nextSerial;

    // Round Robin — the next turn of every note set's round robin, by note set ID
    private IntArrayList noteSetID2RoundRobinTurn;

    // Base \\

    @Override
    protected void create() {

        // Notes
        this.notes = new SamplerNoteStruct[EngineSetting.SAMPLER_NOTE_COUNT];

        for (int i = 0; i < notes.length; i++)
            notes[i] = new SamplerNoteStruct(i);

        // Round Robin
        this.noteSetID2RoundRobinTurn = new IntArrayList();
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.instrumentManager = get(InstrumentManager.class);
        this.noteSetManager = get(NoteSetManager.class);
    }

    // Note On \\

    // Heard as it is, wherever the camera stands; the note's handle, -1 when nothing plays
    public int noteOn(short instrumentID, int pitch, int velocity) {
        return noteOn(instrumentID, pitch, velocity, EngineSetting.SAMPLER_NOTE_FULL_VOLUME);
    }

    // Heard as it is, scaled by a volume of 0 to 1, as a song's clips and lanes set it
    public int noteOn(short instrumentID, int pitch, int velocity, float volume) {
        return start(instrumentID, pitch, velocity, volume, null, 0L, 0f, 0f, 0f);
    }

    // Placed in the world, fading with distance and heard from its side
    public int noteOnAt(
            short instrumentID,
            int pitch,
            int velocity,
            WorldHandle worldHandle,
            WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        return start(instrumentID, pitch, velocity, EngineSetting.SAMPLER_NOTE_FULL_VOLUME, worldHandle,
                place.getChunkCoordinate(), position.x, position.y, position.z);
    }

    // The one way a note begins
    private int start(
            short instrumentID,
            int pitch,
            int velocity,
            float volume,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {

        InstrumentHandle instrument = instrumentManager.getInstrumentHandleFromInstrumentID(instrumentID);
        short noteSetID = instrument.getNoteSetID();

        if (!instrument.plays(pitch) || velocity < EngineSetting.NOTE_VELOCITY_MIN
                || velocity > EngineSetting.NOTE_VELOCITY_MAX)
            return EngineSetting.INDEX_NOT_FOUND;

        if (!noteSetManager.isReady(noteSetID)) {
            noteSetManager.prepare(noteSetID);
            return EngineSetting.INDEX_NOT_FOUND;
        }

        NoteSetHandle noteSet = instrument.getNoteSetHandle();
        NoteRegionStruct region = noteSet.findRegion(pitch, velocity, nextRoundRobinTurn(noteSetID));

        if (region == null)
            return EngineSetting.INDEX_NOT_FOUND;

        letGoPitch(instrument, pitch);
        holdPolyphony(instrument);

        float baseGain = region.getGain()
                * NoteUtility.toVelocityGain(velocity, noteSet.getVelocityTracking())
                * instrument.getVolume()
                * volume;
        float gain = baseGain * NoteEnvelopeUtility.gainAt(noteSet.getEnvelope(), 0f);
        float ratio = NoteUtility.toPitchRatio(pitch, region.getRoot(), region.getTune());
        SamplerNoteStruct note = acquire();
        int voice = worldHandle != null
                ? soundManager.playSoundAt(region.getSoundID(), gain, ratio, 0f, worldHandle, chunkCoordinate, x, y, z)
                : soundManager.playSound(region.getSoundID(), gain, ratio, 0f);

        if (voice == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        soundManager.setVoiceLooping(voice, region.getLoopMode().loopsHeld());

        return note.begin(instrument, noteSet, region, pitch, voice, baseGain, gain, nextSerial++);
    }

    // The note set's round robin turn this note takes, the next note taking the one after
    private int nextRoundRobinTurn(short noteSetID) {

        while (noteSetID2RoundRobinTurn.size() <= noteSetID)
            noteSetID2RoundRobinTurn.add(0);

        int turn = noteSetID2RoundRobinTurn.getInt(noteSetID);

        noteSetID2RoundRobinTurn.set(noteSetID, (turn + 1) & EngineSetting.SAMPLER_ROUND_ROBIN_MASK);
        return turn;
    }

    // A pitch played again lets go the note of the instrument already holding it
    private void letGoPitch(InstrumentHandle instrument, int pitch) {

        for (int i = 0; i < notes.length; i++) {

            SamplerNoteStruct note = notes[i];

            if (note.isHeld() && note.getInstrumentHandle() == instrument && note.getPitch() == pitch)
                letGo(note);
        }
    }

    // An instrument holding as many notes as its polyphony lets its oldest held note go
    private void holdPolyphony(InstrumentHandle instrument) {

        SamplerNoteStruct oldest = null;
        int held = 0;

        for (int i = 0; i < notes.length; i++) {

            SamplerNoteStruct note = notes[i];

            if (!note.isHeld() || note.getInstrumentHandle() != instrument)
                continue;

            held++;

            if (oldest == null || note.getSerial() < oldest.getSerial())
                oldest = note;
        }

        if (held >= instrument.getPolyphony())
            letGo(oldest);
    }

    // A free slot, else the oldest note let go, else the oldest of all, cut for the note taking it
    private SamplerNoteStruct acquire() {

        SamplerNoteStruct oldestLetGo = null;
        SamplerNoteStruct oldest = null;

        for (int i = 0; i < notes.length; i++) {

            SamplerNoteStruct note = notes[i];

            if (!note.isActive())
                return note;

            if (oldest == null || note.getSerial() < oldest.getSerial())
                oldest = note;

            if (!note.isHeld() && (oldestLetGo == null || note.getSerial() < oldestLetGo.getSerial()))
                oldestLetGo = note;
        }

        SamplerNoteStruct taken = oldestLetGo != null ? oldestLetGo : oldest;

        cut(taken);
        return taken;
    }

    // Note Off \\

    // The note let go: it fades over its release, a sustained sample playing on through its tail
    public void noteOff(int noteHandle) {

        SamplerNoteStruct note = resolve(noteHandle);

        if (note != null && note.isHeld())
            letGo(note);
    }

    // The one way a note is let go
    private void letGo(SamplerNoteStruct note) {

        NoteRegionStruct region = note.getRegion();
        float level = NoteEnvelopeUtility.gainAt(note.getNoteSetHandle().getEnvelope(), note.getSecondsSinceOn());

        note.letGo(region.getLoopMode().releases(), level);

        if (!region.getLoopMode().loopsReleased())
            soundManager.setVoiceLooping(note.getVoice(), false);
    }

    // Every note cut at once, its voice stopped
    public void stopAll() {

        for (int i = 0; i < notes.length; i++)
            if (notes[i].isActive())
                cut(notes[i]);
    }

    private void cut(SamplerNoteStruct note) {
        soundManager.stopVoice(note.getVoice());
        note.end();
    }

    // Update \\

    // Every note's envelope moved on and its gain set where it changed; a note whose sample ran out, whose voice
    // was taken or whose release has faded is let go of
    @Override
    protected void update() {

        float deltaTime = internal.getDeltaTime();

        for (int i = 0; i < notes.length; i++) {

            SamplerNoteStruct note = notes[i];

            if (!note.isActive())
                continue;

            if (!soundManager.isVoicePlaying(note.getVoice())) {
                note.end();
                continue;
            }

            note.advance(deltaTime);

            NoteEnvelopeStruct envelope = note.getNoteSetHandle().getEnvelope();

            if (note.isFading() && NoteEnvelopeUtility.isReleased(envelope, note.getSecondsSinceRelease())) {
                cut(note);
                continue;
            }

            float gain = note.getBaseGain() * (note.isFading()
                    ? NoteEnvelopeUtility.releaseGainAt(envelope, note.getLevelAtRelease(),
                            note.getSecondsSinceRelease())
                    : NoteEnvelopeUtility.gainAt(envelope, note.getSecondsSinceOn()));

            if (gain == note.getGain())
                continue;

            note.setGain(gain);
            soundManager.setVoiceVolume(note.getVoice(), gain);
        }
    }

    // The voice the note sounds through, -1 when the handle answers to no note
    public int getNoteVoice(int noteHandle) {

        SamplerNoteStruct note = resolve(noteHandle);

        return note != null ? note.getVoice() : EngineSetting.INDEX_NOT_FOUND;
    }

    // Utility \\

    // The note that answers to the handle, null for none
    private SamplerNoteStruct resolve(int noteHandle) {

        int slot = noteHandle & EngineSetting.SAMPLER_NOTE_SLOT_MASK;

        if (noteHandle < 0 || slot >= notes.length || !notes[slot].answers(noteHandle))
            return null;

        return notes[slot];
    }
}
