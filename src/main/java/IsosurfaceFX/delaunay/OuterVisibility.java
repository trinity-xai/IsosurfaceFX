package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.*;

/**
 * Conservative outer-visibility filter:
 *  - Cast a ray from the face centroid along its outward normal.
 *  - Only count FRONT-FACE intersections.
 *  - Only count hits within a local range: occlusionLimit = k * alpha.
 *  - Ignore triangles sharing any vertex with the tested face.
 *
 * This prevents over-culling on concavities and coplanar noise.
 */
public final class OuterVisibility {

    private OuterVisibility(){}

    /** Filter faces; alpha is used to set the local occlusion range (k * α). */
    public static List<int[]> filter(List<Point3D> pts, List<int[]> faces, double alpha, double kLocal) {
        if (faces.isEmpty()) return faces;

        // Build BVH over triangles once
        List<Triangle> tris = new ArrayList<>(faces.size());
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            tris.add(new Triangle(f[0], f[1], f[2], i));
        }
        BVH bvh = new BVH(pts, tris, faces);

        // Scene scale for robust tmin offset
        double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for (Point3D p: pts){ double x=p.getX(),y=p.getY(),z=p.getZ();
            if(x<minX)minX=x; if(y<minY)minY=y; if(z<minZ)minZ=z;
            if(x>maxX)maxX=x; if(y>maxY)maxY=y; if(z>maxZ)maxZ=z;
        }
        double dx=maxX-minX, dy=maxY-minY, dz=maxZ-minZ;
        double sceneDiag = Math.sqrt(dx*dx+dy*dy+dz*dz);
        final double EPS_ORIGIN = Math.max(1e-4, 1e-3 * (sceneDiag*1e-3)); // small but scaled
        final double T_MIN     = Math.max(1e-5, 1e-4 * sceneDiag);         // ignore hits too close
        final double FAR       = 1e9;
        final double occlusionLimit = Math.max(alpha * Math.max(0.25, kLocal), T_MIN * 10);

