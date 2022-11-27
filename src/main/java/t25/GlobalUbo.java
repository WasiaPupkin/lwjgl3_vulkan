package t25;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.VKUtil;

import static t25.FirstApp.MAX_LIGHTS;

public class GlobalUbo {
    public volatile Matrix4f projection = new Matrix4f();
    public volatile Matrix4f view = new Matrix4f();
    public volatile Vector4f ambientLightColor = new Vector4f(1.f, 1.f, 1.f, .02f); // w is intensity
    public Vector4f numLights = new Vector4f(); // w is number , other just for alignment...
    public final PointLight[] pointLights = new PointLight[MAX_LIGHTS];

    public long sizeOf() {
        return VKUtil.sizeof(projection) +
                VKUtil.sizeof(view) +
                VKUtil.sizeof(ambientLightColor) +
                VKUtil.sizeof(numLights) +
                new PointLight().sizeOf() * MAX_LIGHTS;
    }
}
