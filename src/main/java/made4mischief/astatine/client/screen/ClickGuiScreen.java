package made4mischief.astatine.client.screen;

import java.util.ArrayList;
import java.util.List;
import made4mischief.astatine.client.gui.HoverTooltip;
import made4mischief.astatine.client.gui.component.CategoryColumn;
import made4mischief.astatine.client.gui.component.ComponentManager;
import made4mischief.astatine.client.gui.component.FloatingConfigPanel;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.gui.component.widget.ModuleRow;
import made4mischief.astatine.client.gui.component.widget.SearchBar;
import made4mischief.astatine.client.hud.HudRenderer;
import made4mischief.astatine.client.hud.NotificationRenderer;
import made4mischief.astatine.client.hud.PixelPetRenderer;
import made4mischief.astatine.client.modules.Category;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.modules.ModuleManager;
import made4mischief.astatine.client.modules.render.HUDModule;
import made4mischief.astatine.client.modules.render.HandViewModule;
import made4mischief.astatine.client.modules.render.PixelPetModule;
import made4mischief.astatine.client.modules.smp.BaseDiggerModule;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationManager;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;

@Environment(EnvType.CLIENT)
public class ClickGuiScreen extends Screen {
   private static final float COLUMN_GAP = 12.0F;
   private static final float SEARCH_BAR_HEIGHT = 38.0F;
   private static final float HEADER_X_INSET = 16.0F;
   private static final float SEARCH_BAR_X_OFFSET = 190.0F;
   private static final float SEARCH_BAR_RIGHT_MARGIN = 80.0F;
   private static final float CONFIG_PANEL_SHIFT = 219.0F;
   private static final float CONFIG_PANEL_GAP = 10.0F;
   private static final float ZOOM_FIT_FACTOR = 0.9F;
   private static final float MAX_ZOOM = 1.0F;
   private float zoom = 1.0F;
   private static float guiScale = 1.0F;
   private static final Animation animatedScale = new Animation(1.0F, 1.0F, 200L, AnimationType.EASE_OUT);
   private boolean scaleSliderDragging;
   private static final Theme[] configTabs = ThemeManager.available();
   private static final float CONFIG_MAX_WIDTH = 820.0F;
   private static final float CONFIG_SIDE_MARGIN = 36.0F;
   private static final float CLOSE_BUTTON_X_INSET = 28.0F;
   private static final float THEME_BUTTONS_X_INSET = 30.0F;
   private static final float LABEL_TEXT_SCALE = 0.8F;
   private static final float THEME_TAB_GAP = 22.0F;
   private static final float LABEL_VALUE_GAP = 30.0F;
   private final ComponentManager uiRoot = new ComponentManager();
   private final List<CategoryColumn> columns = new ArrayList<>();
   private SearchBar searchBar;
   private final FloatingConfigPanel config = new FloatingConfigPanel();
   private final HoverTooltip hoverTooltip = new HoverTooltip();
   private float configPanel;
   private float configPanelY;
   private ModuleRow selectedRow;
   private CategoryColumn selectedColumn;
   private final Animation lastSelectedColumn = new Animation(0.0F, 1.0F, 320L, AnimationType.EASE_OUT);
   private boolean draggingColumn;
   private CategoryColumn dragColumn;
   private double dragStartX;
   private double dragStartY;
   private float columnOffsetX;
   private float columnOffsetY;
   private boolean dragActive;
   private boolean configDragging;
   private boolean configDragged;
   private double configDragStartX;
   private double configDragStartY;
   private float configX;
   private float configY;
   private boolean editorMode;
   private boolean showHudPreview;
   private boolean showPetPreview;

   public ClickGuiScreen(){
      super(Text.literal("ClickGUI"));
   }

   public boolean shouldPause(){
      return false;
   }

   public void removed(){
      BaseDiggerModule.forceCloseEditorCamera();
      HudRenderer.endEditorDrag();
      PixelPetRenderer.endEditorDrag();
      super.removed();
   }

   public void tick(){
      super.tick();
      this.uiRoot.tick();
   }

   public void renderBackground(DrawContext ct, int mouseX, int mouseY, float delta){
   }

