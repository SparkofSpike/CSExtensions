package org.Spike.CSExtensions.api;

import org.Spike.CSExtensions.api.SpikeElementsProvider;

public final class CSEAPI {

    private CSEAPI() {}

    public static SpikeElementsAPI getSpikeElementsAPI() {
        return SpikeElementsProvider.getAPI();
    }

    public static boolean isAvailable() {
        return getSpikeElementsAPI() != null;
    }

    public static void reset() {
        SpikeElementsProvider.reset();
    }
}