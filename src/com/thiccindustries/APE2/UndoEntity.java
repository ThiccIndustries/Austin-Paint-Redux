package com.thiccindustries.APE2;

import java.util.LinkedList;

public class UndoEntity {
    public LinkedList<Pixel2i> pixelChanges = new LinkedList<Pixel2i>();

    public void AddPixel(int x, int y, int layer, int colorIndex){
        pixelChanges.addFirst(new Pixel2i(x, y, layer, colorIndex));
    }

    public class Pixel2i{
        public int x;
        public int y;
        public int layer;
        public int colorIndex;

        public Pixel2i(int x, int y, int layer, int colorIndex){
            this.x = x;
            this.y = y;
            this.layer = layer;
            this.colorIndex = colorIndex;
        }
    }

}

