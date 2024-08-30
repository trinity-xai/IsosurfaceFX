// : Tuple3d.java

package IsosurfaceFX;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * A double precision Tuple of 3 elements.
 * <p>
 * Revision 1.0 06.27.2002 unnamed date June-July 2002
 **/
public class Tuple3d implements Cloneable, Tuple3 {

   // ----< Public member variables >-----------------------------------------//

   /** The x element. **/
   public double x;

   /** The y element. **/
   public double y;

   /** The z element. **/
   public double z;

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3d with x, y, z set to zero.
    **/
   public Tuple3d() {
      /*
       * Init to 0's
       */
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3d with x, y, and z set by the parameter
    * doubles.
    * 
    * @param _x
    *           The double value to set this Tuple3d's x component to.
    * @param _y
    *           The double value to set this Tuple3d's y component to.
    * @param _z
    *           The double value to set this Tuple3d's z component to.
    **/
   public Tuple3d(final double _x, final double _y, final double _z) {
      this.x = _x;
      this.y = _y;
      this.z = _z;
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3d, with x, y, and z set in that order,
    * from the parameter double array.
    * 
    * @param _doubles
    *           A double array that must contain at least three values.
    *           The first will be the value the x component is set to, the second will be
    *           the value the y component gets set to, and the third will be the value the
    *           z component gets set to.
    * @throws NullPointerException
    *            if the parameter double array is null.
    * @throws IllegalArgumentException
    *            if the parameter array's length is less
    *            than size three.
    **/
   public Tuple3d(final double[] _doubles) throws NullPointerException, IllegalArgumentException {
      if ((_doubles != null) && (_doubles.length >= 3)) {
         this.x = _doubles[0];
         this.y = _doubles[1];
         this.z = _doubles[2];
      } else {
         throw new IllegalArgumentException();
      } // end if

   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3d with x, y, and z set equal to the
    * respective components in the parameter Tuple3d.
    * 
    * @param _tuple
    *           The Tuple3d instance whose values are to be copied into this
    *           Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public Tuple3d(final Tuple3d _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end constructor

   // ----< Constructor >-----------------------------------------------------//

   /**
    * Constructs and initializes a Tuple3d with x, y, and z set equal to the
    * respective components in the parameter Tuple3f.
    * 
    * @param _tuple
    *           The Tuple3f instance whose values are to be copied into this
    *           Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public Tuple3d(final Tuple3f _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end constructor

   // // Setters ///////////////////////////////////////////////////////////////

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3d to the parameter
    * doubles.
    * 
    * @param _x
    *           The value to set the x component of this Tuple3d to.
    * @param _y
    *           The value to set the y component of this Tuple3d to.
    * @param _z
    *           The value to set the z component of this Tuple3d to.
    **/
   public final void set(final double _x, final double _y, final double _z) {
      this.x = _x;
      this.y = _y;
      this.z = _z;
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x and y values of this Tuple3d from the parameter
    * double array in x, y, z order.
    * 
    * @param _doubles
    *           A double array that must contain at least three values.
    *           The first will be the value the x component is set to, the second will be
    *           the value the y component gets set to, and the third will be the value the
    *           z component gets set to.
    * @throws NullPointerException
    *            if the parameter double array is null.
    * @throws IllegalArgumentException
    *            if the parameter array's length is less
    *            than size three.
    **/
   public void set(final double[] _doubles) throws NullPointerException, IllegalArgumentException {
      if (_doubles.length >= 3) {
         this.x = _doubles[0];
         this.y = _doubles[1];
         this.z = _doubles[2];
      } else {
         throw new IllegalArgumentException();
      } // end if
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3d to the same values
    * as the parameter Tuple3d.
    * 
    * @param _tuple
    *           The Tuple3d instance whose values are to be copied into this
    *           Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void set(final Tuple3d _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end set

   // ----< set >-------------------------------------------------------------//

   /**
    * This method sets the x, y, and z values of this Tuple3d equal to the
    * respective components in the parameter Tuple3f.
    * 
    * @param _tuple
    *           The Tuple3f instance whose values are to be copied into this
    *           Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void set(final Tuple3f _tuple) throws NullPointerException {
      this.x = _tuple.x;
      this.y = _tuple.y;
      this.z = _tuple.z;
   } // end set

   // // Getters ///////////////////////////////////////////////////////////////

   // ----< get >-------------------------------------------------------------//

   /**
    * This is a convenience accessor method that will create a new double array
    * of length three containing the values of the x, y, and z member variables.
    * 
    * @return a double array containing the x, y, and z from this Tuple3d, in
    *         that order.
    **/
   public double[] get() {
      return (new double[] { this.x, this.y, this.z });
   } // end get

   // ----< getDoubles >------------------------------------------------------//

   /**
    * This is a convenience accessor method that will create a new double array
    * of length three containing the values of he x, y, and z member variables.
    * 
    * @return a double array containing the x, y, and z from this Tuple3d, in
    *         that order.
    **/
   public double[] getDoubles() {
      return (new double[] { this.x, this.y, this.z });
   } // end getDoubles

   // ----< get >-------------------------------------------------------------//

   /**
    * This method will populate the parameter double array with the components
    * of this Tuple3d.
    * 
    * @param _doubles
    *           A double array containing the x, y, and z from this
    *           Tuple3d, in that order.
    * @throws NullPointerException
    *            if the _doubles parameter is null.
    * @throws IllegalArgumentException
    *            if the _doubles array parameter has a
    *            length less than three.
    **/
   public final void get(final double[] _doubles) throws NullPointerException, IllegalArgumentException {
      if (_doubles.length >= 3) {
         _doubles[0] = this.x;
         _doubles[1] = this.y;
         _doubles[2] = this.z;
      } else {
         throw new IllegalArgumentException("Double array must be at least of length three");
      } // end if
   } // end get

   // // Mathematical Operations ///////////////////////////////////////////////

   // ----< absolute >--------------------------------------------------------//

   /**
    * This method will set the components of this Tuple3d to their respective
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
    * This method will set the components of this Tuple3d to the absolute values
    * of the respective components in the parameter Tuple3d.
    * 
    * @param _tuple
    *           The Tuple3d whose components' absolute values are set within
    *           this Tuple3d instance.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void absolute(final Tuple3d _tuple) throws NullPointerException {
      this.set(_tuple);
      this.absolute();
   } // end absolute

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the specified Tuple3d to this Tuple3d, element per
    * element, and stores the result in this Tuple3d.
    * 
    * @param _tuple
    *           The Tuple3d that is added to this Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void add(final Tuple3d _tuple) throws NullPointerException {
      this.x += _tuple.x;
      this.y += _tuple.y;
      this.z += _tuple.z;
   } // end add

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the specified Tuple3f to this Tuple3d, element per
    * element, and stores the result in this Tuple3d.
    * 
    * @param _tuple
    *           The Tuple3f that is added to this Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void add(final Tuple3f _tuple) throws NullPointerException {
      this.x += _tuple.x;
      this.y += _tuple.y;
      this.z += _tuple.z;
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
   public final void add(final double x, final double y, final double z) {
      this.x += x;
      this.y += y;
      this.z += z;
   }

   // ----< add >-------------------------------------------------------------//

   /**
    * This method adds the two parameter Tuple3d instances, element per
    * element, and stores the result in this Tuple3d instance.
    * 
    * @param _tuple1
    *           The first of the two tuples to add.
    * @param _tuple2
    *           The second of the two tuples to add.
    * @throws NullPointerException
    *            if either of the parameters is a null.
    **/
   public final void add(final Tuple3d _tuple1, final Tuple3d _tuple2) throws NullPointerException {
      this.x = _tuple1.x + _tuple2.x;
      this.y = _tuple1.y + _tuple2.y;
      this.z = _tuple1.z + _tuple2.z;
   } // end add

   // ----< sub >-------------------------------------------------------------//

   /**
    * This method subtracts the x, y, and z values of the parameter Tuple3d from
    * this Tuple3d, element per element.
    * 
    * @param _tuple
    *           The Tuple3d instance to subtract from this Tuple3d instance.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void sub(final Tuple3d _tuple) throws NullPointerException {
      this.x -= _tuple.x;
      this.y -= _tuple.y;
      this.z -= _tuple.z;
   } // end sub

   // ----< sub >-------------------------------------------------------------//

   /**
    * This method subtracts the x, y, and z values of the parameter Tuple3f from
    * this Tuple3d, element per element.
    * 
    * @param _tuple
    *           The Tuple3f instance to subtract from this Tuple3d instance.
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
    * This method subtracts the second parameter Tuple3d from the first
    * parameter Tuple3d, and stores the result in this Tuple3d.
    * 
    * @param _tuple1
    *           The tuple being subtracted from.
    * @param _tuple2
    *           The tuple doing the subtracting.
    * @throws NullPointerException
    *            if either of the parameters is a null.
    **/
   public final void sub(final Tuple3d _tuple1, final Tuple3d _tuple2) throws NullPointerException {
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
    *           The smallest value any of the components of this Tuple3d
    *           should be.
    **/
   public void clampMin(final double _min) {
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
    *           The largest value any of the components of this Tuple3d
    *           should be.
    **/
   public void clampMax(final double _max) {
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
    *           The smallest value any of the components of this Tuple3d
    *           should be.
    * @param _max
    *           The largest value any of the components of this Tuple3d
    *           should be.
    **/
   public void clamp(final double _min, final double _max) {
      this.clampMin(_min);
      this.clampMax(_max);
   } // end clamp

   // ----< div >-------------------------------------------------------------//

   /**
    * This method divides the entire tuple by a scalar.
    * 
    * @param _denom
    *           The value to divide into each of the Tuple3d's components.
    **/
   public void div(final double _denom) {
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
    *           The value to divide into each of the parameter Tuple3d's
    *           components.
    * @param _tuple
    *           The Tuple3d whose components are to be divided into by the
    *           _denom parameter, the results of which are stored in the corresponding
    *           components of this Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public void div(final double _denom, final Tuple3d _tuple) throws NullPointerException {
      this.x = _tuple.x / _denom;
      this.y = _tuple.y / _denom;
      this.z = _tuple.z / _denom;
   } // end div

   // ----< epsilonEquals >---------------------------------------------------//

   /**
    * This method checks if this Tuple3d is equal to the parameter Tuple3d
    * within an epsilon amount of error.
    * 
    * @param _tuple
    *           The Tuple3d that this Tuple3d is being compared to for
    *           equality.
    * @param _epsilon
    *           The double value "distance" that corresponding tuple
    *           components may be different from each other and still be accepted as
    *           equal.
    * @return a boolean that is true if the values of x, y, and z in the
    *         parameter Tuple3d are within an error margin of _epsilon relative to this
    *         Tuple3d and false otherwise.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final boolean epsilonEquals(final Tuple3d _tuple, final double _epsilon) throws NullPointerException {
      final double tempX = (this.x > _tuple.x ? this.x - _tuple.x : _tuple.x - this.x);
      final double tempY = (this.y > _tuple.y ? this.y - _tuple.y : _tuple.y - this.y);
      final double tempZ = (this.z > _tuple.z ? this.z - _tuple.z : _tuple.z - this.z);
      return ((_epsilon >= tempX) && (_epsilon >= tempY) && (_epsilon >= tempZ));
   } // end epsilonEquals

   // ----< equals >----------------------------------------------------------//

   /**
    * This method is an exact equals check. The _obj parameter must be a
    * Tuple3d instance and the corresponding components of _obj and this
    * Tuple3d must all be equal.
    * 
    * @param _obj
    *           An object that is compared to this Tuple3d for equality.
    * @return a boolean that is true if the parameter object is of the same
    *         class as this object and every element in the parameter object is equal
    *         to the corresponding value in this Tuple3d.
    **/
   @Override
   public boolean equals(final Object _obj) {
      if (this == _obj) {
         return (true);
      } else if ((_obj != null) && (_obj.getClass() == this.getClass())) {
         final Tuple3d t = (Tuple3d) _obj;
         return ((t.x == this.x) && (t.y == this.y) && (t.z == this.z));
      } else {
         return (false);
      } // end if
   } // end equals

   // ----< hashCode >--------------------------------------------------------//

   /**
    * This method is used to generate a hash code for this Tuple3d instance so
    * that it can be stored in certain data structures.
    * 
    * @return an integer hash value for this Tuple3d.
    **/
   @Override
   public int hashCode() {
      // cheap hash algo
      // 1 mil 3
      // if member
      // total =* prime(int)primitive or prime*object.hashcode();

      long total = Double.doubleToRawLongBits(this.x);
      final int PRIME = 1000003;
      total = total * PRIME + Double.doubleToRawLongBits(this.y);
      total = total * PRIME + Double.doubleToRawLongBits(this.z);
      return ((int) total);
   } // end hashCode

   // ----< interpolate >-----------------------------------------------------//

   /**
    * Computes a linear interpolation of alpha between the parameter Tuple3d
    * and this Tuple3d. The results of the calculation are stored in this
    * Tuple3d.
    * 
    * @param _tuple
    *           A Tuple3d instance that is the second of the points to
    *           interpolate between.
    * @param _alpha
    *           The decimal value that represents the percentage from this
    *           Tuple3d to the parameter Tuple3d; used to calculate the resultant
    *           Tuple3d. The value of _alpha should be such that _alpha is between zero
    *           and one inclusive.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    * @throws IllegalArgumentException
    *            if the _alpha parameter is a value less
    *            than zero or more than one.
    **/
   public final void interpolate(final Tuple3d _tuple, final double _alpha) throws NullPointerException,
         IllegalArgumentException {
      // (1 - alpha) * t1 + alpha * t2).
      if ((_alpha < 0) || (_alpha > 1)) {
         throw new IllegalArgumentException("Invalid alpha value (" + _alpha + "): use values between [0 - 1]");
      }

      final double delta = (1 - _alpha);
      this.x = (delta * this.x) + (_alpha * _tuple.x);
      this.y = (delta * this.y) + (_alpha * _tuple.y);
      this.z = (delta * this.z) + (_alpha * _tuple.z);
   } // end interpolate

   // ----< interpolate >-----------------------------------------------------//

   /**
    * Computes linear interpolation of alpha between the first specified tuple
    * and the second specified tuple. The results of the calculation are
    * stored in this Tuple3d.
    * 
    * @param _tuple1
    *           A Tuple3d instance that is the first of the points to
    *           interpolate between.
    * @param _tuple2
    *           A Tuple3d instance that is the second of the points to
    *           interpolate between.
    * @param _alpha
    *           The decimal value that represents the percentage from
    *           _tuple1 to the _tuple2; used to calculate the resultant
    *           Tuple3d. The value of _alpha should be such that _alpha is between zero
    *           and one inclusive.
    * @throws NullPointerException
    *            if the either parameter tuple is null.
    * @throws IllegalArgumentException
    *            if the _alpha parameter is a value less
    *            than zero or more than one.
    **/
   public final void interpolate(final Tuple3d _tuple1, final Tuple3d _tuple2, final double _alpha)
         throws NullPointerException, IllegalArgumentException {
      if ((_alpha < 0) || (_alpha > 1)) {
         throw new IllegalArgumentException("Invalid alpha value (" + _alpha + "): use values between [0 - 1]");
      }

      this.set(_tuple1);
      this.interpolate(_tuple2, _alpha);
   } // end interpolate

   // ----< negate >----------------------------------------------------------//

   /**
    * This method negates the x, y, and z values of this Tuple3d.
    **/
   public void negate() {
      this.x = -this.x;
      this.y = -this.y;
      this.z = -this.z;
   } // end negate

   // ----< negate >----------------------------------------------------------//

   /**
    * This method negates the x, y, and z values of the parameter Tuple3d and
    * sets this Tuple3d's corresponding components to those values. The
    * parameter Tuple3d is not changed.
    * 
    * @param _tuple
    *           The Tuple3d instance whose negated component values are
    *           stored in this Tuple3d.
    **/
   public final void negate(final Tuple3d _tuple) {
      this.x = -_tuple.x;
      this.y = -_tuple.y;
      this.z = -_tuple.z;
   } // end negate

   // ----< scale >-----------------------------------------------------------//

   /**
    * This method multiplies x, y, and z by the parameter scale value.
    * 
    * @param _scale
    *           A scalar multiplier to apply to each of the Tuple3d's
    *           components.
    **/
   public void scale(final double _scale) {
      this.x *= _scale;
      this.y *= _scale;
      this.z *= _scale;
   } // end scale

   // ----< scale >-----------------------------------------------------------//

   /**
    * This method multiples the x, y, and z values of the parameter Tuple3d by
    * the parameter _scale and sets this Tuple3d's corresponding components to
    * those values. The parameter Tuple3d is not changed.
    * 
    * @param _scale
    *           A scalar multiplier to apply to each of the parameter
    *           Tuple3d's components.
    * @param _tuple
    *           The Tuple3d instance whose multipled component values are
    *           stored in this Tuple3d.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void scale(final double _scale, final Tuple3d _tuple) throws NullPointerException {
      this.x = _tuple.x * _scale;
      this.y = _tuple.y * _scale;
      this.z = _tuple.z * _scale;
   } // end scale

   // ----< scaleAdd >--------------------------------------------------------//

   /**
    * This method adds to the x, y, and z components of this Tuple3d to the x,
    * y, and z components of the parameter Tuple3d after the parameter Tuple3d
    * has been scaled by the parameter scale value.
    * <p>
    * ex: This = this + t1 * s
    * 
    * @param _scale
    *           A scalar multiplier to apply to each of the parameter
    *           Tuple3d's components.
    * @param _tuple
    *           The Tuple3d that is added to this Tuple3d, after the scalar
    *           multiple has been applied.
    * @throws NullPointerException
    *            if the parameter tuple is null.
    **/
   public final void scaleAdd(final double _scale, final Tuple3d _tuple) throws NullPointerException {
      this.x += _tuple.x * _scale;
      this.y += _tuple.y * _scale;
      this.z += _tuple.z * _scale;
   } // end scaleAdd

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
   public final double distance(final double x, final double y, final double z) {
      final double xDist = this.x - x;
      final double yDist = this.y - y;
      final double zDist = this.z - z;
      return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
   }
   
   /**
    * Returns the Euclidian distance from the point represented by this Tuple to the specified location.
    * 
    * @param t
    *           The Tuple to find the distance to
    * @return The (positive) distance between this tuple and the specified tuple.
    */
   public final double distance(final Tuple3d t) {
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
      return distanceSquared(t.x, t.y, t.z);
   }

   /**
    * Places the three components of this Vector into the specified buffer at the buffer's current position.
    * The buffer position is modified by this call.
    * 
    * @param buff
    *           A non-null FloatBuffer.
    */
   public void putInto(final FloatBuffer buff) {
      buff.put((float) this.x);
      buff.put((float) this.y);
      buff.put((float) this.z);
   }

//   /**
//    * Converts the specified array of Vector3d to a FloatBuffer. The resulting FloatBuffer will have capacity equal to
//    * 3 * the number of vertices passed in. The buffer will be flipped prior to returning, prepared for reading.
//    * 
//    * @param verts
//    *           A (var-args) array of non-null Vector3d instances.
//    * @return A FloatBuffer containing the vertex data.
//    */
//   public static FloatBuffer toFloatBuffer(final Tuple3d... verts) {
//      final FloatBuffer buff = BufferUtil.newFloatBuffer(verts.length * 3);
//      for (final Tuple3d vert : verts) {
//         buff.put((float) vert.x);
//         buff.put((float) vert.y);
//         buff.put((float) vert.z);
//      }
//      buff.flip();
//      return buff;
//   }

   // ----< toString >--------------------------------------------------------//

   /**
    * This method outputs information about the Tuple3d instance.
    * 
    * @return a String that contains the values of this Tuple3d. It is in the
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

    /**
     *
     * @return
     */
    @Override
   public Tuple3d clone() {
      try {
         return (Tuple3d) super.clone();
      } catch (final CloneNotSupportedException e) {
         assert false;
         throw new IllegalStateException("This should not be reachable.");
      }
   }

   @Override
   public double[] getInto(final double[] location, final int offset) {
      location[offset] = this.x;
      location[offset + 1] = this.y;
      location[offset + 2] = this.z;
      return location;
   }

   @Override
   public float[] getInto(final float[] location, final int offset) {
      location[offset] = (float) this.x;
      location[offset + 1] = (float) this.y;
      location[offset + 2] = (float) this.z;
      return location;
   }

   @Override
   public FloatBuffer getInto(final FloatBuffer result) {
      result.put((float) x).put((float) y).put((float) z);
      return result;
   }

   @Override
   public DoubleBuffer getInto(final DoubleBuffer result) {
      result.put(x).put(y).put(z);
      return result;
   }

   /**
    * Returns the distance between 2 points.
    * 
    * @param x1
    *           The x component of the first point.
    * @param y1
    *           The y component of the first point.
    * @param z1
    *           The z component of the first point.
    * @param x2
    *           The x component of the second point.
    * @param y2
    *           The y component of the second point.
    * @param z2
    *           The z component of the second point.
    * @return The distance between p1 and p2
    */
   public static final double distance(final double x1, final double y1, final double z1, final double x2,
         final double y2, final double z2) {
      final double dx = x2 - x1;
      final double dy = y2 - y1;
      final double dz = z2 - z1;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   /**
    * Returns the squared distance between 2 points.
    * 
    * @param x1
    *           The x component of the first point.
    * @param y1
    *           The y component of the first point.
    * @param z1
    *           The z component of the first point.
    * @param x2
    *           The x component of the second point.
    * @param y2
    *           The y component of the second point.
    * @param z2
    *           The z component of the second point.
    * 
    * @return The squared distance between p1 and p2
    */
   public static final double distanceSquared(final double x1, final double y1, final double z1, final double x2,
         final double y2, final double z2) {
      final double dx = x2 - x1;
      final double dy = y2 - y1;
      final double dz = z2 - z1;
      return dx * dx + dy * dy + dz * dz;
   }

} // end class Tuple3d
