package t27.systems;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import t27.GlobalUbo;
import t27.LveDevice;
import t27.LveFrameInfo;
import t27.LvePipeLine;
import t27.PointLight;
import util.VKUtil;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK12.vkCreatePipelineLayout;
import static t25.FirstApp.MAX_LIGHTS;
import static util.VKUtil._CHECK_;

public class PointLightSystem {

    public static class PointLightPushConstants {
        public Vector4f position = new Vector4f();
        public Vector4f color = new Vector4f();
        float radius;
    }

    public static String vertFilePath = "vulkan/shaders/t27/point_light.vert";
    public static String fragFilePath = "vulkan/shaders/t27/point_light.frag";
    private final LveDevice lveDevice;
    private LvePipeLine lvePipeLine;
    private long pipelineLayout;

    public PointLightSystem(LveDevice lveDevice, long renderPass, long globalSetLayout) {
        this.lveDevice = lveDevice;

        createPipelineLayout(globalSetLayout);
        try {
            createPipeline(renderPass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    fixme
    public void onDestroy() {
//        vkDestroyPipeline(lveDevice.getDevice(), lvePipeLine.graphicsPipeline, null);
        vkDestroyPipelineLayout(lveDevice.getDevice(), pipelineLayout, null);
        lvePipeLine.onDestroy();
    }

    public synchronized void render(LveFrameInfo frameInfo) {
        // sort lights
        Map<Float, Integer> sorted = new TreeMap<>(Comparator.reverseOrder());
        for(var obj : frameInfo.gameObjects().values()) {
            if (obj.pointLight == null) continue;

            // calculate distance
            var offset = frameInfo.camera().getPosition().sub(obj.transform.translation, new Vector3f());
            float disSquared = new Vector3f().distanceSquared(offset);

            sorted.put(disSquared, obj.getId());
        }
        // bind pipline
        lvePipeLine.bind(frameInfo.commandBuffer());

//        fixme try to reuse memory
        LongBuffer globalDescriptorSet = memAllocLong(1).put(0, frameInfo.globalDescriptorSet());
        vkCmdBindDescriptorSets(frameInfo.commandBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, globalDescriptorSet, null);
        memFree(globalDescriptorSet);

        // iterate through sorted lights in reverse order
        for (var obj : sorted.values()) {
            // use game obj id to find light object
            var gameObj = frameInfo.gameObjects().get(obj);

            PointLightPushConstants push = new PointLightPushConstants();
            push.position = new Vector4f(gameObj.transform.translation, 1.f);
            push.color = new Vector4f(gameObj.color, gameObj.pointLight.lightIntensity);
            push.radius = gameObj.transform.scale.x;

            vkCmdPushConstants(frameInfo.commandBuffer(), pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT, 0, pushDataToFloatArr(push));
            vkCmdDraw(frameInfo.commandBuffer(), 6, 1, 0, 0);
        }
    }

    private float[] pushDataToFloatArr(PointLightPushConstants push) {
        return new float[] {
                push.position.x(), push.position.y(), push.position.z(), push.position.w(),
                push.color.x(), push.color.y(), push.color.z(), push.color.w(),
                push.radius
        };
    }

    public void update(LveFrameInfo frameInfo, GlobalUbo ubo) {
        var rotateLight = new Matrix4f().rotate(0.5f * frameInfo.frameTime(), new Vector3f(0.f, -1.f, 0.f));
        int lightIndex = 0;
        for (var obj : frameInfo.gameObjects().values()) {
            if (obj.pointLight == null) continue;

            assert lightIndex < MAX_LIGHTS : "Point lights exceed maximum specified";

            // update light position
//            obj.transform.translation = glm::vec3(rotateLight * glm::vec4(obj.transform.translation, 1.f));
            obj.transform.translation = rotateLight.transformProject(new Vector4f(obj.transform.translation, 1.f), new Vector3f());

            PointLight pointLight = new PointLight();
            pointLight.position = new Vector4f(obj.transform.translation, 1.f);
            pointLight.color = new Vector4f(obj.color, obj.pointLight.lightIntensity);

            // copy light to ubo
            ubo.pointLights[lightIndex] = pointLight;

            lightIndex += 1;
        }
//        ubo.numLights = lightIndex;
        ubo.numLights = new Vector4f(lightIndex);
    }

    /**
     * Create the pipeline layout that is used to generate the rendering pipelines that
     * are based on this descriptor set layout
     */
    private void createPipelineLayout(long globalSetLayout) {

        VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(VKUtil.sizeof(new Vector4f()) + VKUtil.sizeof(new Vector4f()) + Float.BYTES)
                ;

        LongBuffer pDescriptorSetLayout = memAllocLong(1).put(0, globalSetLayout);
        VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType$Default()
                .pSetLayouts(pDescriptorSetLayout)
                .pPushConstantRanges(pushConstantRange)
                ;

        LongBuffer pPipelineLayout = memAllocLong(1);
        _CHECK_(vkCreatePipelineLayout(lveDevice.getDevice(), pipelineLayoutCreateInfo, null, pPipelineLayout), "Failed to create pipeline layout: ");

        pipelineLayout = pPipelineLayout.get(0);

        memFree(pPipelineLayout);
        memFree(pDescriptorSetLayout);
        pipelineLayoutCreateInfo.free();
//        pushConstantRange.free();
    }

    void createPipeline(long renderPass) throws IOException {
        assert renderPass != 0;
        assert pipelineLayout != 0;

//        update pipeline config
        LvePipeLine.PipelineConfigInfo pipelineConfig = new LvePipeLine.PipelineConfigInfo();
        LvePipeLine.defaultPipelineConfigInfo(pipelineConfig);
        LvePipeLine.enableAlphaBlending(pipelineConfig);
        pipelineConfig.attributeDescriptions.clear();
        pipelineConfig.bindingDescriptions.clear();
        pipelineConfig.renderPass = renderPass;
        pipelineConfig.pipelineLayout = pipelineLayout;

        lvePipeLine = new LvePipeLine(lveDevice, vertFilePath, fragFilePath, pipelineConfig);

    }
}
