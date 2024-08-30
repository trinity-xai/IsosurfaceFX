package IsosurfaceFX;

/**
 * This abstract class is the basis for all vertex colorers.
**/
public abstract class IsoSurfaceVertexColorer {

  public static final byte NONE    = 0;
  public static final byte UNLIT   = 1;
  public static final byte LIT     = 2;
  public static final byte BOTH    = 3;

  protected boolean transEnabled; // false by default

  protected byte colorerType;

  //----< Constructor >-----------------------------------------------------//

  /**
   * This is the default constructor.  It's used when there is no translucency
   *  produced by the colorer.
  **/
  public IsoSurfaceVertexColorer() {
    transEnabled = false;
    colorerType = BOTH;
  } // end constructor

  //----< Constructor >-----------------------------------------------------//

  /**
   * This constructor allows the user to specify whether or not translucency
   *  will be produced by the colorer.
   * @param _transEnabled
  **/
  public IsoSurfaceVertexColorer(final boolean _transEnabled) {
    transEnabled = _transEnabled;
    colorerType = BOTH;
  } // end constructor

  //----< Constructor >-----------------------------------------------------//

  /**
   * This is the default constructor.  It's used when there is no translucency
   *  produced by the colorer.
  **/
  public IsoSurfaceVertexColorer(final byte _type) {
    transEnabled = false;
    colorerType = BOTH;
    if(_type >= NONE && _type < BOTH)
      colorerType = _type;
  } // end constructor

  //----< Constructor >-----------------------------------------------------//

  /**
   * This constructor allows the user to specify whether or not translucency
   *  will be produced by the colorer.
   * @param _transEnabled
  **/
  public IsoSurfaceVertexColorer(final boolean _transEnabled, final byte _type) {
    transEnabled = _transEnabled;
    colorerType = BOTH;
    if(_type >= NONE && _type < BOTH)
      colorerType = _type;
  } // end constructor

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  /**
   * Allows enabling of the translucency.
  **/
  public void setTranslucencyEnabled(final boolean _transEnabled) {
    transEnabled = _transEnabled;
  } // end setTranslucencyEnabled

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  //----< isTranslucencyEnabled >-------------------------------------------//

  /**
   * This method returns whether or not translucency is required to show the
   *  output of the colorer's work.
   * @return The boolean containing true if the coloring information contains
   *  translucency, false otherwise.
  **/
  public boolean isTranslucencyEnabled() {
    return(transEnabled);
  } // end isTranslucencyEnabled

  //----< getColorerType >--------------------------------------------------//

  /**
   * This method will return the types of color arrays the colorer is capable
   *  of generating.  It's the responsibility of those writing colorer
   *  subclasses to make sure that the type is consistent (via a call to one
   *  of this class's constructors) with what the subclass will actually
   *  create with the calc* methods.
   * @return a byte containing the type of colorer a IsoSurfaceVertexColorer
   *  is.  Can be one of NONE, UNLIT, LIT, or BOTH.
  **/
  public byte getColorerType() {
    return(colorerType);
  } // end getColorerType

  ////////////////////////////////////////////////////////////////////////////
  // Abstract "Getter" Methods
  ////////////////////////////////////////////////////////////////////////////

  //----< calcUnlitColor >--------------------------------------------------//

  /**
   * This method will calculate the color of the vertex, based on the vertex
   *  itself, the location of the vertex in the original data matrix, and the
   *  value of that point in the matrix.
   * @param _vert A vertex of the geometry to get a color for, in float format.
   * @param _x The index of the x component in the data matrix.
   * @param _y The index of the y component in the data matrix.
   * @param _z The index of the z component in the data matrix.
   * @param _val The value of the data at the point dataMatrix[_x, _y, _z],
   *  in double representation.
   * @return an array of floats containing the components of the color the
   *  vertex is to be colored.  Can be lit or unlit color, the array length
   *  will vary depending on which it is to do.
  **/
  public abstract float[] calcUnlitColor(Vector3f _vert,
                                         int _x, int _y, int _z,
                                         double _val);

  //----< calcUnlitColor >--------------------------------------------------//

  /**
   * This method will calculate the color of the vertex, based on the vertex
   *  itself, the location of the vertex in the original data matrix, and the
   *  value of that point in the matrix.
   * @param _vert A vertex of the geometry to get a color for, in double format.
   * @param _x The index of the x component in the data matrix.
   * @param _y The index of the y component in the data matrix.
   * @param _z The index of the z component in the data matrix.
   * @param _val The value of the data at the point dataMatrix[_x, _y, _z],
   *  in double representation.
   * @return an array of floats containing the components of the color the
   *  vertex is to be colored.  Can be lit or unlit color, the array length
   *  will vary depending on which it is to do.
  **/
  public abstract float[] calcUnlitColor(Vector3d _vert,
                                         int _x, int _y, int _z,
                                         double _val);

  //----< calcLitColor >----------------------------------------------------//

  /**
   * This method will calculate the color of the vertex, based on the vertex
   *  itself, the location of the vertex in the original data matrix, and the
   *  value of that point in the matrix.
   * @param _vert A vertex of the geometry to get a color for, in float format.
   * @param _x The index of the x component in the data matrix.
   * @param _y The index of the y component in the data matrix.
   * @param _z The index of the z component in the data matrix.
   * @param _val The value of the data at the point dataMatrix[_x, _y, _z],
   *  in double representation.
   * @return an array of floats containing the components of the color the
   *  vertex is to be colored.  Can be lit or unlit color, the array length
   *  will vary depending on which it is to do.
  **/
  public abstract float[] calcLitColor(Vector3f _vert,
                                       int _x, int _y, int _z,
                                       double _val);

  //----< calcLitColor >----------------------------------------------------//

  /**
   * This method will calculate the color of the vertex, based on the vertex
   *  itself, the location of the vertex in the original data matrix, and the
   *  value of that point in the matrix.
   * @param _vert A vertex of the geometry to get a color for, in double format.
   * @param _x The index of the x component in the data matrix.
   * @param _y The index of the y component in the data matrix.
   * @param _z The index of the z component in the data matrix.
   * @param _val The value of the data at the point dataMatrix[_x, _y, _z],
   *  in double representation.
   * @return an array of floats containing the components of the color the
   *  vertex is to be colored.  Can be lit or unlit color, the array length
   *  will vary depending on which it is to do.
  **/
  public abstract float[] calcLitColor(Vector3d _vert,
                                       int _x, int _y, int _z,
                                       double _val);

} // end IsoSurfaceVertexColorer
