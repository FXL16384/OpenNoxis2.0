package noxisclient.api.utils.math;

public class Animation {
   private final FadeUtils fadeUtils = new FadeUtils(0L);
   public double from = 0.0;
   public double to = 0.0;

   public double get(double target, long length, Easing ease) {
      if (target != this.to) {
         if (this.from != this.to) {
            this.from = this.from + (this.to - this.from) * this.fadeUtils.ease(ease);
         } else {
            this.from = this.to;
         }

         this.to = target;
         this.fadeUtils.reset();
      }

      this.fadeUtils.setLength(length);
      double progress = this.fadeUtils.ease(ease);
      double result = this.from + (this.to - this.from) * progress;
      if (this.to > this.from) {
         result = Math.min(result, this.to);
      } else {
         result = Math.max(result, this.to);
      }

      return result;
   }
}
