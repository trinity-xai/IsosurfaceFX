package IsosurfaceFX;

import java.util.List;
import javafx.geometry.Point3D;

/**
 *
 * @author Sean Phillips
 */
public class VoxelGrid {
    private final double voxelSize;
    private final int dimX, dimY, dimZ;
    private final Point3D origin;
    private final double[][][] scalarField;

    public VoxelGrid(double voxelSize, int dimX, int dimY, int dimZ, Point3D origin, double[][][] scalarField) {
        this.voxelSize = voxelSize;
        this.dimX = dimX;
        this.dimY = dimY;
        this.dimZ = dimZ;
        this.origin = origin;
        this.scalarField = scalarField;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Point3D> pointCloud;
        private Double voxelSize;
        private Integer resolution; // for longest axis

        public Builder pointCloud(List<Point3D> pointCloud) {
            this.pointCloud = pointCloud;
            return this;
        }

        public Builder voxelSize(double voxelSize) {
            this.voxelSize = voxelSize;
            return this;
        }

        public Builder resolution(int resolution) {
            this.resolution = resolution;
            return this;
        }

        public VoxelGrid build() {
            if (pointCloud == null || pointCloud.isEmpty()) {
                throw new IllegalArgumentException("Point cloud must not be empty");
            }

            // Compute bounding box
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

            for (Point3D p : pointCloud) {
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY());
                maxY = Math.max(maxY, p.getY());
                minZ = Math.min(minZ, p.getZ());
                maxZ = Math.max(maxZ, p.getZ());
            }

            double sizeX = maxX - minX;
            double sizeY = maxY - minY;
            double sizeZ = maxZ - minZ;
            double maxSize = Math.max(sizeX, Math.max(sizeY, sizeZ));

            double actualVoxelSize = this.voxelSize != null
                ? this.voxelSize
                : (this.resolution != null ? maxSize / this.resolution : 0.05);

            int dimX = (int) Math.ceil(sizeX / actualVoxelSize) + 1;
            int dimY = (int) Math.ceil(sizeY / actualVoxelSize) + 1;
            int dimZ = (int) Math.ceil(sizeZ / actualVoxelSize) + 1;

            Point3D origin = new Point3D(minX, minY, minZ);
            double[][][] field = new double[dimX][dimY][dimZ];

            // Field is empty for now; to be filled later via PointCloudToField
            return new VoxelGrid(actualVoxelSize, dimX, dimY, dimZ, origin, field);
        }
    }
    // Internal 3D to 1D index conversion
    private int index(int x, int y, int z) {
        return x + dimX * (y + dimY * z);
    }    
    // Get scalar value at voxel (x, y, z)
    public float get(int x, int y, int z) {
        return (float) scalarField[x][y][z];
    }

    // Set scalar value at voxel (x, y, z)
    public void set(int x, int y, int z, float value) {
        scalarField[x][y][z] = value;
    }
    // Convert voxel coordinates to world-space coordinates
    public Point3D getWorldPosition(int x, int y, int z) {
        double wx = origin.getX() + x * voxelSize;
        double wy = origin.getY() + y * voxelSize;
        double wz = origin.getZ() + z * voxelSize;
        return new Point3D(wx, wy, wz);
    }
    public double getVoxelSize() { return voxelSize; }
    public int getDimX() { return dimX; }
    public int getDimY() { return dimY; }
    public int getDimZ() { return dimZ; }
    public Point3D getOrigin() { return origin; }
    public double[][][] getScalarField() { return scalarField; }
}