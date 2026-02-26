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

    // Internal
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

        List<File> files = collectMenuFiles();

        internalBuildSystem.init();

        // Pass 1: parse and assemble all menus — real elements registered, forward refs
        // deferred
        for (File file : files)
            processMenuFile(file);

        // Pass 2: resolve all deferred cross-menu element references
        internalBuildSystem.resolveAllDeferredRefs();
    }

    private List<File> collectMenuFiles() {

        FileUtility.verifyDirectory(root,
                "Menu JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS
                            .contains(FileUtility.getExtension(f)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throwException("Failed to walk menu directory: " + root.getAbsolutePath(), e);
            return null;
        }
    }

    private void processMenuFile(File file) {

        String menuName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            MenuHandle menuHandle = internalBuildSystem.parseAndAssemble(file, menuName);
            if (menuHandle != null)
                menuManager.addMenu(menuName, menuHandle);
        } catch (RuntimeException e) {
            throwException("Failed to build menu: " + file.getAbsolutePath(), e);
        }
    }
}