package com.AdventureRPG.core.geometrypipeline.modelmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private ModelManager modelManager;
    private InternalBuildSystem internalBuildSystem;

    private int meshDataCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.MODEL_JSON_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.meshDataCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.modelManager = gameEngine.get(ModelManager.class);
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // MeshData Management \\

    void loadMeshData() {
        loadAllFiles();
    }

    // Load \\

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throw new FileException.FileNotFoundException("Mesh JSON directory not found: " + root.getAbsolutePath());

        List<File> candidates = collectCandidateFiles();

        if (candidates.isEmpty())
            return; // nothing to do

        processFilesWithDependencyResolution(candidates);
    }

    private List<File> collectCandidateFiles() {

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
                    .collect(Collectors.toList());
        }

        catch (IOException e) { // TODO: Add my own error
            throw new FileException.FileReadException("MeshDataManager failed to list files: ", e);
        }
    }

    private void processFilesWithDependencyResolution(List<File> candidates) {

        List<File> pending = new ArrayList<>(candidates);
        Map<File, String> lastFailureReasons = new HashMap<>();

        int pass = 0;
        boolean madeProgress;

        while (!pending.isEmpty() && pass < EngineSetting.MAX_MODEL_READ_PASSES) {

            pass++;
            madeProgress = processFilePass(pending, lastFailureReasons);

            if (!madeProgress)
                break; // No progress means further passes will also fail
        }

        handleUnresolvedFiles(pending, lastFailureReasons, pass);
    }

    private boolean processFilePass(List<File> pending, Map<File, String> lastFailureReasons) {

        boolean madeProgress = false;
        Iterator<File> it = pending.iterator();

        while (it.hasNext()) {

            File file = it.next();
            String meshName = FileUtility.getFileName(file);

            MeshHandle meshHandle = attemptBuildMeshHandle(file, lastFailureReasons);

            if (meshHandle == null)
                continue; // Failed for now — try again in next pass

            // Success: compile/register and remove from pending
            try {
                compileMeshData(meshName, meshDataCount, meshHandle);
                meshDataCount++;
                it.remove();
                lastFailureReasons.remove(file);
                madeProgress = true;

            }

            catch (RuntimeException ex) {
                it.remove(); // TODO: Add my own error
                throw new FileException.FileReadException(
                        "Failed while compiling mesh from file: " + file.getAbsolutePath(), ex);
            }
        }

        return madeProgress;
    }

    private MeshHandle attemptBuildMeshHandle(File file, Map<File, String> lastFailureReasons) {

        try {
            return internalBuildSystem.buildMeshHandle(file, meshDataCount);
        }

        catch (RuntimeException ex) {
            lastFailureReasons.put(file, ex.getMessage());
            return null;
        }
    }

    private void handleUnresolvedFiles(List<File> pending, Map<File, String> lastFailureReasons, int pass) {

        if (pending.isEmpty())
            return;

        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append(String.format("Failed to resolve mesh dependencies after %d passes. ", pass));
        errorMsg.append(String.format("%d unresolved files:\n", pending.size()));

        for (File f : pending) {

            errorMsg.append("  - ").append(f.getAbsolutePath());
            String reason = lastFailureReasons.get(f);

            if (reason != null)
                errorMsg.append(" (").append(reason).append(")");

            errorMsg.append("\n");
        }

        // TODO: Add my own error
        throw new FileException.FileReadException(errorMsg.toString());
    }

    private void compileMeshData(String meshName, int meshID, MeshHandle meshHandle) {
        modelManager.addMeshHandle(meshName, meshID, meshHandle);
    }
}