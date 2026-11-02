package noxisclient.api.events.eventbus;

import java.util.function.Consumer;

final class DirectListener<T> implements IListener {
   private final Class<T> target;
   private final int priority;
   private final boolean isStatic;
   private final Consumer<T> executor;

   DirectListener(Class<T> target, int priority, boolean isStatic, Consumer<T> executor) {
      this.target = target;
      this.priority = priority;
      this.isStatic = isStatic;
      this.executor = executor;
   }

   @Override
   public void call(Object event) {
      this.executor.accept(this.target.cast(event));
   }

   @Override
   public Class<?> getTarget() {
      return this.target;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public boolean isStatic() {
      return this.isStatic;
   }
}
