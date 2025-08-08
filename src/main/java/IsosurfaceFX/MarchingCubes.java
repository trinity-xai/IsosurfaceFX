package IsosurfaceFX;

/**
 *
 * @author Sean PHillips
 */
import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MarchingCubes {

    private final VoxelGrid grid;
    private final double isovalue;
    private final boolean interpolate;

    private final int nx, ny, nz;
    // For each grid edge, the index of its vertex in the vertex list, or -1 if none yet
    private final int[][][][] edgeVertexGrid;

    // The outputs for mesh construction
    private final List<Point3D> vertexList = new ArrayList<>();
    private final List<Integer> faceList = new ArrayList<>();

    // Marching Cubes lookup tables (from your original code)
    private static final int[] EDGE_TABLE = MarchingCubesLookupTables.EDGE_TABLE;
    private static final int[][] TRI_TABLE = MarchingCubesLookupTables.TRIANGLE_TABLE;

    // Standard cube corner offset, as before
    private static final int[][] VERTEX_OFFSETS = {
        {0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0},
        {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}
    };

    // For each edge, the indices of its corner endpoints
    private static final int[][] EDGE_VERTICES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    public MarchingCubes(VoxelGrid grid, double isovalue, boolean interpolate) {
        this.grid = grid;
        this.isovalue = isovalue;
        this.interpolate = interpolate;
        this.nx = grid.getDimX();
        this.ny = grid.getDimY();
        this.nz = grid.getDimZ();
        this.edgeVertexGrid = new int[nx][ny][nz][12];
        // Init all indices to -1
        for (int x = 0; x < nx; x++)
            for (int y = 0; y < ny; y++)
                for (int z = 0; z < nz; z++)
                    Arrays.fill(edgeVertexGrid[x][y][z], -1);
    }

    // Call this method to generate the mesh for use with JavaFX TriangleMesh
    public MeshData generateMeshData() {
        for (int x = 0; x < nx - 1; x++) {
            for (int y = 0; y < ny - 1; y++) {
                for (int z = 0; z < nz - 1; z++) {
                    processCube(x, y, z);
                }
            }
        }
        return new MeshData(vertexList, faceList);
    }

    private void processCube(int x, int y, int z) {
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

        int[] edgeIndices = new int[12];

        // For each edge in the cube that is active, get or create its vertex index
        for (int edge = 0; edge < 12; edge++) {
            if ((EDGE_TABLE[cubeIndex] & (1 << edge)) != 0) {
                int[] vIdx = EDGE_VERTICES[edge];
                // Determine which grid cell "owns" this edge (only assign vertex once)
                int ex = x, ey = y, ez = z;
                // For shared edges, always assign to the cube with the smallest (x, y, z)
                // Map for each edge index which direction you must decrement if needed
                switch (edge) {
                    case 0:  break;
                    case 1:  ex += 1; break;
                    case 2:  ex += 1; ey += 1; break;
                    case 3:  ey += 1; break;
                    case 4:  ez += 1; break;
                    case 5:  ex += 1; ez += 1; break;
                    case 6:  ex += 1; ey += 1; ez += 1; break;
                    case 7:  ey += 1; ez += 1; break;
                    case 8:  break;
                    case 9:  ex += 1; break;
                    case 10: ex += 1; ey += 1; break;
                    case 11: ey += 1; break;
                }
                // Check if this edge vertex already exists
                if (edgeVertexGrid[ex][ey][ez][edge] < 0) {
                    Point3D p1 = cubeCorners[vIdx[0]];
                    Point3D p2 = cubeCorners[vIdx[1]];
                    float val1 = cubeValues[vIdx[0]];
                    float val2 = cubeValues[vIdx[1]];
                    Point3D vert = interpolate(p1, p2, val1, val2);
                    edgeVertexGrid[ex][ey][ez][edge] = vertexList.size();
                    vertexList.add(vert);
                }
                edgeIndices[edge] = edgeVertexGrid[ex][ey][ez][edge];
            }
        }

        int[] triTable = TRI_TABLE[cubeIndex];
        for (int i = 0; i < triTable.length; i += 3) {
            if (triTable[i] == -1) break;
            if (i + 2 >= triTable.length) break;
            int a = triTable[i];
            int b = triTable[i + 1];
            int c = triTable[i + 2];
            // For JavaFX, counterclockwise when viewed from outside is standard
            faceList.add(edgeIndices[a]);
            faceList.add(0); // dummy tex coord
            faceList.add(edgeIndices[c]);
            faceList.add(0);
            faceList.add(edgeIndices[b]);
            faceList.add(0);
        }
    }

    // Standard MC edge vertex interpolation
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

    /** 
     * Container for the mesh data, ready for TriangleMesh. 
     * You must still convert Point3D to float[] and int[] as required by JavaFX.
     */
    public static class MeshData {
        public final List<Point3D> vertices;
        public final List<Integer> faces;
        public MeshData(List<Point3D> vertices, List<Integer> faces) {
            this.vertices = vertices;
            this.faces = faces;
        }
    }
}