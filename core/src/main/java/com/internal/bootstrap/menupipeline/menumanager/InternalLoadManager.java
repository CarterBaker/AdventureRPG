package com.internal.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

class InternalLoadManager extends ManagerPackage {

    private File root;
    private MenuManager menuManager;
    private InternalBuildSystem internalBuildSystem;

    // Base \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.MENU_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Loading \\

    void loadMenuData() {

        List<File> files = collectFiles();
        internalBuildSystem.init(root);

        for (File file : files)
            processFile(file);

        internalBuildSystem.resolveAllDeferredRefs();
    }

    private List<File> collectFiles() {

        FileUtility.verifyDirectory(root,
                "Menu/element JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS
                            .contains(FileUtility.getExtension(f)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throwException("Failed to walk directory: " + root.getAbsolutePath(), e);
            return null;
        }
    }

    private void processFile(File file) {

        String filePath = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            // One file can produce multiple MenuHandles — one per declared menu
            for (MenuHandle handle : internalBuildSystem.processFile(file, filePath))
                menuManager.addMenu(handle.getName(), handle);
        } catch (RuntimeException e) {
            throwException("Failed to process file: " + file.getAbsolutePath(), e);
        }
    }
}