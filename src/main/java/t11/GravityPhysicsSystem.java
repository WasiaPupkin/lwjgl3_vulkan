package t11;

import org.joml.Vector2f;

import java.util.Vector;

public class GravityPhysicsSystem {
    float strengthGravity;

    public GravityPhysicsSystem(float strength) {
        strengthGravity = strength;
    }

    // dt stands for delta time, and specifies the amount of time to advance the simulation
    // substeps is how many intervals to divide the forward time step in. More substeps result in a
    // more stable simulation, but takes longer to compute
    void update(Vector<LveGameObject> objs, float dt, int substeps) {
        int substepsLocal = Math.max(substeps, 1);
        float stepDelta = dt / substepsLocal;
        for (int i = 0; i < substepsLocal; i++) {
            stepSimulation(objs, stepDelta);
        }
    }

    Vector2f computeForce(LveGameObject fromObj, LveGameObject toObj) {
        var offset = fromObj.transform2d.translation.sub(toObj.transform2d.translation, new Vector2f());
        float distanceSquared = fromObj.transform2d.translation.distanceSquared(toObj.transform2d.translation);

        // clown town - just going to return 0 if objects are too close together...
        if (Math.abs(distanceSquared) < 1e-10f) {
            return new Vector2f(.0f, .0f);
        }

        float force = strengthGravity * toObj.rigidBody2d.mass * fromObj.rigidBody2d.mass / distanceSquared;
        return offset.mul(force, new Vector2f()).div((float) Math.sqrt(distanceSquared));
    }

    private void stepSimulation(Vector<LveGameObject> physicsObjs, float dt) {
        // Loops through all pairs of objects and applies attractive force between them
        for (var i : physicsObjs) {
            for (var j : physicsObjs) {
                if (i == j) continue;
                var force = computeForce(i, j);
                i.rigidBody2d.velocity.add(force.negate(new Vector2f()).mul(dt).div(i.rigidBody2d.mass));
                j.rigidBody2d.velocity.add(force.mul(dt, new Vector2f()).div(j.rigidBody2d.mass));
            }
        }

        // update each objects position based on its final velocity
        for (LveGameObject obj : physicsObjs) {
            obj.transform2d.translation.add(obj.rigidBody2d.velocity.mul(dt, new Vector2f()));
        }
    }
}
