package t14;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class LveGameObject {
    private final int id;
    LveModel model;
    Vector3f color = new Vector3f();
    TransformComponent transform = new TransformComponent();
    static int currentId = 0;

    private LveGameObject(int id){
        this.id = id;
    }

    public static LveGameObject createGameObject(){
        return new LveGameObject(currentId++);
    }

    public int getId() {
        return id;
    }

    public class TransformComponent {
        Vector3f translation = new Vector3f();
        Vector3f scale = new Vector3f(1.f, 1.f, 1.f);
        Vector3f rotation = new Vector3f();

        // Matrix corresponds to translate * Ry * Rx * Rz * scale transformation
        // Rotation convention uses tait-bryan angles with axis order Y(1), X(2), Z(3)
//        Matrix4f mat4() {
//            var transform = new Matrix4f().translate(translation);
//            transform = transform.rotate(rotation.y, new Vector3f(0.0f, 1.0f, 0.0f), new Matrix4f());
//            transform = transform.rotate(rotation.x, new Vector3f(1.0f, 0.0f, 0.0f), new Matrix4f());
//            transform = transform.rotate(rotation.z, new Vector3f(0.0f, 0.0f, 1.0f), new Matrix4f());
//            transform = transform.scale(scale, new Matrix4f());
//            return transform;
//        }
        // v2 - faster version of rotation

        /**
         * Matrix corresponds to Translate * Ry * Rx * Rz * Scale
         * Rotation corresponds to Tait-Bryan angles of Y(1), X(2), Z(3)
         * <a href="https://en.wikipedia.org/wiki/Euler_angles#Rotation_matrix">Rotation matrix</a>
         * @return mat4
         */
        Matrix4f mat4() {
            float c3 = (float) Math.cos(rotation.z());
            float s3 = (float) Math.sin(rotation.z());
            float c2 = (float) Math.cos(rotation.x());
            float s2 = (float) Math.sin(rotation.x());
            float c1 = (float) Math.cos(rotation.y());
            float s1 = (float) Math.sin(rotation.y());

            var transform = new Matrix4f(
                new Vector4f(scale.x() * (c1 * c3 + s1 * s2 * s3),
                             scale.x() * (c2 * s3),
                             scale.x() * (c1 * s2 * s3 - c3 * s1),
                             0.0f),
                new Vector4f(scale.y() * (c3 * s1 * s2 - c1 * s3),
                             scale.y() * (c2 * c3),
                             scale.y() * (c1 * c3 * s2 + s1 * s3),
                             0.0f),
                new Vector4f(scale.z() * (c2 * s1),
                             scale.z() * (-s2),
                             scale.z() * (c1 * c2),
                             0.0f),
                new Vector4f(translation.x(), translation.y(), translation.z(), 1.0f)
            );
            return transform;
        }
    }
}
