package program.bootstrap.shaderpipeline.uniforms;

import program.core.util.mathematics.matrices.*;
import program.core.util.mathematics.vectors.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.ByteBuffer;

public enum UniformType {

    /*
     * All supported GLSL uniform types with their std140 alignment and size
     * rules. Each constant implements writeElement() to serialize its value
     * into a ByteBuffer for UBO upload.
     */

    // Scalars \\

    FLOAT("float", 4, 4) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putFloat((Float) value);
        }
    },
    DOUBLE("double", 8, 8) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putDouble((Double) value);
        }
    },
    INT("int", 4, 4) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putInt((Integer) value);
        }
    },
    BOOL("bool", 4, 4) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putInt((Boolean) value ? 1 : 0);
        }
    },

    // Vectors \\

    VECTOR2("vec2", 8, 8) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector2 v = (Vector2) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
        }
    },
    VECTOR3("vec3", 16, 12) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector3 v = (Vector3) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
            buffer.putFloat(v.z);
        }
    },
    VECTOR4("vec4", 16, 16) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector4 v = (Vector4) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
            buffer.putFloat(v.z);
            buffer.putFloat(v.w);
        }
    },
    VECTOR2_DOUBLE("dvec2", 16, 16) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector2Double v = (Vector2Double) value;
            buffer.putDouble(v.x);
            buffer.putDouble(v.y);
        }
    },
    VECTOR3_DOUBLE("dvec3", 32, 24) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector3Double v = (Vector3Double) value;
            buffer.putDouble(v.x);
            buffer.putDouble(v.y);
            buffer.putDouble(v.z);
        }
    },
    VECTOR4_DOUBLE("dvec4", 32, 32) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector4Double v = (Vector4Double) value;
            buffer.putDouble(v.x);
            buffer.putDouble(v.y);
            buffer.putDouble(v.z);
            buffer.putDouble(v.w);
        }
    },
    VECTOR2_INT("ivec2", 8, 8) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector2Int v = (Vector2Int) value;
            buffer.putInt(v.x);
            buffer.putInt(v.y);
        }
    },
    VECTOR3_INT("ivec3", 16, 12) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector3Int v = (Vector3Int) value;
            buffer.putInt(v.x);
            buffer.putInt(v.y);
            buffer.putInt(v.z);
        }
    },
    VECTOR4_INT("ivec4", 16, 16) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector4Int v = (Vector4Int) value;
            buffer.putInt(v.x);
            buffer.putInt(v.y);
            buffer.putInt(v.z);
            buffer.putInt(v.w);
        }
    },
    VECTOR2_BOOLEAN("bvec2", 8, 8) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector2Boolean v = (Vector2Boolean) value;
            buffer.putInt(v.x ? 1 : 0);
            buffer.putInt(v.y ? 1 : 0);
        }
    },
    VECTOR3_BOOLEAN("bvec3", 16, 12) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector3Boolean v = (Vector3Boolean) value;
            buffer.putInt(v.x ? 1 : 0);
            buffer.putInt(v.y ? 1 : 0);
            buffer.putInt(v.z ? 1 : 0);
        }
    },
    VECTOR4_BOOLEAN("bvec4", 16, 16) {
        public void writeElement(ByteBuffer buffer, Object value) {
            Vector4Boolean v = (Vector4Boolean) value;
            buffer.putInt(v.x ? 1 : 0);
            buffer.putInt(v.y ? 1 : 0);
            buffer.putInt(v.z ? 1 : 0);
            buffer.putInt(v.w ? 1 : 0);
        }
    },

    // Matrices \\

    MATRIX2("mat2", 16, 32) {
        public void writeElement(ByteBuffer buffer, Object value) {
            float[] m = ((Matrix2) value).val;
            buffer.putFloat(m[0]);
            buffer.putFloat(m[1]); // col 0
            buffer.putFloat(0f);
            buffer.putFloat(0f);
            buffer.putFloat(m[2]);
            buffer.putFloat(m[3]); // col 1
            buffer.putFloat(0f);
            buffer.putFloat(0f);
        }
    },
    MATRIX3("mat3", 16, 48) {
        public void writeElement(ByteBuffer buffer, Object value) {
            float[] m = ((Matrix3) value).val;
            buffer.putFloat(m[0]);
            buffer.putFloat(m[1]);
            buffer.putFloat(m[2]);
            buffer.putFloat(0f); // col 0
            buffer.putFloat(m[3]);
            buffer.putFloat(m[4]);
            buffer.putFloat(m[5]);
            buffer.putFloat(0f); // col 1
            buffer.putFloat(m[6]);
            buffer.putFloat(m[7]);
            buffer.putFloat(m[8]);
            buffer.putFloat(0f); // col 2
        }
    },
    MATRIX4("mat4", 16, 64) {
        public void writeElement(ByteBuffer buffer, Object value) {
            for (float f : ((Matrix4) value).val)
                buffer.putFloat(f);
        }
    },
    MATRIX2_DOUBLE("dmat2", 32, 64) {
        public void writeElement(ByteBuffer buffer, Object value) {
            double[] m = ((Matrix2Double) value).val;
            buffer.putDouble(m[0]);
            buffer.putDouble(m[1]); // col 0
            buffer.putDouble(0d);
            buffer.putDouble(0d);
            buffer.putDouble(m[2]);
            buffer.putDouble(m[3]); // col 1
            buffer.putDouble(0d);
            buffer.putDouble(0d);
        }
    },
    MATRIX3_DOUBLE("dmat3", 32, 96) {
        public void writeElement(ByteBuffer buffer, Object value) {
            double[] m = ((Matrix3Double) value).val;
            buffer.putDouble(m[0]);
            buffer.putDouble(m[1]);
            buffer.putDouble(m[2]);
            buffer.putDouble(0d); // col 0
            buffer.putDouble(m[3]);
            buffer.putDouble(m[4]);
            buffer.putDouble(m[5]);
            buffer.putDouble(0d); // col 1
            buffer.putDouble(m[6]);
            buffer.putDouble(m[7]);
            buffer.putDouble(m[8]);
            buffer.putDouble(0d); // col 2
        }
    },
    MATRIX4_DOUBLE("dmat4", 32, 128) {
        public void writeElement(ByteBuffer buffer, Object value) {
            for (double d : ((Matrix4Double) value).val)
                buffer.putDouble(d);
        }
    },

    // Samplers \\

    SAMPLE_IMAGE_2D("sampler2D", 4, 4) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putInt((Integer) value);
        }
    },
    SAMPLE_IMAGE_2D_ARRAY("sampler2DArray", 4, 4) {
        public void writeElement(ByteBuffer buffer, Object value) {
            buffer.putInt((Integer) value);
        }
    };

    // Internal
    private final String glslName;
    private final int std140Alignment;
    private final int std140Size;

    UniformType(String glslName, int std140Alignment, int std140Size) {
        this.glslName = glslName;
        this.std140Alignment = std140Alignment;
        this.std140Size = std140Size;
    }

    public abstract void writeElement(ByteBuffer buffer, Object value);

    // Lookup \\

    private static final Object2ObjectOpenHashMap<String, UniformType> LOOKUP = new Object2ObjectOpenHashMap<>();
    static {
        for (UniformType type : values())
            LOOKUP.put(type.glslName, type);
    }

    public String getGLSLName() {
        return glslName;
    }

    public int getStd140Alignment() {
        return std140Alignment;
    }

    public int getStd140Size() {
        return std140Size;
    }

    public static UniformType fromString(String glslName) {
        return LOOKUP.get(glslName);
    }
}