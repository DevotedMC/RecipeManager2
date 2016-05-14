package haveric.recipeManager.flags;

import haveric.recipeManager.ErrorReporter;
import haveric.recipeManager.Files;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManager.tools.Tools;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class FlagPotionItem extends Flag {

    @Override
    protected String getFlagType() {
        return FlagType.POTION_ITEM;
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
            "{flag} <basic effect>",
            "{flag} custom <custom effect>", };
    }

    @Override
    protected String[] getDescription() {
        return new String[] {
            "Builds a potion item, only works with POTION item.",
            "",
            "There are 2 types of potions... basic potions which have 1 effect and custom potions which can have multiple effects.",
            "",
            "Building a basic potion:",
            "",
            "Instead of <basic effect> argument you must enter a series of arguments separated by | character, in any order.",
            "Arguments for basic effect:",
            "  type <potion type>     = (REQUIRED) Type of potion, read '" + Files.FILE_INFO_NAMES + "' at 'POTION TYPES' section (not POTION EFFECT TYPE!)",
            "  level <number or max>  = (optional) Potion's level/tier, usually 1(default) or 2, you can enter 'max' to set it at highest supported level",
            "  extended               = (optional) Potion has extended duration",
            "  splash                 = (optional) Throwable/breakable potion instead of drinkable",
            "",
            "",
            "Building a custom potion requires adding individual effects:",
            "",
            "A basic potion still affects the custom potion like the following:",
            "- If no basic potion is defined the bottle will look like 'water bottle' with no effects listed, effects still apply when drank",
            "- Basic potion's type affects bottle liquid color",
            "- Basic potion's splash still affects if the bottle is throwable instead of drinkable",
            "- Basic potion's extended and level do absolutely nothing.",
            "- The first custom effect added is the potion's name, rest of effects are in description (of course you can use @name to change the item name)",
            "",
            "Once you understand that, you may use @potion custom as many times to add as many effects you want.",
            "",
            "Similar syntax to basic effect, arguments separated by | character, can be in any order.",
            "Arguments for custom effect:",
            "  type <effect type>  = (REQUIRED) Type of potion effect, read '" + Files.FILE_INFO_NAMES + "' at 'POTION EFFECT TYPE' section (not POTION TYPE !)",
            "  duration <float>    = (optional) Duration of the potion effect in seconds, default 1 (does not work on HEAL and HARM)",
            "  amplify <number>    = (optional) Amplify the effects of the potion, default 0 (e.g. 2 = <PotionName> III, numbers after potion's max level will display potion.potency.number instead)",
            "  ambient             = (optional) Adds extra visual particles", };
    }

    @Override
    protected String[] getExamples() {
        return new String[] {
            "{flag} level max | type FIRE_RESISTANCE | extended // basic extended fire resistance potion",
            "// advanced potion example:",
            "{flag} type POISON | splash // set the bottle design and set it as splash",
            "{flag} custom type WITHER | duration 10 // add wither effect",
            "{flag} custom duration 2.5 | type BLINDNESS | amplify 5 // add blindness effect", };
    }


    private short data;
    private List<PotionEffect> effects = new ArrayList<PotionEffect>();

    public FlagPotionItem() {
    }

    public FlagPotionItem(FlagPotionItem flag) {
        data = flag.data;
        effects.addAll(flag.effects);
    }

    @Override
    public FlagPotionItem clone() {
        return new FlagPotionItem((FlagPotionItem) super.clone());
    }

    public short getData() {
        return data;
    }

    public void setData(short newData) {
        data = newData;
    }

    public List<PotionEffect> getEffects() {
        return effects;
    }

    public void setEffects(List<PotionEffect> newEffects) {
        if (newEffects == null) {
            effects.clear();
        } else {
            effects = newEffects;
        }
    }

    public void addEffect(PotionEffect effect) {
        effects.add(effect);
    }

    @Override
    protected boolean onValidate() {
        ItemResult result = getResult();

        if (result == null || !(result.getItemMeta() instanceof PotionMeta)) {
            ErrorReporter.getInstance().error("Flag " + getFlagType() + " needs a POTION item!");
            return false;
        }

        return true;
    }

    @Override
    protected boolean onParse(String value) {
        if (value.startsWith("custom")) {
            value = value.substring("custom".length()).trim();
            PotionEffect effect = Tools.parsePotionEffect(value, getFlagType());

            if (effect != null) {
                addEffect(effect);
            }
        } else {
            Potion p = Tools.parsePotion(value, getFlagType());

            if (p != null) {
                setData(p.toDamageValue());
            }
        }

        return true;
    }

    @Override
    protected void onPrepare(Args a) {
        if (!a.hasResult()) {
            a.addCustomReason("Needs result!");
            return;
        }

        if (data != 0) {
            a.result().setDurability(data);
        }

        if (!getEffects().isEmpty()) {
            PotionMeta meta = (PotionMeta) a.result().getItemMeta();

            for (PotionEffect effect : getEffects()) {
                meta.addCustomEffect(effect, true);
            }

            a.result().setItemMeta(meta);
        }
    }
}
