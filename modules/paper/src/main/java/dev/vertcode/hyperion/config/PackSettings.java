package dev.vertcode.hyperion.config;

/**
 * The {@code pack} section of {@code config.yml}.
 *
 * @param hosting how players get the pack
 * @param port the port the built-in web server listens on
 * @param address the address players download from, empty for the one each player joined with
 * @param url the link to the uploaded zip, for {@link PackHosting#URL}
 * @param sendOnJoin whether players are offered the pack when they join
 * @param required whether players who decline or fail to download it are kicked
 * @param resendOnChange whether a changed pack is sent to everyone online
 * @param baseItem the name of the item every model part is shown as, such as {@code paper}
 * @param customModelDataStart the first custom model data number, on 1.21 to 1.21.3
 */
public record PackSettings(
        PackHosting hosting,
        int port,
        String address,
        String url,
        boolean sendOnJoin,
        boolean required,
        boolean resendOnChange,
        String baseItem,
        int customModelDataStart
) {
}
