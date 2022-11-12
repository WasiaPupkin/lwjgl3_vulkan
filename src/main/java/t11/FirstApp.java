package t11;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import static org.joml.Math.cos;
import static org.joml.Math.sin;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class FirstApp {
    private final static int HEIGHT = 1200;
    private final static int WIDTH = 1600;
    private final static float TWO_PI = (float) Math.PI * 2.0f;
    private Collection<LveGameObject> gameObjects;

    private final LveDevice lveDevice;
    private final LveWindow lveWindow;
    private final LveRenderer lveRenderer;

    private final static boolean[] keydown = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private static final AtomicInteger hz = new AtomicInteger();

    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public FirstApp() {
        lveWindow = new LveWindow(WIDTH, HEIGHT, "Hello");
        lveDevice = new LveDevice(lveWindow);
        lveRenderer = new LveRenderer(lveDevice, lveWindow);
        gameObjects = new ArrayList<>();
        loadGameObjects();
    }

    private void loadGameObjects() {
        Vector<LveModel.Vertex> vertices = new Vector<>(List.of(
                new LveModel.Vertex(new Vector2f(0.0f, -0.5f), new Vector3f(1.0f, 0.0f, 0.0f)),
                new LveModel.Vertex(new Vector2f(0.5f, 0.5f), new Vector3f(0.0f, 1.0f, 0.0f)),
                new LveModel.Vertex(new Vector2f(-0.5f, 0.5f), new Vector3f(0.0f, 0.0f, 1.0f))
        ));

        LveModel lveModel = new LveModel(lveDevice, vertices);
        LveGameObject triangle = LveGameObject.createGameObject();
        triangle.model = lveModel;
        triangle.color = new Vector3f(0.1f, 0.8f, 0.1f);
        triangle.transform2d.translation = new Vector2f(0.2f);
        triangle.transform2d.scale = new Vector2f(2.0f, 0.5f);
        triangle.transform2d.rotation = 0.25f * TWO_PI;
        gameObjects.add(triangle);
    }

    public void run() {
        // create some models
        LveModel squareModel = createSquareModel(lveDevice,
                new Vector2f(.5f, .0f));  // offset model by .5 so rotation occurs at edge rather than center of square
        LveModel circleModel = createCircleModel(lveDevice, 64);

        // create physics objects
        Vector<LveGameObject> physicsObjects = new Vector<>();
        LveGameObject red = LveGameObject.createGameObject();
        red.transform2d.scale = new Vector2f(.05f);
        red.transform2d.translation = new Vector2f(.5f, .5f);
        red.color = new Vector3f(1.f, 0.f, 0.f);
        red.rigidBody2d.velocity = new Vector2f(-.5f, .0f);
        red.model = circleModel;
        physicsObjects.add(red);
        LveGameObject blue = LveGameObject.createGameObject();
        blue.transform2d.scale = new Vector2f(.05f);
        blue.transform2d.translation = new Vector2f(-.45f, -.25f);
        blue.color = new Vector3f(0.f, 0.f, 1.f);
        blue.rigidBody2d.velocity = new Vector2f(.5f, .0f);
        blue.model = circleModel;
        physicsObjects.add(blue);

        // create vector field
        Vector<LveGameObject> vectorField = new Vector<>();
        int gridCount = 40;
        for (int i = 0; i < gridCount; i++) {
            for (int j = 0; j < gridCount; j++) {
                LveGameObject vf = LveGameObject.createGameObject();
                vf.transform2d.scale = new Vector2f(0.005f);
                vf.transform2d.translation = new Vector2f(
                        -1.0f + (i + 0.5f) * 2.0f / gridCount,
                        -1.0f + (j + 0.5f) * 2.0f / gridCount);
                vf.color = new Vector3f(1.0f);
                vf.model = squareModel;
                vectorField.add(vf);
            }
        }
        GravityPhysicsSystem gravitySystem = new GravityPhysicsSystem(0.81f);
        Vec2FieldSystem vecFieldSystem = new Vec2FieldSystem();

        SimpleRenderSystem simpleRenderSystem = new SimpleRenderSystem(lveDevice, lveRenderer.getSwapChainRenderPass());

        glfwShowWindow(lveWindow.getWindow());

        // The render loop
//        long lastTime = System.nanoTime();
//        float time = 0.0f;
        int err;
        while (!glfwWindowShouldClose(lveWindow.getWindow())) {
            // Handle window messages. Resize events happen exactly here.
            // So it is safe to use the new swapchain images and framebuffers afterwards.
            glfwPollEvents();

            if (!lveRenderer.isFrameInProgress()){
                VkCommandBuffer commandBuffer = lveRenderer.beginFrame();

                // update systems
                gravitySystem.update(physicsObjects, 1.f / 600, 5);
                vecFieldSystem.update(gravitySystem, physicsObjects, vectorField);

                lveRenderer.beginSwapChainRenderPass(commandBuffer);
                simpleRenderSystem.renderGameObjects(commandBuffer, physicsObjects);
                simpleRenderSystem.renderGameObjects(commandBuffer, vectorField);
                lveRenderer.endSwapChainRenderPass(commandBuffer);
                lveRenderer.endFrame();
            }

//      todo  interesting - memory didn't increase anymore...
//            System.gc();
        }
//        todo?
        simpleRenderSystem.onDestroy();
        lveDevice.onDestroy();

//        framebufferSizeCallback.free();
//        keyCallback.free();
        // We don't bother disposing of all Vulkan resources.
        // Let the OS process manager take care of it.
    }


    LveModel createSquareModel(LveDevice lveDevice, Vector2f offset) {
        float[][] verticess = {
                {-0.5f, -0.5f},
                {0.5f, 0.5f},
                {-0.5f, 0.5f},
                {-0.5f, -0.5f},
                {0.5f, -0.5f},
                {0.5f, 0.5f}
        };

        Vector<LveModel.Vertex> vertices = new Vector<>(List.of(
                new LveModel.Vertex(new Vector2f(-0.5f, -0.5f), null),
                new LveModel.Vertex(new Vector2f(0.5f, 0.5f), null),
                new LveModel.Vertex(new Vector2f(-0.5f, 0.5f), null),
                new LveModel.Vertex(new Vector2f(-0.5f, -0.5f), null),
                new LveModel.Vertex(new Vector2f(0.5f, -0.5f), null),
                new LveModel.Vertex(new Vector2f(0.5f, 0.5f), null)));

        for (LveModel.Vertex v : vertices) {
            v.position.add(offset);
        }
        return new LveModel(lveDevice, vertices);
    }

    LveModel createCircleModel(LveDevice device, int numSides) {
        Vector<LveModel.Vertex> uniqueVertices = new Vector<>();
        for (int i = 0; i < numSides; i++) {
            float angle = (float) (i * 2 * Math.PI / numSides);
            uniqueVertices.add(new LveModel.Vertex(new Vector2f(cos(angle), sin(angle)), null));
        }
        uniqueVertices.add(new LveModel.Vertex(new Vector2f(), null));  // adds center vertex at 0, 0

        Vector<LveModel.Vertex> vertices = new Vector<>();
        for (int i = 0; i < numSides; i++) {
            vertices.add(uniqueVertices.get(i));
            vertices.add(uniqueVertices.get((i + 1) % numSides));
            vertices.add(uniqueVertices.get(numSides));
        }
        return new LveModel(device, vertices);
    }

    public static void main(String[] args) throws IOException {
        new FirstApp().run();
    }
}
