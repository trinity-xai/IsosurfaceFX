package IsosurfaceFX;

/**
 * This interface provides constants that are used to generate the integer
 *  representation of a cube in the skeleton climbing algorithm.
**/
public interface IsoConstInterface {

  // Generic

  public static int FALSE = 0;
  public static int TRUE = 1;

  public static int X = 0;
  public static int Y = 1;
  public static int Z = 2;

  // Which vertices are at or above the threshold. For IsoCube.

  public static int VERT0 = 1;
  public static int VERT1 = VERT0 << 1;
  public static int VERT2 = VERT1 << 1;
  public static int VERT3 = VERT2 << 1;
  public static int VERT4 = VERT3 << 1;
  public static int VERT5 = VERT4 << 1;
  public static int VERT6 = VERT5 << 1;
  public static int VERT7 = VERT6 << 1;

  // Which edges are occupied by a isosurface vertex. For IsoCube.
  
  public static int EDGE_OCC_0  = VERT7 << 1;       // 256
  public static int EDGE_OCC_1  = EDGE_OCC_0 << 1;
  public static int EDGE_OCC_2  = EDGE_OCC_1 << 1;
  public static int EDGE_OCC_3  = EDGE_OCC_2 << 1;
  public static int EDGE_OCC_4  = EDGE_OCC_3 << 1;
  public static int EDGE_OCC_5  = EDGE_OCC_4 << 1;
  public static int EDGE_OCC_6  = EDGE_OCC_5 << 1;
  public static int EDGE_OCC_7  = EDGE_OCC_6 << 1;
  public static int EDGE_OCC_8  = EDGE_OCC_7 << 1;
  public static int EDGE_OCC_9  = EDGE_OCC_8 << 1;
  public static int EDGE_OCC_10 = EDGE_OCC_9 << 1;
  public static int EDGE_OCC_11 = EDGE_OCC_10 << 1;

  // Which edges are grey edges (for step 4 of skeleton climbing).
  //  For IsoCube

  public static int EDGE_GREY_0  = EDGE_OCC_11 << 1;   // 1048576
  public static int EDGE_GREY_1  = EDGE_GREY_0 << 1;
  public static int EDGE_GREY_2  = EDGE_GREY_1 << 1;
  public static int EDGE_GREY_3  = EDGE_GREY_2 << 1;
  public static int EDGE_GREY_4  = EDGE_GREY_3 << 1;
  public static int EDGE_GREY_5  = EDGE_GREY_4 << 1;
  public static int EDGE_GREY_6  = EDGE_GREY_5 << 1;
  public static int EDGE_GREY_7  = EDGE_GREY_6 << 1;
  public static int EDGE_GREY_8  = EDGE_GREY_7 << 1;
  public static int EDGE_GREY_9  = EDGE_GREY_8 << 1;
  public static int EDGE_GREY_10 = EDGE_GREY_9 << 1;
  public static int EDGE_GREY_11 = EDGE_GREY_10 << 1;

} // end interface IsoConstInterface
