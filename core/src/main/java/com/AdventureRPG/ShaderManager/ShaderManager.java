// com.AdventureRPG.ShaderManager.ShaderManager
package com.AdventureRPG.ShaderManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.MaterialManager.MaterialManager;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import java.util.*;

public class ShaderManager implements ShaderProvider {

    private final MaterialManager materialManager;
    private final ShaderProvider fallback;
    // One pool per ShaderProgram; each pool keeps multiple DefaultShaders for
    // differing attribute combos.
    private final IdentityHashMap<ShaderProgram, List<Shader>> pools = new IdentityHashMap<>();

    public ShaderManager(GameManager gameManager) {
        this(gameManager.materialManager, new DefaultShaderProvider());
    }

    public ShaderManager(MaterialManager materialManager, ShaderProvider fallback) {
        this.materialManager = materialManager;
        this.fallback = fallback;
    }

    @Override
    public Shader getShader(Renderable renderable) {
        ShaderProgram prog = materialManager.getShaderForMaterial(renderable.material);
        if (prog == null) {
            return fallback.getShader(renderable); // use LibGDX default shader
        }

        List<Shader> list = pools.computeIfAbsent(prog, k -> new ArrayList<>());

        // Reuse a shader instance that canRender this renderable (matches
        // attributes/bones/etc.)
        for (Shader s : list) {
            if (s.canRender(renderable))
                return s;
        }

        // Otherwise, build a new DefaultShader bound to this ShaderProgram for this
        // attribute combination
        DefaultShader.Config cfg = new DefaultShader.Config();
        DefaultShader s = new DefaultShader(renderable, cfg, prog);
        s.init();
        list.add(s);
        return s;
    }

    @Override
    public void dispose() {
        for (List<Shader> list : pools.values()) {
            for (Shader s : list)
                s.dispose();
        }
        pools.clear();
        fallback.dispose();
    }
}
