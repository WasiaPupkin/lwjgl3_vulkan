package t11;

import org.joml.Vector2f;

import java.util.Vector;

import static java.lang.Math.log;
import static org.joml.Math.atan2;
import static org.joml.Math.clamp;

public class Vec2FieldSystem {

    public void update(GravityPhysicsSystem physicsSystem, Vector<LveGameObject> physicsObjs,
                       Vector<LveGameObject> vectorField) {
        // For each field line we caluclate the net graviation force for that point in space
        for (LveGameObject vf : vectorField) {
            Vector2f direction = new Vector2f();
            for (LveGameObject obj : physicsObjs) {
                direction.add(physicsSystem.computeForce(obj, vf));
            }

            // This scales the length of the field line based on the log of the length
            // values were chosen just through trial and error based on what i liked the look
            // of and then the field line is rotated to point in the direction of the field
            vf.transform2d.scale.x = (float) (0.005f + 0.045f * clamp(log(direction.length() + 1) / 3.f, 0.f, 1.f));
            vf.transform2d.rotation = atan2(direction.y, direction.x);
        }
    }
}
