package util;/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSpecializationInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static util.IOUtil.ioResourceToByteBuffer;
import static java.lang.Math.floor;
import static org.lwjgl.BufferUtils.createByteBuffer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_anyhit_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_closesthit_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_initialize;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_include_callbacks;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_optimization_level;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_target_env;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_target_spirv;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compute_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_env_version_vulkan_1_2;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_intersection_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_miss_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_optimization_level_performance;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_raygen_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_length;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_spirv_version_1_4;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_target_env_vulkan;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_vertex_shader;
import static org.lwjgl.vulkan.EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT;
import static org.lwjgl.vulkan.KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_ERROR_SURFACE_LOST_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_ANY_HIT_BIT_NV;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_INTERSECTION_BIT_NV;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_MISS_BIT_NV;
import static org.lwjgl.vulkan.NVRayTracing.VK_SHADER_STAGE_RAYGEN_BIT_NV;
import static org.lwjgl.vulkan.VK10.VK_ERROR_DEVICE_LOST;
import static org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_FEATURE_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_FORMAT_NOT_SUPPORTED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_INCOMPATIBLE_DRIVER;
import static org.lwjgl.vulkan.VK10.VK_ERROR_INITIALIZATION_FAILED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_LAYER_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_MEMORY_MAP_FAILED;
import static org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY;
import static org.lwjgl.vulkan.VK10.VK_ERROR_OUT_OF_HOST_MEMORY;
import static org.lwjgl.vulkan.VK10.VK_ERROR_TOO_MANY_OBJECTS;
import static org.lwjgl.vulkan.VK10.VK_EVENT_RESET;
import static org.lwjgl.vulkan.VK10.VK_EVENT_SET;
import static org.lwjgl.vulkan.VK10.VK_INCOMPLETE;
import static org.lwjgl.vulkan.VK10.VK_NOT_READY;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_COMPUTE_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_TIMEOUT;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;

/**
 * Utility functions for Vulkan.
 *
 * @author Kai Burjack
 */
public class VKUtil {

    public static final int VK_FLAGS_NONE = 0;
    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    private static int vulkanStageToShadercKind(int stage) {
        switch (stage) {
            case VK_SHADER_STAGE_VERTEX_BIT:
                return shaderc_vertex_shader;
            case VK_SHADER_STAGE_FRAGMENT_BIT:
                return shaderc_fragment_shader;
            case VK_SHADER_STAGE_RAYGEN_BIT_NV:
                return shaderc_raygen_shader;
            case VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV:
                return shaderc_closesthit_shader;
            case VK_SHADER_STAGE_MISS_BIT_NV:
                return shaderc_miss_shader;
            case VK_SHADER_STAGE_ANY_HIT_BIT_NV:
                return shaderc_anyhit_shader;
            case VK_SHADER_STAGE_INTERSECTION_BIT_NV:
                return shaderc_intersection_shader;
            case VK_SHADER_STAGE_COMPUTE_BIT:
                return shaderc_compute_shader;
            default:
                throw new IllegalArgumentException("Stage: " + stage);
        }
    }

