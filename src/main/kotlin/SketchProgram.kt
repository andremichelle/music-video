import net.Playlist
import org.openrndr.application

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            println(Playlist.fetch("synth-wave"))
            extend {
            }
        }
    }
}