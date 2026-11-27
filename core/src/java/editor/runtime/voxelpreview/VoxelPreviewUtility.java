package editor.runtime.voxelpreview;

import application.bootstrap.geometrypipeline.subvoxelmanager.SubVoxelManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import engine.root.EngineUtility;
import engine.util.mathematics.extras.Direction3Vector;

public final class VoxelPreviewUtility extends EngineUtility {

    /*
     * The one way an editor preview picks the tile a block is drawn with:
     * the block's item texture, the one its pieces and the item shaders take,
     * or its side face's tile where it has none.
     */

    // Tiles \\

    public static int resolveBlockTile(SubVoxelManager subVoxelManager, BlockHandle blockHandle) {
        return blockHandle.getItemTextureName() != null
                ? subVoxelManager.resolveTile(blockHandle.getItemTextureName())
                : blockHandle.getTextureForFace(Direction3Vector.NORTH);
    }
}
