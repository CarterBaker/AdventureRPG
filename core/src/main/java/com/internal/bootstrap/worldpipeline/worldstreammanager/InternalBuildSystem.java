package com.internal.bootstrap.worldpipeline.worldstreammanager;

import com.badlogic.gdx.graphics.Pixmap;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.mathematics.vectors.Vector2Int;
import com.badlogic.gdx.Gdx;
import java.io.File;

class InternalBuildSystem extends SystemPackage {

    WorldHandle buildWorld(File root, File file, int worldID) {

        String worldName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        Pixmap world = new Pixmap(Gdx.files.internal(file.getPath()));
        Vector2Int worldScale = calculateWorldScale(world);

        WorldHandle worldHandle = create(WorldHandle.class);

        worldHandle.constructor(
                worldName,
                worldID,
                world,
                worldScale);

        return worldHandle;
    }

    private Vector2Int calculateWorldScale(Pixmap pixMap) {
        int width = pixMap.getWidth();
        int height = pixMap.getHeight();
        int chunksPerPixel = EngineSetting.CHUNKS_PER_PIXEL;
        int chunkSize = EngineSetting.CHUNK_SIZE;

        int worldWidth = width * chunksPerPixel * chunkSize;
        int worldHeight = height * chunksPerPixel * chunkSize;

        return new Vector2Int(worldWidth, worldHeight);
    }
}