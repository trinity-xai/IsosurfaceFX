package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;


public class TriangleMeshConverter {

    public static TriangleMesh convertToMesh(List<Triangle3D> triangles) {
        Map<Point3D, Integer> vertexIndexMap = new LinkedHashMap<>();
        List<Float> pointsList = new ArrayList<>();
        List<Integer> facesList = new ArrayList<>();

        // We use a dummy texture coordinate because TriangleMesh requires it
        float[] texCoords = new float[]{0, 0};

        for (Triangle3D tri : triangles) {
            for (Point3D vertex : new Point3D[]{tri.p1, tri.p2, tri.p3}) {
                int index = vertexIndexMap.computeIfAbsent(vertex, v -> {
                    pointsList.add((float) v.getX());
                    pointsList.add((float) v.getY());
                    pointsList.add((float) v.getZ());
                    return (pointsList.size() / 3) - 1;
                });
                // Each face index refers to a point and a texture coord index
                facesList.add(index); // point index
                facesList.add(0);     // texCoord index (dummy)
            }
        }

        TriangleMesh mesh = new TriangleMesh();

        // Set vertices
        float[] points = new float[pointsList.size()];
        for (int i = 0; i < pointsList.size(); i++) {
            points[i] = pointsList.get(i);
        }
        mesh.getPoints().setAll(points);

        // Set dummy tex coords
        mesh.getTexCoords().setAll(texCoords);

        // Set triangle faces
        int[] faces = new int[facesList.size()];
        for (int i = 0; i < facesList.size(); i++) {
            faces[i] = facesList.get(i);
        }
        mesh.getFaces().setAll(faces);

        return mesh;
    }
}

