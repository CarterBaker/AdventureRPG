package com.AdventureRPG.SettingsSystem;

public class GlobalConstant {

    // Chunk
    public static final int CHUNK_SIZE = 16;
    public static final int MEGA_CHUNK_SIZE = 4;

    // Mesh
    public static final int VERT_POS = 3;
    public static final int VERT_NOR = 3;
    public static final int VERT_COL = 1;
    public static final int VERT_UV0 = 2;
    public static final int VERT_STRIDE = VERT_POS + VERT_NOR + VERT_COL + VERT_UV0;

    public static final int MESH_VERT_LIMIT = 32767;
}
