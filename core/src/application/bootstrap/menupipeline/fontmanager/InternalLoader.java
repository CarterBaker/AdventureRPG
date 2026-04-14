package application.bootstrap.menupipeline.fontmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.menupipeline.fonts.FontHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the fonts directory for TTF/OTF files and loads each one into
     * FontManager via InternalBuilder. Font name is derived from the file stem.
     * No JSON config — size, material, and charset fall back to EngineSetting
     * defaults. Supports on-demand loading by font name to file resolution.
     */

    // Internal
    private File root;
    private FontManager fontManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> fontName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.FONT_PATH);
        this.fontName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Font directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.FONT_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String name = FileUtility.getFileName(file);
                        fontName2File.put(name, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk font directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.fontManager = get(FontManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String name = FileUtility.getFileName(file);

        try {
            FontHandle handle = internalBuilder.build(name, file);
            fontManager.addFont(name, handle);
        } catch (RuntimeException e) {
            throwException("Failed to load font: " + file.getAbsolutePath(), e);
        }
    }

    // On-Demand \\

    void request(String fontName) {

        File file = fontName2File.get(fontName);

        if (file == null)
            throwException("On-demand font load failed — not found in scan registry: \"" + fontName + "\"");

        request(file);
    }
}