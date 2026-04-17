package id.ac.ui.cs.advprog.mysawit.harvest.dto;

public class ApiSuccessResponse<T> {

    private String status;
    private T data;

    public ApiSuccessResponse(String status, T data) {
        this.status = status;
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}