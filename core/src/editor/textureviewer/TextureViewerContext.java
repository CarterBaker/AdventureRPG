package editor.textureviewer;

import editor.textureviewer.atlas.TextureViewerAtlasSystem;
import editor.textureviewer.menu.TextureViewerMenuSystem;
import editor.textureviewer.render.TextureViewerRenderSystem;
import editor.textureviewer.select.TextureViewerSelectSystem;
import engine.editor.EditorInputSystem;
import engine.root.ContextPackage;

public class TextureViewerContext extends ContextPackage {

    /*
     * Editor tab showing the atlas the game assembles for item textures, with
     * every source image outlined. Clicking an image makes it the item
     * editor's brush, so Place and Paint build with that texture.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private TextureViewerAtlasSystem textureViewerAtlasSystem;
    private TextureViewerSelectSystem textureViewerSelectSystem;
    private TextureViewerRenderSystem textureViewerRenderSystem;
    private TextureViewerMenuSystem textureViewerMenuSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.textureViewerAtlasSystem = create(TextureViewerAtlasSystem.class);
        this.textureViewerSelectSystem = create(TextureViewerSelectSystem.class);
        this.textureViewerRenderSystem = create(TextureViewerRenderSystem.class);
        this.textureViewerMenuSystem = create(TextureViewerMenuSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }
}
