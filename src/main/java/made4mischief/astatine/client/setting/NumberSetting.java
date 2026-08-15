package made4mischief.astatine.client.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NumberSetting extends Setting {
   private double value;
   private final double min;
   private final double max;
   private final double step;

   public NumberSetting(String name, double defaultValue, double min, double max, double step){
      super(name);
      this.min = min;
      this.max = max;
      this.step = step;
      this.value = this.snapToStep(defaultValue);
   }

   public double getValue(){
      return this.value;
   }

   public int getValueInt(){
      return (int)Math.round(this.value);
   }

   public float getValueFloat(){
      return (float)this.value;
   }

   public void setValue(double value){
      this.value = this.snapToStep(value);
   }

   public double getMin(){
      return this.min;
   }

   public double getMax(){
      return this.max;
   }

   public double getStep(){
      return this.step;
   }

   private double snapToStep(double input){
      double min = Math.max(this.min, Math.min(this.max, input));
      if (this.step > 0.0) {
         min = this.min + Math.round((min - this.min) / this.step) * this.step;
         min = Math.max(this.min, Math.min(this.max, min));
      }

      return min;
   }
}

