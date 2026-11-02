package noxisclient.mod.gui.items.buttons;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import noxisclient.api.utils.render.Render2DUtil;
import noxisclient.mod.gui.ClickGuiScreen;
import noxisclient.mod.gui.click.SoundSelectScreen;
import noxisclient.mod.gui.items.buttons.Button;
import noxisclient.mod.modules.impl.client.ClickGui;
import noxisclient.mod.modules.impl.render.LogoutSpots;
import noxisclient.mod.modules.settings.impl.EnumSetting;

public class EnumButton
extends Button {
    public final EnumSetting<?> setting;
    private boolean open;

    public EnumButton(EnumSetting<?> setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        Color color = ClickGui.getInstance().color.getValue();
        boolean hovered = this.isHovering(mouseX, mouseY);
        float radius = 3.0f;
        Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().defaultColor.getValue());
        if (hovered) {
            Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().hoverColor.getValue());
        }
        Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, this.y, (float)this.width, (float)this.height, radius, ClickGui.getInstance().color.getValue());
        this.drawString(this.setting.getName() + " " + String.valueOf(Formatting.GRAY) + EnumSetting.getDisplayName(this.setting.getValue()), (double)(this.x + 5.0f), (double)(this.y - 2.0f - (float)ClickGuiScreen.getInstance().getTextOffset()), this.getState() ? enableTextColor : defaultTextColor);
        if (this.open) {
            int y = (int)this.y;
            for (Enum e : (Enum[])((Enum)this.setting.getValue()).getDeclaringClass().getEnumConstants()) {
                String s = e.name();
                String displayName = EnumSetting.getDisplayName(e);
                float enumY = (float)y + (float)this.height;
                Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, enumY, (float)this.width, 11.0f, 3.0f, ClickGui.getInstance().defaultColor.getValue());
                if ((float)mouseX >= this.x && (float)mouseX <= this.x + (float)this.width && mouseY >= (int)enumY && mouseY < (int)enumY + 11) {
                    Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, enumY, (float)this.width, 11.0f, 3.0f, ClickGui.getInstance().hoverColor.getValue());
                }
                if (((Enum)this.setting.getValue()).name().equals(s)) {
                    Render2DUtil.drawRoundedRect(context.getMatrices(), this.x, enumY, (float)this.width, 11.0f, 3.0f, ClickGui.getInstance().color.getValue());
                }
                this.drawString(displayName, (double)((float)this.width / 2.0f - (float)this.getWidth(displayName) / 2.0f + 5.0f + this.x), (double)(enumY - 2.0f - (float)ClickGuiScreen.getInstance().getTextOffset()), ((Enum)this.setting.getValue()).name().equals(s) ? enableTextColor : defaultTextColor);
                y += 11;
            }
        }
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (SoundSelectScreen.isMioSoundSetting(this.setting) && this.isHovering(mouseX, mouseY)) {
            if (mouseButton == 0) {
                SoundSelectScreen.open(this.setting, EnumButton.mc.currentScreen);
                EnumButton.sound();
            } else if (mouseButton == 1) {
                LogoutSpots.playMioSound((LogoutSpots.MioSound)((Object)this.setting.getValue()));
            }
            return;
        }
        if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.open = !this.open;
            EnumButton.sound();
        } else if (mouseButton == 0 && this.open) {
            int y = (int)this.y;
            for (Enum o : (Enum[])((Enum)this.setting.getValue()).getDeclaringClass().getEnumConstants()) {
                if ((float)mouseX > this.x && (float)mouseX < this.x + (float)this.width && mouseY >= y + this.height + 1 && mouseY < y + this.height + 11 + 1) {
                    this.setting.setEnumValue(String.valueOf(o));
                    EnumButton.sound();
                }
                y += 11;
            }
        }
    }

    @Override
    public int getHeight() {
        return super.getHeight() + (this.open ? 11 * ((Enum[])((Enum)this.setting.getValue()).getDeclaringClass().getEnumConstants()).length : 0);
    }

    @Override
    public void toggle() {
        this.setting.increaseEnum();
    }

    @Override
    public boolean getState() {
        return true;
    }
}

