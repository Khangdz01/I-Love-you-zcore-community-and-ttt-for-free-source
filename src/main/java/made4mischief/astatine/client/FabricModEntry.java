package made4mischief.astatine.client;

import made4mischief.astatine.loader.api.AuthenticatedLoaderRuntime;
import net.fabricmc.api.ClientModInitializer;

public class FabricModEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new AstatineClient().start(new AuthenticatedLoaderRuntime());
    }
}
