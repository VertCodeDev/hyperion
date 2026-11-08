package dev.vertcode.hyperion.blueprint;

/**
 * How bad a problem found while loading a model is.
 */
public enum IssueSeverity {

    /**
     * The model loads, but something in it will not look or behave as it does in Blockbench.
     */
    WARNING,

    /**
     * The model cannot load.
     */
    ERROR
}
