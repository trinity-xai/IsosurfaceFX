package IsosurfaceFX;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javafx.geometry.Point3D;

/**
 * Centralized point-cloud factory with simple string-based shapes (backwards compatible)
 * and optional parameters via Options builder.
 * @author Sean Phillips
 */

public final class PointCloud {

    private PointCloud() {}

    // --- Public API ---------------------------------------------------------------------------

    /** Backwards-compatible entry: shape is "sphere", "cube", "torus", plus a few new ones. */
    public static List<Point3D> generate(int count, String shape) {
        return generate(count, shape, Options.defaults());
    }

    /** Parameterized entry: tune radii, seeds, resolutions, etc. */
    public static List<Point3D> generate(int count, String shape, Options opts) {
        String s = (shape == null ? "sphere" : shape).trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "cube":
                return (opts.cubeSampling == Sampling.SURFACE)
                        ? cubeSurface(count, opts.halfExtent, opts.rng())
                        : cubeVolume(count, opts.halfExtent, opts.rng());
            case "torus":
                return torusSurface(count, opts.torusR, opts.torusr, opts.rng());
            case "plus":
            case "cross":
            return (opts.plusSampling == Sampling.SURFACE)
                    ? plus3D(/* rings on surface */ count, opts.plusLen, opts.plusTubeR, opts.plusRings, opts.rng())
                    : plus3DVolume(/* filled bars */ count, opts.plusLen, opts.plusTubeR, opts.rng());
            case "hollowbox":
            case "hollow_box":
                return hollowBoxShell(opts.boxRes, opts.halfExtent, opts.shellThickness, opts.rng());
            case "swisssphere":
            case "swiss_sphere":
                return swissCheeseSphere(opts.shellRes, opts.radius, opts.numHoles, opts.holeRadius, opts.rng());
            case "sphere":
            default:
                return (opts.sphereSampling == Sampling.SURFACE)
                        ? sphereSurface(count, opts.radius, opts.rng())
                        : sphereVolume(count, opts.radius, opts.rng());
        }
    }

    // --- Options / Sampling -------------------------------------------------------------------

    public enum Sampling { SURFACE, VOLUME }

    /** Tunable parameters with a fluent builder. */
    public static final class Options {
        // Common
        private long seed = System.nanoTime();
        private double radius = 40;                  // sphere radius
        private Sampling sphereSampling = Sampling.SURFACE;

        // Cube
        private double halfExtent = 50;              // cube half-size (so edge = 2*halfExtent)
        private Sampling cubeSampling = Sampling.VOLUME;

        // Torus
        private double torusR = 30;                  // major radius
        private double torusr = 10;                  // minor radius

        // Plus (“+”) symbol (three orthogonal tubes)
        private Sampling plusSampling = Sampling.VOLUME; // default to volume for plus
        private double plusLen = 60;                 // bar length
        private double plusTubeR = 6;                // tube radius
        private int plusRings = 24;                  // ring resolution per bar step

        // Hollow box shell
        private int boxRes = 24;                     // samples per face axis
        private double shellThickness = 5;           // inner cavity offset from outer

        // Swiss-cheese sphere
        private int shellRes = 42;                   // samples on outer shell
        private int numHoles = 4;                    // internal void count
        private double holeRadius = 8;               // internal void radius

        public static Options defaults() { return new Options(); }

        public Options withSeed(long seed) { this.seed = seed; return this; }
        public Options withRadius(double r) { this.radius = r; return this; }
        public Options withSphereSampling(Sampling s) { this.sphereSampling = s; return this; }

        public Options withHalfExtent(double h) { this.halfExtent = h; return this; }
        public Options withCubeSampling(Sampling s) { this.cubeSampling = s; return this; }

        public Options withTorusR(double R) { this.torusR = R; return this; }
        public Options withTorusr(double r) { this.torusr = r; return this; }
        
        public Options withPlusSampling(Sampling s) { this.plusSampling = s; return this; }
        public Options withPlusLen(double len) { this.plusLen = len; return this; }
        public Options withPlusTubeR(double r) { this.plusTubeR = r; return this; }
        public Options withPlusRings(int rings) { this.plusRings = rings; return this; }

        public Options withBoxRes(int res) { this.boxRes = res; return this; }
        public Options withShellThickness(double t) { this.shellThickness = t; return this; }

        public Options withShellRes(int res) { this.shellRes = res; return this; }
        public Options withNumHoles(int holes) { this.numHoles = holes; return this; }
        public Options withHoleRadius(double r) { this.holeRadius = r; return this; }

        private Random rng() { return new Random(seed); }
    }

    // --- Implementations ----------------------------------------------------------------------

    // Sphere
    private static List<Point3D> sphereSurface(int count, double R, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double u = rng.nextDouble();
            double v = rng.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = R * Math.sin(phi) * Math.cos(theta);
            double y = R * Math.sin(phi) * Math.sin(theta);
            double z = R * Math.cos(phi);
            pts.add(new Point3D(x, y, z));
        }
        return pts;
    }

    private static List<Point3D> sphereVolume(int count, double R, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // sample radius with cubic root for uniform volume
            double u = rng.nextDouble();
            double r = R * Math.cbrt(u);
            double theta = 2 * Math.PI * rng.nextDouble();
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            pts.add(new Point3D(x, y, z));
        }
        return pts;
    }

    // Cube
    private static List<Point3D> cubeVolume(int count, double half, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pts.add(new Point3D(
                    (rng.nextDouble() * 2 - 1) * half,
                    (rng.nextDouble() * 2 - 1) * half,
                    (rng.nextDouble() * 2 - 1) * half
            ));
        }
        return pts;
    }

    private static List<Point3D> cubeSurface(int count, double half, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // pick a face uniformly among 6, then sample 2D uniform on that face
            int face = rng.nextInt(6);
            double u = (rng.nextDouble() * 2 - 1) * half;
            double v = (rng.nextDouble() * 2 - 1) * half;
            switch (face) {
                case 0: pts.add(new Point3D(+half, u, v)); break; // +X
                case 1: pts.add(new Point3D(-half, u, v)); break; // -X
                case 2: pts.add(new Point3D(u, +half, v)); break; // +Y
                case 3: pts.add(new Point3D(u, -half, v)); break; // -Y
                case 4: pts.add(new Point3D(u, v, +half)); break; // +Z
                default:pts.add(new Point3D(u, v, -half)); break; // -Z
            }
        }
        return pts;
    }

    // Torus (surface)
    private static List<Point3D> torusSurface(int count, double R, double r, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double u = 2 * Math.PI * rng.nextDouble();
            double v = 2 * Math.PI * rng.nextDouble();
            double x = (R + r * Math.cos(v)) * Math.cos(u);
            double y = (R + r * Math.cos(v)) * Math.sin(u);
            double z = r * Math.sin(v);
            pts.add(new Point3D(x, y, z));
        }
        return pts;
    }

    // Plus (+) symbol as three orthogonal tubes (surface)
    private static List<Point3D> plus3D(int approxCount, double len, double tubeR, int rings, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(approxCount);
        // we’ll distribute approxCount across bars; each bar has m steps along length, each with "rings" samples
        int bars = 3;
        int stepsPerBar = Math.max(1, approxCount / (bars * Math.max(1, rings)));
        // axes: X, Y, Z
        sampleTubeAlongAxis(pts, stepsPerBar, rings, len, tubeR, Axis.X, rng);
        sampleTubeAlongAxis(pts, stepsPerBar, rings, len, tubeR, Axis.Y, rng);
        sampleTubeAlongAxis(pts, stepsPerBar, rings, len, tubeR, Axis.Z, rng);
        return pts;
    }
