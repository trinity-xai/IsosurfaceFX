package IsosurfaceFX;

/**
 * This is the interface for classes that does the actual isosurface generation.
 * Their job is to generate vertices for the geometry, normals for the geometry,
 * and color vertices for the geometry.
 */
public interface IsoSurfaceGeneratorInterface {

  ////////////////////////////////////////////////////////////////////////////
  // Getters
  ////////////////////////////////////////////////////////////////////////////

  //----< getVertexArray >--------------------------------------------------//

  /**
   * This method should return the array of vertices that comprises the
   *  isosurface.
   * @return An array of Vector3d objects, one object for each vertex in the
   *  isosurface.
  **/
  public Vector3d[] getVertexArray();

  //----< getNormalArray >--------------------------------------------------//

  /**
   * This method should return the array of normals for the isosurface.  This
   *  array's indices correspond to the equivalent indices in the vertex array.
   * @return An array of Vector3f objects, one object for each vertex in the
   *  isosurface.
  **/
  public Vector3f[] getNormalArray();

  //----< getVertexColorArray >---------------------------------------------//

  /**
   * This method should return a two-dimensional array of floats that are the
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

  public float[][] getVertexColorArray();

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
  public float[][] getVertexMaterialArray();

  //----< getColorerType >--------------------------------------------------//

  /**
   * This method should return the type of coloring information that is in the
   *  vertex color array.
   * @return A byte code indicating the type of color was used.  Could be
   *  one of IsoSurfaceVertexColorer.UNLIT_COLOR,
   *  IsoSurfaceVertexColorer.LIT_COLOR, or 0 for no color information.
  **/
  public byte getColorerType();

  //----< isTranslucencyEnabled >-------------------------------------------//

  /**
   * This method will indicate whether of not the colorer used for this
   *  isosurface is making use of translucency.
   * @return A boolean containing whether or not translucency is enabled.
  **/
  public boolean isTranslucencyEnabled();

  //----< getPolygonCount >-------------------------------------------------//

  /**
   * This method should return the number of polygons in the isosurface.
   * @return An integer containing the polygon count.
  **/
  public int getPolygonCount();

} // end class IsoSurfaceGeneratorInterface
