/**
    https://www.shadertoy.com/view/tlVBWz
*/

#define R			iResolution
#define T			iTime

#define MINDIST     .001
#define MAXDIST     150.

#define PI          3.14159265358

#define r2(a) mat2(cos(a),sin(a),-sin(a),cos(a))
#define hash(a, b) fract(sin(a*1.2664745 + b*.9560333 + 3.) * 14958.5453)

float time;
uniform float peak;

float sampleFreq(float freq) {
    return peak;
}

//@iq of hsv2rgb - updated
vec3 hsv2rgb( float h ) {
    vec3 c = vec3(h,1.,.5);
    vec3 rgb = clamp( abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),6.0)-3.0)-1.0, 0.0, 1.0 );
    return c.z * mix( vec3(1.0), rgb, c.y);
}

vec3 getMouse(vec3 ro) {
    return ro;
}
//http://mercury.sexy/hg_sdf/
float vmax(vec2 v) {	return max(v.x, v.y);						}
float vmax(vec3 v) {	return max(max(v.x, v.y), v.z);				}
float sgn(float x) { 	return (x<0.)?-1.:1.;							}
vec2 sgn(vec2 v)   {	return vec2((v.x<0.)?-1.:1., (v.y<0.)?-1.:1.);	}
float pMod(inout float p, float size) {
    float halfsize = size*0.5,
    c = floor((p + halfsize)/size);
    p = mod(p + halfsize, size) - halfsize;
    return c;
}
vec3 pMod(inout vec3 p, vec3 size) {
    vec3 c = floor((p + size*0.5)/size);
    p = mod(p + size*0.5, size) - size*0.5;
    return c;
}
float pModPolar(inout vec2 p, float repetitions) {
    float angle = 2.*PI/repetitions;
    float a = atan(p.y, p.x) + angle/2.,
    r = length(p),
    c = floor(a/angle);
    a = mod(a,angle) - angle/2.;
    p = vec2(cos(a), sin(a))*r;
    if (abs(c) >= (repetitions/2.)) c = abs(c);
    return c;
}
float fBox(vec3 p, vec3 b) {
    vec3 d = abs(p) - b;
    return length(max(d, vec3(0))) + vmax(min(d, vec3(0)));
}
float fBox2(vec2 p, vec2 b) {
    vec2 d = abs(p) - b;
    return length(max(d, vec2(0))) + vmax(min(d, vec2(0)));
}
//@iq
float sdCap( vec3 p, float h, float r ){
    p.y -= clamp( p.y, 0.0, h );
    return length( p ) - r;
}

vec2 path(in float z){
    vec2 p1 =vec2(2.38*sin(z * .15)+1.38*cos(z * .25), 3.4*cos(z * .0945));
    vec2 p2 =vec2(3.2*sin(z * .179), 4.31*sin(z * .127));
    return (p1 - p2)*.3;
}
// globals and stuff
float glow,iqd,flight,beams,travelSpeed,gcolor,objglow,offWobble,boxsize;
float ga,sa,tm,slp;
vec3 g_hp,s_hp;
mat2 tax,tay,r90,r4,r5;

const vec3 cxz = vec3(3.15,4.75,3.);
const float scale = 3.0;

vec2 fragtail(vec3 pos, float ld) {
    float ss=.85;
    float r = length(pos);

    for (int i = 0;i<2;i++) {
        pos=abs(pos);
        if ( pos.x- pos.y<0.) pos.yx = pos.xy;
        if ( pos.x- pos.z<0.) pos.zx = pos.xz;
        if ( pos.y- pos.z<0.) pos.zy = pos.yz;

        pos.x=scale * pos.x-cxz.x*(scale-.85);
        pos.y=scale * pos.y-cxz.y*(scale-1.);
        pos.z=scale * pos.z;

        if (pos.z>0.5*cxz.z*(scale-1.)) pos.z-=cxz.z*(scale-1.);

        r = fBox2(pos.xy,vec2(scale))-.025;
        ss*=1./scale;
    }
    float rl = log2(ss*.025);
    return vec2(r*ss,1.);
}

//@blackle domain rep https://www.shadertoy.com/view/Wl3fD2
vec2 edge(vec2 p) {
    vec2 p2 = abs(p);
    if (p2.x > p2.y) return vec2((p.x < 0.) ? -1. : 1., 0.);
    else             return vec2(0., (p.y < 0.) ? -1. : 1.);
}

