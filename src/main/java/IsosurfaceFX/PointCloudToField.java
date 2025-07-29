package IsosurfaceFX;

import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import java.util.List;

/**
 *
 * @author Sean Phillips
 */
public class PointCloudToField {
    private List<Point3D> points;
    private double voxelSize = 2.0;
    private int resolution = -1; // use voxelSize if resolution < 0
    private double gaussianRadius = 4.0;

    public static PointCloudToFieldBuilder builder() {
        return new PointCloudToFieldBuilder();
    }

    public VoxelGrid generate() {
        if (points == null || points.isEmpty()) {
            throw new IllegalStateException("Point cloud must be set and non-empty");
        }

        // Use VoxelGrid.Builder to construct base grid
        VoxelGrid.Builder gridBuilder = VoxelGrid.builder().pointCloud(points);
        if (resolution > 0) {
            gridBuilder.resolution(resolution);
        } else {
            gridBuilder.voxelSize(voxelSize);
        }

        VoxelGrid grid = gridBuilder.build();

        int nx = grid.getDimX();
        int ny = grid.getDimY();
        int nz = grid.getDimZ();
        double vs = grid.getVoxelSize();
        Point3D origin = grid.getOrigin();

        for (Point3D p : points) {
            int rx = (int) Math.ceil(gaussianRadius / vs);

            Point3D offset = p.subtract(origin);
            int cx = (int) (offset.getX() / vs);
            int cy = (int) (offset.getY() / vs);
            int cz = (int) (offset.getZ() / vs);

            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -rx; dy <= rx; dy++) {
                    for (int dz = -rx; dz <= rx; dz++) {
                        int x = cx + dx;
                        int y = cy + dy;
                        int z = cz + dz;

                        if (x >= 0 && x < nx && y >= 0 && y < ny && z >= 0 && z < nz) {
                            Point3D voxelPos = grid.getWorldPosition(x, y, z);
                            double distSq = p.distance(voxelPos);
                            double weight = Math.exp(-(distSq * distSq) / (gaussianRadius * gaussianRadius));
                            float existing = grid.get(x, y, z);
                            grid.set(x, y, z, (float) (existing + weight));
                        }
                    }
                }
            }
        }

        return grid;
    }

    private Bounds computeBounds(List<Point3D> pts) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (Point3D p : pts) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }

        return new BoundingBox(minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ);
    }

    public static class PointCloudToFieldBuilder {
        private final PointCloudToField field = new PointCloudToField();

        public PointCloudToFieldBuilder withPoints(List<Point3D> pts) {
            field.points = pts;
            return this;
        }

        public PointCloudToFieldBuilder withVoxelSize(double vs) {
            field.voxelSize = vs;
            return this;
        }

        public PointCloudToFieldBuilder withResolution(int res) {
            field.resolution = res;
            return this;
        }

        public PointCloudToFieldBuilder withGaussianRadius(double radius) {
            field.gaussianRadius = radius;
            return this;
        }

        public VoxelGrid build() {
            return field.generate();
        }
    }
}