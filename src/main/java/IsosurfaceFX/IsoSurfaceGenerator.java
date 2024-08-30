package IsosurfaceFX;

import java.util.Vector;

/**
 * This class does the actual isosurface generation.  It's job is to generate
 *  vertices for the geometry, normals for the geometry, and color vertices
 *  for the geometry.
**/
public class IsoSurfaceGenerator implements IsoConstInterface,
                                            IsoDataMatConstInterface,
                                            IsoSurfaceGeneratorInterface
{

  /** The data matrix that this class uses to build the surface. **/
  private final IsoSurfaceDataMatrix dataMatrix;

  /** A reference to the vertex colorer to use to calculate the colors. **/
  private IsoSurfaceVertexColorer vc;

  /** The default comparitor to use against the lower threshold. **/
  private byte lowOp = EQUAL;

  /** The default lower threshold that data points are compared to. **/
  private double lowThres = 1.0;

  /**
   * The default comparitor to use against the upper threshold. Also used
   *  as the only threshold comparitor when only one threshold comparitor is
   *  specified or if the two thresholds are equal.
  **/
  private byte highOp = EQUAL;

  /**
   * The default upper threshold that data points are compared to.  Also used
   *  as the only threshold when only one threshold is specified or if the two
   *  thresholds are equal.
  **/
  private double highThres = 1.0;

  /** The array of vertices for the geometry. **/
  private Vector3d[] vertexArr;

  /** The Vector of vertices for the geometry. **/
  private Vector<Vector3d> vertVect;

  /**
   * The array of normals for the geometry. Should be a normal for each of
   *  vertices in the vertexArr, i.e., their sizes should be the same.
  **/
  private Vector3f[] normalArr;

  /**
   * The array of isocube values from the table used to generate the isosurface.
   *  They are in (x,y,z) order.
  **/ 
  private int[][][] isoCubeArr;

  /** 
   * The array of color arrays for the vertices of the geometry.  There should
   *  be a one-to-one correspondence between vertColorArr's major dimension
   *  and the vertexArr.
  **/
  private float[][] vertColorArr;

  /** 
   * The array of material arrays for the vertices of the geometry.  There
   *  should be a one-to-one correspondence between vertMaterialArr's major
   *  dimension and the vertexArr.
  **/
  private float[][] vertMaterialArr;

  /**
   * The number of triangles it took to construct the surface from the input
   *  data.
  **/
  private int triCnt = 0;

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor takes the IsoSurfaceDataMatrix as its only parameter.
   *  The class instance will assume that no coloring of vertices is required.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   *  data source to generate the isosurface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix) {
    dataMatrix = _dataMatrix;
    calculateSurface();
  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor takes IsoSurfaceDataMatrix and IsoSurfaceVertexColorer
   *  instances as its parameters.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   *  data source to generate the isosurface.
   * @param _vc A VertexColorer subclass instance to use to derive the colors
   *  of the vertices for the surface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix,
                             final IsoSurfaceVertexColorer _vc)
  {
    dataMatrix = _dataMatrix;
    vc = _vc;
    calculateSurface();
  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor allows the user to specify a threshold with which to
   *  compare data points in the matrix to see if a point should be part of the
   *  isosurface.
   * The class instance will assume that no coloring of vertices is required.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   *  data source to generate the isosurface.
   * @param _op A byte comparitor that compares matrix values against the
   *  specified threshold.  Can be of the values EQUAL, LESS_THAN, LESS_THAN_EQ,
   *  GREATER_THAN, or GREATER_THAN_EQ.
   * @param _threshold The double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix,
                             final byte _op, final double _threshold)
  {
    dataMatrix = _dataMatrix;
    lowOp = highOp = _op;
    lowThres = highThres = _threshold;
    calculateSurface();
  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor takes IsoSurfaceDataMatrix and IsoSurfaceVertexColorer
   *  instances as its parameters.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   *  data source to generate the isosurface.
   * @param _vc A IsoSurfaceVertexColorer subclass instance to use to derive
   *  the colors of the vertices for the surface.
   * @param _op A byte comparitor that compares matrix values against the
   *  specified threshold.  Can be of the values EQUAL, LESS_THAN, LESS_THAN_EQ,
   *  GREATER_THAN, or GREATER_THAN_EQ.
   * @param _threshold The double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix,
                             final IsoSurfaceVertexColorer _vc,
                             final byte _op, final double _threshold)
  {
    dataMatrix = _dataMatrix;
    vc = _vc;
    lowOp = highOp = _op;
    lowThres = highThres = _threshold;
    calculateSurface();
  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor allows the user to specify the lower and upper thresholds
   *  with which to compare data points in the matrix to see if a point should
   *  be part of the isosurface.
   * It should be noted that there is nothing keeping someone from actually
   *  making the lower threshold greater than the higher threshold and using
   *  the appropriate comparitors to still get the result of being within the
   *  range of the thresholds.  There is also no checking on the comparitors
   *  to make sure the ranges intersect.  If done incorrectly, it's possible to
   *  create a blank isosurface.  Just be aware of these dangers, it may save
   *  you considerable debugging time.
   * The class instance will assume that no coloring of vertices is required.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   *  data source to generate the isosurface.
   * @param _lowOp A byte comparitor that compares matrix values against the
   *  specified lower threshold.  Can be of the values EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.
   * @param _lowThres A double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
   * @param _highOp A byte comparitor that compares matrix values against the
   *  specified higher threshold.  Can be of the values EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.
   * @param _highThres A double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix, 
                             final byte _lowOp, final double _lowThres,
                             final byte _highOp, final double _highThres)
  {
    dataMatrix = _dataMatrix;
    lowOp = _lowOp;
    lowThres = _lowThres;
    highOp = _highOp;
    highThres = _highThres;
    calculateSurface();
  } // end constructor

  //----< constructor >-----------------------------------------------------//

  /**
   * This constructor allows the user to specify the lower and upper thresholds
   *  with which to compare data points in the matrix to see if a point should
   *  be part of the isosurface.
   * It should be noted that there is nothing keeping someone from actually
   *  making the lower threshold greater than the higher threshold and using
   *  the appropriate comparitors to still get the result of being within the
   *  range of the thresholds.  There is also no checking on the comparitors
   *  to make sure the ranges intersect.  If done incorrectly, it's possible to
   *  create a blank isosurface.  Just be aware of these dangers, it may save
   *  you considerable debugging time.
   * @param _dataMatrix An IsoSurfaceDataMatrix subclass instance to use as the
   * @param _vc A IsoSurfaceVertexColorer subclass instance to use to derive
   *  the colors of the vertices for the surface.
   * @param _lowOp A byte comparitor that compares matrix values against the
   *  specified lower threshold.  Can be of the values EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.
   * @param _lowThres A double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
   * @param _highOp A byte comparitor that compares matrix values against the
   *  specified higher threshold.  Can be of the values EQUAL, LESS_THAN,
   *  LESS_THAN_EQ, GREATER_THAN, or GREATER_THAN_EQ.
   * @param _highThres A double value that the matrix values are compared to
   *  to determine if they are considered part of the isosurface.
  **/
  public IsoSurfaceGenerator(final IsoSurfaceDataMatrix _dataMatrix,
                             final IsoSurfaceVertexColorer _vc,
                             final byte _lowOp, final double _lowThres,
                             final byte _highOp, final double _highThres)
  {
    dataMatrix = _dataMatrix;
    vc = _vc;
    lowOp = _lowOp;
    lowThres = _lowThres;
    highOp = _highOp;
    highThres = _highThres;
    calculateSurface();
  } // end constructor

  //----< calcIsoCube >-----------------------------------------------------//

  /**
   * This internal method will generate an isocube configuration bitstring
   *  (stored in an int) that is used to access the lookup tables.
   * @param _x The index of the point along the data matrix's x-axis.
   * @param _y The index of the point along the data matrix's y-axis.
   * @param _z The index of the point along the data matrix's z-axis.
  **/
  private int calcIsoCube(final int _x, final int _y, final int _z)
  {
    int config = 0;

    if(lowOp == highOp) {
      if(dataMatrix.isPoint(_x, _y, _z, highOp, highThres)) {
        config = config | VERT0;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y, _z, highOp, highThres)) {
        config = config | VERT1;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y+1, _z, highOp, highThres)) {
        config = config | VERT2;
      } // end if
      if(dataMatrix.isPoint(_x, _y+1, _z, highOp, highThres)) {
        config = config | VERT3;
      } // end if
      if(dataMatrix.isPoint(_x, _y, _z+1, highOp, highThres)) {
        config = config | VERT4;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y, _z+1, highOp, highThres)) {
        config = config | VERT5;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y+1, _z+1, highOp, highThres)) {
        config = config | VERT6;
      } // end if
      if(dataMatrix.isPoint(_x, _y+1, _z+1, highOp, highThres)) {
        config = config | VERT7;
      } // end if
    } else {
      if(dataMatrix.isPoint(_x, _y, _z,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT0;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y, _z,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT1;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y+1, _z,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT2;
      } // end if
      if(dataMatrix.isPoint(_x, _y+1, _z,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT3;
      } // end if
      if(dataMatrix.isPoint(_x, _y, _z+1,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT4;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y, _z+1,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT5;
      } // end if
      if(dataMatrix.isPoint(_x+1, _y+1, _z+1,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT6;
      } // end if
      if(dataMatrix.isPoint(_x, _y+1, _z+1,
                            lowOp, lowThres, highOp, highThres)) {
        config = config | VERT7;
      } // end if
    } // end if

    return(config);

  } // end calcIsoCube

  //----< calculateSurface >------------------------------------------------//

  /**
   * This method is an internal method that will populate the vertex, normal,
   *  and optionally, the vertex color arrays.  This method makes calls on the
   *  dataMatrix instance and the IsoGeomTable arrays.
  **/
  protected void calculateSurface() {

    vertVect = new Vector<Vector3d>();
    final Vector<Vector3f> normVect = new Vector<Vector3f>();

    isoCubeArr = new int[dataMatrix.getXMaxDim()-1]
                        [dataMatrix.getYMaxDim()-1]
                        [dataMatrix.getZMaxDim()-1];


    final Tuple3f cntrd = dataMatrix.getCentroid();

    for(int x = 0; x < dataMatrix.getXMaxDim()-1; x++) {
      for(int y = 0; y < dataMatrix.getYMaxDim()-1; y++) {
        for(int z = 0; z < dataMatrix.getZMaxDim()-1; z++) {

          isoCubeArr[x][y][z] = calcIsoCube(x, y, z);

          final float[] vertArr = IsoGeomTable.geoms[isoCubeArr[x][y][z]];

          if(vertArr != null) {
            int len = 0;
            while(len < vertArr.length) {
              final Vector3d vertex = applyScales(vertArr[len]   + x - cntrd.x,
                                            vertArr[len+1] + y - cntrd.y,
                                            vertArr[len+2] - z + cntrd.z);
              vertVect.add(vertex);
                    // 1 for each vert
              normVect.add(IsoNormTable.norms[isoCubeArr[x][y][z]][len/12]);

              len += 4;
              if(len > 0 && (len % 12 == 0))
                triCnt += 1;
            } // end while
          } // end if

        } // end for
      } // end for
    } // end for

    // Vertices of the geometry
    vertexArr = new Vector3d[vertVect.size()];
    vertVect.toArray(vertexArr);
    // Normals for the aforementioned geometry
    normalArr = new Vector3f[normVect.size()];
    normVect.toArray(normalArr);

    final Vector<float[]> colVect = new Vector<float[]>();
    final Vector<float[]> matVect = new Vector<float[]>();
    calcColorArrays(vertVect, colVect, matVect, vc);
    //calcColorArrays(colVect, matVect, vc);

    // Colors if any
    if(colVect.size() > 0) {
      vertColorArr = new float[colVect.size()][];
      colVect.toArray(vertColorArr);
    } // end if
    // Materials if any
    if(matVect.size() > 0) {
      vertMaterialArr = new float[matVect.size()][];
      matVect.toArray(vertMaterialArr);
    } // end if

  } // end calculateSurface

  //----< applyScales >-----------------------------------------------------//

  /**
   * This method is used by calculateSurface to scale the points in all three
   *  axes.
   * @param _x The index of the point along the data matrix's x-axis.
   * @param _y The index of the point along the data matrix's y-axis.
   * @param _z The index of the point along the data matrix's z-axis.
  **/
  private Vector3d applyScales(float _x, float _y, float _z) {
    _x *= dataMatrix.getXScale();
    _y *= dataMatrix.getYScale();
    _z *= dataMatrix.getZScale();
    return(new Vector3d(_x, _y, _z));
  } // end applyScales

  //----< calcColorArrays >-------------------------------------------------//

  /**
   * This method is the external version of the calcColorArrays method that is
   *  used to recalculate the colors of an already created isosurface.
   * NOTE: must be called AFTER calculateSurface has had a chance to populate
   *  the isoCubeArr.  Since calculateSurface is called via the constructor,
   *  subsequent user calls shouldn't be a problem.
   * @param _colVect A vector of Color4f's, one for each vertex of the geometry,
   *  used when rendering the appearance in an unlit manner.  The contents of
   *  this Vector will be cleared and new contents will be added.
   * @param _matVect A vector of Material3D's, one for each vertex of the
   *  geometry, used when rendering the appearance in a lit manner.  The
   *  contents of this Vector will be cleared and new contents will be added.
   * @param _vc The IsoSurfaceVertexColorer instance to use to calculate the
   *  colors of the vertices along the isosurface.
  **/
  public void calcColorArrays(final Vector<float[]> _colVect,
                              final Vector<float[]> _matVect,
                              final IsoSurfaceVertexColorer _vc)
  {
    calcColorArrays(vertVect, _colVect, _matVect, _vc);
  } // end calcColorArrays

  //----< calcColorArrays >-------------------------------------------------//

  /**
   * This method will populate the Vector parameters with colorer
   *  information if the colorer is present.
   * NOTE: must be called AFTER calculateSurface has had a chance to populate
   *  the isoCubeArr.  Since calculateSurface is called via the constructor,
   *  subsequent user calls shouldn't be a problem.
   * @param _vertVect A Vector containing the Vector3d's that make up the 
   *  vertices of the isosurface.
   * @param _colVect A vector of Color4f's, one for each vertex of the geometry,
   *  used when rendering the appearance in an unlit manner.  The contents of
   *  this Vector will be cleared and new contents will be added.
   * @param _matVect A vector of Material3D's, one for each vertex of the
   *  geometry, used when rendering the appearance in a lit manner.  The
   *  contents of this Vector will be cleared and new contents will be added.
   * @param _vc An IsoSurfaceVertexColorer that is used to calculate what color
   *  (possible both lit and unlit) each vertex of the geometry should be. 
  **/
  public void calcColorArrays(final Vector<Vector3d> _vertVect,
                              final Vector<float[]> _colVect,
                              final Vector<float[]> _matVect,
                              final IsoSurfaceVertexColorer _vc)
  {
    byte colorerType = IsoSurfaceVertexColorer.NONE;
    final double[] ptVals = new double[8];

    if(_vc != null) {
      colorerType = _vc.getColorerType();
      if(colorerType == IsoSurfaceVertexColorer.UNLIT ||
         colorerType == IsoSurfaceVertexColorer.BOTH)
        _colVect.clear();
      if(colorerType == IsoSurfaceVertexColorer.LIT ||
         colorerType == IsoSurfaceVertexColorer.BOTH)
        _matVect.clear();

      int vertIdx = 0;
      for(int x = 0; x < dataMatrix.getXMaxDim()-1; x++) {
        for(int y = 0; y < dataMatrix.getYMaxDim()-1; y++) {
          for(int z = 0; z < dataMatrix.getZMaxDim()-1; z++) {

            final float[] vertArr = IsoGeomTable.geoms[isoCubeArr[x][y][z]];

            if(vertArr != null) {
              int len = 0;
              getPointValues(ptVals, dataMatrix, x, y, z);
              while(len < vertArr.length) {
                //Vector3d vertex = applyScales(vertArr[len]   + x - cntrd.x,
                                              //vertArr[len+1] + y - cntrd.y,
                                              //vertArr[len+2] - z + cntrd.z);
                final Vector3d vertex = _vertVect.get(vertIdx);
                if(_colVect != null)
                  _colVect.add(_vc.calcUnlitColor(vertex, x, y, z,
                                                  ptVals[(int)vertArr[len+3]]));
                if(_matVect != null)
                  _matVect.add(_vc.calcLitColor(vertex, x, y, z,
                                                ptVals[(int)vertArr[len+3]]));

                len += 4;
                vertIdx++;
              } // end while
            } // end if

          } // end for
        } // end for
      } // end for

    } // end if

  } // end calcColorArrays

  //----< getPointValues >--------------------------------------------------//

  /**
   * This method fills the _ptVals array with the values of the isocube.
   * Used by calcColorArrays method. 
   * @param _ptVals A double array that will contain all of the values of the
   *  eight isocube points.  This parameter is modified by the method, so any
   *  existing data in the array will be lost.
   * @param _dm The IsoSurfaceDataMatrix that the values for the points are
   *  pulled from.
   * @param _x The x index of the isocube whose point values are sought.
   * @param _y The y index of the isocube whose point values are sought.
   * @param _z The z index of the isocube whose point values are sought.
  **/
  private void getPointValues(final double[] _ptVals, final IsoSurfaceDataMatrix _dm,
                              final int _x, final int _y, final int _z)
  {
    _ptVals[0] = _dm.getPoint(_x,   _y,   _z);
    _ptVals[1] = _dm.getPoint(_x+1, _y,   _z);
    _ptVals[2] = _dm.getPoint(_x+1, _y+1, _z);
    _ptVals[3] = _dm.getPoint(_x,   _y+1, _z);
    _ptVals[4] = _dm.getPoint(_x,   _y,   _z+1);
    _ptVals[5] = _dm.getPoint(_x+1, _y,   _z+1);
    _ptVals[6] = _dm.getPoint(_x+1, _y+1, _z+1);
    _ptVals[7] = _dm.getPoint(_x,   _y+1, _z+1);

  } // end getPointValues

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  //----< getPolygonCount >-------------------------------------------------//

  /**
   * This method will return the number of polygons in the isosurface.
   * @return An integer containing the polygon count.
  **/
  @Override
public int getPolygonCount() {
    return(triCnt);
  } // end getPolygonCount

  //----< getDataMatrix >---------------------------------------------------//

  /**
   * This method allows access to the data matrix that the generator used to
   *  calculate the isosurface.
   * @return An IsoSurfaceDataMatrix subclass instance that was used in the
   *  generation process.
  **/
  public IsoSurfaceDataMatrix getDataMatrix() {
    return(dataMatrix);
  } // end getDataMatrix

  //----< getVertexArray >--------------------------------------------------//

  /**
   * This method will return the array of vertices that comprises the
   *  isosurface.
   * @return An array of Vector3d objects, one object for each vertex in the
   *  isosurface.
  **/
  @Override
public Vector3d[] getVertexArray() {
    return(vertexArr);
  } // end getVertexArray

  //----< getNormalArray >--------------------------------------------------//

  /**
   * This method will return the array of normals for the isosurface.  This
   *  array's indices correspond to the equivalent indices in the vertex array.
   * @return An array of Vector3f objects, one object for each vertex in the
   *  isosurface.
  **/
  @Override
public Vector3f[] getNormalArray() {
    return(normalArr);
  } // end getNormalArray

  //----< getColorerType >--------------------------------------------------//

  /**
   * This method will return the type of colorer the IsoSurfaceGenerator used
   *  to calculate the per-vertex coloring.
   * @return The byte containing the code of the colorer type.  Can be one of
   *  IsoSurfaceVertexColorer.NONE, IsoSurfaceVertexColorer.LIT,
   *  IsoSurfaceVertexColorer.UNLIT, or IsoSurfaceVertexColorer.BOTH. 
  **/
  @Override
public byte getColorerType() {
    if(vc == null)
      return(IsoSurfaceVertexColorer.NONE);
    else
      return(vc.getColorerType());
  } // end getColorerType

  //----< getVertexColorArray >---------------------------------------------//

  /**
   * This method will return a two-dimensional array of floats that are the
   *  unlit color information for each vertex of the isosurface.
   * The major index (first one) cooresponds to the index of the vertex in the
   *  vertex array.  The minor index (second one) cooresponds to a component of
   *  the color information for that vertex.
   * The order of the floats should be red, green, blue, and translucency.
   *  However, if a VertexColorer is written to output these in a
   *  different order, then that order should be considered when making
   *  calls to the graphics engihe.
   * @return A two-dimensional float array containing unlit color attributes
   *  for each vertex of the surface.
  **/
  @Override
public float[][] getVertexColorArray() {
    return(vertColorArr);
  } // end getVertexColorArray

  //----< getVertexMaterialArray >------------------------------------------//

  /**
   * This method will return a two-dimensional array of floats that are the
   *  lit color information for each vertex of the isosurface.
   * The major index (first one) cooresponds to the index of the vertex in the
   *  vertex array.  The minor index (second one) cooresponds to a component of
   *  the material information for that vertex.
   * The order of the floats should be red, green, blue, and translucency for
   *  each of the material colors: specular, ambient, diffuse, and (optionally
   *  emission), in that order.  The last float is the shininess attribute.
   * So the order should be specular red, specular green, specular blue,
   *  specular translucency, ambient red, ambient green, etc.
   * However, if a VertexColorer is written to output these in a different
   *  order, then that order should be considered when making calls to the
   *  graphics engihe.
   * @return A two-dimensional float array containing lit color attributes
   *  for each vertex of the surface.
  **/
  @Override
public float[][] getVertexMaterialArray() {
    return(vertMaterialArr);
  } // end getVertexMaterialArray

  //----< isTranslucencyEnabled >-------------------------------------------//

  /**
   * This method will indicate whether of not the colorer used for this
   *  isosurface is making use of translucency.
   * @return A boolean containing whether or not translucency is enabled.
  **/
  @Override
public boolean isTranslucencyEnabled() {
    if(vc != null)
      return(vc.isTranslucencyEnabled());
    else
      return(false);
  } // end isTranslucencyEnabled

} // end class IsoSurfaceGenerator
