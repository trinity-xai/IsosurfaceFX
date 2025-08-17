package IsosurfaceFX.delaunay;

import javafx.animation.AnimationTimer;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.transform.Rotate;
import javafx.scene.PerspectiveCamera;

/**
 * Arcball / orbit camera controller for a JavaFX {@link SubScene}.
 * <p>
 * Implements a stable transform rig:
 * <pre>
 * sceneRoot
 *  └─ rig (Translate = pan)
 *      └─ yawGroup (Rotate Y = yaw)
 *          └─ pitchGroup (Rotate X = pitch)
 *              └─ Camera (Translate Z = -radius)
 * </pre>
 * Orbit (LMB drag) adjusts yaw/pitch. Zoom (scroll) adjusts radius.
 * Pan (RMB or MMB drag, or WASD/Space/Ctrl) moves the orbit target by translating {@code rig}.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * PerspectiveCamera cam = new PerspectiveCamera(true);
 * cam.setNearClip(0.1);
 * cam.setFarClip(10000);
 *
 * SubScene sub = new SubScene(root3D, 900, 640, true, SceneAntialiasing.BALANCED);
 * sub.setCamera(cam);
 * sub.setFocusTraversable(true);
 *
 * ArcballCameraController arc = new ArcballCameraController(root3D, cam, sub);
 * arc.setYawPitch(30, 20);
 * arc.setRadius(400);
 * // after adding your model to root3D:
 * arc.centerOnBounds(root3D);  // or pass your mesh group
 * }</pre>
 */
public class ArcballCameraController {

    private final Group rig;        // pan (world-space)
    private final Group yawGroup;   // yaw rotate Y
    private final Group pitchGroup; // pitch rotate X
    private final PerspectiveCamera cam;
    private final SubScene sub;

    // State
    private double yawDeg = 0.0;
    private double pitchDeg = 20.0;
    private double radius = 400.0;

    // Mouse state
    private double lastX, lastY;
    private boolean orbiting = false, panning = false;

    // Tunables
    private double mouseSensitivity = 0.2; // degrees per pixel for orbit
    private double zoomSpeed = 1.0;        // wheel → radius units
    private double panSpeed = 0.8;         // px → world units scale (auto scaled by radius)
    private double keyPanSpeed = 150.0;    // WASD units/sec
    private double boostMultiplier = 3.0;  // Shift speed multiplier

    // Key state (for WASD panning)
    private boolean kW, kA, kS, kD, kUp, kDown, kShift;

    // Timer
    private AnimationTimer timer;
    private long lastNs = 0L;

    /**
     * Create a new arcball/orbit controller and attach it to the scene.
     * The controller builds a camera rig (groups) under {@code sceneRoot}
     * and re-parents the camera into that rig.
     *
     * @param sceneRoot the 3D root group used by the SubScene
     * @param cam       the PerspectiveCamera already set on the SubScene
     * @param sub       the SubScene receiving mouse/keyboard events
     */
    public ArcballCameraController(Group sceneRoot, PerspectiveCamera cam, SubScene sub) {
        this.cam = cam;
        this.sub = sub;

        // Build rig graph
        this.rig = new Group();
        this.yawGroup = new Group();
        this.pitchGroup = new Group();

        sceneRoot.getChildren().add(rig);
        rig.getChildren().add(yawGroup);
        yawGroup.getChildren().add(pitchGroup);
        pitchGroup.getChildren().add(cam);

        // Initial placement: camera points down -Z by default; place at -radius
        cam.setTranslateZ(-radius);
        yawGroup.getTransforms().setAll(new Rotate(yawDeg, Rotate.Y_AXIS));
        pitchGroup.getTransforms().setAll(new Rotate(pitchDeg, Rotate.X_AXIS));

        installHandlers();
        startLoop();
    }

    /* ---------------- Public API ---------------- */

    /** Set yaw/pitch in degrees (pitch clamped to [-89.9, 89.9]). */
    public void setYawPitch(double yawDeg, double pitchDeg) {
        this.yawDeg = yawDeg;
        this.pitchDeg = clamp(pitchDeg, -89.9, 89.9);
        yawGroup.getTransforms().setAll(new Rotate(this.yawDeg, Rotate.Y_AXIS));
        pitchGroup.getTransforms().setAll(new Rotate(this.pitchDeg, Rotate.X_AXIS));
    }

    /** Set orbit radius (distance from target). */
    public void setRadius(double r) {
        this.radius = Math.max(0.1, r);
        cam.setTranslateZ(-this.radius);
    }

    public void setSensitivity(double degPerPixel) { this.mouseSensitivity = degPerPixel; }
    public void setZoomSpeed(double s) { this.zoomSpeed = s; }
    public void setPanSpeed(double s) { this.panSpeed = s; }
    public void setMoveSpeed(double unitsPerSec) { this.keyPanSpeed = unitsPerSec; }
    public void setBoostMultiplier(double m) { this.boostMultiplier = m; }

