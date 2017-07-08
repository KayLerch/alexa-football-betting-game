package io.klerch.alexa.betting.populators.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.klerch.alexa.betting.commons.model.Bet;
import io.klerch.alexa.betting.commons.model.BetDay;
import io.klerch.alexa.betting.commons.model.MatchDay;
import io.klerch.alexa.betting.commons.util.Settings;
import io.klerch.alexa.betting.populators.util.BetScoreUpdateExposure;
import io.klerch.alexa.betting.populators.util.OLApi;
import io.klerch.alexa.betting.populators.util.ResourceFactory;
import io.klerch.alexa.state.handler.AlexaStateHandler;
import io.klerch.alexa.state.utils.AlexaStateException;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PopulateMatchDaysLambda implements RequestHandler<Map<String,Object>, String> {
    private static final Logger log = Logger.getLogger(PopulateMatchDaysLambda.class);

    private Comparator<MatchDay> sortDescByMatchDayId = (o1, o2) -> o2.getMatchDayId().compareTo(o1.getMatchDayId());
    private Comparator<MatchDay> sortAscByMatchDayId = Comparator.comparing(MatchDay::getMatchDayId);
    private AlexaStateHandler dynamoHandler;

    @Override
    public String handleRequest(final Map<String, Object> input, Context context) {
        final String userId = "546334gdf";
        dynamoHandler = ResourceFactory.DEFAULT.getDynamoHandler(userId);
        // read league and season from environment
        final String leagueId = Settings.DEFAULT.forceRead("leagueId");
        final String seasonId = Settings.DEFAULT.forceRead("seasonId");

        // get matchdays from external source
        final Map<String, MatchDay> newMatchDays = OLApi.getMatchDays(leagueId, seasonId);

        try {
            // get matchdays from internal source (dynamo)
            final Map<String, MatchDay> existingMatchdays = dynamoHandler.readModels(MatchDay.class, newMatchDays.keySet());

            // this is where we put all matchdays that got a score-impact change
            final Map<String, MatchDay> updatedMatchDays = new HashMap<>();

            newMatchDays.forEach((matchDayId, matchDay) -> {
                if (existingMatchdays.containsKey(matchDayId)) {
                    // try merge old matchday with new matchday data
                    if (existingMatchdays.get(matchDayId).merge(matchDay)) {
                        // keep in mind this has been updated with a score-impact change
                        updatedMatchDays.put(matchDayId, matchDay);
                    }
                } else {
                    // add new matchday as it's not existing in the old collection
                    existingMatchdays.put(matchDayId, matchDay);
                    updatedMatchDays.put(matchDayId, matchDay);
                }
            });
            // now that we merged existing models with updates coming from external source
            // we write back the made changes to dynamo
            dynamoHandler.writeModels(existingMatchdays.values());

            // select last 3 processed match-days and redundantly save them with other keys (last0, last1, last2)
            final List<MatchDay> lastFinishedMatchDays = existingMatchdays.values().stream().filter(MatchDay::isFinished).sorted(sortDescByMatchDayId).limit(3).collect(Collectors.toList());
            updateRecentMatchDays(lastFinishedMatchDays, dynamoHandler, Settings.DEFAULT.getDbKeyLast(leagueId, seasonId, null));

            // select last 3 running match-days and redundantly save them with other keys (running0, running1, running2)
            final List<MatchDay> runningMatchDays = existingMatchdays.values().stream().filter(MatchDay::isRunning).sorted(sortDescByMatchDayId).limit(3).collect(Collectors.toList());
            updateRecentMatchDays(runningMatchDays, dynamoHandler, Settings.DEFAULT.getDbKeyRunning(leagueId, seasonId, null));

            // select next 3 upcoming match-days and redundantly save them with other keys (next0, next1, next2)
            final List<MatchDay> nextMatchDays = existingMatchdays.values().stream().filter(MatchDay::hasNotStarted).sorted(sortAscByMatchDayId).limit(3).collect(Collectors.toList());
            updateRecentMatchDays(nextMatchDays, dynamoHandler, Settings.DEFAULT.getDbKeyNext(leagueId, seasonId, null));

            // expose score-impact updates of matchdays to kick-off bet-score rollout
            BetScoreUpdateExposure.exposeRequestsFor(updatedMatchDays);
        } catch (final AlexaStateException e) {
            log.error("Error while populating new matchday-data to internal data-store.", e);
        }
        return "1";
    }

    private void guessNext(final String leagueId, final String seasonId) throws AlexaStateException {
        final Optional<MatchDay> nextMatchDay = dynamoHandler.readModel(MatchDay.class, Settings.DEFAULT.getDbKeyNext(leagueId, seasonId, 0));

        nextMatchDay.ifPresent(matchDay -> {
            try {
                final BetDay betDay = dynamoHandler.readModel(BetDay.class, matchDay.getMatchDayKey()).orElse(dynamoHandler.createModel(BetDay.class, matchDay.getMatchDayKey()).forMatchDay(matchDay));
                betDay.matchesWithoutBet().forEach(matchWithoutBet -> {
                    final Integer i1 = new Random().nextInt(5);
                    final Integer i2 = new Random().nextInt(5);
                    betDay.addBet(new Bet().forMatch(matchWithoutBet).withBet(i1, i2));
                });
                betDay.saveState();
            } catch (final AlexaStateException e) {
                log.error(e);
            }});
    }

    private void updateRecentMatchDays(final List<MatchDay> matchDays, final AlexaStateHandler handler, final String recentKey) {
        for (int i = 2; i >= 0; i--) {
            if (matchDays.size() < (i + 1)) {
                // create dummy-model with id of record to remove
                final MatchDay dummy = new MatchDay();
                dummy.setId(recentKey + i);
                try {
                    dummy.withHandler(handler).removeState();
                } catch (final AlexaStateException e) {
                    log.error("Error removing state for " + recentKey + " matchday with id " + dummy.getMatchDayKey() + e);
                }
            }
        }

        final AtomicInteger i = new AtomicInteger(0);
        matchDays.forEach(matchDay -> {
            matchDay.setId(recentKey + (i.getAndIncrement()));
            try {
                matchDay.withHandler(handler).saveState();
            } catch (final AlexaStateException e) {
                log.error("Error writing state for " + recentKey + " matchday with id " + matchDay.getMatchDayKey() + e);
            }
        });
    }


}
