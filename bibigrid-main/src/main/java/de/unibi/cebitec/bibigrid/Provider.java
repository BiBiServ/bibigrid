package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages all provider related work like searching for and executing implementations.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class Provider {
    private static final Logger LOG = LoggerFactory.getLogger(Provider.class);

    private static Provider instance;
    private Map<String, ProviderModule> providers;

    private Provider() {
        providers = new HashMap<>();
        loadAllProviders();
    }

    public static Provider getInstance() {
        return instance != null ? instance : (instance = new Provider());
    }

    private void loadAllProviders() {
        List<Class<ProviderModule>> providerClasses = Factory.getInstance().getImplementations(ProviderModule.class);
        for (Class<ProviderModule> moduleClass : providerClasses) {
            try {
                ProviderModule module = moduleClass.newInstance();
                providers.put(module.getName().toLowerCase(Locale.US), module);
                LOG.info(VerboseOutputFilter.V, "Registered provider module " + module.getName());
            } catch (IllegalAccessException | InstantiationException ex) {
                LOG.error("Failed to load provider module " + moduleClass.getName());
            }
        }
    }

    public String[] getProviderNames() {
        String[] providerNames = new String[providers.size()];
        return providers.keySet().toArray(providerNames);
    }

    /**
     * Finds the provider module with the {@code providerName} in the list of loaded modules.
     *
     * @param providerName The name of the provider as listed in
     *                     {@link de.unibi.cebitec.bibigrid.Provider#getProviderNames()}.
     * @return The provider module with the specified name or null.
     */
    public ProviderModule getProviderModule(String providerName) {
        providerName = providerName.toLowerCase(Locale.US);
        if (!providers.containsKey(providerName)) {
            LOG.error("Malformed meta-mode! use: [" + String.join(", ", getProviderNames()) + "] or leave it blank.");
            return null;
        }
        return providers.get(providerName);
    }
}
