package application.bootstrap.contentpipeline.contentmanager;

import java.io.File;

import application.bootstrap.contentpipeline.content.ContentHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class ContentLoader extends LoaderPackage {

    /*
     * Scans the content ARPG directory and loads every content into
     * ContentManager. Supports on-demand loading by content name, and
     * requestAll() so ContentManager can complete its palette, and the table
     * the surface shader reads, before the first chunk generates. The path
     * below the directory, without its extension, is the content's name.
     */

    // Internal
    private File root;
    private ContentManager contentManager;
    private ContentBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> contentName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(ContentBuilder.class);
    }

    @Override
    protected void get() {
        this.contentManager = get(ContentManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.CONTENT_PATH);
        this.contentName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Content root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String contentName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            contentName2File.put(contentName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String contentName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ContentHandle contentHandle = internalBuilder.build(file, contentName);

        if (contentHandle == null)
            throwException("Failed to build content from: " + file.getAbsolutePath());

        contentManager.addContentHandle(contentHandle);
    }

    // On-Demand \\

    void request(String contentName) {

        if (contentManager.hasContent(contentName))
            return;

        File file = contentName2File.get(contentName);

        if (file == null)
            throwException("On-demand content load failed — no file found for: \"" + contentName + "\"");

        request(file);
    }
}
