package t26;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VkCommandBuffer;
import t26.systems.PointLightSystem;
import t26.systems.SimpleRenderSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

public class FirstApp {
    private final static int HEIGHT = 1200;
    private final static int WIDTH = 1600;
    private Map<Integer, LveGameObject> gameObjects;
    private final LveDevice lveDevice;
    private final LveWindow lveWindow;
    private final LveRenderer lveRenderer;
    private final LveDescriptorPool globalPool;

    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public FirstApp() throws IOException {
        lveWindow = new LveWindow(WIDTH, HEIGHT, "Hello");
        lveDevice = new LveDevice(lveWindow);
        lveRenderer = new LveRenderer(lveDevice, lveWindow);
        gameObjects = new HashMap<>();

        globalPool = new LveDescriptorPool.Builder(lveDevice)
                .setMaxSets(LveSwapChain.MAX_FRAMES_IN_FLIGHT)
                .addPoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, LveSwapChain.MAX_FRAMES_IN_FLIGHT)
                .build();

        loadGameObjects();
    }

    public synchronized void run() {
        Vector<LveBuffer> uboBuffers = new Vector<>();
        GlobalUbo ubo = new GlobalUbo();

        for (int i = 0; i < LveSwapChain.MAX_FRAMES_IN_FLIGHT; i++) {
            var uboBuffser = new LveBuffer(
                    lveDevice,
                    ubo.sizeOf(),
                    1,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    0);
            uboBuffers.add(uboBuffser);
            uboBuffser.map(VK_WHOLE_SIZE, 0); //map memory
        }

//        SimpleRenderSystem simpleRenderSystem = new SimpleRenderSystem(lveDevice, lveRenderer.getSwapChainRenderPass());
        var globalSetLayout = new LveDescriptorSetLayout.Builder(lveDevice)
                .addBinding(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK_SHADER_STAGE_ALL_GRAPHICS, 1)
                .build();

        Vector<Long> globalDescriptorSets = new Vector<>();
        for (int i = 0; i < LveSwapChain.MAX_FRAMES_IN_FLIGHT; i++) {
            var bufferInfo = uboBuffers.get(i).descriptorInfo(VK_WHOLE_SIZE, 0);
            globalDescriptorSets.add(new LveDescriptorWriter(globalSetLayout, globalPool).writeBuffer(0, bufferInfo).build());
//            new LveDescriptorWriter(globalSetLayout, globalPool).writeBuffer(0, bufferInfo).build();
        }

        SimpleRenderSystem simpleRenderSystem = new SimpleRenderSystem(lveDevice, lveRenderer.getSwapChainRenderPass(), globalSetLayout.getDescriptorSetLayout());
        PointLightSystem pointLightSystem = new PointLightSystem(lveDevice, lveRenderer.getSwapChainRenderPass(), globalSetLayout.getDescriptorSetLayout());

        LveCamera camera = new LveCamera();

        var viewerObject = LveGameObject.createGameObject();
        viewerObject.transform.translation.z = -2.5f;
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
                        camera,
                        globalDescriptorSets.get(frameIndex),
                        gameObjects
                );

                // update
                ubo.projection = camera.getProjection();
                ubo.view = camera.getView();
                ubo.inverseView = camera.getInverseView();

                pointLightSystem.update(frameInfo, ubo);

                ByteBuffer uboByteBuffer = memByteBuffer(uboBuffers.get(frameIndex).getMappedMemory(), (int) uboBuffers.get(frameIndex).getBufferSize());
                uboFloatBuffer(ubo, uboByteBuffer.asFloatBuffer());
//                uboByteBuffer.flip();

                uboBuffers.get(frameIndex).writeToIndex(memAddress(uboByteBuffer), frameIndex);
//                uboBuffers.get(frameIndex).writeToBuffer(memAddress(uboByteBuffer), VKUtil.sizeof(new Matrix4f()) + VKUtil.sizeof(new Matrix4f()) + VKUtil.sizeof(new Vector4f()) + (VKUtil.sizeof(new Vector4f()) * 2 * 10) + Integer.BYTES, frameIndex);
//                uboBuffers.get(frameIndex).flushIndex(frameIndex);

                // render
                lveRenderer.beginSwapChainRenderPass(commandBuffer);
                simpleRenderSystem.renderGameObjects(frameInfo);
                pointLightSystem.render(frameInfo);
                lveRenderer.endSwapChainRenderPass(commandBuffer);
                lveRenderer.endFrame();
            }

//      todo  interesting - memory didn't increase anymore...
//            System.gc();
        }
//        todo?
        gameObjects.values().forEach(obj-> {
            if(obj.model!=null) obj.model.onDestroy();
        });
        uboBuffers.forEach(LveBuffer::onDestroy);
        globalSetLayout.onDestroy();
        globalPool.onDestroy();
        simpleRenderSystem.onDestroy();
        pointLightSystem.onDestroy();
        lveRenderer.onDestroy();
        lveDevice.onDestroy();


