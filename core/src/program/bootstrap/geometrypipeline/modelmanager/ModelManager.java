package program.bootstrap.geometrypipeline.modelmanager;

import program.bootstrap.geometrypipeline.mesh.MeshData;
import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.bootstrap.geometrypipeline.mesh.MeshInstance;
import program.bootstrap.geometrypipeline.meshmanager.MeshManager;
import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.geometrypipeline.vao.VAOHandle;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

public class ModelManager extends ManagerPackage {

        /*
         * Creates and removes ModelInstances. All creation paths extract MeshData
         * from their source and funnel through a single internal build method.
         * GPU resource ownership is never tracked here — callers are responsible
         * for releasing any MeshInstances they created when the model is removed.
         */

        // Internal
        private MaterialManager materialManager;
        private MeshManager meshManager;

        // Base \\

        @Override
        protected void get() {

                // Internal
                this.materialManager = get(MaterialManager.class);
                this.meshManager = get(MeshManager.class);
        }

        // Creation \\

        public ModelInstance createModel(MeshData meshData, int materialID) {
                return buildModel(meshData, materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshData meshData, MaterialInstance material) {
                return buildModel(meshData, material);
        }

        public ModelInstance createModel(MeshHandle meshHandle, int materialID) {
                return buildModel(meshHandle.getMeshData(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshHandle meshHandle, MaterialInstance material) {
                return buildModel(meshHandle.getMeshData(), material);
        }

        public ModelInstance createModel(MeshInstance meshInstance, int materialID) {
                return buildModel(meshInstance.getMeshData(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshInstance meshInstance, MaterialInstance material) {
                return buildModel(meshInstance.getMeshData(), material);
        }

        public ModelInstance createModel(ModelInstance modelInstance, int materialID) {
                return buildModel(modelInstance.getMeshData(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(ModelInstance modelInstance, MaterialInstance material) {
                return buildModel(modelInstance.getMeshData(), material);
        }

        public ModelInstance createModel(
                        VAOHandle vaoTemplate,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        int materialID) {

                MeshInstance meshInstance = meshManager.createMesh(vaoTemplate, vertices, indices);

                return buildModel(meshInstance.getMeshData(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(
                        VAOHandle vaoTemplate,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        MaterialInstance material) {

                MeshInstance meshInstance = meshManager.createMesh(vaoTemplate, vertices, indices);

                return buildModel(meshInstance.getMeshData(), material);
        }

        private ModelInstance buildModel(MeshData meshData, MaterialInstance material) {

                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshData, material);

                return modelInstance;
        }

        // Removal \\

        public void removeMesh(MeshInstance meshInstance) {
                meshManager.removeMesh(meshInstance);
        }

        public void removeMesh(ModelInstance modelInstance) {
                meshManager.removeMesh(modelInstance.getMeshData());
        }
}