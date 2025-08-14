package IsosurfaceFX;

public enum SurfaceMode {
    /**
     * Standard Marching Cubes algorithm on a regular grid (SDF or density field).
     * Produces isosurfaces, but can suffer from ambiguous/case cracks.
     */
    MARCHING_CUBES("Marching Cubes"),

    /**
     * Robust, watertight isosurface extraction using Marching Tetrahedra.
     * Works on the same field/grid as Marching Cubes, but guarantees closed surfaces.
     */
    MARCHING_TETRAHEDRA("Marching Tetrahedra"),

    /**
     * Carved Convex Hull method.
     * Approximates a concave hull by filtering faces of the convex hull using circumradius.
     */
    CARVED_CONCAVE_HULL("Carved Convex Hull"),

    /**
     * Contraction Concave Hull method.
     * Approximates a concave hull by using a Point Cloud Contraction theorem.
     */
    CONTRACTION_CONCAVE_HULL("Contraction Concave Hull"),

    /**
     * (Future) True Delaunay+Alpha Shape extraction.
     * For tight, minimal, data-driven concave hulls.
     */
    DELAUNAY_ALPHA_SHAPE("Alpha Shape (Delaunay)");

    private final String displayName;

    SurfaceMode(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get a human-friendly name for UI or logging.
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
