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
import made4mischief.astatine.client.setting.ItemTargetSetting;
import made4mischief.astatine.client.utils.render.RenderUtil;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;

@Environment(EnvType.CLIENT)
public final class ItemTargetSelectorScreen extends Screen {
   private static final int SCREEN_WIDTH = 500;
   private static final int SCREEN_HEIGHT = 340;
   private static final int TITLE_Y = 12;
   private static final int SIDE_PADDING = 16;
   private static final int HEADER_HEIGHT = 42;
   private static final int SEARCH_HEIGHT = 20;
   private static final int SEARCH_GAP = 10;
   private static final int FOOTER_HEIGHT = 38;
   private static final int CELL_SIZE = 40;
   private static final int CELL_GAP = 6;
   private static final int MIN_COLUMNS = 1;
   private static final int BUTTON_WIDTH = 72;
   private static final int BUTTON_HEIGHT = 20;
   private static final float BUTTON_TEXT_SCALE = 0.8F;
   private static final float BUTTON_TEXT_OFFSET = 1.5F;
   private final Screen parentScreen;
   private final ItemTargetSetting setting;
   private final Set<Item> selectedItems = new LinkedHashSet<>();
   private final List<ItemTargetSelectorScreen.ItemEntry> allItems = new ArrayList<>();
   private final List<ItemTargetSelectorScreen.ItemEntry> filteredItems = new ArrayList<>();
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

   public ItemTargetSelectorScreen(Screen parentScreen, ItemTargetSetting setting){
      super(Text.literal(setting.getName()));
      this.parentScreen = parentScreen;
      this.setting = setting;
      this.selectedItems.addAll(setting.getSelectedItems());
      this.populateItems();
   }

   protected void init(){
      this.panelWidth = Math.min(500, this.width - 24);
      this.panelHeight = Math.min(340, this.height - 24);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int var1 = this.panelX + 16;
      int var2 = this.panelY + 42;
      this.searchField = new TextFieldWidget(this.textRenderer, var1, var2, this.panelWidth - 32, 20, Text.literal("Search items"));
      this.searchField.setMaxLength(80);
      this.searchField.setPlaceholder(Text.literal("Search items..."));
      this.searchField.setChangedListener(this::filterItems);
      this.addDrawableChild(this.searchField);
      this.setInitialFocus(this.searchField);
      this.listX = this.panelX + 16;
      this.listY = var2 + 20 + 10;
      this.listWidth = this.panelWidth - 32;
      this.listHeight = Math.max(40, this.panelY + this.panelHeight - 38 - this.listY);
      this.columnsPerPage = Math.max(1, (this.listWidth + 6) / 46);
      this.filterItems("");
   }

   public boolean shouldPause(){
      return false;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta){
      Theme theme = ThemeManager.active();
      context.fill(0, 0, this.width, this.height, -1476395008);
      RenderUtil.drawBo(context, this.panelX - 1, this.panelY - 1, this.panelWidth + 2, this.panelHeight + 2, 13, theme.accent());
      RenderUtil.drawBo(context, this.panelX, this.panelY, this.panelWidth, this.panelHeight, 12, theme.surface());
      String toUpperCase = this.setting.getName().toUpperCase();
      RenderUtil.drawText(context, toUpperCase, this.panelX + 16, this.panelY + 12, theme.accent(), true, 0.95F);
      String size = this.selectedItems.size() + " selected";
      RenderUtil.drawText(
         context, size, this.panelX + this.panelWidth - 16 - RenderUtil.getTextWidth(size, 0.8F), this.panelY + 13, theme.textDim(), false, 0.8F
      );
      this.renderPanelBorder(context, mouseX, mouseY, theme);
      this.renderPagination(context, mouseX, mouseY, theme);
      super.render(context, mouseX, mouseY, delta);
      this.hoverTooltip.render(context, this.getEntryAt(mouseX, mouseY), mouseX, mouseY, this.width, this.height, theme);
   }

