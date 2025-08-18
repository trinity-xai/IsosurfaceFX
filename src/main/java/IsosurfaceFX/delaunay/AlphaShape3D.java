package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import java.util.*;

/**
 * α-shape surface extractor with options for:
 *  - robust outside flood (always on)
 *  - face-level α test (always on)
 *  - epsilon-aware outward orientation (always on)
 *  - optional outer-envelope peeling (peelEnabled)
 *  - optional outer-visibility culling (visibilityEnabled, with kLocal)
 */
public final class AlphaShape3D {

    private AlphaShape3D() {}

    /* ---------------- Convenience entry points ---------------- */

    /** Defaults: peel ON (thicknessK=0.5, 96x48), visibility ON (kLocal=1.0), tolerant predicates. */
    public static TriangleMesh build(List<Point3D> pts, double alpha, Delaunay3D delaunay) {
        return build(pts, alpha, delaunay,
                /*peelEnabled=*/true, 0.5, 96, 48,
                /*visibilityEnabled=*/true, /*kLocal=*/1.0,
                Predicates3D.tolerant());
    }

    /** Same, but let caller toggle peel/visibility and kLocal (GUI-friendly). */
    public static TriangleMesh build(List<Point3D> pts, double alpha, Delaunay3D delaunay,
                                     boolean peelEnabled, double thicknessK, int binsAz, int binsEl,
                                     boolean visibilityEnabled, double kLocal,
                                     Predicates3D pred) {
        List<int[]> tets = delaunay.tetrahedralize(pts);
        return buildFromTets(pts, tets, alpha,
                peelEnabled, thicknessK, binsAz, binsEl,
                visibilityEnabled, kLocal, pred,
                    /*weldTolScale*/ 0.003,   // try 0.002–0.005
    /*areaTolScale*/ 0.001    // try 0.001–0.002
        );
    }

