package dev.aetherpulse.visual;

import org.bukkit.entity.Display;
import org.joml.Matrix4f;

/** One renderable child; transform matrices are evaluated locally without entity teleports. */
public final class VisualObject {
    private final Display display;
    private final Transform transform = new Transform();
    private final boolean block;
    public VisualObject(Display display, boolean block) { this.display = display; this.block = block; }
    public Transform transform() { return transform; }
    public Display display() { return display; }
    public void render(float visibility) {
        Matrix4f matrix = transform.worldMatrix().scale(visibility);
        if (block) matrix.translate(-.5f, -.5f, -.5f);
        display.setInterpolationDelay(0); display.setTransformationMatrix(matrix);
    }
    public void remove() { display.remove(); }
}
