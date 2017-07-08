package io.klerch.alexa.betting.populators.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.klerch.alexa.betting.commons.model.Match;
import io.klerch.alexa.betting.commons.model.MatchDay;
import io.klerch.alexa.betting.commons.util.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OLApi {
    private static final Logger log = Logger.getLogger(OLApi.class);

    public static String getOLAPIdump(final String leagueId, final String seasonId) throws IOException {
        return IOUtils.toString(OLApi.class.getResourceAsStream(String.format("/backup/%1$s-%2$s-backup.json", leagueId, seasonId)));
    }

    public static Map<String, MatchDay> getMatchDays(final String leagueId, final String seasonId) {
        final String endpoint = Settings.DEFAULT.forceRead("getMatchDataEndpoint");
        final String url = String.format("%1$s/%2$s/%3$s", endpoint, leagueId, seasonId);

        final Map<Integer, List<Match>> matchDaysWithMatches = new HashMap<>();
        final Map<String, MatchDay> matchDays = new HashMap<>();

        try {
            final String matchData = Settings.DEFAULT.isTrue("getMatchDataFromOLAPI") ? IOUtils.toString(new URL(url)) : getOLAPIdump(leagueId, seasonId);
            final JsonNode matchNodes = new ObjectMapper().readTree(matchData);

            matchNodes.elements().forEachRemaining(match -> {
                final Integer matchDay = match.get("Group").get("GroupOrderID").asInt();
                matchDaysWithMatches.putIfAbsent(matchDay, new ArrayList<>());
                matchDaysWithMatches.get(matchDay).add(new Match(match));
            });
        } catch (IOException e) {
            log.error(e);
        }

        matchDaysWithMatches.forEach((matchDayId, matches) -> {
            final MatchDay matchDay = new MatchDay(leagueId + "-" + seasonId, matchDayId);
            matches.forEach(matchDay::addMatch);
            matchDays.put(matchDay.getMatchDayKey(), matchDay);
        });
        return matchDays;
    }
}
