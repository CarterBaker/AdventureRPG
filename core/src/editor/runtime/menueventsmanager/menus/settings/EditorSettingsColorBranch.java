package editor.runtime.menueventsmanager.menus.settings;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.Settings;

public class EditorSettingsColorBranch extends BranchPackage {

    /*
     * Fills the settings dialog's Colors page with the editor's color scheme
     * and edits it. Every slot is listed with a swatch shaded from the slot
     * itself and its value as hex. Clicking a slot opens its editor under
     * it: a slider for each channel, an exact hex value typed in the text
     * dialog, and a reset to its default; a second click closes it. A color
     * is edited in place in Settings, so every open menu takes it from the
     * next frame, and it is saved with the rest of the editor's settings.
     */

    // Internal
    private NameDialogBranch nameDialogBranch;
    private EditorSettingsBranch editorSettingsBranch;

    // Defaults
    private Settings defaults;

    // Colors
    private MenuColorStruct[] swatchColors;
    private MenuColorStruct selectedRowColor;

    // Channels
    private String[] channelArgs;

    // Selection
    private EditorSettingsColor selectedColor;
    private ElementInstance selectedHexLabel;

    // Scratch
    private float[] parsedColor;

    // Base \\

    @Override
    protected void create() {

        // Defaults
        this.defaults = new Settings();

        // Colors
        this.swatchColors = new MenuColorStruct[EditorSettingsColor.VALUES.length];

        for (int i = 0; i < swatchColors.length; i++)
            swatchColors[i] = new MenuColorStruct(
                    EditorSettingsColor.VALUES[i].getThemeColor(),
                    EngineSetting.MENU_THEME_ALPHA_DEFAULT);

        this.selectedRowColor = new MenuColorStruct(ThemeColor.ACCENT, EditorSetting.SETTINGS_TAB_ACTIVE_ALPHA);

        // Channels
        this.channelArgs = new String[EngineSetting.COLOR_CHANNEL_COUNT];

        for (int i = 0; i < channelArgs.length; i++)
            channelArgs[i] = Integer.toString(i);

        // Scratch
        this.parsedColor = new float[EngineSetting.COLOR_CHANNEL_COUNT];
    }

    @Override
    protected void get() {
        this.nameDialogBranch = get(NameDialogBranch.class);
        this.editorSettingsBranch = get(EditorSettingsBranch.class);
    }

    // Populate \\

    void populateColors() {

        this.selectedHexLabel = null;
        editorSettingsBranch.injectNote(EditorSetting.SETTINGS_NOTE_COLORS);

        String section = null;

        for (EditorSettingsColor color : EditorSettingsColor.VALUES) {

            if (!color.getSection().equals(section)) {
                section = color.getSection();
                editorSettingsBranch.injectSectionHeader(section);
            }

            injectColorRow(color);

            if (color == selectedColor)
                injectColorEditor(color);
        }

        editorSettingsBranch.injectActionRow(
                EditorSettingsAction.RESET_COLORS,
                EditorSetting.SETTINGS_ACTION_RESET_COLORS);
    }

