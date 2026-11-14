package engine.graphics.gl;

public interface GL40 extends GL30 {

    /*
     * Engine GL40 interface. Extends GL30 with tessellation control and
     * evaluation shader stage support, and the timestamp queries the profiler
     * reads GPU time through.
     */

    // Tessellation \\

    void glPatchParameteri(int pname, int value);

    // Timer Queries \\

    int glGenQuery();

    void glDeleteQuery(int id);

    void glQueryCounter(int id, int target);

    int glGetQueryObjecti(int id, int pname);

    long glGetQueryObjecti64(int id, int pname);
}
