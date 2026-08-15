package made4mischief.astatine.client.screen;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.component.CategoryPanel;
import made4mischief.astatine.client.gui.component.ComponentManager;
import made4mischief.astatine.client.gui.component.Panel;
import made4mischief.astatine.client.gui.component.widget.Button;
import made4mischief.astatine.client.gui.component.widget.ModuleButton;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationManager;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@Environment(EnvType.CLIENT)
public class RenderTestScreen extends Screen {
   private static final int BACKGROUND_COLOR = -435547100;
   private static final int BOX_COLOR = -536863167;
   private static final int ACCENT_COLOR = -16722689;
   private static final int SECONDARY_COLOR = -16737793;
   private static final int TEXT_DIM_COLOR = -6499073;
   private static final int TEXT_MUTED_COLOR = -9525505;
   private final Animation loopAnimation = new Animation(0.0F, 1.0F, 2000L, AnimationType.LINEAR);
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 220L, AnimationType.EASE_OUT);
   private final Animation expandAnimation = new Animation(0.0F, 1.0F, 900L, AnimationType.EASE_IN_OUT);
   private final Animation bounceAnimation = new Animation(0.0F, 1.0F, 1200L, AnimationType.CUBIC);
   private final AnimationType[] easingTypes = AnimationType.values();
   private final Animation[] easingCurveAnimations = new Animation[this.easingTypes.length];
   private boolean hovered;
   private final ComponentManager components = new ComponentManager();
   private String lastActivatedLabel = "(none)";
   private final ComponentManager moduleButtonManager = new ComponentManager();
   private final List<ModuleButton> testModuleButtons = new ArrayList<>();
   private final ComponentManager categoryPanelManager = new ComponentManager();
   private final List<CategoryPanel> testCategoryPanels = new ArrayList<>();
   private static final int PANEL_X_OFFSET = -75;
   private static final int PANEL_START_Y = 92;

   public RenderTestScreen(){
      super(Text.literal("Render Framework Test"));
      this.loopAnimation.start();
      this.expandAnimation.start();
      this.bounceAnimation.start();

      for (int index = 0; index < this.easingTypes.length; index++) {
         this.easingCurveAnimations[index] = new Animation(0.0F, 1.0F, 1400L, this.easingTypes[index]);
         this.easingCurveAnimations[index].start();
      }
   }

   public boolean shouldPause(){
      return false;
   }

   public void tick(){
      super.tick();
      this.components.tick();
      this.moduleButtonManager.tick();
      this.categoryPanelManager.tick();
   }

   protected void init(){
      super.init();
      this.components.resize(this.width, this.height);
      this.components.root().clear();
      int var1 = this.width - 250;
      short var2 = 570;
      Panel panel2 = new Panel(var1, var2, 230.0F, 150.0F);
      panel2.setRadius(10.0F);
      panel2.add(new Button("Click me", var1 + 12, var2 + 12, 100.0F, 24.0F, () -> this.lastActivatedLabel = "Click me"));
      Button button = new Button("Disabled", var1 + 12, var2 + 44, 100.0F, 24.0F, () -> this.lastActivatedLabel = "Disabled (should never fire)");
      button.setEnabled(false);
      panel2.add(button);
      Panel panel = new Panel(var1 + 120, var2 + 10, 98.0F, 130.0F);
      panel.setRadius(8.0F);
      panel.add(new Button("A", var1 + 128, var2 + 18, 40.0F, 24.0F, () -> this.lastActivatedLabel = "Nested A"));
      panel.add(new Button("B", var1 + 128, var2 + 48, 40.0F, 24.0F, () -> this.lastActivatedLabel = "Nested B"));
      panel.add(new Button("bot", var1 + 130, var2 + 82, 44.0F, 26.0F, () -> this.lastActivatedLabel = "Overlap BOTTOM"));
      panel.add(new Button("top", var1 + 146, var2 + 94, 44.0F, 26.0F, () -> this.lastActivatedLabel = "Overlap TOP"));
      panel2.add(panel);
      this.components.root().add(panel2);
      this.resizeButtons();
      this.resizeComponents();
   }

   private void resizeButtons(){
      this.moduleButtonManager.resize(this.width, this.height);
      this.moduleButtonManager.root().clear();
      this.testModuleButtons.clear();
      short var1 = 220;
      byte var2 = 56;
      short var3 = 150;
      byte var4 = 22;
      String[] var5 = new String[]{"KillAura", "Sprint", "Fullbright", "Fly"};
      boolean[] var6 = new boolean[]{true, false, false, false};

      for (int index = 0; index < var5.length; index++) {
         Module module = this.createTestModule(var5[index], var6[index]);
         ModuleButton moduleButton = new ModuleButton(module, var1, var2, var3, var4);
         this.moduleButtonManager.root().add(moduleButton);
         this.testModuleButtons.add(moduleButton);
      }

      this.drawButtonRow(var1, var2);
   }

   private Module createTestModule(String name, boolean enabled){
      Module module = new Module(name, Category.RENDER) {};
      module.setEnabled(enabled);
      return module;
   }

   private void drawButtonRow(int x, int y){
      float var3 = y;

      for (int index = 0; index < this.testModuleButtons.size(); index++) {
         ModuleButton moduleButton = this.testModuleButtons.get(index);
         moduleButton.setPosition(x, var3);
         var3 += moduleButton.getHeight() + 4.0F;
      }
   }

   private void resizeComponents(){
      this.categoryPanelManager.resize(this.width, this.height);
      this.categoryPanelManager.root().clear();
      this.testCategoryPanels.clear();
      float var1 = this.width / 2.0F + -75.0F;
      float var2 = 92.0F;
      float var3 = 150.0F;
      CategoryPanel categoryPanel3 = new CategoryPanel("Combat", var1, var2, var3);
      categoryPanel3.addModule(this.createTestModule("KillAura", true));
      categoryPanel3.addModule(this.createTestModule("CrystalAura", false));
      CategoryPanel categoryPanel2 = new CategoryPanel("Movement", var1, var2, var3);
      categoryPanel2.addModule(this.createTestModule("Sprint", true));
      categoryPanel2.addModule(this.createTestModule("Fly", false));
      categoryPanel2.addModule(this.createTestModule("Speed", false));
      CategoryPanel categoryPanel = new CategoryPanel("Render", var1, var2, var3);
      categoryPanel.addModule(this.createTestModule("ESP", false));
      categoryPanel.addModule(this.createTestModule("FullBright", false));

      for (CategoryPanel categoryPanel4 : new CategoryPanel[]{categoryPanel3, categoryPanel2, categoryPanel}) {
         this.categoryPanelManager.root().add(categoryPanel4);
         this.testCategoryPanels.add(categoryPanel4);
      }

      this.drawComponentRow(var1, var2);
   }

   private void drawComponentRow(float x, float y){
      float var3 = y;

      for (int index = 0; index < this.testCategoryPanels.size(); index++) {
         CategoryPanel categoryPanel = this.testCategoryPanels.get(index);
         categoryPanel.setPosition(x, var3);
         var3 += categoryPanel.getHeight() + 6.0F;
      }
   }

   public boolean mouseClicked(Click click, boolean doubled){
      if (this.categoryPanelManager.mouseClicked(click.x(), click.y(), click.button())) {
         return true;
      } else if (this.moduleButtonManager.mouseClicked(click.x(), click.y(), click.button())) {
         return true;
      } else {
         return this.components.mouseClicked(click.x(), click.y(), click.button()) ? true : super.mouseClicked(click, doubled);
      }
   }

   public boolean mouseReleased(Click click){
      boolean button = this.categoryPanelManager.mouseReleased(click.x(), click.y(), click.button());
      button |= this.moduleButtonManager.mouseReleased(click.x(), click.y(), click.button());
      button |= this.components.mouseReleased(click.x(), click.y(), click.button());
      return button ? true : super.mouseReleased(click);
   }

   public void mouseMoved(double mouseX, double mouseY){
      this.categoryPanelManager.mouseMoved(mouseX, mouseY);
      this.moduleButtonManager.mouseMoved(mouseX, mouseY);
      this.components.mouseMoved(mouseX, mouseY);
      super.mouseMoved(mouseX, mouseY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
      if (this.categoryPanelManager.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
         return true;
      } else if (this.moduleButtonManager.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
         return true;
      } else {
         return this.components.mouseScrolled(mouseX, mouseY, horizontal, vertical) ? true : super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
      }
   }

   public boolean keyPressed(KeyInput keyInput){
      if (this.categoryPanelManager.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers())) {
         return true;
      } else if (this.moduleButtonManager.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers())) {
         return true;
      } else {
         return this.components.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers()) ? true : super.keyPressed(keyInput);
      }
   }

   public boolean charTyped(CharInput charInput){
      if (this.categoryPanelManager.charTyped((char)charInput.codepoint(), charInput.modifiers())) {
         return true;
      } else if (this.moduleButtonManager.charTyped((char)charInput.codepoint(), charInput.modifiers())) {
         return true;
      } else {
         return this.components.charTyped((char)charInput.codepoint(), charInput.modifiers()) ? true : super.charTyped(charInput);
      }
   }

   public void render(DrawContext ct, int mouseX, int mouseY, float delta){
      super.render(ct, mouseX, mouseY, delta);
      AnimationManager.update();
      int var5 = this.width;
      int var6 = this.height;
      ct.fill(0, 0, var5, var6, -435547100);
      this.renderTestBo(ct);
      byte var7 = 56;
      this.renderRadiusRow(ct, 20, var7);
      this.renderAlphaRow(ct, 20, var7 + 110);
      this.renderSizeRow(ct, 20, var7 + 220);
      this.renderOverlapRow(ct, var5 - 250, var7);
      this.renderClippingRow(ct, var5 - 250, var7 + 150);
      this.renderTextRow(ct, 20, var7 + 330, var5);
      this.renderAnimationRow(ct, var5 - 250, var7 + 300, mouseX, mouseY);
      this.renderComponentRow(ct, var5 - 250, var7 + 490, mouseX, mouseY, delta);
      this.renderModuleButton(ct, 220, var7, mouseX, mouseY, delta);
      this.renderButtonStrip(ct, mouseX, mouseY, delta);
      this.renderScreenResize(ct, var5, var6);
   }

   private void renderModuleButton(DrawContext ct, int x, int y, int mouseX, int mouseY, float delta){
      this.drawTitleLabel(ct, "MODULE BUTTON", x, y - 42);
      RenderUtil.drawText(ct, "L-click: toggle Â· R-click: expand", x, y - 28, -6499073, true, 0.9F);
      this.drawButtonRow(x, y);
      this.moduleButtonManager.render(ct, mouseX, mouseY, delta);
      float var7 = y;

      for (int index2 = 0; index2 < this.testModuleButtons.size(); index2++) {
         var7 += this.testModuleButtons.get(index2).getHeight() + 4.0F;
      }

      var7 += 6.0F;

      for (int index = 0; index < this.testModuleButtons.size(); index++) {
         ModuleButton moduleButton = this.testModuleButtons.get(index);
         String expanded = moduleButton.getModule().getName()
            + " â€” "
            + (moduleButton.getModule().isEnabled() ? "ON" : "off")
            + " Â· "
            + (moduleButton.isExpanded() ? "expanded" : "collapsed");
         int enabled = moduleButton.getModule().isEnabled() ? -16722689 : -9525505;
         RenderUtil.drawText(ct, expanded, x, var7 + index * 11, enabled, false, 0.85F);
      }
   }

   private void renderButtonStrip(DrawContext ct, int mouseX, int mouseY, float delta){
      float var5 = this.width / 2.0F + -75.0F;
      this.drawTitleLabel(ct, "CATEGORY PANEL", (int)var5, 62);
      RenderUtil.drawText(ct, "R-click header: expand/collapse Â· L-click: toggle module", var5, 76.0F, -6499073, true, 0.9F);
      this.drawComponentRow(var5, 92.0F);
      this.categoryPanelManager.render(ct, mouseX, mouseY, delta);
   }

   private void renderComponentRow(DrawContext ct, int x, int y, int mouseX, int mouseY, float delta){
      this.drawTitleLabel(ct, "COMPONENT", x, y);
      RenderUtil.drawText(ct, "nested panels Â· hover Â· focus Â· click routing", x, y + 14, -6499073, true, 0.9F);
      this.components.render(ct, mouseX, mouseY, delta);
      RenderUtil.drawText(ct, "last activated: " + this.lastActivatedLabel, x, y + 182, -9525505, true, 0.9F);
   }

   private void renderTestBo(DrawContext ct){
      RenderUtil.drawBo(ct, 20, 14, this.width - 40, 30, 12, -536863167);
      ct.drawTextWithShadow(this.textRenderer, "Render Framework â€” Test Bench", 34, 24, -16722689);
      String var2 = "drawBo validation Â· press ESC to close";
      int width = this.textRenderer.getWidth(var2);
      ct.drawTextWithShadow(this.textRenderer, var2, this.width - 34 - width, 24, -9525505);
   }

   private void renderScreenResize(DrawContext ct, int sw, int sh){
      MinecraftClient client = MinecraftClient.getInstance();
      int currentFps = client.getCurrentFps();
      int scaleFactor = client.getWindow().getScaleFactor();
      double intValue = ((Integer)client.options.getGuiScale().getValue()).intValue();
      String var9 = intValue == 0.0 ? "Auto (" + scaleFactor + "x)" : scaleFactor + "x";
      byte var10 = 26;
      int var11 = sh - var10 - 12;
      RenderUtil.drawBo(ct, 20, var11, sw - 40, var10, 10, -536863167);
      int var12 = var11 + 9;
      int valueOf = 34;
      valueOf = this.drawLabelValue(ct, "FPS", String.valueOf(currentFps), valueOf, var12);
      valueOf = this.drawLabelValue(ct, "GUI SCALE", var9, valueOf, var12);
      valueOf = this.drawLabelValue(ct, "SCALED", this.width + "x" + this.height, valueOf, var12);
      this.drawLabelValue(ct, "WINDOW", client.getWindow().getWidth() + "x" + client.getWindow().getHeight(), valueOf, var12);
   }

   private int drawLabelValue(DrawContext ct, String label, String value, int x, int y){
      ct.drawTextWithShadow(this.textRenderer, label, x, y, -9525505);
      int width2 = this.textRenderer.getWidth(label + " ");
      ct.drawTextWithShadow(this.textRenderer, value, x + width2, y, -16722689);
      int width = this.textRenderer.getWidth(value);
      return x + width2 + width + 28;
   }

   private void renderRadiusRow(DrawContext ct, int x, int y){
      this.drawTitleLabel(ct, "RADIUS", x, y);
      byte var4 = 70;
      byte var5 = 60;
      byte var6 = 12;
      int[] var7 = new int[]{0, 4, 10, 18, var5 / 2};
      int var8 = x;

      for (int var12 : var7) {
         RenderUtil.drawBo(ct, var8, y + 14, var4, var5, var12, -536863167);
         RenderUtil.drawBo(ct, var8 + 6, y + 20, var4 - 12, 4, Math.min(var12, 2), -16722689);
         ct.drawTextWithShadow(this.textRenderer, "r=" + var12, var8 + 6, y + var5, -6499073);
         var8 += var4 + var6;
      }
   }

   private void renderAlphaRow(DrawContext ct, int x, int y){
      this.drawTitleLabel(ct, "ALPHA", x, y);
      byte var4 = 70;
      byte var5 = 60;
      byte var6 = 12;
      RenderUtil.drawBo(ct, x, y + 34, (var4 + var6) * 5 - var6, 14, 4, -16737793);
      int[] var7 = new int[]{40, 90, 140, 200, 255};
      int var8 = x;

      for (int var12 : var7) {
         int var13 = var12 << 24 | 7745;
         RenderUtil.drawBo(ct, var8, y + 14, var4, var5, 10, var13);
         ct.drawTextWithShadow(this.textRenderer, "a=" + var12, var8 + 6, y + var5, -6499073);
         var8 += var4 + var6;
      }
   }

   private void renderSizeRow(DrawContext ct, int x, int y){
      this.drawTitleLabel(ct, "SIZE", x, y);
      int var4 = y + 14;
      int[][] var5 = new int[][]{{14, 14}, {30, 24}, {60, 40}, {100, 54}, {150, 64}};
      int var6 = x;

      for (int[] var10 : var5) {
         int var11 = var10[0];
         int var12 = var10[1];
         RenderUtil.drawBo(ct, var6, var4, var11, var12, 12, -536863167);
         RenderUtil.drawBo(ct, var6, var4, var11, var12, 12, 570479871);
         ct.drawTextWithShadow(this.textRenderer, var11 + "x" + var12, var6, var4 + 68, -6499073);
         var6 += var11 + 14;
      }
   }

   private void renderOverlapRow(DrawContext ct, int x, int y){
      this.drawTitleLabel(ct, "OVERLAP", x, y);
      int var4 = y + 14;
      RenderUtil.drawBo(ct, x, var4, 120, 80, 16, -1711328461);
      RenderUtil.drawBo(ct, x + 40, var4 + 20, 120, 80, 16, -1727998721);
      RenderUtil.drawBo(ct, x + 20, var4 + 45, 120, 80, 16, -1727987866);
   }

   private void renderClippingRow(DrawContext ct, int x, int y){
      this.drawTitleLabel(ct, "CLIPPING (scissor)", x, y);
      int var5 = y + 14;
      short var6 = 150;
      byte var7 = 90;
      RenderUtil.drawBo(ct, x - 2, var5 - 2, var6 + 4, var7 + 4, 10, 855677439);
      ct.enableScissor(x, var5, x + var6, var5 + var7);
      RenderUtil.drawBo(ct, x - 30, var5 - 20, var6 + 90, var7 + 70, 24, -536863167);
      RenderUtil.drawBo(ct, x + 10, var5 + 10, 200, 30, 12, -16722689);
      ct.disableScissor();
      ct.drawTextWithShadow(this.textRenderer, "clipped to window", x, var5 + var7 + 4, -6499073);
   }

   private void renderTextRow(DrawContext ct, int x, int y, int sw){
      this.drawTitleLabel(ct, "TEXT", x, y);
      int var5 = y + 16;
      RenderUtil.drawText(ct, "Scale:", x, var5 + 6, -6499073, true);
      float var6 = x + 52;
      float[] var7 = new float[]{0.5F, 0.75F, 1.0F, 1.5F, 2.0F};

      for (float var11 : var7) {
         RenderUtil.drawText(ct, "Aa" + var11, var6, var5, -16722689, true, var11);
         var6 += RenderUtil.getTextWidth("Aa" + var11, var11) + 14.0F;
      }

      int var18 = var5 + 34;
      RenderUtil.drawText(ct, "Color:", x, var18, -6499073, true);
      int textWidth = x + 52;
      int[] var20 = new int[]{-43691, -11141291, -11167233, -171, -43521};

      for (int var14 : var20) {
         RenderUtil.drawText(ct, "Sample", textWidth, var18, var14, true);
         textWidth = (int)(textWidth + (RenderUtil.getTextWidth("Sample") + 12.0F));
      }

      int var22 = var18 + 16;
      RenderUtil.drawText(ct, "Alpha:", x, var22, -6499073, true);
      int textWidth2 = x + 52;

      for (int var16 : new int[]{60, 120, 180, 255}) {
         int var17 = var16 << 24 | 54527;
         RenderUtil.drawText(ct, "a=" + var16, textWidth2, var22, var17, false);
         textWidth2 = (int)(textWidth2 + (RenderUtil.getTextWidth("a=" + var16) + 14.0F));
      }

      int var25 = var22 + 18;
      RenderUtil.drawText(ct, "Shadow ON", x + 52, var25, -16722689, true);
      RenderUtil.drawText(ct, "Shadow OFF", x + 160, var25, -16722689, false);
      RenderUtil.drawText(ct, "Shadow:", x, var25, -6499073, true);
      int var27 = var25 + 22;
      int var28 = x + 190;
      RenderUtil.drawBo(ct, var28, var27 - 2, 1, 40, 0, -16737793);
      RenderUtil.drawText(ct, "Align:", x, var27 + 14, -6499073, true);
      RenderUtil.drawText(ct, "left-anchored", var28 + 3, var27, -9525505, true);
      RenderUtil.drawCenteredText(ct, "centered", var28, var27 + 13, -16722689, true);
      RenderUtil.drawRightAlignedText(ct, "right-anchored", var28 - 3, var27 + 26, -9525505, true);
      int var29 = var27 + 44;
      RenderUtil.drawText(ct, "Unicode:", x, var29, -6499073, true);
      RenderUtil.drawText(ct, "Ã¡Ã©Ã®ÃµÃ¼ â˜… â˜€ â†’ Â± Â° Â§ â‚¬  Ð–Ð”Ð›  Î±Î²Î³Î´  â‘ â‘¡â‘¢", x + 62, var29, -16722689, true);
      this.renderTextInputRow(ct, x, var29 + 18, sw);
   }

   private void renderTextInputRow(DrawContext ct, int x, int y, int sw){
      byte var5 = 26;
      byte var6 = 5;
      byte var7 = 30;
      byte var8 = 11;
      int index = 0;

      for (int index2 = 0; index2 < var6; index2++) {
         for (int index3 = 0; index3 < var5; index3++) {
            int var12 = x + index3 * var7;
            int var13 = y + index2 * var8;
            if (var12 + var7 <= sw - 20) {
               int var14 = (index2 + index3) % 2 == 0 ? -9525505 : -6499073;
               RenderUtil.drawText(ct, "0" + Integer.toHexString(index & 0xFF), var12, var13, var14, false, 0.75F);
               index++;
            }
         }
      }

      RenderUtil.drawText(ct, index + " strings/frame â€” watch footer FPS", x, y + var6 * var8 + 2, -9525505, true);
   }

   private void renderAnimationRow(DrawContext ct, int x, int y, int mouseX, int mouseY){
      this.drawTitleLabel(ct, "ANIMATION", x, y);
      short var6 = 230;
      int var7 = y + 16;
      RenderUtil.drawText(ct, "progress", x, var7, -6499073, true);
      if (this.loopAnimation.isFinished()) {
         this.loopAnimation.reset();
         this.loopAnimation.start();
      }

      RenderUtil.drawBo(ct, x, var7 + 12, var6, 8, 4, -536863167);
      RenderUtil.drawBo(ct, (float)x, (float)(var7 + 12), var6 * this.loopAnimation.getProgress(), 8.0F, 4.0F, -16722689);
      int var8 = var7 + 30;
      RenderUtil.drawText(ct, "hover fade (move mouse over Box)", x, var8, -6499073, true);
      int var10 = var8 + 12;
      byte var11 = 90;
      byte var12 = 34;
      boolean var13 = mouseX >= x && mouseX <= x + var11 && mouseY >= var10 && mouseY <= var10 + var12;
      if (var13 != this.hovered) {
         this.hoverAnimation.reverse();
         this.hovered = var13;
      }

      float get3 = this.hoverAnimation.get();
      int var15 = withAlpha(-536863167, 120);
      int var16 = lerpColor(var15, -16722689, get3);
      RenderUtil.drawBo(ct, x, var10, var11, var12, 8, var16);
      RenderUtil.drawCenteredText(ct, "HOVER", x + var11 / 2.0F, var10 + 13, -16116700, false);
      int var17 = var10 + var12 + 12;
      RenderUtil.drawText(ct, "expand (ease-in-out)", x, var17, -6499073, true);
      if (this.expandAnimation.isFinished()) {
         this.expandAnimation.reverse();
      }

      float get2 = 20.0F + (var6 - 20) * this.expandAnimation.get();
      RenderUtil.drawBo(ct, (float)x, (float)(var17 + 12), get2, 16.0F, 6.0F, -16737793);
      int var19 = var17 + 36;
      RenderUtil.drawText(ct, "reverse (cubic ping-pong)", x, var19, -6499073, true);
      if (this.bounceAnimation.isFinished()) {
         this.bounceAnimation.reverse();
      }

      RenderUtil.drawBo(ct, x, var19 + 14, var6, 2, 0, -536863167);
      float get4 = x + (var6 - 10) * this.bounceAnimation.get();
      RenderUtil.drawBo(ct, get4, (float)(var19 + 10), 10.0F, 10.0F, 5.0F, -16722689);
      int var21 = var19 + 30;
      RenderUtil.drawText(ct, "easing curves", x, var21, -6499073, true);
      int var22 = var21 + 14;
      byte var23 = 48;
      int var24 = var6 / this.easingCurveAnimations.length;

      for (int index = 0; index < this.easingCurveAnimations.length; index++) {
         Animation animation = this.easingCurveAnimations[index];
         if (animation.isFinished()) {
            animation.reverse();
         }

         float get = clamp01(animation.get());
         int var28 = x + index * var24;
         int round = Math.round(var23 * get);
         RenderUtil.drawBo(ct, var28, var22, var24 - 2, var23, 2, -536863167);
         RenderUtil.drawBo(ct, var28, var22 + (var23 - round), var24 - 2, round, 2, -16722689);
      }

      int var30 = var22 + var23 + 6;
      RenderUtil.drawText(ct, AnimationManager.count() + " anims/frame â€” see footer FPS", x, var30, -9525505, true, 0.9F);
   }

   private static int withAlpha(int color, int alpha){
      return alpha << 24 | color & 16777215;
   }

   private static int lerpColor(int from, int to, float t){
      t = clamp01(t);
      int round4 = Math.round((from >>> 24 & 0xFF) + ((to >>> 24 & 0xFF) - (from >>> 24 & 0xFF)) * t);
      int round3 = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
      int round2 = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
      int round = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
      return round4 << 24 | round3 << 16 | round2 << 8 | round;
   }

   private static float clamp01(float v){
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private void drawTitleLabel(DrawContext ct, String text, int x, int y){
      ct.drawTextWithShadow(this.textRenderer, text, x, y, -16722689);
   }
}

