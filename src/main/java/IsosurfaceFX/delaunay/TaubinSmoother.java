package IsosurfaceFX.delaunay;

import javafx.collections.ObservableFloatArray;
import javafx.scene.shape.TriangleMesh;
import java.util.*;

/**
 * Taubin smoothing (λ/μ) for JavaFX TriangleMesh, in-place.
 * <p>Usage:
 * <pre>
 * // defaults: 20 iters, λ=0.33, μ=-0.34, UNIFORM weights, preserve boundary
 * TaubinSmoother.smooth(mesh);
 *
 * // custom:
 * TaubinSmoother.smooth(mesh, 25, 0.33, -0.34, TaubinSmoother.Weights.COTAN, true);
 * </pre>
 */
public final class TaubinSmoother {

    private TaubinSmoother() {}

    /** Weighting schemes. */
    public enum Weights { UNIFORM, COTAN }

    /** Convenient default: 20 iters, λ=0.33, μ=-0.34, UNIFORM, preserve boundary. */
    public static void smooth(TriangleMesh mesh) {
        smooth(mesh, 20, 0.33, -0.34, Weights.UNIFORM, true);
    }

    /**
     * Taubin smoothing (λ/μ) in-place on a TriangleMesh.
     * @param mesh TriangleMesh (points will be modified)
     * @param iterations number of total passes (even number recommended; code will handle odd)
     * @param lambda first pass step (e.g., 0.33)
     * @param mu second pass step (negative, e.g., -0.34)
     * @param weightMode uniform or cotangent weights
     * @param preserveBoundary if true, vertices on open boundary edges won't move
     */
    public static void smooth(TriangleMesh mesh, int iterations, double lambda, double mu,
                              Weights weightMode, boolean preserveBoundary) {
        if (mesh == null) return;

        // --- read points ---
        ObservableFloatArray arr = mesh.getPoints();
        final int n = arr.size() / 3;
        if (n == 0) return;

        double[] x = new double[n], y = new double[n], z = new double[n];
        for (int i = 0, vi = 0; i < n; i++) {
            x[i] = arr.get(vi++);
            y[i] = arr.get(vi++);
            z[i] = arr.get(vi++);
        }

        // --- faces: JavaFX stores as [p0,t0,p1,t1,p2,t2] ---
        int[] faces = mesh.getFaces().toArray(null);
        int triCount = faces.length / 6;

        // --- build adjacency & (optionally) boundary flags and cotan support ---
        // per-vertex neighbor -> weight (for COTAN), or just neighbor set for UNIFORM
        List<HashMap<Integer, Double>> nbrW = new ArrayList<>(n);
        List<HashSet<Integer>> nbrU = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            if (weightMode == Weights.COTAN) nbrW.add(new HashMap<>());
            else nbrU.add(new HashSet<>());
        }

        // boundary detection: count undirected edges occurrences
        HashMap<Long, Integer> edgeCount = new HashMap<>(triCount * 3 * 2);

        // for cotan, we also collect opposite vertices per edge (up to 2)
        HashMap<Long, int[]> edgeOpp = new HashMap<>(triCount * 3 * 2);

        for (int t = 0; t < triCount; t++) {
            int p0 = faces[6 * t];
            int p1 = faces[6 * t + 2];
            int p2 = faces[6 * t + 4];

            addEdge(edgeCount, p0, p1);
            addEdge(edgeCount, p1, p2);
            addEdge(edgeCount, p2, p0);

            if (weightMode == Weights.COTAN) {
                addOpp(edgeOpp, p0, p1, p2);
                addOpp(edgeOpp, p1, p2, p0);
                addOpp(edgeOpp, p2, p0, p1);
            } else {
                nbrU.get(p0).add(p1); nbrU.get(p0).add(p2);
                nbrU.get(p1).add(p0); nbrU.get(p1).add(p2);
                nbrU.get(p2).add(p0); nbrU.get(p2).add(p1);
            }
        }

        boolean[] isBoundary = new boolean[n];
        if (preserveBoundary) {
            for (Map.Entry<Long, Integer> e : edgeCount.entrySet()) {
                if (e.getValue() == 1) {
                    int a = (int) (e.getKey() >>> 32);
                    int b = (int) (e.getKey() & 0xffffffffL);
                    isBoundary[a] = true;
                    isBoundary[b] = true;
                }
            }
        }

        if (weightMode == Weights.COTAN) {
            // build symmetric cotangent weights
            for (Map.Entry<Long, int[]> e : edgeOpp.entrySet()) {
                int i = (int) (e.getKey() >>> 32);
                int j = (int) (e.getKey() & 0xffffffffL);
                int[] opp = e.getValue(); // up to 2 opposite vertices; -1 if missing
                double w = 0.0;
                if (opp[0] >= 0) w += cotAtOpp(i, j, opp[0], x, y, z);
                if (opp[1] >= 0) w += cotAtOpp(i, j, opp[1], x, y, z);
                if (w < 0) w = 0; // be safe; negative weights can cause artifacts
                nbrW.get(i).merge(j, w, Double::sum);
                nbrW.get(j).merge(i, w, Double::sum);
            }
        }

