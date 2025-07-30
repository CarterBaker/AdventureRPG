package com.AdventureRPG.UISystem;

public class LoadScreen extends MenuType {

    private int maxProgress;
    private int totalProgress;

    private boolean debug = true;

    public LoadScreen(UISystem UISystem, Menu Menu) {
        super(UISystem, Menu);
    }

    public void SetMaxProgrss(int input) {
        this.maxProgress = input;
    }

    public void IncreaseProgrss(int input) {
        this.totalProgress += input;

        if (this.totalProgress >= this.maxProgress) {

            this.totalProgress = this.maxProgress;

            if (debug) {
                printProgress();
                System.out.println();
            }

            return;
        }

        if (debug)
            printProgress();
    }

    private void printProgress() {
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

        if (debug)
            System.out.println("Load Screen opened.");
    }

    @Override
    public void Close() {

        // Tear down or hide UI components

        if (debug)
            System.out.println("Load Screen closed.                         ");
    }

}
