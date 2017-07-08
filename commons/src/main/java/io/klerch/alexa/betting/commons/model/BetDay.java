package io.klerch.alexa.betting.commons.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateIgnore;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateSave;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.stream.Collectors;

public class BetDay extends AlexaStateModel {
    @AlexaStateIgnore
    @JsonIgnore
    MatchDay matchDay;

    @AlexaStateSave(Scope = AlexaScope.USER)
    List<Bet> bets = new ArrayList<>();

    @AlexaStateSave(Scope = AlexaScope.APPLICATION)
    Map<Double, Integer> scoreDispersion = new HashMap<>();

    public BetDay() {
    }

    public void addBet(final Bet bet) {
        bets.removeIf(b -> b.getMatchId().equals(bet.getMatchId()));
        this.bets.add(bet);
    }

    public BetDay forMatchDay(final MatchDay matchDay) {
        this.matchDay = matchDay;

        // at runtime bets need their referenced match-object
        // basically all we do here is to hand over match-objects from matchDay to the individual bet-objects
        // the reason for this is to avoid that match-object redundantly persist in bet-objects in the database
        bets.forEach(bet -> {
            // inject match-object whose id is referenced in the bet
            matchDay.matches.stream().filter(m -> m.getMatchId().equals(bet.getMatchId())).forEach(bet::forMatch);
        });
        return this;
    }

    public double getTotalScore() {
        Validate.notNull(matchDay, "You need to inject matchDay-object with forMatchDay before calling this method.");
        return bets.stream().mapToDouble(Bet::getBetScore).sum();
    }

    public boolean hasOutstandingBets() {
        Validate.notNull(matchDay, "You need to inject matchDay-object with forMatchDay before calling this method.");
        return matchDay.matches.size() > bets.size();
    }

    public List<Bet> getBets() {
        return this.bets;
    }

    public Map<Double, Integer> getScoreDispersion() {
        return this.scoreDispersion;
    }

    public void addScoreToDispersion(final Double score) {
        this.scoreDispersion.putIfAbsent(score, 0);
        this.scoreDispersion.compute(score, (k, v) -> v + 1);
    }

    public Optional<Bet> getBetForMatch(final Match match) {
        return bets.stream().filter(b -> b.getMatchId().equals(match.getMatchId())).findFirst();
    }

    public List<Match> matchesWithoutBet() {
        Validate.notNull(matchDay, "You need to inject matchDay-object with forMatchDay before calling this method.");
        // get all match-ids that already got a bet
        final List<String> matchWithBetIds = bets.stream().map(b -> b.getMatch().getMatchId()).collect(Collectors.toList());
        // invert the selection and return just matches in the matchday without a bet
        return matchDay.matches.stream().filter(m -> !matchWithBetIds.contains(m.getMatchId())).collect(Collectors.toList());
    }
}
