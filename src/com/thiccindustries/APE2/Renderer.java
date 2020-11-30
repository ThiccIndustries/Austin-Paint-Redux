package com.thiccindustries.APE2;
import com.thiccindustries.APE2.io.*;
import org.apache.commons.lang.ArrayUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL;
import java.awt.Color;
import java.io.IOException;
import java.nio.DoubleBuffer;
import java.text.DecimalFormat;


public class Renderer {
    public static double mouseX, mouseY;
    public static int mouseXPixel, mouseYPixel;
    public static int pixelScale = 16;
    public static int uiScale = 4;
    public static int rawScale = 1;
    private static long window;

    public static String fontIndices;
    private static Texture fallbackTexture;
    private static Texture uiCursor;

    private static Texture[] cursorTextures;
    private static String[] toolTexts;

    private static Texture layerBackground;
    private static Texture layerBackground_active;

    private static Texture font;

    private static boolean CURSOR_BLINK;
    private static int cycle = 0;

    public static void initGLFWAndCreateWindow(int windowScale){
        pixelScale = 16 * windowScale;
        uiScale = 4 * windowScale;
        rawScale = windowScale;
        int windowX = (8 * uiScale) + (32 * pixelScale) + (18 * uiScale);
        int windowY = (32 * pixelScale) + (2 * pixelScale);


        if(!GLFW.glfwInit()){
            System.err.println("GLFW library init failed!");
            System.exit(1);
        }

        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, 0);
        window = GLFW.glfwCreateWindow(windowX, windowY, "Austin Paint Redux", 0, 0);
        GLFW.glfwShowWindow(window);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, windowX, windowY, 0, 1, -1);

        //Vsync on
        GLFW.glfwSwapInterval(1);

        //Set callbacks
        GLFW.glfwSetKeyCallback(window, new Keyboard());
        GLFW.glfwSetMouseButtonCallback(window, new Mouse());

    }

    public static void loadResources(){
        fallbackTexture = null;
        try{
            fallbackTexture = TextureLoader.loadTextureAPNoFallback("/res/fallback.ap2");

        } catch (IOException e) {
            e.printStackTrace();
        }
        uiCursor = TextureLoader.loadTextureAP("/res/cursor.ap2", fallbackTexture);
        cursorTextures = new Texture[13];
        cursorTextures[0]   = TextureLoader.loadTextureAP("/res/tools/pencil.ap2", fallbackTexture);
        cursorTextures[1]   = TextureLoader.loadTextureAP("/res/tools/erase.ap2", fallbackTexture);
        cursorTextures[2]   = TextureLoader.loadTextureAP("/res/tools/fill.ap2", fallbackTexture);
        cursorTextures[3]   = TextureLoader.loadTextureAP("/res/tools/selection.ap2", fallbackTexture);
        cursorTextures[4]   = TextureLoader.loadTextureAP("/res/tools/move_selection.ap2", fallbackTexture);
        cursorTextures[5]   = TextureLoader.loadTextureAP("/res/tools/move_pixels.ap2", fallbackTexture);
        cursorTextures[6]   = TextureLoader.loadTextureAP("/res/tools/mirror_horiz.ap2", fallbackTexture);
        cursorTextures[7]   = TextureLoader.loadTextureAP("/res/tools/mirror_vert.ap2", fallbackTexture);
        cursorTextures[8]   = TextureLoader.loadTextureAP("/res/tools/palette.ap2", fallbackTexture);
        cursorTextures[9]   = TextureLoader.loadTextureAP("/res/tools/save.ap2", fallbackTexture);
        cursorTextures[10]  = TextureLoader.loadTextureAP("/res/tools/export.ap2", fallbackTexture);
        cursorTextures[11]  = TextureLoader.loadTextureAP("/res/tools/open.ap2", fallbackTexture);
        cursorTextures[12]  = TextureLoader.loadTextureAP("/res/tools/cursor_text.ap2", fallbackTexture);

        toolTexts = new String[]{
                "Pencil Tool",
                "Erase Tool",
                "Fill Tool",
                "Selection Tool",
                "Move Selection",
                "Move Pixels",
                "Mirror (Horizontal)",
                "Mirror (Vertical)",
                "Edit Palette",
                "Save File",
                "Export Image",
                "Open File",
                ""
        };

        font = TextureLoader.loadTexture("/res/font.png", fallbackTexture);
        fontIndices = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!\"#$%&\'()*+,-./:;<=>?@[\\]^_ {|}~0123456789";

        layerBackground = TextureLoader.loadTextureAP("/res/layerbg_deselected.ap2", fallbackTexture);
        layerBackground_active = TextureLoader.loadTextureAP("/res/layerbg_selected.ap2", fallbackTexture);
    }

    public static void drawBitmapLayers(int [][][] bitmaps, Color[] bitmapColors){
        //Only draw active layers, as the rest would just waste time, and aren't visible in the ui
        for(int layer = 0; layer < 7; layer++){
            for(int y = 0; y < 32; y++){
                for(int x = 0; x < 32; x++){
                    //Draw pixel
                    if(bitmaps[x][y][layer] == -1) {
                        GL11.glColor3f(0, 0, 0);
                        if(layer != 0){
                            continue;
                        }
                    }else {
                        GL11.glColor3ub((byte) bitmapColors[bitmaps[x][y][layer]].getRed(), (byte) bitmapColors[bitmaps[x][y][layer]].getGreen(), (byte) bitmapColors[bitmaps[x][y][layer]].getBlue());
                    }

                    GL11.glBegin(GL11.GL_QUADS);
                    {
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale + pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale + pixelScale);
                    }
                    GL11.glEnd();
                }
            }

        }
    }

    public static void drawBitmapLayersSplit(int [][][] bitmaps, Color[] bitmapColors){
        //Only draw active layers, as the rest would just waste time, and aren't visible in the ui
        for(int layer = 0; layer < 7; layer++){
            int uioffsetX = (9 * uiScale) + (32 * pixelScale);
            int uioffsetY = (18 * uiScale) * (6 - layer) + uiScale;
            for(int y = 0; y < 32; y++){
                for(int x = 0; x < 32; x++){
                    //Draw pixel
                    if(bitmaps[x][y][layer] == -1) {
                        GL11.glColor3f(0, 0, 0);
                    }else {
                        GL11.glColor3ub((byte) bitmapColors[bitmaps[x][y][layer]].getRed(), (byte) bitmapColors[bitmaps[x][y][layer]].getGreen(), (byte) bitmapColors[bitmaps[x][y][layer]].getBlue());
                    }
                    GL11.glBegin(GL11.GL_QUADS);
                    {
                        GL11.glVertex2i(uioffsetX + (x * 2),                     uioffsetY + (y * (2 * rawScale)));
                        GL11.glVertex2i(uioffsetX + (x * 2) + (2 * rawScale),    uioffsetY + (y * (2 * rawScale)));
                        GL11.glVertex2i(uioffsetX + (x * 2) + (2 * rawScale),    uioffsetY + (y * (2 * rawScale)) + (2 * rawScale));
                        GL11.glVertex2i(uioffsetX + (x * 2),                     uioffsetY + (y * (2 * rawScale)) +(2 * rawScale));
                    }
                    GL11.glEnd();
                }
            }

        }
    }

    public static void drawSelection(Selection selection){
        if(selection.selectionStage >= 1){
            //Render first point
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2i((8 * uiScale) + (selection.x1 * pixelScale),                selection.y1 * pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x1 * pixelScale) + pixelScale,   selection.y1 * pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x1 * pixelScale) + pixelScale,   selection.y1 * pixelScale + pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x1 * pixelScale),                selection.y1 * pixelScale + pixelScale);
            }
            GL11.glEnd();
        }

        if(selection.selectionStage >= 2){
            //Render second point
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2i((8 * uiScale) + (selection.x2 * pixelScale),                selection.y2 * pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x2 * pixelScale) + pixelScale,   selection.y2 * pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x2 * pixelScale) + pixelScale,   selection.y2 * pixelScale + pixelScale);
                GL11.glVertex2i((8 * uiScale) + (selection.x2 * pixelScale),                selection.y2 * pixelScale + pixelScale);
            }
            GL11.glEnd();
        }

        if(CURSOR_BLINK && selection.selectionStage == 3){
            for(int x = selection.x1; x <= selection.x2; x++){
                for(int y = selection.y1; y <= selection.y2; y++){
                    //Skip interior space
                    if ((x != selection.x1 && x != selection.x2) && (y != selection.y1 && y != selection.y2))
                        continue;

                    if((y + x ) % 2 == 0) {
                        GL11.glColor3f(0.5f, 0.5f, 0.5f);
                    }else{
                        GL11.glColor3f(0f, 0f, 0f);
                    }

                    GL11.glBegin(GL11.GL_QUADS);
                    {
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale + pixelScale, y * pixelScale + pixelScale);
                        GL11.glVertex2i((8 * uiScale) + x * pixelScale,              y * pixelScale + pixelScale);
                    }
                    GL11.glEnd();
                }
            }
        }
    }

    public static void drawUI(int toolmode, Color[] palette, int selectedColor, int selectedLayer, boolean drawToolTip){
        /* Draw toolbar */

        //Toolbar background
        GL11.glColor3f(0.125f,0.125f,0.125f);
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2i(0,              0);
            GL11.glVertex2i((8 * uiScale),  0);
            GL11.glVertex2i((8 * uiScale),  (32 * pixelScale) + (2 * pixelScale));
            GL11.glVertex2i(0,              (32 * pixelScale) + (2 * pixelScale));
        }
        GL11.glEnd();

        //Selected tool
        int selectedToolHighlightPos = toolmode;
        if(toolmode == 12)
            selectedToolHighlightPos = 8;

        GL11.glColor3ub((byte)0,(byte)74,(byte)127);
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2i(0,              8 * uiScale * selectedToolHighlightPos);
            GL11.glVertex2i((8 * uiScale),  8 * uiScale * selectedToolHighlightPos);
            GL11.glVertex2i((8 * uiScale),  8 * uiScale * selectedToolHighlightPos + (8 * uiScale));
            GL11.glVertex2i(0,              8 * uiScale * selectedToolHighlightPos + (8 * uiScale));
        }
        GL11.glEnd();


        //Tool icons
        for(int i = 0; i < cursorTextures.length - 1; i++){
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1,1,1);
            cursorTextures[i].Bind(0);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glTexCoord2i(0,0); GL11.glVertex2i(0,                  (i * (8 * uiScale)));
                GL11.glTexCoord2i(1,0); GL11.glVertex2i(0 + (8 * uiScale),  (i * (8 * uiScale)));
                GL11.glTexCoord2i(1,1); GL11.glVertex2i(0 + (8 * uiScale),  (i * (8 * uiScale)) + (8 * uiScale));
                GL11.glTexCoord2i(0,1); GL11.glVertex2i(0,                  (i * (8 * uiScale)) + (8 * uiScale));
            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        /*Draw color palette*/
        GL11.glColor3f(0,0,0);
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2i((8 * uiScale),                      (32 * pixelScale) );
            GL11.glVertex2i((8 * uiScale) + (32 * pixelScale),  (32 * pixelScale) );
            GL11.glVertex2i((8 * uiScale) + (32 * pixelScale),  (32 * pixelScale) + (2 * pixelScale) );
            GL11.glVertex2i((8 * uiScale),                      (32 * pixelScale) + (2 * pixelScale) );
        }
        GL11.glEnd();

        for(int i = 0; i < palette.length; i++){
            int uiHeight = (selectedColor == i) ? 0 : 1;
            GL11.glColor3ub((byte)palette[i].getRed(), (byte)palette[i].getGreen(), (byte)palette[i].getBlue());
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2i((8 * uiScale) + (2 * pixelScale * i),                       (32 * pixelScale) + (uiHeight * pixelScale));
                GL11.glVertex2i((8 * uiScale) + (2 * pixelScale * i) + (2 * pixelScale),    (32 * pixelScale) + (uiHeight * pixelScale));
                GL11.glVertex2i((8 * uiScale) + (2 * pixelScale * i) + (2 * pixelScale),    (32 * pixelScale) + (uiHeight * pixelScale) + (2 * pixelScale));
                GL11.glVertex2i((8 * uiScale) + (2 * pixelScale * i),                       (32 * pixelScale) + (uiHeight * pixelScale) + (2 * pixelScale));
            }
            GL11.glEnd();
        }

        /*Draw layers BG*/

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for(int i = 0; i < 7; i++){
            if(i == selectedLayer){
                layerBackground_active.Bind(0);
            }else{
                layerBackground.Bind(0);
            }
            GL11.glColor3f(1,1,1);
            GL11.glBegin(GL11.GL_QUADS);
            {
                int uioffset = (8 * uiScale) + (32 * pixelScale);
                GL11.glTexCoord2i(0,0); GL11.glVertex2i(uioffset,                   (18 * uiScale * (6 - i)) );
                GL11.glTexCoord2i(1,0); GL11.glVertex2i(uioffset + (18 * uiScale),  (18 * uiScale * (6 - i)) );
                GL11.glTexCoord2i(1,1); GL11.glVertex2i(uioffset + (18 * uiScale),  (18 * uiScale * (6 - i)) + (18 * uiScale));
                GL11.glTexCoord2i(0,1); GL11.glVertex2i(uioffset,                   (18 * uiScale * (6 - i)) + (18 * uiScale));
            }
            GL11.glEnd();

        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        /*Logo*/

        GL11.glColor3f(0,0,0);
        GL11.glBegin(GL11.GL_QUADS);
        {
            int uioffsetX = (8 * uiScale) + (32 * pixelScale);
            int uioffsetY = (32 * pixelScale) + (2 * pixelScale) - (40 * rawScale);
            GL11.glVertex2i(uioffsetX,                   uioffsetY);
            GL11.glVertex2i(uioffsetX + (72 * rawScale), uioffsetY);
            GL11.glVertex2i(uioffsetX + (72 * rawScale), uioffsetY + (40 * rawScale));
            GL11.glVertex2i(uioffsetX,                   uioffsetY + (40 * rawScale));
        }
        GL11.glEnd();

        /*Draw mouse cursor*/
        DoubleBuffer mouseBufferX = BufferUtils.createDoubleBuffer(1), mouseBufferY = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(window, mouseBufferX, mouseBufferY);
        mouseX = mouseBufferX.get();
        mouseY = mouseBufferY.get();

        mouseXPixel = (int)mouseX / pixelScale;
        mouseYPixel = (int)mouseY / pixelScale;

        //Draw pixel preview
        if(CURSOR_BLINK && (mouseXPixel > 1 && mouseXPixel < 34) && (mouseYPixel >= 0 && mouseYPixel < 32)){
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(0.5f, 0.5f, 0.5f);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2i(mouseXPixel * pixelScale,              mouseYPixel * pixelScale);
                GL11.glVertex2i(mouseXPixel * pixelScale + pixelScale, mouseYPixel * pixelScale);
                GL11.glVertex2i(mouseXPixel * pixelScale + pixelScale, mouseYPixel * pixelScale + pixelScale);
                GL11.glVertex2i(mouseXPixel * pixelScale,              mouseYPixel * pixelScale + pixelScale);
            }
            GL11.glEnd();
        }

        if(drawToolTip) {
            int mouseTool = mouseYPixel / 2;

            if (mouseTool >= 12)
                mouseTool = 12;
            if(mouseTool < 0)
                mouseTool = 0;

            drawText(toolTexts[mouseTool], (int) mouseX + pixelScale, (int) mouseY + pixelScale, 1, true, Color.white, Color.black);
        }


        GL11.glColor3f(1,1,1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //If inside bitmap field
        if((mouseXPixel > 1 && mouseXPixel < 34) && (mouseYPixel >= 0 && mouseYPixel < 32) && toolmode != 8) {
            cursorTextures[toolmode].Bind(0);
        }else{
            uiCursor.Bind(0);
        }

        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glTexCoord2i(0,0); GL11.glVertex2d( mouseX,                    mouseY );
            GL11.glTexCoord2i(1,0); GL11.glVertex2d( mouseX + (2 * pixelScale), mouseY );
            GL11.glTexCoord2i(1,1); GL11.glVertex2d( mouseX + (2 * pixelScale), mouseY + (2 * pixelScale) );
            GL11.glTexCoord2i(0,1); GL11.glVertex2d( mouseX,                    mouseY + (2 * pixelScale) );
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
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

                GL11.glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
                GL11.glBegin(GL11.GL_QUADS);
                {
                    GL11.glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset);
                    GL11.glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset);
                    GL11.glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset + pixelScale, (y * pixelScale) + uiOffset + pixelScale);
                    GL11.glVertex2i((2 * pixelScale) + (x * pixelScale) + uiOffset, (y * pixelScale) + uiOffset + pixelScale);
                }
                GL11.glEnd();
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
            GL11.glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2i((2 * pixelScale) + uiOffset,                       uiOffset + uiOffsetY);
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 24),   uiOffset + uiOffsetY);
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 24),   uiOffset + uiOffsetY + (pixelScale * 4));
                GL11.glVertex2i((2 * pixelScale) + uiOffset,                       uiOffset + uiOffsetY + (pixelScale * 4));
            }
            GL11.glEnd();

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
            GL11.glBegin(GL11.GL_QUADS);

            int noRed = Clamp(Palette[selectedColor].getRed() - editingColor.getRed(), 0, 255);
            int noGreen = Clamp(Palette[selectedColor].getGreen() - editingColor.getGreen(), 0, 255);
            int noBlue = Clamp(Palette[selectedColor].getBlue() - editingColor.getBlue(), 0, 255);

            int maxRed = Clamp(Palette[selectedColor].getRed() + editingColor.getRed(), 0, 255);
            int maxGreen = Clamp(Palette[selectedColor].getGreen() + editingColor.getGreen(), 0, 255);
            int maxBlue = Clamp(Palette[selectedColor].getBlue() + editingColor.getBlue(), 0, 255);

            GL11.glColor3ub((byte)noRed, (byte)noGreen, (byte)noBlue);
            {
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale),         (uiOffset + pixelScale) + uiOffsetY);
                GL11.glColor3ub((byte)maxRed, (byte)maxGreen, (byte)maxBlue);
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 23),    (uiOffset + pixelScale) + uiOffsetY);
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale * 23),    (uiOffset + pixelScale) + uiOffsetY + (pixelScale * 2));
                GL11.glColor3ub((byte)noRed, (byte)noGreen, (byte)noBlue);
                GL11.glVertex2i((2 * pixelScale) + uiOffset + (pixelScale),         (uiOffset + pixelScale) + uiOffsetY + (pixelScale * 2));
            }
            GL11.glEnd();

            //Get correct value for slider bar
            float colorValue = 0.0f;
            switch(colorIndex){
                case 0:
                    colorValue = (Palette[selectedColor].getRed() / 255f);
                    break;
                case 1:
                    colorValue = (Palette[selectedColor].getGreen() / 255f);
                    break;
                case 2:
                    colorValue = (Palette[selectedColor].getBlue() / 255f);
                    break;
            }

            //Draw slider bar
            float colorPosition = colorValue * (pixelScale * 21) + pixelScale;
            UIColor = new Color(64, 64, 64);
            GL11.glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2f((2 * pixelScale) + uiOffset + colorPosition,                   uiOffset + uiOffsetY);
                GL11.glVertex2f((2 * pixelScale) + uiOffset + colorPosition + (pixelScale),    uiOffset + uiOffsetY);
                GL11.glVertex2f((2 * pixelScale) + uiOffset + colorPosition + (pixelScale),    uiOffset + uiOffsetY + (pixelScale * 4));
                GL11.glVertex2f((2 * pixelScale) + uiOffset + colorPosition,                   uiOffset + uiOffsetY + (pixelScale * 4));
            }
            GL11.glEnd();
        }

        //Color Preview Border
        uiOffsetY = 21 * pixelScale;
        Color UIColor = new Color(127, 127, 127);
        GL11.glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY);
            GL11.glVertex2i((2 * pixelScale) + uiOffset + (13 * pixelScale),    uiOffsetY);
            GL11.glVertex2i((2 * pixelScale) + uiOffset + (13 * pixelScale),    uiOffsetY + (7 * pixelScale));
            GL11.glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY + (7 * pixelScale));
        }
        GL11.glEnd();

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
        GL11.glColor3f(UIColor.getRed() / 255f, UIColor.getGreen() / 255f, UIColor.getBlue() / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        {
            GL11.glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY);
            GL11.glVertex2i((2 * pixelScale) + uiOffset + (11 * pixelScale),    uiOffsetY);
            GL11.glVertex2i((2 * pixelScale) + uiOffset + (11 * pixelScale),    uiOffsetY + (5 * pixelScale));
            GL11.glVertex2i((2 * pixelScale) + uiOffset,                        uiOffsetY + (5 * pixelScale));
        }
        GL11.glEnd();
    }

    public static void drawText(String text, int posX, int posY, int scale, boolean background, Color textColor, Color backgroundColor){

        if(text.length() < 1)
            return;

        char[] charArray = text.toCharArray();

        int charSize = pixelScale * scale;

        if(background){
            GL11.glColor3ub((byte)backgroundColor.getRed(), (byte)backgroundColor.getGreen(), (byte)backgroundColor.getBlue());
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glVertex2f(posX - 2f,                              posY - 2f);
                GL11.glVertex2f(posX + 2f + (charSize * text.length()), posY - 2f);
                GL11.glVertex2f(posX + 2f + (charSize * text.length()), posY + 2f + charSize);
                GL11.glVertex2f(posX - 2f,                              posY + 2f + charSize);
            }
            GL11.glEnd();
        }

        for(int charIndex = 0; charIndex < charArray.length; charIndex++){
            char[] chars = fontIndices.toCharArray();
            int indexLinear = ArrayUtils.indexOf(chars, charArray[charIndex]);

            int indexX = indexLinear % 10;
            int indexY = indexLinear / 10;

            float textureOffsetX = indexX / 10f;
            float textureOFfsetY = indexY / 10f;

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor3ub((byte)textColor.getRed(), (byte)textColor.getGreen(), (byte)textColor.getBlue());
            font.Bind(0);
            GL11.glBegin(GL11.GL_QUADS);
            {
                GL11.glTexCoord2f(textureOffsetX,                   textureOFfsetY);                GL11.glVertex2f(posX + (charIndex * charSize),              posY);
                GL11.glTexCoord2f( (1f / 10f) + textureOffsetX,     textureOFfsetY);                GL11.glVertex2f(posX + (charIndex * charSize) + charSize,   posY);
                GL11.glTexCoord2f( (1f / 10f) + textureOffsetX,     (1f / 10f) + textureOFfsetY);   GL11.glVertex2f(posX + (charIndex * charSize) + charSize,   posY + charSize);
                GL11.glTexCoord2f(textureOffsetX,                   (1f / 10f) + textureOFfsetY);   GL11.glVertex2f(posX + (charIndex * charSize),              posY + charSize);

            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_TEXTURE_2D);

        }
    }

    public static boolean windowShouldClose(){
        return GLFW.glfwWindowShouldClose(window);
    }
    public static void pollWindowEvents(){GLFW.glfwPollEvents();}
    public static void completeFrame(){
        cycle++;

        int blinkRate = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor()).refreshRate() / 2;
        if(cycle % blinkRate == 0){
            CURSOR_BLINK = !CURSOR_BLINK;
        }

        GLFW.glfwSwapBuffers(window);
    }


    public static int Clamp(int value, int min, int max){
        if (value > max)
            return max;
        if(value < min)
            return min;

        return value;
    }
}