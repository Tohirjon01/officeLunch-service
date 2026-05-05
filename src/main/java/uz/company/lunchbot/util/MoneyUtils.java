package uz.company.lunchbot.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static String formatUzs(BigDecimal amount) {
        if (amount == null) {
            return "0 UZS";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount) + " UZS";
    }

    public static String formatSignedUzs(BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return "0 UZS";
        }
        String prefix = amount.signum() > 0 ? "+" : "-";
        return prefix + formatUzs(amount.abs());
    }
}
