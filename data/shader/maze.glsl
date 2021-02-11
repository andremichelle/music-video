// https://www.shadertoy.com/view/MdyfzD
#define KALEIDO 0
#define MAXSTEPS 200
#define SHADOWSTEPS 20

#define v2Resolution iResolution
#define out_color fragColor
#define time iTime

#define pi 3.141592
uniform float peak;

float rnd(float a) {
    return fract(sin(a*1328.478+3212.6534)*9863.1243986);
}

float rnd(vec2 a) {
    return fract(dot(sin(a*vec2(1328.478,4351.3254)+a.yx*vec2(421.675,435.128)+vec2(3212.6534,9673.432)),vec2(9863.1243986,8765.34214)));
}

float plane(vec3 p, vec3 n, float v) {
    return dot(p,n)-v;
}

float sph(vec3 p, float r) {
    return length(p)-r;
}

float box(vec3 p, vec3 s) {
    return length(max(abs(p)-s,0.0f))-.005;
}

float smin(float a,float b,float h) {
    float k=clamp(0.5+0.5*(a-b)/h,0.0,1.0);
    return mix(a,b,k)-k*(1.0-k)*h;
}

float smax(float a,float b,float h) {
    float k=1.0-clamp(0.5+0.5*(a-b)/h,0.0,1.0);
    return mix(a,b,k)+k*(1.0-k)*h;
}

vec3 rep(vec3 p, vec3 s) {
    return (fract(p/s+0.5)-0.5)*s;
}

vec3 repid(vec3 p, vec3 s) {
    return floor(p/s+0.5);
}

mat2 rot(float a) {
    float ca=cos(a*pi);
    float sa=sin(a*pi);
    return mat2(ca,sa,-sa,ca);
}

float block(vec3 p, float s, float s2, vec2 rr) {
    vec3 b0 = rep(p, vec3(s,1.0,s));

    vec2 id=repid(p, vec3(s,1.0,s)).xz;
    float a=rnd(id+rr);
    float b=rnd(id.yx+rr.yx);

    b0.xz*=rot(floor(a*4.0)*0.5);
    vec3 b0b=b0;
    //b0b.xz*=rot(floor(b*3.0+1.0)*0.5);
    b0b.xz*=rot(b>0.5?1.0:b>0.25?0.5:1.5); // more straight lines than turns

    float ms2=(p.y>0.25)?s2:(b>0.65?s2*2.0:s2); // may produce cross lines
    vec3 bsize=vec3(ms2,s2,0)+vec3(0.02);
    vec3 boff=vec3(ms2,0,0);

    float b1=box(b0-boff,bsize);
    float b2=box(b0b-boff,bsize);

    float final=min(b1,b2);
    return final;
}

float map(vec3 p) {

    float d=plane(p,vec3(0,1,0),-0.15);

    float s=(p.y>0.25)?1.6:0.4;
    float s2=(p.y>0.25)?0.2:0.05;
    float s3=0.02;
    vec2 off=vec2(0,0.2);

    float b0 = block(p, s,s2, vec2(0));
    float b1 = block(p+off.yxx, s,s2, vec2(7845.356,134.623));
    float b2 = block(p+off.xxy, s,s2, vec2(964.2365,123.658));
    float b3 = block(p+off.yxy, s,s2, vec2(2761.986,347.642));

    d=min(d,b0);
    d=min(d,b1);
    d=min(d,b2);
    d=min(d,b3);

    d=smin(d, plane(p,vec3(0,1,0),-0.05), 0.02);
    //d=max(d,-plane(p,vec3(0,-1,0),-1.0));

    return d;
}

vec3 norm(vec3 p) {
    vec2 off=vec2(0,0.001);
    return normalize(map(p)-vec3(map(p+off.yxx),map(p+off.xyx),map(p+off.xxy)));
}

vec3 march(vec3 ro, vec3 rd) {
    vec3 p=ro;
    for(int i=0; i<MAXSTEPS; ++i) {
        float d=map(p);
        if(d<0.001) {
            break;
        }
        p+=d*rd*0.5;
    }

    return p;
}

float shadow(vec3 ro, vec3 rd) {
    float md=1.0;
    int steps=SHADOWSTEPS;
    float s=0.2/float(steps);
    float t=0.01;
    for(int i=0; i<steps; ++i) {
        float d=map(ro+rd*t);
        md=min(md,4.0*d/t);
        if(d<0.0001) {
            //md=0.0;
            break;
        }
        t+=s;
    }

    return md;
}

float ambient(vec3 p, vec3 n) {

    float scale = 0.02;
    float d = scale;
    vec3 pos = p - n * d;

    float fac = 1.0;

    for( int i=0; i<5; ++i) {

        float str = 5.5/float(1+i);
        fac *= 1.0-clamp((d-map(pos))*str,0.0,1.0);
        pos -= n * scale;
        d += scale;

    }

    return fac;
}

vec2 mir(vec2 uv, float a) {
    mat2 ra=rot(a);
    uv*=ra;
    uv.x=abs(uv.x);
    uv*=ra;
    return uv;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = vec2(fragCoord.x / v2Resolution.x, fragCoord.y / v2Resolution.y);
    uv -= 0.5;
    uv /= vec2(v2Resolution.y / v2Resolution.x, 1);

    #if KALEIDO
    float mt=time*0.2;
    uv=mir(uv,mt);
    uv=mir(uv,-mt*0.3);
    uv=mir(uv,mt*.5);
    uv=mir(uv,-mt*.1);
    float ma=time*0.5;
    uv+=abs(vec2(cos(ma),sin(ma)))*0.5;
    #endif


    float ct=time*0.2;
    vec3 parc=vec3(0,0,ct);
    vec3 cam=vec3(cos(ct),0.2,sin(ct))*1.0+parc;
    vec3 ta=vec3(0,-0.3 + sin(ct*1.2)*0.4 + 0.3,0)+parc;
    vec3 cz=normalize(ta-cam);
    vec3 cx=normalize(cross(cz,vec3(0,1,0)));
    vec3 cy=normalize(cross(cx,cz));

    vec3 ro=cam;
    vec3 rd=normalize(uv.x*cx+uv.y*cy+cz);

    vec3 p=march(ro,rd);

    float depth=length(p-ro);
    vec3 n=norm(p);

    vec3 col=vec3(0);

    vec3 ldir=normalize(vec3(0.2,-0.7,0.4));

    float shadd=shadow(p,-ldir);

    float shad=clamp(shadd,0.0,1.0);

    float lum=max(0.0, dot(n,ldir));
    col += lum*shad*vec3(1.0,0.8,0.5)*peak;

    float ao=ambient(p, n);

    col += ao*0.6*vec3(0.4,0.5,0.7);

    //col *= min(vec3(1.0)/(depth*depth),vec3(1.0));
    //col=vec3(shad);

    //col=vec3(ao);

    col += exp(-vec3(1.9,1.5,1.2)/depth);

    out_color = vec4(col, 0);
}