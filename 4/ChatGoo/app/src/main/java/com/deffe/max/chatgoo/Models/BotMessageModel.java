package com.deffe.max.chatgoo.Models;

public class BotMessageModel
{
    private String message;
    private String from;
    private String type;
    private String key;
    private String date;
    private String time;
    private String today_date;

    public BotMessageModel() {
    }

    public BotMessageModel(String message, String from, String type, String key, String date, String time, String today_date) {
        this.message = message;
        this.from = from;
        this.type = type;
        this.key = key;
        this.date = date;
        this.time = time;
        this.today_date = today_date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getToday_date() {
        return today_date;
    }

    public void setToday_date(String today_date) {
        this.today_date = today_date;
    }
}
