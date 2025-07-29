package IsosurfaceFX;

import javafx.geometry.Point3D;

/**
 * Basically just a Face class but...
 * @author Sean Phillips
 */
public class Triangle3D {
    public final Point3D p1, p2, p3;

    public Triangle3D(Point3D p1, Point3D p2, Point3D p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }
}

