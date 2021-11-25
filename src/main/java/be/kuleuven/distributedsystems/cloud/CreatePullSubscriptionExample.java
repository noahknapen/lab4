package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Serializer;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.ConsoleHandler;

// overal waar een create opgeroepen wordt, moet je dit in de create() zetten
// TopicAdminSettings.newBuilder()
//         .setTransportChannelProvider(channelProvider)
//         .setCredentialsProvider(credentialsProvider)
//         .build()
public class CreatePullSubscriptionExample {


    @Autowired
    private Publisher publisher;

    public static void main(String... args) throws Exception {

        Quote quote=new Quote("ik.com",new UUID(1L,1L),new UUID(2L,2L));
        String str= Serializer.serialize(quote);
        System.out.println(str);
        Quote quote1=Serializer.deserializeQuote(str);
        System.out.println(quote1.getSeatId().toString());
        System.out.println(2L);
        String customer="ikkeee@HEYYOO.com";



    }

  }