package editor.runtime.colorwheel;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.colorwheel.wheel.ColorWheelSystem;
import editor.runtime.EditorSetting;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ColorWheelContext extends ContextPackage implements PropertiesProvider, CharacterViewRowSource {

    /*
     * Editor tab holding the colour painting uses, a paint program's colour
     * docker: a hue ring with a triangle of saturation and brightness inside
     * it, turned to the hue. While it is the window worked in last the
     * Properties tab holds the colour's hue, saturation and brightness and
     * its red, green and blue, each a slider with a typed value, so turning
     * the wheel moves the sliders and dragging a slider turns the wheel. The
     * colour is the Character Editor's paint colour, so picking it here, in
     * the Properties tab or with Pick is one choice every tab sees at once.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private ColorWheelSystem colorWheelSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.colorWheelSystem = create(ColorWheelSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Wheel \\

    // Called every frame the wheel stays pressed, from the frame it is first pressed
    public void dragWheel(ElementInstance wheelElement) {
        colorWheelSystem.drag(wheelElement);
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
        return EditorSetting.CHARACTER_PAINT_HEADER_COLOR;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {
        characterEditorManager.buildColorRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {
        return EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    @Override
    public void stepRow(String key, int steps) {
        characterEditorManager.step(key, steps);
    }
}
