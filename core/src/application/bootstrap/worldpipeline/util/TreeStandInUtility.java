package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.macrochunk.MacroStandInStruct;
import application.bootstrap.worldpipeline.tree.TreeForm;
import application.bootstrap.worldpipeline.tree.TreeStandIn;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

public final class TreeStandInUtility extends EngineUtility {

    /*
     * Shapes the trees macro terrain stands in one by one, in its flat-colored
     * layout and on the very crown TreeImpostorUtility lays out, so a tree
     * keeps its bulk as it passes from a mega's leaf cards to macro terrain
     * and from one tier of macro terrain to the next. Every shape is a lathe
     * of rings about the tree's upright axis, turned from where its seed
     * starts them: a round lump is a dome of rings each half a side round
     * from the one above, so its facets stagger like foliage, closed by a
     * point above and below; a pointed lump is a cone rising from one ring
     * over a shallow underside; and a trunk is a tapered tube standing in the
     * ground. The farthest tier draws the whole crown as one spire whose foot
     * reaches down to the root, so a tree too far for its trunk to show never
     * floats. Positions are tile-local blocks, and every vertex carries the
     * chunk its tree roots in.
     */

    private static final float TWO_PI = (float) (Math.PI * 2.0);

    // Turn \\

    // How far round a tree's seed starts the rings of every shape it stands as
    public static float resolveTurn(long seed) {
        return BiomeFieldUtility.hash01(seed ^ EngineSetting.TREE_STAND_IN_TURN_SALT) * TWO_PI;
    }

    // Crown \\

    // The crown as one shape — each lump its form lays out, or one spire for the farthest tier
    public static void emitCrown(
            MacroStandInStruct out,
            TreeStandIn standIn,
            TreeForm form,
            float[] crown,
            float[] lumps,
            float rootX,
            float rootY,
            float rootZ,
            float turn,
            int topColor,
            int sideColor,
            float cover) {

        out.beginShape();

        if (standIn == TreeStandIn.SPIRE)
            emitSpire(out, form, crown, rootX, rootY, rootZ, turn, topColor, sideColor, cover);
        else
            emitLumps(out, standIn, form, crown, lumps, rootX, rootY, rootZ, turn, topColor, sideColor, cover);

        out.endShape();
    }

    private static void emitLumps(
            MacroStandInStruct out,
            TreeStandIn standIn,
            TreeForm form,
            float[] crown,
            float[] lumps,
            float rootX,
            float rootY,
            float rootZ,
            float turn,
            int topColor,
            int sideColor,
            float cover) {

        boolean faceted = standIn == TreeStandIn.FACETED;
        int sides = faceted ? EngineSetting.TREE_STAND_IN_FACETED_SIDES : EngineSetting.TREE_STAND_IN_PLAIN_SIDES;
        int count = TreeImpostorUtility.layoutCrown(form, crown, lumps);

        for (int lump = 0; lump < count; lump++) {

            int offset = lump * EngineSetting.TREE_IMPOSTOR_LUMP_FLOATS;
            float centerY = rootY + lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_CENTER_Y];
            float radiusH = lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_H];
            float radiusV = lumps[offset + EngineSetting.TREE_IMPOSTOR_LUMP_RADIUS_V];

