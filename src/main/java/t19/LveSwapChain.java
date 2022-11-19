package t19;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Vector;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK12.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK12.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK12.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK12.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK12.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK12.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK12.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_D32_SFLOAT_S8_UINT;
import static org.lwjgl.vulkan.VK12.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK12.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK12.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK12.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK12.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK12.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
import static org.lwjgl.vulkan.VK12.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK12.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK12.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK12.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK12.vkCreateFence;
import static org.lwjgl.vulkan.VK12.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK12.vkCreateImageView;
import static org.lwjgl.vulkan.VK12.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK12.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK12.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK12.vkQueueSubmit;
import static org.lwjgl.vulkan.VK12.vkResetFences;
import static org.lwjgl.vulkan.VK12.vkWaitForFences;
import static util.VKUtil.UINT64_MAX;
import static util.VKUtil._CHECK_;

public class LveSwapChain {
    static int MAX_FRAMES_IN_FLIGHT = 2;

    private int swapChainImageFormat;
    private int swapChainDepthFormat;
    private VkExtent2D swapChainExtent; //swapChainWidth\swapChainHeight
    private Vector<Long> swapChainFramebuffers = new Vector<>(); //LvePipline#createFramebuffers
    private long renderPass;
    private Vector<Long> depthImages = new Vector<>(); //no?
    private Vector<Long> depthImageMemorys = new Vector<>(); //no?
    private Vector<Long> depthImageViews = new Vector<>(); //no?
    private LongBuffer pSwapchainImages;
    private Vector<Long> swapChainImageViews = new Vector<>(); //imageViews
    private final LveDevice lveDevice;
    private final VkExtent2D windowExtent;
    LongBuffer pSwapChain;
    private long swapChain;
    private LveSwapChain oldSwapChainObj;
    LongBuffer pImageAcquiredSemaphore;
    LongBuffer pRenderCompleteSemaphore;
    private Vector<Long> imageAvailableSemaphores = new Vector<>(); //pImageAcquiredSemaphore
    private Vector<Long> renderFinishedSemaphores = new Vector<>(); //pRenderCompleteSemaphore;
    private LongBuffer pInFlightFences;
    private Vector<Long> inFlightFences = new Vector<>();
//    private LongBuffer imagesInFlight;
    private Vector<Long> imagesInFlight = new Vector<>();
    private int currentFrame = 0;

    public LveSwapChain getOldSwapChain() {return oldSwapChainObj; }

//    // SwapChainExtent may be large then pixel window size on HD displays
//    int swapChainWidth;
//    int swapChainHeight;
//
//    long[] images;
//    long[] imageViews;
//    private final LveWindow lveWindow;
//
//    private final PiplineConfigInfo piplineConfigInfo;
//
//    private final LveDevice.ColorAndDepthFormatAndSpace formatAndSpace;
//    private VkCommandBuffer[] renderCommandBuffers;
//
    public int extentAspectRatio() {
        assert swapChainExtent.height() != 0 : "swapChainExtent.height() == 0";
        return swapChainExtent.width() / swapChainExtent.height();
    }

    public long getRenderPass() {
        return renderPass;
    }
//    public VkCommandBuffer[] getRenderCommandBuffers(){return renderCommandBuffers;}
//    public void setRenderCommandBuffers(VkCommandBuffer[] renderCommandBuffers) {this.renderCommandBuffers = renderCommandBuffers;}

    public LveSwapChain(LveDevice lveDevice, VkExtent2D windowExtent) {
        this.lveDevice = lveDevice;
        this.windowExtent = windowExtent;

        init();
    }

    public LveSwapChain(LveDevice lveDevice, VkExtent2D windowExtent, LveSwapChain previous) {
        this.lveDevice = lveDevice;
        this.windowExtent = windowExtent;
        this.oldSwapChainObj = previous;

        init();
        oldSwapChainObj = null;
    }

    public void init() {
        createSwapChain();
        createImageViews();
        createRenderPass();
        createDepthResources();
        createFramebuffers();
        createSyncObjects();
    }

    int acquireNextImage(IntBuffer pImageIndex) {
        int result = vkWaitForFences(lveDevice.getDevice(), inFlightFences.get(currentFrame), true, UINT64_MAX);
        _CHECK_(result, "Cannot wait next image");

        result = vkAcquireNextImageKHR(lveDevice.getDevice(), swapChain, UINT64_MAX, imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);
//        int currentImageIndex = pImageIndex.get(0); //fixme what is this?
        return result;
    }

