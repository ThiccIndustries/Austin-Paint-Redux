package com.thiccindustries.APE2;
import com.thiccindustries.TLib.*;

import org.apache.commons.lang.ArrayUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWImage;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.*;
import static org.lwjgl.opengl.GL11.*;
import java.awt.Color;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.text.DecimalFormat;

import static com.thiccindustries.APE2.Resources.*;

public class Renderer {
    public static double mouseX, mouseY;
    public static int mouseXPixel, mouseYPixel;
    public static int pixelScale = 16;
    public static int uiScale = 4;
    public static int rawScale = 1;
    public static int blinkrate = 4;

    private static long window;

    private static double lastTime = 0;
    private static int FPS = 0;
    private static boolean CURSOR_BLINK;


    private static final Texture[] layerTextures = new Texture[7];

    public static void initGLFWAndCreateWindow(int windowScale, int blinkRate, boolean vsync){
        blinkrate = blinkRate;

        pixelScale = 16 * windowScale;
        uiScale = 4 * windowScale;
        rawScale = windowScale;
        int windowX = (8 * uiScale) + (32 * pixelScale) + (18 * uiScale);
        int windowY = (32 * pixelScale) + (2 * pixelScale);


        if(!glfwInit()){
            System.err.println("GLFW library init failed!");
            System.exit(1);
        }

        glfwWindowHint(GLFW_RESIZABLE, 0);
        window = glfwCreateWindow(windowX, windowY, "Austin Paint Redux", 0, 0);
        glfwShowWindow(window);
        glfwMakeContextCurrent(window);
        createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, windowX, windowY, 0, 1, -1);

        //Vsync on
        glfwSwapInterval(vsync ? 1 : 0);

        //Set callbacks
        glfwSetKeyCallback(window, new Keyboard());
        glfwSetMouseButtonCallback(window, new Mouse());


        //Set window icon
        try {
            ByteBuffer iconBuffer = APTextureLoader.loadTextureByteBuffer("/res/icon.ap2");
            GLFWImage.Buffer icons = GLFWImage.malloc(1);

            icons
                    .position(0)
                    .width(32)
                    .height(32)
                    .pixels(iconBuffer);


            glfwSetWindowIcon(window, icons);
        }catch(IOException e){
            System.err.println("Failed to set window icon");
        }

