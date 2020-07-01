import audio.secondsToBars
import draw.ShaderToy

class Scene(val trackKey: String, val shadertoy: ShaderToy, val seed: Int, val backgroundAlpha: Double) {
    companion object {
        val list = listOf(
            Scene(
                "you_won_t_understand",
                ShaderToy.fromFile("data/shader/shiny-spheres.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x306709, 0.6
            ),
            Scene(
                "ztdqgahfsdhdzwroap5c2zvkosfzyem",
                ShaderToy.fromFile("data/shader/clouds.fs") {
                    timing = { seconds, _ -> seconds * 0.5 }
                }, 0x6709, 0.1
            ),
            Scene(
                "iwd52a2x",
                ShaderToy.fromFile("data/shader/artifact-at-sea.fs") {
                    timing = { seconds, bpm -> secondsToBars(seconds, bpm) * 2.0 }
                }, 0x30679, 0.2
            )
        )
    }
}