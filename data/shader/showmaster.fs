uniform float iZoom;
uniform float iPeak;
uniform float iRadius;

#define PI 3.14159
#define eps 0.001
vec4 check(vec2 uv) {
    const float s = 10.;
    return vec4(vec3(.5+.5*mod(floor(s*uv.x)+floor(s*uv.y),2.0)), 1.);
}
float iSphere( in vec3 ro, in vec3 rd, in vec4 sph ) {
	vec3 oc = ro - sph.xyz;
	float b = dot( oc, rd );
	float c = dot( oc, oc ) - sph.w*sph.w;
	float h = b*b - c;
	if( h<0.0 ) return -1.0;
	h = sqrt(h);
	float t1 = -b - h;
	float t2 = -b + h;
	if( t1<eps && t2<eps )
		return -1.0;
	return t2;
}
vec4 over( in vec4 a, in vec4 b ) {
    return mix(a, b, 1.-a.w);
}
vec3 camera(vec3 ro, vec3 lookat, vec2 uv, float zoom) {
    vec3 f = normalize(lookat-ro);
    vec3 r = cross(vec3(0., 1., 0.), f);
    vec3 u = cross(f, r);
    vec3 c = ro + f*zoom;
    vec3 i = c + uv.x*r + uv.y*u;
    return normalize(i-ro);
}
void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = (fragCoord-iResolution.xy*0.5)/iResolution.y*2.;
    vec4 sphere = vec4(0., 0., 0., 1.0);
    vec3 ro = vec3(0., 0., 1.);
    vec3 rd = camera(ro, vec3(0.), uv, iZoom);
	float sp = iSphere(ro, rd, sphere);
    if(-1.==sp) {
        return;
    }
    vec3 pos = ro+rd*sp-sphere.xyz;
	float theta = atan(pos.x, pos.z)/PI;
	float phi = acos( -pos.y)/(PI*2.);
    vec2 suv = vec2(.5+theta*.5, phi);
    suv.x += iTime/128.0;
    float a = smoothstep(1., pos.z*.75, length(fract(suv*iRadius)*2.-1.)) * iPeak;
    fragColor = vec4(vec3(0.25*a, 0.97*a, 1.0*a), a);
}