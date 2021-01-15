//I rike the way this file looks
package com.thiccindustries.APE2;

import java.awt.*;
import java.io.IOException;
import com.thiccindustries.TLib.*;

public class Resources {

    public static String fontIndices;
    public static Texture uiCursor;
    public static Texture[] cursorTextures;
    public static Texture layerBackground;
    public static Texture layerBackground_active;
    public static Texture font;

    public static void InitResources(){
        Texture fallbackTexture = null;
        try{
            fallbackTexture = APTextureLoader.loadTextureNoFallback("/res/fallback.ap2");
        } catch (IOException e) {
            e.printStackTrace();
        }

        uiCursor = APTextureLoader.loadTexture("/res/ui/cursor.ap2", fallbackTexture);

        //Load Tool Cursors
        cursorTextures = new Texture[Tool.values().length];

        for(int tool = 0; tool < cursorTextures.length; tool++) {

            if(Tool.values()[tool].loadCursor())
                cursorTextures[tool] = APTextureLoader.loadTexture("/res/tools/" + Tool.values()[tool].getCursor() + ".ap2", fallbackTexture);
            else
                cursorTextures[tool] = uiCursor;

        }


        fontIndices             = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!\"#$%&'()*+,-./:;<=>?@[\\]^_ {|}~0123456789";

        font                    = APTextureLoader.loadHDTexture("/res/ui/font.ap2", fallbackTexture);
        layerBackground         = APTextureLoader.loadTexture("/res/ui/layerbg_deselected.ap2", fallbackTexture);
        layerBackground_active  = APTextureLoader.loadTexture("/res/ui/layerbg_selected.ap2", fallbackTexture);


    }

    public enum Tool{

        //Toolbar tools
        pencil      ("Pencil"),
        erase       ("Erase Tool"),
        fill        ("Fill Tool"),
        select      ("Selection Tool"),
        move_sel    ("Move Selection"),
        move_pixel  ("Move Pixels"),
        mirror_h    ("Mirror Horizontal"),
        mirror_v    ("Mirror Vertical"),
        flatten     ("Flatten"),
        color       ("Edit Palette"),
        save        ("Save Image"),
        export      ("Export BMP"),
        create      ("New Image"),
        open        ("Open Image"),
        settings    ("Settings"),

        //Internal Tools
        txt_color   (color,     "cursor_txt"),  //Hex value tool
        txt_scale   (settings,  "cursor_txt"),  //S cale value tool
        txt_blink   (settings,  "cursor_txt"),  //blink rate tool
        pencil_wait (pencil,    "pencil"),      //Wait for a new mouse press to begin pencil tool (After closing dialog)
        lock_save   (save,      null),          //Lock window while save is open
        lock_export (export,    null),          //Lock window while export is open
        lock_open   (open,      null),          //Lock window while open is open
        lock_color  (color,     null),          //Lock window while saving or opening palette
        save_warn   (pencil,    null),          //Warn the user if exiting w/o saving
        new_warn    (pencil,    null),          //Warn the user if creating a new file w/o saving
        save_color  (color,     null),          //Save the palette
        load_color  (color,     null);          //Load the palette

        private final String    dspName;    //The display name of this tool
        private final Boolean   display;    //True if tool should be displayed on the toolbar
        private final Boolean   hasCursor;  //True if a custom cursor texture needs to be loaded
        private final String    cursorName; //The name of the cursor file to be loaded
        private final Tool      displayTool;//What tool is highlighted when selected. For visible tools, this is a self reference.

        //Constructor for a toolbar tool
        Tool(String name){
            this.dspName        = name;
            this.display        = true;
            this.displayTool    = this;
            this.hasCursor      = true;
            this.cursorName     = this.toString();
        }

        //Constructor for internal tools
        Tool(Tool displayTool, String cursorName){
            this.dspName        = "missingno.";
            this.display        = false;
            this.displayTool    = displayTool;
            this.hasCursor      = cursorName != null;
            this.cursorName     = cursorName;
        }

        public String getName()     { return dspName; }
        public Boolean display()    { return display; }
        public Tool getDisplayTool(){ return displayTool; }
        public Boolean loadCursor() { return hasCursor; }
        public String getCursor()   { return cursorName; }

    }


}
