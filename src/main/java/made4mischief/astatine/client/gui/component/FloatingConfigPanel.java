package made4mischief.astatine.client.gui.component;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.component.event.MouseButton;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import made4mischief.astatine.client.gui.component.setting.ActionComponent;
import made4mischief.astatine.client.gui.component.setting.BlockTargetComponent;
import made4mischief.astatine.client.gui.component.setting.BooleanComponent;
import made4mischief.astatine.client.gui.component.setting.ColorPickerComponent;
import made4mischief.astatine.client.gui.component.setting.EntityTargetComponent;
import made4mischief.astatine.client.gui.component.setting.FriendListComponent;
import made4mischief.astatine.client.gui.component.setting.ItemTargetComponent;
import made4mischief.astatine.client.gui.component.setting.KeybindComponent;
import made4mischief.astatine.client.gui.component.setting.ModeComponent;
import made4mischief.astatine.client.gui.component.setting.SettingComponent;
import made4mischief.astatine.client.gui.component.setting.SliderComponent;
import made4mischief.astatine.client.gui.component.setting.StringComponent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.setting.ActionSetting;
import made4mischief.astatine.client.setting.BlockTargetSetting;
import made4mischief.astatine.client.setting.BooleanSetting;
import made4mischief.astatine.client.setting.ColorSetting;
import made4mischief.astatine.client.setting.EntityTargetSetting;
import made4mischief.astatine.client.setting.FriendListSetting;
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.setting.KeybindSetting;
import made4mischief.astatine.client.setting.ModeSetting;
import made4mischief.astatine.client.setting.NumberSetting;
import made4mischief.astatine.client.setting.Setting;
import made4mischief.astatine.client.setting.StringSetting;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class FloatingConfigPanel extends AbstractComponent {
   public static final float WIDTH = 205.0F;
   private static final float HEADER_HEIGHT = 30.0F;
   public static final float MINIMUM_HEIGHT = 31.0F;
   private static final float CONTENT_X_INSET = 12.0F;
   private static final float ROW_GAP = 10.0F;
   private static final float BOTTOM_PADDING = 14.0F;
   private static final float TOP_PADDING = 10.0F;
   private static final float RADIUS = 14.0F;
   private static final float SCROLL_STEP = 24.0F;
   private String title = "Module";
   private final List<SettingComponent> settingComponents = new ArrayList<>();
   private Module module;
   private final Animation openAnimation = new Animation(0.0F, 1.0F, 300L, AnimationType.EASE_OUT);
   private boolean open;
   private float maximumHeight = Float.POSITIVE_INFINITY;
   private float scrollOffset;

   public FloatingConfigPanel(){
      super(0.0F, 0.0F, 205.0F, 30.0F);
   }

   public Module getModule(){
      return this.module;
   }

   public boolean isOpen(){
      return this.open;
   }

   public boolean isRetired(){
      return !this.open && this.openAnimation.isFinished();
   }

   public void open(Module module, float x, float y){
      this.module = module;
      this.title = module.getName();
      this.setPosition(x, y);
      this.scrollOffset = 0.0F;
      this.populateModuleSettings(module);
      this.open = true;
      this.openAnimation.setTarget(1.0F);
   }

   public void setMaximumHeight(float maximumHeight){
      if (!Float.isFinite(maximumHeight)) {
         this.maximumHeight = Float.POSITIVE_INFINITY;
      } else {
         this.maximumHeight = Math.max(31.0F, maximumHeight);
      }

      this.clampScrollOffset();
   }

   public void close(){
      if (this.open) {
         this.open = false;
         this.openAnimation.setTarget(0.0F);
      }
   }

   public void closeImmediately(){
      this.open = false;
      this.openAnimation.snapTo(0.0F);
   }

   private void populateModuleSettings(Module module){
      this.settingComponents.clear();
      float var2 = 181.0F;

      for (Setting setting : module.getSettings()) {
         Object var5 = null;
         if (setting instanceof NumberSetting var6) {
            var5 = new SliderComponent(var6, 0.0F, 0.0F, var2);
         } else if (setting instanceof ActionSetting var7) {
            var5 = new ActionComponent(var7, 0.0F, 0.0F, var2);
         } else if (setting instanceof ColorSetting var8) {
            var5 = new ColorPickerComponent(var8, 0.0F, 0.0F, var2);
         } else if (setting instanceof BooleanSetting var9) {
            var5 = new BooleanComponent(var9, 0.0F, 0.0F, var2);
         } else if (setting instanceof ModeSetting var10) {
            var5 = new ModeComponent(var10, 0.0F, 0.0F, var2);
         } else if (setting instanceof StringSetting var11) {
            var5 = new StringComponent(var11, 0.0F, 0.0F, var2);
         } else if (setting instanceof FriendListSetting var12) {
            var5 = new FriendListComponent(var12, 0.0F, 0.0F, var2);
         } else if (setting instanceof BlockTargetSetting var13) {
            var5 = new BlockTargetComponent(var13, 0.0F, 0.0F, var2);
         } else if (setting instanceof EntityTargetSetting var14) {
            var5 = new EntityTargetComponent(var14, 0.0F, 0.0F, var2);
         } else if (setting instanceof ItemTargetSetting var15) {
            var5 = new ItemTargetComponent(var15, 0.0F, 0.0F, var2);
         } else if (setting instanceof KeybindSetting var16) {
            var5 = new KeybindComponent(var16, 0.0F, 0.0F, var2);
         }

         if (var5 != null) {
            ((SettingComponent)var5).setParent(this);
            this.settingComponents.add((SettingComponent)var5);
         }
      }

      this.clampScroll();
   }

   @Override
   public float getHeight(){
      return Math.min(30.0F + this.getContentHeight(), this.maximumHeight);
   }

   public float getNaturalHeight(){
      return 30.0F + this.getContentHeight();
   }

   private float getContentHeight(){
      int index2 = 0;
      float var2 = 0.0F;

      for (int index = 0; index < this.settingComponents.size(); index++) {
         SettingComponent settingComponent = this.settingComponents.get(index);
         if (settingComponent.getSetting().isVisible()) {
            var2 += settingComponent.getHeight();
            index2++;
         }
      }

      return index2 == 0 ? 24.0F : 10.0F + var2 + Math.max(0, index2 - 1) * 10.0F + 14.0F;
   }

   private void clampScroll(){
      this.clampScrollOffset();
      float x = this.getX() + 12.0F;
      float closeButtonOffset = this.getCloseButtonOffset();
      float y = this.getY() + 30.0F + 10.0F + closeButtonOffset + this.scrollOffset;

      for (int index = 0; index < this.settingComponents.size(); index++) {
         SettingComponent settingComponent = this.settingComponents.get(index);
         if (!settingComponent.getSetting().isVisible()) {
            settingComponent.setVisible(false);
         } else {
            settingComponent.setVisible(true);
            settingComponent.setPosition(x, y);
            y += settingComponent.getHeight() + 10.0F;
         }
      }
   }

   private float getCloseButtonOffset(){
      return -8.0F * (1.0F - this.openAnimation.get());
   }

   @Override
   public boolean isWithin(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + 205.0F && pointY >= this.getY() && pointY <= this.getY() + this.getHeight();
   }

   private boolean isPointInside(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + 205.0F && pointY >= this.getY() + 30.0F && pointY <= this.getY() + this.getHeight();
   }

   private float getMaxScroll(){
      float height = Math.max(0.0F, this.getHeight() - 30.0F);
      return Math.max(0.0F, this.getContentHeight() - height);
   }

   private void clampScrollOffset(){
      this.scrollOffset = Math.max(-this.getMaxScroll(), Math.min(0.0F, this.scrollOffset));
   }

   @Override
   public void render(GuiRenderContext context){
      float get = this.openAnimation.get();
      if (!(get <= 0.001F) || this.open) {
         Theme theme = context.theme();
         this.clampScroll();
         float x = this.getX();
         float closeButtonOffset = this.getY() + this.getCloseButtonOffset();
         float height = this.getHeight();
         int accent = ColorUtil.scaleAlpha(ColorUtil.lerp(theme.border(), theme.accent(), 0.5F), get);
         context.drawBo(x - 1.0F, closeButtonOffset - 1.0F, 207.0F, height + 2.0F, 15.0F, accent);
         int surface = ColorUtil.scaleAlpha(theme.surface(), get);
         context.drawBo(x, closeButtonOffset, 205.0F, height, 14.0F, surface);
         this.renderSettingRows(context, theme, x, closeButtonOffset, get);
         int round = Math.round(closeButtonOffset + 30.0F);
         int round2 = Math.round(closeButtonOffset + height);
         context.enableScissor(Math.round(x), round, Math.round(x + 205.0F), round2);
         this.renderScrollbar(context, theme, x, closeButtonOffset, get);
         context.disableScissor();
      }
   }

   private void renderSettingRows(GuiRenderContext context, Theme theme, float x, float y, float a){
      int accent3 = ColorUtil.scaleAlpha(theme.accent(), a);
      float textHeight = y + (30.0F - context.textHeight() * 0.9F) / 2.0F;
      context.drawText(this.title.toUpperCase(), x + 12.0F, textHeight, accent3, true, 0.9F);
      String var8 = "CONFIG";
      float textWidth = context.textWidth(var8) * 0.7F;
      float var10 = textWidth + 10.0F;
      float var11 = 12.0F;
      float var12 = x + 205.0F - 12.0F - var10;
      float var13 = y + (30.0F - var11) / 2.0F;
      int accent2 = ColorUtil.scaleAlpha(theme.accent(), 0.35F * a);
      context.drawBo(var12 - 1.0F, var13 - 1.0F, var10 + 2.0F, var11 + 2.0F, 4.0F, accent2);
      int accentSecondary = ColorUtil.scaleAlpha(theme.accentSecondary(), 0.22F * a);
      context.drawBo(var12, var13, var10, var11, 3.0F, accentSecondary);
      int accent = ColorUtil.scaleAlpha(theme.accent(), 0.85F * a);
      context.drawText(var8, var12 + 5.0F, var13 + (var11 - context.textHeight() * 0.7F) / 2.0F, accent, false, 0.7F);
      int border = ColorUtil.scaleAlpha(theme.border(), a);
      context.drawBo(x + 12.0F, y + 30.0F - 1.0F, 181.0F, 1.0F, 0.0F, border);
   }

   private void renderScrollbar(GuiRenderContext context, Theme theme, float x, float y, float a){
      if (!this.hasVisibleSettings()) {
         float textHeight = y + 30.0F + (this.getContentHeight() - context.textHeight() * 0.85F) / 2.0F;
         context.drawCenteredText("x", x + 102.5F, textHeight, ColorUtil.scaleAlpha(theme.textDim(), 0.65F * a), false, 0.85F);
      } else {
         int border = ColorUtil.scaleAlpha(theme.border(), 0.6F * a);
         boolean var7 = true;

         for (int index = 0; index < this.settingComponents.size(); index++) {
            SettingComponent settingComponent = this.settingComponents.get(index);
            if (settingComponent.getSetting().isVisible()) {
               if (!var7) {
                  float y2 = settingComponent.getY() - 5.0F;
                  context.drawBo(x + 12.0F, y2, 181.0F, 1.0F, 0.0F, border);
               }

               var7 = false;
               settingComponent.render(context);
            }
         }
      }
   }

   private boolean hasVisibleSettings(){
      for (int index = 0; index < this.settingComponents.size(); index++) {
         if (this.settingComponents.get(index).getSetting().isVisible()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void tick(){
      for (int index = 0; index < this.settingComponents.size(); index++) {
         this.settingComponents.get(index).tick();
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (!this.open) {
         return false;
      } else {
         if (this.isPointInside(event.x(), event.y())) {
            for (int index = this.settingComponents.size() - 1; index >= 0; index--) {
               if (this.settingComponents.get(index).mouseClicked(event)) {
                  return true;
               }
            }
         }

         if (event.button() != MouseButton.OTHER && this.isWithin(event.x(), event.y())) {
            event.consume(this);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      boolean var2 = false;

      for (int index = this.settingComponents.size() - 1; index >= 0; index--) {
         if (this.settingComponents.get(index).mouseReleased(event)) {
            var2 = true;
         }
      }

      return var2;
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      for (int index = 0; index < this.settingComponents.size(); index++) {
         this.settingComponents.get(index).mouseMoved(event);
      }

      return false;
   }

   protected boolean onMouseScrolled(MouseScrollEvent event){
      if (this.open && this.isPointInside(event.x(), event.y())) {
         for (int index = this.settingComponents.size() - 1; index >= 0; index--) {
            if (this.settingComponents.get(index).mouseScrolled(event)) {
               return true;
            }
         }

         if (event.vertical() != 0.0 && this.getMaxScroll() > 0.0F) {
            this.scrollOffset = this.scrollOffset + (float)event.vertical() * 24.0F;
            this.clampScrollOffset();
         }

         event.consume(this);
         return true;
      } else {
         return false;
      }
   }
}

