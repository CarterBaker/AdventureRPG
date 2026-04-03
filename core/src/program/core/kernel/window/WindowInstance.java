package program.core.kernel.window;

import program.core.app.ApplicationListener;
import program.core.app.Screen;
import program.bootstrap.geometrypipeline.vaomanager.VAOManager;
import program.bootstrap.renderpipeline.rendermanager.RenderManager;
import program.bootstrap.renderpipeline.rendermanager.RenderQueueHandle;
import program.core.engine.ContextPackage;
import program.core.engine.InstancePackage;
import program.core.kernel.windowmanager.WindowManager;
import program.core.util.camera.CameraInstance;
import program.core.util.camera.OrthographicCameraInstance;

public class WindowInstance extends InstancePackage implements Screen, ApplicationListener {

    /*
     * Runtime window wrapper. Owns WindowData, RenderQueueHandle, and both
     * cameras. WindowData and cameras are assigned in constructor(), while
     * render queue setup occurs in awake().
     */

    // Data
    private WindowData windowData;
    private long nativeHandle;

    // Render Queue
    private RenderQueueHandle renderQueueHandle;

    // Context
    private ContextPackage context;
    private Class<? extends ContextPackage> pendingContextType;

    // Cameras
    private CameraInstance activeCamera;
    private OrthographicCameraInstance orthoCamera;

    // Internal
    private RenderManager renderManager;
    private VAOManager vaoManager;
    private WindowManager windowManager;

    // Internal \\

    public void constructor(WindowData windowData) {
        this.windowData = windowData;

        // Cameras
        this.activeCamera = internal.createCamera(
                internal.settings.FOV,
                windowData.getWidth(),
                windowData.getHeight());

        this.orthoCamera = internal.createOrthographicCamera(
                windowData.getWidth(),
                windowData.getHeight());
    }

    @Override
    protected void get() {

        // Internal
        this.renderManager = get(RenderManager.class);
        this.vaoManager = get(VAOManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {

        // Render Queue
        this.renderQueueHandle = create(RenderQueueHandle.class);
        this.renderQueueHandle.constructor();

    }

    // ApplicationListener — Detached Window Path \\

    @Override
    public void create() {
        if (!hasContext() && pendingContextType != null) {
            internal.createContext(pendingContextType, this);
            pendingContextType = null;
        }
    }

    @Override
    public void render() {
    }

    @Override
    public void resize(int width, int height) {
        windowData.setWidth(width);
        windowData.setHeight(height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        vaoManager.removeWindowVAOs(getWindowID());
        renderManager.removeWindowResources(this);

        if (context != null)
            internal.destroyContext(context);

        windowManager.removeWindow(this);
    }

    // Screen — Main Window Path \\

    @Override
    public void render(float delta) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    // Context \\

    public ContextPackage getContext() {
        return context;
    }

    public void setContext(ContextPackage context) {
        this.context = context;
    }

    public boolean hasContext() {
        return context != null;
    }

    public void setPendingContextType(Class<? extends ContextPackage> pendingContextType) {
        this.pendingContextType = pendingContextType;
    }

    // Native Handle \\

    public long getNativeHandle() {
        return nativeHandle;
    }

    public void setNativeHandle(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    public boolean hasNativeHandle() {
        return nativeHandle != 0L;
    }

    // Cameras \\

    public CameraInstance getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(CameraInstance activeCamera) {
        this.activeCamera = activeCamera;
    }

    public OrthographicCameraInstance getOrthoCamera() {
        return orthoCamera;
    }

    public void setOrthoCamera(OrthographicCameraInstance orthoCamera) {
        this.orthoCamera = orthoCamera;
    }

    // Accessible \\

    public WindowData getWindowData() {
        return windowData;
    }

    public RenderQueueHandle getRenderQueueHandle() {
        return renderQueueHandle;
    }

    public int getWindowID() {
        return windowData.getWindowID();
    }

    public int getWidth() {
        return windowData.getWidth();
    }

    public int getHeight() {
        return windowData.getHeight();
    }

    public String getTitle() {
        return windowData.getTitle();
    }
}
