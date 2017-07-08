package io.klerch.alexa.betting.commons.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.klerch.alexa.betting.commons.util.Settings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

public class Match {
    private String matchId;
    private String team1;
    private String team2;
    private Double team1Rate = 1.0;
    private Double team2Rate = 1.0;
    private Double drawRate = 1.0;
    private Date matchDate;
    private boolean matchIsFinished = false;
    private Integer team1Score;
    private Integer team2Score;

    public Match() {}

    public Match(final JsonNode payload) {
        this.matchId = payload.get("MatchID").asText();
        Validate.notBlank(matchId, "MatchId is blank or could not be read from JSON-playload.");

        this.team1 = Optional.ofNullable(payload.get("Team1").get("ShortName")).filter(sn -> StringUtils.isNotBlank(sn.textValue())).orElse(payload.get("Team1").get("TeamName")).textValue();
        Validate.notBlank(team1, "Team1 is blank or could not be read from match with id " + matchId);

        this.team2 = Optional.ofNullable(payload.get("Team2").get("ShortName")).filter(sn -> StringUtils.isNotBlank(sn.textValue())).orElse(payload.get("Team2").get("TeamName")).textValue();
        Validate.notBlank(team2, "Team2 is blank or could not be read from match with id " + matchId);

        this.matchIsFinished = Optional.ofNullable(payload.get("MatchIsFinished")).filter(finished -> finished.asBoolean(false)).isPresent();

        try {
            this.matchDate = DateUtils.parseDate(payload.get("MatchDateTimeUTC").textValue(), "yyyy-MM-dd'T'HH:mm:ssXXX");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        payload.get("MatchResults").elements().forEachRemaining(result -> {
            // only interested in final score
            if ("2".equals(result.get("ResultTypeID").asText())) {
                team1Score = result.get("PointsTeam1").asInt();
                team2Score = result.get("PointsTeam2").asInt();
            }
        });
    }

    public String getMatchId() {
        return this.matchId;
    }

    public boolean isMatchIsFinished() {
        return this.matchIsFinished;
    }

    public void setMatchIsFinished(final boolean matchIsFinished) {
        this.matchIsFinished = matchIsFinished;
    }

    @JsonIgnore
    public boolean isMatchProgressing() {
        return !isMatchIsFinished() && team1Score != null && team2Score != null;
    }

    public Double getBetScore(final Integer betTeam1, final Integer betTeam2) {
        if (!isMatchIsFinished() && !isMatchProgressing()) return 0.0;

        final Settings settings = Settings.DEFAULT;
        // get core score
        final Integer coreScore = ((team1Score - betTeam1) == 0 && (team2Score - betTeam2) == 0) ? settings.readAsInt("scoreCorrectResult").orElse(Settings.DEFAULT_SCORE_CORRECT_RESULT) :
                (team1Score - team2Score) == (betTeam1 - betTeam2) ? settings.readAsInt("scoreCorrectMargin").orElse(Settings.DEFAULT_SCORE_CORRECT_MARGIN) :
                        Integer.compare(team1Score, team2Score) == Integer.compare(betTeam1, betTeam2) ? settings.readAsInt("scoreCorrectTrend").orElse(Settings.DEFAULT_SCORE_CORRECT_TREND) : 0;

        // apply rates to core score
        return team1Score > team2Score ? coreScore * team1Rate : team2Score < team1Score ? coreScore * team2Rate : coreScore * drawRate;
    }

    public boolean containsScoreImpactUpdate(final Match newMatch) {
        // if live-score is enabled, update to score impacts bet score of users
        return ((Settings.DEFAULT.liveScoreEnabled() && (
                Integer.compare(this.getTeam1Score(), newMatch.getTeam1Score()) != 0 ||
                Integer.compare(this.getTeam2Score(), newMatch.getTeam2Score()) != 0 ||
                Double.compare(this.getTeam1Rate(), newMatch.getTeam1Rate()) != 0 ||
                Double.compare(this.getTeam2Rate(), newMatch.getTeam2Rate()) != 0 ||
                Double.compare(this.getDrawRate(), newMatch.getDrawRate()) != 0)) ||
                // if match status changes (most likely from unfinished to finished)
                this.matchIsFinished != newMatch.isMatchIsFinished());
    }

    public String getTeam1() {
        return team1;
    }

    public void setTeam1(String team1) {
        this.team1 = team1;
    }

    public String getTeam2() {
        return team2;
    }

    public void setTeam2(String team2) {
        this.team2 = team2;
    }

    public Double getTeam1Rate() {
        return team1Rate;
    }

    public void setTeam1Rate(Double team1Rate) {
        this.team1Rate = team1Rate;
    }

    public Double getTeam2Rate() {
        return team2Rate;
    }

    public void setTeam2Rate(Double team2Rate) {
        this.team2Rate = team2Rate;
    }

    public Double getDrawRate() {
        return drawRate;
    }

    public void setDrawRate(Double drawRate) {
        this.drawRate = drawRate;
    }

    public Integer getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(Integer team1Score) {
        this.team1Score = team1Score;
    }

    public Integer getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(Integer team2Score) {
        this.team2Score = team2Score;
    }

    public Date getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(Date matchDate) {
        this.matchDate = matchDate;
    }
}
