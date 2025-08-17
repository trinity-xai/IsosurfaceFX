package IsosurfaceFX;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
 *
 * @author Sean Phillips
 */
public class ContractionConcaveHullMini {
    
private ContractionConcaveHullMini() {}

    public static TriangleMesh build(List<Point3D> points, double influenceRadius) {
        // 1) Convex hull
        Point3d[] qh = points.stream().map(p -> new Point3d(p.getX(), p.getY(), p.getZ())).toArray(Point3d[]::new);
        QuickHull3D hull = new QuickHull3D(qh);
        Point3d[] H = hull.getVertices();
        int[][] F = hull.getFaces();

        // 2) Map hull vertices back to input indices (ε match works for generated clouds)
        int[] map = mapHullVertsToInputIndices(H, points, 1e-9);

        // 3) Triangulate faces → working surface as list of int[3] in input-index space
        List<int[]> faces = new ArrayList<>();
        for (int[] poly : F) {
            if (poly.length < 3) continue;
            int i0 = map[poly[0]];
            for (int k = 1; k + 1 < poly.length; k++) faces.add(new int[]{ i0, map[poly[k]], map[poly[k+1]] });
        }

        // 4) Mark which points already on surface
        int n = points.size();
        boolean[] on = new boolean[n];
        for (int[] t : faces) on[t[0]] = on[t[1]] = on[t[2]] = true;

        // 5) Priority queue of interior points by distance to nearest facet
        double med = medianHullEdgeLength(H, F);
        final double maxAttach = Math.max(1e-6, influenceRadius + 0.5 * med*2.0);
        PriorityQueue<Cand> Q = new PriorityQueue<>(Comparator.comparingDouble(c -> c.d));

        for (int i = 0; i < n; i++) if (!on[i]) {
            NF nf = nearestFacet(i, points, faces);
            if (nf != null) Q.add(new Cand(i, nf.fi, nf.d));
        }

        // 6) Iterate: pick closest, replace facet with 3 new triangles
        int maxIters = Math.min(n * 5, 20000), it = 0;
        while (!Q.isEmpty() && it++ < maxIters) {
            Cand c = Q.poll();
            if (on[c.pi]) continue;

            NF nf = nearestFacet(c.pi, points, faces); // refresh since faces changed
            if (nf == null || nf.d > maxAttach) continue;

            int[] f = faces.get(nf.fi);
            Point3D A = points.get(f[0]), B = points.get(f[1]), C = points.get(f[2]);
            Point3D P = points.get(c.pi);

            //if (!isOutsideFacet(P, A, B, C)) continue; // cheap “outside” guard
            if (!isInsideFacet(P, A, B, C)) continue;   // attach only from inside

            // Reference normal of the facet being replaced
            double[] refN = new double[]{
                (B.getY() - A.getY()) * (C.getZ() - A.getZ()) - (B.getZ() - A.getZ()) * (C.getY() - A.getY()),
                (B.getZ() - A.getZ()) * (C.getX() - A.getX()) - (B.getX() - A.getX()) * (C.getZ() - A.getZ()),
                (B.getX() - A.getX()) * (C.getY() - A.getY()) - (B.getY() - A.getY()) * (C.getX() - A.getX())
            };

            // Replace with oriented triangles
            faces.remove(nf.fi);
            faces.add(oriented(f[0], f[1], c.pi, points, refN));
            faces.add(oriented(f[1], f[2], c.pi, points, refN));
            faces.add(oriented(f[2], f[0], c.pi, points, refN));
            on[c.pi] = true;

            // Re-seed queue (simple & safe; optimize later if needed)
            Q.clear();
            for (int i = 0; i < n; i++) if (!on[i]) {
                NF nf2 = nearestFacet(i, points, faces);
                if (nf2 != null) Q.add(new Cand(i, nf2.fi, nf2.d));
            }
        }

        // 7) Export to JavaFX TriangleMesh
        TriangleMesh out = new TriangleMesh();
        for (Point3D p : points) out.getPoints().addAll((float)p.getX(), (float)p.getY(), (float)p.getZ());
        out.getTexCoords().addAll(0, 0);
        for (int[] t : faces) out.getFaces().addAll(t[0], 0, t[1], 0, t[2], 0);
        return out;
    }

