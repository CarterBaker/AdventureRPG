package application.bootstrap.renderpipeline.fbomanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.renderpipeline.fbo.FboData;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    private InternalBuilder internalBuilder;
    private FboManager fboManager;

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.fboManager = get(FboManager.class);
    }

    @Override
    protected void scan() {
        File root = new File(EngineSetting.FBO_CATALOG_JSON_PATH);
        FileUtility.verifyDirectory(root, "FBO directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
                    .forEach(fileQueue::offer);
        } catch (IOException e) {
            throwException("Failed to walk FBO directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void load(File file) {
        ObjectArrayList<FboData> dataList = internalBuilder.buildData(file);

        for (int i = 0; i < dataList.size(); i++)
            fboManager.addFboData(dataList.get(i));
    }
}
