package engine.graphics.gl;

public interface GL40 extends GL30 {

    /*
     * Engine GL40 interface. Extends GL30 with tessellation control and
     * evaluation shader stage support.
     */

    // Constants

    int GL_PATCHES = 0x000E;
    int GL_PATCH_VERTICES = 0x8E72;
    int GL_TESS_CONTROL_SHADER = 0x8E88;
    int GL_TESS_EVALUATION_SHADER = 0x8E87;

    // Tessellation \\

    void glPatchParameteri(int pname, int value);
}