package made4mischief.astatine.loader.api;

public interface LoaderRuntime {
    void requireActive();
    String claimInitialConfiguration();
    boolean flushConfiguration(long timeoutMs);
    boolean configurationChanged(String json);
}
