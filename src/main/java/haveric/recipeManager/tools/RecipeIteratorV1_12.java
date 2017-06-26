package haveric.recipeManager.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import haveric.recipeManager.recipes.BaseRecipe;
import haveric.recipeManager.recipes.CombineRecipe;
import haveric.recipeManager.recipes.CraftRecipe;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManager.recipes.SmeltRecipe;
import net.minecraft.server.v1_12_R1.CraftingManager;
import net.minecraft.server.v1_12_R1.IRecipe;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.RecipesFurnace;
import net.minecraft.server.v1_12_R1.RegistryMaterials;
import net.minecraft.server.v1_12_R1.ShapedRecipes;
import net.minecraft.server.v1_12_R1.ShapelessRecipes;
import net.minecraft.server.v1_12_R1.Items;
import net.minecraft.server.v1_12_R1.NonNullList;
import net.minecraft.server.v1_12_R1.RecipeItemStack;

import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftFurnaceRecipe;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_12_R1.inventory.RecipeIterator;

public class RecipeIteratorV1_12 implements Iterator<Recipe> {
    private RecipeIterator backing = null;
    private Iterator<IRecipe> recipes = null;
    private List<MinecraftKey> recipeRemoves = new LinkedList<>();
    private Map<MinecraftKey, BaseRecipe> recipeReplaces = new HashMap<>();

    private Iterator<ItemStack> smeltingCustom = null;
    private List<ItemStack> recipeSmeltingCustom = new LinkedList<>();
    private Map<ItemStack, BaseRecipe> smeltingCustomReplace = new HashMap<>();

    private Iterator<ItemStack> smeltingVanilla = null;
    private List<ItemStack> recipeSmeltingVanilla = new LinkedList<>();
    private Map<ItemStack, BaseRecipe> smeltingVanillaReplace = new HashMap<>();

    enum RemoveFrom {
        RECIPES, CUSTOM, VANILLA
    }

    RemoveFrom removeFrom = null;
    IRecipe removeRecipe = null;
    ItemStack removeItem = null;

    public RecipeIteratorV1_12(Iterator<Recipe> backing) {
        if (backing instanceof RecipeIterator) {
            backing = (RecipeIterator) backing;
            recipes = CraftingManager.recipes.iterator();
            smeltingCustom = RecipesFurnace.getInstance().customRecipes.keySet().iterator();
            smeltingVanilla = RecipesFurnace.getInstance().recipes.keySet().iterator();
        } else {
            throw new IllegalArgumentException("This version is not supported.");
        }
    }

    /**
     * If nothing more is accessible, finalize any removals before informing caller of nothing new.
     */
    @Override
    public boolean hasNext() {
        boolean next = recipes.hasNext() || smeltingCustom.hasNext() || smeltingVanilla.hasNext();
        if (!next) {
            finish();
        }
        return next;
    }

    @Override
    public Recipe next() {
        if (recipes.hasNext()) {
            removeFrom = RemoveFrom.RECIPES;
            removeRecipe = recipes.next();
            return removeRecipe.toBukkitRecipe();
        } else {
            ItemStack item;
            if (smeltingCustom.hasNext()) {
                removeFrom = RemoveFrom.CUSTOM;
                item = smeltingCustom.next();
            } else {
                removeFrom = RemoveFrom.VANILLA;
                item = smeltingVanilla.next();
            }
            removeItem = item;

            CraftItemStack stack = CraftItemStack.asCraftMirror(RecipesFurnace.getInstance().getResult(item));

            return new CraftFurnaceRecipe(stack, CraftItemStack.asCraftMirror(item));
        }
    }

    /**
     * Backing list is now immutable in 1.12.
     * 
     * Instead of removing directly, we accrue removals, and apply them when instructed to or when we reach the end of the list.
     */
    public void remove() {
        if (removeFrom == null) {
            throw new IllegalStateException();
        }
        switch (removeFrom) {
        case RECIPES:
            try {
                Field keyF = removeRecipe.getClass().getField("key");
                MinecraftKey key = (MinecraftKey) keyF.get(removeRecipe);
                System.err.println("Registered to remove " + key.toString());
                recipeRemoves.add(key);
            } catch (Exception e) {
                e.printStackTrace();
            }
        case CUSTOM:
            recipeSmeltingCustom.add(removeItem);
        case VANILLA:
            recipeSmeltingVanilla.add(removeItem);
        }
    }
    
