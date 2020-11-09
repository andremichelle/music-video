// https://www.shadertoy.com/view/WstfRr
//--- Flythrough
// by Catzpaw 2020

#define ITER 192
#define EPS 1e-5
#define NEAR 0.0
#define FAR 120.0

#define CUE 0.

#define OCT 4
#define ZERO min(0,iFrame)

mat2 rot(float a){float s=sin(a),c=cos(a);return mat2(c,s,-s,c);}
vec3 hsv(float h,float s,float v){return ((clamp(abs(fract(h+vec3(0.,.666,.333))*6.-3.)-1.,0.,1.)-1.)*s+1.)*v;}
float sdCube(vec3 p,float s){p=abs(p)-s;return length(max(p,0.))+min(max(p.x,max(p.y,p.z)),0.)-.03;}
float sdCubeField(vec3 p,float s,float i){p=mod(p,i)-i*.5;return sdCube(p,s);}
float sdBeam(vec3 p,float s){p=abs(p)-s;return length(max(p.xy,0.))+min(max(p.x,p.y),0.)-.03;}
vec3 camPath(float t){
    return vec3(sin(t*.131)*5.+sin(t*.197)*5.+sin(t*.32)*3.,sin(t*.107)*4.+sin(t*.104)*4.+sin(t*.317)*3.,t);
}
float map(vec3 p){
    float d=0.,ld=0.,s=1.;
    float t=min(sdBeam(p-camPath(p.z),.5),sdBeam(p-camPath(p.z+60.),1.5));
    t=min(t,sdBeam(p-camPath(p.z+280.),4.));
    ld=max(-sdCubeField(p+15.,4.95,30.),d);
    for(int i=0;i<OCT;i++){
        vec3 q=vec3(sin(s*180.)*50.,sin(s*190.)*50.,sin(s*154.)*50.);
        d=max(-sdCubeField(p+q,s*.3,s),ld);
        ld=d;s*=1.;p*=s*vec3(0.801,1.003,0.901);
    }
    d=max(-t,d);
    return d;
}



// "Log-Bisection Tracing" by Nimitz
// https://www.shadertoy.com/view/4sSXzD
float trace(vec3 ro,vec3 rd,out float n){
    float t=NEAR,lt=t;
    vec3 p=ro+rd*t;
    float d=map(p);
    bool s=d>0.?true:false;
    bool b=false;
    for(int i=ZERO;i<ITER;i++){
        if(abs(d)<EPS*t||t>FAR)break;
        if((d>0.)!=s){b=true;break;}
        lt=t;
        t+=d>.5?d*.5:log(abs(d)+1.);
        p=ro+rd*t;
        d=map(p);
        n+=.6;
    }
    if(b){
        float m=0.;
        p=ro+lt*rd;
        s=map(p)>0.?true:false;;
        for(int i=ZERO;i<6;i++){
            m=(lt+t)*.5;
            p=ro+rd*m;
            d=map(p);
            if(abs(d)<EPS*t)break;
            (d<0.)==s?t=m:lt=m;
        }
        t=(lt+t)*.5;
    }
    return t;
}

//MAIN
void mainImage( out vec4 fragColor, in vec2 fragCoord ){
    vec2 uv=(fragCoord-.5*iResolution.xy)/iResolution.y;
    vec3 rd=vec3(uv,-.4);
    float z=-(iTime+CUE)*2.2;
    vec3 ro=camPath(z);
    vec3 cr=normalize(camPath(z-2.5)-camPath(z-3.));
    rd.xz*=rot(cr.x*1.8);
    rd.yz*=rot(cr.y*1.5);
    float n=0.,v=trace(ro+vec3(0,-0.2,0),rd,n)/FAR;n/=float(ITER);
    fragColor=vec4(mix(hsv(1.-n,1.-n,v),vec3(1),n),1);
}

//VR
void mainVR(out vec4 fragColor,in vec2 fragCoord,in vec3 fragRayOri,in vec3 fragRayDir){
    vec2 uv=(fragCoord.xy-vec2(.25,.5)*iResolution.xy)/iResolution.y;
    float z=-(iTime+CUE)*2.2;
    vec3 rd=fragRayDir;
    vec3 ro=camPath(z)+fragRayOri;
    vec3 cr=normalize(camPath(z-2.5)-camPath(z-3.));
    rd.xz*=rot(cr.x*1.8);
    rd.yz*=rot(cr.y*1.5);
    float n=0.,v=trace(ro+vec3(0,-0.2,0),rd,n)/FAR;n/=float(ITER);
    fragColor=vec4(mix(hsv(1.-n,1.-n,v),vec3(1),n),1);
}