        int submitCommandBuffers(PointerBuffer pCommandBuffers, IntBuffer pImageIndex) {
        if (imagesInFlight.get(pImageIndex.get(0)) != null) {
            vkWaitForFences(lveDevice.getDevice(), imagesInFlight.get(pImageIndex.get(0)), true, UINT64_MAX);
        }
        imagesInFlight.add(pImageIndex.get(0), inFlightFences.get(currentFrame));

        // Info struct to submit a command buffer which will wait on the semaphore
        IntBuffer pWaitDstStageMask = memAllocInt(1);
        pWaitDstStageMask.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

        LongBuffer pWaitSemaphore = memAllocLong(1);
        pWaitSemaphore.put(0, imageAvailableSemaphores.get(currentFrame));
        pWaitSemaphore.flip();

        LongBuffer pSignalSemaphore = memAllocLong(1);
        pSignalSemaphore.put(0, renderFinishedSemaphores.get(currentFrame));
        pSignalSemaphore.flip();

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
                .sType$Default()
                .waitSemaphoreCount(1)
                .pWaitSemaphores(pWaitSemaphore)
                .pWaitDstStageMask(pWaitDstStageMask)
                .pCommandBuffers(pCommandBuffers)
                .pSignalSemaphores(pSignalSemaphore);

        vkResetFences(lveDevice.getDevice(), inFlightFences.get(currentFrame));

        _CHECK_(vkQueueSubmit(lveDevice.getGraphicsQueue(), submitInfo, inFlightFences.get(currentFrame)), "Can't submit graphics queue");

        LongBuffer pSwapchain = memAllocLong(1);
        pSwapchain.put(0, swapChain);
        pSwapchain.flip();

        // Info struct to present the current swapchain image to the display
        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc()
                .sType$Default()
                .pWaitSemaphores(pSignalSemaphore)
                .swapchainCount(1)
                .pSwapchains(pSwapchain)
                .pImageIndices(pImageIndex);

        int result = vkQueuePresentKHR(lveDevice.getPresentQueue(), presentInfo);
        _CHECK_(result, "Cannot wait next image");

        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        return result;
    }

    public void createSwapChain() {
        LveDevice.SwapChainSupportDetails swapChainSupport = lveDevice.getSwapChainSupport();

        VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
        int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
        VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

        int imageCount = swapChainSupport.capabilities.minImageCount() + 1;
        if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount > swapChainSupport.capabilities.maxImageCount()) {
            imageCount = swapChainSupport.capabilities.maxImageCount();
        }

        int preTransform;
        if ((swapChainSupport.capabilities.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
            preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
        } else {
            preTransform = swapChainSupport.capabilities.currentTransform();
        }
//        todo?
//        swapChainSupport.capabilities.free();

        VkSwapchainCreateInfoKHR swapchainCI = VkSwapchainCreateInfoKHR.calloc()
                .sType$Default()
                .surface(lveDevice.getWindowSurface())
                .minImageCount(imageCount)
                .imageFormat(surfaceFormat.format())
                .imageColorSpace(surfaceFormat.colorSpace())
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(preTransform)
                .imageArrayLayers(1)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .presentMode(presentMode)
                .clipped(true)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .imageExtent(extent);

        LveDevice.QueueFamilyIndices indices = lveDevice.findPhysicalQueueFamilies();
        IntBuffer queueFamilyIndices = memAllocInt(2); //todo not freed
        queueFamilyIndices.put(indices.graphicsFamily).put(indices.presentFamily).flip();

        if (indices.graphicsFamily != indices.presentFamily) {
            swapchainCI.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
            .pQueueFamilyIndices(queueFamilyIndices);
        } else {
            swapchainCI.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .pQueueFamilyIndices(null);  // Optional
        }

        LongBuffer pSwapChain = memAllocLong(1);
        _CHECK_(vkCreateSwapchainKHR(lveDevice.getDevice(), swapchainCI, null, pSwapChain), "Failed to create swap chain: ");
        swapchainCI.free();
        swapChain = pSwapChain.get(0);
        memFree(pSwapChain);

        // If we just re-created an existing swapchain, we should destroy the old swapchain at this point.
        // Note: destroying the swapchain also cleans up all its associated presentable images once the platform is done with them.
//        if (oldSwapChain != VK_NULL_HANDLE) {
//            vkDestroySwapchainKHR(lveDevice.getDevice(), oldSwapChain, null);
//        }

        IntBuffer pImageCount = memAllocInt(1);
        _CHECK_(vkGetSwapchainImagesKHR(lveDevice.getDevice(), swapChain, pImageCount, null), "Failed to get number of swapchain images: ");
        imageCount = pImageCount.get(0);

        pSwapchainImages = memAllocLong(imageCount);
        _CHECK_(vkGetSwapchainImagesKHR(lveDevice.getDevice(), swapChain, pImageCount, pSwapchainImages), "Failed to get swapchain images: ");
        memFree(pImageCount);

        swapChainImageFormat = surfaceFormat.format();
        swapChainExtent = extent;
    }

