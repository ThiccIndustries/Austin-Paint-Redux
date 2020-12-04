package com.thiccindustries.APE2;

import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class Mouse extends GLFWMouseButtonCallback {
    public static boolean[] buttons = new boolean[16];
    public static int[] actions = new int[16];

    @Override
    public void invoke(long window, int button, int action, int mods){
        buttons[button] = action != GLFW_RELEASE;
        actions[button] = action;
    }

    public static boolean GetButton(int button){
        return buttons[button];
    }

    public static boolean GetButtonDown(int button){
        return actions[button] == GLFW_PRESS;
    }

    @SuppressWarnings("unused")
    public static boolean GetButtonUp(int button){
        return actions[button] == GLFW_RELEASE;
    }

    public static void Update(){
        Arrays.fill(actions, -1);
    }
}
