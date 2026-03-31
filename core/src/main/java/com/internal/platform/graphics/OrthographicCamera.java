package com.internal.platform.graphics;

import com.internal.platform.math.Matrix4;

public class OrthographicCamera {
    public final Matrix4 combined = new Matrix4();

    public void setToOrtho(boolean yDown, float width, float height) {
        for (int i=0;i<16;i++) combined.val[i]=0f;
        combined.val[0] = 2f / width;
        combined.val[5] = 2f / height * (yDown ? -1f : 1f);
        combined.val[10] = -1f;
        combined.val[12] = -1f;
        combined.val[13] = yDown ? 1f : -1f;
        combined.val[15] = 1f;
    }

    public void update() {}
}
