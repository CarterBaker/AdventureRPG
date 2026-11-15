package editor.bootstrap.charactereditorpipeline.util;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.RiggedMeshStruct;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.EngineUtility;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class CharacterUnwrapUtility extends EngineUtility {

    /*
     * Least squares conformal mapping (LSCM, Lévy et al. 2002), the unwrap
     * Blender, Maya and RizomUV start from: an island of quads, each split
     * into two triangles, is laid flat so every triangle keeps its angles
     * as closely as the least squares allow, the Cauchy-Riemann conditions
     * of each triangle weighted by its area. Two vertices are pinned so the
     * answer is unique: the island's pinned corners where at least two
     * distinct vertices are pinned, otherwise the two vertices farthest
     * apart along the island's longest side, laid that far apart. The
     * normal equations are solved by conjugate gradients with a Jacobi
     * preconditioner. Every corner of the island's faces takes the UV of
     * its vertex, so the island stays joined; seams cut islands apart
     * before they reach here.
     */

    // Unwrap \\

    // Lays one island flat, writing every corner of its faces; false when it has no area or cannot be solved
    public static boolean unwrap(RiggedMeshStruct mesh, IntArrayList faces) {

        Int2IntOpenHashMap vertex2Local = new Int2IntOpenHashMap();
        IntArrayList vertices = new IntArrayList();

        vertex2Local.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);

        for (int i = 0; i < faces.size(); i++)
            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int vertexIndex = mesh.getFaceVertex(faces.getInt(i), c);

                if (vertex2Local.get(vertexIndex) == EngineSetting.INDEX_NOT_FOUND) {
                    vertex2Local.put(vertexIndex, vertices.size());
                    vertices.add(vertexIndex);
                }
            }

        int count = vertices.size();
        double[] solved = new double[count * 2];
        boolean[] pinned = new boolean[count];

        if (count < EngineSetting.QUAD_VERTEX_COUNT - 1
                || !resolvePins(mesh, faces, vertices, vertex2Local, solved, pinned))
            return false;

        if (!solve(mesh, faces, vertex2Local, solved, pinned))
            return false;

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int local = vertex2Local.get(mesh.getFaceVertex(faceIndex, c));
                mesh.setFaceUV(faceIndex, c, (float) solved[local * 2], (float) solved[local * 2 + 1]);
            }
        }

        return true;
    }

    // Pins \\

    // The island's own pinned corners where two distinct vertices are pinned, else its two farthest vertices
    private static boolean resolvePins(
            RiggedMeshStruct mesh,
            IntArrayList faces,
            IntArrayList vertices,
            Int2IntOpenHashMap vertex2Local,
            double[] solved,
            boolean[] pinned) {

        int pinnedCount = 0;

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int c = 0; c < EngineSetting.QUAD_VERTEX_COUNT; c++) {

                int local = vertex2Local.get(mesh.getFaceVertex(faceIndex, c));

                if (!mesh.isCornerPinned(faceIndex, c) || pinned[local])
                    continue;

                pinned[local] = true;
                solved[local * 2] = mesh.getFaceU(faceIndex, c);
                solved[local * 2 + 1] = mesh.getFaceV(faceIndex, c);
                pinnedCount++;
            }
        }

        if (pinnedCount >= 2)
            return true;

        Arrays.fill(pinned, false);
        Arrays.fill(solved, 0.0);

        return pinFarthest(mesh, vertices, solved, pinned);
    }

    // The vertices lowest and highest along the island's longest side, laid on the u axis as far apart as they stand
    private static boolean pinFarthest(
            RiggedMeshStruct mesh,
            IntArrayList vertices,
            double[] solved,
            boolean[] pinned) {

        float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
        float[] max = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

        for (int i = 0; i < vertices.size(); i++)
            for (int axis = 0; axis < min.length; axis++) {
                min[axis] = Math.min(min[axis], mesh.getAxis(vertices.getInt(i), axis));
                max[axis] = Math.max(max[axis], mesh.getAxis(vertices.getInt(i), axis));
            }

        int longest = 0;

        for (int axis = 1; axis < min.length; axis++)
            if (max[axis] - min[axis] > max[longest] - min[longest])
                longest = axis;

        int low = 0;
        int high = 0;

        for (int i = 1; i < vertices.size(); i++) {

            if (mesh.getAxis(vertices.getInt(i), longest) < mesh.getAxis(vertices.getInt(low), longest))
                low = i;

            if (mesh.getAxis(vertices.getInt(i), longest) > mesh.getAxis(vertices.getInt(high), longest))
                high = i;
        }

        double distance = Math.sqrt(distanceSquared(mesh, vertices.getInt(low), vertices.getInt(high)));

        if (low == high || distance <= EditorSetting.UV_UNWRAP_EPSILON)
            return false;

        int lowLocal = low;
        int highLocal = high;

        pinned[lowLocal] = true;
        pinned[highLocal] = true;
        solved[highLocal * 2] = distance;

        return true;
    }

    // Solve \\

    // The normal equations of every triangle's conformality rows, pinned vertices moved to the right-hand side
    private static boolean solve(
            RiggedMeshStruct mesh,
            IntArrayList faces,
            Int2IntOpenHashMap vertex2Local,
            double[] solved,
            boolean[] pinned) {

        int unknowns = solved.length;
        int[] free2Unknown = new int[unknowns];
        int[] unknown2Free = new int[unknowns];
        int freeCount = 0;

        for (int u = 0; u < unknowns; u++) {

            if (pinned[u / 2]) {
                unknown2Free[u] = EngineSetting.INDEX_NOT_FOUND;
                continue;
            }

            unknown2Free[u] = freeCount;
            free2Unknown[freeCount++] = u;
        }

        Int2DoubleOpenHashMap[] rows = new Int2DoubleOpenHashMap[freeCount];
        double[] rhs = new double[freeCount];
        int[] rowVariables = new int[EditorSetting.UV_UNWRAP_ROW_TERMS];
        double[] rowCoefficients = new double[EditorSetting.UV_UNWRAP_ROW_TERMS];
        double[] triangle = new double[EditorSetting.UV_UNWRAP_TRIANGLE_TERMS];
        int[] locals = new int[EditorSetting.UV_UNWRAP_TRIANGLE_CORNERS];
        boolean anyArea = false;

        for (int f = 0; f < freeCount; f++)
            rows[f] = new Int2DoubleOpenHashMap();

        for (int i = 0; i < faces.size(); i++) {

            int faceIndex = faces.getInt(i);

            for (int t = 0; t < 2; t++) {

                if (!resolveTriangle(mesh, faceIndex, t, triangle))
                    continue;

                anyArea = true;
                locals[0] = vertex2Local.get(mesh.getFaceVertex(faceIndex, 0));
                locals[1] = vertex2Local.get(mesh.getFaceVertex(faceIndex, t + 1));
                locals[2] = vertex2Local.get(mesh.getFaceVertex(faceIndex, t + 2));

                for (int row = 0; row < 2; row++) {

                    for (int k = 0; k < locals.length; k++) {

                        double gradientX = triangle[k * 2];
                        double gradientY = triangle[k * 2 + 1];
                        double weight = triangle[EditorSetting.UV_UNWRAP_TRIANGLE_TERMS - 1];

                        rowVariables[k * 2] = locals[k] * 2;
                        rowVariables[k * 2 + 1] = locals[k] * 2 + 1;
                        rowCoefficients[k * 2] = (row == 0 ? gradientX : gradientY) * weight;
                        rowCoefficients[k * 2 + 1] = (row == 0 ? -gradientY : gradientX) * weight;
                    }

                    accumulate(rowVariables, rowCoefficients, unknown2Free, solved, rows, rhs);
                }
            }
        }

        if (!anyArea)
            return false;

        double[] answer = conjugateGradient(rows, rhs);

        if (answer == null)
            return false;

        for (int f = 0; f < freeCount; f++)
            solved[free2Unknown[f]] = answer[f];

        return true;
    }

    // One triangle of a quad in its own plane: each corner's barycentric gradient, then the square root of its area;
    // false for a triangle with no area
    private static boolean resolveTriangle(RiggedMeshStruct mesh, int faceIndex, int half, double[] triangle) {

        int a = mesh.getFaceVertex(faceIndex, 0);
        int b = mesh.getFaceVertex(faceIndex, half + 1);
        int c = mesh.getFaceVertex(faceIndex, half + 2);

        double abx = mesh.getX(b) - mesh.getX(a);
        double aby = mesh.getY(b) - mesh.getY(a);
        double abz = mesh.getZ(b) - mesh.getZ(a);
        double acx = mesh.getX(c) - mesh.getX(a);
        double acy = mesh.getY(c) - mesh.getY(a);
        double acz = mesh.getZ(c) - mesh.getZ(a);
        double abLength = Math.sqrt(abx * abx + aby * aby + abz * abz);
        double nx = aby * acz - abz * acy;
        double ny = abz * acx - abx * acz;
        double nz = abx * acy - aby * acx;
        double twiceArea = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (abLength <= EditorSetting.UV_UNWRAP_EPSILON || twiceArea <= EditorSetting.UV_UNWRAP_EPSILON)
            return false;

        // The triangle laid in its plane: a at the origin, b along x, c above it
        double cx = (acx * abx + acy * aby + acz * abz) / abLength;
        double cy = twiceArea / abLength;
        double[] x = { 0.0, abLength, cx };
        double[] y = { 0.0, 0.0, cy };

        for (int k = 0; k < EditorSetting.UV_UNWRAP_TRIANGLE_CORNERS; k++) {

            int next = (k + 1) % EditorSetting.UV_UNWRAP_TRIANGLE_CORNERS;
            int last = (k + 2) % EditorSetting.UV_UNWRAP_TRIANGLE_CORNERS;

            triangle[k * 2] = (y[next] - y[last]) / twiceArea;
            triangle[k * 2 + 1] = (x[last] - x[next]) / twiceArea;
        }

        triangle[EditorSetting.UV_UNWRAP_TRIANGLE_TERMS - 1] = Math.sqrt(twiceArea * 0.5);
        return true;
    }

    // A row's outer product added to the normal matrix over the free unknowns, its pinned terms moved across
    private static void accumulate(
            int[] variables,
            double[] coefficients,
            int[] unknown2Free,
            double[] solved,
            Int2DoubleOpenHashMap[] rows,
            double[] rhs) {

        for (int p = 0; p < variables.length; p++) {

            int freeP = unknown2Free[variables[p]];

            if (freeP == EngineSetting.INDEX_NOT_FOUND)
                continue;

            for (int q = 0; q < variables.length; q++) {

                double product = coefficients[p] * coefficients[q];
                int freeQ = unknown2Free[variables[q]];

                if (freeQ == EngineSetting.INDEX_NOT_FOUND)
                    rhs[freeP] -= product * solved[variables[q]];
                else
                    rows[freeP].addTo(freeQ, product);
            }
        }
    }

    // The symmetric system solved by preconditioned conjugate gradients; null when it does not settle
    private static double[] conjugateGradient(Int2DoubleOpenHashMap[] rows, double[] rhs) {

        int size = rhs.length;
        int[][] columns = new int[size][];
        double[][] values = new double[size][];
        double[] inverseDiagonal = new double[size];

        for (int r = 0; r < size; r++) {

            columns[r] = rows[r].keySet().toIntArray();
            values[r] = new double[columns[r].length];

            for (int k = 0; k < columns[r].length; k++)
                values[r][k] = rows[r].get(columns[r][k]);

            double diagonal = rows[r].get(r);
            inverseDiagonal[r] = diagonal > 0.0 ? 1.0 / diagonal : 1.0;
        }

        double[] x = new double[size];
        double[] residual = rhs.clone();
        double[] preconditioned = new double[size];
        double[] direction = new double[size];
        double[] product = new double[size];
        double rhsNorm = Math.sqrt(dot(rhs, rhs));

        if (rhsNorm == 0.0)
            return x;

        for (int r = 0; r < size; r++)
            preconditioned[r] = residual[r] * inverseDiagonal[r];

        System.arraycopy(preconditioned, 0, direction, 0, size);

        double rho = dot(residual, preconditioned);
        int limit = Math.max(
                EditorSetting.UV_UNWRAP_MIN_ITERATIONS,
                size * EditorSetting.UV_UNWRAP_ITERATIONS_PER_UNKNOWN);

        for (int iteration = 0; iteration < limit; iteration++) {

            multiply(columns, values, direction, product);

            double curvature = dot(direction, product);

            if (!(curvature > 0.0))
                return null;

            double step = rho / curvature;

            for (int r = 0; r < size; r++) {
                x[r] += step * direction[r];
                residual[r] -= step * product[r];
            }

            if (Math.sqrt(dot(residual, residual)) <= EditorSetting.UV_UNWRAP_TOLERANCE * rhsNorm)
                return x;

            for (int r = 0; r < size; r++)
                preconditioned[r] = residual[r] * inverseDiagonal[r];

            double nextRho = dot(residual, preconditioned);
            double ratio = nextRho / rho;

            for (int r = 0; r < size; r++)
                direction[r] = preconditioned[r] + ratio * direction[r];

            rho = nextRho;
        }

        return Double.isFinite(dot(x, x)) ? x : null;
    }

    private static void multiply(int[][] columns, double[][] values, double[] vector, double[] out) {

        for (int r = 0; r < columns.length; r++) {

            double sum = 0.0;

            for (int k = 0; k < columns[r].length; k++)
                sum += values[r][k] * vector[columns[r][k]];

            out[r] = sum;
        }
    }

    // Utility \\

    private static double dot(double[] a, double[] b) {

        double sum = 0.0;

        for (int i = 0; i < a.length; i++)
            sum += a[i] * b[i];

        return sum;
    }

    private static double distanceSquared(RiggedMeshStruct mesh, int a, int b) {

        double dx = mesh.getX(b) - mesh.getX(a);
        double dy = mesh.getY(b) - mesh.getY(a);
        double dz = mesh.getZ(b) - mesh.getZ(a);

        return dx * dx + dy * dy + dz * dz;
    }
}
