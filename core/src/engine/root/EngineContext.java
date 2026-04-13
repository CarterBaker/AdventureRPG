package engine.root;

import engine.util.graphics.GL20;
import engine.util.graphics.GL30;
import engine.util.graphics.Graphics;
import engine.util.input.Input;

public class EngineContext {

    /*
     * Global access point for platform-level singletons. Set once during
     * backend bootstrap and read-only for the rest of the session.
     */

    // Platform
    public static Graphics graphics;
    public static Input input;
    public static GL20 gl;
    public static GL20 gl20;
    public static GL30 gl30;
}