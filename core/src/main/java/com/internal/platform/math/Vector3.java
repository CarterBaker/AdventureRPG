package com.internal.platform.math;

public class Vector3 {
    public float x,y,z;
    public Vector3() { this(0,0,0);} 
    public Vector3(float x,float y,float z){this.x=x;this.y=y;this.z=z;}
    public Vector3 set(float x,float y,float z){this.x=x;this.y=y;this.z=z;return this;}
}
