package draw

import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.io.File

class ShaderToy(fsCode: String) {
    var uniforms: (Shader) -> Unit = { Unit }
    var timing: (Double, Double) -> Double = { seconds, bpm -> seconds }

    companion object {
        fun fromFile(pathname: String, configure: ShaderToy.() -> Unit): ShaderToy {
            val shaderToy = ShaderToy(File(pathname).readText(Charsets.UTF_8))
            shaderToy.configure()
            return shaderToy
        }

        @Suppress("unused")
        fun exampleCode(): ShaderToy {
            return ShaderToy(
                """
                void mainImage( out vec4 fragColor, in vec2 fragCoord )
                {
                    // Normalized pixel coordinates (from 0 to 1)
                    vec2 uv = fragCoord/iResolution.xy;
                
                    // Time varying pixel color
                    vec3 col = 0.5 + 0.5*cos(iTime+uv.xyx+vec3(0,2,4));
                
                    // Output to screen
                    fragColor = vec4(col,1.0);
                }
                """
            )
        }

        private val vertexBuffer: VertexBuffer = init()

        private const val vsCode = """
                #version 330                
                in vec3 a_position;
                void main() {
                    gl_Position = vec4(a_position, 1.0);                
                }
                """

        private const val pre_fsCode = """
            #version 330
            in vec4 gl_FragCoord;
            out vec4 o_output;
            uniform float iTime;
            uniform vec2 iResolution;
        """

        private const val after_fsCode = """
            void main() {
                mainImage(o_output, gl_FragCoord.xy);
            }
        """

        private fun init(): VertexBuffer {
            val geometry: VertexBuffer = vertexBuffer(vertexFormat {
                position(3)
            }, 6)
            geometry.put {
                write(
                    Vector3(-1.0, 1.0, 0.0),
                    Vector3(1.0, 1.0, 0.0),
                    Vector3(1.0, -1.0, 0.0),
                    Vector3(-1.0, 1.0, 0.0),
                    Vector3(1.0, -1.0, 0.0),
                    Vector3(-1.0, -1.0, 0.0)
                )
            }
            return geometry
        }
    }

    private val shader: Shader

    init {
        shader = Shader.createFromCode(vsCode, pre_fsCode + fsCode + after_fsCode, name = "shadertoy")
    }

    fun render(size: Vector2, seconds: Double, bpm: Double) {
        shader.begin()
        shader.uniform("iTime", timing(seconds, bpm).toFloat())
        shader.uniform("iResolution", size)
        uniforms(shader)
        Driver.instance.drawVertexBuffer(
            shader, listOf(vertexBuffer),
            DrawPrimitive.TRIANGLES, 0, 6
        )
        shader.end()
    }
}