   protected void init(){
      super.init();
      this.uiRoot.resize(this.width, this.height);
      this.uiRoot.root().clear();
      this.columns.clear();

      for (Category category : Category.values()) {
         List<Module> list = ModuleManager.INSTANCE.getModules(category);
         if (!list.isEmpty()) {
            CategoryColumn categoryColumn = new CategoryColumn(capitalizeCategory(category), 0.0F, 0.0F);
            categoryColumn.setActive(true);
            categoryColumn.setOnConfigRequest(this::toggleRow);

            for (Module module : list) {
               categoryColumn.addModule(module);
            }

            this.columns.add(categoryColumn);
         }
      }

      float columnsWidth2 = this.getColumnsWidth();
      this.searchBar = new SearchBar("Search modules...", 0.0F, 0.0F, columnsWidth2, 38.0F);
      this.searchBar.setOnChange(this::filterModules);
      float height = 0.0F;

      for (int index2 = 0; index2 < this.columns.size(); index2++) {
         height = Math.max(height, this.columns.get(index2).getHeight());
      }

      float columnsWidth = this.getColumnsWidth() + 32.0F;
      float var13 = height + 84.0F;
      this.configPanel = (this.width - columnsWidth) / 2.0F;
      this.configPanelY = (this.height - var13) / 2.0F;
      this.uiRoot.root().add(this.searchBar);
      this.uiRoot.root().add(this.config);

      for (int index = 0; index < this.columns.size(); index++) {
         this.uiRoot.root().add(this.columns.get(index));
      }

      this.dismissSearchBar();
   }

   private float getColumnsWidth(){
      int size = this.columns.size();
      return size == 0 ? 220.0F : size * 220.0F + (size - 1) * 12.0F;
   }

   private float getColumnsHeight(){
      float height = 0.0F;

      for (int index = 0; index < this.columns.size(); index++) {
         height = Math.max(height, this.columns.get(index).getHeight());
      }

      return 54.0F + height;
   }

   private static String capitalizeCategory(Category category){
      String toLowerCase = category.name().toLowerCase();
      return Character.toUpperCase(toLowerCase.charAt(0)) + toLowerCase.substring(1);
   }

   private void dismissSearchBar(){
      if (this.searchBar != null) {
         if (this.isEditorMode()) {
            this.config.setMaximumHeight(this.height - 16.0F);
            if (this.configDragged) {
               this.setConfigPos(this.configX, this.configY);
            } else {
               this.config.setPosition((this.width - 205.0F) / 2.0F, Math.max(8.0F, (this.height - this.config.getHeight()) / 2.0F));
            }
         } else {
            float var1 = this.configPanel;
            float var2 = this.configPanelY;
            float get2 = this.lastSelectedColumn.get() * 219.0F;
            float columnsWidth = this.getColumnsWidth() + 32.0F + get2;
            float topSliderTotalWidth = 200.0F;
            this.searchBar.setPosition(var1 + 190.0F, var2 + 7.0F);
            this.searchBar.setSize(Math.max(120.0F, columnsWidth - 190.0F - topSliderTotalWidth - 40.0F), 38.0F);
            float var5 = var2 + 52.0F + 16.0F;
            int indexOf = this.selectedColumn == null ? -1 : this.columns.indexOf(this.selectedColumn);
            float var7 = var1 + 16.0F;

            for (int index = 0; index < this.columns.size(); index++) {
               CategoryColumn categoryColumn = this.columns.get(index);
               float var10 = indexOf >= 0 && index > indexOf ? get2 : 0.0F;
               categoryColumn.setPosition(var7 + var10, var5);
               var7 += 232.0F;
            }

            if (this.selectedColumn != null && this.selectedRow != null) {
               float x = this.selectedColumn.getX() + 220.0F + 10.0F;
               float get = x - (1.0F - this.lastSelectedColumn.get()) * 219.0F;
               this.config.setMaximumHeight(this.selectedColumn.getPanelHeight());
               this.config.setPosition(get, this.selectedColumn.getPanelTop());
            }
         }
      }
   }

   private void resetZoom(){
      if (this.isEditorMode()) {
         this.zoom = 1.0F;
      } else {
         float height = 0.0F;

         for (int index = 0; index < this.columns.size(); index++) {
            height = Math.max(height, this.columns.get(index).getHeight());
         }

         float columnsWidth = this.getColumnsWidth() + 32.0F + 219.0F;
         float var3 = height + 84.0F;
         if (!(columnsWidth <= 0.0F) && !(var3 <= 0.0F)) {
            MinecraftClient client = MinecraftClient.getInstance();
            float framebufferHeight = Math.min((float)this.width * 0.95F / columnsWidth, (float)this.height * 0.95F / var3);
            float baseZoom = Math.min(1.0F, framebufferHeight);
            this.zoom = baseZoom * animatedScale.get();
         } else {
            this.zoom = animatedScale.get();
         }
      }
   }

   private double toWorldX(double screenX){
      return (screenX - this.width / 2.0) / this.zoom + this.width / 2.0;
   }

   private double toWorldY(double screenY){
      return (screenY - this.height / 2.0) / this.zoom + this.height / 2.0;
   }

   private float getWorldMinX(){
      return this.width * 0.5F * (1.0F - 1.0F / this.zoom);
   }

   private float getWorldMaxX(){
      return this.width * 0.5F * (1.0F + 1.0F / this.zoom);
   }

