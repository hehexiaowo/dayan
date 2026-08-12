package com.dayan.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** client 端评价入参（sessionCode 走路径，不在此 DTO）。 */
@Data
public class ClientEvaluationDTO {
    @Min(value = 1) @Max(value = 5) private Integer attitudeRating;
    @Min(value = 1) @Max(value = 5) private Integer professionalRating;
    @Min(value = 1) @Max(value = 5) private Integer responsivenessRating;
    @Min(value = 1) @Max(value = 5) private Integer satisfactionRating;
    private String content;
}