// scene map
vec2 map (in vec3 pos, float sg) {
    vec3 p = pos-vec3(0.,0.,0);

    vec2 res = vec2(100.,-1.);
    float msize = 6.325;

    // set path(s) vector(s)
    vec2 tun = p.xy - path(p.z);
    vec3 q = vec3(tun,p.z);
    vec3 o = vec3(tun+vec2(0.,.0),p.z+travelSpeed+offWobble+4.25);
    vec3 s = q;
    vec3 ss=s;
    // mods and vectors
    float pid = floor((q.z+(msize/2.))/msize);
    slp = mod(pid,16.);
    float sz = slp<12. ? slp<6. ? 4. : 6. : 10.;
    float zz = sz*.5;
    pModPolar(q.xy,sz);
    pModPolar(s.xy,zz);

    vec3 r =s;
    vec3 fs=s-vec3(2.85,0,0);
    r = vec3(abs(r.x),abs(r.y),r.z);

    fs.z*=2.;
    vec2 center = floor(fs.xz) + .5;
    vec2 neighbour = center + edge(fs.xz - center);

    float chs = floor(center.y);
    float bmod = mod(chs,18.);

    float height = (sampleFreq(bmod*.0465));
    height=smoothstep(.001,1.,height)*1.45;

    ga=height;
    float ids = pMod(s.z,msize);
    float qid = pMod(q.z,msize);
    float ld = mod(ids,6.);
    float lq = mod(ids,2.);

    pMod(q.x,msize);
    pMod(q.y,msize);

    iqd=qid;
    o.zx*=r5;
    o.yz*=r4;

    o = abs(o)-(offWobble*.5);
    float obj = fBox(o,vec3(.025+boxsize))-.015;
    if(obj<res.x ) {
        res = vec2(obj,11.);
        g_hp=o;
    }

    //float zprs= mod(chs, tm <12.? tm <8.? tm <4.? 2.: floor(height*1. ) : 5.: floor(height*1.2));
    float zprs= mod(chs, tm <8.? tm <4.? tm <4.? 2.: 2.: 5.: floor(height*1.45));

    vec2 bspace = vec2(2.16,1.75);
    float d4a = length(r.xy-bspace)-.1;
    float d4 =  length(r.xy-bspace)-.04+.027+.027*sin(r.z-time*4.5);
    if(d4<res.x ) {
        res = vec2(d4,12.);
        g_hp=r;
    }

    // fractal
    vec2 d1 = fragtail(q,ld);
    d1.x = min(length(abs(s.xy)-vec2(4.25,2.))-.35,d1.x);
    d1.x = max(d1.x,-d4a);

    s.z=abs(s.z);
    float blt = sdCap(s-vec3(2.45,-.58,2.725),1.16 ,.015);
    if(lq<1.) d1.x = min(blt,d1.x);
    if(d1.x<res.x) {
        res = d1.xy;
        g_hp = q;
    }

    float me =   fBox(fs-vec3(0,0,center.y),   vec3(.02,.25, .15));
    float next = fBox(fs-vec3(0,0,neighbour.y),vec3(.02,.0001+height, .15));
    float dlt = min(me, next);
    if(dlt<res.x) {
        float mid= zprs<4.? zprs<3.? zprs<2.? 3. : 4. : 4.  : 3.;
        res = vec2(dlt,tm <8. ? mid : 4.);
        g_hp = fs;
    }

    //sg prevents glow from changing for ao
    if(sg==1.)beams += .00005/(.000003+d4*d4);
    if(sg==1.&&lq<1.)flight += .00025/(.0000001+blt*blt);
    if(sg==1.&&zprs<.1)glow += .00015/(.000002+dlt*dlt);
    if(sg==1.)objglow += .0005/(.2+obj*obj);
    return res;
}

vec2 marcher(vec3 ro, vec3 rd, float sg, int maxstep){
    float d =  .0,
    m = -1.;
    float glowDist = 1e9;
    int i = 0;
    for(i=0;i<maxstep;i++){
        vec3 p = ro + rd * d;
        vec2 t = map(p, sg);
        if(t.x<d*MINDIST)break;
        d += i<150 ? i<75 ? t.x*.35 : t.x*.75 : t.x;
        m  = t.y;
        if(d>MAXDIST)break;
    }
    return vec2(d,m);
}

