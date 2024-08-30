package IsosurfaceFX;

import java.lang.reflect.Array;

/**
 * This abstract class is the basis for the data that an IsoSurfaceGenerator
 *  needs to construct an IsoSurface.  This is a 3-D matrix that can have
 *  variable-length data in two of the three dimensions.  The static length
 *  dimension is the one that is always the first index of the construct.  The
 *  second dimension is the one that is considered the major axis; the third
 *  is the minor axis.  This varies the positions of the x,y,z tuples within
 *  the data structure, so population and access of the matrix should be done
 *  with great care.
**/
public abstract class IsoSurfaceDataMatrix implements IsoDataMatConstInterface {
  /**
   * This member variable stores the independent axis data (or the plane that
   *  accomodates variable length arrays, depending on how you look at it).
   *  Can be set to one of XZ_PLANE, XY_PLANE, or YZ_PLANE.
  **/
  protected byte plane;

  /**
   * This is the maximum size the matrix can be in the x-axis.  Necessary to
   *  keep track of to allow for a smaller array than maximum size.
  **/
  protected int xMaxDim;

  /**
   * This is the maximum size the matrix can be in the y-axis.  Necessary to
   *  keep track of to allow for a smaller array than maximum size.
  **/
  protected int yMaxDim;

  /**
   * This is the maximum size the matrix can be in the z-axis.  Necessary to
   *  keep track of to allow for a smaller array than maximum size.
  **/
  protected int zMaxDim;

  /** A scaling factor for the x-axis. **/
  protected float xScale;
  /** A scaling factor for the y-axis. **/
  protected float yScale;
  /** A scaling factor for the z-axis. **/
  protected float zScale;

  /**
   * The array of offsets at each level of the independent axis.  This allows
   *  shifting of the data if the size of the arrays are not uniform.
   * It is chosen alphabetically from the dependent axes.  So if the matrix's
   *  indepentent axis is Y (XZ_PLANE), then these offsets will be for the
   *  x-axis since x comes before z in the alphabet.
  **/
  protected int[] majorAxisOffsets; 

  /**
   * The array of offsets at each level of the independent axis.  This allows
   *  shifting of the data if the size of the arrays are not uniform.
   *  This is the leftover axis after the major axis is determined.
   * So if the matrix's indepentent axis is Y (XZ_PLANE), then these offsets
   *  will be for the z-axis since x was chosen alphabetically to be the major
   *  axis.
  **/
  protected int[] minorAxisOffsets; // the leftover axis aftr major is detrmd.

  /**
   * This is the centroid of the geometry.  It allows you to pick an arbitrary
   *  point within the geometry to be the geometry's position.  When rotations
   *  are applied to the geometry, they will occur about this point.  It may
   *  help to think of it as a "center of gravity", though that's not really
   *  what it represents.
  **/
  protected Tuple3f centroid;

  /** The reference to the underlying data matrix. **/
  protected Object matRef;

  // Have to add the least value in the subclasses.

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
  **/
  public IsoSurfaceDataMatrix(final byte _plane,
                              final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                              final float _xScale, final float _yScale, final float _zScale,
                              final int[] _majAxisOffs, final int[] _minAxisOffs,
                              final Tuple3f _centroidPercentages)
  {
    set(_plane, _xMaxDim, _yMaxDim, _zMaxDim,
        _xScale, _yScale, _zScale,
        _majAxisOffs, _minAxisOffs, _centroidPercentages);
  } // end constructor
 
  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor is like the previous one with the exception of no offset
   *  arrays.  They are derived from the length of the independent axis.
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
  **/
  public IsoSurfaceDataMatrix(final byte _plane,
                              final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                              final float _xScale, final float _yScale, final float _zScale,
                              final Tuple3f _centroidPercentages)
  {
    set(_plane, _xMaxDim, _yMaxDim, _zMaxDim,
        _xScale, _yScale, _zScale, _centroidPercentages);
  } // end constructor

  ////////////////////////////////////////////////////////////////////////////
  // Setters
  ////////////////////////////////////////////////////////////////////////////

  //----< set >-------------------------------------------------------------//

  /**
   * This is the component set method.  It takes the pieces that make up a
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
   *
   * Note: An invalid plane value will default the class to the XZ_PLANE.
  **/
  protected void set(final byte _plane, final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                     final float _xScale, final float _yScale, final float _zScale,
                     final int[] _majAxisOffs, final int[] _minAxisOffs,
                     final Tuple3f _centroidPercentages)
  {
    if(_plane >= 0 && _plane <= 2)
      plane = _plane;
    else
      plane = XZ_PLANE;       // Nod to alarm for this default behavior.

    xMaxDim = _xMaxDim;
    yMaxDim = _yMaxDim;
    zMaxDim = _zMaxDim;

    xScale = _xScale;
    yScale = _yScale;
    zScale = _zScale;

    // Should these be copied?
    majorAxisOffsets = _majAxisOffs;
    minorAxisOffsets = _minAxisOffs;

    centroid = determineCentroid(_centroidPercentages);
  } // end set

