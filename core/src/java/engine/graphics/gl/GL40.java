package engine.graphics.gl;

public interface GL40 extends GL30 {

    /*
     * Engine GL40 interface. Extends GL30 with tessellation control and
     * evaluation shader stage support, the timestamp queries the profiler
     * reads GPU time through, the block-compressed texture arrays streamed
     * tiles live in, and the queries that check the context can hold them.
     */

    // Tessellation \\

    void glPatchParameteri(int pname, int value);

    // Timer Queries \\

    int glGenQuery();

    void glDeleteQuery(int id);

    void glQueryCounter(int id, int target);

    int glGetQueryObjecti(int id, int pname);

    long glGetQueryObjecti64(int id, int pname);

    // Compressed Textures \\

    void glCompressedTexImage3D(int target, int level, int internalformat, int width, int height, int depth,
            int border, int imageSize, long data);

    void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width,
            int height, int depth, int format, int imageSize, long data);

    void glTexParameterf(int target, int pname, float param);

    // Queries \\

    int glGetInteger(int pname);

    float glGetFloat(int pname);

    String glGetStringi(int name, int index);
}
