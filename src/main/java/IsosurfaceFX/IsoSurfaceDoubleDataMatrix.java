package IsosurfaceFX;

/**
 * This implementation of the IsoSurfaceDataMatrix creates an underlying 3-D
 *  double array as storage for the data set.
**/
public class IsoSurfaceDoubleDataMatrix extends IsoSurfaceDataMatrix {

  // Inherits plane
  // Inherits xMaxDim, yMaxDim, zMaxDim
  // Inherits xScale, yScale, zScale
  // Inherits majorAxisOffset, minorAxisOffset

  /**
   * The minumum value a point in the matrix can have.  leastValue is used
   *  for indices of the matrix that are outside the bounds of the arrays that
   *  make up the matrix.  This facilitates memory saving by saying that every
   *  point beyond the scope of the arrays is a point of value leastValue.  This
   *  is similar to the concept of a sparse matrix.
  **/
  private final double leastValue;

  /**
   * The 3-D matrix of values that represent the data of the isosurface that
   *  is to be generated.
  **/
  private double[][][] matrix;

  //----< constructor >-----------------------------------------------------//

  /**
   * This is the component constructor.  It takes the pieces that make up a
   *  data matrix (aside from the actual matrix of data) and sets its internal
   *  member variables to them.
   * @param _plane The plane of the two dependent axes are in.  The offsets
   *  will be applied to the these two axes.
   * @param _xMaxDim The maximum size any of the arrays in the x-axis may have. 
   * @param _yMaxDim The maximum size any of the arrays in the y-axis may have. 
   * @param _zMaxDim The maximum size any of the arrays in the z-axis may have.
   * @param _xScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the x-axis.
   * @param _yScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the y-axis.
   * @param _zScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the z-axis.
   * @param _majAxisOffs This is an array of offsets that allows for centering
   *  the data along the major axis.
   * @param _minAxisOffs This is an array of offsets that allows for centering
   *  the data along the minor axis.
   * @param _centroidPercentages A Tuple3f containing the percentages from the
   *  lower-left-front of the data set that the should be considered the center
   *  of said data set.  For example, (0.0, 0.0, 0.0) will make the
   *  lower-left-front corner of the data the center of the data, and
   *  (1.0, 1.0, 1.0) will make the upper-right-back the center of the data.
   *  This center is typically the point which about rotations of the data
   *  will be applied.
   * @param _leastValue The minumum value a point in the matrix can have.
   *  leastValue is used for indices of the matrix that are outside the bounds
   *  of the arrays that make up the matrix.  This facilitates memory saving by
   *  saying that every point beyond the scope of the arrays is a point of
   *  value leastValue.  This is similar to the concept of a sparse matrix.
  **/
  public IsoSurfaceDoubleDataMatrix(final byte _plane,
                                   final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                                   final float _xScale, final float _yScale, final float _zScale,
                                   final int[] _majAxisOffs, final int[] _minAxisOffs,
                                   final Tuple3f _centroidPercentages,
                                   final double _leastValue)
  {
    super(_plane, _xMaxDim, _yMaxDim, _zMaxDim, _xScale, _yScale, _zScale,
          _majAxisOffs, _minAxisOffs, _centroidPercentages);

    leastValue = _leastValue;

      // Determine the size of the independent axis.
    switch(plane) {
      case XZ_PLANE : matrix = new double[_yMaxDim][][];
                      break;
      case XY_PLANE : matrix = new double[_zMaxDim][][];
                      break;
      default :       matrix = new double[_xMaxDim][][];
    } // end switch
    matRef = matrix;

  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This is the component constructor.  It takes the pieces that make up a
   *  data matrix (aside from the actual matrix of data) and sets its internal
   *  member variables to them.
   * This constructor takes no offset arrays.  They are derived from the length
   *  of the independent axis.
   * @param _plane The plane of the two dependent axes are in.  The offsets
   *  will be applied to the these two axes.
   * @param _xMaxDim The maximum size any of the arrays in the x-axis may have. 
   * @param _yMaxDim The maximum size any of the arrays in the y-axis may have. 
   * @param _zMaxDim The maximum size any of the arrays in the z-axis may have.
   * @param _xScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the x-axis.
   * @param _yScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the y-axis.
   * @param _zScale The scaling factor that translates one meter to whatever
   *  units are used in the data along the z-axis.
   * @param _centroidPercentages A Tuple3f containing the percentages from the
   *  lower-left-front of the data set that the should be considered the center
   *  of said data set.  For example, (0.0, 0.0, 0.0) will make the
   *  lower-left-front corner of the data the center of the data, and
   *  (1.0, 1.0, 1.0) will make the upper-right-back the center of the data.
   *  This center is typically the point which about rotations of the data
   *  will be applied.
   * @param _leastValue The minumum value a point in the matrix can have.
   *  leastValue is used for indices of the matrix that are outside the bounds
   *  of the arrays that make up the matrix.  This facilitates memory saving by
   *  saying that every point beyond the scope of the arrays is a point of
   *  value leastValue.  This is similar to the concept of a sparse matrix.
  **/
  public IsoSurfaceDoubleDataMatrix(final byte _plane,
                                    final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                                    final float _xScale, final float _yScale, final float _zScale,
                                    final Tuple3f _centroidPercentages,
                                    final byte _leastValue)
  {
    super(_plane, _xMaxDim, _yMaxDim, _zMaxDim,
          _xScale, _yScale, _zScale, _centroidPercentages);

    leastValue = _leastValue;

      // Determine the size of the independent axis.
    switch(plane) {
      case XZ_PLANE : matrix = new double[_yMaxDim][][];
                      break;
      case XY_PLANE : matrix = new double[_zMaxDim][][];
                      break;
      default :       matrix = new double[_xMaxDim][][];
    } // end switch
    matRef = matrix;    

  } // end constructor

  ////////////////////////////////////////////////////////////////////////////
  // Setters
  ////////////////////////////////////////////////////////////////////////////

  //----< setPlaneData >----------------------------------------------------//

  /**
   * This method allows us to fill in the data per the dependent axes of the
   *  data matrix.  The dependent axes are specifed by the _plane parameter of 
   *  the constructor.
   * @param _idx The index in the independent axis of the matrix the plane of
   *  data corresponds to.
   * @param _dataPlane A two dimensional double array that contains a plane's
   *  worth of 3-D data to be inserted at the index specified.
  **/
  public void setPlaneData(final int _idx, final double[][] _dataPlane) {
    super.setPlaneData(_idx, _dataPlane);
    //matrix[_idx] = _dataPlane;
  } // end setPlaneData

  //----< setPlaneData >----------------------------------------------------//

  /**
   * This method allows us to fill in the data per the dependent axes of the
   *  data matrix.  The dependent axes are specifed by the _plane parameter of 
   *  the constructor.  This method also allows the user to specify the point
   *  in the 2-D plane that is the center of the data.  The offsets for the
   *  data matrix can be calculated from this information.
   * @param _idx The index in the independent axis of the matrix the plane of
   *  data corresponds to.
   * @param _dataPlane A two dimensional double array that contains a plane's
   *  worth of 3-D data to be inserted at the index specified.
   * @param _majAxisCenter The index value along the major axis of the data
   *  plane that is the center of this plane.
   * @param _minAxisCenter The index value along the minor axis of the data
   *  plane that is the center of this plane.
  **/
  public void setPlaneData(final int _idx, final double[][] _dataPlane,
                           final int _majAxisCenter, final int _minAxisCenter)
  {
    super.setPlaneData(_idx, _dataPlane, _majAxisCenter, _minAxisCenter);
  } // end setPlaneData

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  //----< getPoint >--------------------------------------------------------//

  /**
   * This method will return the value of in the matrix at the point
   *  (_x, _y, _z).  Any indices not within the range of the matrix dimensions
   *  will automatically return the leastValue specified for the matrix.
   * @param _x The index of the point along the x-axis.
   * @param _y The index of the point along the y-axis.
   * @param _z The index of the point along the z-axis.
   * @return A double containing the value of the point at location
   *  (_x, _y, _z).
  **/
  @Override
public double getPoint(final int _x, final int _y, final int _z) {
    double retPt = leastValue;   // Rtrn leastValue if can't find val in mtrx.

    int indAxis;
    int majAxis;
    int minAxis;
    int majAxisIdx, minAxisIdx;

    switch(plane) {
      case XZ_PLANE : indAxis = _y;
                      majAxis = _x;
                      minAxis = _z;
                      break;
      case XY_PLANE : indAxis = _z;
                      majAxis = _x;
                      minAxis = _y;
                      break;
      default :       indAxis = _x;
                      majAxis = _y;
                      minAxis = _z;
    } // end switch

    if(matrix != null && indAxis >= 0 && indAxis < matrix.length) {
      majAxisIdx = majAxis - majorAxisOffsets[indAxis];
      if(matrix[indAxis] != null && majAxisIdx >= 0
         && majAxisIdx < matrix[indAxis].length)
      {
        minAxisIdx = minAxis - minorAxisOffsets[indAxis];
        if(matrix[indAxis][majAxisIdx] != null && minAxisIdx >= 0
           && minAxisIdx < matrix[indAxis][majAxisIdx].length)
          retPt = matrix[indAxis][majAxisIdx][minAxisIdx];
      } // end if
    } // end if

    return(retPt);

  } // end getPoint

} // end class IsoSurfaceDoubleDataMatrix
