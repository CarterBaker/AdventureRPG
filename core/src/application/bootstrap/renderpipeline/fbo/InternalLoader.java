package application.bootstrap.renderpipeline.fbo;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
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
        File file = new File(EngineSetting.FBO_CATALOG_JSON_PATH);
        if (file.exists())
            fileQueue.offer(file);
    }

    @Override
    protected void load(File file) {
        ObjectArrayList<FboHandle> handles = internalBuilder.build(file);

        for (int i = 0; i < handles.size(); i++)
            fboManager.addFboHandle(handles.get(i));
    }
}
