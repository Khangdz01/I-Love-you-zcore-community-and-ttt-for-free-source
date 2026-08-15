package made4mischief.astatine;

import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Astatine {
   public static final String MOD_ID = "astatine";
   public static final Logger LOGGER = LoggerFactory.getLogger("astatine");

   public static Identifier id(String path){
      return Identifier.of("astatine", path);
   }
}
