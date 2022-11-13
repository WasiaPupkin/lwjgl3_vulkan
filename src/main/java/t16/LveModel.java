package t16;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Map;
import java.util.Vector;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memCopy;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK12.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK12.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK12.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK12.vkCmdDraw;
import static org.lwjgl.vulkan.VK12.vkMapMemory;
import static org.lwjgl.vulkan.VK12.vkUnmapMemory;
import static util.VKUtil._CHECK_;
import static util.VKUtil.matrixToFloatBuffer;
import static util.VKUtil.matrixToIntBuffer;

public class LveModel {
    static class Builder {
        public Vector<Vertex> vertices = new Vector<>();
        public int[] indices;
    }
    private final static int BYTES_PER_ELEMENT = 4;
    private final LveDevice lveDevice;
    private Vector<Vertex> vertices;
    private float[][] verts;
    private float[][] mergedMatrices;
    private float[] mergedMatrices1;
    private int vertexCount;
    private long vertexBuffer;
    private long vertexBufferMemory;
    private boolean hasIndexBuffer = false;
    private long indexBuffer;
    private long indexBufferMemory;
    private int indexCount;

    public LveModel(LveDevice lveDevice, LveModel.Builder builder) {
        this.lveDevice = lveDevice;
//        this.vertices = builder.vertices;
        this.mergedMatrices = toFloatArr(builder.vertices);

        createVertexBuffers(mergedMatrices);
        createIndexBuffers(builder.indices);
    }

//    fixme
    void onDestroy() {
//        vkDestroyBuffer(lveDevice.device(), vertexBuffer, nullptr);
//        vkFreeMemory(lveDevice.device(), vertexBufferMemory, nullptr);
//
//        if (hasIndexBuffer) {
//            vkDestroyBuffer(lveDevice.device(), indexBuffer, nullptr);
//            vkFreeMemory(lveDevice.device(), indexBufferMemory, nullptr);
//        }
    }

//    private long createVertexBuffer1(){
//        // строки * столбцы * 4byte
//        ByteBuffer vertexBuffer = memAlloc(mergedMatrices.length * mergedMatrices[0].length * BYTES_PER_ELEMENT);
////        vertexBuffer.alignmentOffset(1, 8);
////        FloatBuffer hz = vertexBuffer
//        matrixToFloatBuffer(mergedMatrices, vertexBuffer.asFloatBuffer());
//
//        return allocateVerticesBuffer(device.getDevice(), device.getMemoryProperties(), vertexBuffer);
//    }
    private float[][] toFloatArr(Vector<Vertex> vertices) {
        float[][] mergedMatrices = new float[vertices.size()][];
        for (int i = 0; i < vertices.size(); i++) {
            mergedMatrices[i] = new float[]{vertices.get(i).position.x(), vertices.get(i).position.y(), vertices.get(i).position.z(), vertices.get(i).color.x(), vertices.get(i).color.y(), vertices.get(i).color.z()};
        }
        return mergedMatrices;
    }

//    private long createVertexBuffer(){
//        // строки * столбцы * 4byte
//        ByteBuffer vertexBuffer = memAlloc(mergedMatrices.length * mergedMatrices[0].length * BYTES_PER_ELEMENT);
//        matrixToFloatBuffer(mergedMatrices, vertexBuffer.asFloatBuffer());
//
//        return allocateVerticesBuffer(lveDevice.getDevice(), lveDevice.getMemoryProperties(), vertexBuffer);
//    }

