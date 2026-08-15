package made4mischief.astatine.client.modules.render;

import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public class HandViewModule extends Module {
   private static final String MAIN_HAND_LABEL = "Main Hand";
   private static final String OFF_HAND_LABEL = "Off Hand";
   private final ModeSetting editingHandSetting = this.addMode("Editing Hand", "Main Hand", new String[]{"Main Hand", "Off Hand"});
   private final NumberSetting mainHandXSetting = this.addNumber("Main Hand X", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting mainHandYSetting = this.addNumber("Main Hand Y", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting mainHandZSetting = this.addNumber("Main Hand Z", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting mainHandScaleSetting = this.addNumber("Main Hand Scale", 1.0, 0.1, 3.0, 0.05);
   private final NumberSetting mainHandRotateXSetting = this.addNumber("Main Hand Rotate X", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting mainHandRotateYSetting = this.addNumber("Main Hand Rotate Y", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting mainHandRotateZSetting = this.addNumber("Main Hand Rotate Z", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting offHandXSetting = this.addNumber("Off Hand X", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting offHandYSetting = this.addNumber("Off Hand Y", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting offHandZSetting = this.addNumber("Off Hand Z", 0.0, -2.0, 2.0, 0.05);
   private final NumberSetting offHandScaleSetting = this.addNumber("Off Hand Scale", 1.0, 0.1, 3.0, 0.05);
   private final NumberSetting offHandRotateXSetting = this.addNumber("Off Hand Rotate X", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting offHandRotateYSetting = this.addNumber("Off Hand Rotate Y", 0.0, -180.0, 180.0, 1.0);
   private final NumberSetting offHandRotateZSetting = this.addNumber("Off Hand Rotate Z", 0.0, -180.0, 180.0, 1.0);
   private static HandViewModule instance;

   public HandViewModule(){
      super("HandView", Category.RENDER, "Chỉnh vị trí, kích thước và góc xoay của tay.");
      instance = this;
      this.mainHandXSetting.visibleWhen(this::isMainHand);
      this.mainHandYSetting.visibleWhen(this::isMainHand);
      this.mainHandZSetting.visibleWhen(this::isMainHand);
      this.mainHandScaleSetting.visibleWhen(this::isMainHand);
      this.mainHandRotateXSetting.visibleWhen(this::isMainHand);
      this.mainHandRotateYSetting.visibleWhen(this::isMainHand);
      this.mainHandRotateZSetting.visibleWhen(this::isMainHand);
      this.offHandXSetting.visibleWhen(this::isOffHand);
      this.offHandYSetting.visibleWhen(this::isOffHand);
      this.offHandZSetting.visibleWhen(this::isOffHand);
      this.offHandScaleSetting.visibleWhen(this::isOffHand);
      this.offHandRotateXSetting.visibleWhen(this::isOffHand);
      this.offHandRotateYSetting.visibleWhen(this::isOffHand);
      this.offHandRotateZSetting.visibleWhen(this::isOffHand);
   }

   private boolean isMainHand(){
      return this.editingHandSetting.is("Main Hand");
   }

   private boolean isOffHand(){
      return this.editingHandSetting.is("Off Hand");
   }

   public static double getOffsetX(Hand hand){
      if (instance == null) {
         return 0.0;
      } else {
         return isMainHand(hand) ? instance.offHandXSetting.getValue() : instance.mainHandXSetting.getValue();
      }
   }

   public static double getOffsetY(Hand hand){
      if (instance == null) {
         return 0.0;
      } else {
         return isMainHand(hand) ? instance.offHandYSetting.getValue() : instance.mainHandYSetting.getValue();
      }
   }

   public static double getOffsetZ(Hand hand){
      if (instance == null) {
         return 0.0;
      } else {
         return isMainHand(hand) ? instance.offHandZSetting.getValue() : instance.mainHandZSetting.getValue();
      }
   }

   public static float getScale(Hand hand){
      if (instance == null) {
         return 1.0F;
      } else {
         return isMainHand(hand) ? (float)instance.offHandScaleSetting.getValue() : (float)instance.mainHandScaleSetting.getValue();
      }
   }

   public static float getRotX(Hand hand){
      if (instance == null) {
         return 0.0F;
      } else {
         return isMainHand(hand) ? (float)instance.offHandRotateXSetting.getValue() : (float)instance.mainHandRotateXSetting.getValue();
      }
   }

   public static float getRotY(Hand hand){
      if (instance == null) {
         return 0.0F;
      } else {
         return isMainHand(hand) ? (float)instance.offHandRotateYSetting.getValue() : (float)instance.mainHandRotateYSetting.getValue();
      }
   }

   public static float getRotZ(Hand hand){
      if (instance == null) {
         return 0.0F;
      } else {
         return isMainHand(hand) ? (float)instance.offHandRotateZSetting.getValue() : (float)instance.mainHandRotateZSetting.getValue();
      }
   }

   private static boolean isMainHand(Hand hand){
      return hand == Hand.OFF_HAND;
   }
}
