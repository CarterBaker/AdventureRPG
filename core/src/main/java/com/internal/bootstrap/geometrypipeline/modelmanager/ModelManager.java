package com.internal.bootstrap.geometrypipeline.modelmanager;

import com.internal.bootstrap.geometrypipeline.mesh.MeshInstance;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshStruct;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

/*
 * Creates and removes ModelInstance objects. Both creation paths extract a
 * MeshStruct from their source and delegate to a single internal method.
 * GPU resource ownership is never tracked here — callers are responsible for
 * releasing MeshInstances they created when the model is removed.
 */
public class ModelManager extends ManagerPackage {

        // Internal
        private MaterialManager materialManager;
        private MeshManager meshManager;

        // Base \\

        @Override
        protected void get() {
                this.materialManager = get(MaterialManager.class);
                this.meshManager = get(MeshManager.class);
        }

        // Creation \\

        public ModelInstance createModel(MeshStruct meshStruct, int materialID) {
                return buildModel(meshStruct, materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshStruct meshStruct, MaterialInstance material) {
                return buildModel(meshStruct, material);
        }

        public ModelInstance createModel(MeshHandle meshHandle, int materialID) {
                return buildModel(meshHandle.getMeshStruct(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshHandle meshHandle, MaterialInstance material) {
                return buildModel(meshHandle.getMeshStruct(), material);
        }

        public ModelInstance createModel(MeshInstance meshInstance, int materialID) {
                return buildModel(meshInstance.getMeshStruct(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(MeshInstance meshInstance, MaterialInstance material) {
                return buildModel(meshInstance.getMeshStruct(), material);
        }

        public ModelInstance createModel(ModelInstance modelInstance, int materialID) {
                return buildModel(modelInstance.getMeshStruct(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(ModelInstance modelInstance, MaterialInstance material) {
                return buildModel(modelInstance.getMeshStruct(), material);
        }

        public ModelInstance createModel(
                        VAOHandle vaoTemplate,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        int materialID) {

                MeshInstance meshInstance = meshManager.createMesh(vaoTemplate, vertices, indices);
                return buildModel(meshInstance.getMeshStruct(), materialManager.cloneMaterial(materialID));
        }

        public ModelInstance createModel(
                        VAOHandle vaoTemplate,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        MaterialInstance material) {

                MeshInstance meshInstance = meshManager.createMesh(vaoTemplate, vertices, indices);
                return buildModel(meshInstance.getMeshStruct(), material);
        }

        private ModelInstance buildModel(MeshStruct meshStruct, MaterialInstance material) {
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshStruct, material);
                return modelInstance;
        }

        // Removal \\

        public void removeMesh(MeshInstance meshInstance) {
                meshManager.removeMesh(meshInstance);
        }

        public void removeMesh(ModelInstance modelInstance) {
                meshManager.removeMesh(modelInstance.getMeshStruct());
        }
}