package t12;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.VKUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugReport.vkCreateDebugReportCallbackEXT;
import static org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK12.*;
import static util.VKUtil._CHECK_;
import static util.VKUtil.allocateLayerBuffer;
import static util.VKUtil.getMemoryType;

public class LveDevice {
    private Logger logger = LoggerFactory.getLogger(LveDevice.class);

    private static boolean debug = System.getProperty("NDEBUG") == null;
//    private static boolean debug = false;
    private static String[] layers = {
            "VK_LAYER_LUNARG_standard_validation",
            "VK_LAYER_KHRONOS_validation",
    };

    private final VkInstance vkInstance;
    private final long debugCallbackHandle;
    private final VkPhysicalDevice vkPhysicalDevice;
    private LveWindow lveWindow;
    private final long commandPool;     // VkCommandPool
    private VkDevice vkDevice;
    private long windowSurface;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    public LveDevice(LveWindow lweWindow) {
        if (!glfwVulkanSupported()) {
            throw new AssertionError("GLFW failed to find the Vulkan loader");
        }
        this.lveWindow = lweWindow;
        vkInstance = createInstance();
        debugCallbackHandle = setupDebugging();
        windowSurface = createSurface();
        vkPhysicalDevice = pickPhysicalDevice();
        createLogicalDevice();
        commandPool = createCommandPool();
    }

    public VkDevice getDevice() {return vkDevice; }
    public VkInstance getVkInstance() { return vkInstance; }
    public long getCommandPool() {return commandPool; }
    public long getDebugCallbackHandle(){return debugCallbackHandle;}
    public long getWindowSurface() {return windowSurface; }
    public VkQueue getGraphicsQueue() {return graphicsQueue; }
    public VkQueue getPresentQueue() {return presentQueue; }

    private PointerBuffer getRequiredExtensions() {
        /* Look for instance extensions */
        logger.info("Available Extensions: ");
        VKUtil.enumerateSupportedInstanceExtensions().forEach(logger::info);
        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
        if (requiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
        }else {
            logger.info("Required Extensions: ");
            for(int i=0; i<requiredExtensions.capacity(); i++){
                logger.info(requiredExtensions.getStringUTF8(i));
            }
        }
        return requiredExtensions;
    }

    private long createSurface() {
        LongBuffer pSurface = memAllocLong(1);
        _CHECK_(glfwCreateWindowSurface(getVkInstance(), lveWindow.getWindow(), null, pSurface),"Failed to create surface: ");
        long surface = pSurface.get(0);

//        todo?
        memFree(pSurface);

        return surface;
    }
    /**
     * Create a Vulkan instance using LWJGL 3.
     *
     * @return the VkInstance handle
     */
    private VkInstance createInstance() {
        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType$Default()
                .apiVersion(VK_API_VERSION_1_2);
        PointerBuffer requiredExtensions = getRequiredExtensions();
        PointerBuffer ppEnabledExtensionNames = memAllocPointer(requiredExtensions.remaining() + 1);
        ppEnabledExtensionNames.put(requiredExtensions);
        ByteBuffer VK_EXT_DEBUG_REPORT_EXTENSION = memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
        ppEnabledExtensionNames.put(VK_EXT_DEBUG_REPORT_EXTENSION);
        ppEnabledExtensionNames.flip();
        PointerBuffer ppEnabledLayerNames = debug ? allocateLayerBuffer(layers) : null;

        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType$Default()
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames)
                .ppEnabledLayerNames(ppEnabledLayerNames);

        PointerBuffer pInstance = memAllocPointer(1);
        _CHECK_(vkCreateInstance(pCreateInfo, null, pInstance), "Failed to create VkInstance: ");
        long instance = pInstance.get(0);
        memFree(pInstance);

