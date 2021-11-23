package be.kuleuven.distributedsystems.cloud.controller;

import be.kuleuven.distributedsystems.cloud.Model;
import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class APIController {
    private final Model model;
    private Booking booking;//deze booking moet aangemaakt worden door confirmQQuote
    @Autowired
    public APIController(Model model) {
        this.model = model;
    }

    @PostMapping(path = "/addToCart", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    public ResponseEntity<Void> addToCart(
            //add ticket to booking
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.add(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/removeFromCart")
    public ResponseEntity<Void> removeFromCart(
            //removeticket from booking
            @ModelAttribute Quote quote,
            @RequestHeader(value = "referer") String referer,
            @CookieValue(value = "cart", required = false) String cartString) {
        List<Quote> cart = Cart.fromCookie(cartString);
        cart.remove(quote);
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, referer);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @PostMapping("/confirmCart")
    public ResponseEntity<Void> confirmCart(
            //write Booking to confirmed bookings
            @CookieValue(value = "cart", required = false) String cartString) throws Exception {
        List<Quote> cart = Cart.fromCookie(cartString);
        this.model.confirmQuotes(new ArrayList<>(cart), AuthController.getUser().getEmail());
        cart.clear();
        ResponseCookie cookie = Cart.toCookie(cart);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        headers.add(HttpHeaders.LOCATION, "/account");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
