package dev.aetherpulse.visual;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Local TRS with a cycle-checked parent. Matrices preserve nested nonuniform scales. */
public final class Transform {
    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1);
    private Transform parent;
    public Vector3f position() { return position; }
    public Quaternionf rotation() { return rotation; }
    public Vector3f scale() { return scale; }
    public Transform parent() { return parent; }
    public Transform parent(Transform value) {
        for (Transform p = value; p != null; p = p.parent) if (p == this) throw new IllegalArgumentException("Transform cycle");
        parent = value; return this;
    }
    public Matrix4f localMatrix() { return new Matrix4f().translationRotateScale(position, rotation, scale); }
    public Matrix4f worldMatrix() { return parent == null ? localMatrix() : parent.worldMatrix().mul(localMatrix()); }
    public Transform interpolate(Transform other, float alpha) {
        float t = Math.clamp(alpha, 0, 1);
        Transform result = new Transform();
        position.lerp(other.position, t, result.position);
        rotation.slerp(other.rotation, t, result.rotation);
        scale.lerp(other.scale, t, result.scale);
        return result;
    }
}
