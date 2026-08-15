package made4mischief.astatine.loader.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public final class AuthenticatedLoaderRuntime implements LoaderRuntime {
    private static final File CONFIG_FILE = new File("astatine-config.json");

    @Override
    public void requireActive() {
    }

    @Override
    public String claimInitialConfiguration() {
        try {
            if (CONFIG_FILE.exists()) {
                return Files.readString(CONFIG_FILE.toPath(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        return "{}";
    }

    @Override
    public boolean flushConfiguration(long timeoutMs) {
        return true;
    }

    @Override
    public boolean configurationChanged(String json) {
        try {
            Files.writeString(CONFIG_FILE.toPath(), json, StandardCharsets.UTF_8);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
