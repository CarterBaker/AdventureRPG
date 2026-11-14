package application.bootstrap.worldpipeline.settlementmanager;

import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.settlement.SettlementSiteStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

class SettlementCellStruct extends StructPackage {

    /*
     * Everything settled about one settlement cell of one world, filled in as
     * it is first needed and cached by SettlementCacheBranch: the revision of
     * hand placements and biomes it was settled under, its site, null when
     * no settlement stands there, its plan once planned, and the roads
     * leading east and south to its neighbours once resolved, either null
     * when none runs. Every field past the site is guarded by the cache's
     * lock.
     */

    // Identity
    private final WorldHandle worldHandle;
    private final long revision;

    // Site
    private final SettlementSiteStruct site;

    // Plan
    private SettlementPlanStruct plan;
    private boolean planned;

    // Links — indexed east then south
    private final LayoutPlanStruct[] links;
    private final boolean[] linksResolved;

    // Constructor \\

    SettlementCellStruct(WorldHandle worldHandle, long revision, SettlementSiteStruct site) {

        // Identity
        this.worldHandle = worldHandle;
        this.revision = revision;

        // Site
        this.site = site;

        // Links
        this.links = new LayoutPlanStruct[EngineSetting.SETTLEMENT_LINK_DIRECTIONS];
        this.linksResolved = new boolean[EngineSetting.SETTLEMENT_LINK_DIRECTIONS];
    }

    // Management \\

    void setPlan(SettlementPlanStruct plan) {
        this.plan = plan;
        this.planned = true;
    }

    void setLink(int direction, LayoutPlanStruct link) {
        this.links[direction] = link;
        this.linksResolved[direction] = true;
    }

    // Accessible \\

    WorldHandle getWorldHandle() {
        return worldHandle;
    }

    long getRevision() {
        return revision;
    }

    SettlementSiteStruct getSite() {
        return site;
    }

    SettlementPlanStruct getPlan() {
        return plan;
    }

    boolean isPlanned() {
        return planned;
    }

    LayoutPlanStruct getLink(int direction) {
        return links[direction];
    }

    boolean isLinkResolved(int direction) {
        return linksResolved[direction];
    }
}
