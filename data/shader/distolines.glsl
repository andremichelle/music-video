// ----- Ray marching options ----- //
#define AA_SAMPLES 2 // antialisaing
// #define LOW_QUALITY // if your computer isn't powerful enough
#ifdef LOW_QUALITY
#define MAX_STEPS 25
#else
#define MAX_STEPS 200
#endif
#define MAX_DIST 50.
#define SURF_DIST 0.0001
#define NORMAL_DELTA 0.0001

#define FBM_MAX_ITER 10

// ----- easingFunct -----//
float quadin(float t) { return t*t;}
float quadOut(float t) { return -t*(t-2.0);}
float cubicIn(float t) { return t*t*t;}
float cubicOut(float t) { return -t*t*t+1.0;}
float circleOut(float t) { return pow(1.0-(1.0-t)*(1.0-t), 0.5); }
float circleIn(float t) { return 1.0- pow(1.0-t*t, 0.5); }
float gauss(float t, float s) { return exp(-(t*t)/(2.*s*s)); }
    // sub interpolation used in smoothstep
    #define hermiteInter(t) t * t * (3.0 - 2.0 * t)

    // ----- UsefulConstants ----- //
    #define PI  3.14159265358979323846264338327

    // ----- Useful functions ----- //
    #define rot2(a) mat2(cos(a), -sin(a), sin(a), cos(a))
float maxComp(vec2 v) { return max(v.x , v.y); }
float maxComp(vec3 v) { return max(max(v.x , v.y), v.z); }
float cro(vec2 a,vec2 b) { return a.x*b.y - a.y*b.x; }
float map(float a, float b, float t) {return a + t * (b - a); } // considering that t is in [0-1]
float mult(vec2 v) { return v.x*v.y; }
float mult(vec3 v) { return v.x*v.y*v.z; }
float sum(vec2 v) { return v.x+v.y; }
float sum(vec3 v) { return v.x+v.y+v.z; }
    #define saturate(v) clamp(v, 0., 1.)

// ----- Noise stuff ----- //
// Based on Morgan McGuire and David Hoskins
// https://www.shadertoy.com/view/4dS3Wd
// https://www.shadertoy.com/view/4djSRW

