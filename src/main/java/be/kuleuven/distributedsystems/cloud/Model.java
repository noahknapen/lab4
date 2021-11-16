package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class Model {

    String RELIABLE_THEATRE_URL = "https://reliabletheatrecompany.com/";
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";

    private ApplicationContext context;

    @Autowired
    private WebClient.Builder webClientBuilder;

    public List<Show> getShows() {
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

    public Show getShow(String company, UUID showId) {
        Show show = null;

        for (Show potentialShow : this.getShows()) {
            if (potentialShow.getShowId().equals(showId) && Objects.equals(potentialShow.getCompany(), company)) {
                show = new Show(company, showId, potentialShow.getName(), potentialShow.getLocation(), potentialShow.getImage());
            }
        }
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        // TODO: return a list with all possible times for the given show
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

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        Seat seat;

        // WebClient.builder()  context.getBean
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
                // .getContent();

        // seat = new ArrayList<>(seatCollection);

        return seat;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        Ticket ticket;

        // WebClient.builder()  context.getBean
        ticket = webClientBuilder
                .baseUrl("https://" + company + "/")
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("shows")
                        .pathSegment(showId.toString())
                        .pathSegment("seats")
                        .pathSegment(seatId.toString())
                        .pathSegment("ticket")
                        // .queryParam("time", time.toString())
                        // .queryParam("available", "true")
                        .queryParam("key", API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
                .block();
        // .getContent();

        // seat = new ArrayList<>(seatCollection);

        return ticket;
    }

    public List<Booking> getBookings(String customer) {
        System.out.println(customer);
        Ticket ticket;

        // WebClient.builder()  context.getBean
//        ticket = webClientBuilder
//                .baseUrl("https://" + company + "/")
//                .build()
//                .get()
//                .uri(uriBuilder -> uriBuilder
//                        .pathSegment("shows")
//                        .pathSegment(showId.toString())
//                        .pathSegment("seats")
//                        .pathSegment(seatId.toString())
//                        .pathSegment("ticket")
//                        // .queryParam("time", time.toString())
//                        // .queryParam("available", "true")
//                        .queryParam("key", API_KEY)
//                        .build())
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Ticket>() {})
//                .block();
//        // .getContent();
//
//        // seat = new ArrayList<>(seatCollection);
//
//        return ticket;
        return null;
    }

    public List<Booking> getAllBookings() {
        // TODO: return all bookings
        return new ArrayList<>();
    }

    public Set<String> getBestCustomers() {
        // TODO: return the best customer (highest number of tickets, return all of them if multiple customers have an equal amount)
        return null;
    }

    public void confirmQuotes(List<Quote> quotes, String customer) {
        // TODO: reserve all seats for the given quotes
    }
}
