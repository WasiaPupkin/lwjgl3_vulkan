package t26;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.vulkan.VK12.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK12.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK12.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK12.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK12.VK_COMPARE_OP_LESS;
import static org.lwjgl.vulkan.VK12.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK12.VK_DYNAMIC_STATE_SCISSOR;
import static org.lwjgl.vulkan.VK12.VK_DYNAMIC_STATE_VIEWPORT;
import static org.lwjgl.vulkan.VK12.VK_FRONT_FACE_CLOCKWISE;
import static org.lwjgl.vulkan.VK12.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK12.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK12.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK12.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK12.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK12.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK12.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK12.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK12.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK12.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK12.vkDestroyShaderModule;
import static util.VKUtil._CHECK_;
import static util.VKUtil.glslToSpirv;

public class LvePipeLine {

    private final static int BYTES_PER_ELEMENT = 4;
    private final LveDevice lveDevice;
    long graphicsPipeline;
    long vertShaderModule;
    long fragShaderModule;

    public void onDestroy(){
        vkDestroyShaderModule(lveDevice.getDevice(), vertShaderModule, null);
        vkDestroyShaderModule(lveDevice.getDevice(), fragShaderModule, null);
        vkDestroyPipeline(lveDevice.getDevice(), graphicsPipeline, null);
    }

    public LvePipeLine(LveDevice lveDevice, String vertPath, String fragPath, PipelineConfigInfo configInfo) throws IOException {
        this.lveDevice = lveDevice;
        createGraphicsPipeline(vertPath, fragPath, configInfo);
    }

    public long createShaderModule(String classPath, VkDevice device, int stage) throws IOException {
        ByteBuffer shaderCode = glslToSpirv(classPath, stage);
        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
                .sType$Default()
                .pCode(shaderCode);
        LongBuffer pShaderModule = memAllocLong(1);
        _CHECK_(vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule), "Failed to create shader module: ");
        long shaderModule = pShaderModule.get(0);
        memFree(pShaderModule);

        return shaderModule;
    }

    public VkPipelineShaderStageCreateInfo createShaderModule(VkDevice device, String classPath, int stage) throws IOException {
            return VkPipelineShaderStageCreateInfo.create()
                    .sType$Default()
                    .stage(stage)
                    .module(createShaderModule(classPath, device, stage))
                    .pName(memUTF8("main"));
    }

    public void createGraphicsPipeline(String vertFilePath, String fragFilePath, PipelineConfigInfo configInfo) throws IOException {
        assert configInfo.pipelineLayout != VK_NULL_HANDLE : "Cannot create graphics pipline - no pipeline layout provided in configinfo";
        assert configInfo.renderPass != VK_NULL_HANDLE : "Cannot create graphics pipline - no renderPass provided in configinfo";

        // Load shaders
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2);
        var vertShader = createShaderModule(lveDevice.getDevice(), vertFilePath, VK_SHADER_STAGE_VERTEX_BIT);
        var fragShader = createShaderModule(lveDevice.getDevice(), fragFilePath, VK_SHADER_STAGE_FRAGMENT_BIT);
        vertShaderModule = vertShader.module();
        fragShaderModule = fragShader.module();
        shaderStages.get(0).set(vertShader);
        shaderStages.get(1).set(fragShader);

        var bindingDescriptions = configInfo.bindingDescriptions;
        var attributeDescriptions = configInfo.attributeDescriptions;

        VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc()
                .sType$Default()
                .pVertexBindingDescriptions(bindingDescriptions)
                .pVertexAttributeDescriptions(attributeDescriptions);

        // Assign states
        VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1)
                .sType$Default()
                .layout(configInfo.pipelineLayout) // <- the layout used for this pipeline (NEEDS TO BE SET! even though it is basically empty)
                .renderPass(configInfo.renderPass) // <- renderpass this pipeline is attached to
//                .subpass(subPass)
                .pVertexInputState(vertexInputStateCreateInfo)
                .pInputAssemblyState(configInfo.inputAssemblyInfo)
                .pRasterizationState(configInfo.rasterizationInfo)
                .pColorBlendState(configInfo.colorBlendInfo)
                .pMultisampleState(configInfo.multisampleInfo)
                .pViewportState(configInfo.viewportInfo)
                .pDepthStencilState(configInfo.depthStencilInfo)
                .pStages(shaderStages)
                .pDynamicState(configInfo.dynamicStateInfo)
                .basePipelineIndex(-1)
                .basePipelineHandle(VK_NULL_HANDLE)
                ;

        // Create rendering pipeline
        LongBuffer pPipelines = memAllocLong(1);
        _CHECK_(vkCreateGraphicsPipelines(lveDevice.getDevice(), VK_NULL_HANDLE, pipelineCreateInfo, null, pPipelines), "Failed to create pipeline: ");
        graphicsPipeline = pPipelines.get(0);
