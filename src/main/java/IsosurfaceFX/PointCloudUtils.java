package IsosurfaceFX;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.geometry.Point3D;

/**
 *
 * @author Sean Phillips
 */
public class PointCloudUtils {
public static Point3D computeBoundingBoxMin(List<Point3D> points) {
    double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
    for (Point3D p : points) {
        minX = Math.min(minX, p.getX());
        minY = Math.min(minY, p.getY());
        minZ = Math.min(minZ, p.getZ());
    }
    return new Point3D(minX, minY, minZ);
}

public static Point3D computeBoundingBoxMax(List<Point3D> points) {
    double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
    for (Point3D p : points) {
        maxX = Math.max(maxX, p.getX());
        maxY = Math.max(maxY, p.getY());
        maxZ = Math.max(maxZ, p.getZ());
    }
    return new Point3D(maxX, maxY, maxZ);
}    
    public static Map<Point3D, Point3D> estimateNormals(List<Point3D> pointCloud, int k) {
        Map<Point3D, Point3D> normalMap = new HashMap<>();

        for (Point3D p : pointCloud) {
            List<Point3D> neighbors = findKNearestNeighbors(p, pointCloud, k);
            Point3D normal = estimateNormal(p, neighbors);
            normalMap.put(p, normal.normalize());
        }

        return normalMap;
    }
    public static List<Point3D> findKNearestNeighbors(Point3D target, List<Point3D> cloud, int k) {
        return cloud.stream()
                .filter(p -> !p.equals(target)) // exclude the target point itself
                .sorted(Comparator.comparingDouble(p -> p.distance(target)))
                .limit(k)
                .collect(Collectors.toList());
    }

    public static Point3D estimateNormal(Point3D point, List<Point3D> neighbors) {
        // Compute centroid
        double cx = 0, cy = 0, cz = 0;
        for (Point3D p : neighbors) {
            cx += p.getX();
            cy += p.getY();
            cz += p.getZ();
        }
        int n = neighbors.size();
        cx /= n;
        cy /= n;
        cz /= n;

        // Build covariance matrix
        double[][] cov = new double[3][3];
        for (Point3D p : neighbors) {
            double dx = p.getX() - cx;
            double dy = p.getY() - cy;
            double dz = p.getZ() - cz;

            cov[0][0] += dx * dx;
            cov[0][1] += dx * dy;
            cov[0][2] += dx * dz;

            cov[1][0] += dy * dx;
            cov[1][1] += dy * dy;
            cov[1][2] += dy * dz;

            cov[2][0] += dz * dx;
            cov[2][1] += dz * dy;
            cov[2][2] += dz * dz;
        }

        FixedMatrix matrix = new FixedMatrix(cov);
        Point3D normal = matrix.smallestEigenVectorPowerIter(50, 1e-6);
        return normal.normalize();
    }

}
