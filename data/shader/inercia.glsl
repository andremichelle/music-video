
// Shader coded live during Inercia Shader Royale 2020 Competion: 1st place
// Shader was made in about 50m, with 7 other coders competing at the same time
//
// Shader evolved a lot during the show
// You can activate various parts here:

float globalSpeed = 0.5; // was at 1 with lug00ber set!
#define USE_RAINBOW 0
#define USE_SYMMETRY 0
#define USE_POSTFX 1
#define USE_LINES 0


float time;

vec3 repeat(vec3 p, vec3 s) {
    return (fract(p/s+.5)-.5)*s;
}
vec3 repid(vec3 p, vec3 s) {
    return floor(p/s+.5);
}

mat2 rot(float a) {
    float ca=cos(a);
    float sa=sin(a);
    return mat2(ca,sa,-sa,ca);
}

vec3 rnd(vec3 p) {

    return fract(sin(p*452.512+p.yzx*847.512+p.zxy*245.577)*512.844);
}

float tick(float t, float d) {
    t/=d;
    return (floor(t) + pow(smoothstep(0.,1.,fract(t)), 10.))*d;
}

float rnd(float t) {
    return fract(sin(t*457.588)*942.512);
}

float curve(float t, float d) {
    t/=d;
    return mix(rnd(floor(t)), rnd(floor(t)+1.), pow(smoothstep(0.,1.,fract(t)), 10.));
}

vec3 aco=vec3(0);
float map(vec3 p) {

    vec3 bp=p;

    float t3 = time*0.4;
    p.xz *= rot(t3 + sin(p.y*0.3+t3*.7)*.5);
    p.xy *= rot(t3 + sin(p.z*.4+t3*.6)*.5);
    p.yz *= rot(t3 + sin(p.x*.2+t3*.8)*.5);

    float d=length(p)-3.;

    float sd=3./length(p);
    d = min(d, length(p.xz)-sd);
    d = min(d, length(p.xy)-sd);
    d = min(d, length(p.yz)-sd);

    float an = pow(fract(time*0.5),2.);
    float ep = 0.2;
    ep -= an*0.1 + d*0.05;
    float d3 = abs(length(p)-an*20.-3.)-ep;
    d3=max(d3, 0.1);
    aco += vec3(0.8,0.5,1) * 0.004 / (0.05 + abs(d3));

    d = min(d, d3);

    vec3 p6 = bp;
    p6.xz=abs(p6.xz);
    if(p6.z>p6.x) p6.xz = p6.zx;
    p6.x -= 22.;
    p6.z = abs(p6.z)-6.-sin(bp.y*.03 + time)*5.-sin(bp.y*.1 + time)*10.;
    p6.z = abs(p6.z)-3.-sin(bp.y*.07 + time)*5.-sin(bp.y*.2 + time*1.3)*4.;

    float d7 = length(p6.xz) - 1.;
    d7=max(d7,0.2);
    d = min(d, d7);
    aco += vec3(0.8,0.5,1) * 0.008*exp(fract(-time*2.)) / (0.05 + abs(d7));

    d *= 0.7;

    p.x += tick(time, 1.)*4.;

    if(d<0.1) {
        for(float i=1.; i<11.; ++i) {

            p-= 0.8;
            float ss = 9./i;
            vec3 id = repid(p, vec3(ss));
            vec3 p2 = repeat(p, vec3(ss));
            p2 += rnd(id)*0.2;

            float d2 = length(p2)-ss*0.3;
            d=max(d, -d2);

            p.xz *= rot(0.7);
            p.yz *= rot(0.6);
        }

        vec3 p4 = repeat(p + time*4., vec3(8));
        float d4 = max(abs(p4.x)-0.1, d);
        aco += vec3(0.4,0.5,1.9) * 0.026 / (0.15 + abs(d4));
        float d5 = max(abs(p4.y)-0.1, d);
        aco += vec3(0.8,0.5,0.4) * 0.016 / (0.15 + abs(d5));
        d = min(d, d4);
        d = min(d, d5);
    }

    return d;
}

