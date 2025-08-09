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
    private enum Axis { X, Y, Z }
    public static void accumulateNormalsIntoOctree(
            OctreeNode root,
            List<Point3D> points,
            Map<Point3D, Point3D> normalMap,
            double influenceRadius) {

        for (Point3D point : points) {
            Point3D normal = normalMap.get(point);
            if (normal == null) {
                continue;
            }

            // Find the node that contains this point
            OctreeNode containingNode = root.findLeafContaining(point);
            if (containingNode != null) {
                // Use Gaussian or constant weight
                double weight = 1.0;
                containingNode.accumulateNormal(normal, weight);
            }
        }
    }
private static Point3D findAccumulatedNormalAt(OctreeNode node, Point3D point) {
    if (!node.contains(point)) return Point3D.ZERO;

    if (node.isLeaf()) {
        return node.getAccumulatedNormal();
    }

    for (OctreeNode child : node.getChildren()) {
        if (child != null && child.contains(point)) {
            return findAccumulatedNormalAt(child, point);
        }
    }

    return Point3D.ZERO;
}    
private static Point3D offset(Point3D p, double delta, Axis axis) {
    switch (axis) {
        case X: return new Point3D(p.getX() + delta, p.getY(), p.getZ());
        case Y: return new Point3D(p.getX(), p.getY() + delta, p.getZ());
        case Z: return new Point3D(p.getX(), p.getY(), p.getZ() + delta);
        default: throw new IllegalArgumentException();
    }
}

private static double getComponent(Point3D p, Axis axis) {
    switch (axis) {
        case X: return p.getX();
        case Y: return p.getY();
        case Z: return p.getZ();
        default: throw new IllegalArgumentException();
    }
}
    
private static double differenceAlongAxis(OctreeNode node, Point3D center, double h, Axis axis) {
    Point3D plus = offset(center, h / 2, axis);
    Point3D minus = offset(center, -h / 2, axis);

    Point3D vPlus = findAccumulatedNormalAt(node, plus);
    Point3D vMinus = findAccumulatedNormalAt(node, minus);

    double componentPlus = getComponent(vPlus, axis);
    double componentMinus = getComponent(vMinus, axis);

    return (componentPlus - componentMinus) / h;
}    
public static void computeDivergenceField(OctreeNode node) {
    if (node.isLeaf()) {
        // Finite difference approximation of divergence
        double h = node.getSize(); // voxel width
        Point3D center = node.getCenter();

        // Partial derivatives: approximate with neighboring leaves
        double divX = differenceAlongAxis(node, center, h, Axis.X);
        double divY = differenceAlongAxis(node, center, h, Axis.Y);
        double divZ = differenceAlongAxis(node, center, h, Axis.Z);

        node.setDivergence(divX + divY + divZ);
    } else {
        for (OctreeNode child : node.getChildren()) {
            if (child != null) {
                computeDivergenceField(child);
            }
        }
    }
}

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

    public static Point3D computeCentroid(List<Point3D> points) {
        double sumX = 0, sumY = 0, sumZ = 0;
        for (Point3D p : points) {
            sumX += p.getX();
            sumY += p.getY();
            sumZ += p.getZ();
        }
        int n = points.size();
        return new Point3D(sumX / n, sumY / n, sumZ / n);
    }
   
public static Map<Point3D, Point3D> smoothNormals(Map<Point3D, Point3D> normalMap, List<Point3D> cloud, int k) {
    Map<Point3D, Point3D> newNormals = new HashMap<>();
    for (Point3D p : cloud) {
        List<Point3D> neighbors = findKNearestNeighbors(p, cloud, k);
        Point3D normal = normalMap.get(p);
        if (normal == null) continue;
        int agree = 0;
        for (Point3D n : neighbors) {
            Point3D nn = normalMap.get(n);
            if (nn != null && normal.dotProduct(nn) > 0) agree++;
        }
        if (agree < neighbors.size() / 2) {
            normal = normal.multiply(-1); // flip if most neighbors disagree
        }
        // Optional: average with neighbors for smoothness
        Point3D avg = normal;
        for (Point3D n : neighbors) {
            Point3D nn = normalMap.get(n);
            if (nn != null) avg = avg.add(nn);
        }
        avg = avg.normalize();
        newNormals.put(p, avg);
    }
    return newNormals;
}
    public static Map<Point3D, Point3D> estimateNormals(List<Point3D> pointCloud, int k) {
        Map<Point3D, Point3D> normalMap = new HashMap<>();

        // Step 0: Compute global centroid of the point cloud
        Point3D centroid = computeCentroid(pointCloud);

        for (Point3D p : pointCloud) {
            List<Point3D> neighbors = findKNearestNeighbors(p, pointCloud, k);
            Point3D normal = estimateNormal(p, neighbors);
            if (normal == null) {
                continue;
            }

            // Step 1: Reorient normal to point outward from centroid
            Point3D toCentroid = centroid.subtract(p);
            if (normal.dotProduct(toCentroid) > 0) {
                normal = normal.multiply(-1); // Flip inward-pointing normal
            }

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
