package editor.bootstrap.layoutplannerpipeline.layoutplannermanager;

import java.util.Objects;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerNode;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerParameter;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerRole;
import editor.bootstrap.propertiespipeline.util.PaletteRowUtility;
import editor.bootstrap.propertiespipeline.util.PlannerFieldUtility;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutPlannerManager extends ManagerPackage {

    /*
     * Owns the Layout Planner's shared state: the settlement type designed,
     * the one the Hierarchy and the Palette tab pick, the node of its layout
     * picked in the Layers tab, which nodes the preview hides, and how the
     * preview is planned: the architecture it is built in, empty for the one
     * the ground under it picks, the numbered layout it is planned from and
     * the site it stands at, among the places the world names. The Layers tab
     * lists the layout as nodes, Settlement the root and its stages under it,
     * each labelled with what its fields say; the Properties tab holds the
     * fields of the node picked, each with a slider and a typed value, edited
     * in the type's entry through PlannerFieldUtility, and its roles, each
     * stepped round every role an architecture fills. The Tools tab's
     * Preview actions plan the next layout or site, and the File menu makes,
     * saves and reverts settlement types.
     */

    // Internal
    private InfoManager infoManager;

    // Choice
    private String settlementName;
    private LayoutPlannerNode node;
    private boolean[] hidden;

    // Preview
    private String architectureName;
    private int layout;
    private int site;

    // Status
    private String statusMessage;
    private int revision;

    // Base \\

    @Override
    protected void create() {

        // Choice
        this.node = LayoutPlannerNode.SETTLEMENT;
        this.hidden = new boolean[LayoutPlannerNode.values().length];

        // Preview
        this.architectureName = "";
        this.layout = EditorSetting.SETTLEMENT_PLANNER_LAYOUT_DEFAULT;

        // Status
        this.statusMessage = "";
    }

    @Override
    protected void get() {
        this.infoManager = get(InfoManager.class);
    }

    @Override
    protected void awake() {

        infoManager.addSelectionListener(
                EditorSetting.INFO_SCHEMA_SETTLEMENTS, entry -> applySelection(entry.getEntryName()));
        infoManager.addChangeListener(EditorSetting.INFO_SCHEMA_SETTLEMENTS, this::applyChange);
    }

    // Selection \\

    // A settlement type picked in the Palette tab, opened in the Hierarchy and the Info Panel alike
    public void selectSettlement(String selectedName) {

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_SETTLEMENTS, selectedName, selectedName);
        applySelection(selectedName);
    }

    private void applySelection(String selectedName) {

        if (Objects.equals(selectedName, settlementName))
            return;

        this.settlementName = selectedName;
        notifyChanged();
    }

    private void applyChange(InfoEntryStruct entry) {

        if (entry.getEntryName().equals(settlementName))
            notifyChanged();
    }

    // The settlement type opened in the Hierarchy so the Info Panel's own New and Duplicate act on it — false with
    // none
    public boolean focusSettlement() {

        if (settlementName == null) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return false;
        }

        infoManager.selectEntry(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName);
        return true;
    }

    // Layers \\

    // Settlement the root, its stages under it, each labelled with what its fields say
    public void buildLayers(ObjectArrayList<LayerNodeStruct> roots) {

        LayoutPlannerNode[] nodes = LayoutPlannerNode.values();
        LayerNodeStruct root = new LayerNodeStruct(
                nodes[0].name(), describeNode(nodes[0]), true, false, true, true);

        if (node == nodes[0])
            root.markChosen();

        for (int i = 1; i < nodes.length; i++)
            root.addChild(new LayerNodeStruct(
                    nodes[i].name(), describeNode(nodes[i]), nodes[i] == node,
                    nodes[i].hideable, !hidden[i], false));

        roots.add(root);
    }

    public void selectLayer(String layerKey) {

        LayoutPlannerNode selected = LayoutPlannerNode.valueOf(layerKey);

        if (selected == node)
            return;

        this.node = selected;
        notifyChanged();
    }

    // A stage's eye: what it lays hidden in the preview or shown again
    public void toggleLayerShown(String layerKey) {

        LayoutPlannerNode toggled = LayoutPlannerNode.valueOf(layerKey);

        if (!toggled.hideable)
            return;

        hidden[toggled.ordinal()] = !hidden[toggled.ordinal()];
        notifyChanged();
    }

    // A node's name, then each of its fields' values in order
    private String describeNode(LayoutPlannerNode described) {

        StringBuilder label = new StringBuilder(described.displayName);

        if (settlementName == null)
            return label.toString();

        LayoutPlannerParameter[] parameters = LayoutPlannerParameter.values();
        int shown = 0;

        for (int i = 0; i < parameters.length && shown < EditorSetting.LAYOUT_PLANNER_SUMMARY_PARAMETERS; i++) {

            if (parameters[i].node != described)
                continue;

            label.append(shown == 0
                    ? EditorSetting.LAYOUT_PLANNER_LABEL_SEPARATOR
                    : EditorSetting.LAYOUT_PLANNER_VALUE_SEPARATOR).append(formatValue(parameters[i]));
            shown++;
        }

        return label.toString();
    }

    // Palette \\

    // Every settlement type, then every architecture the preview may be built in
    public void buildPaletteRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(EditorSetting.LAYOUT_PLANNER_PALETTE_SETTLEMENTS));
        PaletteRowUtility.addEntryRows(
                infoManager, rows, EditorSetting.INFO_SCHEMA_SETTLEMENTS,
                EditorSetting.LAYOUT_PLANNER_ROW_SETTLEMENT, settlementName);

        rows.add(CharacterViewRowStruct.header(EditorSetting.LAYOUT_PLANNER_PALETTE_ARCHITECTURES));
        rows.add(CharacterViewRowStruct.item(
                EditorSetting.LAYOUT_PLANNER_ROW_ARCHITECTURE, EditorSetting.LAYOUT_PLANNER_LABEL_BY_BIOME,
                architectureName.isEmpty()));
        PaletteRowUtility.addEntryRows(
                infoManager, rows, EditorSetting.INFO_SCHEMA_ARCHITECTURES,
                EditorSetting.LAYOUT_PLANNER_ROW_ARCHITECTURE, architectureName);
    }

    // A Palette row chosen: a settlement type to design, or the architecture its preview is built in
    public void choosePaletteRow(String rowKey) {

        if (rowKey.startsWith(EditorSetting.LAYOUT_PLANNER_ROW_SETTLEMENT)) {
            selectSettlement(rowKey.substring(EditorSetting.LAYOUT_PLANNER_ROW_SETTLEMENT.length()));
            return;
        }

        this.architectureName = rowKey.substring(EditorSetting.LAYOUT_PLANNER_ROW_ARCHITECTURE.length());
        notifyChanged();
    }

    // Fields \\

    // The picked node's fields, a choice stepped round its names and a number slid across its bounds
    public void buildFieldRows(ObjectArrayList<CharacterViewRowStruct> rows) {

        rows.add(CharacterViewRowStruct.header(node.displayName + EditorSetting.LAYOUT_PLANNER_FIELDS_SUFFIX));

        if (settlementName == null)
            return;

        LayoutPlannerParameter[] parameters = LayoutPlannerParameter.values();

        for (int i = 0; i < parameters.length; i++) {

            if (parameters[i].node != node)
                continue;

            rows.add(PlannerFieldUtility.toRow(
                    infoManager, EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, parameters[i]));
        }

        LayoutPlannerRole[] roles = LayoutPlannerRole.values();

        for (int i = 0; i < roles.length; i++)
            if (roles[i].node == node)
                rows.add(CharacterViewRowStruct.choice(roles[i].name(), roles[i].label, readRole(roles[i])));
    }

    // A field moved a whole number of its steps, a choice round its names, a role round every role an architecture
    // fills
    public void stepField(String key, int steps) {

        if (isRole(key)) {
            stepRole(LayoutPlannerRole.valueOf(key), steps);
            return;
        }

        LayoutPlannerParameter parameter = LayoutPlannerParameter.valueOf(key);

        if (!PlannerFieldUtility.step(
                infoManager, EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, parameter, steps)) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return;
        }

        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    public String getFieldText(String key) {
        return isRole(key)
                ? readRole(LayoutPlannerRole.valueOf(key))
                : formatValue(LayoutPlannerParameter.valueOf(key));
    }

    public boolean isFieldValid(String key, String text) {
        return isRole(key)
                ? gatherRoles().contains(text.strip())
                : LayoutPlannerParameter.valueOf(key).choices == null && PlannerFieldUtility.parse(text) != null;
    }

    // A field set to a value typed, held within its bounds
    public void setField(String key, String text) {

        if (isRole(key)) {
            writeRole(LayoutPlannerRole.valueOf(key), text.strip());
            return;
        }

        LayoutPlannerParameter parameter = LayoutPlannerParameter.valueOf(key);
        Float value = PlannerFieldUtility.parse(text);

        if (value == null)
            return;

        if (!PlannerFieldUtility.set(
                infoManager, EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, parameter, value)) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return;
        }

        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    private String formatValue(LayoutPlannerParameter parameter) {
        return PlannerFieldUtility.format(
                infoManager, EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, parameter);
    }

    // Roles \\

    private boolean isRole(String key) {
        return key.startsWith(EditorSetting.LAYOUT_PLANNER_ROLE_KEY_PREFIX);
    }

    // The role a field names, empty while the type names none
    private String readRole(LayoutPlannerRole role) {

        InfoEntryStruct entry = findEntry();

        if (entry == null)
            return "";

        ArpgObjectStruct holder = role.group.isEmpty()
                ? entry.getArpg()
                : ArpgUtility.hasObject(entry.getArpg(), role.group) ? entry.getArpg().getAsObject(role.group) : null;

        return holder != null ? ArpgUtility.getString(holder, role.key, "") : "";
    }

    // A role field stepped round every role an architecture fills, in order
    private void stepRole(LayoutPlannerRole role, int steps) {

        ObjectArrayList<String> roleNames = gatherRoles();

        if (roleNames.isEmpty())
            return;

        int current = Math.max(0, roleNames.indexOf(readRole(role)));

        writeRole(role, roleNames.get(Math.floorMod(current + steps, roleNames.size())));
    }

    private void writeRole(LayoutPlannerRole role, String roleName) {

        if (findEntry() == null) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return;
        }

        infoManager.editEntry(
                EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName,
                settlementArpg -> resolveGroup(settlementArpg, role.group).addProperty(role.key, roleName));
        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_SET + role.label + " " + roleName);
    }

    private ArpgObjectStruct resolveGroup(ArpgObjectStruct settlementArpg, String group) {

        if (group.isEmpty())
            return settlementArpg;

        if (!ArpgUtility.hasObject(settlementArpg, group))
            settlementArpg.add(group, new ArpgObjectStruct());

        return settlementArpg.getAsObject(group);
    }

    // Every role any architecture fills, sorted
    private ObjectArrayList<String> gatherRoles() {

        ObjectArrayList<String> roleNames = new ObjectArrayList<>();
        ObjectArrayList<InfoEntryStruct> architectures =
                infoManager.getEntries(EditorSetting.INFO_SCHEMA_ARCHITECTURES);

        for (int i = 0; i < architectures.size(); i++)
            if (ArpgUtility.hasObject(architectures.get(i).getArpg(), "roles"))
                for (String roleName : architectures.get(i).getArpg().getAsObject("roles").keySet())
                    if (!roleNames.contains(roleName))
                        roleNames.add(roleName);

        roleNames.sort(null);

        return roleNames;
    }

    private InfoEntryStruct findEntry() {

        if (settlementName == null || !infoManager.hasDocument(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName))
            return null;

        return infoManager.findEntry(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName, settlementName);
    }

    // Actions \\

    // A Tools tab Preview action run by its key: the next numbered layout, or the next site
    public void act(String actionKey) {

        if (actionKey.equals(EditorSetting.LAYOUT_PLANNER_KEY_NEXT_LAYOUT))
            this.layout++;
        else if (actionKey.equals(EditorSetting.LAYOUT_PLANNER_KEY_NEXT_SITE))
            this.site++;
        else
            return;

        notifyChanged();
    }

    // The placement the preview is planned as, standing at a site among the places the world names, at the world's
    // origin while it names none
    public WorldPlacementStruct resolvePlacement(ObjectArrayList<WorldPlacementStruct> sites) {

        WorldPlacementStruct standing = sites.isEmpty() ? null : sites.get(Math.floorMod(site, sites.size()));

        return new WorldPlacementStruct(
                WorldPlacementKind.SETTLEMENT,
                settlementName, architectureName, layout,
                standing != null ? standing.getWorldX() : 0L, standing != null ? standing.getWorldZ() : 0L, 0);
    }

    // File \\

    public void save() {

        if (!focusSettlement())
            return;

        String message = infoManager.saveDocument(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName)
                ? EditorSetting.LAYOUT_PLANNER_MESSAGE_SAVED
                : EditorSetting.LAYOUT_PLANNER_MESSAGE_SAVE_FAILED;

        setStatusMessage(message + settlementName);
    }

    public void revert() {

        if (!focusSettlement())
            return;

        infoManager.revertDocument(EditorSetting.INFO_SCHEMA_SETTLEMENTS, settlementName);
        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_REVERTED + settlementName);
    }

    // Status \\

    public void setStatusMessage(String message) {

        this.statusMessage = message;
        notifyChanged();
    }

    private void notifyChanged() {
        this.revision++;
    }

    // Accessible \\

    // The settlement type designed, null before one is chosen
    public String getSettlementName() {
        return settlementName;
    }

    public LayoutPlannerNode getNode() {
        return node;
    }

    // Whether the preview hides what a stage lays
    public boolean isHidden(LayoutPlannerNode hiddenNode) {
        return hidden[hiddenNode.ordinal()];
    }

    // The architecture the preview is built in, empty for the one the ground picks
    public String getArchitectureName() {
        return architectureName;
    }

    public int getLayout() {
        return layout;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getRevision() {
        return revision;
    }
}
