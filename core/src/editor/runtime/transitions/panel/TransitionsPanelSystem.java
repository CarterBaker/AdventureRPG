package editor.runtime.transitions.panel;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.renderpipeline.fbomanager.FBOManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import editor.bootstrap.recorderpipeline.recordermanager.RecorderManager;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.bootstrap.transitionpipeline.transitionmanager.TransitionManager;
import editor.bootstrap.transitionpipeline.util.TransitionKind;
import editor.runtime.EditorSetting;
import editor.runtime.transitions.TransitionsSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TransitionsPanelSystem extends SystemPackage {

    /*
     * Binds this window to a UI render target and shows the transitions.
     * When the transitions' revision moves it refills the list — a row for
     * every transition by its display name with how long it first runs, the
     * selected one marked — and the selected transition's details: how it
     * carries one picture into the next, the way it sweeps when it sweeps,
     * the colour it passes through or opens onto, its edge, its pace and its
     * length. The status line follows the recorder's, so laying a transition
     * from here says what it did.
     */

    // Internal
    private MenuManager menuManager;
    private FBOManager fboManager;
    private TransitionManager transitionManager;
    private RecorderManager recorderManager;

    // Menus
    private MenuInstance transitionsMenu;
    private ElementInstance statusLabel;
    private ElementInstance listTitle;

    // State
    private int shownRevision;
    private int shownRecorderRevision;

    // Base \\

    @Override
    protected void create() {

        // State
        this.shownRevision = EngineSetting.INDEX_NOT_FOUND;
        this.shownRecorderRevision = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.menuManager = get(MenuManager.class);
        this.fboManager = get(FBOManager.class);
        this.transitionManager = get(TransitionManager.class);
        this.recorderManager = get(RecorderManager.class);
    }

    @Override
    protected void awake() {

        WindowInstance window = context.getWindow();

        menuManager.setMenuTargetFbo(window, fboManager.cloneFbo(RuntimeSetting.FBO_UI, window));
        this.transitionsMenu = menuManager.openMenu(EditorSetting.TRANSITIONS_MENU, window);

        this.statusLabel = requireEntryPoint(TransitionsSetting.ENTRY_STATUS);
        this.listTitle = requireEntryPoint(TransitionsSetting.ENTRY_LIST_TITLE);

        requireEntryPoint(TransitionsSetting.ENTRY_TRANSITIONS);
        requireEntryPoint(TransitionsSetting.ENTRY_DETAILS);
    }

    private ElementInstance requireEntryPoint(int entryPoint) {

        ElementInstance element = transitionsMenu.getEntryPoint(entryPoint);

        if (element == null)
            throwException("Transitions menu '" + EditorSetting.TRANSITIONS_MENU + "' has no entry point "
                    + entryPoint + ".");

        return element;
    }

    // Update \\

    @Override
    protected void update() {

        if (transitionManager.getRevision() != shownRevision) {
            this.shownRevision = transitionManager.getRevision();
            refreshTransitions();
            refreshDetails();
        }

        if (recorderManager.getRevision() != shownRecorderRevision) {
            this.shownRecorderRevision = recorderManager.getRevision();
            statusLabel.setFontText(recorderManager.getStatusText());
        }
    }

    // Transitions \\

    // A row for every transition in name order, each picked up to drag it onto a clip's edge
    private void refreshTransitions() {

        menuManager.ejectAll(transitionsMenu, TransitionsSetting.ENTRY_TRANSITIONS);

        ObjectArrayList<TransitionHandle> transitions = transitionManager.getTransitions();
        TransitionHandle selected = transitionManager.getSelectedTransition();

        listTitle.setFontText(String.format(TransitionsSetting.FORMAT_LIST_TITLE, transitions.size()));

        if (transitions.isEmpty()) {
            injectText(TransitionsSetting.ENTRY_TRANSITIONS, TransitionsSetting.TEXT_NO_TRANSITIONS);
            return;
        }

        for (int i = 0; i < transitions.size(); i++) {

            TransitionHandle transitionHandle = transitions.get(i);
            ElementInstance row = menuManager.inject(
                    transitionsMenu,
                    TransitionsSetting.ENTRY_TRANSITIONS,
                    transitionHandle == selected
                            ? TransitionsSetting.TEMPLATE_TRANSITION_ROW_SELECTED
                            : TransitionsSetting.TEMPLATE_TRANSITION_ROW);

            row.setOnDragArgOverride(transitionHandle.getTransitionName());
            row.findChildById(TransitionsSetting.ELEMENT_ROW_LABEL).setFontText(transitionHandle.getDisplayName());
            row.findChildById(TransitionsSetting.ELEMENT_ROW_VALUE).setFontText(
                    String.format(TransitionsSetting.FORMAT_SECONDS, transitionHandle.getSeconds()));
        }
    }

    // Details \\

    // Everything the selected transition looks like, as its file sets it, and how it is laid
    private void refreshDetails() {

        menuManager.ejectAll(transitionsMenu, TransitionsSetting.ENTRY_DETAILS);

        TransitionHandle transitionHandle = transitionManager.getSelectedTransition();
        TransitionKind kind = transitionHandle.getKind();
        Vector3 color = transitionHandle.getColor();

        injectHeader(transitionHandle.getDisplayName());
        injectDetail(TransitionsSetting.TEXT_NAME, transitionHandle.getTransitionName());
        injectDetail(TransitionsSetting.TEXT_KIND, kind.getLabel());

        if (kind == TransitionKind.WIPE || kind == TransitionKind.PUSH)
            injectDetail(TransitionsSetting.TEXT_DIRECTION, transitionHandle.getDirection().getLabel());

        injectDetail(TransitionsSetting.TEXT_COLOR,
                String.format(TransitionsSetting.FORMAT_COLOR, color.x, color.y, color.z));

        if (kind == TransitionKind.WIPE || kind == TransitionKind.IRIS)
            injectDetail(TransitionsSetting.TEXT_SOFTNESS,
                    String.format(TransitionsSetting.FORMAT_DECIMAL, transitionHandle.getSoftness()));

        injectDetail(TransitionsSetting.TEXT_EASING, transitionHandle.getEasing().getLabel());
        injectDetail(TransitionsSetting.TEXT_LENGTH,
                String.format(TransitionsSetting.FORMAT_SECONDS, transitionHandle.getSeconds()));
        injectText(TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEXT_DRAG_HINT);
        injectText(TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEXT_CUT_HINT);
        injectText(TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEXT_EDGE_HINT);
        injectText(TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEXT_LENGTH_HINT);
        injectText(TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEXT_EDIT_HINT);
    }

    private void injectDetail(String label, String value) {

        ElementInstance row = menuManager.inject(
                transitionsMenu, TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEMPLATE_DETAIL_ROW);

        row.findChildById(TransitionsSetting.ELEMENT_ROW_LABEL).setFontText(label);
        row.findChildById(TransitionsSetting.ELEMENT_ROW_VALUE).setFontText(value);
    }

    // Utility \\

    private void injectHeader(String text) {
        menuManager.inject(transitionsMenu, TransitionsSetting.ENTRY_DETAILS, TransitionsSetting.TEMPLATE_HEADER_ROW)
                .findChildById(TransitionsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }

    private void injectText(int entryPoint, String text) {
        menuManager.inject(transitionsMenu, entryPoint, TransitionsSetting.TEMPLATE_TEXT_ROW)
                .findChildById(TransitionsSetting.ELEMENT_ROW_LABEL).setFontText(text);
    }
}
