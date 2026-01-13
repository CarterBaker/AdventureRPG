package com.internal.bootstrap.worldpipeline.blockmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalLoadManager extends ManagerPackage {

    // Internal
    private BlockManager blockManager;
    private InternalBuildSystem internalBuildSystem;
    private File root;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.root = new File(EngineSetting.BLOCK_JSON_PATH);
    }

    @Override
    protected void get() {

        // Internal
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void release() {

        // Internal
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Load \\

    void loadBlocks() {

        FileUtility.verifyDirectory(root, "[BlockManager] The root folder could not be verified");

        Path rootPath = root.toPath();

        try (var stream = Files.walk(rootPath)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> FileUtility.hasExtension(path.toFile(), EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(path -> processJsonFile(path.toFile()));
        }

        catch (IOException e) {
            throwException("BlockManager failed to load one or more files: ", e);
        }
    }

    private void processJsonFile(File jsonFile) {

        ObjectArrayList<BlockHandle> blocks = internalBuildSystem.compileBlocks(jsonFile);

        for (int i = 0; i < blocks.size(); i++)
            blockManager.addBlock(blocks.get(i));
    }
}