    // ---- helpers (tight) ----
    private record Cand(int pi, int fi, double d) {}
    private record NF(int fi, double d) {}

// Ensures triangle (i,j,k) is wound so its normal points in the same general direction as refN
/**
 * Returns a triangle index triplet whose winding makes its geometric normal
 * point in the same general direction as a given reference normal.
 * <p>
 * This is useful when replacing a facet with new triangles: the new triangles
 * should keep the original facet's outward orientation so that normals remain
 * consistent and the mesh stays closed.
 *
 * <h4>How it works</h4>
 * <ol>
 *   <li>Compute the triangle's (unnormalized) geometric normal
 *       {@code N = (B - A) x (C - A)} using the right-hand rule.</li>
 *   <li>Take the dot product {@code dot = N · refN} with the provided
 *       reference normal (also unnormalized).</li>
 *   <li>If {@code dot < 0}, the triangle is wound opposite to {@code refN}
 *       — swap the last two indices to flip its orientation.</li>
 *   <li>If the triangle is degenerate (very small |N|) or {@code refN} is
 *       near zero, return the original order unchanged.</li>
 * </ol>
 *
 * <p><b>Notes:</b> The sign of the dot product is invariant to uniform
 * scaling, so neither {@code N} nor {@code refN} need to be normalized.
 *
 * @param i    index of the first vertex (A) in {@code pts}
 * @param j    index of the second vertex (B) in {@code pts}
 * @param k    index of the third vertex (C) in {@code pts}
 * @param pts  list of 3D points; indices {@code i,j,k} must be valid
 * @param refN reference outward normal for the facet being replaced;
 *             3 elements (x,y,z), not necessarily unit length
 * @return an int[3] = {i, j, k} if already aligned with {@code refN},
 *         otherwise {i, k, j} to flip the winding
 */
private static int[] oriented(int i, int j, int k, List<Point3D> pts, double[] refN) {
    // Safety: if refN is unusable, keep original winding.
    if (refN == null || refN.length < 3) return new int[]{i, j, k};
    double rnx = refN[0], rny = refN[1], rnz = refN[2];
    double refLen2 = rnx*rnx + rny*rny + rnz*rnz;
    if (refLen2 < 1e-20) return new int[]{i, j, k};

    // Fetch triangle vertices
    Point3D A = pts.get(i);
    Point3D B = pts.get(j);
    Point3D C = pts.get(k);

    // Edge vectors: AB and AC
    double abx = B.getX() - A.getX();
    double aby = B.getY() - A.getY();
    double abz = B.getZ() - A.getZ();

    double acx = C.getX() - A.getX();
    double acy = C.getY() - A.getY();
    double acz = C.getZ() - A.getZ();

    // Geometric normal via cross product N = AB x AC (unnormalized)
    double nx = aby * acz - abz * acy;
    double ny = abz * acx - abx * acz;
    double nz = abx * acy - aby * acx;

    // Degenerate triangle check (area ~ 0): keep original winding
    double nLen2 = nx*nx + ny*ny + nz*nz;
    if (nLen2 < 1e-20) return new int[]{i, j, k};

    // Compare direction with reference normal
    double dot = nx * rnx + ny * rny + nz * rnz;

    // If pointing opposite, flip winding by swapping (j,k)
    return (dot >= 0.0) ? new int[]{i, j, k} : new int[]{i, k, j};
}
private static boolean isInsideFacet(Point3D P, Point3D A, Point3D B, Point3D C) {
    double ux = B.getX()-A.getX(), uy = B.getY()-A.getY(), uz = B.getZ()-A.getZ();
    double vx = C.getX()-A.getX(), vy = C.getY()-A.getY(), vz = C.getZ()-A.getZ();
    double nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
    double side = nx*(P.getX()-A.getX()) + ny*(P.getY()-A.getY()) + nz*(P.getZ()-A.getZ());
    return side < -1e-7; // inside the hull w.r.t. facet normal
}

