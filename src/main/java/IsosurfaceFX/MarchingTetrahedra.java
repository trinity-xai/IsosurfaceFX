package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

public class MarchingTetrahedra {

    // ──────────────────────────────
    // Cube → 6 tetrahedra subdivision (matches your setup)
    // ──────────────────────────────
    private static final int[][] TETS = {
        {0, 5, 1, 6},
        {0, 1, 2, 6},
        {0, 2, 3, 6},
        {0, 3, 7, 6},
        {0, 7, 4, 6},
        {0, 4, 5, 6}
    };

    // Cube corner integer offsets
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

    // 6 edges of a tetrahedron (local indices 0..3)
    private static final int[][] TET_EDGES = {
        {0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}
    };

    // 12 cube edges as pairs of cube corner indices
    private static final int[][] CUBE_EDGE_VERTS = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0},
        {4, 5}, {5, 6}, {6, 7}, {7, 4},
        {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };

    // For each cube edge: which axis it lies on (0=x, 1=y, 2=z)
    private static final int[] EDGE_AXIS = {0, 1, 0, 1, 0, 1, 0, 1, 2, 2, 2, 2};

    // 16 MT cases; each triangle is given by 3 *tet edge indices*
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
    private final boolean interpolate;                 // linear interpolation along edges
    private final boolean outwardWithIncreasingValues; // true=SDF (inside<outside), false=Gaussian density (inside>outside)

    // global cache for cube edge vertices: [nx][ny][nz][12]
    private int[][][][] edgeVertexCache;

    // output pools
    private List<Point3D> vertices;

    // ──────────────────────────────
    // Constructors
    // ──────────────────────────────
    public MarchingTetrahedra(VoxelGrid grid, double isovalue, boolean interpolate) {
        // default assumes SDF-like field (values increase outward)
        this(grid, isovalue, interpolate, false); 
    }

    public MarchingTetrahedra(VoxelGrid grid, double isovalue, boolean interpolate, boolean outwardWithIncreasingValues) {
        this.grid = grid;
        this.isovalue = isovalue;
        this.interpolate = interpolate;
        this.outwardWithIncreasingValues = outwardWithIncreasingValues;
    }

    // ──────────────────────────────
    // Public API
    // ──────────────────────────────
    public TriangleMesh generateMesh() {
        int nx = grid.getDimX();
        int ny = grid.getDimY();
        int nz = grid.getDimZ();

        // 12 cube edges per cell
        edgeVertexCache = new int[nx][ny][nz][12];
        for (int x = 0; x < nx; x++)
            for (int y = 0; y < ny; y++)
                for (int z = 0; z < nz; z++)
                    Arrays.fill(edgeVertexCache[x][y][z], -1);

        vertices = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        for (int x = 0; x < nx - 1; x++)
            for (int y = 0; y < ny - 1; y++)
                for (int z = 0; z < nz - 1; z++)
                    processCube(x, y, z, faces);

        // Build JavaFX mesh
        TriangleMesh mesh = new TriangleMesh();

        float[] pointsArray = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            Point3D p = vertices.get(i);
            pointsArray[3 * i]     = (float) p.getX();
            pointsArray[3 * i + 1] = (float) p.getY();
            pointsArray[3 * i + 2] = (float) p.getZ();
        }
        mesh.getPoints().setAll(pointsArray);

        // JavaFX requires at least one texcoord
        mesh.getTexCoords().setAll(0, 0);

        int[] facesArray = faces.stream().mapToInt(Integer::intValue).toArray();
        mesh.getFaces().setAll(facesArray);

        return mesh;
    }

    // ──────────────────────────────
    // Core extraction by cube
    // ──────────────────────────────