float gao(vec3 p, vec3 n, float s) {
    return clamp(map(p+n*s)/s, 0.,1.);
}

float pulse;

void cam(inout vec3 p) {
    float t=time * 0.3 + curve(pulse, 0.6)*27.3;
    p.yz *= rot(t*0.7);
    p.xz *= rot(t);
}


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    time = mod(iTime*globalSpeed, 300.);
    pulse = floor(time*0.5);

    vec2 uv = vec2(fragCoord.x / iResolution.x, fragCoord.y / iResolution.y);
    uv -= 0.5;
    uv /= vec2(iResolution.y / iResolution.x, 1);

    float alpha = 1.0;

    #if USE_POSTFX
    float se = curve(time - length(uv)*0.5, 0.15);
    if(se>0.6&&iTime>18.0) {
        float gd = 8.+curve(time, 0.1)*64.;
        vec2 tmp = smoothstep(0.8,0.9,fract(uv*gd));
        uv=floor(uv*gd)/gd;
        alpha = mix(1.,max(tmp.x,tmp.y)*.5+.5,se);
    }
        #endif

        #if USE_SYMMETRY
    uv.y += pow(curve(time, 0.3),2.0)*0.2;

    uv *= 1.0+curve(time - length(uv), 0.3)*.3;

    if(curve(time, 2.0)>0.6) uv=abs(uv);
    #endif


    vec3 s=vec3(0,0,-6. - curve(pulse, 0.5)*10.);
    s.x += (curve(pulse, 0.4)-.5)*8.;
    s.y += (curve(pulse, 0.7)-.5)*8.;
    vec3 r=normalize(vec3(-uv, 1. + sin(time*curve(pulse, 0.3)*2.0)*.5));

    cam(s);
    cam(r);

    vec3 p=s;
    for(int i=0; i<100; ++i) {
        float d=map(p);
        if(d<0.0001) break;
        if(d>200.0) break;
        p+=r*d;
    }

    float fog=1.-clamp(length(p-s)/100., 0.,1.);

    vec3 col=vec3(0);

    col += aco*.9;

    vec2 off=vec2(0.01,0);
    vec3 n=normalize(map(p)-vec3(map(p-off.xyy), map(p-off.yxy), map(p-off.yyx)));

    float ao = gao(p,n,0.3);
    ao *= gao(p,n,0.6)*.5+.5;
    ao *= gao(p,n,1.2)*.5+.5;

    for(float i=1.; i<30.; ++i) {
        float dd=0.1*i;
        col += map(p+r*dd) * fog * 0.06 * vec3(dd+0.3,0.6,0.9+dd*.3) * ao;
    }

    float fre=pow(1.-abs(dot(n,r)), 3.);
    col += fre * vec3(1,.5,.7) * 1.4 * (0.5-0.5*n.y) * ao * fog;

    col += (1.-fog) * mix(vec3(0), vec3(0.5,0.6,0.7), pow(abs(r.x), 4.)) * 2.;
    col += (1.-fog) * mix(vec3(0), vec3(0.7,0.6,0.3), pow(abs(r.z), 4.)) * 2.;

    #if USE_RAINBOW
    float t5 = time*.3;
    col.xz *= rot(t5);
    col.xy *= rot(t5*.7);
    col=abs(col);
    #endif

    col *= alpha;

    vec3 bcol = col;

    #if USE_LINES
    if(abs(length(uv)-0.3-curve(floor(time*5.)*.4, 0.4))<0.05) col = 1.-col;
    if(abs(length(uv)-0.4-curve(floor(time*5.)*.4, 0.5))<0.05) col = 1.-col;

    uv *= 0.4;
    for(int i=0; i<3; ++i) {

        uv *= rot(0.7+time*0.2);
        uv = abs(uv)-curve(time, 0.3)*0.3;
        uv *= 1.2;

        if(abs(uv.y)<0.003) col = 1.-col;
    }
        #endif

    col *= 1.2-length(uv);
    fragColor = vec4(col, 1);
}