   private void renderPanelBorder(DrawContext context, int mouseX, int mouseY, Theme theme){
      context.enableScissor(this.listX - 1, this.listY - 1, this.listX + this.listWidth + 1, this.listY + this.listHeight + 1);
      int var5 = this.pageInde * this.columnsPerPage;
      int columnsPerPage = Math.min(this.filteredItems.size(), (this.pageInde + this.getColumnsPerPage()) * this.columnsPerPage);

      for (int index = var5; index < columnsPerPage; index++) {
         int var8 = index - var5;
         int var9 = this.listX + var8 % this.columnsPerPage * 46;
         int var10 = this.listY + var8 / this.columnsPerPage * 46;
         ItemTargetSelectorScreen.ItemEntry var11 = this.filteredItems.get(index);
         boolean item = this.selectedItems.contains(var11.item());
         boolean pointInRect = this.isPointInRect(mouseX, mouseY, var9, var10, 40, 40);
         int border = item ? theme.accent() : ColorUtil.scaleAlpha(theme.border(), pointInRect ? 1.0F : 0.7F);
         int surfaceElevated = pointInRect ? theme.surfaceElevated() : ColorUtil.scaleAlpha(theme.surfaceElevated(), 0.72F);
         RenderUtil.drawBo(context, var9 - 1, var10 - 1, 42, 42, 7, border);
         RenderUtil.drawBo(context, var9, var10, 40, 40, 6, surfaceElevated);
         context.drawItem(var11.icon(), var9 + 12, var10 + 12);
         if (item) {
            RenderUtil.drawBo(context, var9 + 40 - 9, var10 + 3, 6, 6, 3, theme.accent());
         }
      }

      context.disableScissor();
      if (this.filteredItems.isEmpty()) {
         RenderUtil.drawCenteredText(
            context, "No items found", this.panelX + this.panelWidth / 2.0F, this.listY + this.listHeight / 2.0F, theme.textDim(), false, 0.9F
         );
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
      int surface = primary
         ? (hovered ? theme.accent() : ColorUtil.lerp(theme.surfaceElevated(), theme.accent(), 0.35F))
         : (hovered ? theme.surfaceElevated() : theme.surface());
      RenderUtil.drawBo(context, x - 1, y - 1, 74, 22, 7, border);
      RenderUtil.drawBo(context, x, y, 72, 20, 6, surface);
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
         int var3 = this.panelY + this.panelHeight - 38;
         int var4 = this.panelX + this.panelWidth - 16 - 72;
         int var5 = var4 - 8 - 72;
         int var6 = var3 + 10;
         if (this.isPointInRect(click.x(), click.y(), var4, var6, 72, 20)) {
            this.setting.setSelectedItems(this.selectedItems);
            this.applySelection();
            return true;
         } else if (this.isPointInRect(click.x(), click.y(), var5, var6, 72, 20)) {
            this.applySelection();
            return true;
         } else {
            int comp_4799 = this.getCellIndexAt(click.x(), click.y());
            if (comp_4799 >= 0 && comp_4799 < this.filteredItems.size()) {
               Item item = this.filteredItems.get(comp_4799).item();
               if (this.selectedItems.contains(item)) {
                  this.selectedItems.remove(item);
               } else {
                  if (this.setting.getMaximumSelections() == 1) {
                     this.selectedItems.clear();
                  } else if (this.selectedItems.size() >= this.setting.getMaximumSelections()) {
                     return true;
                  }

                  this.selectedItems.add(item);
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
         if (vertical < 0.0) {
            this.pageInde = Math.min(this.getPageCount(), this.pageInde + 1);
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
         this.client.setScreen((Screen)(this.parentScreen instanceof ClickGuiScreen ? new ClickGuiScreen() : this.parentScreen));
      }
   }

   private void populateItems(){
      for (Item item : Registries.ITEM) {
         if (item != Items.AIR) {
            String string = item.getName().getString();
            String toString = Registries.ITEM.getId(item).toString();
            this.allItems.add(new ItemTargetSelectorScreen.ItemEntry(item, string, toString, new ItemStack(item)));
         }
      }

      this.allItems.sort(Comparator.comparing(ItemTargetSelectorScreen.ItemEntry::displayName, String.CASE_INSENSITIVE_ORDER));
   }

   private void filterItems(String query){
      String toLowerCase = query.trim().toLowerCase(Locale.ROOT);
      this.filteredItems.clear();

      for (ItemTargetSelectorScreen.ItemEntry itemEntry : this.allItems) {
         if (itemEntry.displayName().toLowerCase(Locale.ROOT).contains(toLowerCase) || itemEntry.identifier().toLowerCase(Locale.ROOT).contains(toLowerCase)) {
            this.filteredItems.add(itemEntry);
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
         int var7 = var5 / 46;
         int var8 = var6 / 46;
         return var7 < this.columnsPerPage && var5 % 46 < 40 && var6 % 46 < 40 ? (this.pageInde + var8) * this.columnsPerPage + var7 : -1;
      }
   }

   private String getEntryAt(double mouseX, double mouseY){
      int cellIndexAt = this.getCellIndexAt(mouseX, mouseY);
      return cellIndexAt >= 0 && cellIndexAt < this.filteredItems.size() ? this.filteredItems.get(cellIndexAt).displayName() : null;
   }

   private int getColumnsPerPage(){
      return Math.max(1, (this.listHeight + 6) / 46);
   }

   private int getPageCount(){
      int size = (this.filteredItems.size() + this.columnsPerPage - 1) / this.columnsPerPage;
      return Math.max(0, size - this.getColumnsPerPage());
   }

   private boolean isPointInRect(double p, double py, int x, int y, int w, int h){
      return p >= x && p <= x + w && py >= y && py <= y + h;
   }

   @Environment(EnvType.CLIENT)
   private record ItemEntry(Item item, String displayName, String identifier, ItemStack icon){
   }
}

