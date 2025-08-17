package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import java.util.*;

/** α-shape boundary from a Delaunay tetrahedralization. */
public final class AlphaShape3D {
    public static TriangleMesh build(List<Point3D> pts, double alpha, Delaunay3D delaunay) {
        List<int[]> tets = delaunay.tetrahedralize(pts);
        double a2 = alpha*alpha;

        BitSet keep = new BitSet(tets.size());
        for (int i=0;i<tets.size();i++) {
            int[] T = tets.get(i);
            double r2 = Geometry3D.circumsphere(pts.get(T[0]),pts.get(T[1]),pts.get(T[2]),pts.get(T[3])).r2();
            if (r2 <= a2) keep.set(i);
        }
        Map<Long,int[]> tri = new HashMap<>();
        Map<Long,Integer> cnt = new HashMap<>();
        for (int ti=keep.nextSetBit(0); ti>=0; ti=keep.nextSetBit(ti+1)) {
            int[] T = tets.get(ti);
            int[][] faces = {{T[0],T[1],T[2]}, {T[0],T[1],T[3]}, {T[0],T[2],T[3]}, {T[1],T[2],T[3]}};
            for (int[] f : faces) {
                long k = Geometry3D.triKeyUnordered(f[0],f[1],f[2]);
                cnt.put(k, cnt.getOrDefault(k,0)+1);
                tri.putIfAbsent(k, f);
            }
        }
        List<int[]> boundary = new ArrayList<>();
        for (var e : cnt.entrySet()) if (e.getValue()==1) boundary.add(tri.get(e.getKey()));

        double[] ctr = centroid(pts);
        List<int[]> faces = new ArrayList<>(boundary.size());
        for (int[] f : boundary) faces.add(orientedOutward(f, pts, ctr));

        return TriangleMeshExport.fromFaces(pts, faces);
    }

    private static double[] centroid(List<Point3D> pts){ double sx=0,sy=0,sz=0; int n=pts.size();
        for (Point3D p:pts){ sx+=p.getX(); sy+=p.getY(); sz+=p.getZ(); }
        return new double[]{sx/n, sy/n, sz/n}; }

    private static int[] orientedOutward(int[] f, List<Point3D> pts, double[] ctr){
        Point3D A=pts.get(f[0]), B=pts.get(f[1]), C=pts.get(f[2]);
        double ux=B.getX()-A.getX(), uy=B.getY()-A.getY(), uz=B.getZ()-A.getZ();
        double vx=C.getX()-A.getX(), vy=C.getY()-A.getY(), vz=C.getZ()-A.getZ();
        double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
        double cx=(A.getX()+B.getX()+C.getX())/3.0, cy=(A.getY()+B.getY()+C.getY())/3.0, cz=(A.getZ()+B.getZ()+C.getZ())/3.0;
        double dx=cx-ctr[0], dy=cy-ctr[1], dz=cz-ctr[2];
        return (nx*dx + ny*dy + nz*dz) >= 0 ? new int[]{f[0],f[1],f[2]} : new int[]{f[0],f[2],f[1]};
    }
}
