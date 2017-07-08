package io.klerch.alexa.betting.commons.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.klerch.alexa.state.model.AlexaScope;
import io.klerch.alexa.state.model.AlexaStateModel;
import io.klerch.alexa.state.model.AlexaStateSave;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AlexaStateSave(Scope = AlexaScope.APPLICATION)
public class MatchDay extends AlexaStateModel {
    List<Match> matches;
    String leagueId;
    Integer matchDayId;

    public MatchDay() {

    }

    public MatchDay(final String leagueId, final Integer matchDayId) {
        this.leagueId = leagueId;
        this.matchDayId = matchDayId;
        this.matches = new ArrayList<>();
        setId(getMatchDayKey());
    }

    public void addMatch(final Match match) {
        // remove existing match to avoid duplicates
        matches.removeIf(m -> m.getMatchId().equals(match.getMatchId()));
        matches.add(match);
    }

    public boolean merge(final MatchDay newMatchDay) {
        final List<String> newMatchDayIds = newMatchDay.getMatches().stream().map(Match::getMatchId).collect(Collectors.toList());
        // remove existing matches that are not contained in the new matchday data
        boolean hadScoreImpactUpdates = matches.removeIf(m -> !newMatchDayIds.contains(m.getMatchId()));

        for (final Match newMatch : newMatchDay.getMatches()) {
            final Optional<Match> oldMatch = matches.stream().filter(m -> m.getMatchId().equals(newMatch.getMatchId())).findFirst();
            if (!oldMatch.isPresent()) {
                addMatch(newMatch);
                hadScoreImpactUpdates = true;
            } else {
                final boolean hadScoreImpactUpdate = oldMatch.get().containsScoreImpactUpdate(newMatch);
                // replace matches
                matches.remove(oldMatch.get());
                matches.add(newMatch);
                hadScoreImpactUpdates = hadScoreImpactUpdates || hadScoreImpactUpdate;
            }
        }
        return hadScoreImpactUpdates;
    }
    public String getLeagueId() { return this.leagueId; }

    public Integer getMatchDayId() {
        return this.matchDayId;
    }

    @JsonIgnore
    public String getMatchDayKey() {
        return String.format("%1$s-%2$s", leagueId, matchDayId);
    }

    public boolean isFinished() {
        return matches.stream().allMatch(Match::isMatchIsFinished);
    }

    public boolean hasNotStarted() {
        return matches.stream().noneMatch(Match::isMatchIsFinished);
    }

    public boolean isRunning() {
        return !isFinished() && !hasNotStarted();
    }

    public long matchesNotFinished() {
        return matches.stream().filter(match -> !match.isMatchIsFinished()).count();
    }

    public List<Match> getMatches() {
        return this.matches;
    }
}
