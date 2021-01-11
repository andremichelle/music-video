// https://www.shadertoy.com/view/WtcyDM

// cyclic noise learned from nimitz
// it is literally invented by nimitz

#define pi acos(-1.)
#define rot(a) mat2(cos(a),-sin(a),sin(a),cos(a))

#define pmod(p,a) (mod(p - 0.5*a,a) - 0.5*a)

vec3 glow = vec3(0);
vec3 ro;


#define xor(a,b,c) min(max(a,-(b)), max(-(a) + c,b))

vec2 map(vec3 po){
    vec2 d = vec2(10e5);

    vec4 p = vec4(po,1.);

    p.xz -= 2.;

    p = abs(p);
    p.xz -= 2.;

    p.y = pmod(p.y,5.);
    for(int i = 0; i <6; i++){


        p.xyz = pmod(p.xyz,vec3(5,4,5));


        p = abs(p);

        if(p.x - p.y < 0.) p.xy = p.yx;
        if(p.x - p.z < 0.) p.xz = p.zx;
        if(p.z - p.y < 0.) p.zy = p.yz;



        if(i == 5 || i == 3){
            p.y -= .6 + sin(iTime*0.4)*1.4;
            float dpp = dot(p.xyz,p.xyz);
            dpp = clamp(dpp,0.,0.2 + sin(iTime)*0.05);
            p = p/dpp;
        }


        p.xy *= rot(-0.25*pi);
        p.xy -= 0.5;
        p *= 1.4 + sin(iTime*0.5)*0.3;

        float ld = max(p.z,p.x)/p.w - 0.002;

        d.x = xor(-d.x, ld,0.4);

    }

    p.xyz /= p.w;



    p = abs(p);

    glow += exp(-d.x*40.);

    d.x *= 0.7;

    d.x = max(d.x,-length(po.xz) + 0.4);
    d.x = mix(d.x,0.1,smoothstep(0.2,0.,length(po-ro)));
    return d;
}


mat3 getOrthogonalBasis(vec3 lookAt){
    vec3 dir = normalize(lookAt);
    vec3 right = normalize(cross(vec3(0,1,0),dir));
    vec3 up = normalize(cross(dir, right));
    return mat3(right,up,dir);
}


float cyclicNoise(vec3 p){
    float noise = 0.;

    vec3 seed = vec3(-4. ,-2.,0.5);

    float amp = 1.;
    float gain = 0.6;
    float lacunarity = 1.4;
    int octaves = 5;


    float warp = 0.3+ sin(iTime)*0.;
    float warpTrk = 1.2 ;
    float warpTrkGain = 1.5;

    mat3 rotMatrix = getOrthogonalBasis(seed);

    for(int i = 0; i < octaves; i++){
        // Some domain warping. Similar to fbm.
        p += sin(p.zxy*warpTrk*0.2 - 2.*warpTrk)*warp;
        // Calculate some noise value. 
        noise += sin(dot(cos(p), sin(p.zxy )))*amp;
        //noise += abs(sin(dot(cos(p), sin(p.zxy ))))*amp;

        p *= rotMatrix;
        p *= lacunarity;

        warpTrk *= warpTrkGain;
        amp *= gain;
    }

    return noise*0.5;
}


float mapCloud(vec3 p){

    vec3 op = p;
    float d = length(p.xz);
    p *= 12.;

    p.y -= iTime*4.;

    float n = cyclicNoise(p)*0.4;

    d -= sin(length(op.y)*4. + iTime*2.)*0.04;
    d -= 0.4 +  n*0.3;
    d = smoothstep(0.05,0.,d)*3.;
    return d;
}




vec3 getRd(vec3 ro, vec3 lookAt, vec2 uv){
    vec3 dir = normalize(lookAt - ro);
    vec3 right = normalize(cross(vec3(0,1,0),dir));
    vec3 up = normalize(cross(dir, right));

    return normalize(dir + right*uv.x + up*uv.y);
}
vec3 getNormal(vec3 p){
    vec2 t = vec2(0.001,0.);
    return normalize(map(p).x - vec3(
    map(p - t.xyy).x,
    map(p - t.yxy).x,
    map(p - t.yyx).x
    ));
}