    /** Pan the orbit target by world delta (adds to rig translate). */
    public void panBy(double dx, double dy, double dz) {
        rig.setTranslateX(rig.getTranslateX() + dx);
        rig.setTranslateY(rig.getTranslateY() + dy);
        rig.setTranslateZ(rig.getTranslateZ() + dz);
    }

    /**
     * Center the target on a node's bounds and set a comfortable radius (≈ 0.8 * AABB diagonal).
     * Call this after adding/refreshing your model.
     */
    public void centerOnBounds(Node node) {
        Bounds b = node.getBoundsInParent();
        double cx = (b.getMinX() + b.getMaxX()) * 0.5;
        double cy = (b.getMinY() + b.getMaxY()) * 0.5;
        double cz = (b.getMinZ() + b.getMaxZ()) * 0.5;
        rig.setTranslateX(cx);
        rig.setTranslateY(cy);
        rig.setTranslateZ(cz);

        double diag = Math.sqrt(sq(b.getWidth()) + sq(b.getHeight()) + sq(b.getDepth()));
        setRadius(Math.max(1.0, diag * 0.8));
    }

    /* ---------------- Internals ---------------- */

    private void installHandlers() {
        // Mouse press: LMB orbit, RMB/MMB pan
        sub.setOnMousePressed(e -> {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            orbiting = e.isPrimaryButtonDown();
            panning = e.isSecondaryButtonDown() || e.isMiddleButtonDown();
            sub.requestFocus();
        });

        sub.setOnMouseReleased(e -> { orbiting = false; panning = false; });

        sub.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - lastX;
            double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();

            if (orbiting) {
                yawDeg   += dx * mouseSensitivity;
                pitchDeg -= dy * mouseSensitivity;
                pitchDeg  = clamp(pitchDeg, -89.9, 89.9);
                yawGroup.getTransforms().setAll(new Rotate(yawDeg, Rotate.Y_AXIS));
                pitchGroup.getTransforms().setAll(new Rotate(pitchDeg, Rotate.X_AXIS));
            } else if (panning) {
                // Pan along camera right/up in world; scale by radius so pan feels consistent
                double yaw = Math.toRadians(yawDeg);
                double rx = Math.cos(yaw), rz = -Math.sin(yaw); // right in XZ
                double ux = 0, uy = 1, uz = 0;                  // world up
                double f = panSpeed * (radius * 0.002);
                double dWx = -(rx * dx + ux * dy) * f;
                double dWy = -(0  * dx + uy * dy) * f;
                double dWz = -(rz * dx + uz * dy) * f;
                panBy(dWx, dWy, dWz);
            }
        });

        // Scroll = zoom (change radius)
        sub.setOnScroll(e -> {
            double delta = -e.getDeltaY() * zoomSpeed * 0.1;
            setRadius(radius + delta);
        });

        // Key input for panning (WASD + Space/Ctrl)
        sub.setOnKeyPressed(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.W) kW = true;
            else if (c == KeyCode.A) kA = true;
            else if (c == KeyCode.S) kS = true;
            else if (c == KeyCode.D) kD = true;
            else if (c == KeyCode.SPACE) kUp = true;
            else if (c == KeyCode.CONTROL) kDown = true;
            else if (c == KeyCode.SHIFT) kShift = true;
        });
        sub.setOnKeyReleased(e -> {
            KeyCode c = e.getCode();
            if (c == KeyCode.W) kW = false;
            else if (c == KeyCode.A) kA = false;
            else if (c == KeyCode.S) kS = false;
            else if (c == KeyCode.D) kD = false;
            else if (c == KeyCode.SPACE) kUp = false;
            else if (c == KeyCode.CONTROL) kDown = false;
            else if (c == KeyCode.SHIFT) kShift = false;
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
        if (!(kW || kA || kS || kD || kUp || kDown)) return;

        double speed = keyPanSpeed * (kShift ? boostMultiplier : 1.0);
        double step = speed * dt;

        // right/forward in XZ from yaw
        double yaw = Math.toRadians(yawDeg);
        double rx = Math.cos(yaw), rz = -Math.sin(yaw);
        double fx = Math.sin(yaw), fz = Math.cos(yaw);

        double dx = 0, dy = 0, dz = 0;
        if (kW)    { dx += fx * step; dz += fz * step; }
        if (kS)    { dx -= fx * step; dz -= fz * step; }
        if (kD)    { dx += rx * step; dz += rz * step; }
        if (kA)    { dx -= rx * step; dz -= rz * step; }
        if (kUp)   { dy += step; }
        if (kDown) { dy -= step; }

        panBy(dx, dy, dz);
    }

    /* ---------------- Utils ---------------- */

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
    private static double sq(double x) { return x * x; }
}