package cpw.mods.forge.cursepacklocator;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CursePack {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Path gameDir;
    private final JsonObject manifest;
    private final FileCacheManager fileCacheManager;
    private boolean validPack;
    private CompletableFuture<Void> packDownload;
    private Path curseModDir;
    private List<String> fileNames = new ArrayList<>();
    private Set<String> excludedProjectIds = new HashSet<>();
    private Set<String> excludedFileIds = new HashSet<>();
    private Consumer<String> progressUpdater;

    public CursePack(final Path gameDir, final FileCacheManager fileCacheManager) {
        this.fileCacheManager = fileCacheManager;
        JsonObject manifest1 = null;
        this.gameDir = gameDir;
        if (!Files.exists(gameDir.resolve("manifest.json"))) {
            LOGGER.info("No manifest.json file found, skipping");
        } else {
            try (JsonReader jsonReader = new Gson().newJsonReader(Files.newBufferedReader(gameDir.resolve("manifest.json")))) {
                manifest1 = new JsonParser().parse(jsonReader).getAsJsonObject();
                curseModDir = DirHandler.createOrGetDirectory(gameDir, "mods");
                validPack = true;
            } catch (IOException e) {
                LOGGER.info("Error trying to load manifest.json", e);
            }
        }
        manifest = manifest1;
        final Path exclusions = gameDir.resolve("exclusions.json");
        if (Files.exists(exclusions)) {
            final JsonObject jsonObject = FileCacheManager.loadJsonFromFile(exclusions);
            StreamSupport.stream(jsonObject.getAsJsonArray("excludedProjectIds").spliterator(), false)
                    .map(JsonElement::getAsString)
                    .forEach(excludedProjectIds::add);
            StreamSupport.stream(jsonObject.getAsJsonArray("excludedFileIds").spliterator(), false)
                    .map(JsonElement::getAsString)
                    .forEach(excludedFileIds::add);
        }
    }

    public boolean isValidPack() {
        return validPack;
    }

    public void startPackDownload(final Consumer<String> progressUpdater) {
        this.progressUpdater = progressUpdater;
        final JsonArray files = manifest.getAsJsonArray("files");
        progressUpdater.accept("Retrieving files for curseforge pack "+manifest.get("name").getAsString());
        LOGGER.info("Found {} files in pack to consider", files.size());
        progressUpdater.accept("Found "+files.size()+" files in pack");
        final ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(System.getProperty("cpd.maxThreads", String.valueOf(Runtime.getRuntime().availableProcessors()))));
        final CompletableFuture<List<String>> result = CompletableFuture.completedFuture(Collections.synchronizedList(fileNames));
        packDownload = CompletableFuture.allOf(StreamSupport.stream(files.spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .filter(file-> !excludedProjectIds.contains(file.get("projectID").getAsString())
                        && !excludedFileIds.contains(file.get("fileID").getAsString()))
                .map(file ->
                        CompletableFuture.supplyAsync(() -> fetchFile(file), executorService)
                                .thenApply(PackFile::getFileName)
                                .thenCombine(result, (packFile, res) -> res.add(packFile))
                )
                .toArray(CompletableFuture[]::new))
                .thenRun(executorService::shutdown)
                .thenRun(this::extractOverrides)
                .whenComplete(this::finished);
    }

    private void extractOverrides() {
        final Path overridePath = gameDir.resolve("overrides");
        try (Stream<Path> overrides = Files.walk(overridePath)) {
            overrides.forEach(path -> copyFile(path, overridePath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void copyFile(final Path path, final Path rootPath) {
        final Path relative = rootPath.relativize(path);
        if (Files.isDirectory(path))
            DirHandler.createDirIfNeeded(gameDir.resolve(relative));
        else {
            final Path target = gameDir.resolve(relative);
            try {
                Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void finished(final Void voivod, final Throwable throwable) {
        if (throwable != null) {
            validPack = false;
            LOGGER.catching(throwable);
        } else {
            LOGGER.info("Successfully fetched {} files", fileNames.size());
        }
    }

    private PackFile fetchFile(final JsonObject file) {
        final String projectID = file.get("projectID").getAsString();
        final String fileID = file.get("fileID").getAsString();

        PackFile packFile = new PackFile(projectID, fileID, this.progressUpdater);
        packFile.loadFileIntoPlace(getCurseModPath(), this.fileCacheManager);
        return packFile;
    }

    public void waitForPackDownload() {
        try {
            packDownload.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Path getCurseModPath() {
        return curseModDir;
    }
}
