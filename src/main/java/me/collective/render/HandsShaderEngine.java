package me.collective.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.awt.Color;
import java.nio.FloatBuffer;

public class HandsShaderEngine {
    public static final HandsShaderEngine INSTANCE = new HandsShaderEngine();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private int shaderProgram = -1;
    private int uSampler0 = -1;
    private int uSampler1 = -1;
    private int uTintColor = -1;
    private int uSecondColor = -1;
    private int uTime = -1;
    private int uMinDepth = -1;
    private int uMaxDepth = -1;
    private int uMode = -1;
    private int uGlow = -1;

    private int vao = -1;
    private int vbo = -1;

    private boolean initialized = false;
    private boolean shaderFailed = false;

    private static final String VERTEX_SHADER =
            "#version 150\n" +
            "in vec2 aPos;\n" +
            "out vec2 texCoord;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
            "    texCoord = aPos * 0.5 + 0.5;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 150\n" +
            "in vec2 texCoord;\n" +
            "out vec4 fragColor;\n" +
            "\n" +
            "uniform sampler2D Sampler0;\n" + // Color buffer
            "uniform sampler2D Sampler1;\n" + // Depth buffer
            "uniform vec4 TintColor;\n" +
            "uniform vec4 SecondColor;\n" +
            "uniform float Time;\n" +
            "uniform float MinDepth;\n" +
            "uniform float MaxDepth;\n" +
            "uniform int Mode;\n" + // 0: Cosmic Plasma, 1: Electric / Lightning, 2: Cyber Grid, 3: Hologram Smoke, 4: Pure Glow
            "uniform float Glow;\n" +
            "\n" +
            "float hash(vec2 p) {\n" +
            "    p = fract(p * vec2(123.34, 345.45));\n" +
            "    p += dot(p, p + 34.345);\n" +
            "    return fract(p.x * p.y);\n" +
            "}\n" +
            "\n" +
            "float noise(vec2 p) {\n" +
            "    vec2 i = floor(p);\n" +
            "    vec2 f = fract(p);\n" +
            "    f = f * f * (3.0 - 2.0 * f);\n" +
            "    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),\n" +
            "               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);\n" +
            "}\n" +
            "\n" +
            "float fbm(vec2 p) {\n" +
            "    float v = 0.0;\n" +
            "    float a = 0.5;\n" +
            "    for (int i = 0; i < 5; i++) {\n" +
            "        v += noise(p) * a;\n" +
            "        p = p * 2.02 + vec2(8.4, 5.7);\n" +
            "        a *= 0.5;\n" +
            "    }\n" +
            "    return v;\n" +
            "}\n" +
            "\n" +
            "float ridged(vec2 p) {\n" +
            "    float v = 0.0;\n" +
            "    float a = 0.55;\n" +
            "    for (int i = 0; i < 4; i++) {\n" +
            "        float r = 1.0 - abs(noise(p) * 2.0 - 1.0);\n" +
            "        v += r * a;\n" +
            "        p = p * 2.18 + vec2(3.1, 9.2);\n" +
            "        a *= 0.52;\n" +
            "    }\n" +
            "    return v;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = texCoord;\n" +
            "    float d = texture(Sampler1, uv).r;\n" +
            "\n" +
            "    // In Minecraft 1.21+, hand depth is rendered in [0.0, MaxDepth] range (typically < 0.005)\n" +
            "    // Discard any world background or distant pixels strictly\n" +
            "    if (d >= 0.9999 || d > MaxDepth) discard;\n" +
            "\n" +
            "    float mask = 1.0 - smoothstep(MinDepth, MaxDepth, d);\n" +
            "    if (mask <= 0.001) discard;\n" +
            "\n" +
            "    float t = Time;\n" +
            "    vec3 finalCol = TintColor.rgb;\n" +
            "    float pattern = 1.0;\n" +
            "\n" +
            "    if (Mode == 0) {\n" + // Cosmic Plasma
            "        vec2 flow = uv * 3.0;\n" +
            "        vec2 drift = vec2(t * 0.20, -t * 0.15);\n" +
            "        vec2 warp = vec2(fbm(flow + drift), fbm(flow - drift + vec2(3.2, 1.7)));\n" +
            "        vec2 q = flow + warp * 1.5;\n" +
            "        float n = fbm(q + drift);\n" +
            "        float veins = pow(clamp(ridged(q * 2.0 - drift * 0.5), 0.0, 1.0), 2.0);\n" +
            "        pattern = 0.4 + 0.6 * n + veins * Glow;\n" +
            "        finalCol = mix(TintColor.rgb, SecondColor.rgb, clamp(n + veins * 0.5, 0.0, 1.0));\n" +
            "    } else if (Mode == 1) {\n" + // Lightning / Electric
            "        vec2 flow = uv * 4.0;\n" +
            "        float zap1 = abs(sin((flow.x * 2.0 + flow.y * 3.0) + t * 4.0 + fbm(flow * 2.0 + t)));\n" +
            "        float zap2 = abs(sin((flow.x * -2.5 + flow.y * 2.0) - t * 3.5 + fbm(flow * 2.5 - t)));\n" +
            "        float bolt = pow(1.0 - min(zap1, zap2), 6.0);\n" +
            "        pattern = 0.3 + bolt * (1.5 + Glow * 2.0);\n" +
            "        finalCol = mix(TintColor.rgb, vec3(1.0), clamp(bolt * 1.2, 0.0, 1.0));\n" +
            "    } else if (Mode == 2) {\n" + // Cyber Grid / Wire
            "        vec2 grid = fract(uv * 40.0);\n" +
            "        float line = step(0.92, grid.x) + step(0.92, grid.y);\n" +
            "        float scan = sin(uv.y * 30.0 - t * 5.0) * 0.5 + 0.5;\n" +
            "        pattern = 0.25 + clamp(line * 0.8 + scan * 0.4, 0.0, 1.0) * (1.0 + Glow);\n" +
            "        finalCol = mix(TintColor.rgb, SecondColor.rgb, scan);\n" +
            "    } else if (Mode == 3) {\n" + // Hologram / Smoke
            "        vec2 flow = uv * 2.5 + vec2(0.0, -t * 0.6);\n" +
            "        float smoke = fbm(flow);\n" +
            "        float scan = sin(uv.y * 120.0 + t * 6.0) * 0.5 + 0.5;\n" +
            "        pattern = (0.35 + 0.65 * smoke) * (0.8 + 0.3 * scan);\n" +
            "        finalCol = mix(TintColor.rgb, SecondColor.rgb, smoke);\n" +
            "    } else {\n" + // Pure Glow
            "        pattern = 0.8 + 0.3 * sin(t * 2.5);\n" +
            "        finalCol = TintColor.rgb;\n" +
            "    }\n" +
            "\n" +
            "    float finalAlpha = clamp(TintColor.a * mask * pattern, 0.0, 1.0);\n" +
            "    if (finalAlpha <= 0.002) discard;\n" +
            "\n" +
            "    // Standard premultiplied / alpha output for blending\n" +
            "    fragColor = vec4(finalCol * finalAlpha, finalAlpha);\n" +
            "}\n";

