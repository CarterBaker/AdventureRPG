package editor.runtime.worldeditor;

import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;

public interface WorldEditorLayerProvider {

    /*
     * Names the world layer a world canvas tab works on, so the view, tool
     * and render systems every such tab shares paint, show and describe that
     * layer: the biomes in the World Editor, the heights in the Height
     * Editor.
     */

    WorldEditorLayer getWorldEditorLayer();
}
