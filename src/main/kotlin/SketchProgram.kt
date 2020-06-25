import net.TrackApi
import org.openrndr.application

// https://github.com/openrndr/orx

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            println("bpm: ${TrackApi.fetch("6517jp6j9").track.bpm}")
        }
    }
}