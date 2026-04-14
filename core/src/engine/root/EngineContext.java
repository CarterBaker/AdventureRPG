package engine.root;

import engine.util.display.Display;
import engine.util.graphics.gl.GL20;
import engine.util.graphics.gl.GL30;
import engine.util.input.Input;

public class EngineContext {

    /*
     * Global access point for platform-level singletons. Set once during
     * backend bootstrap and read-only for the rest of the session.
     */

    // Platform
    public static Display graphics;
    public static Input input;
    public static GL20 gl20;
    public static GL30 gl30;
}