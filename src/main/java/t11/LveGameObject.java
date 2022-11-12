package t11;

import org.joml.Matrix2f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class LveGameObject {
    private final int id;

    LveModel model;
    Vector3f color;
    Transform2dComponent transform2d = new Transform2dComponent();
    RigidBody2dComponent rigidBody2d = new RigidBody2dComponent();
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

    public class Transform2dComponent {
        Vector2f translation = new Vector2f();
        Vector2f scale = new Vector2f(1.f, 1.f);
        float rotation;
        Matrix2f mat2() {
            float s = (float) Math.sin(rotation);
            float c = (float) Math.cos(rotation);

//            Vector2f vec1 = new Vector2f(s, c);
//            Vector2f vec2 = new Vector2f(-c, s);
//
            Vector2f vec1 = new Vector2f(c, -s);
            Vector2f vec2 = new Vector2f(s, c);

//            double[][] rotMat = {{c, s}, {-s, c}};
//            double[][] scaleMat = {{scale.x(), .0f}, {.0f, scale.y()}};
//
//            RealMatrix firstMatrix = new Array2DRowRealMatrix(rotMat);
//            RealMatrix secondMatrix = new Array2DRowRealMatrix(scaleMat);
//            RealMatrix actual = firstMatrix.multiply(secondMatrix);
//            Matrix2d actualMat = new Matrix2d(new Vector2d(actual.getRow(0)), new Vector2d(actual.getRow(1)));

//            Matrix2f etalon = new Matrix2f(0.0f, 2.0f, -0.5f,0.0f);

            Matrix2f rotationMatrix = new Matrix2f(vec1, vec2);
            Matrix2f scaleMatrix = new Matrix2f(scale.x(), .0f,
                                                .0f, scale.y());

//            return scaleMatrix;
            return rotationMatrix.mul(scaleMatrix);
        }
    }

    public class RigidBody2dComponent {
        Vector2f velocity;
        float mass = 1.0f;
    }
}