    private void createImageViews() {
        for (int i = 0; i < pSwapchainImages.remaining(); i++) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc()
                    .sType$Default()
                    .image(pSwapchainImages.get(i))
                    .format(swapChainImageFormat)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .levelCount(1)
                    .layerCount(1)
                    .baseMipLevel(0)
                    .baseArrayLayer(0);

            LongBuffer pBufferView = memAllocLong(1);
            _CHECK_(vkCreateImageView(lveDevice.getDevice(), viewInfo, null, pBufferView), "Failed to create image view: ");
            swapChainImageViews.add(pBufferView.get(0));

            memFree(pBufferView);
        }
//        todo?
//        memFree(pSwapchainImages);
    }

    public void createRenderPass() {
        VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2);
        attachments.get(0) // <- color attachment
                .format(getSwapChainImageFormat())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        attachments.get(1) // <- depth-stencil attachment
                .format(findDepthFormat())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkAttachmentReference depthReference = VkAttachmentReference.calloc()
                .attachment(1)
                .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
//                .colorAttachmentCount(colorReference.remaining())
                .colorAttachmentCount(1)
                .pColorAttachments(colorReference) // <- only color attachment
                .pDepthStencilAttachment(depthReference) // <- and depth-stencil
                ;

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .srcAccessMask(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .dstSubpass(0)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
//                .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)
                ;

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc()
                .sType$Default()
                .pAttachments(attachments)
                .pSubpasses(subpass)
                .pDependencies(dependency)
                ;

        LongBuffer pRenderPass = memAllocLong(1);
        _CHECK_(vkCreateRenderPass(lveDevice.getDevice(), renderPassInfo, null, pRenderPass), "Failed to create clear render pass: ");
        renderPass = pRenderPass.get(0);

        // free memory
        memFree(pRenderPass);
        dependency.free();
        renderPassInfo.free();
        depthReference.free();
        colorReference.free();
        subpass.free();
        attachments.free();
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream().filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB
                && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR).findFirst().orElse(availableFormats.get(0));
    }

    private int chooseSwapPresentMode(IntBuffer pPresentModes) {
        // Try to use mailbox mode. Low latency and non-tearing
        int swapchainPresentMode = VK_PRESENT_MODE_FIFO_KHR;
        for (int i = 0; i < pPresentModes.remaining(); i++) {
            if (pPresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                swapchainPresentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                break;
            }
            if ((swapchainPresentMode != VK_PRESENT_MODE_MAILBOX_KHR) && (pPresentModes.get(i) == VK_PRESENT_MODE_IMMEDIATE_KHR)) {
                swapchainPresentMode = VK_PRESENT_MODE_IMMEDIATE_KHR;
            }
        }
//        System.out.println("SwapChain Present mode: " + swapchainPresentMode);
        memFree(pPresentModes);
        return swapchainPresentMode;
    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        VkExtent2D swapchainExtent = windowExtent;
        // width and height are either both 0xFFFFFFFF, or both not 0xFFFFFFFF.
        if (capabilities.currentExtent().width() == 0xFFFFFFFF) {
            if (swapchainExtent.width() < capabilities.minImageExtent().width()) {
                swapchainExtent.width(capabilities.minImageExtent().width());
            } else if (swapchainExtent.width() > capabilities.maxImageExtent().width()) {
                swapchainExtent.width(capabilities.maxImageExtent().width());
            }

            if (swapchainExtent.height() < capabilities.minImageExtent().height()) {
                swapchainExtent.height(capabilities.minImageExtent().height());
            } else if (swapchainExtent.height() > capabilities.maxImageExtent().height()) {
                swapchainExtent.height(capabilities.maxImageExtent().height());
            }
        } else {
            // If the surface size is defined, the swap chain size must match
            swapchainExtent.set(capabilities.currentExtent());
        }
        return swapchainExtent;
    }

    public int findDepthFormat() {
        return lveDevice.findSupportedFormat(
                new int[]{VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT},
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }

    int getSwapChainImageFormat() { return swapChainImageFormat; }
    public VkExtent2D getSwapChainExtent() { return swapChainExtent; }
    int imageCount() { return pSwapchainImages.remaining(); }

    private void createDepthResources() {
        int depthFormat = findDepthFormat();
        swapChainDepthFormat = depthFormat;
        VkExtent2D swapChainExtent = getSwapChainExtent();

        for (int i = 0; i < imageCount(); i++) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc()
                    .sType$Default()
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent(it -> it
                            .width(swapChainExtent.width())
                            .height(swapChainExtent.height())
                            .depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(depthFormat)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .flags(0);

            long depthImage = lveDevice.createImageWithInfo(imageInfo, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT); //todo need test not sure here

            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc()
                    .sType$Default()
                    .format(depthFormat)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .image(depthImage)
                    .subresourceRange(it -> it
                            .aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1)
                    );

            LongBuffer pBufferView = memAllocLong(1);
            _CHECK_(vkCreateImageView(lveDevice.getDevice(), viewInfo, null, pBufferView), "Failed to create image view: ");
            depthImageViews.add(pBufferView.get(0));

            memFree(pBufferView);
        }
    }

    public void createFramebuffers() {
        swapChainFramebuffers.clear();

        for (int i = 0; i < imageCount(); i++) {
            LongBuffer attachments = memAllocLong(2);
            attachments.put(swapChainImageViews.get(i)).put(depthImageViews.get(i)).flip();

            VkFramebufferCreateInfo fci = VkFramebufferCreateInfo.calloc()
                    .sType$Default()
                    .pAttachments(attachments)
                    .height(swapChainExtent.height())
                    .width(swapChainExtent.width())
                    .renderPass(renderPass)
                    .layers(1);

            // Create a framebuffer for each swapchain image
            LongBuffer pFramebuffer = memAllocLong(1);
            _CHECK_(vkCreateFramebuffer(lveDevice.getDevice(), fci, null, pFramebuffer), "Failed to create framebuffer: ");
            swapChainFramebuffers.add(pFramebuffer.get(0));

            memFree(pFramebuffer);
            fci.free();
            memFree(attachments);
        }

    }

    private void createSyncObjects() {
        imageAvailableSemaphores.clear();
        renderFinishedSemaphores.clear();
        inFlightFences.clear();
        imagesInFlight.clear();
        imagesInFlight.setSize(MAX_FRAMES_IN_FLIGHT);

        // Info struct to create a semaphore
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.calloc()
                .sType$Default()
                .flags(VK_FENCE_CREATE_SIGNALED_BIT);

        pImageAcquiredSemaphore = memAllocLong(1);
        pRenderCompleteSemaphore = memAllocLong(1);
        LongBuffer pFences = memAllocLong(1);

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

            String errMessage = "failed to create synchronization objects for a frame!";
            _CHECK_(vkCreateSemaphore(lveDevice.getDevice(), semaphoreCreateInfo, null, pImageAcquiredSemaphore), errMessage);
            imageAvailableSemaphores.add(pImageAcquiredSemaphore.get(0));
            _CHECK_(vkCreateSemaphore(lveDevice.getDevice(), semaphoreCreateInfo, null, pRenderCompleteSemaphore), errMessage);
            renderFinishedSemaphores.add(pRenderCompleteSemaphore.get(0));
            _CHECK_(vkCreateFence(lveDevice.getDevice(), fenceCreateInfo, null, pFences), errMessage);
            inFlightFences.add(pFences.get(0));
        }
//        todo where should free?
        memFree(pImageAcquiredSemaphore);
        memFree(pRenderCompleteSemaphore);
        memFree(pFences);
    }

    boolean compareSwapFormats(LveSwapChain swapChain) {
        return swapChain.swapChainDepthFormat == swapChainDepthFormat && swapChain.swapChainImageFormat == swapChainImageFormat;
    }

    public long getFrameBuffer(int index) {
        return swapChainFramebuffers.get(index);
    }

    public void destroySemaphores() {
        // cleanup synchronization objects
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

//            LongBuffer pWaitSemaphore = memAllocLong(1);
//            pWaitSemaphore.put(0, imageAvailableSemaphores.get(currentFrame));
//            pWaitSemaphore.flip();

            vkDestroySemaphore(lveDevice.getDevice(), imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(lveDevice.getDevice(), renderFinishedSemaphores.get(i), null);
        }
    }
}
