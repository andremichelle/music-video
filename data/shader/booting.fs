void mainImage( out vec4 fragColor, in vec2 fragCoord ) {
    vec2 uv = (fragCoord-iResolution.xy*0.5)/iResolution.y*2.0;

    float tt = fract(iTime/16.0)*8.0;
    float a = atan(uv.y, uv.x) - tt * 6.283185/8.0;
    float l = length(uv);

    float x = 48.0*(l-0.3+sin(tt*6.283185/2.0)*0.01);
    float c = abs(cos(x*2.0)/x)*max(0.0,(1.75-abs(x*0.001*(0.5*sin(tt)*0.5))));
    float d = 0.0;
    float t = tt*3.14159;
    d += sin(a*1.0+t);
    d += sin(a*2.0-t);
    d += sin(a*3.0+t);
    d += sin(a*2.0-t);
    d += sin(a*1.0+t);
    float amount = c*d;
    vec3 col = vec3(1.0,0.8,0.2)*(0.05+amount*0.3);
    fragColor = vec4(col,1.0);
}