            if (isPointed(form))
                emitLathe(
                        out, sides, turn, rootX, centerY, rootZ, radiusH, radiusV,
                        EngineSetting.TREE_STAND_IN_CONE_LEVELS, EngineSetting.TREE_STAND_IN_CONE_REACHES,
                        EngineSetting.TREE_STAND_IN_CONE_TIP, EngineSetting.TREE_STAND_IN_CONE_FOOT,
                        topColor, sideColor, cover);
            else if (faceted)
                emitLathe(
                        out, sides, turn, rootX, centerY, rootZ, radiusH, radiusV,
                        EngineSetting.TREE_STAND_IN_DOME_LEVELS, EngineSetting.TREE_STAND_IN_DOME_REACHES,
                        EngineSetting.TREE_STAND_IN_DOME_TIP, EngineSetting.TREE_STAND_IN_DOME_FOOT,
                        topColor, sideColor, cover);
            else
                emitLathe(
                        out, sides, turn, rootX, centerY, rootZ, radiusH, radiusV,
                        EngineSetting.TREE_STAND_IN_DIAMOND_LEVELS, EngineSetting.TREE_STAND_IN_DIAMOND_REACHES,
                        EngineSetting.TREE_STAND_IN_DIAMOND_TIP, EngineSetting.TREE_STAND_IN_DIAMOND_FOOT,
                        topColor, sideColor, cover);
        }
    }

    // The whole crown as one diamond, or one spire for a pointed form, widest at its waist, its tip at the crown's
    // top and its foot at the root
    private static void emitSpire(
            MacroStandInStruct out,
            TreeForm form,
            float[] crown,
            float rootX,
            float rootY,
            float rootZ,
            float turn,
            int topColor,
            int sideColor,
            float cover) {

        float radius = crown[EngineSetting.TREE_IMPOSTOR_CROWN_RADIUS];

        if (radius <= 0f)
            return;

        float bottom = crown[EngineSetting.TREE_IMPOSTOR_CROWN_BOTTOM];
        float top = crown[EngineSetting.TREE_IMPOSTOR_CROWN_TOP];
        float waist = bottom + (top - bottom) * (isPointed(form)
                ? EngineSetting.TREE_STAND_IN_SPIRE_POINTED_WAIST
                : EngineSetting.TREE_STAND_IN_SPIRE_ROUND_WAIST);

        emitLathe(
                out, EngineSetting.TREE_STAND_IN_PLAIN_SIDES, turn, rootX, rootY + waist, rootZ, radius, 1f,
                EngineSetting.TREE_STAND_IN_DIAMOND_LEVELS, EngineSetting.TREE_STAND_IN_DIAMOND_REACHES,
                top - waist, -waist - EngineSetting.TREE_TRUNK_SINK_BLOCKS,
                topColor, sideColor, cover);
    }

    private static boolean isPointed(TreeForm form) {
        return form == TreeForm.CONIFER || form == TreeForm.COLUMNAR;
    }

    // Trunk \\

    // A tapered tube from below the ground up into the crown — none for the farthest tier, where it cannot show
    public static void emitTrunk(
            MacroStandInStruct out,
            TreeStandIn standIn,
            float rootX,
            float rootY,
            float rootZ,
            float halfWidth,
            float top,
            float turn,
            int topColor,
            int sideColor,
            float cover) {

        if (standIn == TreeStandIn.SPIRE || top <= 0f)
            return;

        int sides = standIn == TreeStandIn.FACETED
                ? EngineSetting.TREE_STAND_IN_FACETED_TRUNK_SIDES
                : EngineSetting.TREE_STAND_IN_PLAIN_TRUNK_SIDES;
        float radius = halfWidth * EngineSetting.TREE_STAND_IN_TRUNK_REACH;

        out.beginShape();

        int upper = emitRing(
                out, sides, turn, rootX, rootY + top, rootZ, radius * EngineSetting.TREE_STAND_IN_TRUNK_TAPER,
                topColor, sideColor, cover);
        int lower = emitRing(
                out, sides, turn, rootX, rootY - EngineSetting.TREE_TRUNK_SINK_BLOCKS, rootZ, radius,
                topColor, sideColor, cover);

        connectRings(out, upper, lower, sides, false);
        out.endShape();
    }

    // Lathe \\

    // Rings about an upright axis at levels and reaches of a half-height and a radius, each half a side round from
    // the one above, closed by a point at the tip above and the foot below
    private static void emitLathe(
            MacroStandInStruct out,
            int sides,
            float turn,
            float centerX,
            float centerY,
            float centerZ,
            float radiusH,
            float radiusV,
            float[] levels,
            float[] reaches,
            float tip,
            float foot,
            int topColor,
            int sideColor,
            float cover) {

        float halfSide = (float) Math.PI / sides;
        int tipVertex = out.addVertex(centerX, centerY + tip * radiusV, centerZ, topColor, sideColor, cover);
        int firstRing = tipVertex + 1;

        for (int ring = 0; ring < levels.length; ring++)
            emitRing(
                    out, sides, turn + ring * halfSide, centerX, centerY + levels[ring] * radiusV, centerZ,
                    reaches[ring] * radiusH, topColor, sideColor, cover);

        int footVertex = out.addVertex(centerX, centerY + foot * radiusV, centerZ, topColor, sideColor, cover);
        int lastRing = firstRing + (levels.length - 1) * sides;

        connectPoint(out, tipVertex, firstRing, sides);

        for (int ring = 0; ring < levels.length - 1; ring++)
            connectRings(out, firstRing + ring * sides, firstRing + (ring + 1) * sides, sides, true);

        connectPoint(out, footVertex, lastRing, sides);
    }

    // One ring of corners about an upright axis — the index of its first
    private static int emitRing(
            MacroStandInStruct out,
            int sides,
            float turn,
            float centerX,
            float y,
            float centerZ,
            float radius,
            int topColor,
            int sideColor,
            float cover) {

        int first = EngineSetting.INDEX_NOT_FOUND;

        for (int side = 0; side < sides; side++) {

            float angle = turn + side * TWO_PI / sides;
            int vertex = out.addVertex(
                    centerX + (float) Math.cos(angle) * radius, y, centerZ + (float) Math.sin(angle) * radius,
                    topColor, sideColor, cover);

            if (side == 0)
                first = vertex;
        }

        return first;
    }

    private static void connectPoint(MacroStandInStruct out, int point, int ring, int sides) {
        for (int side = 0; side < sides; side++)
            out.addTriangle(point, ring + side, ring + (side + 1) % sides);
    }

    // The band between two rings — a staggered lower ring sits half a side round, so each of its corners lies
    // under the middle of an upper edge
    private static void connectRings(MacroStandInStruct out, int upper, int lower, int sides, boolean staggered) {

        for (int side = 0; side < sides; side++) {

            int next = (side + 1) % sides;

            if (staggered) {
                out.addTriangle(upper + side, upper + next, lower + side);
                out.addTriangle(upper + next, lower + next, lower + side);
                continue;
            }

            out.addTriangle(upper + side, upper + next, lower + next);
            out.addTriangle(upper + side, lower + next, lower + side);
        }
    }
}
