package draw

import org.openrndr.draw.ShadeStyle
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter

@Description("Conic")
class Conic(a0: Double = 0.0, a1: Double = 360.0, frac: Double = 1.0) : ShadeStyle() {
    @DoubleParameter("start angle", 0.0, 360.0, order = 0)
    var a0: Double by Parameter()

    @DoubleParameter("end angle", 0.0, 360.0, order = 1)
    var a1: Double by Parameter()

    @DoubleParameter("frac", 0.0, 1.0, order = 2)
    var frac: Double by Parameter()

    init {
        this.a0 = a0
        this.a1 = a1
        this.frac = frac

        fragmentTransform = """
                float PI = 3.1415926536;
                float TAU = 2.0 * PI;
                float a0 = radians(p_a0);
                float a1 = radians(p_a1);
                float cr = cos(a0);
                float sr = sin(a0);
                float ad = mod(a1 - a0, TAU);
                mat2 rm = mat2(cr, -sr, sr, cr);
                vec2 rp = rm * va_position;
                float ac = atan(-rp.y, -rp.x) + PI;
                ad += max(0.0, sign(-ad)) * TAU;
                float f = ac / ad;  
                vec3 a = vec3(1.0, 0.0, 0.0);
                vec3 b = vec3(0.0, 1.0, 0.0);
                float alpha = fract(f+0.02) / p_frac;
                x_fill = vec4(x_fill.rgb, alpha * x_fill.a);
                """
    }
}

fun conic(a0: Double = 0.0, a1: Double = 360.0, frac: Double = 1.0): ShadeStyle {
    return Conic(a0, a1, frac)
}