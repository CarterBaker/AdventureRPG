package application.bootstrap.soundpipeline.soundmanager;

import application.bootstrap.soundpipeline.sound.SoundCategory;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.sound.SoundVoiceStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class SoundVoiceBranch extends BranchPackage {

    /*
     * Plays sounds through a fixed pool of voices, one audio source each, made
     * once the device opens. A sound takes a free voice, else the oldest voice
     * that does not loop, else the oldest of all. A voice answers only to the
     * handle it was given, so a handle kept after its sound ended does nothing.
     * The listener always stands at the origin of the audio scene facing the
     * way the listening camera faces, and a placed voice stands at its offset
     * from the camera through the world's wrap, worked out again whenever the
     * camera or the voice moves; a voice placed in another world than the
     * camera's is silent. A voice's loudness is its own volume times its
     * sound's, its category's and the master volume, the last two read from
     * the player's settings and set again on every voice the frame they
     * change. What a voice sounds like
     * from where the camera stands — how loud once its distance fades it, and
     * how far to the left or the right — is worked out the way the device
     * does, so a recording can mix it again. Each frame a voice whose sound has
     * run out is let go.
     */

    // Pool
    private SoundVoiceStruct[] voices;
    private boolean audible;
    private long nextSerial;

    // Volume — as last read from the settings, so a change reaches every voice once
    private float masterVolume;
    private float[] categoryVolumes;

    // Listener
    private WorldHandle listenerWorld;
    private long listenerChunk;
    private Vector3 listenerPosition;
    private Vector3 listenerDirection;
    private Vector3 listenerUp;
    private float[] listenerOrientation;

    // Scratch
    private Vector3 offsetScratch;

    // Base \\

    @Override
    protected void create() {

        // Pool
        this.voices = new SoundVoiceStruct[0];

        // Volume
        this.categoryVolumes = new float[SoundCategory.VALUES.length];
        readVolumes();

        // Listener
        this.listenerPosition = new Vector3();
        this.listenerDirection = new Vector3(0f, 0f, EngineSetting.SOUND_LISTENER_FALLBACK_FORWARD_Z);
        this.listenerUp = new Vector3(0f, EngineSetting.SOUND_LISTENER_FALLBACK_UP_Y, 0f);
        this.listenerOrientation = new float[EngineSetting.SOUND_LISTENER_ORIENTATION_FLOATS];

        // Scratch
        this.offsetScratch = new Vector3();
    }

    // Device \\

    // The pool's sources made once the device is open; with no device every sound is refused and nothing plays
    void open() {

        this.voices = new SoundVoiceStruct[EngineSetting.SOUND_VOICE_COUNT];

        for (int i = 0; i < voices.length; i++)
            voices[i] = new SoundVoiceStruct(EngineContext.audio.alGenSource(), i);

        EngineContext.audio.alDistanceModel(EngineSetting.AL_INVERSE_DISTANCE_CLAMPED);
        EngineContext.audio.alListener3f(EngineSetting.AL_POSITION, 0f, 0f, 0f);
        orientListener();
        this.audible = true;
    }

    void close() {

        for (int i = 0; i < voices.length; i++) {
            EngineContext.audio.alSourceStop(voices[i].getSource());
            EngineContext.audio.alDeleteSource(voices[i].getSource());
        }

        this.voices = new SoundVoiceStruct[0];
        this.audible = false;
    }

    // Update \\

    // Every voice whose sound has run out let go, and every voice's loudness set again once the settings change it
    @Override
    protected void update() {

        if (haveVolumesChanged()) {
            readVolumes();
            applyGains();
        }

        for (int i = 0; i < voices.length; i++) {

            SoundVoiceStruct voice = voices[i];

            if (voice.isActive() && !voice.isPaused() && EngineContext.audio.alGetSourcei(
                    voice.getSource(), EngineSetting.AL_SOURCE_STATE) != EngineSetting.AL_PLAYING)
                voice.end();
        }
    }

    // Start \\

    // The one way a sound begins: it takes a voice, which is set to play it from the offset into its samples, placed
    // when given a world; the voice's handle comes back, -1 when nothing can play
    int start(
            SoundHandle soundHandle,
            int ownerWindowID,
            float volume,
            float pitch,
            float offsetSeconds,
            boolean looping,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {

        if (!audible)
            return EngineSetting.INDEX_NOT_FOUND;

        SoundVoiceStruct voice = acquire();
        int source = voice.getSource();
        int handle = voice.begin(soundHandle, ownerWindowID, volume, pitch, looping,
                internal.getTime(), nextSerial++);

        if (worldHandle != null)
            voice.place(worldHandle, chunkCoordinate, x, y, z);

        EngineContext.audio.alSourceStop(source);
        EngineContext.audio.alSourcei(source, EngineSetting.AL_BUFFER, soundHandle.getBuffer());
        EngineContext.audio.alSourcei(source, EngineSetting.AL_LOOPING, looping
                ? EngineSetting.AL_TRUE
                : EngineSetting.AL_FALSE);
        EngineContext.audio.alSourcef(source, EngineSetting.AL_PITCH, soundHandle.getPitch() * pitch);
        EngineContext.audio.alSourcef(source, EngineSetting.AL_REFERENCE_DISTANCE, soundHandle.getReferenceDistance());
        EngineContext.audio.alSourcef(source, EngineSetting.AL_MAX_DISTANCE, soundHandle.getMaxDistance());
        EngineContext.audio.alSourcef(source, EngineSetting.AL_ROLLOFF_FACTOR, voice.isSpatial()
                ? soundHandle.getRolloff()
                : 0f);
        EngineContext.audio.alSourcei(source, EngineSetting.AL_SOURCE_RELATIVE, voice.isSpatial()
                ? EngineSetting.AL_FALSE
                : EngineSetting.AL_TRUE);
        placeVoice(voice);
        applyGain(voice);
        EngineContext.audio.alSourcef(source, EngineSetting.AL_SEC_OFFSET,
                Math.clamp(offsetSeconds, 0f, soundHandle.getLength()));
        EngineContext.audio.alSourcePlay(source);
        return handle;
    }

    // A free voice, else the oldest that does not loop, else the oldest of all, stopped for the sound taking it
    private SoundVoiceStruct acquire() {

        SoundVoiceStruct oldestOnce = null;
        SoundVoiceStruct oldest = null;

        for (int i = 0; i < voices.length; i++) {

            SoundVoiceStruct voice = voices[i];

            if (!voice.isActive())
                return voice;

            if (oldest == null || voice.getSerial() < oldest.getSerial())
                oldest = voice;

            if (!voice.isLooping() && (oldestOnce == null || voice.getSerial() < oldestOnce.getSerial()))
                oldestOnce = voice;
        }

        SoundVoiceStruct taken = oldestOnce != null ? oldestOnce : oldest;

        EngineContext.audio.alSourceStop(taken.getSource());
        taken.end();
        return taken;
    }

    // Control \\

    void stop(int handle) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null)
            return;

        EngineContext.audio.alSourceStop(voice.getSource());
        voice.end();
    }

    void pause(int handle) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null || voice.isPaused())
            return;

        EngineContext.audio.alSourcePause(voice.getSource());
        voice.setPaused(true);
    }

    void resume(int handle) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null || !voice.isPaused())
            return;

        EngineContext.audio.alSourcePlay(voice.getSource());
        voice.setPaused(false);
    }

    void setOffset(int handle, float offsetSeconds) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice != null)
            EngineContext.audio.alSourcef(voice.getSource(), EngineSetting.AL_SEC_OFFSET,
                    Math.clamp(offsetSeconds, 0f, voice.getSoundHandle().getLength()));
    }

    void setVolume(int handle, float volume) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null)
            return;

        voice.setVolume(volume);
        applyGain(voice);
    }

    void setLooping(int handle, boolean looping) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null)
            return;

        voice.setLooping(looping);
        EngineContext.audio.alSourcei(voice.getSource(), EngineSetting.AL_LOOPING, looping
                ? EngineSetting.AL_TRUE
                : EngineSetting.AL_FALSE);
    }

    // A placed voice follows what it sounds from
    void move(int handle, WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {

        SoundVoiceStruct voice = resolve(handle);

        if (voice == null || !voice.isSpatial())
            return;

        voice.place(worldHandle, chunkCoordinate, x, y, z);
        placeVoice(voice);
        applyGain(voice);
    }

    // Seconds into its sound's samples the voice has reached, 0 for a handle no voice answers to
    float getOffset(int handle) {

        SoundVoiceStruct voice = resolve(handle);

        return voice != null
                ? EngineContext.audio.alGetSourcef(voice.getSource(), EngineSetting.AL_SEC_OFFSET)
                : 0f;
    }

    // The voice that answers to the handle, null for none
    SoundVoiceStruct resolve(int handle) {

        int slot = handle & EngineSetting.SOUND_VOICE_SLOT_MASK;

        if (handle < 0 || slot >= voices.length || !voices[slot].answers(handle))
            return null;

        return voices[slot];
    }

    // Volume \\

    private boolean haveVolumesChanged() {

        if (settings.masterVolume != masterVolume)
            return true;

        for (int i = 0; i < categoryVolumes.length; i++)
            if (resolveSettingsVolume(SoundCategory.VALUES[i]) != categoryVolumes[i])
                return true;

        return false;
    }

    private void readVolumes() {

        this.masterVolume = settings.masterVolume;

        for (int i = 0; i < categoryVolumes.length; i++)
            categoryVolumes[i] = resolveSettingsVolume(SoundCategory.VALUES[i]);
    }

    // The settings' volume for the category
    private float resolveSettingsVolume(SoundCategory category) {
        return switch (category) {
            case MUSIC -> settings.musicVolume;
            case EFFECT -> settings.effectVolume;
            case AMBIENT -> settings.ambientVolume;
            case INTERFACE -> settings.interfaceVolume;
            case VOICE -> settings.voiceVolume;
        };
    }

    float getMasterVolume() {
        return masterVolume;
    }

    float getCategoryVolume(SoundCategory category) {
        return categoryVolumes[category.ordinal()];
    }

    private void applyGains() {
        for (int i = 0; i < voices.length; i++)
            if (voices[i].isActive())
                applyGain(voices[i]);
    }

    // The voice's own volume times its sound's, its category's and the master's, silent in a world not the camera's
    private void applyGain(SoundVoiceStruct voice) {

        SoundHandle soundHandle = voice.getSoundHandle();
        boolean elsewhere = voice.isSpatial() && voice.getWorldHandle() != listenerWorld;

        EngineContext.audio.alSourcef(voice.getSource(), EngineSetting.AL_GAIN, elsewhere ? 0f
                : voice.getVolume() * soundHandle.getVolume()
                        * categoryVolumes[soundHandle.getCategory().ordinal()] * masterVolume);
    }

    // Listener \\

    // The listener moves to the camera, and every placed voice is set again at its offset from it
    void placeListener(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 position,
            Vector3 direction,
            Vector3 up) {

        boolean worldChanged = worldHandle != listenerWorld;

        this.listenerWorld = worldHandle;
        this.listenerChunk = chunkCoordinate;
        listenerPosition.set(position);
        listenerDirection.set(direction);
        listenerUp.set(up);

        if (!audible)
            return;

        orientListener();

        for (int i = 0; i < voices.length; i++) {

            SoundVoiceStruct voice = voices[i];

            if (!voice.isActive() || !voice.isSpatial())
                continue;

            placeVoice(voice);

            if (worldChanged)
                applyGain(voice);
        }
    }

    private void orientListener() {

        listenerOrientation[0] = listenerDirection.x;
        listenerOrientation[1] = listenerDirection.y;
        listenerOrientation[2] = listenerDirection.z;
        listenerOrientation[3] = listenerUp.x;
        listenerOrientation[4] = listenerUp.y;
        listenerOrientation[5] = listenerUp.z;
        EngineContext.audio.alListenerfv(EngineSetting.AL_ORIENTATION, listenerOrientation);
    }

    // A placed voice stands at its offset from the camera, a voice that is not placed on the listener itself
    private void placeVoice(SoundVoiceStruct voice) {

        if (!voice.isSpatial() || voice.getWorldHandle() != listenerWorld) {
            EngineContext.audio.alSource3f(voice.getSource(), EngineSetting.AL_POSITION, 0f, 0f, 0f);
            return;
        }

        resolveOffset(voice, offsetScratch);
        EngineContext.audio.alSource3f(voice.getSource(), EngineSetting.AL_POSITION,
                offsetScratch.x, offsetScratch.y, offsetScratch.z);
    }

    // Where the voice stands from the camera, across chunks and the world's wrap
    private Vector3 resolveOffset(SoundVoiceStruct voice, Vector3 out) {

        Vector3 position = voice.getPosition();

        return resolveOffset(voice.getChunkCoordinate(), position.x, position.y, position.z, out);
    }

    private Vector3 resolveOffset(long chunkCoordinate, float x, float y, float z, Vector3 out) {

        long delta = WorldWrapUtility.unwrapToGridCoordinate(listenerWorld, listenerChunk, chunkCoordinate);

        return out.set(
                Coordinate2Long.unpackX(delta) * EngineSetting.CHUNK_SIZE + x - listenerPosition.x,
                y - listenerPosition.y,
                Coordinate2Long.unpackY(delta) * EngineSetting.CHUNK_SIZE + z - listenerPosition.z);
    }

    // How far a place stands from the camera, in blocks; endlessly far in another world or before any camera
    float resolveListenerDistance(WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {

        if (worldHandle == null || worldHandle != listenerWorld)
            return Float.MAX_VALUE;

        return resolveOffset(chunkCoordinate, x, y, z, offsetScratch).length();
    }

    // Heard \\

    // How loud the voice reaches the camera — its own and its sound's volume, faded by its distance as the device
    // fades it; silent in another world
    float resolveHeardGain(SoundVoiceStruct voice) {

        SoundHandle soundHandle = voice.getSoundHandle();
        float gain = voice.getVolume() * soundHandle.getVolume();

        if (!voice.isSpatial())
            return gain;

        if (voice.getWorldHandle() != listenerWorld)
            return 0f;

        float reference = soundHandle.getReferenceDistance();
        float distance = Math.clamp(resolveOffset(voice, offsetScratch).length(),
                reference, soundHandle.getMaxDistance());

        return gain * reference / (reference + soundHandle.getRolloff() * (distance - reference));
    }

    // How far to the camera's right the voice is heard, -1 wholly left to 1 wholly right, 0 when it is not placed
    float resolveHeardPan(SoundVoiceStruct voice) {

        if (!voice.isSpatial() || voice.getWorldHandle() != listenerWorld)
            return 0f;

        Vector3 offset = resolveOffset(voice, offsetScratch);
        float distance = offset.length();
        float rightX = listenerDirection.y * listenerUp.z - listenerDirection.z * listenerUp.y;
        float rightY = listenerDirection.z * listenerUp.x - listenerDirection.x * listenerUp.z;
        float rightZ = listenerDirection.x * listenerUp.y - listenerDirection.y * listenerUp.x;
        float rightLength = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);

        if (distance <= 0f || rightLength <= 0f)
            return 0f;

        return Math.clamp((offset.x * rightX + offset.y * rightY + offset.z * rightZ) / (distance * rightLength),
                -1f, 1f);
    }

    // Accessible \\

    boolean isAudible() {
        return audible;
    }

    SoundVoiceStruct[] getVoices() {
        return voices;
    }
}
