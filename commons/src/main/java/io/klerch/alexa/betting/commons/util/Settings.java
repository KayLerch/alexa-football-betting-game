package io.klerch.alexa.betting.commons.util;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class Settings {
    // fallback default values
    public static final String COMMONS_PROFILE = "commons";
    public static final Integer DEFAULT_SCORE_CORRECT_RESULT = 3;
    public static final Integer DEFAULT_SCORE_CORRECT_MARGIN = 2;
    public static final Integer DEFAULT_SCORE_CORRECT_TREND = 1;

    private static final Logger log = Logger.getLogger(Settings.class);
    private final Properties properties = new Properties();

    public static final Settings DEFAULT = new Settings(Optional.ofNullable(System.getenv("profile")).orElse(System.getProperty("profile")));

    Settings(final String profile) {
        log.info("Try read settings for profile '" + profile + "'");
        Optional.ofNullable(getProfileResourceAsStream(profile)).ifPresent(is -> {
            try {
                properties.load(is);
            } catch (IOException e) {
                log.error("Could not load profile settings for " + profile, e);
            }
        });
        Optional.ofNullable(getProfileResourceAsStream(COMMONS_PROFILE)).ifPresent(is -> {
            try {
                properties.load(is);
            } catch (IOException e) {
                log.error("Could not load common settings.", e);
            }
        });
    }

    private InputStream getProfileResourceAsStream(final String profile) {
        // try get hidden my-resources. if not available use default
        return Optional.ofNullable(Settings.class.getClass().getResourceAsStream("/" + profile + ".my.properties"))
                .orElse(Settings.class.getClass().getResourceAsStream("/" + profile + ".properties"));
    }

    public String forceRead(final String key) {
        return Optional.ofNullable(properties.getProperty(key)).orElseThrow(() -> new RuntimeException("Could not read property '" + key + "'."));
    }

    public Integer forceReadAsInt(final String key) {
        return Optional.of(forceRead(key)).filter(NumberUtils::isNumber).map(NumberUtils::toInt).orElseThrow(() -> new RuntimeException("Could not convert value of property '" + key + "' to Integer."));
    }

    public Double forceReadAsDouble(final String key) {
        return Optional.of(forceRead(key)).filter(NumberUtils::isNumber).map(NumberUtils::toDouble).orElseThrow(() -> new RuntimeException("Could not convert value of property '" + key + "' to Double."));
    }

    public Long forceReadAsLong(final String key) {
        return Optional.of(forceRead(key)).filter(NumberUtils::isNumber).map(NumberUtils::toLong).orElseThrow(() -> new RuntimeException("Could not convert value of property '" + key + "' to Long."));
    }

    public Optional<String> read(final String key) {
        return properties.containsKey(key) ? Optional.of(properties.getProperty(key, "")) : Optional.empty();
    }

    public Optional<Integer> readAsInt(final String key) {
        return read(key).filter(NumberUtils::isNumber).map(NumberUtils::toInt);
    }

    public Optional<Double> readAsDouble(final String key) {
        return read(key).filter(NumberUtils::isNumber).map(NumberUtils::toDouble);
    }

    public Optional<Long> readAsLong(final String key) {
        return read(key).filter(NumberUtils::isNumber).map(NumberUtils::toLong);
    }

    public Optional<Float> readAsFloat(final String key) {
        return read(key).filter(NumberUtils::isNumber).map(NumberUtils::toFloat);
    }

    public boolean isTrue(final String key) {
        return read(key).orElse("false").equalsIgnoreCase("true");
    }

    public boolean isFalse(final String key) {
        return !isTrue(key);
    }

    public boolean liveScoreEnabled() {
        return isTrue("liveScoreEnabled");
    }

    public String getDbKeyNext() {
        return forceRead("dbKeyNext");
    }

    public String getDbKeyLast() {
        return forceRead("dbKeyLast");
    }

    public String getDbKeyRunning() {
        return forceRead("dbKeyRunning");
    }

    public String getDbKeyNext(final String leagueId, final String seasonId, final Integer index) {
        return getDbKey(leagueId, seasonId, index, "dbKeyNext");
    }

    public String getDbKeyLast(final String leagueId, final String seasonId, final Integer index) {
        return getDbKey(leagueId, seasonId, index, "dbKeyLast");
    }

    public String getDbKeyRunning(final String leagueId, final String seasonId, final Integer index) {
        return getDbKey(leagueId, seasonId, index, "dbKeyRunning");
    }

    private String getDbKey(final String leagueId, final String seasonId, final Integer index, final String settingId) {
        return leagueId + "-" + seasonId + "-" + forceRead(settingId) + (index != null ? index : "");
    }
}
