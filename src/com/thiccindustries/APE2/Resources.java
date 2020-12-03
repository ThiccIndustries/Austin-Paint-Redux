package com.thiccindustries.APE2;

import com.thiccindustries.APE2.io.Texture;
import com.thiccindustries.APE2.io.TextureLoader;

import java.io.IOException;

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
            fallbackTexture = TextureLoader.loadTextureAPNoFallback("/res/fallback.ap2");
        } catch (IOException e) {
            e.printStackTrace();
        }

        uiCursor = TextureLoader.loadTextureAP("/res/ui/cursor.ap2", fallbackTexture);

        //Load Tool Cursors
        cursorTextures = new Texture[Tool.values().length];

        for(int tool = 0; tool < cursorTextures.length; tool++) {

            if(Tool.values()[tool].loadCursor())
                cursorTextures[tool] = TextureLoader.loadTextureAP("/res/tools/" + Tool.values()[tool].toString() + ".ap2", fallbackTexture);
            else
                cursorTextures[tool] = uiCursor;

        }


        fontIndices             = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!\"#$%&'()*+,-./:;<=>?@[\\]^_ {|}~0123456789";

        font                    = TextureLoader.loadTextureHDAP("/res/ui/font.ap2", fallbackTexture);
        layerBackground         = TextureLoader.loadTextureAP("/res/ui/layerbg_deselected.ap2", fallbackTexture);
        layerBackground_active  = TextureLoader.loadTextureAP("/res/ui/layerbg_selected.ap2", fallbackTexture);


    }


    public enum Tool{

        //Toolbar tools
        pencil      ("Pencil Tool"),
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
        open        ("Open Image"),

        //Internal Tools
        txt_color   (color, true),
        save_warn   (pencil, false),
        pencil_wait (pencil, true);

        private final String    name;       //The display name of this tool
        private final Boolean   display;    //True if tool should be displayed on the toolbar
        private final Boolean   hasCursor;  //True if a custom cursor texture needs to be loaded
        private final Tool      displayTool;//What tool is highlighted when selected. For visible tools, this is a self reference.

        //Constructor for a toolbar tool
        Tool(String name){
            this.name           = name;
            this.display        = true;
            this.displayTool    = this;
            this.hasCursor      = true;
        }

        //Constructor for internal tools
        Tool(Tool displayTool, boolean hasCursor){
            this.name           = "missingno.";
            this.display        = false;
            this.displayTool    = displayTool;
            this.hasCursor      = hasCursor;
        }

        public String getName()     { return name; }
        public Boolean display()    { return display; }
        public Tool getdisplayTool(){ return displayTool; }
        public Boolean loadCursor() { return hasCursor; }
    }
}
