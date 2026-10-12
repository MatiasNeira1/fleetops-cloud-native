package com.fleetops.flota.api.response;

import java.math.BigDecimal;

public record PlaceResponse(
    String name,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
