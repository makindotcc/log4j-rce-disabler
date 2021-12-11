package cc.makin.log4jfix;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        // unexploitable (11.12.2021) logger
        System.out.println("[INFO] Initializing log4j anti bomb.");
        setNoneMatcherAsDefault();
    }

    private static void setNoneMatcherAsDefault() {
        try {
            Object noneMatcher = getNoneMatcher();

            Class<?> strSubstitutor = Class.forName("org.apache.logging.log4j.core.lookup.StrSubstitutor");
            Field defaultPrefixField = strSubstitutor.getField("DEFAULT_PREFIX");
            defaultPrefixField.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(defaultPrefixField, defaultPrefixField.getModifiers() & ~Modifier.FINAL);

            defaultPrefixField.set(null, noneMatcher);
        } catch (NoSuchFieldException | ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException("Unsupported log4j version.", e);
        }
    }

    private static Object getNoneMatcher() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> strMatcherClazz = Class.forName("org.apache.logging.log4j.core.lookup.StrMatcher");
        Field noneMatcherField = strMatcherClazz.getDeclaredField("NONE_MATCHER");
        noneMatcherField.setAccessible(true);
        return noneMatcherField.get(null);
    }
}