    /**
     * A new augment where instead of removing recipes, we directly "replace" them, in place, without removal.
     * The consequence is they will have the same minecraft key and indexes. RM however will handle crafting. 
     * 
     * @param recipe
     */
    public void replace(BaseRecipe recipe) {
        if (removeFrom == null) {
            throw new IllegalStateException();
        }
        switch (removeFrom) {
        case RECIPES: // Idea here is "in-place" replacement. So @Override will use this.
            if (recipe instanceof CraftRecipe || recipe instanceof CombineRecipe) {
                try {
                    Field keyF = removeRecipe.getClass().getField("key");
                    MinecraftKey key = (MinecraftKey) keyF.get(removeRecipe);
                    System.err.println("Registered to replace " + key.toString());
                    recipeRemoves.add(key);
                    recipeReplaces.put(key, recipe);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("You cannot replace a crafting recipe with a smelting recipe!");
            }
        case CUSTOM: // furnace recipes are safe to just remove and replace like normal, but we'll do 'em in place 
            // just to be safe.
            if (recipe instanceof SmeltRecipe) {
                recipeSmeltingCustom.add(removeItem);
                smeltingCustomReplace.put(removeItem, recipe);
            } else {
                throw new IllegalStateException("You cannot replace a custom smelting recipe with a crafting recipe!");
            }
        case VANILLA:
            if (recipe instanceof SmeltRecipe) {
                recipeSmeltingVanilla.add(removeItem);
                smeltingVanillaReplace.put(removeItem, recipe);
            } else {
                throw new IllegalStateException("You cannot replace a vanilla smelting recipe with a crafting recipe!");
            }
        }
        
    }

    private Field stripPrivateFinal(Class clazz, String field) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Field fieldF = clazz.getDeclaredField(field);
        fieldF.setAccessible(true);
        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(fieldF, fieldF.getModifiers() & ~Modifier.FINAL);
        return fieldF;
    }
    
