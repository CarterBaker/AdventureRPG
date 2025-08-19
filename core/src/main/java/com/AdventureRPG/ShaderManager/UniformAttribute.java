package com.AdventureRPG.ShaderManager;

import com.badlogic.gdx.graphics.g3d.Attribute;

public class UniformAttribute extends Attribute {

    public static final String Alias = "uniform";
    public static final long Type = register(Alias);

    public enum UniformType {
        FLOAT, INT, BOOL, VEC2, VEC3, VEC4, COLOR, MATRIX4
    }

    public final String name;
    public final UniformType uniformType;
    public Object value;

    public UniformAttribute(String name, UniformType uniformType, Object value) {
        super(Type);
        this.name = name;
        this.uniformType = uniformType;
        this.value = value;
    }

    @Override
    public Attribute copy() {
        return new UniformAttribute(name, uniformType, value);
    }

    @Override
    public int compareTo(Attribute o) {
        return 0; // uniforms don’t need sorting
    }
}