    public static ByteBuffer glslToSpirv(String classPath, int vulkanStage) throws IOException {
        ByteBuffer src = ioResourceToByteBuffer(classPath, 1024);
        long compiler = shaderc_compiler_initialize();
        long options = shaderc_compile_options_initialize();
        ShadercIncludeResolve resolver;
        ShadercIncludeResultRelease releaser;
        shaderc_compile_options_set_target_env(options, shaderc_target_env_vulkan, shaderc_env_version_vulkan_1_2);
        shaderc_compile_options_set_target_spirv(options, shaderc_spirv_version_1_4);
        shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);
        shaderc_compile_options_set_include_callbacks(options, resolver = new ShadercIncludeResolve() {
            public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
                ShadercIncludeResult res = ShadercIncludeResult.calloc();
                try {
                    String src = classPath.substring(0, classPath.lastIndexOf('/')) + "/" + memUTF8(requested_source);
                    res.content(ioResourceToByteBuffer(src, 1024));
                    res.source_name(memUTF8(src));
                    return res.address();
                } catch (IOException e) {
                    throw new AssertionError("Failed to resolve include: " + src);
                }
            }
        }, releaser = new ShadercIncludeResultRelease() {
            public void invoke(long user_data, long include_result) {
                ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
                memFree(result.source_name());
                result.free();
            }
        }, 0L);
        long res;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            res = shaderc_compile_into_spv(compiler, src, vulkanStageToShadercKind(vulkanStage), stack.UTF8(classPath), stack.UTF8("main"), options);
            if (res == 0L)
                throw new AssertionError("Internal error during compilation!");
        }
        if (shaderc_result_get_compilation_status(res) != shaderc_compilation_status_success) {
            throw new AssertionError("Shader compilation failed: " + shaderc_result_get_error_message(res));
        }
        int size = (int) shaderc_result_get_length(res);
        ByteBuffer resultBytes = createByteBuffer(size);
        resultBytes.put(shaderc_result_get_bytes(res));
        resultBytes.flip();
        shaderc_result_release(res);
        shaderc_compiler_release(compiler);
        releaser.free();
        resolver.free();
        return resultBytes;
    }

    public static void _CHECK_(int ret, String msg) {
        if (ret != VK_SUCCESS)
            throw new AssertionError(msg + ": " + translateVulkanResult(ret));
    }

    public static void loadShader(VkPipelineShaderStageCreateInfo info, VkSpecializationInfo specInfo, MemoryStack stack, VkDevice device, String classPath,
                                  int stage) throws IOException {
        ByteBuffer shaderCode = glslToSpirv(classPath, stage);
        LongBuffer pShaderModule = stack.mallocLong(1);
        _CHECK_(vkCreateShaderModule(device, VkShaderModuleCreateInfo.calloc(stack).sType$Default().pCode(shaderCode).flags(0), null, pShaderModule),
                "Failed to create shader module");
        info.stage(stage).pSpecializationInfo(specInfo).module(pShaderModule.get(0)).pName(stack.UTF8("main"));
    }

    /**
     * Translates a Vulkan {@code VkResult} value to a String describing the result.
     *
     * @param result the {@code VkResult} value
     *
     * @return the result description
     */
    public static String translateVulkanResult(int result) {
        switch (result) {
            // Success codes
            case VK_SUCCESS:
                return "Command successfully completed.";
            case VK_NOT_READY:
                return "A fence or query has not yet completed.";
            case VK_TIMEOUT:
                return "A wait operation has not completed in the specified time.";
            case VK_EVENT_SET:
                return "An event is signaled.";
            case VK_EVENT_RESET:
                return "An event is unsignaled.";
            case VK_INCOMPLETE:
                return "A return array was too small for the result.";
            case VK_SUBOPTIMAL_KHR:
                return "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";

            // Error codes
            case VK_ERROR_OUT_OF_HOST_MEMORY:
                return "A host memory allocation has failed.";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY:
                return "A device memory allocation has failed.";
            case VK_ERROR_INITIALIZATION_FAILED:
                return "Initialization of an object could not be completed for implementation-specific reasons.";
            case VK_ERROR_DEVICE_LOST:
                return "The logical or physical device has been lost.";
            case VK_ERROR_MEMORY_MAP_FAILED:
                return "Mapping of a memory object has failed.";
            case VK_ERROR_LAYER_NOT_PRESENT:
                return "A requested layer is not present or could not be loaded.";
            case VK_ERROR_EXTENSION_NOT_PRESENT:
                return "A requested extension is not supported.";
            case VK_ERROR_FEATURE_NOT_PRESENT:
                return "A requested feature is not supported.";
            case VK_ERROR_INCOMPATIBLE_DRIVER:
                return "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
            case VK_ERROR_TOO_MANY_OBJECTS:
                return "Too many objects of the type have already been created.";
            case VK_ERROR_FORMAT_NOT_SUPPORTED:
                return "A requested format is not supported on this device.";
            case VK_ERROR_SURFACE_LOST_KHR:
                return "A surface is no longer available.";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR:
                return "The requested window is already connected to a VkSurfaceKHR, or to some other non-Vulkan API.";
            case VK_ERROR_OUT_OF_DATE_KHR:
                return "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the "
                        + "swapchain will fail. Applications must query the new surface properties and recreate their swapchain if they wish to continue"
                        + "presenting to the surface.";
            case VK_ERROR_INCOMPATIBLE_DISPLAY_KHR:
                return "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an"
                        + " image.";
            case VK_ERROR_VALIDATION_FAILED_EXT:
                return "A validation layer found an error.";
            default:
                return String.format("%s [%d]", "Unknown", Integer.valueOf(result));
        }
    }

    public static final PointerBuffer allocateLayerBuffer(String[] layers) {
        final Set<String> availableLayers = getAvailableLayers();

        PointerBuffer ppEnabledLayerNames = memAllocPointer(layers.length);
        System.out.println("Using layers:");
        for (int i = 0; i < layers.length; i++) {
            final String layer = layers[i];
            if (availableLayers.contains(layer)) {
                System.out.println("\t" + layer);
                ppEnabledLayerNames.put(memUTF8(layer));
            }
        }
        ppEnabledLayerNames.flip();
        return ppEnabledLayerNames;
    }

    private static final Set<String> getAvailableLayers() {
        final Set<String> res = new HashSet<>();
        final int[] ip = new int[1];
        vkEnumerateInstanceLayerProperties(ip, null);
        final int count = ip[0];

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            if (count > 0) {
                final VkLayerProperties.Buffer instanceLayers = VkLayerProperties.malloc(count, stack);
                vkEnumerateInstanceLayerProperties(ip, instanceLayers);
                for (int i = 0; i < count; i++) {
                    final String layerName = instanceLayers.get(i).layerNameString();
                    res.add(layerName);
                }
            }
        }

        return res;
    }

    // Will be in LWJGL 3.3.2
    public static PointerBuffer pointersOfElements(MemoryStack stack, CustomBuffer<?> buffer) {
        int remaining = buffer.remaining();
        long addr = buffer.address();
        long sizeof = buffer.sizeof();
        PointerBuffer pointerBuffer = stack.mallocPointer(remaining);
        for (int i = 0; i < remaining; i++) {
            pointerBuffer.put(i, addr + sizeof * i);
        }
        return pointerBuffer;
    }

    public static List<String> enumerateSupportedInstanceExtensions() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pPropertyCount = stack.mallocInt(1);
            _CHECK_(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pPropertyCount, null),
                    "Could not enumerate number of instance extensions");
            int propertyCount = pPropertyCount.get(0);
            VkExtensionProperties.Buffer pProperties = VkExtensionProperties.malloc(propertyCount, stack);
            _CHECK_(vkEnumerateInstanceExtensionProperties((ByteBuffer) null, pPropertyCount, pProperties),
                    "Could not enumerate instance extensions");
            List<String> res = new ArrayList<>(propertyCount);
            for (int i = 0; i < propertyCount; i++) {
                res.add(pProperties.get(i).extensionNameString());
            }
            return res;
        }
    }

    public static float[][] mergeMatrix(float[][] A, float[][] B){
        int firstArrayRows = A.length;
        int secondArrayRows = B.length;

        if(firstArrayRows != secondArrayRows) throw new UnsupportedOperationException("У складываемых массивов должно быть одинаковое количество строк");

        int firstArrayCols = A[0].length;
        int secondArrayCols = B[0].length;

        // Matrix to store the result
        float[][] C = new float[firstArrayRows][firstArrayCols + secondArrayCols];

        // Merge the two matrices
        for(int i = 0; i < firstArrayRows; i++) {
            for(int j = 0; j < secondArrayCols; j++) {
                // To store elements of matrix A
                try {
                    C[i][j] = A[i][j];
                } catch (Exception ignored) {}
                // To store elements of matrix B
                C[i][j + firstArrayCols] = B[i][j];
            }
        }
        return C;
    }

    public static void matrixToFloatBuffer(float[][] array, FloatBuffer buffer) {
        List.of(array).forEach(cols -> List.of(cols).forEach(buffer::put));
    }

    public static void matrixToIntBuffer(int[] array, IntBuffer buffer) {
        List.of(array).forEach(buffer::put);
    }

    public static boolean getMemoryType(VkPhysicalDeviceMemoryProperties deviceMemoryProperties, int typeBits, int properties, IntBuffer typeIndex) {
        int bits = typeBits;
        for (int i = 0; i < 32; i++) {
            if ((bits & 1) == 1) {
                if ((deviceMemoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    typeIndex.put(0, i);
                    return true;
                }
            }
            bits >>= 1;
        }
        return false;
    }

    public static int sizeof(Object type) {
        if (type instanceof Vector2f vec2) {
            return (new float[] {vec2.x(), vec2.y()}).length * Float.BYTES;
        } else if (type instanceof Vector3f vec3) {
            return (new float[] {vec3.x(), vec3.y(), vec3.z()}).length * Float.BYTES;
        } else if (type instanceof Matrix4f mat4) {
            return (new float[] {mat4.m00(), mat4.m01(), mat4.m02(), mat4.m03(),
                    mat4.m10(),mat4.m11(),mat4.m12(),mat4.m13(),
                    mat4.m20(),mat4.m21(),mat4.m22(),mat4.m23(),
                    mat4.m30(),mat4.m31(),mat4.m32(),mat4.m33()}).length * Float.BYTES;
        }
        throw new UnsupportedOperationException("Unknown data type");
    }

    public static float modulo(float x, float y){
        return (float) (x - y * floor(x / y));
    }
}
