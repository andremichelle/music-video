/*
    https://www.shadertoy.com/view/wtfBDf
*/


#define SKEW_GRID
#define FAR 20.

float objID;

mat2 rot2(in float a){ float c = cos(a), s = sin(a); return mat2(c, -s, s, c); }

float hash21(vec2 p){  return fract(sin(dot(p, vec2(27.609, 57.583)))*43758.5453); }

float hash31(vec3 p){
    return fract(sin(dot(p, vec3(12.989, 78.233, 57.263)))*43758.5453);
}

vec2 path(in float z){
    return vec2(3.*sin(z*.1) + .5*cos(z*.4), 0);
}

vec3 getTex(in vec2 p){
    vec3 tx = vec3(cos(p.xy)*sin(p.x), sin(p.y)+cos(p.x)); // Me
    //vec3 tx = textureLod(iChannel0, p, 0.).xyz;
    return abs(tx);
}

// Height map value, which is just the pixel's greyscale value.
float hm(in vec2 p){ return dot(getTex(p), vec3(.299, .587, .114)); }


// IQ's extrusion formula.
float opExtrusion(in float sdf, in float pz, in float h, in float sf){

    // Slight rounding. A little nicer, but slower.
    vec2 w = vec2( sdf, abs(pz) - h - sf/2.);
    return min(max(w.x, w.y), 0.) + length(max(w + sf, 0.)) - sf;
}

/*
// IQ's unsigned box formula.
float sBoxSU(in vec2 p, in vec2 b, in float sf){

  return length(max(abs(p) - b + sf, 0.)) - sf;
}
*/

// IQ's signed box formula.
float sBoxS(in vec2 p, in vec2 b, in float sf){

    p = abs(p) - b + sf;
    return length(max(p, 0.)) + min(max(p.x, p.y), 0.) - sf;
}

// Skewing coordinates. "s" contains the X and Y skew factors.
vec2 skewXY(vec2 p, vec2 s){

    return mat2(1, -s.y, -s.x, 1)*p;
}

// Unskewing coordinates. "s" contains the X and Y skew factors.
vec2 unskewXY(vec2 p, vec2 s){

    return inverse(mat2(1, -s.y, -s.x, 1))*p;
}

vec2 gP;

vec4 blocks(vec3 q){
    const vec2 scale = vec2(1./5.);

    // Block dimension: Length to height ratio with additional scaling.
    const vec2 dim = scale;
    // A helper vector, but basically, it's the size of the repeat cell.
    const vec2 s = dim*2.;


    #ifdef SKEW_GRID
    // Skewing half way along X, and Y.
    const vec2 sk = vec2(-.5, .5);
    #else
    const vec2 sk = vec2(0);
    #endif

    // Distance.
    float d = 1e5;
    // Cell center, local coordinates and overall cell ID.
    vec2 p, ip;

    // Individual brick ID.
    vec2 id = vec2(0);
    vec2 cntr = vec2(0);

    // Four block corner postions.
    const vec2[4] ps4 = vec2[4](vec2(-.5, .5), vec2(.5), vec2(.5, -.5), vec2(-.5));

    // Height scale.
    #ifdef FLAT_GRID
    const float hs = 0.; // Zero height pylons for the flat grid.
    #else
    const float hs = .4;
    #endif

    float height = 0.; // Pylon height.


    // Local cell coordinate copy.
    gP = vec2(0);

    for(int i = 0; i<4; i++){

        // Block center.
        cntr = ps4[i]/2. -  ps4[0]/2.;

        // Skewed local coordinates.
        p = skewXY(q.xz, sk);
        ip = floor(p/s - cntr) + .5; // Local tile ID.
        p -= (ip + cntr)*s; // New local position.

        // Unskew the local coordinates.
        p = unskewXY(p, sk);

        // Correct positional individual tile ID.
        vec2 idi = ip + cntr;


        // Unskewing the rectangular cell ID.
        idi = unskewXY(idi*s, sk);


        // The larger grid cell face.
        //
        vec2 idi1 = idi; // Block's central position, and ID.
        float h1 = hm(idi1);
        #ifdef QUANTIZE_HEIGHTS
        h1 = floor(h1*20.999)/20.; // Discreet height units.
        #endif
        h1 *= hs; // Scale the height.

        // Larger face and height extrusion.
        float face1 = sBoxS(p, 2./5.*dim - .02*scale.x, .015);
        //float face1 = length(p) - 2./5.*dim.x;
        float face1Ext = opExtrusion(face1, q.y + h1, h1, .006);


        // The second, smaller face.
        //
        //vec2 offs = vec2(3./5., -1./5.)*dim;
        vec2 offs = unskewXY(dim*.5, sk);
        vec2 idi2 = idi + offs;  // Block's central position, and ID.
        float h2 = hm(idi2);
        #ifdef QUANTIZE_HEIGHTS
        h2 = floor(h2*20.999)/20.; // Discreet height units.
        #endif
        h2 *= hs; // Scale the height.

        // Smaller face and height extrusion.
        float face2 = sBoxS(p - offs, 1./5.*dim - .02*scale.x, .015);
        //float face2 = length(p - offs) - 1./5.*dim.x;
        float face2Ext = opExtrusion(face2, q.y + h2, h2, .006);

        // Pointed face tips, for an obelisque look, but I wasn't feeling it. :)
        //face1Ext += face1*.25;
        //face2Ext += face2*.25;

        vec4 di = face1Ext<face2Ext? vec4(face1Ext, idi1, h1) : vec4(face2Ext, idi2, h2);



        // If applicable, update the overall minimum distance value,
        // ID, and box ID. 
        if(di.x<d){
            d = di.x;
            id = di.yz;
            height = di.w;

            // Setting the local coordinates: This is hacky, but I needed a 
            // copy for the rendering portion, so put this in at the last minute.
            gP = p;

        }

    }

    // Return the distance, position-based ID and pylong height.
    return vec4(d, id, height);
}

