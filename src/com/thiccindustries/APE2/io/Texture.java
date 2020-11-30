package com.thiccindustries.APE2.io;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Texture {
    public int TileCountX;
    public int TileCountY;

    private int id;

    public Texture(int id, int tileCountX, int tileCountY){
        this.id = id;
        this.TileCountX = tileCountX;
        this.TileCountY = tileCountY;

    }

    public int getId(){
        return id;
    }

    public void Bind(int sampler){
        if(sampler >= 0 && sampler <= 31) {
            glEnable(GL_TEXTURE_2D);
            glActiveTexture(GL_TEXTURE0 + sampler);
            glBindTexture(GL_TEXTURE_2D, id);
        }
    }


}