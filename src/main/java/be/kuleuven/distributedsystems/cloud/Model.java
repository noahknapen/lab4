package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class Model {

    String RELIABLE_THEATRE_URL = "reliabletheatrecompany.com";
    String API_KEY = "/?key=wCIoTqec6vGJijW2meeqSokanZuqOL";

    @Autowired
    private ApplicationContext context;

    public List<Show> getShows() {
        List<Show> shows = null;

        // WebClient.builder()  context.getBean
        WebClient client = WebClient.create(String.format("https://%s/shows%s", RELIABLE_THEATRE_URL, API_KEY));
        Mono<JSONObject> monoResponse = client.get().accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToMono(JSONObject.class);
        System.out.println("\n[Model] monoResponse: " + monoResponse + "\n" );

        JSONObject jsonResponse = monoResponse.block();
        System.out.println("\n[Model] jsonResponse: " + jsonResponse + "\n" );


//        JSONObject jsonResponse = monoResponse.block();
//        JSONArray jsonShows = (JSONArray) ((JSONObject) jsonResponse.get("_embedded")).get("shows");
//
//        for (Object object : jsonShows) {
//            JSONObject jsonShow = (JSONObject) object;
//            String company = (String) jsonShow.get("company");
//            UUID showId = UUID.fromString((String) jsonShow.get("showId"));
//            String name = (String) jsonShow.get("name");
//            String location = (String) jsonShow.get("name");
//            String image = (String) jsonShow.get("image");
//
//            Show show = new Show(company, showId, name, location, image);
//
//            shows.add(show);
//        }
        return new ArrayList<>();
    }

    public Show getShow(String company, UUID showId) {
        Show show = null;

        for (Show potentialShow : this.getShows()) {

            if (potentialShow.getShowId() == showId && Objects.equals(potentialShow.getCompany(), company)) {
                show = new Show(company, showId, potentialShow.getName(), potentialShow.getLocation(), potentialShow.getImage());
            }
        }
        return show;
    }

    public List<LocalDateTime> getShowTimes(String company, UUID showId) {
        // TODO: return a list with all possible times for the given show
        return new ArrayList<>();
    }

    public List<Seat> getAvailableSeats(String company, UUID showId, LocalDateTime time) {
        // TODO: return all available seats for a given show and time
        return new ArrayList<>();
    }

    public Seat getSeat(String company, UUID showId, UUID seatId) {
        // TODO: return the given seat
        return null;
    }

    public Ticket getTicket(String company, UUID showId, UUID seatId) {
        // TODO: return the ticket for the given seat
        return null;
    }

    public List<Booking> getBookings(String customer) {
        // TODO: return all bookings from the customer
        return new ArrayList<>();
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