   private float getWorldMinY(){
      return this.height * 0.5F * (1.0F - 1.0F / this.zoom);
   }

   private float getWorldMaxY(){
      return this.height * 0.5F * (1.0F + 1.0F / this.zoom);
   }

   private void toggleRow(ModuleRow row){
      if (this.selectedRow == row) {
         this.closePopups();
      } else {
         this.expandColumn(row);
      }
   }

   private void expandColumn(ModuleRow row){
      CategoryColumn categoryColumn = this.getColumnForRow(row);
      if (this.selectedColumn != null && this.selectedColumn != categoryColumn) {
         this.selectedColumn.setActive(true);
      }

      this.selectedRow = row;
      this.selectedColumn = categoryColumn;
      this.configDragging = false;
      this.configDragged = false;
      if (isHUDLikeModule(row.getModule())) {
         this.editorMode = true;
         this.showHudPreview = row.getModule() instanceof HUDModule;
         this.showPetPreview = row.getModule() instanceof PixelPetModule;
         this.setSearchBarVisible(false);
         this.lastSelectedColumn.snapTo(0.0F);
         this.dragActive = false;
         HudRenderer.endEditorDrag();
         PixelPetRenderer.endEditorDrag();
         this.config.open(row.getModule(), 0.0F, 0.0F);
         if (row.getModule() instanceof BaseDiggerModule) {
            BaseDiggerModule.beginEditorPreview();
         }

         this.dismissSearchBar();
      } else {
         this.dismissSearchBar();
         float x = (categoryColumn != null ? categoryColumn.getX() + 220.0F : this.configPanel) + 10.0F;
         this.config.open(row.getModule(), x, row.getY());
         this.lastSelectedColumn.setTarget(1.0F);
      }
   }

   private void closePopups(){
      this.uiRoot.focus(null);
      if (this.isEditorMode()) {
         if (this.selectedRow != null && this.selectedRow.getModule() instanceof BaseDiggerModule) {
            BaseDiggerModule.endEditorPreview();
         }

         this.editorMode = false;
         this.showHudPreview = false;
         this.showPetPreview = false;
         HudRenderer.endEditorDrag();
         PixelPetRenderer.endEditorDrag();
         this.configDragging = false;
         this.configDragged = false;
         this.config.closeImmediately();
         this.lastSelectedColumn.snapTo(0.0F);
         this.dragActive = false;
         this.selectedRow = null;
         this.selectedColumn = null;
         this.setSearchBarVisible(true);
         this.dismissSearchBar();
      } else {
         this.config.close();
         this.configDragging = false;
         this.configDragged = false;
         this.lastSelectedColumn.setTarget(0.0F);
         this.dragActive = true;
      }
   }

   private void setSearchBarVisible(boolean visible){
      if (this.searchBar != null) {
         this.searchBar.setVisible(visible);
      }

      for (int index = 0; index < this.columns.size(); index++) {
         this.columns.get(index).setVisible(visible);
      }
   }

   private static boolean isHUDLikeModule(Module module){
      return module instanceof HUDModule || module instanceof PixelPetModule || module instanceof HandViewModule || module instanceof BaseDiggerModule;
   }

   private CategoryColumn getColumnForRow(ModuleRow row){
      for (int index = 0; index < this.columns.size(); index++) {
         if (this.columns.get(index).getRows().contains(row)) {
            return this.columns.get(index);
         }
      }

      return null;
   }

   private void filterModules(String query){
      for (int index = 0; index < this.columns.size(); index++) {
         this.columns.get(index).filter(query);
      }

      if (this.selectedRow != null && !this.selectedRow.isVisible()) {
         this.closePopups();
      }
   }

