package editor.runtime.entityeditor;

import editor.bootstrap.entityeditorpipeline.entityeditormanager.EntityEditorManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.characterview.panel.CharacterViewDockSystem;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.EditorSetting;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class EntityEditorContext extends ContextPackage implements CharacterViewRowSource, PropertiesProvider {

    /*
     * Docker window listing every entity, the race the picked one is made of
     * and every gender that race may be made as, with the model each
     * requires. Picking an entity selects it for every window: the Properties
     * tab shows and edits its fields, and the character tabs open its race;
     * picking a gender previews it there, wearing its own model. The state
     * lives in EntityEditorManager.
     */

    // Internal
    private EntityEditorManager entityEditorManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private CharacterViewDockSystem characterViewDockSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.characterViewDockSystem = create(CharacterViewDockSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void get() {
        this.entityEditorManager = get(EntityEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Rows \\

    @Override
    public int getRowRevision() {
        return entityEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {
        return EditorSetting.ENTITY_EDITOR_TITLE;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        entityEditorManager.buildListRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.ENTITY_EDITOR_ROW_TEMPLATES;
    }

    // The window lists no number rows, so no slider of it ever asks for a step
    @Override
    public void stepRow(String key, int steps) {
    }

    // Properties \\

    // The entity picked here, shown in the Properties tab for editing
    @Override
    public PropertiesContent getPropertiesContent() {
        return PropertiesContent.INFO;
    }
}
