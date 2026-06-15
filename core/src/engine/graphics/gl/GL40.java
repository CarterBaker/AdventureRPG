package engine.graphics.gl;

public interface GL40 extends GL30 {

    /*
     * Engine GL40 interface. Extends GL30 with tessellation control and
     * evaluation shader stage support.
     */

    // Tessellation \\

    void glPatchParameteri(int pname, int value);
}