package application.bootstrap.soundpipeline.soundmanager;

import application.bootstrap.soundpipeline.sound.SoundCategory;
import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.sound.SoundPcmStruct;
import application.bootstrap.soundpipeline.sound.SoundVoiceStruct;
import application.bootstrap.soundpipeline.util.SoundDecodeUtility;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SoundManager extends ManagerPackage {

    /*
     * Owns the audio device, the palette of every loaded sound, and the voices
     * they play through, so any system can play any sound: get this manager,
     * then playSound(name) for a sound heard as it is, or playSoundAt(name,
     * world, place) for one placed in the world, which grows quieter with
     * distance and is heard from its side when the sound is spatial. A name is
     * resolved once to its ID for anything played often. Every play hands back
     * a voice handle to stop, pause, resume, seek, quieten or move what it
     * started; a handle whose sound has ended does nothing. Each voice
     * remembers the window whose systems asked for it, so a recording can
     * capture one window's sounds alone. The window playing the game places the
     * listener at its camera every frame. The master and category volumes of
     * the player's settings scale everything that plays. With no audio device the game runs silent and
     * every play hands back -1. Decoded samples are only ever kept in the
     * device's buffers; a tool that mixes sounds itself decodes them again.
     */

    // Internal
    private WindowManager windowManager;
    private SoundVoiceBranch soundVoiceBranch;

    // Palette
    private Object2IntOpenHashMap<String> soundName2SoundID;
    private ObjectArrayList<SoundHandle> soundID2SoundHandle;
    private int soundCount;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.soundName2SoundID = RegistryUtility.createNameIndex();
        this.soundID2SoundHandle = RegistryUtility.createPalette();

        this.soundVoiceBranch = create(SoundVoiceBranch.class);
        create(SoundLoader.class);
    }

    @Override
    protected void get() {
        this.windowManager = get(WindowManager.class);
    }

    // The device opens before any sound loads, so every sound's samples go straight to it
    @Override
    protected void awake() {

        if (EngineContext.audio.openDevice())
            soundVoiceBranch.open();
        else
            errorLog("No audio device could be opened; the game runs silent.");
    }

    @Override
    protected void dispose() {

        if (!soundVoiceBranch.isAudible())
            return;

        soundVoiceBranch.close();

        for (int i = 0; i < soundID2SoundHandle.size(); i++) {

            SoundHandle soundHandle = soundID2SoundHandle.get(i);

            if (soundHandle != null)
                EngineContext.audio.alDeleteBuffer(soundHandle.getBuffer());
        }

        EngineContext.audio.closeDevice();
    }

    // Management \\

    int registerSoundName(String soundName) {
        return RegistryUtility.registerID(
                soundName2SoundID, soundID2SoundHandle, soundName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addSound(SoundHandle soundHandle) {

        if (soundID2SoundHandle.get(soundHandle.getSoundID()) != null)
            throwException("Duplicate sound name: '" + soundHandle.getSoundName()
                    + "' was registered more than once — two files share one name under '"
                    + EngineSetting.SOUND_PATH + "'.");

        soundID2SoundHandle.set(soundHandle.getSoundID(), soundHandle);
        this.soundCount++;
    }

    // The samples go to a buffer of the device, none while the game runs silent
    int createBuffer(SoundPcmStruct pcm) {

        if (!soundVoiceBranch.isAudible())
            return EngineSetting.AL_NONE;

        int buffer = EngineContext.audio.alGenBuffer();

        EngineContext.audio.alBufferData(
                buffer,
                pcm.getChannels() == EngineSetting.SOUND_MONO_CHANNELS
                        ? EngineSetting.AL_FORMAT_MONO16
                        : EngineSetting.AL_FORMAT_STEREO16,
                pcm.getSamples(),
                pcm.getSampleRate());

        return buffer;
    }

    // On-Demand \\

    public void request(String soundName) {
        ((SoundLoader) internalLoader).request(soundName);
    }

    // Play \\

    public int playSound(String soundName) {
        return playSound(getSoundIDFromSoundName(soundName));
    }

    public int playSound(int soundID) {
        return playSound(soundID, EngineSetting.DEFAULT_SOUND_VOLUME, EngineSetting.DEFAULT_SOUND_PITCH, 0f);
    }

    public int playSound(int soundID, float volume, float pitch) {
        return playSound(soundID, volume, pitch, 0f);
    }

    // Heard as it is, wherever the camera stands, from the offset into its samples
    public int playSound(int soundID, float volume, float pitch, float offsetSeconds) {

        SoundHandle soundHandle = getSoundHandleFromSoundID(soundID);

        return soundVoiceBranch.start(soundHandle, resolveOwnerWindowID(), volume, pitch, offsetSeconds,
                soundHandle.isLoop(), null, 0L, 0f, 0f, 0f);
    }

    public int playSoundAt(String soundName, WorldHandle worldHandle, WorldPositionStruct place) {
        return playSoundAt(getSoundIDFromSoundName(soundName), worldHandle, place);
    }

    public int playSoundAt(int soundID, WorldHandle worldHandle, WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        return playSoundAt(soundID, EngineSetting.DEFAULT_SOUND_VOLUME, EngineSetting.DEFAULT_SOUND_PITCH, 0f,
                worldHandle, place.getChunkCoordinate(), position.x, position.y, position.z);
    }

    // Placed at a position within its chunk of the world, when the sound is spatial; a sound that is not is heard as
    // it is
    public int playSoundAt(
            int soundID,
            float volume,
            float pitch,
            float offsetSeconds,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {

        SoundHandle soundHandle = getSoundHandleFromSoundID(soundID);

        return soundVoiceBranch.start(soundHandle, resolveOwnerWindowID(), volume, pitch, offsetSeconds,
                soundHandle.isLoop(), soundHandle.isSpatial() ? worldHandle : null, chunkCoordinate, x, y, z);
    }

    // The window whose systems are running now, -1 outside every window's
    private int resolveOwnerWindowID() {

        WindowInstance window = windowManager.getContextWindow();

        return window != null ? window.getWindowID() : EngineSetting.INDEX_NOT_FOUND;
    }

    // Voices \\

    public void stopVoice(int voice) {
        soundVoiceBranch.stop(voice);
    }

    public void pauseVoice(int voice) {
        soundVoiceBranch.pause(voice);
    }

    public void resumeVoice(int voice) {
        soundVoiceBranch.resume(voice);
    }

    public void setVoiceOffset(int voice, float offsetSeconds) {
        soundVoiceBranch.setOffset(voice, offsetSeconds);
    }

    public void setVoiceVolume(int voice, float volume) {
        soundVoiceBranch.setVolume(voice, volume);
    }

    public void setVoiceLooping(int voice, boolean looping) {
        soundVoiceBranch.setLooping(voice, looping);
    }

    public void moveVoice(int voice, WorldHandle worldHandle, WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        moveVoice(voice, worldHandle, place.getChunkCoordinate(), position.x, position.y, position.z);
    }

    public void moveVoice(int voice, WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {
        soundVoiceBranch.move(voice, worldHandle, chunkCoordinate, x, y, z);
    }

    // True while the voice plays, false once it has ended or while it waits paused
    public boolean isVoicePlaying(int voice) {

        SoundVoiceStruct voiceStruct = soundVoiceBranch.resolve(voice);

        return voiceStruct != null && !voiceStruct.isPaused();
    }

    public boolean isVoicePaused(int voice) {

        SoundVoiceStruct voiceStruct = soundVoiceBranch.resolve(voice);

        return voiceStruct != null && voiceStruct.isPaused();
    }

    // Seconds into its sound's samples the voice has reached
    public float getVoiceOffset(int voice) {
        return soundVoiceBranch.getOffset(voice);
    }

    // Every voice of the pool, playing or not, by its slot
    public int getVoiceCount() {
        return soundVoiceBranch.getVoices().length;
    }

    public SoundVoiceStruct getVoice(int slot) {
        return soundVoiceBranch.getVoices()[slot];
    }

    public float resolveHeardGain(SoundVoiceStruct voice) {
        return soundVoiceBranch.resolveHeardGain(voice);
    }

    public float resolveHeardPan(SoundVoiceStruct voice) {
        return soundVoiceBranch.resolveHeardPan(voice);
    }

    // Listener \\

    // The camera every placed sound is heard from
    public void placeListener(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 position,
            Vector3 direction,
            Vector3 up) {
        soundVoiceBranch.placeListener(worldHandle, chunkCoordinate, position, direction, up);
    }

    // How far a place stands from the camera, in blocks, so a far source holds back from taking a voice
    public float getListenerDistance(WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {
        return soundVoiceBranch.resolveListenerDistance(worldHandle, chunkCoordinate, x, y, z);
    }

    public float getListenerDistance(WorldHandle worldHandle, WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        return getListenerDistance(worldHandle, place.getChunkCoordinate(), position.x, position.y, position.z);
    }

    // Volume \\

    // As the player's settings set it
    public float getMasterVolume() {
        return soundVoiceBranch.getMasterVolume();
    }

    public float getCategoryVolume(SoundCategory category) {
        return soundVoiceBranch.getCategoryVolume(category);
    }

    // Decode \\

    // The sound's samples decoded again from its file at the rate given, over the channels it plays with, for a tool
    // that mixes sounds itself
    public SoundPcmStruct decodeSound(int soundID, int sampleRate) {

        SoundHandle soundHandle = getSoundHandleFromSoundID(soundID);

        return SoundDecodeUtility.decodeAt(soundHandle.getFile(), sampleRate, soundHandle.getChannels(),
                soundHandle.getKeepSeconds(), soundHandle.getLoopCrossfadeSeconds());
    }

    // Accessible \\

    public boolean isAudible() {
        return soundVoiceBranch.isAudible();
    }

    public boolean hasSound(String soundName) {
        return RegistryUtility.getHandle(soundName2SoundID, soundID2SoundHandle, soundName) != null;
    }

    // The sound's ID, loaded now when its file has not been yet, -1 when the project has no such sound
    public int findSoundID(String soundName) {

        if (!hasSound(soundName) && internalLoader != null && ((SoundLoader) internalLoader).hasFile(soundName))
            request(soundName);

        return hasSound(soundName) ? soundName2SoundID.getInt(soundName) : EngineSetting.INDEX_NOT_FOUND;
    }

    public int getSoundIDFromSoundName(String soundName) {

        if (!hasSound(soundName) && internalLoader != null)
            request(soundName);

        if (!hasSound(soundName))
            throwException("Sound \"" + soundName + "\" was not found under '" + EngineSetting.SOUND_PATH
                    + "' — check the name matches the file's path below it, without its extension.");

        return soundName2SoundID.getInt(soundName);
    }

    public SoundHandle getSoundHandleFromSoundID(int soundID) {

        SoundHandle handle = RegistryUtility.getHandle(soundID2SoundHandle, soundID);

        if (handle == null)
            throwException("No handle registered for sound ID: " + soundID);

        return handle;
    }

    public SoundHandle getSoundHandleFromSoundName(String soundName) {
        return getSoundHandleFromSoundID(getSoundIDFromSoundName(soundName));
    }

    // How many sounds have loaded, which moves whenever another one does
    public int getSoundCount() {
        return soundCount;
    }

    // Every loaded sound, in the order they were loaded, added to the list given
    public ObjectArrayList<SoundHandle> collectSoundHandles(ObjectArrayList<SoundHandle> out) {

        for (int i = 0; i < soundID2SoundHandle.size(); i++) {

            SoundHandle soundHandle = soundID2SoundHandle.get(i);

            if (soundHandle != null)
                out.add(soundHandle);
        }

        return out;
    }
}
