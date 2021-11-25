package be.kuleuven.distributedsystems.cloud;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.*;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PublisherExample {

    private TransportChannelProvider channelProvider;
    private CredentialsProvider credentialsProvider;

    public void main(String... args) throws Exception {
        String hostport = System.getenv("PUBSUB_EMULATOR_HOST");
        System.out.println("[PUBLISHEREXAMPLE]" + hostport);
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();//of waar de pub sub emulator is

        try {
            channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
            credentialsProvider = NoCredentialsProvider.create();

            TopicName topicName = TopicName.of("demo-distributed-systems-kul", "my_topic");

            // Set the channel and credentials provider when creating a `Publisher`.
            // Similarly for Subscriber
            Publisher publisher =
                    Publisher.newBuilder(topicName)
                            .setChannelProvider(channelProvider)
                            .setCredentialsProvider(credentialsProvider)
                            .build();

            String message = "Hello World!";
            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            String messageId = messageIdFuture.get();
            System.out.println("Published message ID: " + messageId);

            /*
            // TODO(developer): Replace these variables before running the sample.
            String projectId = "demo-distributed-systems-kul";
            String subscriptionId = "stop";
            String topicId = "my_topic";
            String pushEndpoint = "http://localhost:3000/";

<<<<<<< Updated upstream
            createPushSubscriptionExample(projectId, subscriptionId, topicId, pushEndpoint, channelProvider, credentialsProvider);
*/

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        /*// shutdown client
        finally {
            channel.shutdown();
        }*/
    }

    public void createPushSubscriptionExample() throws IOException {
    // public static void createPushSubscriptionExample(String projectId, String subscriptionId, String topicId, String pushEndpoint, TransportChannelProvider channelProvider, CredentialsProvider credentialsProvider) throws IOException {

        // TODO(developer): Replace these variables before running the sample.
        String projectId = "demo-distributed-systems-kul";
        String subscriptionId = "my_sub";
        String topicId = "my_topic";
        String pushEndpoint = "http://localhost:3000/";

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


            MessageReceiver receiver =
                    (PubsubMessage message, AckReplyConsumer consumer) -> {
                        // Handle incoming message, then ack the received message.
                        System.out.println("Id: " + message.getMessageId());
                        System.out.println("Data: " + message.getData().toStringUtf8());
                        // Print message attributes.
                        message
                                .getAttributesMap()
                                .forEach((key, value) -> System.out.println(key + " = " + value));
                        consumer.ack();
                    };

            Subscriber subscriber = null;
            try {
                subscriber = Subscriber.newBuilder(subscriptionName, receiver).setChannelProvider(channelProvider)
                        //.setTransportChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
                // Start the subscriber.
                subscriber.startAsync().awaitRunning();
                System.out.printf("Listening for messages on %s:\n", subscriptionName.toString());
                // Allow the subscriber to run for 30s unless an unrecoverable error occurs.
                subscriber.awaitTerminated(30, TimeUnit.SECONDS);
            } catch (TimeoutException timeoutException) {
                // Shut down the subscriber after 30s. Stop receiving messages.
                subscriber.stopAsync();
            }
        }


    }


}

