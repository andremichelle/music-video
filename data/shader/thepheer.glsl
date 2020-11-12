// https://www.shadertoy.com/view/tdycRh
uniform float lPeak;
uniform float rPeak;

// https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
float sdPlane(vec3 p, vec3 n, float h) {
    return dot(p, n) + h;
}

// https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
float sdSphere(vec3 p, float s) {
    return length(p) - s;
}

// https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
float sdBox(vec3 p, vec3 b) {
    vec3 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, max(q.y, q.z)), 0.0);
}

// https://iquilezles.org/www/articles/distfunctions/distfunctions.htm
float sdTorus(vec3 p, vec2 t) {
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q) - t.y;
}

// https://iquilezles.org/www/articles/smin/smin.htm
float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0);
    return min(a, b) - 0.25*h*h/k;
}

// https://iquilezles.org/www/articles/smin/smin.htm
float smax(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0);
    return max(a, b) + 0.25*h*h/k;
}

// https://gamedev.stackexchange.com/a/148088
vec3 srgb(vec3 rgb) {
    bvec3 t = lessThan(rgb, vec3(0.0031308));
    vec3 a = 1.055*pow(rgb, vec3(1.0/2.4)) - 0.055;
    vec3 b = 12.92*rgb;
    return mix(a, b, t);
}

float prod3(vec3 v) {
    return v.x*v.y*v.z;
}

mat3 rot(vec3 axis, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    vec3 a = axis*(1.0 - c);
    vec4 b = vec4(axis*s, c);
    vec2 n = vec2(-1.0, 1.0);
    return mat3(
    axis.xyx*a.xxz + b.wzy*n.yxy,
    axis.yyz*a.xyy + b.zwx*n.yyx,
    axis.xzz*a.zyz + b.yxw*n.xyy
    );
}
// light, material, raymarching result
struct L { vec3 c; vec3 p; };
struct M { vec3 c; float r; float d; };
struct R { M m; vec3 p; float t; };

// M mmin(M a, M b) { if (a.d < b.d) return a; return b; }
// M mmax(M a, M b) { if (a.d > b.d) return a; return b; }
M msmin(M a, M b, float k) {
    float h = clamp(0.5 + 0.5*(a.d - b.d)/k, 0.0, 1.0);
    return M(mix(a.c, b.c, h), mix(a.r, b.r, h), mix(a.d, b.d, h) - k*h*(1.0 - h));
}

M map(vec3 p) {
    const vec2 n = vec2(0.0, 1.0);
    mat3 r = rot(n.yxx, 0.3*iTime) * rot(n.xyx, 0.5*iTime) * rot(n.xxy, 0.7*iTime);
    vec3 t = vec3(vec2(1.0, 1.5)*sin(vec2(0.6, 0.3)*iTime), 0.0);
    return msmin(
    M(vec3(1.0), 1.0, -sdBox(p, vec3(3.0, 1.5, 3.0))),
    msmin(
    M(vec3(1.0), 0.02, sdSphere(p + t, 1.0) + 0.001*prod3(sin(60.0*p))),
    M(vec3(1.0), 0.98, sdBox(r*(p - t), vec3(0.75))),
    1.0
    ),
    0.5
    );
}

// https://iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
vec3 normal(vec3 p) {
    const vec3 k = vec3(1.0, -1.0, 1e-4);
    return normalize(
    k.xyy*map(p + k.xyy*k.z).d +
    k.yyx*map(p + k.yyx*k.z).d +
    k.yxy*map(p + k.yxy*k.z).d +
    k.xxx*map(p + k.xxx*k.z).d
    );
}

bool raymarch(vec3 rc, vec3 ro, vec3 rd, out R rr) {
    // rayconfig rc: threshold, near, far
    for (rr.t = rc.y; rr.t < rc.z; rr.t += rr.m.d) {
        rr.p = ro + rr.t*rd;
        rr.m = map(rr.p);
        if (rr.m.d < rc.x) return true;
    }
    return false;
}

vec3 color(vec3 p, vec3 n) {
    const L lights[] = L[] (
    L(vec3(3.0, 2.0, 1.0), vec3(-2.5, 1.0, 2.5)),
    L(vec3(1.0, 2.0, 3.0), vec3(+2.5, 1.0, 2.5))
    );
    float peaks[] = float[](lPeak, rPeak);
    vec3 c = vec3(0.0); // 0.002 * vec3(0.1, 2.0, 4.0);
    for (int i = 0; i < 2; i++) {
        vec3 lp = lights[i].p - p;
        float ll = length(lp);
        vec3 ld = lp/ll;
        float la = max(0.0, dot(ld, n)/exp(ll));
        c += la*lights[i].c * peaks[i];
    }
    return c;
}

vec3 render(vec2 uv) {
    vec3 rc = vec3(1e-3, 1e-2, 1e+1);
    vec3 rt = vec3(2.8*uv,  0.0);
    vec3 ro = vec3(1.4*uv, -2.0);
    vec3 rd = normalize(rt - ro);
    R rr;

    vec3 c = vec3(0.0);
    for (float r = 1.0; r > 1e-4 && raymarch(rc, ro, rd, rr);) {
        // if (rr.m.d < 0.0) { c.r += r; break; }
        vec3 n = normal(rr.p);
        float a = rr.m.r*r;
        c += a*rr.m.c*color(rr.p, n);
        r -= a;
        ro = rr.p;
        rd = reflect(rd, n);
    }
    return c;
}

void mainImage(out vec4 rgba, vec2 xy) {
    vec2 r = vec2(iResolution.x/iResolution.y, 1.0);
    vec2 uv = 2.0*xy/iResolution.xy - 1.0;
    rgba = vec4(srgb(render(r*uv)), 1.0);
}