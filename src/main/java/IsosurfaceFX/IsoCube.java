package IsosurfaceFX;

/**
 * This class is used to determine the occupied edges of the cube (for triangle
 *  reduction) as well as code to calculate the "grey" edges of the cube.  Grey
 *  edges are the one's that are interpolated upon to reduce the triangle
 *  count.
 * This class is used by IsoDriver.
**/
public class IsoCube implements IsoConstInterface {

  /** The x component of the IsoCube's lower/left/back corner location. **/
  protected int x;
  /** The y component of the IsoCube's lower/left/back corner location. **/
  protected int y;
  /** The z component of the IsoCube's lower/left/back corner location. **/
  protected int z;

  /**
   * The bitstring containing all the information about the Isocube given at
   *  location (x, y, z).
  **/
  protected int config;

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor will take in the indices of an IsoCube and the vertices
   *  of that cube that are within the specified thresholds and determine
   *  which edges are occupied and which edges are "grey" edges.
   * @param _x The x component of the IsoCube's lower/left/back corner
   *  location.
   * @param _y The y component of the IsoCube's lower/left/back corner
   *  location.
   * @param _z The z component of the IsoCube's lower/left/back corner
   *  location.
   * @param _verts An integer that contains the bitstring encoding of which of
   *  the eight vertices of the cube are within the thresholds specified.
  **/
  public IsoCube(final int _x, final int _y, final int _z, final int _verts) {
    x = _x;
    y = _y;
    z = _z;

    // Only care about the lower 8 bits, & 255 wipes out the rest of the bits.
    config = getCubeOccEdges((_verts & 255));
    config = config | getCubeGreyEdges(x, y, z);

  } // end constructor

  //----< getCubeOccEdges >-------------------------------------------------//

  /**
   * This method finds the occupied edges of the cube given the bitstring
   *  encoding of the cube vertices that are "good".
   * @param _verts An integer that contains the bitstring encoding of which of
   *  the eight vertices of the cube are within the thresholds specified.
   * @return An int containing the bitstring encoding of the vertices and the
   *  occupied edges.
  **/
  public static int getCubeOccEdges(final int _verts) {
    int config = _verts;

    if(((config & VERT0) == VERT0) && ((config & VERT1) == 0) ||
       ((config & VERT0) == 0) && ((config & VERT1) == VERT1))
    {
      config = config | EDGE_OCC_0;
    } // end if
    if(((config & VERT1) == VERT1) && ((config & VERT2) == 0) ||
       ((config & VERT1) == 0) && ((config & VERT2) == VERT2))
    {
      config = config | EDGE_OCC_1;
    } // end if
    if(((config & VERT2) == VERT2) && ((config & VERT3) == 0) ||
       ((config & VERT2) == 0) && ((config & VERT3) == VERT3))
    {
      config = config | EDGE_OCC_2;
    } // end if
    if(((config & VERT3) == VERT3) && ((config & VERT0) == 0) ||
       ((config & VERT3) == 0) && ((config & VERT0) == VERT0))
    {
      config = config | EDGE_OCC_3;
    } // end if

    if(((config & VERT4) == VERT4) && ((config & VERT5) == 0) ||
       ((config & VERT4) == 0) && ((config & VERT5) == VERT5))
    {
      config = config | EDGE_OCC_4;
    } // end if
    if(((config & VERT5) == VERT5) && ((config & VERT6) == 0) ||
       ((config & VERT5) == 0) && ((config & VERT6) == VERT6))
    {
      config = config | EDGE_OCC_5;
    } // end if
    if(((config & VERT6) == VERT6) && ((config & VERT7) == 0) ||
       ((config & VERT6) == 0) && ((config & VERT7) == VERT7))
    {
      config = config | EDGE_OCC_6;
    } // end if
    if(((config & VERT7) == VERT7) && ((config & VERT4) == 0) ||
       ((config & VERT7) == 0) && ((config & VERT4) == VERT4))
    {
      config = config | EDGE_OCC_7;
    } // end if

    if(((config & VERT0) == VERT0) && ((config & VERT4) == 0) ||
       ((config & VERT0) == 0) && ((config & VERT4) == VERT4))
    {
      config = config | EDGE_OCC_8;
    } // end if
    if(((config & VERT1) == VERT1) && ((config & VERT5) == 0) ||
       ((config & VERT1) == 0) && ((config & VERT5) == VERT5))
    {
      config = config | EDGE_OCC_9;
    } // end if
    if(((config & VERT2) == VERT2) && ((config & VERT6) == 0) ||
       ((config & VERT2) == 0) && ((config & VERT6) == VERT6))
    {
      config = config | EDGE_OCC_10;
    } // end if
    if(((config & VERT3) == VERT3) && ((config & VERT7) == 0) ||
       ((config & VERT3) == 0) && ((config & VERT7) == VERT7))
    {
      config = config | EDGE_OCC_11;
    } // end if

    return(config);    

  } // end calcCube

  //----< getCubeGreyEdges >------------------------------------------------//

  /**
   * This method finds the grey edges of the cube given the indices of the
   *  lower/left/back point on the cube.
   * @param _x The x component of the IsoCube's lower/left/back corner
   *  location.
   * @param _y The y component of the IsoCube's lower/left/back corner
   *  location.
   * @param _z The z component of the IsoCube's lower/left/back corner
   *  location.
   * @return An integer containing the bitstring encoding of the vertices, 
   *  occupied edges, and grey edges of the IsoCube.
  **/
  public int getCubeGreyEdges(final int _x, final int _y, final int _z) {

      // Vertical lines
    if(_x%2 == 0 && _z%2 == 0)
      config = config | EDGE_GREY_3;
    else if(_x%2 != 0 && _z%2 == 0)
      config = config | EDGE_GREY_1;
    else if(_x%2 == 0 && _z%2 != 0)
      config = config | EDGE_GREY_7;
    else if(_x%2 != 0 && _z%2 != 0)
      config = config | EDGE_GREY_5;

      // Horizontal lines
    if(_y%2 != 0 && _z%2 != 0)
      config = config | EDGE_GREY_0;
    else if(_y%2 == 0 && _z%2 != 0)
      config = config | EDGE_GREY_2;
    else if(_y%2 != 0 && _z%2 == 0)
      config = config | EDGE_GREY_4;
    else if(_y%2 == 0 && _z%2 == 0)
      config = config | EDGE_GREY_6;

      // Depth lines
    if(_x%2 != 0 && _y%2 == 0)
      config = config | EDGE_GREY_8;
    else if(_x%2 == 0 && _y%2 == 0)
      config = config | EDGE_GREY_9;
    else if(_x%2 == 0 && _y%2 != 0)
      config = config | EDGE_GREY_10;
    else if(_x%2 != 0 && _y%2 != 0)
      config = config | EDGE_GREY_11;

    return(config);

  } // end getCubeGreyEdges

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  /**
   * This method is a getter for the bitstring of IsoCube information.
   * @return An integer containing the bitstring encoding of the vertices, 
   *  occupied edges, and grey edges of the IsoCube.
  **/
  public int getBitString() {
    return(config);
  } // end getBitString

} // end class IsoCube