        ArrayList<int[]> out = new ArrayList<>(faces.size());
        for (int idx = 0; idx < faces.size(); idx++) {
            int[] f = faces.get(idx);
            Point3D A = pts.get(f[0]), B = pts.get(f[1]), C = pts.get(f[2]);

            // centroid
            double cx = (A.getX()+B.getX()+C.getX())/3.0;
            double cy = (A.getY()+B.getY()+C.getY())/3.0;
            double cz = (A.getZ()+B.getZ()+C.getZ())/3.0;

            // outward unit normal (based on face orientation)
            double ux = B.getX()-A.getX(), uy = B.getY()-A.getY(), uz = B.getZ()-A.getZ();
            double vx = C.getX()-A.getX(), vy = C.getY()-A.getY(), vz = C.getZ()-A.getZ();
            double nx = uy*vz - uz*vy;
            double ny = uz*vx - ux*vz;
            double nz = ux*vy - uy*vx;
            double nlen = Math.sqrt(nx*nx+ny*ny+nz*nz);
            if (nlen < 1e-12) continue; // degenerate
            nx/=nlen; ny/=nlen; nz/=nlen;

            // start slightly above the surface to avoid self-intersection
            double ox = cx + nx*EPS_ORIGIN;
            double oy = cy + ny*EPS_ORIGIN;
            double oz = cz + nz*EPS_ORIGIN;

            // Ignore this face and all faces sharing any vertex
            Set<Integer> ignore = bvh.vertexRingTriIds(idx);

            boolean occluded = bvh.rayFrontAnyHit(pts, ox,oy,oz, nx,ny,nz, T_MIN, Math.min(FAR, occlusionLimit), ignore);
            if (!occluded) out.add(f);
        }
        return out;
    }

    /* ---------------- Small BVH implementation ---------------- */

    private static final class Triangle {
        final int a,b,c;
        final int id;
        Triangle(int a,int b,int c,int id){ this.a=a; this.b=b; this.c=c; this.id=id; }
    }

    private static final class AABB {
        double minX, minY, minZ, maxX, maxY, maxZ;
        AABB(){ minX=minY=minZ=+1e300; maxX=maxY=maxZ=-1e300; }
        void include(Point3D p){
            double x=p.getX(), y=p.getY(), z=p.getZ();
            if (x<minX) minX=x; if (y<minY) minY=y; if (z<minZ) minZ=z;
            if (x>maxX) maxX=x; if (y>maxY) maxY=y; if (z>maxZ) maxZ=z;
        }
        void include(AABB o){
            if (o.minX<minX) minX=o.minX; if (o.minY<minY) minY=o.minY; if (o.minZ<minZ) minZ=o.minZ;
            if (o.maxX>maxX) maxX=o.maxX; if (o.maxY>maxY) maxY=o.maxY; if (o.maxZ>maxZ) maxZ=o.maxZ;
        }
        boolean rayHit(double ox,double oy,double oz, double dx,double dy,double dz, double tmin,double tmax){
            double invX = 1.0/(Math.abs(dx)<1e-18? 1e-18:dx);
            double invY = 1.0/(Math.abs(dy)<1e-18? 1e-18:dy);
            double invZ = 1.0/(Math.abs(dz)<1e-18? 1e-18:dz);
            double tx1 = (minX-ox)*invX, tx2 = (maxX-ox)*invX;
            double ty1 = (minY-oy)*invY, ty2 = (maxY-oy)*invY;
            double tz1 = (minZ-oz)*invZ, tz2 = (maxZ-oz)*invZ;
            double t0 = Math.max(Math.max(Math.min(tx1,tx2), Math.min(ty1,ty2)), Math.min(tz1,tz2));
            double t1 = Math.min(Math.min(Math.max(tx1,tx2), Math.max(ty1,ty2)), Math.max(tz1,tz2));
            return t1 >= Math.max(t0, tmin) && t0 <= tmax;
        }
    }

    private static final class BVHNode {
        AABB box = new AABB();
        BVHNode left, right;
        int leafStart = -1, leafCount = 0; // range into indices[]
    }

    private static final class BVH {
        final List<Triangle> tris;
        final int[] indices;     // permutation of triangle indices
        final BVHNode root;
        final Map<Integer, Set<Integer>> edgeAdj = new HashMap<>();
        final Map<Integer, Set<Integer>> vertAdj = new HashMap<>();

        BVH(List<Point3D> pts, List<Triangle> tris, List<int[]> faces) {
            this.tris = tris;
            this.indices = new int[tris.size()];
            for (int i=0;i<indices.length;i++) indices[i] = i;
            this.root = build(pts, 0, indices.length, 0);
            buildAdjacency(faces); // 1-ring (edge + vertex) for ignore set
        }

        // Build a median-split BVH on triangle centroids
        private BVHNode build(List<Point3D> pts, int start, int end, int depth){
            BVHNode node = new BVHNode();

            // compute bounds
            for (int i=start;i<end;i++){
                Triangle t = tris.get(indices[i]);
                AABB tb = triAABB(pts, t);
                node.box.include(tb);
            }
            int count = end - start;
            if (count <= 8) {
                node.leafStart = start;
                node.leafCount = count;
                return node;
            }

            // longest axis
            double ex=node.box.maxX-node.box.minX, ey=node.box.maxY-node.box.minY, ez=node.box.maxZ-node.box.minZ;
            int axis = (ex>=ey && ex>=ez)?0 : (ey>=ez?1:2);

            // boxed sort for primitive index array
            Integer[] tmp = new Integer[count];
            for (int i = 0; i < count; i++) tmp[i] = indices[start + i];
            Arrays.sort(tmp, Comparator.comparingDouble(ii -> centroid(pts, tris.get(ii), axis)));
            for (int i = 0; i < count; i++) indices[start + i] = tmp[i];

            int mid = (start + end) >>> 1;
            node.left  = build(pts, start, mid, depth+1);
            node.right = build(pts, mid, end,   depth+1);
            return node;
        }

        boolean rayFrontAnyHit(List<Point3D> pts, double ox,double oy,double oz, double dx,double dy,double dz,
                               double tmin,double tmax, Set<Integer> ignoreIds) {
            return rayFrontAnyHitRec(pts, root, ox,oy,oz, dx,dy,dz, tmin,tmax, ignoreIds);
        }

        private boolean rayFrontAnyHitRec(List<Point3D> pts, BVHNode node,
                                          double ox,double oy,double oz, double dx,double dy,double dz,
                                          double tmin,double tmax, Set<Integer> ignoreIds) {
            if (!node.box.rayHit(ox,oy,oz, dx,dy,dz, tmin,tmax)) return false;
            if (node.leafStart >= 0) {
                for (int i=node.leafStart;i<node.leafStart+node.leafCount;i++){
                    Triangle t = tris.get(indices[i]);
                    if (ignoreIds != null && ignoreIds.contains(t.id)) continue;
                    double tHit = rayTriFront(pts, t, ox,oy,oz, dx,dy,dz, tmin, tmax);
                    if (tHit >= 0.0) return true;
                }
                return false;
            }
            return rayFrontAnyHitRec(pts, node.left, ox,oy,oz, dx,dy,dz, tmin,tmax, ignoreIds)
                || rayFrontAnyHitRec(pts, node.right,ox,oy,oz, dx,dy,dz, tmin,tmax, ignoreIds);
        }

        // Front-face-only Möller–Trumbore: also rejects backface hits
        private static double rayTriFront(List<Point3D> pts, Triangle T,
                                          double ox,double oy,double oz, double dx,double dy,double dz,
                                          double tmin,double tmax) {
            Point3D A = pts.get(T.a), B = pts.get(T.b), C = pts.get(T.c);
            double e1x=B.getX()-A.getX(), e1y=B.getY()-A.getY(), e1z=B.getZ()-A.getZ();
            double e2x=C.getX()-A.getX(), e2y=C.getY()-A.getY(), e2z=C.getZ()-A.getZ();

            // backface rejection using face normal
            double nx = e1y*e2z - e1z*e2y;
            double ny = e1z*e2x - e1x*e2z;
            double nz = e1x*e2y - e1y*e2x;
            double ndotd = nx*dx + ny*dy + nz*dz;
            if (ndotd >= -1e-12) return -1; // triangle not facing the ray

            // standard MT
            double px = dy*e2z - dz*e2y;
            double py = dz*e2x - dx*e2z;
            double pz = dx*e2y - dy*e2x;

            double det = e1x*px + e1y*py + e1z*pz;
            if (Math.abs(det) < 1e-12) return -1; // parallel

            double inv = 1.0/det;
            double sx = ox - A.getX(), sy = oy - A.getY(), sz = oz - A.getZ();
            double u = (sx*px + sy*py + sz*pz) * inv;
            if (u < 0 || u > 1) return -1;

            double qx = sy*e1z - sz*e1y;
            double qy = sz*e1x - sx*e1z;
            double qz = sx*e1y - sy*e1x;
            double v  = (dx*qx + dy*qy + dz*qz) * inv;
            if (v < 0 || u+v > 1) return -1;

            double t  = (e2x*qx + e2y*qy + e2z*qz) * inv;
            return (t >= tmin && t <= tmax) ? t : -1;
        }

        private static AABB triAABB(List<Point3D> pts, Triangle t){
            AABB b = new AABB();
            b.include(pts.get(t.a)); b.include(pts.get(t.b)); b.include(pts.get(t.c));
            return b;
        }
        private static double centroid(List<Point3D> pts, Triangle t, int axis){
            Point3D A=pts.get(t.a),B=pts.get(t.b),C=pts.get(t.c);
            if (axis==0) return (A.getX()+B.getX()+C.getX())/3.0;
            if (axis==1) return (A.getY()+B.getY()+C.getY())/3.0;
            return (A.getZ()+B.getZ()+C.getZ())/3.0;
        }

        // Build both edge and vertex adjacency (1-ring)
        private void buildAdjacency(List<int[]> faces) {
            Map<Long, Integer> edgeOwner = new HashMap<>(tris.size()*3);
            Map<Integer, Set<Integer>> vertToTris = new HashMap<>();

            for (int id = 0; id < faces.size(); id++) {
                int[] f = faces.get(id);
                int a=f[0], b=f[1], c=f[2];

                // vertices→tris
                vertToTris.computeIfAbsent(a, s->new HashSet<>()).add(id);
                vertToTris.computeIfAbsent(b, s->new HashSet<>()).add(id);
                vertToTris.computeIfAbsent(c, s->new HashSet<>()).add(id);

                // edge adjacency
                int[] v = {a,b,c};
                for (int e=0;e<3;e++){
                    int i=v[e], j=v[(e+1)%3];
                    long k = edgeKey(i,j);
                    Integer prev = edgeOwner.putIfAbsent(k, id);
                    if (prev != null) {
                        edgeAdj.computeIfAbsent(id,   s->new HashSet<>()).add(prev);
                        edgeAdj.computeIfAbsent(prev, s->new HashSet<>()).add(id);
                    }
                }
            }

            // vertex adjacency from vert→tris map
            for (Set<Integer> ts : vertToTris.values()) {
                for (int a : ts) {
                    Set<Integer> set = vertAdj.computeIfAbsent(a, s->new HashSet<>());
                    set.addAll(ts);
                }
            }
            // include self in both maps
            for (int i=0;i<faces.size();i++) {
                edgeAdj.computeIfAbsent(i, s->new HashSet<>()).add(i);
                vertAdj.computeIfAbsent(i, s->new HashSet<>()).add(i);
            }
        }

        /** Triangles to ignore for a given face: 1-ring (edge OR vertex shared). */
        Set<Integer> vertexRingTriIds(int triId){
            Set<Integer> out = new HashSet<>();
            Set<Integer> e = edgeAdj.get(triId);
            Set<Integer> v = vertAdj.get(triId);
            if (e != null) out.addAll(e);
            if (v != null) out.addAll(v);
            return out;
        }

        private static long edgeKey(int a,int b){
            int i=Math.min(a,b), j=Math.max(a,b);
            return (((long)i)<<32) | (j & 0xffffffffL);
        }
    }
}