package editor.bootstrap;

import editor.bootstrap.animationeditorpipeline.AnimationEditorPipeline;
import editor.bootstrap.animationtreeeditorpipeline.AnimationTreeEditorPipeline;
import editor.bootstrap.biomeplannerpipeline.BiomePlannerPipeline;
import editor.bootstrap.brushpipeline.BrushPipeline;
import editor.bootstrap.cameraplannerpipeline.CameraPlannerPipeline;
import editor.bootstrap.charactereditorpipeline.CharacterEditorPipeline;
import editor.bootstrap.commandpipeline.CommandPipeline;
import editor.bootstrap.imagepipeline.ImagePipeline;
import editor.bootstrap.infopipeline.InfoPipeline;
import editor.bootstrap.itemeditorpipeline.ItemEditorPipeline;
import editor.bootstrap.recorderpipeline.RecorderPipeline;
import editor.bootstrap.settlementplannerpipeline.SettlementPlannerPipeline;
import editor.bootstrap.shortcutpipeline.ShortcutPipeline;
import editor.bootstrap.soundlibrarypipeline.SoundLibraryPipeline;
import editor.bootstrap.tabpipeline.TabPipeline;
import editor.bootstrap.toolpipeline.ToolPipeline;
import editor.bootstrap.worldeditorpipeline.WorldEditorPipeline;
import editor.runtime.menueventsmanager.EditorMenuEventsManager;
import engine.root.AssemblyPackage;

public class EditorBootstrapAssembly extends AssemblyPackage {

    /*
     * Editor bootstrap root. Registers editor-global bootstrap managers that
     * must be available before runtime editor contexts and menu reflection run:
     * the tab pipeline, the info pipeline behind every hierarchy tab and info
     * panel, the brush presets and the one set of tools every editor that
     * takes tools shares, the item editor pipeline shared by every item
     * editor, the image editing framework and the world editor pipeline built
     * on it, the settlement planner pipeline whose design the world editor
     * places, the camera planner pipeline whose cameras the main menu looks
     * out from, the biome planner pipeline whose small world the biome viewer
     * flies over, the character, animation and animation tree editor
     * pipelines, the command pipeline that routes console commands to every
     * Dev window, the recorder pipeline that records, plays back and exports
     * from a Dev window, the shortcut pipeline that runs every tool's keys,
     * and the one set of editor menu branches the toolbar, every tab's
     * chrome, and every tool route their callbacks to.
     */

    // Internal \\

    @Override
    protected void create() {
        create(TabPipeline.class);
        create(InfoPipeline.class);
        create(BrushPipeline.class);
        create(ToolPipeline.class);
        create(ItemEditorPipeline.class);
        create(ImagePipeline.class);
        create(WorldEditorPipeline.class);
        create(SettlementPlannerPipeline.class);
        create(CameraPlannerPipeline.class);
        create(BiomePlannerPipeline.class);
        create(CharacterEditorPipeline.class);
        create(AnimationEditorPipeline.class);
        create(AnimationTreeEditorPipeline.class);
        create(CommandPipeline.class);
        create(RecorderPipeline.class);
        create(SoundLibraryPipeline.class);
        create(ShortcutPipeline.class);
        create(EditorMenuEventsManager.class);
    }
}
