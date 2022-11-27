package t25;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;

import java.util.Vector;

import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK12.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK12.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK12.VK_SUCCESS;
import static org.lwjgl.vulkan.VK12.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK12.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK12.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK12.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK12.vkCmdSetScissor;
import static org.lwjgl.vulkan.VK12.vkCmdSetViewport;
import static org.lwjgl.vulkan.VK12.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK12.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK12.vkQueueWaitIdle;
import static t25.LveSwapChain.MAX_FRAMES_IN_FLIGHT;
import static util.VKUtil._CHECK_;

public class LveRenderer {
    private final LveDevice lveDevice;
    private final LveWindow lveWindow;
    private LveSwapChain lveSwapChain;
    private Vector<VkCommandBuffer> commandBuffers;
    private int currentImageIndex;
    private volatile boolean isFrameStarted;
    VkRenderPassBeginInfo renderPassBeginInfo;
    volatile int currentFrameIndex = 0;

    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    public void onDestroy() {
//        didn't work
//        vkFreeCommandBuffers(lveDevice.getDevice(), lveDevice.getCommandPool(), pCommandBuffers);
        commandBuffers.clear();

        lveSwapChain.onDestroy();
        vkDestroySwapchainKHR(lveDevice.getDevice(), lveSwapChain.getSwapChain(), null);
    }

    private void freeCommandBuffers() {
//        fixme
    }

    public LveRenderer(LveDevice lveDevice, LveWindow lveWindow) {
        this.lveDevice = lveDevice;
        this.lveWindow = lveWindow;

        recreateSwapChain();
        createCommandBuffers();
    }

    public float getAspectRatio() { return lveSwapChain.extentAspectRatio(); }

    public synchronized VkCommandBuffer beginFrame() {
        assert !isFrameStarted : "Can't call begin frame while already in progress";

        var ret = lveSwapChain.acquireNextImage();
        int result = ret.get("result");
        currentImageIndex = ret.get("currentImageIndex");

        if (result == VK_ERROR_OUT_OF_DATE_KHR) {
            recreateSwapChain();
            return null;
        }

        if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
            throw new UnsupportedOperationException("Failed to acquire swap chain image");
        }

        isFrameStarted = true;
        VkCommandBuffer commandBuffer = getCurrentCommandBuffer(); // todo why we need local var here?

        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc().sType$Default();
        // Begin recording Command Buffer
        _CHECK_(vkBeginCommandBuffer(commandBuffer, cmdBufInfo), "Failed to begin render command buffer: ");
        cmdBufInfo.free();

        return commandBuffer;
    }

    public synchronized void endFrame() {
        assert isFrameStarted : "Can't call endFrame while frame not in progress";
        VkCommandBuffer commandBuffer = getCurrentCommandBuffer();

        // Stop recording Command Buffer
        _CHECK_(vkEndCommandBuffer(commandBuffer), "Failed to stop record command buffer: ");
        PointerBuffer pCommandBuffers = memAllocPointer(1)
                .put(commandBuffer)
                .flip();

        int result = lveSwapChain.submitCommandBuffers(pCommandBuffers, currentImageIndex);
        memFree(pCommandBuffers);

        if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || lveWindow.wasWindowResized()) {
            lveWindow.resetWindowResizedFlag();
            recreateSwapChain();
        } else if (result != VK_SUCCESS) {
            throw new UnsupportedOperationException("Failed to present swap chain image");
        }

        // Create and submit post present barrier
        vkQueueWaitIdle(lveDevice.getPresentQueue());
        vkQueueWaitIdle(lveDevice.getGraphicsQueue());
        // Destroy this semaphore (we will create a new one in the next frame)
