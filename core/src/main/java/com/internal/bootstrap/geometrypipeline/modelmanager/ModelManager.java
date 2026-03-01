package com.internal.bootstrap.geometrypipeline.modelmanager;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

/*
 * Creates and removes ModelInstance objects. Mesh geometry is delegated to
 * MeshManager. Material instances are either passed in directly or cloned from
 * a material ID. VAO cloning is delegated to VAOManager.
 */
public class ModelManager extends ManagerPackage {

        // Internal
        private MaterialManager materialManager;
        private MeshManager meshManager;
        private VAOManager vaoManager;
        private VBOManager vboManager;
        private IBOManager iboManager;

        // Internal \\

        @Override
        protected void get() {
                this.materialManager = get(MaterialManager.class);
                this.meshManager = get(MeshManager.class);
                this.vaoManager = get(VAOManager.class);
                this.vboManager = get(VBOManager.class);
                this.iboManager = get(IBOManager.class);
        }

        // Accessible \\

        public VAOHandle cloneVAO(VAOHandle templateVAO) {
                return vaoManager.cloneVAO(templateVAO);
        }

        public ModelInstance createModel(
                        VAOHandle vaoHandle,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        int materialID) {
                MeshHandle meshHandle = meshManager.createMesh(vaoHandle, vertices, indices);
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, materialManager.cloneMaterial(materialID));
                return modelInstance;
        }

        public ModelInstance createModel(
                        VAOHandle vaoHandle,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        MaterialInstance material) {
                MeshHandle meshHandle = meshManager.createMesh(vaoHandle, vertices, indices);
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, material);
                return modelInstance;
        }

        public ModelInstance createModel(
                        VAOHandle vaoHandle,
                        VBOHandle vboHandle,
                        IBOHandle iboHandle,
                        int materialID) {
                MeshHandle meshHandle = meshManager.createMesh(vaoHandle, vboHandle, iboHandle);
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, materialManager.cloneMaterial(materialID));
                return modelInstance;
        }

        public ModelInstance createModel(
                        VAOHandle vaoHandle,
                        VBOHandle vboHandle,
                        IBOHandle iboHandle,
                        MaterialInstance material) {
                MeshHandle meshHandle = meshManager.createMesh(vaoHandle, vboHandle, iboHandle);
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, material);
                return modelInstance;
        }

        public ModelInstance createModel(
                        MeshHandle meshHandle,
                        int materialID) {
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, materialManager.cloneMaterial(materialID));
                return modelInstance;
        }

        public ModelInstance createModel(
                        MeshHandle meshHandle,
                        MaterialInstance material) {
                ModelInstance modelInstance = create(ModelInstance.class);
                modelInstance.constructor(meshHandle, material);
                return modelInstance;
        }

        public void removeMesh(ModelInstance modelInstance) {
                meshManager.removeMesh(modelInstance.getMeshHandle());
        }
}