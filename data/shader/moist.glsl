// https://www.shadertoy.com/view/WdccDj

// Fork of "18 - Sea of Sinewaves" by Krabcode. https://shadertoy.com/view/3ddcz2
// 2020-10-02 16:01:39
// forked again from https://www.shadertoy.com/view/WstcDB


// Based on Ray Marching for Dummies!"
// by Martijn Steinrucken aka BigWings/CountFrolic - 2018
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
// https://www.shadertoy.com/view/XlGBW3

const int MAX_STEPS = 1;
const float MAX_DIST = 2.;
const float SURF_DIST = 0.0001;
const float NORMAL_DIST = 0.001;
const float SHININESS = 10.;
const float DOWNSTEP = 0.01;
const float PI = 3.14159;

vec2 uv;
vec2 lightOffset;
vec3 lightPos;

float rayLength;
float closestDist;
float hitDist;
vec3 hit;
vec3 intersect;
vec2 m;

mat2 rotate(float a){
    return mat2(cos(a), -sin(a), sin(a), cos(a));
}


float wave(vec2 p) {
    float v = sin(p.x*0.7 + sin(p.y*2.2) + sin(p.y * .43));
    return v*v;
}

const mat2 rot = mat2(0.5, 0.86, -0.86, 0.5);

float get(vec2 p,float t)
{
    float v = 0.0;//abs(sin(p.x+p.y*1.4))*0.1;
    v += wave(p);
    p.y += t;
    p *= rot;
    v += wave(p);
    p.y += t * .17;	//0.17
    p *= rot;
    v += wave(p);
    v+=pow(abs(sin(p.x+v)),2.0);

    v = abs(1.5 - v);
    return v;
}

float sdf(vec3 p){
    float v = get(p.xy*6.0,iTime)*0.1;
    v = smoothstep(0.02,3.5,v)*2.5;

    return p.z+v;
}

float rayMarch(vec3 ro, vec3 rd)
{
    float dO=0.;
    for(int i=0; i<MAX_STEPS; i++) {
        vec3 p = ro + rd*dO;
        float dS = sdf(p);
        closestDist = min(dS, closestDist);
        dO += dS*DOWNSTEP;
        if(dO>MAX_DIST || dS<SURF_DIST) break;
    }
    return dO;
}

vec3 normal(vec3 p)
{
    float d = sdf(p);
    vec2 e = vec2(NORMAL_DIST, 0);
    vec3 n = d - vec3(
    sdf(p-e.xyy),
    sdf(p-e.yxy),
    sdf(p-e.yyx));
    return normalize(n);
}

float diffuseLight(vec3 p, vec3 normal)
{
    vec3 l = normalize(lightPos-p);
    float dif = clamp(dot(normal, l), 0., 1.);
    float d = rayMarch(p+normal*SURF_DIST*2., l);
    if(d<length(lightPos-p)){ dif *= .1; }
    return dif;
}


float specularLight(vec3 p, vec3 rayDir, vec3 normal) {
    vec3 lightDir = normalize(p-lightPos);
    vec3 reflectionDirection = reflect(-lightDir, normal);
    float specularAngle = max(dot(reflectionDirection, rayDir), 0.);
    return pow(specularAngle, SHININESS);
}

float render(vec2 uv)
{
    vec3 rayOrigin = vec3(uv+vec2(0., 0.25), 0.);
    vec3 rayDir = normalize(vec3(uv.x, uv.y, 1.));
    hitDist = rayMarch(rayOrigin, rayDir);
    hit = rayOrigin + rayDir * hitDist;
    vec3 normal = normal(hit);
    float diff = diffuseLight(hit, normal);
    float spec = specularLight(hit, rayDir, normal);
    return .5*diff + .5*spec;
}

float aaRender(vec2 uv){
    vec2 third = vec2(1./iResolution.x, 1./iResolution.y) / 3.0;
    vec2 mult = vec2(1, -1);
    float c1 = render(uv+third*mult.xx);
    float c2 = render(uv+third*mult.xy);
    float c3 = render(uv+third*mult.yx);
    float c4 = render(uv+third*mult.yy);
    return (c1+c2+c3+c4) / 4.;
}

vec3 gammaCorrection(vec3 rgb){
    float gamma = 2.2;
    rgb = smoothstep(0., 1., rgb);
    return pow(max(rgb, 0.), vec3(1.0/gamma));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    //m = iMouse.xy / iResolution.xy;
    //time = 100.+float(iFrame)*.0005;
    lightPos = vec3(0.,5.0,-16.0);
    uv = (fragCoord-.5*iResolution.xy)/iResolution.y;
    float lit = render(uv);
    lit *= smoothstep(MAX_DIST*.15, MAX_DIST*.05, hitDist);
    vec3 col = vec3(0.2,1.2,1.8)*lit;
    fragColor = vec4(gammaCorrection(col),1.0);
}