//        lveSwapChain.destroySemaphores();
//        vkDestroySemaphore(lveDevice.getDevice(), pImageAcquiredSemaphore.get(0), null);
//        vkDestroySemaphore(lveDevice.getDevice(), pRenderCompleteSemaphore.get(0), null);

        isFrameStarted = false;
        currentFrameIndex = (currentFrameIndex + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public synchronized void beginSwapChainRenderPass(VkCommandBuffer commandBuffer) {
        assert isFrameStarted : "Can't call beginSwapChainRenderPass while frame not in progress";
        assert commandBuffer.equals(getCurrentCommandBuffer()) : "Can't begin render pass on command buffer from a different frame";

        // Specify clear color (cornflower blue)
        VkClearValue.Buffer clearValues = VkClearValue.calloc(2);
        clearValues.get(0).color()
                .float32(0, 100 / 255.0f)
                .float32(1, 149 / 255.0f)
                .float32(2, 237 / 255.0f)
                .float32(3, 1.0f);

        // Specify clear depth-stencil
        clearValues.get(1).depthStencil()
                .depth(1.0f)
                .stencil(0);

        renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
                .sType$Default()
                .renderPass(lveSwapChain.getRenderPass())
                .framebuffer(lveSwapChain.getFrameBuffer(currentImageIndex))
                .renderArea(ra -> ra
                        .offset(it -> it.x(0).y(0))
                        .extent(lveSwapChain.getSwapChainExtent()))
                .pClearValues(clearValues);
// Set target frame buffer
//        renderPassBeginInfo.framebuffer();

        vkCmdBeginRenderPass(commandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
        renderPassBeginInfo.free();

        // Update dynamic viewport state
        VkViewport.Buffer vkViewportBuffer = VkViewport.calloc(1)
                .height(lveSwapChain.getSwapChainExtent().height())
                .width(lveSwapChain.getSwapChainExtent().width())
                .minDepth(0.0f)
                .maxDepth(1.0f);

        vkCmdSetViewport(commandBuffer, 0, vkViewportBuffer);
        vkViewportBuffer.free();
        // Update dynamic scissor state
        VkRect2D.Buffer scissorBuffer = VkRect2D.calloc(1)
                .extent(lveSwapChain.getSwapChainExtent())
                .offset(it -> it.x(0).y(0));

        vkCmdSetScissor(commandBuffer, 0, scissorBuffer);
        scissorBuffer.free();
    }

    public synchronized void endSwapChainRenderPass(VkCommandBuffer commandBuffer) {
        assert isFrameStarted : "Can't call endSwapChainRenderPass while frame not in progress";
        assert commandBuffer.equals(getCurrentCommandBuffer()) : "Can't end render pass on command buffer from a different frame";

        vkCmdEndRenderPass(commandBuffer);
    }

    public synchronized boolean isFrameInProgress() {
        return isFrameStarted;
    }

    public VkCommandBuffer getCurrentCommandBuffer() {
        assert isFrameStarted : "Cannot get command buffer when frame not in progress";
        return commandBuffers.get(currentImageIndex);
    }

    public synchronized int getFrameIndex() {
        assert isFrameStarted : "Cannot get frame index when frame not in progress";
        return currentFrameIndex;
    }

    public long getSwapChainRenderPass() {
        return lveSwapChain.getRenderPass();
    }

    public LveSwapChain getSwapChain() {
        return lveSwapChain;
    }

    void recreateSwapChain() {
        VkExtent2D extent = lveWindow.getExtent();
        while (extent.width() == 0 || extent.height() == 0) {
            extent = lveWindow.getExtent();
            glfwWaitEvents();
        }

        vkDeviceWaitIdle(lveDevice.getDevice());
        if (lveSwapChain == null) {
            lveSwapChain = new LveSwapChain(lveDevice, extent);
        } else {
            LveSwapChain oldSwapChain = lveSwapChain;
            lveSwapChain = new LveSwapChain(lveDevice, extent, oldSwapChain);

            if(!oldSwapChain.compareSwapFormats(lveSwapChain)) {
                throw new UnsupportedOperationException("Swap chain image\\depth format has changed");
            }
        }
    }

    /**
     * Create render command buffers
     *
     * @return
     */
    private void createCommandBuffers() {
        commandBuffers = new Vector<>();
        commandBuffers.setSize(LveSwapChain.MAX_FRAMES_IN_FLIGHT);

        // Create the render command buffers (one command buffer per framebuffer image)
        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
                .sType$Default()
                .commandPool(lveDevice.getCommandPool())
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(commandBuffers.size());
        PointerBuffer pCommandBuffer = memAllocPointer(commandBuffers.size());
        _CHECK_(vkAllocateCommandBuffers(lveDevice.getDevice(), cmdBufAllocateInfo, pCommandBuffer), "Failed to allocate render command buffer: ");

        for (int i = 0; i < commandBuffers.size(); i++) {
            commandBuffers.set(i, new VkCommandBuffer(pCommandBuffer.get(i), lveDevice.getDevice()));
        }

        memFree(pCommandBuffer);
        cmdBufAllocateInfo.free();
    }

}
