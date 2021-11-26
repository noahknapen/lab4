package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.firebase.messaging.Message;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.pubsub.v1.PubsubMessage;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.SerializationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@RestController
public class ViewController {
    private final Model model;
    private final Gson gson = new Gson();
    private final JsonParser jsonParser = new JsonParser();

    @Autowired
    public ViewController(Model model) {
        this.model = model;
    }

    @GetMapping("/_ah/warmup")
    public void warmup() {
    }

    @GetMapping({"/", "/shows"})
    public ModelAndView viewShows(
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("shows");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());
        modelAndView.addObject("shows", this.model.getShows());
        return modelAndView;
    }

    @GetMapping("/shows/{company}/{showId}")
    public ModelAndView viewShowTimes(
            @PathVariable String company,
            @PathVariable UUID showId,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("show_times");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());
        modelAndView.addObject("show",
                this.model.getShow(company, showId));
        modelAndView.addObject("showTimes",
                this.model.getShowTimes(company, showId)
                        .stream()
                        .sorted()
                        .collect(Collectors.toList()));
        return modelAndView;
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/subscription")
    public ResponseEntity<Void> puttickets(@RequestBody String body){

        JsonElement jsonRoot = jsonParser.parse(body);
        String messageStr = jsonRoot.getAsJsonObject().get("message").getAsJsonObject().get("data").getAsString();
        System.out.println(messageStr);

        String decoded = decode(messageStr);
        String[] snipped= decoded.split("::::::::");
        String customer=snipped[0];
        List<Quote> quotes=Serializer.deserializeListQuote(snipped[1]);
        for (Quote quote:quotes){
            System.out.println("COMPANYY "+quote.getCompany());
        }
        System.out.println("DECODED "+decoded);
        System.out.println(body);
        ArrayList<Ticket> tickets= new ArrayList<>();

        try {
            for (Quote quote : quotes) {
                UUID show = quote.getShowId();
                UUID seat = quote.getSeatId();
                String company = quote.getCompany();

                Ticket ticket = model.putTicket(company, show, seat, customer);
                tickets.add(ticket);
            }
        } catch (WebClientResponseException e) {
            // One of the tickets was stolen, so do not make a booking
            return ResponseEntity.ok().build();
        }

        Booking booking = this.model.createBooking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer);
        this.model.registerBooking(booking);

        // all or nothing semantics: create all bookings, but only register them if they are all successfull

        return ResponseEntity.ok().build();
    }

    private String decode(String data) {
        return new String(Base64.getDecoder().decode(data));
    }

    @GetMapping("/shows/{company}/{showId}/{time}")
    public ModelAndView viewShowSeats(
            @PathVariable String company,
            @PathVariable UUID showId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime time,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("show_seats");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());
        modelAndView.addObject("show",
                this.model.getShow(company, showId));
        modelAndView.addObject("time",
                time.format(DateTimeFormatter.ofPattern("d MMM uuuu  H:mm")));
        modelAndView.addObject("seats",
                this.model.getAvailableSeats(company, showId, time)
                        .stream()
                        .filter(seat -> quotes.stream()
                                .noneMatch(quote -> quote.equals(new Quote(seat.getCompany(), seat.getShowId(), seat.getSeatId()))))
                        .sorted(Comparator.comparing(Seat::getType)
                                .thenComparing(seat -> seat.getName().substring(0, 1))
                                .thenComparing(seat -> Integer.parseInt(seat.getName().substring(1))))
                        .collect(Collectors.groupingBy(Seat::getType)));
        return modelAndView;
    }

    @GetMapping("/cart")
    public ModelAndView viewCart(
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("cart");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());

        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();
        for (var q : quotes) {
            if (!shows.containsKey(q.getShowId())) {
                shows.put(q.getShowId(), this.model.getShow(q.getCompany(), q.getShowId()));
            }
            if (!seats.containsKey(q.getSeatId())) {
                seats.put(q.getSeatId(), this.model.getSeat(q.getCompany(), q.getShowId(), q.getSeatId()));
            }
        }

        modelAndView.addObject("quotes", quotes);
        modelAndView.addObject("shows", shows);
        modelAndView.addObject("seats", seats);
        return modelAndView;
    }

    @GetMapping("/account")
    public ModelAndView viewAccount(
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("account");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());
        var bookings = this.model.getBookings(AuthController.getUser().getEmail());

        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();
        for (var b : bookings) {
            for (var t : b.getTickets()) {
                if (!shows.containsKey(t.getShowId())) {
                    shows.put(t.getShowId(), this.model.getShow(t.getCompany(), t.getShowId()));
                }
                if (!seats.containsKey(t.getSeatId())) {
                    seats.put(t.getSeatId(), this.model.getSeat(t.getCompany(), t.getShowId(), t.getSeatId()));
                }
            }
        }

        modelAndView.addObject("bookings", bookings);
        modelAndView.addObject("seats", seats);
        modelAndView.addObject("shows", shows);
        return modelAndView;
    }

    @GetMapping("/manager")
    public ModelAndView viewManager(
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {
        if (!AuthController.getUser().isManager()) {
            return viewShows(cartString);
        }

        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("manager");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());
        var bookings = this.model.getAllBookings();

        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();
        for (var b : bookings) {
            for (var t : b.getTickets()) {
                if (!shows.containsKey(t.getShowId())) {
                    shows.put(t.getShowId(), this.model.getShow(t.getCompany(), t.getShowId()));
                }
                if (!seats.containsKey(t.getSeatId())) {
                    seats.put(t.getSeatId(), this.model.getSeat(t.getCompany(), t.getShowId(), t.getSeatId()));
                }
            }
        }

        modelAndView.addObject("bookings", bookings);
        modelAndView.addObject("seats", seats);
        modelAndView.addObject("shows", shows);
        modelAndView.addObject("bestCustomers", this.model.getBestCustomers());
        return modelAndView;
    }
}