float getTwist(float z){ return z*.08; }



// Block ID -- It's a bit lazy putting it here, but it works. :)
vec3 gID;

// Speaking of lazy, here's some global glow variables. :D
// Glow: XYZ is for color (unused), and W is for individual 
// blocks.
vec4 gGlow = vec4(0);

// The extruded image.
float map(vec3 p){

    // Wrap the scene around the path. This mutates the geometry,
    // but it's easier to implement. By the way, it's possible to
    // snap the geometry around the path, and I've done that in
    // other examples.
    p.xy -= path(p.z);

    // Twist the geometry along Z. It's cheap and visually effective.
    // Demosceners having been doing this for as long as I can remember.
    p.xy *= rot2(getTwist(p.z));


    // Turning one plane into two. It's an old trick.
    p.y = abs(p.y) - 1.25;

    // There are gaps between the pylons, so a floor needs to go in
    // to stop light from getting though.
    float fl = -p.y + .01;

    #ifdef PTH_INDPNT_GRD
    // Keep the blocks independent of the camera movement, but still 
    // twisting with warped space.
    p.xy += path(p.z);
    #endif

    // The extruded blocks.
    vec4 d4 = blocks(p);
    gID = d4.yzw; // Individual block ID.

    // Only alowing certain blocks to glow. We're including some 
    // animation in there as well.
    float rnd = hash21(gID.xy);
    //
    // Standard blinking lights animation.
    gGlow.w = smoothstep(.992, .997, sin(rnd*6.2831 + iTime/4.)*.5 + .5);
    //gGlow.w = rnd>.05? 0. : 1.; // Static version.


    // Overall object ID.
    objID = fl<d4.x? 1. : 0.;

    // Combining the floor with the extruded blocks.
    return min(fl, d4.x);

}


// Basic raymarcher.
float trace(in vec3 ro, in vec3 rd){

    // Overall ray distance and scene distance.
    float t = 0., d;

    // Zero out the glow.
    gGlow = vec4(0);

    // Random dithering -- This is on the hacky side, but we're trying to cheap out 
    // on the glow by calculating it inside the raymarching loop instead of it's 
    // own one. If the the jump off point was too close to the closest object in the
    // scene, you wouldn't do this.
    t = hash31(ro.zxy + rd.yzx)*.25;

    for(int i = 0; i<128; i++){

        d = map(ro + rd*t); // Distance function.

        // Adding in the glow. There'd be better and worse ways to do it.
        float ad = abs(d + (hash31(ro + rd) - .5)*.05);
        const float dst = .25;
        if(ad<dst){
            gGlow.xyz += gGlow.w*(dst - ad)*(dst - ad)/(1. + t);
        }

        // Note the "t*b + a" addition. Basically, we're putting less emphasis on accuracy, as
        // "t" increases. It's a cheap trick that works in most situations... Not all, though.
        if(abs(d)<.001*(1. + t*.05) || t>FAR) break; // Alternative: 0.001*max(t*.25, 1.), etc.

        t += i<32? d*.4 : d*.7;
        //t += d*.5; 
    }

    return min(t, FAR);
}


