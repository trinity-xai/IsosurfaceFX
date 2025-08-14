package IsosurfaceFX;

import java.util.List;

/**
 *
 * @author Sean Phillips
 */
import javafx.geometry.Point3D;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class PointCloudToField {

   public enum FieldMode {
        GAUSSIAN,
        SDF,          // point-normal projection SDF
        VOXEL_SDF,    // voxel-centric SDF
        UDF_TSDF,      // unsigned distance + outside flood-fill sign
        MLS_TSDF
    }

    private final List<Point3D> pointCloud;
    private final FieldMode mode;
    private final double influenceRadius;
    private final Map<Point3D, Point3D> normalMap;

    public PointCloudToField(List<Point3D> pointCloud,
                             FieldMode mode,
                             double influenceRadius,
                             Map<Point3D, Point3D> normalMap) {
        this.pointCloud = pointCloud;
        this.mode = mode;
        this.influenceRadius = influenceRadius;
        this.normalMap = normalMap;
    }

    public void applyTo(VoxelGrid grid) {
        switch (mode) {
            case GAUSSIAN -> computeGaussian(grid);
            case SDF      -> computeSignedDistanceField(grid);
            case VOXEL_SDF-> computeSignedDistanceFieldVoxelCentric(grid); 
            case UDF_TSDF -> computeUdfTsdf(grid);
//            case UDF_TSDF -> computeUnionOfBallsTsdf(grid);
            case MLS_TSDF -> computeMLS_TSDFVoxelCentric(grid);
        }
    }
private void computeMLS_TSDFVoxelCentric(VoxelGrid grid) {
    final int nx = grid.getDimX(), ny = grid.getDimY(), nz = grid.getDimZ();
    final double h  = grid.getVoxelSize();
    final Point3D o = grid.getOrigin();

    // Kernel size: tie sigma to your influenceRadius
    final double sigma = Math.max(1e-6, influenceRadius * 0.5);
    final double twoSigma2 = 2.0 * sigma * sigma;
    // Truncation (helps robustness and marching stability)
    final double tau = Math.max(h * 2.5, influenceRadius);

    // Precompute a simple uniform grid hash for kNN-ish neighborhood (fast + dep-free)
    final double cell = Math.max(h, sigma);
    Map<Long, List<Point3D>> buckets = new HashMap<>();
    Map<Point3D, Point3D> normals = this.normalMap; // already smoothed upstream

    for (Point3D p : pointCloud) {
        long key = hashCell(p, cell);
        buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
    }

    // Neighborhood query helper
    final int R = 2; // search +/- 2 cells in each axis (~ enough for gaussian falloff)
    BiFunction<Point3D, List<Point3D>, Point3D> normalOf = (p, list) -> normals.getOrDefault(p, Point3D.ZERO);

    for (int x = 0; x < nx; x++) {
        final double X = o.getX() + x * h;
        for (int y = 0; y < ny; y++) {
            final double Y = o.getY() + y * h;
            for (int z = 0; z < nz; z++) {
                final double Z = o.getZ() + z * h;
                Point3D vc = new Point3D(X, Y, Z);

                // gather candidates from neighboring hash buckets
                int cx = (int)Math.floor(X / cell);
                int cy = (int)Math.floor(Y / cell);
                int cz = (int)Math.floor(Z / cell);

                double num = 0.0, den = 0.0;
                for (int dx = -R; dx <= R; dx++) {
                    for (int dy = -R; dy <= R; dy++) {
                        for (int dz = -R; dz <= R; dz++) {
                            long k = pack(cx + dx, cy + dy, cz + dz);
                            List<Point3D> lst = buckets.get(k);
                            if (lst == null) continue;

                            for (Point3D p : lst) {
                                Point3D n = normalOf.apply(p, lst);
                                if (n == Point3D.ZERO) continue;

                                double rx = X - p.getX(), ry = Y - p.getY(), rz = Z - p.getZ();
                                double r2 = rx*rx + ry*ry + rz*rz;
                                // fast reject far samples
                                if (r2 > (influenceRadius * influenceRadius)) continue;

                                double w = Math.exp(-r2 / twoSigma2);
                                // signed distance along the oriented plane at p
                                double di = rx * n.getX() + ry * n.getY() + rz * n.getZ();

                                num += w * di;
                                den += w;
                            }
                        }
                    }
                }

                float sdf;
                if (den < 1e-12) {
                    // No support → mark as 'far outside' (positive by convention)
                    sdf = Float.POSITIVE_INFINITY;
                } else {
                    double d = num / den;
                    // truncation for a TSDF flavor (stabilizes marching)
                    if (d >  tau) d =  tau;
                    if (d < -tau) d = -tau;
                    sdf = (float) d;
                }
                grid.set(x, y, z, sdf);
            }
        }
    }
}

// simple 3D hash (no deps)
private static long hashCell(Point3D p, double cell) {
    int ix = (int)Math.floor(p.getX() / cell);
    int iy = (int)Math.floor(p.getY() / cell);
    int iz = (int)Math.floor(p.getZ() / cell);
    return pack(ix, iy, iz);
}
private static long pack(int x, int y, int z) {
    // pack 3 signed ints into a long deterministically
    return  (((long)x) & 0x1FFFFFL) << 42
          | (((long)y) & 0x1FFFFFL) << 21
          | (((long)z) & 0x1FFFFFL);
}
    // ------------------------------------------------------------
    // UDF -> TSDF (normals-free)  
    // ------------------------------------------------------------
    private void computeUdfTsdf(VoxelGrid grid) {
        // 1) Build unsigned distance field near the data
        double searchRadius = Math.max(influenceRadius, grid.getVoxelSize() * 2.0);
        computeUnsignedDistanceFieldVoxelCentric(grid, searchRadius);

        // 2) Sign it via an outside flood-fill:
        //    Treat “far” voxels on the boundary as outside seeds.
        double outsideThreshold = Math.max(grid.getVoxelSize() * 1.5, 0.5 * searchRadius);
        signUnsignedDistanceWithFloodFill(grid, outsideThreshold);
        // Result: outside = +distance, inside = -distance (increases outward)
    }

    // Build unsigned distance field using a simple spatial hash (no libs)
    private void computeUnsignedDistanceFieldVoxelCentric(VoxelGrid grid, double searchRadius) {
        final int nx = grid.getDimX(), ny = grid.getDimY(), nz = grid.getDimZ();
        final double h = grid.getVoxelSize();
        final Point3D o = grid.getOrigin();

        // Initialize to a large value
        for (int x=0; x<nx; x++)
            for (int y=0; y<ny; y++)
                for (int z=0; z<nz; z++)
                    grid.set(x,y,z, Float.POSITIVE_INFINITY);

        // Spatial hash cell size: slightly larger than search radius to reduce neighbor rings
        final double cell = Math.max(h, searchRadius);
        final Map<Long, List<Point3D>> buckets = new HashMap<>(pointCloud.size()*2);

        // Bucketize points
        for (Point3D p : pointCloud) {
            int ix = (int)Math.floor((p.getX() - o.getX()) / cell);
            int iy = (int)Math.floor((p.getY() - o.getY()) / cell);
            int iz = (int)Math.floor((p.getZ() - o.getZ()) / cell);
            long key = hash(ix, iy, iz);
            buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        final int r = (int)Math.ceil(searchRadius / cell);

        // Voxel-centric sweep (parallelize outer loop if you like)
        for (int x=0; x<nx; x++) {
            for (int y=0; y<ny; y++) {
                for (int z=0; z<nz; z++) {
                    Point3D vc = new Point3D(o.getX()+x*h, o.getY()+y*h, o.getZ()+z*h);

                    int cx = (int)Math.floor((vc.getX() - o.getX()) / cell);
                    int cy = (int)Math.floor((vc.getY() - o.getY()) / cell);
                    int cz = (int)Math.floor((vc.getZ() - o.getZ()) / cell);

                    double best = Double.POSITIVE_INFINITY;
                    for (int dx=-r; dx<=r; dx++)
                        for (int dy=-r; dy<=r; dy++)
                            for (int dz=-r; dz<=r; dz++) {
                                List<Point3D> bin = buckets.get(hash(cx+dx, cy+dy, cz+dz));
                                if (bin == null) continue;
                                for (Point3D q : bin) {
                                    double d = vc.distance(q);
                                    if (d < best) best = d;
                                }
                            }

                    // If no neighbors found, leave as large; else set the min distance
                    grid.set(x,y,z, (float)best);
                }
            }
        }
    }

    // Flood-fill “outside” and set signs: outside positive, inside negative
    private void signUnsignedDistanceWithFloodFill(VoxelGrid grid, double outsideThreshold) {
        final int nx = grid.getDimX(), ny = grid.getDimY(), nz = grid.getDimZ();
        final boolean[][][] outside = new boolean[nx][ny][nz];
        ArrayDeque<int[]> q = new ArrayDeque<>();

        // Seed at boundary voxels that are "far" from points (>= threshold)
        for (int x=0; x<nx; x++) for (int y=0; y<ny; y++) for (int z=0; z<nz; z++) {
            boolean boundary = (x==0 || y==0 || z==0 || x==nx-1 || y==ny-1 || z==nz-1);
            if (!boundary) continue;
            float d = grid.get(x,y,z);
            if (Float.isFinite(d) && d >= outsideThreshold) {
                outside[x][y][z] = true;
                q.add(new int[]{x,y,z});
            }
        }

        // 6-connected flood fill
        final int[][] N6 = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!q.isEmpty()) {
            int[] a = q.poll();
            for (int[] d : N6) {
                int nxu=a[0]+d[0], nyu=a[1]+d[1], nzu=a[2]+d[2];
                if (nxu<0||nyu<0||nzu<0||nxu>=nx||nyu>=ny||nzu>=nz) continue;
                if (outside[nxu][nyu][nzu]) continue;
                float val = grid.get(nxu,nyu,nzu);
                if (!Float.isFinite(val)) continue;      // skip infinities (unreliable)
                if (val >= outsideThreshold) {
                    outside[nxu][nyu][nzu] = true;
                    q.add(new int[]{nxu,nyu,nzu});
                }
            }
        }

        // Assign sign: + outside, - inside
        for (int x=0; x<nx; x++) for (int y=0; y<ny; y++) for (int z=0; z<nz; z++) {
            float d = grid.get(x,y,z);
            if (!Float.isFinite(d)) continue;
            grid.set(x,y,z, outside[x][y][z] ?  d : -d);
        }
    }
// Strategy: 
// 1) Mark a narrow surface band: unsigned distance < tau
// 2) Flood-fill from ALL boundary voxels through NON-band voxels
// 3) Outside = visited; Inside = not visited
private void signUnsignedDistanceViaBandFlood(VoxelGrid grid, double tau) {
    final int nx = grid.getDimX(), ny = grid.getDimY(), nz = grid.getDimZ();

    // 1) Classify voxels
    final byte[][][] band = new byte[nx][ny][nz];  // 1 = surface band, 0 = free
    for (int x=0;x<nx;x++) for (int y=0;y<ny;y++) for (int z=0;z<nz;z++) {
        float d = grid.get(x,y,z);
        if (!Float.isFinite(d)) continue;
        if (d < tau) band[x][y][z] = 1;
    }

    // 2) Flood from ALL boundary voxels that are NOT in the band
    final boolean[][][] outside = new boolean[nx][ny][nz];
    ArrayDeque<int[]> q = new ArrayDeque<>();

    // seed every boundary cell not in surface band
    for (int x=0;x<nx;x++) for (int y=0;y<ny;y++) {
        int z0=0, z1=nz-1;
        if (band[x][y][z0]==0) { outside[x][y][z0]=true; q.add(new int[]{x,y,z0}); }
        if (band[x][y][z1]==0) { outside[x][y][z1]=true; q.add(new int[]{x,y,z1}); }
    }
    for (int x=0;x<nx;x++) for (int z=0;z<nz;z++) {
        int y0=0, y1=ny-1;
        if (band[x][y0][z]==0) { outside[x][y0][z]=true; q.add(new int[]{x,y0,z}); }
        if (band[x][y1][z]==0) { outside[x][y1][z]=true; q.add(new int[]{x,y1,z}); }
    }
    for (int y=0;y<ny;y++) for (int z=0;z<nz;z++) {
        int x0=0, x1=nx-1;
        if (band[x0][y][z]==0) { outside[x0][y][z]=true; q.add(new int[]{x0,y,z}); }
        if (band[x1][y][z]==0) { outside[x1][y][z]=true; q.add(new int[]{x1,y,z}); }
    }

    // 6-connected flood through non-band space
    final int[][] N6 = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    while (!q.isEmpty()) {
        int[] a=q.poll();
        for (int[] d:N6) {
            int nxu=a[0]+d[0], nyu=a[1]+d[1], nzu=a[2]+d[2];
            if (nxu<0||nyu<0||nzu<0||nxu>=nx||nyu>=ny||nzu>=nz) continue;
            if (outside[nxu][nyu][nzu]) continue;
            if (band[nxu][nyu][nzu]==1) continue; // cannot cross the surface band
            outside[nxu][nyu][nzu]=true;
            q.add(new int[]{nxu,nyu,nzu});
        }
    }

    // 3) Assign sign using the unsigned value already in the grid
    for (int x=0;x<nx;x++) for (int y=0;y<ny;y++) for (int z=0;z<nz;z++) {
        float u = grid.get(x,y,z);
        if (!Float.isFinite(u)) continue;
        grid.set(x,y,z, outside[x][y][z] ?  +u : -u);
    }
}

    // Simple 3D integer hash (no collisions needed across neighboring cells)
    private static long hash(int ix, int iy, int iz) {
        // Mix and pack into long; wide shifts avoid overlap
        long x = ((long)ix & 0x1FFFFFL);      // 21 bits
        long y = ((long)iy & 0x1FFFFFL) << 21;
        long z = ((long)iz & 0x1FFFFFL) << 42;
        return x ^ y ^ z;
    }
private void computeUnionOfBallsTsdf(VoxelGrid grid) {
    final int nx=grid.getDimX(), ny=grid.getDimY(), nz=grid.getDimZ();
    final double h = grid.getVoxelSize();
    final Point3D o = grid.getOrigin();

    // 1) Spatial hash 
    double cell = Math.max(h, influenceRadius);
    Map<Long, List<Point3D>> buckets = new HashMap<>(pointCloud.size()*2);
    for (Point3D p : pointCloud) {
        int ix = (int)Math.floor((p.getX()-o.getX())/cell);
        int iy = (int)Math.floor((p.getY()-o.getY())/cell);
        int iz = (int)Math.floor((p.getZ()-o.getZ())/cell);
        buckets.computeIfAbsent(hash(ix,iy,iz), k -> new ArrayList<>()).add(p);
    }

    // 2) Per-point local radii (k≈6–8; scale≈0.5 is a good start)
    Map<Point3D, Double> rMap = estimateLocalRadii(pointCloud, buckets, cell, o, /*k*/8, /*scale*/0.5);

    // 3) Narrow search: only within ~ (r_i + band). Use band≈2h to be safe.
    double band = 2.0*h;
    int r = (int)Math.ceil((influenceRadius + band) / cell);

    // 4) Evaluate φ(x) = min_i (‖x − p_i‖ − r_i) over nearby buckets
    for (int x=0; x<nx; x++) for (int y=0; y<ny; y++) for (int z=0; z<nz; z++) {
        Point3D vc = new Point3D(o.getX()+x*h, o.getY()+y*h, o.getZ()+z*h);

        int cx = (int)Math.floor((vc.getX()-o.getX())/cell);
        int cy = (int)Math.floor((vc.getY()-o.getY())/cell);
        int cz = (int)Math.floor((vc.getZ()-o.getZ())/cell);

        double best = Double.POSITIVE_INFINITY;
        for (int dx=-r; dx<=r; dx++)
            for (int dy=-r; dy<=r; dy++)
                for (int dz=-r; dz<=r; dz++) {
                    List<Point3D> bin = buckets.get(hash(cx+dx, cy+dy, cz+dz));
                    if (bin == null) continue;
                    for (Point3D q : bin) {
                        double ri = rMap.getOrDefault(q, influenceRadius*0.5);
                        double val = vc.distance(q) - ri;
                        if (val < best) best = val;
                    }
                }
        grid.set(x,y,z, (float)Math.abs(best)); // unsigned for now
    }

//    // 5) Sign with the outside flood-fill 
//    double spacingGuess = medianRadius(rMap);                 // ~typical r_i
//    double outsideThreshold = Math.max(1.5*h, 1.0*spacingGuess);
//    signUnsignedDistanceWithFloodFill(grid, outsideThreshold);

    // tau ~ 1–2 voxels is plenty. Start with 2*voxelSize.
    double tau = 2.0 * grid.getVoxelSize();
    signUnsignedDistanceViaBandFlood(grid, tau);    
}

private static double medianRadius(Map<Point3D, Double> rMap) {
    if (rMap.isEmpty()) return 1.0;
    ArrayList<Double> rs = new ArrayList<>(rMap.values());
    Collections.sort(rs);
    int n = rs.size();
    return (n%2==1)? rs.get(n/2) : 0.5*(rs.get(n/2-1)+rs.get(n/2));
}    
 private Map<Point3D, Double> estimateLocalRadii(List<Point3D> pts,
                                                Map<Long, List<Point3D>> buckets,
                                                double cellSize,
                                                Point3D origin,
                                                int k, double scale) {
    Map<Point3D, Double> radii = new HashMap<>(pts.size()*2);
    for (Point3D p : pts) {
        // gather neighbors from adjacent buckets
        int ix = (int)Math.floor((p.getX()-origin.getX())/cellSize);
        int iy = (int)Math.floor((p.getY()-origin.getY())/cellSize);
        int iz = (int)Math.floor((p.getZ()-origin.getZ())/cellSize);
        List<Double> dists = new ArrayList<>(k*2);
        for (int dx=-1; dx<=1; dx++)
            for (int dy=-1; dy<=1; dy++)
                for (int dz=-1; dz<=1; dz++) {
                    List<Point3D> bin = buckets.get(hash(ix+dx, iy+dy, iz+dz));
                    if (bin == null) continue;
                    for (Point3D q : bin) {
                        if (q == p) continue;
                        dists.add(p.distance(q));
                    }
                }
        if (dists.isEmpty()) { radii.put(p, scale * influenceRadius); continue; }
        Collections.sort(dists);
        // use median of first k distances (or all if fewer)
        int take = Math.min(k, dists.size());
        double med = (take % 2 == 1)
                ? dists.get(take/2)
                : 0.5*(dists.get(take/2-1) + dists.get(take/2));
        radii.put(p, scale * med); // r_i = scale * local spacing
    }
    return radii;
}   
private void computeSignedDistanceFieldVoxelCentric(VoxelGrid grid) {
    int dimX = grid.getDimX();
    int dimY = grid.getDimY();
    int dimZ = grid.getDimZ();
    double voxelSize = grid.getVoxelSize();
    Point3D origin = grid.getOrigin();

    // Build a spatial index for points (e.g., KD-Tree) for speed!
    // For demonstration, this is O(N) per voxel
    for (int x = 0; x < dimX; x++) {
        for (int y = 0; y < dimY; y++) {
            for (int z = 0; z < dimZ; z++) {
                Point3D voxelCenter = new Point3D(
                        origin.getX() + x * voxelSize,
                        origin.getY() + y * voxelSize,
                        origin.getZ() + z * voxelSize
                );

                double minDist = Double.POSITIVE_INFINITY;
                double sign = 1.0;
                Point3D closestNormal = null;

                for (Point3D point : pointCloud) {
                    double dist = voxelCenter.distance(point);
                    if (dist < minDist) {
                        minDist = dist;
                        closestNormal = normalMap.get(point);
                        sign = (closestNormal != null && voxelCenter.subtract(point).dotProduct(closestNormal) < 0) ? -1.0 : 1.0;
                    }
                }
                grid.set(x, y, z, (float) (minDist * sign));
            }
        }
    }
}
    private void computeGaussian(VoxelGrid grid) {
        int dimX = grid.getDimX();
        int dimY = grid.getDimY();
        int dimZ = grid.getDimZ();
        double voxelSize = grid.getVoxelSize();
        Point3D origin = grid.getOrigin();
        double cutoffRadius = influenceRadius;
        double cutoffRadiusSq = cutoffRadius * cutoffRadius;
        double sigmaSq = influenceRadius * influenceRadius;

        int influenceCells = (int) Math.ceil(cutoffRadius / voxelSize);

        pointCloud.parallelStream().forEach(p -> {
            int cx = (int) ((p.getX() - origin.getX()) / voxelSize);
            int cy = (int) ((p.getY() - origin.getY()) / voxelSize);
            int cz = (int) ((p.getZ() - origin.getZ()) / voxelSize);

            for (int dx = -influenceCells; dx <= influenceCells; dx++) {
                int x = cx + dx;
                if (x < 0 || x >= dimX) {
                    continue;
                }

                for (int dy = -influenceCells; dy <= influenceCells; dy++) {
                    int y = cy + dy;
                    if (y < 0 || y >= dimY) {
                        continue;
                    }

                    for (int dz = -influenceCells; dz <= influenceCells; dz++) {
                        int z = cz + dz;
                        if (z < 0 || z >= dimZ) {
                            continue;
                        }

                        Point3D voxelCenter = new Point3D(
                                origin.getX() + x * voxelSize,
                                origin.getY() + y * voxelSize,
                                origin.getZ() + z * voxelSize
                        );

                        double dxSq = voxelCenter.getX() - p.getX();
                        double dySq = voxelCenter.getY() - p.getY();
                        double dzSq = voxelCenter.getZ() - p.getZ();
                        double distSq = dxSq * dxSq + dySq * dySq + dzSq * dzSq;

                        if (distSq > cutoffRadiusSq) {
                            continue;
                        }

                        float contrib = (float) Math.exp(-distSq / (2.0 * sigmaSq));
                        grid.set(x, y, z, grid.get(x, y, z) + contrib);
                    }
                }
            }
        }
        );

    }
private void computeSignedDistanceField(VoxelGrid grid) {
    int dimX = grid.getDimX();
    int dimY = grid.getDimY();
    int dimZ = grid.getDimZ();
    double voxelSize = grid.getVoxelSize();
    Point3D origin = grid.getOrigin();

    // Step 1: Initialize all voxels to +∞ (unvisited)
    IntStream.range(0, dimX).parallel().forEach(x -> {
        for (int y = 0; y < dimY; y++) {
            for (int z = 0; z < dimZ; z++) {
                grid.set(x, y, z, Float.POSITIVE_INFINITY);
            }
        }
    });

    double maxDist = influenceRadius;

    // Step 2: For each point in the cloud
    pointCloud.parallelStream().forEach(point -> {
        Point3D normal = normalMap.get(point);
        if (normal == null) return;

        int centerX = (int) ((point.getX() - origin.getX()) / voxelSize);
        int centerY = (int) ((point.getY() - origin.getY()) / voxelSize);
        int centerZ = (int) ((point.getZ() - origin.getZ()) / voxelSize);
        int radiusInVoxels = (int) Math.ceil(maxDist / voxelSize);

        // Visit all voxels within the spherical influence zone
        for (int dx = -radiusInVoxels; dx <= radiusInVoxels; dx++) {
            int x = centerX + dx;
            if (x < 0 || x >= dimX) continue;

            for (int dy = -radiusInVoxels; dy <= radiusInVoxels; dy++) {
                int y = centerY + dy;
                if (y < 0 || y >= dimY) continue;

                for (int dz = -radiusInVoxels; dz <= radiusInVoxels; dz++) {
                    int z = centerZ + dz;
                    if (z < 0 || z >= dimZ) continue;

                    Point3D voxelCenter = new Point3D(
                        origin.getX() + x * voxelSize,
                        origin.getY() + y * voxelSize,
                        origin.getZ() + z * voxelSize
                    );

                    Point3D offset = voxelCenter.subtract(point);
                    double distance = offset.magnitude();

                    if (distance > maxDist) continue;

                    // Sign the distance based on the dot product with the point normal
                    double dot = offset.dotProduct(normal);
                    float signedDistance = (float) (distance * (dot >= 0 ? 1.0 : -1.0));

                    synchronized (grid) {
                        float current = grid.get(x, y, z);
                        if (Math.abs(signedDistance) < Math.abs(current)) {
                            grid.set(x, y, z, signedDistance);
                        }
                    }
                }
            }
        }
    });
}


// Utility clamp method
private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
}


    private void debugCenterValues(VoxelGrid grid) {
        System.out.println("Sample SDF values near center:");
        for (int x = grid.getDimX() / 2 - 2; x <= grid.getDimX() / 2 + 2; x++) {
            for (int y = grid.getDimY() / 2 - 2; y <= grid.getDimY() / 2 + 2; y++) {
                for (int z = grid.getDimZ() / 2 - 2; z <= grid.getDimZ() / 2 + 2; z++) {
                    float v = grid.get(x, y, z);
                    if (v != Float.POSITIVE_INFINITY) {
                        System.out.printf("(%d,%d,%d): %.3f\n", x, y, z, v);
                    }
                }
            }
        }

    }

    private void debugMinMax(VoxelGrid grid) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        for (int x = 0; x < grid.getDimX(); x++) {
            for (int y = 0; y < grid.getDimY(); y++) {
                for (int z = 0; z < grid.getDimZ(); z++) {
                    float v = grid.get(x, y, z);
                    if (Float.isFinite(v)) {
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                    }
                }
            }
        }
        System.out.println("Voxel Grid Value Range: [" + min + ", " + max + "]");
    }
}
