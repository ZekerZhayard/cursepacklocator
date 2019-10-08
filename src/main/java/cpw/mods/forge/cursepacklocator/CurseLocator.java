package cpw.mods.forge.cursepacklocator;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.launcher.FMLTweaker;

public class CurseLocator implements ITweaker {
    private CursePack cursePack;
    private FileCacheManager fileCacheManager;
    private FMLTweaker fmlTweaker;

    public CurseLocator() {
        this.fmlTweaker = new FMLTweaker();
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.fmlTweaker.acceptOptions(args, gameDir, assetsDir, profile);
        this.fileCacheManager = new FileCacheManager(assetsDir.toPath());
        this.cursePack = new CursePack(gameDir.toPath(), this.fileCacheManager);

        Consumer<String> progressUpdater = str -> {};
        this.fileCacheManager.setProgressUpdater(progressUpdater);
        this.cursePack.startPackDownload(progressUpdater);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        this.cursePack.waitForPackDownload();
        this.fmlTweaker.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return this.fmlTweaker.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        return this.fmlTweaker.getLaunchArguments();
    }
}