// Standard normal function. It's not as fast as the tetrahedral calculation, but more symmetrical.
vec3 getNormal(in vec3 p){

    const vec2 e = vec2(.001, 0);

    //vec3 n = normalize(vec3(map(p + e.xyy) - map(p - e.xyy),
    //map(p + e.yxy) - map(p - e.yxy),	map(p + e.yyx) - map(p - e.yyx)));

    // This mess is an attempt to speed up compiler time by contriving a break... It's 
    // based on a suggestion by IQ. I think it works, but I really couldn't say for sure.
    float sgn = 1.;
    float mp[6];
    vec3[3] e6 = vec3[3](e.xyy, e.yxy, e.yyx);
    for(int i = 0; i<6; i++){
        mp[i] = map(p + sgn*e6[i/2]);
        sgn = -sgn;
        if(sgn>2.) break; // Fake conditional break;
    }

    return normalize(vec3(mp[0] - mp[1], mp[2] - mp[3], mp[4] - mp[5]));
}



// Cheap shadows are hard. In fact, I'd almost say, shadowing particular scenes with limited 
// iterations is impossible... However, I'd be very grateful if someone could prove me wrong. :)
float softShadow(vec3 ro, vec3 lp, vec3 n, float k){

    // More would be nicer. More is always nicer, but not really affordable... Not on my slow test machine, anyway.
    const int iter = 24;

    ro += n*.0015;
    vec3 rd = lp - ro; // Unnormalized direction ray.


    float shade = 1.;
    float t = 0.;//.0015; // Coincides with the hit condition in the "trace" function.  
    float end = max(length(rd), 0.0001);
    //float stepDist = end/float(maxIterationsShad);
    rd /= end;

    // Max shadow iterations - More iterations make nicer shadows, but slow things down. Obviously, the lowest 
    // number to give a decent shadow is the best one to choose. 
    for (int i = 0; i<iter; i++){

        float d = map(ro + rd*t);
        shade = min(shade, k*d/t);
        //shade = min(shade, smoothstep(0., 1., k*h/dist)); // Subtle difference. Thanks to IQ for this tidbit.
        // So many options here, and none are perfect: dist += min(h, .2), dist += clamp(h, .01, stepDist), etc.
        t += clamp(d, .01, .25);


        // Early exits from accumulative distance function calls tend to be a good thing.
        if (d<0. || t>end) break;
    }

    // Sometimes, I'll add a constant to the final shade value, which lightens the shadow a bit --
    // It's a preference thing. Really dark shadows look too brutal to me. Sometimes, I'll add 
    // AO also just for kicks. :)
    return max(shade, 0.);
}


// I keep a collection of occlusion routines... OK, that sounded really nerdy. :)
// Anyway, I like this one. I'm assuming it's based on IQ's original.
float calcAO(in vec3 p, in vec3 n)
{
    float sca = 3., occ = 0.;
    for( int i = 0; i<5; i++ ){

        float hr = float(i + 1)*.15/5.;
        float d = map(p + n*hr);
        occ += (hr - d)*sca;
        sca *= .7;
    }

    return clamp(1. - occ, 0., 1.);
}

/*
// Compact, self-contained version of IQ's 3D value noise function. I have a transparent noise
// example that explains it, if you require it.
float n3D(in vec3 p){
    
	const vec3 s = vec3(7, 157, 113);
	vec3 ip = floor(p); p -= ip; 
    vec4 h = vec4(0., s.yz, s.y + s.z) + dot(ip, s);
    p = p*p*(3. - 2.*p); //p *= p*p*(p*(p * 6. - 15.) + 10.);
    h = mix(fract(sin(h)*43758.5453), fract(sin(h + s.x)*43758.5453), p.x);
    h.xy = mix(h.xz, h.yw, p.y);
    return mix(h.x, h.y, p.z); // Range: [0, 1].
}

// Very basic pseudo environment mapping... and by that, I mean it's fake. :) However, it 
// does give the impression that the surface is reflecting the surrounds in some way.
//
// More sophisticated environment mapping:
// UI easy to integrate - XT95    
// https://www.shadertoy.com/view/ldKSDm
vec3 envMap(vec3 p){
    
    p *= 6.;
    p.y += iTime;
    
    float n3D2 = n3D(p*2.);
   
    // A bit of fBm.
    float c = n3D(p)*.57 + n3D2*.28 + n3D(p*4.)*.15;
    c = smoothstep(.45, 1., c); // Putting in some dark space.
    
    p = vec3(c, c*c, c*c*c*c); // Bluish tinge.
    
    return mix(p, p.xzy, n3D2*.4); // Mixing in a bit of purple.

}
*/


