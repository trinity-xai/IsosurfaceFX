package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.List;

/** Geometric predicates; start tolerant, swap to exact later if needed. */
public interface Predicates3D {
    /** Signed 6*volume; >0 if D is above plane ABC (right-hand rule). */
    double orient3d(Point3D A, Point3D B, Point3D C, Point3D D);
    /** True if P is inside (or on) circumsphere of tetrahedron T. */
    boolean inSphere(List<Point3D> pts, int[] tet, Point3D P);
    /** Orientation epsilon threshold. */
    double orientEps();

    /** Default tolerant implementation using doubles. */
    static Predicates3D tolerant() {
        return new Predicates3D() {
            private static final double EPS_ORIENT = 1e-12, EPS_INSPHERE = 1e-10;
            @Override public double orient3d(Point3D A, Point3D B, Point3D C, Point3D D) {
                double adx=A.getX()-D.getX(), ady=A.getY()-D.getY(), adz=A.getZ()-D.getZ();
                double bdx=B.getX()-D.getX(), bdy=B.getY()-D.getY(), bdz=B.getZ()-D.getZ();
                double cdx=C.getX()-D.getX(), cdy=C.getY()-D.getY(), cdz=C.getZ()-D.getZ();
                return adx*(bdy*cdz - bdz*cdy) - ady*(bdx*cdz - bdz*cdx) + adz*(bdx*cdy - bdy*cdx);
            }
            @Override
            public boolean inSphere(List<Point3D> P, int[] T, Point3D X) {
                Point3D A = P.get(T[0]), B = P.get(T[1]), C = P.get(T[2]), D = P.get(T[3]);

                // Translate so D is at origin (improves conditioning)
                double ax = A.getX()-D.getX(), ay = A.getY()-D.getY(), az = A.getZ()-D.getZ();
                double bx = B.getX()-D.getX(), by = B.getY()-D.getY(), bz = B.getZ()-D.getZ();
                double cx = C.getX()-D.getX(), cy = C.getY()-D.getY(), cz = C.getZ()-D.getZ();
                double dx = X.getX()-D.getX(), dy = X.getY()-D.getY(), dz = X.getZ()-D.getZ();

                double a2 = ax*ax + ay*ay + az*az;
                double b2 = bx*bx + by*by + bz*bz;
                double c2 = cx*cx + cy*cy + cz*cz;
                double d2 = dx*dx + dy*dy + dz*dz;

                // 4x4 determinant for in-sphere (expanded with minors)
                double det =
                    ax * (by*cz*d2 + bz*cy*d2 - by*dz*c2 - bz*dy*c2 + cy*dz*b2 - cz*dy*b2)
                  - ay * (bx*cz*d2 + bz*cx*d2 - bx*dz*c2 - bz*dx*c2 + cx*dz*b2 - cz*dx*b2)
                  + az * (bx*cy*d2 + by*cx*d2 - bx*dy*c2 - by*dx*c2 + cx*dy*b2 - cy*dx*b2)
                  - a2 * (bx*(cy*dz - cz*dy) - by*(cx*dz - cz*dx) + bz*(cx*dy - cy*dx));

                // Orientation of tet ABCD (with D at origin)
                double orient =
                    ax*(by*cz - bz*cy) - ay*(bx*cz - bz*cx) + az*(bx*cy - by*cx);

                if (Math.abs(orient) < 1e-18) {
                    // Nearly degenerate tet → be permissive so the algorithm progresses
                    return true;
                }
                // For positively oriented tet, det > 0 means X is inside the circumsphere.
                // If orientation is negative, flip the sign.
                if (orient < 0) det = -det;

                return det >= -1e-12; // small tolerance
            }

            @Override public double orientEps() { return EPS_ORIENT; }
        };
    }
}