    private void createVertexBuffers(float[][] fVertices) {
        vertexCount = fVertices.length;
        assert vertexCount >= 3 : "Vertex count must be at least 3";
        int bufferSize = fVertices[0].length * Float.BYTES * vertexCount;

        long stagingBuffer;
        long stagingBufferMemory;
        Map<String, Long> stagingCallback = lveDevice.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        stagingBuffer = stagingCallback.get("buffer");
        stagingBufferMemory = stagingCallback.get("bufferMemory");

        PointerBuffer pData = memAllocPointer(1);
        _CHECK_(vkMapMemory(lveDevice.getDevice(), stagingBufferMemory, 0, bufferSize, 0, pData), "Failed to map vertex memory: ");
        long data = pData.get(0);
        memFree(pData);

        ByteBuffer vertexByteBuffer = memAlloc(bufferSize);
        matrixToFloatBuffer(fVertices, vertexByteBuffer.asFloatBuffer());

        memCopy(memAddress(vertexByteBuffer), data, bufferSize);
        memFree(vertexByteBuffer);
        vkUnmapMemory(lveDevice.getDevice(), stagingBufferMemory);

        Map<String, Long> callback = lveDevice.createBuffer(bufferSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        vertexBuffer = callback.get("buffer");
        vertexBufferMemory = callback.get("bufferMemory");

        lveDevice.copyBuffer(stagingBuffer, vertexBuffer, bufferSize);

        vkDestroyBuffer(lveDevice.getDevice(), stagingBuffer, null);
        vkFreeMemory(lveDevice.getDevice(), stagingBufferMemory, null);
    }

    private void createIndexBuffers(int[] indices) {
        indexCount  = indices.length;
        hasIndexBuffer = indexCount > 0;

        if (!hasIndexBuffer) {
            return;
        }

        int bufferSize = Integer.BYTES * indexCount;

        long stagingBuffer;
        long stagingBufferMemory;
        Map<String, Long> stagingCallback = lveDevice.createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        stagingBuffer = stagingCallback.get("buffer");
        stagingBufferMemory = stagingCallback.get("bufferMemory");

        PointerBuffer pData = memAllocPointer(1);
        _CHECK_(vkMapMemory(lveDevice.getDevice(), stagingBufferMemory, 0, bufferSize, 0, pData), "Failed to map vertex memory: ");
        long data = pData.get(0);
        memFree(pData);

        ByteBuffer vertexByteBuffer = memAlloc(bufferSize);
        matrixToIntBuffer(indices, vertexByteBuffer.asIntBuffer());

        memCopy(memAddress(vertexByteBuffer), data, bufferSize);
        memFree(vertexByteBuffer);
        vkUnmapMemory(lveDevice.getDevice(), stagingBufferMemory);

        Map<String, Long> callback = lveDevice.createBuffer(bufferSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        indexBuffer = callback.get("buffer");
        indexBufferMemory = callback.get("bufferMemory");

        lveDevice.copyBuffer(stagingBuffer, indexBuffer, bufferSize);

        vkDestroyBuffer(lveDevice.getDevice(), stagingBuffer, null);
        vkFreeMemory(lveDevice.getDevice(), stagingBufferMemory, null);
    }

    // Bind triangle vertices
    public void bind(VkCommandBuffer renderCommandBuffer){
        LongBuffer offsets = memAllocLong(1);
        offsets.put(0, 0L);
        LongBuffer pBuffers = memAllocLong(1);
        pBuffers.put(0, vertexBuffer);
        vkCmdBindVertexBuffers(renderCommandBuffer, 0, pBuffers, offsets);
        memFree(pBuffers);
        memFree(offsets);

        if (hasIndexBuffer) {
            vkCmdBindIndexBuffer(renderCommandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32);
        }
    }

    // Draw triangle
    public void draw(VkCommandBuffer renderCommandBuffer){
        if (hasIndexBuffer) {
            vkCmdDrawIndexed(renderCommandBuffer, indexCount, 1, 0, 0, 0);
        } else {
            vkCmdDraw(renderCommandBuffer, vertexCount, 1, 0, 0);
        }
    }

    static class Vertex {
        Vector3f position;
        Vector3f color;

        public Vertex(Vector3f position, Vector3f color) {
            this.position = position;
            this.color = color;
        }

        public static VkVertexInputBindingDescription.Buffer getBindingDescriptions(){

            return VkVertexInputBindingDescription.calloc(1)
                    .binding(0) // <- we bind our vertex buffer to point 0
                    .stride(sizeof(new Vector3f()) * 2) // offset for the next buffer element per vertex [(x,y),(r,g,b,a)] <-- that was 2+4
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        }

        public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(){
            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(2);
            // Location 0 : Position
            attributeDescriptions.get(0)
                    .binding(0) // <- binding point used in the VkVertexInputBindingDescription
                    .location(0) // <- location in the shader's attribute layout (inside the shader source)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(0);
            // Location 1 : Color
            attributeDescriptions.get(1)
                    .binding(0) // <- binding point used in the VkVertexInputBindingDescription
                    .location(1) // <- location in the shader's attribute layout (inside the shader source)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(sizeof(new Vector3f())); // color followed by verts. so to get it, we must offset verts in bytes

            return attributeDescriptions;
        }

        public static int sizeof() {
            return sizeof(new Vector2f()) + sizeof(new Vector3f());
        }

        public static int sizeof(Object type) {
            if (type instanceof Vector2f vec2) {
                return (new float[] {vec2.x(), vec2.y()}).length * Float.BYTES;
            } else if (type instanceof Vector3f vec3) {
                return (new float[] {vec3.x(), vec3.y(), vec3.z()}).length * Float.BYTES;
            }
            throw new UnsupportedOperationException("Unknown data type");
        }

        public static int offsetof(Object type) {
            if (type instanceof Vector2f vec2) {
                return 0;
            } else if (type instanceof Vector3f vec3) {
                return sizeof(new Vector2f());
            }
            throw new UnsupportedOperationException("Unknown data type");
        }
    }
}
