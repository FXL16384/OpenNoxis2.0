package noxisclient.api.utils.math;

public enum Easing {
   Linear {
      @Override
      public double ease(double progress) {
         return progress;
      }
   },
   SineIn {
      @Override
      public double ease(double progress) {
         return 1.0 - Math.cos(progress * Math.PI / 2.0);
      }
   },
   SineOut {
      @Override
      public double ease(double progress) {
         return Math.sin(progress * Math.PI / 2.0);
      }
   },
   SineInOut {
      @Override
      public double ease(double progress) {
         return -(Math.cos(Math.PI * progress) - 1.0) / 2.0;
      }
   },
   CubicIn {
      @Override
      public double ease(double progress) {
         return Math.pow(progress, 3.0);
      }
   },
   CubicOut {
      @Override
      public double ease(double progress) {
         return 1.0 - Math.pow(1.0 - progress, 3.0);
      }
   },
   CubicInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5 ? 4.0 * Math.pow(progress, 3.0) : 1.0 - Math.pow(-2.0 * progress + 2.0, 3.0) / 2.0;
      }
   },
   QuadIn {
      @Override
      public double ease(double progress) {
         return Math.pow(progress, 2.0);
      }
   },
   QuadOut {
      @Override
      public double ease(double progress) {
         return 1.0 - (1.0 - progress) * (1.0 - progress);
      }
   },
   QuadInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5 ? 8.0 * Math.pow(progress, 4.0) : 1.0 - Math.pow(-2.0 * progress + 2.0, 4.0) / 2.0;
      }
   },
   QuartIn {
      @Override
      public double ease(double progress) {
         return Math.pow(progress, 4.0);
      }
   },
   QuartOut {
      @Override
      public double ease(double progress) {
         return 1.0 - Math.pow(1.0 - progress, 4.0);
      }
   },
   QuartInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5 ? 8.0 * Math.pow(progress, 4.0) : 1.0 - Math.pow(-2.0 * progress + 2.0, 4.0) / 2.0;
      }
   },
   QuintIn {
      @Override
      public double ease(double progress) {
         return Math.pow(progress, 5.0);
      }
   },
   QuintOut {
      @Override
      public double ease(double progress) {
         return 1.0 - Math.pow(1.0 - progress, 5.0);
      }
   },
   QuintInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5 ? 16.0 * Math.pow(progress, 5.0) : 1.0 - Math.pow(-2.0 * progress + 2.0, 5.0) / 2.0;
      }
   },
   CircIn {
      @Override
      public double ease(double progress) {
         return 1.0 - Math.sqrt(1.0 - Math.pow(progress, 2.0));
      }
   },
   CircOut {
      @Override
      public double ease(double progress) {
         return Math.sqrt(1.0 - Math.pow(progress - 1.0, 2.0));
      }
   },
   CircInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5
            ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * progress, 2.0))) / 2.0
            : (Math.sqrt(1.0 - Math.pow(-2.0 * progress + 2.0, 2.0)) + 1.0) / 2.0;
      }
   },
   ExpoIn {
      @Override
      public double ease(double progress) {
         return progress == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * progress - 10.0);
      }
   },
   ExpoOut {
      @Override
      public double ease(double progress) {
         return progress == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * progress);
      }
   },
   ExpoInOut {
      @Override
      public double ease(double progress) {
         return progress == 0.0
            ? 0.0
            : (progress == 1.0 ? 1.0 : (progress < 0.5 ? Math.pow(2.0, 20.0 * progress - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * progress + 10.0)) / 2.0));
      }
   },
   BackIn {
      @Override
      public double ease(double progress) {
         double c1 = 1.70158;
         double c3 = c1 + 1.0;
         return c3 * progress * progress * progress - c1 * progress * progress;
      }
   },
   BackOut {
      @Override
      public double ease(double progress) {
         double c1 = 1.70158;
         double c3 = c1 + 1.0;
         return 1.0 + c3 * Math.pow(progress - 1.0, 3.0) + c1 * Math.pow(progress - 1.0, 2.0);
      }
   },
   BackInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5
            ? Math.pow(2.0 * progress, 2.0) * (7.189819 * progress - 2.5949095) / 2.0
            : (Math.pow(2.0 * progress - 2.0, 2.0) * (3.5949095 * (progress * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
      }
   },
   BounceIn {
      @Override
      public double ease(double progress) {
         return 1.0 - this.bounceOut(1.0 - progress);
      }

      private double bounceOut(double progress) {
         if (progress < 0.36363636363636365) {
            return 7.5625 * progress * progress;
         } else if (progress < 0.7272727272727273) {
            double var5;
            return 7.5625 * (var5 = progress - 0.5454545454545454) * var5 + 0.75;
         } else {
            double var3;
            double var4;
            return progress < 0.9090909090909091
               ? 7.5625 * (var3 = progress - 0.8181818181818182) * var3 + 0.9375
               : 7.5625 * (var4 = progress - 0.9545454545454546) * var4 + 0.984375;
         }
      }
   },
   BounceOut {
      @Override
      public double ease(double progress) {
         if (progress < 0.36363636363636365) {
            return 7.5625 * progress * progress;
         } else if (progress < 0.7272727272727273) {
            double var5;
            return 7.5625 * (var5 = progress - 0.5454545454545454) * var5 + 0.75;
         } else {
            double var3;
            double var4;
            return progress < 0.9090909090909091
               ? 7.5625 * (var3 = progress - 0.8181818181818182) * var3 + 0.9375
               : 7.5625 * (var4 = progress - 0.9545454545454546) * var4 + 0.984375;
         }
      }
   },
   BounceInOut {
      @Override
      public double ease(double progress) {
         return progress < 0.5 ? (1.0 - this.bounceOut(1.0 - 2.0 * progress)) / 2.0 : (1.0 + this.bounceOut(2.0 * progress - 1.0)) / 2.0;
      }

      private double bounceOut(double progress) {
         if (progress < 0.36363636363636365) {
            return 7.5625 * progress * progress;
         } else if (progress < 0.7272727272727273) {
            double var5;
            return 7.5625 * (var5 = progress - 0.5454545454545454) * var5 + 0.75;
         } else {
            double var3;
            double var4;
            return progress < 0.9090909090909091
               ? 7.5625 * (var3 = progress - 0.8181818181818182) * var3 + 0.9375
               : 7.5625 * (var4 = progress - 0.9545454545454546) * var4 + 0.984375;
         }
      }
   },
   ElasticIn {
      @Override
      public double ease(double progress) {
         if (progress == 0.0) {
            return 0.0;
         } else {
            return progress == 1.0 ? 1.0 : -Math.pow(2.0, 10.0 * progress - 10.0) * Math.sin((progress * 10.0 - 10.75) * (Math.PI * 2) / 3.0);
         }
      }
   },
   ElasticOut {
      @Override
      public double ease(double progress) {
         if (progress == 0.0) {
            return 0.0;
         } else {
            return progress == 1.0 ? 1.0 : Math.pow(2.0, -10.0 * progress) * Math.sin((progress * 10.0 - 0.75) * (Math.PI * 2) / 3.0) + 1.0;
         }
      }
   },
   ElasticInOut {
      @Override
      public double ease(double progress) {
         if (progress == 0.0) {
            return 0.0;
         } else if (progress == 1.0) {
            return 1.0;
         } else {
            return progress < 0.5
               ? 0.5 * Math.pow(2.0, 20.0 * progress - 10.0) * Math.sin((20.0 * progress - 11.125) * (Math.PI * 2) / 4.5)
               : 0.5 * Math.pow(2.0, -20.0 * progress + 10.0) * Math.sin((20.0 * progress - 11.125) * (Math.PI * 2) / 4.5) + 1.0;
         }
      }
   };

   public abstract double ease(double var1);
}