        //Init layer textures
        for(int i = 0; i < 7; i++){
            layerTextures[i] = APTextureLoader.createEmptyTexture();
        }
    }

    @SuppressWarnings("unused")
    public static void temp_debugFont(){
        glColor3f(1,1,1);
        font.Bind(0);
        glEnable(GL_TEXTURE_2D);
        glBegin(GL_QUADS);
        {
            glTexCoord2i(0,0); glVertex2i(0,0);
            glTexCoord2i(1,0); glVertex2i((8 * uiScale) + (32 * pixelScale) + (18 * uiScale),0);
            glTexCoord2i(1,1); glVertex2i((8 * uiScale) + (32 * pixelScale) + (18 * uiScale),(32 * pixelScale) + (2 * pixelScale));
            glTexCoord2i(0,1); glVertex2i(0,(32 * pixelScale) + (2 * pixelScale));
        }
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }

    public static void drawBitmapLayers(boolean allLayers, int layerToDraw){
        //Only draw active layers, as the rest would just waste time, and aren't visible in the ui
        for(int layer = 0; layer < 7; layer++){
            if(!allLayers && layer != layerToDraw)
                continue;

            //Disable transparency if first layer or only one layer being drawn
            if(layer == 0 || !allLayers)
                glDisable(GL_BLEND);
            else
                glEnable(GL_BLEND);

            glEnable(GL_TEXTURE_2D);

            layerTextures[layer].Bind(0);
            glBegin(GL_QUADS);
            {
                glTexCoord2i(0,0); glVertex2i((8 * uiScale),                      0);
                glTexCoord2i(1,0); glVertex2i((8 * uiScale) + (32 * pixelScale),  0);
                glTexCoord2i(1,1); glVertex2i((8 * uiScale) + (32 * pixelScale),  (32 * pixelScale));
                glTexCoord2i(0,1); glVertex2i((8 * uiScale),                      (32 * pixelScale));

            }
            glEnd();
            glDisable(GL_TEXTURE_2D);
        }

        glEnable(GL_BLEND);
    }

    public static void drawBitmapLayerPreviews(){
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        for(int layer = 0; layer < 7; layer++){
            int uioffsetX = (9 * uiScale) + (32 * pixelScale);
            int uioffsetY = (18 * uiScale) * (6 - layer) + uiScale;

            layerTextures[layer].Bind(0);
            glBegin(GL_QUADS);
            {
                glTexCoord2i(0,0); glVertex2i(uioffsetX,                      uioffsetY);
                glTexCoord2i(1,0); glVertex2i(uioffsetX + (64 * rawScale),    uioffsetY);
                glTexCoord2i(1,1); glVertex2i(uioffsetX + (64 * rawScale),    uioffsetY + (64 * rawScale));
                glTexCoord2i(0,1); glVertex2i(uioffsetX,                      uioffsetY + (64 * rawScale));
            }
            glEnd();

        }
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
    }

    public static void drawSelection(Selection selection){
        if(selection.selectionStage >= 1){
            //Render first point
            glColor3f(0.5f, 0.5f, 0.5f);
            glBegin(GL_QUADS);
            {
                glVertex2i((8 * uiScale) + (selection.x1 * pixelScale),                selection.y1 * pixelScale);
                glVertex2i((8 * uiScale) + (selection.x1 * pixelScale) + pixelScale,   selection.y1 * pixelScale);
                glVertex2i((8 * uiScale) + (selection.x1 * pixelScale) + pixelScale,   selection.y1 * pixelScale + pixelScale);
                glVertex2i((8 * uiScale) + (selection.x1 * pixelScale),                selection.y1 * pixelScale + pixelScale);
            }
            glEnd();
        }

        if(selection.selectionStage >= 2){
            //Render second point
            glColor3f(0.5f, 0.5f, 0.5f);
            glBegin(GL_QUADS);
            {
                glVertex2i((8 * uiScale) + (selection.x2 * pixelScale),                selection.y2 * pixelScale);
                glVertex2i((8 * uiScale) + (selection.x2 * pixelScale) + pixelScale,   selection.y2 * pixelScale);
                glVertex2i((8 * uiScale) + (selection.x2 * pixelScale) + pixelScale,   selection.y2 * pixelScale + pixelScale);
                glVertex2i((8 * uiScale) + (selection.x2 * pixelScale),                selection.y2 * pixelScale + pixelScale);
            }
            glEnd();
        }

        int p2x = 0, p2y = 0;
        switch(selection.selectionStage){
            case 1:
            {
                p2x = TUtils.clamp(mouseXPixel - 2, 0, 31);
                p2y = TUtils.clamp(mouseYPixel,     0, 31);
                break;
            }

            case 3:
            {
                p2x = selection.x2;
                p2y = selection.y2;
                break;
            }
        }

        if(CURSOR_BLINK && selection.selectionStage >= 1){
            for(int x = selection.x1; x <= p2x; x++){
                for(int y = selection.y1; y <= p2y; y++){
                    //Skip interior space
                    if ((x != selection.x1 && x != p2x) && (y != selection.y1 && y != p2y))
                        continue;

                    if((y + x ) % 2 == 0) {
                        glColor3f(0.5f, 0.5f, 0.5f);
                    }else{
                        glColor3f(0f, 0f, 0f);
                    }

                    glBegin(GL_QUADS);
                    {
                        glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale);
                        glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale);
                        glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale + pixelScale);
                        glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale + pixelScale);
                    }
                    glEnd();
                }
            }
        }
    }

    public static void drawUI(Tool toolmode, Color[] palette, int selectedColor, int selectedLayer, boolean drawToolTip){
        /* Draw toolbar */

        //Toolbar background
        glColor3f(0.125f,0.125f,0.125f);
        glBegin(GL_QUADS);
        {
            glVertex2i(0,              0);
            glVertex2i((8 * uiScale),  0);
            glVertex2i((8 * uiScale),  (32 * pixelScale) + (2 * pixelScale));
            glVertex2i(0,              (32 * pixelScale) + (2 * pixelScale));
        }
        glEnd();

        //Selected tool
        int selectedToolHighlightPos = toolmode.getDisplayTool().ordinal();

        glColor3ub((byte)0,(byte)74,(byte)127);
        glBegin(GL_QUADS);
        {
            glVertex2i(0,              8 * uiScale * selectedToolHighlightPos);
            glVertex2i((8 * uiScale),  8 * uiScale * selectedToolHighlightPos);
            glVertex2i((8 * uiScale),  8 * uiScale * selectedToolHighlightPos + (8 * uiScale));
            glVertex2i(0,              8 * uiScale * selectedToolHighlightPos + (8 * uiScale));
        }
        glEnd();


        //Tool icons
        for(int i = 0; i < Tool.values().length; i++){

            //Only display visible tools
            if(!Tool.values()[i].display())
                continue;

            cursorTextures[ Tool.values()[i].getDisplayTool().ordinal() ].Bind(0);

            glEnable(GL_TEXTURE_2D);
            glColor3f(1,1,1);
            glBegin(GL_QUADS);
            {
                glTexCoord2i(0,0); glVertex2i(0,                  (i * (8 * uiScale)));
                glTexCoord2i(1,0); glVertex2i((8 * uiScale),      (i * (8 * uiScale)));
                glTexCoord2i(1,1); glVertex2i((8 * uiScale),      (i * (8 * uiScale)) + (8 * uiScale));
                glTexCoord2i(0,1); glVertex2i(0,                  (i * (8 * uiScale)) + (8 * uiScale));
            }
            glEnd();
            glDisable(GL_TEXTURE_2D);
        }

        /*Draw color palette*/
        glColor3f(0,0,0);
        glBegin(GL_QUADS);
        {
            glVertex2i((8 * uiScale),                      (32 * pixelScale) );
            glVertex2i((8 * uiScale) + (32 * pixelScale),  (32 * pixelScale) );
            glVertex2i((8 * uiScale) + (32 * pixelScale),  (32 * pixelScale) + (2 * pixelScale) );
            glVertex2i((8 * uiScale),                      (32 * pixelScale) + (2 * pixelScale) );
        }
        glEnd();

        for(int i = 0; i < palette.length; i++){
            int uiHeight = (selectedColor == i) ? 0 : 1;
            glColor3ub((byte)palette[i].getRed(), (byte)palette[i].getGreen(), (byte)palette[i].getBlue());
            glBegin(GL_QUADS);
            {
                glVertex2i((8 * uiScale) + (2 * pixelScale * i),                       (32 * pixelScale) + (uiHeight * pixelScale));
                glVertex2i((8 * uiScale) + (2 * pixelScale * i) + (2 * pixelScale),    (32 * pixelScale) + (uiHeight * pixelScale));
                glVertex2i((8 * uiScale) + (2 * pixelScale * i) + (2 * pixelScale),    (32 * pixelScale) + (uiHeight * pixelScale) + (2 * pixelScale));
                glVertex2i((8 * uiScale) + (2 * pixelScale * i),                       (32 * pixelScale) + (uiHeight * pixelScale) + (2 * pixelScale));
            }
            glEnd();
        }

        /*Draw layers BG*/

        glEnable(GL_TEXTURE_2D);
        for(int i = 0; i < 7; i++){
            if(i == selectedLayer){
                layerBackground_active.Bind(0);
            }else{
                layerBackground.Bind(0);
            }
            glColor3f(1,1,1);
            glBegin(GL_QUADS);
            {
                int uioffset = (8 * uiScale) + (32 * pixelScale);
                glTexCoord2i(0,0); glVertex2i(uioffset,                   (18 * uiScale * (6 - i)) );
                glTexCoord2i(1,0); glVertex2i(uioffset + (18 * uiScale),  (18 * uiScale * (6 - i)) );
                glTexCoord2i(1,1); glVertex2i(uioffset + (18 * uiScale),  (18 * uiScale * (6 - i)) + (18 * uiScale));
                glTexCoord2i(0,1); glVertex2i(uioffset,                   (18 * uiScale * (6 - i)) + (18 * uiScale));
            }
            glEnd();

        }
        glDisable(GL_TEXTURE_2D);

        /*Logo*/

        glColor3f(0,0,0);
        glBegin(GL_QUADS);
        {
            int uioffsetX = (8 * uiScale) + (32 * pixelScale);
            int uioffsetY = (32 * pixelScale) + (2 * pixelScale) - (40 * rawScale);
            glVertex2i(uioffsetX,                   uioffsetY);
            glVertex2i(uioffsetX + (72 * rawScale), uioffsetY);
            glVertex2i(uioffsetX + (72 * rawScale), uioffsetY + (40 * rawScale));
            glVertex2i(uioffsetX,                   uioffsetY + (40 * rawScale));
        }
        glEnd();

        /*Draw mouse cursor*/
        DoubleBuffer mouseBufferX = BufferUtils.createDoubleBuffer(1), mouseBufferY = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, mouseBufferX, mouseBufferY);
        mouseX = mouseBufferX.get();
        mouseY = mouseBufferY.get();

        mouseXPixel = (int)mouseX / pixelScale;
        mouseYPixel = (int)mouseY / pixelScale;

        //Draw pixel preview
        if(CURSOR_BLINK && (mouseXPixel > 1 && mouseXPixel < 34) && (mouseYPixel >= 0 && mouseYPixel < 32)){
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.5f, 0.5f, 0.5f);
            glBegin(GL_QUADS);
            {
                glVertex2i(mouseXPixel * pixelScale,              mouseYPixel * pixelScale);
                glVertex2i(mouseXPixel * pixelScale + pixelScale, mouseYPixel * pixelScale);
                glVertex2i(mouseXPixel * pixelScale + pixelScale, mouseYPixel * pixelScale + pixelScale);
                glVertex2i(mouseXPixel * pixelScale,              mouseYPixel * pixelScale + pixelScale);
            }
            glEnd();
        }

        if(drawToolTip) {
            int mouseTool = mouseYPixel / 2;

            if(mouseTool < 0)
                mouseTool = 0;

            if(mouseTool >= Tool.values().length)
                mouseTool = Tool.values().length - 1;

            if (Tool.values()[mouseTool].display())
                drawText(Tool.values()[mouseTool].getName(), (int) mouseX + pixelScale, (int) mouseY + pixelScale, 1, true, Color.white, Color.black);
        }


        glColor3f(1,1,1);
        glEnable(GL_TEXTURE_2D);
        //If inside bitmap field
        if((mouseXPixel > 1 && mouseXPixel < 34) && (mouseYPixel >= 0 && mouseYPixel < 32) && toolmode != Tool.color && toolmode != Tool.settings) {

            //Render tool specific cursor
            cursorTextures[toolmode.ordinal()].Bind(0);
        }else{
            uiCursor.Bind(0);
        }

        glBegin(GL_QUADS);
        {
            glTexCoord2i(0,0); glVertex2d( mouseX,                    mouseY );
            glTexCoord2i(1,0); glVertex2d( mouseX + (2 * pixelScale), mouseY );
            glTexCoord2i(1,1); glVertex2d( mouseX + (2 * pixelScale), mouseY + (2 * pixelScale) );
            glTexCoord2i(0,1); glVertex2d( mouseX,                    mouseY + (2 * pixelScale) );
        }
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }
    //Debug info
    public static void drawDebugInf(Tool toolmode, Color[] palette, int activeColor, int activeLayer, boolean safeExit) {
        //Color Selector
        int uiOffset = 2 * (pixelScale);

        //Window Border
        for (int x = 0; x < 28; x++) {
            for (int y = 0; y < 18; y++) {
                //checkboard pattern
                Color UIColor = new Color(127, 127, 127);
                if ((x > 0 && x < 27) && (y > 0 && y < 17))
                    UIColor = Color.black;

                glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
                glBegin(GL_QUADS);
                {
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset + pixelScale);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset + pixelScale);
                }
                glEnd();
            }
        }

        drawText("Debug Info: (F3)", 6 * pixelScale, (4 * pixelScale), 1, false, Color.white, null);

        drawText("FPS:   ", 6 * pixelScale,     6 * pixelScale, 1, false, Color.white, null);
        drawFPS(            13 * pixelScale,    6 * pixelScale, 1, false, Color.white, null);

        drawText("Tool:  " + toolmode.toString(),    6 * pixelScale,    8 * pixelScale, 1, false, Color.white, null);
        drawText("\"" + toolmode.getName() + "\"",   13 * pixelScale,   9 * pixelScale, 1, false, Color.white, null);

        drawText("Color: " + activeColor, 6 * pixelScale, 11 * pixelScale, 1, false, Color.white, null);

        String hex = "#" + String.format("%02X", palette[activeColor].getRed()) + String.format("%02X", palette[activeColor].getGreen()) + String.format("%02X", palette[activeColor].getBlue());
        drawText(hex, 16 * pixelScale, 11 * pixelScale, 1, false,  Color.white, null);

        drawText("Layer: " + activeLayer, 6 * pixelScale, 13 * pixelScale, 1, false, Color.white, null);

        drawText("Exit warn: " + !safeExit, 6 * pixelScale, 15 * pixelScale, 1, false, Color.white, null);

        drawText("Version: " + Application.verNum, 6 * pixelScale, 17 * pixelScale, 1, false, Color.white, null);
    }

    //Settings window
    public static void drawSettings(int scale, boolean vsync, String buttonText){
        //Color Selector
        int uiOffset = 2 * (pixelScale);
        int winW = 28;
        int winH = 16;
        //Window Border
        for (int x = 0; x < winW; x++) {
            for (int y = 0; y < winH; y++) {
                //checkboard pattern
                Color UIColor = new Color(127, 127, 127);
                if ((x > 0 && x < 27) && (y > 0 && y < 15))
                    UIColor = Color.black;

                glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
                glBegin(GL_QUADS);
                {
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset + pixelScale);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset + pixelScale);
                }
                glEnd();
            }
        }

        drawText("Settings:", 6 * pixelScale, (4 * pixelScale), 1, false, Color.white, null);

        //Window scale
        drawText("Scale (restart): "   + scale, 6 * pixelScale, (7 * pixelScale), 1, false, Color.white, null);
        drawText("Blink Rate: "     + blinkrate, 6 * pixelScale, (9 * pixelScale), 1, false, Color.white, null);
        //Vsync
        String vsyncTxt = vsync ? "On" : "Off";
        drawText("VSync: ", 6 * pixelScale, (11 * pixelScale), 1, false, Color.white, null);

        //Vsync button
        drawText(vsyncTxt, 13 * pixelScale, (11 * pixelScale), 1, true, Color.black, Color.gray);

        //Draw button
        int buttonTextCharOffset = 11 - TUtils.clamp(buttonText.length() / 2, 0, 11);
        uiOffset = (2 + buttonTextCharOffset) * pixelScale;

        drawText("Version: " + Application.verNum, 6 * pixelScale, (13 * pixelScale), 1, false, Color.white, null);

        drawText(buttonText, uiOffset + (5 * pixelScale), ((winH - 1) * pixelScale), 1, true, Color.black, Color.gray);

    }
    //Draw a dialog box
    public static void drawDialog(String[] messageLines, String buttonText) {

        int uiOffset    = 2 * (pixelScale);
        int uiOffsetY   = 10 * (pixelScale);

        //Window Border
        for (int x = 0; x < 28; x++) {
            for (int y = 0; y < 14; y++) {
                Color UIColor = new Color(127, 127, 127);
                if ((x > 0 && x < 27) && (y > 0 && y < 13))
                    UIColor = Color.black;

                glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
                glBegin(GL_QUADS);
                {
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset,                 (y * pixelScale) + uiOffsetY);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale,    (y * pixelScale) + uiOffsetY);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale,    (y * pixelScale) + uiOffsetY + pixelScale);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset,                 (y * pixelScale) + uiOffsetY + pixelScale);
                }
                glEnd();
            }
        }

        int textCharOffset;
        for(int i = 0; i < messageLines.length; i++){
            textCharOffset = 12 - TUtils.clamp(messageLines[i].length() / 2, 0, 12);
            drawText(messageLines[i], uiOffset + (4 * pixelScale) + (textCharOffset * pixelScale), uiOffsetY + ((i + 2) * pixelScale), 1, false, Color.white, null);
        }

        //Draw button
        int buttonTextCharOffset = 11 - TUtils.clamp(buttonText.length() / 2, 0, 11);
        uiOffset = (2 + buttonTextCharOffset) * pixelScale;
        uiOffsetY = 20 * pixelScale;

        drawText(buttonText, uiOffset + (5 * pixelScale), (uiOffsetY + pixelScale), 1, true, Color.black, Color.gray);

    }

    //Palette Editing screen
    public static void drawColorSelection(Color[] Palette, int selectedColor, boolean toolmode) {
        //Color Selector
        int uiOffset = 2 * (pixelScale);

        //Window Border
        for(int x = 0; x < 28; x ++){
            for(int y = 0; y < 28; y++){
                //checkboard pattern
                Color UIColor = new Color(127, 127, 127);
                if((x > 0 && x < 27) && (y > 0 && y < 27))
                    UIColor = Color.black;

                glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
                glBegin(GL_QUADS);
                {
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset + pixelScale);
                    glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset + pixelScale);
                }
                glEnd();
            }
        }

        //Render color selectors
        uiOffset = 4 * pixelScale;
        int uiOffsetY;

        //Loop though the three primary colors and create a slider box for each one
        for(int colorIndex = 0; colorIndex < 3; colorIndex++){
            uiOffsetY = pixelScale * (5 * colorIndex);

            //Draw Slider Border
            Color UIColor = new Color(127, 127, 127);
            glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
            glBegin(GL_QUADS);
            {
                glVertex2i((2 * pixelScale) + uiOffset,                       uiOffset + uiOffsetY);
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 24),   uiOffset + uiOffsetY);
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 24),   uiOffset + uiOffsetY + (pixelScale * 4));
                glVertex2i((2 * pixelScale) + uiOffset,                       uiOffset + uiOffsetY + (pixelScale * 4));
            }
            glEnd();

            //Get correct color value for fading bar
            Color editingColor = new Color(0,0,0);
            switch(colorIndex){
                case 0:
                    editingColor = new Color(1.0f, 0, 0);
                    break;
                case 1:
                    editingColor = new Color(0, 1.0f, 0);
                    break;
                case 2:
                    editingColor = new Color(0, 0, 1.0f);
                    break;
            }

            //Draw fading color bar
            glBegin(GL_QUADS);

            int noRed       = TUtils.clamp(Palette[selectedColor].getRed() - editingColor.getRed(), 0, 255);
            int noGreen     = TUtils.clamp(Palette[selectedColor].getGreen() - editingColor.getGreen(), 0, 255);
            int noBlue      = TUtils.clamp(Palette[selectedColor].getBlue() - editingColor.getBlue(), 0, 255);

            int maxRed      = TUtils.clamp(Palette[selectedColor].getRed() + editingColor.getRed(), 0, 255);
            int maxGreen    = TUtils.clamp(Palette[selectedColor].getGreen() + editingColor.getGreen(), 0, 255);
            int maxBlue     = TUtils.clamp(Palette[selectedColor].getBlue() + editingColor.getBlue(), 0, 255);

            glColor3ub((byte)noRed, (byte)noGreen, (byte)noBlue);
            {
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale),         (uiOffset + pixelScale) + uiOffsetY);
                glColor3ub((byte)maxRed, (byte)maxGreen, (byte)maxBlue);
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 23),    (uiOffset + pixelScale) + uiOffsetY);
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 23),    (uiOffset + pixelScale) + uiOffsetY + (pixelScale * 2));
                glColor3ub((byte)noRed, (byte)noGreen, (byte)noBlue);
                glVertex2i((2 * pixelScale) + uiOffset + (pixelScale),         (uiOffset + pixelScale) + uiOffsetY + (pixelScale * 2));
            }
            glEnd();

            //Get correct value for slider bar
            float colorValue = 0.0f;
            switch(colorIndex){
                case 0:
                    colorValue = (Palette[selectedColor].getRed());
                    break;
                case 1:
                    colorValue = (Palette[selectedColor].getGreen());
                    break;
                case 2:
                    colorValue = (Palette[selectedColor].getBlue());
                    break;
            }

            //Draw slider bar
            float colorPosition = ((pixelScale * 21) * (colorValue / 255f)) + pixelScale;

            int colorPositionHexMult = Math.round(colorPosition / 16);
            int colorPositionClamped = colorPositionHexMult * 16;
            UIColor = new Color(64, 64, 64);
            glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
            glBegin(GL_QUADS);
            {
                glVertex2f((2 * pixelScale) + uiOffset + colorPositionClamped,                   uiOffset + uiOffsetY);
                glVertex2f((2 * pixelScale) + uiOffset + colorPositionClamped + (pixelScale),    uiOffset + uiOffsetY);
                glVertex2f((2 * pixelScale) + uiOffset + colorPositionClamped + (pixelScale),    uiOffset + uiOffsetY + (pixelScale * 4));
                glVertex2f((2 * pixelScale) + uiOffset + colorPositionClamped,                   uiOffset + uiOffsetY + (pixelScale * 4));
            }
            glEnd();
        }

        //Color Preview Border
        uiOffsetY = 21 * pixelScale;
        Color UIColor = new Color(127, 127, 127);
        glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
        glBegin(GL_QUADS);
        {
            glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY);
            glVertex2i((2 * pixelScale) + uiOffset + (13 * pixelScale),    uiOffsetY);
            glVertex2i((2 * pixelScale) + uiOffset + (13 * pixelScale),    uiOffsetY + (7 * pixelScale));
            glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY + (7 * pixelScale));
        }
        glEnd();

        //Hex preview
        uiOffset = 4 * pixelScale;
        uiOffsetY = 17 * pixelScale;

        UIColor = Palette[selectedColor];
        String hex = "#" + String.format("%02X", UIColor.getRed()) + String.format("%02X", UIColor.getGreen()) + String.format("%02X", UIColor.getBlue());

        drawText(hex, (5 * pixelScale) + uiOffset, (2 * pixelScale) + uiOffsetY, 1, toolmode && CURSOR_BLINK, Color.white, Color.darkGray);


        DecimalFormat df = new DecimalFormat("000");
        drawText("RED  : " + df.format(UIColor.getRed()),   (20 * pixelScale), uiOffsetY + (5 * pixelScale), 1, false, Color.white, null);
        drawText("GREEN: " + df.format(UIColor.getGreen()), (20 * pixelScale), uiOffsetY + (7 * pixelScale), 1, false, Color.white, null);
        drawText("BLUE : " + df.format(UIColor.getBlue()),  (20 * pixelScale), uiOffsetY + (9 * pixelScale), 1, false, Color.white, null);

        //Color Preview Fill
        uiOffset = 5 * pixelScale;
        uiOffsetY = 22 * pixelScale;
        glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
        glBegin(GL_QUADS);
        {
            glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY);
            glVertex2i((2 * pixelScale) + uiOffset + (11 * pixelScale),    uiOffsetY);
            glVertex2i((2 * pixelScale) + uiOffset + (11 * pixelScale),    uiOffsetY + (5 * pixelScale));
            glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY + (5 * pixelScale));
        }
        glEnd();


        drawText("Save", (20 * pixelScale), (19 * pixelScale), 1, true, Color.black, Color.gray);
        drawText("Load", (26 * pixelScale), (19 * pixelScale), 1, true, Color.black, Color.gray);
    }

    public static void drawFPS(int posX, int posY, int scale, boolean background, Color textColor, Color backgroundColor){
        DecimalFormat df = new DecimalFormat("###");
        drawText(df.format(FPS), posX, posY, scale, background, textColor, backgroundColor);
    }

    public static void drawText(String text, int posX, int posY, int scale, boolean background, Color textColor, Color backgroundColor){

        String[] splitStrings = text.split("\n");
        for(int i = 0; i < splitStrings.length; i++){
            drawTextLine(splitStrings[i], posX, posY + (pixelScale * scale * i), scale, background, textColor, backgroundColor);
        }

    }

    private static void drawTextLine(String text, int posX, int posY, int scale, boolean background, Color textColor, Color backgroundColor) {

        if(text.length() < 1)
            return;

        char[] charArray = text.toCharArray();

        int charSize = pixelScale * scale;

        if(background){

            final int bgBorder = 2 * rawScale;

            glColor3ub((byte)backgroundColor.getRed(), (byte)backgroundColor.getGreen(), (byte)backgroundColor.getBlue());
            glBegin(GL_QUADS);
            {
                glVertex2f(posX,                              posY - bgBorder);
                glVertex2f(posX + (charSize * text.length()), posY - bgBorder);
                glVertex2f(posX + (charSize * text.length()), posY + bgBorder + charSize);
                glVertex2f(posX,                              posY + bgBorder + charSize);
            }
            glEnd();
        }

        for(int charIndex = 0; charIndex < charArray.length; charIndex++){
            char[] chars = fontIndices.toCharArray();
            int indexLinear = ArrayUtils.indexOf(chars, charArray[charIndex]);

            int indexX = indexLinear % 12;
            int indexY = indexLinear / 12;

            float textureOffsetX = indexX / 12f;
            float textureOFfsetY = indexY / 8f;

            glEnable(GL_TEXTURE_2D);
            glColor3ub((byte)textColor.getRed(), (byte)textColor.getGreen(), (byte)textColor.getBlue());
            font.Bind(0);
            glBegin(GL_QUADS);
            {
                glTexCoord2f(textureOffsetX,                   textureOFfsetY);                glVertex2f(posX + (charIndex * charSize),              posY);
                glTexCoord2f( (1f / 12f) + textureOffsetX,     textureOFfsetY);                glVertex2f(posX + (charIndex * charSize) + charSize,   posY);
                glTexCoord2f( (1f / 12f) + textureOffsetX,     (1f / 8f) + textureOFfsetY);   glVertex2f(posX + (charIndex * charSize) + charSize,   posY + charSize);
                glTexCoord2f(textureOffsetX,                   (1f / 8f) + textureOFfsetY);   glVertex2f(posX + (charIndex * charSize),              posY + charSize);

            }
            glEnd();
            glDisable(GL_TEXTURE_2D);

        }
    }


    //GLFW command pass though
    public static boolean windowShouldClose(){
        return glfwWindowShouldClose(window);
    }
    public static void pollWindowEvents(){glfwPollEvents();}
    public static void completeFrame(){

        if( (int)(glfwGetTime() * (float)blinkrate) > (int)(lastTime * (float)blinkrate) ){
            FPS = (int)(1f / (glfwGetTime() - lastTime));
            CURSOR_BLINK = !CURSOR_BLINK;
        }

        lastTime = glfwGetTime();
        glfwSwapBuffers(window);
    }

    public static void abortWindowClose() {
        glfwSetWindowShouldClose(window, false);
    }

    public static void updateLayer(Color[] bitmapColors, int[][][] layeredPixelArray, int i) {
        APTextureLoader.updateAPTexture(layerTextures[i], bitmapColors, layeredPixelArray, i);
    }
}
