package com.internal.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.menupipeline.menu.MenuHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Discovers menu JSON files in scan(), processes one file per load() call —
 * each file may produce multiple MenuHandles — then resolves all deferred
 * element refs in onComplete() once every file has been processed.
 * On-demand: resolves the owning file from the menu name, loads it
 * immediately, then resolves deferred refs for that file right away.
 * Self-releases when the queue empties.
 */
class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private MenuManager menuManager;
    private InternalBuildSystem internalBuildSystem;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Reverse map — menu name → owning file resource name
    private Object2ObjectOpenHashMap<String, String> menuName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MENU_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.menuName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root,
                "Menu/element JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk menu directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void awake() {
        internalBuildSystem.init(root);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String filePath = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            for (MenuHandle handle : internalBuildSystem.processFile(file, filePath)) {
                menuName2ResourceName.put(handle.getName(), filePath);
                menuManager.addMenu(handle.getName(), handle);
            }
        } catch (RuntimeException e) {
            throwException("Failed to process menu file: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    protected void onComplete() {
        internalBuildSystem.resolveAllDeferredRefs();
    }

    // On-Demand Loading \\

    void request(String menuName) {

        // If we already tracked this menu name to a file, use it directly
        String resourceName = menuName2ResourceName.get(menuName);

        // Otherwise scan resourceName2File for the owning file —
        // menu names are fileName/menuId so the file stem is the prefix
        if (resourceName == null) {
            String prefix = menuName.contains("/")
                    ? menuName.substring(0, menuName.lastIndexOf('/'))
                    : menuName;
            for (String rn : resourceName2File.keySet()) {
                if (rn.endsWith(prefix) || rn.equals(prefix)) {
                    resourceName = rn;
                    break;
                }
            }
        }

        if (resourceName == null)
            throwException(
                    "On-demand menu load failed — no file found for menu: \"" + menuName + "\"");

        File file = resourceName2File.get(resourceName);

        request(file);

        // Deferred refs must resolve immediately — onComplete() may have
        // already fired or will fire only after all remaining queued files
        // are processed, which is too late for this synchronous request.
        internalBuildSystem.resolveAllDeferredRefs();
    }
}