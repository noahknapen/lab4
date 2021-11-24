package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class Model {

    String RELIABLE_THEATRE_URL = "https://reliabletheatrecompany.com/";
    String API_KEY = "wCIoTqec6vGJijW2meeqSokanZuqOL";
    ArrayList<Booking> bookings = new ArrayList<>();

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

    public Ticket getTicket(String company, UUID showId, UUID seatId, String customer) {
        Ticket ticket;

        // WebClient.builder()  context.getBean
        try {
            ticket = webClientBuilder
                    .baseUrl("https://" + company + "/")
                    .build()
                    .put()
                    .uri(uriBuilder -> uriBuilder
                            .pathSegment("shows")
                            .pathSegment(showId.toString())
                            .pathSegment("seats")
                            .pathSegment(seatId.toString())
                            .pathSegment("ticket")
                            // .queryParam("time", time.toString())
                            // .queryParam("available", "true")
                            .queryParam("key", API_KEY)
                            .queryParam("customer", customer)
                            .build())
                    .retrieve()
                    .bodyToMono(Ticket.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.out.println("Ticket has been stolen");
            ticket = new Ticket();
        }
        // .getContent();

        // seat = new ArrayList<>(seatCollection);

        return ticket;
    }

    public List<Booking> getAllBookings() {

        return bookings;}


    public List<Booking> getBookings(String customer) {
        ArrayList<Booking> customerBookings = new ArrayList<>();
        try{
            for (Booking booking : this.bookings) {
                if (booking.getCustomer().equals(customer)) {
                    customerBookings.add(booking);
                }
            }
            return customerBookings;
        }


        catch (Error e){
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

    public void confirmQuotes(List<Quote> quotes, String customer) {
        //dit maakt een booking save list of bookings univailble all those seats is tthis an hardcoded database?
        ArrayList<Ticket> tickets= new ArrayList<>();
        for (Quote quote:quotes){
            UUID show= quote.getShowId();
            UUID seat= quote.getSeatId();
            String company = quote.getCompany();

            Ticket ticket= getTicket(company, show, seat, customer);
            tickets.add(ticket);
        }
        Booking booking= new Booking(UUID.randomUUID(),
                LocalDateTime.now(),
                tickets,
                customer
        );
        bookings.add(booking);
    }
}
