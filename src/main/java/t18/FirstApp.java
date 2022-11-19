package t18;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.IOException;
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

    public FirstApp() throws IOException {
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
            camera.setPerspectiveProjection((float) Math.toRadians(50.0f), aspect, 0.1f, 100.f);

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

    private void loadGameObjects() throws IOException {
        LveModel lveModel = LveModel.createModelFromFile(lveDevice, "vulkan/models/flat_vase.obj");
        LveGameObject flatVase  = LveGameObject.createGameObject();
        flatVase.model = lveModel;
        flatVase.transform.translation = new Vector3f(-.5f, .5f, 2.5f);
        flatVase.transform.scale = new Vector3f(3.f, 1.5f, 3.f);
        gameObjects.add(flatVase);

        lveModel = LveModel.createModelFromFile(lveDevice, "vulkan/models/smooth_vase.obj");
        LveGameObject smoothVase   = LveGameObject.createGameObject();
        smoothVase.model = lveModel;
        smoothVase.transform.translation = new Vector3f(.5f, .5f, 2.5f);
        smoothVase.transform.scale = new Vector3f(3.f, 1.5f, 3.f);
        gameObjects.add(smoothVase);
    }

    public static void main(String[] args) throws IOException {
        new FirstApp().run();
    }
}
