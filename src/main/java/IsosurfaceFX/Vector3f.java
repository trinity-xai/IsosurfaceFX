package IsosurfaceFX;

/**
 * A floating precision vector of 3 elements.
 * <p>
 * Revision 1.0 06.20.2002 unnamed date: June-July 2002
 *
 */
public class Vector3f extends Tuple3f {

   // ----< Constructor
   // >-----------------------------------------------------//

   /**
    * Constructs and initializes a vector with each element in the vector equal
    * to zero.
    */
   public Vector3f() {
      /*
       * Initializes to 0 vector.
       */
   } // end constructor

   // ----< Constructor
   // >-----------------------------------------------------//

   /**
    * Constructs and initializes a vector with x, y, and z set by the parameter
    * floats.
    *
    * @param _x
    *           The float value to set this Vector3f's x component to.
    * @param _y
    *           The float value to set this Vector3f's y component to.
    * @param _z
    *           The float value to set this Vector3f's z component to.
    */
   public Vector3f(final float _x, final float _y, final float _z) {
      super(_x, _y, _z);
   } // end constructor

   // ----< Constructor
   // >-----------------------------------------------------//

   /**
    * Constructs and initializes a Vector3f, with x, y, and z set in that
    * order, from the parameter float array.
    *
    * @param _floats
    *           A float array that must contain at least three values. The
    *           first will be the value the x component is set to, the second
    *           will be the value the y component gets set to, and the third
    *           will be the value the z component gets set to.
    * @throws NullPointerException
    *            if the parameter float array is null.
    * @throws IllegalArgumentException
    *            if the parameter array's length is less
    *            than size three.
    */
   public Vector3f(final float[] _floats) throws NullPointerException, IllegalArgumentException {
      super(_floats);
   } // end constructor

   // ----< Constructor
   // >-----------------------------------------------------//

   /**
    * Constructs and initializes a Vector3f with x, y, and z set equal to the
    * respective components in the parameter Tuple3f.
    *
    * @param _tuple
    *           The Tuple3f instance whose values are to be copied into
    *           this Vector3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    */
   public Vector3f(final Tuple3f _tuple) throws NullPointerException {
      super(_tuple);
   } // end constructor

   // ----< Constructor
   // >-----------------------------------------------------//

   /**
    * Constructs and initializes a Vector3f with x, y, and z set equal to the
    * respective components in the parameter Tuple3d. The components are cast
    * to floats before assignment.
    *
    * @param _tuple
    *           The Tuple3d instance whose values are to be copied into
    *           this Vector3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    */
   public Vector3f(final Tuple3d _tuple) throws NullPointerException {
      super(_tuple);
   } // end constructor

   // // Mathematical Operations
   // ///////////////////////////////////////////////

   // ----< angle
   // >-----------------------------------------------------------//

   /**
    * This method calculates the angle (in radians) between this vector and the
    * parameter vector.
    *
    * @param _vector
    *           The vector to find the angle with.
    * @return a double value of the angle between this vector, and the
    *         parameter _vector in three-space.
    * @throws NullPointerException
    *            if the parameter vector is null.
    */
   public float angle(final Vector3f _vector) throws NullPointerException {
      // acos near 0 and PI acos is bad, |atan2(sin,cos)| is better.

      final double x1 = y * _vector.z - z * _vector.y;
      final double y1 = z * _vector.x - x * _vector.z;
      final double z1 = x * _vector.y - y * _vector.x;

      final double z2 = Math.sqrt((x1 * x1) + (y1 * y1) + (z1 * z1));
      final float result = (float) Math.atan2(z2, dot(_vector));
      return (result < 0 ? -result : result);
   } // end angle

   // ----< dot
   // >-------------------------------------------------------------//