//        framebufferSizeCallback.free();
//        keyCallback.free();
        // We don't bother disposing of all Vulkan resources.
        // Let the OS process manager take care of it.
    }

    public synchronized static void uboFloatBuffer(GlobalUbo ubo, FloatBuffer buffer) {
        float[] array = {ubo.projection.m00(), ubo.projection.m01(), ubo.projection.m02(), ubo.projection.m03(),
                ubo.projection.m10(),ubo.projection.m11(),ubo.projection.m12(),ubo.projection.m13(),
                ubo.projection.m20(),ubo.projection.m21(),ubo.projection.m22(),ubo.projection.m23(),
                ubo.projection.m30(),ubo.projection.m31(),ubo.projection.m32(),ubo.projection.m33(),
                ubo.view.m00(), ubo.view.m01(), ubo.view.m02(), ubo.view.m03(),
                ubo.view.m10(),ubo.view.m11(),ubo.view.m12(),ubo.view.m13(),
                ubo.view.m20(),ubo.view.m21(),ubo.view.m22(),ubo.view.m23(),
                ubo.view.m30(),ubo.view.m31(),ubo.view.m32(),ubo.view.m33(),
                ubo.inverseView.m00(), ubo.inverseView.m01(), ubo.inverseView.m02(), ubo.inverseView.m03(),
                ubo.inverseView.m10(),ubo.inverseView.m11(),ubo.inverseView.m12(),ubo.inverseView.m13(),
                ubo.inverseView.m20(),ubo.inverseView.m21(),ubo.inverseView.m22(),ubo.inverseView.m23(),
                ubo.inverseView.m30(),ubo.inverseView.m31(),ubo.inverseView.m32(),ubo.inverseView.m33(),
                ubo.ambientLightColor.x(), ubo.ambientLightColor.y(), ubo.ambientLightColor.z(), ubo.ambientLightColor.w(),
                0,0,0, ubo.numLights.w(),
                ubo.pointLights[0].position.x(), ubo.pointLights[0].position.y(), ubo.pointLights[0].position.z(), ubo.pointLights[0].position.w(),
                ubo.pointLights[0].color.x(), ubo.pointLights[0].color.y(), ubo.pointLights[0].color.z(), ubo.pointLights[0].color.w(),

                ubo.pointLights[1].position.x(), ubo.pointLights[1].position.y(), ubo.pointLights[1].position.z(), ubo.pointLights[1].position.w(),
                ubo.pointLights[1].color.x(), ubo.pointLights[1].color.y(), ubo.pointLights[1].color.z(), ubo.pointLights[1].color.w(),

                ubo.pointLights[2].position.x(), ubo.pointLights[2].position.y(), ubo.pointLights[2].position.z(), ubo.pointLights[2].position.w(),
                ubo.pointLights[2].color.x(), ubo.pointLights[2].color.y(), ubo.pointLights[2].color.z(), ubo.pointLights[2].color.w(),

                ubo.pointLights[3].position.x(), ubo.pointLights[3].position.y(), ubo.pointLights[3].position.z(), ubo.pointLights[3].position.w(),
                ubo.pointLights[3].color.x(), ubo.pointLights[3].color.y(), ubo.pointLights[3].color.z(), ubo.pointLights[3].color.w(),

                ubo.pointLights[4].position.x(), ubo.pointLights[4].position.y(), ubo.pointLights[4].position.z(), ubo.pointLights[4].position.w(),
                ubo.pointLights[4].color.x(), ubo.pointLights[4].color.y(), ubo.pointLights[4].color.z(), ubo.pointLights[4].color.w(),

                ubo.pointLights[5].position.x(), ubo.pointLights[5].position.y(), ubo.pointLights[5].position.z(), ubo.pointLights[5].position.w(),
                ubo.pointLights[5].color.x(), ubo.pointLights[5].color.y(), ubo.pointLights[5].color.z(), ubo.pointLights[5].color.w()
        };
//        System.out.println();
        List.of(array).forEach(cols -> List.of(cols).forEach(buffer::put));
    }

    private void loadGameObjects() throws IOException {
        LveModel lveModel = LveModel.createModelFromFile(lveDevice, "vulkan/models/flat_vase.obj");
        LveGameObject flatVase  = LveGameObject.createGameObject();
        flatVase.model = lveModel;
        flatVase.transform.translation = new Vector3f(-.5f, .5f, 0.f);
        flatVase.transform.scale = new Vector3f(3.f, 1.5f, 3.f);
        gameObjects.put(flatVase.getId(), flatVase);

        lveModel = LveModel.createModelFromFile(lveDevice, "vulkan/models/smooth_vase.obj");
        LveGameObject smoothVase   = LveGameObject.createGameObject();
        smoothVase.model = lveModel;
        smoothVase.transform.translation = new Vector3f(.5f, .5f, 0.f);
        smoothVase.transform.scale = new Vector3f(3.f, 1.5f, 3.f);
        gameObjects.put(smoothVase.getId(), smoothVase);

        lveModel = LveModel.createModelFromFile(lveDevice, "vulkan/models/quad.obj");
        LveGameObject floor = LveGameObject.createGameObject();
        floor.model = lveModel;
        floor.transform.translation = new Vector3f(0.f, .5f, 0.f);
        floor.transform.scale = new Vector3f(3.f, 1.f, 3.f);
        gameObjects.put(floor.getId(), floor);

        List<Vector3f> lightColors = List.of(
            new Vector3f(1.f, .1f, .1f),
            new Vector3f(.1f, .1f, 1.f),
            new Vector3f(.1f, 1.f, .1f),
            new Vector3f(1.f, 1.f, .1f),
            new Vector3f(.1f, 1.f, 1.f),
            new Vector3f(1.f, 1.f, 1.f)
        );

        for (int i = 0; i < lightColors.size(); i++) {
            var pointLight = LveGameObject.makePointLight(1.2f, 0.1f, null);
            pointLight.color = lightColors.get(i);
            var rotateLight = new Matrix4f().rotate((float)(i * 2 * Math.PI / lightColors.size()), new Vector3f(0.f, -1.f, 0.f));
//            pointLight.transform.translation = glm::vec3(rotateLight * glm::vec4(-1.f, -1.f, -1.f, 1.f));
            pointLight.transform.translation = rotateLight.transformProject(new Vector4f(-1.f, -1.f, -1.f, 1.f), new Vector3f());
            gameObjects.put(pointLight.getId(), pointLight);
        }
    }

    public static void main(String[] args) throws IOException {
        new FirstApp().run();
    }
}
