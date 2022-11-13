package t16;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class FirstApp {
    private final static int HEIGHT = 1200;
    private final static int WIDTH = 1600;
    private final static float TWO_PI = (float) Math.PI * 2.0f;
    private Vector<LveGameObject> gameObjects;

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
        gameObjects = new Vector<>();
        loadGameObjects();
    }

    public void run() {
        SimpleRenderSystem simpleRenderSystem = new SimpleRenderSystem(lveDevice, lveRenderer.getSwapChainRenderPass());
        LveCamera camera = new LveCamera();
//         camera.setViewDirection(new Vector3f(0.f), new Vector3f(0.5f, 0.f, 1.f), null);
//        camera.setViewTarget(new Vector3f(-1.f, -2.f, -2.f), new Vector3f(0.f, 0.f, 2.5f), null);

        var viewerObject = LveGameObject.createGameObject();
        KeyboardMovementController cameraController = new KeyboardMovementController();

        glfwShowWindow(lveWindow.getWindow());

        // The render loop
        float frameTime = 0.0f;
        long currentTime = System.nanoTime();
        int err;
        while (!glfwWindowShouldClose(lveWindow.getWindow())) {
            // Handle window messages. Resize events happen exactly here.
            // So it is safe to use the new swapchain images and framebuffers afterwards.
            glfwPollEvents();

            long newTime  = System.nanoTime();
            frameTime = (newTime - currentTime) / 1E9f;
            currentTime = newTime;

            cameraController.moveInPlaneXZ(lveWindow.getWindow(), frameTime, viewerObject);
            camera.setViewYXZ(viewerObject.transform.translation, viewerObject.transform.rotation);

            float aspect = lveRenderer.getAspectRatio();
//             camera.setOrthographicProjection(-aspect, aspect, -1, 1, -1, 10);
            camera.setPerspectiveProjection((float) Math.toRadians(50.0f), aspect, 0.1f, 10.f);

            if (!lveRenderer.isFrameInProgress()){
                VkCommandBuffer commandBuffer = lveRenderer.beginFrame();

                lveRenderer.beginSwapChainRenderPass(commandBuffer);
                simpleRenderSystem.renderGameObjects(commandBuffer, gameObjects, camera);
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

    /**
     * Temporary helper function, creates a 1x1x1 cube centered at offset with an index buffer
     * @param lveDevice device handle object
     * @param offset    offset of vertices
     * @return cube model
     */
    LveModel createCubeModel(LveDevice lveDevice, Vector3f offset) {
        LveModel.Builder modelBuilder = new LveModel.Builder();
        modelBuilder.vertices = new Vector<>(List.of(
                // left face (white)
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(0.9f, 0.9f, 0.9f)),
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector3f(0.9f, 0.9f, 0.9f)),
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector3f(0.9f, 0.9f, 0.9f)),
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector3f(0.9f, 0.9f, 0.9f)),

                // right face (yellow)
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector3f(0.8f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.8f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector3f(0.8f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector3f(0.8f, 0.8f, 0.1f)),

                // top face (orange, remember y-axis points down)
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(0.9f, 0.6f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector3f(0.9f, 0.6f, 0.1f)),
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector3f(0.9f, 0.6f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector3f(0.9f, 0.6f, 0.1f)),

                // bottom face (red)
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector3f(0.8f, 0.1f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.8f, 0.1f, 0.1f)),
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector3f(0.8f, 0.1f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector3f(0.8f, 0.1f, 0.1f)),

                // nose fase (blue)
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, 0.5f), new Vector3f(0.1f, 0.1f, 0.8f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, 0.5f), new Vector3f(0.1f, 0.1f, 0.8f)),
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, 0.5f), new Vector3f(0.1f, 0.1f, 0.8f)),
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, 0.5f), new Vector3f(0.1f, 0.1f, 0.8f)),

                // tail face (green)
                new LveModel.Vertex(new Vector3f(-0.5f, -0.5f, -0.5f), new Vector3f(0.1f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, 0.5f, -0.5f), new Vector3f(0.1f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(-0.5f, 0.5f, -0.5f), new Vector3f(0.1f, 0.8f, 0.1f)),
                new LveModel.Vertex(new Vector3f(0.5f, -0.5f, -0.5f), new Vector3f(0.1f, 0.8f, 0.1f))
        ));
        for (LveModel.Vertex v : modelBuilder.vertices) {
            v.position.add(offset);
        }

        modelBuilder.indices = new int[]{0, 1, 2, 0, 3, 1, 4, 5, 6, 4, 7, 5, 8, 9, 10, 8, 11, 9,
                12, 13, 14, 12, 15, 13, 16, 17, 18, 16, 19, 17, 20, 21, 22, 20, 23, 21};

        return new LveModel(lveDevice, modelBuilder);
    }

    private void loadGameObjects() {
        LveModel lveModel = createCubeModel(lveDevice, new Vector3f(.0f, .0f, .0f));
        LveGameObject cube = LveGameObject.createGameObject();
        cube.model = lveModel;
        cube.transform.translation = new Vector3f(.0f, .0f, 2.5f);
        cube.transform.scale = new Vector3f(.5f, .5f, .5f);
        gameObjects.add(cube);
    }

    public static void main(String[] args) throws IOException {
        new FirstApp().run();
    }
}
