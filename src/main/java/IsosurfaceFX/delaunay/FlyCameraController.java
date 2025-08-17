package IsosurfaceFX.delaunay;

import javafx.animation.AnimationTimer;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 *
 * @author Sean Phillips
 */
/** Simple fly/orbit camera controller for a JavaFX SubScene. */
public class FlyCameraController {
    private final PerspectiveCamera cam;
    private final SubScene sub;

    // state
    private double yawDeg = 0.0;      // left/right (about +Y)
    private double pitchDeg = 0.0;    // up/down (about +X)
    private double px = 0, py = 0, pz = -400; // camera position in world coords

    // tunables
    private double mouseSensitivity = 0.2; // degrees per pixel
    private double moveSpeed = 150;        // units per second
    private double boostMultiplier = 3.0;  // Shift speed multiplier
    private double scrollSpeed = 1.0;      // wheel units → world units factor

    // key state
    private boolean kW, kA, kS, kD, kUp, kDown, kShift;

    // mouse
    private double lastMouseX, lastMouseY;
    private boolean dragging = false;

    // timer
    private AnimationTimer timer;
    private long lastNs = 0;

    FlyCameraController(PerspectiveCamera cam, SubScene sub) {
        this.cam = cam;
        this.sub = sub;
        installHandlers();
        startLoop();
        applyTransform();
    }

    void setSpeed(double unitsPerSec){ this.moveSpeed = unitsPerSec; }
    void setBoostMultiplier(double m){ this.boostMultiplier = m; }
    void setSensitivity(double degPerPixel){ this.mouseSensitivity = degPerPixel; }
    void setScrollSpeed(double s){ this.scrollSpeed = s; }

    /* ---------------- input & loop ---------------- */

    private void installHandlers() {
        // Mouse look
        sub.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                dragging = true;
                lastMouseX = e.getSceneX();
                lastMouseY = e.getSceneY();
                sub.requestFocus();
            }
        });
        sub.setOnMouseReleased(e -> {
            if (!e.isPrimaryButtonDown()) dragging = false;
        });
        sub.setOnMouseDragged(e -> {
            if (!dragging) return;
            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();

            yawDeg   += dx * mouseSensitivity;
            pitchDeg -= dy * mouseSensitivity;
            // clamp pitch to avoid gimbal lock flips
            pitchDeg = Math.max(-89.9, Math.min(89.9, pitchDeg));
            applyTransform();
        });

        // Wheel = dolly forward/back along current forward direction
        sub.setOnScroll(e -> {
            double amount = e.getDeltaY() * scrollSpeed * 0.1; // tweak factor
            double[] fwd = forward();
            px += fwd[0] * amount;
            py += fwd[1] * amount;
            pz += fwd[2] * amount;
            applyTransform();
        });

        // Keys (WASD + Space/Ctrl + Shift)
        sub.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case W -> kW = true;
                case A -> kA = true;
                case S -> kS = true;
                case D -> kD = true;
                case SPACE -> kUp = true;
                case CONTROL -> kDown = true;
                case SHIFT -> kShift = true;
                default -> {}
            }
        });
        sub.setOnKeyReleased(e -> {
            switch (e.getCode()) {
                case W -> kW = false;
                case A -> kA = false;
                case S -> kS = false;
                case D -> kD = false;
                case SPACE -> kUp = false;
                case CONTROL -> kDown = false;
                case SHIFT -> kShift = false;
                default -> {}
            }
        });
    }

    private void startLoop() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastNs == 0) { lastNs = now; return; }
                double dt = (now - lastNs) / 1_000_000_000.0;
                lastNs = now;
                update(dt);
            }
        };
        timer.start();
    }

    private void update(double dt) {
        // movement basis vectors from yaw/pitch
        double[] fwd = forward();
        double[] right = right();
        double[] up = new double[]{0,1,0}; // world up; fine for fly cam

        double speed = moveSpeed * (kShift ? boostMultiplier : 1.0);
        double vx = 0, vy = 0, vz = 0;

        if (kW) { vx += fwd[0]; vy += fwd[1]; vz += fwd[2]; }
        if (kS) { vx -= fwd[0]; vy -= fwd[1]; vz -= fwd[2]; }
        if (kD) { vx += right[0]; vy += right[1]; vz += right[2]; }
        if (kA) { vx -= right[0]; vy -= right[1]; vz -= right[2]; }
        if (kUp){ vx += up[0];   vy += up[1];   vz += up[2]; }
        if (kDown){vx -= up[0];  vy -= up[1];   vz -= up[2]; }

        // normalize (only if moving)
        double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (len > 1e-9) {
            vx = (vx/len) * speed * dt;
            vy = (vy/len) * speed * dt;
            vz = (vz/len) * speed * dt;
            px += vx; py += vy; pz += vz;
            applyTransform();
        }
    }

    /* ---------------- math & transform ---------------- */

    // forward vector from yaw/pitch (right-handed, +Z forward when yaw=pitch=0)
    private double[] forward() {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cy = Math.cos(yaw),  sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        // look dir
        double fx =  sy * cp;
        double fy = -sp;
        double fz =  cy * cp;
        return new double[]{fx, fy, fz};
    }

    // right vector (perp to forward & world up)
    private double[] right() {
        double yaw = Math.toRadians(yawDeg);
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        // in XZ-plane, +right when yaw increases
        return new double[]{ cy, 0, -sy };
    }

    private void applyTransform() {
        // We set camera orientation via two Rotates (pitch X, yaw Y) and a Translate (position)
        // Order matters in JavaFX: transforms apply in the order they are added.
        cam.getTransforms().setAll(
            new Rotate(pitchDeg, Rotate.X_AXIS),
            new Rotate(yawDeg,   Rotate.Y_AXIS),
            new Translate(px, py, pz)
        );
    }
}
