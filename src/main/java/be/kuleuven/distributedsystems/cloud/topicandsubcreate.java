package be.kuleuven.distributedsystems.cloud;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;

public class topicandsubcreate {
    public static void main(String... args) throws Exception {
        String projectId = "demo-distributed-systems-kul";
        String subscriptionId = "my_sub";
        String topicId = "my_topic";
        String pushEndpoint = "http://localhost:8080/";
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();//of waar de pub sub emulator is

        // TODO(developer): Replace these variables before running the sample.
        TransportChannelProvider channelProvider =
                FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        createTopicExample(projectId, topicId,channelProvider,credentialsProvider);
        createPushSubscriptionExample(
               projectId,subscriptionId,topicId,pushEndpoint, channelProvider,credentialsProvider);
    }

    public static void createTopicExample(String projectId, String topicId,TransportChannelProvider channelProvider,CredentialsProvider credentialsProvider) throws IOException {
        try (TopicAdminClient topicClient =
                TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                                    .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build());) {
            TopicName topicName = TopicName.of(projectId, topicId);
            Topic topic = topicClient.createTopic(topicName);
            System.out.println("Created topic: " + topic.getName());
        }
    }
    public static void createPushSubscriptionExample(
            String projectId, String subscriptionId, String topicId, String pushEndpoint, TransportChannelProvider channelProvider, CredentialsProvider credentialsProvider)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                        .setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build()
        )) {
            TopicName topicName = TopicName.of(projectId, topicId);
            ProjectSubscriptionName subscriptionName =
                    ProjectSubscriptionName.of(projectId, subscriptionId);
            PushConfig pushConfig = PushConfig.newBuilder().setPushEndpoint(pushEndpoint).build();

            // Create a push subscription with default acknowledgement deadline of 10 seconds.
            // Messages not successfully acknowledged within 10 seconds will get resent by the server.
            Subscription subscription =
                    subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 10);

            System.out.println("Created push subscription: " + subscription.getName());
        }
    }
}


