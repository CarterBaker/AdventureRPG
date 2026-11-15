package application.runtime.menueventsmanager.menus.dialogue;

import java.util.concurrent.ThreadLocalRandom;

import application.bootstrap.dialoguepipeline.dialoguemanager.DialogueManager;
import application.bootstrap.dialoguepipeline.util.DialogueExchangeStruct;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.npc.NpcInstance;
import application.bootstrap.entitypipeline.npc.NpcMind;
import application.bootstrap.entitypipeline.npcmanager.NpcManager;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.menupipeline.menu.MenuInstance;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.runtime.RuntimeSetting;
import engine.input.Input;
import engine.input.InputListener;
import engine.input.Keys;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.settings.KeyBindings;

public class DialogueBranch extends BranchPackage implements InputListener {

    /*
     * Holds this context's talk between its player and a resident. The
     * activate binding opens it on a calm resident the player faces within
     * reach, which then stands and attends to the player. The player types
     * what it says — every character the font can draw, Backspace taking the
     * last one back — and Enter says it: DialogueManager holds the turn, the
     * resident's answer is shown with the line it answers, and residents in
     * earshot hear how it moved the resident's regard. Nothing earlier is
     * kept or shown; what was said before must be remembered. A reply that
     * ends the talk — the resident leaving, turning on the player or running
     * — is acted on at once and its words linger a moment before the menu
     * closes; the pause binding closes it sooner, and it closes by itself if
     * the resident falls, leaves the world or stops being calm. Typed
     * characters are read from whichever OS window this context lives in.
     */

    // Internal
    private MenuManager menuManager;
    private InputManager inputManager;
    private PlayerManager playerManager;
    private NpcManager npcManager;
    private DialogueManager dialogueManager;

    // Talk
    private DialogueExchangeStruct exchange;
    private NpcInstance partner;
    private MenuInstance talkMenu;
    private boolean closeArmed;
    private boolean ended;
    private float endSeconds;

    // Typing
    private Input textInput;
    private StringBuilder typed;
    private StringBuilder lineScratch;
    private float caretElapsed;
    private boolean caretShown;

    // Base \\

    @Override
    protected void create() {

        // Typing
        this.typed = new StringBuilder();
        this.lineScratch = new StringBuilder();
    }

    @Override
    protected void get() {

        // Internal
        this.menuManager = get(MenuManager.class);
        this.inputManager = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
        this.npcManager = get(NpcManager.class);
        this.dialogueManager = get(DialogueManager.class);
    }

    @Override
    protected void awake() {

        // Talk
        this.exchange = dialogueManager.createExchange();
    }

    @Override
    protected void update() {

        WindowInstance window = context.getWindow();

        bindTextInput(window);

        if (talkMenu == null) {

            if (inputManager.bindingClicked(KeyBindings.ACTIVATE, window))
                openMenu(window, findPartner(window));

            return;
        }

        boolean closePressed = inputManager.bindingClicked(KeyBindings.PAUSE, window) && closeArmed;

        if (closePressed || !isPartnerPresent() || isEndOver()) {
            closeMenu();
            return;
        }

        if (!ended)
            updateTyping();

        this.closeArmed = talkMenu.isVisible();
    }

    @Override
    protected void dispose() {

        releaseTextInput();

        if (partner != null)
            npcManager.endTalk(partner);
    }

    // Partner \\

    // The calm resident the window's player faces within reach of a word, none when it faces no such one or a
    // menu already holds its input
    public NpcInstance findPartner(WindowInstance window) {

        int windowID = window.getWindowID();

        if (talkMenu != null || !playerManager.isPlayingForWindow(windowID)
                || window.getMenuListHandle().isInputLocked())
            return null;

        EntityInstance faced = playerManager.findFacedEntityForWindow(windowID, RuntimeSetting.DIALOGUE_REACH_BLOCKS);
        NpcInstance resident = faced != null ? npcManager.findResident(faced) : null;

        return resident != null && !resident.isFallen() && resident.getMind() == NpcMind.CALM ? resident : null;
    }

    // True while the resident is still in the world and, until the talk has ended, still calm
    private boolean isPartnerPresent() {
        return npcManager.findResident(partner.getEntityInstance()) == partner
                && !partner.isFallen()
                && (ended || partner.getMind() == NpcMind.CALM);
    }

    // Open / Close \\

    private void openMenu(WindowInstance window, NpcInstance resident) {

        if (resident == null)
            return;

        this.partner = resident;
        this.talkMenu = menuManager.openMenu(RuntimeSetting.MENU_DIALOGUE, window);
        this.closeArmed = false;
        this.ended = false;
        this.caretElapsed = 0f;
        this.caretShown = true;

        typed.setLength(0);
        exchange.begin(ThreadLocalRandom.current().nextLong());
        npcManager.beginTalk(resident, playerManager.getPlayerForWindow(window.getWindowID()));

        setText(RuntimeSetting.ENTRY_DIALOGUE_HEARD, EngineSetting.DIALOGUE_TEXT_NONE);
        showSaid(EngineSetting.DIALOGUE_TEXT_NONE);
        refreshTyped();
    }

