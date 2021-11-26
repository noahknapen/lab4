package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@RestController
public class ViewController {
    private final Model model;
    private final JsonParser jsonParser = new JsonParser();
    private final int MAX_RETRIES = 10;
    private int RETRIES = 10;

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

        while (true) {
            try {
                modelAndView.addObject("shows", this.model.getShows());
                this.RETRIES = this.MAX_RETRIES;
                return modelAndView;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
                }
            }
        }
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

        while (true) {
            try {
                modelAndView.addObject("show",
                        this.model.getShow(company, showId));
                modelAndView.addObject("showTimes",
                        this.model.getShowTimes(company, showId)
                                .stream()
                                .sorted()
                                .collect(Collectors.toList()));

                this.RETRIES = this.MAX_RETRIES;
                return modelAndView;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
                }
            }
        }
    }

    @ResponseStatus(value = HttpStatus.ACCEPTED)
    @PostMapping("/subscription")
    public ResponseEntity<Void> putTickets(@RequestBody String body) {

        JsonElement jsonRoot = jsonParser.parse(body);
        String messageStr = jsonRoot.getAsJsonObject().get("message").getAsJsonObject().get("data").getAsString();

        String decoded = decode(messageStr);
        String[] snipped= decoded.split("::::::::");
        String customer=snipped[0];
        List<String> quotes=Serializer.deserializeListQuote(snipped[1]);

        ArrayList<Ticket> tickets= new ArrayList<>();

        boolean success = false;

        while (!success)
            try {
                for (String quote : quotes) {

                    String[] snipped2=quote.split(":");
                    Ticket ticket = model.putTicket(snipped2[0], snipped2[1], snipped2[2], customer);
                    tickets.add(ticket);
                    success = true;
                    this.RETRIES = this.MAX_RETRIES;
                }
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return ResponseEntity.ok().build();
                }
            }

        if (tickets.size() != quotes.size()) {
            // some tickets didn't get registered so abort booking
            return ResponseEntity.ok().build();
        }

        Booking booking = this.model.createBooking(UUID.randomUUID(), LocalDateTime.now(), tickets, customer);
        this.model.registerBooking(booking);

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

        while (true) {
            try {
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

                this.RETRIES = this.MAX_RETRIES;
                return modelAndView;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
                }
            }
        }
    }

    @GetMapping("/cart")
    public ModelAndView viewCart(
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> quotes = Cart.fromCookie(cartString);
        ModelAndView modelAndView = new ModelAndView("cart");
        modelAndView.addObject("cartLength",
                Integer.toString(quotes.size()));
        modelAndView.addObject("manager", AuthController.getUser().isManager());

        boolean success = false;
        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();

        while (!success) {
            try {
                for (var q : quotes) {
                    if (!shows.containsKey(q.getShowId())) {
                        shows.put(q.getShowId(), this.model.getShow(q.getCompany(), q.getShowId()));
                    }
                    if (!seats.containsKey(q.getSeatId())) {
                        seats.put(q.getSeatId(), this.model.getSeat(q.getCompany(), q.getShowId(), q.getSeatId()));
                    }
                }
                this.RETRIES = this.MAX_RETRIES;
                success = true;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
                }
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

        boolean success = false;
        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();

        while (!success) {
            try {
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
                this.RETRIES = this.MAX_RETRIES;
                success = true;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
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

        boolean success = false;
        var shows = new HashMap<UUID, Show>();
        var seats = new HashMap<UUID, Seat>();

        while (!success) {
            try {
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
                success = true;
            } catch (WebClientResponseException e) {
                if (this.RETRIES > 0) {
                    this.RETRIES -= 1;
                } else {
                    return modelAndView;
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
