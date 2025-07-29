package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SyntheticPointCloudGenerator {
    public enum PointCloudShape {
        SPHERE,
        CUBE,
        TORUS
    }
    private static final Random random = new Random();

    public static List<Point3D> generate(PointCloudShape shape, int count) {
        switch (shape) {
            case SPHERE:
                return generateSphere(count);
            case CUBE:
                return generateCube(count);
            case TORUS:
                return generateTorus(count);
            default:
                throw new IllegalArgumentException("Unknown shape: " + shape);
        }
    }

    private static List<Point3D> generateSphere(int count) {
        List<Point3D> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            points.add(new Point3D(x, y, z));
        }
        return points;
    }

    private static List<Point3D> generateCube(int count) {
        List<Point3D> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double x = 2 * random.nextDouble() - 1;
            double y = 2 * random.nextDouble() - 1;
            double z = 2 * random.nextDouble() - 1;
            points.add(new Point3D(x, y, z));
        }
        return points;
    }

    private static List<Point3D> generateTorus(int count) {
        List<Point3D> points = new ArrayList<>(count);
        double R = 0.7;  // Major radius
        double r = 0.3;  // Minor radius

        for (int i = 0; i < count; i++) {
            double u = 2 * Math.PI * random.nextDouble();
            double v = 2 * Math.PI * random.nextDouble();
            double x = (R + r * Math.cos(v)) * Math.cos(u);
            double y = (R + r * Math.cos(v)) * Math.sin(u);
            double z = r * Math.sin(v);
            points.add(new Point3D(x, y, z));
        }
        return points;
    }
}
