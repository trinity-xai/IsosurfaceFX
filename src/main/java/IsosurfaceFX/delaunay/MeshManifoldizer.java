package IsosurfaceFX.delaunay;

import javafx.geometry.Point3D;
import java.util.*;

/**
 * Enforces a 2-manifold triangle surface:
 *  1) Build undirected edge -> incident tri list
 *  2) Iteratively remove any triangle that touches a "bad" edge (edge degree != 2)
 *  3) Keep only the largest connected component
 *  4) Return faces; points are not modified (indices remain valid)
 *
 * Assumes faces reference the provided points list.
 */
public final class MeshManifoldizer {
    private MeshManifoldizer(){}

    public static List<int[]> enforceTwoManifold(List<Point3D> pts, List<int[]> faces) {
        if (faces.isEmpty()) return faces;

        // Work list of faces (copy because we'll remove)
        ArrayList<int[]> F = new ArrayList<>(faces);

        // Rebuild until stable: remove any tri that touches an edge with degree != 2
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            // edge -> incident faces
            HashMap<Long, ArrayList<Integer>> edgeFaces = new HashMap<>(F.size()*2);
            for (int fi = 0; fi < F.size(); fi++) {
                int[] t = F.get(fi);
                add(edgeFaces, t[0], t[1], fi);
                add(edgeFaces, t[1], t[2], fi);
                add(edgeFaces, t[2], t[0], fi);
            }
            // mark faces touching any bad edge
            boolean[] drop = new boolean[F.size()];
            int badEdges = 0;
            for (var e : edgeFaces.entrySet()) {
                ArrayList<Integer> lst = e.getValue();
                int deg = lst.size();
                if (deg != 2) {
                    badEdges++;
                    for (int fi : lst) if (fi >= 0 && fi < drop.length) drop[fi] = true;
                }
            }
            if (badEdges > 0) {
                ArrayList<int[]> kept = new ArrayList<>(F.size());
                for (int i = 0; i < F.size(); i++) if (!drop[i]) kept.add(F.get(i));
                changed = kept.size() != F.size();
                F = kept;
            }
        } while (changed && ++guard < 8); // small fixed-point loop

        // If everything vanished, return empty
        if (F.isEmpty()) return F;

        // Keep only the largest connected component (triangle adjacency by shared edge)
        List<List<Integer>> comps = componentsByEdge(F);
        if (comps.size() <= 1) return F;

        int bestIdx = 0, bestSize = 0;
        for (int i = 0; i < comps.size(); i++) {
            int sz = comps.get(i).size();
            if (sz > bestSize) { bestSize = sz; bestIdx = i; }
        }
        HashSet<Integer> keepSet = new HashSet<>(comps.get(bestIdx));
        ArrayList<int[]> out = new ArrayList<>(bestSize);
        for (int i = 0; i < F.size(); i++) if (keepSet.contains(i)) out.add(F.get(i));
        return out;
    }

    private static void add(HashMap<Long, ArrayList<Integer>> map, int a, int b, int fi) {
        long k = edgeKey(a, b);
        map.computeIfAbsent(k, s -> new ArrayList<>()).add(fi);
    }
    private static long edgeKey(int a, int b) {
        int i = Math.min(a, b), j = Math.max(a, b);
        return (((long) i) << 32) | (j & 0xffffffffL);
    }

    private static List<List<Integer>> componentsByEdge(List<int[]> faces) {
        // Build tri adjacency via shared edges
        HashMap<Long, ArrayList<Integer>> edgeFaces = new HashMap<>(faces.size()*2);
        for (int fi = 0; fi < faces.size(); fi++) {
            int[] t = faces.get(fi);
            add(edgeFaces, t[0], t[1], fi);
            add(edgeFaces, t[1], t[2], fi);
            add(edgeFaces, t[2], t[0], fi);
        }
        ArrayList<HashSet<Integer>> adj = new ArrayList<>(faces.size());
        for (int i = 0; i < faces.size(); i++) 
            adj.add(new HashSet<>());
        for (var e : edgeFaces.entrySet()) {
            ArrayList<Integer> lst = e.getValue();
            for (int i = 0; i < lst.size(); i++) for (int j = i+1; j < lst.size(); j++) {
                int a = lst.get(i), b = lst.get(j);
                adj.get(a).add(b);
                adj.get(b).add(a);
            }
        }
        // BFS components
        boolean[] seen = new boolean[faces.size()];
        ArrayList<List<Integer>> comps = new ArrayList<>();
        for (int i = 0; i < faces.size(); i++) if (!seen[i]) {
            ArrayList<Integer> comp = new ArrayList<>();
            Deque<Integer> dq = new ArrayDeque<>();
            dq.add(i); seen[i] = true;
            while (!dq.isEmpty()) {
                int u = dq.removeFirst();
                comp.add(u);
                for (int v : adj.get(u)) if (!seen[v]) { seen[v]=true; dq.add(v); }
            }
            comps.add(comp);
        }
        return comps;
    }
}
