package se.kalen.tibber.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Static utility methods for Tibber GraphQL client API.
 * 
 * @author Martin Kalén
 */
public class FormatUtil {

    private FormatUtil() {
        // Not for public use
    }

    /**
     * Returns an API-compatible string representation of the specified date-time.
     * @param dateTime date time instance
     * @return String representation of the specified date-time, suitable for API parameter usage. <code>null</code>
     * if dateTime is null.
     */
    public static String toString(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
