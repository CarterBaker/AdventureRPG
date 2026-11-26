package editor.bootstrap.treeimpostorpipeline.treeimpostormanager;

import java.io.File;

import application.bootstrap.worldpipeline.tree.TreeHandle;
import application.bootstrap.worldpipeline.tree.TreeImpostorStruct;
import application.bootstrap.worldpipeline.treemanager.TreeImpostorArpgUtility;
import application.bootstrap.worldpipeline.treemanager.TreeManager;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeImpostorManager extends ManagerPackage {

    /*
     * Keeps every tree species' impostor current with its file. At start each
     * species whose impostor is missing, or carries another source than its
     * file gives now, is queued; a species saved, through any window, goes to
     * the front at once, carrying the source of what was saved. One species
     * bakes at a time through TreeImpostorBakeBranch, a view a frame, so the
     * editor never stalls; a species queued twice bakes once, from its latest
     * source. What bakes and how many wait is told through the status, which
     * the tree tabs show.
     */

    // Internal
    private TreeManager treeManager;
    private InfoManager infoManager;
    private TreeImpostorBakeBranch treeImpostorBakeBranch;

    // Queue — the species waiting and the source each carries, front first
    private ObjectArrayList<String> queuedNames;
    private LongArrayList queuedSources;

    // Status
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.treeImpostorBakeBranch = create(TreeImpostorBakeBranch.class);

        // Queue
        this.queuedNames = new ObjectArrayList<>();
        this.queuedSources = new LongArrayList();
    }

    @Override
    protected void get() {
        this.treeManager = get(TreeManager.class);
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {
        infoManager.addSaveListener(EditorSetting.INFO_SCHEMA_TREES, this::applySave);
    }

    @Override
    protected void start() {
        queueStale();
    }

    // Update \\

    @Override
    protected void update() {

        if (treeImpostorBakeBranch.isIdle()) {
            beginNext();
            return;
        }

        if (treeImpostorBakeBranch.step())
            revision++;
    }

    // Queue \\

    // Every loaded species whose impostor is missing or stale, in palette order
    private void queueStale() {

        File treeRoot = new File(EngineSetting.TREE_PATH);

        for (int slot = 1; slot < treeManager.getTreeSlotCount(); slot++) {

            TreeHandle treeHandle = treeManager.findTreeHandleByID((short) slot);

            if (treeHandle == null)
                continue;

            ArpgObjectStruct treeArpg = ArpgUtility.tryLoadObject(
                    ArpgUtility.resolveFile(treeRoot, treeHandle.getTreeName()));

            if (treeArpg == null)
                continue;

            long source = TreeImpostorArpgUtility.computeSource(treeArpg);
            TreeImpostorStruct impostor = treeHandle.getImpostor();

            if (impostor == null || impostor.getSource() != source)
                queue(treeHandle.getTreeName(), source, false);
        }
    }

    // A species just saved baked next, from what was saved
    private void applySave(InfoEntryStruct entry) {
        queue(entry.getEntryName(), TreeImpostorArpgUtility.computeSource(entry.getArpg()), true);
    }

    // A species queued at the back or the front, any earlier place it held given up
    private void queue(String treeName, long source, boolean first) {

        int index = queuedNames.indexOf(treeName);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            queuedNames.remove(index);
            queuedSources.removeLong(index);
        }

        queuedNames.add(first ? 0 : queuedNames.size(), treeName);
        queuedSources.add(first ? 0 : queuedSources.size(), source);
        revision++;
    }

    // The species at the front taken off and begun, those no longer loaded passed over
    private void beginNext() {

        while (!queuedNames.isEmpty()) {

            String treeName = queuedNames.remove(0);
            long source = queuedSources.removeLong(0);

            revision++;

            if (treeImpostorBakeBranch.begin(treeName, source))
                return;
        }
    }

    // Accessible \\

    // The species baking now, null while none is
    public String getBakingName() {
        return treeImpostorBakeBranch.getTreeName();
    }

    public int getQueuedCount() {
        return queuedNames.size();
    }

    public int getRevision() {
        return revision;
    }
}
