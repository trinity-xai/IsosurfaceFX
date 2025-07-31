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
        SDF
    }
    private final List<Point3D> pointCloud;
    private final FieldMode mode;
    private final double influenceRadius;
    private final Map<Point3D, Point3D> normalMap;

    public PointCloudToField(List<Point3D> pointCloud, FieldMode mode, double influenceRadius, Map<Point3D, Point3D> normalMap) {
        this.pointCloud = pointCloud;
        this.mode = mode;
        this.influenceRadius = influenceRadius;
        this.normalMap = normalMap;
    }

    public void applyTo(VoxelGrid grid) {
        switch (mode) {
            case GAUSSIAN -> {
                computeGaussian(grid);
            }

            case SDF -> {
                computeSignedDistanceField(grid);
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

        // Pre-initialize grid with high values
        IntStream.range(0, dimX).parallel().forEach(x -> {
            for (int y = 0; y < dimY; y++) {
                for (int z = 0; z < dimZ; z++) {
                    grid.set(x, y, z, Float.POSITIVE_INFINITY);
                }
            }
        });

        for (Point3D p : pointCloud) {
            Point3D normal = normalMap.get(p);
            if (normal == null) {
                continue;
            }

            int cx = (int) ((p.getX() - origin.getX()) / voxelSize);
            int cy = (int) ((p.getY() - origin.getY()) / voxelSize);
            int cz = (int) ((p.getZ() - origin.getZ()) / voxelSize);

            int influenceCells = (int) Math.ceil(influenceRadius / voxelSize);

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

                        Point3D offset = voxelCenter.subtract(p);
                        double distance = offset.magnitude();

                        // ✅ Only update voxels within the influence radius
                        if (distance > influenceRadius) {
                            continue;
                        }

                        float currentValue = grid.get(x, y, z);
                        if (distance < currentValue) {
                            double dot = (distance == 0.0) ? 1.0 : offset.normalize().dotProduct(normal);
                            float signedDistance = (float) (distance * (dot >= 0 ? 1.0 : -1.0));
                            grid.set(x, y, z, signedDistance);
                        }
                    }
                }
            }
        }
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
