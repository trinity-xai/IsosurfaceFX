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
    private final double[] scalarField;

    public VoxelGrid(double voxelSize, int dimX, int dimY, int dimZ, Point3D origin, double[] scalarField) {
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
    private Integer resolution;
    private Double margin;
    private Point3D origin;
    private Double sizeX, sizeY, sizeZ;

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

    public Builder margin(double margin) {
        this.margin = margin;
        return this;
    }

    public Builder origin(Point3D origin) {
        this.origin = origin;
        return this;
    }

    public Builder size(double sizeX, double sizeY, double sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        return this;
    }

    public VoxelGrid build() {
        if (pointCloud == null || pointCloud.isEmpty()) {
            throw new IllegalArgumentException("Point cloud must not be empty");
        }

        // Compute bounding box if origin or sizes are missing
        Point3D min = PointCloudUtils.computeBoundingBoxMin(pointCloud);
        Point3D max = PointCloudUtils.computeBoundingBoxMax(pointCloud);

        double usedMargin = (margin != null) ? margin : 0.0;

        Point3D usedOrigin = origin;
        if (usedOrigin == null) {
            usedOrigin = min.subtract(usedMargin, usedMargin, usedMargin);
        }

        double usedSizeX = sizeX;
        double usedSizeY = sizeY;
        double usedSizeZ = sizeZ;

        usedSizeX = (max.getX() - min.getX()) + 2 * usedMargin;
        usedSizeY = (max.getY() - min.getY()) + 2 * usedMargin;
        usedSizeZ = (max.getZ() - min.getZ()) + 2 * usedMargin;

        double maxSize = Math.max(usedSizeX, Math.max(usedSizeY, usedSizeZ));
        double actualVoxelSize = voxelSize != null
            ? voxelSize
            : (resolution != null ? maxSize / resolution : 0.05);

        int dimX = (int) Math.ceil(usedSizeX / actualVoxelSize) + 1;
        int dimY = (int) Math.ceil(usedSizeY / actualVoxelSize) + 1;
        int dimZ = (int) Math.ceil(usedSizeZ / actualVoxelSize) + 1;

        int totalVoxels = dimX * dimY * dimZ;
        double[] field = new double[totalVoxels]; // zeros by default

        return new VoxelGrid(actualVoxelSize, dimX, dimY, dimZ, usedOrigin, field);
    }
}

    // Internal 3D to 1D index conversion
    private int index(int x, int y, int z) {
        return x + dimX * (y + dimY * z);
    }

    // Get scalar value at voxel (x, y, z)
    public float get(int x, int y, int z) {
        return (float) scalarField[index(x, y, z)];
    }

    // Set scalar value at voxel (x, y, z)
    public void set(int x, int y, int z, float value) {
        scalarField[index(x, y, z)] = value;
    }

    // Convert voxel coordinates to world-space coordinates
    public Point3D getWorldPosition(int x, int y, int z) {
        double wx = origin.getX() + x * voxelSize;
        double wy = origin.getY() + y * voxelSize;
        double wz = origin.getZ() + z * voxelSize;
        return new Point3D(wx, wy, wz);
    }

    // Accessors
    public double getVoxelSize() { return voxelSize; }
    public int getDimX() { return dimX; }
    public int getDimY() { return dimY; }
    public int getDimZ() { return dimZ; }
    public Point3D getOrigin() { return origin; }
    public double[] getScalarField() { return scalarField; }
}