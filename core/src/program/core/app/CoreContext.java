package program.core.app;

import program.core.graphics.Graphics;
import program.core.input.Input;
import program.core.util.graphics.gl.GL20;
import program.core.util.graphics.gl.GL30;

public class CoreContext {

    /*
     * Global access point for platform-level singletons. Set once during
     * backend bootstrap and read-only for the rest of the session.
     */

    public static Application app;
    public static Graphics graphics;
    public static Input input;
    public static GL20 gl;
    public static GL20 gl20;
    public static GL30 gl30;
}