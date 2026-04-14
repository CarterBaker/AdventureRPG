package application.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import application.bootstrap.shaderpipeline.shader.ShaderSourceStruct;
import application.bootstrap.shaderpipeline.shader.ShaderType;
import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalBuilder extends BuilderPackage {

    /*
     * Populates ShaderSourceStructs during scan and assembles program-level
     * ShaderSourceStructs from JSON descriptors. Everything produced here is
     * bootstrap-only and GCs when InternalLoader self-destructs.
     */

    // Internal
    private InternalLoader internalLoader;
    private File root;

    // Base \\

    @Override
    protected void get() {
        this.internalLoader = get(InternalLoader.class);
        this.root = new File(EngineSetting.SHADER_PATH);
    }

    // Parse \\

    void parseShaderFile(ShaderSourceStruct source) {

        String rawText = FileParserUtility.convertFileToRawText(source.getShaderFile());
        ObjectArrayList<String> lines = FileParserUtility.convertRawTextToArray(rawText);

        source.setVersion(parseVersionInfo(lines));

        parseUniforms(source, lines);
        parseIncludes(source, lines);
    }

    // Version \\

    private String parseVersionInfo(ObjectArrayList<String> lines) {
        for (int i = 0; i < lines.size(); i++)
            if (FileParserUtility.lineStartsWith(lines.get(i), "#version"))
                return lines.get(i);
        return null;
    }

    // Uniforms \\

    private void parseUniforms(ShaderSourceStruct source, ObjectArrayList<String> lines) {

        boolean insideBlock = false;
        String currentBlockName = null;
        int braceDepth = 0;

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i).trim();

            if (line.isEmpty())
                continue;

            if (isUniformBlockStart(line)) {

                currentBlockName = parseUniformBlockName(line);

                if (currentBlockName != null) {
                    insideBlock = true;
                    braceDepth = FileParserUtility.countCharInString(line, '{') -
                            FileParserUtility.countCharInString(line, '}');
                }

                continue;
            }

            if (insideBlock) {

                braceDepth += FileParserUtility.countCharInString(line, '{') -
                        FileParserUtility.countCharInString(line, '}');

                if (braceDepth <= 0) {
                    if (currentBlockName != null)
                        source.addBufferBlockName(currentBlockName);
                    currentBlockName = null;
                    insideBlock = false;
                }

                continue;
            }

            if (line.contains("uniform")) {

                String fullDeclaration = line;

                while (!fullDeclaration.endsWith(";") && i + 1 < lines.size()) {
                    i++;
                    String next = lines.get(i).trim();
                    if (!next.isEmpty())
                        fullDeclaration += " " + next;
                }

                parseUniformDeclaration(fullDeclaration, source);
            }
        }
    }

    private boolean isUniformBlockStart(String line) {
        return line.contains("uniform") &&
                line.contains("{") &&
                !line.contains(";") &&
                !line.matches(".*\\buniform\\s+\\w+\\s+\\w+.*");
    }

    private String parseUniformBlockName(String line) {

        try {

            int uniformIdx = line.indexOf("uniform");
            int braceIdx = line.indexOf("{");

            if (uniformIdx == -1 || braceIdx == -1)
                return null;

            String blockName = line.substring(uniformIdx + 7, braceIdx).trim();
            blockName = blockName.replaceAll("\\(.*?\\)", "").trim();

            return blockName.isEmpty() ? null : blockName;
        } catch (Exception e) {
            return throwException("Failed to parse uniform block name: " + line, e);
        }
    }

    private void parseUniformDeclaration(String declaration, ShaderSourceStruct source) {

        declaration = declaration.replace(";", "").trim();

        if (declaration.isEmpty() ||
                declaration.equals("}") ||
                declaration.startsWith("struct"))
            return;

        if (!declaration.contains("uniform"))
            return;

        declaration = declaration.replaceFirst("\\buniform\\b", "").trim();

        int lastDelimiter = FileParserUtility.findLastTypeDelimiter(declaration);

        if (lastDelimiter == -1)
            return;

        String typeStr = declaration.substring(0, lastDelimiter).trim();
        String namesStr = declaration.substring(lastDelimiter).trim();

        UniformType uniformType = UniformType.fromString(typeStr);

        if (uniformType == null)
            return;

        parseVariableNames(namesStr, uniformType, source);
    }

    private void parseVariableNames(
            String namesStr,
            UniformType uniformType,
            ShaderSourceStruct source) {

        String[] names = namesStr.split(",");

        for (int i = 0; i < names.length; i++) {

            String name = names[i].trim();

            if (name.isEmpty())
                continue;

            int arrayCount = 1;
            String variableName = name;

            if (name.contains("[")) {

                int bracketStart = name.indexOf("[");
                int bracketEnd = name.indexOf("]");

                if (bracketStart != -1 && bracketEnd > bracketStart) {

                    String countStr = name.substring(bracketStart + 1, bracketEnd).trim();

                    if (countStr.isEmpty())
                        throwException("Unsized arrays not supported: " + name);

                    arrayCount = FileParserUtility.parseIntOrDefault(countStr, 0);

                    if (arrayCount == 0)
                        throwException("Could not resolve array size for: " + name);

                    variableName = name.substring(0, bracketStart).trim();
                }
            }

            source.addUniformDeclaration(new UniformData(uniformType, variableName, arrayCount));
        }
    }

    // Includes \\

    private void parseIncludes(ShaderSourceStruct source, ObjectArrayList<String> lines) {

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if (FileParserUtility.lineStartsWith(line, "#include")) {

                String includePath = FileParserUtility.extractPayloadAfterToken(line, "#include");

                if (includePath != null && !includePath.isEmpty()) {
                    ShaderSourceStruct include = internalLoader.getSourceStruct(includePath);
                    if (include != null)
                        source.addDirectInclude(include);
                }
            }
        }
    }

    // Build Assembly \\

    ShaderSourceStruct buildAssembly(File jsonFile) {

        Path rootPath = root.toPath();
        String relativePath = rootPath.relativize(jsonFile.toPath()).toString().replace("\\", "/");
        String shaderName = relativePath.endsWith(".json")
                ? relativePath.substring(0, relativePath.length() - 5)
                : relativePath;

        JsonObject obj = JsonUtility.loadJsonObject(jsonFile);
        ShaderSourceStruct vertSource = internalLoader.getSourceStruct(obj.get("vert").getAsString());
        ShaderSourceStruct fragSource = internalLoader.getSourceStruct(obj.get("frag").getAsString());

        if (vertSource == null || fragSource == null)
            throwException("JSON error: " + jsonFile.getName() + " — vert or frag file not found.");

        if (vertSource.getShaderType() != ShaderType.VERT || fragSource.getShaderType() != ShaderType.FRAG)
            throwException("JSON error: " + jsonFile.getName() + " — vert/frag type mismatch.");

        ShaderSourceStruct assembly = new ShaderSourceStruct(ShaderType.PROGRAM, shaderName, null);
        assembly.setVert(vertSource);
        assembly.setFrag(fragSource);

        return collectIncludes(assembly);
    }

    private ShaderSourceStruct collectIncludes(ShaderSourceStruct assembly) {

        ObjectArrayList<ShaderSourceStruct> visited = new ObjectArrayList<>();

        collectPostOrder(assembly, assembly.getVert(), visited);
        collectPostOrder(assembly, assembly.getFrag(), visited);

        return assembly;
    }

    private void collectPostOrder(
            ShaderSourceStruct assembly,
            ShaderSourceStruct source,
            ObjectArrayList<ShaderSourceStruct> visited) {

        if (visited.contains(source))
            return;

        visited.add(source);

        ObjectArrayList<ShaderSourceStruct> directIncludes = source.getDirectIncludes();

        for (int i = 0; i < directIncludes.size(); i++)
            collectPostOrder(assembly, directIncludes.get(i), visited);

        if (source.getShaderType() == ShaderType.INCLUDE &&
                !assembly.getFlattenedIncludes().contains(source))
            assembly.addFlattenedInclude(source);
    }
}