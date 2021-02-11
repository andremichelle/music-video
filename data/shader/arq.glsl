// https://www.shadertoy.com/view/WtyyDd
float det=.00002;
float maxdist=.5;
vec3 ldir=vec3(0.,1.,4.);
float fcol;
float t;
float obj=0.;

mat2 rot(float a) {
    float s=sin(a),c=cos(a);
    return mat2(c,s,-s,c);
}

float noise(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}


float kset(vec3 p) {
    p=abs(1.-mod(p*800.,2.));
    p+=vec3(.2,.3,.4);
    for (int i=0; i<10; i++) {
        p=abs(p)/dot(p,p)-.9;
    }
    return length(p);
}


vec3 path(float t) {
    vec3 p = vec3(sin(t*12.)*.1,cos(t*7.)*.15,t);
    return p;
}


float de(vec3 p) {
    vec3 pth=path(p.z);
    float yw=.0425+p.y-(1.+sin(p.z*20.))*.02-pth.y;
    p.y-=3.21;
    p.x+=.25;
    p.xy-=pth.xy;
    p.z=abs(.5-fract(p.z));
    vec3 pos=p;
    float der=1.;
    float m=100.;
    for(int i=0;i<13;i++){
        p=abs(p)-vec3(0.,2.2,0.1+sin(pos.z)*0.1);
        float sc=2./clamp(dot(p,p),0.25,1.);
        p*=sc;
        der*=sc;
        p = p - vec3(.5,1.,.5);
        m=min(m,dot(p,p));
    }
    fcol=1.-pow(m*.6,3.);
    fcol=fcol*1.3-clamp(1.-kset(pos)*.8,.2,.5);
    return (length(p)-2.)/der*.7;
}

vec3 normal(vec3 p) {
    vec3 e=vec3(0.,det*2.,0.);
    return normalize(vec3(de(p+e.yxx),de(p+e.xyx),de(p+e.xxy))-de(p));
}

float ao(vec3 p, vec3 n) {
    float st=.0002;
    float ao=0.;
    for(float i=0.; i<6.; i++ ) {
        float td=st*i;
        float d=de(p+n*td);
        ao+=max(0.,(td-d)/td);
    }
    return clamp(1.-ao*.3,0.,1.);
}

float de_light(vec3 p) {
    p.xy*=rot(iTime*.2);
    p.xz*=rot(iTime*.2);
    float xy=sin(atan(p.x,p.y)*8.)*.00015;
    float xz=sin(atan(p.x,p.z)*8.)*.00015;
    float yz=sin(atan(p.y,p.z)*8.)*.00015;
    return length(p)-.0005-xy-xz-yz;
}

float shadow(vec3 p, vec3 lightpos, vec3 ldir) {
    float td=.0001,sh=1.,d=det;
    for (int i=0; i<60; i++) {
        p-=ldir*d;
        d=de(p);
        td+=d;
        sh=min(sh,15.*d/td);
        if (sh<.001 || distance(p,lightpos) < .02) break;
    }
    return clamp(sh,0.,1.);
}

float shade(vec3 p, vec3 from, vec3 dir, vec3 lightpos) {
    float fade=max(0.1,1.-sqrt(distance(p,lightpos))*2.5);
    vec3 ldir=normalize(p-lightpos);
    float col=fcol;
    vec3 n=normal(p);
    float ao=ao(p,n);
    float sh=shadow(p,lightpos,ldir);
    float amb=.3;
    float dif=max(0.,dot(ldir,-n))*sh*.7;
    return col*(amb*ao+dif)*fade;
}


float march(vec3 from, vec3 dir, vec3 lightpos, vec2 uv) {
    vec3 p, pl;
    float td=0.,tdl=0.,d,dl, fade=1.,col=0.;
    float g=10.,lg=0.;


    float n=noise(uv+iTime*1.23487)*.005;
    td+=n;
    tdl+=n;
    p=pl=from;

    for(int i=0; i<250; i++) {
        p+=d*dir;
        pl+=dl*dir;
        dl=de_light(pl-lightpos);
        det*=1.+td*td*.7;
        d=de(p);
        if (td>maxdist || d<det) break;
        td+=d;
        tdl+=dl;
        lg=max(lg,max(0.,.1-dl)/.1);
        g++;
    }
    if (d<.01 && td<maxdist) {
        p-=dir*det*2.;
        col+=shade(p, from, dir, lightpos);
    } else td=maxdist;
    col+=td*.5;
    col=pow(abs(col),1.25);
    col+=pow(lg,12.);
    return col;
}

mat3 lookat(vec3 dir, vec3 up) {
    dir=normalize(dir);vec3 rt=normalize(cross(dir,normalize(up)));
    return mat3(rt,cross(rt,dir),dir);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = (fragCoord-iResolution.xy*.5)/iResolution.y;
    t=iTime*.01;
    vec3 from = path(t);
    vec3 dir = normalize(vec3(uv,1.));
    vec3 lightpos = path(t+.02);
    dir=lookat(lightpos-from,vec3(0.,1.,0.))*dir;
    float col = march(from, dir, lightpos, uv);
    col+=noise(uv+iTime*.0231)*.1;
    fragColor = vec4(col);
}