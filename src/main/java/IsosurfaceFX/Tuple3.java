package IsosurfaceFX;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Common interface for 2D tuples that use (single or double precision) floating point values for their components.
 */
public interface Tuple3 {

   /**
    * Accessor that stores this tuple's 3 component values into the specified array starting at the specified offset
    * index.
    * 
    * @param result
    *           Non-null array to store the components in. Must have length at least offset + 3.
    * @param offset
    *           The index at which to store the components. Values will be written into the result array at indices
    *           'offset', 'offset + 1', and 'offset + 2'.
    * @return The result array that was passed in for convenience.
    */
   public double[] getInto(final double[] result, int offset);

   /**
    * Accessor that stores this tuple's 3 component values into the specified array starting at the specified offset
    * index.
    * 
    * @param result
    *           Non-null array to store the components in. Must have length at least offset + 3.
    * @param offset
    *           The index at which to store the components. Values will be written into the result array at indices
    *           'offset', 'offset + 1', and 'offset + 2'.
    * @return The result array that was passed in for convenience.
    */
   
   public float[] getInto( final float[] result, int offset);

   /**
    * Accessor that stores this tuple's 3 component values into the specified FloatBuffer at its current position.
    * 
    * @param result
    *           Non-null FloatBuffer. Must have limit at least position() + 3.
    * @return The result buffer that was passed in for convenience.
    */
   
   public FloatBuffer getInto( final FloatBuffer result);

   /**
    * Accessor that stores this tuple's 3 component values into the specified FloatBuffer at its current position.
    * 
    * @param result
    *           Non-null FloatBuffer. Must have limit at least position() + 3.
    * @return The result buffer that was passed in for convenience.
    */
   
   public DoubleBuffer getInto( final DoubleBuffer result);
}
