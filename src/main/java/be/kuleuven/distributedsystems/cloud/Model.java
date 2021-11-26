package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@Component
public class Model {

    String THEATRE_URL = "https://reliabletheatrecompany.com/";
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    ArrayList<Booking> bookings = new ArrayList<>();

    private ApplicationContext context;

    @Autowired
    private Publisher publisher;

    @Bean
    public Publisher publisher() throws IOException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();//of waar de pub sub emulator is


        TransportChannelProvider channelProvider =
                    FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        TopicName topicName = TopicName.of("demo-distributed-systems-kul", "my_topic");
        Publisher publisher =
                Publisher.newBuilder(topicName)
                        .setChannelProvider(channelProvider)
                        .setCredentialsProvider(credentialsProvider)
                        .build();
        return publisher;
    }

    @Autowired
    private WebClient.Builder webClientBuilder;

    public List<Show> getShows() {
        List<Show> shows;

        // WebClient.builder()  context.getBean
        Collection<Show> showsCollection = webClientBuilder
                .baseUrl(THEATRE_URL)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Show>>() {})
                .block()
                .getContent();

        shows = new ArrayList<>(showsCollection);

        return shows;
    }

    public Show getShow(String company, UUID showId) {
        Show show;

        show = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Show>() {})
                .block();

        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        List<LocalDateTime> times;

        Collection<LocalDateTime> timesCollection = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("times")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<LocalDateTime>>() {})
                .block()
                .getContent();

        times = new ArrayList<>(timesCollection);

        return times;

    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        List<Seat> seats;

        // WebClient.builder()  context.getBean
        Collection<Seat> seatsCollection = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .queryParam("time", time.toString())
                        .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<CollectionModel<Seat>>() {})
                .block()
                .getContent();

        seats = new ArrayList<>(seatsCollection);

        return seats;
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        Seat seat;

        seat = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Seat>() {})
                .block();

        return seat;
    }

    public Ticket putTicket(String company, String showId, String seatId, String customer) throws WebClientResponseException{
        Ticket ticket;
        System.out.println("Company "+company+"showId "+showId+"seatId "+seatId+"Customer "+customer);

        ticket = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId)
                        .pathSegment("seats")
                        .pathSegment(seatId)
                        .pathSegment("ticket")
                        .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .queryParam("customer", customer)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .block();

        return ticket;
    }

    public Booking createBooking(UUID id, LocalDateTime time, List<Ticket> tickets, String customer) {

        return new Booking(id, time, tickets, customer);
    }

    public void registerBooking(Booking booking) {
        bookings.add(booking);
    }

    public List<Booking> getAllBookings() {
        return bookings;
    }

    public List<Booking> getBookings(String customer) {
       /* try {
            this.publisher.createPushSubscriptionExample();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        ArrayList<Booking> customerBookings = new ArrayList<>();
        try{
            for (Booking booking : this.bookings) {
                if (booking.getCustomer().equals(customer)) {
                    customerBookings.add(booking);
                }
            }
            return customerBookings;
        } catch (Error e){
            System.out.println(e.getCause());
            return Collections.EMPTY_LIST;
        }
    }

    public Set<String> getBestCustomers() {

        if (this.bookings.size() == 0) {
            Set<String> dummy= new HashSet<>();
            dummy.add("no bookings");
            return dummy;
        }

        LinkedHashMap<String, Integer> customerTickets = new LinkedHashMap<>();
        try {
            for (Booking booking : this.bookings) {
                String customer = booking.getCustomer();

                if (customerTickets.containsKey(customer)) {
                    customerTickets.put(customer, customerTickets.get(customer) + booking.getTickets().size());
                } else {
                    customerTickets.put(customer, booking.getTickets().size());
                }
            }

            Set<String> bestCustomers = new LinkedHashSet<>();
            int highestTickets = this.bookings.get(0).getTickets().size();

            bestCustomers.add(this.bookings.get(0).getCustomer());

            for (String customer : customerTickets.keySet()) {

                if (customerTickets.get(customer) > highestTickets) {
                    highestTickets = customerTickets.get(customer);
                    bestCustomers.clear();
                    bestCustomers.add(customer);
                } else if (customerTickets.get(customer) == highestTickets) {
                    bestCustomers.add(customer);
                }
            }
            return bestCustomers;
        }
        catch (Error e){
            System.out.println(e.getCause());
            Set<String> dummy= new HashSet<>();
            dummy.add("no bookings");
            return dummy;
        }



    }

    public void confirmQuotes(List<Quote> quotes, String customer) throws ExecutionException, InterruptedException { //hierin pub subben dit zijn allemaal put requests zet in de subriber

        String message = customer+"::::::::"+Serializer.serialize(quotes);
        ByteString data = ByteString.copyFromUtf8(message);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
        ApiFuture<String> messageIdFuture =publisher.publish(pubsubMessage);
        String messageId = messageIdFuture.get();
        System.out.println("Published message ID: " + messageId);
    }
}
