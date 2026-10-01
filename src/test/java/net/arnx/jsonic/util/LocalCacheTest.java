package net.arnx.jsonic.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

public class LocalCacheTest {
    @Test
    public void lazyErrorMessagesRespectLocaleAndArguments() {
        String bundle = "net.arnx.jsonic.Messages";
        for (Locale locale : Arrays.asList(Locale.JAPANESE, Locale.ENGLISH)) {
            LocalCache cache = new LocalCache(
                    bundle, locale, TimeZone.getTimeZone("UTC"));
            ResourceBundle expected = ResourceBundle.getBundle(bundle, locale);
            assertEquals(expected.getString("json.parse.StringNotClosedError"),
                    cache.getMessage("json.parse.StringNotClosedError"));
            String key = "json.parse.UnexpectedChar";
            assertEquals(MessageFormat.format(expected.getString(key), '?'),
                    cache.getMessage(key, '?'));
        }
    }
}
