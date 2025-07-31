package IsosurfaceFX;

/**
 *
 * @author Sean Phillips
 */
import javafx.geometry.Point3D;

public class FixedMatrix {
    private final double[][] m; // 3x3 matrix: m[row][col]

    public FixedMatrix(double[][] values) {
        if (values.length != 3 || values[0].length != 3)
            throw new IllegalArgumentException("Matrix must be 3x3");
        this.m = new double[3][3];
        for (int i = 0; i < 3; i++)
            System.arraycopy(values[i], 0, this.m[i], 0, 3);
    }

    public Point3D multiply(Point3D v) {
        double x = m[0][0] * v.getX() + m[0][1] * v.getY() + m[0][2] * v.getZ();
        double y = m[1][0] * v.getX() + m[1][1] * v.getY() + m[1][2] * v.getZ();
        double z = m[2][0] * v.getX() + m[2][1] * v.getY() + m[2][2] * v.getZ();
        return new Point3D(x, y, z);
    }

    public FixedMatrix add(double scalar) {
        double[][] result = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                result[i][j] = m[i][j] + (i == j ? scalar : 0);
        return new FixedMatrix(result);
    }

    public FixedMatrix subtract(double scalar) {
        return add(-scalar);
    }

    public Point3D smallestEigenVectorPowerIter(int maxIters, double tol) {
        // Spectral shift: add small multiple of identity so smallest becomes dominant
        double trace = m[0][0] + m[1][1] + m[2][2];
        double shift = trace + 1e-3;

        FixedMatrix shifted = this.subtract(shift);

        Point3D v = new Point3D(1, 1, 1).normalize();
        for (int iter = 0; iter < maxIters; iter++) {
            Point3D vNext = shifted.multiply(v);
            double mag = vNext.magnitude();
            if (mag < 1e-10) break; // avoid divide by zero
            vNext = new Point3D(vNext.getX() / mag, vNext.getY() / mag, vNext.getZ() / mag);

            if (vNext.distance(v) < tol)
                return vNext;

            v = vNext;
        }
        return v;
    }

    public static FixedMatrix fromCovariance(Point3D[] neighbors, Point3D centroid) {
        double xx = 0, xy = 0, xz = 0;
        double yy = 0, yz = 0, zz = 0;
        for (Point3D p : neighbors) {
            double dx = p.getX() - centroid.getX();
            double dy = p.getY() - centroid.getY();
            double dz = p.getZ() - centroid.getZ();
            xx += dx * dx;
            xy += dx * dy;
            xz += dx * dz;
            yy += dy * dy;
            yz += dy * dz;
            zz += dz * dz;
        }
        int n = neighbors.length;
        double[][] m = {
            {xx / n, xy / n, xz / n},
            {xy / n, yy / n, yz / n},
            {xz / n, yz / n, zz / n}
        };
        return new FixedMatrix(m);
    }
}
