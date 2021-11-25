package be.kuleuven.distributedsystems.cloud;

import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;

public class CreatePushSubscriptionExample {
    public static void main(String... args) throws Exception {
        // TODO(developer): Replace these variables before running the sample.
        String projectId = "demo-distributed-systems-kul";
        String subscriptionId = "my_sub_push";
        String topicId = "my_topic";
        String pushEndpoint = "http://localhost:3000/";

        createPushSubscriptionExample(projectId, subscriptionId, topicId, pushEndpoint);
    }

    public static void createPushSubscriptionExample(
            String projectId, String subscriptionId, String topicId, String pushEndpoint)
            throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
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