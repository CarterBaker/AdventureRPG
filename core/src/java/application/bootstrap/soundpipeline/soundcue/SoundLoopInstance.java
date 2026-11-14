package application.bootstrap.soundpipeline.soundcue;

import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import application.bootstrap.soundpipeline.soundmanager.SoundManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3;

public class SoundLoopInstance extends InstancePackage {

    /*
     * A sound cue held as a loop by the system that owns it — the wind, the
     * rain, a river, a sail in the breeze. The owner holds it every frame at
     * an intensity from 0 to 1, and the loop's loudness eases toward it over
     * the cue's fade: it starts once it is heard at all, follows its place
     * when it is placed, and lets its voice go once it has faded out. A voice
     * taken by another sound is begun again while the loop is still wanted.
     * Holding it allocates nothing.
     */

    // Internal
    private SoundManager soundManager;
    private SoundCueManager soundCueManager;

    // Cue
    private int cueID;
    private float cueVolume;
    private float fadeSeconds;

    // Voice
    private int voice;
    private float level;

    // Internal \\

    @Override
    protected void create() {

        // Voice
        this.voice = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.soundManager = get(SoundManager.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    // Constructor \\

    public void constructor(int cueID) {

        SoundCueHandle cueHandle = soundCueManager.getCueHandleFromCueID(cueID);

        // Cue
        this.cueID = cueID;
        this.cueVolume = cueHandle.getVolume();
        this.fadeSeconds = cueHandle.getFadeSeconds();
    }

    // Hold \\

    // Heard as it is, wherever the camera stands
    public void hold(float intensity) {
        holdAt(intensity, null, 0L, 0f, 0f, 0f);
    }

    public void holdAt(float intensity, WorldHandle worldHandle, WorldPositionStruct place) {

        Vector3 position = place.getPosition();

        holdAt(intensity, worldHandle, place.getChunkCoordinate(), position.x, position.y, position.z);
    }

    // The one way the loop is held: its level eased toward the intensity, begun, followed or let go as it sounds
    public void holdAt(float intensity, WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {

        float target = Math.clamp(intensity, 0f, 1f);
        float step = internal.getDeltaTime() / fadeSeconds;

        this.level = target > level ? Math.min(target, level + step) : Math.max(target, level - step);

        if (level <= 0f) {
            stop();
            return;
        }

        if (!soundManager.isVoicePlaying(voice) && !soundManager.isVoicePaused(voice)) {
            begin(worldHandle, chunkCoordinate, x, y, z);
            return;
        }

        soundManager.setVoiceVolume(voice, cueVolume * level);

        if (worldHandle != null)
            soundManager.moveVoice(voice, worldHandle, chunkCoordinate, x, y, z);
    }

    private void begin(WorldHandle worldHandle, long chunkCoordinate, float x, float y, float z) {

        this.voice = worldHandle != null
                ? soundCueManager.playCueAt(cueID, level, worldHandle, chunkCoordinate, x, y, z)
                : soundCueManager.playCue(cueID, level);
        soundManager.setVoiceLooping(voice, true);
    }

    // Lets the loop go at once, its level back at nothing
    public void stop() {

        soundManager.stopVoice(voice);
        this.voice = EngineSetting.INDEX_NOT_FOUND;
        this.level = 0f;
    }

    // Accessible \\

    public boolean isSounding() {
        return level > 0f;
    }

    public float getLevel() {
        return level;
    }
}
