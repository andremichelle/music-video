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
            //t7v13b2wyz83 (has cover)
            //6517jp6j9 (no cover)
            println("cover: ${TrackApi.fetch("6517jp6j9").track.coverUrl}")
        }
    }
}