package com.AdventureRPG.core.geometrypipeline.modelmanager;

class MeshHandle {

    // Internal
    final int vao;
    final int vbo;
    final int ibo;
    final int indexCount;

    MeshHandle(
            int vao,
            int vbo,
            int ibo,
            int indexCount) {

        // Internal
        this.vao = vao;
        this.vbo = vbo;
        this.ibo = ibo;
        this.indexCount = indexCount;
    }
}