        // --- Taubin passes (λ, μ alternating) ---
        double[] nx = new double[n], ny = new double[n], nz = new double[n];
        double[] sx = x, sy = y, sz = z; // current buffers
        double[] dx = nx, dy = ny, dz = nz;

        for (int it = 0; it < iterations; it++) {
            final double s = (it % 2 == 0) ? lambda : mu;

            if (weightMode == Weights.UNIFORM) {
                for (int i = 0; i < n; i++) {
                    if (preserveBoundary && isBoundary[i]) { dx[i] = sx[i]; dy[i] = sy[i]; dz[i] = sz[i]; continue; }
                    HashSet<Integer> N = nbrU.get(i);
                    if (N.isEmpty()) { dx[i] = sx[i]; dy[i] = sy[i]; dz[i] = sz[i]; continue; }
                    double mx = 0, my = 0, mz = 0;
                    for (int j : N) { mx += sx[j] - sx[i]; my += sy[j] - sy[i]; mz += sz[j] - sz[i]; }
                    double inv = 1.0 / N.size();
                    dx[i] = sx[i] + s * (mx * inv);
                    dy[i] = sy[i] + s * (my * inv);
                    dz[i] = sz[i] + s * (mz * inv);
                }
            } else { // COTAN
                for (int i = 0; i < n; i++) {
                    if (preserveBoundary && isBoundary[i]) { dx[i] = sx[i]; dy[i] = sy[i]; dz[i] = sz[i]; continue; }
                    HashMap<Integer, Double> N = nbrW.get(i);
                    if (N.isEmpty()) { dx[i] = sx[i]; dy[i] = sy[i]; dz[i] = sz[i]; continue; }
                    double wx = 0, wy = 0, wz = 0, W = 0;
                    for (Map.Entry<Integer, Double> en : N.entrySet()) {
                        int j = en.getKey();
                        double w = en.getValue();
                        wx += w * (sx[j] - sx[i]);
                        wy += w * (sy[j] - sy[i]);
                        wz += w * (sz[j] - sz[i]);
                        W  += Math.max(w, 0);
                    }
                    if (W < 1e-18) { dx[i] = sx[i]; dy[i] = sy[i]; dz[i] = sz[i]; continue; }
                    double inv = 1.0 / W;
                    dx[i] = sx[i] + s * (wx * inv);
                    dy[i] = sy[i] + s * (wy * inv);
                    dz[i] = sz[i] + s * (wz * inv);
                }
            }

            // swap buffers (dx -> sx for next pass)
            double[] tx = sx; sx = dx; dx = tx;
            double[] ty = sy; sy = dy; dy = ty;
            double[] tz = sz; sz = dz; dz = tz;
        }

        // If iterations is odd, smoothed positions are in sx/sy/sz already.
        // Write back to mesh:
        for (int i = 0, vi = 0; i < n; i++) {
            arr.set(vi++, (float) sx[i]);
            arr.set(vi++, (float) sy[i]);
            arr.set(vi++, (float) sz[i]);
        }
    }

    /* ----------------------- helpers ----------------------- */

    private static void addEdge(HashMap<Long, Integer> map, int a, int b) {
        long k = edgeKey(a, b);
        map.merge(k, 1, Integer::sum);
    }

    private static void addOpp(HashMap<Long, int[]> edgeOpp, int a, int b, int opp) {
        long k = edgeKey(a, b);
        int[] arr = edgeOpp.get(k);
        if (arr == null) {
            edgeOpp.put(k, new int[]{opp, -1});
        } else if (arr[0] != opp && arr[1] != opp) {
            arr[1] = opp; // second triangle (if present)
        }
    }

    private static long edgeKey(int a, int b) {
        int i = Math.min(a, b), j = Math.max(a, b);
        return (((long) i) << 32) | (j & 0xffffffffL);
    }

    /** cot(angle at vertex k) where angle is between (i-k) and (j-k). */
    private static double cotAtOpp(int i, int j, int k, double[] x, double[] y, double[] z) {
        double ux = x[i] - x[k], uy = y[i] - y[k], uz = z[i] - z[k];
        double vx = x[j] - x[k], vy = y[j] - y[k], vz = z[j] - z[k];
        double dot = ux * vx + uy * vy + uz * vz;
        double cx = uy * vz - uz * vy;
        double cy = uz * vx - ux * vz;
        double cz = ux * vy - uy * vx;
        double A2 = Math.sqrt(cx * cx + cy * cy + cz * cz); // ||u×v||
        if (A2 < 1e-18) return 0.0;
        return dot / A2;
    }
}