/** Uniform samples in a solid cylinder aligned to the given axis. */
private static void sampleCylinderAlongAxis(List<Point3D> out, int count, double len, double r, Axis axis, Random rng) {
    for (int n = 0; n < count; n++) {
        // Uniform along length
        double s = -len/2 + rng.nextDouble() * len;
        // Uniform in disk: radius via sqrt(u), angle random
        double rr = r * Math.sqrt(rng.nextDouble());
        double ang = 2 * Math.PI * rng.nextDouble();
        double rx = rr * Math.cos(ang);
        double ry = rr * Math.sin(ang);

        double x = 0, y = 0, z = 0;
        switch (axis) {
            case X -> { x = s; y = rx; z = ry; }
            case Y -> { y = s; x = rx; z = ry; }
            case Z -> { z = s; x = rx; y = ry; }
        }
        out.add(new Point3D(x, y, z));
    }
}
    private static List<Point3D> plus3DVolume(int totalCount, double len, double tubeR, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(totalCount);
        // Split samples roughly evenly across the three bars
        int perBar = Math.max(1, totalCount / 3);
        sampleCylinderAlongAxis(pts, perBar, len, tubeR, Axis.X, rng);
        sampleCylinderAlongAxis(pts, perBar, len, tubeR, Axis.Y, rng);
        sampleCylinderAlongAxis(pts, totalCount - 2*perBar, len, tubeR, Axis.Z, rng); // remainder
        return pts;
    }

    private enum Axis { X, Y, Z }
    private static void sampleTubeAlongAxis(List<Point3D> out, int steps, int rings, double len, double r, Axis axis, Random rng) {
        for (int i = 0; i < steps; i++) {
            double t = (i + 0.5) / steps;
            double s = -len / 2 + t * len; // position along axis
            for (int k = 0; k < rings; k++) {
                double a = 2 * Math.PI * (k + rng.nextDouble()) / rings; // jitter ring samples
                double rx = r * Math.cos(a), ry = r * Math.sin(a);
                double x = 0, y = 0, z = 0;
                switch (axis) {
                    case X -> { x = s; y = rx; z = ry; }
                    case Y -> { y = s; x = rx; z = ry; }
                    case Z -> { z = s; x = rx; y = ry; }
                }
                out.add(new Point3D(x, y, z));
            }
        }
    }

    // Hollow box: outer and inner shells (six faces each)
    private static List<Point3D> hollowBoxShell(int res, double half, double thickness, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(6 * (res + 1) * (res + 1) * 2);
        double h2 = Math.max(0, half - thickness);
        sampleBoxFaceGrid(pts, res, +half, Axis.X);
        sampleBoxFaceGrid(pts, res, -half, Axis.X);
        sampleBoxFaceGrid(pts, res, +half, Axis.Y);
        sampleBoxFaceGrid(pts, res, -half, Axis.Y);
        sampleBoxFaceGrid(pts, res, +half, Axis.Z);
        sampleBoxFaceGrid(pts, res, -half, Axis.Z);
        // inner cavity
        sampleBoxFaceGrid(pts, res, +h2, Axis.X);
        sampleBoxFaceGrid(pts, res, -h2, Axis.X);
        sampleBoxFaceGrid(pts, res, +h2, Axis.Y);
        sampleBoxFaceGrid(pts, res, -h2, Axis.Y);
        sampleBoxFaceGrid(pts, res, +h2, Axis.Z);
        sampleBoxFaceGrid(pts, res, -h2, Axis.Z);
        return pts;
    }

    private static void sampleBoxFaceGrid(List<Point3D> out, int res, double d, Axis axis) {
        for (int i = 0; i <= res; i++) for (int j = 0; j <= res; j++) {
            double u = (double) i / res, v = (double) j / res;
            double half = Math.abs(d);
            double x=0, y=0, z=0;
            switch (axis) {
                case X -> { x = d; y = -half + 2 * half * u; z = -half + 2 * half * v; }
                case Y -> { y = d; x = -half + 2 * half * u; z = -half + 2 * half * v; }
                case Z -> { z = d; x = -half + 2 * half * u; y = -half + 2 * half * v; }
            }
            out.add(new Point3D(x, y, z));
        }
    }

    // Swiss-cheese sphere: outer shell + several internal spherical shells
    private static List<Point3D> swissCheeseSphere(int shellRes, double R, int holes, double holeR, Random rng) {
        ArrayList<Point3D> pts = new ArrayList<>(shellRes * shellRes + holes * 24 * 24);
        // outer shell
        for (int i = 0; i < shellRes; i++) {
            double u = (i + 0.5) / shellRes;
            for (int j = 0; j < shellRes; j++) {
                double v = (j + 0.5) / shellRes;
                double th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1);
                double x = R * Math.sin(ph) * Math.cos(th);
                double y = R * Math.sin(ph) * Math.sin(th);
                double z = R * Math.cos(ph);
                pts.add(new Point3D(x, y, z));
            }
        }
        // internal voids
        for (int h = 0; h < holes; h++) {
            // place centers inside outer sphere, avoiding boundary
            double rr = (R - 2 * holeR) * 0.9;
            // uniform in ball:
            double rad = rr * Math.cbrt(rng.nextDouble());
            double t = 2 * Math.PI * rng.nextDouble();
            double p = Math.acos(2 * rng.nextDouble() - 1);
            double cx = rad * Math.sin(p) * Math.cos(t);
            double cy = rad * Math.sin(p) * Math.sin(t);
            double cz = rad * Math.cos(p);

            int res = 24;
            for (int i = 0; i < res; i++) {
                double u = (i + 0.5) / res;
                for (int j = 0; j < res; j++) {
                    double v = (j + 0.5) / res;
                    double th = 2 * Math.PI * u, ph = Math.acos(2 * v - 1);
                    double x = cx + holeR * Math.sin(ph) * Math.cos(th);
                    double y = cy + holeR * Math.sin(ph) * Math.sin(th);
                    double z = cz + holeR * Math.cos(ph);
                    pts.add(new Point3D(x, y, z));
                }
            }
        }
        return pts;
    }
}
