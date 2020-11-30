package com.thiccindustries.APE2;

import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class Keyboard extends GLFWKeyCallback {
    private static boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private static int[] actions = new int[GLFW_KEY_LAST + 1];

    @Override
    public void invoke(long window, int key, int scancode, int action, int mods){
        keys[key] = action != GLFW_RELEASE;
        actions[key] = action;
    }

    //This is expensive AF only use if necessary
    public static int GetAnyKey(){
        for(int i = 0; i < actions.length; i++){
            if(actions[i] == GLFW_PRESS)
                return i;
        }
        return -1;
    }

    public static boolean GetKey(int keycode){
        return keys[keycode];
    }

    public static boolean GetKeyDown(int keycode){
        return actions[keycode] == GLFW_PRESS;
    }

    public static boolean GetKeyUp(int keycode){
        return actions[keycode] == GLFW_RELEASE;
    }

    protected static void Update()
    {
        Arrays.fill(actions, -1);
    }

}