    private static int[] mapHullVertsToInputIndices(Point3d[] H, List<Point3D> P, double eps) {
        int[] m = new int[H.length];
        for (int i = 0; i < H.length; i++) {
            int best = 0; double best2 = Double.POSITIVE_INFINITY;
            for (int j = 0; j < P.size(); j++) {
                double dx = H[i].x - P.get(j).getX(), dy = H[i].y - P.get(j).getY(), dz = H[i].z - P.get(j).getZ();
                double d2 = dx*dx + dy*dy + dz*dz;
                if (d2 < best2) { best2 = d2; best = j; }
                if (d2 <= eps*eps) { best = j; break; }
            }
            m[i] = best;
        }
        return m;
    }

    private static double medianHullEdgeLength(Point3d[] H, int[][] F) {
        ArrayList<Double> e = new ArrayList<>();
        for (int[] poly : F) for (int i = 0; i < poly.length; i++) {
            Point3d a = H[poly[i]], b = H[poly[(i+1)%poly.length]];
            double dx=a.x-b.x, dy=a.y-b.y, dz=a.z-b.z;
            e.add(Math.sqrt(dx*dx+dy*dy+dz*dz));
        }
        Collections.sort(e);
        return e.isEmpty() ? 0.0 : e.get(e.size()/2);
    }

    private static NF nearestFacet(int pi, List<Point3D> pts, List<int[]> faces) {
        Point3D P = pts.get(pi);
        double best = Double.POSITIVE_INFINITY; int bestFi = -1;
        for (int fi = 0; fi < faces.size(); fi++) {
            int[] f = faces.get(fi);
            double d = pointTriangleDistance(P, pts.get(f[0]), pts.get(f[1]), pts.get(f[2]));
            if (d < best) { best = d; bestFi = fi; }
        }
        return bestFi < 0 ? null : new NF(bestFi, best);
    }

    private static boolean isOutsideFacet(Point3D P, Point3D A, Point3D B, Point3D C) {
        double ux = B.getX()-A.getX(), uy = B.getY()-A.getY(), uz = B.getZ()-A.getZ();
        double vx = C.getX()-A.getX(), vy = C.getY()-A.getY(), vz = C.getZ()-A.getZ();
        double nx = uy*vz - uz*vy, ny = uz*vx - ux*vz, nz = ux*vy - uy*vx;
        double side = nx*(P.getX()-A.getX()) + ny*(P.getY()-A.getY()) + nz*(P.getZ()-A.getZ());
        return side > 1e-7;
    }

    // Compact point–triangle distance: project + clamp barycentrics
    private static double pointTriangleDistance(Point3D P, Point3D A, Point3D B, Point3D C) {
        // Vectors
        double[] ap = v(P, A), ab = v(B, A), ac = v(C, A);
        // Compute barycentric coordinates of projection
        double d00 = dot(ab, ab), d01 = dot(ab, ac), d11 = dot(ac, ac);
        double[] n = cross(ab, ac);
        double[] apn = cross(ap, n);                // perpendicular to n in plane
        double v = dot(apn, ac) / (d00*d11 - d01*d01 + 1e-18);
        double w = dot(cross(ab, ap), n) / (dot(n,n) + 1e-18); // fallback; keep robust
        // Clamp to triangle (simple clamp; good enough for generated clouds)
        v = clamp(v, 0, 1);
        w = clamp(w, 0, 1 - v);
        double u = 1 - v - w;
        double[] q = new double[]{
            u*A.getX() + v*B.getX() + w*C.getX(),
            u*A.getY() + v*B.getY() + w*C.getY(),
            u*A.getZ() + v*B.getZ() + w*C.getZ()
        };
        return Math.sqrt( sq(P.getX()-q[0]) + sq(P.getY()-q[1]) + sq(P.getZ()-q[2]) );
    }
    private static double[] v(Point3D p, Point3D q){ return new double[]{p.getX()-q.getX(), p.getY()-q.getY(), p.getZ()-q.getZ()}; }
    private static double dot(double[] a,double[] b){ return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private static double[] cross(double[] a,double[] b){ return new double[]{ a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0] }; }
    private static double clamp(double x,double lo,double hi){ return Math.max(lo, Math.min(hi, x)); }
    private static double sq(double x){ return x*x; }
}
