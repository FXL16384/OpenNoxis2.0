package noxisclient.mod.gui.click;

import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.gui.DrawContext;
import noxisclient.mod.gui.click.ModuleButton;
import noxisclient.mod.gui.click.SettingButton;
import noxisclient.mod.gui.click.SoundSelectScreen;
import noxisclient.mod.gui.items.Item;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class EnumSettingButton
extends SettingButton {
    private final EnumSetting enumSetting;
    private int index;
    private boolean open;

    public EnumSettingButton(EnumSetting<?> setting, ModuleButton parent, float x, float y) {
        super(setting, parent, x, y, 99.0f, 9.0f + 4.0f);
        this.enumSetting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ModuleButton.beginShaderBatch();
        this.renderBackground(context, mouseX, mouseY, delta);
        ModuleButton.flushShaderBatch(context);
        this.renderForeground(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        Objects.requireNonNull(EnumSettingButton.mc.textRenderer);
        this.height = 9.0f + 4.0f;
        ClickGui gui = ClickGui.getInstance();
        if (gui.isThunderStyle()) {
            if (this.shouldUseShaderBackground()) {
                this.queueShaderBackground(context, this.isParentModuleEnabled());
                this.queueOptionShaderBackgrounds(context, 12.0f, this.y + this.height * 1.0f + 1.0f);
            } else {
                this.renderThunderOptionBackgrounds(context, mouseX, mouseY);
            }
            return;
        }
        int hoverAlpha = this.getHoverAlpha(mouseX, mouseY);
        Color baseColor = gui.color.getValue();
        int alpha = Math.min(255, 100 + hoverAlpha);
        int buttonColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha).getRGB();
        if (this.shouldUseShaderBackground()) {
            this.queueShaderBackground(context, this.isParentModuleEnabled());
        } else {
            this.rect(context, this.x, this.y, this.width * 1.0f, this.height * 1.0f, buttonColor);
        }
        if (this.open && !SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
            Object valEnum = this.enumSetting.getValue();
            if (valEnum == null) {
                return;
            }
            Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
            if (values == null) {
                return;
            }
            float optionY = this.y + this.height * 1.0f + 1.0f;
            for (Object value : values) {
                boolean optionHovered;
                Enum option = (Enum)value;
                boolean bl = optionHovered = (float)mouseX >= this.x && (float)mouseX <= this.x + this.width * 1.0f && (float)mouseY >= optionY && (float)mouseY <= optionY + this.height * 1.0f;
                if (this.shouldUseShaderBackground()) {
                    this.queueShaderBackground(context, option == valEnum, this.x, optionY, this.width * 1.0f, this.height * 1.0f);
                } else {
                    int optionColor;
                    int optionAlpha;
                    if (option == valEnum) {
                        optionAlpha = Math.min(255, 100 + (optionHovered ? 80 : 0));
                        optionColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), optionAlpha).getRGB();
                    } else {
                        optionAlpha = optionHovered ? 150 : 80;
                        optionColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), optionAlpha).getRGB();
                    }
                    this.rect(context, this.x, optionY, this.width * 1.0f, this.height * 1.0f, optionColor);
                }
                optionY += this.height * 1.0f;
            }
        }
    }

    private void renderThunderOptionBackgrounds(DrawContext context, int mouseX, int mouseY) {
        if (!this.open || SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
            return;
        }
        Object valEnum = this.enumSetting.getValue();
        if (valEnum == null) {
            return;
        }
        Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
        if (values == null) {
            return;
        }
        ClickGui gui = ClickGui.getInstance();
        float optionHeight = 12.0f;
        float optionY = this.y + this.height * 1.0f + 1.0f;
        for (Object value : values) {
            boolean optionHovered;
            Enum option = (Enum)value;
            int optionColor = gui.getThunderSettingColor(option == valEnum, optionHovered = (float)mouseX >= this.x && (float)mouseX <= this.x + this.width * 1.0f && (float)mouseY >= optionY && (float)mouseY <= optionY + optionHeight);
            if ((optionColor >>> 24 & 0xFF) > 0) {
                context.fill((int)this.x, (int)optionY, (int)(this.x + this.width * 1.0f), (int)(optionY + optionHeight), optionColor);
            }
            optionY += optionHeight;
        }
    }

    @Override
    public void renderForeground(DrawContext context, int mouseX, int mouseY, float delta) {
        Objects.requireNonNull(EnumSettingButton.mc.textRenderer);
        this.height = 9.0f + 4.0f;
        ClickGui gui = ClickGui.getInstance();
        if (gui.isThunderStyle()) {
            String val = EnumSetting.getDisplayName(this.enumSetting.getValue());
            if (this.open && !SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
                context.fill((int)(this.x + 4.0f), (int)this.y, (int)(this.x + 5.0f), (int)(this.y + 17.0f), gui.getThunderAccentColor());
            }
            this.drawString(context, this.setting.getName(), this.x + 6.0f, this.y + this.height * 1.0f / 2.0f, gui.getThunderTextColor(false));
            float valueWidth = (float)EnumSettingButton.mc.textRenderer.getWidth(val) * 1.0f;
            this.drawString(context, val, this.x + this.width * 1.0f - 18.0f - valueWidth, this.y + this.height * 1.0f / 2.0f, gui.getThunderTextColor(true));
            if (this.open && !SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
                Object valEnum = this.enumSetting.getValue();
                if (valEnum == null) {
                    return;
                }
                Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
                if (values == null) {
                    return;
                }
                float optionY = this.y + this.height * 1.0f + 1.0f;
                for (Object value : values) {
                    Enum option = (Enum)value;
                    String optionName = EnumSetting.getDisplayName(option);
                    float optionWidth = (float)EnumSettingButton.mc.textRenderer.getWidth(optionName) * 1.0f;
                    int optionColor = option == valEnum ? gui.getThunderAccentColor() : gui.getThunderTextColor(false);
                    this.drawString(context, optionName, this.x + this.width * 1.0f / 2.0f - optionWidth / 2.0f, optionY + 2.0f, optionColor);
                    optionY += 12.0f;
                }
            }
            return;
        }
        int labelText = gui.getDefaultTextColor();
        int valueText = gui.getEnableTextColor();
        this.drawString(context, this.setting.getName(), this.x + 2.0f, this.y + 4.0f, labelText);
        float textLeng = (float)EnumSettingButton.mc.textRenderer.getWidth(this.setting.getName()) * 1.0f;
        String val = EnumSetting.getDisplayName(this.enumSetting.getValue());
        this.drawString(context, " " + val, this.x + 2.0f + textLeng, this.y + 4.0f, valueText);
        if (this.open && !SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
            Object valEnum = this.enumSetting.getValue();
            if (valEnum == null) {
                return;
            }
            Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
            if (values == null) {
                return;
            }
            float optionY = this.y + this.height * 1.0f + 1.0f;
            for (int i = 0; i < values.length; ++i) {
                Enum option = (Enum)values[i];
                String optionName = EnumSetting.getDisplayName(option);
                int optionTextColor = option == valEnum ? valueText : labelText;
                this.drawString(context, optionName, this.x + 2.0f, optionY + 4.0f, optionTextColor);
                optionY += this.height * 1.0f;
            }
        }
    }

    private void queueOptionShaderBackgrounds(DrawContext context, float optionHeight, float optionY) {
        if (!this.open || SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
            return;
        }
        Object valEnum = this.enumSetting.getValue();
        if (valEnum == null) {
            return;
        }
        Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
        if (values == null) {
            return;
        }
        for (Object value : values) {
            Enum option = (Enum)value;
            this.queueShaderBackground(context, option == valEnum, this.x, optionY, this.width * 1.0f, optionHeight);
            optionY += optionHeight;
        }
    }

    @Override
    public float getHeight() {
        return this.height;
    }

    public float getTotalHeight() {
        if (this.open) {
            Object valEnum = this.enumSetting.getValue();
            if (valEnum == null) {
                return this.height;
            }
            Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
            if (values == null) {
                return this.height;
            }
            return this.height + (float)values.length * this.height + 1.0f;
        }
        return this.height;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return;
        }
        if (this.isWithin(mouseX, mouseY)) {
            if (SoundSelectScreen.isMioSoundSetting(this.enumSetting)) {
                if (button == 0) {
                    SoundSelectScreen.open(this.enumSetting, EnumSettingButton.mc.currentScreen);
                    Item.sound();
                } else if (button == 1) {
                    LogoutSpots.playMioSound((LogoutSpots.MioSound)((Object)this.enumSetting.getValue()));
                }
                return;
            }
            if (button == 0) {
                Object val = this.enumSetting.getValue();
                if (val == null) {
                    return;
                }
                Object[] enumConstants = ((Enum)val).getDeclaringClass().getEnumConstants();
                if (enumConstants == null) {
                    return;
                }
                int currentIndex = Arrays.asList(enumConstants).indexOf(val);
                this.index = currentIndex + 1 >= enumConstants.length ? 0 : currentIndex + 1;
                this.enumSetting.setValue((Enum)enumConstants[this.index]);
            } else if (button == 1) {
                this.open = !this.open;
            }
            Item.sound();
        } else if (this.open) {
            Object valEnum = this.enumSetting.getValue();
            if (valEnum == null) {
                return;
            }
            Object[] values = ((Enum)valEnum).getDeclaringClass().getEnumConstants();
            if (values == null) {
                return;
            }
            float optionY = this.y + this.height * 1.0f + 1.0f;
            for (int i = 0; i < values.length; ++i) {
                if (mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width * 1.0f) && mouseY >= (double)optionY && mouseY <= (double)(optionY + this.height * 1.0f)) {
                    this.enumSetting.setValue((Enum)values[i]);
                    this.index = i;
                    this.open = false;
                    Item.sound();
                    break;
                }
                optionY += this.height * 1.0f;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
    }

    @Override
    public void charTyped(char chr, int modifiers) {
    }
}

