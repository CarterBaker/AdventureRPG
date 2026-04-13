package application.bootstrap.itempipeline.tooltypemanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.itempipeline.tooltype.ToolTypeData;
import application.bootstrap.itempipeline.tooltype.ToolTypeHandle;
import application.core.engine.BuilderPackage;
import application.core.util.FileUtility;
import application.core.util.JsonUtility;
import application.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses tool type JSON files and builds ToolTypeHandle instances. Each
     * JSON file may contain multiple tool entries under a 'tools' array.
     * Bootstrap-only.
     */

    // Build \\

    ObjectArrayList<ToolTypeHandle> build(File jsonFile, File root) {

        String pathPrefix = FileUtility.getPathWithFileNameWithoutExtension(root, jsonFile);
        JsonObject rootJson = JsonUtility.loadJsonObject(jsonFile);
        JsonArray toolArray = JsonUtility.validateArray(rootJson, "tools");
        ObjectArrayList<ToolTypeHandle> tools = new ObjectArrayList<>();

        for (int i = 0; i < toolArray.size(); i++) {
            JsonObject toolJson = toolArray.get(i).getAsJsonObject();
            ToolTypeHandle tool = parseTool(toolJson, pathPrefix);
            if (tool != null)
                tools.add(tool);
        }

        return tools;
    }

    // Parse \\

    private ToolTypeHandle parseTool(JsonObject toolJson, String pathPrefix) {

        String localName = JsonUtility.validateString(toolJson, "name");
        String toolTypeName = pathPrefix + "/" + localName;
        short toolTypeID = RegistryUtility.toShortID(toolTypeName);
        String defaultModelPath = JsonUtility.getString(toolJson, "model", "");

        ToolTypeData toolTypeData = new ToolTypeData(toolTypeName, toolTypeID, defaultModelPath);

        ToolTypeHandle tool = create(ToolTypeHandle.class);
        tool.constructor(toolTypeData);

        return tool;
    }
}