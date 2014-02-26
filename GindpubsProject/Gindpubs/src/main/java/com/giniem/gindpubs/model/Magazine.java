package com.giniem.gindpubs.model;

/**
 * Created by Francisco Contereras on 9/12/13.
 */
public class Magazine {

    private String name;
    private String title;
    private String info;
    private String date;
    private Integer size;
    private String cover;
    private String url;
    private Integer sizeMB;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSizeMB() {
        return sizeMB;
    }

    public void setSizeMB(Integer sizeMB) {
        this.sizeMB = sizeMB;
    }
}
