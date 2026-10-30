package engine.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import engine.audio.Audio;
import engine.root.EngineSetting;

class Lwjgl3Audio implements Audio {

    /*
     * Thin delegation layer mapping the engine Audio interface onto LWJGL3's
     * OpenAL and stb_vorbis bindings. Holds only the device and context it
     * opened; every other method is a direct forward to the underlying call.
     */

    // Device
    private long device;
    private long context;

    // Device \\

    public boolean openDevice() {

        this.device = ALC10.alcOpenDevice((ByteBuffer) null);

        if (device == EngineSetting.AL_NONE)
            return false;

        ALCCapabilities deviceCapabilities = ALC.createCapabilities(device);

        this.context = ALC10.alcCreateContext(device, (IntBuffer) null);

        if (context == EngineSetting.AL_NONE || !ALC10.alcMakeContextCurrent(context)) {
            closeDevice();
            return false;
        }

        AL.createCapabilities(deviceCapabilities);
        return true;
    }

    public void closeDevice() {

        if (context != EngineSetting.AL_NONE) {
            ALC10.alcMakeContextCurrent(EngineSetting.AL_NONE);
            ALC10.alcDestroyContext(context);
            this.context = EngineSetting.AL_NONE;
        }

        if (device != EngineSetting.AL_NONE) {
            ALC10.alcCloseDevice(device);
            this.device = EngineSetting.AL_NONE;
        }
    }

    // Objects \\

    public int alGenSource() {
        return AL10.alGenSources();
    }

    public void alDeleteSource(int source) {
        AL10.alDeleteSources(source);
    }

    public int alGenBuffer() {
        return AL10.alGenBuffers();
    }

    public void alDeleteBuffer(int buffer) {
        AL10.alDeleteBuffers(buffer);
    }

    public void alBufferData(int buffer, int format, short[] data, int frequency) {
        AL10.alBufferData(buffer, format, data, frequency);
    }

    // Sources \\

    public void alSourcei(int source, int param, int value) {
        AL10.alSourcei(source, param, value);
    }

    public void alSourcef(int source, int param, float value) {
        AL10.alSourcef(source, param, value);
    }

    public void alSource3f(int source, int param, float x, float y, float z) {
        AL10.alSource3f(source, param, x, y, z);
    }

    public int alGetSourcei(int source, int param) {
        return AL10.alGetSourcei(source, param);
    }

    public float alGetSourcef(int source, int param) {
        return AL10.alGetSourcef(source, param);
    }

    public void alSourcePlay(int source) {
        AL10.alSourcePlay(source);
    }

    public void alSourcePause(int source) {
        AL10.alSourcePause(source);
    }

    public void alSourceStop(int source) {
        AL10.alSourceStop(source);
    }

    // Listener \\

    public void alListenerf(int param, float value) {
        AL10.alListenerf(param, value);
    }

    public void alListener3f(int param, float x, float y, float z) {
        AL10.alListener3f(param, x, y, z);
    }

    public void alListenerfv(int param, float[] values) {
        AL10.alListenerfv(param, values);
    }

    public void alDistanceModel(int model) {
        AL10.alDistanceModel(model);
    }

    // Decoding \\

    public short[] decodeVorbis(String path, int[] format) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            ShortBuffer decoded = STBVorbis.stb_vorbis_decode_filename(path, channels, sampleRate);

            if (decoded == null)
                return null;

            short[] samples = new short[decoded.remaining()];

            decoded.get(samples);
            LibCStdlib.free(decoded);
            format[EngineSetting.SOUND_VORBIS_FORMAT_CHANNELS] = channels.get(0);
            format[EngineSetting.SOUND_VORBIS_FORMAT_SAMPLE_RATE] = sampleRate.get(0);
            return samples;
        }
    }
}
