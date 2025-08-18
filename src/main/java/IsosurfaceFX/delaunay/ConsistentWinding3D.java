package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.*;

/**
 * Makes triangle windings consistent across the surface by propagating orientation
 * along shared, directed edges. Then, per connected component, possibly flips
 * the entire component so its average normal points "outward" (away from AABB center).
 *
 * Assumes faces are closed or mostly manifold; tolerates small non-manifolds.
 */
public final class ConsistentWinding3D {
    private ConsistentWinding3D(){}

    /** Returns a new list with consistent windings; original list is not modified. */
    public static List<int[]> orient(List<Point3D> pts, List<int[]> faces) {
        if (faces.isEmpty()) return faces;

        // AABB center
        double minX=1e300,minY=1e300,minZ=1e300,maxX=-1e300,maxY=-1e300,maxZ=-1e300;
        for (Point3D p : pts) {
            double x=p.getX(), y=p.getY(), z=p.getZ();
            if (x<minX) minX=x; if (y<minY) minY=y; if (z<minZ) minZ=z;
            if (x>maxX) maxX=x; if (y>maxY) maxY=y; if (z>maxZ) maxZ=z;
        }
        final double Cx=(minX+maxX)*0.5, Cy=(minY+maxY)*0.5, Cz=(minZ+maxZ)*0.5;

        // Build directed-edge adjacency: (u->v) maps to list of triangle indices that contain edge u->v
        Map<Long, List<Integer>> edgeDir = new HashMap<>(faces.size()*3);
        for (int ti=0; ti<faces.size(); ti++) {
            int[] f = faces.get(ti);
            addEdge(edgeDir, f[0], f[1], ti);
            addEdge(edgeDir, f[1], f[2], ti);
            addEdge(edgeDir, f[2], f[0], ti);
        }

        // Copy faces we will mutate (flip) locally
        List<int[]> out = new ArrayList<>(faces.size());
        for (int[] f : faces) out.add(new int[]{f[0],f[1],f[2]});

        boolean[] seen = new boolean[out.size()];
        for (int seed=0; seed<out.size(); seed++) if (!seen[seed]) {
            // BFS per connected component; enforce consistency across shared edges
            Deque<Integer> dq = new ArrayDeque<>();
            dq.add(seed); seen[seed]=true;

            List<Integer> comp = new ArrayList<>();
            comp.add(seed);

            while (!dq.isEmpty()) {
                int a = dq.removeFirst();
                int[] fa = out.get(a);

                // for each directed edge in fa, find neighbors that share the *opposite* directed edge
                int[][] edges = {{fa[0],fa[1]}, {fa[1],fa[2]}, {fa[2],fa[0]}};
                for (int[] e : edges) {
                    int u=e[0], v=e[1];

                    // neighbors that have edge v->u (opposite direction)
                    List<Integer> neigh = edgeDir.get(edgeKey(v,u));
                    if (neigh == null) continue;
                    for (int b : neigh) {
                        if (b == a || seen[b]) continue;
                        int[] fb = out.get(b);

                        // fb currently has v->u somewhere; we want u->v in fb to match fa's orientation.
                        // If fb has edge u->v instead, fb orientation matches fa along this edge ⇒ flip fb.
                        if (hasDirectedEdge(fb, u, v)) {
                            // flip b
                            out.set(b, new int[]{fb[0], fb[2], fb[1]});
                            fb = out.get(b);
                        }
                        // now fb should contain directed edge v->u
                        seen[b] = true;
                        dq.add(b);
                        comp.add(b);
                    }
                }
            }

            // Decide global flip for this component so avg normal points away from AABB center
            double ax=0, ay=0, az=0, nx=0, ny=0, nz=0;
            for (int ti : comp) {
                int[] f = out.get(ti);
                Point3D A=pts.get(f[0]), B=pts.get(f[1]), C=pts.get(f[2]);
                double cx=(A.getX()+B.getX()+C.getX())/3.0;
                double cy=(A.getY()+B.getY()+C.getY())/3.0;
                double cz=(A.getZ()+B.getZ()+C.getZ())/3.0;
                ax += (cx-Cx); ay += (cy-Cy); az += (cz-Cz);
                double ux=B.getX()-A.getX(), uy=B.getY()-A.getY(), uz=B.getZ()-A.getZ();
                double vx=C.getX()-A.getX(), vy=C.getY()-A.getY(), vz=C.getZ()-A.getZ();
                nx += uy*vz - uz*vy;
                ny += uz*vx - ux*vz;
                nz += ux*vy - uy*vx;
            }
            double dot = nx*ax + ny*ay + nz*az;
            if (dot < 0) {
                // flip all faces in component
                for (int ti : comp) {
                    int[] f = out.get(ti);
                    out.set(ti, new int[]{f[0], f[2], f[1]});
                }
            }
        }
        return out;
    }

    private static void addEdge(Map<Long, List<Integer>> map, int u, int v, int tri) {
        long k = (((long)u)<<32) | (v & 0xffffffffL);
        map.computeIfAbsent(k, s->new ArrayList<>()).add(tri);
    }
    private static boolean hasDirectedEdge(int[] f, int u, int v) {
        return (f[0]==u && f[1]==v) || (f[1]==u && f[2]==v) || (f[2]==u && f[0]==v);
    }
    // Add inside ConsistentWinding3D (as a private static helper)
    private static long edgeKey(int u, int v) {
        // directed edge key (u -> v)
        return (((long) u) << 32) | (v & 0xffffffffL);
    }
}