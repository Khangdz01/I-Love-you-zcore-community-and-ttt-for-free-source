package made4mischief.astatine.client.gui.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import made4mischief.astatine.client.gui.component.event.MouseClickEvent;
import made4mischief.astatine.client.gui.component.event.MouseMoveEvent;
import made4mischief.astatine.client.gui.component.event.MouseReleaseEvent;
import made4mischief.astatine.client.gui.component.event.MouseScrollEvent;
import made4mischief.astatine.client.gui.component.theme.Theme;
import made4mischief.astatine.client.gui.component.widget.ModuleRow;
import made4mischief.astatine.client.modules.Module;
import made4mischief.astatine.client.utils.render.animation.Animation;
import made4mischief.astatine.client.utils.render.animation.AnimationType;
import made4mischief.astatine.client.utils.render.core.ColorUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CategoryColumn extends AbstractComponent {
   public static final float WIDTH = 220.0F;
   private static final float HEADER_HEIGHT = 35.0F;
   private static final float HEADER_GAP = 8.0F;
   private static final float PANEL_RADIUS = 16.0F;
   private static final float HEADER_RADIUS = 12.0F;
   private static final float ROW_X_INSET = 10.0F;
   private static final float ROW_HEIGHT = 40.0F;
   private static final float ROW_GAP = 6.0F;
   private static final int VISIBLE_ROW_COUNT = 9;
   private static final float ROW_STEP = 46.0F;
   private static final float LIST_TOP_INSET = 30.0F;
   private static final float COLUMN_HEIGHT = 468.0F;
   private final String title;
   private final List<ModuleRow> rows = new ArrayList<>();
   private final List<ModuleRow> unmodifiableRows = Collections.unmodifiableList(this.rows);
   private final Animation hoverAnimation = new Animation(0.0F, 1.0F, 220L, AnimationType.EASE_OUT);
   private boolean hovered;
   private final Animation activeAnimation = new Animation(0.0F, 1.0F, 300L, AnimationType.EASE_OUT);
   private boolean active;
   private static final long SCROLL_DURATION = 150L;
   private static final float SCROLL_STEP = 46.0F;
   private final Animation scrollOffset = new Animation(0.0F, 0.0F, 150L, AnimationType.EASE_OUT);
   private float scrollTarget;
   private boolean dragging;
   private Consumer<ModuleRow> onConfigRequest;

   public CategoryColumn(String title, float x, float y){
      super(x, y, 220.0F, 35.0F);
      this.title = title;
   }

   public String getTitle(){
      return this.title;
   }

   public void addModule(Module module){
      if (module != null) {
         ModuleRow moduleRow = new ModuleRow(module, this.getX() + 10.0F, 0.0F, 200.0F, 40.0F);
         moduleRow.setOnConfigRequest(this.onConfigRequest);
         moduleRow.setParent(this);
         this.rows.add(moduleRow);
         this.layoutModules();
      }
   }

   public List<ModuleRow> getRows(){
      return this.unmodifiableRows;
   }

   public void setOnConfigRequest(Consumer<ModuleRow> onConfigRequest){
      this.onConfigRequest = onConfigRequest;

      for (int index = 0; index < this.rows.size(); index++) {
         this.rows.get(index).setOnConfigRequest(onConfigRequest);
      }
   }

   public boolean isActive(){
      return this.active;
   }

   public void setActive(boolean active){
      if (this.active != active) {
         this.active = active;
         this.activeAnimation.setTarget(active ? 1.0F : 0.0F);
      }
   }

   public void setDragging(boolean dragging){
      this.dragging = dragging;
   }

   public boolean isWithinTab(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + 220.0F && pointY >= this.getY() && pointY <= this.getY() + 35.0F;
   }

   @Override
   public float getHeight(){
      return 43.0F + this.getColumnWidth();
   }

   private float getListStartY(){
      return this.getY() + 35.0F + 8.0F;
   }

   public float getPanelTop(){
      return this.getListStartY();
   }

   private float getColumnWidth(){
      return 468.0F;
   }

   public float getPanelHeight(){
      return this.getColumnWidth();
   }

   private int getVisibleRowCount(){
      int index2 = 0;

      for (int index = 0; index < this.rows.size(); index++) {
         if (this.rows.get(index).isVisible()) {
            index2++;
         }
      }

      return index2;
   }

   public int filter(String query){
      String trim = query == null ? "" : query.toLowerCase().trim();
      int index2 = 0;

      for (int index = 0; index < this.rows.size(); index++) {
         ModuleRow moduleRow = this.rows.get(index);
         boolean contains = trim.isEmpty() || moduleRow.getModule().getName().toLowerCase().contains(trim);
         moduleRow.setVisible(contains);
         if (contains) {
            index2++;
         }
      }

      this.scrollTarget = 0.0F;
      this.scrollOffset.snapTo(0.0F);
      this.layoutModules();
      return index2;
   }

   private void layoutModules(){
      float x = this.getX() + 10.0F;
      float var2 = 200.0F;
      float scrollOffset = this.getScrollOffset();
      int index2 = 0;

      for (int index = 0; index < this.rows.size(); index++) {
         ModuleRow moduleRow = this.rows.get(index);
         if (moduleRow.isVisible()) {
            float listStartY = this.getListStartY() + 30.0F + index2 * 46.0F - scrollOffset;
            moduleRow.setPosition(x, listStartY);
            moduleRow.setSize(var2, 40.0F);
            index2++;
         }
      }
   }

   @Override
   public boolean isWithin(double pointX, double pointY){
      return pointX >= this.getX() && pointX <= this.getX() + 220.0F && pointY >= this.getY() && pointY <= this.getY() + this.getHeight();
   }

   @Override
   public void render(GuiRenderContext context){
      if (this.isVisible()) {
         Theme theme = context.theme();
         this.layoutModules();
         float get = this.activeAnimation.get();
         this.renderModuleButtons(context, theme, get);
         this.renderModuleNames(context, theme, get);
      }
   }

   private void renderModuleButtons(GuiRenderContext context, Theme theme, float activeAmt){
      float x = this.getX();
      float y = this.getY();
      boolean mouseY = context.mouseX() >= x && context.mouseX() <= x + 220.0F && context.mouseY() >= y && context.mouseY() <= y + 35.0F;
      if (mouseY != this.hovered) {
         this.hoverAnimation.setTarget(mouseY ? 1.0F : 0.0F);
         this.hovered = mouseY;
      }

      float max = Math.max(activeAmt, this.dragging ? 1.0F : 0.0F);
      int accent = ColorUtil.scaleAlpha(theme.accent(), 0.28F + 0.37F * max);
      context.drawBo(x - 1.5F, y - 1.5F, 223.0F, 38.0F, 13.5F, accent);
      int surface = ColorUtil.scaleAlpha(theme.surface(), 0.55F);
      surface = ColorUtil.lerp(surface, theme.surfaceElevated(), this.hoverAnimation.get() * 0.6F);
      surface = ColorUtil.lerp(surface, theme.surfaceElevated(), activeAmt);
      if (this.dragging) {
         surface = ColorUtil.lerp(theme.surfaceElevated(), theme.accent(), 0.18F);
      }

      context.drawBo(x, y, 220.0F, 35.0F, 12.0F, surface);
      int get = ColorUtil.lerp(theme.textDim(), theme.accent(), Math.max(activeAmt, this.hoverAnimation.get() * 0.5F));
      float textHeight = y + (35.0F - context.textHeight()) / 2.0F;
      context.drawCenteredText(this.title.toUpperCase(), x + 110.0F, textHeight, get, false, 0.92F);
   }

   private void renderModuleNames(GuiRenderContext context, Theme theme, float activeAmt){
      float x = this.getX();
      float listStartY = this.getListStartY();
      float columnWidth = this.getColumnWidth();
      int accent = ColorUtil.scaleAlpha(theme.accent(), 0.28F + 0.2F * activeAmt);
      context.drawBo(x - 1.5F, listStartY - 1.5F, 223.0F, columnWidth + 3.0F, 17.5F, accent);
      context.drawBo(x, listStartY, 220.0F, columnWidth, 16.0F, theme.surface());
      int round2 = Math.round(listStartY + 30.0F);
      int round = Math.round(listStartY + columnWidth - 30.0F);
      context.enableScissor(Math.round(x + 10.0F), round2, Math.round(x + 220.0F - 10.0F), round);
      this.renderRows(context);
      context.disableScissor();
   }

   private void renderRows(GuiRenderContext context){
      float listStartY2 = this.getListStartY() + 30.0F;
      float listStartY = this.getListStartY() + 468.0F - 30.0F;

      for (int index = 0; index < this.rows.size(); index++) {
         ModuleRow moduleRow = this.rows.get(index);
         if (moduleRow.isVisible()) {
            float y = Math.min(moduleRow.getY() + 40.0F, listStartY) - Math.max(moduleRow.getY(), listStartY2);
            if (y > 0.0F) {
               float var7 = clamp01(y / 40.0F);
               float var8 = 1.0F - (1.0F - var7) * (1.0F - var7);
               float var9 = 0.76F + 0.24F * var8;
               this.renderRow(context, moduleRow, var9);
            }
         }
      }
   }

   private void renderRow(GuiRenderContext context, ModuleRow row, float scale){
      if (scale >= 0.999F) {
         row.render(context);
      } else {
         float width = row.getX() + row.getWidth() / 2.0F;
         float height = row.getY() + row.getHeight() / 2.0F;
         context.pushMatrix();
         context.translate(width, height);
         context.scale(scale, scale);
         context.translate(-width, -height);
         row.render(context);
         context.popMatrix();
      }
   }

   private float getScrollOffset(){
      return clamp(this.scrollOffset.get(), 0.0F, this.getMaxScroll());
   }

   private float getMaxScroll(){
      return Math.max(0.0F, (this.getVisibleRowCount() - 9) * 46.0F);
   }

   @Override
   public void tick(){
      for (int index = 0; index < this.rows.size(); index++) {
         this.rows.get(index).tick();
      }
   }

   @Override
   protected boolean onMouseClicked(MouseClickEvent event){
      if (!this.isWithinModuleViewport(event.x(), event.y())) {
         return false;
      } else {
         for (int index = 0; index < this.rows.size(); index++) {
            ModuleRow moduleRow = this.rows.get(index);
            if (moduleRow.isVisible() && this.isRowVisible(moduleRow) && moduleRow.mouseClicked(event)) {
               return true;
            }
         }

         return false;
      }
   }

   @Override
   protected boolean onMouseReleased(MouseReleaseEvent event){
      boolean var2 = false;

      for (int index = 0; index < this.rows.size(); index++) {
         ModuleRow moduleRow = this.rows.get(index);
         if (moduleRow.isVisible() && this.isRowVisible(moduleRow) && moduleRow.mouseReleased(event)) {
            var2 = true;
         }
      }

      return var2;
   }

   @Override
   protected boolean onMouseMoved(MouseMoveEvent event){
      for (int index = 0; index < this.rows.size(); index++) {
         this.rows.get(index).mouseMoved(event);
      }

      return false;
   }

   protected boolean onMouseScrolled(MouseScrollEvent event){
      float maxScroll = this.getMaxScroll();
      if (this.isPointInside(event.x(), event.y()) && event.vertical() != 0.0 && !(maxScroll <= 0.0F)) {
         event.consume(this);
         float vertical = clamp(this.scrollTarget - (float)event.vertical() * 46.0F, 0.0F, maxScroll);
         if (Math.abs(vertical - this.scrollTarget) > 0.001F) {
            this.scrollTarget = vertical;
            this.scrollOffset.setDuration(150L);
            this.scrollOffset.setTarget(this.scrollTarget);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isWithinModuleViewport(double pointX, double pointY){
      float listStartY2 = this.getListStartY() + 30.0F;
      float listStartY = this.getListStartY() + 468.0F - 30.0F;
      return pointX >= this.getX() + 10.0F && pointX <= this.getX() + 220.0F - 10.0F && pointY >= listStartY2 && pointY <= listStartY;
   }

   public boolean isRowUnderPointer(ModuleRow row, double pointX, double pointY){
      return row != null && row.isVisible() && this.isRowVisible(row) && this.isWithinModuleViewport(pointX, pointY) && row.isWithin(pointX, pointY);
   }

   private boolean isRowVisible(ModuleRow row){
      float listStartY2 = this.getListStartY() + 30.0F;
      float listStartY = this.getListStartY() + 468.0F - 30.0F;
      return row.getY() + row.getHeight() > listStartY2 && row.getY() < listStartY;
   }

   private boolean isPointInside(double pointX, double pointY){
      float listStartY = this.getListStartY();
      return pointX >= this.getX() && pointX <= this.getX() + 220.0F && pointY >= listStartY && pointY <= listStartY + 468.0F;
   }

   private static float clamp(float value, float minimum, float maximum){
      return Math.max(minimum, Math.min(maximum, value));
   }

   private static float clamp01(float value){
      return clamp(value, 0.0F, 1.0F);
   }
}

