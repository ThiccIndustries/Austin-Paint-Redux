/**This putrid texture loader was brought to you by Thicc Industries!**/

package com.thiccindustries.APE2.io;

import de.matthiasmann.twl.utils.PNGDecoder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TextureLoader {

    //Loads a texture with no fallback, program will crash if the file does not exist
    public static Texture loadTextureNoFallback(String fileName) throws NullPointerException, IOException {
        PNGDecoder decoder = new PNGDecoder(TextureLoader.class.getResourceAsStream(fileName));
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * decoder.getWidth() * decoder.getHeight());

        decoder.decode(buffer, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);

        //flip the buffer because opengl is weird
        buffer.flip();

        int id = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        //disable garbage filtering
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        //upload image data into texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        return new Texture(id, decoder.getWidth() / 16, decoder.getHeight() / 16);
    }

    //Loads a texture with fallback
    public static Texture loadTexture(String fileName, Texture fallback) {
        try {
            return loadTextureNoFallback(fileName);
        } catch (Exception e) {
            System.err.println("Texture File: " + fileName + " does not exist, using fallback texture!");
            return fallback;
        }
    }

    /*Loads an austin paint texture with no fallback*/
    public static Texture loadTextureAPNoFallback(String fileName) throws IOException {
        ByteBuffer buffer = loadTextureBytesAP(fileName);

        int id = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        //disable garbage filtering
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        //upload image data into texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 32, 32, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        return new Texture(id, 2, 2);
    }

    //Loads an ap texture with fallback
    public static Texture loadTextureAP(String fileName, Texture fallback){
        try{
            return loadTextureAPNoFallback(fileName);
        }catch (Exception e) {
            System.err.println("Texture File: " + fileName + " does not exist, using fallback texture!");
            return fallback;
        }
    }

    //Loads an HD ap texture with fallback
    public static Texture loadTextureHDAP(String fileName, Texture fallback){
        try{
            return loadTextureHDAPNoFallback(fileName);
        } catch (Exception e) {
            System.err.println("Texture File: " + fileName + " does not exist, using fallback texture!");
            e.printStackTrace();
            return fallback;
        }
    }

    public static Texture loadTextureHDAPNoFallback(String fileName) throws IOException, ArrayIndexOutOfBoundsException{
        ByteBuffer buffer = ByteBuffer.allocateDirect(96 * 64 * 4);

        InputStream ios = TextureLoader.class.getResourceAsStream(fileName);
        byte[] fileBytes = IOUtils.toByteArray(ios);

        byte[] palette = new byte[16 * 3];
        //Read color values
        for(int i = 0; i < 16; i++){
            palette[(i * 3)]        = fileBytes[(i * 3) + 16];      //Red
            palette[(i * 3) + 1]    = fileBytes[(i * 3) + 16 + 1];  //Green
            palette[(i * 3) + 2]    = fileBytes[(i * 3) + 16 + 2];  //Blue
        }

        //Get Layers from image
        int[][][] layeredPixelArray = FileManager.extractPixelArray(fileBytes);

        int[] stitchedHDArray = stitchHDAPLayers(layeredPixelArray);

        for(int i = 0; i < stitchedHDArray.length; i++){
                if(stitchedHDArray[i] == -1){
                    buffer.put((byte)0x00);
                    buffer.put((byte)0x00);
                    buffer.put((byte)0x00);
                    buffer.put((byte)0x00);
                }
                else {
                    buffer.put(palette[stitchedHDArray[i] * 3]);
                    buffer.put(palette[stitchedHDArray[i] * 3 + 1]);
                    buffer.put(palette[stitchedHDArray[i] * 3 + 2]);
                    buffer.put((byte) 0xFF);
                }
            }

        buffer.flip();

        int id = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        //disable garbage filtering
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        //upload image data into texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 96, 64, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        return new Texture(id, 3, 2);

    }

    private static int[] stitchHDAPLayers(int[][][] layeredPixelArray) {
        int[] stitchedArray = new int[96 * 64];
        //Top half
        for(int y = 0; y < 32; y++){
            for(int i = 0; i < 3; i++) {
                for (int x = 0; x < 32; x++) {
                    stitchedArray[(y * 96) + (i * 32) + x] = layeredPixelArray[x][y][i];
                    stitchedArray[(y * 32 * 3) + (i * 32) + x + 3072] = layeredPixelArray[x][y][i + 3];
                }
            }
        }

        return stitchedArray;
    }

    //Gets a byte buffer from an ap file
    public static ByteBuffer loadTextureBytesAP(String fileName) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocateDirect(32 * 32 * 4);

            InputStream ios = TextureLoader.class.getResourceAsStream(fileName);
            byte[] fileBytes = IOUtils.toByteArray(ios);

            byte[] palette = new byte[16 * 3];
            //Read color values
            for(int i = 0; i < 16; i++){
                palette[(i * 3)]        = fileBytes[(i * 3) + 16];      //Red
                palette[(i * 3) + 1]    = fileBytes[(i * 3) + 16 + 1];  //Green
                palette[(i * 3) + 2]    = fileBytes[(i * 3) + 16 + 2];  //Blue
            }

            int firstLayerTransColor = fileBytes[1108] >> 4;
            //Read pixels and store in buffer
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 16; x++) {
                    byte currentByte = fileBytes[x + (y * 16) + 592 + 4]; //Load first layer only
                    char firstPixelHexChar = String.format("%02x", currentByte).charAt(0);
                    char secondPixelHexChar = String.format("%02x", currentByte).charAt(1);

                    int highPixelIndex = Character.digit(firstPixelHexChar, 16);
                    int lowPixelIndex = Character.digit(secondPixelHexChar, 16);

                    //Top pixel color values
                    buffer.put( palette[highPixelIndex * 3] );
                    buffer.put( palette[highPixelIndex * 3 + 1] );
                    buffer.put( palette[highPixelIndex * 3 + 2] );

                    if(highPixelIndex == firstLayerTransColor)
                        buffer.put((byte) 0x00); //Transparent Pixel
                    else
                        buffer.put((byte) 0xFF); //Alpha, always 255

                    //Bottom pixel color values
                    buffer.put( palette[lowPixelIndex * 3] );
                    buffer.put( palette[lowPixelIndex * 3 + 1] );
                    buffer.put( palette[lowPixelIndex * 3 + 2] );

                    if(lowPixelIndex == firstLayerTransColor)
                        buffer.put((byte) 0x00); //Transparent Pixel
                    else
                        buffer.put((byte) 0xFF); //Alpha, always 255
                }
            }


            buffer.flip();
            return buffer;
    }
}