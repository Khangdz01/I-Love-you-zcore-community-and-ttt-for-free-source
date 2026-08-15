package made4mischief.astatine.loader.api.event;

public class KeyEvent {
    private final int key;
    private final int scanCode;
    private final int action;

    public KeyEvent(int key, int scanCode, int action) {
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
    }

    public int getKey() {
        return this.key;
    }

    public int getScanCode() {
        return this.scanCode;
    }

    public int getAction() {
        return this.action;
    }
}
