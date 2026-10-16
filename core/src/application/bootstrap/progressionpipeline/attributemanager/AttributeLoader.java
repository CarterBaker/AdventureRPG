package application.bootstrap.progressionpipeline.attributemanager;

import java.io.File;

import application.bootstrap.progressionpipeline.attribute.AttributeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AttributeLoader extends LoaderPackage {

    /*
     * Scans the attribute ARPG directory and loads every attribute into
     * AttributeManager. Each attribute's name is mapped to its file during
     * scan, so a name asked for before the bootstrap batch reaches it loads
     * its file on demand.
     */

    // Internal
    private File root;
    private AttributeManager attributeManager;
    private AttributeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> attributeName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ATTRIBUTE_PATH);
        this.attributeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Attribute directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(AttributeBuilder.class);
    }

    @Override
    protected void get() {
        this.attributeManager = get(AttributeManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct attributeArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "attributes");

        for (int i = 0; i < attributeArray.size(); i++) {
            ArpgObjectStruct attributeArpg = attributeArray.get(i).getAsObject();
            attributeName2File.put(ArpgUtility.validateString(attributeArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<AttributeHandle> attributes = internalBuilder.build(file);

        for (int i = 0; i < attributes.size(); i++)
            attributeManager.addAttributeHandle(attributes.get(i));
    }

    // On-Demand \\

    void request(String attributeName) {

        File file = attributeName2File.get(attributeName);

        if (file == null)
            throwException("On-demand attribute load failed — no file found for: \"" + attributeName + "\"");

        request(file);
    }
}
