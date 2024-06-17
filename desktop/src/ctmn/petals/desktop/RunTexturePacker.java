package ctmn.petals.desktop;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class RunTexturePacker {
    public static void main(String[] args) {
        pack();
    }

    public static void pack() {
        System.out.println(RunTexturePacker.class.getSimpleName() + "Packing...");
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.pot = true;
        settings.fast = true;
        settings.combineSubdirectories = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.edgePadding = true;
        settings.duplicatePadding = true;
        settings.minHeight = 640;
        settings.minWidth = 640;
        settings.maxHeight = 4096;
        settings.maxWidth = 4096;
        settings.scaleResampling = new TexturePacker.Resampling[]{TexturePacker.Resampling.nearest};

        //settings.scale = new float[]{4F};
        process(settings, "textures/tiles", "./", "tiles");
        process(settings, "textures/tiles_winter", "./", "tiles_winter");

        process(settings, "textures/units", "./", "units");

        process(settings, "textures/effects", "./", "effects");

        process(settings, "textures/misc", "./", "misc");

        settings.scale = new float[]{3F};
        process(settings, "textures/gui", "./skin", "wafer-ui");
    }

    private static void process(TexturePacker.Settings settings, String input, String output, String packFileName) {
        try {
            TexturePacker.process(settings, input, output, packFileName);
        } catch (Exception e) {
            System.out.println("Error processing " + packFileName + ". " + e.getMessage());
        }
    }
}
