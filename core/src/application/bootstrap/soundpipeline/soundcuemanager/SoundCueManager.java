package application.bootstrap.soundpipeline.soundcuemanager;

import java.util.SplittableRandom;

import application.bootstrap.soundpipeline.sound.SoundHandle;
import application.bootstrap.soundpipeline.soundcue.SoundCueHandle;
import application.bootstrap.soundpipeline.soundcue.SoundSurfaceStruct;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SoundCueManager extends ManagerPackage {

    /*
     * Owns the palette of sound cues — what the game plays for each kind of
     * moment — and plays them through SoundManager, so a system names the
     * moment and the data decides how it sounds. Each play picks one of the
     * cue's sounds at random, never the one it played last while it has
     * another, and strays from the cue's volume and pitch by up to their
     * variances. A cue's sounds resolve to IDs the first time it plays; a
     * sound not in the project yet is left out, and a cue left with none plays
     * nothing and says so once, so the game runs whole while its sounds are
     * still being gathered. A system resolves a cue's name to its ID once and
     * plays by ID; SoundLoopInstance holds a cue as a loop that fades with its
     * intensity. A sound placed farther from the camera than it carries is
     * not begun, so distant footsteps never take a voice from what is heard.
     * A surface — what a block or covering is made of — names four cues under
     * one folder, resolved once and shared by everything made of it.
     */

    // Internal
    private SoundManager soundManager;

    // Palette
    private Object2IntOpenHashMap<String> cueName2CueID;
    private ObjectArrayList<SoundCueHandle> cueID2CueHandle;

    // Surfaces
    private Object2ObjectOpenHashMap<String, SoundSurfaceStruct> surfaceName2SurfaceStruct;

    // Random
    private SplittableRandom random;

    // Scratch
    private IntArrayList resolveScratch;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.cueName2CueID = RegistryUtility.createNameIndex();
        this.cueID2CueHandle = RegistryUtility.createPalette();

        // Surfaces
        this.surfaceName2SurfaceStruct = new Object2ObjectOpenHashMap<>();

        // Random
        this.random = new SplittableRandom(EngineSetting.SOUND_CUE_RANDOM_SEED);

        // Scratch
        this.resolveScratch = new IntArrayList();

        create(SoundCueLoader.class);
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
    }

    // Management \\

    int registerCueName(String cueName) {
        return RegistryUtility.registerID(
                cueName2CueID, cueID2CueHandle, cueName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addCue(SoundCueHandle cueHandle) {

        if (cueID2CueHandle.get(cueHandle.getCueID()) != null)
            throwException("Duplicate sound cue name: '" + cueHandle.getCueName()
                    + "' was registered more than once");

        cueID2CueHandle.set(cueHandle.getCueID(), cueHandle);
    }

    // On-Demand \\

    public void request(String cueName) {
        ((SoundCueLoader) internalLoader).request(cueName);
    }

    // Play \\

    // Heard as it is, wherever the camera stands; the voice's handle comes back, -1 when nothing plays
    public int playCue(int cueID) {
        return playCue(cueID, 1f);
    }

    public int playCue(int cueID, float volumeScale) {
        return start(cueID, volumeScale, null, 0L, 0f, 0f, 0f);
    }

    public int playCueAt(int cueID, WorldHandle worldHandle, WorldPositionStruct place) {
        return playCueAt(cueID, 1f, worldHandle, place);
    }

    public int playCueAt(int cueID, float volumeScale, WorldHandle worldHandle, WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        return start(cueID, volumeScale, worldHandle, place.getChunkCoordinate(),
                position.x, position.y, position.z);
    }

    // Placed at a position within its chunk of the world, for a cue whose sounds are spatial
    public int playCueAt(
            int cueID,
            float volumeScale,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {
        return start(cueID, volumeScale, worldHandle, chunkCoordinate, x, y, z);
    }

    // The one way a cue plays: a variant chosen, its volume and pitch strayed, placed when given a world; a cue ID
    // of INDEX_NOT_FOUND, a moment that names no cue, plays nothing
    private int start(
            int cueID,
            float volumeScale,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {

        if (cueID == EngineSetting.INDEX_NOT_FOUND)
            return EngineSetting.INDEX_NOT_FOUND;

        SoundCueHandle cueHandle = getCueHandleFromCueID(cueID);
        int soundID = pickSound(cueHandle);

        if (soundID == EngineSetting.INDEX_NOT_FOUND || volumeScale <= 0f
                || worldHandle != null && isBeyondHearing(soundID, worldHandle, chunkCoordinate, x, y, z))
            return EngineSetting.INDEX_NOT_FOUND;

        float volume = Math.clamp(cueHandle.getVolume() * volumeScale * stray(cueHandle.getVolumeVariance()), 0f, 1f);
        float pitch = cueHandle.getPitch() * stray(cueHandle.getPitchVariance());

        return worldHandle != null
                ? soundManager.playSoundAt(soundID, volume, pitch, 0f, worldHandle, chunkCoordinate, x, y, z)
                : soundManager.playSound(soundID, volume, pitch, 0f);
    }

    // One of the cue's sounds at random, never the last one played while it has another
    private int pickSound(SoundCueHandle cueHandle) {

        if (!cueHandle.isResolved())
            resolve(cueHandle);

        int[] soundIDs = cueHandle.getSoundIDs();

        if (soundIDs.length == 0)
            return EngineSetting.INDEX_NOT_FOUND;

        int last = cueHandle.getLastVariant();
        int variant = soundIDs.length == 1 ? 0
                : last < 0 ? random.nextInt(soundIDs.length)
                        : random.nextInt(soundIDs.length - 1);

        if (soundIDs.length > 1 && last >= 0 && variant >= last)
            variant++;

        cueHandle.setLastVariant(variant);
        return soundIDs[variant];
    }

    // The cue's sounds the project has, by ID; one with none says so once
    private void resolve(SoundCueHandle cueHandle) {

        String[] soundNames = cueHandle.getSoundNames();

        resolveScratch.clear();

        for (int i = 0; i < soundNames.length; i++) {

            int soundID = soundManager.findSoundID(soundNames[i]);

            if (soundID != EngineSetting.INDEX_NOT_FOUND)
                resolveScratch.add(soundID);
        }

        cueHandle.resolve(resolveScratch.toIntArray());

        if (resolveScratch.isEmpty())
            log("Sound cue '" + cueHandle.getCueName() + "' plays nothing yet: none of its sounds is under '"
                    + EngineSetting.SOUND_PATH + "'.");
    }

    // Farther from the camera than the sound carries, when it is placed at all
    private boolean isBeyondHearing(
            int soundID,
            WorldHandle worldHandle,
            long chunkCoordinate,
            float x,
            float y,
            float z) {

        SoundHandle soundHandle = soundManager.getSoundHandleFromSoundID(soundID);
        float distance = soundManager.getListenerDistance(worldHandle, chunkCoordinate, x, y, z);

        return soundHandle.isSpatial() && distance > soundHandle.getMaxDistance();
    }

    // A factor from 1 - variance to 1 + variance
    private float stray(float variance) {
        return variance > 0f ? 1f + (float) ((random.nextDouble() * 2.0 - 1.0) * variance) : 1f;
    }

    // Surfaces \\

    // The cues of the surface named, resolved once and shared; null for SOUND_SURFACE_NONE, the surface of silence
    public SoundSurfaceStruct getSurfaceStruct(String surfaceName) {

        if (surfaceName.equals(EngineSetting.SOUND_SURFACE_NONE))
            return null;

        SoundSurfaceStruct surfaceStruct = surfaceName2SurfaceStruct.get(surfaceName);

        if (surfaceStruct == null) {

            String folder = EngineSetting.SOUND_SURFACE_CUE_PATH + surfaceName;

            surfaceStruct = new SoundSurfaceStruct(
                    getCueIDFromCueName(folder + EngineSetting.SOUND_SURFACE_STEP_CUE),
                    getCueIDFromCueName(folder + EngineSetting.SOUND_SURFACE_DIG_CUE),
                    getCueIDFromCueName(folder + EngineSetting.SOUND_SURFACE_BREAK_CUE),
                    getCueIDFromCueName(folder + EngineSetting.SOUND_SURFACE_PLACE_CUE));
            surfaceName2SurfaceStruct.put(surfaceName, surfaceStruct);
        }

        return surfaceStruct;
    }

    // Accessible \\

    public boolean hasCue(String cueName) {
        return RegistryUtility.getHandle(cueName2CueID, cueID2CueHandle, cueName) != null;
    }

    public int getCueIDFromCueName(String cueName) {

        if (!hasCue(cueName) && internalLoader != null)
            request(cueName);

        if (!hasCue(cueName))
            throwException("Sound cue \"" + cueName + "\" was not found under '" + EngineSetting.SOUND_CUE_PATH
                    + "' — check the name matches the file's path below it, without its extension.");

        return cueName2CueID.getInt(cueName);
    }

    public SoundCueHandle getCueHandleFromCueID(int cueID) {

        SoundCueHandle handle = RegistryUtility.getHandle(cueID2CueHandle, cueID);

        if (handle == null)
            throwException("No handle registered for sound cue ID: " + cueID);

        return handle;
    }

    // Every cue loaded, added to the list given
    public ObjectArrayList<SoundCueHandle> collectCueHandles(ObjectArrayList<SoundCueHandle> out) {

        for (int i = 0; i < cueID2CueHandle.size(); i++) {

            SoundCueHandle cueHandle = cueID2CueHandle.get(i);

            if (cueHandle != null)
                out.add(cueHandle);
        }

        return out;
    }
}
