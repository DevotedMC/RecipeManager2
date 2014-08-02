package ro.thehunters.digi.recipeManager.flags;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

public class FlagBroadcast extends Flag {
    // Flag definition and documentation

    private static final FlagType TYPE;
    protected static final String[] A;
    protected static final String[] D;
    protected static final String[] E;

    static {
        TYPE = FlagType.BROADCAST;

        A = new String[] { "{flag} <text> | [permission]", };

        D = new String[] { "Prints a chat message for all online players.", "Using this flag more than once will overwrite the previous message.", "", "Optionally you can set a permission node that will define who sees the message.", "", "Colors are supported (<red>, &5, etc).", "The message can also contain variables:", "  {player}         = crafter's name or '(nobody)' if not available", "  {playerdisplay}  = crafter's display name or '(nobody)' if not available", "  {result}         = the result item name or '(nothing)' if recipe failed.", "  {recipename}     = recipe's custom or autogenerated name or '(unknown)' if not available", "  {recipetype}     = recipe type or '(unknown)' if not available", "  {inventorytype}  = inventory type or '(unknown)' if not available", "  {world}          = world name of event location or '(unknown)' if not available", "  {x}              = event location's X coord or '(?)' if not available", "  {y}              = event location's Y coord or '(?)' if not available", "  {z}              = event location's Z coord or '(?)' if not available", };

        E = new String[] { "{flag} {playerdisplay} <green>crafted something!", "{flag} '{player}' crafted '{recipename}' at {world}: {x}, {y}, {z} | ranks.admins", };
    }

    // Flag code

    private String message;
    private String permission;

    public FlagBroadcast() {
    }

    public FlagBroadcast(FlagBroadcast flag) {
        message = flag.message;
        permission = flag.permission;
    }

    @Override
    public FlagBroadcast clone() {
        return new FlagBroadcast(this);
    }

    @Override
    public FlagType getType() {
        return TYPE;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Override
    protected boolean onParse(String value) {
        String[] split = value.split("\\|", 2);

        setMessage(split[0].trim());
        setPermission(null);

        if (split.length > 1) {
            setPermission(split[1].trim().toLowerCase());
        }

        return true;
    }

    @Override
    protected void onCrafted(Args a) {
        Validate.notNull(message);

        if (permission == null) {
            Bukkit.broadcastMessage(a.parseVariables(message));
        } else {
            Bukkit.broadcast(a.parseVariables(message), permission);
        }
    }
}