   public void render(DrawContext ct, int mouseX, int mouseY, float delta){
      super.render(ct, mouseX, mouseY, delta);
      AnimationManager.update();
      BaseDiggerModule.updateEditorCamera();
      if (this.dragActive && this.lastSelectedColumn.isFinished()) {
         this.selectedRow = null;
         this.selectedColumn = null;
         this.dragActive = false;
      }

      this.toggleHudEditor();
      Theme theme = ThemeManager.active();
      this.resetZoom();
      this.dismissSearchBar();
      ct.getMatrices().pushMatrix();
      ct.getMatrices().translate(this.width / 2.0F, this.height / 2.0F);
      ct.getMatrices().scale(this.zoom, this.zoom);
      ct.getMatrices().translate(-this.width / 2.0F, -this.height / 2.0F);
      if (this.isEditorMode()) {
         if (this.showHudPreview) {
            HudRenderer.renderEditorPreview(ct);
         }

         if (this.showPetPreview) {
            PixelPetRenderer.renderEditorPreview(ct);
         }

         this.uiRoot.render(ct, this.toWorldX(mouseX), this.toWorldY(mouseY), delta);
         ct.getMatrices().popMatrix();
         NotificationRenderer.renderOverlay(ct);
      } else {
         float var6 = this.configPanel;
         float var7 = this.configPanelY;
         float height = 0.0F;

         for (int index = 0; index < this.columns.size(); index++) {
            height = Math.max(height, this.columns.get(index).getHeight());
         }

         float get = this.lastSelectedColumn.get() * 219.0F;
         float columnsWidth = this.getColumnsWidth() + 32.0F + get;
         float var11 = height + 84.0F;
         int accent = ColorUtil.scaleAlpha(theme.accent(), 0.35F);
         RenderUtil.drawBo(ct, var6 - 1.5F, var7 - 1.5F, columnsWidth + 3.0F, var11 + 3.0F, 16.5F, accent);
         int surface = ColorUtil.withAlpha(theme.surface(), 180);
         RenderUtil.drawBo(ct, var6, var7, columnsWidth, var11, 15.0F, surface);
         int border = ColorUtil.scaleAlpha(theme.border(), 0.5F);
         RenderUtil.drawBo(ct, var6 + 16.0F, var7 + 52.0F, columnsWidth - 32.0F, 1.0F, 0.0F, border);
         float var15 = var6 + 16.0F;
         RenderUtil.drawText(ct, "ASTATINE", var15, var7 + 9.0F, theme.accent(), true, 1.3F);
         int text = ColorUtil.lerp(theme.accentSecondary(), theme.text(), 0.55F);
         RenderUtil.drawText(ct, "CLIENT", var15 + 1.0F, var7 + 31.0F, text, true, 0.76F);
         float var17 = var6 + columnsWidth - 28.0F;
         float var18 = var7 + 18.0F;
         RenderUtil.drawText(ct, "x", var17, var18, theme.textDim(), true, 1.0F);

         // Header Scale Slider
         float topSliderWidth = 100.0F;
         float topSliderHeight = 6.0F;
         float topSliderTrackX = var17 - 20.0F - topSliderWidth;
         float topScaleLabelWidth = RenderUtil.getTextWidth("SCALE ", 0.76F);
         float topSliderStartX = topSliderTrackX - topScaleLabelWidth - 6.0F;
         float topScaleTextY = var7 + 18.0F;
         float topSliderTrackY = var7 + 21.0F;

         RenderUtil.drawText(ct, "SCALE", topSliderStartX, topScaleTextY, ColorUtil.scaleAlpha(theme.text(), 0.7F), true, 0.76F);
         RenderUtil.drawBo(ct, topSliderTrackX, topSliderTrackY, topSliderWidth, topSliderHeight, 3.0F, ColorUtil.scaleAlpha(theme.surfaceElevated(), 0.8F));
         float scaleFraction = (guiScale - 0.5F) / 1.5F;
         scaleFraction = Math.max(0.0F, Math.min(1.0F, scaleFraction));
         float fillWidth = Math.max(topSliderHeight, topSliderWidth * scaleFraction);
         RenderUtil.drawBo(ct, topSliderTrackX, topSliderTrackY, fillWidth, topSliderHeight, 3.0F, theme.accent());
         float knobX = topSliderTrackX + topSliderWidth * scaleFraction;
         float knobY = topSliderTrackY + topSliderHeight / 2.0F;
         RenderUtil.drawCircle(ct, knobX, knobY, 5.0F, theme.accent());
         RenderUtil.drawCircle(ct, knobX, knobY, 2.5F, -1);
         String scaleStr = String.format("%.1fx", guiScale);
         RenderUtil.drawText(ct, scaleStr, topSliderTrackX + topSliderWidth * 0.5F - RenderUtil.getTextWidth(scaleStr, 0.65F) * 0.5F, topScaleTextY - 10.0F, theme.accent(), true, 0.65F);

         double toWorldX = this.toWorldX(mouseX);
         double toWorldY = this.toWorldY(mouseY);
         this.uiRoot.render(ct, toWorldX, toWorldY, delta);
         String moduleNameAt = this.getModuleNameAt(toWorldX, toWorldY);
         ct.getMatrices().popMatrix();
         this.hoverTooltip.render(ct, moduleNameAt, mouseX, mouseY, this.width, this.height, theme);
         this.renderServerInfo(ct, theme);
         NotificationRenderer.renderOverlay(ct);
      }
   }

   private String getModuleNameAt(double mouseX, double mouseY){
      for (int index2 = this.columns.size() - 1; index2 >= 0; index2--) {
         CategoryColumn categoryColumn = this.columns.get(index2);
         if (categoryColumn.isWithin(mouseX, mouseY)) {
            for (ModuleRow moduleRow : categoryColumn.getRows()) {
               if (moduleRow.isHovered()) {
                  return moduleRow.getModule().getName();
               }
            }
         }
      }

      return null;
   }

