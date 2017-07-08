package io.klerch.alexa.betting.commons.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.Validate;

public class Bet {
    @JsonIgnore
    private Match match;
    private String matchId;
    private Integer team1Score;
    private Integer team2Score;
    private Double betScore = 0.0;

    public Bet() {
    }

    public Bet forMatch(final Match match) {
        this.match = match;
        this.matchId = match.getMatchId();
        // apply bet-score
        this.betScore = match.getBetScore(team1Score, team2Score);
        return this;
    }

    public String getMatchId() {
        return this.matchId;
    }

    public void setBet(final Integer team1Score, final Integer team2Score) {
        Validate.notNull(match, "You need to inject match-object with forMatch before calling this method. Match-objects populate automatically for bets if you hand over MatchDay to BetDay via forMatchDay-method.");
        Validate.isTrue(!match.isMatchIsFinished(), "Cannot set new bet on already finished match.");
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }

    public Bet withBet(final Integer team1Score, final Integer team2Score) {
        setBet(team1Score, team2Score);
        return this;
    }


    public Double getBetScore() {
        return this.betScore;
    }

    public Integer getTeam1Score() {
        return this.team1Score;
    }

    public void setTeam1Score(final Integer team1Score) {
        this.team1Score = team1Score;
    }

    public Integer getTeam2Score() {
        return this.team2Score;
    }

    public void setTeam2Score(final Integer team2Score) {
        this.team2Score = team2Score;
    }

    public Match getMatch() {
        Validate.notNull(match, "You need to inject match-object with forMatch before calling this method. Match-objects populate automatically for bets if you hand over MatchDay to BetDay via forMatchDay-method.");
        return this.match;
    }
}
