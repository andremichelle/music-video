import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.shape.Rectangle
import org.openrndr.text.writer

fun main() {
    application {
        configure {
            width = 1280
            height = 720
        }
        program {
            val text = renderTarget(512, 512) {
                colorBuffer(ColorFormat.RGBa, ColorType.FLOAT32)
                depthBuffer()
            }

            drawer.isolatedWithTarget(text) {
                val font = loadFont("data/fonts/IBMPlexMono-Regular.ttf", 8.0)
                drawer.fontMap = font
                drawer.fill = ColorRGBa.WHITE
                font.contentScale
                writer {
                    box = Rectangle(0.0, 0.0, 512.0, 512.0)
                    text(
                        "Eine wunderbare Heiterkeit hat meine ganze Seele eingenommen, gleich den süßen Frühlingsmorgen, die ich mit ganzem Herzen genieße. Ich bin allein und freue mich meines Lebens in dieser Gegend, die für solche Seelen geschaffen ist wie die meine. Ich bin so glücklich, mein Bester, so ganz in dem Gefühle von ruhigem Dasein versunken, daß meine Kunst darunter leidet. Ich könnte jetzt nicht zeichnen, nicht einen Strich, und bin nie ein größerer Maler gewesen als in diesen Augenblicken. Wenn das liebe Tal um mich dampft, und die hohe Sonne an der Oberfläche der undurchdringlichen Finsternis meines Waldes ruht, und nur einzelne Strahlen sich in das innere Heiligtum stehlen, ich dann im hohen Grase am fallenden Bache liege, und näher an der Erde tausend mannigfaltige Gräschen mir merkwürdig werden; wenn ich das Wimmeln der kleinen Welt zwischen Halmen, die unzähligen, unergründlichen Gestalten der Würmchen, der Mückchen näher an meinem Herzen fühle, und fühle die Gegenwart des Allmächtigen, der uns nach seinem Bilde schuf, das Wehen des Alliebenden, der uns in ewiger Wonne schwebend trägt und erhält; mein Freund! Wenn's dann um meine Augen dämmert, und die Welt um mich her und der Himmel ganz in meiner Seele ruhn wie die Gestalt einer"
                    )
                }
            }

            extend {
                drawer.clear(ColorRGBa.TRANSPARENT)
//                drawer.drawStyle.clip = Rectangle(0.0, 0.0, 512.0, 128.0)
                drawer.image(text.colorBuffer(0), 0.0, 0.0)
            }
        }
    }
}