package made4mischief.astatine.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import made4mischief.astatine.client.gui.HoverTooltip;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.theme.ThemeManager;
import made4mischief.astatine.client.setting.EntityTargetSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;

@Environment(EnvType.CLIENT)
public class EntityTargetSelectorScreen extends Screen {
   private static final int MAX_WIDTH = 430;
   private static final int MAX_HEIGHT = 310;
   private static final int TITLE_Y = 12;
   private static final int SIDE_PADDING = 16;
   private static final int HEADER_HEIGHT = 42;
   private static final int SEARCH_HEIGHT = 20;
   private static final int SEARCH_GAP = 10;
   private static final int FOOTER_HEIGHT = 38;
   private static final int CELL_SIZE = 44;
   private static final int CELL_HIT_SIZE = 44;
   private static final int CELL_GAP = 6;
   private static final int MIN_COLUMNS = 1;
   private static final int BUTTON_WIDTH = 72;
   private static final int BUTTON_HEIGHT = 20;
   private static final int BUTTON_GAP = 8;
   private static final float BUTTON_TEXT_SCALE = 0.8F;
   private static final float BUTTON_TEXT_OFFSET = 1.5F;
   private final Screen parentScreen;
   private final EntityTargetSetting setting;
   private final Set<EntityType<?>> selectedTypes = new LinkedHashSet<>();
   private final List<EntityTargetSelectorScreen.EntityEntry> allEntries = new ArrayList<>();
   private final List<EntityTargetSelectorScreen.EntityEntry> filteredEntries = new ArrayList<>();
   private final HoverTooltip hoverTooltip = new HoverTooltip();
   private TextFieldWidget searchField;
   private int panelX;
   private int panelY;
   private int panelWidth;
   private int panelHeight;
   private int listX;
   private int listY;
   private int listWidth;
   private int listHeight;
   private int columnsPerPage;
   private int pageInde;

   public EntityTargetSelectorScreen(Screen parentScreen, EntityTargetSetting setting){
      super(Text.literal(setting.getName()));
      this.parentScreen = parentScreen;
      this.setting = setting;
      this.selectedTypes.addAll(setting.getSelectedTypes());
      this.populatePresets();
   }

