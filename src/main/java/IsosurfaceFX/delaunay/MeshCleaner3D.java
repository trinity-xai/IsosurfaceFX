package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;

import java.util.*;

/**
 * Cleans a triangle soup into a manifold-like skin:
 *  1) Weld (merge) near-coincident vertices via spatial hashing
 *  2) Drop degenerate & tiny-area faces, and de-duplicate identical faces
 *  3) Prune non-manifold edges (edges with >2 incident faces) keeping the two
 *     faces with largest "outerness" (centroid radius from AABB center)
 *
 * Designed for post-processing alpha-shape faces on volume point clouds.
 */
public final class MeshCleaner3D {

    private MeshCleaner3D(){}

    /** Result of cleaning: remapped points and faces. */
    public static final class CleanResult {
        public final List<Point3D> points;
        public final List<int[]>   faces;
        public CleanResult(List<Point3D> points, List<int[]> faces){
            this.points = points; this.faces = faces;
        }
    }
// Add to MeshCleaner3D
public static CleanResult clusterWeld(List<Point3D> pts, List<int[]> faces,
                                      double clusterRadius, double areaTol2) {
    // 1) simple grid buckets by clusterRadius
    double inv = 1.0 / Math.max(1e-12, clusterRadius);
    Map<Long, List<Integer>> cell = new HashMap<>();
    for (int i=0;i<pts.size();i++) {
        Point3D p = pts.get(i);
        long k = cellKey(p.getX(), p.getY(), p.getZ(), inv);
        cell.computeIfAbsent(k, s->new ArrayList<>()).add(i);
    }
    // 2) compute cluster centroids
    List<Point3D> newPts = new ArrayList<>();
    int[] map = new int[pts.size()];
    for (List<Integer> group : cell.values()) {
        double sx=0, sy=0, sz=0; for (int idx : group) { Point3D p=pts.get(idx); sx+=p.getX(); sy+=p.getY(); sz+=p.getZ(); }
        Point3D c = new Point3D(sx/group.size(), sy/group.size(), sz/group.size());
        int newIdx = newPts.size();
        newPts.add(c);
        for (int idx : group) map[idx] = newIdx;
    }
    // 3) remap & dedupe/area filter
    HashSet<Long> seen = new HashSet<>(faces.size()*2);
    ArrayList<int[]> F = new ArrayList<>(faces.size());
    for (int[] f : faces) {
        int a=map[f[0]], b=map[f[1]], c=map[f[2]];
        if (a==b || b==c || c==a) continue;
        Point3D A=newPts.get(a), B=newPts.get(b), C=newPts.get(c);
        double ux=B.getX()-A.getX(), uy=B.getY()-A.getY(), uz=B.getZ()-A.getZ();
        double vx=C.getX()-A.getX(), vy=C.getY()-A.getY(), vz=C.getZ()-A.getZ();
        double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
        double area2 = nx*nx+ny*ny+nz*nz;
        if (area2 < areaTol2) continue;
        long key = triKeyUnordered(a,b,c);
        if (seen.add(key)) F.add(new int[]{a,b,c});
    }
    // 4) prune non-manifold edges exactly like clean()
    CleanResult tmp = new CleanResult(newPts, F);
    return pruneNonManifold(tmp);
}

// expose prune step
private static CleanResult pruneNonManifold(CleanResult in){
    List<Point3D> P = in.points; List<int[]> F = new ArrayList<>(in.faces);
    if (F.isEmpty()) return in;

    double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
    for (Point3D p: P){ double x=p.getX(),y=p.getY(),z=p.getZ();
        if (x<minX)minX=x; if (y<minY)minY=y; if (z<minZ)minZ=z;
        if (x>maxX)maxX=x; if (y>maxY)maxY=y; if (z>maxZ)maxZ=z;
    }
    final double Cx=(minX+maxX)/2, Cy=(minY+maxY)/2, Cz=(minZ+maxZ)/2;

    HashMap<Long, ArrayList<Integer>> edgeFaces = new HashMap<>(F.size()*2);
    for (int fi=0; fi<F.size(); fi++) {
        int[] t=F.get(fi);
        int[][] e={{t[0],t[1]},{t[1],t[2]},{t[2],t[0]}};
        for (int[] ed: e){
            long ek = edgeKeyUnordered(ed[0],ed[1]);
            edgeFaces.computeIfAbsent(ek,s->new ArrayList<>()).add(fi);
        }
    }
    double[] rho = new double[F.size()];
    for (int i=0;i<F.size();i++){
        int[] t=F.get(i);
        Point3D A=P.get(t[0]), B=P.get(t[1]), C=P.get(t[2]);
        double cx=(A.getX()+B.getX()+C.getX())/3.0;
        double cy=(A.getY()+B.getY()+C.getY())/3.0;
        double cz=(A.getZ()+B.getZ()+C.getZ())/3.0;
        double rx=cx-Cx, ry=cy-Cy, rz=cz-Cz;
        rho[i]=Math.sqrt(rx*rx+ry*ry+rz*rz);
    }
    boolean[] keep = new boolean[F.size()];
    Arrays.fill(keep,true);
    for (var e : edgeFaces.entrySet()){
        List<Integer> lst=e.getValue();
        if (lst.size()<=2) continue;
        lst.sort((i,j)->Double.compare(rho[j],rho[i]));
        for (int k=2;k<lst.size();k++) keep[lst.get(k)]=false;
    }
    ArrayList<int[]> F2=new ArrayList<>();
    for (int i=0;i<F.size();i++) if (keep[i]) F2.add(F.get(i));
    return new CleanResult(P,F2);
}
    /**
     * Clean a mesh with reasonable defaults based on scene size.
     * @param pts    original points (not modified)
     * @param faces  triangle list (each int[3] indices into pts)
     */
    public static CleanResult clean(List<Point3D> pts, List<int[]> faces){
        if (faces.isEmpty()) return new CleanResult(pts, faces);

        // Scene scale
        double minX=1e300,minY=1e300,minZ=1e300, maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for (Point3D p: pts){ double x=p.getX(),y=p.getY(),z=p.getZ();
            if (x<minX)minX=x; if (y<minY)minY=y; if (z<minZ)minZ=z;
            if (x>maxX)maxX=x; if (y>maxY)maxY=y; if (z>maxZ)maxZ=z;
        }
        double dx=maxX-minX, dy=maxY-minY, dz=maxZ-minZ;
        double sceneDiag = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Defaults: weld ~0.2% of diag, area threshold ~ (0.1% diag)^2
        double weldTol   = 0.002 * sceneDiag;
        double areaTol2  = Math.pow(0.001 * sceneDiag, 2);

        return clean(pts, faces, weldTol, areaTol2);
    }

