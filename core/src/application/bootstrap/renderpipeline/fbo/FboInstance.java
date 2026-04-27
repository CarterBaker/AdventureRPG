package application.bootstrap.renderpipeline.fbo;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class FboInstance extends InstancePackage {

    private FboHandle handle;
    private MeshData blitMeshOverride;
    private MaterialInstance blitMaterialOverride;

    private FboManager fboManager;

    public void constructor(FboHandle handle) {
        this.handle = handle;
    }

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
    }

    public int getTextureId() {
        IntArrayList textures = handle.getTextures();
        return textures.isEmpty() ? 0 : textures.getInt(textures.size() - 1);
    }

    public void bind() {
        fboManager.bind(this);
    }

    public void unbind() {
        fboManager.unbind();
    }

    public void resize(int width, int height) {
        fboManager.resize(this, width, height);
    }

    public FboData getFboData() {
        return handle.getData();
    }

    public IntArrayList getFramebuffers() {
        return handle.getFramebuffers();
    }

    public IntArrayList getTextures() {
        return handle.getTextures();
    }

    public IntArrayList getDepthRenderbuffers() {
        return handle.getDepthRenderbuffers();
    }

    public int getWidth() {
        return handle.getWidth();
    }

    public int getHeight() {
        return handle.getHeight();
    }

    FboHandle getHandle() {
        return handle;
    }

    public MeshData getBlitMeshOverride() {
        return blitMeshOverride;
    }

    public MaterialInstance getBlitMaterialOverride() {
        return blitMaterialOverride;
    }

    public void setBlitOverride(MeshData mesh, MaterialInstance material) {
        this.blitMeshOverride = mesh;
        this.blitMaterialOverride = material;
    }
}
