package id.nationcore.managers;

import id.nationcore.NationCore;
import id.nationcore.models.*;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.UUID;

public class DiplomacyManager {

    private final NationCore plugin;

    public DiplomacyManager(NationCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Renders a status' coloured name for raw {@link Player#sendMessage} output.
     * {@link DiplomacyStatus#getColoredName()} carries ampersand colour codes,
     * which Bukkit does not translate automatically in raw chat.
     */
    private String colored(DiplomacyStatus status) {
        return ChatColor.translateAlternateColorCodes('&', status.getColoredName());
    }

    public boolean canManageDiplomacy(UUID playerUUID, Nation nation) {
        if (nation == null) return false;

        if (nation.getLeaderUUID().equals(playerUUID)) {
            return true;
        }

        if (nation.getType() == GovernmentType.REPUBLIC) {
            Government gov = nation.getRepublicGovernment();
            if (gov != null) {
                UUID modUUID = gov.getCabinetMember(Government.CabinetPosition.DEFENSE);
                return playerUUID.equals(modUUID);
            }
        } else if (nation.getType() == GovernmentType.COMMUNIST) {
            CommunistGovernment gov = nation.getCommunistGovernment();
            if (gov != null) {
                CommunistGovernment.PolitburoMember mod = gov.getPolitburoMember(CommunistGovernment.PolitburoPosition.DEFENSE);
                if (mod != null && mod.getUuid().equals(playerUUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void proposeDiplomacy(Player sender, Nation senderNation, Nation targetNation, DiplomacyStatus targetStatus) {
        if (senderNation.getId().equals(targetNation.getId())) {
            sender.sendMessage(ChatColor.RED + "You cannot propose diplomacy to your own nation.");
            return;
        }

        if (!canManageDiplomacy(sender.getUniqueId(), senderNation)) {
            sender.sendMessage(ChatColor.RED + "Only the Leader and the Defense Minister may propose diplomacy.");
            return;
        }

        DiplomacyStatus currentStatus = senderNation.getDiplomacyStatusWith(targetNation.getId());
        if (currentStatus == targetStatus) {
            sender.sendMessage(ChatColor.RED + "Your relation with " + targetNation.getName() + " is already " + colored(targetStatus) + ChatColor.RED + ".");
            return;
        }

        // Block if there is already a pending outgoing request to this target
        DiplomacyRequest existingRequest = targetNation.getDiplomacyRequest(senderNation.getId());
        if (existingRequest != null) {
            sender.sendMessage(ChatColor.RED + "Your nation already has a pending proposal to " + targetNation.getName() + ". Awaiting their response.");
            return;
        }

        DiplomacyRequest newRequest = new DiplomacyRequest(
                senderNation.getId(),
                targetNation.getId(),
                targetStatus,
                sender.getUniqueId()
        );

        targetNation.addDiplomacyRequest(newRequest);

        sender.sendMessage(ChatColor.GREEN + "Successfully proposed " + colored(targetStatus) + ChatColor.GREEN + " status to " + targetNation.getName() + ".");

        // Notify the target nation's leader so they can respond
        Player targetLeader = plugin.getServer().getPlayer(targetNation.getLeaderUUID());
        if (targetLeader != null && targetLeader.isOnline()) {
            targetLeader.sendMessage(ChatColor.YELLOW + "[DIPLOMACY] " + senderNation.getName() + " proposed " + colored(targetStatus) + ChatColor.YELLOW + " status. Use /nc diplomacy accept " + senderNation.getName() + " to respond.");
        }
    }

    public void acceptDiplomacy(Player responder, Nation targetNation, String senderNationId) {
        if (!canManageDiplomacy(responder.getUniqueId(), targetNation)) {
            responder.sendMessage(ChatColor.RED + "Only the Leader and the Defense Minister may respond to diplomacy.");
            return;
        }

        DiplomacyRequest request = targetNation.getDiplomacyRequest(senderNationId);
        if (request == null) {
            responder.sendMessage(ChatColor.RED + "There is no pending proposal from that nation.");
            return;
        }

        NationManager nationManager = plugin.getNationManager();
        Nation senderNation = nationManager.getNation(senderNationId);

        if (senderNation != null) {
            DiplomacyStatus status = request.getRequestedStatus();

            // Set two-way
            senderNation.setDiplomacyStatus(targetNation.getId(), status);
            targetNation.setDiplomacyStatus(senderNation.getId(), status);

            responder.sendMessage(ChatColor.GREEN + "Proposal accepted. Your relation with " + senderNation.getName() + " is now " + colored(status) + ChatColor.GREEN + ".");

            Player senderPlayer = plugin.getServer().getPlayer(request.getRequestedBy());
            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(ChatColor.GREEN + "[DIPLOMACY] " + targetNation.getName() + " ACCEPTED your proposal. Relation is now: " + colored(status));
            }
        } else {
            responder.sendMessage(ChatColor.RED + "The proposing nation could not be found.");
        }

        targetNation.removeDiplomacyRequest(senderNationId);
    }

    public void rejectDiplomacy(Player responder, Nation targetNation, String senderNationId) {
        if (!canManageDiplomacy(responder.getUniqueId(), targetNation)) {
            responder.sendMessage(ChatColor.RED + "Only the Leader and the Defense Minister may respond to diplomacy.");
            return;
        }

        DiplomacyRequest request = targetNation.getDiplomacyRequest(senderNationId);
        if (request == null) {
            responder.sendMessage(ChatColor.RED + "There is no pending proposal from that nation.");
            return;
        }

        NationManager nationManager = plugin.getNationManager();
        Nation senderNation = nationManager.getNation(senderNationId);

        if (senderNation != null) {
            responder.sendMessage(ChatColor.YELLOW + "You REJECTED the proposal from " + senderNation.getName() + ".");

            Player senderPlayer = plugin.getServer().getPlayer(request.getRequestedBy());
            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(ChatColor.RED + "[DIPLOMACY] " + targetNation.getName() + " REJECTED your proposal.");
            }
        }

        targetNation.removeDiplomacyRequest(senderNationId);
    }
}
