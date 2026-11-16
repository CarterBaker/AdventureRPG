package editor.runtime.uveditor;

import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.charactereditorpipeline.util.CharacterEditorMode;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuProvider;
import editor.bootstrap.filemenupipeline.filemenu.FileMenuTableStruct;
import editor.bootstrap.propertiespipeline.properties.MapsProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.bootstrap.toolpipeline.tool.ToolActionProvider;
import editor.bootstrap.toolpipeline.tool.ToolCanvasProvider;
import editor.bootstrap.toolpipeline.util.ToolCanvas;
import editor.runtime.EditorSetting;
import editor.runtime.charactereditor.mode.CharacterEditorModeSystem;
import editor.runtime.charactereditor.mode.CharacterModeProvider;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.filemenusource.FileMenuSourceSystem;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import editor.runtime.toolcanvas.ToolCanvasSystem;
import editor.runtime.uveditor.render.UVEditorRenderSystem;
import editor.runtime.uveditor.tool.UVEditorToolSystem;
import editor.runtime.uveditor.view.UVEditorViewSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UVEditorContext extends ContextPackage
        implements ToolCanvasProvider, ToolActionProvider, PropertiesProvider, MapsProvider,
        CharacterViewRowSource, FileMenuProvider, CharacterModeProvider {

    /*
     * The UV Editor, the flat half of the UV tool beside the Character UV
     * tab, as Blender's UV Editing workspace pairs its UV Editor with the 3D
     * view: the UVs of the part's faces selected there laid over the tile
     * they are textured with, selected, moved, turned and scaled with the
     * Tools tab's tools, and unwrapped, packed, evened, stitched, pinned and
     * flipped by its actions; the Properties tab fine-tunes the centre of
     * the UVs selected, the Maps tab lists the maps of the texture under
     * them, and the File and Edit menus save and undo the character. While it is the active window it works the editor in the
     * UV mode on the region picked for the modelling tabs. All state lives in
     * CharacterEditorManager; this tab reads input, keeps its view and draws.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private EditorInputSystem editorInputSystem;
    private CharacterEditorModeSystem characterEditorModeSystem;
    private UVEditorToolSystem uvEditorToolSystem;
    private UVEditorViewSystem uvEditorViewSystem;
    private UVEditorRenderSystem uvEditorRenderSystem;
    private ToolCanvasSystem toolCanvasSystem;
    private PropertiesSourceSystem propertiesSourceSystem;
    private FileMenuSourceSystem fileMenuSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.editorInputSystem = create(EditorInputSystem.class);
        this.characterEditorModeSystem = create(CharacterEditorModeSystem.class);
        this.uvEditorToolSystem = create(UVEditorToolSystem.class);
        this.uvEditorViewSystem = create(UVEditorViewSystem.class);
        this.uvEditorRenderSystem = create(UVEditorRenderSystem.class);
        this.toolCanvasSystem = create(ToolCanvasSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
        this.fileMenuSourceSystem = create(FileMenuSourceSystem.class);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Mode \\

    @Override
    public CharacterEditorMode getMode() {
        return CharacterEditorMode.UV;
    }

    // Tools \\

    @Override
    public ToolCanvas getToolCanvas() {
        return ToolCanvas.UV_EDITOR;
    }

    // The tab's actions run exactly as their keys run them, in the UV mode on the region picked
    @Override
    public void runToolAction(String actionKey) {
        characterEditorModeSystem.adopt();
        uvEditorToolSystem.act(actionKey);
    }

    // Properties \\

    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.ROWS;
    }

    @Override
    public int getRowRevision() {
        return characterEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.CHARACTER_PROPERTIES_TITLE + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + EditorSetting.TAB_TITLE_UV_EDITOR;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildPropertyRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        characterEditorManager.step(key, steps);
    }

    // Maps \\

    @Override
    public int getMapsRevision() {
        return characterEditorManager.getRevision();
    }

    @Override
    public String getMapsTitle() {
        return EditorSetting.TAB_TITLE_MAPS + EditorSetting.CHARACTER_VIEW_STATUS_SEPARATOR
                + EditorSetting.TAB_TITLE_UV_EDITOR;
    }

    @Override
    public void buildMapsRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildMapRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getMapsTemplates() {
        return EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepMapsRow(String key, int steps) {
        characterEditorManager.step(key, steps);
    }

    // File Menu \\

    @Override
    public FileMenuTableStruct getFileMenuTable() {
        return EditorSetting.FILE_MENU_CHARACTER;
    }
}
