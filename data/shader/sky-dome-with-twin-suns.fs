// https://www.shadertoy.com/view/WlXcRN

#define PI  3.141592654
#define TAU (2.0*PI)

const vec3  skyCol1       = vec3(0.35, 0.45, 0.6);
const vec3  skyCol2       = vec3(0.0);
const vec3  skyCol3       = pow(skyCol1, vec3(0.25));
const vec3  sunCol1       = vec3(1.0,0.6,0.4);
const vec3  sunCol2       = vec3(1.0,0.9,0.7);
const vec3  smallSunCol1  = vec3(1.0,0.5,0.25)*0.5;
const vec3  smallSunCol2  = vec3(1.0,0.5,0.25)*0.5;
const vec3  ringColor     = sqrt(vec3(0.95, 0.65, 0.45));
const vec4  planet        = vec4(80.0, -20.0, 100.0, 50.0)*1000.0;
const vec3  ringsNormal   = normalize(vec3(1.0, 1.25, 0.0));
const vec4  rings         = vec4(ringsNormal, -dot(ringsNormal, planet.xyz));

// From: https://iquilezles.org/www/articles/intersectors/intersectors.htm
float rayPlane(vec3 ro, vec3 rd, vec4 plane) {
  return -(dot(ro,plane.xyz)+plane.w)/dot(rd,plane.xyz);
}

vec2 raySphere(vec3 ro, vec3 rd, vec4 sphere) {
  vec3 ce = sphere.xyz;
  float ra = sphere.w;
  vec3 oc = ro - ce;
  float b = dot(oc, rd);
  float c = dot(oc, oc) - ra*ra;
  float h = b*b - c;
  if (h<0.0) return vec2(-1.0); // no intersection
  h = sqrt(h);
  return vec2(-b-h, -b+h);
}

vec3 sunDirection() {
  return normalize(vec3(-0.5, 0.085, 1.0));
}

vec3 smallSunDirection() {
  return normalize(vec3(-0.2, -0.05, 1.0));
}

vec3 rocketDirection() {
  return normalize(vec3(0.0, -0.2+mod(iTime, 90.0)*0.0125, 1.0));
}

float psin(float f) {
  return 0.5 + 0.5*sin(f);
}

vec3 skyColor(vec3 ro, vec3 rd) {
  vec3 sunDir = sunDirection();
  vec3 smallSunDir = smallSunDirection();

  float sunDot = max(dot(rd, sunDir), 0.0);
  float smallSunDot = max(dot(rd, smallSunDir), 0.0);
  
  float angle = atan(rd.y, length(rd.xz))*2.0/PI;

  vec3 skyCol = mix(mix(skyCol1, skyCol2, smoothstep(0.0 , 1.0, 5.0*angle)), skyCol3, smoothstep(0.0, 1.0, -5.0*angle));
  
  vec3 sunCol = 0.5*sunCol1*pow(sunDot, 20.0) + 8.0*sunCol2*pow(sunDot, 2000.0);
  vec3 smallSunCol = 0.5*smallSunCol1*pow(smallSunDot, 200.0) + 8.0*smallSunCol2*pow(smallSunDot, 20000.0);

  vec3 dustCol = pow(sunCol2*ringColor, vec3(1.75))*smoothstep(0.05, -0.1, rd.y)*0.5;

  vec2 si = raySphere(ro, rd, planet);
  float pi = rayPlane(ro, rd, rings);
  
  float dustTransparency = smoothstep(-0.075, 0.0, rd.y);
  
  vec3 planetSurface = ro + si.x*rd;
  vec3 planetNormal = normalize(planetSurface - planet.xyz);
  float planetDiff = max(dot(planetNormal, sunDir), 0.0);
  float planetBorder = max(dot(planetNormal, -rd), 0.0);
  float planetLat = (planetSurface.x+planetSurface.y)*0.0005;
  vec3 planetCol = mix(1.3*vec3(0.9, 0.8, 0.7), 0.3*vec3(0.9, 0.8, 0.7), pow(psin(planetLat+1.0)*psin(sqrt(2.0)*planetLat+2.0)*psin(sqrt(3.5)*planetLat+3.0), 0.5));

  vec3 rocketDir = rocketDirection();
  float rocketDot = max(dot(rd, rocketDir), 0.0);
  float rocketDot2 = max(dot(normalize(rd.xz), normalize(rocketDir.xz)), 0.0);
  vec3 rocketCol = vec3(0.25)*(3.0*smoothstep(-1.0, 1.0, psin(iTime*15.0*TAU))*pow(rocketDot, 70000.0) + smoothstep(-0.25, 0.0, rd.y - rocketDir.y)*step(rd.y, rocketDir.y)*pow(rocketDot2, 1000000.0))*dustTransparency*(1.0 - smoothstep(0.5, 0.6, rd.y));

  float borderTransparency = smoothstep(0.0, 0.1, planetBorder);
  
  vec3 ringsSurface = ro + pi*rd;
  float ringsDist = length(ringsSurface - planet.xyz)*1.0;
  float ringsPeriod = ringsDist*0.001;
  const float ringsMax = 150000.0*0.655;
  const float ringsMin = 100000.0*0.666;
  float ringsMul = pow(psin(ringsPeriod+1.0)*psin(sqrt(0.5)*ringsPeriod+2.0)*psin(sqrt(0.45)*ringsPeriod+4.0)*psin(sqrt(0.35)*ringsPeriod+5.0), 0.25);
  float ringsMix = psin(ringsPeriod*10.0)*psin(ringsPeriod*10.0*sqrt(2.0))*(1.0 - smoothstep(50000.0, 200000.0, pi));
  vec3 ringsCol = mix(vec3(0.125), 0.75*ringColor, ringsMix)*step(-pi, 0.0)*step(ringsDist, ringsMax)*step(-ringsDist, -ringsMin)*ringsMul;
  vec3 final = vec3(0.0);
  final += ringsCol*(step(pi, si.x) + step(si.x, 0.0));
  final += step(0.0, si.x)*pow(planetDiff, 0.75)*mix(planetCol, ringsCol, 0.0)*dustTransparency*borderTransparency + ringsCol*(1.0 - borderTransparency);
  final += skyCol + sunCol + smallSunCol + dustCol + rocketCol;
  return final;
}

void mainImage(out vec4 fragColor, vec2 fragCoord) {
  vec2 q = fragCoord.xy/iResolution.xy;
  vec2 p = -1.0 + 2.0*q;
  p.x *= iResolution.x/iResolution.y;
  vec3 ro  = vec3(0.0, 0.0, -2.0);
  vec3 la  = vec3(0.0, 0.4, 0.0);
  vec3 ww = normalize(la - ro);
  vec3 uu = normalize(cross(vec3(0.0,1.0,0.0), ww));
  vec3 vv = normalize(cross(ww, uu));
  vec3 rd = normalize(p.x*uu + p.y*vv + 2.0*ww);
  vec3 col = skyColor(ro, rd);
  fragColor = vec4(col, 1.0);
}