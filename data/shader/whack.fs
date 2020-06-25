//https://www.shadertoy.com/view/WtXcWB
//CC0 1.0 Universal https://creativecommons.org/publicdomain/zero/1.0/
//To the extent possible under law, Blackle Mori has waived all copyright and related or neighboring rights to this work.
#define AA_SAMPLES 2
//the following functions assume that p is inside the cube of radius 1 centered at the origin
//closest vertex of the cube to p
vec3 vertex(vec3 p) {
    return step(0.,p)*2.-1.;
}
//closest face of the cube to p
vec3 face(vec3 p) {
    vec3 ap = abs(p);
    if (ap.x>=max(ap.z,ap.y)) return vec3(sign(p.x),0.,0.);
    if (ap.y>=max(ap.z,ap.x)) return vec3(0.,sign(p.y),0.);
    if (ap.z>=max(ap.x,ap.y)) return vec3(0.,0.,sign(p.z));
    return vec3(0);
}
//closest edge of the cube to p
vec3 edge(vec3 p) {
    vec3 mask = vec3(1)-abs(face(p));
    vec3 v = vertex(p);
    vec3 a = v*mask.zxy, b = v*mask.yzx;
    return distance(p,a)<distance(p,b)?a:b;
}
float super(vec2 p) {
    return sqrt(length(p*p));
}
float corner(vec2 p, float h) {
    vec2 q = p - vec2(0,h);
    return super(max(q,0.)) + min(0.,max(q.x,q.y));
}
//returns rhombic dodecahedron tessalation data for p
//x: distance to circle of radius .6 in current cell
//y: distance to circle of radius .6 in closest adjacent cell
//zw: ID of cell
vec4 grid(vec3 p) {
    vec3 id = floor(p)+.5;
    vec3 m = sign(mod(id,2.)-1.);
    if (m.x*m.y*m.z<0.) id += face(p-id);
    p -= id;
    float d1 = length(p)-.6;
    p -= edge(p);
    float d2 = length(p)-.6;
    return vec4(d1,d2,id);
}
#define FBI floatBitsToInt
float hash(float a, float b) {
    int x = FBI(cos(a))^FBI(a);
    int y = FBI(cos(b))^FBI(b);
    return float((x*x+y)*(y*y-x)+x)/2.14e9;
}
//springy impulse
float spring(float x) {
    return smoothstep(-.4,.4,x) + smoothstep(-.3,.3,x) - smoothstep(-.7,.7,x);
}
float smin(float a, float b, float k) {
    float h = max(0.,k-abs(b-a))/k;
    return min(a,b) - h*h*h*k/6.;
}
vec3 smin(vec3 a, vec3 b, float k) {
    vec3 h = max(vec3(0),k-abs(b-a))/k;
    return min(a,b) - h*h*h*k/6.;
}
vec3 erot(vec3 p, vec3 ax, float ro) {
    return mix(dot(p,ax)*ax,p,cos(ro))+sin(ro)*cross(ax,p);
}
//mtime set by "pixel_color" to influence the time used by the scene
float mtime;
//lots of globals set by "scene"
vec2 gid;
vec3 glocal;
float gnd;
float gt;
float scene(vec3 p) {
    //ds1 chooses z coordinate in 2d slicing of the rhombic dodecahedron tessalation
    //by varying it over space, we get different sized circles
    float ds1 = dot(cos(p.xy/5.), sin(p.xy/4.))*.06;
    vec3 p3 = vec3(p.xy, ds1);
    vec4 g = grid(p3);
    gid = g.zw;
    float s1 = hash(gid.x,gid.y);
    float s2 = hash(s1,s1);
    gt = sin(s1*100.+mtime*mix(1.,2.,s2*.5+.5))-.4;
    float h = spring(gt)*2.-.5;
    vec2 crd = vec2(g.x,p.z);
    vec2 crd2 = vec2(g.y,p.z);
    float maxheight = 1.7;
    gnd = corner(crd*vec2(-1,1)+vec2(0.08,0.),0.)-.04; //ground holes
    //transform things into local coordinates for piston
    crd.y -= h;
    glocal = p - vec3(gid,h);
    glocal = erot(glocal,vec3(0,0,1),s1*100.+gt*2.);
    float curr = corner(crd, 0.); //distance to current piston
    //little holes on side of piston
    vec3 lp = glocal;
    lp.z = asin(sin(lp.z*5.+.5))/5.;
    curr = -smin(-curr, length(lp.yz)-0.05,.03);
    float adjacent = corner(crd2, maxheight); //distance to adjacent piston (assumes maximally extended)
    return min(gnd,min(curr, adjacent)-.02);
}
vec3 norm(vec3 p) {
    mat3 k = mat3(p,p,p)-mat3(0.01);
    return normalize(scene(p) - vec3(scene(k[0]),scene(k[1]),scene(k[2])));
}
vec3 skylight(vec3 p) {
    float d = dot(p,normalize(vec3(1)));
    return vec3(1)*d*.2+.2 + pow(max(0.,d),10.)*1.5;
}
float smpl(vec3 p, vec3 dir, float dist) {
    return smoothstep(-dist,dist,scene(p+dir*dist));
}
vec3 pixel_color(vec2 uv, float time)
{
    mtime = time;
    vec3 cam = normalize(vec3(1.5,uv));
    vec3 init = vec3(-7,0,0);
    float yrot = 0.7+sin(time*.3)*.2;
    float zrot = time*.2;
    cam = erot(cam,vec3(0,1,0),yrot);
    init = erot(init,vec3(0,1,0),yrot);
    cam = erot(cam,vec3(0,0,1),zrot);
    init = erot(init,vec3(0,0,1),zrot);
    init.xy += time*vec2(.5,sqrt(2.));
    init.z += 2.;
    vec3 p =init;
    bool hit = false;
    float dist; int i;
    for (i = 0; i < 200 && !hit; i++) {
        dist = scene(p);
        hit = dist*dist < 1e-6;
        p += dist*cam;
        if(distance(p,init)>50.)break;
    }
    //save globals locally
    bool g = gnd == dist;
    vec2 id = gid;
    float s1 = hash(gid.y,gid.x);
    float s2 = hash(s1,gid.x);
    vec3 local = g ? p : glocal+vec3(id,0);
    float fog = min(1.,smoothstep(5.,50.,distance(p,init))+smoothstep(100.,200.,float(i)));
    vec3 n = norm(p);
    vec3 r = reflect(cam,n);
    float ao = smpl(p,n,.1);
    //brushed metal tops. not sure if this is the right way, but it works!
    if (!g && n.z>.9) {
        float ang = atan(p.x-id.x,p.y-id.y);
        float ang2 = atan(local.x-id.x,local.y-id.y);
        local = vec3(ang2/2.,length(p.xy-id)*40.,local.z+id.x*.9+id.y*.4);
        n = normalize(vec3(cos(ang*2.),sin(ang*2.),1));
    }
    //rough texture
    float sharpness = .9;
    //fake reflection occlusion
    float ro = sqrt(smpl(p,r,.9)*smpl(p,r,.5)*smpl(p,r,.2));
    float spec = length(sin(r*3.*sharpness)*.4+.6)/sqrt(3.) * smoothstep(-1.,-.0,p.z);
    float fres = 1.-abs(dot(cam,n))*.5;
    vec3 mcol = abs(erot(vec3(0.4,0.6,0.9), normalize(vec3(0,s2,2)), s1*.6));
    if (g) mcol = vec3(0.1);
    vec3 col = (mcol*spec + pow(spec,10.*sharpness))*ro*ao*fres*1.5;
    vec3 bgcol = skylight(cam);
    vec3 fragColor = hit ? mix(col,bgcol,fog) : bgcol;
    return fragColor;
}
vec2 weyl_2d(int n) {
    return fract(vec2(n*12664745, n*9560333)/exp2(24.));
}
void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = (fragCoord-.5*iResolution.xy)/iResolution.y;
    fragColor = vec4(0);
    for (int i = 0; i < AA_SAMPLES; i++) {
        vec2 uv2 = uv + weyl_2d(i)/iResolution.y*1.25;
        fragColor += vec4(pixel_color(uv2, iTime), 1.);
    }
    fragColor.xyz *= 0.5;
	fragColor.xyz = fragColor.xyz/fragColor.w;
}