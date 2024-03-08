package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL15C.glBindBuffer
import org.lwjgl.opengl.GL15C.glGenBuffers
import org.lwjgl.opengl.GL30C.glBindBufferBase
import org.lwjgl.opengl.GL30C.glGetProgrami
import org.lwjgl.opengl.GL42C.GL_ATOMIC_COUNTER_BUFFER
import org.lwjgl.opengl.GL43C.*
import org.lwjgl.opengl.GL45C
import org.openrndr.draw.*
import org.openrndr.internal.Driver

private val logger = KotlinLogging.logger {}

interface ShaderBufferBindingsGL3 : ShaderBufferBindings, ShaderUniformsGL3 {
    val ssbo: Int

    val ssboResourceIndices: MutableMap<String, Int>

    fun resourceIndex(name: String): Int = ssboResourceIndices.getOrPut(name) {
        val resourceIndex = GL45C.glGetProgramResourceIndex(programObject, GL_SHADER_STORAGE_BLOCK, name)
        if (resourceIndex == -1) {
            logger.warn {
                "no resource index for buffer '${name}'"
            }
        }
        resourceIndex
    }

    fun createSSBO(): Int {
        return if ((Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_3 && (Driver.instance as DriverGL3).version.type == DriverTypeGL.GL) {
            glGenBuffers()
        } else {
            -1
        }
    }

    override fun buffer(name: String, vertexBuffer: VertexBuffer) {
        require(ssbo != -1)
        val resourceIndex = resourceIndex(name)

        if (resourceIndex != -1) {
            val result = IntArray(1)
            glGetProgramResourceiv(
                programObject,
                GL_SHADER_STORAGE_BLOCK,
                resourceIndex,
                intArrayOf(GL_BUFFER_BINDING),
                intArrayOf(1),
                result
            )
            val bindingIndex = result[0]
            vertexBuffer as VertexBufferGL3
            if (bindingIndex != -1) {
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingIndex, vertexBuffer.buffer)
            } else {
                error("no binding index for '${name}'")
            }
        }
    }

    override fun buffer(name: String, shaderStorageBuffer: ShaderStorageBuffer) {
        require(ssbo != -1)
        val resourceIndex = resourceIndex(name)

        if (resourceIndex != -1) {
            val result = IntArray(1)
            glGetProgramResourceiv(
                programObject,
                GL_SHADER_STORAGE_BLOCK,
                resourceIndex,
                intArrayOf(GL_BUFFER_BINDING),
                intArrayOf(1),
                result
            )
            val bindingIndex = result[0]
            shaderStorageBuffer as ShaderStorageBufferGL43
            if (bindingIndex != -1) {
                glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, bindingIndex, shaderStorageBuffer.buffer)
            } else {
                logger.warn { "no binding index for '${name}'" }
            }
        }
    }

    override fun buffer(name: String, counterBuffer: AtomicCounterBuffer) {
        require(ssbo != -1)
        if (name !in this.uniforms.keys) {
            val uniformCount = glGetProgrami(programObject, GL_ACTIVE_UNIFORMS)

            val index = (0 until uniformCount).indexOfFirst {
                val uniformName = glGetActiveUniformName(programObject, it)
                uniformName == name
            }
            uniforms[name] = index
        }


        val uniformIndex = uniformIndex(name)

        if (uniformIndex != -1) {
            val uniformIndices = intArrayOf(uniformIndex)
            val result = IntArray(1)
            glGetActiveUniformsiv(
                programObject, uniformIndices,
                GL_UNIFORM_ATOMIC_COUNTER_BUFFER_INDEX, result
            )
            val bufferIndex = result[0]
            if (bufferIndex != -1) {
                val bindingIndex = glGetActiveAtomicCounterBufferi(
                    programObject, bufferIndex,
                    GL_ATOMIC_COUNTER_BUFFER_BINDING
                )
                if (bindingIndex != -1) {
                    glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo)
                    counterBuffer as AtomicCounterBufferGL42
                    glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, bindingIndex, counterBuffer.buffer)
                } else {
                    logger.warn { "no binding point for '$name'" }
                }
            } else {
                logger.warn { "no atomic counter buffer for '$name'" }
            }
        } else {
            logger.warn { "no uniform for '$name'" }
        }
    }
}