    public void finish() {
        if (!recipeRemoves.isEmpty() || !recipeReplaces.isEmpty()) {
            RegistryMaterials<MinecraftKey, IRecipe> existing = CraftingManager.recipes;
            existing.iterator().forEachRemaining(recipe -> {
                try {
                    Field keyF = recipe.getClass().getField("key");
                    MinecraftKey key = (MinecraftKey) keyF.get(recipe);
                    if (recipe instanceof ShapedRecipes && (recipeReplaces.containsKey(key) || recipeRemoves.contains(key))) {
                        ShapedRecipes shaped = (ShapedRecipes) recipe;
                        Field widthF = stripPrivateFinal(ShapedRecipes.class, "width");
                        Field heightF = stripPrivateFinal(ShapedRecipes.class, "height");
                        Field itemsF = stripPrivateFinal(ShapedRecipes.class, "items");
                        Field resultF = stripPrivateFinal(ShapedRecipes.class, "result");

                        // now for the _real_ fun, modifying an unmodifiable recipe.
                        if (recipeReplaces.containsKey(key)) {
                            // Now, in this case, we shift the map to the one in our Recipe.
                            BaseRecipe base = recipeReplaces.get(key);
                            if (base instanceof CraftRecipe) {
                                System.err.println("Craft: Replacing " + key.toString());
                                CraftRecipe craft = (CraftRecipe) base;
                                widthF.setInt(shaped, craft.getWidth());
                                heightF.setInt(shaped, craft.getHeight());
                                
                                ItemResult result = craft.getFirstResult();
                                resultF.set(shaped, CraftItemStack.asNMSCopy(result.toItemStack()));
                                
                                NonNullList<RecipeItemStack> newList = null;
                                org.bukkit.inventory.ItemStack[] recipeItems = craft.getIngredients();
                                RecipeItemStack[] convertedItems = new RecipeItemStack[recipeItems.length];
                                int i = 0;
                                for(org.bukkit.inventory.ItemStack recipeItem : recipeItems) {
                                    convertedItems[i++] = RecipeItemStack.a(new ItemStack[] {CraftItemStack.asNMSCopy(recipeItem)}); 
                                }
                                newList = NonNullList.a(convertedItems[0], convertedItems);
                                itemsF.set(shaped, newList);
                            } else {
                                // TODO: ERROR
                            }
                        } else if (recipeRemoves.contains(key)) {
                            System.err.println("Craft: Removing and nullifying " + key.toString());
                            // So for shaped recipes, my thought is just to replace the ItemStack with something
                            // nonsensical, set height and width to 1, and hope it isn't cached in too many places.
                            // Oh, and set result to air.
                            widthF.setInt(shaped, 1);
                            heightF.setInt(shaped, 1);
                            resultF.set(shaped, new ItemStack(Items.a, 1));
                            itemsF.set(shaped, NonNullList.a(1, RecipeItemStack.a(Items.a)));
                            
                            // validate.
                            ShapedRecipes sr = (ShapedRecipes) CraftingManager.a(key);
                            ShapedRecipe bukkit = sr.toBukkitRecipe();
                            String[] shape = bukkit.getShape();
                            for (String s : shape) {
                                System.out.print(" " + key.toString() + " " + s);
                            }
                        }
                        
                        
                        
                    } else if (recipe instanceof ShapelessRecipes && (recipeReplaces.containsKey(key) || recipeRemoves.contains(key))) {
                        ShapelessRecipes shapeless = (ShapelessRecipes) recipe;
                        Field ingredientsF = stripPrivateFinal(ShapelessRecipes.class, "ingredients");
                        Field resultF = stripPrivateFinal(ShapelessRecipes.class, "result");
                        if (recipeReplaces.containsKey(key)) {
                            BaseRecipe base = recipeReplaces.get(key);
                            if (base instanceof CombineRecipe) {
                                System.err.println("Combine: Replacing " + key.toString());
                                CombineRecipe combine = (CombineRecipe) base;

                                ItemResult result = combine.getFirstResult();
                                resultF.set(shapeless, CraftItemStack.asNMSCopy(result.toItemStack()));
                                
                                NonNullList<RecipeItemStack> newList = null;
                                List<org.bukkit.inventory.ItemStack> recipeItems = combine.getIngredients();
                                RecipeItemStack[] convertedItems = new RecipeItemStack[recipeItems.size()];
                                int i = 0;
                                for(org.bukkit.inventory.ItemStack recipeItem : recipeItems) {
                                    convertedItems[i++] = RecipeItemStack.a(new ItemStack[] {CraftItemStack.asNMSCopy(recipeItem)});
                                }
                                newList = NonNullList.a(convertedItems[0], convertedItems);
                                ingredientsF.set(shapeless, newList);
                            } else { 
                                // TODO: ERROR
                            }
                        } else if (recipeRemoves.contains(key)) {
                            System.err.println("Combine: Remove " + key.toString());
                            resultF.set(shapeless, new ItemStack(Items.a, 1));
                            ingredientsF.set(shapeless, NonNullList.a(1, RecipeItemStack.a(Items.a)));                            

                            // validate.
                            ShapelessRecipes sr = (ShapelessRecipes) CraftingManager.a(key);
                            ShapelessRecipe bukkit = sr.toBukkitRecipe();
                            List<org.bukkit.inventory.ItemStack> shape = bukkit.getIngredientList();
                            for (org.bukkit.inventory.ItemStack s : shape) {
                                System.out.print(" " + key.toString() + " " + s.toString());
                            }
                        }
                    } else {
                        // TODO: ERROR
                    }
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        if (!recipeSmeltingCustom.isEmpty() || !smeltingCustomReplace.isEmpty()) {
            RecipesFurnace furnaces = RecipesFurnace.getInstance();
            recipeSmeltingCustom.forEach(item -> {
                if (smeltingCustomReplace.containsKey(item)) {
                    SmeltRecipe replacement = (SmeltRecipe) smeltingCustomReplace.get(item);
                    furnaces.customRecipes.put(item, CraftItemStack.asNMSCopy(replacement.getResult().toItemStack()));
                    // furnaces.customExperience.put(item, ??)  no equiv!?
                } else {
                    furnaces.customRecipes.remove(item);
                    furnaces.customExperience.remove(item);
                }
            });
        }
        if (!recipeSmeltingVanilla.isEmpty() || !smeltingVanillaReplace.isEmpty()) {
            RecipesFurnace furnaces = RecipesFurnace.getInstance();
            try {
                Field experienceF = RecipesFurnace.class.getDeclaredField("experience");
                experienceF.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<ItemStack, Float> experience = (Map<ItemStack, Float>) experienceF.get(furnaces);
                recipeSmeltingVanilla.forEach(item -> {
                    if (smeltingVanillaReplace.containsKey(item)) {
                        SmeltRecipe replacement = (SmeltRecipe) smeltingVanillaReplace.get(item);
                        furnaces.recipes.put(item, CraftItemStack.asNMSCopy(replacement.getResult().toItemStack()));
                        // experiences.put(item, ??) no equiv?!
                    } else {
                        furnaces.recipes.remove(item);
                        experience.remove(item);
                    }
                });
            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();
            }

        }
    }
}
