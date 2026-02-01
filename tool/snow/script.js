const canvas = document.getElementById('glCanvas');
const gl = canvas.getContext('webgl');

if (!gl) {
    alert('WebGL not supported');
}

// Vertex Shader: Full-screen quad
const vsSource = `
    attribute vec4 aVertexPosition;
    void main() {
        gl_Position = aVertexPosition;
    }
`;

// Fragment Shader: The core snowflake logic using SDFs and Polar Folding
const fsSource = `
    precision highp float;

  uniform vec2 uResolution;
    uniform float uTime;
    uniform float uSeed;
    uniform float uComplexity;
    uniform float uThickness;
    uniform float uGlow;
    uniform vec2 uMouse;

    #define PI 3.14159265359

  // Fold space into 12 segments (6-fold symmetry + reflection)
    vec2 pFold(vec2 p) {
        float a = atan(p.y, p.x) + PI/6.0;
        float r = length(p);
        float segment = floor(a / (PI/3.0));
        a = mod(a, PI/3.0) - PI/6.0;
        a = abs(a);
        return vec2(cos(a), sin(a)) * r;
    }

    // SDF for a line segment
  float sdLine(vec2 p, vec2 a, vec2 b) {
        vec2 pa = p - a, ba = b - a;
        float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
      return length(pa - ba * h);
    }

    // Pseudo-random based on seed
    float hash(float n) {
        return fract(sin(n) * 43758.5453123);
    }

    void main() {
        vec2 uv = (gl_FragCoord.xy - 0.5 * uResolution.xy) / min(uResolution.y, uResolution.x);
        
        // Rotation based on time and mouse
      float rot = uTime * 0.05 + (uMouse.x - 0.5) * 2.0;
        float s = sin(rot), c = cos(rot);
        uv *= mat2(c, -s, s, c);

        // Apply 6-fold symmetry fold
        vec2 p = pFold(uv);

        float d = 1e10;
        float thickness = uThickness;

        // Main stem
      d = min(d, sdLine(p, vec2(0.0), vec2(0.4, 0.0)));

        // Procedural branches
        for (float i = 1.0; i <= 10.0; i++) {
            if (i > uComplexity) break;
            
            float h1 = hash(uSeed + i);
            float h2 = hash(uSeed * i + 0.5);
            float h3 = hash(uSeed - i * 2.0);

          float pos = 0.05 + i * 0.035; // Position along stem
          float len = 0.05 + h2 * 0.12; // Length of branch
          float ang = PI/3.0 * (0.4 + h3 * 0.4); // Angle of branch

            vec2 start = vec2(pos, 0.0);
            vec2 end = start + vec2(cos(ang), sin(ang)) * len;
            
            d = min(d, sdLine(p, start, end, thickness * (1.0 - pos*2.0)));
            
            // Sub-branches
            if (uComplexity > 4.0) {
                vec2 subStart = start + (end - start) * 0.5;
                vec2 subEnd = subStart + vec2(cos(ang + 0.5), sin(ang + 0.5)) * len * 0.5;
                d = min(d, sdLine(p, subStart, subEnd, thickness * 0.5));
            }
        }

        // Coloring and Glow
        float mask = smoothstep(0.002, 0.0, d);
      float glow = exp(-d * (15.0 + (1.0 - uGlow) * 80.0));
        
      vec3 baseColor = vec3(0.85, 0.95, 1.0);
        vec3 finalColor = mask * baseColor;
      finalColor += glow * uGlow * vec3(0.5, 0.8, 1.0);

        // Vignette
        finalColor *= 1.0 - length(uv) * 0.8;

        gl_FragColor = vec4(finalColor, 1.0);
    }
`;

function createShader(gl, type, source) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.error(gl.getShaderInfoLog(shader));
        gl.deleteShader(shader);
        return null;
    }
    return shader;
}

const vertexShader = createShader(gl, gl.VERTEX_SHADER, vsSource);
const fragmentShader = createShader(gl, gl.FRAGMENT_SHADER, fsSource);

const program = gl.createProgram();
gl.attachShader(program, vertexShader);
gl.attachShader(program, fragmentShader);
gl.linkProgram(program);

const positionBuffer = gl.createBuffer();
gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
const positions = [
    -1.0,  1.0,
     1.0,  1.0,
    -1.0, -1.0,
     1.0, -1.0,
];
gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STATIC_DRAW);

const state = {
  seed: Math.random() * 1000,
  complexity: 6,
  thickness: 0.004,
  glow: 0.2,
  mouseX: 0.5,
  mouseY: 0.5
};

// UI Listeners
document.getElementById('complexity').addEventListener('input', (e) => state.complexity = parseFloat(e.target.value));
document.getElementById('thickness').addEventListener('input', (e) => state.thickness = parseFloat(e.target.value));
document.getElementById('glow').addEventListener('input', (e) => state.glow = parseFloat(e.target.value));
document.getElementById('randomize').addEventListener('click', () => state.seed = Math.random() * 1000);
window.addEventListener('mousemove', (e) => {
    state.mouseX = e.clientX / window.innerWidth;
    state.mouseY = e.clientY / window.innerHeight;
});
window.addEventListener('mousedown', (e) => {
    if (e.target.tagName !== 'INPUT' && e.target.tagName !== 'BUTTON') {
      state.seed = Math.random() * 1000;
    }
});

function render(time) {
    time *= 0.001; // convert to seconds

    gl.viewport(0, 0, canvas.width, canvas.height);
  gl.clearColor(0.02, 0.04, 0.06, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT);

    gl.useProgram(program);

    const positionLocation = gl.getAttribLocation(program, 'aVertexPosition');
    gl.enableVertexAttribArray(positionLocation);
    gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0);

    // Set Uniforms
    gl.uniform2f(gl.getUniformLocation(program, 'uResolution'), canvas.width, canvas.height);
    gl.uniform1f(gl.getUniformLocation(program, 'uTime'), time);
    gl.uniform1f(gl.getUniformLocation(program, 'uSeed'), state.seed);
    gl.uniform1f(gl.getUniformLocation(program, 'uComplexity'), state.complexity);
    gl.uniform1f(gl.getUniformLocation(program, 'uThickness'), state.thickness);
    gl.uniform1f(gl.getUniformLocation(program, 'uGlow'), state.glow);
    gl.uniform2f(gl.getUniformLocation(program, 'uMouse'), state.mouseX, state.mouseY);

    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    requestAnimationFrame(render);
}

function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
}

window.addEventListener('resize', resize);
resize();
requestAnimationFrame(render);