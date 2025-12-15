package com.gpak.tools;

import com.gpak.tools.textminator.model.Rule;
import com.gpak.tools.textminator.util.ConfigUtil;
import com.gpak.tools.textminator.core.ToolContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigUtilTest {

    @TempDir
    Path tempDir;

    private File writeConfig(String content) throws Exception {
        Path p = tempDir.resolve("textminator.properties");
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    @Test
    void loadsValidSingleRule() throws Exception {
        File cfg = writeConfig("""
            email.regex=\\b[\\w.+-]+@[\\w.-]+\\.[\\w.-]+\\b
            email.replacement=<EMAIL>
            email.order=1
            email.enabled=true
            """);

        List<Rule> rules = ConfigUtil.loadConfigFile(cfg);

        assertEquals(1, rules.size());
        Rule r = rules.get(0);

        assertEquals("email", r.getName());
        assertEquals("<EMAIL>", r.getReplacement());
        assertEquals(1, r.getOrder());
        assertTrue(r.isEnabled());
        assertNotNull(r.getPattern());
    }

    @Test
    void sortsRulesByOrderThenName() throws Exception {
        File cfg = writeConfig("""
            b.regex=bb
            b.replacement=<B>
            b.order=2
            b.enabled=true

            a.regex=aa
            a.replacement=<A>
            a.order=2
            a.enabled=true

            c.regex=cc
            c.replacement=<C>
            c.order=1
            c.enabled=true
            """);

        List<Rule> rules = ConfigUtil.loadConfigFile(cfg);

        assertEquals(3, rules.size());
        assertEquals("c", rules.get(0).getName());
        assertEquals("a", rules.get(1).getName());
        assertEquals("b", rules.get(2).getName());
    }

    @Test
    void throwsWhenOrderMissing() throws Exception {
        File cfg = writeConfig("""
            email.regex=aa
            email.replacement=<EMAIL>
            email.enabled=true
            """);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ConfigUtil.loadConfigFile(cfg));

        assertTrue(ex.getMessage().contains("order"));
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    void usesDefaultReplacementWhenMissing() throws Exception {
        File cfg = writeConfig("""
            email.regex=aa
            email.order=1
            email.enabled=true
            """);

        List<Rule> rules = ConfigUtil.loadConfigFile(cfg);

        assertEquals(1, rules.size());
        assertEquals(ToolContext.DEFUALT_REPLACEMENT_VALUE,
            rules.get(0).getReplacement()
        );
    }

    @Test
    void defaultsEnabledToTrueWhenMissing() throws Exception {
        File cfg = writeConfig("""
            email.regex=aa
            email.replacement=<EMAIL>
            email.order=1
            """);

        List<Rule> rules = ConfigUtil.loadConfigFile(cfg);

        assertEquals(1, rules.size());
        assertTrue(rules.get(0).isEnabled());
    }

    @Test
    void throwsWhenAllRulesDisabled() throws Exception {
        File cfg = writeConfig("""
            email.regex=aa
            email.replacement=<EMAIL>
            email.order=1
            email.enabled=false
            """);

        assertThrows(IllegalStateException.class, () -> ConfigUtil.loadConfigFile(cfg));
    }

    @Test
    void throwsWhenRegexIsInvalid() throws Exception {
        File cfg = writeConfig("""
            email.regex=(
            email.replacement=<EMAIL>
            email.order=1
            email.enabled=true
            """);

        assertThrows(RuntimeException.class, () -> ConfigUtil.loadConfigFile(cfg));
    }

    @Test
    void throwsWhenOrderIsNotInteger() throws Exception {
        File cfg = writeConfig("""
            email.regex=aa
            email.replacement=<EMAIL>
            email.order=abc
            email.enabled=true
            """);

        assertThrows(RuntimeException.class, () -> ConfigUtil.loadConfigFile(cfg));
    }

    @Test
    void throwsWhenNoRulesFoundInConfig() throws Exception {
        File cfg = writeConfig("""
            # only comments
            # no rules here
            """);

        assertThrows(IllegalStateException.class, () -> ConfigUtil.loadConfigFile(cfg));
    }
}
