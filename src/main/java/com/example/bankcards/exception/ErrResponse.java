package com.example.bankcards.exception;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrResponse {
    public String error;
    public String message;
    public int statusCode;
    public String timestamp;
}