   private boolean isEditorMode(){
      return this.editorMode;
   }

   private void renderServerInfo(DrawContext ct, Theme theme){
      MinecraftClient client = MinecraftClient.getInstance();
      String currentServerEntry = client.getCurrentServerEntry() != null ? client.getCurrentServerEntry().address : "Singleplayer";
      int latency = 0;
      if (client.getNetworkHandler() != null && client.player != null) {
         PlayerListEntry playerListEntry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
         if (playerListEntry != null) {
            latency = playerListEntry.getLatency();
         }
      }

      int scaleFactor = Math.max(1, client.getWindow().getScaleFactor());
      float var7 = 1.0F / scaleFactor;
      float configPanelWidth = this.getConfigPanelWidth(scaleFactor);
      float var9 = 36.0F;
      float var10 = (this.width - configPanelWidth) / 2.0F;
      float configPanelHeight = this.getConfigPanelHeight(scaleFactor) - var9 - 18.0F;
      ct.getMatrices().pushMatrix();
      ct.getMatrices().translate(this.width / 2.0F, this.height / 2.0F);
      ct.getMatrices().scale(var7, var7);
      ct.getMatrices().translate(-this.width / 2.0F, -this.height / 2.0F);
      int accent = ColorUtil.scaleAlpha(theme.accent(), 0.65F);
      RenderUtil.drawBo(ct, var10 - 1.5F, configPanelHeight - 1.5F, configPanelWidth + 3.0F, var9 + 3.0F, 15.5F, accent);
      RenderUtil.drawBo(ct, var10, configPanelHeight, configPanelWidth, var9, 14.0F, theme.surfaceElevated());
      float textHeight2 = configPanelHeight + (var9 - RenderUtil.getTextHeight(0.8F)) / 2.0F;
      float configButtonX = this.getConfigButtonX(var10, configPanelWidth);
      float var15 = configButtonX - 58.0F;
      float var16 = var15 - 28.0F;
      float var17 = var16 - 24.0F;

      String currentFps = String.valueOf(client.getCurrentFps());
      String var19 = latency + "MS";
      String gameVersion = client.getGameVersion();
      float var21 = var10 + 28.0F;
      this.drawLabelValue(ct, theme, "FPS", currentFps, var21, textHeight2);
      var21 += this.getLabelValueWidth("FPS", currentFps) + 30.0F;
      this.drawLabelValue(ct, theme, "PING", var19, var21, textHeight2);
      var21 += this.getLabelValueWidth("PING", var19) + 30.0F;
      float labelValueWidth = this.getLabelValueWidth("VERSION", gameVersion);
      float max = Math.max(0.0F, var17 - var21 - 30.0F - labelValueWidth);
      String toUpperCase = this.padValueText("SERVER", currentServerEntry.toUpperCase(), max);
      this.drawLabelValue(ct, theme, "SERVER", toUpperCase, var21, textHeight2);
      var21 += this.getLabelValueWidth("SERVER", toUpperCase) + 30.0F;
      this.drawLabelValue(ct, theme, "VERSION", gameVersion, var21, textHeight2);

      RenderUtil.drawBo(ct, var16, configPanelHeight + 10.0F, 1.0F, 16.0F, 0.0F, ColorUtil.scaleAlpha(theme.accent(), 0.25F));
      float textHeight = configPanelHeight + (var9 - RenderUtil.getTextHeight(0.74F)) / 2.0F;
      RenderUtil.drawText(ct, "THEME", var15, textHeight, ColorUtil.scaleAlpha(theme.text(), 0.6F), false, 0.74F);

      for (int index = 0; index < configTabs.length; index++) {
         float var27 = configButtonX + index * 22.0F;
         float var28 = configPanelHeight + var9 / 2.0F;
         if (ThemeManager.active() == configTabs[index]) {
            RenderUtil.drawCircle(ct, var27, var28, 10.0F, 352321535);
            RenderUtil.drawCircle(ct, var27, var28, 8.0F, -1275068417);
         }

         RenderUtil.drawCircle(ct, var27, var28, 7.0F, configTabs[index].accent());
         if (configTabs[index] == ThemeManager.custom()) {
            RenderUtil.drawCircle(ct, var27, var28, 3.0F, ThemeManager.custom().accentSecondary());
         }
      }

      ct.getMatrices().popMatrix();
   }

   private void drawLabelValue(DrawContext ct, Theme theme, String label, String value, float x, float y){
      RenderUtil.drawText(ct, label + " ", x, y, ColorUtil.scaleAlpha(theme.text(), 0.8F), false, 0.8F);
      float textWidth = x + RenderUtil.getTextWidth(label + " ", 0.8F);
      RenderUtil.drawText(ct, value, textWidth, y, theme.accent(), false, 0.8F);
   }