  //----< set >-------------------------------------------------------------//

  /**
   * This is the component set method without the axisOffsets arrays.  It takes
   *  the pieces that make up a data matrix (aside from the actual matrix of
   *  data) and sets its internal member variables to them.  It will calculate
   *  the size of the offset arrays based on the size of the independent axis
   *  and initialize all the values in the arrays to zero.
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
   *
   * Note: An invalid plane value will default the class to the XZ_PLANE.
  **/
  protected void set(final byte _plane, final int _xMaxDim, final int _yMaxDim, final int _zMaxDim,
                     final float _xScale, final float _yScale, final float _zScale,
                     final Tuple3f _centroidPercentages)
  {
    if(_plane >= 0 && _plane <= 2)
      plane = _plane;
    else
      plane = XZ_PLANE;       // Nod to alarm for this default behavior.

    xMaxDim = _xMaxDim;
    yMaxDim = _yMaxDim;
    zMaxDim = _zMaxDim;

    xScale = _xScale;
    yScale = _yScale;
    zScale = _zScale;

    int offsDim;
    if(plane == XZ_PLANE) {
      offsDim = yMaxDim;
    } else if(plane == XY_PLANE) {
      offsDim = zMaxDim;
    } else { // YZ_PLANE
      offsDim = xMaxDim;
    } // end if

    majorAxisOffsets = new int[offsDim];
    minorAxisOffsets = new int[offsDim];
    for(int i = 0; i < offsDim; i++) {
      majorAxisOffsets[i] = 0;
      minorAxisOffsets[i] = 0;
    } // end for

    centroid = determineCentroid(_centroidPercentages);
  } // end set

  //----< determineCentroid >-----------------------------------------------//

  /**
   * This method is used to find the centroid of the data matrix.  This is used
   *  to tell where to center the geometry of the isosurface.
   * @param _centroidPercentages A Tuple3f containing the percentages from the
   *  lower-left-front of the data set that the should be considered the center
   *  of said data set.  For example, (0.0, 0.0, 0.0) will make the
   *  lower-left-front corner of the data the center of the data, and
   *  (1.0, 1.0, 1.0) will make the upper-right-back the center of the data.
   *  This center is typically the point which about rotations of the data
   *  will be applied.
   * @return A Tuple3f containing the centroid of the data matrix.
  **/
  private Tuple3f determineCentroid(final Tuple3f _centroidPercentages) {
    Tuple3f centroid;
    centroid = new Tuple3f(xMaxDim * _centroidPercentages.x,
                           yMaxDim * _centroidPercentages.y,
                           zMaxDim * _centroidPercentages.z);
    return(centroid);
  } // end determineCentroid

  //----< setMajorAxisOffset >----------------------------------------------//

  /**
   * This method allows setting of the offsets after the class has been created,
   *  for instances where it's not known in the beginning what the offset has
   *  to be.  This particular method is for the major axis offsets.
   * @param _idx The index in the independent axis that this offset corresponds
   *  to.
   * @param _offset The amount of offset in the major axis, at the specified
   *  index, that the data must be adjusted.
  **/
  public void setMajorAxisOffset(final int _idx, final int _offset) {
    if(_idx >= 0 && _idx < majorAxisOffsets.length) {
      majorAxisOffsets[_idx] = _offset;
    } // end if
  } // end setMajorAxisOffset

  //----< setMinorAxisOffset >----------------------------------------------//

  /**
   * This method allows setting of the offsets after the class has been created,
   *  for instances where it's not known in the beginning what the offset has
   *  to be.  This particular method is for the minor axis offsets.
   * @param _idx The index in the independent axis that this offset corresponds
   *  to.
   * @param _offset The amount of offset in the minor axis, at the specified
   *  index, that the data must be adjusted.
  **/
  public void setMinorAxisOffset(final int _idx, final int _offset) {
    if(_idx >= 0 && _idx < minorAxisOffsets.length) {
      minorAxisOffsets[_idx] = _offset;
    } // end if
  } // end setMinorAxisOffset

  //----< setPlaneData >----------------------------------------------------//

