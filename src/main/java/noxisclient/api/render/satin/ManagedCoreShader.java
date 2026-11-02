package noxisclient.api.render.satin;

import net.minecraft.client.gl.ShaderProgram;
import noxisclient.api.render.satin.uniform.UniformFinder;

public interface ManagedCoreShader extends UniformFinder {
   ShaderProgram getProgram();

   void release();
}
