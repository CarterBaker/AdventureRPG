package application.bootstrap.itempipeline.tooltypemanager;

import java.io.File;

import application.bootstrap.itempipeline.tooltype.ToolTypeData;
import application.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import engine.root.BuilderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ToolTypeBuilder extends BuilderPackage {

    /*
     * Parses tool type ARPG files and builds ToolTypeHandle instances. Each
     * ARPG file may contain multiple tool entries under a 'tools' array.
     * Bootstrap-only.
     */

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
        short toolTypeID = RegistryUtility.toShortID(toolTypeName);
        String defaultModelPath = ArpgUtility.getString(toolArpg, "model", "");

        ToolTypeData toolTypeData = new ToolTypeData(toolTypeName, localName, toolTypeID, defaultModelPath);

        ToolTypeHandle tool = create(ToolTypeHandle.class);
        tool.constructor(toolTypeData);

        return tool;
    }
}