package com.thomascook.hotelbooking.service;

import com.thomascook.hotelbooking.dto.response.BookingResponse;
import com.thomascook.hotelbooking.dto.response.NightlyPriceLine;
import com.thomascook.hotelbooking.dto.response.PriceBreakdownResponse;
import com.thomascook.hotelbooking.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Renders the printable booking voucher as one self-contained HTML document (inline CSS, no external assets).
 * It is built from the same BookingResponse the UI shows, so the numbers always match. Every dynamic value is escaped.
 */
@Service
public class VoucherService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private static final String STYLE = """
            body{font-family:Arial,Helvetica,sans-serif;color:#1f2933;background:#f3f4f6;margin:0;padding:24px}
            .voucher{max-width:760px;margin:0 auto;background:#fff;border:1px solid #d9dde3;border-radius:8px;padding:32px;position:relative}
            h1{margin:0;font-size:22px} h2{font-size:14px;text-transform:uppercase;letter-spacing:.06em;color:#52606d;margin:28px 0 8px}
            .head{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #1f2933;padding-bottom:16px}
            .ref{font-size:20px;font-weight:bold;letter-spacing:.04em}
            .badge{display:inline-block;padding:3px 10px;border-radius:12px;font-size:12px;font-weight:bold}
            .confirmed{background:#d1fadf;color:#05603a} .cancelled{background:#fee4e2;color:#b42318}
            .grid{display:grid;grid-template-columns:1fr 1fr;gap:6px 24px} .label{color:#52606d;font-size:12px}
            table{width:100%;border-collapse:collapse;font-size:14px} th,td{padding:8px;border-bottom:1px solid #e4e7eb;text-align:left}
            th.num,td.num{text-align:right} .weekend{background:#fff8e6} .total td{font-weight:bold;border-top:2px solid #1f2933;font-size:16px}
            .banner{background:#fee4e2;color:#b42318;padding:10px 14px;border-radius:6px;margin-top:16px;font-weight:bold}
            .note{font-size:12px;color:#52606d;margin-top:24px} .actions{max-width:760px;margin:0 auto 12px;text-align:right}
            button{padding:8px 16px;font-size:14px;cursor:pointer}
            @media print{body{background:#fff;padding:0}.voucher{border:none;padding:0}.actions{display:none}}
            """;

    public String render(BookingResponse booking) {
        PriceBreakdownResponse price = booking.price();
        boolean cancelled = booking.status() == BookingStatus.CANCELLED;
        String statusClass = cancelled ? "cancelled" : "confirmed";

        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Booking Voucher ").append(esc(booking.bookingReference())).append("</title>")
                .append("<style>").append(STYLE).append("</style></head><body>")
                .append("<div class=\"actions\"><button onclick=\"window.print()\">Print / Save as PDF</button></div>")
                .append("<div class=\"voucher\">");

        html.append("<div class=\"head\"><div><h1>Booking Confirmation Voucher</h1>")
                .append("<div class=\"label\">Present this voucher at check-in</div></div>")
                .append("<div style=\"text-align:right\"><div class=\"ref\">").append(esc(booking.bookingReference())).append("</div>")
                .append("<span class=\"badge ").append(statusClass).append("\">").append(booking.status()).append("</span></div></div>");

        if (cancelled && booking.cancelledAt() != null) {
            html.append("<div class=\"banner\">This booking was cancelled on ")
                    .append(DATE_TIME.format(booking.cancelledAt())).append(". This voucher is no longer valid.</div>");
        }

        html.append("<h2>Hotel</h2><div class=\"grid\">")
                .append(field("Hotel", booking.hotelName()))
                .append(field("Location", booking.hotelLocation()))
                .append(field("Room type", booking.roomType()))
                .append(field("Rooms", "1"))
                .append("</div>");

        html.append("<h2>Guest and stay</h2><div class=\"grid\">")
                .append(field("Guest name", booking.guestName()))
                .append(field("Guest email", booking.guestEmail()))
                .append(field("Check-in", DATE.format(booking.checkInDate())))
                .append(field("Check-out", DATE.format(booking.checkOutDate())))
                .append(field("Nights", String.valueOf(booking.nights())))
                .append(field("Booked on", DATE_TIME.format(booking.createdAt())))
                .append("</div>");

        html.append("<h2>Price breakdown</h2><table><thead><tr><th>Night</th><th class=\"num\">Rate</th>")
                .append("<th class=\"num\">Weekend surcharge</th><th class=\"num\">Amount</th></tr></thead><tbody>");
        for (NightlyPriceLine night : price.nightlyBreakdown()) {
            html.append("<tr").append(night.weekendNight() ? " class=\"weekend\"" : "").append("><td>")
                    .append(DATE.format(night.date())).append("</td><td class=\"num\">")
                    .append(money(price, night.basePrice())).append("</td><td class=\"num\">")
                    .append(night.weekendNight() ? "+" + money(price, night.surcharge()) : "-")
                    .append("</td><td class=\"num\">").append(money(price, night.price())).append("</td></tr>");
        }
        html.append("<tr><td colspan=\"3\">Room charges (").append(price.nights()).append(" x ")
                .append(money(price, price.pricePerNight())).append(")</td><td class=\"num\">")
                .append(money(price, price.baseAmount())).append("</td></tr>")
                .append("<tr><td colspan=\"3\">Weekend surcharge (").append(price.weekendSurchargePercent().stripTrailingZeros().toPlainString())
                .append("% on ").append(price.weekendNights()).append(" Fri/Sat night(s))</td><td class=\"num\">")
                .append(money(price, price.surchargeAmount())).append("</td></tr>")
                .append("<tr class=\"total\"><td colspan=\"3\">Total amount</td><td class=\"num\">")
                .append(money(price, price.totalAmount())).append("</td></tr></tbody></table>");

        html.append("<p class=\"note\">Friday and Saturday nights carry a weekend surcharge. Please carry a valid photo ID. ")
                .append("Quote your booking reference for any change or cancellation.</p>")
                .append("</div></body></html>");
        return html.toString();
    }

    private static String field(String label, String value) {
        return "<div><div class=\"label\">" + esc(label) + "</div><div>" + esc(value) + "</div></div>";
    }

    private static String money(PriceBreakdownResponse price, BigDecimal amount) {
        return esc(price.currency()) + "&nbsp;" + String.format(Locale.ENGLISH, "%,.2f", amount);
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
