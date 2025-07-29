package IsosurfaceFX;

/**
 *
 * @author Sean PHillips
 */
import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.List;

public class MarchingCubes {
    private final VoxelGrid grid;
    private final double isovalue;
    private final boolean interpolate;

    public MarchingCubes(VoxelGrid grid, double isovalue, boolean interpolate) {
        this.grid = grid;
        this.isovalue = isovalue;
        this.interpolate = interpolate;
    }

    public List<Triangle3D> generateMesh() {
        List<Triangle3D> triangles = new ArrayList<>();

        int nx = grid.getDimX();
        int ny = grid.getDimY();
        int nz = grid.getDimZ();

        for (int x = 0; x < nx - 1; x++) {
            for (int y = 0; y < ny - 1; y++) {
                for (int z = 0; z < nz - 1; z++) {
                    processCube(x, y, z, triangles);
                }
            }
        }

        return triangles;
    }

    private void processCube(int x, int y, int z, List<Triangle3D> triangles) {
        float[] cubeValues = new float[8];
        Point3D[] cubeCorners = new Point3D[8];

        for (int i = 0; i < 8; i++) {
            int dx = VERTEX_OFFSETS[i][0];
            int dy = VERTEX_OFFSETS[i][1];
            int dz = VERTEX_OFFSETS[i][2];
            cubeValues[i] = grid.get(x + dx, y + dy, z + dz);
            cubeCorners[i] = grid.getWorldPosition(x + dx, y + dy, z + dz);
        }

        int cubeIndex = 0;
        for (int i = 0; i < 8; i++) {
            if (cubeValues[i] < isovalue) {
                cubeIndex |= (1 << i);
            }
        }

        if (EDGE_TABLE[cubeIndex] == 0)
            return;

        Point3D[] edgeVertices = new Point3D[12];
        for (int i = 0; i < 12; i++) {
            if ((EDGE_TABLE[cubeIndex] & (1 << i)) != 0) {
                int v1 = EDGE_VERTICES[i][0];
                int v2 = EDGE_VERTICES[i][1];
                edgeVertices[i] = interpolate(
                    cubeCorners[v1], cubeCorners[v2],
                    cubeValues[v1], cubeValues[v2]
                );
            }
        }

        int[] triTable = TRI_TABLE[cubeIndex];
        for (int i = 0; i < triTable.length; i += 3) {
            if (triTable[i] == -1) break; //sentinel terminator
            if (i + 2 >= triTable.length) break; // defensive overflow check            
            int a = triTable[i];
            int b = triTable[i + 1];
            int c = triTable[i + 2];

            if (a == -1 || b == -1 || c == -1) break; //break at sentinel value

            triangles.add(new Triangle3D(
                edgeVertices[a],
                edgeVertices[b],
                edgeVertices[c]
            ));
        }
    }

    private Point3D interpolate(Point3D p1, Point3D p2, float val1, float val2) {
        if (!interpolate || Math.abs(isovalue - val1) < 1e-5)
            return p1;
        if (Math.abs(isovalue - val2) < 1e-5)
            return p2;
        if (Math.abs(val1 - val2) < 1e-5)
            return p1;

        double mu = (isovalue - val1) / (val2 - val1);
        return new Point3D(
            p1.getX() + mu * (p2.getX() - p1.getX()),
            p1.getY() + mu * (p2.getY() - p1.getY()),
            p1.getZ() + mu * (p2.getZ() - p1.getZ())
        );
    }

    // Standard MC corner index -> offset
    private static final int[][] VERTEX_OFFSETS = {
        {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
        {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}
    };

    // Edge-to-vertex index pairs
    private static final int[][] EDGE_VERTICES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    // Edge table: tells which edges are active for each cubeIndex
    private static final int[] EDGE_TABLE = MarchingCubesLookupTables.EDGE_TABLE;

    // Triangle table: maps cubeIndex to list of edges to form triangles
    private static final int[][] TRI_TABLE = MarchingCubesLookupTables.TRIANGLE_TABLE;
}
