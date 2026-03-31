package com.internal.platform.graphics;

import com.internal.platform.math.Matrix4;
import com.internal.platform.math.Vector3;

public class PerspectiveCamera {
    public float fieldOfView;
    public float viewportWidth;
    public float viewportHeight;
    public float near = 0.1f;
    public float far = 1000f;
    public final Vector3 position = new Vector3();
    public final Vector3 direction = new Vector3(0,0,-1);
    public final Vector3 up = new Vector3(0,1,0);
    public final Matrix4 projection = new Matrix4();
    public final Matrix4 view = new Matrix4();
    public final Matrix4 combined = new Matrix4();

    public PerspectiveCamera(float fov, float viewportWidth, float viewportHeight) {
        this.fieldOfView = fov;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    public void update() {
        float aspect = viewportHeight == 0f ? 1f : viewportWidth / viewportHeight;
        setPerspective(projection.val, fieldOfView, aspect, near, far);
        setLookAt(view.val, position, direction, up);
        multiply(combined.val, projection.val, view.val);
    }

    private static void setPerspective(float[] m, float fovDeg, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fovDeg) * 0.5));
        for (int i=0;i<16;i++) m[i]=0f;
        m[0]=f/aspect; m[5]=f; m[10]=(far+near)/(near-far); m[11]=-1f; m[14]=(2f*far*near)/(near-far);
    }

    private static void setLookAt(float[] m, Vector3 pos, Vector3 dir, Vector3 up) {
        float fx=dir.x, fy=dir.y, fz=dir.z;
        float fl=(float)Math.sqrt(fx*fx+fy*fy+fz*fz); if(fl!=0){fx/=fl;fy/=fl;fz/=fl;}
        float sx = fy*up.z - fz*up.y, sy = fz*up.x - fx*up.z, sz = fx*up.y - fy*up.x;
        float sl=(float)Math.sqrt(sx*sx+sy*sy+sz*sz); if(sl!=0){sx/=sl;sy/=sl;sz/=sl;}
        float ux = sy*fz - sz*fy, uy = sz*fx - sx*fz, uz = sx*fy - sy*fx;
        m[0]=sx; m[1]=ux; m[2]=-fx; m[3]=0;
        m[4]=sy; m[5]=uy; m[6]=-fy; m[7]=0;
        m[8]=sz; m[9]=uz; m[10]=-fz; m[11]=0;
        m[12]=-(sx*pos.x+sy*pos.y+sz*pos.z);
        m[13]=-(ux*pos.x+uy*pos.y+uz*pos.z);
        m[14]=(fx*pos.x+fy*pos.y+fz*pos.z);
        m[15]=1;
    }

    private static void multiply(float[] out, float[] a, float[] b) {
        float[] r = new float[16];
        for (int c=0;c<4;c++) for (int rI=0;rI<4;rI++) r[c*4+rI]=a[0*4+rI]*b[c*4+0]+a[1*4+rI]*b[c*4+1]+a[2*4+rI]*b[c*4+2]+a[3*4+rI]*b[c*4+3];
        System.arraycopy(r,0,out,0,16);
    }
}