float sdBox(vec2 p){
    p = abs(p); return max(p.y,p.x);
}
void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = (fragCoord - 0.5*iResolution.xy)/iResolution.y;

    float db = 10e5;
    vec2 buv = uv;
    /*
    for(int i = 0; i < 5; i++){
        float bx = abs(sdBox(buv) - 0.4) - .54;
        
        buv = pmod(buv, 1.4);
        buv = abs(buv);
        if(buv.x + buv.y < 0.) buv.xy = buv.yx;
        if(buv.x - buv.y < 0.) buv.xy = buv.yx;
        buv.x -= 0.04 + sin(iTime)*.4;
        buv *= rot(0.25*pi );
        
        db = xor(db,bx,0.4);
    
    }
    
    db -= 0.2;
    if (db < 0.){
        uv = uv.yx;
    } 
    
    */
    buv = uv + 1000.;
    vec3 col = vec3(0);
    ro = vec3(0);
    ro.xz += vec2(sin(iTime*0.5),cos(iTime));

    ro = normalize(ro)*( 0.75 + length(vec2(sin(iTime*0.6),cos(iTime*0.4))*0.5));
    ro.y += iTime*0.2;

    vec3 lookAt = vec3(0);

    lookAt.y = ro.y;

    lookAt.y += length(vec2(sin(iTime*0.6),cos(iTime*0.4))*0.5)*2. - 1.;

    vec3 rd = getRd(ro, lookAt, uv);

    vec3 sunDir = normalize(vec3(1));


    vec3 p = ro;
    float t = 0.; bool hit = false;
    vec2 d;
    float marchi = 0.;
    for(; marchi < 90.; marchi++){
        d = map(p);

        if(d.x < 0.001){
            hit = true;
            break;
        }
        p = ro + rd*(t += d.x);
    }

    vec3 hitC = vec3(0);

    if(hit){
        vec3 n = getNormal(p);
        vec3 albedo = n + 0.5;

        #define ao(a) smoothstep(0.,1.,map(p + (n + sunDir*0.4)*a).x/a)

        hitC += albedo*0.;

        float diff = max(dot(n,sunDir),0.);

        float aof = ao(0.01)*ao(0.1)*ao(0.07)*2. + 0.1;
        hitC = mix(max(vec3(0.9,0.2,0.)*2. - sin(iTime*0.2) - 1.4,0.),hitC,diff);

        //hitC = mix(vec3(0.9,0.2,0.5)*0.9,hitC,diff);
        hitC = mix(vec3(0.1,0.2,0.5)*0.2,hitC,aof);
    }



    float volSteps = 7.;
    float volDist = min(t, 1.6);
    float volStSz = volDist/volSteps;
    vec3 volP = ro;
    vec3 volAccum = vec3(0);

    float tDens = 0.;


    for(float i = 0.; i < volSteps; i++){
        //float dens = pow(abs(sin(length(volP*25.)*1. + iTime)),12.)*1.44;
        //float dens = smoothstep(0.1,0., length(volP) - 0.3);
        float dens = mapCloud(volP);
        float odens = mapCloud(volP + sunDir*0.3);

        float diff = clamp(dens - odens*0.95, 0., 1.);
        vec3 absorption = mix(vec3(0.4,0.7,0.9),vec3(0.8,0.6,0.1)*0.2 ,clamp(tDens*.4, 0., 1.));
        vec3 fringe = mix(vec3(1.8,0.1,0.1)*1., vec3(0.4,0.7,0.9)*0.0, pow(clamp(dens*0.8 - 1.9, 0., 1.),0.2));

        vec3 c = mix(vec3(0.1,0.1,0.4)*0.3,vec3(0.6,0.3,0.2)*1.,diff);

        c *= absorption;

        dens = dens*(1. - tDens)*volStSz;

        volAccum += c*dens*1.5 + fringe*dens;



        tDens += dens;

        if(tDens > 1.){
            break;
        }

        volP += rd*volStSz;
    }
    //col += marchi*0.02*(0.5 + 0.4*sin(vec3(1.,4.8,4.8) + uv.xyx));

    col += hitC*1.;

    col = mix(col,vec3(0.04,0.01,0.1),smoothstep(0.,1.,marchi*0.01));

    //col += glow*.02*(smoothstep(1.,0.,t*0.06));


    col = mix(col + volAccum*0., volAccum*1., smoothstep(0.,1.,pow(tDens*1.,  1.)) );

    col = mix(col,smoothstep(0.,1.,col*1.5),0.4);


    col = 1. - col*(3. + sin(iTime));



    if (abs(db) < 0.4){
        col = 1. - col;
    }

    //col.xz *= rot(iTime);

    //vec3 rotAround = normalize(vec3(sin(iTime),cos(iTime*0.5),sin(iTime*0.4)));
    vec3 rotAround = normalize(vec3(-1.,1.,-1));

    mat3 matRotAround = mat3(
    rotAround.x, 0., 0.,
    0., rotAround.y, 0.,
    0., 0., rotAround.z
    );
    col += 1.;
    col *= matRotAround;

    //col.xy *= rot(-0.4+ sin(iTime)*0.4 + (uv.y)*0.2);
    //col.xy *= rot(sin(iTime)*0.2);

    col *= inverse(matRotAround);
    col -= 1.;

    col *= 1. - dot(uv,uv)*0.5;
    col = pow(abs(col), vec3(0.45454));

    fragColor = vec4(col,1.0);
}