  /**
   * This method allows us to fill in the data per the dependent axes of the
   *  data matrix.  The dependent axes are specifed by the _plane parameter of 
   *  the constructor.
   * @param _idx The index in the independent axis of the matrix the plane of
   *  data corresponds to.
   * @param _dataPlane An object reference that contains a plane's worth of 3-D
   *  data to be inserted at the index specified.
  **/
  protected void setPlaneData(final int _idx, final Object _dataPlane)
  {
    if(matRef == null)
      System.out.println("Houston, we have a problem");
    Array.set(matRef, _idx, _dataPlane);  // throws
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
   * @param _dataPlane An object reference that contains a plane's worth of 3-D
   *  data to be inserted at the index specified.
   * @param _majAxisCenter The index value along the major axis of the data
   *  plane that is the center of this plane.
   * @param _minAxisCenter The index value along the minor axis of the data
   *  plane that is the center of this plane.
  **/
  protected void setPlaneData(final int _idx, final Object _dataPlane,
                              final int _majAxisCenter, final int _minAxisCenter)
  {
    setPlaneData(_idx, _dataPlane);   // throws
    calculatePlaneCenter(_idx, _majAxisCenter, _minAxisCenter);
  } // end setPlaneData

  //----< calculatePlaneCenter >--------------------------------------------//

  /**
   * This method will derive the offsets of the major and minor axes based on
   *  the center data passed in to the method.
   * @param _idx The index of the independent axis that the plane data
   *  corresponds to.
   * @param _majAxisCenter The value of the point 
  **/
  private void calculatePlaneCenter(final int _idx, final int _majAxisCenter,
                                    final int _minAxisCenter)
  {
    int majDim;
    int minDim;

    if(plane == XZ_PLANE) {
      majDim = xMaxDim;
      minDim = zMaxDim;
    } else if(plane == XY_PLANE) {
      majDim = xMaxDim;
      minDim = yMaxDim;
    } else { // YZ_PLANE
      majDim = yMaxDim;
      minDim = zMaxDim;
    } // end if

    majorAxisOffsets[_idx] = (majDim/2) - _majAxisCenter;
    minorAxisOffsets[_idx] = (minDim/2) - _minAxisCenter;

  } // end calculatePlaneCenter

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  //----< isPoint >---------------------------------------------------------//

  /**
   * This method will check to see if the point at location (x, y, z) is
   *  between the values of the specified lower and higher thresholds inclusive.
   * @param _x The index of the x-axis of the point in question.
   * @param _y The index of the y-axis of the point in question.
   * @param _z The index of the z-axis of the point in question.
   * @param _lowComparitor A byte that is of the value EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.  This is used when
   *  comparing the value at point (_x, _y, _z) with the low threshold value.
   * @param _lowThres a double that is the lower bounds of the data point
   *  in question.
   * @param _highComparitor A byte that is of the value EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.  This is used when
   *  comparing the value at point (_x, _y, _z) with the high threshold value.
   * @param _highThres a double that is the upper bounds of the data point
   *  in question.
   * @return A boolean value that is true when the point falls within the
   *  parameters of the low and high thresholds according to the comparitors
   *  passed in.
  **/
  public boolean isPoint(final int _x, final int _y, final int _z,
                         final byte _lowComparitor, final double _lowThres,
                         final byte _highComparitor, final double _highThres)
  {
    final double val = this.getPoint(_x, _y, _z);
    return(compareOp(val, _lowComparitor, _lowThres)
           && compareOp(val, _highComparitor, _highThres));
  } // end isPoint

  //----< isPoint >---------------------------------------------------------//

  /**
   * This method will check to see if the value at location (x, y, z)
   *   conforms to the comparitor operation.
   * @param _x The index of the x-axis of the point in question.
   * @param _y The index of the y-axis of the point in question.
   * @param _z The index of the z-axis of the point in question.
   * @param _comparitor A byte that is of the value EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.  This is used when
   *  comparing the value at point (_x, _y, _z) with the threshold value.
   * @param _threshold a double that is the bound of the data point
   *  in question.
   * @return A boolean value that is true when the point conforms to the
   *  parameters of the threshold according to the comparitor passed in.
  **/
  public boolean isPoint(final int _x, final int _y, final int _z,
                         final byte _comparitor, final double _threshold)
  {
    return(compareOp(getPoint(_x, _y, _z), _comparitor, _threshold));
  } // end isPoint

  //----< getPoint >--------------------------------------------------------//

  /**
   * This abstract method must be implemented by the sub classes to access the
   *  point in the underlying arrays.
   * @param _x The index of the x-axis of the point in question.
   * @param _y The index of the y-axis of the point in question.
   * @param _z The index of the z-axis of the point in question.
   * @return The double value of the point at coordinate (_x, _y, _z).
  **/
  public abstract double getPoint(int _x, int _y, int _z);

  //----< compareOp >-------------------------------------------------------//

  /**
   * This method applies the comparitor operator specifed by the user to the
   *  value with respect to the threshold.  This method is used by the isPoint
   *  methods to do their comparisons.
   * @param _val A double value (from the matrix) that is to be compared to the
   *  threshold to see if it meets the criteria for being a detection point.
   * @param _comparitor A byte that is of the value EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.  This is used when
   *  comparing the value at point (x, y, z) with the threshold value.
   * @param _threshold a double that is the bound of the data point
   *  in question.
  **/
  private boolean compareOp(final double _val, final byte _comparitor, final double _threshold) {
    switch (_comparitor) {
      case EQUAL :           return(_val == _threshold);
      case LESS_THAN :       return(_val < _threshold);
      case LESS_THAN_EQ :    return(_val <= _threshold);
      case GREATER_THAN :    return(_val > _threshold);
      case GREATER_THAN_EQ : return(_val >= _threshold);
    } // end switch
    return false;
  } // end compareOp

  //----< getPlane >--------------------------------------------------------//

  /**
   * This method will get the plane that the dependent axes are in (the axes
   *  which may contain variable-length data).
   * @return An integer representing one of three possible planes:
   *  XY_PLANE, XZ_PLANE, YZ_PLANE.
  **/
  public int getPlane() { return(plane); }

  //----< getXMaxDim >------------------------------------------------------//

  /**
   * This method will return the maximum size of the matrix on the x-axis.
   * @return An integer containing the maximum size that any array along the
   *  x-axis may be.
  **/
  public int getXMaxDim() { return(xMaxDim); }

  //----< getYMaxDim >------------------------------------------------------//

  /**
   * This method will return the maximum size of the matrix on the y-axis.
   * @return An integer containing the maximum size that any array along the
   *  y-axis may be.
  **/
  public int getYMaxDim() { return(yMaxDim); }

  //----< getZMaxDim >------------------------------------------------------//

  /**
   * This method will return the maximum size of the matrix on the z-axis.
   * @return An integer containing the maximum size that any array along the
   *  z-axis may be.
  **/
  public int getZMaxDim() { return(zMaxDim); }

  //----< getXScale >-------------------------------------------------------//

  /**
   * The scaling of the data set along the x-axis.
   * @return A float containing the scale.
  **/
  public float getXScale() { return(xScale); }

  //----< getYScale >-------------------------------------------------------//

  /**
   * The scaling of the data set along the y-axis.
   * @return A float containing the scale.
  **/
  public float getYScale() { return(yScale); }

  //----< getZScale >-------------------------------------------------------//

  /**
   * The scaling of the data set along the z-axis.
   * @return A float containing the scale.
  **/
  public float getZScale() { return(zScale); }

  //----< getMajorAxisOffsets >---------------------------------------------//

  /**
   * This method will return the offsets of each array along the major axis.
   *  The size of the array should be the same length as the maximum dimension
   *  of the major axis.
   * @return an integer array containing the values of how much each array on
   *  the major axis must be shifted on the major axis to properly align the
   *  data.
  **/
  public int[] getMajorAxisOffsets() { return(majorAxisOffsets); }

  //----< getMinorAxisOffsets >---------------------------------------------//

  /**
   * This method will return the offsets of each array along the minor axis.
   *  The size of the array should be the same length as the maximum dimension
   *  of the minor axis.
   * @return an integer array containing the values of how much each array on
   *  the minor axis must be shifted on the major axis to properly align the
   *  data.
  **/
  public int[] getMinorAxisOffsets() { return(minorAxisOffsets); }

  //----< getCentroid >-----------------------------------------------------//

  /**
   * This method returns the centroid of the data set.  This value is the point
   *  that has had the percentages applied to the data set, not the actual
   *  percentages that were used in the constructor to calculate said point.
   * @return A Tuple3f containing the the (x, y, z) coordinate that is the
   *  centroid of the data set.
  **/
  public Tuple3f getCentroid() { return(centroid); }

  //----< toString >--------------------------------------------------------//

  /**
   * This method reports information about the data set.  Currently, it will
   *  output the x, y, and z dimensions of the matrix.
   * @return A String containing the data about the data matrix.
  **/
  @Override
public String toString() {
    String str = "X dim: " + xMaxDim;
    str = str.concat("\nY dim: " + yMaxDim);
    str = str.concat("\nZ dim: " + zMaxDim);
    return(str);
  } // end toString

} // end class IsoSurfaceDataMatrix
