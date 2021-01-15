package com.thiccindustries.APE2;

import com.thiccindustries.TLib.APTextureLoader;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;

public class FileManager {
    private static final Color[] defaultPalette = {
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

    //Save the file
    public static void saveFileFromImage(String filePath, int[][][] pixelArray, Color[] palette){
        File fileToSave = new File(filePath);

        FileOutputStream fos;

        /*Doing this with a hex string is EXTREMELY dumb, but I dont care*/
        StringBuilder sb = new StringBuilder();

        /*Really austin?*/
        sb.append("41555354494E5041494E540056322E30"); // "AUSTIN.PAINT.v2.0"

        //Add palette information
        for(int i = 0; i < 16; i++){
            int r = palette[i].getRed();
            int g = palette[i].getGreen();
            int b = palette[i].getBlue();
            sb.append(String.format("%02X", r))
              .append(String.format("%02X", g))
              .append(String.format("%02X", b));
        }

        //Add legacy AP2 pixel array for APE and austin paint 2
        int[][][] _pixelArray = new int[32][32][7];

        for(int i = 0; i < 7; i++){
            for(int x = 0; x < 32; x++){
                for (int y = 0; y < 32; y++){
                    _pixelArray[x][y][i] = pixelArray[x][y][i];
                }
            }
        }

        int[][] flatPixelArray = flattenAPImage(_pixelArray);



        for(int y = 0; y < 32; y++) {
            for (int x = 0; x < 16; x++) {
                sb.append(Integer.toHexString(flatPixelArray[x * 2][y]))
                  .append(Integer.toHexString(flatPixelArray[(x * 2) + 1][y]));
            }
        }

       // APR header
        sb.append("2E415041494E542E2E52454455582E2E"); // Austin did it so im allowed to do it as well. The best part is that I don't even test for it.

        int[] unusedColors = new int[7];
        boolean[] layerEmpty = new boolean[7];

        for(int i = 0; i < 7; i++){
            unusedColors[i] = findUnusedColor(_pixelArray, i);


            layerEmpty[i] = isLayerEmpty(_pixelArray, i);
            if(layerEmpty[i])
                sb.append("0");
            else
                sb.append("1");
        }

        sb.append("F");

        for(int i = 0; i < 7; i++){
            if(unusedColors[i] == -1){
                System.err.println("No transparency color for layer: " + i + "using 0. this WILL cause issues.");
                unusedColors[i] = 0;
            }

            if(layerEmpty[i]) //Skip unused layers to save on file space
                continue;

            for(int y = 0; y < 32; y++){
                for(int x = 0; x < 16; x++){
                    if(_pixelArray[(x * 2)][y][i] == -1)
                        _pixelArray[(x * 2)][y][i] = unusedColors[i];

                    if(_pixelArray[(x * 2) + 1][y][i] == -1)
                        _pixelArray[(x * 2) + 1][y][i] = unusedColors[i];

                    sb.append(Integer.toHexString(_pixelArray[x * 2][y][i]))
                      .append(Integer.toHexString(_pixelArray[(x * 2) + 1][y][i]));
               }
            }
        }

        //Transparency Color Headers
        for(int i = 0; i < 7; i++){
            sb.append(Integer.toHexString(unusedColors[i]));
        }

        sb.append("F");
        /*Why does StringBuilder even need a function to do this, why can i not just cast it*/

        String hexDump = sb.toString();

        byte[] rawPixelData = new byte[hexDump.length() / 2];

        //Convert pixel array from one byte per pixel to 4 bits per pixel
        for(int i = 0; i < (rawPixelData.length * 2); i+=2){
            rawPixelData[(i / 2)] = (byte)((Character.digit(hexDump.charAt(i), 16) << 4)
                    + Character.digit(hexDump.charAt(i + 1), 16));
        }
        try {
            //Write file
            fileToSave.createNewFile(); //I dont care what intellij says, it doesn't work if i dont add this.
            fos = new FileOutputStream(fileToSave);
            fos.write(rawPixelData);
            fos.close();
        }catch(IOException e){
            System.err.print("Unknown IO Error.");
        }
    }

    //Save BMP file
    public static void saveBMPFromImage(String filePath, int[][][] pixelArray, Color[] palette){
        int[][] flatPixelArray = flattenAPImage(pixelArray);
        StringBuilder sb = new StringBuilder();
        //Header Info
        sb.append("424D760200000000000076000000280000002000000020000000010004000000000000000000C40E0000C40E00001000000010000000");

        //Blue Green Red fuck bmp all my homies hate bmp
        for(int i = 0; i < 16; i++){
            int b = palette[i].getBlue();
            int g = palette[i].getGreen();
            int r = palette[i].getRed();

            sb.append(String.format("%02X", b))
              .append(String.format("%02X", g))
              .append(String.format("%02X", r)).append("FF");
        }

        for(int y = 31; y >= 0; y--){
            for(int x = 0; x < 16; x++){
                sb.append(Integer.toHexString(flatPixelArray[x * 2][y]))
                  .append(Integer.toHexString(flatPixelArray[(x * 2) + 1][y]));
            }
        }

        String hexDump = sb.toString();

        //Convert data
        byte[] byteData = new byte[hexDump.length() / 2];
        for(int i = 0; i < (byteData.length * 2); i+=2) {
            byteData[i / 2] = (byte)((Character.digit(hexDump.charAt(i), 16) << 4)
                                    + Character.digit(hexDump.charAt(i + 1), 16));
        }

        File file = new File(filePath);
        FileOutputStream fos;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(byteData);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static int findUnusedColor(int[][][] pixelArray, int layer) {
        int unusedColor = 0;
        boolean colorUnused = false;
        for(int i = 0; i < 16 && !colorUnused; i++){
            colorUnused = true;
            unusedColor = i;
            for(int x = 0; x < 32 && colorUnused; x++){
                for(int y = 0; y < 32; y++){
                    if (pixelArray[x][y][layer] == i) {
                        colorUnused = false;
                        break;
                    }
                }
            }
        }

        //No unused colors
        if(!colorUnused) {
            return -1;
        }
        else {
            return unusedColor;
        }
    }

    private static boolean isLayerEmpty(int[][][] pixelArray, int layer){
        boolean empty = true;
        for(int x = 0; x < 32 && empty; x++){
            for(int y = 0; y < 32; y++){
                if (pixelArray[x][y][layer] != -1) {
                    empty = false;
                    break;
                }
            }
        }

        return empty;
    }

    //Load a saved file
    public static APFile loadPixelArrayFromFile(String filePath, boolean internal) {

        System.out.println("Loading file: " + filePath + (internal ? " [internal]" : ""));
        APFile fileLoaded = new APFile();
        fileLoaded.pixelArray = new int[32][32][7];

        for(int layer = 0; layer < 7; layer++) {
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    fileLoaded.pixelArray[x][y][layer] = -1;
                }
            }
        }

        //Create a local copy of the palette to prevent destructive changes
        System.arraycopy(defaultPalette, 0, fileLoaded.palette, 0, 16);

        byte[] rawPixelData = new byte[0];
        try {
            if(internal){
                InputStream ios = APTextureLoader.class.getResourceAsStream(filePath);
                rawPixelData = IOUtils.toByteArray(ios);
            }else {
                File file = new File(filePath);
                FileInputStream fis;
                rawPixelData = new byte[(int) file.length()];

                fis = new FileInputStream(file);
                fis.read(rawPixelData); //No its literally not.
                fis.close();
            }
        } catch (FileNotFoundException e) {
            System.err.print("File did not exist.");
            return fileLoaded;
        } catch (IOException e) {
            System.err.print("Unknown IO Error.");
        }
        //Get Color Palette Information
        for (int i = 0; i < (16 * 3); i += 3) {
            fileLoaded.palette[i / 3] = new Color(rawPixelData[i + 16] & 0xff, rawPixelData[i + 16 + 1] & 0xff, rawPixelData[i + 16 + 2] & 0xff);
        }

        System.out.println(rawPixelData.length);
        if (rawPixelData.length == 576 || rawPixelData.length == 593 || rawPixelData.length == 65536) {
            System.out.println("File format is: AP2 / APE / APA");

            for(int i = 1; i < 7; i++){
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 32; x++){
                        fileLoaded.pixelArray[x][y][i] = -1;
                    }
                }
            }
            /*Converting from Byte -> Char -> Int is probably the dumbest thing ive ever done but it works*/
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 16; x++) {
                    Byte currentByte = rawPixelData[x + (y * 16) + 64];
                    char firstPixelHexChar = String.format("%02x", currentByte).charAt(0);
                    char secondPixelHexChar = String.format("%02x", currentByte).charAt(1);
                    fileLoaded.pixelArray[x * 2][y][0] = Character.digit(firstPixelHexChar, 16);

                    if(fileLoaded.pixelArray[x * 2][y][0] == 0)
                        fileLoaded.pixelArray[x * 2][y][0] = -1;
                    fileLoaded.pixelArray[x * 2 + 1][y][0] = Character.digit(secondPixelHexChar, 16);

                    if(fileLoaded.pixelArray[x * 2 + 1][y][0] == 0)
                        fileLoaded.pixelArray[x * 2 + 1][y][0] = -1;
                }
            }

        }
        if(rawPixelData.length > 576 && rawPixelData.length != 4180 && rawPixelData.length != 593 && rawPixelData.length != 65536){
            System.out.println("File format is: A.P.R Compressed.");
            boolean[] activeLayers = new boolean[7];
            int activeLayerTotal = 0;
            //Get active layers
            for(int i = 0; i < 4; i++){
                int toplayer = rawPixelData[592 + i] >> 4;
                int bottomlayer = rawPixelData[592 + i] & 0x0F;
                if(toplayer == 1){
                    activeLayers[(i * 2)] = true;
                    activeLayerTotal++;
                }
                if(bottomlayer == 1) {
                    activeLayers[(i * 2) + 1] = true;
                    activeLayerTotal++;
                }
            }


            int transColorOffset = (592 + 4 + (activeLayerTotal * 512));
            //Get transparency colors
            int[] transColors = new int[8];
            for(int i = 0; i < 4; i++){
                byte transparencyColor = rawPixelData[transColorOffset + i];
                char topHalf = String.format("%02x", transparencyColor ).charAt(0);
                transColors[i * 2] = Character.digit(topHalf, 16);
                char bottomHalf = String.format("%02x", transparencyColor).charAt(1);
                transColors[i * 2 + 1] = Character.digit(bottomHalf, 16);
            }
            int layerPosOffset = 0;
            for(int i = 0; i < 7; i++){
                if(!activeLayers[i]){
                    continue;
                }
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 16; x++) {
                        Byte currentByte = rawPixelData[x + (y * 16) + 592 + 4 + layerPosOffset];
                        char firstPixelHexChar = String.format("%02x", currentByte).charAt(0);
                        char secondPixelHexChar = String.format("%02x", currentByte).charAt(1);
                        fileLoaded.pixelArray[x * 2][y][i]     = Character.digit(firstPixelHexChar, 16);
                        fileLoaded.pixelArray[x * 2 + 1][y][i] = Character.digit(secondPixelHexChar, 16);

                        if(fileLoaded.pixelArray[x * 2][y][i] == transColors[i]){
                            fileLoaded.pixelArray[x * 2][y][i] = -1;
                        }

                        if(fileLoaded.pixelArray[x * 2 + 1][y][i] == transColors[i]){
                            fileLoaded.pixelArray[x * 2 + 1][y][i] = -1;
                        }
                    }
                }

                layerPosOffset+=512;
            }


        }
        if(rawPixelData.length == 4180){
            System.out.println("File format is: A.P.R. Uncompressed.");

            int transColorOffset = (592 + (7 * 512));
            //Get transparency colors
            int[] transColors = new int[8];
            for(int i = 0; i < 4; i++){
                byte transparencyColor = rawPixelData[transColorOffset + i];
                char topHalf = String.format("%02x", transparencyColor ).charAt(0);
                transColors[i * 2] = Character.digit(topHalf, 16);
                char bottomHalf = String.format("%02x", transparencyColor).charAt(1);
                transColors[i * 2 + 1] = Character.digit(bottomHalf, 16);
            }

            int layerPosOffset = 0;
            for(int i = 0; i < 7; i++){
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 16; x++) {
                        Byte currentByte = rawPixelData[x + (y * 16) + 592 + layerPosOffset];
                        char firstPixelHexChar = String.format("%02x", currentByte).charAt(0);
                        char secondPixelHexChar = String.format("%02x", currentByte).charAt(1);
                        fileLoaded.pixelArray[x * 2][y][i]     = Character.digit(firstPixelHexChar, 16);
                        fileLoaded.pixelArray[x * 2 + 1][y][i] = Character.digit(secondPixelHexChar, 16);

                        if(fileLoaded.pixelArray[x * 2][y][i] == transColors[i]){
                            fileLoaded.pixelArray[x * 2][y][i] = -1;
                        }

                        if(fileLoaded.pixelArray[x * 2 + 1][y][i] == transColors[i]){
                            fileLoaded.pixelArray[x * 2 + 1][y][i] = -1;
                        }
                    }
                }
                layerPosOffset+=512;
            }


        }
        return fileLoaded;
    }

    public static Settings readSettingsFile(String filePath){
        Settings settings = new Settings();

        File file = new File(filePath);

        FileInputStream fis;
        byte[] rawPixelData = new byte[(int) file.length()];

        try {
            fis = new FileInputStream(file);
            fis.read(rawPixelData); //No its literally not.
            fis.close();

        } catch (FileNotFoundException e) {
            System.err.print("File did not exist.");
            return null;
        } catch (IOException e) {
            System.err.print("Unknown IO Error.");
        }

        settings.scale      = rawPixelData[0];
        settings.blinkrate  = rawPixelData[1];
        settings.sync       = rawPixelData[2] == 1;

        System.out.println("Settings file loaded.");

        return settings;
    }

    public static void writeSettingsFile(String filePath, int scale, int blinkrate, boolean sync){
        byte[] settingBytes = new byte[3];

        settingBytes[0] = (byte)scale;
        settingBytes[1] = (byte)blinkrate;
        settingBytes[2] = (byte)(sync ? 1 : 0);

        File file = new File(filePath);
        FileOutputStream fos;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(settingBytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Flattens the Austin Paint Redux layered image into an array that can be read by APE / AP2
    public static int[][] flattenAPImage(int[][][] layeredArray){
        int[][] flattenedArray = new int[32][32];
        for(int i = 0; i < 7; i++){
            for(int y = 0; y < 32; y++){
                for(int x = 0; x < 32; x++){
                    if(layeredArray[x][y][i] == -1)
                        continue;

                    flattenedArray[x][y] = layeredArray[x][y][i];
                }
            }
        }
        return flattenedArray;
    }

    public static void initFileSystem(String folderDir) {

        System.out.println("Creating file system at: " + folderDir);
        File file = new File(folderDir);
        boolean success = file.mkdir();
        if (success) {
            System.out.println("Austin paint folder created successfully.");
        }else if (file.exists() && file.isDirectory()){
            System.out.println("Austin paint folder found.");
        }else{
            System.err.println("IO error while creating folder!");
        }
    }

    //Save a palette file
    public static void saveFileFromColors(String filePath, Color[] bitmapColors) {
        byte[] paletteBytes = new byte[48];

        for(int i = 0; i < 16; i++){
            paletteBytes[i * 3]     = (byte)bitmapColors[i].getRed();
            paletteBytes[i * 3 + 1] = (byte)bitmapColors[i].getGreen();
            paletteBytes[i * 3 + 2] = (byte)bitmapColors[i].getBlue();
        }

        File file = new File(filePath);
        FileOutputStream fos;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(paletteBytes);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Color[] loadColorsFromFile(String filePath) {
        Color[] loadedPalette = new Color[16];

        File file = new File(filePath);

        FileInputStream fis;
        byte[] rawPixelData = new byte[(int) file.length()];

        try {
            fis = new FileInputStream(file);
            fis.read(rawPixelData); //No its literally not.
            fis.close();

        } catch (FileNotFoundException e) {
            System.err.print("File did not exist.");
            return null;
        } catch (IOException e) {
            System.err.print("Unknown IO Error.");
            return null;
        }

        if(rawPixelData.length != 48){
            System.err.print("Not a palette file");
            return null;
        }

        for(int i = 0; i < 16; i++){
            loadedPalette[i] = new Color(
                    (int)rawPixelData[i * 3] & 0xFF,
                    (int)rawPixelData[i * 3 + 1] & 0xFF,
                    (int)rawPixelData[i * 3 + 2] & 0xFF);
        }

        return loadedPalette;
    }

    public static class APFile{
        public int[][][] pixelArray;
        public Color[] palette = new Color[16];
    }

    public static class FileList{
        public int[] fileSizes;
        public String[] fileNames;

        public FileList(int[] fileSizes, String[] fileNames){
            this.fileNames = fileNames;
            this.fileSizes = fileSizes;
        }
    }

    public static class Settings{
        public int blinkrate = 4;
        public int scale = 1;
        public boolean sync = false;
    }
}




