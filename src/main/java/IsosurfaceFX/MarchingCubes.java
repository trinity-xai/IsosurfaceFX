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

    // ---- Lookup tables (reuse your existing class) ----
    private static final int[]   EDGE_TABLE  = MarchingCubesLookupTables.EDGE_TABLE;
    private static final int[][] TRI_TABLE   = MarchingCubesLookupTables.TRIANGLE_TABLE;

    // Cube corner offsets (same convention as your MC/MT)
    private static final int[][] CUBE_VERTS = {
        {0,0,0},{1,0,0},{1,1,0},{0,1,0},
        {0,0,1},{1,0,1},{1,1,1},{0,1,1}
    };

    // 12 cube edges as corner pairs (matches TRI_TABLE edge indices)
    private static final int[][] CUBE_EDGE_VERTS = {
        {0,1}, {1,2}, {2,3}, {3,0},
        {4,5}, {5,6}, {6,7}, {7,4},
        {0,4}, {1,5}, {2,6}, {3,7}
    };

    // Which axis each cube edge lies on: 0=x, 1=y, 2=z
    private static final int[] EDGE_AXIS = {0,1,0,1, 0,1,0,1, 2,2,2,2};

    // ----------------------------------------------------

    private final VoxelGrid grid;
    private final double isovalue;
    private final boolean interpolate;
    private final boolean outwardWithIncreasingValues; // true=SDF (increase outward), false=density (decrease outward)

    // Shared across cubes: [nx][ny][nz][12] -> vertex index (or -1)
    private int[][][][] edgeVertexCache;

    public static final class MeshData {
        public final List<Point3D> vertices;
        public final List<Integer> faces; // JavaFX (pointIndex, texIndex) repeating; texIndex is 0 always
        public MeshData(List<Point3D> v, List<Integer> f) { this.vertices = v; this.faces = f; }
    }

    public MarchingCubes(VoxelGrid grid, double isovalue, boolean interpolate) {
        this(grid, isovalue, interpolate, true); // default assumes SDF
    }
    public MarchingCubes(VoxelGrid grid, double isovalue, boolean interpolate, boolean outwardWithIncreasingValues) {
        this.grid = grid;
        this.isovalue = isovalue;
        this.interpolate = interpolate;
        this.outwardWithIncreasingValues = outwardWithIncreasingValues;
    }

    public MeshData generateMeshData() {
        int nx = grid.getDimX(), ny = grid.getDimY(), nz = grid.getDimZ();

        edgeVertexCache = new int[nx][ny][nz][12];
        for (int x=0;x<nx;x++)
            for (int y=0;y<ny;y++)
                for (int z=0;z<nz;z++)
                    Arrays.fill(edgeVertexCache[x][y][z], -1);

        List<Point3D> vertices = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        for (int x=0; x<nx-1; x++) {
            for (int y=0; y<ny-1; y++) {
                for (int z=0; z<nz-1; z++) {
                    processCube(x,y,z, vertices, faces);
                }
            }
        }
        return new MeshData(vertices, faces);
    }

    // ---- core per-cube extraction with gradient-based winding correction ----
    private void processCube(int x, int y, int z, List<Point3D> vertices, List<Integer> faces) {

        // 1) Gather corner positions & scalar values
        Point3D[] p = new Point3D[8];
        float[]   v = new float[8];
        for (int i=0;i<8;i++) {
            int dx = CUBE_VERTS[i][0], dy = CUBE_VERTS[i][1], dz = CUBE_VERTS[i][2];
            p[i] = grid.getWorldPosition(x+dx, y+dy, z+dz);
            v[i] = grid.get(x+dx, y+dy, z+dz);
        }

        // 2) Compute cubeIndex
        int cubeIndex = 0;
        for (int i=0;i<8;i++) if (v[i] < isovalue) cubeIndex |= (1<<i);

        // No intersection?
        int edgeMask = EDGE_TABLE[cubeIndex];
        if (edgeMask == 0) return;

        // 3) For each active edge, ensure we have a cached vertex index
        int[] edgeToVertexIndex = new int[12]; // indices in 'vertices' list
        Arrays.fill(edgeToVertexIndex, -1);

        for (int e=0; e<12; e++) {
            if ((edgeMask & (1<<e)) == 0) continue;

            int a = CUBE_EDGE_VERTS[e][0];
            int b = CUBE_EDGE_VERTS[e][1];

            // Determine which cell "owns" this edge (only decrement along the edge axis)
            int ex = x, ey = y, ez = z;
            int axis = EDGE_AXIS[e];
            int[] va = CUBE_VERTS[a];
            int[] vb = CUBE_VERTS[b];
            if (axis == 0 && vb[0] < va[0]) ex--;
            if (axis == 1 && vb[1] < va[1]) ey--;
            if (axis == 2 && vb[2] < va[2]) ez--;

            // Guard for outer border
            if (ex < 0 || ey < 0 || ez < 0 ||
                ex >= edgeVertexCache.length ||
                ey >= edgeVertexCache[0].length ||
                ez >= edgeVertexCache[0][0].length) {
                // We are on extreme grid boundary: just compute (no cache)
                Point3D ip = interpolate(p[a], p[b], v[a], v[b]);
                int idx = vertices.size();
                vertices.add(ip);
                edgeToVertexIndex[e] = idx;
                continue;
            }

            int idx = edgeVertexCache[ex][ey][ez][e];
            if (idx == -1) {
                Point3D ip = interpolate(p[a], p[b], v[a], v[b]);
                idx = vertices.size();
                vertices.add(ip);
                edgeVertexCache[ex][ey][ez][e] = idx;
            }
            edgeToVertexIndex[e] = idx;
        }

        // 4) Emit triangles from TRI_TABLE[cubeIndex]
        int[] tri = TRI_TABLE[cubeIndex];
        for (int i=0; i<tri.length; i+=3) {
            int e0 = tri[i];
            if (e0 == -1) break;
            int e1 = tri[i+1];
            int e2 = tri[i+2];

            int i0 = edgeToVertexIndex[e0];
            int i1 = edgeToVertexIndex[e1];
            int i2 = edgeToVertexIndex[e2];
            if (i0 < 0 || i1 < 0 || i2 < 0) continue; // safety

            // --- Gradient-based outward orientation (safer) ---
            Point3D a = vertices.get(i0);
            Point3D b = vertices.get(i1);
            Point3D c = vertices.get(i2);

            Point3D n = b.subtract(a).crossProduct(c.subtract(a)); // geometric normal
            Point3D center = new Point3D(
                (a.getX()+b.getX()+c.getX())/3.0,
                (a.getY()+b.getY()+c.getY())/3.0,
                (a.getZ()+b.getZ()+c.getZ())/3.0
            );
            Point3D g = gradientAt(center);
            double gLen = g.magnitude();
            if (gLen > 1e-6) {
                double dot = n.dotProduct(g);
                boolean flip = outwardWithIncreasingValues ? (dot < 0.0) : (dot > 0.0);
                if (flip) {
                    // swap i1 <-> i2
                    int tmp = i1; i1 = i2; i2 = tmp;
                }
            }
            // JavaFX winding (CCW is front). After possible flip, write CCW:
            faces.add(i0); faces.add(0);
            faces.add(i1); faces.add(0);
            faces.add(i2); faces.add(0);
        }
    }

    // ---- gradient sampler (central differences in grid space, normalized) ----
    private Point3D gradientAt(Point3D p) {
        double inv = 1.0 / grid.getVoxelSize();
        Point3D o = grid.getOrigin();

        double gx = (p.getX() - o.getX()) * inv;
        double gy = (p.getY() - o.getY()) * inv;
        double gz = (p.getZ() - o.getZ()) * inv;

        int x = (int)Math.round(gx);
        int y = (int)Math.round(gy);
        int z = (int)Math.round(gz);

        x = Math.max(1, Math.min(grid.getDimX()-2, x));
        y = Math.max(1, Math.min(grid.getDimY()-2, y));
        z = Math.max(1, Math.min(grid.getDimZ()-2, z));

        float dx = (grid.get(x+1,y,z) - grid.get(x-1,y,z)) * 0.5f;
        float dy = (grid.get(x,y+1,z) - grid.get(x,y-1,z)) * 0.5f;
        float dz = (grid.get(x,y,z+1) - grid.get(x,y,z-1)) * 0.5f;

        Point3D g = new Point3D(dx, dy, dz);
        double m = g.magnitude();
        return (m > 1e-12) ? g.multiply(1.0/m) : g;
    }

    // ---- linear interpolation along an edge ----
    private Point3D interpolate(Point3D p1, Point3D p2, float v1, float v2) {
        if (!interpolate || Math.abs(isovalue - v1) < 1e-5) return p1;
        if (Math.abs(isovalue - v2) < 1e-5) return p2;
        if (Math.abs(v1 - v2) < 1e-5)       return p1;
        double mu = (isovalue - v1) / (v2 - v1);
        return new Point3D(
            p1.getX() + mu * (p2.getX() - p1.getX()),
            p1.getY() + mu * (p2.getY() - p1.getY()),
            p1.getZ() + mu * (p2.getZ() - p1.getZ())
        );
    }
}
