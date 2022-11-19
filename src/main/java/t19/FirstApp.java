package t19;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VkCommandBuffer;
import util.VKUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

public class FirstApp {

    public class GlobalUbo {
        public Matrix4f projectionView = new Matrix4f();
        public Vector3f lightDirection = new Vector3f(1.f, -3.f, -1.f).normalize();
    }
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
//        var minOffsetAlignment = Math.min(
//                lveDevice.gpu_props.limits().minUniformBufferOffsetAlignment(),
//                lveDevice.gpu_props.limits().nonCoherentAtomSize()
//        );
//        LveBuffer globalUboBuffer = new LveBuffer(
//                lveDevice,
//                VKUtil.sizeof(new Matrix4f()) + VKUtil.sizeof(new Vector3f()),
//                LveSwapChain.MAX_FRAMES_IN_FLIGHT,
//                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
//                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
//                minOffsetAlignment
//        );
//        globalUboBuffer.map(VK_WHOLE_SIZE, 0);

        Vector<LveBuffer> uboBuffers = new Vector<>();
        for (int i = 0; i < LveSwapChain.MAX_FRAMES_IN_FLIGHT; i++) {
            var uboBuffser = new LveBuffer(
                    lveDevice,
                    VKUtil.sizeof(new Matrix4f()) + VKUtil.sizeof(new Vector3f()),
                    1,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    0);
            uboBuffers.add(uboBuffser);
            uboBuffser.map(VK_WHOLE_SIZE, 0);
        }

        SimpleRenderSystem simpleRenderSystem = new SimpleRenderSystem(lveDevice, lveRenderer.getSwapChainRenderPass());
        LveCamera camera = new LveCamera();

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

                int frameIndex = lveRenderer.getFrameIndex();
                LveFrameInfo frameInfo = new LveFrameInfo(
                        frameIndex,
                        frameTime,
                        commandBuffer,
                        camera
                );
                // update
                GlobalUbo ubo = new GlobalUbo();
                ubo.projectionView = camera.getProjection().mul(camera.getView(), new Matrix4f());

                ByteBuffer uboByteBuffer = memAlloc(VKUtil.sizeof(ubo.projectionView));
                uboFloatBuffer(ubo, uboByteBuffer.asFloatBuffer());

                uboBuffers.get(frameIndex).writeToIndex(memAddress(uboByteBuffer), frameIndex);
//                uboBuffers.get(frameIndex).flushIndex(frameIndex);
                memFree(uboByteBuffer);

                // render
                lveRenderer.beginSwapChainRenderPass(commandBuffer);
                simpleRenderSystem.renderGameObjects(frameInfo, gameObjects);
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

    public static void uboFloatBuffer(GlobalUbo ubo, FloatBuffer buffer) {
        float[] array = {ubo.projectionView.m00(), ubo.projectionView.m01(), ubo.projectionView.m02(), ubo.projectionView.m03(),
                ubo.projectionView.m10(),ubo.projectionView.m11(),ubo.projectionView.m12(),ubo.projectionView.m13(),
                ubo.projectionView.m20(),ubo.projectionView.m21(),ubo.projectionView.m22(),ubo.projectionView.m23(),
                ubo.projectionView.m30(),ubo.projectionView.m31(),ubo.projectionView.m32(),ubo.projectionView.m33()};
        List.of(array).forEach(cols -> List.of(cols).forEach(buffer::put));
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
