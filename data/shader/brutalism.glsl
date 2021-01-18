// https://www.shadertoy.com/view/wt33RN

// "Refracted brutalism" - Result of a 2.5h improv live coding session on Twitch

// MAJOR THANKX go to: LJ, Crundle and Alkama for the hive mind and
// help with schlick-fresneled refracted reflections and size coded shadows

// LIVE SHADER CODING, SHADER SHOWDOWN STYLE, EVERY TUESDAYS 21:00 Uk time:
// https://www.twitch.tv/evvvvil_

// "How you expect to run with wolves come night, when you spend all day sparring with puppies." Omar - The Wire

vec2 z, v, e=vec2(0.01, 0);float t, tt;vec3 np, bp, no, po, ld, al;//global variables
float bo(vec3 p, vec3 r){ p=abs(p)-r;return max(max(p.x, p.y), p.z); }//box primitive
mat2 r2(float r){ return mat2(cos(r), sin(r), -sin(r), cos(r)); }//2d rotate function
vec2 fb(vec3 p)// base geometry piece function
{
    vec2 h, t=vec2(bo(p, vec3(2, 5, 2)), 5);//basic raymarching modelling with primitives and material id
    t.x=max(t.x, -bo(p, vec3(1.5, 15, 1.5)));//removing from blue box
    t.x=min(t.x, bo(p, vec3(15, 0.3, 0.3)));//adding box to blue box
    t.x=min(t.x, bo(p, vec3(0.4, 15, 0.4)));//adding more box to blue box
    h=vec2(length(p.xz)-8., 3);// black structural cylinder
    h.x=max(h.x, -(length(p.xz)-6.));//remove from black cylinder
    h.x=max(h.x, bo(p, vec3(15, 0.5, 15)));//intersection cut of black cylinder into flat cross
    t=t.x<h.x?t:h;//merging blue and black materials
    h=vec2(length(p.xz)-7.5, 6);//white structural cylinder
    h.x=max(h.x, -(length(p.xz)-6.5));//removing from white cylinder
    h.x=max(h.x, bo(p, vec3(15, 0.8, 15)));//intersection white cylinder into flat cross
    h.x=min(h.x, bo(p, vec3(0.2, 15, 0.6)));//adding big white edges to blue. BRUTALISM MEANS BRUTALISM
    t=t.x<h.x?t:h;//merge blue and black with white material
    return t;
}
vec2 mp(vec3 p)
{
    p.xy*=r2(sin(1.57+p.z*.2)*.1);//overall wave
    np=p;//setup new postion
    np.z=mod(np.z+tt*2., 30.)-15.;//make it infiniote along z axis
    for (int i=0;i<6;i++){ //basic kaleidoscopic function
        np=abs(np)-vec3(8.5, 0.0, 4.5);//push geom out each iteration with symatery abs creating more geometry
        np.xz*=r2(.785);//90 degree rotate each iteration
    }
    vec2 h, t=fb(np);//make complex network based on simple basic geometry piece function by passing in the new more complex position np
    bp=np+vec3(0, -12, 0); bp.xy*=r2(1.57);//derive new position 2 bp out of first new position np
    h=fb(abs(bp*.5)-vec3(4, 7.5, 0));//make more complex geom based on simple basic piece function by passing another new position 2 bp
    h.x*=2.; t=t.x<h.x?t:h;//adapt size and merge both complex networks of geometry
    return t;
}
vec2 tr(vec3 ro, vec3 rd, float _max, int iter)//main trace  / raycast / raymarching loop function
{
    vec2 h, t= vec2(.1);//0.1 is near plane
    for (int i=0;i<iter;i++){ //march for iter amount of iterations
        h=mp(ro+rd*t.x);//get distance to geom
        if (h.x<.0001||t.x>_max) break;//conditional break we hit something or gone too far
        t.x+=h.x;t.y=h.y;//huge step forward and remember material id
    }
    if (t.x>_max) t.y=0.;//if we hit far plane return material id = 0, we will use it later to check if we hit something
    return t;
}
float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}
vec4 texNoise(vec2 n) {
    n *= 10.2;
    const vec2 d = vec2(0.0, 1.0);
    vec2 b = floor(n), f = smoothstep(vec2(0.0), vec2(1.0), fract(n));
    return vec4(mix(mix(rand(b), rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y));
}
    #define a(d) clamp(mp(po+no*d).x/d, 0., 1.)
    #define s(d) smoothstep(0., 1., mp(po+ld*d).x/d)
void mainImage(out vec4 fragColor, in vec2 fragCoord)//2 lines above are a = ambient occlusion and s = sub surface scattering
{
    vec2 uv=(fragCoord.xy/iResolution.xy-0.5)/vec2(iResolution.y/iResolution.x, 1);//get UVs
    tt=mod(iTime, 59.66)+4.72;//modulo time to avoid glitchy artifact and also nicely reset camera / scene
    vec3 lp=vec3(3.+cos(tt*.2)*10., 6.+sin(tt*.4)*0.5, -10), //light position
    ro=lp*mix(vec3(1), vec3(-1, 2.2, 1), ceil(cos(tt))), // ray origin = camera position
    cw=normalize(vec3(sin(tt*.4)*2., cos(tt*.2)*10., 0)-ro), //camera forward vector
    cu=normalize(cross(cw, vec3(0, 1, 0))), //camera left vector ?
    cv=normalize(cross(cu, cw)), //camera up vector ?
    rd=mat3(cu, cv, cw)*normalize(vec3(uv, .5)), co, fo;//ray direction
    lp+=vec3(0, 5.+sin(tt)*.5, 5);// light position offset animation
    v=vec2(abs(atan(rd.x, rd.z)), rd.y);//polar uv for pseudo clouds
    co=fo=clamp(vec3(.1)+(1.-length(uv))+0.5*texNoise(v).r, 0., 1.);// background with pseudo clouds made from noise
    z=tr(ro, rd, 50., 128);t=z.x;// let's trace and get result
    if (z.y>0.){ // we hit something
        po=ro+rd*t;// get position where we hit
        ld=normalize(lp-po);//get light direction from light pos
        no=normalize(mp(po).x-vec3(mp(po-e.xyy).x, mp(po-e.yxy).x, mp(po-e.yyx).x));//LJ's "fit in da pocket" normals calculation
        float line=ceil(cos(np.x*2.1))-ceil(cos(np.x*2.1+.1));// lines on blue geometry
        al=clamp(line+mix(vec3(0.0, .3, .7), vec3(.1, .5, .7), sin(1.57+np)*.5+.5), 0., 1.);//blue geomtry albedo colour: subtle gradient with lines
        if (z.y<5.) al=vec3(0); if (z.y>5.) al=vec3(1);//material id < 5 -> black; material id > 5 -> white
        float dif=max(0., dot(no, ld)), // diffuse lighting
        fr=pow(1.-abs(dot(rd, no)), 4.), // Get schlick fresnel for reflections before we refract. Major thankx to LJ for this
        spo=exp2(15.*texNoise(0.3*vec2(.05, .1)*vec2(np.x, dot(np.yz, vec2(.5)))).r), // Gloss specular map made from noise
        sp=pow(max(dot(reflect(-ld, no), -rd), 0.), spo), //Specular lighting
        ldd=length(lp-po), attn=1.0-pow(min(1.0, ldd/15.), 4.0);//point light attenuation
        attn*=ldd>tr(po, ld, min(15., ldd), 50).x?.5:1.;// Size coded shadows. Major thankx to crundle and alkama for this
        co=(sp+al*(a(.05)*a(1.))*(dif+s(.1)))*attn;// mix all lighting into final lighting result
if (z.y==5.){ //REFLECTIONS If we hit material ID of 5 (blue geometry)
    rd=refract(rd, -no, 1.+(spo*.002-.05));// Gloss specular map reused to refract ray for the reflections
    z=tr(po+rd*0.01, rd, 50., 80);//Shoot ray again from surface to refracted reflected ray - Major thankx to shane for +rd*0.01 offset
    po=po+rd*z.x; ld=normalize(lp-po);// get reflection pos we hit and light direction
    no=normalize(mp(po).x-vec3(mp(po-e.xyy).x, mp(po-e.yxy).x, mp(po-e.yyx).x));//LJ's "fit in da pocket" normals calculation
    al=mix(vec3(0.0, .3, .7), vec3(.1, .5, .7), sin(1.57+np)*.5+.5);//reflected albedo is more simple without lines to simplify & optimize
    attn=1.0-pow(min(1.0, ldd/20.), 4.0);//attenuate reflections lighting bit less than before to fake ambient lighting reflections
    if (z.y<5.) al=vec3(0); if (z.y>5.) al=vec3(1);//material id < 5 -> black; material id > 5 -> white
    float dif=max(0., dot(no, ld));//naive reflections lighting. Just diffuse*albedo. No ao, no sss, no specular to optmize & make reflections more homogenous
    co+=dif*al*fr*attn;// add reflections lighting to final colour with schlick fresneled and gloss map refractions (line 86-87)
}
}
co=mix(fo, co, exp(-.00003*t*t*t));// fog
fragColor = vec4(pow(co, vec3(.45)), 1);// naive gamma correction
}