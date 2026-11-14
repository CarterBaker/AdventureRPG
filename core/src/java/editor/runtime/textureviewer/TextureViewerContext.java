package editor.runtime.textureviewer;

import editor.runtime.input.EditorInputSystem;
import editor.runtime.textureviewer.atlas.TextureViewerAtlasSystem;
import editor.runtime.textureviewer.menu.TextureViewerMenuSystem;
import editor.runtime.textureviewer.render.TextureViewerRenderSystem;
import editor.runtime.textureviewer.select.TextureViewerSelectSystem;
import engine.root.ContextPackage;

public class TextureViewerContext extends ContextPackage {

    /*
     * Editor tab showing every item texture as a grid of its streamed tiles,
     * each outlined. Clicking a tile makes it the item editor's brush, so
     * Place and Paint build with that texture.
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
