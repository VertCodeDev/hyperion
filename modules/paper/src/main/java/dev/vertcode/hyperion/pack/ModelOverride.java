package dev.vertcode.hyperion.pack;

/**
 * A custom model data number and the model it picks, for 1.21 to 1.21.3.
 *
 * @param customModelData the number
 * @param model the model's id
 */
record ModelOverride(int customModelData, String model) {
}
