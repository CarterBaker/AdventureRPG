package application.bootstrap.itempipeline.tooltypemanager;

import java.io.File;

import application.bootstrap.itempipeline.tooltype.ToolSwing;
import application.bootstrap.itempipeline.tooltype.ToolTypeData;
import application.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ToolTypeBuilder extends BuilderPackage {

    /*
     * Parses tool type ARPG files and builds ToolTypeHandle instances. Each
     * ARPG file may contain multiple tool entries under a 'tools' array, each
     * under the ID ToolTypeManager assigns its name, with the model it is drawn
     * with and how it is swung, overhead unless it says level.
     * Bootstrap-only.
     */

    // Internal
    private ToolTypeManager toolTypeManager;

    // Base \\

    @Override
    protected void get() {
        this.toolTypeManager = get(ToolTypeManager.class);
    }

    // Build \\

    ObjectArrayList<ToolTypeHandle> build(File arpgFile, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, arpgFile);
        ArpgObjectStruct rootArpg = ArpgUtility.loadObject(arpgFile);
        ArpgArrayStruct toolArray = ArpgUtility.validateArray(rootArpg, "tools");
        ObjectArrayList<ToolTypeHandle> tools = new ObjectArrayList<>();

        for (int i = 0; i < toolArray.size(); i++) {
            ArpgObjectStruct toolArpg = toolArray.get(i).getAsObject();
            ToolTypeHandle tool = parseTool(toolArpg, pathPrefix);
            if (tool != null)
                tools.add(tool);
        }

        return tools;
    }

    // Parse \\

    private ToolTypeHandle parseTool(ArpgObjectStruct toolArpg, String pathPrefix) {

        String localName = ArpgUtility.validateString(toolArpg, "name");
        String toolTypeName = pathPrefix + "/" + localName;
        short toolTypeID = toolTypeManager.registerToolTypeName(toolTypeName);
        String defaultModelPath = ArpgUtility.getString(toolArpg, "model", "");
        ToolSwing swing = ArpgUtility.getEnum(toolArpg, "swing", ToolSwing.class, ToolSwing.OVERHEAD);

        ToolTypeData toolTypeData = new ToolTypeData(toolTypeName, localName, toolTypeID, defaultModelPath, swing);

        ToolTypeHandle tool = create(ToolTypeHandle.class);
        tool.constructor(toolTypeData);

        return tool;
    }
}