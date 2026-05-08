package com.trip.vo.response;

import java.util.List;

/**
 * 统一分页响应对象。
 *
 * @param <T> 列表元素类型
 */
public class PageResultVO<T> {

    private List<T> list;
    private long pageNum;
    private long pageSize;
    private long total;
    private long pages;

    public static <T> PageResultVO<T> of(List<T> list, long pageNum, long pageSize, long total, long pages) {
        PageResultVO<T> vo = new PageResultVO<>();
        vo.setList(list);
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        vo.setTotal(total);
        vo.setPages(pages);
        return vo;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getPageNum() {
        return pageNum;
    }

    public void setPageNum(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }
}
