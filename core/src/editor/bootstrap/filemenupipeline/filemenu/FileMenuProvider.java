package editor.bootstrap.filemenupipeline.filemenu;

public interface FileMenuProvider {

    /*
     * Names the methods the File and Edit menus run while this window is the
     * one worked in last, the same ones its keys run, so a menu item and its
     * shortcut always do the same thing. An action the table leaves out is
     * shown muted and does nothing.
     */

    FileMenuTableStruct getFileMenuTable();
}
