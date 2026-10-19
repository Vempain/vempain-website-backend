package fi.poltsi.vempain.website.traffic;

/**
 * What the access log knows about the caller of one request.
 *
 * @param clientIp     the real client address, or the socket address when no proxy header applies
 * @param clientSource {@code socket}, {@code cloudflare} or {@code forwarded}: where the address came from
 * @param country      ISO 3166-1 alpha-2 code, or {@code unknown}
 * @param userAgent    the {@code User-Agent} header truncated to {@value ClientRequestResolver#USER_AGENT_MAX_LENGTH} characters, or null
 * @param bot          whether the user agent looks like a crawler or a command line client
 */
public record ClientRequest(String clientIp, String clientSource, String country, String userAgent, boolean bot) {

    public static final String UNKNOWN_COUNTRY = "unknown";
}
