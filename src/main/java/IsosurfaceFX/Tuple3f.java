package IsosurfaceFX;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * A floating precision Tuple of 3 elements.
 * <p>
 * Revision 1.0 06.27.2002 unnamed date: June-July 2002
 **/
public class Tuple3f implements Tuple3 {

   // ----< Public member variables >-----------------------------------------//

   /** The x element. **/
   public float x;

   /** The y element. **/
   public float y;

   /** The z element. **/
   public float z;

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3f with x, y, and z set to zero.
    **/
   public Tuple3f() {
      /*
       * Init to 0's
       */
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3f with x, y, and z set by the parameter
    * floats.
    *
    * @param _x
    *           The float value to set this Tuple3f's x component to.
    * @param _y
    *           The float value to set this Tuple3f's y component to.
    * @param _z
    *           The float value to set this Tuple3f's z component to.
    **/
   public Tuple3f(final float _x, final float _y, final float _z) {
      this.x = _x;
      this.y = _y;
      this.z = _z;
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3f, with x, y, and z set in that order,
    * from the parameter float array.
    *
    * @param _floats
    *           A float array that must contain at least three values. The
    *           first will be the value the x component is set to, the second will be
    *           the value the y component gets set to, and the third will be the value the
    *           z component gets set to.
    * @throws NullPointerException
    *            if the parameter float array is null.
    * @throws IllegalArgumentException
    *            if the parameter array's length is less
    *            than size three.
    **/
   public Tuple3f(final float[] _floats) throws NullPointerException, IllegalArgumentException {
      if (_floats.length >= 3) {
         this.x = _floats[0];
         this.y = _floats[1];
         this.z = _floats[2];
      } else {
         throw new IllegalArgumentException();
      } // end if

   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3f with x, y, and z set equal to the
    * respective components in the parameter Tuple3f.
    *
    * @param _tuple
    *           The Tuple3f instance whose values are to be copied into this
    *           Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public Tuple3f(final Tuple3f _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3f with x, y, and z set equal to the
    * respective components in the parameter Tuple3d. The components are cast
    * to floats before assignment.
    *
    * @param _tuple
    *           The Tuple3d instance whose values are to be copied into this
    *           Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public Tuple3f(final Tuple3d _tuple) throws NullPointerException {
      this.x = (float) _tuple.x;
      this.y = (float) _tuple.y;
      this.z = (float) _tuple.z;
   } // end constructor

   // // Setters ///////////////////////////////////////////////////////////////

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3f to the parameter
    * floats.
    *
    * @param _x
    *           The value to set the x component of this Tuple3f to.
    * @param _y
    *           The value to set the y component of this Tuple3f to.
    * @param _z
    *           The value to set the z component of this Tuple3f to.
    **/
   public void set(final float _x, final float _y, final float _z) {
      this.x = _x;
      this.y = _y;
      this.z = _z;
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x and y values of this Tuple3f from the parameter
    * float array in x, y, z order.
    *
    * @param _floats
    *           A float array that must contain at least three values. The
    *           first will be the value the x component is set to, the second will be
    *           the value the y component gets set to, and the third will be the value the
    *           z component gets set to.
    * @throws NullPointerException
    *            if the parameter float array is null.
    * @throws IllegalArgumentException
    *            if the parameter array's length is less
    *            than size three.
    **/
   public void set(final float[] _floats) throws NullPointerException, IllegalArgumentException {
      if (_floats.length >= 3) {
         this.x = _floats[0];
         this.y = _floats[1];
         this.z = _floats[2];
      } else {
         throw new IllegalArgumentException();
      } // end if
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3f to the same values
    * as the parameter Tuple3f.
    *
    * @param _tuple
    *           The Tuple3f instance whose values are to be copied into this
    *           Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void set(final Tuple3f _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3f equal to the
    * respective components in the parameter Tuple3d. The components are cast
    * to floats before assignment.
    *
    * @param _tuple
    *           The Tuple3d instance whose values are to be copied into this
    *           Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void set(final Tuple3d _tuple) throws NullPointerException {
      this.x = (float) _tuple.x;
      this.y = (float) _tuple.y;
      this.z = (float) _tuple.z;
   } // end set

   // // Getters ///////////////////////////////////////////////////////////////

   // ----< get >-------------------------------------------------------------//

   /**
    * This is a convenience accessor method that will create a new float array
    * of length three containing the values of the x, y, and z member variables.
    *
    * @return a float array containing the x, y, and z from this Tuple3f, in that
    *         order.
    **/
   public float[] get() {
      return (new float[] { this.x, this.y, this.z });
   } // end get

   // ----< getFloats >-------------------------------------------------------//

   /**
    * This is a convenience accessor method that will create a new float array
    * of length three containing the values of the x, y, and z member variables.
    *
    * @return a float array containing the x, y, and z from this Tuple3f, in that
    *         order.
    **/
   public float[] getFloats() {
      return (new float[] { this.x, this.y, this.z });
   } // end getFloats

   // ----< get >-------------------------------------------------------------//

   /**
    * This method will populate the parameter float array with the components
    * of this Tuple3f.
    *
    * @param _floats
    *           A float array containing the x, y, and z from this Tuple3f,
    *           in that order.
    * @throws NullPointerException
    *            if the _floats parameter is null.
    * @throws IllegalArgumentException
    *            if the _floats array parameter has a
    *            length less than three.
    **/
   public void get(final float[] _floats) throws NullPointerException, IllegalArgumentException {
      if (_floats.length >= 3) {
         _floats[0] = this.x;
         _floats[1] = this.y;
         _floats[2] = this.z;
      } else {
         throw new IllegalArgumentException("Float array must be at least of length three");
      } // end if
   } // end get

   // ----< get >-------------------------------------------------------------//

   /**
    * This method will populate the parameter Tuple3f with the corresponding
    * components of this Tuple3f.
    *
    * @param _tuple
    *           A Tuple3f instance to copy this Tuple3f's components into.
    * @throws NullPointerException
    *            if the _tuple parameter is null.
    **/
   public final void get(final Tuple3f _tuple) throws NullPointerException {
      _tuple.x = this.x;
      _tuple.y = this.y;
      _tuple.z = this.z;
   } // end get

   // // Mathematical Operations ///////////////////////////////////////////////

   /**
    * Returns the Euclidian distance from the point represented by this Tuple to the specified location.
    *
    * @param x
    *           The x component of the point to find the distance to.
    * @param y
    *           The y component of the point to find the distance to.
    * @param z
    *           The z component of the point to find the distance to.
    * @return The (positive) distance between this tuple and the specified triple.
    */
   public final float distance(final float x, final float y, final float z) {
      final float xDist = this.x - x;
      final float yDist = this.y - y;
      final float zDist = this.z - z;
      return (float)Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
   }
   
   /**
    * Returns the Euclidian distance from the point represented by this Tuple to the specified location.
    * 
    * @param t
    *           The Tuple to find the distance to
    * @return The (positive) distance between this tuple and the specified tuple.
    */
   public final float distance(final Tuple3f t) {
      return distance(t.x, t.y, t.z);
   }

   /**
    * Returns the Euclidian distance from the point represented by this Tuple to the specified location.
    *
    * @param x
    *           The x component of the point to find the distance to.
    * @param y
    *           The y component of the point to find the distance to.
    * @param z
    *           The z component of the point to find the distance to.
    * @return The (positive) distance between this tuple and the specified triple.
    */
   public final double distanceSquared(final double x, final double y, final double z) {
      final double xDist = this.x - x;
      final double yDist = this.y - y;
      final double zDist = this.z - z;
      return xDist * xDist + yDist * yDist + zDist * zDist;
   }

   /**
    * Returns the Euclidian distance from the point represented by this Tuple to the specified location.
    * 
    * @param t
    *           The Tuple to find the distance to
    * @return The (positive) distance between this tuple and the specified tuple.
    */
   public final double distanceSquared(final Tuple3d t) {
      final double xDist = this.x - x;
      final double yDist = this.y - y;
      final double zDist = this.z - z;
      return xDist * xDist + yDist * yDist + zDist * zDist;
   }

   // ----< absolute >--------------------------------------------------------//

   /**
    * This method will set the components of this Tuple3f to their respective
    * absolute values.
    **/
   public void absolute() {
      if (this.x < 0) {
         this.x = -this.x;
      }
      if (this.y < 0) {
         this.y = -this.y;
      }
      if (this.z < 0) {
         this.z = -this.z;
      }
   } // end absolute

   // ----< absolute >--------------------------------------------------------//

   /**
    * This method will set the components of this Tuple3f to the absolute values
    * of the respective components in the parameter Tuple3f.
    *
    * @param _tuple
    *           The Tuple3f whose components' absolute values are set within
    *           this Tuple3f instance.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void absolute(final Tuple3f _tuple) throws NullPointerException {
      this.set(_tuple);
      this.absolute();
   } // end absolute

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the specified Tuple3f to this Tuple3f, element per
    * element, and stores the result in this Tuple3f.
    *
    * @param _tuple
    *           The Tuple3f that is added to this Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void add(final Tuple3f _tuple) throws NullPointerException {
      this.x += _tuple.x;
      this.y += _tuple.y;
      this.z += _tuple.z;
   } // end add

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the specified Tuple3d to this Tuple3f, element per
    * element, and stores the result in this Tuple3f.
    *
    * @param _tuple
    *           The Tuple3d that is added to this Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void add(final Tuple3d _tuple) throws NullPointerException {
      this.x += (float) _tuple.x;
      this.y += (float) _tuple.y;
      this.z += (float) _tuple.z;
   } // end add
   
   /**
    * Adds the specified vector components to this tuple.
    * 
    * @param x
    *           The amount to add to the x component.
    * @param y
    *           The amount to add to the y component.
    * @param z
    *           The amount to add to the z component.
    */
   public final void add(final float x, final float y, final float z) {
      this.x += x;
      this.y += y;
      this.z += z;
   }

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the two parameter Tuple3f instances, element per
    * element, and stores the result in this Tuple3f instance.
    *
    * @param _tuple1
    *           The first of the two tuples to add.
    * @param _tuple2
    *           The second of the two tuples to add.
    * @throws NullPointerException
    *            if either of the parameters is a null.
    **/
   public final void add(final Tuple3f _tuple1, final Tuple3f _tuple2) throws NullPointerException {
      this.x = _tuple1.x + _tuple2.x;
      this.y = _tuple1.y + _tuple2.y;
      this.z = _tuple1.z + _tuple2.z;
   } // end add

   // ----< sub >-------------------------------------------------------------//

   /**
    * This method subtracts the x, y, and z values of the parameter Tuple3f from
    * this Tuple3f, element per element.
    *
    * @param _tuple
    *           The Tuple3f instance to subtract from this Tuple3f instance.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void sub(final Tuple3f _tuple) throws NullPointerException {
      this.x -= _tuple.x;
      this.y -= _tuple.y;
      this.z -= _tuple.z;
   } // end sub

   // ----< sub >-------------------------------------------------------------//

   /**
    * This method subtracts the x, y, and z values of the parameter Tuple3d from
    * this Tuple3f, element per element.
    *
    * @param _tuple
    *           The Tuple3d instance to subtract from this Tuple3f instance.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void sub(final Tuple3d _tuple) throws NullPointerException {
      this.x -= (float) _tuple.x;
      this.y -= (float) _tuple.y;
      this.z -= (float) _tuple.z;
   } // end sub

   // ----< sub >-------------------------------------------------------------//

   /**
    * This method subtracts the second parameter Tuple3f from the first
    * parameter Tuple3f, and stores the result in this Tuple3f.
    *
    * @param _tuple1
    *           The tuple being subtracted from.
    * @param _tuple2
    *           The tuple doing the subtracting.
    * @throws NullPointerException
    *            if either of the parameters is a null.
    **/
   public final void sub(final Tuple3f _tuple1, final Tuple3f _tuple2) throws NullPointerException {
      this.x = _tuple1.x - _tuple2.x;
      this.y = _tuple1.y - _tuple2.y;
      this.z = _tuple1.z - _tuple2.z;
   } // end sub

   // ----< clampMin >--------------------------------------------------------//

   /**
    * This method sets a minimum value for x, y, and z. If x, y, or z is less
    * than the specified minimum, it will be set to the value of the _min
    * parameter.
    *
    * @param _min
    *           The smallest value any of the components of this Tuple3f
    *           should be.
    **/
   public void clampMin(final float _min) {
      if (this.x < _min) {
         this.x = _min;
      }
      if (this.y < _min) {
         this.y = _min;
      }
      if (this.z < _min) {
         this.z = _min;
      }
   } // end clampMin

   // ----< clampMax >--------------------------------------------------------//

   /**
    * This method sets a maximum value for x, y, and z. If x, y, or z is greater
    * than the specified maximum, it will be set to the value of the _max
    * parameter.
    *
    * @param _max
    *           The largest value any of the components of this Tuple3f
    *           should be.
    **/
   public void clampMax(final float _max) {
      if (this.x > _max) {
         this.x = _max;
      }
      if (this.y > _max) {
         this.y = _max;
      }
      if (this.z > _max) {
         this.z = _max;
      }
   } // end clampMax

   // ----< clamp >-----------------------------------------------------------//

   /**
    * This method will set both the minimum and maximum values of x, y, and z.
    * If x, y, or z is less than the specified minimum, it is set equal to _min.
    * If x, y, or z is greater than the specified maximum, it is set equal to
    * _max.
    * Behavior for this method is only defined for when _min >= _max.
    *
    * @param _min
    *           The smallest value any of the components of this Tuple3f
    *           should be.
    * @param _max
    *           The largest value any of the components of this Tuple3f
    *           should be.
    **/
   public void clamp(final float _min, final float _max) {
      this.clampMin(_min);
      this.clampMax(_max);
   } // end clamp

   // ----< div >-------------------------------------------------------------//

   /**
    * This method divides the entire tuple by a scalar.
    *
    * @param _denom
    *           The value to divide into each of the Tuple3f's components.
    **/
   public void div(final float _denom) {
      this.x /= _denom;
      this.y /= _denom;
      this.z /= _denom;
   } // end div

   // ----< div >-------------------------------------------------------------//

   /**
    * This method divides the parameter tuple by a scalar and stores the results
    * in this tuple.
    *
    * @param _denom
    *           The value to divide into each of the parameter Tuple3f's
    *           components.
    * @param _tuple
    *           The Tuple3f whose components are to be divided into by the
    *           _denom parameter, the results of which are stored in the corresponding
    *           components of this Tuple3f.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public void div(final float _denom, final Tuple3f _tuple) throws NullPointerException {
      this.x = _tuple.x / _denom;
      this.y = _tuple.y / _denom;
      this.z = _tuple.z / _denom;
   } // end div

   // ----< epsilonEquals >---------------------------------------------------//

   /**
    * This method checks if this Tuple3f is equal to the parameter Tuple3f
    * within an epsilon amount of error.
    *
    * @param _tuple
    *           The Tuple3f that this Tuple3f is being compared to for
    *           equality.
    * @param _epsilon
    *           The float value "distance" that corresponding tuple
    *           components may be different from each other and still be accepted as
    *           equal.
    * @return a boolean that is true if the values of x, y, and z in the
    *         parameter Tuple3f are within an error margin of _epsilon relative to this
    *         Tuple3f and false otherwise.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final boolean epsilonEquals(final Tuple3f _tuple, final float _epsilon) throws NullPointerException {
      final float tempX = (this.x > _tuple.x ? this.x - _tuple.x : _tuple.x - this.x);
      final float tempY = (this.y > _tuple.y ? this.y - _tuple.y : _tuple.y - this.y);
      final float tempZ = (this.z > _tuple.z ? this.z - _tuple.z : _tuple.z - this.z);
      return ((_epsilon >= tempX) && (_epsilon >= tempY) && (_epsilon >= tempZ));
   } // end epsilonEquals

   // ----< equals >----------------------------------------------------------//

   /**
    * This method is an exact equals check. The _obj parameter must be a
    * Tuple3f instance and the corresponding components of _obj and this
    * Tuple3f must all be equal.
    *
    * @param _obj
    *           An object that is compared to this Tuple3f for equality.
    * @return a boolean that is true if the parameter object is of the same
    *         class as this object and every element in the parameter object is equal
    *         to the corresponding value in this Tuple3f.
    **/
   @Override
   public boolean equals(final Object _obj) {
      if (this == _obj) {
         return (true);
      } else if ((_obj != null) && (_obj.getClass() == this.getClass())) {
         final Tuple3f t = (Tuple3f) _obj;
         return ((t.x == this.x) && (t.y == this.y) && (t.z == this.z));
      } else {
         return (false);
      } // end if
   } // end equals

   // ----< hashCode >--------------------------------------------------------//

   /**
    * This method is used to generate a hash code for this Tuple3f instance so
    * that it can be stored in certain data structures.
    *
    * @return an integer hash value for this Tuple3f.
    **/
   @Override
   public int hashCode() {
      // cheap hash algo
      // 1 mil 3
      // if member
      // total =* prime(int)primitive or prime*object.hashcode();

      int total = Float.floatToRawIntBits(this.x);
      final int PRIME = 1000003;
      total = total * PRIME + Float.floatToRawIntBits(this.y);
      total = total * PRIME + Float.floatToRawIntBits(this.z);
      return (total);
   } // end hashCode

   // ----< interpolate >-----------------------------------------------------//

   /**
    * Computes a linear interpolation of alpha between the parameter Tuple3f
    * and this Tuple3f. The results of the calculation are stored in this
    * Tuple3f.
    *
    * @param _tuple
    *           A Tuple3f instance that is the second of the points to
    *           interpolate between.
    * @param _alpha
    *           The decimal value that represents the percentage from this
    *           Tuple3f to the parameter Tuple3f; used to calculate the resultant
    *           Tuple3f. The value of _alpha should be such that _alpha is between zero
    *           and one inclusive.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    * @throws IllegalArgumentException
    *            if the _alpha parameter is a value less
    *            than zero or more than one.
    **/
   public final void interpolate(final Tuple3f _tuple, final float _alpha) throws NullPointerException,
         IllegalArgumentException {
      // (1 - alpha) * _tuple + alpha * t2).
      if ((_alpha < 0) || (_alpha > 1)) {
         throw new IllegalArgumentException("Invalid alpha value (" + _alpha + "): use values between [0 - 1]");
      }

      final float delta = (1 - _alpha);
      this.x = (delta * this.x) + (_alpha * _tuple.x);
      this.y = (delta * this.y) + (_alpha * _tuple.y);
      this.z = (delta * this.z) + (_alpha * _tuple.z);
   } // end interpolate

   // ----< interpolate >-----------------------------------------------------//

   /**
    * Computes linear interpolation of alpha between the first specified tuple
    * and the second specified tuple. The results of the calculation are
    * stored in this Tuple3f.
    *
    * @param _tuple1
    *           A Tuple3f instance that is the first of the points to
    *           interpolate between.
    * @param _tuple2
    *           A Tuple3f instance that is the second of the points to
    *           interpolate between.
    * @param _alpha
    *           The decimal value that represents the percentage from
    *           _tuple1 to the _tuple2; used to calculate the resultant
    *           Tuple3f. The value of _alpha should be such that _alpha is between zero
    *           and one inclusive.
    * @throws NullPointerException
    *            if the either parameter tuple is null.
    * @throws IllegalArgumentException
    *            if the _alpha parameter is a value less
    *            than zero or more than one.
    **/
   public final void interpolate(final Tuple3f _tuple1, final Tuple3f _tuple2, final float _alpha)
         throws NullPointerException, IllegalArgumentException {
      if ((_alpha < 0) || (_alpha > 1)) {
         throw new IllegalArgumentException("Invalid alpha value (" + _alpha + "): use values between [0 - 1]");
      }
      this.set(_tuple1);
      this.interpolate(_tuple2, _alpha);
   } // end interpolate

   // ----< negate >----------------------------------------------------------//

   /**
    * This method negates the x, y, and z values of this Tuple3f.
    **/
   public void negate() {
      this.x = -this.x;
      this.y = -this.y;
      this.z = -this.z;
   } // end negate

   // ----< scale >-----------------------------------------------------------//

   /**
    * This method multiplies x, y, and z by the parameter scale value.
    *
    * @param _scale
    *           A scalar multiplier to apply to each of the Tuple3f's
    *           components.
    **/
   public void scale(final float _scale) {
      this.x *= _scale;
      this.y *= _scale;
      this.z *= _scale;
   } // end scale

   // ----< scaleAdd >--------------------------------------------------------//

   /**
    * This method adds to the x, y, and z components of this Tuple3f to the x,
    * y, and z components of the parameter Tuple3f after the parameter Tuple3f
    * has been scaled by the parameter scale value.
    *
    * @param _scale
    *           A scalar multiplier to apply to each of the parameter
    *           Tuple3f's components.
    * @param _tuple
    *           The Tuple3f that is added to this Tuple3f, after the scalar
    *           multiple has been applied.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void scaleAdd(final float _scale, final Tuple3f _tuple) throws NullPointerException {
      this.x += _tuple.x * _scale;
      this.y += _tuple.y * _scale;
      this.z += _tuple.z * _scale;
   } // end scaleAdd

   // ----< toString >--------------------------------------------------------//

   /**
    * Places the three components of this Vector into the specified buffer at the buffer's current position.
    * The buffer position is modified by this call.
    *
    * @param buff
    *           A non-null FloatBuffer.
    */
   public void putInto(final FloatBuffer buff) {
      buff.put(this.x);
      buff.put(this.y);
      buff.put(this.z);
   }

   /**
    * Places the three components of this Vector into the specified buffer at the buffer's current position.
    * The buffer position is modified by this call.
    *
    * @param buff
    *           A non-null FloatBuffer.
    */
   public void putInto(final ByteBuffer buff) {
      buff.putFloat(this.x);
      buff.putFloat(this.y);
      buff.putFloat(this.z);
   }

//   /**
//    * Converts the specified array of Vector3f to a FloatBuffer. The resulting
//    * FloatBuffer will have capacity equal to 3 * the number of vertices passed
//    * in. The buffer will be flipped prior to returning, prepared for reading.
//    *
//    * @param verts
//    *           A (var-args) array of non-null Vector3f instances.
//    * @return A FloatBuffer containing the vertex data.
//    */
//   public static FloatBuffer toFloatBuffer(final Tuple3f... verts) {
//      final FloatBuffer buff = BufferUtil.newFloatBuffer(verts.length * 3);
//      for (final Tuple3f vert : verts) {
//         buff.put(vert.x);
//         buff.put(vert.y);
//         buff.put(vert.z);
//      }
//      buff.flip();
//      return buff;
//   }
//   
//   public static FloatBuffer toFloatBuffer(final List<? extends Tuple3f> verts) {
//      final FloatBuffer buff = BufferUtil.newFloatBuffer(verts.size() * 3);
//      for (final Tuple3f vert : verts) {
//         buff.put(vert.x);
//         buff.put(vert.y);
//         buff.put(vert.z);
//      }
//      buff.flip();
//      return buff;
//   }

   /**
    * This method outputs information about the Tuple3f instance.
    *
    * @return a String that contains the values of this Tuple3f. It is in the
    *         form "(x,y,z)".
    **/
   @Override
   public String toString() {
      final StringBuilder buff = new StringBuilder();
      buff.append("(");
      buff.append(this.x);
      buff.append(",");
      buff.append(this.y);
      buff.append(",");
      buff.append(this.z);
      buff.append(")");
      return (buff.toString());
   } // end toString

   @Override
   public double[] getInto(final double[] location, final int offset) {
      location[offset] = this.x;
      location[offset + 1] = this.y;
      location[offset + 2] = this.z;
      return location;
   }

   @Override
   public float[] getInto(final float[] location, final int offset) {
      location[offset] = this.x;
      location[offset + 1] = this.y;
      location[offset + 2] = this.z;
      return location;
   }

   @Override
   public FloatBuffer getInto(final FloatBuffer result) {
      result.put(x).put(y).put(z);
      return result;
   }

   @Override
   public DoubleBuffer getInto(final DoubleBuffer result) {
      result.put(x).put(y).put(z);
      return result;
   }
} // end class Tuple3f
