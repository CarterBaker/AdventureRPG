package editor.runtime.menueventsmanager.menus;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.imageeditorpipeline.imageeditormanager.ImageEditorManager;
import editor.runtime.characterview.util.CharacterViewUtility;
import editor.runtime.EditorSetting;
import editor.runtime.slider.SliderUtility;
import engine.input.Input;
import engine.root.BranchPackage;

public class NewImageDialogBranch extends BranchPackage {

    /*
     * The File menu's New dialog for the Image Editor, as a painting tool's
     * New Document dialog: opened over the whole window the request came
     * from once the image is named, it holds the name, the width and height
     * (each a slider across the sizes an image may take, step buttons and a
     * typed value) and the background its first layer is filled with, clear,
     * white, black or the paint colour. Create makes the image and closes;
     * Cancel or Escape closes with nothing made. It follows its window's size
     * while open.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private ImageEditorManager imageEditorManager;
    private NameDialogBranch nameDialogBranch;

    // Dialog
    private MenuInstance dialog;
    private ElementInstance nameRow;
    private ElementInstance widthRow;
    private ElementInstance heightRow;
    private ElementInstance backgroundRow;

    // Image — what Create makes
    private String imageName;
    private int width;
    private int height;
    private int background;

    // Base \\

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.imageEditorManager = get(ImageEditorManager.class);
        this.nameDialogBranch = get(NameDialogBranch.class);
    }

    @Override
    protected void update() {

        if (dialog == null)
            return;

        WindowInstance window = dialog.getWindow();

        if (!window.hasCompositeTarget()) {
            close();
            return;
        }

        fitToWindow(window);

        Input rawInput = inputManager.getRawInput(window);

        if (!nameDialogBranch.isOpen() && rawInput.isKeyClicked(EditorSetting.KEY_ESCAPE))
            cancel();
    }

    // Dialog \\

    // Opens over the window asked from on a name no image has yet, at the default size and background
    public void open(WindowInstance window, String imageName) {

        if (dialog != null)
            return;

        this.imageName = imageName;
        this.width = EditorSetting.IMAGE_EDITOR_SIZE_DEFAULT;
        this.height = EditorSetting.IMAGE_EDITOR_SIZE_DEFAULT;
        this.background = EditorSetting.IMAGE_EDITOR_BACKGROUND_DEFAULT;
        this.dialog = menuManager.openMenuWindow(EditorSetting.MENU_NEW_IMAGE_DIALOG, window.getGLWindow());

        dialog.getEntryPoint(EditorSetting.ENTRY_NEW_IMAGE_TITLE).setFontText(EditorSetting.NEW_IMAGE_TITLE);
        dialog.getEntryPoint(EditorSetting.ENTRY_NEW_IMAGE_STATUS).setFontText(EditorSetting.NEW_IMAGE_HINT);
        injectRows();
        refreshRows();
    }

    private void close() {

        if (dialog == null)
            return;

        menuManager.closeMenuWindow(dialog);
        this.dialog = null;
    }

    private void fitToWindow(WindowInstance window) {

        WindowInstance osWindow = window.getGLWindow();

        if (!window.isPlacedAt(0f, 0f, osWindow.getWidth(), osWindow.getHeight()))
            window.place(0f, 0f, osWindow.getWidth(), osWindow.getHeight());
    }

    // Rows \\

    private void injectRows() {

        menuManager.inject(dialog, EditorSetting.ENTRY_NEW_IMAGE_OPTIONS, EditorSetting.MENU_NEW_IMAGE_HEADER_ROW,
                row -> row.findChildById(EditorSetting.ELEMENT_NEW_IMAGE_ROW_LABEL)
                        .setFontText(EditorSetting.NEW_IMAGE_HEADER));
        this.nameRow = injectRow(EditorSetting.MENU_NEW_IMAGE_TEXT_ROW,
                EditorSetting.NEW_IMAGE_LABEL_NAME, EditorSetting.NEW_IMAGE_FIELD_NAME);
        this.widthRow = injectRow(EditorSetting.MENU_NEW_IMAGE_SLIDER_ROW,
                EditorSetting.NEW_IMAGE_LABEL_WIDTH, EditorSetting.NEW_IMAGE_FIELD_WIDTH);
        this.heightRow = injectRow(EditorSetting.MENU_NEW_IMAGE_SLIDER_ROW,
                EditorSetting.NEW_IMAGE_LABEL_HEIGHT, EditorSetting.NEW_IMAGE_FIELD_HEIGHT);
        this.backgroundRow = injectRow(EditorSetting.MENU_NEW_IMAGE_OPTION_ROW,
                EditorSetting.NEW_IMAGE_LABEL_BACKGROUND, EditorSetting.NEW_IMAGE_FIELD_BACKGROUND);
    }

    // A row whose every button hands back the field it sets
    private ElementInstance injectRow(String template, String label, String field) {
        return menuManager.inject(dialog, EditorSetting.ENTRY_NEW_IMAGE_OPTIONS, template, row -> {
            row.findChildById(EditorSetting.ELEMENT_NEW_IMAGE_ROW_LABEL).setFontText(label);
            bind(row, EditorSetting.ELEMENT_NEW_IMAGE_ROW_VALUE_BUTTON, field);
            bind(row, EditorSetting.ELEMENT_NEW_IMAGE_ROW_PREVIOUS, field);
            bind(row, EditorSetting.ELEMENT_NEW_IMAGE_ROW_NEXT, field);

            ElementInstance track = row.findChildById(EditorSetting.ELEMENT_NEW_IMAGE_SLIDER_TRACK);

            if (track != null)
                track.setOnDragArgOverride(field);
        });
    }

    private void bind(ElementInstance row, String elementID, String field) {

        ElementInstance element = row.findChildById(elementID);

        if (element != null)
            element.setActionArgOverride(field);
    }

    // Every row's value, and each slider's knob, where its field stands
    private void refreshRows() {
        setValue(nameRow, imageName);
        refreshSize(widthRow, width);
        refreshSize(heightRow, height);
        setValue(backgroundRow, EditorSetting.IMAGE_EDITOR_BACKGROUND_LABELS[background]);
    }

    private void refreshSize(ElementInstance row, int size) {

        ElementInstance track = row.findChildById(EditorSetting.ELEMENT_NEW_IMAGE_SLIDER_TRACK);

        SliderUtility.placeKnob(track, EditorSetting.ELEMENT_NEW_IMAGE_SLIDER_KNOB, SliderUtility.resolveStep(
                (float) (size - EditorSetting.IMAGE_EDITOR_SIZE_MIN)
                        / (EditorSetting.IMAGE_EDITOR_SIZE_MAX - EditorSetting.IMAGE_EDITOR_SIZE_MIN)));
        setValue(row, Integer.toString(size));
    }

    private void setValue(ElementInstance row, String value) {
        row.findChildById(EditorSetting.ELEMENT_NEW_IMAGE_ROW_VALUE).setFontText(value);
    }

    // Fields \\

    public void dragSlider(String field, WindowInstance window, ElementInstance track) {

        if (dialog == null)
            return;

        float fraction = SliderUtility.toFraction(SliderUtility.resolveStep(
                SliderUtility.resolveFraction(inputManager.getHoverMouseX(window), track)));

        setSize(field, Math.round(EditorSetting.IMAGE_EDITOR_SIZE_MIN
                + fraction * (EditorSetting.IMAGE_EDITOR_SIZE_MAX - EditorSetting.IMAGE_EDITOR_SIZE_MIN)));
    }

    public void previousOption(String field) {
        stepField(field, -1);
    }

    public void nextOption(String field) {
        stepField(field, 1);
    }

    // A size moves by its step, the background through its choices
    private void stepField(String field, int direction) {

        if (dialog == null)
            return;

        if (field.equals(EditorSetting.NEW_IMAGE_FIELD_BACKGROUND)) {
            this.background = Math.floorMod(
                    background + direction,
                    EditorSetting.IMAGE_EDITOR_BACKGROUND_LABELS.length);
            refreshRows();
            return;
        }

        setSize(field, getSize(field) + direction * EditorSetting.IMAGE_EDITOR_SIZE_STEP);
    }

    // The field's value opened for typing: a name no image has, or a whole size within the limits
    public void typeOption(String field, WindowInstance window) {

        if (dialog == null || field.equals(EditorSetting.NEW_IMAGE_FIELD_BACKGROUND))
            return;

        if (field.equals(EditorSetting.NEW_IMAGE_FIELD_NAME)) {
            nameDialogBranch.openText(window, EditorSetting.DIALOG_TITLE_NEW_IMAGE, imageName,
                    imageEditorManager::isImageNameAvailable, typed -> {
                        this.imageName = typed;
                        refreshRows();
                    });
            return;
        }

        nameDialogBranch.openText(window, EditorSetting.DIALOG_TITLE_EDIT_VALUE, Integer.toString(getSize(field)),
                typed -> CharacterViewUtility.isNumber(typed)
                        && imageEditorManager.isSizeValid(Math.round(CharacterViewUtility.parseNumber(typed))),
                typed -> setSize(field, Math.round(CharacterViewUtility.parseNumber(typed))));
    }

    // The single setter a size's slider, steps and typed value share, held within the limits
    private void setSize(String field, int size) {

        int held = Math.clamp(size, EditorSetting.IMAGE_EDITOR_SIZE_MIN, EditorSetting.IMAGE_EDITOR_SIZE_MAX);

        if (field.equals(EditorSetting.NEW_IMAGE_FIELD_WIDTH))
            this.width = held;
        else
            this.height = held;

        refreshRows();
    }

    private int getSize(String field) {
        return field.equals(EditorSetting.NEW_IMAGE_FIELD_WIDTH) ? width : height;
    }

    // Buttons \\

    public void create() {

        if (dialog == null)
            return;

        imageEditorManager.newImage(imageName, width, height, imageEditorManager.resolveBackground(background));
        close();
    }

    public void cancel() {
        close();
    }
}
