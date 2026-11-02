package noxisclient.license;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

final class GateDialog {
   private GateDialog() {
   }

   static String requestKey(String message) {
      if (GraphicsEnvironment.isHeadless()) {
         return null;
      } else {
         AtomicReference<String> result = new AtomicReference<>();
         Runnable show = () -> result.set((String)JOptionPane.showInputDialog(null, message, "Noxis Authorization", -1, null, null, ""));

         try {
            if (SwingUtilities.isEventDispatchThread()) {
               show.run();
            } else {
               SwingUtilities.invokeAndWait(show);
            }

            return result.get();
         } catch (InterruptedException var4) {
            Thread.currentThread().interrupt();
            return null;
         } catch (InvocationTargetException var5) {
            return null;
         }
      }
   }
}
