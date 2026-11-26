package editor.runtime.menueventsmanager.menus.search;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.util.MenuColorStruct;
import application.bootstrap.menupipeline.util.ThemeColor;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SearchFieldStruct extends StructPackage {

    /*
     * The text typed into one search field and the label that shows it: the
     * placeholder in the muted color while it is empty and not focused,
     * otherwise the query with a caret while focused. It takes any character
     * the editor font draws, up to the search length, and matches text
     * ignoring case, so every search box in the editor types and matches the
     * same way.
     */

    // Field
    private final StringBuilder query;
    private final String placeholder;
    private ElementInstance label;
    private boolean focused;

    // Colors
    private final MenuColorStruct textColor;
    private final MenuColorStruct placeholderColor;

    // Constructor \\

    public SearchFieldStruct(String placeholder) {

        // Field
        this.query = new StringBuilder();
        this.placeholder = placeholder;

        // Colors
        this.textColor = new MenuColorStruct(ThemeColor.TEXT, EngineSetting.MENU_THEME_ALPHA_DEFAULT);
        this.placeholderColor = new MenuColorStruct(ThemeColor.TEXT_MUTED, EngineSetting.MENU_THEME_ALPHA_DEFAULT);
    }

    // Management \\

    // The label the field shows itself in, null once that label is gone
    public void bind(ElementInstance label) {
        this.label = label;
        refreshLabel();
    }

    public void focus() {
        this.focused = true;
        refreshLabel();
    }

    public void blur() {
        this.focused = false;
        refreshLabel();
    }

    // True when the character was taken
    public boolean type(char character) {

        if (!focused
                || EngineSetting.FONT_DEFAULT_CHARSET.indexOf(character) == EngineSetting.INDEX_NOT_FOUND
                || query.length() >= EditorSetting.SEARCH_QUERY_MAX_LENGTH)
            return false;

        query.append(character);
        refreshLabel();

        return true;
    }

    // True when a character was taken away
    public boolean erase() {

        if (query.length() == 0)
            return false;

        query.setLength(query.length() - 1);
        refreshLabel();

        return true;
    }

    // True when there was a query to clear
    public boolean clear() {

        if (query.length() == 0)
            return false;

        query.setLength(0);
        refreshLabel();

        return true;
    }

    private void refreshLabel() {

        if (label == null)
            return;

        boolean showsPlaceholder = query.length() == 0 && !focused;

        label.setFontText(showsPlaceholder
                ? placeholder
                : focused ? query + EditorSetting.SEARCH_CARET : query.toString());
        label.setColorOverride(showsPlaceholder ? placeholderColor : textColor);
    }

    // Matching \\

    // Whether the text holds the query anywhere, ignoring case; an empty query matches everything
    public boolean matches(String text) {
        return indexIn(text) != EngineSetting.INDEX_NOT_FOUND;
    }

    // Whether the text opens with the query, ignoring case
    public boolean opens(String text) {
        return indexIn(text) == 0;
    }

    private int indexIn(String text) {

        int length = query.length();

        if (length == 0)
            return 0;

        if (text == null)
            return EngineSetting.INDEX_NOT_FOUND;

        for (int start = 0; start + length <= text.length(); start++)
            if (regionMatches(text, start, length))
                return start;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean regionMatches(String text, int start, int length) {

        for (int i = 0; i < length; i++)
            if (Character.toLowerCase(text.charAt(start + i)) != Character.toLowerCase(query.charAt(i)))
                return false;

        return true;
    }

    // Accessible \\

    public boolean isFocused() {
        return focused;
    }

    public boolean isEmpty() {
        return query.length() == 0;
    }
}
