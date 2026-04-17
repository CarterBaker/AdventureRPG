package engine.root;

import engine.graphics.display.Display;
import engine.graphics.gl.GL20;
import engine.graphics.gl.GL30;
import engine.input.Input;

public class EngineContext {

    /*
     * Global access point for platform-level singletons. Set once during
     * backend bootstrap and read-only for the rest of the session.
     */

    // Platform
    public static Display display;
    public static Input input;
    public static GL20 gl20;
    public static GL30 gl30;
}