void mainImage( out vec4 fragColor, in vec2 fragCoord ){


    // Screen coordinates.
    vec2 uv = (fragCoord - iResolution.xy*.5)/iResolution.y;

    // Camera Setup.
    vec3 ro = vec3(0, 0, iTime*1.5); // Camera position, doubling as the ray origin.
    ro.xy += path(ro.z);
    vec2 roTwist = vec2(0, 0);
    roTwist *= rot2(-getTwist(ro.z));
    ro.xy += roTwist;

    vec3 lk = vec3(0, 0, ro.z + .25); // "Look At" position.
    lk.xy += path(lk.z);
    vec2 lkTwist = vec2(0, -.1); // Only twist horizontal and vertcal.
    lkTwist *= rot2(-getTwist(lk.z));
    lk.xy += lkTwist;

    vec3 lp = vec3(0, 0, ro.z + 3.); // Light.
    lp.xy += path(lp.z);
    vec2 lpTwist = vec2(0, -.3); // Only twist horizontal and vertcal.
    lpTwist *= rot2(-getTwist(lp.z));
    lp.xy += lpTwist;



    // Using the above to produce the unit ray-direction vector.
    float FOV = 1.; // FOV - Field of view.
    float a = getTwist(ro.z);
    // Swiveling the camera about the XY-plane.
    a += (path(ro.z).x - path(lk.z).x)/(ro.z - lk.z)/4.;
    vec3 fw = normalize(lk - ro);
    //vec3 up = normalize(vec3(-fw.x, 0, -fw.z));
    vec3 up = vec3(sin(a), cos(a), 0);
    //vec3 up = vec3(0, 1, 0);
    vec3 cu = normalize(cross(up, fw));
    vec3 cv = cross(fw, cu);

    // Unit direction ray.
    vec3 rd = normalize(uv.x*cu + uv.y*cv + fw/FOV);


    // Raymarch to the scene.
    float t = trace(ro, rd);

    // Save the block ID, object ID and local coordinates.
    vec3 svGID = gID;
    float svObjID = objID;
    vec2 svP = gP;

    vec3 svGlow = gGlow.xyz;


    // Initiate the scene color to black.
    vec3 col = vec3(0);

    // The ray has effectively hit the surface, so light it up.
    if(t < FAR){


        // Surface position and surface normal.
        vec3 sp = ro + rd*t;
        //vec3 sn = getNormal(sp, edge, crv, ef, t);
        vec3 sn = getNormal(sp);


        // Texel color. 
        vec3 texCol;

        // Transforming the texture coordinates according to the camera path
        // and Z warping.
        vec3 txP = sp;
        txP.xy -= path(txP.z);
        txP.xy *= rot2(getTwist(txP.z));
        #ifdef PTH_INDPNT_GRD
        txP.xy += path(txP.z);
        #endif

        // The extruded grid.
        if(svObjID<.5){

            // Coloring the individual blocks with the saved ID.
            vec3 tx = getTex(svGID.xy);

            // Ramping the shade up a bit.
            texCol = smoothstep(-.5, 1., tx)*vec3(1, .8, 1.8);


            // Very fake, but very cheap, bump mapping. Render some equispaced horizontal
            // dark lines, and some light adjacent ones. As you can see, it gives the
            // impression of horizontally segmented grooves on the pylons.
            const float lvls = 8.;

            // Vertical lines... A bit too much for this example, but useful for a fake
            // voxel setup.
            //float vLn = min(abs(txP.x - svGID.x), abs(txP.z - svGID.y));

            // Horizontal lines (planes, technically) around the pylons.
            float yDist = (1.25 + abs(txP.y) + svGID.z*2.);
            float hLn = abs(mod(yDist  + .5/lvls, 1./lvls) - .5/lvls);
            float hLn2 = abs(mod(yDist + .5/lvls - .008, 1./lvls) - .5/lvls);

            // Omitting the top and bottom planes... I was in a hurry, and it seems to
            // work, but there'd be better ways to do this. 
            if(yDist - 2.5<.25/lvls) hLn = 1e5;
            if(yDist - 2.5<.25/lvls) hLn2 = 1e5;

            // Rendering the dark and light lines using 2D layering techniques.
            texCol = mix(texCol, texCol*2., 1. - smoothstep(0., .003, hLn2 - .0035));
            texCol = mix(texCol, texCol/2.5, 1. - smoothstep(0., .003, hLn - .0035));


            // Render a dot on the face center of each extruded block for whatever reason...
            // They were there as markers to begin with, so I got used to them. :)
            float fDot = length(txP.xz - svGID.xy) - .0086;
            texCol = mix(texCol, texCol*2., 1. - smoothstep(0., .005, fDot - .0035));
            texCol = mix(texCol, vec3(0), 1. - smoothstep(0., .005, fDot));



        }
        else {

            // The dark floor in the background. Hidden behind the pylons, but
            // there are very slight gaps, so it's still necessary.
            texCol = vec3(0);
        }


        // Light direction vector.
        vec3 ld = lp - sp;

        // Distance from respective light to the surface point.
        float lDist = max(length(ld), .001);

        // Normalize the light direction vector.
        ld /= lDist;


        // Shadows and ambient self shadowing.
        float sh = softShadow(sp, lp, sn, 16.);
        float ao = calcAO(sp, sn); // Ambient occlusion.
        sh = min(sh + ao*.25, 1.);

        // Light attenuation, based on the distances above.
        float atten = 3./(1. + lDist*lDist*.5);


        // Diffuse lighting.
        float diff = max( dot(sn, ld), 0.);
        diff *= diff*1.35; // Ramping up the diffuse.

        // Specular lighting.
        float spec = pow(max(dot(reflect(ld, sn), rd ), 0.), 32.);

        // Fresnel term. Good for giving a surface a bit of a reflective glow.
        float fre = pow(clamp(1. - abs(dot(sn, rd))*.5, 0., 1.), 4.);

        // Schlick approximation. I use it to tone down the specular term. It's pretty subtle,
        // so could almost be aproximated by a constant, but I prefer it. Here, it's being
        // used to give a hard clay consistency... It "kind of" works.
        //float Schlick = pow( 1. - max(dot(rd, normalize(rd + ld)), 0.), 5.);
        //float freS = mix(.15, 1., Schlick);  //F0 = .2 - Glass... or close enough.

        // Combining the above terms to procude the final color.
        col = texCol*(diff + ao*.25 + vec3(1, .4, .2)*fre*.25 + vec3(1, .4, .2)*spec*4.);


        // Fake environmental lighting: Interesting, but I couldn't justify it, both
        // from a visual and logical standpoint.
        //vec3 cTex = envMap(reflect(rd, sn)); // Be sure to uncomment the function above.
        //col += col*cTex.zyx*4.;


        // Shading.
        col *= ao*sh*atten;

    }


    // Applying the glow -- You perform this outside the hit logic block. The reason
    // I mention this is that I make this mistake all the time and spend ages trying
    // to figure out why it's not working. :) As for how you apply it, that's up to
    // you. I made the following up, and I'd imagine there'd be nicer ways to apply 
    // it, but it'll do.
    svGlow.xyz *= mix(vec3(4, 1, 2), vec3(4, 2, 1), min(svGlow.xyz*3.5, 1.25));
    col *= .25 + svGlow.xyz*8.;

    // Some colorful fog: Like the above, it's been tweaked to produce something
    // colorful that, hopefully, helps the scene. The cool thing about fog is that
    // it's about as cheap an operation as you could hope for, but has virtually
    // no impact on the frame rate. With that in mind, it's definitely worth taking
    // the time to get it looking the way you'd like it to look.
    vec3 fog =  mix(vec3(4, 1, 2), vec3(4, 2, 1), rd.y*.5 + .5);
    fog = mix(fog, fog.zyx, smoothstep(0., .35, uv.y - .35));
    col = mix(col, fog/1.5, smoothstep(0., .99, t*t/FAR/FAR));


    #ifdef GRAYSCALE
    // Grayscale... or almost grayscale. :)
    col = mix(col, vec3(1)*dot(col, vec3(.299, .587, .114)), .75);
    #endif


    #ifdef REVERSE_PALETTE
    col = col.zyx; // A more calming blue, for those who don't like fiery things.
    #endif
    fragColor = vec4(sqrt(max(col, 0.)), 1);
}