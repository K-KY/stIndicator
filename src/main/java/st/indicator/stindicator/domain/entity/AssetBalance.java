package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;

public class AssetBalance {
    private final String asset;
    private final BigDecimal walletBalance;
    private final BigDecimal availableBalance;
    private final BigDecimal unrealizedProfit;

    public AssetBalance(String asset, BigDecimal walletBalance, BigDecimal availableBalance, BigDecimal unrealizedProfit) {
        this.asset = asset;
        this.walletBalance = walletBalance;
        this.availableBalance = availableBalance;
        this.unrealizedProfit = unrealizedProfit;
    }

    public String getAsset() {
        return asset;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getUnrealizedProfit() {
        return unrealizedProfit;
    }
}