   /**
    * This method will calculate the dot product of this vector and the
    * parameter vector. The dot product is also known as the scalar product.
    *
    * @param _vector
    *           The vector to dot product with this vector.
    * @return a float value of the dot product between this Vector and the
    *         parameter _vector.
    * @throws NullPointerException
    *            if the parameter vector is null.
    */
   public float dot(final Vector3f _vector) throws NullPointerException {
      return ((x * _vector.x) + (y * _vector.y) + (z * _vector.z));
   } // end dot

   // ----< length
   // >----------------------------------------------------------//

   /**
    * This method will calculate the length of this vector.
    *
    * @return a float value which is the length/magnitude of this vector.
    */
   public float length() {
      return ((float) Math.sqrt(lengthSquared()));
   } // end length

   // ----< lengthSquared
   // >---------------------------------------------------//

   /**
    * This method will calculate the length squared of this vector. Primarily
    * used by the length and magnitude methods to calculate the length of the
    * vector.
    *
    * @return a float containing the length of this vector squared.
    */
   public float lengthSquared() {
      return ((x * x) + (y * y) + (z * z));
   } // end lengthSquared

   // ----< magnitude
   // >-------------------------------------------------------//

   /**
    * This method will calculate the magnitude of this vector.
    *
    * @return a float value which is the length/magnitude of this vector.
    */
   public float magnitude() {
      return (length());
   } // end magnitude

   // ----< normalize
   // >-------------------------------------------------------//

   /**
    * Computes the normal of this vector, storing the results in this vector.
    * This wipes out the original values contained in the vector's x, y, and z
    * member variables.
    */
   public void normalize() {
      final float mag = magnitude();
      if (mag != 0) {
         x /= mag;
         y /= mag;
         z /= mag;
      } // end if
   } // end normalize

   // ----< cross
   // >-----------------------------------------------------------//

   /**
    * This method with calculate the cross product of the two parameter vectors and store the results in this vector.
    *
    * <p>
    * i.e. this = _Vector1 x _vector2
    *
    * @param _vector1
    *           The first of the two vectors whose cross product is sought.
    * @param _vector2
    *           The second of the two vectors whose cross product is sought.
    * @throws NullPointerException
    *            if either parameter vector is null.
    **/
   public void cross(final Vector3f _vector1, final Vector3f _vector2) throws NullPointerException {
      final float newx = _vector1.y * _vector2.z - _vector1.z * _vector2.y;
      final float newy = _vector1.z * _vector2.x - _vector1.x * _vector2.z;
      final float newz = _vector1.x * _vector2.y - _vector1.y * _vector2.x;

      x = newx;
      y = newy;
      z = newz;
   } // end cross

   /**
    * This method with calculate the cross product of this vector with the parameter vector and store the results in this vector.
    *
    * <p>
    * i.e. this = this x _vector1
    *
    * @param _vector1
    *           The first of the two vectors whose cross product is sought.
    * @throws NullPointerException
    *            if the parameter vector is null.
    **/
   public void cross(final Vector3f _vector1) throws NullPointerException {
      final float newx = y * _vector1.z - z * _vector1.y;
      final float newy = z * _vector1.x - x * _vector1.z;
      final float newz = x * _vector1.y - y * _vector1.x;

      x = newx;
      y = newy;
      z = newz;
   } // end cross

   /**
    * Converts (by widening cast of components) this vector to a Vector3d.
    *
    * @return A new double-precision (Vector3d) represention of this vector.
    */
   public Vector3d asVector3d() {
      return new Vector3d(x, y, z);
   }

   /**
    * Converts the specified array of Vector3f to a float[]. The resulting float[] will have capacity equal
    * to 3 * the number of vertices passed in.
    *
    * @param verts
    *           A (var-args) array of non-null Vector3f instances.
    * @return A float[] containing the vertex data.
    */
   public static float[] toFloatArray(final Vector3f... verts) {
      final float[] buff = new float[verts.length * 3];
      for (int i = 0; i < verts.length; ++i) {
         buff[i * 3] = verts[i].x;
         buff[i * 3 + 1] = verts[i].y;
         buff[i * 3 + 2] = verts[i].z;
      }
      return buff;
   }

} // end class Vector3f