float hash1(float p) {
    p = fract(p * .1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash1(vec2 p) {
    vec3 p3  = fract(vec3(p.xyx) * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash1(vec3 p3) {
    p3  = fract(p3 * .1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash2(float p) {
    vec3 p3 = fract(vec3(p) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec2 hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec2 hash2(vec3 p3) {
    p3 = fract(p3 * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xx+p3.yz)*p3.zy);
}

vec3 hash3(float p) {
    vec3 p3 = fract(vec3(p) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yzx+33.33);
    return fract((p3.xxy+p3.yzz)*p3.zyx);
}

vec3 hash3(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz+33.33);
    return fract((p3.xxy+p3.yzz)*p3.zyx);
}

vec3 hash3(vec3 p3) {
    p3 = fract(p3 * vec3(.1031, .1030, .0973));
    p3 += dot(p3, p3.yxz+33.33);
    return fract((p3.xxy + p3.yxx)*p3.zyx);
}

vec4 hash4(float p) {
    vec4 p4 = fract(vec4(p) * vec4(.1031, .1030, .0973, .1099));
    p4 += dot(p4, p4.wzxy+33.33);
    return fract((p4.xxyz+p4.yzzw)*p4.zywx);
}

vec4 hash4(vec2 p) {
    vec4 p4 = fract(vec4(p.xyxy) * vec4(.1031, .1030, .0973, .1099));
    p4 += dot(p4, p4.wzxy+33.33);
    return fract((p4.xxyz+p4.yzzw)*p4.zywx);

}

vec4 hash4(vec3 p) {
    vec4 p4 = fract(vec4(p.xyzx)  * vec4(.1031, .1030, .0973, .1099));
    p4 += dot(p4, p4.wzxy+33.33);
    return fract((p4.xxyz+p4.yzzw)*p4.zywx);
}

vec4 hash4(vec4 p4) {
    p4 = fract(p4  * vec4(.1031, .1030, .0973, .1099));
    p4 += dot(p4, p4.wzxy+33.33);
    return fract((p4.xxyz+p4.yzzw)*p4.zywx);
}

float perlinNoise(float x) {
    float id = floor(x);
    float f = fract(x);
    float u = f;
    return mix(hash1(id), hash1(id + 1.0), u);
}

float perlinNoise(vec2 x) {
    vec2 id = floor(x);
    vec2 f = fract(x);

    float a = hash1(id);
    float b = hash1(id + vec2(1.0, 0.0));
    float c = hash1(id + vec2(0.0, 1.0));
    float d = hash1(id + vec2(1.0, 1.0));
    // Same code, with the clamps in smoothstep and common subexpressions
    // optimized away.
    vec2 u = hermiteInter(f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float perlinNoise(vec3 x) {
    const vec3 step = vec3(110., 241., 171.);

    vec3 id = floor(x);
    vec3 f = fract(x);

    // For performance, compute the base input to a 1D hash from the integer part of the argument and the 
    // incremental change to the 1D based on the 3D -> 1D wrapping
    float n = dot(id, step);

    vec3 u = hermiteInter(f);
    return mix(mix(mix( hash1(n + dot(step, vec3(0, 0, 0))), hash1(n + dot(step, vec3(1, 0, 0))), u.x),
    mix( hash1(n + dot(step, vec3(0, 1, 0))), hash1(n + dot(step, vec3(1, 1, 0))), u.x), u.y),
    mix(mix( hash1(n + dot(step, vec3(0, 0, 1))), hash1(n + dot(step, vec3(1, 0, 1))), u.x),
    mix( hash1(n + dot(step, vec3(0, 1, 1))), hash1(n + dot(step, vec3(1, 1, 1))), u.x), u.y), u.z);
}

float fbm (vec2 x, float H, int octaves) {
    float G = exp2(-H);
    float v = 0.;
    float f = 1.;
    float amp = 1.;
    float aSum = 1.;

    vec2 shift = vec2(100.);
    for ( int i=0; i < FBM_MAX_ITER; ++i) {
        if( i >= octaves) break;
        v += amp * perlinNoise(f*x);
        f *= 2.;
        amp *= G;
        aSum += amp;
        // Rotate and shift to reduce axial bias
        x = rot2(0.5) * x + shift;
    }
    return v / aSum;
}

float fbm (vec3 x, float H, int octaves) {
    float G = exp2(-H);
    float v = 0.;
    float f = 1.;
    float amp = 1.;
    float aSum = 1.;

    for ( int i=0; i < FBM_MAX_ITER; ++i) {
        if( i >= octaves) break;
        v += amp * perlinNoise(f*x);
        f *= 2.;
        amp *= G;
        aSum += amp;
    }
    return v / aSum;
}

float distanceField(vec3 p) {
    float final = perlinNoise(p*0.06125) -0.5;
    float other = perlinNoise(p*0.06125 + 1234.567) - 0.5;
    final = 1./(abs(final*final*other));
    return final*0.0001;
}

vec3 computeCamDir(vec2 uv, vec3 camPos, vec3 camUp, vec3 lookAtPos) {
    vec3 camVec = normalize(lookAtPos - camPos);
    vec3 sideNorm = normalize(cross(camUp, camVec));
    vec3 upNorm = cross(camVec, sideNorm);
    vec3 worldFacing = (camPos + camVec);
    vec3 worldPix = worldFacing + uv.x * sideNorm + uv.y * upNorm;
    return normalize(worldPix - camPos);
}

// return dist, marchingCount, maxDist
vec3 rayMarching(vec3 O, vec3 D, int steps, inout float density) { // ray origin and dir
    float t = 0.0;
    float marchingCount = 0.0;
    float maxD = 1.e-10;
    for(int i = 0; i < steps; i++) {
        if( i > MAX_STEPS) break;
        vec3 newPos = O + D * t;
        float d = distanceField(newPos);

        // custom incrementation here for this sketch
        t += 3.; // precision handling
        density += d*0.01;
        ++marchingCount;
        maxD = max(maxD, d);

        // If we are very close to the object, consider it as a hit and exit this loop
        if( t > MAX_DIST || abs(d) < SURF_DIST*0.99) break;
    }
    return vec3(t, marchingCount, maxD);
}

float densityMarching(vec3 O, vec3 D) {
    float density = 0.;
    // ray marching
    int steps = 12;
    float t = 0.0;
    for(int i = 0; i < steps; i++) {
        if( i > MAX_STEPS) break;
        vec3 newPos = O + D * t;
        float d = distanceField(newPos + 12.);

        // custom incrementation here for this sketch
        t += 3.; // precision handling
        density += d*0.01;
        // If we are very close to the object, consider it as a hit and exit this loop
        if( t > MAX_DIST || abs(d) < SURF_DIST*0.99) break;
    }
    return density;
}

vec3 render(vec3 O, vec3 D) {
    vec3 cyan = vec3(25., 80., 122.)/255.;
    vec3 blue = vec3(21., 71., 199.)/255.;
    vec3 orange = vec3(255., 184., 113.)/255.;

    float density =  densityMarching(O, D);

    vec3 col = blue*0.1;
    col += pow(density, 0.45)*orange*0.5;
    col += gauss(density*2., 0.1) * cyan * 0.2;
    return saturate(col);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {

    vec2 uv = (fragCoord - .5*iResolution.xy)/iResolution.y;
    uv *= 2.; // zoom
    float delay = 12.; // loop delay in s
    float a = 2.*PI*iTime/delay; // angle
    float r = 3.; // radius
    float y = sin(a + 12.)*0.5;
    vec3 O = vec3(r*cos(a), y, r*sin(a)); // origin

    vec3 finalColor = vec3(0.);
    #if AA_SAMPLES > 1
    for (float i = 0.0; i < float(AA_SAMPLES); i++) {
        for (float j = 0.0; j < float(AA_SAMPLES); j++) {
            vec2 deltaUV = (vec2(i, j) / float(AA_SAMPLES) *2.0 - 1.0) / iResolution.y;
            uv += deltaUV;
            #endif
            vec3 D = computeCamDir(uv, O, vec3(0.,1.,0.), vec3(0.)); // dir
            finalColor += render(O, D);
            #if AA_SAMPLES > 1
        }
    }
    finalColor /= float(AA_SAMPLES * AA_SAMPLES); // Average samples
    #endif

    // color grading
    finalColor *= vec3(1.0 ,0.9, 0.98) *1.2;

    fragColor = vec4(finalColor,1.);
}