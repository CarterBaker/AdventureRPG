package editor.runtime.menueventsmanager;

import editor.runtime.menueventsmanager.menus.AnimationEditorBranch;
import editor.runtime.menueventsmanager.menus.AnimationTreeEditorBranch;
import editor.runtime.menueventsmanager.menus.CharacterEditorBranch;
import editor.runtime.menueventsmanager.menus.CommandConsoleBranch;
import editor.runtime.menueventsmanager.menus.ConsoleBranch;
import editor.runtime.menueventsmanager.menus.EditorBranch;
import editor.runtime.menueventsmanager.menus.ExportDialogBranch;
import editor.runtime.menueventsmanager.menus.InfoPanelBranch;
import editor.runtime.menueventsmanager.menus.ItemEditorBranch;
import editor.runtime.menueventsmanager.menus.NameDialogBranch;
import editor.runtime.menueventsmanager.menus.ProfilerBranch;
import editor.runtime.menueventsmanager.menus.RecorderBranch;
import editor.runtime.menueventsmanager.menus.TabBranch;
import editor.runtime.menueventsmanager.menus.WorldEditorBranch;
import editor.runtime.menueventsmanager.menus.WorldMapBranch;
import editor.runtime.menueventsmanager.menus.settings.EditorSettingsBranch;
import editor.runtime.menueventsmanager.menus.settings.EditorSettingsColorBranch;
import editor.runtime.menueventsmanager.menus.settings.EditorSettingsOptionBranch;
import editor.runtime.menueventsmanager.menus.settings.EditorSettingsShortcutBranch;
import engine.root.ManagerPackage;

public class EditorMenuEventsManager extends ManagerPackage {

    /*
     * Owns the editor's menu callback branches, shared by every editor window.
     * The settings dialog's branches come first, so the dialog reads its keys
     * before the text dialog it opens over itself closes on them.
     */

    @Override
    protected void create() {
        create(EditorSettingsBranch.class);
        create(EditorSettingsOptionBranch.class);
        create(EditorSettingsShortcutBranch.class);
        create(EditorSettingsColorBranch.class);
        create(NameDialogBranch.class);
        create(ExportDialogBranch.class);
        create(EditorBranch.class);
        create(TabBranch.class);
        create(ItemEditorBranch.class);
        create(InfoPanelBranch.class);
        create(ConsoleBranch.class);
        create(CommandConsoleBranch.class);
        create(ProfilerBranch.class);
        create(WorldMapBranch.class);
        create(WorldEditorBranch.class);
        create(CharacterEditorBranch.class);
        create(AnimationEditorBranch.class);
        create(AnimationTreeEditorBranch.class);
        create(RecorderBranch.class);
    }
}
