package application.bootstrap.dialoguepipeline.relationmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.relation.RelationData;
import application.bootstrap.dialoguepipeline.relation.RelationHandle;
import application.bootstrap.dialoguepipeline.util.DialogueFeatureUtility;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RelationBuilder extends BuilderPackage {

    /*
     * Parses relation ARPG files and builds RelationHandle instances. Each
     * file lists its relations under a "relations" array, each named by its
     * "name" field: the "display_name" and "description" it is shown by, the
     * "bond" of regard it starts from and the "inverse" relation its holder
     * is in turn, itself when none is named. An inverse is registered by name
     * as it is read, so two relations may name each other. The name stranger
     * is reserved for beings that are nothing to each other. Bootstrap-only.
     */

    // Internal
    private RelationManager relationManager;

    // Base \\

    @Override
    protected void get() {
        this.relationManager = get(RelationManager.class);
    }

    // Build \\

    ObjectArrayList<RelationHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct relationArray = ArpgUtility.validateArray(rootArpg, "relations");
        ObjectArrayList<RelationHandle> relations = new ObjectArrayList<>();

        for (int i = 0; i < relationArray.size(); i++)
            relations.add(parseRelation(relationArray.get(i).getAsObject(), arpgFile));

        return relations;
    }

    // Parse \\

    private RelationHandle parseRelation(ArpgObjectStruct relationArpg, File arpgFile) {

        String relationName = ArpgUtility.validateString(relationArpg, "name");
        String owner = "Relation '" + relationName + "' in " + arpgFile.getPath();
        float bond = ArpgUtility.getFloat(relationArpg, "bond", 0f);

        if (relationName.equals(EngineSetting.DIALOGUE_RELATION_STRANGER))
            throwException(owner + " takes the reserved name \"" + EngineSetting.DIALOGUE_RELATION_STRANGER
                    + "\", what beings that are nothing to each other are.");

        if (Math.abs(bond) > EngineSetting.AFFINITY_MAX)
            throwException(owner + " has a \"bond\" of " + bond + " — regard runs from -" + EngineSetting.AFFINITY_MAX
                    + " to " + EngineSetting.AFFINITY_MAX + ".");

        RelationData relationData = new RelationData(
                relationName,
                relationManager.registerRelationName(relationName),
                DialogueFeatureUtility.hashName(relationName),
                ArpgUtility.getString(relationArpg, "display_name", relationName),
                ArpgUtility.getString(relationArpg, "description", EngineSetting.DIALOGUE_TEXT_NONE),
                bond,
                relationManager.registerRelationName(ArpgUtility.getString(relationArpg, "inverse", relationName)));

        RelationHandle relationHandle = create(RelationHandle.class);
        relationHandle.constructor(relationData);

        return relationHandle;
    }
}
