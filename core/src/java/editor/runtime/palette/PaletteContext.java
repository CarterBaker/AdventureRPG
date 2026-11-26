package editor.runtime.palette;

import application.bootstrap.menupipeline.element.ElementInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.propertiespipeline.properties.PaletteProvider;
import editor.bootstrap.propertiespipeline.properties.PropertiesProvider;
import editor.bootstrap.propertiespipeline.propertiesmanager.PropertiesManager;
import editor.bootstrap.propertiespipeline.util.PaletteContent;
import editor.bootstrap.propertiespipeline.util.PropertiesContent;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewPanelSystem;
import editor.runtime.characterview.panel.CharacterViewRowSource;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import editor.runtime.characterview.panel.CharacterViewTemplateStruct;
import editor.runtime.palette.panel.BiomePaletteSystem;
import editor.runtime.palette.panel.ImagePaletteSystem;
import editor.runtime.palette.panel.PaletteSystem;
import editor.runtime.palette.panel.RowPaletteSystem;
import editor.runtime.palette.wheel.ColorWheelSystem;
import editor.runtime.propertiessource.PropertiesSourceSystem;
import engine.root.ContextPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PaletteContext extends ContextPackage implements PropertiesProvider, CharacterViewRowSource {

    /*
     * The editor's one Palette tab, shared by every window, where a tool's
     * choices are picked as Krita's and Photoshop's palettes and swatches are:
     * it shows what the window worked in last that has a palette offers — the
     * colour wheel over the maps of the image it paints, the list a tool
     * picks from (parts, bones, gear, layers, nodes), the biomes the World
     * Editor paints, or nothing to pick from — and what is picked is that
     * window's shared choice. While it is the window worked in last and shows
     * the wheel, the Properties tab holds the colour's hue, saturation and
     * brightness and its red, green and blue, so turning the wheel moves the
     * sliders and dragging a slider turns the wheel; otherwise the Properties
     * tab keeps what the window it lists shows there, the settings of what is
     * picked here.
     */

    // Internal
    private CharacterEditorManager characterEditorManager;
    private PropertiesManager propertiesManager;
    private CharacterViewPanelSystem characterViewPanelSystem;
    private ColorWheelSystem colorWheelSystem;
    private PaletteSystem paletteSystem;
    private ImagePaletteSystem imagePaletteSystem;
    private RowPaletteSystem rowPaletteSystem;
    private BiomePaletteSystem biomePaletteSystem;
    private PropertiesSourceSystem propertiesSourceSystem;

    // Internal \\

    @Override
    protected void create() {
        this.characterViewPanelSystem = create(CharacterViewPanelSystem.class);
        this.colorWheelSystem = create(ColorWheelSystem.class);
        this.paletteSystem = create(PaletteSystem.class);
        this.imagePaletteSystem = create(ImagePaletteSystem.class);
        this.rowPaletteSystem = create(RowPaletteSystem.class);
        this.biomePaletteSystem = create(BiomePaletteSystem.class);
        this.propertiesSourceSystem = create(PropertiesSourceSystem.class);
    }

    @Override
    protected void get() {
        this.characterEditorManager = get(CharacterEditorManager.class);
        this.propertiesManager = get(PropertiesManager.class);
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

    // The colour's rows while the wheel is shown, otherwise whatever the window listed shows
    @Override
    public PropertiesContent getPropertiesContent() {

        PaletteProvider palette = propertiesManager.getPalette();

        if (palette == null || palette.getPaletteContent() == PaletteContent.IMAGE)
            return PropertiesContent.ROWS;

        return palette instanceof PropertiesProvider provider
                ? provider.getPropertiesContent()
                : PropertiesContent.INFO;
    }

    @Override
    public int getRowRevision() {

        CharacterViewRowSource listed = resolveListedRows();

        return listed != null ? listed.getRowRevision() : characterEditorManager.getRevision();
    }

    @Override
    public String getRowTitle() {

        CharacterViewRowSource listed = resolveListedRows();

        return listed != null ? listed.getRowTitle() : EditorSetting.CHARACTER_PAINT_HEADER_COLOR;
    }

    @Override
    public void buildRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        CharacterViewRowSource listed = resolveListedRows();

        if (listed != null)
            listed.buildRows(rows);
        else
            characterEditorManager.buildColorRows(rows);
    }

    @Override
    public CharacterViewTemplateStruct getRowTemplates() {

        CharacterViewRowSource listed = resolveListedRows();

        return listed != null ? listed.getRowTemplates() : EditorSetting.CHARACTER_EDITOR_ROW_TEMPLATES;
    }

    // A slider's steps: a listed row's go to the window whose rows the tab lists, a Properties row's to the window
    // whose settings it keeps, the colour's to the paint colour
    @Override
    public void stepRow(String key, int steps) {

        CharacterViewRowSource listed = resolveListedRows();

        if (rowPaletteSystem.listsRow(key))
            rowPaletteSystem.stepRow(key, steps);
        else if (listed != null)
            listed.stepRow(key, steps);
        else
            characterEditorManager.step(key, steps);
    }

    // The window listed, whose settings the Properties tab keeps while no wheel is shown; null while one is
    private CharacterViewRowSource resolveListedRows() {

        PaletteProvider palette = propertiesManager.getPalette();

        return palette != null
                && palette.getPaletteContent() != PaletteContent.IMAGE
                && palette instanceof CharacterViewRowSource rowSource
                ? rowSource
                : null;
    }
}
