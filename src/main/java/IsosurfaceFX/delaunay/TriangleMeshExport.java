package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;
import java.util.List;

/** Keeps export concerns out of the geometry code. */
public final class TriangleMeshExport {
    private TriangleMeshExport(){}
    public static TriangleMesh fromFaces(List<Point3D> pts, List<int[]> faces){
        TriangleMesh mesh = new TriangleMesh();
        for (Point3D p: pts) mesh.getPoints().addAll((float)p.getX(), (float)p.getY(), (float)p.getZ());
        mesh.getTexCoords().addAll(0,0);
        for (int[] f: faces) mesh.getFaces().addAll(f[0],0,f[1],0,f[2],0);
        return mesh;
    }
}
