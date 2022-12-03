package t27;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.joml.Math.tan;

public class LveCamera {
    private Matrix4f projectionMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f inverseViewMatrix = new Matrix4f();

    public Matrix4f getProjection() {
        return projectionMatrix;
    }
    public Matrix4f getView() { return viewMatrix; }
    public Matrix4f getInverseView() { return inverseViewMatrix; }
    public Vector3f getPosition() { return inverseViewMatrix.getColumn(3, new Vector3f()) ;}

    void setOrthographicProjection(float left, float right, float top, float bottom, float near, float far) {
        projectionMatrix = new Matrix4f();
        projectionMatrix.m00(2.f / (right - left));
        projectionMatrix.m11(2.f / (bottom - top));
        projectionMatrix.m22(1.f / (far - near));
        projectionMatrix.m30(-(right + left) / (right - left));
        projectionMatrix.m31(-(bottom + top) / (bottom - top));
        projectionMatrix.m32(-near / (far - near));
    }

    void setPerspectiveProjection(float fovy, float aspect, float near, float far) {
        assert Math.abs(aspect - Math.ulp(0.0)) > 0.0f;
        float tanHalfFovy = tan(fovy / 2.f);
        projectionMatrix = new Matrix4f(new Vector4f(0.0f), new Vector4f(0.0f), new Vector4f(0.0f), new Vector4f(0.0f));
        projectionMatrix.m00(1.f / (aspect * tanHalfFovy));
        projectionMatrix.m11(1.f / (tanHalfFovy));
        projectionMatrix.m22(far / (far - near));
        projectionMatrix.m23(1.f);
        projectionMatrix.m32(-(far * near) / (far - near));
    }

    void setViewDirection(Vector3f position, Vector3f direction, Vector3f up) {
        Vector3f localUp = Objects.isNull(up) ? new Vector3f(0.f, -1.f, 0.f) : up;
        Vector3f w = direction.normalize(new Vector3f());
        Vector3f u = localUp.cross(w, new Vector3f()).normalize(new Vector3f());
        Vector3f v = u.cross(w, new Vector3f());

        viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x);
        viewMatrix.m10(u.y);
        viewMatrix.m20(u.z);
        viewMatrix.m01(v.x);
        viewMatrix.m11(v.y);
        viewMatrix.m21(v.z);
        viewMatrix.m02(w.x);
        viewMatrix.m12(w.y);
        viewMatrix.m22(w.z);
        viewMatrix.m30(-position.dot(u));
        viewMatrix.m31(-position.dot(v));
        viewMatrix.m32(-position.dot(w));

        inverseViewMatrix = new Matrix4f();
        inverseViewMatrix.m00(u.x);
        inverseViewMatrix.m01(u.y);
        inverseViewMatrix.m02(u.z);
        inverseViewMatrix.m10(v.x);
        inverseViewMatrix.m11(v.y);
        inverseViewMatrix.m12(v.z);
        inverseViewMatrix.m20(w.x);
        inverseViewMatrix.m21(w.y);
        inverseViewMatrix.m22(w.z);
        inverseViewMatrix.m30(position.x);
        inverseViewMatrix.m31(position.y);
        inverseViewMatrix.m32(position.z);
    }

    void setViewTarget(Vector3f position, Vector3f target, Vector3f up) {
        Vector3f localUp = Objects.isNull(up) ? new Vector3f(0.f, -1.f, 0.f) : up;
        setViewDirection(position, target.sub(position, new Vector3f()), localUp);
    }

    void setViewYXZ(Vector3f position, Vector3f rotation) {
        float c3 = cos(rotation.z);
        float s3 = sin(rotation.z);
        float c2 = cos(rotation.x);
        float s2 = sin(rotation.x);
        float c1 = cos(rotation.y);
        float s1 = sin(rotation.y);

        Vector3f u = new Vector3f((c1 * c3 + s1 * s2 * s3), (c2 * s3), (c1 * s2 * s3 - c3 * s1));
        Vector3f v = new Vector3f((c3 * s1 * s2 - c1 * s3), (c2 * c3), (c1 * c3 * s2 + s1 * s3));
        Vector3f w = new Vector3f((c2 * s1), (-s2), (c1 * c2));

        viewMatrix = new Matrix4f();
        viewMatrix.m00(u.x);
        viewMatrix.m10(u.y);
        viewMatrix.m20(u.z);
        viewMatrix.m01(v.x);
        viewMatrix.m11(v.y);
        viewMatrix.m21(v.z);
        viewMatrix.m02(w.x);
        viewMatrix.m12(w.y);
        viewMatrix.m22(w.z);
        viewMatrix.m30(-position.dot(u));
        viewMatrix.m31(-position.dot(v));
        viewMatrix.m32(-position.dot(w));

        inverseViewMatrix = new Matrix4f();
        inverseViewMatrix.m00(u.x);
        inverseViewMatrix.m01(u.y);
        inverseViewMatrix.m02(u.z);
        inverseViewMatrix.m10(v.x);
        inverseViewMatrix.m11(v.y);
        inverseViewMatrix.m12(v.z);
        inverseViewMatrix.m20(w.x);
        inverseViewMatrix.m21(w.y);
        inverseViewMatrix.m22(w.z);
        inverseViewMatrix.m30(position.x);
        inverseViewMatrix.m31(position.y);
        inverseViewMatrix.m32(position.z);
    }
}
