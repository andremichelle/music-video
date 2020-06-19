float rand(vec2  n){ return fract(sin(n.x*11.+n.y*17.) * 43758.5453123);}
float rand(float n){ return fract(sin(n)               * 43758.5453123);}
float sdRoundBox(vec2 p, vec2 b, float r) {
  vec2 q = abs(p) - b;
  return length(max(q,0.)) + min(max(q.x,q.y),0.) - r;
}
void mainImage(out vec4 O, vec2 u) {
    O = vec4(0.0, 0.0, 0.0, 1.0);
    vec2  R = iResolution.xy,
          U = (u - R*.5) / R.y;
    float t = -iTime;
    U.x += sin(t*0.015625*3.14159)*.25;
    U.y += sin(t*0.015625*3.14159*2.0)*.25;
    for(float i = 0. ; i < 1. ; i += .2) {
        float u = fract(t*.125+i),
              q = rand(i+10.)*8. + u*2. + sin(t*0.0625*3.14159)*2.;
        vec2  P = U *u*90. * mat2(cos(q), -sin(q), sin(q), cos(q)),
              f = 2.*fract(P)-1.;
        float rn = rand(floor(P)),
              c = pow(abs(sin((iTime*0.0625+rn)*3.14159)),32.),
              s = smoothstep(.1,.01, length(f) - c);
        O = max(O, vec4(1,1,.2*c,1) *s*(1.-u));
    }
}