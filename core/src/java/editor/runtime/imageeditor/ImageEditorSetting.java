package editor.runtime.imageeditor;

import engine.input.Buttons;

public class ImageEditorSetting {

    /*
     * Constants used only by ImageEditorContext and the systems it owns — its
     * material and the uniforms it sets, its render order and the button its
     * tools paint with.
     */

    // Materials
    public static final String MATERIAL_IMAGE = "editor/ImageEditorMaterial";

    // Uniforms
    public static final String UNIFORM_IMAGE_TEXTURE = "u_imageTexture";
    public static final String UNIFORM_HAS_IMAGE = "u_hasImage";
    public static final String UNIFORM_IMAGE_EXTENT = "u_imageExtent";

    // Render Order
    public static final int DEPTH_IMAGE = 0;

    // Pointer
    public static final int BUTTON_PAINT = Buttons.LEFT;
}
