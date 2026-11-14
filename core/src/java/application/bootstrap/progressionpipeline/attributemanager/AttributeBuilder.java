package application.bootstrap.progressionpipeline.attributemanager;

import java.io.File;

import application.bootstrap.progressionpipeline.attribute.AttributeData;
import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AttributeBuilder extends BuilderPackage {

    /*
     * Parses attribute ARPG files and builds AttributeHandle instances. Each
     * file lists its attributes under an "attributes" array, each named by its
     * "name" field and registered under the ID AttributeManager assigns that
     * name, in file order. Bootstrap-only.
     */

    // Internal
    private AttributeManager attributeManager;

    // Base \\

    @Override
    protected void get() {
        this.attributeManager = get(AttributeManager.class);
    }

    // Build \\

    ObjectArrayList<AttributeHandle> build(File arpgFile) {

        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct attributeArray = ArpgUtility.validateArray(rootArpg, "attributes");
        ObjectArrayList<AttributeHandle> attributes = new ObjectArrayList<>();

        for (int i = 0; i < attributeArray.size(); i++)
            attributes.add(parseAttribute(attributeArray.get(i).getAsObject()));

        return attributes;
    }

    // Parse \\

    private AttributeHandle parseAttribute(ArpgObjectStruct attributeArpg) {

        String attributeName = ArpgUtility.validateString(attributeArpg, "name");
        short attributeID = attributeManager.registerAttributeName(attributeName);

        AttributeData attributeData = new AttributeData(
                attributeName,
                attributeID,
                ArpgUtility.getString(attributeArpg, "display_name", attributeName),
                ArpgUtility.getString(attributeArpg, "abbreviation", attributeName),
                ArpgUtility.getString(attributeArpg, "description", EngineSetting.PROGRESSION_TEXT_NONE));

        AttributeHandle attributeHandle = create(AttributeHandle.class);
        attributeHandle.constructor(attributeData);

        return attributeHandle;
    }
}
