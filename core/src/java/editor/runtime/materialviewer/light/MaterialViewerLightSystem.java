package editor.runtime.materialviewer.light;

import application.kernel.inputpipeline.input.RawInputHandle;
import editor.bootstrap.toolpipeline.toolmanager.ToolManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.util.CharacterPoseUtility;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.materialviewer.MaterialViewerSetting;
import editor.runtime.viewcamera.ViewCameraSystem;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class MaterialViewerLightSystem extends SystemPackage {

    /*
     * The Light tool: a press on the shape takes the light, and dragging
     * turns it about the shape, across the window turning it round the
     * world's up and along the window tilting it about the camera's right,
     * as a material preview's light is turned by hand. A press beside the
     * shape is left to the view camera.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private ViewCameraSystem viewCameraSystem;
    private ToolManager toolManager;

    // Light — the way it shines, world space, unit length
    private Vector3 lightDirection;

    // Drag
    private boolean dragging;
    private float dragX;
    private float dragY;

    // Scratch
    private Vector3 rayOrigin;
    private Vector3 rayDirection;

    // Base \\

    @Override
    protected void create() {

        // Light
        this.lightDirection = new Vector3(
                MaterialViewerSetting.LIGHT_DIRECTION_DEFAULT[0],
                MaterialViewerSetting.LIGHT_DIRECTION_DEFAULT[1],
                MaterialViewerSetting.LIGHT_DIRECTION_DEFAULT[2]).normalize();

        // Scratch
        this.rayOrigin = new Vector3();
        this.rayDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
        this.toolManager = get(ToolManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        if (dragging) {
            updateDrag(rawInput);
            return;
        }

        if (toolManager.getTool(ToolCanvas.MATERIAL_VIEWER) != EditorTool.LIGHT
                || !editorInputSystem.isPointerActive()
                || viewCameraSystem.isDragging()
                || !editorInputSystem.isClicked(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)
                || !hitsShape(rawInput.getMouseX(), rawInput.getMouseY()))
            return;

        editorInputSystem.claim(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT);
        this.dragging = true;
        this.dragX = rawInput.getMouseX();
        this.dragY = rawInput.getMouseY();
    }

    // The light turned by how far the pointer moved since the last frame, until the button is let go
    private void updateDrag(RawInputHandle rawInput) {

        if (!rawInput.isButtonHeld(EditorSetting.CHARACTER_VIEW_BUTTON_SELECT)) {
            this.dragging = false;
            return;
        }

        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();
        Vector3 right = viewCameraSystem.getRight();

        turn(0f, 1f, 0f, (mouseX - dragX) * MaterialViewerSetting.LIGHT_TURN_PER_PIXEL);
        turn(right.x, right.y, right.z, (dragY - mouseY) * MaterialViewerSetting.LIGHT_TURN_PER_PIXEL);

        this.dragX = mouseX;
        this.dragY = mouseY;
    }

    // The light turned about a unit axis by an angle, in radians, kept unit length
    private void turn(float ax, float ay, float az, float angle) {

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float x = lightDirection.x;
        float y = lightDirection.y;
        float z = lightDirection.z;
        float along = (ax * x + ay * y + az * z) * (1f - cos);

        lightDirection.set(
                x * cos + (ay * z - az * y) * sin + ax * along,
                y * cos + (az * x - ax * z) * sin + ay * along,
                z * cos + (ax * y - ay * x) * sin + az * along).normalize();
    }

    // Whether the ray through the pixel meets the sphere every shape fits
    private boolean hitsShape(float mouseX, float mouseY) {

        CharacterPoseUtility.resolveRay(context.getWindow(), mouseX, mouseY, rayOrigin, rayDirection);

        float radius = MaterialViewerSetting.SHAPE_RADIUS;
        float along = -(rayOrigin.x * rayDirection.x + rayOrigin.y * rayDirection.y + rayOrigin.z * rayDirection.z);
        float nearX = rayOrigin.x + rayDirection.x * along;
        float nearY = rayOrigin.y + rayDirection.y * along;
        float nearZ = rayOrigin.z + rayDirection.z * along;

        return along > 0f && nearX * nearX + nearY * nearY + nearZ * nearZ <= radius * radius;
    }

    // Accessible \\

    public Vector3 getLightDirection() {
        return lightDirection;
    }
}
