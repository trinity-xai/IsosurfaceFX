package IsosurfaceFX;

import java.util.List;

/**
 *
 * @author Sean Phillips
 */
import javafx.geometry.Point3D;
import java.util.*;
import java.util.stream.IntStream;

public class PointCloudToField {

   public enum FieldMode {
        GAUSSIAN,
        SDF,          // point-normal projection SDF
        VOXEL_SDF,    // voxel-centric SDF
        UDF_TSDF      // unsigned distance + outside flood-fill sign
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
            case VOXEL_SDF-> computeSignedDistanceFieldVoxelCentric(grid); // your working version
            case UDF_TSDF -> computeUdfTsdf(grid);                         // <-- NEW
        }
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

    // Simple 3D integer hash (no collisions needed across neighboring cells)
    private static long hash(int ix, int iy, int iz) {
        // Mix and pack into long; wide shifts avoid overlap
        long x = ((long)ix & 0x1FFFFFL);      // 21 bits
        long y = ((long)iy & 0x1FFFFFL) << 21;
        long z = ((long)iz & 0x1FFFFFL) << 42;
        return x ^ y ^ z;
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
