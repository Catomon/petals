package ctmn.petals.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class RectRenderer {

    public static ShapeRenderer shapeRenderer = new ShapeRenderer();

    private static final Array<Rectangle> rectangles = new Array<Rectangle>();

    public static boolean isDrawing = true;

    public static void add(float fl, float fl1, float fl2, float fl3) {
        if (isDrawing)
            rectangles.add(new Rectangle(fl, fl1, fl2, fl3));
    }

    public static void render() {
        shapeRenderer.setColor(Color.GREEN);
        shapeRenderer.setAutoShapeType(true);
        shapeRenderer.begin();

        for (Rectangle rectangle : rectangles)
            shapeRenderer.rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);

        rectangles.clear();

        shapeRenderer.end();
    }
}
