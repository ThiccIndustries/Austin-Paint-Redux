package com.thiccindustries.APE2;
import com.thiccindustries.APE2.io.FileManager;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.lwjgl.glfw.GLFW;
import java.awt.*;
import java.io.File;
import java.util.LinkedList;

import static com.thiccindustries.APE2.Resources.*;

public class Application {

    private static int[][][] layeredPixelArray = new int[32][32][7]; //x, y, layer
    private static int activeLayer = 0;
    private static int activeColor = 0;

    private static Selection curSelection;
    private static int[][] clipboardBuffer;

    private static boolean drawStacked = true;
    private static boolean debugDraw = false;

    private static boolean safeExit = true; //Save to exit without warning

    private static int lastClickX, lastClickY;

    private static final LinkedList<UndoEntity> UndoStack = new LinkedList<>();

    private static final LinkedList<UndoEntity> RedoStack = new LinkedList<>();


    private static String hexBuffer = "";

    private static Color[] bitmapColors = {
            new Color(0, 0, 0),
            new Color(63,63,63),
            new Color(127,127,127),
            new Color(255,255,255),
            new Color(0,0,255),
            new Color(0,0,127),
            new Color(0,255,0),
            new Color(0,127,0),
            new Color(255,0,0),
            new Color(127,0,0),
            new Color(0,255,255),
            new Color(0,127,127),
            new Color(255,255,0),
            new Color(127,127,0),
            new Color(255,0,255),
            new Color(127,0,127),
    };

    private static Tool toolmode = Tool.pencil;

