package ctmn.petals.desktop;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class RunTexturePacker {
    public static void main(String[] args) {
        pack();
    }

    public static void pack() {
        try {
            System.out.println(RunTexturePacker.class.getSimpleName() + "Running...");
            TexturePacker.Settings settings = new TexturePacker.Settings();
            settings.pot = true;
            settings.fast = true;
            settings.combineSubdirectories = true;
            settings.paddingX = 2;
            settings.paddingY = 2;
            settings.edgePadding = true;
            settings.duplicatePadding = true;
            settings.maxHeight = 4096;
            settings.maxWidth = 4096;
            settings.scale = new float[]{4F};
            settings.scaleResampling = new TexturePacker.Resampling[]{TexturePacker.Resampling.nearest};
            TexturePacker.process(settings, "textures", "./", "textures");
            TexturePacker.process(settings, "textures/tiles", "./", "tiles");
            TexturePacker.process(settings, "textures/units", "./", "units");
            settings.scale = new float[]{3F};
            TexturePacker.process(settings, "textures/gui", "./skin", "wafer-ui");
            System.out.println(RunTexturePacker.class.getSimpleName() + "Done.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(RunTexturePacker.class.getSimpleName() + "Fail.");
        }
    }
}
