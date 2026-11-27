package editor.bootstrap.layoutplannerpipeline.layoutplannermanager;

import java.util.Objects;

import application.bootstrap.worldpipeline.world.WorldPlacementKind;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import editor.bootstrap.infopipeline.infoentry.InfoEntryStruct;
import editor.bootstrap.infopipeline.infomanager.InfoManager;
import editor.bootstrap.layerspipeline.layers.LayerNodeStruct;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerNode;
import editor.bootstrap.layoutplannerpipeline.layoutplanner.LayoutPlannerParameter;
import editor.runtime.EditorSetting;
import editor.runtime.characterview.panel.CharacterViewRowStruct;
import engine.root.ManagerPackage;
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
     * in the type's entry through LayoutPlannerEditBranch. The Tools tab's
     * Preview actions plan the next layout or site, and the File menu makes,
     * saves and reverts settlement types.
     */

    // Internal
    private InfoManager infoManager;
    private LayoutPlannerEditBranch layoutPlannerEditBranch;

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

        // Internal
        this.layoutPlannerEditBranch = create(LayoutPlannerEditBranch.class);

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
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_SETTLEMENTS, EditorSetting.LAYOUT_PLANNER_ROW_SETTLEMENT,
                settlementName);

        rows.add(CharacterViewRowStruct.header(EditorSetting.LAYOUT_PLANNER_PALETTE_ARCHITECTURES));
        rows.add(CharacterViewRowStruct.item(
                EditorSetting.LAYOUT_PLANNER_ROW_ARCHITECTURE, EditorSetting.LAYOUT_PLANNER_LABEL_BY_BIOME,
                architectureName.isEmpty()));
        addEntryRows(rows, EditorSetting.INFO_SCHEMA_ARCHITECTURES, EditorSetting.LAYOUT_PLANNER_ROW_ARCHITECTURE,
                architectureName);
    }

    private void addEntryRows(
            ObjectArrayList<CharacterViewRowStruct> rows,
            String schemaName,
            String prefix,
            String chosen) {

        ObjectArrayList<InfoEntryStruct> entries = infoManager.getEntries(schemaName);

        for (int i = 0; i < entries.size(); i++) {

            String name = entries.get(i).getEntryName();
            rows.add(CharacterViewRowStruct.item(prefix + name, name, name.equals(chosen)));
        }
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

            LayoutPlannerParameter parameter = parameters[i];

            if (parameter.node != node)
                continue;

            rows.add(parameter.choices != null
                    ? CharacterViewRowStruct.choice(parameter.name(), parameter.label, formatValue(parameter))
                    : CharacterViewRowStruct.ranged(
                            parameter.name(), parameter.label, formatValue(parameter),
                            layoutPlannerEditBranch.read(settlementName, parameter),
                            parameter.min, parameter.max, parameter.step));
        }
    }

    // A field moved a whole number of its steps, a choice round its names
    public void stepField(String key, int steps) {

        LayoutPlannerParameter parameter = LayoutPlannerParameter.valueOf(key);

        if (!layoutPlannerEditBranch.step(settlementName, parameter, steps)) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return;
        }

        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    public String getFieldText(String key) {
        return formatValue(LayoutPlannerParameter.valueOf(key));
    }

    public boolean isFieldValid(String key, String text) {
        return LayoutPlannerParameter.valueOf(key).choices == null && parseField(text) != null;
    }

    // A field set to a value typed, held within its bounds
    public void setField(String key, String text) {

        LayoutPlannerParameter parameter = LayoutPlannerParameter.valueOf(key);
        Float value = parseField(text);

        if (value == null)
            return;

        if (!layoutPlannerEditBranch.set(settlementName, parameter, value)) {
            setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_NONE);
            return;
        }

        setStatusMessage(EditorSetting.LAYOUT_PLANNER_MESSAGE_SET + parameter.label + " " + formatValue(parameter));
    }

    private Float parseField(String text) {

        try {
            float value = Float.parseFloat(text.trim());
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // A field's value as the planner shows it: a choice by its name, a whole number as one, a decimal to two places
    private String formatValue(LayoutPlannerParameter parameter) {

        float value = layoutPlannerEditBranch.read(settlementName, parameter);

        if (parameter.choices != null)
            return parameter.choices.getEnumConstants()[Math.round(value)].name().toLowerCase();

        if (parameter.whole)
            return Integer.toString(Math.round(value));

        return String.format(EditorSetting.LAYOUT_PLANNER_VALUE_FORMAT, value);
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
