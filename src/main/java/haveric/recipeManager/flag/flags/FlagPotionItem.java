package haveric.recipeManager.flag.flags;

import com.google.common.collect.ObjectArrays;
import haveric.recipeManager.ErrorReporter;
import haveric.recipeManager.Files;
import haveric.recipeManager.flag.Flag;
import haveric.recipeManager.flag.FlagType;
import haveric.recipeManager.flag.args.Args;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManager.tools.Tools;
import haveric.recipeManager.tools.Version;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlagPotionItem extends Flag {

    @Override
    public String getFlagType() {
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
        String[] description = new String[] {
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
        };

        if (Version.has19Support()) {
            description = ObjectArrays.concat(description, new String[] {
                    "  lingering              = (optional) Lingering potion instead of drinkable",
            }, String.class);
        }
        description = ObjectArrays.concat(description, new String[] {
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
                "  type <effect type>  = (REQUIRED) Type of potion effect, read '" + Files.FILE_INFO_NAMES + "' at 'POTION EFFECT TYPE' section (not POTION TYPE!)",
                "  duration <float>    = (optional) Duration of the potion effect in seconds, default 1 (does not work on HEAL and HARM)",
                "  amplify <number>    = (optional) Amplify the effects of the potion, default 0 (e.g. 2 = <PotionName> III, numbers after potion's max level will display potion.potency.number instead)",
                "  ambient             = (optional) Adds extra visual particles",
        }, String.class);

        return description;
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
    private List<PotionEffect> effects = new ArrayList<>();
    private ItemStack customPotion;

    public FlagPotionItem() {
    }

    public FlagPotionItem(FlagPotionItem flag) {
        data = flag.data;
        effects.addAll(flag.effects);
        customPotion = flag.customPotion.clone();
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

    public ItemStack getCustomPotion() {
        return customPotion;
    }

    public void setCustomPotion(ItemStack potion) {
        customPotion = potion;
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
    public boolean onValidate() {
        ItemResult result = getResult();

        if (result == null || !(result.getItemMeta() instanceof PotionMeta)) {
            ErrorReporter.getInstance().error("Flag " + getFlagType() + " needs a POTION item!");
            return false;
        }

        return true;
    }

    @Override
    public boolean onParse(String value) {
        if (value.startsWith("custom")) {
            value = value.substring("custom".length()).trim();
            PotionEffect effect = Tools.parsePotionEffect(value, getFlagType());

            if (effect != null) {
                addEffect(effect);
            }
        } else {
            if (Version.has19Support()) {
                setCustomPotion(Tools.parsePotion19(value, getFlagType()));
            } else {
                Potion p = Tools.parsePotion18(value, getFlagType());

                if (p != null) {
                    setData(p.toDamageValue());
                }
            }
        }

        return true;
    }

    @Override
    public void onPrepare(Args a) {
        if (!a.hasResult()) {
            a.addCustomReason("Needs result!");
            return;
        }

        if (getCustomPotion() != null) {
            PotionMeta meta = (PotionMeta) a.result().getItemMeta();
            PotionMeta customMeta = (PotionMeta) getCustomPotion().getItemMeta();

            customMeta.setDisplayName(meta.getDisplayName());
            customMeta.setLore(meta.getLore());
            if (meta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                    customMeta.addEnchant(e.getKey(), e.getValue(), true);
                }
            }

            a.result().setItemMeta(customMeta);
        } else if (data != 0) {
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
