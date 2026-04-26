package application.bootstrap.renderpipeline.panel;

import application.bootstrap.renderpipeline.fbo.FboInstance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class FboViewInstance extends PanelViewInstance {

    private ObjectArrayList<FboInstance> chain;

    public void constructor(ObjectArrayList<FboInstance> chain) {
        this.chain = chain;
    }

    @Override
    public void render(int x, int y, int w, int h) {
        if (chain == null)
            return;

        Object[] elements = chain.elements();
        int count = chain.size();

        for (int i = 0; i < count; i++) {
            FboInstance fbo = (FboInstance) elements[i];
            fbo.bind();
            fbo.unbind();
        }
    }
}