    /**
     * Clean with custom tolerances.
     * @param weldTol    merge vertices closer than this
     * @param areaTol2   drop faces whose (2*area)^2 < areaTol2
     */
    public static CleanResult clean(List<Point3D> pts, List<int[]> faces,
                                    double weldTol, double areaTol2) {
        if (faces.isEmpty()) return new CleanResult(pts, faces);

        // 1) WELD: spatial hash grid
        double inv = (weldTol > 0) ? 1.0 / weldTol : 1e9;
        HashMap<Long, Integer> cellToVert = new HashMap<>(pts.size()*2);
        int n = pts.size();
        int[] map = new int[n];
        List<Point3D> newPts = new ArrayList<>(n);

        for (int i=0;i<n;i++){
            Point3D p = pts.get(i);
            long k = cellKey(p.getX(), p.getY(), p.getZ(), inv);
            Integer idx = cellToVert.get(k);
            if (idx == null) {
                idx = newPts.size();
                newPts.add(p);
                cellToVert.put(k, idx);
            }
            map[i] = idx;
        }

        // 2) REMAP & DROP degenerate/too small & DEDUPE unordered faces
        HashSet<Long> seenFace = new HashSet<>(faces.size()*2);
        ArrayList<int[]> F = new ArrayList<>(faces.size());

        for (int[] f : faces) {
            int a = map[f[0]], b = map[f[1]], c = map[f[2]];
            if (a==b || b==c || c==a) continue; // degenerate after weld

            // area2 = ||(b-a) x (c-a)||^2
            Point3D A=newPts.get(a), B=newPts.get(b), C=newPts.get(c);
            double ux=B.getX()-A.getX(), uy=B.getY()-A.getY(), uz=B.getZ()-A.getZ();
            double vx=C.getX()-A.getX(), vy=C.getY()-A.getY(), vz=C.getZ()-A.getZ();
            double cxvX = uy*vz - uz*vy, cxvY = uz*vx - ux*vz, cxvZ = ux*vy - uy*vx;
            double area2 = (cxvX*cxvX + cxvY*cxvY + cxvZ*cxvZ);
            if (area2 < areaTol2) continue;

            // unordered key to remove duplicates regardless of winding
            long key = triKeyUnordered(a,b,c);
            if (seenFace.add(key)) {
                F.add(new int[]{a,b,c});
            }
        }

        // 3) NON-MANIFOLD EDGE pruning: keep at most two faces per undirected edge
        //    Choose faces with largest centroid radius (favor outer skin)
        if (!F.isEmpty()) {
            // AABB center used as reference
            double minX=1e300,minY=1e300,minZ=1e300, maxX=-1e300,maxY=-1e300,maxZ=-1e300;
            for (Point3D p: newPts){ double x=p.getX(),y=p.getY(),z=p.getZ();
                if (x<minX)minX=x; if (y<minY)minY=y; if (z<minZ)minZ=z;
                if (x>maxX)maxX=x; if (y>maxY)maxY=y; if (z>maxZ)maxZ=z;
            }
            final double Cx=(minX+maxX)/2, Cy=(minY+maxY)/2, Cz=(minZ+maxZ)/2;

            // build edge→faces list
            HashMap<Long, ArrayList<Integer>> edgeFaces = new HashMap<>(F.size()*2);
            for (int fi=0; fi<F.size(); fi++) {
                int[] t = F.get(fi);
                int[][] e = {{t[0],t[1]}, {t[1],t[2]}, {t[2],t[0]}};
                for (int[] ed : e) {
                    long ek = edgeKeyUnordered(ed[0], ed[1]);
                    edgeFaces.computeIfAbsent(ek, s->new ArrayList<>()).add(fi);
                }
            }

            // centroid radius cache
            double[] rho = new double[F.size()];
            for (int i=0;i<F.size();i++){
                int[] t=F.get(i);
                Point3D A=newPts.get(t[0]), B=newPts.get(t[1]), C=newPts.get(t[2]);
                double cx=(A.getX()+B.getX()+C.getX())/3.0;
                double cy=(A.getY()+B.getY()+C.getY())/3.0;
                double cz=(A.getZ()+B.getZ()+C.getZ())/3.0;
                double rx=cx-Cx, ry=cy-Cy, rz=cz-Cz;
                rho[i] = Math.sqrt(rx*rx + ry*ry + rz*rz);
            }

            // mark faces to drop if an edge has >2 incident faces (keep top 2 by rho)
            boolean[] keep = new boolean[F.size()];
            Arrays.fill(keep, true);

            for (Map.Entry<Long, ArrayList<Integer>> e : edgeFaces.entrySet()) {
                List<Integer> lst = e.getValue();
                if (lst.size() <= 2) continue;
                // sort by descending rho
                lst.sort((i,j) -> Double.compare(rho[j], rho[i]));
                for (int k=2; k<lst.size(); k++) keep[lst.get(k)] = false;
            }

            ArrayList<int[]> F2 = new ArrayList<>(F.size());
            for (int i=0;i<F.size();i++) if (keep[i]) F2.add(F.get(i));
            F = F2;
        }

        return new CleanResult(newPts, F);
    }

    /* ---------- helpers ---------- */

    private static long cellKey(double x, double y, double z, double inv) {
        long ix = (long)Math.floor(x * inv);
        long iy = (long)Math.floor(y * inv);
        long iz = (long)Math.floor(z * inv);
        // 3D Morton-lite hash (simple mixing)
        long k = ix * 73856093L ^ iy * 19349663L ^ iz * 83492791L;
        return k;
    }

    /** Unordered triangle key for dedupe. */
    private static long triKeyUnordered(int a, int b, int c) {
        int i = Math.min(a, Math.min(b, c));
        int k = Math.max(a, Math.max(b, c));
        int j = a + b + c - i - k;
        return (((long) i) << 42) | (((long) j) << 21) | (long) k;
    }

    /** Unordered edge key. */
    private static long edgeKeyUnordered(int a, int b) {
        int i = Math.min(a, b), j = Math.max(a, b);
        return (((long)i) << 32) | (j & 0xffffffffL);
    }
}
