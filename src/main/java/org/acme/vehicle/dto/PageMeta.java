package org.acme.vehicle.dto;

public class PageMeta {
    public int page;
    public int limit;
    public long total;

    public PageMeta() {}

    public PageMeta(int page, int limit, long total) {
        this.page = page;
        this.limit = limit;
        this.total = total;
    }
}