//        memFree(pPipelines);

        shaderStages.free();
        configInfo.multisampleInfo.free();
        configInfo.depthStencilInfo.free();
        configInfo.dynamicStateInfo.free();
        configInfo.viewportInfo.free();
        configInfo.colorBlendInfo.free();
        configInfo.colorBlendAttachment.free();
        configInfo.rasterizationInfo.free();
        configInfo.inputAssemblyInfo.free();
        memFree(configInfo.dynamicStateInfo.pDynamicStates());
    }

    public void bind(VkCommandBuffer renderCommandBuffer) {
        vkCmdBindPipeline(renderCommandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
    }

    public static void defaultPipelineConfigInfo(PipelineConfigInfo configInfo){
        configInfo.viewportInfo = VkPipelineViewportStateCreateInfo.calloc()
                .sType$Default()
//                .pViewports(viewport)
//                .pScissors(scissor)
                .viewportCount(1) // <- one viewport
                .scissorCount(1); // <- one scissor rectangle

        // Vertex input state
        // Describes the topoloy used with this pipeline
        configInfo.inputAssemblyInfo = VkPipelineInputAssemblyStateCreateInfo.calloc()
                .sType$Default()
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

        // Rasterization state
        configInfo.rasterizationInfo = VkPipelineRasterizationStateCreateInfo.calloc()
                .sType$Default()
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_NONE) // <- VK_CULL_MODE_BACK_BIT would work here, too!
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .lineWidth(1.0f)
                .depthClampEnable(false)
                .depthBiasEnable(false)
        ;

        // Multi sampling state
        // No multi sampling used in this example
        configInfo.multisampleInfo = VkPipelineMultisampleStateCreateInfo.calloc()
                .sType$Default()
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
//                .sampleShadingEnable(false)
//                .minSampleShading(1.0f)
        ;

        // Color blend state
        // Describes blend modes and color masks
        configInfo.colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1)
//                .colorWriteMask(0xF); // <- RGBA
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
//                .blendEnable(false)
        ;
        configInfo.colorBlendInfo = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType$Default()
//                .logicOpEnable(true)
//                .logicOp(VK_LOGIC_OP_COPY)
                .pAttachments(configInfo.colorBlendAttachment)
        ;

        // Depth and stencil state
        // Describes depth and stenctil test and compare ops
        configInfo.depthStencilInfo = VkPipelineDepthStencilStateCreateInfo.calloc()
                // No depth test/write and no stencil used
                .sType$Default()
                .depthTestEnable(true)
                .depthWriteEnable(true)
//                .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL);
                .depthCompareOp(VK_COMPARE_OP_LESS);

        // Enable dynamic states
        // Describes the dynamic states to be used with this pipeline
        // Dynamic states can be set even after the pipeline has been created
        // So there is no need to create new pipelines just for changing
        // a viewport's dimensions or a scissor box
        IntBuffer pDynamicStates = memAllocInt(2);
        pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip();
        configInfo.dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc()
                // The dynamic state properties themselves are stored in the command buffer
                .sType$Default()
                .pDynamicStates(pDynamicStates);
//        }

        configInfo.bindingDescriptions = LveModel.Vertex.getBindingDescriptions();
        configInfo.attributeDescriptions = LveModel.Vertex.getAttributeDescriptions();
    }

    public static class PipelineConfigInfo {
        public VkVertexInputBindingDescription.Buffer bindingDescriptions = null;
        public VkVertexInputAttributeDescription.Buffer attributeDescriptions = null;
//        VkVertexInputAttributeDescription.Buffer attributeDescriptions;

        VkPipelineViewportStateCreateInfo viewportInfo;
        VkPipelineInputAssemblyStateCreateInfo inputAssemblyInfo;
        VkPipelineRasterizationStateCreateInfo rasterizationInfo;
        VkPipelineMultisampleStateCreateInfo multisampleInfo;
        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment;
        VkPipelineColorBlendStateCreateInfo colorBlendInfo;
        VkPipelineDepthStencilStateCreateInfo depthStencilInfo;
        //    VkDynamicState.Buffer dynamicStateEnables;
        VkPipelineDynamicStateCreateInfo dynamicStateInfo;
        public long pipelineLayout = 0;
        public long renderPass = 0;
//    int subpass = 0;
    }
}
