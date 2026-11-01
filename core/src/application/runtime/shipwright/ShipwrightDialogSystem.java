package application.runtime.shipwright;

import application.bootstrap.menupipeline.element.ElementInstance;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwrightmanager.ShipwrightManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipwrightDialogSystem extends SystemPackage {

    /*
     * The drafting table's dialogs, one at a time over the table, whose own
     * menus are hidden and which takes no other input while one is open. Open shows every ship kept where the
     * table writes, one row each, to pick from. Starting a new ship, opening
     * another or closing the table — walking away in the game, closing the
     * tab in the editor — goes ahead at once over saved work; over unsaved
     * work it asks first: save it and go on, go on without saving, or stay.
     * A save that cannot be made says why in a notice, and the ship stays on
     * the sheet. The pause binding answers any dialog as staying. What closing
     * means is the table's to say: in the game it walks away, in the editor
     * the tab sets its own.
     */

    // Internal
    private ShipwrightSystem shipwrightSystem;
    private ShipwrightManager shipwrightManager;
    private MenuManager menuManager;
    private InputManager inputManager;

    // Dialog — the one open, null while none is
    private MenuInstance dialogMenu;

    // Leave — what the open question would go on to do
    private ShipwrightLeave pendingLeave;
    private String pendingShipName;

    // Close — what closing the table does
    private Runnable closeAction;

    // Base \\

    @Override
    protected void create() {

        // Leave
        this.pendingShipName = EngineSetting.SHIP_NAME_NONE;
    }

    @Override
    protected void get() {
        this.shipwrightSystem = get(ShipwrightSystem.class);
        this.shipwrightManager = get(ShipwrightManager.class);
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
    }

    @Override
    protected void awake() {

        // Close
        this.closeAction = shipwrightSystem::close;
    }

    // Sets what closing the table does, in place of walking away from it
    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    // Update \\

    @Override
    protected void update() {

        if (isOpen() && inputManager.bindingClicked(KeyBindings.PAUSE, context.getWindow()))
            closeDialog();
    }

    // Leave \\

    // Starts a new ship, opens a kept one or closes the table, asking first over unsaved work
    public void requestLeave(ShipwrightLeave leave, String shipName) {

        if (!shipwrightSystem.isOpen())
            return;

        if (!shipwrightSystem.getSession().isDirty()) {
            leave(leave, shipName);
            return;
        }

        this.pendingLeave = leave;
        this.pendingShipName = shipName;
        openConfirm();
    }

    // Whether the table may close now; over unsaved work it asks instead, and closes itself once answered
    public boolean allowClose() {

        if (!shipwrightSystem.isOpen() || !shipwrightSystem.getSession().isDirty())
            return true;

        requestLeave(ShipwrightLeave.CLOSE, EngineSetting.SHIP_NAME_NONE);

        return false;
    }

    private void leave(ShipwrightLeave leave, String shipName) {

        ShipwrightSessionInstance session = shipwrightSystem.getSession();

        switch (leave) {
            case NEW -> shipwrightManager.newShip(session);
            case OPEN -> shipwrightManager.openShip(session, shipName);
            case CLOSE -> closeAction.run();
        }
    }

    // Save \\

    // Saves the ship on the sheet, showing why in a notice when it cannot be saved; true once it is saved
    public boolean save() {

        ShipwrightSessionInstance session = shipwrightSystem.getSession();

        if (shipwrightManager.saveShip(session))
            return true;

        openNotice(session.getStatus());

        return false;
    }

    // Answers \\

    // Saves, then goes on with what was asked once the save is made
    public void confirmSave() {

        closeDialog();

        if (save())
            leave(pendingLeave, pendingShipName);
    }

    public void confirmDiscard() {
        closeDialog();
        leave(pendingLeave, pendingShipName);
    }

    // Ships \\

    // Every ship kept where the table writes, one row each
    public void openShipList() {

        if (!openDialog(EngineSetting.SHIPWRIGHT_MENU_SHIPS))
            return;

        ObjectArrayList<String> shipNames = shipwrightManager.listShips(shipwrightSystem.getSession());

        if (shipNames.isEmpty()) {
            injectShipRow(EngineSetting.SHIPWRIGHT_STATUS_NONE_KEPT, EngineSetting.SHIP_NAME_NONE);
            return;
        }

        for (int i = 0; i < shipNames.size(); i++)
            injectShipRow(shipNames.get(i), shipNames.get(i));
    }

    private void injectShipRow(String label, String shipName) {
        menuManager.inject(dialogMenu, EngineSetting.SHIPWRIGHT_ENTRY_SHIP_LIST,
                EngineSetting.SHIPWRIGHT_TEMPLATE_SHIP_ROW, element -> {
                    element.setActionArgOverride(shipName);
                    element.findChildById(EngineSetting.SHIPWRIGHT_ELEMENT_SHIP_ROW_LABEL).setFontText(label);
                });
    }

    // Opens a ship picked from the list, asking first over unsaved work
    public void pickShip(String shipName) {

        closeDialog();

        if (!shipName.isEmpty())
            requestLeave(ShipwrightLeave.OPEN, shipName);
    }

    // Dialogs \\

    private void openConfirm() {

        if (!openDialog(EngineSetting.SHIPWRIGHT_MENU_CONFIRM))
            return;

        String displayName = shipwrightSystem.getSession().getBlueprint().getDisplayName();

        setMessage((displayName.isEmpty() ? EngineSetting.SHIPWRIGHT_NAME_UNNAMED_SHIP : displayName)
                + EngineSetting.SHIPWRIGHT_TEXT_UNSAVED_CHANGES);
    }

    private void openNotice(String message) {

        if (openDialog(EngineSetting.SHIPWRIGHT_MENU_NOTICE))
            setMessage(message);
    }

    // Opens a dialog over the table in place of any open one, false when no table is open
    private boolean openDialog(String menuName) {

        if (!shipwrightSystem.isOpen())
            return false;

        closeDialog();
        shipwrightSystem.setMenusShown(false);
        this.dialogMenu = menuManager.openMenu(menuName, context.getWindow());

        return true;
    }

    private void setMessage(String message) {

        ElementInstance label = dialogMenu.getEntryPoint(EngineSetting.SHIPWRIGHT_ENTRY_DIALOG_MESSAGE);

        if (label != null)
            label.setFontText(message);
    }

    // Closes the open dialog, answering a question as staying
    public void closeDialog() {

        if (dialogMenu == null)
            return;

        this.dialogMenu = menuManager.closeMenu(dialogMenu);
        shipwrightSystem.setMenusShown(true);
    }

    // Accessible \\

    public boolean isOpen() {
        return dialogMenu != null;
    }
}
