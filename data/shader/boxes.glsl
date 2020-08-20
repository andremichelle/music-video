const float EPSILON = 1e-4;
uniform float iPeak;

/* https://www.shadertoy.com/view/lt33z7 */
vec3 rayDir(in vec2 p, in vec2 s, float fov) {
    p -= s/2.;
    return normalize(vec3(p, s.y/tan(radians(fov)/2.)));
}

/* https://iquilezles.org/www/articles/distfunctions/distfunctions.htm */
float sdBox( vec3 p, vec3 b ) {
    vec3 q = abs(p) - b;
    return length(max(q,0.)) + min(max(q.x,max(q.y,q.z)),0.);
}

float sdScene(in vec3 p) {
    p = abs(p);
    p = mod(p, 2.) - 1.;
    return sdBox(p, vec3(iPeak));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 dir = rayDir(fragCoord, iResolution.xy, 60.);
    vec3 eye = vec3(0., -5.,fract(iTime/2.)*2.);

    float h = 1., d, i;
    for (i = 0.; i < 50.; ++i) {
        d = sdScene(eye + dir*h);
        if (d < EPSILON || h > 100.) break;
        h += d;
    }
    float a = 45./(i+1.)/h;
    fragColor.rgb = mix(vec3(0., .1, .2), vec3(1.), a);
    fragColor.a   = 1.;
}