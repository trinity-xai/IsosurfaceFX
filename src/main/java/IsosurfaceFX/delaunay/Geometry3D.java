package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.List;

public final class Geometry3D {
    private Geometry3D(){}

public static int[] addSuperTetrahedron(List<Point3D> pts) {
    // AABB center & size
    double minX=+1e300,minY=+1e300,minZ=+1e300, maxX=-1e300,maxY=-1e300,maxZ=-1e300;
    for (Point3D p: pts) {
        minX=Math.min(minX,p.getX()); minY=Math.min(minY,p.getY()); minZ=Math.min(minZ,p.getZ());
        maxX=Math.max(maxX,p.getX()); maxY=Math.max(maxY,p.getY()); maxZ=Math.max(maxZ,p.getZ());
    }
    double cx=(minX+maxX)/2.0, cy=(minY+maxY)/2.0, cz=(minZ+maxZ)/2.0;
    double size=Math.max(Math.max(maxX-minX, maxY-minY), maxZ-minZ);

    // Sphere radius large enough to enclose all points comfortably
    double Rbig = size * 100 + 1; // 100x the scene size is plenty

    // Regular tetrahedron on the sphere of radius Rbig
    // Use the 4 cube corners (±1,±1,±1) that form a regular tet; normalize to radius Rbig
    double s = Rbig / Math.sqrt(3.0);
    Point3D v0 = new Point3D(cx + s, cy + s, cz + s);
    Point3D v1 = new Point3D(cx - s, cy - s, cz + s);
    Point3D v2 = new Point3D(cx - s, cy + s, cz - s);
    Point3D v3 = new Point3D(cx + s, cy - s, cz - s);

    int i0=pts.size(); pts.add(v0);
    int i1=pts.size(); pts.add(v1);
    int i2=pts.size(); pts.add(v2);
    int i3=pts.size(); pts.add(v3);
    return new int[]{ i0,i1,i2,i3 };
}

    public static boolean usesAny(int[] tet, int[] superIdx) {
        for (int v : tet) for (int s : superIdx) if (v==s) return true;
        return false;
    }

    /** Key for unordered triangle (pack 3 sorted ints). */
    public static long triKeyUnordered(int a,int b,int c){
        int i=Math.min(a,Math.min(b,c)), k=Math.max(a,Math.max(b,c));
        int j=a+b+c - i - k; return (((long)i)<<42)|(((long)j)<<21)|(long)k;
    }

    /** Circumsphere of tetrahedron ABCD relative to A. */
    public static Circum circumsphere(Point3D A, Point3D B, Point3D C, Point3D D) {
        double bx=B.getX()-A.getX(), by=B.getY()-A.getY(), bz=B.getZ()-A.getZ();
        double cx=C.getX()-A.getX(), cy=C.getY()-A.getY(), cz=C.getZ()-A.getZ();
        double dx=D.getX()-A.getX(), dy=D.getY()-A.getY(), dz=D.getZ()-A.getZ();
        double bb=bx*bx+by*by+bz*bz, cc=cx*cx+cy*cy+cz*cz, dd=dx*dx+dy*dy+dz*dz;
        double m11=bx,m12=by,m13=bz, m21=cx,m22=cy,m23=cz, m31=dx,m32=dy,m33=dz;
        double det = m11*(m22*m33 - m23*m32) - m12*(m21*m33 - m23*m31) + m13*(m21*m32 - m22*m31);
        double denom = 2.0*det; if (Math.abs(denom) < 1e-18) return Circum.invalid();
        double t1=0.5*bb, t2=0.5*cc, t3=0.5*dd;
        double cxr=( t1*(m22*m33 - m23*m32) - m12*(t2*m33 - m23*t3) + m13*(t2*m32 - m22*t3) )/denom;
        double cyr=( m11*(t2*m33 - m23*t3) - t1*(m21*m33 - m23*m31) + m13*(m21*t3 - t2*m31) )/denom;
        double czr=( m11*(m22*t3 - t2*m32) - m12*(m21*t3 - t2*m31) + t1*(m21*m32 - m22*m31) )/denom;
        double r2 = cxr*cxr + cyr*cyr + czr*czr;
        return new Circum(true, A.getX()+cxr, A.getY()+cyr, A.getZ()+czr, r2);
    }

    public record Circum(boolean valid, double cx, double cy, double cz, double r2) {
        public static Circum invalid(){ return new Circum(false,0,0,0,0); }
    }
}
