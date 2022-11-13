package t13;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import static org.joml.Math.tan;

public class LveCamera {
    private Matrix4f projectionMatrix = new Matrix4f();

    public Matrix4f getProjection() {
        return projectionMatrix;
    }

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
}