    public static void main(String[] args){
        //Init array to -1
        for(int layer = 0; layer < 7; layer++) {
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    layeredPixelArray[x][y][layer] = -1;
                }
            }
        }


        if(args.length > 0) {
            System.out.println(args[0]);
            FileManager.APFile apfile = FileManager.loadPixelArrayFromFile(args[0]);
            layeredPixelArray = apfile.pixelArray;
            bitmapColors = apfile.palette;
        }


        Renderer.initGLFWAndCreateWindow(1);

        Resources.InitResources();
        
        curSelection = new Selection();

        while(!Renderer.windowShouldClose()){
            //Update input
            Mouse.Update();
            Keyboard.Update();
            Renderer.pollWindowEvents();

            Actions();

            //Close has been requested by the user
            if(Renderer.windowShouldClose()){
                if(!(safeExit || toolmode == Tool.save_warn)) {
                    toolmode = Tool.save_warn;
                    Renderer.abortWindowClose();
                }
            }

            //Begin rendering
            Renderer.drawBitmapLayers(layeredPixelArray, bitmapColors, drawStacked, activeLayer);
            Renderer.drawBitmapLayersSplit(layeredPixelArray, bitmapColors);
            Renderer.drawSelection(curSelection);

            //Draw palette editing screen, this also occurs if the tool is txt_color
            if(toolmode.getdisplayTool() == Tool.color)
                Renderer.drawColorSelection(bitmapColors, activeColor, toolmode == Tool.txt_color);

            if(toolmode == Tool.save_warn)
                Renderer.drawDialog(new String[]{"Warning: unsaved changes", "press close again", "to exit."}, "Cancel", Tool.pencil);

            //Draws debug screen
            if(debugDraw)
                Renderer.drawDebugInf(toolmode, bitmapColors, activeColor, activeLayer, safeExit);

            Renderer.drawUI(toolmode, bitmapColors, activeColor, activeLayer, Renderer.mouseXPixel < 2);

            Renderer.drawText("Fps: ",(int)(34.5 * Renderer.pixelScale), 32 * Renderer.pixelScale - Renderer.uiScale, 1, false, Color.white, null);
            Renderer.drawFPS((int)(34.5 * Renderer.pixelScale), 33 * Renderer.pixelScale, 1, false, Color.white, null);

            //Finish rendering
            Renderer.completeFrame();
        }

        System.exit(0);
    }

    private static void Actions() {
        //Prevent any actions until file dialog is closed
        if(toolmode == Tool.lock_save || toolmode == Tool.lock_export || toolmode == Tool.lock_open)
            return;

        //Require another mouse click before acting as a pencil (Pencil_wait)
        if(toolmode == Tool.pencil_wait && Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
            toolmode = Tool.pencil;
        }

        //Toolbar
        if(Renderer.mouseXPixel < 2){
            //Select new tool
            if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                int selectedToolOrdinal = (int)Renderer.mouseY / (8 * Renderer.uiScale);

                if(selectedToolOrdinal >= Tool.values().length)
                    selectedToolOrdinal = Tool.values().length - 1;

                if(!Tool.values()[selectedToolOrdinal].display())
                    selectedToolOrdinal = toolmode.ordinal();

                toolmode = Tool.values()[selectedToolOrdinal];
            }
        }

        //Color selector
        if((Renderer.mouseXPixel > 2 && Renderer.mouseXPixel < 34) && (Renderer.mouseYPixel > 31)){
            //Select new color
            if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                activeColor = (Renderer.mouseXPixel - 2) / 2;
            }
        }

        //Layer previews
        if((Renderer.mouseXPixel > 33) && (Renderer.mouseYPixel < 31)){
            //Select layer
            if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                activeLayer = 6 - (int)Renderer.mouseY / (18 * Renderer.uiScale);
            }
        }

        //Flip horiz
        if(toolmode == Tool.mirror_h){
            if (curSelection.selectionStage == 3) {
                int[][] flippedPixelArray = new int[curSelection.x2 - curSelection.x1 + 1][curSelection.y2 - curSelection.y1 + 1];

                clipboardBuffer = resetClipboardBuffer();

                for (int x = 0; x <= curSelection.x2 - curSelection.x1; x++) {
                    for (int y = 0; y <= curSelection.y2 - curSelection.y1; y++) {
                        flippedPixelArray[(curSelection.x2 - curSelection.x1) - x][y] = layeredPixelArray[x + curSelection.x1][y + curSelection.y1][activeLayer];
                    }
                }

                for (int x = curSelection.x1; x < curSelection.x2; x++) {
                    for (int y = curSelection.y1; y < curSelection.y2; y++) {
                        clipboardBuffer[x][y] = flippedPixelArray[x - curSelection.x1][y - curSelection.y1];
                    }
                }

                //Paste
                UndoEntity ue = new UndoEntity();
                UndoStack.addFirst(ue);
                safeExit = false;

                RedoStack.clear();

                for (int x = curSelection.x1; x <= curSelection.x2; x++) {
                    for (int y = curSelection.y1; y < curSelection.y2; y++) {
                        ue.AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                        layeredPixelArray[x][y][activeLayer] = -1;
                    }
                }

                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        if (clipboardBuffer[x][y] == -1)
                            continue;
                        ue.AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                        layeredPixelArray[x][y][activeLayer] = clipboardBuffer[x][y];
                    }
                }
            }
            toolmode = Tool.pencil;
        }

        //Flip vert
        if(toolmode == Tool.mirror_v) {
            if (curSelection.selectionStage != 3) {
                toolmode = Tool.pencil;
            } else {
                int[][] flippedPixelArray = new int[curSelection.x2 - curSelection.x1 + 1][curSelection.y2 - curSelection.y1 + 1];

                clipboardBuffer = resetClipboardBuffer();

                for (int x = 0; x <= curSelection.x2 - curSelection.x1; x++) {
                    for (int y = 0; y <= curSelection.y2 - curSelection.y1; y++) {
                        flippedPixelArray[x][(curSelection.y2 - curSelection.y1) - y] = layeredPixelArray[x + curSelection.x1][y + curSelection.y1][activeLayer];
                    }
                }

                for (int x = curSelection.x1; x < curSelection.x2; x++) {
                    for (int y = curSelection.y1; y < curSelection.y2; y++) {
                        clipboardBuffer[x][y] = flippedPixelArray[x - curSelection.x1][y - curSelection.y1];
                    }
                }

                //Paste
                UndoEntity ue = new UndoEntity();
                UndoStack.addFirst(ue);
                safeExit = false;
                RedoStack.clear();
                for (int x = curSelection.x1; x <= curSelection.x2; x++) {
                    for (int y = curSelection.y1; y < curSelection.y2; y++) {
                        ue.AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                        layeredPixelArray[x][y][activeLayer] = -1;
                    }
                }

                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        if (clipboardBuffer[x][y] == -1)
                            continue;
                        ue.AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                        layeredPixelArray[x][y][activeLayer] = clipboardBuffer[x][y];
                    }
                }
                toolmode = Tool.pencil;
            }
        }

        //bitmap field
        if((Renderer.mouseXPixel > 1 && Renderer.mouseXPixel < 34) && (Renderer.mouseYPixel >= 0 && Renderer.mouseYPixel < 32)) {

                if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_2)){
                    curSelection.ResetSelection();
                }

                //Pencil
                if(toolmode == Tool.pencil){
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                        UndoEntity newStroke = new UndoEntity();
                        UndoStack.addFirst(newStroke);
                        RedoStack.clear();
                    }
                    if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        UndoStack.getFirst().AddPixel(Renderer.mouseXPixel - 2, Renderer.mouseYPixel, activeLayer, layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer]);
                        safeExit = false;

                        layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer] = activeColor;
                    }
                }

                //Erase
                if(toolmode == Tool.erase){
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                        UndoEntity newStroke = new UndoEntity();
                       UndoStack.addFirst(newStroke);
                        RedoStack.clear();
                    }
                    if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        UndoStack.getFirst().AddPixel(Renderer.mouseXPixel - 2, Renderer.mouseYPixel, activeLayer, layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer]);
                        safeExit = false;
                        layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer] = -1;
                    }
                }

                //Fill
                if(toolmode == Tool.fill){
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        safeExit = false;

                        //No finished selection
                        if(curSelection.selectionStage != 3) {
                            UndoEntity newStroke = new UndoEntity();
                            UndoStack.addFirst(newStroke);
                            RedoStack.clear();
                            for (int y = 0; y < 32; y++) {
                                for (int x = 0; x < 32; x++) {
                                    UndoStack.getFirst().AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                                }
                            }

                            //Start flood fill
                            floodFill(layeredPixelArray, activeLayer, null, false, Renderer.mouseXPixel - 2, Renderer.mouseYPixel, layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer], activeColor);


                        }else{
                            UndoEntity newStroke = new UndoEntity();
                            UndoStack.addFirst(newStroke);
                            RedoStack.clear();


                            //Create undo object
                            for(int x = curSelection.x1; x <= curSelection.x2; x++){
                                for(int y = curSelection.y1; y <= curSelection.y2; y++){
                                    UndoStack.getFirst().AddPixel(x, y, activeLayer, layeredPixelArray[x][y][activeLayer]);
                                }
                            }

                            //Start flood fill
                            floodFill(layeredPixelArray, activeLayer, curSelection, true, Renderer.mouseXPixel - 2, Renderer.mouseYPixel, layeredPixelArray[Renderer.mouseXPixel - 2][Renderer.mouseYPixel][activeLayer], activeColor);

                        }


                    }
                }

                //Select
                if(toolmode == Tool.select) {
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        if (curSelection.selectionStage == 3) { //Finish Selection and reset
                            curSelection.ResetSelection();
                            curSelection.selectionStage = 0;
                        }

                        if (curSelection.selectionStage == 1) { //Second point
                            curSelection.x2 = Renderer.mouseXPixel - 2;
                            curSelection.y2 = Renderer.mouseYPixel;
                            curSelection.selectionStage = 2;
                            curSelection.CompleteSelection();
                        }

                        if (curSelection.selectionStage == 0) {
                            curSelection.x1 = Renderer.mouseXPixel - 2;
                            curSelection.y1 = Renderer.mouseYPixel;
                            curSelection.selectionStage = 1;
                        }
                    }
                }

                //Move select
                if(toolmode == Tool.move_sel) {
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                        lastClickX = Renderer.mouseXPixel - 2;
                        lastClickY = Renderer.mouseYPixel;
                    }

                    if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_1)){
                        if(lastClickX != (Renderer.mouseXPixel - 2)){

                            if(!(lastClickX == curSelection.x1 || lastClickX == curSelection.x2) && !(lastClickY == curSelection.y1 || lastClickY == curSelection.y2)) {
                                curSelection.x1 += ((Renderer.mouseXPixel - 2) - lastClickX);
                                curSelection.x2 += ((Renderer.mouseXPixel - 2) - lastClickX);
                            }else{
                                if(lastClickX == curSelection.x1)
                                    curSelection.x1 += ((Renderer.mouseXPixel - 2) - lastClickX);
                                if(lastClickX == curSelection.x2)
                                    curSelection.x2 += ((Renderer.mouseXPixel - 2) - lastClickX);
                            }

                            lastClickX = Renderer.mouseXPixel - 2;
                        }
                        if(lastClickY != Renderer.mouseYPixel){

                            if(!(lastClickX == curSelection.x1 || lastClickX == curSelection.x2) && !(lastClickY == curSelection.y1 || lastClickY == curSelection.y2)) {
                                curSelection.y1 += (Renderer.mouseYPixel - lastClickY);
                                curSelection.y2 += (Renderer.mouseYPixel - lastClickY);
                            }else{
                                if(lastClickY == curSelection.y1)
                                    curSelection.y1 += (Renderer.mouseYPixel - lastClickY);
                                if(lastClickY == curSelection.y2)
                                    curSelection.y2 += (Renderer.mouseYPixel - lastClickY);
                            }

                            lastClickY = Renderer.mouseYPixel;
                        }

                        if(curSelection.x1 > 31)
                            curSelection.x1 = 31;
                        if(curSelection.x2 > 31)
                            curSelection.x2 = 31;
                        if(curSelection.y1 > 31)
                            curSelection.y1 = 31;
                        if(curSelection.y2 > 31)
                            curSelection.y2 = 31;

                        if(curSelection.x1 < 0)
                            curSelection.x1 = 0;
                        if(curSelection.x2 < 0)
                            curSelection.x2 = 0;
                        if(curSelection.y1 < 0)
                            curSelection.y1 = 0;
                        if(curSelection.y2 < 0)
                            curSelection.y2 = 0;


                    }
                }

                //Move pixels
                if(toolmode == Tool.move_pixel){
                    if(Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1)){
                        lastClickX = Renderer.mouseXPixel - 2;
                        lastClickY = Renderer.mouseYPixel;
                        UndoEntity ue = new UndoEntity();
                        UndoStack.addFirst(ue);
                        RedoStack.clear();
                    }

                    if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_1)){
                        safeExit = false;

                        if(lastClickX != (Renderer.mouseXPixel - 2)){
                            //Cut
                            clipboardBuffer = new int[32][32];
                            for (int y = 0; y < 32; y++) {
                                for (int x = 0; x < 32; x++) {
                                    clipboardBuffer[x][y] = -1;
                                }
                            }


                            for(int x = curSelection.x1; x <= curSelection.x2; x++){
                                for(int y = curSelection.y1; y <= curSelection.y2; y++){
                                    if(x < 0 || x > 31 || y < 0 || y > 31)
                                        continue;

                                    if(layeredPixelArray[x][y][activeLayer] == -1)
                                        continue;
                                    UndoStack.getFirst().AddPixel(x,y,activeLayer, layeredPixelArray[x][y][activeLayer]);
                                    clipboardBuffer[x][y] = layeredPixelArray[x][y][activeLayer];
                                    layeredPixelArray[x][y][activeLayer] = -1;
                                }
                            }

                            //Paste

                            for(int x = 0; x < 32; x++){
                                for(int y = 0; y < 32; y++){
                                    if(x + ((Renderer.mouseXPixel - 2) - lastClickX) < 0 || x + ((Renderer.mouseXPixel - 2) - lastClickX) > 31)
                                        continue;

                                    if(clipboardBuffer[x][y] == -1)
                                        continue;
                                    UndoStack.getFirst().AddPixel(x + ((Renderer.mouseXPixel - 2) - lastClickX),y,activeLayer, layeredPixelArray[x + ((Renderer.mouseXPixel - 2) - lastClickX)][y][activeLayer]);
                                    layeredPixelArray[x + ((Renderer.mouseXPixel - 2) - lastClickX)][y][activeLayer] = clipboardBuffer[x][y];
                                }
                            }
                            curSelection.x1 += ((Renderer.mouseXPixel - 2) - lastClickX);
                            curSelection.x2 += ((Renderer.mouseXPixel - 2) - lastClickX);

                            if(curSelection.x1 < 0)
                                curSelection.x1 = 0;
                            if(curSelection.x1 > 31)
                                curSelection.x1 = 31;

                            if(curSelection.x2 < 0)
                                curSelection.x2 = 0;
                            if(curSelection.x2 > 31)
                                curSelection.x2 = 31;

                            lastClickX = Renderer.mouseXPixel - 2;
                        }

                        if(lastClickY != Renderer.mouseYPixel){
                            //Cut
                            clipboardBuffer = new int[32][32];
                            for (int y = 0; y < 32; y++) {
                                for (int x = 0; x < 32; x++) {
                                    clipboardBuffer[x][y] = -1;
                                }
                            }

                            for(int x = curSelection.x1; x <= curSelection.x2; x++){
                                for(int y = curSelection.y1; y <= curSelection.y2; y++){
                                    if(x < 0 || x > 31 || y < 0 || y > 31)
                                        continue;

                                    if(layeredPixelArray[x][y][activeLayer] == -1)
                                        continue;
                                    UndoStack.getFirst().AddPixel(x,y,activeLayer, layeredPixelArray[x][y][activeLayer]);
                                    clipboardBuffer[x][y] = layeredPixelArray[x][y][activeLayer];
                                    layeredPixelArray[x][y][activeLayer] = -1;
                                }
                            }

                            //Paste


                            for(int x = 0; x < 32; x++){
                                for(int y = 0; y < 32; y++){
                                    if(y + (Renderer.mouseYPixel - lastClickY) < 0 || y + (Renderer.mouseYPixel - lastClickY) > 31)
                                        continue;

                                    if(clipboardBuffer[x][y] == -1)
                                        continue;
                                    UndoStack.getFirst().AddPixel(x,y + (Renderer.mouseYPixel - lastClickY),activeLayer, layeredPixelArray[x][y + (Renderer.mouseYPixel - lastClickY)][activeLayer]);
                                    layeredPixelArray[x][y + (Renderer.mouseYPixel - lastClickY)][activeLayer] = clipboardBuffer[x][y];
                                }
                            }
                            curSelection.y1 += (Renderer.mouseYPixel - lastClickY);
                            curSelection.y2 += (Renderer.mouseYPixel - lastClickY);

                            if(curSelection.y1 < 0)
                                curSelection.y1 = 0;
                            if(curSelection.y1 > 31)
                                curSelection.y1 = 31;

                            if(curSelection.y2 < 0)
                                curSelection.y2 = 0;
                            if(curSelection.y2 > 31)
                                curSelection.y2 = 31;

                            lastClickY = Renderer.mouseYPixel;
                        }
                    }
                }

        }

        //Flatten
        if(toolmode == Tool.flatten){
            safeExit = false;

            //this boy be MASSIVE
            UndoEntity ue = new UndoEntity();

            int[][] layer0 = FileManager.flattenAPImage(layeredPixelArray);

            for(int i = 0; i < 7; i++){
                for(int x = 0; x < 32; x++){
                    for(int y = 0; y < 32; y++){
                        ue.AddPixel(x, y, i, layeredPixelArray[x][y][i]);
                        layeredPixelArray[x][y][i] = -1;
                    }
                }
            }
            UndoStack.addFirst(ue);
            RedoStack.clear();

            for(int x = 0; x < 32; x ++){
                for(int y = 0; y < 32; y++){
                    layeredPixelArray[x][y][0] = layer0[x][y];
                }
            }

            toolmode = Tool.pencil;
        }

        //Palette Changer
        if(toolmode == Tool.color){
            if((Renderer.mouseXPixel >= 9 && Renderer.mouseXPixel < 16) && Renderer.mouseYPixel == 19){
                //Text editing
                hexBuffer = "";
                toolmode = Tool.txt_color;
            }

            if(Mouse.GetButton(GLFW.GLFW_MOUSE_BUTTON_1)){
                if (Renderer.mouseXPixel > 6 && Renderer.mouseXPixel < 29) {
                    //Red
                    if (Renderer.mouseYPixel > 3 && Renderer.mouseYPixel < 8) {
                        float Red = (255f / 21f) * (Renderer.mouseXPixel - 7);
                        int redint = (int)Renderer.Clamp(Red, 0, 255);

                        int Green = bitmapColors[activeColor].getGreen();
                        int Blue = bitmapColors[activeColor].getBlue();

                        bitmapColors[activeColor] = new Color(redint, Green, Blue);
                    }

                    //Green
                    if (Renderer.mouseYPixel > 8 && Renderer.mouseYPixel < 14) {
                        int Red = bitmapColors[activeColor].getRed();

                        float Green = (255f / 21f) * (Renderer.mouseXPixel - 7);
                        int greenint = (int)Renderer.Clamp(Green, 0, 255);

                        int Blue = bitmapColors[activeColor].getBlue();

                        bitmapColors[activeColor] = new Color(Red, greenint, Blue);
                    }

                    //Blue
                    if (Renderer.mouseYPixel > 13 && Renderer.mouseYPixel < 19) {
                        int Red = bitmapColors[activeColor].getRed();
                        int Green = bitmapColors[activeColor].getGreen();

                        float Blue = (255f / 21f) * (Renderer.mouseXPixel - 7);
                        int blueint = (int)Renderer.Clamp(Blue, 0, 255);

                        bitmapColors[activeColor] = new Color(Red, Green, blueint);
                    }
                }
            }
        }

        //Save
        if(toolmode == Tool.save){
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (Throwable ignored) {
            }

            EventQueue.invokeLater(() -> {
                toolmode = Tool.lock_save;
                JFileChooser fileChooser = new JFileChooser();

                //Prevent dumbness
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(".ap2") || f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "Austin Paint 2 Files";
                    }
                });

                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                fileChooser.setSelectedFile(new File("untitled.ap2"));
                int result = fileChooser.showSaveDialog(null);

                try {
                    String path = fileChooser.getSelectedFile().getPath();
                    if (!path.endsWith(".ap2")) {
                        path += ".ap2";
                    }

                    if (result == JFileChooser.APPROVE_OPTION) {
                        FileManager.saveFileFromImage(path, layeredPixelArray, bitmapColors);
                        safeExit = true;
                    }
                }catch(Exception ignored){}
                toolmode = Tool.pencil;
            });
        }

        //Export
        if(toolmode == Tool.export){
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (Throwable ignored) {
            }

            EventQueue.invokeLater(() -> {
                toolmode = Tool.lock_export;

                JFileChooser fileChooser = new JFileChooser();

                //Prevent dumbness
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(".bmp") || f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "Bitmap Image Files";
                    }
                });
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                fileChooser.setSelectedFile(new File("untitled.bmp"));
                int result = fileChooser.showSaveDialog(null);

                try {
                    String path = fileChooser.getSelectedFile().getPath();
                    if (!path.endsWith(".bmp")) {
                        path += ".bmp";
                    }
                    if (result == JFileChooser.APPROVE_OPTION) {
                        FileManager.saveBMPFromImage(path, layeredPixelArray, bitmapColors);
                    }
                }catch(NullPointerException ignored){}
                toolmode = Tool.pencil;
            });
        }

        //Open
        if(toolmode == Tool.open){
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch (Throwable ignored) {
            }

            EventQueue.invokeLater(() -> {
                toolmode = Tool.lock_open;
                JFileChooser fileChooser = new JFileChooser();

                //Prevent dumbness
                fileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(".ap2") || f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return "Austin Paint 2 Files";
                    }
                });

                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int result = fileChooser.showOpenDialog(null);

                if(result == JFileChooser.APPROVE_OPTION){
                    FileManager.APFile apfile = FileManager.loadPixelArrayFromFile(fileChooser.getSelectedFile().getPath());
                    layeredPixelArray = apfile.pixelArray;
                    bitmapColors = apfile.palette;
                }
                toolmode = Tool.pencil;
            });
        }

        //Text Entry Tool (Internal)
        if(toolmode == Tool.txt_color){
            int activekey = Keyboard.GetAnyKey();
            if(activekey != -1 && IsCharValidHex((char)activekey)) {

                if (hexBuffer.length() >= 6) {
                    hexBuffer = "";
                }
                hexBuffer += (char)activekey;

                StringBuilder finalHexValue = new StringBuilder(hexBuffer);
                for(int i = 0; i < 6 - hexBuffer.length(); i++){
                    finalHexValue.append("0");
                }


                bitmapColors[activeColor] = new Color(
                        Integer.parseInt(finalHexValue.substring(0, 2), 16),
                        Integer.parseInt(finalHexValue.substring(2, 4), 16),
                        Integer.parseInt(finalHexValue.substring(4, 6), 16));

            }
            if(!((Renderer.mouseXPixel >= 9 && Renderer.mouseXPixel < 16) && Renderer.mouseYPixel == 19)){
                toolmode = Tool.color;

            }
        }

        //Save warning box (Internal)
        if(toolmode == Tool.save_warn){
            //Clicked dismiss button
            if((Renderer.mouseXPixel >= 15 && Renderer.mouseXPixel <= 20) && Renderer.mouseYPixel == 21 && Mouse.GetButtonDown(GLFW.GLFW_MOUSE_BUTTON_1))
                toolmode = Tool.pencil_wait;
        }

        //Change color index right
        if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_TAB) && !Keyboard.GetKey(GLFW.GLFW_KEY_LEFT_SHIFT)){
            activeColor++;
            if(activeColor > 15)
                activeColor = 0;
        }

        //Change color index left
        if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_TAB) && Keyboard.GetKey(GLFW.GLFW_KEY_LEFT_SHIFT)){
            activeColor--;
            if(activeColor < 0)
                activeColor = 15;
        }

        //CTRL commands
        if(Keyboard.GetKey(GLFW.GLFW_KEY_LEFT_CONTROL)){
            //undo
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_Z) && UndoStack.size() > 0){
                safeExit = false;

                UndoEntity ue = UndoStack.getFirst();
                UndoEntity re = new UndoEntity();

                for(UndoEntity.Pixel2i i : ue.pixelChanges) {
                    re.AddPixel(i.x, i.y, i.layer, layeredPixelArray[i.x][i.y][i.layer]);
                }
                RedoStack.addFirst(re);

                for(UndoEntity.Pixel2i i : ue.pixelChanges){
                    layeredPixelArray[i.x][i.y][i.layer] = i.colorIndex;
                }
                UndoStack.removeFirst();
            }

            //redo
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_Y) && RedoStack.size() > 0){
                safeExit = false;

                UndoEntity re = RedoStack.getFirst();
                UndoEntity ue = new UndoEntity();

                for(UndoEntity.Pixel2i i : re.pixelChanges) {
                    ue.AddPixel(i.x, i.y, i.layer, layeredPixelArray[i.x][i.y][i.layer]);
                }
                UndoStack.addFirst(ue);



                for(UndoEntity.Pixel2i i : re.pixelChanges){
                    layeredPixelArray[i.x][i.y][i.layer] = i.colorIndex;
                }

                RedoStack.removeFirst();
            }

            //cut
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_X) && curSelection.selectionStage == 3){
                safeExit = false;

                clipboardBuffer = new int[32][32];
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) {
                        clipboardBuffer[x][y] = -1;
                    }
                }

                UndoEntity ue = new UndoEntity();
                UndoStack.addFirst(ue);
                RedoStack.clear();

                for(int x = curSelection.x1; x <= curSelection.x2; x++){
                    for(int y = curSelection.y1; y <= curSelection.y2; y++){
                        if(layeredPixelArray[x][y][activeLayer] == -1)
                            continue;
                        ue.AddPixel(x,y,activeLayer, layeredPixelArray[x][y][activeLayer]);
                        clipboardBuffer[x][y] = layeredPixelArray[x][y][activeLayer];
                        layeredPixelArray[x][y][activeLayer] = -1;
                    }
                }
                curSelection.ResetSelection();
            }

            //copy
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_C) && curSelection.selectionStage == 3){
                clipboardBuffer = new int[32][32];
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++) {
                        clipboardBuffer[x][y] = -1;
                    }
                }

                for(int x = curSelection.x1; x <= curSelection.x2; x++){
                    for(int y = curSelection.y1; y <= curSelection.y2; y++){
                        if(layeredPixelArray[x][y][activeLayer] == -1)
                            continue;

                        clipboardBuffer[x][y] = layeredPixelArray[x][y][activeLayer];
                    }
                }
                curSelection.ResetSelection();
            }

            //paste
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_V) && clipboardBuffer != null){
                safeExit = false;

                UndoEntity ue = new UndoEntity();
                UndoStack.addFirst(ue);
                RedoStack.clear();

                for(int x = 0; x < 32; x++){
                    for(int y = 0; y < 32; y++){
                        if(clipboardBuffer[x][y] == -1)
                            continue;
                        ue.AddPixel(x,y,activeLayer, layeredPixelArray[x][y][activeLayer]);
                        layeredPixelArray[x][y][activeLayer] = clipboardBuffer[x][y];
                    }
                }
            }

            //Save
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_S)){
                toolmode = Tool.save;
            }
        }

        //ALT commands
        if(Keyboard.GetKey(GLFW.GLFW_KEY_LEFT_ALT)){
            if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_S)){
                drawStacked = !drawStacked;
            }
        }

        //Enable debug
        if(Keyboard.GetKeyDown(GLFW.GLFW_KEY_F3)){
            debugDraw = !debugDraw;
        }
    }

    //Create a new, empty clipboard buffer
    private static int[][] resetClipboardBuffer() {
        int[][] newBuffer = new int[32][32]; //While this is wasteful memory wise, it prevents the clipboard buffer from ever being null
        for(int x = 0; x < 32; x++){
            for(int y = 0; y < 32; y++){
                newBuffer[x][y] = -1;
            }
        }
        return newBuffer;
    }

    private static boolean IsCharValidHex(char getActiveKey) {
        final String hexChars = "ABDCEF1234567890";
        return hexChars.indexOf(getActiveKey) != -1;
    }

    private static void floodFill(int[][][] layeredPixelArray, int affectedLayer, Selection curSelection, boolean useSelectionBounds, int x, int y, int targetColorIndex, int colorIndex){
        int maxX = useSelectionBounds ? curSelection.x2 : 31;
        int minX = useSelectionBounds ? curSelection.x1 : 0;

        int maxY = useSelectionBounds ? curSelection.y2 : 31;
        int minY = useSelectionBounds ? curSelection.y1 : 0;

        if((x > maxX || x < minX) || (y > maxY || y < minY) || layeredPixelArray[x][y][affectedLayer] == colorIndex)
            return;

        if(layeredPixelArray[x][y][affectedLayer] != targetColorIndex)
            return;

        layeredPixelArray[x][y][affectedLayer] = colorIndex;

        //Spread in the 4 directions
        floodFill(layeredPixelArray, affectedLayer, curSelection, useSelectionBounds, x + 1, y, targetColorIndex, colorIndex);
        floodFill(layeredPixelArray, affectedLayer, curSelection, useSelectionBounds, x - 1, y, targetColorIndex, colorIndex);
        floodFill(layeredPixelArray, affectedLayer, curSelection, useSelectionBounds, x, y + 1, targetColorIndex, colorIndex);
        floodFill(layeredPixelArray, affectedLayer, curSelection, useSelectionBounds, x, y - 1, targetColorIndex, colorIndex);
    }

}