   protected void init(){
      this.panelWidth = Math.min(430, this.width - 24);
      this.panelHeight = Math.min(310, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int var1 = this.panelX + 16;
      int var2 = this.panelY + 42;
      int var3 = this.panelWidth - 32;
      this.searchField = new TextFieldWidget(this.textRenderer, var1, var2, var3, 20, Text.literal("Search entities"));
      this.searchField.setMaxLength(64);
      this.searchField.setPlaceholder(Text.literal("Search entities..."));
      this.searchField.setChangedListener(this::filterEntities);
      this.addDrawableChild(this.searchField);
      this.setInitialFocus(this.searchField);
      this.listX = this.panelX + 16;
      this.listY = var2 + 20 + 10;
      this.listWidth = this.panelWidth - 32;
      this.listHeight = Math.max(44, this.panelY + this.panelHeight - 38 - this.listY);
      this.columnsPerPage = Math.max(1, (this.listWidth + 6) / 50);
      this.filterEntities("");
   }

   public boolean shouldPause(){
      return false;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta){
      Theme theme = ThemeManager.active();
      context.fill(0, 0, this.width, this.height, -1476395008);
      RenderUtil.drawBo(context, this.panelX - 1, this.panelY - 1, this.panelWidth + 2, this.panelHeight + 2, 13, theme.accent());
      RenderUtil.drawBo(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight, 12, theme.surface());
      RenderUtil.drawText(context, this.setting.getName().toUpperCase(Locale.ROOT), this.panelX + 16, this.panelY + 12, theme.accent(), true, 0.95F);
      String size = this.selectedTypes.size() + " selected";
      float textWidth = RenderUtil.getTextWidth(size, 0.8F);
      RenderUtil.drawText(context, size, this.panelX + this.panelWidth - 16 - textWidth, this.panelY + 13, theme.textDim(), false, 0.8F);
      this.renderPanelBorder(context, mouseX, mouseY, theme);
      this.renderPagination(context, mouseX, mouseY, theme);
      super.render(context, mouseX, mouseY, delta);
      this.hoverTooltip.render(context, this.getEntryAt(mouseX, mouseY), mouseX, mouseY, this.width, this.height, theme);
   }

   private void renderPanelBorder(DrawContext context, int mouseX, int mouseY, Theme theme){
      context.enableScissor(this.listX - 1, this.listY - 1, this.listX + this.listWidth + 1, this.listY + this.listHeight + 1);
      int columnsPerPage = this.getColumnsPerPage();
      int var6 = this.pageInde * this.columnsPerPage;
      int size = Math.min(this.filteredEntries.size(), (this.pageInde + columnsPerPage) * this.columnsPerPage);

      for (int index = var6; index < size; index++) {
         int var9 = index - var6;
         int var10 = var9 % this.columnsPerPage;
         int var11 = var9 / this.columnsPerPage;
         int var12 = this.listX + var10 * 50;
         int var13 = this.listY + var11 * 50;
         this.renderEntityCell(context, this.filteredEntries.get(index), var12, var13, mouseX, mouseY, theme);
      }

      context.disableScissor();
      if (this.filteredEntries.isEmpty()) {
         RenderUtil.drawCenteredText(
            context,
            "No entities found",
            this.panelX + this.panelWidth / 2.0F,
            this.listY + this.listHeight / 2.0F - RenderUtil.getTextHeight() / 2.0F,
            theme.textDim(),
            false,
            0.9F
         );
      }
   }

   private void renderEntityCell(DrawContext context, EntityTargetSelectorScreen.EntityEntry entry, int cellX, int cellY, int mouseX, int mouseY, Theme theme){
      boolean entityType = this.selectedTypes.contains(entry.entityType());
      boolean pointInRect = this.isPointInRect(mouseX, mouseY, cellX, cellY, 44, 44);
      int border = entityType ? theme.accent() : ColorUtil.scaleAlpha(theme.border(), pointInRect ? 1.0F : 0.7F);
      int surfaceElevated = pointInRect ? theme.surfaceElevated() : ColorUtil.scaleAlpha(theme.surfaceElevated(), 0.72F);
      RenderUtil.drawBo(context, cellX - 1, cellY - 1, 46, 46, 7, border);
      RenderUtil.drawBo(context, cellX, cellY, 44, 44, 6, surfaceElevated);
      context.drawItem(entry.icon(), cellX + 14, cellY + 14);
      if (entityType) {
         RenderUtil.drawBo(context, cellX + 44 - 10, cellY + 4, 6, 6, 3, theme.accent());
      }
   }

   private void renderPagination(DrawContext context, int mouseX, int mouseY, Theme theme){
      int var5 = this.panelY + this.panelHeight - 38;
      RenderUtil.drawBo(context, this.panelX + 16, var5, this.panelWidth - 32, 1, 0, theme.border());
      int var6 = this.panelX + this.panelWidth - 16 - 72;
      int var7 = var6 - 8 - 72;
      int var8 = var5 + 10;
      this.renderPageButton(context, "Cancel", var7, var8, this.isPointInRect(mouseX, mouseY, var7, var8, 72, 20), false, theme);
      this.renderPageButton(context, "Save", var6, var8, this.isPointInRect(mouseX, mouseY, var6, var8, 72, 20), true, theme);
   }

   private void renderPageButton(DrawContext context, String label, int x, int y, boolean hovered, boolean primary, Theme theme){
      int border = primary ? theme.accent() : theme.border();
      int accent;
      if (primary) {
         accent = hovered ? theme.accent() : ColorUtil.lerp(theme.surfaceElevated(), theme.accent(), 0.35F);
      } else {
         accent = hovered ? theme.surfaceElevated() : theme.surface();
      }

      RenderUtil.drawBo(context, x - 1, y - 1, 74, 22, 7, border);
      RenderUtil.drawBo(context, x, y, 72, 20, 6, accent);
      float var10 = x + 36.0F - 1.5F;
      float textHeight = y + (20.0F - RenderUtil.getTextHeight(0.8F)) / 2.0F - 1.5F;
      RenderUtil.drawCenteredText(context, label, var10, textHeight, primary && hovered ? -15723495 : theme.text(), true, 0.8F);
   }

   public boolean mouseClicked(Click click, boolean doubled){
      if (super.mouseClicked(click, doubled)) {
         return true;
      } else if (click.button() != 0) {
         return false;
      } else {
         int var3 = this.panelX + this.panelWidth - 16 - 72;
         int var4 = var3 - 8 - 72;
         int var5 = this.panelY + this.panelHeight - 38 + 10;
         if (this.isPointInRect(click.x(), click.y(), var3, var5, 72, 20)) {
            this.setting.setSelectedTypes(this.selectedTypes);
            this.applySelection();
            return true;
         } else if (this.isPointInRect(click.x(), click.y(), var4, var5, 72, 20)) {
            this.applySelection();
            return true;
         } else {
            int comp_4799 = this.getCellIndexAt(click.x(), click.y());
            if (comp_4799 >= 0 && comp_4799 < this.filteredEntries.size()) {
               EntityType entityType = this.filteredEntries.get(comp_4799).entityType();
               if (!this.selectedTypes.add(entityType)) {
                  this.selectedTypes.remove(entityType);
               }

               return true;
            } else {
               return false;
            }
         }
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical){
      if (!this.isPointInRect(mouseX, mouseY, this.listX, this.listY, this.listWidth, this.listHeight)) {
         return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
      } else {
         int pageCount = this.getPageCount();
         if (vertical < 0.0) {
            this.pageInde = Math.min(pageCount, this.pageInde + 1);
         } else if (vertical > 0.0) {
            this.pageInde = Math.max(0, this.pageInde - 1);
         }

         return true;
      }
   }

   public void method_25419(){
      this.applySelection();
   }

   private void applySelection(){
      if (this.client != null) {
         Object var1 = this.parentScreen instanceof ClickGuiScreen ? new ClickGuiScreen() : this.parentScreen;
         this.client.setScreen((Screen)var1);
      }
   }

   private void populatePresets(){
      this.allEntries.add(new EntityTargetSelectorScreen.EntityEntry(EntityType.PLAYER, "Player", new ItemStack(Items.PLAYER_HEAD)));

      for (EntityType entityType : Registries.ENTITY_TYPE) {
         if (entityType != EntityType.PLAYER) {
            SpawnEggItem spawnEggItem = SpawnEggItem.forEntity(entityType);
            if (spawnEggItem != null) {
               this.allEntries.add(new EntityTargetSelectorScreen.EntityEntry(entityType, entityType.getName().getString(), new ItemStack(spawnEggItem)));
            }
         }
      }

      this.allEntries.sort(Comparator.comparing(EntityTargetSelectorScreen.EntityEntry::displayName, String.CASE_INSENSITIVE_ORDER));
   }

   private void filterEntities(String searchText){
      String toLowerCase = searchText.trim().toLowerCase(Locale.ROOT);
      this.filteredEntries.clear();

      for (EntityTargetSelectorScreen.EntityEntry entityEntry : this.allEntries) {
         if (entityEntry.displayName().toLowerCase(Locale.ROOT).contains(toLowerCase)) {
            this.filteredEntries.add(entityEntry);
         }
      }

      this.pageInde = 0;
   }

   private int getCellIndexAt(double mouseX, double mouseY){
      if (!this.isPointInRect(mouseX, mouseY, this.listX, this.listY, this.listWidth, this.listHeight)) {
         return -1;
      } else {
         int var5 = (int)mouseX - this.listX;
         int var6 = (int)mouseY - this.listY;
         int var7 = var5 / 50;
         int var8 = var6 / 50;
         return var7 < this.columnsPerPage && var5 % 50 < 44 && var6 % 50 < 44 ? (this.pageInde + var8) * this.columnsPerPage + var7 : -1;
      }
   }

   private String getEntryAt(double mouseX, double mouseY){
      int cellIndexAt = this.getCellIndexAt(mouseX, mouseY);
      return cellIndexAt >= 0 && cellIndexAt < this.filteredEntries.size() ? this.filteredEntries.get(cellIndexAt).displayName() : null;
   }

   private int getColumnsPerPage(){
      return Math.max(1, (this.listHeight + 6) / 50);
   }

   private int getPageCount(){
      int size = (this.filteredEntries.size() + this.columnsPerPage - 1) / this.columnsPerPage;
      return Math.max(0, size - this.getColumnsPerPage());
   }

   private boolean isPointInRect(double pointX, double pointY, int x, int y, int width, int height){
      return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
   }

   @Environment(EnvType.CLIENT)
   private record EntityEntry(EntityType<?> entityType, String displayName, ItemStack icon){
   }
}

