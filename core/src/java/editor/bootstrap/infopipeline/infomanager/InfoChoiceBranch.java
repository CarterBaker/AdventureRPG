package editor.bootstrap.infopipeline.infomanager;

import editor.bootstrap.infopipeline.infoschema.InfoFieldStruct;
import editor.bootstrap.infopipeline.infoschema.InfoReferenceStruct;
import editor.bootstrap.infopipeline.infotarget.InfoTargetStruct;
import editor.bootstrap.infopipeline.util.InfoReferenceKind;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InfoChoiceBranch extends BranchPackage {

    /*
     * Lists every value a field may take, for the Properties tab to step
     * through: false and true, an enum's values in the schema's order, or for
     * a string naming other content the values its reference allows (such as
     * none) followed by every entry of the schema it names, or every file of
     * the directory it names, as the game would find them, in alphabetical
     * order. A keyed reference names the schema its sibling picks. A field
     * naming something inside its own entry is typed, not stepped.
     */

    // Internal
    private InfoEditBranch infoEditBranch;
    private InfoReferenceBranch infoReferenceBranch;

    // Choices — main thread only
    private ObjectArrayList<String> choices;
    private ObjectArrayList<String> names;

    // Base \\

    @Override
    protected void create() {

        // Choices
        this.choices = new ObjectArrayList<>();
        this.names = new ObjectArrayList<>();
    }

    @Override
    protected void get() {
        this.infoEditBranch = get(InfoEditBranch.class);
        this.infoReferenceBranch = get(InfoReferenceBranch.class);
    }

    // Choices \\

    boolean isChoice(InfoFieldStruct field) {

        if (field == null)
            return false;

        return switch (field.getType()) {
            case BOOLEAN, ENUM -> true;
            case STRING -> field.hasReference() && field.getReference().getKind() != InfoReferenceKind.LOCAL;
            default -> false;
        };
    }

    // Writes the choice a number of places along from the value's own, wrapping at either end
    void cycle(InfoTargetStruct target, int direction) {

        collectChoices(target);

        if (choices.isEmpty())
            return;

        int current = findChoice(infoEditBranch.formatValue(target.getValue(), target.resolveType()));
        int next = current != EngineSetting.INDEX_NOT_FOUND
                ? Math.floorMod(current + direction, choices.size())
                : direction > 0 ? 0 : choices.size() - 1;
        ArpgElementStruct value = infoEditBranch.parse(target, choices.get(next));

        if (value != null)
            target.write(value);
    }

    private void collectChoices(InfoTargetStruct target) {

        choices.clear();

        InfoFieldStruct field = target.getField();

        switch (target.resolveType()) {
            case BOOLEAN -> {
                choices.add(Boolean.FALSE.toString());
                choices.add(Boolean.TRUE.toString());
            }
            case ENUM -> choices.addAll(field.getValues());
            case STRING -> collectReferenceChoices(field.getReference(), target.getContainer());
            default -> {
            }
        }
    }

    private void collectReferenceChoices(InfoReferenceStruct reference, ArpgElementStruct container) {

        if (reference == null)
            return;

        choices.addAll(reference.getAllowedValues());
        names.clear();

        switch (reference.getKind()) {
            case SCHEMA -> collectSchemaNames(reference, container);
            case RESOURCE -> infoReferenceBranch.collectResources(
                    reference.getDirectory(), reference.getNaming(), names);
            case LOCAL -> {
            }
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < names.size(); i++)
            if (!choices.contains(names.get(i)))
                choices.add(names.get(i));
    }

    private void collectSchemaNames(InfoReferenceStruct reference, ArpgElementStruct container) {

        String schemaName = resolveSchemaName(reference, container);

        if (schemaName != null)
            infoReferenceBranch.collectNames(schemaName, names);
    }

    // The schema a reference names, a keyed one through its sibling's value, null while the sibling picks none
    private String resolveSchemaName(InfoReferenceStruct reference, ArpgElementStruct container) {

        if (!reference.isKeyed())
            return reference.getSchemaName();

        if (container == null || !container.isObject())
            return null;

        ArpgObjectStruct sibling = container.getAsObject();

        return ArpgUtility.hasString(sibling, reference.getKeyField())
                ? reference.resolveSchemaName(sibling.get(reference.getKeyField()).getAsString())
                : null;
    }

    private int findChoice(String text) {

        for (int i = 0; i < choices.size(); i++)
            if (choices.get(i).equalsIgnoreCase(text))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }
}
