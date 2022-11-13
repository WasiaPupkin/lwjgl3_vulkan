package t16;

import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static util.VKUtil._CHECK_;

public class LveWindow {
    private GLFWKeyCallback keyCallback;
    private int width;
    private int height;
    private String name;
    private final long window;
    private boolean frameBufferResized;

    public long getWindow(){return window;}

    public LveWindow(int width, int height, String name) {
        this.width = width;
        this.height = height;
        this.name = name;
        window = initWindow();
    }

    public long initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        // Create GLFW window
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        long window = glfwCreateWindow(width, height, name, NULL, NULL);

        // Handle canvas resize
        GLFWFramebufferSizeCallback framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int widthCallback, int heightCallback) {
                if (widthCallback <= 0 || heightCallback <= 0)
                    return;
                frameBufferResized = true;
                width = widthCallback;
                height = heightCallback;
            }
        };
        glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);

        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action != GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
                if (key == -1)
                    return;
//                keydown[key] = action == GLFW_PRESS || action == GLFW_REPEAT;
            }
        });

        return window;
    }

    public void onDestroy() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private long createWindowSurface(VkInstance vkInstance) {
        LongBuffer pSurface = memAllocLong(1);
        _CHECK_(glfwCreateWindowSurface(vkInstance, window, null, pSurface),"Failed to create surface: ");
        return pSurface.get(0);
    }

    public VkExtent2D getExtent() {
        return VkExtent2D.create().set(width, height);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    boolean wasWindowResized() {return frameBufferResized; }
    void resetWindowResizedFlag() { frameBufferResized = false; }
}
