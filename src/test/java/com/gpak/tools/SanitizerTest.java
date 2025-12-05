package com.gpak.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.gpak.tools.textminator.core.Sanitizer;
import com.gpak.tools.textminator.model.Rule;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizerTest {

    private Sanitizer sanitizer;

    @BeforeEach
    void setUp() {
        // Rules
        Rule email = new Rule("email", Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"), "<EMAIL>", 1, true);
        Rule uuid = new Rule("uuid", Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\b"), "<UUID>", 2, true);
        Rule ipv4 = new Rule("ipv4", Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\b"), "<IPV4>", 3, true);
        Rule ipv6 = new Rule("ipv6", Pattern.compile("\\b[0-9a-fA-F:]{2,39}\\b"), "<IPV6>", 4, true);        
    
        ArrayList<Rule> rules = new ArrayList<>();
        rules.add(email);
        rules.add(uuid);
        rules.add(ipv4);
        rules.add(ipv6);

        sanitizer = new Sanitizer(rules, false, false);
    }

    @Test
    void sanitizesEmailUuidIpv4Ipv6() {
        String input = "user john.doe@example.com from 192.168.1.10 " +
                       "uuid 123e4567-e89b-12d3-a456-426614174000 " +
                       "ipv6 2001:0db8:0000:0000:0000:ff00:0042:8329";

        String expected = "user <EMAIL> from <IPV4> uuid <UUID> ipv6 <IPV6>";
        assertEquals(expected, sanitizer.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesEmail() {
        String input = "user john.doe@example.com";
        String expected = "user <EMAIL>";

        assertEquals(expected, sanitizer.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesUuid() {
        String input = "Oh look! A UUID! 123e4567-e89b-12d3-a456-426614174000";
        String expected = "Oh look! A UUID! <UUID>";

        assertEquals(expected, sanitizer.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesIpv4() {
        String input = "And this is an 12.23.45.67";
        String expected = "And this is an <IPV4>";

        assertEquals(expected, sanitizer.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesIpv6() {
        String input = "And this is an ip v6 2001:0db8:0000:0000:0000:ff00:0042:8329";
        String expected = "And this is an ip v6 <IPV6>";

        assertEquals(expected, sanitizer.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesEmailRuleDisabled() {
        Rule email = new Rule("email", Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+"), "<EMAIL>", 1, false);
        
        ArrayList<Rule> rules = new ArrayList<>();
        rules.add(email);

        Sanitizer sanitizerRuleDisabled = new Sanitizer(rules, false, false);

        String input = "user john.doe@example.com";
        String expected = "user john.doe@example.com";

        assertEquals(expected, sanitizerRuleDisabled.sanitizeLine(input).getLine());
    }

    @Test
    void sanitizesRfcIpV6() {
        Rule ipv6  = new Rule("ipv6",  Pattern.compile("\\b(?:fe80:(?::[0-9A-Fa-f]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(?:ffff(?::0{1,4}){0,1}:){0,1}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}|(?:[0-9A-Fa-f]{1,4}:){1,4}:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}|(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}|(?:[0-9A-Fa-f]{1,4}:){1,7}:|(?:[0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4}|(?:[0-9A-Fa-f]{1,4}:){1,5}(?::[0-9A-Fa-f]{1,4}){1,2}|(?:[0-9A-Fa-f]{1,4}:){1,4}(?::[0-9A-Fa-f]{1,4}){1,3}|(?:[0-9A-Fa-f]{1,4}:){1,3}(?::[0-9A-Fa-f]{1,4}){1,4}|(?:[0-9A-Fa-f]{1,4}:){1,2}(?::[0-9A-Fa-f]{1,4}){1,5}|[0-9A-Fa-f]{1,4}:(?:(?::[0-9A-Fa-f]{1,4}){1,6})|:(?:(?::[0-9A-Fa-f]{1,4}){1,7}|:))\\b"), "<IPV6>", 1, true);
        
        ArrayList<Rule> rules = new ArrayList<>();
        rules.add(ipv6);

        Sanitizer sanitizerRfcIpV6 = new Sanitizer(rules, false, false);

        String input = "And this is an ip v6 fe80::7:8%eth0 ip.add.re.ss";
        String expected = "And this is an ip v6 <IPV6> ip.add.re.ss";

        assertEquals(expected, sanitizerRfcIpV6.sanitizeLine(input).getLine());
    }
}