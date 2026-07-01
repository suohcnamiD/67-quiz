<script setup lang="ts">
import { onMounted, onUnmounted, useTemplateRef, ref } from 'vue'

const canvas = useTemplateRef<HTMLCanvasElement>('canvas')
const visible = ref(false)

onMounted(() => {
  const el = canvas.value!

  function syncSize() {
    const w = el.clientWidth || 1280
    const h = el.clientHeight || 720
    if (el.width !== w || el.height !== h) {
      el.width = w
      el.height = h
    }
  }

  const ro = new ResizeObserver(syncSize)
  ro.observe(el)
  syncSize()

  const gl = (el.getContext('webgl') ?? el.getContext('experimental-webgl')) as WebGLRenderingContext | null
  if (!gl) return
  const ctx = gl

  const vs = `attribute vec2 a_position;
varying vec2 v_texCoord;
void main() {
  v_texCoord = a_position * 0.5 + 0.5;
  gl_Position = vec4(a_position, 0.0, 1.0);
}`

  const fs = `precision highp float;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_mouse;
varying vec2 v_texCoord;

void main() {
    vec2 uv = v_texCoord;
    float time = u_time * 0.2;

    float f = 0.0;
    f += 0.5000 * sin(uv.x * 4.0 + time);
    f += 0.2500 * sin(uv.y * 3.0 + time * 1.2);
    f += 0.1250 * sin((uv.x + uv.y) * 5.0 + time * 0.8);

    vec2 m = u_mouse / u_resolution;
    float dist = distance(uv, m);
    f += 0.1 * (1.0 - smoothstep(0.0, 0.5, dist));

    vec3 color_bg = vec3(0.05, 0.0, 0.0);
    vec3 color_accent = vec3(0.72, 0.11, 0.11);

    vec3 final_color = mix(color_bg, color_accent * 0.4, 0.5 + 0.5 * sin(f + time));

    float vignette = 1.0 - smoothstep(0.5, 1.5, length(uv - 0.5));
    final_color *= vignette;

    gl_FragColor = vec4(final_color, 1.0);
}`

  function createShader(type: number, src: string) {
    const s = ctx.createShader(type)!
    ctx.shaderSource(s, src)
    ctx.compileShader(s)
    return s
  }

  const prog = ctx.createProgram()!
  ctx.attachShader(prog, createShader(ctx.VERTEX_SHADER, vs))
  ctx.attachShader(prog, createShader(ctx.FRAGMENT_SHADER, fs))
  ctx.linkProgram(prog)
  ctx.useProgram(prog)

  const buf = ctx.createBuffer()
  ctx.bindBuffer(ctx.ARRAY_BUFFER, buf)
  ctx.bufferData(ctx.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), ctx.STATIC_DRAW)

  const pos = ctx.getAttribLocation(prog, 'a_position')
  ctx.enableVertexAttribArray(pos)
  ctx.vertexAttribPointer(pos, 2, ctx.FLOAT, false, 0, 0)

  const uTime = ctx.getUniformLocation(prog, 'u_time')
  const uRes = ctx.getUniformLocation(prog, 'u_resolution')
  const uMouse = ctx.getUniformLocation(prog, 'u_mouse')

  const mouse = { x: el.width / 2, y: el.height / 2 }

  function onMouseMove(e: MouseEvent) {
    const rect = el.getBoundingClientRect()
    if (!rect.width || !rect.height) return
    mouse.x = ((e.clientX - rect.left) / rect.width) * el.width
    mouse.y = (1 - (e.clientY - rect.top) / rect.height) * el.height
  }
  window.addEventListener('mousemove', onMouseMove)

  let raf = 0
  let firstFrame = true
  function render(t: number) {
    syncSize()
    ctx.viewport(0, 0, el.width, el.height)
    ctx.uniform1f(uTime, t * 0.001)
    ctx.uniform2f(uRes, el.width, el.height)
    ctx.uniform2f(uMouse, mouse.x, mouse.y)
    ctx.drawArrays(ctx.TRIANGLE_STRIP, 0, 4)
    if (firstFrame) {
      firstFrame = false
      visible.value = true
    }
    raf = requestAnimationFrame(render)
  }
  raf = requestAnimationFrame(render)

  onUnmounted(() => {
    cancelAnimationFrame(raf)
    ro.disconnect()
    window.removeEventListener('mousemove', onMouseMove)
  })
})
</script>

<template>
  <canvas ref="canvas" class="shader-bg" :class="{ 'shader-bg--visible': visible }" />
</template>

<style scoped>
.shader-bg {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  display: block;
  z-index: 0;
  opacity: 0;
  transition: opacity 1.2s ease;
  /* Decorative background — never intercept clicks, focus, or scroll. */
  pointer-events: none;
}
.shader-bg--visible {
  opacity: 1;
}
</style>