    private void injectColorRow(EditorSettingsColor color) {

        boolean selected = color == selectedColor;

        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_COLOR_ROW,
                row -> {
                    ElementInstance hexLabel = row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_VALUE);

                    row.setActionArgOverride(color.name());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL).setFontText(color.getLabel());
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_COLOR_SWATCH)
                            .setColorOverride(swatchColors[color.ordinal()]);
                    hexLabel.setFontText(formatHex(color.resolve(settings)));

                    if (!selected)
                        return;

                    row.setColorOverride(selectedRowColor);
                    this.selectedHexLabel = hexLabel;
                });
    }

    private void injectColorEditor(EditorSettingsColor color) {

        float[] rgba = color.resolve(settings);

        for (int channel = 0; channel < EngineSetting.COLOR_CHANNEL_COUNT; channel++)
            injectChannel(channel, rgba[channel]);

        editorSettingsBranch.injectActionRow(
                EditorSettingsAction.ENTER_HEX,
                EditorSetting.SETTINGS_ACTION_ENTER_HEX);
        editorSettingsBranch.injectActionRow(
                EditorSettingsAction.RESET_COLOR,
                EditorSetting.SETTINGS_ACTION_RESET_COLOR);
    }

    private void injectChannel(int channel, float value) {
        editorSettingsBranch.injectRow(
                EditorSetting.MENU_EDITOR_SETTINGS_CHANNEL_ROW,
                row -> {
                    ElementInstance track = row.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_TRACK);
                    track.setOnDragArgOverride(channelArgs[channel]);
                    row.findChildById(EditorSetting.ELEMENT_SETTINGS_ROW_LABEL)
                            .setFontText(EditorSetting.SETTINGS_COLOR_CHANNEL_LABELS[channel]);
                    refreshChannel(track, value);
                });
    }

    // Selection \\

    public void selectColor(String colorName) {

        if (!editorSettingsBranch.isOpen())
            return;

        EditorSettingsColor color = EditorSettingsColor.valueOf(colorName);

        this.selectedColor = color == selectedColor ? null : color;
        editorSettingsBranch.refreshOptions();
    }

    // Channels \\

    public void dragChannel(String channelArg, WindowInstance window, ElementInstance track) {

        if (!editorSettingsBranch.isOpen() || selectedColor == null)
            return;

        float[] rgba = selectedColor.resolve(settings);
        int channel = Integer.parseInt(channelArg);
        float value = editorSettingsBranch.resolveTrackFraction(window, track);

        rgba[channel] = value;

        if (!editorSettingsBranch.placeKnob(track, value))
            return;

        track.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_VALUE).setFontText(formatChannel(value));

        if (selectedHexLabel != null)
            selectedHexLabel.setFontText(formatHex(rgba));
    }

    private void refreshChannel(ElementInstance track, float value) {
        editorSettingsBranch.placeKnob(track, value);
        track.findChildById(EditorSetting.ELEMENT_SETTINGS_SLIDER_VALUE).setFontText(formatChannel(value));
    }

    // Hex \\

    void openHexDialog(WindowInstance window) {

        if (selectedColor == null)
            return;

        nameDialogBranch.openText(
                window,
                EditorSetting.SETTINGS_DIALOG_TITLE_HEX_PREFIX + selectedColor.getLabel(),
                formatHex(selectedColor.resolve(settings)),
                this::parseHex,
                this::applyHex);
    }

    private void applyHex(String text) {

        if (selectedColor == null || !editorSettingsBranch.isOpen() || !parseHex(text))
            return;

        System.arraycopy(parsedColor, 0, selectedColor.resolve(settings), 0, EngineSetting.COLOR_CHANNEL_COUNT);
        editorSettingsBranch.refreshOptions();
    }

    // Reads "#RRGGBB" or "#RRGGBBAA", the hash optional, into the parsed color; false when it is neither
    private boolean parseHex(String text) {

        String digits = text.startsWith(EditorSetting.SETTINGS_HEX_PREFIX)
                ? text.substring(EditorSetting.SETTINGS_HEX_PREFIX.length())
                : text;
        int channels = digits.length() / EditorSetting.SETTINGS_HEX_DIGITS_PER_CHANNEL;

        if (digits.length() % EditorSetting.SETTINGS_HEX_DIGITS_PER_CHANNEL != 0
                || channels < EngineSetting.COLOR_CHANNEL_COUNT - 1
                || channels > EngineSetting.COLOR_CHANNEL_COUNT)
            return false;

        parsedColor[EngineSetting.COLOR_CHANNEL_COUNT - 1] = EngineSetting.COLOR_CHANNEL_MAX;

        for (int channel = 0; channel < channels; channel++) {

            int high = Character.digit(
                    digits.charAt(channel * EditorSetting.SETTINGS_HEX_DIGITS_PER_CHANNEL),
                    EditorSetting.SETTINGS_HEX_RADIX);
            int low = Character.digit(
                    digits.charAt(channel * EditorSetting.SETTINGS_HEX_DIGITS_PER_CHANNEL + 1),
                    EditorSetting.SETTINGS_HEX_RADIX);

            if (high < 0 || low < 0)
                return false;

            parsedColor[channel] = (high * EditorSetting.SETTINGS_HEX_RADIX + low)
                    / EngineSetting.COLOR_CHANNEL_BYTE_MAX;
        }

        return true;
    }

    // Reset \\

    void resetSelectedColor() {

        if (selectedColor == null)
            return;

        resetColor(selectedColor);
        editorSettingsBranch.setStatus(selectedColor.getLabel() + EditorSetting.SETTINGS_STATUS_COLOR_RESET_SUFFIX);
        editorSettingsBranch.refreshOptions();
    }

    void resetColors() {

        for (EditorSettingsColor color : EditorSettingsColor.VALUES)
            resetColor(color);

        editorSettingsBranch.setStatus(EditorSetting.SETTINGS_STATUS_COLORS_RESET);
        editorSettingsBranch.refreshOptions();
    }

    private void resetColor(EditorSettingsColor color) {
        System.arraycopy(
                color.resolve(defaults), 0,
                color.resolve(settings), 0,
                EngineSetting.COLOR_CHANNEL_COUNT);
    }

    // Text \\

    private String formatChannel(float value) {
        return Integer.toString(Math.round(value * EngineSetting.COLOR_CHANNEL_BYTE_MAX));
    }

    private String formatHex(float[] rgba) {
        return String.format(
                EditorSetting.SETTINGS_FORMAT_HEX,
                Math.round(rgba[0] * EngineSetting.COLOR_CHANNEL_BYTE_MAX),
                Math.round(rgba[1] * EngineSetting.COLOR_CHANNEL_BYTE_MAX),
                Math.round(rgba[2] * EngineSetting.COLOR_CHANNEL_BYTE_MAX),
                Math.round(rgba[3] * EngineSetting.COLOR_CHANNEL_BYTE_MAX));
    }
}
