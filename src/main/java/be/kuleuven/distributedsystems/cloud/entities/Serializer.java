package be.kuleuven.distributedsystems.cloud.entities;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Serializer{


    public static String serialize(UUID uuid) {
        System.out.println("UUID = "+String.valueOf(uuid.getLeastSignificantBits())+":"+String.valueOf(uuid.getMostSignificantBits()));
        return String.valueOf(uuid.getLeastSignificantBits())+":"+String.valueOf(uuid.getMostSignificantBits());
    }
    public static String serialize(Quote quote){
        System.out.println("Quote = "+quote.getCompany()+":"+serialize(quote.getShowId())+":"+serialize(quote.getSeatId()));
        return quote.getCompany()+":"+serialize(quote.getShowId())+":"+serialize(quote.getSeatId());
    }
    public static String serialize(List<Quote> quotes){
        String totalsting="";
        for (Quote quote:quotes){
            totalsting+="//"+serialize(quote);
        }
        return totalsting;
    }
    public static Quote deserializeQuote(String sting){
        System.out.println("Quote to DEserialize: "+sting);
        String[] snipped=sting.split(":");
        System.out.println(new Long(snipped[1]) + snipped[2]);
        Quote quote=new Quote(snipped[0],new UUID(new Long(snipped[1]) ,new Long(snipped[2])),new UUID(new Long(snipped[3]) ,new Long(snipped[4])));
        return quote;
        }

    public static List<Quote> deserializeListQuote(String string){
        string=string.substring(2,string.length());
        String[] snipped=string.split("//");

        ArrayList<Quote> quotes=new ArrayList<>();
        for (String sting:snipped){
            quotes.add(deserializeQuote(sting));

        }
        return quotes;
    }


}
