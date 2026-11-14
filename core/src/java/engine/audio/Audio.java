package engine.audio;

public interface Audio {

    /*
     * Engine audio interface. Maps the subset of OpenAL calls the engine uses,
     * the opening and closing of the output device, a buffer's loop points
     * (AL_SOFT_loop_points, which the device must offer), and the decoding of
     * an Ogg Vorbis file. Implemented by the backend audio delegation class.
     */

    // Device \\

    boolean openDevice();

    void closeDevice();

    // Objects \\

    int alGenSource();

    void alDeleteSource(int source);

    int alGenBuffer();

    void alDeleteBuffer(int buffer);

    void alBufferData(int buffer, int format, short[] data, int frequency);

    // The stretch, in sample frames, a source looping the buffer repeats; set before any source holds the buffer
    void setBufferLoopPoints(int buffer, int startFrame, int endFrame);

    // Sources \\

    void alSourcei(int source, int param, int value);

    void alSourcef(int source, int param, float value);

    void alSource3f(int source, int param, float x, float y, float z);

    int alGetSourcei(int source, int param);

    float alGetSourcef(int source, int param);

    void alSourcePlay(int source);

    void alSourcePause(int source);

    void alSourceStop(int source);

    // Listener \\

    void alListenerf(int param, float value);

    void alListener3f(int param, float x, float y, float z);

    void alListenerfv(int param, float[] values);

    void alDistanceModel(int model);

    // Decoding \\

    // The file's samples interleaved, no more than the seconds given of them when that is above nothing, its channel
    // count and sample rate written to the format given; null when the file cannot be decoded
    short[] decodeVorbis(String path, float maxSeconds, int[] format);
}
