// Refactoring Blackpool - Result of an improvised live coding session on Twitch
// LIVE SHADER CODING, SHADER SHOWDOWN STYLE, EVERY TUESDAYS 20:00 Uk time: 
// https://www.twitch.tv/evvvvil_

// "One day kids, I'll refactor Blackpool" - Le Corbusier 
uniform float peak;
vec2 z,v,e=vec2(.00035,-.00035);float t,tt,b,g=0.,gg=0.;vec3 np,pp,po,no,al,ld,cp,op;
float bo(vec2 p, vec2 b ){ p= abs(p)-b;return length(max(p,0.))+min(max(p.x,p.y),0.);}
float box(vec3 p, vec3 b ){ p= abs(p)-b;return max(max(p.x,p.y),p.z);}
mat2 r2(float r){return mat2(cos(r),sin(r),-sin(r),cos(r));}
float ex(vec3 p,float sdf,float h){vec2 w=vec2(sdf,abs(p.y)-h);return min(max(w.x,w.y),0.0)+length(max(w,0.0));}
vec2 mp( vec3 p)
{
    op=p;
    p.x=mod(p.x-tt*0.7,20.)-10.;
    np=vec3(p.xz*.5,1.0);
    pp=p-vec3(0,3,0);
    pp.xz=mod(pp.xz,5.)-2.5;
    pp.yz*=r2(.785);
    vec2 h=vec2(1000,6),t=vec2(1000,5);
    float cutBox=box(pp,vec3(2.0,1.5,2.5));
    for(int i=0;i<4;i++){
        b=float(i);
        np.xy=abs(np.xy)-2.0;
        np.xy*=r2(.785*(b+1.));
        np*=1.8;
        np.y=abs(np.y)-1.5;
        t.x=min(t.x,ex(p,max(bo(np.xy,vec2(5,.2))/np.z*2.0,-cutBox),1.0+b*.5));
        t.x=abs(t.x)-.02*max(sin(p.y*10.),0.1)-.04*clamp(sin(np.x*2.5),.1,.5);
        h.x=min(h.x,ex(p,bo(np.xy,vec2(5.,0.01))/np.z*2.0,.25+b*.5-0.1*cos(op.x*1.3+1.5)));
        t.x=max(t.x,abs(p.y)-0.3-b*.5);
    }
    h.x=max(h.x,-cutBox+.1);
    g+=0.1/(0.1+h.x*h.x*(1.-sin(op.x*1.3)*.9)*1600.);
    cp=vec3(np.xy,p.y*2.);
    h.x=min(h.x,box(cp+vec3(-5.,2,0),vec3(1.7,1,5.+sin(p.x*.7)))/np.z*2.);
    float part=length(cos(cp.yz*20.));
    part=max(part,p.y-1.5);
    gg+=0.1/(0.1+part*part*12.);
    t.x=min(t.x,part);
    t=t.x<h.x?t:h;
    return t;
}
vec2 tr( vec3 ro, vec3 rd)
{
    vec2 h,t=vec2(.1);
    for(int i=0;i<128;i++){
        h=mp(ro+rd*t.x);
        if(t.x<.0001||t.x>20.) break;
        t.x+=h.x;t.y=h.y;
    }
    if(t.x>20.) t.y=0.;
    return t;
}
    #define a(d) clamp(mp(po+no*d).x/d,0.,1.)
    #define s(d) smoothstep(0.,1.,mp(po+ld*d).x/d)
void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv=(fragCoord.xy/iResolution.xy-0.5)/vec2(iResolution.y/iResolution.x,1);
    tt=mod(iTime,64.0);
    vec3 ro=vec3(10.,2.9-sin(tt*.2)*.8,-4.+ceil(cos(tt*0.1963495408))*8.+cos(tt*0.1963495408)*2.),
    cw=normalize(vec3(0,-6,0)-ro),cu=normalize(cross(cw,vec3(0,1,0))),
    cv=normalize(cross(cu,cw)),rd=mat3(cu,cv,cw)*normalize(vec3(uv,.5)),co,fo;
    co=fo=vec3(.18,.16,.2)-length(uv)*.25;
    ld=normalize(vec3(.2,.4,-.3));
    z=tr(ro,rd);t=z.x;
    if(z.y>0.){
        po=ro+rd*t;
        no=normalize(e.xyy*mp(po+e.xyy).x+
        e.yyx*mp(po+e.yyx).x+
        e.yxy*mp(po+e.yxy).x+
        e.xxx*mp(po+e.xxx).x);
        no-=0.3*ceil(abs(sin(cp*10.))-.1);no=normalize(no);
        al=mix(vec3(0.4),vec3(0.0,0.15,0.75 ),cp.y*.5*peak);
        if(z.y>5.) al=vec3(1.2);
        float dif=max(0.,dot(no,ld)),
        fr=pow(1.+dot(no,rd),4.),
        sp=pow(max(dot(reflect(-ld,no),-rd),0.),40.);
        co=mix(sp+al*(a(.12)+.2)*(dif+s(2.)),fo,min(fr,0.4));
co=mix(fo,co,exp(-.0007*t*t*t)*(0.5+peak*0.5));
}
fragColor = vec4(pow(co+g*.1*vec3(.1,.2,.7)+gg*.05*vec3(.4,.1,.1),vec3(.65)),1);
}