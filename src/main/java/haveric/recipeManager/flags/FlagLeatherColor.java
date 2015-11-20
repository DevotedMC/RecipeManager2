package haveric.recipeManager.flags;

import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import haveric.recipeManager.ErrorReporter;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManager.tools.Tools;

public class FlagLeatherColor extends Flag {
    private static final FlagType TYPE = FlagType.LEATHERCOLOR;

    @Override
    protected String[] getArguments() {
        return new String[] {
            "{flag} <red> <green> <blue>", };
    }

    @Override
    protected String[] getDescription() {
        return new String[] {
            "Changes result's leather armor color, colors must be 3 numbers ranged from 0 to 255, the red, green and blue channels.",
            "",
            "Specific items: leather armor.", };
    }

    @Override
    protected String[] getExamples() {
        return new String[] {
            "{flag} 255 100 50", };
    }


    private Color color;

    public FlagLeatherColor() {
    }

    public FlagLeatherColor(FlagLeatherColor flag) {
        color = flag.color;
    }

    @Override
    public FlagLeatherColor clone() {
        super.clone();
        return new FlagLeatherColor(this);
    }

    @Override
    public FlagType getType() {
        return TYPE;
    }

    @Override
    protected boolean onValidate() {
        ItemResult result = getResult();

        if (result == null || !(result.getItemMeta() instanceof LeatherArmorMeta)) {
            return ErrorReporter.error("Flag " + getType() + " needs a leather armor item!");
        }

        return true;
    }

    @Override
    protected boolean onParse(String value) {
        color = Tools.parseColor(value);

        if (color == null) {
            return ErrorReporter.error("Flag " + getType() + " has invalid color numbers!", "Use 3 numbers ranging from 0 to 255, e.g. 255 128 0 for orange.");
        }

        return true;
    }

    @Override
    protected void onPrepare(Args a) {
        if (!a.hasResult()) {
            a.addCustomReason("Needs result!");
            return;
        }

        if (!applyOnItem(a.result(), color)) {
            a.addCustomReason("Needs leather armor!");
        }
    }

    private boolean applyOnItem(ItemStack item, Color newColor) {
        ItemMeta meta = item.getItemMeta();

        if (!(meta instanceof LeatherArmorMeta)) {
            return false;
        }

        LeatherArmorMeta leather = (LeatherArmorMeta) meta;

        leather.setColor(newColor);

        item.setItemMeta(leather);

        return true;
    }
}
