package com.thomascook.hotelbooking.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Bound from {@code app.pricing.*}; used by the pricing service in Phase 2. */
@ConfigurationProperties(prefix = "app.pricing")
public record PricingProperties(
        @DefaultValue("15") BigDecimal weekendSurchargePercent,
        @DefaultValue("INR") String currency) {
}
