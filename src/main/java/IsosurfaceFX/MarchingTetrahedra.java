package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;


public class MarchingTetrahedra {

    // The standard six tetrahedra for a unit cube
    private static final int[][] TETS = {
        {0, 5, 1, 6},
        {0, 1, 2, 6},
        {0, 2, 3, 6},
        {0, 3, 7, 6},
        {0, 7, 4, 6},
        {0, 4, 5, 6}
    };

    // Cube corners for reference (same as Marching Cubes)
    private static final int[][] CUBE_VERTS = {
        {0, 0, 0}, // 0
        {1, 0, 0}, // 1
        {1, 1, 0}, // 2
        {0, 1, 0}, // 3
        {0, 0, 1}, // 4
        {1, 0, 1}, // 5
        {1, 1, 1}, // 6
        {0, 1, 1}  // 7
    };

    // Edge pairs for a tetrahedron
    private static final int[][] TET_EDGES = {
        {0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}
    };

    // For each of the 16 cases, up to 2 triangles per tetrahedron
    // Each triangle: list of 3 edge numbers
    // -1 as sentinel (unused)
    private static final int[][][] TRI_TABLE = {
        {}, // 0000
        {{0, 2, 1}}, // 0001
        {{0, 3, 4}}, // 0010
        {{1, 2, 4}, {1, 4, 3}}, // 0011
        {{1, 5, 3}}, // 0100
        {{0, 2, 5}, {0, 5, 3}}, // 0101
        {{0, 1, 5}, {0, 5, 4}}, // 0110
        {{2, 5, 4}}, // 0111
        {{2, 5, 4}}, // 1000
        {{0, 1, 5}, {0, 5, 4}}, // 1001
        {{0, 2, 5}, {0, 5, 3}}, // 1010
        {{1, 5, 3}}, // 1011
        {{1, 2, 4}, {1, 4, 3}}, // 1100
        {{0, 3, 4}}, // 1101
        {{0, 2, 1}}, // 1110
        {} // 1111
    };

    private final VoxelGrid grid;
    private final double isovalue;
    private final boolean interpolate;

    public MarchingTetrahedra(VoxelGrid grid, double isovalue, boolean interpolate) {
        this.grid = grid;
        this.isovalue = isovalue;
        this.interpolate = interpolate;
    }

    // Main mesh generation: returns a ready-to-use TriangleMesh
    public TriangleMesh generateMesh() {
        List<Point3D> vertices = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        Map<Point3D, Integer> vertexMap = new HashMap<>();

        int nx = grid.getDimX();
        int ny = grid.getDimY();
        int nz = grid.getDimZ();

        for (int x = 0; x < nx - 1; x++) {
            for (int y = 0; y < ny - 1; y++) {
                for (int z = 0; z < nz - 1; z++) {
                    processCube(x, y, z, vertices, faces, vertexMap);
                }
            }
        }

        // Build JavaFX TriangleMesh
        TriangleMesh mesh = new TriangleMesh();
        float[] pointsArray = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            Point3D p = vertices.get(i);
            pointsArray[3 * i] = (float) p.getX();
            pointsArray[3 * i + 1] = (float) p.getY();
            pointsArray[3 * i + 2] = (float) p.getZ();
        }
        mesh.getPoints().setAll(pointsArray);
        mesh.getTexCoords().setAll(0, 0);
        int[] facesArray = faces.stream().mapToInt(Integer::intValue).toArray();
        mesh.getFaces().setAll(facesArray);

        return mesh;
    }

    private void processCube(int x, int y, int z,
                             List<Point3D> vertices,
                             List<Integer> faces,
                             Map<Point3D, Integer> vertexMap) {
        // Get positions and values for the 8 corners
        Point3D[] cornerPos = new Point3D[8];
        float[] cornerVal = new float[8];
        for (int i = 0; i < 8; i++) {
            int dx = CUBE_VERTS[i][0], dy = CUBE_VERTS[i][1], dz = CUBE_VERTS[i][2];
            cornerPos[i] = grid.getWorldPosition(x + dx, y + dy, z + dz);
            cornerVal[i] = grid.get(x + dx, y + dy, z + dz);
        }
        // For each of the 6 tetrahedra
        for (int[] tet : TETS) {
            Point3D[] tetPos = new Point3D[4];
            float[] tetVal = new float[4];
            for (int i = 0; i < 4; i++) {
                tetPos[i] = cornerPos[tet[i]];
                tetVal[i] = cornerVal[tet[i]];
            }
            // Build bitmask for case
            int caseIdx = 0;
            for (int i = 0; i < 4; i++)
                if (tetVal[i] < isovalue) caseIdx |= (1 << i);
            int[][] triDefs = TRI_TABLE[caseIdx];
            for (int[] tri : triDefs) {
                int[] vIdx = new int[3];
                for (int j = 0; j < 3; j++) {
                    int e = tri[j];
                    int a = TET_EDGES[e][0];
                    int b = TET_EDGES[e][1];
                    Point3D pa = tetPos[a], pb = tetPos[b];
                    float va = tetVal[a], vb = tetVal[b];
                    Point3D interpPt = interpolate(pa, pb, va, vb);
                    // Deduplicate by coordinates
                    vIdx[j] = vertexMap.computeIfAbsent(interpPt, v -> {
                        vertices.add(v);
                        return vertices.size() - 1;
                    });
                }
                // Add triangle to mesh (ensure winding is outward—flip if needed)
                faces.add(vIdx[0]); faces.add(0);
                faces.add(vIdx[1]); faces.add(0);
                faces.add(vIdx[2]); faces.add(0);
            }
        }
    }

    // Linear interpolation between two points
    private Point3D interpolate(Point3D p1, Point3D p2, float v1, float v2) {
        if (!interpolate || Math.abs(isovalue - v1) < 1e-5)
            return p1;
        if (Math.abs(isovalue - v2) < 1e-5)
            return p2;
        if (Math.abs(v1 - v2) < 1e-5)
            return p1;
        double mu = (isovalue - v1) / (v2 - v1);
        return new Point3D(
                p1.getX() + mu * (p2.getX() - p1.getX()),
                p1.getY() + mu * (p2.getY() - p1.getY()),
                p1.getZ() + mu * (p2.getZ() - p1.getZ())
        );
    }
}