// Tetrahedron technique @iq
// https://www.iquilezles.org/www/articles/normalsSDF/normalsSDF.htm
/** saved original
vec3 getNormal(vec3 p, float t){
    float e = t*MINDIST;
    vec2 h = vec2(1.,-1.)*.5773;
    return normalize( h.xyy*map( p + h.xyy*e, 0. ).x +
                      h.yyx*map( p + h.yyx*e, 0. ).x +
                      h.yxy*map( p + h.yxy*e, 0. ).x +
                      h.xxx*map( p + h.xxx*e, 0. ).x );
}
*/
// @Shane Tetrahedral normal function.
vec3 getNormal(in vec3 p, float t) {
    // This mess is an attempt to improve compiler time by contriving a break... It's
    // based on a suggestion by IQ. I think it works, but I really couldn't say for sure.
    // It's definitely one of those "use at your own risk" situations.
    const vec2 h = vec2(1.,-1.)*.5773;
    vec3 n = vec3(0);
    vec3[4] e4 = vec3[4](h.xyy, h.yyx, h.yxy, h.xxx);

    for(int i = min(0, iFrame); i<4; i++){
        n += e4[i]*map(p + e4[i]*t*MINDIST, 0.).x;
        if(n.x>1e8) break; // Fake non-existing conditional break.
    }

    return normalize(n);
}
//camera setup
vec3 camera(vec3 lp, vec3 ro, vec2 uv) {
    vec3 f=normalize(lp-ro),//camera forward
    r=normalize(cross(vec3(0,1,0),f)),//camera right
    u=normalize(cross(f,r)),//camera up
    c=ro+f*.35,//zoom
    i=c+uv.x*r+uv.y*u,//screen coords
    rd=i-ro;//ray direction
    return rd;
}

float getStripes(vec2 uv){
    uv*=65.;
    float sd = mod(floor(uv.x * 2.5), 3.);
    return (sd<1.) ? 1. : 0.;
}


vec3 getColor(float m) {
    vec3 h = vec3(.03);
    // basic materials
    if(m==1.) h = vec3(.0001);
    if(m==3.) h = mix( hsv2rgb(gcolor*.0115) ,vec3(.01),getStripes(s_hp.yz*.167));
    if(m==4.) h = hsv2rgb((gcolor*.0115)+.28);
    return h;
}

void mainImage( out vec4 O, in vec2 F ) {
    time = iTime;//+ texture(iChannel1,F/8.).r * iTimeDelta;
    tm = mod(time*.3, 12.);

    travelSpeed = (time * 4.15);
    offWobble = 1.+.5*sin(tm)+sampleFreq(.75);
    boxsize = smoothstep(.001,1.,sampleFreq(slp<6. ? .75 : .15))*.7;
    r4 =r2(time);
    r5 =r2(time);

    // pixel screen coordinates
    vec2 uv = (F.xy - R.xy*0.5)/max(R.x,R.y);
    vec3 C = vec3(0.);//default color
    vec3 FC = vec3(.125);

    vec3 lp = vec3(0.,0.,0.-travelSpeed);
    vec3 ro = vec3(0.,.01,.15);
    ro = getMouse(ro);
    ro +=lp;

    lp.xy += path(lp.z);
    ro.xy += path(ro.z);
    // solve for Ray direction
    vec3 rd = camera(lp,ro,uv);
    vec2 t = marcher(ro,rd,1.,200);
    //save id val before reflect
    gcolor = iqd;
    s_hp = g_hp;
    vec3 hf =  hsv2rgb(gcolor*.0115);
    vec3 hz =  hsv2rgb((gcolor*.0125)+.5);

    float d = t.x,
    m = t.y;

    // if visible
    if(d<MAXDIST){
        // step next point
        vec3 p = ro + rd * d;

        vec3 h = getColor(m);
        vec3 n = getNormal(p,d);

        vec3 lpos  = vec3(.0,.0,-.3)+lp;
        lpos.xy = path(lpos.z);
        vec3 l = normalize(lpos-p);
        float dif = clamp(dot(n,l),0.,1.);

        C += dif*h;

        if(m==11.){
            vec3 rr = reflect(rd,n);
            vec2 y = marcher(p+.0001,rr,1.,200);
            if(y.x<MAXDIST){
                p+=y.x*rr;
                h = getColor(y.y);
                n = getNormal(p,d);
                l = normalize(lpos-p);
                dif = clamp(dot(n,l),0.,1.);

                C += dif*h;
            }

        }
    } else {
        C = FC;
    }


    C = mix(FC,C,  exp(-.00000425*t.x*t.x*t.x));
    C += abs(glow*.7)*hf;
    C += abs(flight*.75);
    C += abs(beams*.65)*hz;
    C += abs(objglow*.65)*hf;
    C = clamp(C,0.,1.);
    O = vec4(C,1.0);
}
