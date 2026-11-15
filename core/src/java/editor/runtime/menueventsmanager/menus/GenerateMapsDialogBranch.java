package editor.runtime.menueventsmanager.menus;

import java.nio.ByteBuffer;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.charactereditorpipeline.charactereditormanager.CharacterEditorManager;
import editor.bootstrap.imagepipeline.util.MapGenerateOption;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import editor.runtime.slider.SliderUtility;
import engine.input.Input;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class GenerateMapsDialogBranch extends BranchPackage {

    /*
     * The File menu's Generate Maps dialog: opened over the whole window the
     * request came from, on the albedo of the part painted, it lists every
     * setting that turns the albedo into its maps (a slider each, a Yes or No
     * row for a switch) beside a preview worked out again whenever a setting
     * moves, showing the result lit by the maps or any one map. Generate
     * writes every map asked for over its layer and closes; Cancel or Escape
     * closes with nothing written. It follows its window's size while open.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private TextureManager textureManager;
    private CharacterEditorManager characterEditorManager;

    // Dialog
    private MenuInstance dialog;
    private ObjectArrayList<ElementInstance> optionRows;

    // Preview — its texture, the texels sent to it and the revision it shows
    private int previewTexture;
    private ByteBuffer previewTexels;
    private int shownRevision;

    // Base \\

    @Override
    protected void create() {

        int size = EditorSetting.MAP_GENERATE_PREVIEW_SIZE;

        // Dialog
        this.optionRows = new ObjectArrayList<>();

        // Preview
        this.previewTexels = BufferUtility.newByteBuffer(size * size * EngineSetting.COLOR_CHANNEL_COUNT);
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.textureManager = get(TextureManager.class);
        this.characterEditorManager = get(CharacterEditorManager.class);
    }

    @Override
    protected void update() {

        if (dialog == null)
            return;

        WindowInstance window = dialog.getWindow();

        if (!window.hasCompositeTarget() || !characterEditorManager.isMapGenerationOpen()) {
            close();
            return;
        }

        fitToWindow(window);
        refreshPreview();

        Input rawInput = inputManager.getRawInput(window);

        if (rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE))
            cancel();
    }

    @Override
    protected void dispose() {

        if (previewTexture != 0)
            textureManager.deleteTexture2D(previewTexture);

        this.previewTexture = 0;
    }

    // Dialog \\

    // Opens over the window asked from, on the albedo of the part painted; the status says why when it cannot
    public void open(WindowInstance window) {

        if (dialog != null || !characterEditorManager.openMapGeneration())
            return;

        this.dialog = menuManager.openMenuWindow(EditorSetting.MENU_GENERATE_MAPS_DIALOG, window.getGLWindow());
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;

        if (previewTexture != 0)
            textureManager.deleteTexture2D(previewTexture);

        this.previewTexture = textureManager.createTexture2D(
                characterEditorManager.getMapPreviewWidth(),
                characterEditorManager.getMapPreviewRows(),
                EngineSetting.GL_CLAMP_TO_EDGE,
                EngineSetting.GL_LINEAR);

        ElementInstance preview = dialog.getEntryPoint(EditorSetting.ENTRY_GENERATE_MAPS_PREVIEW);

        if (preview != null && preview.hasSprite())
            preview.getSpriteInstance().setTexture(previewTexture);

        dialog.getEntryPoint(EditorSetting.ENTRY_GENERATE_MAPS_TITLE).setFontText(
                EditorSetting.MAP_GENERATE_TITLE
                        + CharacterViewUtility.toResourceLabel(characterEditorManager.getMapTextureName()));
        dialog.getEntryPoint(EditorSetting.ENTRY_GENERATE_MAPS_STATUS).setFontText(EditorSetting.MAP_GENERATE_HINT);
        injectOptions();
    }

    private void close() {

        if (dialog == null)
            return;

        characterEditorManager.closeMapGeneration();
        menuManager.closeMenuWindow(dialog);
        this.dialog = null;
        optionRows.clear();
    }

    private void fitToWindow(WindowInstance window) {

        WindowInstance osWindow = window.getGLWindow();

        if (!window.isPlacedAt(0f, 0f, osWindow.getWidth(), osWindow.getHeight()))
            window.place(0f, 0f, osWindow.getWidth(), osWindow.getHeight());
    }

    // Options \\

    // A header per group of settings, then a slider per value and a Yes or No row per switch
    private void injectOptions() {

        optionRows.clear();

        for (MapGenerateOption option : MapGenerateOption.VALUES) {

            int group = findGroup(option);

            if (group != EngineSetting.INDEX_NOT_FOUND)
                menuManager.inject(dialog, EditorSetting.ENTRY_GENERATE_MAPS_OPTIONS,
                        EditorSetting.MENU_GENERATE_MAPS_HEADER_ROW,
                        row -> row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_ROW_LABEL)
                                .setFontText(EditorSetting.MAP_GENERATE_GROUP_LABELS[group]));

            optionRows.add(menuManager.inject(dialog, EditorSetting.ENTRY_GENERATE_MAPS_OPTIONS,
                    option.isToggle()
                            ? EditorSetting.MENU_GENERATE_MAPS_OPTION_ROW
                            : EditorSetting.MENU_GENERATE_MAPS_SLIDER_ROW,
                    row -> {
                        row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_ROW_LABEL).setFontText(option.getLabel());
                        bindRow(row, option);
                    }));
        }

        refreshRows();
    }

    // The group a setting opens, by its place among the groups' first settings; none for any other
    private int findGroup(MapGenerateOption option) {

        for (int i = 0; i < EditorSetting.MAP_GENERATE_GROUP_STARTS.length; i++)
            if (EditorSetting.MAP_GENERATE_GROUP_STARTS[i] == option)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private void bindRow(ElementInstance row, MapGenerateOption option) {

        if (!option.isToggle()) {
            row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_SLIDER_TRACK).setOnDragArgOverride(option.name());
            return;
        }

        row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_ROW_PREVIOUS).setActionArgOverride(option.name());
        row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_ROW_NEXT).setActionArgOverride(option.name());
    }

    // Every row's value and knob where its setting stands
    private void refreshRows() {

        for (int i = 0; i < optionRows.size(); i++) {

            MapGenerateOption option = MapGenerateOption.VALUES[i];
            ElementInstance row = optionRows.get(i);
            float value = characterEditorManager.getMapOption(option);

            if (option.isToggle()) {
                row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_ROW_VALUE).setFontText(option.formatValue(value));
                continue;
            }

            ElementInstance track = row.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_SLIDER_TRACK);

            SliderUtility.placeKnob(track, EditorSetting.ELEMENT_GENERATE_MAPS_SLIDER_KNOB,
                    SliderUtility.resolveStep((value - option.getMin()) / (option.getMax() - option.getMin())));
            track.findChildById(EditorSetting.ELEMENT_GENERATE_MAPS_SLIDER_VALUE)
                    .setFontText(option.formatValue(value));
        }

        dialog.getEntryPoint(EditorSetting.ENTRY_GENERATE_MAPS_PREVIEW_VALUE).setFontText(
                EditorSetting.MAP_GENERATE_PREVIEW_LABELS[characterEditorManager.getMapPreview()]);
    }

    public void dragSlider(String optionName, WindowInstance window, ElementInstance track) {

        if (dialog == null)
            return;

        MapGenerateOption option = MapGenerateOption.valueOf(optionName);
        float fraction = SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track);

        characterEditorManager.setMapOption(option,
                option.getMin() + SliderUtility.toFraction(SliderUtility.resolveStep(fraction))
                        * (option.getMax() - option.getMin()));
    }

    public void previousOption(String optionName) {
        stepOption(optionName, -1);
    }

    public void nextOption(String optionName) {
        stepOption(optionName, 1);
    }

    private void stepOption(String optionName, int direction) {

        if (dialog != null)
            characterEditorManager.stepMapOption(MapGenerateOption.valueOf(optionName), direction);
    }

    public void previousPreview() {

        if (dialog != null)
            characterEditorManager.stepMapPreview(-1);
    }

    public void nextPreview() {

        if (dialog != null)
            characterEditorManager.stepMapPreview(1);
    }

    // Preview \\

    // The preview worked out again, and every row set, once whenever a setting or the map shown moves
    private void refreshPreview() {

        if (characterEditorManager.getMapRevision() == shownRevision)
            return;

        this.shownRevision = characterEditorManager.getMapRevision();
        characterEditorManager.renderMapPreview(previewTexels);
        textureManager.updateTexture2D(previewTexture, 0, 0, characterEditorManager.getMapPreviewWidth(),
                characterEditorManager.getMapPreviewRows(), previewTexels);
        refreshRows();
    }

    // Buttons \\

    public void generate() {

        if (dialog == null)
            return;

        characterEditorManager.generateMaps();
        close();
    }

    public void cancel() {
        close();
    }
}
