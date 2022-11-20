package t20;

import org.joml.Vector3f;

import static org.joml.Math.clamp;
import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Q;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static util.VKUtil.modulo;

public class KeyboardMovementController {
    interface KeyMappings {
        int moveLeft = GLFW_KEY_A;
        int moveRight = GLFW_KEY_D;
        int moveForward = GLFW_KEY_W;
        int moveBackward = GLFW_KEY_S;
        int moveUp = GLFW_KEY_E;
        int moveDown = GLFW_KEY_Q;
        int lookLeft = GLFW_KEY_LEFT;
        int lookRight = GLFW_KEY_RIGHT;
        int lookUp = GLFW_KEY_UP;
        int lookDown = GLFW_KEY_DOWN;
    }

    float moveSpeed = 3.f;
    float lookSpeed = 1.5f;

    public void moveInPlaneXZ(long window, float dt, LveGameObject gameObject) {
        Vector3f rotate = new Vector3f(0);

        if (glfwGetKey(window, KeyMappings.lookRight) == GLFW_PRESS) rotate.y += 1.f;
        if (glfwGetKey(window, KeyMappings.lookLeft) == GLFW_PRESS) rotate.y -= 1.f;
        if (glfwGetKey(window, KeyMappings.lookUp) == GLFW_PRESS) rotate.x += 1.f;
        if (glfwGetKey(window, KeyMappings.lookDown) == GLFW_PRESS) rotate.x -= 1.f;

        if (rotate.dot(rotate) > Math.ulp(0.0)) {
            gameObject.transform.rotation.add(rotate.normalize(new Vector3f()).mul(lookSpeed * dt));
        }

        // limit pitch values between about +/- 85ish degrees
        gameObject.transform.rotation.x = clamp(gameObject.transform.rotation.x(), -1.5f, 1.5f);
        gameObject.transform.rotation.y = modulo(gameObject.transform.rotation.y(), (float) (2*Math.PI));

        float yaw = gameObject.transform.rotation.y();
        Vector3f forwardDir = new Vector3f(sin(yaw), 0.f, cos(yaw));
        Vector3f rightDir = new Vector3f(forwardDir.z, 0.f, -forwardDir.x);
        Vector3f upDir = new Vector3f(0.f, -1.f, 0.f);

        Vector3f moveDir = new Vector3f(0.f);
        if (glfwGetKey(window, KeyMappings.moveForward) == GLFW_PRESS) moveDir.add(forwardDir);
        if (glfwGetKey(window, KeyMappings.moveBackward) == GLFW_PRESS) moveDir.sub(forwardDir);
        if (glfwGetKey(window, KeyMappings.moveRight) == GLFW_PRESS) moveDir.add(rightDir);
        if (glfwGetKey(window, KeyMappings.moveLeft) == GLFW_PRESS) moveDir.sub(rightDir);
        if (glfwGetKey(window, KeyMappings.moveUp) == GLFW_PRESS) moveDir.add(upDir);
        if (glfwGetKey(window, KeyMappings.moveDown) == GLFW_PRESS) moveDir.sub(upDir);

        if (moveDir.dot(moveDir) > Math.ulp(0.0)) {
            gameObject.transform.translation.add(moveDir.normalize(new Vector3f()).mul(moveSpeed  * dt));
        }
    }
}
