package editor.bootstrap.itemeditorpipeline.itemeditormanager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.rigmanager.RigManager;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelModelStruct;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelQuadListStruct;
import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureSourceStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasLibraryStruct;
import application.bootstrap.shaderpipeline.texturemanager.AliasStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureBakeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureCompressionScratchStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import editor.bootstrap.imagepipeline.imagemanager.ImageManager;
import editor.runtime.EditorSetting;
import engine.assets.image.Pixmap;
import engine.assets.image.PixmapUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemBakeBranch extends BranchPackage {

    /*
     * Bakes an item for the mesh tools, as a voxel modeller exports a mesh:
     * the cubes and walls merged greedily into quads, every quad unwrapped
     * flat as its own island laid in the block's own texel directions, the
     * islands packed into one square atlas at as many texels a sub-voxel as
     * the largest tier holds, and every map the parts' materials carry baked
     * into it from each part's own material, the way a material-ID bake
     * fills a mesh's maps before it is painted over. The maps are written as
     * a texture of the item pack the bake keeps them in, under the item's
     * name, baked and shown at once, so the Paint tab paints them as any
     * texture; the mesh is skinned wholly to the root of the one-bone item
     * rig every item shares, so no item needs a rig of its own. A wall is
     * one quad seen from both sides, so it becomes two faces sharing one
     * island. Baking again lays the maps out afresh from the cubes, as an
     * export does, painting over the last bake.
     */

    // Internal
    private SubVoxelManager subVoxelManager;
    private TextureManager textureManager;
    private ImageManager imageManager;
    private RigManager rigManager;

    // Directory
    private File textureRoot;
    private File bakeRoot;

    // Status — why the last bake could not be done
    private String refusal;

    // Scratch — the quads, each one's island in texels and where it was packed, the sub-voxel corners of the quad
    // worked, a cell a wall is looked for in, and the vertex at each corner position
    private SubVoxelQuadListStruct quads;
    private IntArrayList islandX;
    private IntArrayList islandY;
    private float[] corners;
    private int[] cornerCells;
    private int[] wallCell;
    private Long2IntOpenHashMap position2Vertex;
    private TextureCompressionScratchStruct compressionScratch;

    // Base \\

    @Override
    protected void create() {

        // Directory
        this.textureRoot = new File(EngineSetting.BLOCK_TEXTURE_PATH);
        this.bakeRoot = new File(EngineSetting.BAKED_TEXTURE_PATH);

        // Scratch
        this.quads = new SubVoxelQuadListStruct();
        this.islandX = new IntArrayList();
        this.islandY = new IntArrayList();
        this.corners = new float[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.AXIS_COUNT];
        this.cornerCells = new int[EngineSetting.QUAD_VERTEX_COUNT * EngineSetting.AXIS_COUNT];
        this.wallCell = new int[EngineSetting.AXIS_COUNT];
        this.position2Vertex = new Long2IntOpenHashMap();
        this.position2Vertex.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        this.compressionScratch = new TextureCompressionScratchStruct();
    }

    @Override
    protected void get() {
        this.subVoxelManager = get(SubVoxelManager.class);
        this.textureManager = get(TextureManager.class);
        this.imageManager = get(ImageManager.class);
        this.rigManager = get(RigManager.class);
    }

    // Bake \\

    // The item's mesh for the mesh tools, its maps baked and shown under the texture name; null, with the refusal
    // kept, when the item is empty or too large for the atlas
    RiggedMeshStruct bake(SubVoxelModelStruct model, String textureName) {

        subVoxelManager.meshModel(model, quads);

        if (quads.isEmpty()) {
            this.refusal = EditorSetting.ITEM_EDITOR_MESSAGE_EMPTY;
            return null;
        }

        int texels = EditorSetting.ITEM_BAKE_TEXELS_PER_CELL;
        int atlasSize = resolveAtlas(texels);

        while (atlasSize == EngineSetting.INDEX_NOT_FOUND && texels > 1) {
            texels /= 2;
            atlasSize = resolveAtlas(texels);
        }

        if (atlasSize == EngineSetting.INDEX_NOT_FOUND) {
            this.refusal = EditorSetting.ITEM_EDITOR_MESSAGE_BAKE_TOO_LARGE;
            return null;
        }

        if (!bakeMaps(model, textureName, texels, atlasSize))
            return null;

        return buildMesh(model, textureName, texels, atlasSize);
    }

    // Atlas \\

    // The smallest atlas side every island fits in at the texels a sub-voxel, each packed where it lies;
    // INDEX_NOT_FOUND when none does
    private int resolveAtlas(int texels) {

        int[] sizes = EditorSetting.ITEM_BAKE_ATLAS_SIZES;

        for (int i = 0; i < sizes.length; i++)
            if (pack(sizes[i], texels))
                return sizes[i];

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Every island packed into shelves, tallest first, the shelves filled left to right and stacked upward
    private boolean pack(int atlasSize, int texels) {

        int[] order = new int[quads.size()];

        for (int i = 0; i < order.length; i++)
            order[i] = i;

        IntArrays.quickSort(order, (a, b) -> Integer.compare(
                toIslandHeight(b, texels), toIslandHeight(a, texels)));

        islandX.size(quads.size());
        islandY.size(quads.size());

        int x = 0;
        int y = 0;
        int shelf = 0;

        for (int i = 0; i < order.length; i++) {

            int quad = order[i];
            int width = toIslandWidth(quad, texels);
            int height = toIslandHeight(quad, texels);

            if (x + width > atlasSize) {
                y += shelf;
                x = 0;
                shelf = 0;
            }

            if (x + width > atlasSize || y + height > atlasSize)
                return false;

            islandX.set(quad, x);
            islandY.set(quad, y);
            x += width;
            shelf = Math.max(shelf, height);
        }

        return true;
    }

    private int toIslandWidth(int quad, int texels) {
        return quads.getWidth(quad) * texels + 2 * EditorSetting.ITEM_BAKE_PADDING;
    }

    private int toIslandHeight(int quad, int texels) {
        return quads.getHeight(quad) * texels + 2 * EditorSetting.ITEM_BAKE_PADDING;
    }

    // Maps \\

    // Every map any part's material carries baked into the atlas, each island read from its part's own material and
    // the alias's neutral value where a material lacks the map, written as the texture's images, baked and shown
    private boolean bakeMaps(SubVoxelModelStruct model, String textureName, int texels, int atlasSize) {

        AliasLibraryStruct library = textureManager.getAliasLibrary();
        ObjectArrayList<AliasStruct> aliases = new ObjectArrayList<>();
        ObjectArrayList<String> suffixes = new ObjectArrayList<>();
        BufferedImage[][] partImages = new BufferedImage[model.getPartCount()][library.getAliasCount()];

        readMaterials(model, library, partImages, aliases, suffixes);

        BufferedImage[] images = new BufferedImage[aliases.size()];
        AliasStruct[] bakedAliases = new AliasStruct[aliases.size()];
        int[] pixels = new int[atlasSize * atlasSize];

        clearSources(textureName);

        for (int a = 0; a < aliases.size(); a++) {

            AliasStruct alias = aliases.get(a);
            int aliasIndex = findAliasIndex(library, alias);

            Arrays.fill(pixels, alias.getAliasColor().getRGB());

            for (int quad = 0; quad < quads.size(); quad++)
                paintIsland(quad, partImages[quads.getPart(quad)][aliasIndex], texels, atlasSize, pixels);

            images[a] = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
            images[a].setRGB(0, 0, atlasSize, atlasSize, pixels, 0, atlasSize);
            bakedAliases[a] = alias;

            if (!writeSource(textureName, suffixes.get(a), images[a]))
                return false;
        }

        TextureSourceStruct source = TextureBakeUtility.collectTileSource(textureRoot, textureName, library);
        TextureBakeStruct bake = TextureBakeUtility.bakeImages(
                textureName,
                images,
                bakedAliases,
                library,
                TextureBakeUtility.computeChecksum(source),
                compressionScratch);

        TextureBakeUtility.writeBake(bakeRoot, bake);
        textureManager.applyTileBake(bake);
        return true;
    }

    // Each part's material read map by map, and every alias any of them carries listed once in the library's order
    // with the suffix its image takes: the Paint tab's own for a map it paints, otherwise the material's
    private void readMaterials(
            SubVoxelModelStruct model,
            AliasLibraryStruct library,
            BufferedImage[][] partImages,
            ObjectArrayList<AliasStruct> aliases,
            ObjectArrayList<String> suffixes) {

        String[] aliasSuffixes = new String[library.getAliasCount()];

        for (int part = 0; part < model.getPartCount(); part++) {

            TextureSourceStruct source = TextureBakeUtility.collectTileSource(
                    textureRoot, model.getPart(part).getTextureName(), library);

            for (int i = 0; i < source.getSourceCount(); i++) {

                int aliasIndex = findAliasIndex(library, source.getAlias(i));

                partImages[part][aliasIndex] = TextureBakeUtility.readImage(source.getFile(i));

                if (aliasSuffixes[aliasIndex] == null)
                    aliasSuffixes[aliasIndex] = resolveSuffix(library, source.getAlias(i), source.getFile(i));
            }
        }

        for (int i = 0; i < aliasSuffixes.length; i++)
            if (aliasSuffixes[i] != null) {
                aliases.add(library.getAlias(i));
                suffixes.add(aliasSuffixes[i]);
            }
    }

    private static String resolveSuffix(AliasLibraryStruct library, AliasStruct alias, File sourceFile) {

        String[] paintSuffixes = EditorSetting.CHARACTER_PAINT_MAP_SUFFIXES;

        for (int i = 0; i < paintSuffixes.length; i++)
            if (library.findAlias(paintSuffixes[i]) == alias)
                return paintSuffixes[i];

        String[] parts = FileUtility.splitFileNameByUnderscore(FileUtility.getFileName(sourceFile));
        return parts[parts.length - 1];
    }

    private static int findAliasIndex(AliasLibraryStruct library, AliasStruct alias) {

        for (int i = 0; i < library.getAliasCount(); i++)
            if (library.getAlias(i) == alias)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // One island's texels read from the material at the block positions they cover, its padding repeating its edge;
    // a material without the map leaves the neutral value already laid
    private void paintIsland(int quad, BufferedImage material, int texels, int atlasSize, int[] pixels) {

        if (material == null)
            return;

        int face = quads.getFace(quad);
        int padding = EditorSetting.ITEM_BAKE_PADDING;
        int width = quads.getWidth(quad) * texels;
        int height = quads.getHeight(quad) * texels;
        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;

        resolveCornerCells(quad);

        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;

        for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {
            minU = Math.min(minU, toFaceU(face, corner));
            minV = Math.min(minV, toFaceV(face, corner));
        }

        for (int ty = 0; ty < height + 2 * padding; ty++) {
            for (int tx = 0; tx < width + 2 * padding; tx++) {

                float u = minU + (Math.max(0, Math.min(width - 1, tx - padding)) + 0.5f) / texels;
                float v = minV + (Math.max(0, Math.min(height - 1, ty - padding)) + 0.5f) / texels;
                int column = islandX.getInt(quad) + tx;
                int row = atlasSize - 1 - (islandY.getInt(quad) + ty);

                pixels[row * atlasSize + column] = sample(material, toRepeat(u / resolution), toRepeat(v / resolution));
            }
        }
    }

    // The material's texel at a block UV, v running up the image as the engine's UVs do
    private static int sample(BufferedImage material, float u, float v) {

        int x = Math.min(material.getWidth() - 1, (int) (u * material.getWidth()));
        int y = Math.min(material.getHeight() - 1, (int) ((1f - v) * material.getHeight()));

        return material.getRGB(x, y);
    }

    private static float toRepeat(float blocks) {
        return blocks - (float) Math.floor(blocks);
    }

    // Images \\

    // Every image a last bake left under the texture's name taken away, so no map of an older material stays
    private void clearSources(String textureName) {

        String instanceName = textureName.substring(
                textureName.lastIndexOf(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR) + 1);
        File packFolder = new File(textureRoot, EditorSetting.ITEM_BAKE_PACK);
        ObjectArrayList<File> files = FileUtility.collectFilesShallow(
                packFolder, EngineSetting.TEXTURE_FILE_EXTENSIONS);

        for (int i = 0; i < files.size(); i++)
            if (FileUtility.splitFileNameByUnderscore(FileUtility.getFileName(files.get(i)))[0].equals(instanceName)
                    && !files.get(i).delete())
                errorLog("Could not remove the old baked item image: " + files.get(i).getAbsolutePath());
    }

    private boolean writeSource(String textureName, String suffix, BufferedImage image) {

        String instanceName = textureName.substring(
                textureName.lastIndexOf(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR) + 1);
        File packFolder = new File(textureRoot, EditorSetting.ITEM_BAKE_PACK);
        File file = new File(packFolder, instanceName + EditorSetting.CHARACTER_PAINT_SUFFIX_SEPARATOR + suffix
                + EditorSetting.CHARACTER_PAINT_EXTENSION_SEPARATOR + EditorSetting.IMAGE_FORMAT);
        Pixmap pixmap = PixmapUtility.fromBufferedImage(image, false);

        packFolder.mkdirs();

        boolean written = imageManager.writeImage(file, pixmap);
        pixmap.dispose();

        if (!written)
            this.refusal = EditorSetting.ITEM_EDITOR_MESSAGE_NOT_WRITTEN + file.getPath();

        return written;
    }

    // Mesh \\

    // The quads as faces of a rigged mesh on the item rig, every corner one vertex wherever quads meet, each face
    // textured with the baked maps and carrying its island's UVs, every edge a seam, every vertex wholly the root's
    private RiggedMeshStruct buildMesh(SubVoxelModelStruct model, String textureName, int texels, int atlasSize) {

        RigHandle rigHandle = rigManager.getRigHandleFromRigName(EditorSetting.ITEM_BAKE_RIG);
        RiggedMeshStruct mesh = new RiggedMeshStruct(rigHandle);
        int rootBone = rigHandle.getBoneIndex(EditorSetting.ITEM_BAKE_ROOT_BONE);
        int texture = mesh.resolveTexture(textureName);
        int[] vertices = new int[EngineSetting.QUAD_VERTEX_COUNT];

        position2Vertex.clear();

        for (int quad = 0; quad < quads.size(); quad++) {

            resolveCornerCells(quad);

            for (int corner = 0; corner < vertices.length; corner++)
                vertices[corner] = resolveVertex(mesh, corner, rootBone);

            addFace(mesh, quad, vertices, texture, false, texels, atlasSize);

            if (isWall(model, quad))
                addFace(mesh, quad, vertices, texture, true, texels, atlasSize);
        }

        return mesh;
    }

    // One face over the quad's corners, wound the other way for a wall's back, its corners' UVs on the quad's island
    private void addFace(
            RiggedMeshStruct mesh,
            int quad,
            int[] vertices,
            int texture,
            boolean back,
            int texels,
            int atlasSize) {

        int face = quads.getFace(quad);
        int padding = EditorSetting.ITEM_BAKE_PADDING;
        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;

        for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {
            minU = Math.min(minU, toFaceU(face, corner));
            minV = Math.min(minV, toFaceV(face, corner));
        }

        int[] order = back ? EditorSetting.ITEM_BAKE_BACK_CORNERS : EditorSetting.ITEM_BAKE_FRONT_CORNERS;
        int faceIndex = mesh.addFace(
                vertices[order[0]], vertices[order[1]], vertices[order[2]], vertices[order[3]], texture);

        for (int corner = 0; corner < order.length; corner++) {

            float u = (islandX.getInt(quad) + padding + (toFaceU(face, order[corner]) - minU) * texels) / atlasSize;
            float v = (islandY.getInt(quad) + padding + (toFaceV(face, order[corner]) - minV) * texels) / atlasSize;

            mesh.setFaceUV(faceIndex, corner, u, v);
            mesh.setSeam(vertices[order[corner]], vertices[order[(corner + 1) % order.length]], true);
        }
    }

    // The vertex at the corner's position, made, skinned wholly to the root, the first time a quad meets it
    private int resolveVertex(RiggedMeshStruct mesh, int corner, int rootBone) {

        int offset = corner * EngineSetting.AXIS_COUNT;
        long key = toPositionKey(cornerCells[offset], cornerCells[offset + 1], cornerCells[offset + 2]);
        int vertex = position2Vertex.get(key);

        if (vertex != EngineSetting.INDEX_NOT_FOUND)
            return vertex;

        vertex = mesh.addVertex(corners[offset], corners[offset + 1], corners[offset + 2]);
        mesh.addInfluence(vertex, rootBone, 1f);
        position2Vertex.put(key, vertex);
        return vertex;
    }

    // A wall's quad lies where the model holds a wall, its plane on its face's axis
    private boolean isWall(SubVoxelModelStruct model, int quad) {

        int axis = subVoxelManager.getFaceAxis(quads.getFace(quad));
        int uAxis = (axis + 1) % EngineSetting.AXIS_COUNT;
        int vAxis = (axis + 2) % EngineSetting.AXIS_COUNT;

        wallCell[axis] = quads.getPlane(quad);
        wallCell[uAxis] = quads.getU(quad);
        wallCell[vAxis] = quads.getV(quad);

        return model.isWallInside(axis, wallCell[0], wallCell[1], wallCell[2])
                && model.hasWall(axis, wallCell[0], wallCell[1], wallCell[2]);
    }

    // Corners \\

    // The quad's corners in winding order, in blocks and in whole sub-voxels
    private void resolveCornerCells(int quad) {

        subVoxelManager.resolveQuadCorners(quads, quad, corners);

        for (int i = 0; i < corners.length; i++)
            cornerCells[i] = Math.round(corners[i] * EngineSetting.SUB_VOXEL_RESOLUTION);
    }

    // A corner's place across its face in sub-voxels, along the face's u and v as the item shader lays a block's
    // texels on that face, unbroken across blocks
    private float toFaceU(int face, int corner) {
        return EditorSetting.ITEM_BAKE_FACE_U_SIGN[face]
                * cornerCells[corner * EngineSetting.AXIS_COUNT + EditorSetting.ITEM_BAKE_FACE_U_AXIS[face]];
    }

    private float toFaceV(int face, int corner) {
        return EditorSetting.ITEM_BAKE_FACE_V_SIGN[face]
                * cornerCells[corner * EngineSetting.AXIS_COUNT + EditorSetting.ITEM_BAKE_FACE_V_AXIS[face]];
    }

    private static long toPositionKey(int x, int y, int z) {
        return ((long) x << EditorSetting.ITEM_BAKE_KEY_SHIFT_X)
                | ((long) y << EditorSetting.ITEM_BAKE_KEY_SHIFT_Y)
                | z;
    }

    // Accessible \\

    String getRefusal() {
        return refusal;
    }
}
