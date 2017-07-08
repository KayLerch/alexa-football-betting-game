package io.klerch.alexa.betting.populators.util;

import com.amazon.speech.speechlet.Application;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import io.klerch.alexa.betting.commons.util.Settings;
import io.klerch.alexa.state.handler.AWSDynamoStateHandler;

import java.util.UUID;

public class ResourceFactory {
    private Settings settings;
    public static final ResourceFactory DEFAULT = new ResourceFactory(Settings.DEFAULT);

    private ResourceFactory(final Settings settings) {
        this.settings = settings;
    }

    public AWSDynamoStateHandler getDynamoHandler() {
        return getDynamoHandler(null);
    }

    public AWSDynamoStateHandler getDynamoHandler(final String uid) {
        final long readCapacity = settings.forceReadAsLong("dynamoReadCapacityUnit");
        final long writeCapacity = settings.forceReadAsLong("dynamoWriteCapacityUnit");
        final AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(settings.forceRead("awsRegion"))
                .build();
        return new AWSDynamoStateHandler(getSession(uid), dynamoDB, readCapacity, writeCapacity);
    }

    public AmazonSNS getSNSClient() {
        return AmazonSNSClientBuilder.standard()
                .withRegion(settings.forceRead("awsRegion"))
                .build();
    }

    private Session getSession(final String uid) {
        final String skillId = settings.forceRead("skillId");
        // only skill-id needs to be set as we're writing to DB in APPLICATION-scope only
        return Session.builder()
                .withApplication(new Application(skillId))
                // fake user-id - it won't matter as we're writing to DB in APPLICATION-scope only
                .withUser(User.builder().withUserId(uid).build())
                .withSessionId(UUID.randomUUID().toString())
                .withIsNew(false)
                .build();
    }
}
