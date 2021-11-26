package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.databind.JsonSerializer;
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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.checkerframework.checker.units.qual.A;
import org.eclipse.jetty.util.ajax.JSONPojoConvertor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class Model {

    String RELIABLE_THEATRE_URL = "https://reliabletheatrecompany.com/";
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

    public List<Show> getShows() {//skip
        List<Show> shows = null;

        // WebClient.builder()  context.getBean
        Collection<Show> showsCollection = webClientBuilder
                .baseUrl(RELIABLE_THEATRE_URL)
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

    public Show getShow(String company, UUID showId) {//skip
        Show show = null;

        for (Show potentialShow : this.getShows()) {
            if (potentialShow.getShowId().equals(showId) && Objects.equals(potentialShow.getCompany(), company)) {
                show = new Show(company, showId, potentialShow.getName(), potentialShow.getLocation(), potentialShow.getImage());
            }
        }
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {//ni doen
        List<LocalDateTime> times;

        // WebClient.builder()  context.getBean
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

    public Seat getSeat(String company, UUID showId, UUID seatId) { //googlefiene
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
                        // .queryParam("time", time.toString())
                        // .queryParam("available", "true")
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

        // try {
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
                        .queryParam("key", API_KEY)
                        .queryParam("customer", customer)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .block();
        // }
        /*catch (WebClientResponseException e) {
            e.printStackTrace();
            System.out.println("Ticket has been stolen");
            ticket = new Ticket();
        }*/

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
        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
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
        //dit maakt een booking save list of bookings univailble all those seats is tthis an hardcoded database?
        /*Quote quote=new Quote("ik.com",new UUID(1L,1L),new UUID(2L,2L));
        String str= Serializer.serialize(quote);
        System.out.println(str);
        Quote quote1=Serializer.deserializeQuote(str);
        System.out.println(quote1.getSeatId().toString());
        System.out.println(2L);
        quotes.set(0, quote);*/

        String message = customer+"::::::::"+Serializer.serialize(quotes);
        ByteString data = ByteString.copyFromUtf8(message);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
        ApiFuture<String> messageIdFuture =publisher.publish(pubsubMessage);
        String messageId = messageIdFuture.get();
        System.out.println("Published message ID: " + messageId);
//        ArrayList<Ticket> tickets= new ArrayList<>();
//        for (Quote quote:quotes){
//            UUID show= quote.getShowId();
//            UUID seat= quote.getSeatId();
//            String company = quote.getCompany();
//
//            Ticket ticket= putTicket(company, show, seat, customer);
//            tickets.add(ticket);
//        }
//        Booking booking= new Booking(UUID.randomUUID(),
//                LocalDateTime.now(),
//                tickets,
//                customer
//        );
//        bookings.add(booking);
//        // TODO: reserve all seats for the given quotes
    }
}
