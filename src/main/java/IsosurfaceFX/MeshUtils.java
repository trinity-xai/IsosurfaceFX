package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */
import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import javafx.scene.shape.TriangleMesh;

public final class MeshUtils {

    private MeshUtils() {
    }

    public static TriangleMesh toTriangleMesh(Point3d[] vertices, List<int[]> faces) {
        // Ensure outward orientation
        MeshUtils.orientFacesOutward(vertices, faces);

        TriangleMesh mesh = new TriangleMesh();

        // Points
        float[] pts = new float[vertices.length * 3];
        for (int i = 0; i < vertices.length; i++) {
            pts[3 * i] = (float) vertices[i].x;
            pts[3 * i + 1] = (float) vertices[i].y;
            pts[3 * i + 2] = (float) vertices[i].z;
        }
        mesh.getPoints().setAll(pts);

        // Dummy texcoords
        mesh.getTexCoords().setAll(0, 0);

        // Faces (after outward fix, write v0,v1,v2 in CCW)
        int[] faceData = new int[faces.size() * 6]; // (p,t) * 3
        int k = 0;
        for (int[] f : faces) {
            faceData[k++] = f[0];
            faceData[k++] = 0;
            faceData[k++] = f[1];
            faceData[k++] = 0;
            faceData[k++] = f[2];
            faceData[k++] = 0;
        }
        mesh.getFaces().setAll(faceData);

        // Optional: smooth shading
         int[] groups = MeshUtils.singleSmoothingGroup(faces.size());
         mesh.getFaceSmoothingGroups().setAll(groups);
        return mesh;
    }

    /**
     * Re-orient each face so its geometric normal points away from the mesh
     * centroid.
     */
    public static void orientFacesOutward(Point3d[] verts, List<int[]> faces) {
        // Mesh centroid (vertex-average is fine for orientation)
        double cx = 0, cy = 0, cz = 0;
        for (Point3d p : verts) {
            cx += p.x;
            cy += p.y;
            cz += p.z;
        }
        int vn = Math.max(verts.length, 1);
        cx /= vn;
        cy /= vn;
        cz /= vn;

        for (int[] f : faces) {
            int i0 = f[0], i1 = f[1], i2 = f[2];
            Point3d a = verts[i0], b = verts[i1], c = verts[i2];

            // n = (b-a) x (c-a)
            double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
            double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
            double nx = aby * acz - abz * acy;
            double ny = abz * acx - abx * acz;
            double nz = abx * acy - aby * acx;

            // triangle centroid
            double tx = (a.x + b.x + c.x) / 3.0;
            double ty = (a.y + b.y + c.y) / 3.0;
            double tz = (a.z + b.z + c.z) / 3.0;

            // vector from mesh centroid to triangle centroid
            double vx = tx - cx, vy = ty - cy, vz = tz - cz;

            // If normal points inward (n · v < 0), swap to flip winding
            double dot = nx * vx + ny * vy + nz * vz;
            if (dot < 0.0) {
                int tmp = f[1];
                f[1] = f[2];
                f[2] = tmp;
            }
        }
    }

    /**
     * Optional: single smoothing group for nicer shading in JavaFX.
     */
    public static int[] singleSmoothingGroup(int triangleCount) {
        int[] groups = new int[triangleCount];
        Arrays.fill(groups, 1);
        return groups;
    }
public static double medianHullEdgeLength(Point3d[] verts, int[][] faces) {
    List<Double> L = new ArrayList<>();
    for (int[] f : faces) {
        int a=f[0], b=f[1], c=f[2];
        L.add(verts[a].distance(verts[b]));
        L.add(verts[b].distance(verts[c]));
        L.add(verts[c].distance(verts[a]));
    }
    Collections.sort(L);
    int n = L.size();
    return (n%2==1) ? L.get(n/2) : 0.5*(L.get(n/2-1)+L.get(n/2));
}
    public static List<int[]> concaveHullFaces(QuickHull3D hull, double alpha) {
        // Get all hull vertices and all faces (as vertex indices)
        Point3d[] points = hull.getVertices();
        int[][] faces = hull.getFaces();

        List<int[]> concaveFaces = new ArrayList<>();

        for (int[] face : faces) {
            Point3d a = points[face[0]];
            Point3d b = points[face[1]];
            Point3d c = points[face[2]];

            double r = getTriangleCircumradius(a, b, c);
            if (r <= alpha) {
                concaveFaces.add(face);
            }
        }
        return concaveFaces;
    }

    public static double getTriangleCircumradius(Point3d a, Point3d b, Point3d c) {
        double ab = a.distance(b);
        double bc = b.distance(c);
        double ca = c.distance(a);
        double s = (ab + bc + ca) / 2.0;
        double area = Math.sqrt(s * (s - ab) * (s - bc) * (s - ca));
        if (area == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (ab * bc * ca) / (4.0 * area);
    }
}
