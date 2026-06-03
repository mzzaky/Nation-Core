package id.nationcore.placeholders;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.bukkit.OfflinePlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import id.nationcore.NationCore;
import id.nationcore.models.CabinetDecision;
import id.nationcore.models.ExecutiveOrder;
import id.nationcore.models.ExecutiveOrder.ExecutiveOrderType;
import id.nationcore.models.Government.CabinetPosition;
import id.nationcore.models.TaxRecord.PlayerTaxData;

public class NationExpansion extends PlaceholderExpansion {

    private final NationCore plugin;

    public NationExpansion(NationCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return "NationCore Team";
    }

    @Override
    public String getIdentifier() {
        return "nation";
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // === GOVERNMENT PLACEHOLDERS ===
        if (params.equalsIgnoreCase("president")) {
            String name = plugin.getGovernmentManager().getGovernment().getPresidentName();
            return name != null ? name : "None";
        }

        if (params.equalsIgnoreCase("term_time")) {
            long time = plugin.getGovernmentManager().getTermRemainingTime();
            return formatTime(time);
        }

        if (params.equalsIgnoreCase("approval_rating")) {
            return String.format("%.1f", plugin.getGovernmentManager().getGovernment().getCurrentApprovalRating());
        }

        if (params.equalsIgnoreCase("consecutive_terms")) {
            return String.valueOf(plugin.getGovernmentManager().getGovernment().getConsecutiveTerms());
        }

        if (params.equalsIgnoreCase("salary_payouts")) {
            return String.format("%.2f", plugin.getGovernmentManager().getGovernment().getTotalSalaryPayouts());
        }

        // Cabinet member placeholders
        if (params.equalsIgnoreCase("defense_minister")) {
            return getCabinetMemberName(CabinetPosition.DEFENSE);
        }

        if (params.equalsIgnoreCase("treasury_minister")) {
            return getCabinetMemberName(CabinetPosition.TREASURY);
        }



        if (params.equalsIgnoreCase("president_uuid")) {
            java.util.UUID uuid = plugin.getGovernmentManager().getGovernment().getPresidentUUID();
            return uuid != null ? uuid.toString() : "None";
        }

        if (params.equalsIgnoreCase("president_online")) {
            java.util.UUID uuid = plugin.getGovernmentManager().getGovernment().getPresidentUUID();
            if (uuid == null)
                return "No";
            return org.bukkit.Bukkit.getPlayer(uuid) != null ? "Yes" : "No";
        }

        if (params.equalsIgnoreCase("cabinet_count")) {
            return String.valueOf(plugin.getGovernmentManager().getGovernment().getCabinet().size());
        }

        if (params.equalsIgnoreCase("cabinet_salary_total")) {
            return String.format("%.2f", plugin.getGovernmentManager().getGovernment().getTotalSalaryPayouts());
        }

        // === EXECUTIVE ORDER PLACEHOLDERS ===
        var eoManager = plugin.getExecutiveOrderManager();

        if (params.equalsIgnoreCase("order_active")) {
            List<ExecutiveOrder> orders = eoManager.getActiveOrders();
            return orders.isEmpty() ? "No" : "Yes";
        }

        if (params.equalsIgnoreCase("order_count")) {
            return String.valueOf(eoManager.getActiveOrders().size());
        }

        if (params.equalsIgnoreCase("order_name")) {
            List<ExecutiveOrder> orders = eoManager.getActiveOrders();
            if (orders.isEmpty())
                return "None";
            return orders.get(0).getType().getDisplayName();
        }

        if (params.equalsIgnoreCase("order_time")) {
            List<ExecutiveOrder> orders = eoManager.getActiveOrders();
            if (orders.isEmpty())
                return "0s";
            return formatTime(orders.get(0).getRemainingTime());
        }

        if (params.equalsIgnoreCase("order_effect")) {
            List<ExecutiveOrder> orders = eoManager.getActiveOrders();
            if (orders.isEmpty())
                return "None";
            return orders.get(0).getType().getEffectDescription();
        }

        if (params.equalsIgnoreCase("order_cooldown")) {
            long remaining = eoManager.getOrderCooldownRemaining(null);
            return remaining <= 0 ? "Ready" : formatTime(remaining);
        }



        if (params.equalsIgnoreCase("order_purge_active")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            return (onlineP != null && eoManager.isPurgeActive(onlineP)) ? "Yes" : "No";
        }

        if (params.equalsIgnoreCase("order_xp_multiplier")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            return onlineP != null ? String.format("%.2f", eoManager.getXPMultiplier(onlineP)) : "1.00";
        }

        if (params.equalsIgnoreCase("order_vault_multiplier")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            return onlineP != null ? String.format("%.2f", eoManager.getVaultMultiplier(onlineP)) : "1.00";
        }

        if (params.equalsIgnoreCase("order_farming_multiplier")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            return onlineP != null ? String.format("%.2f", eoManager.getFarmingMultiplier(onlineP)) : "1.00";
        }

        if (params.equalsIgnoreCase("order_pvp_damage_multiplier")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            return onlineP != null ? String.format("%.2f", eoManager.getPvPDamageMultiplier(onlineP)) : "1.00";
        }

        if (params.equalsIgnoreCase("order_shop_discount")) {
            org.bukkit.entity.Player onlineP = (player != null) ? player.getPlayer() : null;
            double discount = onlineP != null ? eoManager.getShopDiscount(onlineP) : 0.0;
            return discount > 0 ? String.format("%.0f%%", discount * 100) : "0%";
        }

        // Specific executive order status checkers (%nation_order_GOLDEN_AGE%, etc.)
        for (ExecutiveOrderType type : ExecutiveOrderType.values()) {
            String key = "order_" + type.name().toLowerCase();
            if (params.equalsIgnoreCase(key)) {
                return eoManager.isOrderActive(type) ? "Yes" : "No";
            }
            String timeKey = "order_" + type.name().toLowerCase() + "_time";
            if (params.equalsIgnoreCase(timeKey)) {
                ExecutiveOrder order = eoManager.getActiveOrder(type);
                return order != null ? formatTime(order.getRemainingTime()) : "0s";
            }
        }

        // === CABINET MINISTER ORDER (DECISION) PLACEHOLDERS ===
        var cabinetManager = plugin.getCabinetManager();

        if (params.equalsIgnoreCase("minister_order_active")) {
            return cabinetManager.getAllActiveDecisions().isEmpty() ? "No" : "Yes";
        }

        if (params.equalsIgnoreCase("minister_order_count")) {
            return String.valueOf(cabinetManager.getAllActiveDecisions().size());
        }

        if (params.equalsIgnoreCase("minister_order_drop_multiplier")) {
            return String.format("%.2f", cabinetManager.getGlobalDropMultiplier());
        }

        if (params.equalsIgnoreCase("minister_order_xp_multiplier")) {
            return String.format("%.2f", cabinetManager.getGlobalXpMultiplier());
        }

        if (params.equalsIgnoreCase("minister_order_tax_holiday")) {
            return cabinetManager.isTaxHolidayActive() ? "Yes" : "No";
        }



        // Per-minister active decision name and time
        // (%nation_minister_defense_order%, etc.)
        for (CabinetDecision.CabinetPosition pos : CabinetDecision.CabinetPosition.values()) {
            String posKey = "minister_" + pos.name().toLowerCase() + "_order";
            if (params.equalsIgnoreCase(posKey)) {
                var decisions = cabinetManager.getActiveDecisionsByPosition(pos);
                if (decisions == null || decisions.isEmpty())
                    return "None";
                return decisions.get(0).getType().getDisplayName();
            }
            String posTimeKey = "minister_" + pos.name().toLowerCase() + "_order_time";
            if (params.equalsIgnoreCase(posTimeKey)) {
                var decisions = cabinetManager.getActiveDecisionsByPosition(pos);
                if (decisions == null || decisions.isEmpty())
                    return "0s";
                return formatTime(decisions.get(0).getRemainingTime());
            }
        }

        // === ELECTION PLACEHOLDERS ===
        if (params.equalsIgnoreCase("phase")) {
            if (plugin.getElectionManager().isElectionActive()) {
                return plugin.getElectionManager().getElection().getCurrentPhase().getDisplayName();
            }
            return "None";
        }

        if (params.equalsIgnoreCase("phase_time")) {
            long time = plugin.getElectionManager().getPhaseRemainingTime();
            return formatTime(time);
        }

        if (params.equalsIgnoreCase("candidates_count")) {
            return String.valueOf(plugin.getElectionManager().getElection().getCandidates().size());
        }

        if (params.equalsIgnoreCase("total_votes")) {
            return String.valueOf(plugin.getElectionManager().getElection().getTotalVotes());
        }

        if (params.equalsIgnoreCase("election_active")) {
            return plugin.getElectionManager().isElectionActive() ? "Yes" : "No";
        }

        // === TREASURY PLACEHOLDERS ===
        if (params.equalsIgnoreCase("treasury")) {
            return String.format("%.2f", plugin.getTreasuryManager().getBalance());
        }

        if (params.equalsIgnoreCase("treasury_income")) {
            return String.format("%.2f", plugin.getTreasuryManager().getTotalIncome());
        }

        if (params.equalsIgnoreCase("treasury_expenses")) {
            return String.format("%.2f", plugin.getTreasuryManager().getTotalExpenses());
        }

        // === TAX SYSTEM PLACEHOLDERS (Global) ===
        var taxManager = plugin.getTaxManager();
        var taxRecord = taxManager.getTaxRecord();

        if (params.equalsIgnoreCase("tax_enabled")) {
            return taxManager.isEnabled() ? "Yes" : "No";
        }

        if (params.equalsIgnoreCase("tax_amount")) {
            return String.format("%.2f", taxManager.getTaxAmount());
        }

        if (params.equalsIgnoreCase("tax_interval")) {
            return "24";
        }

        if (params.equalsIgnoreCase("tax_next_collection")) {
            return formatTime(taxManager.getTimeUntilNextCollection());
        }

        if (params.equalsIgnoreCase("tax_total_collected")) {
            return String.format("%.2f", taxRecord.getTotalTaxCollected());
        }

        if (params.equalsIgnoreCase("tax_cycle_count")) {
            return String.valueOf(taxRecord.getTotalCollectionCycles());
        }

        if (params.equalsIgnoreCase("tax_debtor_count")) {
            return String.valueOf(taxManager.getDebtorCount());
        }

        if (params.equalsIgnoreCase("tax_outstanding_debt")) {
            return String.format("%.2f", taxManager.getTotalOutstandingDebt());
        }

        if (params.equalsIgnoreCase("tax_penalty_rate")) {
            return String.format("%.0f%%", taxManager.getLatePenaltyRate() * 100);
        }

        // === PLAYER-SPECIFIC PLACEHOLDERS ===
        if (player != null) {
            var playerData = plugin.getDataManager().getPlayerData(player.getUniqueId());

            // Government role
            if (params.equalsIgnoreCase("is_president")) {
                return plugin.getGovernmentManager().isPresident(player.getUniqueId()) ? "Yes" : "No";
            }

            if (params.equalsIgnoreCase("is_cabinet")) {
                return plugin.getGovernmentManager().isCabinetMember(player.getUniqueId()) ? "Yes" : "No";
            }

            if (params.equalsIgnoreCase("cabinet_position")) {
                CabinetPosition pos = plugin.getGovernmentManager().getCabinetPosition(player.getUniqueId());
                return pos != null ? pos.getDisplayName() : "None";
            }

            // Player statistics (lifetime)
            if (playerData != null) {
                if (params.equalsIgnoreCase("votes_cast")) {
                    return String.valueOf(playerData.getTotalVotesCast());
                }

                if (params.equalsIgnoreCase("times_ran")) {
                    return String.valueOf(playerData.getTimesRanForPresident());
                }

                if (params.equalsIgnoreCase("times_president")) {
                    return String.valueOf(playerData.getTimesServedAsPresident());
                }

                if (params.equalsIgnoreCase("times_cabinet")) {
                    return String.valueOf(playerData.getTimesServedAsCabinet());
                }

                if (params.equalsIgnoreCase("endorsements_given")) {
                    return String.valueOf(playerData.getEndorsementsGiven());
                }

                if (params.equalsIgnoreCase("endorsements_received")) {
                    return String.valueOf(playerData.getEndorsementsReceived());
                }

                if (params.equalsIgnoreCase("total_donations")) {
                    return String.format("%.2f", playerData.getTotalDonations());
                }

                if (params.equalsIgnoreCase("playtime_hours")) {
                    return String.format("%.1f", playerData.getPlaytimeHours());
                }

                // Arena statistics
                if (params.equalsIgnoreCase("arena_kills")) {
                    return String.valueOf(playerData.getArenaKills());
                }

                if (params.equalsIgnoreCase("arena_deaths")) {
                    return String.valueOf(playerData.getArenaDeaths());
                }

                if (params.equalsIgnoreCase("arena_kd")) {
                    int deaths = playerData.getArenaDeaths();
                    if (deaths == 0)
                        return String.format("%.2f", (double) playerData.getArenaKills());
                    return String.format("%.2f", (double) playerData.getArenaKills() / deaths);
                }

                if (params.equalsIgnoreCase("killstreak")) {
                    return String.valueOf(playerData.getCurrentKillstreak());
                }

                if (params.equalsIgnoreCase("best_killstreak")) {
                    return String.valueOf(playerData.getBestKillstreak());
                }

                // === TAX SYSTEM PLACEHOLDERS (Player-specific) ===
                PlayerTaxData taxData = taxRecord.getPlayerTaxData(player.getUniqueId().toString());

                if (params.equalsIgnoreCase("tax_status")) {
                    if (taxData == null)
                        return "N/A";
                    if (taxData.isExempt())
                        return "Exempt";
                    if (taxData.getOutstandingDebt() > 0)
                        return "In Debt";
                    return "Paid";
                }

                if (params.equalsIgnoreCase("tax_debt")) {
                    return taxData != null ? String.format("%.2f", taxData.getOutstandingDebt()) : "0.00";
                }

                if (params.equalsIgnoreCase("tax_missed")) {
                    return taxData != null ? String.valueOf(taxData.getMissedPayments()) : "0";
                }

                if (params.equalsIgnoreCase("tax_is_exempt")) {
                    return taxData != null && taxData.isExempt() ? "Yes" : "No";
                }

                if (params.equalsIgnoreCase("tax_last_paid")) {
                    if (taxData == null || taxData.getLastPaymentTime() == 0)
                        return "Never";
                    return DateTimeFormatter.ofPattern("dd MMM yyyy")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(taxData.getLastPaymentTime()));
                }

                if (params.equalsIgnoreCase("tax_total_paid")) {
                    return taxData != null ? String.format("%.2f", taxData.getTotalAmountPaid()) : "0.00";
                }
            }

            // Current election participation
            if (params.equalsIgnoreCase("has_voted")) {
                return plugin.getElectionManager().getElection().hasVoted(player.getUniqueId()) ? "Yes" : "No";
            }

            if (params.equalsIgnoreCase("is_candidate")) {
                return plugin.getElectionManager().getElection().getCandidate(player.getUniqueId()) != null ? "Yes"
                        : "No";
            }

            // Candidate-specific data
            var candidate = plugin.getElectionManager().getElection().getCandidate(player.getUniqueId());
            if (candidate != null) {
                if (params.equalsIgnoreCase("candidate_votes")) {
                    return String.format("%.1f",
                            plugin.getElectionManager().getElection().getCandidateVotes(player.getUniqueId()));
                }

                if (params.equalsIgnoreCase("candidate_endorsements")) {
                    return String.valueOf(candidate.getEndorsementCount());
                }

                if (params.equalsIgnoreCase("candidate_slogan")) {
                    return candidate.getSlogan() != null ? candidate.getSlogan() : "None";
                }
            }
        }

        return null;
    }

    private String getCabinetMemberName(CabinetPosition position) {
        var member = plugin.getGovernmentManager().getGovernment().getCabinetMemberObject(position);
        return member != null ? member.getName() : "None";
    }

    private String formatTime(long millis) {
        if (millis <= 0)
            return "0s";
        long seconds = millis / 1000;
        long days = seconds / (24 * 3600);
        long hours = (seconds % (24 * 3600)) / 3600;
        long minutes = (seconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0)
            sb.append(days).append("d ");
        if (hours > 0)
            sb.append(hours).append("h ");
        if (minutes > 0)
            sb.append(minutes).append("m");
        if (sb.length() == 0)
            return seconds + "s";
        return sb.toString().trim();
    }
}
