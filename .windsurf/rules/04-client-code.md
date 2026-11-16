---
trigger: glob
globs: src/client/**/*.java
---

# Client-Side Code Rules (1.21.10)

## HUD Rendering (NEW API)
```java
// ✅ CORRECT - HudElementRegistry (1.21+)
HudElementRegistry.attachElementBefore(
    VanillaHudElements.HOTBAR,
    Identifier.of("manaborn", "my_hud"),
    (context, tickCounter) -> {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden) return;
        
        float tickDelta = tickCounter.getTickDelta(false);
        MyHudOverlay.render(context, tickDelta);
    }
);

// ❌ WRONG - HudRenderCallback (deprecated in 1.21)
HudRenderCallback.EVENT.register((context, tickDelta) -> {});
```

## HUD Imports
```java
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.DrawContext;
```

## DrawContext Methods
```java
context.fill(x1, y1, x2, y2, color);
context.drawBorder(x, y, width, height, color);
context.drawText(textRenderer, text, x, y, color, shadow);
context.drawTexture(texture, x, y, u, v, width, height, texWidth, texHeight);
context.drawItem(stack, x, y);
```

## Client State Pattern
```java
public class ClientMyState {
    private static int value = 0;
    
    public static void set(int newValue) {
        value = newValue;
    }
    
    public static int get() {
        return value;
    }
}
```

## Key Bindings
```java
private static KeyBinding myKey = KeyBindingHelper.registerKeyBinding(
    new KeyBinding("key.manaborn.my_action", InputUtil.Type.KEYSYM, 
                   GLFW.GLFW_KEY_X, "category.manaborn.keys")
);

ClientTickEvents.END_CLIENT_TICK.register(client -> {
    while (myKey.wasPressed()) {
        // Handle key press
    }
});
```
