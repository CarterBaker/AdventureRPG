package application.bootstrap.worldpipeline.tree;

public enum TreeForm {

    /*
     * How a tree species grows its limbs, named in lower case by tree data.
     * Every form grows from the same trunk; the form decides where limbs set
     * out from it, how their length runs up the crown, and where the leaves
     * gather.
     */

    BROADLEAF, // Limbs fork from the upper trunk into a spreading crown, leaves at the twig tips
    CONIFER, // A leader to the crown, whorls of near-level boughs shortening upward, needles along every bough
    COLUMNAR, // A leader to the crown, short upswept boughs all along it, leaves along every bough
    WEEPING, // A broadleaf crown whose twigs arc down into hanging curtains of leaves
    SHRUB, // Several stems from the ground, short and twiggy, leaves along and at the tips
    PALM // One bending trunk with a crown of arching fronds and no boughs
}