 /** Build from precomputed tets with cluster-weld cleanup (boundary-only; no per-triangle α filter by default). */
public static TriangleMesh buildFromTets(List<Point3D> pts, List<int[]> tets, double alpha,
                                         boolean peelEnabled, double thicknessK, int binsAz, int binsEl,
                                         boolean visibilityEnabled, double kLocal,
                                         Predicates3D pred,
                                         double clusterTolScale, double areaTolScale) {
    final int m = tets.size();
    if (m == 0) return TriangleMeshExport.fromFaces(pts, List.of());

    // ---- scene AABB & diag
    double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
    for (Point3D p : pts) {
        double x=p.getX(), y=p.getY(), z=p.getZ();
        if (x<minX) minX=x; if (y<minY) minY=y; if (z<minZ) minZ=z;
        if (x>maxX) maxX=x; if (y>maxY) maxY=y; if (z>maxZ) maxZ=z;
    }
    final double Cx=(minX+maxX)*0.5, Cy=(minY+maxY)*0.5, Cz=(minZ+maxZ)*0.5;
    double dx=maxX-minX, dy=maxY-minY, dz=maxZ-minZ;
    double sceneDiag = Math.sqrt(dx*dx + dy*dy + dz*dz);

    final double a2 = alpha * alpha;

    /* 1) kept tets (circumsphere r ≤ α) */
    boolean[] kept = new boolean[m];
    for (int i = 0; i < m; i++) {
        int[] T = tets.get(i);
        var c = Geometry3D.circumsphere(
            pts.get(T[0]), pts.get(T[1]), pts.get(T[2]), pts.get(T[3]));
        kept[i] = c.valid() && c.r2() <= a2;
    }

    /* 2) face adjacency */
    int[][] nbr = new int[m][4];
    for (int[] row : nbr) Arrays.fill(row, -1);
    Map<Long,int[]> owner = new HashMap<>(m*3);
    for (int ti=0; ti<m; ti++) {
        int[] T = tets.get(ti);
        int[][] facesIdx = {
            {T[1],T[2],T[3],0},{T[0],T[2],T[3],1},{T[0],T[1],T[3],2},{T[0],T[1],T[2],3}
        };
        for (int[] f : facesIdx) {
            long k = triKeyUnordered(f[0], f[1], f[2]);
            int opp = f[3];
            int[] prev = owner.putIfAbsent(k, new int[]{ti, opp});
            if (prev != null) {
                nbr[ti][opp] = prev[0];
                nbr[prev[0]][prev[1]] = ti;
            }
        }
    }

    /* 3) outside flood: seed with any hull-adjacent tet; flow through non-kept only */
    boolean[] outside = new boolean[m];
    Deque<Integer> dq = new ArrayDeque<>();
    for (int ti=0; ti<m; ti++) {
        if (nbr[ti][0] < 0 || nbr[ti][1] < 0 || nbr[ti][2] < 0 || nbr[ti][3] < 0) {
            if (!kept[ti]) outside[ti] = true;
            dq.add(ti);
        }
    }
    while (!dq.isEmpty()) {
        int u = dq.removeFirst();
        for (int f=0; f<4; f++) {
            int v = nbr[u][f];
            if (v >= 0 && !outside[v] && !kept[v]) {
                outside[v] = true;
                dq.addLast(v);
            }
        }
    }

    /* 4) boundary faces + epsilon-aware outward orientation
          IMPORTANT: We do NOT apply a per-triangle α filter here (it breaks manifoldness on volume clouds). */
    final boolean FACE_ALPHA_FILTER = false; // set true to restore old behavior
    final double eps = pred.orientEps();
    List<int[]> faces = new ArrayList<>();
    List<double[]> centroids = new ArrayList<>();

    for (int ti=0; ti<m; ti++) if (kept[ti]) {
        int[] T = tets.get(ti);
        int[][] fl = { {T[1],T[2],T[3],0},{T[0],T[2],T[3],1},{T[0],T[1],T[3],2},{T[0],T[1],T[2],3} };
        for (int[] f : fl) {
            int a=f[0], b=f[1], c=f[2], opp=f[3];
            int tj = nbr[ti][opp];
            boolean boundary = (tj < 0) || outside[tj];
            if (!boundary) continue;

            Point3D A=pts.get(a), B=pts.get(b), C=pts.get(c);

            if (FACE_ALPHA_FILTER) {
                double r2 = triangleCircumradius2(A,B,C);
                if (r2 > a2*1.001) continue;
            }

            // outward orientation using tet's opposite vertex D
            Point3D D = pts.get(T[opp]);
            double o = pred.orient3d(A,B,C,D);
            int[] add;
            if (o > eps) {
                add = new int[]{a, c, b};       // flip to point away from D
            } else if (o < -eps) {
                add = new int[]{a, b, c};       // already outward
            } else {
                // nearly degenerate → use AABB-center heuristic
                double nx = (B.getY()-A.getY())*(C.getZ()-A.getZ()) - (B.getZ()-A.getZ())*(C.getY()-A.getY());
                double ny = (B.getZ()-A.getZ())*(C.getX()-A.getX()) - (B.getX()-A.getX())*(C.getZ()-A.getZ());
                double nz = (B.getX()-A.getX())*(C.getY()-A.getY()) - (B.getY()-A.getY())*(C.getX()-A.getX());
                double cx=(A.getX()+B.getX()+C.getX())/3.0;
                double cy=(A.getY()+B.getY()+C.getY())/3.0;
                double cz=(A.getZ()+B.getZ()+C.getZ())/3.0;
                double dot = nx*(cx-Cx) + ny*(cy-Cy) + nz*(cz-Cz);
                add = (dot >= 0) ? new int[]{a,b,c} : new int[]{a,c,b};
            }
            faces.add(add);
            centroids.add(new double[]{ (A.getX()+B.getX()+C.getX())/3.0,
                                         (A.getY()+B.getY()+C.getY())/3.0,
                                         (A.getZ()+B.getZ()+C.getZ())/3.0 });
        }
    }
    System.out.println("α-face candidates (boundary-only): " + faces.size());

    /* 5) optional peel */
    List<int[]> afterPeel = faces;
    if (peelEnabled) {
        afterPeel = peelOuterEnvelope(Cx, Cy, Cz, faces, centroids, alpha, thicknessK, binsAz, binsEl);
    }
    System.out.println("after peel: " + afterPeel.size());

    /* 6) consistent winding across patches */
    List<int[]> wound = ConsistentWinding3D.orient(pts, afterPeel);
    System.out.println("after winding: " + wound.size());

    /* 7) optional visibility cull (keep modest or OFF for now) */
    List<int[]> visible = wound;
    if (visibilityEnabled) {
        visible = OuterVisibility.filter(pts, wound, alpha, kLocal);
    }
    System.out.println("after visibility (kLocal=" + kLocal + "): " + visible.size());

    /* 8) CLUSTER WELD cleanup (default): collapse vertices within clusterR; dedupe/sliver filter; prune >2 edges */
    double clusterR = Math.max(1e-12, clusterTolScale * sceneDiag);
    double areaTol2 = Math.pow(Math.max(1e-12, areaTolScale * sceneDiag), 2);

    MeshCleaner3D.CleanResult cr = MeshCleaner3D.clusterWeld(pts, visible, clusterR, areaTol2);
    System.out.println("after cluster-weld: verts=" + cr.points.size() + ", faces=" + cr.faces.size()
            + "  (clusterR=" + String.format("%.4f", clusterR) + ", areaTol=" + String.format("%.6f", Math.sqrt(areaTol2)) + ")");

    // DEBUG: edge-degree histogram after weld (helps verify manifoldness)
    printEdgeDegreeStats(cr.faces);

    /* 9) Final consistent winding after weld (ensures coherence post-merge) */
    List<int[]> finalFaces = ConsistentWinding3D.orient(cr.points, cr.faces);
    System.out.println("final faces (post-weld winding): " + finalFaces.size());

    /* 10) Export */
    return TriangleMeshExport.fromFaces(cr.points, finalFaces);
}

/* --- helper for quick diagnostics --- */
private static void printEdgeDegreeStats(List<int[]> faces){
    Map<Long,Integer> deg = new HashMap<>(faces.size()*2);
    for (int[] f : faces){
        int a=f[0],b=f[1],c=f[2];
        long e1=((((long)Math.min(a,b))<<32) | (Math.max(a,b)&0xffffffffL));
        long e2=((((long)Math.min(b,c))<<32) | (Math.max(b,c)&0xffffffffL));
        long e3=((((long)Math.min(c,a))<<32) | (Math.max(c,a)&0xffffffffL));
        deg.put(e1, deg.getOrDefault(e1,0)+1);
        deg.put(e2, deg.getOrDefault(e2,0)+1);
        deg.put(e3, deg.getOrDefault(e3,0)+1);
    }
    int d1=0,d2=0,dgt2=0;
    for (int v : deg.values()){
        if (v==1) d1++;
        else if (v==2) d2++;
        else dgt2++;
    }
    System.out.println("edge-degree: deg1=" + d1 + ", deg2=" + d2 + ", deg>2=" + dgt2);
}

