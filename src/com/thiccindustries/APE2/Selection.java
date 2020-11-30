package com.thiccindustries.APE2;

public class Selection {
    public int x1,x2,y1,y2;
    public int selectionStage = 0;

    public void CompleteSelection(){
        System.out.println("Attempting Selection: (" + x1 + "," + y1 + ") (" + x2 + "," + y2 + ")");
        if(selectionStage == 2) {
            if(x1 > x2 || y1 > y2){
                System.out.println("Invalid Selection");
                selectionStage = 0;
                ResetSelection();
            }
            selectionStage = 3;
        }
    }

    public void ResetSelection(){
        x1 = 0;
        x2 = 0;
        y1 = 0;
        y2 = 0;

        selectionStage = 0;
    }
}
