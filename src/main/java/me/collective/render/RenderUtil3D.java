package me.collective.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class RenderUtil3D {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vector2f worldToScreen(double x, double y, double z, int screenWidth, int screenHeight) {
        if (mc.gameRenderer == null) return null;
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return null;

        Vec3d camPos = camera.getCameraPos();
        double relX = x - camPos.x;
        double relY = y - camPos.y;
        double relZ = z - camPos.z;

        Quaternionf camRot = camera.getRotation();
        Matrix4f viewMatrix = new Matrix4f().rotation(camRot.conjugate(new Quaternionf()));

        float fov = mc.options.getFov().getValue().floatValue();
        Matrix4f projMatrix = mc.gameRenderer.getBasicProjectionMatrix(fov);

        Matrix4f viewProj = new Matrix4f(projMatrix).mul(viewMatrix);
        Vector4f pos = new Vector4f((float) relX, (float) relY, (float) relZ, 1.0f);
        viewProj.transform(pos);

        if (pos.w <= 0.05f) {
            return null; // behind camera
        }

        float ndcX = pos.x / pos.w;
        float ndcY = pos.y / pos.w;

        float screenX = (ndcX + 1.0f) * 0.5f * screenWidth;
        float screenY = (1.0f - ndcY) * 0.5f * screenHeight;

        return new Vector2f(screenX, screenY);
    }
}
