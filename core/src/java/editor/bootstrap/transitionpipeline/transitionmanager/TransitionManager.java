package editor.bootstrap.transitionpipeline.transitionmanager;

import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.transitionpipeline.transition.TransitionData;
import editor.bootstrap.transitionpipeline.transition.TransitionHandle;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TransitionManager extends ManagerPackage {

    /*
     * Owns every transition and the one selected, shared by every tool and
     * window that cuts footage together the way a paint program's brushes
     * are shared by every tool that strokes. A transition is only a look —
     * how it carries one picture into the next, the way it sweeps, its
     * colour, edge and pace — and how long it runs when first laid; how long
     * it runs where it is laid is the clip's. Transitions load from ARPG, one
     * to a file, and are listed by their display names. They are content like
     * any other: the Hierarchy and Info Panel edit, create, copy and delete
     * them, and every change reaches here live, rebuilding the transition in
     * place under its ID, so a clip playing it takes the edit at once. A name
     * no transition answers to any longer plays as the default, and the
     * selection falls back to the default when its transition is deleted. A
     * transition picked in the Hierarchy becomes the selection, and a new
     * selection is shown in the Info Panel.
     */

    // Internal
    private InfoManager infoManager;

    // Palette
    private Object2IntOpenHashMap<String> transitionName2TransitionID;
    private ObjectArrayList<TransitionHandle> transitionID2TransitionHandle;
    private ObjectArrayList<TransitionHandle> transitions;

    // Selection
    private TransitionHandle defaultTransition;
    private TransitionHandle selectedTransition;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.transitionName2TransitionID = RegistryUtility.createNameIndex();
        this.transitionID2TransitionHandle = RegistryUtility.createPalette();
        this.transitions = new ObjectArrayList<>();
        create(TransitionLoader.class);
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        this.defaultTransition = getTransitionHandleFromTransitionName(EditorSetting.TRANSITION_DEFAULT);
        this.selectedTransition = defaultTransition;

        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_TRANSITIONS, this::rebuildTransition);
        infoManager.addDeleteListener(EditorSetting.INFO_SCHEMA_TRANSITIONS, this::retireTransition);
        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_TRANSITIONS,
                entry -> selectTransition(entry.getEntryName()));
    }

    // Management \\

    int registerTransitionName(String transitionName) {
        return RegistryUtility.registerID(
                transitionName2TransitionID,
                transitionID2TransitionHandle,
                transitionName,
                EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // A transition built for the first time is listed; one built again takes its new data in place
    void addTransitionData(TransitionData transitionData) {

        TransitionHandle transitionHandle = RegistryUtility.getHandle(
                transitionID2TransitionHandle, transitionData.getTransitionID());

        if (transitionHandle == null) {
            transitionHandle = create(TransitionHandle.class);
            transitionID2TransitionHandle.set(transitionData.getTransitionID(), transitionHandle);
        }

        transitionHandle.constructor(transitionData);

        if (!transitions.contains(transitionHandle))
            transitions.add(transitionHandle);

        transitions.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getDisplayName(), b.getDisplayName()));
        notifyChanged();
    }

    // Live Edit \\

    private void rebuildTransition(InfoEntryStruct entry) {

        try {
            ((TransitionLoader) internalLoader).build(entry.getEntryName(), entry.getArpg());
        } catch (InternalException e) {
            errorLog(EditorSetting.TRANSITION_MESSAGE_REFUSED + e.getMessage());
        }
    }

    // A deleted transition leaves the list; the selection moves to the default
    private void retireTransition(InfoEntryStruct entry) {

        TransitionHandle transitionHandle = RegistryUtility.getHandle(
                transitionName2TransitionID, transitionID2TransitionHandle, entry.getEntryName());

        if (transitionHandle == null || !transitions.remove(transitionHandle))
            return;

        if (transitionHandle == selectedTransition)
            this.selectedTransition = defaultTransition;

        notifyChanged();
    }

    // Selection \\

    public void selectTransition(String transitionName) {

        if (!hasTransition(transitionName))
            return;

        TransitionHandle transitionHandle = getTransitionHandleFromTransitionName(transitionName);

        if (transitionHandle == selectedTransition)
            return;

        this.selectedTransition = transitionHandle;
        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_TRANSITIONS, transitionName, transitionName);
        notifyChanged();
    }

    // Status \\

    private void notifyChanged() {
        revision++;
    }

    // Accessible \\

    public TransitionHandle getTransitionHandleFromTransitionName(String transitionName) {

        TransitionHandle transitionHandle = RegistryUtility.getHandle(
                transitionName2TransitionID, transitionID2TransitionHandle, transitionName);

        if (transitionHandle == null)
            throwException("Transition not found: \"" + transitionName + "\"");

        return transitionHandle;
    }

    public TransitionHandle getTransitionHandleFromTransitionID(int transitionID) {

        TransitionHandle transitionHandle = RegistryUtility.getHandle(transitionID2TransitionHandle, transitionID);

        if (transitionHandle == null)
            throwException("Transition not found for ID " + transitionID);

        return transitionHandle;
    }

    // True for a transition listed under a name, false for none or one deleted
    public boolean hasTransition(String transitionName) {

        TransitionHandle transitionHandle = RegistryUtility.getHandle(
                transitionName2TransitionID, transitionID2TransitionHandle, transitionName);

        return transitionHandle != null && transitions.contains(transitionHandle);
    }

    // The transition a clip names, or the default when none answers to the name any longer
    public TransitionHandle resolveTransition(String transitionName) {
        return hasTransition(transitionName)
                ? getTransitionHandleFromTransitionName(transitionName)
                : defaultTransition;
    }

    public ObjectArrayList<TransitionHandle> getTransitions() {
        return transitions;
    }

    public TransitionHandle getSelectedTransition() {
        return selectedTransition;
    }

    public int getRevision() {
        return revision;
    }
}
