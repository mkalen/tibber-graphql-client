package se.kalen.tibber.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * Static utility methods for Tibber GraphQL client API.
 * 
 * @author Martin Kalén
 */
public class FormatUtil {

    private static Encoder encoder = Base64.getEncoder();

    private FormatUtil() {
        // Not for public use
    }

    /**
     * Returns an API-compatible Base64-encoded string representation of the specified date-time.
     * @param dateTime date time instance
     * @return Base64-encoded String representation of the specified date-time, suitable for API parameter usage.
     * <code>null</code> if dateTime is null.
     */
    public static String toCursorString(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return toCursorString(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
    }

    /**
     * Returns an API-compatible Base64-encoded string representation of the specified date-time.
     * @param dateTime ISO 8601-formatted date time
     * @return Base64-encoded String representation of the specified date-time, suitable for API parameter usage.
     * <code>null</code> if dateTime is null.
     */
    public static String toCursorString(final String dateTimeString) {
        if (dateTimeString == null) {
            return null;
        }
        return encoder.encodeToString(dateTimeString.getBytes());
    }

}
