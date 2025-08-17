package IsosurfaceFX.delaunay;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;

/**
 * Minimal Bowyer–Watson 3D Delaunay tetrahedralization (pure Java) with
 * pragmatic performance tweaks suitable for medium point sets (few 1e3–1e4):
 * <ul>
 *   <li>Kickstart: force-remove the super-tet for the first point.</li>
 *   <li>Locality: check only a sliding window of most recently added tets first.</li>
 *   <li>Proximity pruning: broad-phase gate by vertex distance before inSphere.</li>
 *   <li>Failsafe: if all else fails, split the largest tet to ensure progress.</li>
 * </ul>
 *
 * Plug in robust predicates via {@link Predicates3D} (use the determinant-based
 * inSphere recommended earlier). Output is a list of 4-tuples (indices) into
 * the original input point list.
 */
public final class BowyerWatson3D implements Delaunay3D {

    private final Predicates3D predicates;

    public BowyerWatson3D(Predicates3D predicates) {
        this.predicates = predicates;
    }

    @Override
    public List<int[]> tetrahedralize(List<Point3D> points) {
        final int n = points.size();

        // --- Scene scale (for proximity pruning) ---------------------------------
        double minX=+1e300,minY=+1e300,minZ=+1e300, maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for (Point3D p: points) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        double dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        double sceneDiag = Math.sqrt(dx*dx + dy*dy + dz*dz);
        final double searchRadius = sceneDiag * 0.25;     // tweakable neighborhood radius
        final double searchR2     = searchRadius * searchRadius;

        // --- Insertion order: preserve input order to keep spatial locality ------
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        // (Optional future optimization: sort by a Morton/Hilbert key instead)

        // --- Working point list (we append super vertices here) -------------------
        List<Point3D> pts = new ArrayList<>(points);

        // --- Seed with a large super tetrahedron ---------------------------------
        int[] S = Geometry3D.addSuperTetrahedron(pts);
        List<int[]> tets = new ArrayList<>(1024);
        tets.add(new int[]{ S[0], S[1], S[2], S[3] });

        // --- Insert points incrementally -----------------------------------------
        final int RECENT_WINDOW = 256; // how many most-recent tets to try first
        int inserted = 0;

        for (int pi : order) {
            Point3D P = pts.get(pi);

            // (A) Find all "bad" tets (whose circumsphere contains P)
            BitSet bad;

            if (tets.size() == 1) {
                // Kickstart: remove the super-tet for the very first insertion
                bad = new BitSet(1);
                bad.set(0);
            } else {
                bad = new BitSet(tets.size());

                // 1) First pass: only check most recently added tets (locality)
                int start = Math.max(0, tets.size() - RECENT_WINDOW);
                for (int ti = start; ti < tets.size(); ti++) {
                    if (predicates.inSphere(pts, tets.get(ti), P)) bad.set(ti);
                }

                // 2) If none found, do a proximity-pruned pass over all tets
                if (bad.isEmpty()) {
                    final double Px = P.getX(), Py = P.getY(), Pz = P.getZ();
                    for (int ti = 0; ti < tets.size(); ti++) {
                        int[] T = tets.get(ti);
                        Point3D A = pts.get(T[0]), B = pts.get(T[1]), C = pts.get(T[2]), D = pts.get(T[3]);

                        // Broad-phase distance gate to vertices
                        double d2A = sq(Px - A.getX()) + sq(Py - A.getY()) + sq(Pz - A.getZ());
                        if (d2A > searchR2) {
                            double d2B = sq(Px - B.getX()) + sq(Py - B.getY()) + sq(Pz - B.getZ());
                            if (d2B > searchR2) {
                                double d2C = sq(Px - C.getX()) + sq(Py - C.getY()) + sq(Pz - C.getZ());
                                if (d2C > searchR2) {
                                    double d2D = sq(Px - D.getX()) + sq(Py - D.getY()) + sq(Pz - D.getZ());
                                    if (d2D > searchR2) continue; // all four vertices far → skip
                                }
                            }
                        }
                        if (predicates.inSphere(pts, T, P)) bad.set(ti);
                    }
                }

                // 3) Failsafe: if still nothing, do a full scan once; if still empty, split one tet
                if (bad.isEmpty()) {
                    for (int ti = 0; ti < tets.size(); ti++) {
                        if (predicates.inSphere(pts, tets.get(ti), P)) { bad.set(ti); }
                    }
                    if (bad.isEmpty()) {
                        // Split the largest-span tet to guarantee progress
                        int pick = 0; double bestSpan = -1;
                        for (int ti = 0; ti < tets.size(); ti++) {
                            int[] T = tets.get(ti);
                            Point3D a=pts.get(T[0]), b=pts.get(T[1]), c=pts.get(T[2]), d=pts.get(T[3]);
                            double span = maxEdgeSpan2(a,b,c,d);
                            if (span > bestSpan) { bestSpan = span; pick = ti; }
                        }
                        bad.set(pick);
                    }
                }
            }

            // (B) Build cavity boundary (faces that appear exactly once among "bad")
            Map<Long, int[]> boundary = new HashMap<>();
            for (int ti = bad.nextSetBit(0); ti >= 0; ti = bad.nextSetBit(ti + 1)) {
                int[] T = tets.get(ti);
                int[][] faces = { {T[0],T[1],T[2]}, {T[0],T[1],T[3]}, {T[0],T[2],T[3]}, {T[1],T[2],T[3]} };
                for (int[] f : faces) {
                    long key = Geometry3D.triKeyUnordered(f[0], f[1], f[2]);
                    if (boundary.containsKey(key)) boundary.remove(key); else boundary.put(key, f);
                }
            }

            // (C) Remove "bad" tets
            for (int ti = bad.length() - 1; ti >= 0; ti--) if (bad.get(ti)) tets.remove(ti);

            // (D) Retetrahedralize the cavity by connecting P to each boundary face
            for (int[] f : boundary.values()) {
                int a = f[0], b = f[1], c = f[2];
                // ensure positive orientation (tet volume > 0)
                if (predicates.orient3d(pts.get(a), pts.get(b), pts.get(c), P) <= predicates.orientEps()) {
                    int tmp = a; a = b; b = tmp;
                }
                tets.add(new int[]{ a, b, c, pi });
            }

            // optional progress print
            inserted++;
            // if ((inserted % 500) == 0) System.out.println("Inserted " + inserted + " / " + n + "  |  tets=" + tets.size());
        }

        // (E) Drop any tets touching the super vertices
        List<int[]> out = new ArrayList<>(tets.size());
        for (int[] T : tets) if (!Geometry3D.usesAny(T, S)) out.add(T);
        return out;
    }

    /* ---------------- helpers ---------------- */

    private static double sq(double x) { return x * x; }

    /** Largest squared edge length among the 6 edges of a tetrahedron (quick size proxy). */
    private static double maxEdgeSpan2(Point3D a, Point3D b, Point3D c, Point3D d) {
        Point3D[] v = {a,b,c,d};
        double best = 0;
        for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) {
            double dx = v[i].getX() - v[j].getX();
            double dy = v[i].getY() - v[j].getY();
            double dz = v[i].getZ() - v[j].getZ();
            double s = dx*dx + dy*dy + dz*dz;
            if (s > best) best = s;
        }
        return best;
    }
}