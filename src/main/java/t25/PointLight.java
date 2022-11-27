package t25;

import org.joml.Vector4f;
import util.VKUtil;

public class PointLight {
    public Vector4f position = new Vector4f(); // ignore w
    public Vector4f color = new Vector4f(); // w is intensity

    public long sizeOf() {
        return VKUtil.sizeof(position) + VKUtil.sizeof(color);
    }
}
