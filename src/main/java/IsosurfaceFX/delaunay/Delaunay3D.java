package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.List;

/** Produces a Delaunay tetrahedralization of a 3D point set. */
public interface Delaunay3D {
    /** @return tets as 4-tuples of indices into the input points list. */
    List<int[]> tetrahedralize(List<Point3D> points);
}
