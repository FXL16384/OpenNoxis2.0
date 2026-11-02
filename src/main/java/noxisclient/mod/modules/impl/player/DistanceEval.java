package noxisclient.mod.modules.impl.player;

class DistanceEval {
   final String mode;
   final double distanceValue;
   final double left;
   final double right;
   final boolean pass;
   final String reason;

   DistanceEval(String mode, double distanceValue, double left, double right, boolean pass, String reason) {
      this.mode = mode;
      this.distanceValue = distanceValue;
      this.left = left;
      this.right = right;
      this.pass = pass;
      this.reason = reason;
   }
}
