#version 450
#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_shader_storage_buffer_object : enable
#extension GL_EXT_scalar_block_layout : enable

layout (std430, binding=0) uniform Uniforms {
    mat4 mvp;
};
layout (location = 0) in vec4 pos;
layout (location = 1) in vec4 inColor;
layout (location = 0) out vec4 outColor;
void main() {
    outColor = inColor;
    gl_Position = mvp * pos;
}