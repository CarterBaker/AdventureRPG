package com.AdventureRPG.UISystem;

public class LoadScreen extends MenuType {

    private boolean debug = false; // TODO: Remove debug line

    private int maxProgress;
    private int totalProgress;

    public LoadScreen(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    public void SetMaxProgrss(int input) {
        this.maxProgress = input;
    }

    public void SetProgrss(int input) {
        this.totalProgress = maxProgress - input;

        if (this.totalProgress >= this.maxProgress) {

            this.totalProgress = this.maxProgress;

            if (debug) { // TODO: Remove debug line
                printProgress();
                System.out.println();
            }

            return;
        }

        if (debug) // TODO: Remove debug line
            printProgress();
    }

    private void printProgress() { // TODO: Remove debug line
        System.out.print("\rLoading progress: " + totalProgress + " / " + maxProgress +
                " (" + getPercentage() + "%)");

        System.out.flush();
    }

    private int getPercentage() {
        if (maxProgress == 0)
            return 100;
        return (int) (((double) totalProgress / maxProgress) * 100);
    }

    @Override
    public void Open() {

        // Build or display UI components
        totalProgress = 0;

        if (debug) // TODO: Remove debug line
            System.out.println("Load Screen opened.");
    }

    @Override
    public void Close() {
        if (debug) { // TODO: Remove debug line
            if (totalProgress < maxProgress)
                System.out.print("!!!\n");

            System.out.println("Load Screen closed.");
        }
    }

}
