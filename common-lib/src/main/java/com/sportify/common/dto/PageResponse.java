package com.sportify.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean last;
    private String sortBy;
    private String sortDir;

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / (size > 0 ? size : 1));
        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .last(page >= totalPages - 1 || totalPages == 0)
                .build();
    }

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalItems, String sortBy, String sortDir) {
        int totalPages = (int) Math.ceil((double) totalItems / (size > 0 ? size : 1));
        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .last(page >= totalPages - 1 || totalPages == 0)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }
}
