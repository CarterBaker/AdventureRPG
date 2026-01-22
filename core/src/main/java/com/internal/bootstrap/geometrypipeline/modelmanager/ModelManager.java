package com.internal.bootstrap.geometrypipeline.modelmanager;

import com.internal.bootstrap.geometrypipeline.ibomanager.IBOHandle;
import com.internal.bootstrap.geometrypipeline.ibomanager.IBOManager;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshHandle;
import com.internal.bootstrap.geometrypipeline.meshmanager.MeshManager;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOManager;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOHandle;
import com.internal.bootstrap.geometrypipeline.vbomanager.VBOManager;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

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

                // Internal
                this.materialManager = get(MaterialManager.class);
                this.meshManager = get(MeshManager.class);
                this.vaoManager = get(VAOManager.class);
                this.vboManager = get(VBOManager.class);
                this.iboManager = get(IBOManager.class);
        }

        // Accessible \\

        public ModelHandle createModel(
                        VAOHandle vaoHandle,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        int materialID) {

                MaterialHandle materialHandle = materialManager.getMaterialFromMaterialID(materialID);

                MeshHandle meshHandle = meshManager.createMesh(
                                vaoHandle,
                                vertices,
                                indices);

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public ModelHandle createModel(
                        VAOHandle vaoHandle,
                        FloatArrayList vertices,
                        ShortArrayList indices,
                        MaterialHandle materialHandle) {

                MeshHandle meshHandle = meshManager.createMesh(
                                vaoHandle,
                                vertices,
                                indices);

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public ModelHandle createModel(
                        VAOHandle vaoHandle,
                        VBOHandle vboHandle,
                        IBOHandle iboHandle,
                        int materialID) {

                MaterialHandle materialHandle = materialManager.getMaterialFromMaterialID(materialID);

                MeshHandle meshHandle = meshManager.createMesh(
                                vaoHandle,
                                vboHandle,
                                iboHandle);

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public ModelHandle createModel(
                        VAOHandle vaoHandle,
                        VBOHandle vboHandle,
                        IBOHandle iboHandle,
                        MaterialHandle materialHandle) {

                MeshHandle meshHandle = meshManager.createMesh(
                                vaoHandle,
                                vboHandle,
                                iboHandle);

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public ModelHandle createModel(
                        MeshHandle meshHandle,
                        int materialID) {

                MaterialHandle materialHandle = materialManager.getMaterialFromMaterialID(materialID);

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public ModelHandle createModel(
                        MeshHandle meshHandle,
                        MaterialHandle materialHandle) {

                ModelHandle modelHandle = create(ModelHandle.class);
                modelHandle.constructor(
                                meshHandle,
                                materialHandle);

                return modelHandle;
        }

        public void removeMesh(ModelHandle modelHandle) {
                meshManager.removeMesh(modelHandle.getMeshHandle());
        }
}