package editor.bootstrap.imagepipeline.imagedocument;

import java.io.File;

import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import editor.bootstrap.imagepipeline.imagehistory.ImageHistoryStruct;
import editor.bootstrap.imagepipeline.imageregion.ImageRegionStruct;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector3Double;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ImageDocumentInstance extends InstancePackage {

    /*
     * One image open for editing: its name, the file it saves to, and the
     * pixels it edits in place — its owner's own Pixmap, so whatever reads
     * them sees every change at once — and whether its edges wrap, or, for a
     * globe's image, whether its pixels are cube map faces side by side, a
     * pixel past a face's edge being the one on the face beyond, read about
     * the face an edit is anchored on. It holds
     * its GPU texture with the region still to upload, the stroke being
     * drawn, the undo step being recorded and its undo and redo history. The
     * revision moves on every change; dirty marks changes not yet saved.
     */

    // Identity
    private String imageName;
    private File file;

    // Pixels
    private Pixmap pixmap;
    private boolean wrapping;

    // Cube — the side of one face, none for a flat image, the face edits are anchored on and scratch to fold with
    private int cubeFaceTexels;
    private int anchorFace;
    private GlobeTexelStruct cubeTexel;
    private Vector3Double cubeDirection;

    // GPU
    private int texture;
    private boolean textureRequested;
    private ImageRegionStruct uploadRegion;

    // Stroke
    private boolean stroking;
    private int strokeX;
    private int strokeY;

    // History
    private Int2IntOpenHashMap stepBeforeColors;
    private ImageRegionStruct stepRegion;
    private ObjectArrayList<ImageHistoryStruct> undoSteps;
    private ObjectArrayList<ImageHistoryStruct> redoSteps;
    private long historyPixels;

    // State
    private boolean dirty;
    private int revision;

    // Internal \\

    @Override
    protected void create() {

        // GPU
        this.uploadRegion = new ImageRegionStruct();

        // Cube
        this.cubeTexel = new GlobeTexelStruct();
        this.cubeDirection = new Vector3Double();

        // History
        this.stepBeforeColors = new Int2IntOpenHashMap();
        this.stepRegion = new ImageRegionStruct();
        this.undoSteps = new ObjectArrayList<>();
        this.redoSteps = new ObjectArrayList<>();
    }

    // Constructor \\

    public void constructor(String imageName, File file, Pixmap pixmap, boolean wrapping) {

        // Identity
        this.imageName = imageName;
        this.file = file;

        // Pixels
        this.pixmap = pixmap;
        this.wrapping = wrapping;
        this.cubeFaceTexels = 0;
        this.anchorFace = 0;
    }

    // Cube \\

    // The image read as cube map faces side by side, each faceTexels square, its edges joined across the cube
    public void setCubeFaces(int faceTexels) {
        this.cubeFaceTexels = faceTexels;
        this.wrapping = false;
    }

    // Edits anchored on the face holding an image column, a column past either end held to the faces there are
    public void anchorCube(int imageX) {

        if (cubeFaceTexels > 0)
            this.anchorFace = Math.max(0, Math.min(EngineSetting.GLOBE_FACE_COUNT - 1,
                    Math.floorDiv(imageX, cubeFaceTexels)));
    }

    // GPU \\

    public void requestTexture() {
        this.textureRequested = true;
    }

    public void setTexture(int texture) {
        this.texture = texture;
    }

    public void queueUpload(ImageRegionStruct region) {
        uploadRegion.include(region);
    }

    // Stroke \\

    public void beginStroke(int x, int y) {
        this.stroking = true;
        moveStroke(x, y);
    }

    public void moveStroke(int x, int y) {
        this.strokeX = x;
        this.strokeY = y;
    }

    public void endStroke() {
        this.stroking = false;
    }

    // History \\

    public void adjustHistoryPixels(long delta) {
        this.historyPixels += delta;
    }

    // State \\

    public void markEdited() {

        this.dirty = true;
        this.revision++;
    }

    public void markSaved() {

        this.dirty = false;
        this.revision++;
    }

    // Accessible \\

    public String getImageName() {
        return imageName;
    }

    public File getFile() {
        return file;
    }

    public Pixmap getPixmap() {
        return pixmap;
    }

    public int getWidth() {
        return pixmap.getWidth();
    }

    public int getHeight() {
        return pixmap.getHeight();
    }

    public boolean isWrapping() {
        return wrapping;
    }

    public boolean isCube() {
        return cubeFaceTexels > 0;
    }

    public int getCubeFaceTexels() {
        return cubeFaceTexels;
    }

    public int getAnchorFace() {
        return anchorFace;
    }

    public GlobeTexelStruct getCubeTexel() {
        return cubeTexel;
    }

    public Vector3Double getCubeDirection() {
        return cubeDirection;
    }

    public int getTexture() {
        return texture;
    }

    public boolean isTextureRequested() {
        return textureRequested;
    }

    public ImageRegionStruct getUploadRegion() {
        return uploadRegion;
    }

    public boolean isStroking() {
        return stroking;
    }

    public int getStrokeX() {
        return strokeX;
    }

    public int getStrokeY() {
        return strokeY;
    }

    public Int2IntOpenHashMap getStepBeforeColors() {
        return stepBeforeColors;
    }

    public ImageRegionStruct getStepRegion() {
        return stepRegion;
    }

    public ObjectArrayList<ImageHistoryStruct> getUndoSteps() {
        return undoSteps;
    }

    public ObjectArrayList<ImageHistoryStruct> getRedoSteps() {
        return redoSteps;
    }

    public long getHistoryPixels() {
        return historyPixels;
    }

    public boolean isDirty() {
        return dirty;
    }

    public int getRevision() {
        return revision;
    }
}