    public void closeMenu() {

        if (talkMenu == null)
            return;

        npcManager.endTalk(partner);
        this.partner = null;
        this.talkMenu = menuManager.closeMenu(talkMenu);
    }

    public boolean isOpen() {
        return talkMenu != null;
    }

    // Typing \\

    private void updateTyping() {

        Input rawInput = textInput;

        if (rawInput.isKeyClicked(Keys.ENTER) || rawInput.isKeyClicked(Keys.NUMPAD_ENTER)) {
            say();
            return;
        }

        if (rawInput.isKeyClicked(Keys.BACKSPACE) && typed.length() > 0) {
            typed.deleteCharAt(typed.length() - 1);
            refreshTyped();
        }

        this.caretElapsed += internal.getDeltaTime();
        boolean shown = (int) (caretElapsed / RuntimeSetting.DIALOGUE_CARET_BLINK_SECONDS) % 2 == 0;

        if (shown == caretShown)
            return;

        this.caretShown = shown;
        refreshTyped();
    }

    @Override
    public void onChar(char character) {

        if (talkMenu == null || ended || !closeArmed || typed.length() >= RuntimeSetting.DIALOGUE_TYPED_MAX_LENGTH
                || EngineSetting.FONT_DEFAULT_CHARSET.indexOf(character) == EngineSetting.INDEX_NOT_FOUND)
            return;

        typed.append(character);
        this.caretElapsed = 0f;
        this.caretShown = true;
        refreshTyped();
    }

    private void bindTextInput(WindowInstance window) {

        Input rawInput = inputManager.getRawInput(window);

        if (rawInput == textInput)
            return;

        releaseTextInput();
        this.textInput = rawInput;

        if (textInput != null)
            textInput.addListener(this);
    }

    private void releaseTextInput() {

        if (textInput == null)
            return;

        textInput.removeListener(this);
        this.textInput = null;
    }

    // Say \\

    // What was typed is said to the resident, its answer is shown and acted on, and those in earshot hear it
    private void say() {

        if (typed.length() == 0)
            return;

        EntityInstance player = playerManager.getPlayerForWindow(talkMenu.getWindowID());
        EntityInstance listener = partner.getEntityInstance();

        exchange.getHeard().setLength(0);
        exchange.getHeard().append(typed);
        dialogueManager.converse(exchange, listener, player);
        npcManager.overhear(player, listener, exchange.getAffinityShift());

        lineScratch.setLength(0);
        lineScratch.append(RuntimeSetting.DIALOGUE_HEARD_PREFIX).append(typed);
        setText(RuntimeSetting.ENTRY_DIALOGUE_HEARD, lineScratch.toString());
        showSaid(exchange.getSaid());

        typed.setLength(0);
        refreshTyped();
        act(player);
    }

    // What the reply does besides being said
    private void act(EntityInstance player) {
        switch (exchange.getAction()) {
            case NONE -> {
            }
            case LEAVE -> end();
            case ATTACK -> {
                npcManager.confront(partner, player);
                end();
            }
            case FLEE -> {
                npcManager.frighten(partner, player);
                end();
            }
        }
    }

    // The talk is over: the resident stops attending, and its last words linger before the menu closes
    private void end() {
        npcManager.endTalk(partner);
        this.ended = true;
        this.endSeconds = RuntimeSetting.DIALOGUE_END_SECONDS;
    }

    private boolean isEndOver() {

        if (!ended)
            return false;

        this.endSeconds -= internal.getDeltaTime();

        return endSeconds <= 0f;
    }

    // Text \\

    // The answer broken at spaces over the menu's lines, whatever is left over kept on the last
    private void showSaid(CharSequence said) {

        int start = 0;

        for (int line = 0; line < RuntimeSetting.DIALOGUE_SAID_LINES; line++) {

            int end = line == RuntimeSetting.DIALOGUE_SAID_LINES - 1 ? said.length() : findBreak(said, start);

            setText(RuntimeSetting.ENTRY_DIALOGUE_SAID + line, said.subSequence(start, end).toString());
            start = Math.min(said.length(), end + 1);
        }
    }

    // Where the line starting at an index ends: the last space within the line's length, or the text's end
    private int findBreak(CharSequence text, int start) {

        if (text.length() - start <= RuntimeSetting.DIALOGUE_LINE_LENGTH)
            return text.length();

        for (int i = start + RuntimeSetting.DIALOGUE_LINE_LENGTH; i > start; i--)
            if (text.charAt(i) == ' ')
                return i;

        return start + RuntimeSetting.DIALOGUE_LINE_LENGTH;
    }

    private void refreshTyped() {

        lineScratch.setLength(0);
        lineScratch.append(RuntimeSetting.DIALOGUE_TYPED_PREFIX).append(typed);

        if (caretShown && !ended)
            lineScratch.append(RuntimeSetting.DIALOGUE_CARET);

        setText(RuntimeSetting.ENTRY_DIALOGUE_TYPED, lineScratch.toString());
    }

    private void setText(int entryPoint, String text) {
        talkMenu.getEntryPoint(entryPoint).setFontText(text);
    }
}