   private float getLabelValueWidth(String label, String value){
      return RenderUtil.getTextWidth(label + " ", 0.8F) + RenderUtil.getTextWidth(value, 0.8F);
   }

   private String padValueText(String label, String value, float pairMaxWidth){
      float textWidth = RenderUtil.getTextWidth(label + " ", 0.8F);
      float var5 = pairMaxWidth - textWidth;
      if (var5 <= 0.0F) {
         return "";
      } else if (RenderUtil.getTextWidth(value, 0.8F) <= var5) {
         return value;
      } else {
         String var6 = "...";
         if (RenderUtil.getTextWidth(var6, 0.8F) > var5) {
            return "";
         } else {
            for (int index = value.length() - 1; index > 0; index--) {
               String substring = value.substring(0, index) + var6;
               if (RenderUtil.getTextWidth(substring, 0.8F) <= var5) {
                  return substring;
               }
            }

            return var6;
         }
      }
   }

   private float getConfigPanelWidth(int guiScale){
      float var2 = (float)this.width * guiScale - 36.0F;
      return Math.min(820.0F, Math.max(360.0F, var2));
   }

   private float getConfigButtonX(float barX, float barWidth){
      return barX + barWidth - 30.0F - (configTabs.length - 1) * 22.0F;
   }

   private float getConfigPanelHeight(int guiScale){
      return this.height * 0.5F * (1.0F + guiScale);
   }

   private double toScaledX(double screenX){
      int scaleFactor = Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
      return (screenX - this.width / 2.0) * scaleFactor + this.width / 2.0;
   }

   private double toScaledY(double screenY){
      int scaleFactor = Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
      return (screenY - this.height / 2.0) * scaleFactor + this.height / 2.0;
   }

   private boolean isMouseInsideConfig(double screenX, double screenY){
      int scaleFactor = Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor());
      double toScaledX = this.toScaledX(screenX);
      double toScaledY = this.toScaledY(screenY);
      float configPanelWidth = this.getConfigPanelWidth(scaleFactor);
      float var11 = (this.width - configPanelWidth) / 2.0F;
      float configButtonX = this.getConfigButtonX(var11, configPanelWidth);
      float var9 = 36.0F;
      float configPanelHeight = this.getConfigPanelHeight(scaleFactor) - var9 - 18.0F;

      for (int index = 0; index < configTabs.length; index++) {
         float var15Theme = configButtonX + index * 22.0F;
         double var16Theme = toScaledX - var15Theme;
         double var18Theme = toScaledY - (configPanelHeight + var9 / 2.0F);
         if (var16Theme * var16Theme + var18Theme * var18Theme <= 100.0) {
            if (configTabs[index] == ThemeManager.custom()) {
               this.toggleHudEditor();
            }

            ThemeManager.set(configTabs[index]);
            if (configTabs[index] == ThemeManager.custom()) {
               this.expandHudColumn();
            }

            return true;
         }
      }

