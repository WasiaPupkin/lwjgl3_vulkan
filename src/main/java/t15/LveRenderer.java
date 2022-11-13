package t15;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkViewport;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Vector;

import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
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
import static t15.LveSwapChain.MAX_FRAMES_IN_FLIGHT;
import static util.VKUtil._CHECK_;

public class LveRenderer {
    private LveDevice lveDevice;
    private LveWindow lveWindow;
    private LveSwapChain lveSwapChain;

    private Vector<VkCommandBuffer> commandBuffers;
    boolean mustRecreate = true;
    private IntBuffer pCurrentImageIndex;
    private int currentImageIndex;
    private boolean isFrameStarted;

    VkSemaphoreCreateInfo semaphoreCreateInfo;
    LongBuffer pImageAcquiredSemaphore;
    LongBuffer pRenderCompleteSemaphore;
    IntBuffer pImageIndex;
    VkRenderPassBeginInfo renderPassBeginInfo;

    int currentFrameIndex = 0;

    VkSubmitInfo submitInfo;
    VkPresentInfoKHR presentInfo;
    IntBuffer pWaitDstStageMask;
    PointerBuffer pCommandBuffers;
    LongBuffer pSwapchains;
    /**
     * This is just -1L, but it is nicer as a symbolic constant.
     */
    private static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;

    private void onDestroy() {
        presentInfo.free();
        memFree(pWaitDstStageMask);
        submitInfo.free();
        memFree(pImageAcquiredSemaphore);
        memFree(pRenderCompleteSemaphore);
        semaphoreCreateInfo.free();
        memFree(pSwapchains);
        memFree(pCommandBuffers);
//        didn't work
//        vkFreeCommandBuffers(lveDevice.getDevice(), lveDevice.getCommandPool(), pCommandBuffers);
        commandBuffers.clear();
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

    public VkCommandBuffer beginFrame() {
        assert !isFrameStarted : "Can't call begin frame while already in progress";

        pCurrentImageIndex = memAllocInt(1);
        int result = lveSwapChain.acquireNextImage(pCurrentImageIndex);
        currentImageIndex = pCurrentImageIndex.get(0);
//        memFree(pCurrentImageIndex); //todo hz

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

    public void endFrame() {
        assert isFrameStarted : "Can't call endFrame while frame not in progress";
        VkCommandBuffer commandBuffer = getCurrentCommandBuffer();

        // Stop recording Command Buffer
        _CHECK_(vkEndCommandBuffer(commandBuffer), "Failed to stop record command buffer: ");
        PointerBuffer pCommandBuffers = memAllocPointer(1)
                .put(commandBuffer)
                .flip();

        int result = lveSwapChain.submitCommandBuffers(pCommandBuffers, pCurrentImageIndex);

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

    public void beginSwapChainRenderPass(VkCommandBuffer commandBuffer) {
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

        // Bind descriptor sets describing shader binding points
//        LongBuffer descriptorSets = memAllocLong(1).put(0, lveDevice.getDescriptorSet());
//        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, piplineConfigInfo.pipelineLayout, 0, descriptorSets, null);
//        memFree(descriptorSets);
    }

    public void endSwapChainRenderPass(VkCommandBuffer commandBuffer) {
        assert isFrameStarted : "Can't call endSwapChainRenderPass while frame not in progress";
        assert commandBuffer.equals(getCurrentCommandBuffer()) : "Can't end render pass on command buffer from a different frame";

        vkCmdEndRenderPass(commandBuffer);
    }

    public boolean isFrameInProgress() {
        return isFrameStarted;
    }

    public VkCommandBuffer getCurrentCommandBuffer() {
        assert isFrameStarted : "Cannot get command buffer when frame not in progress";
        return commandBuffers.get(currentImageIndex);
    }

    int getFrameIndex() {
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
        commandBuffers = new Vector<>(LveSwapChain.MAX_FRAMES_IN_FLIGHT);
        for (int i = 0; i< LveSwapChain.MAX_FRAMES_IN_FLIGHT; i++) {
            commandBuffers.add(null);
        }

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
//            commandBuffers.add(new VkCommandBuffer(pCommandBuffer.get(i), lveDevice.getDevice()));
        }
        memFree(pCommandBuffer);
        cmdBufAllocateInfo.free();
    }

}