        VkInstance ret = new VkInstance(instance, pCreateInfo);
        pCreateInfo.free();
        if(ppEnabledLayerNames != null) memFree(ppEnabledLayerNames);
        memFree(VK_EXT_DEBUG_REPORT_EXTENSION);
        memFree(ppEnabledExtensionNames);
        memFree(appInfo.pApplicationName());
        memFree(appInfo.pEngineName());
        appInfo.free();
        return ret;
    }

    private long setupDebugging() {
        final VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
            public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                logger.error("ERROR OCCURED: " + VkDebugReportCallbackEXT.getString(pMessage));
                return 0;
            }
        };

        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
                .sType$Default()
                .pfnCallback(debugCallback)
                .flags(VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT);

        LongBuffer pCallback = memAllocLong(1);
        _CHECK_(vkCreateDebugReportCallbackEXT(vkInstance, dbgCreateInfo, null, pCallback), "Failed to create VkInstance: ");
        long callbackHandle = pCallback.get(0);
        memFree(pCallback);
        dbgCreateInfo.free();

        return callbackHandle;
    }

    private VkPhysicalDevice pickPhysicalDevice() {
        IntBuffer pPhysicalDeviceCount = memAllocInt(1);
        _CHECK_(vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, null), "Failed to get number of physical devices: ");

        PointerBuffer pPhysicalDevices = memAllocPointer(pPhysicalDeviceCount.get(0));
        _CHECK_(vkEnumeratePhysicalDevices(vkInstance, pPhysicalDeviceCount, pPhysicalDevices), "Failed to get physical devices: ");
        long physicalDevice = pPhysicalDevices.get(0);
        memFree(pPhysicalDeviceCount);
        memFree(pPhysicalDevices);

        return new VkPhysicalDevice(physicalDevice, vkInstance);
    }

    private void createLogicalDevice() {
        QueueFamilyIndices queueFamilyIndices = findQueueFamilies(vkPhysicalDevice);
        Set<Integer> tmp = new HashSet<>();
        tmp.add(queueFamilyIndices.graphicsFamily);
        tmp.add(queueFamilyIndices.presentFamily);
        List<Integer> uniqueQueueFamilies = new ArrayList<>(tmp);

        VkDeviceQueueCreateInfo.Buffer queueCreateInfoBuffer = VkDeviceQueueCreateInfo.create(uniqueQueueFamilies.size());

        for (int i=0; i<uniqueQueueFamilies.size(); i++) {
            FloatBuffer pQueuePriorities = memAllocFloat(1).put(0.0f);
            pQueuePriorities.flip();

            queueCreateInfoBuffer.get(i)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(uniqueQueueFamilies.get(i))
                    .pQueuePriorities(pQueuePriorities);

            memFree(pQueuePriorities);
        }

        PointerBuffer extensions = memAllocPointer(1);
        ByteBuffer VK_KHR_SWAPCHAIN_EXTENSION = memUTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        extensions.put(VK_KHR_SWAPCHAIN_EXTENSION);
        extensions.flip();

        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.create()
                .samplerAnisotropy(true);

        VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc()
                .sType$Default()
                .pQueueCreateInfos(queueCreateInfoBuffer)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(extensions);

        PointerBuffer pDevice = memAllocPointer(1);
        _CHECK_(vkCreateDevice(vkPhysicalDevice, deviceCreateInfo, null, pDevice), "Failed to create device: ");
        long device = pDevice.get(0);
        memFree(pDevice);

        vkDevice = new VkDevice(device, vkPhysicalDevice, deviceCreateInfo);

        PointerBuffer pGraphicsQueue = memAllocPointer(1);
        vkGetDeviceQueue(vkDevice, queueFamilyIndices.graphicsFamily, 0, pGraphicsQueue);
        long lGraphicsQueue = pGraphicsQueue.get(0);
        memFree(pGraphicsQueue);

        PointerBuffer pPresentQueue = memAllocPointer(1);
        vkGetDeviceQueue(vkDevice, queueFamilyIndices.presentFamily, 0, pPresentQueue);
        long lPresentQueue = pPresentQueue.get(0);
        memFree(pPresentQueue);

        graphicsQueue =  new VkQueue(lGraphicsQueue, vkDevice);
        presentQueue = new VkQueue(lPresentQueue, vkDevice);

        // free memory
        deviceCreateInfo.free();
        memFree(VK_KHR_SWAPCHAIN_EXTENSION);
        memFree(extensions);
    }

    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        IntBuffer pQueueFamilyPropertyCount = memAllocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pQueueFamilyPropertyCount, null);
        int queueFamilyCount = pQueueFamilyPropertyCount.get(0);
        VkQueueFamilyProperties.Buffer queueProps = VkQueueFamilyProperties.calloc(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pQueueFamilyPropertyCount, queueProps);
        memFree(pQueueFamilyPropertyCount);

        int i = 0;
        for (var props : queueProps) {

            if (props.queueCount() > 0 && (props.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                indices.graphicsFamily = i;
                indices.graphicsFamilyHasValue = true;
            }

            IntBuffer supportsPresent = memAllocInt(1);
            supportsPresent.position(i);
            _CHECK_(vkGetPhysicalDeviceSurfaceSupportKHR(device, i, windowSurface, supportsPresent),
                    "Failed to physical device surface support: ");
            boolean presentSupport = supportsPresent.get(i) == VK_TRUE;

            if (props.queueCount() > 0 && presentSupport) {
                indices.presentFamily = i;
                indices.presentFamilyHasValue = true;
            }

            if (indices.isComplete()) {
                break;
            }

            i++;
            memFree(supportsPresent);
        }

        // free memory
        queueProps.free();

        return indices;
    }

    static class QueueFamilyIndices {
        int graphicsFamily;
        int presentFamily;
        boolean graphicsFamilyHasValue = false;
        boolean presentFamilyHasValue = false;
        boolean isComplete() { return graphicsFamilyHasValue && presentFamilyHasValue; }
    }

    private long createCommandPool() {
        QueueFamilyIndices queueFamilyIndices = findQueueFamilies(vkPhysicalDevice);

        VkCommandPoolCreateInfo cmdPoolInfo = VkCommandPoolCreateInfo.calloc()
                .sType$Default()
                .queueFamilyIndex(queueFamilyIndices.graphicsFamily)
                .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT | VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

        LongBuffer pCmdPool = memAllocLong(1);
        _CHECK_(vkCreateCommandPool(vkDevice, cmdPoolInfo, null, pCmdPool), "Failed to create command pool: ");
        long commandPool = pCmdPool.get(0);

        // free memory
        cmdPoolInfo.free();
        memFree(pCmdPool);

        return commandPool;
    }

    public void onDestroy() {
        vkDestroyCommandPool(vkDevice, commandPool, null);
        vkDestroyDevice(vkDevice, null);

        if (debug) {
            vkDestroyDebugReportCallbackEXT(vkInstance, getDebugCallbackHandle(), null);
        }

        vkDestroySurfaceKHR(vkInstance, windowSurface, null);
        vkDestroyInstance(vkInstance, null);
    }

    static class SwapChainSupportDetails {
        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;
    }

    public SwapChainSupportDetails getSwapChainSupport() { return querySwapChainSupport(vkPhysicalDevice); }

    private SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice physicalDevice) {
        SwapChainSupportDetails swapChainSupportDetails = new SwapChainSupportDetails();

        // Get physical device surface properties and formats
        swapChainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.calloc();
        _CHECK_(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, windowSurface, swapChainSupportDetails.capabilities),
                "Failed to get physical device surface capabilities: ");

        // Get list of supported formats
        IntBuffer pFormatCount = memAllocInt(1);
        _CHECK_(vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, windowSurface, pFormatCount, null),
                "Failed to query number of physical device surface formats: ");
        int formatCount = pFormatCount.get(0);

        swapChainSupportDetails.formats = VkSurfaceFormatKHR.calloc(formatCount);
        _CHECK_(vkGetPhysicalDeviceSurfaceFormatsKHR(vkPhysicalDevice, windowSurface, pFormatCount, swapChainSupportDetails.formats),
                "Failed to query physical device surface formats: ");
        memFree(pFormatCount);

        IntBuffer pPresentModeCount = memAllocInt(1);
        _CHECK_(vkGetPhysicalDeviceSurfacePresentModesKHR(vkPhysicalDevice, windowSurface, pPresentModeCount, null), "Failed to get number of physical device surface presentation modes: ");
        int presentModeCount = pPresentModeCount.get(0);

        swapChainSupportDetails.presentModes = memAllocInt(presentModeCount);
        _CHECK_(vkGetPhysicalDeviceSurfacePresentModesKHR(vkPhysicalDevice, windowSurface, pPresentModeCount, swapChainSupportDetails.presentModes), "Failed to get physical device surface presentation modes: ");
        memFree(pPresentModeCount);

        return swapChainSupportDetails;
    }

    int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(vkPhysicalDevice, memoryProperties);

        IntBuffer memoryTypeIndex = memAllocInt(1);
        getMemoryType(memoryProperties, typeFilter, properties, memoryTypeIndex);
        int memType = memoryTypeIndex.get(0);
        memFree(memoryTypeIndex);
        return memType;
    }

    public QueueFamilyIndices findPhysicalQueueFamilies() { return findQueueFamilies(vkPhysicalDevice); }

    int findSupportedFormat(int[] candidates, int tiling, int features) {
        for (int format : candidates) {
            VkFormatProperties props = VkFormatProperties.calloc();
            vkGetPhysicalDeviceFormatProperties(vkPhysicalDevice, format, props);

            if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                return format;
            } else if (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                return format;
            }
        }
        throw new UnsupportedOperationException("failed to find supported format");
    }

    public Map<String, Long> createBuffer(long size, int usage, int properties) {
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
                .sType$Default();

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
                .sType$Default()
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer pBuffer = memAllocLong(1);
        _CHECK_(vkCreateBuffer(vkDevice, bufferInfo, null, pBuffer), "failed to create vertex buffer");
        long verticesBuf = pBuffer.get(0);
        memFree(pBuffer);
        bufferInfo.free();

        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc();
        vkGetBufferMemoryRequirements(vkDevice, verticesBuf, memRequirements);

        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties));

        LongBuffer pBufferMemory = memAllocLong(1);
        _CHECK_(vkAllocateMemory(vkDevice, allocInfo, null, pBufferMemory), "failed to allocate vertex buffer memory!");
        long bufferMemory = pBufferMemory.get(0);
        memFree(pBufferMemory);

        _CHECK_(vkBindBufferMemory(vkDevice, verticesBuf, bufferMemory, 0),"Failed to bind memory to vertex buffer: ");

        Map<String, Long> map = new HashMap<>();
        map.put("buffer", verticesBuf);
        map.put("bufferMemory", bufferMemory);

        return map;
    }

    public long createImageWithInfo(VkImageCreateInfo imageInfo, int properties) {
        VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc();
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
                .sType$Default();

        LongBuffer pImage = memAllocLong(1);
        _CHECK_(vkCreateImage(vkDevice, imageInfo, null, pImage), "Failed to create image: ");
        long image = pImage.get(0);

        memFree(pImage); //todo hz?
        imageInfo.free(); //todo hz?

        vkGetImageMemoryRequirements(vkDevice, image, memRequirements);

        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties));

        LongBuffer pImageMemory = memAllocLong(1);
        _CHECK_(vkAllocateMemory(vkDevice, allocInfo, null, pImageMemory), "Failed to create image memory: ");
        long imageMemory = pImageMemory.get(0);
        memFree(pImageMemory);
        allocInfo.free();

        _CHECK_(vkBindImageMemory(vkDevice, image, imageMemory, 0), "Failed to bind image to memory: ");

        return image;
    }
}