      return false;
   }

   private void updateScaleFromMouse(double mouseX, float trackX, float sliderWidth){
      float fraction = (float)((mouseX - trackX) / sliderWidth);
      fraction = Math.max(0.0F, Math.min(1.0F, fraction));
      float newScale = 0.5F + fraction * 1.5F;
      newScale = Math.round(newScale * 10.0F) / 10.0F;
      if (Math.abs(newScale - guiScale) > 0.01F) {
         guiScale = newScale;
         animatedScale.setTarget(guiScale);
      }
   }

   private void toggleHudEditor(){
      HUDModule hUDModule = ModuleManager.INSTANCE.getModule(HUDModule.class);
      if (hUDModule != null) {
         ThemeManager.setCustomColors(hUDModule.getCustomAccent(), hUDModule.getCustomSecondary());
      }
   }

   private void expandHudColumn(){
      for (CategoryColumn categoryColumn : this.columns) {
         for (ModuleRow moduleRow : categoryColumn.getRows()) {
            if (moduleRow.getModule() instanceof HUDModule) {
               this.expandColumn(moduleRow);
               return;
            }
         }
      }
   }

   public boolean mouseClicked(Click click, boolean doubled){
      if (this.isEditorMode()) {
         double comp_4798 = this.toWorldX(click.x());
         double comp_4799 = this.toWorldY(click.y());
         if (click.button() == 0 && this.config.isOpen() && this.isInConfigHeader(comp_4798, comp_4799)) {
            this.beginConfigDrag(comp_4798, comp_4799);
            return true;
         } else {
            boolean button = this.uiRoot.mouseClicked(comp_4798, comp_4799, click.button());
            if (!button && this.showHudPreview && click.button() == 0 && HudRenderer.beginEditorDrag(comp_4798, comp_4799, this.width, this.height)) {
               return true;
            } else if (!button
               && this.showPetPreview
               && click.button() == 0
               && PixelPetRenderer.beginEditorDrag(comp_4798, comp_4799, this.width, this.height)) {
               return true;
            } else if (button
               || this.config.isWithin(comp_4798, comp_4799)
               || this.showHudPreview && HudRenderer.isEditorDragging()
               || this.showPetPreview && PixelPetRenderer.isEditorDragging()) {
               return button || super.mouseClicked(click, doubled);
            } else {
               this.closePopups();
               return true;
            }
         }
      } else if (click.button() == 0 && this.isMouseInsideConfig(click.x(), click.y())) {
         return true;
      } else {
         double comp_47982 = this.toWorldX(click.x());
         double comp_47992 = this.toWorldY(click.y());
         int button2 = click.button();
         float var8 = this.configPanel;
         float var9 = this.configPanelY;
         float get = this.lastSelectedColumn.get() * 219.0F;
         float columnsWidth = this.getColumnsWidth() + 32.0F + get;
         if (button2 == 0 && comp_47982 >= var8 + columnsWidth - 32.0F && comp_47982 <= var8 + columnsWidth - 12.0F && comp_47992 >= var9 + 10.0F && comp_47992 <= var9 + 36.0F) {
            this.close();
            return true;
         } else {
            // Header scale slider hit detection
            float var17Close = var8 + columnsWidth - 28.0F;
            float topSliderWidth = 100.0F;
            float topSliderTrackX = var17Close - 20.0F - topSliderWidth;
            float topSliderTrackY = var9 + 21.0F;
            if (button2 == 0 && comp_47982 >= topSliderTrackX - 8.0F && comp_47982 <= topSliderTrackX + topSliderWidth + 8.0F && comp_47992 >= var9 + 10.0F && comp_47992 <= var9 + 36.0F) {
               this.scaleSliderDragging = true;
               this.updateScaleFromMouse(comp_47982, topSliderTrackX, topSliderWidth);
               return true;
            }

            boolean var12 = comp_47982 >= var8 && comp_47982 <= var8 + columnsWidth && comp_47992 >= var9 && comp_47992 <= var9 + 52.0F;
            boolean var13 = comp_47982 >= var8 + 190.0F && comp_47982 <= topSliderTrackX - 40.0F && comp_47992 >= var9 + 7.0F && comp_47992 <= var9 + 45.0F;
            boolean var14 = comp_47982 >= var8 + columnsWidth - 32.0F && comp_47982 <= var8 + columnsWidth - 12.0F && comp_47992 >= var9 + 10.0F && comp_47992 <= var9 + 36.0F;
            boolean var14Slider = comp_47982 >= topSliderTrackX - 8.0F && comp_47982 <= topSliderTrackX + topSliderWidth + 8.0F && comp_47992 >= var9 + 10.0F && comp_47992 <= var9 + 36.0F;
            if (button2 == 0 && var12 && !var13 && !var14 && !var14Slider) {
               this.beginColumnDrag(comp_47982, comp_47992, null);
               return true;
            } else {
               if (button2 == 0) {
                  for (int index = 0; index < this.columns.size(); index++) {
                     CategoryColumn categoryColumn = this.columns.get(index);
                     if (categoryColumn.isWithinTab(comp_47982, comp_47992)) {
                        this.beginColumnDrag(comp_47982, comp_47992, categoryColumn);
                        return true;
                     }
                  }
               }

               boolean mouseClicked = this.uiRoot.mouseClicked(comp_47982, comp_47992, button2);
               if (!mouseClicked && this.selectedRow != null && !this.config.isWithin(comp_47982, comp_47992)) {
                  this.closePopups();
               }

               return mouseClicked || super.mouseClicked(click, doubled);
            }
         }
      }
   }

   public boolean mouseReleased(Click click){
      if (this.scaleSliderDragging) {
         this.scaleSliderDragging = false;
         return true;
      } else if (this.configDragging) {
         this.configDragging = false;
         return true;
      } else if (HudRenderer.isEditorDragging()) {
         HudRenderer.endEditorDrag();
         return true;
      } else if (PixelPetRenderer.isEditorDragging()) {
         PixelPetRenderer.endEditorDrag();
         return true;
      } else if (this.draggingColumn) {
         this.endColumnDrag();
         return true;
      } else {
         boolean button = this.uiRoot.mouseReleased(this.toWorldX(click.x()), this.toWorldY(click.y()), click.button());
         return button || super.mouseReleased(click);
      }
   }

   public void openModuleConfig(double mouseX, double mouseY){
      double toWorldX = this.toWorldX(mouseX);
      double toWorldY = this.toWorldY(mouseY);
      if (this.scaleSliderDragging) {
         float var8 = this.configPanel;
         float get = this.lastSelectedColumn.get() * 219.0F;
         float columnsWidth = this.getColumnsWidth() + 32.0F + get;
         float var17Close = var8 + columnsWidth - 28.0F;
         float topSliderWidth = 100.0F;
         float topSliderTrackX = var17Close - 20.0F - topSliderWidth;
         this.updateScaleFromMouse(toWorldX, topSliderTrackX, topSliderWidth);
      }

      if (this.configDragging) {
         this.dragConfig(toWorldX, toWorldY);
      }

      if (this.showHudPreview && HudRenderer.isEditorDragging()) {
         HudRenderer.updateEditorDrag(toWorldX, toWorldY, this.width, this.height);
      }

      if (this.showPetPreview && PixelPetRenderer.isEditorDragging()) {
         PixelPetRenderer.updateEditorDrag(toWorldX, toWorldY, this.width, this.height);
      }

      if (this.draggingColumn) {
         this.dragColumn(toWorldX, toWorldY);
      }

      this.uiRoot.mouseMoved(toWorldX, toWorldY);
      super.mouseMoved(mouseX, mouseY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
      double toWorldX = this.toWorldX(mouseX);
      double toWorldY = this.toWorldY(mouseY);
      return this.uiRoot.mouseScrolled(this.toWorldX(mouseX), this.toWorldY(mouseY), horizontal, vertical)
         ? true
         : super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
   }

   public boolean keyPressed(KeyInput keyInput){
      if (keyInput.key() == 256 && this.selectedRow != null) {
         this.closePopups();
         return true;
      } else {
         return this.uiRoot.keyPressed(keyInput.key(), keyInput.scancode(), keyInput.modifiers()) ? true : super.keyPressed(keyInput);
      }
   }

   public boolean charTyped(CharInput charInput){
      return this.uiRoot.charTyped((char)charInput.codepoint(), charInput.modifiers()) ? true : super.charTyped(charInput);
   }

   private void beginColumnDrag(double mouseX, double mouseY, CategoryColumn column){
      this.draggingColumn = true;
      this.dragColumn = column;
      if (this.dragColumn != null) {
         this.dragColumn.setDragging(true);
      }

      this.dragStartX = mouseX;
      this.dragStartY = mouseY;
      this.columnOffsetX = this.configPanel;
      this.columnOffsetY = this.configPanelY;
   }

   private void dragColumn(double mouseX, double mouseY){
      float var5 = (float)(mouseX - this.dragStartX);
      float var6 = (float)(mouseY - this.dragStartY);
      float var7 = this.columnOffsetX + var5;
      float var8 = this.columnOffsetY + var6;
      float height = 0.0F;

      for (int index = 0; index < this.columns.size(); index++) {
         height = Math.max(height, this.columns.get(index).getHeight());
      }

      float get = this.lastSelectedColumn.get() * 219.0F;
      float columnsWidth = this.getColumnsWidth() + 32.0F + get;
      float var12 = height + 84.0F;
      float worldMinX = this.getWorldMinX();
      float worldMinY = this.getWorldMinY();
      float worldMaxX = Math.max(worldMinX, this.getWorldMaxX() - columnsWidth);
      float worldMaxY = Math.max(worldMinY, this.getWorldMaxY() - var12);
      this.configPanel = Math.min(Math.max(var7, worldMinX), worldMaxX);
      this.configPanelY = Math.min(Math.max(var8, worldMinY), worldMaxY);
   }

   private void endColumnDrag(){
      this.draggingColumn = false;
      if (this.dragColumn != null) {
         this.dragColumn.setDragging(false);
         this.dragColumn = null;
      }
   }

   private boolean isInConfigHeader(double mouseX, double mouseY){
      return mouseX >= this.config.getX() && mouseX <= this.config.getX() + 205.0F && mouseY >= this.config.getY() && mouseY <= this.config.getY() + 30.0F;
   }

   private void beginConfigDrag(double mouseX, double mouseY){
      this.configDragging = true;
      this.configDragged = true;
      this.configX = this.config.getX();
      this.configY = this.config.getY();
      this.configDragStartX = mouseX - this.configX;
      this.configDragStartY = mouseY - this.configY;
   }

   private void dragConfig(double mouseX, double mouseY){
      if (this.configDragging) {
         this.setConfigPos((float)(mouseX - this.configDragStartX), (float)(mouseY - this.configDragStartY));
      }
   }

   private void setConfigPos(float x, float y){
      this.configX = clamp(x, 4.0F, Math.max(4.0F, this.width - 205.0F - 4.0F));
      this.configY = clamp(y, 4.0F, Math.max(4.0F, this.height - this.config.getHeight() - 4.0F));
      this.config.setPosition(this.configX, this.configY);
   }

   private static float clamp(float value, float min, float max){
      return Math.max(min, Math.min(max, value));
   }
}

