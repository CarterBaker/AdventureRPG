package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.JsonUtility;
import com.google.gson.JsonObject;

public class InternalBuildSystem extends SystemFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Base \\

    @Override
    protected void init() {

        // Internal
        this.internalLoadManager = gameEngine.get(InternalLoadManager.class);
    }

    // Compile \\

    void parseShaderFile(ShaderDataInstance shaderDataInstance) {
        parseVersionInfo(shaderDataInstance);
        parseIncludes(shaderDataInstance);
        parseUniforms(shaderDataInstance);
    }

    // Version \\

    private void parseVersionInfo(ShaderDataInstance shaderDataInstance) {

        File shaderFile = shaderDataInstance.shaderFile();
        var versionLines = GLSLUtility.findLinesStartingWith(shaderFile, "#version");

        if (versionLines.isEmpty()) {
            shaderDataInstance.setVersion(0);
            return;
        }

        String line = versionLines.get(0);
        String[] parts = line.split("\\s+");

        if (parts.length < 2) {
            shaderDataInstance.setVersion(0);
            return;
        }

        try {
            int version = Integer.parseInt(parts[1]);
            shaderDataInstance.setVersion(version);
        }

        catch (NumberFormatException e) {
            shaderDataInstance.setVersion(0);
        }
    }

    // Includes \\

    private void parseIncludes(ShaderDataInstance shaderDataInstance) {

        File shaderFile = shaderDataInstance.shaderFile();
        var includeLines = GLSLUtility.findLinesStartingWith(shaderFile, "#include");

        for (String line : includeLines) {

            String includeName = null;
            line = line.trim();

            if (line.startsWith("#include")) {

                String remainder = line.substring(8).trim();

                if ((remainder.startsWith("<") && remainder.endsWith(">")) ||
                        (remainder.startsWith("\"") && remainder.endsWith("\"")))
                    includeName = remainder.substring(1, remainder.length() - 1);
                else
                    includeName = remainder;
            }

            if (includeName != null && !includeName.isEmpty()) {

                ShaderDataInstance includeShader = internalLoadManager.getShaderData(includeName);

                if (includeShader != null)
                    shaderDataInstance.addIncludes(includeShader);
            }
        }
    }

    // Uniforms \\

    private void parseUniforms(ShaderDataInstance shaderDataInstance) {

        File shaderFile = shaderDataInstance.shaderFile();
        var uniformLines = GLSLUtility.findLinesStartingWith(shaderFile, "uniform");

        for (String line : uniformLines) {

            String trimmed = line.trim();

            if (trimmed.endsWith(";"))
                trimmed = trimmed.substring(0, trimmed.length() - 1);

            String[] parts = trimmed.split("\\s+");
            if (parts.length < 3)
                continue;

            String typeString = parts[1];
            String name = parts[2];

            UniformType type = UniformType.fromString(typeString);
            if (type == null) // TODO: Add my own custom error here
                continue;

            shaderDataInstance.addUniform(new UniformDataInstance(type, name, line));
        }
    }

    // Compile \\

    ShaderDefinitionInstance compileShader(File jsonFile) {

        String shaderName = FileUtility.getFileName(jsonFile);
        JsonObject obj = JsonUtility.loadJsonObject(jsonFile);
        ShaderDataInstance vertData = internalLoadManager.getShaderData(obj.get("vert").getAsString());
        ShaderDataInstance fragData = internalLoadManager.getShaderData(obj.get("frag").getAsString());

        if (vertData == null || fragData == null)
            return null; // TODO: Throw a custom error

        if (vertData.shaderType() != ShaderType.VERT || fragData.shaderType() != ShaderType.FRAG)
            return null; // TODO: Throw a custom error

        return sortIncludes(
                new ShaderDefinitionInstance(
                        shaderName,
                        vertData,
                        fragData));
    }

    private ShaderDefinitionInstance sortIncludes(ShaderDefinitionInstance shaderDefinition) {

        collectRecursiveIncludes(shaderDefinition, shaderDefinition.vert);
        collectRecursiveIncludes(shaderDefinition, shaderDefinition.frag);

        return shaderDefinition;
    }

    private void collectRecursiveIncludes(
            ShaderDefinitionInstance shaderDefinition,
            ShaderDataInstance shaderData) {

        if (shaderData.shaderType() == ShaderType.INCLUDE &&
                !shaderDefinition.getIncludes().contains(shaderData))
            shaderDefinition.addInclude(shaderData);

        for (ShaderDataInstance include : shaderData.getIncludes())
            collectRecursiveIncludes(shaderDefinition, include);
    }
}
