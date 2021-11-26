package be.kuleuven.distributedsystems.cloud.entities;

import be.kuleuven.distributedsystems.cloud.Model;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Serializer{


    public static String serialize(UUID uuid) {

        return String.valueOf(uuid.getLeastSignificantBits())+":"+String.valueOf(uuid.getMostSignificantBits());
    }
    public static String serialize(Quote quote){

        return quote.getCompany()+":"+ quote.getShowId().toString()+":"+quote.getSeatId().toString();
    }
    public static String serialize(List<Quote> quotes){
        String totalsting="";
        for (Quote quote:quotes){
            totalsting+="/////"+serialize(quote);
        }
        return totalsting;
    }

    public static Quote deserializeQuote(String sting){

        String[] snipped=sting.split(":");

        Quote quote=new Quote(snipped[0],new UUID(new Long(snipped[1]) ,new Long(snipped[2])),new UUID(new Long(snipped[3]) ,new Long(snipped[4])));
        return quote;
        }

    public static List<String> deserializeListQuote(String string){
        string=string.substring(5,string.length());

        List<String> wordList = Arrays.asList(string.split("/////"));
        return wordList;
    }


}
