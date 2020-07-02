import audio.WavStream
import org.openrndr.application
import java.nio.file.Paths

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            test("synth-wave")
            test("tech-house")

            extend {
            }
        }
    }
}

private fun test(folder: String) {
    val wavFile = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/$folder/mix.wav").toFile()
    require(wavFile.exists())
    println(WavStream.forFile(wavFile))
}