    /* ---------------- Outer-envelope peeling ---------------- */

    private static List<int[]> peelOuterEnvelope(double Cx, double Cy, double Cz,
                                                 List<int[]> faces,
                                                 List<double[]> centroids,
                                                 double alpha, double k,
                                                 int binsAz, int binsEl) {
        if (faces.isEmpty()) return faces;

        final double eps = Math.max(1e-6, k * alpha);
        final double[][] maxR = new double[binsAz][binsEl];
        for (double[] row : maxR) Arrays.fill(row, -Double.MAX_VALUE);

        final int n = faces.size();
        int[] azIdx = new int[n], elIdx = new int[n];
        double[] rho = new double[n];

        for (int i=0;i<n;i++){
            double[] q = centroids.get(i);
            double vx = q[0]-Cx, vy=q[1]-Cy, vz=q[2]-Cz;
            double r  = Math.sqrt(vx*vx + vy*vy + vz*vz) + 1e-12;
            rho[i] = r;
            double az = Math.atan2(vz, vx);
            double el = Math.asin(vy / r);
            int ia = (int)Math.floor((az + Math.PI)  / (2*Math.PI) * binsAz);
            int ie = (int)Math.floor((el + Math.PI/2)/ (Math.PI)    * binsEl);
            ia = Math.max(0, Math.min(binsAz-1, ia));
            ie = Math.max(0, Math.min(binsEl-1, ie));
            azIdx[i]=ia; elIdx[i]=ie;
            if (r > maxR[ia][ie]) maxR[ia][ie] = r;
        }

        ArrayList<int[]> out = new ArrayList<>();
        for (int i=0;i<n;i++){
            if (rho[i] >= maxR[azIdx[i]][elIdx[i]] - eps) out.add(faces.get(i));
        }
        return out;
    }

    /* ---------------- Geometry helpers ---------------- */

    /** Triangle circumradius squared using side lengths and (2*area)^2. */
    private static double triangleCircumradius2(Point3D A, Point3D B, Point3D C) {
        double ab = dist2(A,B), bc = dist2(B,C), ca = dist2(C,A);
        double[] u = sub(B,A), v = sub(C,A);
        double area2 = norm2(cross(u,v)); // (2*area)^2
        if (area2 < 1e-18) return Double.POSITIVE_INFINITY;
        double a = Math.sqrt(ab), b = Math.sqrt(bc), c = Math.sqrt(ca);
        double num = (a*b*c); num *= num;
        double den = 4.0 * area2; // area2 = (2A)^2 ⇒ 16A^2; R^2 = (abc)^2 / (16A^2) = num / (4*area2)
        return num / den;
    }

    private static double[] sub(Point3D P, Point3D Q){ return new double[]{ P.getX()-Q.getX(), P.getY()-Q.getY(), P.getZ()-Q.getZ() }; }
    private static double[] cross(double[] u, double[] v){ return new double[]{ u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0] }; }
    private static double norm2(double[] u){ return u[0]*u[0]+u[1]*u[1]+u[2]*u[2]; }
    private static double dist2(Point3D P, Point3D Q){
        double dx=P.getX()-Q.getX(), dy=P.getY()-Q.getY(), dz=P.getZ()-Q.getZ();
        return dx*dx + dy*dy + dz*dz;
    }
    private static long triKeyUnordered(int a, int b, int c) {
        int i = Math.min(a, Math.min(b, c));
        int k = Math.max(a, Math.max(b, c));
        int j = a + b + c - i - k;
        return (((long) i) << 42) | (((long) j) << 21) | (long) k;
    }
}