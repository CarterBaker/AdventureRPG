package editor.bootstrap.charactereditorpipeline.charactereditormanager;

import java.awt.Color;
import java.io.File;
import java.awt.image.BufferedImage;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.brushpipeline.brush.BrushHandle;
import editor.bootstrap.brushpipeline.brushmanager.BrushManager;
import editor.bootstrap.brushpipeline.util.BrushUtility;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterMeshDocumentInstance;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintCanvasStruct;
import editor.bootstrap.charactereditorpipeline.charactermesh.CharacterPaintStampStruct;
import editor.bootstrap.imagepipeline.imagedocument.ImageDocumentInstance;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.bootstrap.toolpipeline.util.EditorTool;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class CharacterPaintBranch extends BranchPackage {

    /*
     * Paints a character's texture maps in Paint mode by projection, the way
     * texture painting tools do: every texel of every face the viewport
     * sees under the brush is found through the face's corner UVs, placed
     * on the window through the same corners, and painted by how much of the
     * brush tip covers it there, so a stroke follows the screen across UV
     * seams and a texel within the bleed of an island's edge is painted
     * with it. Each map of a texture opens as a canvas on its first stroke
     * — the image beside the texture, or its alias's default where none is
     * there — layered in image documents, so every stroke, fill and erase
     * is one undo step of the layer it touched, and each change is laid
     * flat and the texture baked again from its images and canvases into
     * the tile pools, as often as a stroke allows and whenever one ends. Brush paints the colour, Erase wears the base
     * back to the map's default and a layer back to nothing, Smooth blurs,
     * Fill covers the island of faces clicked and Pick takes the colour
     * under the pointer. Saving writes each changed map flat into its image
     * and bakes its texture's streamed files again; reverting puts the tile
     * back as its images hold it. Layers above the
     * base last the session and are laid into the image when it is saved.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private TextureManager textureManager;
    private ImageManager imageManager;
    private BrushManager brushManager;

    // Paint — the map painted, by its place in CHARACTER_PAINT_MAP_LABELS, and the colour, 0 to 1 per channel
    private int mapIndex;
    private float[] color;

    // Canvases — per texture one slot per map, those a stroke has begun on, and whether one is under way
    private Object2ObjectOpenHashMap<String, CharacterPaintCanvasStruct[]> textureName2Canvases;
    private ObjectArrayList<CharacterPaintCanvasStruct> strokeCanvases;
    private boolean stroking;

    // History — the layer each step painted, oldest first, across every canvas
    private ObjectArrayList<ImageDocumentInstance> undoLayers;
    private ObjectArrayList<ImageDocumentInstance> redoLayers;

    // Scratch
    private IntArrayList textureScratch;
    private IntArrayList islandFaces;
    private BooleanArrayList islandSeen;
    private IntArrayList vertexStarts;
    private IntArrayList vertexFaces;
    private IntArrayList vertexFill;
    private int[] channelSums;

    // Preview — the textures painted since they were last baked, and when that was
    private ObjectLinkedOpenHashSet<String> pendingPreviews;
    private TextureCompressionScratchStruct compressionScratch;
    private File textureRoot;
    private File bakeRoot;
    private long lastPreviewNanos;

    // Base \\

    @Override
    protected void create() {

        // Paint
        this.color = EditorSetting.CHARACTER_PAINT_COLOR_DEFAULT.clone();

        // Canvases
        this.textureName2Canvases = new Object2ObjectOpenHashMap<>();
        this.strokeCanvases = new ObjectArrayList<>();

        // History
        this.undoLayers = new ObjectArrayList<>();
        this.redoLayers = new ObjectArrayList<>();

        // Scratch
        this.textureScratch = new IntArrayList();
        this.islandFaces = new IntArrayList();
        this.islandSeen = new BooleanArrayList();
        this.vertexStarts = new IntArrayList();
        this.vertexFaces = new IntArrayList();
        this.vertexFill = new IntArrayList();
        this.channelSums = new int[EngineSetting.COLOR_CHANNEL_COUNT];

        // Preview
        this.pendingPreviews = new ObjectLinkedOpenHashSet<>();
        this.compressionScratch = new TextureCompressionScratchStruct();
        this.textureRoot = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.bakeRoot = new File(EngineSetting.BAKED_TEXTURE_PATH);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.textureManager = get(TextureManager.class);
        this.imageManager = get(ImageManager.class);
        this.brushManager = get(BrushManager.class);
    }

    // Update \\

    @Override
    protected void update() {
        flushPreviews(false);
    }

    // Canvases \\

    // The canvas of a texture's map being painted, opened from its image on first use
    private CharacterPaintCanvasStruct resolveCanvas(String textureName) {

        CharacterPaintCanvasStruct[] canvases = textureName2Canvases.get(textureName);

        if (canvases == null) {
            canvases = new CharacterPaintCanvasStruct[EditorSetting.CHARACTER_PAINT_MAP_LABELS.length];
            textureName2Canvases.put(textureName, canvases);
        }

        if (canvases[mapIndex] == null)
            canvases[mapIndex] = openCanvas(textureName, mapIndex);

        return canvases[mapIndex];
    }

    // The canvas the Properties tab shows: the active part's first texture under the map being painted
    CharacterPaintCanvasStruct resolveActiveCanvas(CharacterMeshDocumentInstance document) {

        if (document == null || document.getMesh().getTextureCount() == 0)
            return null;

        return resolveCanvas(document.getMesh().getTextureName(0));
    }

    private CharacterPaintCanvasStruct openCanvas(String textureName, int map) {

        TextureHandle textureHandle = textureManager.getTextureHandleFromTextureName(textureName);
        AliasStruct alias = textureManager.getAliasLibrary().findAlias(EditorSetting.CHARACTER_PAINT_MAP_ALIASES[map]);
        File file = new File(
                new File(EngineSetting.BLOCK_TEXTURE_PATH, textureHandle.getArrayName()),
                textureName.substring(textureName.lastIndexOf(EditorSetting.CHARACTER_PAINT_IMAGE_SEPARATOR) + 1)
                        + EditorSetting.CHARACTER_PAINT_SUFFIX_SEPARATOR
                        + EditorSetting.CHARACTER_PAINT_MAP_SUFFIXES[map]
                        + EditorSetting.CHARACTER_PAINT_EXTENSION_SEPARATOR
                        + EditorSetting.IMAGE_FORMAT);
        CharacterPaintCanvasStruct canvas = new CharacterPaintCanvasStruct(
                textureName,
                map,
                file,
                textureHandle.getTileID(),
                alias,
                textureHandle.getTileWidth(),
                textureHandle.getTileHeight(),
                toRGBA(alias.getAliasColor()));

        openLayer(canvas, loadBase(canvas), EditorSetting.CHARACTER_PAINT_LAYER_BASE);
        PixmapUtility.copyPixels(canvas.getLayer(0).getPixmap(), canvas.getComposite());

        return canvas;
    }

    // The map's image as its file holds it, or the map's default everywhere where there is none of the tile's size
    private Pixmap loadBase(CharacterPaintCanvasStruct canvas) {

        File file = canvas.getFile();

        if (file.isFile()) {

            Pixmap image = new Pixmap(file);

            if (image.getWidth() == canvas.getWidth() && image.getHeight() == canvas.getHeight())
                return image;

            image.dispose();
        }

        Pixmap blank = new Pixmap(canvas.getWidth(), canvas.getHeight(), Pixmap.Format.RGBA8888);

        for (int y = 0; y < canvas.getHeight(); y++)
            for (int x = 0; x < canvas.getWidth(); x++)
                blank.drawPixel(x, y, canvas.getDefaultColor());

        return blank;
    }

    // A layer of the canvas as an image document, laid flat into the composite whenever anything changes it
    private ImageDocumentInstance openLayer(CharacterPaintCanvasStruct canvas, Pixmap pixmap, String layerName) {

        String imageName = EditorSetting.CHARACTER_PAINT_IMAGE_PREFIX + canvas.getTextureName()
                + EditorSetting.CHARACTER_PAINT_IMAGE_SEPARATOR
                + EditorSetting.CHARACTER_PAINT_MAP_SUFFIXES[canvas.getMapIndex()]
                + EditorSetting.CHARACTER_PAINT_IMAGE_SEPARATOR + canvas.getNextLayerNumber();
        ImageDocumentInstance layer = imageManager.openImage(imageName, canvas.getFile(), pixmap, false);

        imageManager.addEditListener(imageName, region -> recomposite(
                canvas, region.getMinX(), region.getMinY(), region.getMaxX(), region.getMaxY()));
        canvas.addLayer(layer, layerName, 1f);

        return layer;
    }

    // Composite \\

    // A rectangle of the canvas laid flat, each layer over the base by its alpha and opacity, and queued for preview
    private void recomposite(CharacterPaintCanvasStruct canvas, int minX, int minY, int maxX, int maxY) {

        Pixmap composite = canvas.getComposite();

        for (int y = minY; y <= maxY; y++)
            for (int x = minX; x <= maxX; x++) {

                int flat = canvas.getLayer(0).getPixmap().getPixel(x, y);

                for (int l = 1; l < canvas.getLayerCount(); l++) {

                    int over = canvas.getLayer(l).getPixmap().getPixel(x, y);
                    float share = (over & EngineSetting.PACKED_COLOR_CHANNEL_MASK)
                            / EngineSetting.COLOR_CHANNEL_BYTE_MAX * canvas.getOpacity(l);

                    flat = mixColor(flat, over | EngineSetting.PACKED_COLOR_CHANNEL_MASK, share, false);
                }

                composite.drawPixel(x, y, flat);
            }

        upload(canvas);
    }

    // The texture a change touched, baked again and swapped into the pools once the preview is due
    private void upload(CharacterPaintCanvasStruct canvas) {
        pendingPreviews.add(canvas.getTextureName());
    }

    // Every texture painted since the last preview, baked from its images with its open canvases laid in; at most
    // once every preview interval while a stroke is under way, so a stroke stays smooth
    private void flushPreviews(boolean force) {

        long now = System.nanoTime();

        boolean early = now - lastPreviewNanos < EditorSetting.CHARACTER_PAINT_PREVIEW_NANOS;

        if (pendingPreviews.isEmpty() || (!force && early))
            return;

        for (String textureName : pendingPreviews)
            bakeTexture(textureName, false);

        pendingPreviews.clear();
        lastPreviewNanos = now;
    }

    // A texture baked from its source images, each map open for painting taken from its canvas instead; written to
    // the baked files when saved, so the game streams what was painted
    private void bakeTexture(String textureName, boolean write) {

        AliasLibraryStruct library = textureManager.getAliasLibrary();
        TextureSourceStruct source = TextureBakeUtility.collectTileSource(textureRoot, textureName, library);
        CharacterPaintCanvasStruct[] canvases = textureName2Canvases.get(textureName);
        ObjectArrayList<BufferedImage> images = new ObjectArrayList<>();
        ObjectArrayList<AliasStruct> aliases = new ObjectArrayList<>();

        for (int i = 0; i < source.getSourceCount(); i++) {

            CharacterPaintCanvasStruct canvas = findCanvas(canvases, source.getAlias(i));
            images.add(canvas != null
                    ? PixmapUtility.toBufferedImage(canvas.getComposite())
                    : TextureBakeUtility.readImage(source.getFile(i)));
            aliases.add(source.getAlias(i));
        }

        for (int m = 0; canvases != null && m < canvases.length; m++) {
            if (canvases[m] != null && !aliases.contains(canvases[m].getAlias())) {
                images.add(PixmapUtility.toBufferedImage(canvases[m].getComposite()));
                aliases.add(canvases[m].getAlias());
            }
        }

        TextureBakeStruct bake = TextureBakeUtility.bakeImages(
                textureName,
                images.toArray(new BufferedImage[0]),
                aliases.toArray(new AliasStruct[0]),
                library,
                write ? TextureBakeUtility.computeChecksum(source) : 0L,
                compressionScratch);

        if (write)
            TextureBakeUtility.writeBake(bakeRoot, bake);

        textureManager.replaceTileBake(textureManager.getTileIDFromTextureName(textureName), bake);
    }

    private CharacterPaintCanvasStruct findCanvas(CharacterPaintCanvasStruct[] canvases, AliasStruct alias) {

        for (int m = 0; canvases != null && m < canvases.length; m++)
            if (canvases[m] != null && canvases[m].getAlias() == alias)
                return canvases[m];

        return null;
    }

    // Strokes \\

    void beginStroke() {
        this.stroking = true;
        strokeCanvases.clear();
    }

    // One stamp: every texel of the faces under the brush painted by how much of the tip covers it on the window
    void stamp(
            CharacterMeshDocumentInstance document,
            CharacterPaintStampStruct stamp,
            EditorTool tool,
            float strength) {

        if (!stroking || stamp.isEmpty())
            return;

        RiggedMeshStruct mesh = document.getMesh();
        BrushHandle brush = brushManager.getActiveBrush();

        collectTextures(mesh, stamp);

        for (int t = 0; t < textureScratch.size(); t++) {

            int texture = textureScratch.getInt(t);
            CharacterPaintCanvasStruct canvas = resolveCanvas(mesh.getTextureName(texture));
            ImageDocumentInstance layer = beginCanvas(canvas);

            for (int i = 0; i < stamp.getFaceCount(); i++)
                if (mesh.getFaceTexture(stamp.getFace(i)) == texture)
                    paintFace(canvas, layer, mesh, stamp.getFace(i), stamp, i, brush, tool, strength);

            imageManager.flushPaint(layer);
        }
    }

    private void collectTextures(RiggedMeshStruct mesh, CharacterPaintStampStruct stamp) {

        textureScratch.clear();

        for (int i = 0; i < stamp.getFaceCount(); i++) {

            int texture = mesh.getFaceTexture(stamp.getFace(i));

            if (!textureScratch.contains(texture))
                textureScratch.add(texture);
        }
    }

    // The layer a stroke paints on a canvas, its undo step opened the first time the stroke reaches the canvas
    private ImageDocumentInstance beginCanvas(CharacterPaintCanvasStruct canvas) {

        ImageDocumentInstance layer = canvas.getLayer(canvas.getActiveLayer());

        if (!strokeCanvases.contains(canvas)) {
            strokeCanvases.add(canvas);
            imageManager.beginPaint(layer);
        }

        return layer;
    }

    // Every canvas the stroke reached closed as one undo step of the layer it painted
    void endStroke() {

        for (int i = 0; i < strokeCanvases.size(); i++) {

            CharacterPaintCanvasStruct canvas = strokeCanvases.get(i);
            ImageDocumentInstance layer = canvas.getLayer(canvas.getActiveLayer());
            boolean painted = !layer.getStepBeforeColors().isEmpty();

            imageManager.endStroke(layer);

            if (painted) {
                undoLayers.add(layer);
                redoLayers.clear();
            }
        }

        while (undoLayers.size() > EditorSetting.IMAGE_HISTORY_MAX_STEPS)
            undoLayers.remove(0);

        strokeCanvases.clear();
        this.stroking = false;
        flushPreviews(true);
        characterEditorManager.notifyChanged();
    }

    // Fill \\

    // The island of faces joined corner to corner around the face clicked, sharing its texture, covered whole
    void fill(CharacterMeshDocumentInstance document, int faceIndex, float strength) {

        RiggedMeshStruct mesh = document.getMesh();
        int texture = mesh.getFaceTexture(faceIndex);
        BrushHandle brush = brushManager.getActiveBrush();

        collectIsland(mesh, faceIndex, texture);
        beginStroke();

        CharacterPaintCanvasStruct canvas = resolveCanvas(mesh.getTextureName(texture));
        ImageDocumentInstance layer = beginCanvas(canvas);

        for (int i = 0; i < islandFaces.size(); i++)
            paintFace(canvas, layer, mesh, islandFaces.getInt(i), null, 0, brush, EditorTool.BRUSH, strength);

        imageManager.flushPaint(layer);
        endStroke();
    }

    private void collectIsland(RiggedMeshStruct mesh, int startFace, int texture) {

        indexVertexFaces(mesh);
        islandFaces.clear();
        islandSeen.clear();
        islandSeen.size(mesh.getFaceCount());
        islandFaces.add(startFace);
        islandSeen.set(startFace, true);

        for (int next = 0; next < islandFaces.size(); next++) {

            int face = islandFaces.getInt(next);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertex = mesh.getFaceVertex(face, c);

                for (int i = vertexStarts.getInt(vertex); i < vertexStarts.getInt(vertex + 1); i++) {

                    int neighbour = vertexFaces.getInt(i);

                    if (islandSeen.getBoolean(neighbour) || mesh.getFaceTexture(neighbour) != texture)
                        continue;

                    islandSeen.set(neighbour, true);
                    islandFaces.add(neighbour);
                }
            }
        }
    }

    // The faces meeting at each vertex, one run per vertex, counted then placed
    private void indexVertexFaces(RiggedMeshStruct mesh) {

        int vertexCount = mesh.getVertexCount();

        vertexStarts.clear();
        vertexStarts.size(vertexCount + 1);
        vertexFill.clear();
        vertexFill.size(vertexCount);

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int slot = mesh.getFaceVertex(f, c) + 1;
                vertexStarts.set(slot, vertexStarts.getInt(slot) + 1);
            }

        for (int v = 1; v <= vertexCount; v++)
            vertexStarts.set(v, vertexStarts.getInt(v) + vertexStarts.getInt(v - 1));

        vertexFaces.clear();
        vertexFaces.size(vertexStarts.getInt(vertexCount));

        for (int f = 0; f < mesh.getFaceCount(); f++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertex = mesh.getFaceVertex(f, c);
                int slot = vertexStarts.getInt(vertex) + vertexFill.getInt(vertex);

                vertexFaces.set(slot, f);
                vertexFill.set(vertex, vertexFill.getInt(vertex) + 1);
            }
    }

    // Pick \\

    // The colour the flattened map shows where a face is hit, at its UV there
    void pick(CharacterMeshDocumentInstance document, int faceIndex, float u, float v) {

        RiggedMeshStruct mesh = document.getMesh();
        CharacterPaintCanvasStruct canvas = resolveCanvas(mesh.getTextureName(mesh.getFaceTexture(faceIndex)));
        int x = Math.clamp((int) Math.floor(u * canvas.getWidth()), 0, canvas.getWidth() - 1);
        int y = Math.clamp((int) Math.floor((1f - v) * canvas.getHeight()), 0, canvas.getHeight() - 1);
        int picked = canvas.getComposite().getPixel(x, y);

        for (int channel = 0; channel < color.length; channel++)
            color[channel] = toChannel(picked, channel) / EngineSetting.COLOR_CHANNEL_BYTE_MAX;

        characterEditorManager.notifyChanged();
    }

    // Raster \\

    // Both triangles of a face, a stamp's coverage on the window weighing each texel, or every texel whole without one
    private void paintFace(
            CharacterPaintCanvasStruct canvas,
            ImageDocumentInstance layer,
            RiggedMeshStruct mesh,
            int faceIndex,
            CharacterPaintStampStruct stamp,
            int stampIndex,
            BrushHandle brush,
            EditorTool tool,
            float strength) {

        paintTriangle(canvas, layer, mesh, faceIndex, 0, 1, 2, stamp, stampIndex, brush, tool, strength);
        paintTriangle(canvas, layer, mesh, faceIndex, 0, 2, 3, stamp, stampIndex, brush, tool, strength);
    }

    private void paintTriangle(
            CharacterPaintCanvasStruct canvas,
            ImageDocumentInstance layer,
            RiggedMeshStruct mesh,
            int faceIndex,
            int a,
            int b,
            int c,
            CharacterPaintStampStruct stamp,
            int stampIndex,
            BrushHandle brush,
            EditorTool tool,
            float strength) {

        int width = canvas.getWidth();
        int height = canvas.getHeight();
        float ax = mesh.getFaceU(faceIndex, a) * width;
        float ay = (1f - mesh.getFaceV(faceIndex, a)) * height;
        float bx = mesh.getFaceU(faceIndex, b) * width;
        float by = (1f - mesh.getFaceV(faceIndex, b)) * height;
        float cx = mesh.getFaceU(faceIndex, c) * width;
        float cy = (1f - mesh.getFaceV(faceIndex, c)) * height;
        float area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);

        if (Math.abs(area) <= EditorSetting.CHARACTER_PAINT_AREA_EPSILON)
            return;

        float bleed = EditorSetting.CHARACTER_PAINT_BLEED_PIXELS / Math.abs(area);
        float reachA = -bleed * (float) Math.hypot(cx - bx, cy - by);
        float reachB = -bleed * (float) Math.hypot(ax - cx, ay - cy);
        float reachC = -bleed * (float) Math.hypot(bx - ax, by - ay);
        float margin = EditorSetting.CHARACTER_PAINT_BLEED_PIXELS;
        int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx)) - margin));
        int minY = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy)) - margin));
        int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx)) + margin));
        int maxY = Math.min(height - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy)) + margin));

        for (int py = minY; py <= maxY; py++)
            for (int px = minX; px <= maxX; px++) {

                float x = px + 0.5f;
                float y = py + 0.5f;
                float wa = ((bx - x) * (cy - y) - (cx - x) * (by - y)) / area;
                float wb = ((cx - x) * (ay - y) - (ax - x) * (cy - y)) / area;
                float wc = 1f - wa - wb;

                if (wa < reachA || wb < reachB || wc < reachC)
                    continue;

                float coverage = stamp == null ? 1f : BrushUtility.resolveCoverage(
                        brush,
                        wa * stamp.getCornerX(stampIndex, a) + wb * stamp.getCornerX(stampIndex, b)
                                + wc * stamp.getCornerX(stampIndex, c) - stamp.getCenterX(),
                        wa * stamp.getCornerY(stampIndex, a) + wb * stamp.getCornerY(stampIndex, b)
                                + wc * stamp.getCornerY(stampIndex, c) - stamp.getCenterY(),
                        stamp.getRadius());

                if (coverage > 0f)
                    paintTexel(canvas, layer, px, py, tool, coverage * strength);
            }
    }

    // One texel by the tool: Brush toward the colour, Erase back to the default or to nothing, Smooth toward its
    // neighbours; the base keeps its own alpha, which only its image sets
    private void paintTexel(
            CharacterPaintCanvasStruct canvas,
            ImageDocumentInstance layer,
            int x,
            int y,
            EditorTool tool,
            float amount) {

        int before = layer.getPixmap().getPixel(x, y);
        boolean base = canvas.getActiveLayer() == 0;
        int after = switch (tool) {
            case ERASE -> base
                    ? mixColor(before, canvas.getDefaultColor(), amount, false)
                    : mixColor(before, before & ~EngineSetting.PACKED_COLOR_CHANNEL_MASK, amount, true);
            case SMOOTH -> mixColor(before, averageTexel(layer, x, y), amount, !base);
            default -> mixColor(before, toRGBA(), amount, !base);
        };

        imageManager.paintPixel(layer, x, y, after);
    }

    private int averageTexel(ImageDocumentInstance layer, int x, int y) {

        int[] sums = channelSums;
        int count = 0;

        for (int channel = 0; channel < sums.length; channel++)
            sums[channel] = 0;

        for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {

                int sample = imageManager.pickColor(layer, x + dx, y + dy);

                for (int channel = 0; channel < sums.length; channel++)
                    sums[channel] += toChannel(sample, channel);

                count++;
            }

        int average = 0;

        for (int channel = 0; channel < sums.length; channel++)
            average |= Math.round(sums[channel] / (float) count) << toShift(channel);

        return average;
    }

    // Two RGBA8888 colours mixed a share of the way, alpha only where it is asked for
    private int mixColor(int from, int to, float share, boolean alpha) {

        int mixed = 0;
        float t = Math.clamp(share, 0f, 1f);

        for (int channel = 0; channel < EngineSetting.COLOR_CHANNEL_COUNT; channel++) {

            int a = toChannel(from, channel);
            int b = channel == EngineSetting.COLOR_CHANNEL_COUNT - 1 && !alpha ? a : toChannel(to, channel);

            mixed |= Math.round(a + (b - a) * t) << toShift(channel);
        }

        return mixed;
    }

    private int toChannel(int rgba, int channel) {
        return (rgba >>> toShift(channel)) & EngineSetting.PACKED_COLOR_CHANNEL_MASK;
    }

    private int toShift(int channel) {
        return (EngineSetting.COLOR_CHANNEL_COUNT - 1 - channel) * Byte.SIZE;
    }

    // The paint colour, whole, as RGBA8888
    private int toRGBA() {

        int rgba = EngineSetting.PACKED_COLOR_CHANNEL_MASK;

        for (int channel = 0; channel < color.length; channel++)
            rgba |= Math.round(Math.clamp(color[channel], 0f, 1f) * EngineSetting.COLOR_CHANNEL_BYTE_MAX)
                    << toShift(channel);

        return rgba;
    }

    private int toRGBA(Color source) {
        return source.getRed() << toShift(0) | source.getGreen() << toShift(1) | source.getBlue() << toShift(2)
                | source.getAlpha();
    }

    // Layers \\

    void addLayer(CharacterPaintCanvasStruct canvas) {

        if (canvas.getLayerCount() >= EditorSetting.CHARACTER_PAINT_MAX_LAYERS) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_PAINT_MESSAGE_LAYER_FULL);
            return;
        }

        String layerName = EditorSetting.CHARACTER_PAINT_LAYER_NAME + canvas.getNextLayerNumber();

        openLayer(canvas, new Pixmap(canvas.getWidth(), canvas.getHeight(), Pixmap.Format.RGBA8888), layerName);
        canvas.setActiveLayer(canvas.getLayerCount() - 1);
        canvas.markLayersChanged(true);
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_PAINT_MESSAGE_LAYER_ADDED + layerName);
    }

    // The layer painted removed with its history, the one below it painted next, the map laid flat again
    void removeLayer(CharacterPaintCanvasStruct canvas) {

        int layerIndex = canvas.getActiveLayer();

        if (layerIndex == 0) {
            characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_PAINT_MESSAGE_LAYER_BASE);
            return;
        }

        String layerName = canvas.getLayerName(layerIndex);

        closeLayer(canvas.removeLayer(layerIndex));
        canvas.setActiveLayer(layerIndex - 1);
        canvas.markLayersChanged(true);
        recomposite(canvas, 0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
        characterEditorManager.setStatusMessage(EditorSetting.CHARACTER_PAINT_MESSAGE_LAYER_REMOVED + layerName);
    }

    void stepLayer(CharacterPaintCanvasStruct canvas, int direction) {
        canvas.setActiveLayer(Math.floorMod(canvas.getActiveLayer() + direction, canvas.getLayerCount()));
        characterEditorManager.notifyChanged();
    }

    void setOpacity(CharacterPaintCanvasStruct canvas, float opacity) {

        if (canvas.getActiveLayer() == 0)
            return;

        canvas.setOpacity(canvas.getActiveLayer(), Math.clamp(opacity, 0f, 1f));
        canvas.markLayersChanged(true);
        recomposite(canvas, 0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
        characterEditorManager.notifyChanged();
    }

    private void closeLayer(ImageDocumentInstance layer) {

        undoLayers.removeIf(step -> step == layer);
        redoLayers.removeIf(step -> step == layer);

        imageManager.closeImage(layer.getImageName());
    }

    // History \\

    boolean undo() {
        return stepHistory(undoLayers, redoLayers, true);
    }

    boolean redo() {
        return stepHistory(redoLayers, undoLayers, false);
    }

    private boolean stepHistory(
            ObjectArrayList<ImageDocumentInstance> from,
            ObjectArrayList<ImageDocumentInstance> to,
            boolean back) {

        while (!from.isEmpty()) {

            ImageDocumentInstance layer = from.pop();

            if (back ? imageManager.undo(layer) : imageManager.redo(layer)) {
                to.push(layer);
                return true;
            }
        }

        return false;
    }

    // Files \\

    boolean isDirty() {

        for (CharacterPaintCanvasStruct[] canvases : textureName2Canvases.values())
            for (CharacterPaintCanvasStruct canvas : canvases)
                if (canvas != null && canvas.isDirty())
                    return true;

        return false;
    }

    // Every changed map laid flat into its image and its texture baked again from them; false when one could not
    // be written
    boolean save() {

        boolean saved = true;
        ObjectLinkedOpenHashSet<String> savedTextures = new ObjectLinkedOpenHashSet<>();

        for (CharacterPaintCanvasStruct[] canvases : textureName2Canvases.values())
            for (CharacterPaintCanvasStruct canvas : canvases) {

                if (canvas == null || !canvas.isDirty())
                    continue;

                if (!imageManager.writeImage(canvas.getFile(), canvas.getComposite())) {
                    saved = false;
                    continue;
                }

                for (int l = 0; l < canvas.getLayerCount(); l++)
                    canvas.getLayer(l).markSaved();

                canvas.markLayersChanged(false);
                savedTextures.add(canvas.getTextureName());
            }

        for (String textureName : savedTextures)
            bakeTexture(textureName, true);

        return saved;
    }

    // Every changed map put back as its image holds it, its tile baked from the images again, and its canvas let go
    void revert() {

        for (CharacterPaintCanvasStruct[] canvases : textureName2Canvases.values())
            for (int m = 0; m < canvases.length; m++) {

                CharacterPaintCanvasStruct canvas = canvases[m];

                if (canvas == null || !canvas.isDirty())
                    continue;

                Pixmap base = loadBase(canvas);

                PixmapUtility.copyPixels(base, canvas.getComposite());
                base.dispose();
                upload(canvas);

                for (int l = canvas.getLayerCount() - 1; l >= 0; l--)
                    closeLayer(canvas.getLayer(l));

                canvases[m] = null;
            }

        flushPreviews(true);
        characterEditorManager.notifyChanged();
    }

    // Status \\

    void appendStatus(StringBuilder status, CharacterPaintCanvasStruct canvas) {

        status.append(EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR)
                .append(EditorSetting.CHARACTER_PAINT_MAP_LABELS[mapIndex]);

        if (canvas != null && canvas.isDirty())
            status.append(EditorSetting.CHARACTER_VIEW_DIRTY_MARKER);
    }

    // Values \\

    void stepMap(int direction) {
        this.mapIndex = Math.floorMod(mapIndex + direction, EditorSetting.CHARACTER_PAINT_MAP_LABELS.length);
        characterEditorManager.notifyChanged();
    }

    void setColor(int channel, float value) {
        color[channel] = Math.clamp(value, 0f, 1f);
        characterEditorManager.notifyChanged();
    }

    int getMapIndex() {
        return mapIndex;
    }

    float getColor(int channel) {
        return color[channel];
    }

    boolean isStroking() {
        return stroking;
    }
}
