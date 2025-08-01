package IsosurfaceFX;

import java.util.List;
import javafx.geometry.Point3D;

    /**
     * The octree node in the 3d space.
     * Each node can have eight children nodes.
     * The leaf node has the indices of points that is located in this cell.
     */
    public class OctreeNode extends Box {

        /**
         * default value is root index
         * the index is generated in createOctree()
         **/
        Long index = 0L;

        /**
         * an octree node holds the indices of 3d points in the List
         * in a non-leaf node, field indices is null
         **/
        List<Integer> indices = null;

        /**
         * in a non-leaf node, field indices is null
         **/
        OctreeNode[] children = null;

        int depth = 0;
        // For accumulating vector field (normal contributions)
        private Point3D normalSum = Point3D.ZERO;
        private double weightSum = 0.0;

        // For storing divergence at this node (computed from normal field)
        private double divergence = 0.0;

        // For storing the scalar function value (solution to Poisson equation)
        private double scalarFieldValue = 0.0;
        private double size = 1;  // Length of one edge of the cube


        public OctreeNode(Point3D center, double size, int depth) {
            this.center = new Point3D(center.getX(), center.getY(), center.getZ());
            this.xExtent = size;
            this.yExtent = size;
            this.zExtent = size;
            this.size = size;
            this.depth = depth;
        }
        public Point3D getAccumulatedNormal() {
            if (weightSum == 0) return Point3D.ZERO;
            return new Point3D(
                normalSum.getX() / weightSum,
                normalSum.getY() / weightSum,
                normalSum.getZ() / weightSum
            ).normalize();
        }        
        public OctreeNode findLeafContaining(Point3D point) {
            if (isLeaf()) {
                return this;
            }
            for (OctreeNode child : children) {
                if (child != null && child.contains(point)) {
                    return child.findLeafContaining(point);
                }
            }
            return null;
        }

        @Override
        public boolean contains(Point3D p) {
            double half = size / 2.0;
            return Math.abs(p.getX() - center.getX()) <= half &&
                   Math.abs(p.getY() - center.getY()) <= half &&
                   Math.abs(p.getZ() - center.getZ()) <= half;
        }
        public double getSize() {
            return size;
        }
        public void setSize(double size) {
            this.size = size;
        }        
        public Long getIndex() {
            return index;
        }

        public void setIndex(long i) {
            this.index = i;
        }

        public List<Integer> getIndices() {
            return indices;
        }

        public void setIndices(List<Integer> indices) {
            this.indices = indices;
        }

        public OctreeNode[] getChildren() {
            return children;
        }

        public void setChildren(OctreeNode[] nodes) {
            this.children = nodes;
        }

        public int getDepth() {
            return depth;
        }

        public boolean isLeaf() {
            return children == null;
        }
        public void accumulateNormal(Point3D normal, double weight) {
            normalSum = normalSum.add(normal.multiply(weight));
            weightSum += weight;
        }

        public Point3D getAverageNormal() {
            return weightSum > 0 ? normalSum.multiply(1.0 / weightSum) : Point3D.ZERO;
        }

        public void setDivergence(double value) {
            this.divergence = value;
        }

        public double getDivergence() {
            return divergence;
        }

        public void setScalarFieldValue(double value) {
            this.scalarFieldValue = value;
        }

        public double getScalarFieldValue() {
            return scalarFieldValue;
        }        
    }