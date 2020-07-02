import audio.WavStream
import org.openrndr.application
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val folder = "synth-wave"
            val wavPath: Path = Paths.get("/Users/andre.michelle/Documents/Audiotool/Mixes/$folder/mix.wav")
            val wavFile = wavPath.toFile()
            require(wavFile.exists())

            val stream =
                WavStream.forFile(File("/Users/andre.michelle/Documents/Audiotool/Mixes/cache/mixdown/78qeujew8w.wav"))
            println(stream)

            val target = FloatArray(512)
            stream.readChannelFloat(target, 0, 0)
            for (index in 0.until(16)) {
                println("$index: ${target[index]}")
            }
            stream.readChannelFloat(target, 0, 0)
            for (index in 0.until(16)) {
                println("$index: ${target[index]}")
            }



            extend {
            }
        }
    }
}