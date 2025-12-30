package com.AdventureRPG.core.engine;

public abstract class InstancePackage extends EngineUtility {

    // Internal
    static final ThreadLocal<CreationContext> CREATION_CONTEXT = new ThreadLocal<>();

    protected final EnginePackage internal;
    protected final SystemPackage owner;

    // Internal //

    public InstancePackage() {

        // Internal
        CreationContext ctx = CREATION_CONTEXT.get();

        if (ctx == null)
            throwException("Instances must be created via internal engine create() method");

        if (ctx.internal == null || ctx.owner == null)
            throwException("Cannot create instance without a proper internal engine or owner reference");

        this.internal = ctx.internal;
        this.owner = ctx.owner;
    }

    static final class CreationContext {
        final EnginePackage internal;
        final SystemPackage owner;

        CreationContext(EnginePackage internal, SystemPackage owner) {
            this.internal = internal;
            this.owner = owner;
        }
    }

    static void setupCreation(EnginePackage internal, SystemPackage owner) {
        CREATION_CONTEXT.set(new CreationContext(internal, owner));
    }

    // Accessible \\

    protected final <T extends InstancePackage> T create(Class<T> instanceClass) {

        InstancePackage.setupCreation(internal, owner);

        if (!InstancePackage.class.isAssignableFrom(instanceClass))
            throwException("Cannot create non-InstancePackage class: " + instanceClass.getName());

        try {
            return instanceClass.getDeclaredConstructor().newInstance();
        }

        catch (Exception e) {

            throwException("Failed to create instance: " + e.getMessage());

            return null;
        }

        finally {
            InstancePackage.CREATION_CONTEXT.remove();
        }
    }
}