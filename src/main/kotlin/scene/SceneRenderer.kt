package scene

import org.openrndr.Program

abstract class SceneRenderer {
    abstract fun render(program: Program, seconds: Double)
}