package editor.runtime.worldeditor;

import editor.bootstrap.worldeditorpipeline.util.WorldEditorLayer;

public interface WorldEditorLayerProvider {

    /*
     * Names the world layer a world canvas window works on now, the one
     * picked in the Layers tab, so the File and Edit menus undo and redo the
     * history of that layer.
     */

    WorldEditorLayer getWorldEditorLayer();
}