    private void init() {
        if (initialized || shaderFailed) return;
        try {
            int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vert, VERTEX_SHADER);
            GL20.glCompileShader(vert);
            if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
                shaderFailed = true;
                return;
            }

            int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(frag, FRAGMENT_SHADER);
            GL20.glCompileShader(frag);
            if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
                shaderFailed = true;
                return;
            }

            shaderProgram = GL20.glCreateProgram();
            GL20.glAttachShader(shaderProgram, vert);
            GL20.glAttachShader(shaderProgram, frag);
            GL20.glBindAttribLocation(shaderProgram, 0, "aPos");
            GL20.glLinkProgram(shaderProgram);
            if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
                shaderFailed = true;
                return;
            }

            uSampler0 = GL20.glGetUniformLocation(shaderProgram, "Sampler0");
            uSampler1 = GL20.glGetUniformLocation(shaderProgram, "Sampler1");
            uTintColor = GL20.glGetUniformLocation(shaderProgram, "TintColor");
            uSecondColor = GL20.glGetUniformLocation(shaderProgram, "SecondColor");
            uTime = GL20.glGetUniformLocation(shaderProgram, "Time");
            uMinDepth = GL20.glGetUniformLocation(shaderProgram, "MinDepth");
            uMaxDepth = GL20.glGetUniformLocation(shaderProgram, "MaxDepth");
            uMode = GL20.glGetUniformLocation(shaderProgram, "Mode");
            uGlow = GL20.glGetUniformLocation(shaderProgram, "Glow");

            // Screen-filling triangle
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

            FloatBuffer buf = BufferUtils.createFloatBuffer(6);
            buf.put(new float[]{
                    -1.0f, -1.0f,
                     3.0f, -1.0f,
                    -1.0f,  3.0f
            }).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
            GL20.glEnableVertexAttribArray(0);
            GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            initialized = true;
        } catch (Throwable t) {
            shaderFailed = true;
        }
    }

    public void render(Color color, Color secondColor, float opacity, float speed, float minDepth, float maxDepth, int mode, float glow) {
        if (mc.getFramebuffer() == null) return;
        if (!(mc.getFramebuffer().getColorAttachment() instanceof GlTexture colorTex)) return;
        if (!(mc.getFramebuffer().getDepthAttachment() instanceof GlTexture depthTex)) return;

        init();
        if (shaderFailed || !initialized) return;

        int cId = colorTex.getGlId();
        int dId = depthTex.getGlId();
        if (cId <= 0 || dId <= 0) return;

        int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int prevTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);

        int fbWidth = mc.getFramebuffer().textureWidth;
        int fbHeight = mc.getFramebuffer().textureHeight;

        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glViewport(0, 0, fbWidth, fbHeight);

            GL20.glUseProgram(shaderProgram);

            // Bind textures
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, cId);
            GL20.glUniform1i(uSampler0, 0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, dId);
            GL20.glUniform1i(uSampler1, 1);

            // Uniforms
            float r = color.getRed() / 255.0f;
            float g = color.getGreen() / 255.0f;
            float b = color.getBlue() / 255.0f;
            GL20.glUniform4f(uTintColor, r, g, b, opacity);

            float r2 = secondColor.getRed() / 255.0f;
            float g2 = secondColor.getGreen() / 255.0f;
            float b2 = secondColor.getBlue() / 255.0f;
            GL20.glUniform4f(uSecondColor, r2, g2, b2, 1.0f);

            float time = ((float) (System.currentTimeMillis() % 1000000L) / 1000.0f) * speed;
            GL20.glUniform1f(uTime, time);
            GL20.glUniform1f(uMinDepth, minDepth);
            GL20.glUniform1f(uMaxDepth, maxDepth);
            GL20.glUniform1i(uMode, mode);
            GL20.glUniform1f(uGlow, glow);

            // Blending & Depth
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            // Draw full screen pass
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            GL30.glBindVertexArray(0);

            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        } catch (Throwable ignored) {
        } finally {
            if (!blendWasEnabled) GL11.glDisable(GL11.GL_BLEND);
            if (depthTestWasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (scissorWasEnabled) GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(depthMask);
            GL30.glBindVertexArray(prevVao);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTex0);
            GL20.glUseProgram(prevProgram);
        }
    }
}