private void processCube(int x, int y, int z, List<Integer> faces) {
    // 1) Gather cube corner positions and scalar values
    Point3D[] cubeCorners = new Point3D[8];
    float[]   cubeVals    = new float[8];
    for (int i = 0; i < 8; i++) {
        int dx = CUBE_VERTS[i][0], dy = CUBE_VERTS[i][1], dz = CUBE_VERTS[i][2];
        cubeCorners[i] = grid.getWorldPosition(x + dx, y + dy, z + dz);
        cubeVals[i]    = grid.get(x + dx, y + dy, z + dz);
    }

    // 2) Local map: unordered corner pair -> cube-edge index (only the 12 cube edges)
    Map<Long, Integer> vertPairToCubeEdge = new HashMap<>(24);
    for (int e = 0; e < 12; e++) {
        int a = CUBE_EDGE_VERTS[e][0], b = CUBE_EDGE_VERTS[e][1];
        vertPairToCubeEdge.put(makeKey(a, b), e);
        vertPairToCubeEdge.put(makeKey(b, a), e);
    }

    // 3) Per-cube caches for diagonals (avoid dup vertices within this cube)
    int[][] faceDiagCache = new int[6][2]; // 6 faces × 2 diagonals
    for (int f = 0; f < 6; f++) { faceDiagCache[f][0] = -1; faceDiagCache[f][1] = -1; }
    int[] bodyDiagCache = new int[4];      // 4 body diagonals
    Arrays.fill(bodyDiagCache, -1);

    // 4) Process 6 tetrahedra
    for (int tetIdx = 0; tetIdx < TETS.length; tetIdx++) {
        int[] tet = TETS[tetIdx];

        // 4a) Case mask for tet corners (inside if < isovalue)
        int caseIdx = 0;
        for (int i = 0; i < 4; i++)
            if (cubeVals[tet[i]] < isovalue) caseIdx |= (1 << i);

        // 4b) Emit triangles for this case
        int[][] triDefs = TRI_TABLE[caseIdx];
        for (int[] tri : triDefs) {
            int[] vIdx = new int[3];

            // Build triangle vertices by intersecting tet edges with the iso surface
            for (int j = 0; j < 3; j++) {
                int localA = TET_EDGES[tri[j]][0];
                int localB = TET_EDGES[tri[j]][1];
                int cubeA  = tet[localA];
                int cubeB  = tet[localB];

                // Try cube edge first (globally cached for watertight stitching)
                Integer cubeEdgeIdx = vertPairToCubeEdge.get(makeKey(cubeA, cubeB));
                if (cubeEdgeIdx != null) {
                    // Edge ownership: decrement only along the axis that edge lies on
                    int ex = x, ey = y, ez = z;
                    int axis = EDGE_AXIS[cubeEdgeIdx];
                    int[] va = CUBE_VERTS[cubeA];
                    int[] vb = CUBE_VERTS[cubeB];
                    if (axis == 0 && vb[0] < va[0]) ex--;
                    if (axis == 1 && vb[1] < va[1]) ey--;
                    if (axis == 2 && vb[2] < va[2]) ez--;

                    // Bounds guard (outermost grid only)
                    if (ex < 0 || ey < 0 || ez < 0 ||
                        ex >= edgeVertexCache.length ||
                        ey >= edgeVertexCache[0].length ||
                        ez >= edgeVertexCache[0][0].length) {
                        vIdx[j] = -1; // mark missing; triangle skipped below
                        continue;
                    }

                    int idx = edgeVertexCache[ex][ey][ez][cubeEdgeIdx];
                    if (idx == -1) {
                        Point3D ip = interpolate(cubeCorners[cubeA], cubeCorners[cubeB],
                                                 cubeVals[cubeA],     cubeVals[cubeB]);
                        idx = vertices.size();
                        vertices.add(ip);
                        edgeVertexCache[ex][ey][ez][cubeEdgeIdx] = idx;
                    }
                    vIdx[j] = idx;
                    continue;
                }

                // Not a cube edge → try body diagonal
                int bodyId = bodyDiagonalId(cubeA, cubeB);
                if (bodyId >= 0) {
                    int idx = bodyDiagCache[bodyId];
                    if (idx == -1) {
                        Point3D ip = interpolate(cubeCorners[cubeA], cubeCorners[cubeB],
                                                 cubeVals[cubeA],     cubeVals[cubeB]);
                        idx = vertices.size();
                        vertices.add(ip);
                        bodyDiagCache[bodyId] = idx;
                    }
                    vIdx[j] = idx;
                    continue;
                }

                // Otherwise it must be a face diagonal
                FaceDiag fd = faceDiagonalKey(cubeA, cubeB);
                if (fd == null) {
                    throw new IllegalStateException("Edge is neither cube edge, face diag, nor body diag: " + cubeA + "-" + cubeB);
                }
                int f = fd.face(), d = fd.diag();
                int idx = faceDiagCache[f][d];
                if (idx == -1) {
                    Point3D ip = interpolate(cubeCorners[cubeA], cubeCorners[cubeB],
                                             cubeVals[cubeA],     cubeVals[cubeB]);
                    idx = vertices.size();
                    vertices.add(ip);
                    faceDiagCache[f][d] = idx;
                }
                vIdx[j] = idx;
            }

            // Skip triangle if any vertex was skipped (only at extreme outer boundary)
            if (vIdx[0] < 0 || vIdx[1] < 0 || vIdx[2] < 0) continue;

            // ── Safer gradient-based winding correction ──
            Point3D a = vertices.get(vIdx[0]);
            Point3D b = vertices.get(vIdx[1]);
            Point3D c = vertices.get(vIdx[2]);

            Point3D n = b.subtract(a).crossProduct(c.subtract(a)); // geometric normal
            Point3D center = new Point3D(
                (a.getX() + b.getX() + c.getX()) / 3.0,
                (a.getY() + b.getY() + c.getY()) / 3.0,
                (a.getZ() + b.getZ() + c.getZ()) / 3.0
            );

            Point3D g = gradientAt(center);
            double gLen = g.magnitude();
            if (gLen > 1e-6) { // only trust the flip if the gradient is meaningful
                double dot = n.dotProduct(g);
                boolean shouldFlip = outwardWithIncreasingValues ? (dot < 0.0) : (dot > 0.0);
                if (shouldFlip) {
                    int tmp = vIdx[1];
                    vIdx[1] = vIdx[2];
                    vIdx[2] = tmp;
                }
            } // else: leave as-is

            // JavaFX uses CCW as front; write (after possible flip)
            faces.add(vIdx[0]); faces.add(0);
            faces.add(vIdx[2]); faces.add(0);
            faces.add(vIdx[1]); faces.add(0);
        }
    }
}

    // ──────────────────────────────
    // Gradient sampling (central differences, normalized)
    // ──────────────────────────────
    private Point3D gradientAt(Point3D p) {
        // world → grid coordinates
        double inv = 1.0 / grid.getVoxelSize();
        Point3D o = grid.getOrigin();
        double gx = (p.getX() - o.getX()) * inv;
        double gy = (p.getY() - o.getY()) * inv;
        double gz = (p.getZ() - o.getZ()) * inv;

        int x = (int)Math.round(gx);
        int y = (int)Math.round(gy);
        int z = (int)Math.round(gz);

        // clamp to interior to allow ±1 sampling
        x = Math.max(1, Math.min(grid.getDimX()-2, x));
        y = Math.max(1, Math.min(grid.getDimY()-2, y));
        z = Math.max(1, Math.min(grid.getDimZ()-2, z));

        float dx = (grid.get(x+1, y,   z  ) - grid.get(x-1, y,   z  )) * 0.5f;
        float dy = (grid.get(x,   y+1, z  ) - grid.get(x,   y-1, z  )) * 0.5f;
        float dz = (grid.get(x,   y,   z+1) - grid.get(x,   y,   z-1)) * 0.5f;

        Point3D g = new Point3D(dx, dy, dz);
        double m = g.magnitude();
        return (m > 1e-12) ? g.multiply(1.0 / m) : g; // normalized (direction only)
    }

    // ──────────────────────────────
    // Helpers: classify diagonal types inside a cube
    // ──────────────────────────────
    /** body diagonal id (0..3) or -1 if not body diagonal */
    private static int bodyDiagonalId(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);
        if (lo == 0 && hi == 6) return 0; // 0-6
        if (lo == 1 && hi == 7) return 1; // 1-7
        if (lo == 2 && hi == 4) return 2; // 2-4
        if (lo == 3 && hi == 5) return 3; // 3-5
        return -1;
    }

    /** face-diagonal descriptor */
    private static record FaceDiag(int face, int diag) {} // face ∈ [0..5], diag ∈ [0..1]

    /**
     * Map unordered (a,b) cube-corner pair to a face diagonal:
     * Faces (0..5): z0, z1, y0, y1, x0, x1
     * Each face has two diagonals (diag 0/1).
     */
    private static FaceDiag faceDiagonalKey(int a, int b) {
        int lo = Math.min(a, b), hi = Math.max(a, b);

        // z=0 face {0,1,2,3}: 0-2 (d0), 1-3 (d1)
        if (lo == 0 && hi == 2) return new FaceDiag(0, 0);
        if (lo == 1 && hi == 3) return new FaceDiag(0, 1);

        // z=1 face {4,5,6,7}: 4-6 (d0), 5-7 (d1)
        if (lo == 4 && hi == 6) return new FaceDiag(1, 0);
        if (lo == 5 && hi == 7) return new FaceDiag(1, 1);

        // y=0 face {0,1,5,4}: 0-5 (d0), 1-4 (d1)
        if (lo == 0 && hi == 5) return new FaceDiag(2, 0);
        if (lo == 1 && hi == 4) return new FaceDiag(2, 1);

        // y=1 face {3,2,6,7}: 3-6 (d0), 2-7 (d1)
        if (lo == 3 && hi == 6) return new FaceDiag(3, 0);
        if (lo == 2 && hi == 7) return new FaceDiag(3, 1);

        // x=0 face {0,3,7,4}: 0-7 (d0), 3-4 (d1)
        if (lo == 0 && hi == 7) return new FaceDiag(4, 0);
        if (lo == 3 && hi == 4) return new FaceDiag(4, 1);

        // x=1 face {1,2,6,5}: 1-6 (d0), 2-5 (d1)
        if (lo == 1 && hi == 6) return new FaceDiag(5, 0);
        if (lo == 2 && hi == 5) return new FaceDiag(5, 1);

        return null;
    }

    // unordered pair key
    private static long makeKey(int a, int b) {
        return (((long) Math.min(a, b)) << 32) | Math.max(a, b);
    }

    // linear interpolation on an edge
    private Point3D interpolate(Point3D p1, Point3D p2, float v1, float v2) {
        if (!interpolate || Math.abs(isovalue - v1) < 1e-5) return p1;
        if (Math.abs(isovalue - v2) < 1e-5) return p2;
        if (Math.abs(v1 - v2) < 1e-5) return p1;

        double mu = (isovalue - v1) / (v2 - v1);
        return new Point3D(
            p1.getX() + mu * (p2.getX() - p1.getX()),
            p1.getY() + mu * (p2.getY() - p1.getY()),
            p1.getZ() + mu * (p2.getZ() - p1.getZ())
        );
    }
}