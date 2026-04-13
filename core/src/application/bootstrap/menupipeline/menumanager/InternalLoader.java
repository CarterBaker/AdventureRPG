package application.bootstrap.menupipeline.menumanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.menupipeline.menu.MenuHandle;
import engine.root.LoaderPackage;
import engine.settings.EngineSetting;
import engine.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Discovers menu JSON files in scan(), processes one file per load() call —
     * each file may produce multiple MenuHandles — then resolves all deferred
     * element refs in onComplete() once every file has been processed.
     * On-demand: resolves the owning file from the menu name, loads it immediately,
     * then resolves deferred refs for that file.
     */

    // Internal
    private File root;
    private MenuManager menuManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> menuName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MENU_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.menuName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root,
                "Menu JSON directory not found: " + root.getAbsolutePath());

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
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
    }

    @Override
    protected void awake() {
        internalBuilder.init(root);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String filePath = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            for (MenuHandle handle : internalBuilder.processFile(file, filePath)) {
                menuName2ResourceName.put(handle.getName(), filePath);
                menuManager.addMenu(handle.getName(), handle);
            }
        } catch (RuntimeException e) {
            throwException("Failed to process menu file: " + file.getAbsolutePath(), e);
        }
    }

    @Override
    protected void onComplete() {
        internalBuilder.resolveAllDeferredRefs();
    }

    // On-Demand \\

    void request(String menuName) {

        String resourceName = menuName2ResourceName.get(menuName);

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
            throwException("On-demand menu load failed — no file found for: \"" + menuName + "\"");

        request(resourceName2File.get(resourceName));
        internalBuilder.resolveAllDeferredRefs();
    }
}