package io.klerch.alexa.betting.populators.util;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.klerch.alexa.betting.commons.model.MatchDay;
import io.klerch.alexa.betting.commons.util.Settings;
import org.apache.log4j.Logger;

import java.util.Map;

public class BetScoreUpdateExposure {
    private static final Logger log = Logger.getLogger(BetScoreUpdateExposure.class);

    public static void exposeRequestsFor(final Map<String, MatchDay> updatedMatchDays) {
        if (updatedMatchDays.isEmpty()) {
            log.info("There were no score-impacting updates to match-days. No bet-score update request to expose.");
            return;
        }
        if (!Settings.DEFAULT.isTrue("exposeBetScoreUpdateRequests")) {
            log.info("There were score-impacting updates to match-days but rolling out this to all bet-days is disabled.");
            return;
        }
        AmazonSNS amazonSNS = ResourceFactory.DEFAULT.getSNSClient();
        final String topicArn = Settings.DEFAULT.forceRead("exposeBetScoreUpdateTopicArn");

        final ObjectMapper o = new ObjectMapper();
        try {
            final String payload = o.writeValueAsString(updatedMatchDays.keySet());
            final PublishRequest publishRequest = new PublishRequest(topicArn, payload);
            amazonSNS.publish(publishRequest);
            log.info("Sent out bet-score-update exposure for : '" + payload + "'");
        } catch (final JsonProcessingException e) {
            log.error("Could not serialize list of matchday-ids for bet-score-update exposure.");
        }
    }
}
