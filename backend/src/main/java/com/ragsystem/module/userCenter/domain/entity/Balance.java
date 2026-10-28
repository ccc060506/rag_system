package com.ragsystem.module.userCenter.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Balance {
    private boolean is_available;
    private List<BalanceInfo> balance_infos;

    @Data
    public static class BalanceInfo {
        private String currency;
        private String total_balance;
        private String granted_balance;
        private String topped_up_balance;
    }
}
