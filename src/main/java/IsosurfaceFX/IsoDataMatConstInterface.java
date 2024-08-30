package IsosurfaceFX;

/**
 * This interface contains constants for the Isosurface data matrix classes.
**/
public interface IsoDataMatConstInterface {
  /**
   * This centroid value is in the lower-leftmost point of the geometry.  This
   *  means all the geometry is drawn in the pos-x, pos-y, neg-z quadrant of
   *  3-D cartesian space.
  **/
  public static final Tuple3f LOWER_LEFT = new Tuple3f(0.0f, 0.0f, 0.0f);

  /**
   * This centroid value is right in the middle of the geometry.  This point is
   *  the median point of the each of the min and max vertex points in each of
   *  the three coordinate axes.
  **/
  public static final Tuple3f CENTER = new Tuple3f(0.5f, 0.5f, 0.5f);

  /**
   * This centroid value is in the middle of the geometry with respect to the
   *  xz-plane, like CENTER.  However, the y component of the centroid will be
   *  the minimum of the y-components of the set of vertices that comprise the
   *  geometry.  This one was added to support radar isosurfaces, the reason
   *  the code was implemented in the first place.
  **/
  public static final Tuple3f CENTER_SANS_ALT = new Tuple3f(0.5f, 0.0f, 0.5f);

  /**
   * This is a switch to inform the matrix which plane contains variable
   *  length data.  When it's this value, the Y-axis is the one that must have
   *  a set size, meaning there are no axis offsets applied to this axis to
   *  shift points to the right locations, the data must be padded.
   *
   * For example (and continuing with the radar usage), if the altitude is the
   *  independent variable, as is the case with horizontal detection plots, all
   *  the platters have points for the entire set of altitudes represented, then
   *  the plane should be set to the XZ_PLANE.  There should be a major and
   *  minor offset posted for each of the altitudes within the data, i.e. the
   *  offset arrays have the same cardinality as the y-dimension of the matrix.
   *  In our example, the x-axis would be the major and the z-axis the minor.
  **/
  public static final byte XZ_PLANE = 0;

  /**
   * Just like XZ_PLANE, except that z is the indepentent axis.
  **/
  public static final byte XY_PLANE = 1;

  /**
   * Just like XZ_PLANE, except that x is the indepentent axis.
  **/
  public static final byte YZ_PLANE = 2;

  // Comparitors

  /** The comparitor to use when equality is desired. **/
  public static final byte EQUAL = 0;

  /**
   * The comparitor to use when values less than the specified threshold are
   *  desired.
  **/
  public static final byte LESS_THAN = 1;

  /**
   * The comparitor to use when values less than or equal to the specified
   *  threshold are desired.
  **/
  public static final byte LESS_THAN_EQ = 2;
 
  /**
   * The comparitor to use when values greater than the specified threshold are
   *  desired.
  **/
  public static final byte GREATER_THAN = 3;

  /**
   * The comparitor to use when values greater than or equal to the specified
   *  threshold are desired.
  **/
  public static final byte GREATER_THAN_EQ = 4;

} // end interface IsoDataMatConstInterface
