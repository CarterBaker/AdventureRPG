package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.io.File;
import java.util.List;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.JsonUtility;
import com.AdventureRPG.Core.Util.Exceptions.FileException;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;
import com.google.gson.JsonObject;

public class InternalBuildSystem extends SystemFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Base \\

    @Override
    protected void init() {
        this.internalLoadManager = gameEngine.get(InternalLoadManager.class);
    }

    // Compile \\

    void parseShaderFile(ShaderDataInstance shaderDataInstance) {
        parseVersionInfo(shaderDataInstance);
        parseLayouts(shaderDataInstance);
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

        String[] parts = splitLine(versionLines.get(0));
        int version = (parts.length >= 2) ? parseIntSafe(parts[1], 0) : 0;
        shaderDataInstance.setVersion(version);
    }

    // Layouts \\

    private void parseLayouts(ShaderDataInstance shaderDataInstance) {

        File shaderFile = shaderDataInstance.shaderFile();
        var allLines = FileUtility.readAllLines(shaderFile);

        int i = 0;

        while (i < allLines.size()) {

            if (!allLines.get(i).trim().contains("layout")) {
                i++;
                continue;
            }

            int binding = extractBindingValue(allLines, i);
            i = findLineAfter(allLines, i, ")");

            String blockName = extractBlockName(allLines, i);
            if (blockName == null) {
                i++;
                continue;
            }

            int braceIndex = findLineAfter(allLines, i, "{");
            if (braceIndex == -1) {
                i++;
                continue;
            }

            var blockLines = GLSLUtility.extractBracketBlock(shaderFile, allLines.get(braceIndex));
            LayoutDataInstance block = buildLayoutBlock(blockName, binding, blockLines);
            shaderDataInstance.addLayoutBlock(block);

            i = braceIndex + 1;
        }
    }

    private LayoutDataInstance buildLayoutBlock(String blockName, int binding, List<String> blockLines) {

        LayoutDataInstance block = new LayoutDataInstance(blockName, binding);
        int index = 0;

        for (String line : blockLines) {

            String trimmed = trimAndRemoveSemicolon(line);
            if (trimmed.isEmpty() || trimmed.startsWith("//"))
                continue;

            String[] parts = splitLine(trimmed);
            if (parts.length < 2)
                continue;

            UniformType utype = UniformType.fromString(parts[0]);
            if (utype != null)
                block.addUniform(index++, new UniformDataInstance(utype, parts[1]));
        }

        return block;
    }

    // Includes \\

    private void parseIncludes(ShaderDataInstance shaderDataInstance) {

        File shaderFile = shaderDataInstance.shaderFile();
        var includeLines = GLSLUtility.findLinesStartingWith(shaderFile, "#include");

        for (String line : includeLines) {
            String includeName = extractIncludePath(line);

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

            String trimmed = trimAndRemoveSemicolon(line);
            String[] parts = splitLine(trimmed);

            if (parts.length < 3)
                continue;

            UniformType type = UniformType.fromString(parts[1]);
            if (type == null)
                throw new GraphicException.ShaderProgramException(
                        "There was a problem retrieving the uniform type. Uniform type not found");

            shaderDataInstance.addUniform(new UniformDataInstance(type, parts[2]));
        }
    }

    // Compile \\

    ShaderDefinitionInstance compileShader(File jsonFile) {

        String shaderName = FileUtility.getFileName(jsonFile);
        JsonObject obj = JsonUtility.loadJsonObject(jsonFile);
        ShaderDataInstance vertData = internalLoadManager.getShaderData(obj.get("vert").getAsString());
        ShaderDataInstance fragData = internalLoadManager.getShaderData(obj.get("frag").getAsString());

        if (vertData == null || fragData == null)
            throw new FileException.FileReadException(
                    "Json data error: " + jsonFile.getName() + ", The vert or frag file defined could not be found");

        if (vertData.shaderType() != ShaderType.VERT || fragData.shaderType() != ShaderType.FRAG)
            throw new GraphicException.ShaderProgramException(
                    "Json data error: " + jsonFile.getName()
                            + ", The vert or frag files defined do not match the corresponding type");

        return sortIncludes(new ShaderDefinitionInstance(shaderName, vertData, fragData));
    }

    private ShaderDefinitionInstance sortIncludes(ShaderDefinitionInstance shaderDefinition) {

        collectRecursiveIncludes(shaderDefinition, shaderDefinition.vert);
        collectRecursiveIncludes(shaderDefinition, shaderDefinition.frag);

        return shaderDefinition;
    }

    private void collectRecursiveIncludes(ShaderDefinitionInstance shaderDefinition, ShaderDataInstance shaderData) {

        if (shaderData.shaderType() == ShaderType.INCLUDE && !shaderDefinition.getIncludes().contains(shaderData))
            shaderDefinition.addInclude(shaderData);

        for (ShaderDataInstance include : shaderData.getIncludes())
            collectRecursiveIncludes(shaderDefinition, include);
    }

    // Utility \\

    private String[] splitLine(String line) {
        return line.trim().split("\\s+");
    }

    private String trimAndRemoveSemicolon(String line) {
        String trimmed = line.trim();
        return trimmed.endsWith(";") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int findLineAfter(List<String> lines, int startIndex, String token) {
        for (int i = startIndex; i < lines.size(); i++) {
            if (lines.get(i).contains(token))
                return i;
        }
        return -1;
    }

    private int extractBindingValue(List<String> lines, int startIndex) {
        StringBuilder header = new StringBuilder();
        int i = startIndex;

        while (i < lines.size()) {
            String line = lines.get(i).trim();
            header.append(" ").append(line);
            if (line.contains(")"))
                break;
            i++;
        }

        String text = header.toString();
        int open = text.indexOf("(");
        int close = text.indexOf(")");

        if (open != -1 && close != -1) {
            String inside = text.substring(open + 1, close);
            for (String token : inside.split(",")) {
                token = token.trim();
                if (token.startsWith("binding")) {
                    int eq = token.indexOf('=');
                    if (eq != -1)
                        return parseIntSafe(token.substring(eq + 1).trim(), -1);
                }
            }
        }

        return -1;
    }

    private String extractBlockName(List<String> lines, int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("uniform")) {
                String[] parts = splitLine(line);
                if (parts.length >= 2)
                    return parts[1].replace("{", "").trim();
                break;
            }
        }
        return null;
    }

    private String extractIncludePath(String line) {
        line = line.trim();
        if (!line.startsWith("#include"))
            return null;

        String remainder = line.substring(8).trim();

        if ((remainder.startsWith("<") && remainder.endsWith(">")) ||
                (remainder.startsWith("\"") && remainder.endsWith("\"")))
            return remainder.substring(1, remainder.length() - 1);

        return remainder;
    }
}